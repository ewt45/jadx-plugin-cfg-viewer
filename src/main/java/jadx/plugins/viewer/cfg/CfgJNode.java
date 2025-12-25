package jadx.plugins.viewer.cfg;

import static jadx.api.resources.ResourceContentType.CONTENT_BINARY;

import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jadx.api.resources.ResourceContentType;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.UiUtils;

public class CfgJNode extends JNode {
	private static final long serialVersionUID = 3745790873881800827L;
	private static final ImageIcon ICON = UiUtils.openSvgIcon("nodes/ImagesFileType");
	private final MethodNode methodNode;
	private final MyPluginOptions options;

	public CfgJNode(MethodNode node, MyPluginOptions options) {
		methodNode = node;
		this.options = options;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public String makeString() {
		return methodNode.getMethodInfo().makeSignature(true);
	}

	@Override
	public Icon getIcon() {
		return ICON;
	}

	// FIXME jadx 1.5.3 之后才有这个函数。等 1.5.4 发布了就加上
//	@Override
	public boolean hasContent() {
		return true;
	}

	@Override
	public ResourceContentType getContentType() {
		return CONTENT_BINARY;
	}

	@Override
	public @Nullable ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new CfgImagePanel(tabbedPane, this);
	}

	public MethodNode getMethodNode() {
		return methodNode;
	}

	public MyPluginOptions getPluginOptions() {
		return options;
	}
}
