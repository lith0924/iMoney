package org.springblade.modules.business.imoperationlog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.business.imoperationlog.entity.ImOperationLog;
import org.springblade.modules.business.imoperationlog.service.IImOperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/blade-business/im-operation-log")
@Api(value = "操作审计日志表", tags = "操作审计日志表接口")
public class ImOperationLogController extends BladeController {

	private final IImOperationLogService service;

	@GetMapping("/detail")
	@ApiOperation(value = "详情", notes = "传入对象")
	public R<ImOperationLog> detail(ImOperationLog param) {
		ImOperationLog detail = service.getOne(Condition.getQueryWrapper(param));
		return R.data(detail);
	}

	@GetMapping("/list")
	@ApiOperation(value = "分页", notes = "传入对象")
	public R<IPage<ImOperationLog>> list(@ApiIgnore @RequestParam Map<String, Object> param, Query query) {
		IPage<ImOperationLog> pages = service.page(Condition.getPage(query), Condition.getQueryWrapper(param, ImOperationLog.class));
		return R.data(pages);
	}

	@PostMapping("/submit")
	@ApiOperation(value = "新增或修改", notes = "传入对象")
	public R submit(@Valid @RequestBody ImOperationLog param) {
		return R.status(service.saveOrUpdate(param));
	}

	@PostMapping("/remove")
	@ApiOperation(value = "删除", notes = "传入主键集合")
	public R remove(@ApiParam(value = "主键集合", required = true) @RequestParam String ids) {
		return R.status(service.removeByIds(Func.toLongList(ids)));
	}
}
