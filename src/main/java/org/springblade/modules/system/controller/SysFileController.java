package org.springblade.modules.system.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.springblade.common.utils.PageMethods;
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.entity.SysFile;
import org.springblade.modules.system.service.SysFileService;
import org.springblade.modules.system.vo.SysFilePageParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description: 业务文件
 * @author: 鹿现策
 * @create: 2023-01-22 17:24:17
 **/


@RestController
@RequestMapping(value = "/SysFile")
@Api(value = "业务文件", tags = "业务文件")
@Log4j2
public class SysFileController {
	@Resource
	private SysFileService sysFileService;

	@Value("${spring.profiles.active}")
	private String active;

	@ApiOperation("业务文件分页")
	@GetMapping("/page")
	public R<IPage<SysFile>> selectSysFileByPage(SysFilePageParam param) {
		IPage iPage = PageMethods.TransportPage(param);
		IPage page = sysFileService.selectByPage(iPage, param);
		return R.data(page);
	}

	@ApiOperation("业务文件详情")
	@GetMapping("/getById")
	public R<SysFile> selectById(Long id) {
		SysFile SysFile = sysFileService.selectById(id);
		return R.data(SysFile);
	}

	@ApiOperation("业务文件新增")
	@PostMapping("/insert")
	public R insert(@RequestBody SysFile sysFile) {
		sysFileService.insert(sysFile);
		return R.data(true);
	}

	@ApiOperation("业务文件修改")
	@PostMapping("/update")
	public R update(@RequestBody SysFile sysFile) {
		sysFileService.update(sysFile);
		return R.data(true);
	}

	@ApiOperation("业务文件删除")
	@DeleteMapping("/delete")
	public R deleteById(@RequestParam(name = "id", required = true) Long id) {
		sysFileService.deleteById(id);
		return R.data(true);
	}

	@ApiOperation("业务文件批量删除")
	@PostMapping("/deleteBatch")
	public R deleteBatch(@RequestBody List<Long> idList) {
		sysFileService.deleteBatch(idList);
		return R.data(true);
	}


	@PostMapping("/upload")
	@ApiOperation("业务文件上传")
	public Object upLoadFromProduction(@RequestPart("file") MultipartFile uploadFile) {
		Object uploaded = sysFileService.upload(uploadFile);
		return uploaded;
	}

	@PostMapping("/uploadProd")
	@ApiOperation("上传到oss上")
	public Object uploadProd(@RequestParam("file") MultipartFile multipartFile) {
		return sysFileService.uploadProd(multipartFile);
	}
}
