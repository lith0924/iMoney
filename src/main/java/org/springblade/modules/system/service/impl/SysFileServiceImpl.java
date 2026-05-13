package org.springblade.modules.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.springblade.common.utils.EmojiFilterUtil;
import org.springblade.common.utils.IdUtils;
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.entity.SysFile;
import org.springblade.modules.system.mapper.SysFileMapper;
import org.springblade.modules.system.service.SysFileService;
import org.springblade.modules.system.vo.SysFilePageParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @Description: 业务文件
 * @author: 鹿现策
 * @create: 2023-01-22 17:24:17
 **/


@Transactional(rollbackFor = Exception.class)
@Service
@Log4j2
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

	private final RestTemplate restTemplate = new RestTemplate();
	@Autowired
	private SysFileMapper sysFileMapper;
	@Value("${aliOss.AccessKeySecret}")
	private String accessKeySecret;
	@Value("${aliOss.OSSAccessKeyId}")
	private String OSSAccessKeyId;
	@Value("${aliOss.bucket}")
	private String bucket;
	@Value("${aliOss.region}")
	private String region;
	@Value("${aliOss.domain}")
	private String aliOssDomain;
	@Value("${tempPath}")
	private String tempPath;
	@Value("${interviewPath}")
	private String interviewPath;

	// 过滤特殊字符
	public static String StringFilter(String str) throws PatternSyntaxException {

		// 清除掉所有特殊字符
		String regEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“'。，、？]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		return m.replaceAll("").trim();
	}

	@Override
	public IPage<SysFile> selectByPage(IPage<SysFile> iPage, SysFilePageParam param) {
		IPage<SysFile> page = sysFileMapper.selectByPage(iPage, param);
		return page;
	}

	@Override
	public SysFile selectById(Long id) {
		return sysFileMapper.selectById(id);
	}

	@Override
	public SysFile upload(MultipartFile multipartFile) {
		Long id = IdUtils.nextId(IdUtils.FILE_NAME);
		SysFile sysFile = new SysFile();
		sysFile.setId(id);
		sysFile.setFileName(multipartFile.getOriginalFilename());
		sysFile.setSize(String.valueOf(multipartFile.getSize()));
		String[] split = multipartFile.getOriginalFilename().split("\\.");
		Path fileStorageLocation = Paths.get(tempPath).toAbsolutePath().normalize();
		// 创建文件保存的路径
		String fileName = id + "." + split[1];
		try {
			// 将文件保存到指定目录
			Path targetLocation = fileStorageLocation.resolve(fileName);
			Files.copy(multipartFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			throw new RuntimeException("上传失败!" + ex.getMessage());
		}
		sysFile.setFilePath(interviewPath + fileName);
		sysFile.setType(multipartFile.getContentType());
		sysFileMapper.insert(sysFile);
		return sysFile;
	}

	@Override
	public R<SysFile> uploadProd(MultipartFile uploadFile) {
		try {
			log.info("上传文件开始:" + uploadFile.getOriginalFilename());
			// 文件名不包含点说明是无效文件
			if (!uploadFile.getOriginalFilename().contains(".")) {
				throw new RuntimeException("上传失败，文件格式错误");
			}
			String oldFile = URLDecoder.decode(uploadFile.getOriginalFilename());
			String oldName = uploadFile.getOriginalFilename().substring(0, URLDecoder.decode(uploadFile.getOriginalFilename()).lastIndexOf("."));
			oldName = EmojiFilterUtil.filterEmoji(oldName);
			oldName = StringFilter(oldName);
			String suffix = oldFile.substring(oldFile.lastIndexOf("."));//文件后缀
			log.info("解析名称后缀:" + oldName + suffix);

			Long id = IdUtils.nextId();
			String newName = id + suffix;
			String path = newName;

			path = path.replaceAll("\\\\", "/");
			SysFile sysFile = new SysFile();
			sysFile.setId(id);
			sysFile.setFileName(oldName);
			sysFile.setType(suffix);


			R uploadResult = this.uploadFileByOss(path, uploadFile.getBytes(), newName);
			if (!uploadResult.isSuccess()) {
				return uploadResult;
			}
			sysFile.setFilePath(uploadResult.getData().toString());
			sysFileMapper.insert(sysFile);
			return R.data(sysFile);
		} catch (Exception e) {
			log.error("uploadFile Exception:", e);
			throw new RuntimeException("上传失败！"+e.getMessage());
		}


//		// 目标接口的URL
//		String externalApiUrl = "https://print.qidouxing.com/api/SysFile/upload";
//
//		// 设置请求头
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//		// 构建请求体
//		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//		try {
//			body.add("file", new ByteArrayResource(multipartFile.getBytes()) {
//				@Override
//				public String getFilename() {
//					return multipartFile.getOriginalFilename(); // 设置文件名
//				}
//			});
//		} catch (IOException e) {
//			return null;
//		}
//
//		// 创建请求实体
//		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//
//		// 发送请求
//		ResponseEntity<String> response = restTemplate.exchange(
//			externalApiUrl,
//			HttpMethod.POST,
//			requestEntity,
//			String.class
//		);

	}

	public R<String> uploadFileByOss(String fileKey, byte[] content, String... fileName) {
		OSS ossClient = new OSSClientBuilder().build(region, OSSAccessKeyId, accessKeySecret);
		try {
			if (ObjectUtil.isNotNull(fileName)) {
				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentDisposition("attachment; filename=" + URLEncoder.encode(fileName[0], "UTF-8")); // 设置Content-Disposition
				ossClient.putObject(bucket, "oss-saas-file/" + fileKey, new ByteArrayInputStream(content), metadata);
			} else {
				ossClient.putObject(bucket, "oss-saas-file/" + fileKey, new ByteArrayInputStream(content));
			}
		} catch (Exception e) {
			log.error("aliyun oss upload error:", e);
			throw new RuntimeException("上传失败");
		} finally {
			ossClient.shutdown();
		}
		String url = aliOssDomain + "/" + fileKey;
		return R.data(url);
	}

	@Override
	public String saveFile(String imageUrl, String fileName) {
		try {
			URL url = new URL(imageUrl);
			InputStream is = new BufferedInputStream(url.openStream());
			OutputStream os = new FileOutputStream(tempPath + "/" + fileName);

			byte[] data = new byte[1024];
			int count;
			while ((count = is.read(data, 0, 1024)) != -1) {
				os.write(data, 0, count);
			}

			is.close();
			os.close();
			System.out.println("图片下载成功，保存路径：" + tempPath + "/" + fileName);
			return interviewPath + fileName;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("图片下载失败");
		}
		return "";
	}

	@Override
	public void insert(SysFile sysFile) {
		sysFile.setId(IdUtils.nextId(IdUtils.SYS_FILE));
		sysFileMapper.insert(sysFile);
	}

	@Override
	public void update(SysFile sysFile) {
		sysFileMapper.updateById(sysFile);
	}

	@Override
	public void deleteById(Long id) {
		sysFileMapper.deleteSysFileById(id);
	}

	@Override
	public void deleteBatch(List<Long> idList) {
		sysFileMapper.deleteBatchIds(idList);
	}

	/**
	 * 上传ZIP文件到OSS，支持自定义文件名
	 * 绕过原有的文件名重命名机制，保持用户指定的文件名
	 *
	 * @param fileContent ZIP文件的字节内容
	 * @param customFileName 自定义文件名（不包含.zip后缀）
	 * @return OSS访问URL
	 */
	public String uploadZipToOssWithCustomName(byte[] fileContent, String customFileName) {
		return fallbackZipUpload(fileContent, customFileName);
	}

	/**
	 * 回退到现有的ZIP上传方法
	 */
	private String fallbackZipUpload(byte[] fileContent, String customFileName) {
		try {
			log.info("使用回退方法上传ZIP文件: {}", customFileName);

			// 创建临时文件
			java.nio.file.Path tempPath = java.nio.file.Paths.get(this.tempPath);
			java.nio.file.Path tempFile = tempPath.resolve( customFileName + ".zip");

			// 确保临时目录存在
			java.nio.file.Files.createDirectories(tempPath);

			// 写入临时文件
			java.nio.file.Files.write(tempFile, fileContent);

			// 使用现有的上传方法
			R<String> stringR = uploadFileByOss(IdUtils.nextId()+"/" + tempFile.getFileName().toString(), fileContent, tempFile.getFileName().toString());
			String ossUrl =stringR.getData() ;
				// 清理临时文件
			java.nio.file.Files.deleteIfExists(tempFile);

			log.info("回退方法上传ZIP文件成功，原始文件名: {}, OSS URL: {}", customFileName, ossUrl);
			return ossUrl;

		} catch (Exception e) {
			log.error("回退上传ZIP文件方法也失败", e);
			throw new RuntimeException("所有上传ZIP文件方法都失败: " + e.getMessage());
		}
	}
}
