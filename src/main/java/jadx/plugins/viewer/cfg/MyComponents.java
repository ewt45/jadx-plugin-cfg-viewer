package jadx.plugins.viewer.cfg;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Function;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import kotlin.Pair;

class MyComponents {
	/**
	 * 顶部工具栏。位于左上角。背景色为白色半透明。
	 */
	public static JPanel topbarPanel() {
		// setAlignmentX/Y 和我想得不一样。直接重写 setBounds 强制设置坐标为 0,0 吧。外部调用设置坐标可以用 setLocation
		JPanel topbarPanel = new JPanel() {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				super.setBounds(0, 0, width, height);
			}
		};
		topbarPanel.setLayout(new BoxLayout(topbarPanel, BoxLayout.Y_AXIS));
		// 只有 isOpaque 为 true 时颜色才会显示
		topbarPanel.setBackground(moreTransparent(backgroundColor()));
		return topbarPanel;
	}

	/**
	 * 选择 dump type 的按钮组。
	 *
	 * @param defaultDumpType 初始时选中哪个。
	 */
	public static JPanel radioButtonsPanel(MyPluginOptions.DumpType defaultDumpType, ItemListener itemListener) {
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setAlignmentX(0);
		buttonsPanel.setAlignmentY(0);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

		ButtonGroup buttonGroup = new ButtonGroup();
		for (MyPluginOptions.DumpType option : MyPluginOptions.DumpType.values()) {
			JRadioButton btn = new JRadioButton(option.name());
			if (option == defaultDumpType)
				btn.setSelected(true);
			btn.setMargin(new Insets(8, 8, 8, 8));
			btn.addItemListener(itemListener);
			buttonGroup.add(btn);
			buttonsPanel.add(btn);
		}
		return buttonsPanel;
	}

	/**
	 * 复制文字或图片到剪切板的按钮。两个按钮宽度各占一半。
	 */
	public static JPanel copyBtnPanel(JPanel topbarPanel) {
		JPanel copyPanel = new JPanel();
		copyPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
		copyPanel.setLayout(new BoxLayout(copyPanel, BoxLayout.X_AXIS));
		copyPanel.setAlignmentX(0);
		JButton copyText = new JButton("复制为文本") {
			@Override
			public Dimension getMaximumSize() {
				Dimension dimension = super.getMaximumSize();
				dimension.width = topbarPanel.getWidth() / 2;
				return dimension;
			}
		};
		JButton copyImage = new JButton("复制为图片") {
			@Override
			public Dimension getMaximumSize() {
				Dimension dimension = super.getMaximumSize();
				dimension.width = topbarPanel.getWidth() / 2;
				return dimension;
			}
		};
		copyPanel.add(copyText);
		copyPanel.add(copyImage);
		return copyPanel;
	}

	/**
	 * 显示文字 “加载中”。画面居中。
	 *
	 * @param parent 其父组件。用于获取居中时的总宽高。
	 */
	public static JLabel loadingLabel(JComponent parent) {
		return new JLabel(NLS.textLoading) {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				Point centeredXY = getCenteredXY(x, y, parent, this);
				super.setBounds(centeredXY.x, centeredXY.y, width, height);
			}
		};
	}

	/** 从 {@link UIManager#getColor(Object)} 获取背景色。 */
	public static Color backgroundColor() {
		Color bgColor = UIManager.getColor("Panel.background");
		if (bgColor == null) bgColor = Color.WHITE;
		return bgColor;
	}

	/**
	 * 将给定的颜色 alpha 减少1/5
	 */
	public static Color moreTransparent(Color color) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() * 4 / 5);
	}

	/** @see #moreTransparent(Color) */
	public static void moreTransparent(JComponent component) {
		component.setBackground(moreTransparent(component.getBackground()));
	}

	/**
	 * 用于在 {@link JComponent#setBounds(int, int, int, int)} 中获取该组件居中时的 xy。
	 * 如果宽或高超出 parent, 则 xy 改为 0,0 (左上角）
	 *
	 * @param parent 用于获取总宽高
	 */
	public synchronized static Point getCenteredXY(int x, int y, JComponent parent, JComponent child) {
		Dimension parentSize = parent.getSize();
		Dimension childSize = child.getSize();
		Point centeredXY = new Point();
		centeredXY.setLocation(x, y);
		if (!((parentSize.width <= 0 || parentSize.height <= 0 || childSize.width <= 0 || childSize.height <= 0))) {
			int centeredX = (parentSize.width - childSize.width) / 2;
			int centeredY = (parentSize.height - childSize.height) / 2;
			// 若居中超出 parent 范围，则改回位于左上角
			centeredXY.setLocation(Math.max(centeredX, 0), Math.max(centeredY, 0));
		}
		return centeredXY;
	}

	public static JPopupMenu popupMenu(Function<Void, Pair<String, Image>> contentGetter) {
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem copyText = new JMenuItem("复制为文本");
		copyText.addActionListener(e -> {
			Pair<String, Image> pair = contentGetter.apply(null);
			String text = pair == null ? null : pair.getFirst();
			if (text == null) return;
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(contentGetter.apply(null).getFirst()), null);
		});
		JMenuItem copyImage = new JMenuItem("复制为图片");
		copyImage.addActionListener(e -> {
			Pair<String, Image> pair = contentGetter.apply(null);
			Image rawImage = pair == null ? null : pair.getSecond();
			CfgViewerPlugin.LOG.debug("复制图片：{}", rawImage);
			if (pair == null) return;
//			// 不转换格式会报错 啊没事了是判断 flavor 写反了
//			BufferedImage formattedImage = new BufferedImage(rawImage.getWidth(null), rawImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
//			Graphics2D g = formattedImage.createGraphics();
//			g.drawImage(rawImage, 0, 0, null);
//			g.dispose();
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {

				@Override
				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[]{DataFlavor.imageFlavor};
				}

				@Override
				public boolean isDataFlavorSupported(DataFlavor flavor) {
					return DataFlavor.imageFlavor.equals(flavor);
				}

				@Override
				public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
					if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
					return rawImage;
				}
			}, null);
		});
		popupMenu.add(copyText);
		popupMenu.add(copyImage);

		return popupMenu;
	}
}
