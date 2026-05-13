package org.springblade.modules.business.imilink.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.imilink.service.IlinkBotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping({"/blade-business/ilink", "/ilink-qr"})
@Api(value = "ilink机器人", tags = "ilink机器人接口")
public class IlinkBotController extends BladeController {

	private final IlinkBotService ilinkBotService;

	@PostMapping("/login/start")
	@ApiOperation(value = "开始扫码登录", notes = "返回二维码内容，客户端渲染为二维码后使用微信扫码；clientId 不传时自动创建新的客户端会话，传入时复用指定会话")
	public R<Map<String, Object>> startLogin(@RequestParam(required = false) String clientId) {
		return R.data(ilinkBotService.startLogin(clientId));
	}

	@PostMapping("/login/wait")
	@ApiOperation(value = "等待登录完成", notes = "阻塞等待扫码确认，默认最多等待60秒；多个客户端时必须传 login/start 返回的 clientId")
	public R<Map<String, Object>> waitLogin(@RequestParam(required = false) String clientId) {
		return R.data(ilinkBotService.waitLogin(clientId));
	}

	@GetMapping("/status")
	@ApiOperation(value = "登录状态", notes = "传 clientId 查看单个客户端；不传则返回全部客户端状态")
	public R<Map<String, Object>> status(@RequestParam(required = false) String clientId) {
		return R.data(ilinkBotService.status(clientId));
	}

	@PostMapping("/poll")
	@ApiOperation(value = "手动拉取一次消息", notes = "登录后拉取一次消息并触发命令处理和回复；多个客户端时必须传 login/start 返回的 clientId")
	public R<Map<String, Object>> poll(@RequestParam(required = false) String clientId) {
		return R.data(ilinkBotService.pollOnce(clientId));
	}

	@PostMapping("/stop")
	@ApiOperation(value = "停止指定客户端", notes = "关闭指定 clientId 的 ilink 客户端；多个客户端时必须传 login/start 返回的 clientId")
	public R<Map<String, Object>> stop(@RequestParam(required = false) String clientId) {
		return R.data(ilinkBotService.stop(clientId));
	}

	@PostMapping("/stop/all")
	@ApiOperation(value = "停止全部客户端", notes = "关闭所有 ilink 客户端实例")
	public R<Map<String, Object>> stopAll() {
		return R.data(ilinkBotService.stopAll());
	}
}
