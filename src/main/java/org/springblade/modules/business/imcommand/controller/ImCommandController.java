package org.springblade.modules.business.imcommand.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.imcommand.dto.ImCommandExecuteResponse;
import org.springblade.modules.business.imcommand.service.IImCommandService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/blade-business/im-command")
@Api(value = "指令总入口", tags = "指令总入口接口")
public class ImCommandController extends BladeController {

	private final IImCommandService imCommandService;


	@PostMapping("/execute")
	@ApiOperation(value = "执行指令", notes = "传入微信用户ID与文本指令，执行记账业务并返回回复文案")
	public R<ImCommandExecuteResponse> execute(@RequestBody CommandRequest request) {
		return R.data(imCommandService.execute(request.getWxUserId(), request.getCommand()));
	}

	@Data
	@ApiModel(value = "CommandRequest对象", description = "指令请求")
	public static class CommandRequest {
		@ApiModelProperty(value = "指令文本", required = true)
		private String command;

		@ApiModelProperty(value = "微信用户ID，ilink 消息中的 from_user_id")
		private String wxUserId;
	}
}
