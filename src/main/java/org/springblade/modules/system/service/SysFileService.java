package org.springblade.modules.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.entity.SysFile;
import org.springblade.modules.system.vo.SysFilePageParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Description: 业务文件
 * @author: 鹿现策
 * @create: 2023-01-22 17:24:17
 **/


public interface SysFileService extends IService<SysFile> {
	IPage<SysFile> selectByPage(IPage<SysFile> iPage, SysFilePageParam param);

	SysFile selectById(Long id);

	SysFile upload(MultipartFile file);

	R<SysFile> uploadProd(MultipartFile file);

	String saveFile(String url, String fileName);

	void insert(SysFile sysFile);

	void update(SysFile sysFile);

	void deleteById(Long id);

	void deleteBatch(List<Long> idList);

	/**
	 * 上传ZIP文件到OSS，支持自定义文件名
	 * 绕过原有的文件名重命名机制，保持用户指定的文件名
	 *
	 * @param fileContent ZIP文件的字节内容
	 * @param customFileName 自定义文件名（不包含.zip后缀）
	 * @return OSS访问URL
	 */
	String uploadZipToOssWithCustomName(byte[] fileContent, String customFileName);
}
