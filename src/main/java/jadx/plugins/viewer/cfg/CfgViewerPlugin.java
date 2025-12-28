package jadx.plugins.viewer.cfg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;


/*
META-INF 中 指向该类

创建临时文件时应该在 CfgViewerPlugin#tmpDir 中。
插件设置参考 JavaConvertOptions。
多语言：目前 plugin info 和 首选项 不支持实时刷新。就用 Locale.getDefault() 吧。（ jadx 的 locale 获取：mainWindow.getSettings().getLangLocale() ）
注意在没获取到 guiContext 之前别用那些图形类（成员属性，函数返回值也不行）

如何显示 dot？本地 dot 命令创建 png？还是第三方 java swing 库解析 dot 并显示，还是网页？
图片显示用 piccolo2d https://piccolo2d.org/learn/out-of-box-java.html

图形库依赖用的 compileOnly,

构建 jar：
	1. git 添加 tag
	2. github action 里输入 tag 生成 artifact
	3. 创建 release, 添加 jar 文件，命名格式 <仓库名>-<版本号>.jar
	4. 确保可以 从 cli/gui 安装: jadx plugins --install github:ewt45:jadx-plugin-cfg-viewer


调试：
	1. 复制到 jadx 项目中
	2. 根目录 settings.gradle.kts 和 jadx-cli 的build.gradle.kts 添加该模块依赖
	3. 算了直接在 jadx 项目新建一个分支吧。来回只需要复制 java 代码部分即可。
 */

// TODO MyListeners.imageFitNow 和 缩ImageZoom 缩放范围根据图片尺寸而定
// TODO 等待官方版本更新：1.5.4 后 CfgJNode 里重写 hasContent 方法。等待支持多语言。
// FIXME 1. 全部 passes 执行完，dump 的和在中间 dump 的不一样。2. block id 还是不对，多了一轮

public class CfgViewerPlugin implements JadxPlugin {
	static final Logger LOG = LoggerFactory.getLogger(CfgViewerPlugin.class);
	public static final String PLUGIN_ID = "cfg-viewer-ewt45";
	private boolean isGui = false;

	@Override
	public JadxPluginInfo getPluginInfo() {
		JadxPluginInfo info = new JadxPluginInfo(PLUGIN_ID, NLS.infoName, NLS.infoDescription);
		info.setRequiredJadxVersion("1.5.3, r2504");
		info.setHomepage("https://github.com/ewt45/jadx-plugin-cfg-viewer");
		return info;
	}

	@Override
	public synchronized void init(JadxPluginContext context) {
		// 仅当有 gui 时交由另外的类处理，cli 中不访问 gui 相关的类
		if (context.getGuiContext() != null) {
			isGui = true;
			CfgViewerPluginGuiDelegate.onPluginInit(context);
		}
	}

	@Override
	public void unload() {
		if (isGui) {
			CfgViewerPluginGuiDelegate.onPluginUnload();
		}
	}
}
