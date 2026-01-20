package jadx.plugins.viewer.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class MyUtils {
	public interface TempFileConsumer<T, R> {
		R accept(T t) throws Exception;
	}

	/**
	 * 新建一个临时文件夹，执行某操作。操作执行完成后删除该文件夹。
	 */
	public static <R> R tempDirDeleteOnFinish(Path tempRootDir, TempFileConsumer<File, R> action) throws Exception {
		Path tempDir = Files.createTempDirectory(tempRootDir, null);
		File tempDirFile = tempDir.toFile();
		tempDirFile.deleteOnExit();

		R r = action.accept(tempDirFile);

		FileUtils.deleteDirIfExists(tempDir);
		return r;
	}

	/**
	 * 新建一个临时文件，执行某操作。操作执行完成后删除该文件。
	 */
	public static <R> R tempFileDeleteOnFinish(
			Path tempRootDir, @Nullable String prefix, @Nullable String suffix, TempFileConsumer<File, R> action) throws Exception {
		Path tempFile = Files.createTempFile(tempRootDir, prefix, suffix);
		File tempFileFile = tempFile.toFile();
		tempFileFile.deleteOnExit();

		R r = action.accept(tempFileFile);

		FileUtils.deleteDirIfExists(tempFile);
		return r;
	}

	/**
	 * 从一个目录向内部寻找，要求每层有且只有一个文件（夹），为文件时停止并返回该文件。
	 */
	static File getOneDotFile(@NotNull File dir) {
		File dotFile = dir;
		while (dotFile != null && dotFile.isDirectory()) {
			File[] list = dotFile.listFiles();
			dotFile = (list == null || list.length != 1) ? null : list[0];
		}
		if (dotFile == null) {
			throw new JadxRuntimeException(NLS.exceptionDotFileNull + dir.getAbsolutePath());
		}
		CfgViewerPlugin.LOG.debug(NLS.logTmpDotFile, dotFile);
		return dotFile;
	}
}
