package jadx.plugins.viewer.cfg;

import org.jetbrains.annotations.NotNull;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import jadx.api.JadxArgs;
import jadx.core.Jadx;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.DotGraphVisitor;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * 存储某个 MethodNode 的 dot 数据。
 * dot 字符串一直保存，图片会在窗口打开时加载，窗口关闭时删除。
 * dot 字符串一直保存主要是因为如果重复 MethodNode.reload() BlockNode 标号会越来越大
 */
public class CfgDotData {

	private final Map<MyPluginOptions.DumpType, String> dotMap = new HashMap<>();
	private final Map<MyPluginOptions.DumpType, BufferedImage> imageMap = new HashMap<>();
	public final String mthFullId;

	public CfgDotData(MethodNode methodNode) {
		this.mthFullId = methodNode.getMethodInfo().getFullId();
	}

	public String getDot(MyPluginOptions.DumpType dumpType) {
		return dotMap.get(dumpType);
	}

	public Image getImage(MyPluginOptions.DumpType dumpType) {
		return imageMap.get(dumpType);
	}

	/**
	 * 遍历一遍 passes, 将 3 个类型的 DotGraphVisitor 执行一遍，获取 dot 文字存入 dotMap
	 */
	public void loadDot(MethodNode methodNode, Path tmpDir) throws Exception {
		if (isDotLoaded()) {
			return;
		}
		// 先 reload 生成 insn, 然后 passes 生成 block. DotGraphVisitor 需要 block
		// FIXME reload 还是会留一些内容
		//  新建进程用 cli 也不行因为从头加载占用内存太大, classNode.reload 也不行和 method.reload 结果一样。
		methodNode.reload();

		// 为 args 设置 cfgOutput, 然后从获得的 pass 里判断 DotGraphVisitor，手动save
		JadxArgs args = methodNode.root().getArgs();
		boolean[] originalCfgOutput = {args.isRawCFGOutput(), args.isCfgOutput()};

		// 参考 Jadx.getRegionsModePasses() 中, 根据 args 在不同阶段创建了 3 个 DotGraphVisitor
		// 在 visit 阶段找到这 3 个改为调用 save().
		// DotGraphVisitor 在 passes 中位置不同生成的 dot 也不同，所以不能全执行完 passes 再手动创建 DotGraphVisitor
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

				// 1. 判断 dumpType 2. 执行 Visitor 3. 找到生成的 dot 文件 4. 读取，存入 cache
				MyPluginOptions.DumpType dumpType = MyPluginOptions.DumpType.getDumTypeByOrder(dotPassCount);
				MyUtils.tempDirDeleteOnFinish(tmpDir, dotDir -> {
					((DotGraphVisitor) pass).save(dotDir, methodNode);
					File dotFile = MyUtils.getOneDotFile(dotDir);
					dotMap.put(dumpType, Files.readString(dotFile.toPath()));
					return 0;
				});
			}
		} finally {
			// 恢复原本设置
			args.setRawCFGOutput(originalCfgOutput[0]);
			args.setCfgOutput(originalCfgOutput[1]);
			methodNode.unload();
		}
	}

	/**
	 * 加载图片缓存
	 */
	public void loadImage(Path tempDir) throws Exception {
		for (Map.Entry<MyPluginOptions.DumpType, String> entry : dotMap.entrySet()) {
			MyPluginOptions.DumpType dumpType = entry.getKey();
			String dotString = entry.getValue();

			MyUtils.tempFileDeleteOnFinish(tempDir, null, ".dot", dotFile -> {
				Path dotPath = dotFile.toPath();
				Files.writeString(dotPath, dotString);
				BufferedImage bufferedImage = dot2Png(dotPath, tempDir);
				imageMap.put(dumpType, bufferedImage);
				return 0;
			});
		}
	}

	@NotNull
	private static BufferedImage dot2Png(Path dotFile, Path tempDir) throws Exception {
		return MyUtils.tempFileDeleteOnFinish(tempDir, null, ".png", pngFile -> {
			Process process = new ProcessBuilder("dot", "-Tpng", dotFile.toAbsolutePath().toString(), "-o", pngFile.getAbsolutePath()).start();
			String errorStr;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				errorStr = reader.lines().collect(Collectors.joining("\n"));
			}
			int result = process.waitFor();
			if (result != 0 || !errorStr.isEmpty()) {
				throw new JadxRuntimeException(String.format(NLS.exceptionDot2PngFail, result, errorStr));
			}
			CfgViewerPlugin.LOG.debug(NLS.logTmpPngFile, pngFile);
			return ImageIO.read(pngFile);
		});
	}

	/**
	 * 是否已经存储了所有 dumpType 对应的 dot 字符串
	 */
	public boolean isDotLoaded() {
		List<MyPluginOptions.DumpType> allType = Arrays.stream(MyPluginOptions.DumpType.values()).collect(Collectors.toList());
		return dotMap.keySet().containsAll(allType);
	}

	/**
	 * 是否已经存储了所有 dumpType 对应的图片
	 */
	public boolean isImageLoaded() {
		List<MyPluginOptions.DumpType> allType = Arrays.stream(MyPluginOptions.DumpType.values()).collect(Collectors.toList());
		return imageMap.keySet().containsAll(allType);
	}

	/**
	 * 清空图片缓存
	 */
	public void unloadImage() {
		imageMap.clear();
	}
}
