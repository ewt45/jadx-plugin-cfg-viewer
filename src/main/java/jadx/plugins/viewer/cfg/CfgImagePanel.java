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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

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
import kotlin.Pair;

public class CfgImagePanel extends ContentPanel {
	private static final long serialVersionUID = -4676535827617942121L;

	private final PCanvas pCanvas;
	private final PImage pImage;
	private final JLabel dotVersionLabel;
	private final MyPluginOptions options;
	private final HashMap<MyPluginOptions.DumpType, Pair<String, Image>> imageCaches = new HashMap<>();
	private MyPluginOptions.DumpType currentDumpType;

	public CfgImagePanel(TabbedPane panel, CfgJNode node) {
		super(panel, node);
		setLayout(new OverlayLayout(this));

		MethodNode methodNode = node.getMethodNode();
		options = node.getPluginOptions();
		currentDumpType = options.getDefaultDumpType();

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
		pImageContainer.addInputEventListener(MyListeners.imageZoom(pCanvas, pImage));

		// 图片显示或切换时，缩放至显示全部内容
		pImage.addPropertyChangeListener(PImage.PROPERTY_IMAGE, evt -> {
			if (evt.getNewValue() == null) return;
			MyListeners.imageFit(pCanvas, pImage);
		});

		// 右键菜单，复制文本或图片
		JPopupMenu contextMenu = MyComponents.imagePopupMenu(v -> imageCaches.get(currentDumpType));
		pImageContainer.addInputEventListener(MyListeners.popupMenu(contextMenu));

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

		pCanvas.setVisible(false);

		// 非 ui 线程参考 BinaryContentPanel
		SwingUtilities.invokeLater(() -> processAndDisplay(methodNode, options.getDefaultDumpType()));
	}


	/**
	 * 从 MethodNode 创建控制流图并转为 png 图片，显示在屏幕上。
	 * 如果出错则显示报错
	 */
	private void processAndDisplay(MethodNode methodNode, MyPluginOptions.DumpType dumpType) {
		currentDumpType = dumpType;
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

				String dotVersion = getDotVersion();
				Image bufferedImage;
				if (imageCaches.get(dumpType) != null) {
					bufferedImage = imageCaches.get(dumpType).getSecond();
				} else {
					// methodNode -> cfg -> png
					File dotFile = dumpCFG(methodNode, dumpType);
					File pngFile = dot2Png(dotFile);
					bufferedImage = ImageIO.read(pngFile);
					imageCaches.put(dumpType, new Pair<>(Files.readString(dotFile.toPath()), bufferedImage));
				}

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

		// 多次加载会导致 Block ID 出错，所以仅加载一次。
		if (!methodNode.isLoaded()) {
			// 先 reload 生成 insn, 然后 passes 生成 block. DotGraphVisitor 需要 block
			methodNode.reload();

			List<IDexTreeVisitor> passes = Jadx.getRegionsModePasses(methodNode.root().getArgs());
			for (IDexTreeVisitor pass : passes) pass.init(methodNode.root());
			for (IDexTreeVisitor pass : passes) pass.visit(methodNode);
		}

		if (dumpType == MyPluginOptions.DumpType.GENERAL) DotGraphVisitor.dump().save(dotDir, methodNode);
		else if (dumpType == MyPluginOptions.DumpType.RAW) DotGraphVisitor.dumpRaw().save(dotDir, methodNode);
		else if (dumpType == MyPluginOptions.DumpType.REGION) DotGraphVisitor.dumpRegions().save(dotDir, methodNode);

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
		imageCaches.clear();
	}
}
