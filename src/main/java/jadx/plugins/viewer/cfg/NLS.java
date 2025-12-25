package jadx.plugins.viewer.cfg;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;


class NLS {
	private NLS() {
	}

	static {
		init(null);
	}

	/**
	 * 上次初始化时的语言。
	 * 初始化时：
	 * - 如果该值为 null，则按指定语言或系统默认语言初始化。若指定了语言，初始化后应设置到该值上。
	 * - 如果该值不为 null, 则仅当指定语言且与该语言不同时重新初始化。
	 */
	@org.jetbrains.annotations.Nullable
	private static String initedLang = null;


	public static String infoName;
	public static String infoDescription;
	public static String optionViewCFG;
	public static String prefDefaultDumpType;
	public static String textDumpErrorHappened;
	public static String textLoading;
	public static String logNotSupportMouseWheelBlock;
	public static String logTmpDotFile;
	public static String logTmpPngFile;
	public static String exceptionVisitorNot3;
	public static String exceptionDotFileNull;
	public static String exceptionDot2PngFail;
	public static String exceptionDotNotInstalled;
	public static String exceptionNoMainWindow;
	public static String exceptionNotMethodNode;


	/**
	 * 初始化字符串为对应语言。
	 *
	 * @param jadxLocale 不为 null 时强制初始化，为 null 时仅当未初始化过时才初始化。
	 */
	public static void init(@Nullable Locale jadxLocale) {
		String lang = (jadxLocale != null ? jadxLocale : Locale.getDefault()).getLanguage();
		// 若没指定语言，以曾经指定过的语言优先。若指定语言但没有变化则跳过。
		if ((jadxLocale == null && initedLang != null)
				|| (jadxLocale != null && lang.equals(initedLang)))
			return;

		// 若指定语言，记录本次初始化的语言。
		if (jadxLocale != null)
			initedLang = lang;

		if (lang.equals("zh")) {
			infoName = "查看控制流图（CFG）";
			infoDescription = "创建函数的控制流图（CFG），以图片格式显示。需要运行环境已安装 graphviz（用到 'dot' 命令）。" +
					"\n\n使用方法：使用 Jadx-GUI 打开一个反编译后的 Java 代码界面，鼠标放在一个函数名上右键 -> 查看函数控制流图。";
			optionViewCFG = "查看函数控制流图";
			prefDefaultDumpType = "默认控制流图类型";
			logNotSupportMouseWheelBlock = "尚未支持 mouseWheelRotatedByBlock 缩放。";
			textDumpErrorHappened = "生成控制流图时出现错误:\n";
			textLoading = "加载中……";
			logTmpDotFile = "生成临时 dot 文件: {}";
			logTmpPngFile = "生成临时 png 文件: {}";
			exceptionVisitorNot3 = "Jadx.getRegionsModePasses() 返回的 DotGraphVisitor 个数不为 3！代码可能不兼容了！";
			exceptionDotFileNull = "无法找到 .dot 控制流图，原因是 .dot 生成失败或生成了多个。寻找目录：";
			exceptionDot2PngFail = "dot 转 png 失败，返回码 = %d\n错误输出: %s";
			exceptionDotNotInstalled = "无法找到 dot 命令，请安装 graphviz: https://graphviz.org/download/";
			exceptionNoMainWindow = "guiCtx.getMainFrame() 返回类型不再是 MainWindow";
			exceptionNotMethodNode = "选中节点不是 MethodNode";

		} else {
			infoName = "CFG Viewer";
			infoDescription = "Generate method's CFG and display it as image. Graphviz needs to be installed in runtime environment." +
					"\n\nUsage: Open a decompiled Java code tab and right click on a method's name -> View method CFG";
			optionViewCFG = "View method CFG";
			prefDefaultDumpType = "Default CFG type";
			logNotSupportMouseWheelBlock = "mouseWheelRotatedByBlock not implemented.";
			textDumpErrorHappened = "Error occurred while generating CFG:\n";
			textLoading = "Loading ...";
			logTmpDotFile = "Temporary dot file generated: {}";
			logTmpPngFile = "Temporary png file generated: {}";
			exceptionVisitorNot3 = "The number of DotGraphVisitor returned by Jadx.getRegionsModePasses() is not 3！This version of Jadx is not supported！";
			exceptionDotFileNull = ".dot CFG file not found because jadx dump none or multiple .dot files. Directory searched: ";
			exceptionDot2PngFail = "Failed to convert dot to png, exit code = %d\nerror output: %s";
			exceptionDotNotInstalled = "dot command not found. Please install graphviz: https://graphviz.org/download/";
			exceptionNoMainWindow = "guiCtx.getMainFrame() return type is not longer MainWindow";
			exceptionNotMethodNode = "Selected node is not MethodNode";

		}
	}
}
