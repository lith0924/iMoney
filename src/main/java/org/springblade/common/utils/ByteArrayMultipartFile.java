package org.springblade.common.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * 内置一个简单的 MultipartFile 实现类，用于File转换
 */
public class ByteArrayMultipartFile implements MultipartFile {
	private final byte[] content;
	private final String name;
	private final String originalFilename;
	private final String contentType;

	/**
	 * 构造函数
	 *
	 * @param content          文件内容
	 * @param originalFilename 文件原始名字
	 * @param name             字段名
	 * @param contentType      文件类型
	 */
	public ByteArrayMultipartFile(byte[] content, String originalFilename, String name, String contentType) {
		this.content = content;
		this.originalFilename = originalFilename;
		this.name = name;
		this.contentType = contentType;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getOriginalFilename() {
		return this.originalFilename;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public boolean isEmpty() {
		return (this.content == null || this.content.length == 0);
	}

	@Override
	public long getSize() {
		return this.content.length;
	}

	@Override
	public byte[] getBytes() {
		return this.content;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(this.content);
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		try (OutputStream os = new FileOutputStream(dest)) {
			os.write(this.content);
		}
	}
}
