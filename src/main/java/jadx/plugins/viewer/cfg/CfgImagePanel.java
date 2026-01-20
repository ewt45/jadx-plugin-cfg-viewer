package jadx.plugins.viewer.cfg;


import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.piccolo2d.PCanvas;
import org.piccolo2d.PLayer;
import org.piccolo2d.nodes.PImage;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;
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
	// 存储 dumType 对应的 cfg 文字和图片
	private final CfgDotData dotData;
	private MyPluginOptions.DumpType currentDumpType;

	public CfgImagePanel(TabbedPane panel, CfgJNode node) {
		super(panel, node);
		setLayout(new OverlayLayout(this));

		MethodNode methodNode = node.getMethodNode();
		options = node.getPluginOptions();
		dotData = node.getCfgDotData();
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
		JPopupMenu contextMenu = MyComponents.imagePopupMenu(v ->
				new Pair<>(dotData.getDot(currentDumpType), dotData.getImage(currentDumpType)));
		pImageContainer.addInputEventListener(MyListeners.popupMenu(contextMenu));

		// 左上角显示 dot 版本
		dotVersionLabel = new JLabel();
		dotVersionLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

		// 单选按钮组 dump type
		// 为什么这个要设置 alignment 为 0 否则 label 会串位置呢
		JPanel radioPanel = MyComponents.radioButtonsPanel(options.getDefaultDumpType(), e -> {
			if (e.getStateChange() != ItemEvent.SELECTED) return;
			onChangeDumpType(MyPluginOptions.DumpType.valueOf(((JRadioButton) e.getSource()).getText()));
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
		SwingUtilities.invokeLater(() -> initImageCache(methodNode));
	}

	/**
	 * 初始化图片缓存。该函数应仅在初始时执行一次
	 */
	private void initImageCache(MethodNode methodNode) {
		getMainWindow().getBackgroundExecutor().startLoading(() -> {
			// 1. 显示加载中
			// 2. 从 methodNode 获取 dot, 再转为 png (新建线程？），存入 cache
			// 3. 显示 png 或错误
			try {
				UiUtils.notUiThreadGuard();

				UiUtils.uiRunAndWait(() -> {
					pCanvas.setVisible(false);
					pImage.setImage((Image) null);
				});

				String dotVersion = getDotVersion();
				dotData.loadDot(methodNode, options.getTempDir());
				dotData.loadImage(options.getTempDir());

				// 显示
				UiUtils.uiRunAndWait(() -> {
					dotVersionLabel.setText(dotVersion);
					pImage.setImage(dotData.getImage(currentDumpType));
					pCanvas.setVisible(true);
				});
			} catch (Exception e) {
				CfgViewerPlugin.LOG.error("", e);

				// 直接在构造函数里用 uiRun 会报错？
				UiUtils.uiRunAndWait(() -> {
					RSyntaxTextArea infoText = AbstractCodeArea.getDefaultArea(getMainWindow());
					infoText.setText(NLS.textDumpErrorHappened + Utils.getStackTrace(e));
					RTextScrollPane scrollPane = new RTextScrollPane(infoText);

					removeAll();
					add(scrollPane);
				});
			}
		});
	}

	/** 用户切换 dumpType 时，切换到对应的图片显示 */
	private void onChangeDumpType(MyPluginOptions.DumpType dumpType) {
		currentDumpType = dumpType;
		// 加入到 executor 确保在初始化后执行
		getMainWindow().getBackgroundExecutor().startLoading(() -> {
			UiUtils.uiRunAndWait(() -> {
				pImage.setImage(dotData.getImage(currentDumpType));
			});
		});
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
		dotData.unloadImage();
	}
}
