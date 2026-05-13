package org.springblade.common.utils;

import lombok.extern.log4j.Log4j2;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
public class ZipUtil {
	private static final int BUFFER_SIZE = 8 * 1024 * 1024; // 8MB buffer
	private static final int MAX_PARALLEL_FILES = 10; // 最大并行处理文件数

    /**
     * 压缩成ZIP
     *
     * @param srcDir           压缩文件夹路径
     * @param outDir           压缩文件输出路径
     * @param KeepDirStructure 是否保留原来的目录结构,true:保留目录结构;false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
     */
    public static void toZip(String srcDir, String outDir, boolean KeepDirStructure) throws Exception {
        FileOutputStream out = new FileOutputStream(outDir);
        ZipOutputStream zos = new ZipOutputStream(out);
        File sourceFile = new File(srcDir);
        compress(sourceFile, zos, sourceFile.getName(), KeepDirStructure);
        if (zos != null) {
            zos.close();
        }
    }

    /**
     * 压缩成ZIP
     *
     * @param srcFiles 需要压缩的文件列表
     * @param outDir   压缩文件输出路径
     */
    public static void toZip(List<File> srcFiles, File outDir) throws Exception {
		try (FileOutputStream out = new FileOutputStream(outDir);
			 ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out, BUFFER_SIZE))) {

			// 设置压缩级别
			zos.setLevel(1); // 使用较低的压缩级别以提高速度

			// 使用ConcurrentHashMap来存储已处理的文件路径，避免重复
			Map<String, Boolean> processedFiles = new ConcurrentHashMap<>();

			// 并行处理文件
			CompletableFuture<?>[] futures = srcFiles.stream()
				.map(srcFile -> CompletableFuture.runAsync(() -> {
					try {
						if (srcFile.isDirectory()) {
							processDirectory(srcFile, zos, processedFiles);
						} else {
							processFile(srcFile, zos, processedFiles);
						}
					} catch (Exception e) {
						log.error("Error processing file: " + srcFile.getPath(), e);
						throw new CompletionException(e);
					}
				}))
				.toArray(CompletableFuture[]::new);

			// 等待所有文件处理完成
			CompletableFuture.allOf(futures).join();
        }
    }

    /**
     * 压缩成ZIP
     *
     * @param srcFiles 需要压缩的文件map
     * @param outDir   压缩文件输出路径
     */
    public static void mapToZip(Map<String, List<File>> srcFiles, File outDir) throws Exception {
        FileOutputStream out = new FileOutputStream(outDir);
        ZipOutputStream zos = new ZipOutputStream(out);
        for (Map.Entry<String, List<File>> entry : srcFiles.entrySet()) {
            for (File file : entry.getValue()) {
                byte[] buf = new byte[1024 * 1024];
                String[] split = file.getName().split("\\.");
                zos.putNextEntry(new ZipEntry(entry.getKey() + "." + split[1]));
                int len;
                FileInputStream in = new FileInputStream(file);
                while ((len = in.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
                in.close();
            }
        }
        if (zos != null) {
            zos.close();
        }
    }

    /**
     * 递归压缩方法
     *
     * @param sourceFile       源文件
     * @param zos              zip输出流
     * @param name             压缩后的名称
     * @param KeepDirStructure 是否保留原来的目录结构,true:保留目录结构;false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
     */
    private static void compress(File sourceFile, ZipOutputStream zos, String name, boolean KeepDirStructure) throws Exception {
        byte[] buf = new byte[BUFFER_SIZE];
        if (sourceFile.isFile()) {
            // 向zip输出流中添加一个zip实体，构造器中name为zip实体的文件的名字
            zos.putNextEntry(new ZipEntry(name));
            // copy文件到zip输出流中
            int len;
            FileInputStream in = new FileInputStream(sourceFile);
            while ((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            // Complete the entry
            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                // 需要保留原来的文件结构时,需要对空文件夹进行处理
                if (KeepDirStructure) {
                    // 空文件夹的处理
                    zos.putNextEntry(new ZipEntry(name + "/"));
                    // 没有文件，不需要文件的copy
                    zos.closeEntry();
                }
            } else {
                for (File file : listFiles) {
                    // 判断是否需要保留原来的文件结构
                    if (KeepDirStructure) {
                        // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                        // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                        compress(file, zos, name + "/" + file.getName(), KeepDirStructure);
                    } else {
                        compress(file, zos, file.getName(), KeepDirStructure);
                    }
                }
			}
		}
	}

	private static void processDirectory(File dir, ZipOutputStream zos, Map<String, Boolean> processedFiles) throws IOException {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				String entryPath = dir.getName() + "/" + file.getName();
				if (!processedFiles.containsKey(entryPath)) {
					if (file.isDirectory()) {
						processDirectory(file, zos, processedFiles);
					} else {
						processFile(file, zos, processedFiles, entryPath);
					}
				}
			}
		}
	}

	private static void processFile(File file, ZipOutputStream zos, Map<String, Boolean> processedFiles) throws IOException {
		processFile(file, zos, processedFiles, file.getName());
	}

	private static void processFile(File file, ZipOutputStream zos, Map<String, Boolean> processedFiles, String entryPath) throws IOException {
		if (processedFiles.putIfAbsent(entryPath, true) == null) {
			synchronized (zos) {
				ZipEntry zipEntry = new ZipEntry(entryPath);
				zos.putNextEntry(zipEntry);

				try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()), BUFFER_SIZE)) {
					byte[] buffer = new byte[BUFFER_SIZE];
					int len;
					while ((len = bis.read(buffer)) != -1) {
						zos.write(buffer, 0, len);
					}
				}

				zos.closeEntry();
            }
        }
    }

    public static HttpServletResponse downloadZip(File file, HttpServletResponse response) throws Exception {
        if (file.exists()) {
            // 以流的形式下载文件。
            InputStream fis = new BufferedInputStream(new FileInputStream(file.getPath()));
            // 清空response
            response.reset();
            OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
            response.setContentType("application/octet-stream");
            //如果输出的是中文名的文件，在此处就要用URLEncoder.encode方法进行处理
            response.setHeader("Content-Disposition", "attachment;filename="
                    + new String(file.getName().getBytes("UTF-8"), "ISO8859-1"));
            byte[] buffer = new byte[fis.available()];
            int i = -1;
            while ((i = fis.read(buffer)) != -1) {   //不能一次性读完，大文件会内存溢出（不能直接fis.read(buffer);）
                toClient.write(buffer, 0, i);
            }
            fis.close();
            toClient.flush();
            toClient.close();

            File f = new File(file.getPath());
            f.delete();
        } else {
            throw new FileNotFoundException();
        }
        return response;
    }

    public static void main(String[] args) throws Exception {
        /** 测试压缩方法1  */
        ZipUtil.toZip("D:\\zgzckj\\upload\\498f05720ebb49c8a5b5e5789079893d", "D:\\zgzckj\\upload\\498f05720ebb49c8a5b5e5789079893d.zip", false);
        System.out.println("");

    }
}
