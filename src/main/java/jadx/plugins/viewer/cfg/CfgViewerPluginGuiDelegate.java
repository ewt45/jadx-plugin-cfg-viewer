package jadx.plugins.viewer.cfg;

import java.util.Objects;

import javax.swing.JFrame;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.ui.MainWindow;

/**
 * 如果 plugin 检测到 gui 环境，则交由这个类来处理实际操作，因为 plugin 需要避免用到 gui 类 以防 cli 运行出错。
 * <p>
 * plugin 只应调用静态方法。
 */
public class CfgViewerPluginGuiDelegate {
	private static final CfgViewerPluginGuiDelegate instance = new CfgViewerPluginGuiDelegate();

	private final MyPluginOptions options = new MyPluginOptions();


	/** {@link JadxPlugin#init(JadxPluginContext)} 时，且 gui 存在时调用。 */
	public static void onPluginInit(JadxPluginContext context) {
		instance.onPluginInitInternal(context);
	}

	/** {@link JadxPlugin#unload()} 时调用。 */
	public static void onPluginUnload() {
		instance.onPluginUnloadInternal();
	}


	private void onPluginInitInternal(JadxPluginContext context) {
		JadxGuiContext guiCtx = Objects.requireNonNull(context.getGuiContext());
		// 本地化（等 jadx 侧实现）
//		MainWindow mainWindow = getMainWindow();
//		NLS.init(mainWindow.getSettings().getLangLocale().get());
		// 用于后续获取临时文件夹
		options.setFiles(context.files());
		// 注册首选项中的插件选项
		context.registerOptions(options);
		// 注册右键菜单项
		guiCtx.addPopupMenuAction(
				NLS.optionViewCFG,
				iCodeNodeRef -> iCodeNodeRef instanceof MethodNode,
				null,
				iCodeNodeRef -> {
					if (!(iCodeNodeRef instanceof MethodNode)) throw new RuntimeException(NLS.exceptionNotMethodNode);
					MethodNode methodNode = (MethodNode) iCodeNodeRef;
					CfgJNode jNode = new CfgJNode(methodNode, options);
					getMainWindow(guiCtx).getTabsController().selectTab(jNode);
				}
		);
	}

	public static MainWindow getMainWindow(JadxGuiContext guiCtx) {
		if (guiCtx == null) throw new RuntimeException("JadxGuiContext = null");
		JFrame frame = guiCtx.getMainFrame();
		if (!(frame instanceof MainWindow)) throw new RuntimeException(NLS.exceptionNoMainWindow);
		return (MainWindow) frame;
	}

	private void onPluginUnloadInternal() {
	}
}
