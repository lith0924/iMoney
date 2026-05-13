package org.springblade.modules.business.imcommand.service.impl;

import org.springblade.modules.business.imcommand.dto.ImCommandExecuteResponse;
import org.springblade.modules.business.imcommand.service.IImCommandReplyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImCommandReplyServiceImpl implements IImCommandReplyService {

	@Override
	public ImCommandExecuteResponse success(ImCommandExecuteResponse result, String reply) {
		result.setSuccess(true);
		result.setReply(formatStandardReply(reply, true));
		return result;
	}

	@Override
	public ImCommandExecuteResponse failure(ImCommandExecuteResponse result, String code, String reply) {
		result.setSuccess(false);
		result.setCode(code);
		result.setReply(formatStandardReply(reply, false));
		return result;
	}

	@Override
	public String formatStandardReply(String reply, boolean success) {
		String content = StringUtils.hasText(reply) ? reply.trim() : "-";
		if (content.startsWith("我是iMoney你的专属会计")) {
			return content;
		}
		if (content.startsWith("| iMoney |")) {
			return content;
		}
		List<String> lines = new ArrayList<>();
		String[] rawLines = content.split("\\r?\\n");
		for (String rawLine : rawLines) {
			String line = rawLine == null ? "" : rawLine.trim();
			if (StringUtils.hasText(line)) {
				lines.add(line);
			}
		}
		if (lines.isEmpty()) {
			lines.add("-");
		}
		if (!success && !lines.get(0).startsWith("操作失败")) {
			lines.add(0, "操作失败");
		}
		StringBuilder sb = new StringBuilder();
		sb.append("| iMoney |\n");
		sb.append("| :--- |\n");
		for (String line : lines) {
			sb.append("| ").append(safeTableCell(line)).append(" |\n");
		}
		return sb.toString();
	}

	private String safeTableCell(String text) {
		if (!StringUtils.hasText(text)) {
			return "ㅤ";
		}
		return text.replace("|", "¦");
	}
}
