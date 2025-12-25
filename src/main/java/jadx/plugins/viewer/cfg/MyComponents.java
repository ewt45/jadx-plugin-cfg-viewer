package jadx.plugins.viewer.cfg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;

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
}
