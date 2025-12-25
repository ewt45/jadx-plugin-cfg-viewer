package jadx.plugins.viewer.cfg;

import java.nio.file.Path;

import jadx.api.JadxArgs;
import jadx.api.plugins.data.IJadxFiles;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;
import jadx.core.Jadx;

/**
 * 如果添加了属性请在复制构造函数中复制属性
 */
public class MyPluginOptions extends BasePluginOptionsBuilder {
	private DumpType defaultDumpType = DumpType.GENERAL;
	private IJadxFiles files;

	@Override
	public void registerOptions() {
		enumOption(CfgViewerPlugin.PLUGIN_ID + ".defaultDumpType", DumpType.values(), DumpType::valueOf)
				.description(NLS.prefDefaultDumpType)
				.defaultValue(DumpType.GENERAL)
				.setter(v -> defaultDumpType = v);
	}

	public DumpType getDefaultDumpType() {
		return defaultDumpType;
	}

	public Path getTempDir() {
		return files.getPluginTempDir();
	}

	public void setFiles(IJadxFiles files) {
		this.files = files;
	}

	public enum DumpType {
		GENERAL(2),
		RAW(1),
		REGION(3);

		/**
		 * 在 {@link Jadx#getRegionsModePasses(JadxArgs)} 中出现的顺序，从 1 开始。
		 */
		public final int orderInRegionsModePasses;

		DumpType(int i) {
			orderInRegionsModePasses = i;
		}
	}
}
