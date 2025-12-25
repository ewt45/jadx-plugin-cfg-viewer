package jadx.plugins.viewer.cfg;


import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jetbrains.annotations.NotNull;
import org.piccolo2d.PCanvas;
import org.piccolo2d.PLayer;
import org.piccolo2d.nodes.PImage;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import jadx.api.JadxArgs;
import jadx.core.Jadx;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.DotGraphVisitor;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.UiUtils;

public class CfgImagePanel extends ContentPanel {
	private static final long serialVersionUID = -4676535827617942121L;

	private final PCanvas pCanvas;
	private final PImage pImage;
	private final JLabel dotVersionLabel;
	private final MyPluginOptions options;

	public CfgImagePanel(TabbedPane panel, CfgJNode node) {
		super(panel, node);
		setLayout(new OverlayLayout(this));

		MethodNode methodNode = node.getMethodNode();
		options = node.getPluginOptions();

		// 给自身添加一个滚轮监听防止滚到其他 tab
		addMouseWheelListener(e -> {
		});

		// 如果拖拽事件只设置到 image 上，那么移动出界就拽不回来了。所以套一层
		PLayer pImageContainer = new PLayer();
		pImageContainer.setBounds(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)); // 要手动设置宽高否则不会占满 pCanvas

		// 背景色
//			PPath layerBackground = PPath.createRectangle(0 , 0, 1000, 1000);
//			layerBackground.setPaint(Color.RED);
//			layerBackground.setTransparency(0.3f);
//			pImageContainer.addChild(layerBackground);

		pImage = new PImage();
		pImageContainer.addChild(pImage);

		pCanvas = new PCanvas();
		pCanvas.setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		pCanvas.setBackground(MyComponents.backgroundColor());
		// 好像 pCanvas 也可以添加 JComponent? pCanvas.add
		pCanvas.getLayer().addChild(pImageContainer);
		// 禁用原拖拽和缩放, 设置自己的拖拽和缩放。
		pCanvas.setPanEventHandler(null);
		pCanvas.setZoomEventHandler(null);
		pImageContainer.addInputEventListener(MyListeners.imageDrag(pImage));
		pImageContainer.addInputEventListener(MyListeners.imageZoom(pImage));

		// 图片显示或切换时，缩放至显示全部内容
		pImage.addPropertyChangeListener(PImage.PROPERTY_IMAGE, evt -> {
			if (evt.getNewValue() == null) return;
			MyListeners.imageFit(pCanvas, pImage);
		});

		// 左上角显示 dot 版本
		dotVersionLabel = new JLabel();
		dotVersionLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

		// 单选按钮组 dump type
		// 为什么这个要设置 alignment 为 0 否则 label 会串位置呢
		JPanel radioPanel = MyComponents.radioButtonsPanel(options.getDefaultDumpType(), e -> {
			if (e.getStateChange() != ItemEvent.SELECTED) return;
			MyPluginOptions.DumpType dumpType = MyPluginOptions.DumpType.valueOf(((JRadioButton) e.getSource()).getText());
			processAndDisplay(methodNode, dumpType);
		});

		JPanel topbarPanel = MyComponents.topbarPanel();
		topbarPanel.add(dotVersionLabel);
		topbarPanel.add(radioPanel);

		JLabel loadingLabel = MyComponents.loadingLabel(this);

		// 注意：OverlayLayout Panel 添加的控件，重写 setBounds 强制设置 x,y
		add(topbarPanel);
		add(pCanvas);
		add(loadingLabel);

		pCanvas.requestFocus();
		pCanvas.setVisible(false);

