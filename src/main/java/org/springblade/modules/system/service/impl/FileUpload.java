package org.springblade.modules.system.service.impl;

import com.jcraft.jsch.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileUpload {
	public static boolean upload(MultipartFile multipartFile, String name, Integer port, String pwd, String username, String host, String path) {
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(username, host, port);
			session.setPassword(pwd);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
			channelSftp.connect();

			channelSftp.cd(path);
			InputStream inputStream = multipartFile.getInputStream();
			channelSftp.put(inputStream, name);
			channelSftp.disconnect();
			session.disconnect();
			System.out.println("上传成功！！");
			return true;
		} catch (JSchException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (SftpException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

//	void uploadChild(ChannelSftp channelSftp, String url) throws SftpException, FileNotFoundException {
//		String cdUrl = "";
//		ExecuteCMD executeCMD = new ExecuteCMD();
//		executeCMD.execute("mkdir -p " + ServerConstant.remoteFilePath + "/" + url);
//		cdUrl = url + "/";
//
//		File libDirectory = new File(ServerConstant.localFilePath + "\\" + url);
//		File[] libFiles = libDirectory.listFiles(); // 获取文件夹下的所有文件和子文件夹
//		for (File file : libFiles) {
//			String isExist;
//			try {
//				channelSftp.stat(ServerConstant.remoteFilePath + cdUrl + file.getName());
//				isExist = "已存在";
//			} catch (SftpException e) {
//				//文件不存在
//				try (InputStream inputStream = new FileInputStream(file)) {
//					channelSftp.cd(ServerConstant.remoteFilePath + "/" + cdUrl);
//					channelSftp.put(inputStream, file.getName());
//					isExist = "上传成功";
//				} catch (FileNotFoundException ex) {
//					System.out.println("文件未找到：" + file.getName());
//					continue;
//				} catch (IOException ex) {
//					System.out.println("文件上传失败：" + file.getName());
//					continue;
//				}
//			}
//			System.out.println("正在上传:" + file.getName() + "----" + isExist);
//		}
//		executeCMD.execute("du -sh " + ServerConstant.remoteFilePath + "/" + url);
//		System.out.print("文件数量：");
//		executeCMD.execute("ls -1q " + ServerConstant.remoteFilePath + "/" + url + " | wc -l");
//	}
}