		// 非 ui 线程参考 BinaryContentPanel
		SwingUtilities.invokeLater(() -> processAndDisplay(methodNode, options.getDefaultDumpType()));
	}


	/**
	 * 从 MethodNode 创建控制流图并转为 png 图片，显示在屏幕上。
	 * 如果出错则显示报错
	 */
	private void processAndDisplay(MethodNode methodNode, MyPluginOptions.DumpType dumpType) {
		getMainWindow().getBackgroundExecutor().startLoading(() -> {
			// 1. 显示加载中
			// 2. 从 methodNode 获取 dot, 再转为 png (新建线程？）
			// 3. 显示 png 或错误
			try {
				UiUtils.notUiThreadGuard();

				UiUtils.uiRunAndWait(() -> {
					pCanvas.setVisible(false);
					pImage.setImage((Image) null);
				});

				// methodNode -> cfg -> png
				String dotVersion = getDotVersion();
				File dotFile = dumpCFG(methodNode, dumpType);
				File pngFile = dot2Png(dotFile);
				BufferedImage bufferedImage = ImageIO.read(pngFile);

				// 显示
				UiUtils.uiRunAndWait(() -> {
					dotVersionLabel.setText(dotVersion);
					pImage.setImage(bufferedImage);
					pCanvas.setVisible(true);
				});
			} catch (Exception e) {
				CfgViewerPlugin.LOG.error("", e);

				// 直接在构造函数里用 uiRun 会报错？
				RSyntaxTextArea infoText = AbstractCodeArea.getDefaultArea(getMainWindow());
				infoText.setText(NLS.textDumpErrorHappened + Utils.getStackTrace(e));
				RTextScrollPane scrollPane = new RTextScrollPane(infoText);

				removeAll();
				add(scrollPane);
			}
		});
	}


	@NotNull
	private File dumpCFG(MethodNode methodNode, MyPluginOptions.DumpType dumpType) throws IOException, JadxException {
		File dotDir = Files.createTempDirectory(options.getTempDir(), "jadx-dot").toFile();
		dotDir.deleteOnExit();

		// 先 reload 生成 insn, 然后 passes 生成 block. DotGraphVisitor 需要 block
		// 参考 Jadx.getRegionsModePasses() 中, 根据 args 在不同阶段创建了 3 个 DotGraphVisitor
		// 在 visit 阶段找到这 3 个改为调用 save()
		methodNode.reload();

		// 为 args 设置 cfgOutput, 然后从获得的 pass 里判断 DotGraphVisitor，手动save
		JadxArgs args = methodNode.root().getArgs();
		boolean[] originalCfgOutput = {args.isRawCFGOutput(), args.isCfgOutput()};
		try {
			args.setCfgOutput(true);
			args.setRawCFGOutput(true);
			List<IDexTreeVisitor> passes = Jadx.getRegionsModePasses(args);
			if (3 != passes.stream().filter(it -> it instanceof DotGraphVisitor).count()) {
				throw new JadxRuntimeException(NLS.exceptionVisitorNot3);
			}

			for (IDexTreeVisitor pass : passes) {
				pass.init(methodNode.root());
			}

			int dotPassCount = 0;
			for (IDexTreeVisitor pass : passes) {
				if (!(pass instanceof DotGraphVisitor)) {
					pass.visit(methodNode);
					continue;
				}
				// 找到对应类型的 DotGraphVisitor, 手动保存
				dotPassCount++;
				if (dotPassCount == dumpType.orderInRegionsModePasses) {
					((DotGraphVisitor) pass).save(dotDir, methodNode);
				}
			}
		} finally {
			// 恢复原本设置
			args.setRawCFGOutput(originalCfgOutput[0]);
			args.setCfgOutput(originalCfgOutput[1]);
		}

		File dotFile = dotDir;
		while (dotFile != null && dotFile.isDirectory()) {
			File[] list = dotFile.listFiles();
			dotFile = (list == null || list.length != 1) ? null : list[0];
		}
		if (dotFile == null) {
			throw new JadxRuntimeException(NLS.exceptionDotFileNull + dotDir.getAbsolutePath());
		}
		CfgViewerPlugin.LOG.debug(NLS.logTmpDotFile, dotFile);
		return dotFile;
	}

	@NotNull
	private File dot2Png(File dotFile) throws IOException, InterruptedException {
		File pngFile = Files.createTempFile(options.getTempDir(), dotFile.getName(), ".png").toFile();
		pngFile.deleteOnExit();
		Process process = new ProcessBuilder("dot", "-Tpng", dotFile.getAbsolutePath(), "-o", pngFile.getAbsolutePath()).start();
		String errorStr;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
			errorStr = reader.lines().collect(Collectors.joining("\n"));
		}
		int result = process.waitFor();
		if (result != 0 || !errorStr.isEmpty()) {
			throw new JadxRuntimeException(String.format(NLS.exceptionDot2PngFail, result, errorStr));
		}
		CfgViewerPlugin.LOG.debug(NLS.logTmpPngFile, pngFile);
		return pngFile;
	}

	/**
	 * 获取电脑上 dot 命令版本。获取失败时会抛出异常
	 */
	private static String getDotVersion() throws JadxRuntimeException {
		String output = "";
		try {
			Process process = new ProcessBuilder("dot", "-V").redirectErrorStream(true).start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				output = reader.lines().collect(Collectors.joining("\n"));
			}
			int result = process.waitFor();
			if (result != 0 || !output.contains("version")) {
				throw new JadxRuntimeException(NLS.exceptionDotNotInstalled);
			}
		} catch (Exception e) {
			throw new JadxRuntimeException(NLS.exceptionDotNotInstalled, e);
		}
		return output;
	}

	@Override
	public void loadSettings() {
		// no op
	}

	// 子布局会重叠。需要返回 false。或者设置 canvas.setOpaque(false);
	@Override
	public boolean isOptimizedDrawingEnabled() {
		return false;
	}

	@Override
	public void dispose() {
		super.dispose();
		pImage.setImage((Image) null);
	}
}
