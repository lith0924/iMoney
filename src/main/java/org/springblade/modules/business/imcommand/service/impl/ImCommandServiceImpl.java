package org.springblade.modules.business.imcommand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.modules.business.imcommand.dto.ImCommandExecuteResponse;
import org.springblade.modules.business.imcommand.enums.EnumImCommandRoute;
import org.springblade.modules.business.imcommand.service.IImCommandService;
import org.springblade.modules.business.imcommand.service.IImCommandReplyService;
import org.springblade.modules.business.imai.dto.AiMessageResponse;
import org.springblade.modules.business.imai.enums.EnumAiApiUrl;
import org.springblade.modules.business.imai.service.IAiChatService;
import org.springblade.modules.business.imaiparselog.entity.ImAiParseLog;
import org.springblade.modules.business.imaiparselog.service.IImAiParseLogService;
import org.springblade.modules.business.imbudget.entity.ImBudgetSetting;
import org.springblade.modules.business.imbudget.service.IImBudgetSettingService;
import org.springblade.modules.business.imexporttask.entity.ImExportTask;
import org.springblade.modules.business.imexporttask.service.IImExportTaskService;
import org.springblade.modules.business.imgroup.entity.ImGroup;
import org.springblade.modules.business.imgroup.service.IImGroupService;
import org.springblade.modules.business.imgroupmember.entity.ImGroupMember;
import org.springblade.modules.business.imgroupmember.service.IImGroupMemberService;
import org.springblade.modules.business.iminvitecode.entity.ImInviteCode;
import org.springblade.modules.business.iminvitecode.service.IImInviteCodeService;
import org.springblade.modules.business.immemberalias.entity.ImMemberAlias;
import org.springblade.modules.business.immemberalias.service.IImMemberAliasService;
import org.springblade.modules.business.imoperationlog.service.IImOperationLogService;
import org.springblade.modules.business.imtxn.entity.ImTxn;
import org.springblade.modules.business.imtxn.service.IImTxnService;
import org.springblade.modules.business.imuser.entity.ImUser;
import org.springblade.modules.business.imuser.service.IImUserService;
import org.springblade.modules.business.imusercontext.entity.ImUserContext;
import org.springblade.modules.business.imusercontext.service.IImUserContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImCommandServiceImpl implements IImCommandService {

	private static final Logger log = LoggerFactory.getLogger(ImCommandServiceImpl.class);
	private static final int SCOPE_PRIVATE = 1;
	private static final int SCOPE_SHARED = 2;
	private static final int TXN_EXPENSE = 1;
	private static final int TXN_INCOME = 2;
	private static final String DEFAULT_GROUP_SUFFIX = "的共享账本";
	private static final String[] NICK_ADJECTIVES = new String[]{"开心", "勇敢", "温柔", "机智", "自由", "安静", "阳光", "可爱", "坚强", "聪明", "快乐", "洒脱"};
	private static final String[] NICK_NOUNS = new String[]{"小鹿", "地瓜", "海豚", "星星", "云朵", "橙子", "松鼠", "月亮", "青蛙", "燕子", "熊猫", "蝴蝶"};
	private static final Pattern TXN_PATTERN = Pattern.compile("^([+-])\\s*(\\d+(?:\\.\\d{1,2})?)(?:\\s+(.+))?$");
	private static final Pattern BUDGET_PATTERN = Pattern.compile("^预算\\s*(日|月|年)\\s*(\\d+(?:\\.\\d{1,2})?)$");
	private static final Pattern BUDGET_CLOSE_PATTERN = Pattern.compile("^关闭预算\\s*(日|月|年)$");
	private static final Pattern MONTH_PATTERN = Pattern.compile("^(\\d{4})[-/](\\d{1,2})$");
	private static final Pattern TXN_LIST_PAGE_PATTERN = Pattern.compile("^(.+?)#(\\d+)$");
	private static final Pattern TXN_LIST_DATE_PATTERN = Pattern.compile("^(查看账单记录|账单记录|列表)\\s+(.+)$");
	private static final Pattern DAILY_RECORD_INDEX_PATTERN = Pattern.compile("^账单记录\\s*(\\d+)$");
	private static final int TXN_LIST_PAGE_SIZE = 20;
	private static final int BUDGET_DAY = 1;
	private static final int BUDGET_MONTH = 2;
	private static final int BUDGET_YEAR = 3;
	private static final String[] EXPENSE_HINTS = new String[]{"花", "花了", "消费", "买", "支付", "支出", "扣款", "用了", "吃了", "打车", "买了"};
	private static final String[] INCOME_HINTS = new String[]{"收入", "赚", "赚了", "收款", "到账", "发工资", "工资", "报销", "退款", "退回"};
	private static final Random RANDOM = new Random();
	private static final int BOT_ID_MAX_LENGTH = 64;
	private static final int BOT_TOKEN_MAX_LENGTH = 255;

	@Autowired
	private IImUserService userService;
	@Autowired
	private IImUserContextService userContextService;
	@Autowired
	private IImTxnService txnService;
	@Autowired
	private IImOperationLogService operationLogService;
	@Autowired
	private IImGroupService groupService;
	@Autowired
	private IImGroupMemberService groupMemberService;
	@Autowired
	private IImInviteCodeService inviteCodeService;
	@Autowired
	private IImMemberAliasService memberAliasService;
	@Autowired
	private IImExportTaskService exportTaskService;
	@Autowired
	private IImAiParseLogService aiParseLogService;
	@Autowired
	private IImBudgetSettingService budgetSettingService;
	@Autowired
	private IAiChatService aiChatService;
	@Autowired
	private IImCommandReplyService replyService;

	@Value("${imai.zhipu.api-key:}")
	private String zhipuApiKey;
	@Value("${imai.zhipu.model:glm-4-flash}")
	private String zhipuModel;


	@Override
	public ImCommandExecuteResponse execute(String wxUserId, String command) {
		String rawCommand = command == null ? "" : command.trim();
		CommandMeta commandMeta = parseCommandMeta(rawCommand);
		String cmd = commandMeta.command;
		ImCommandExecuteResponse result = new ImCommandExecuteResponse();
		log.info("[im-command] execute start, wxUserId={}, rawCommand={}, normalizedCommand={}", wxUserId, rawCommand, cmd);

		if (!StringUtils.hasText(wxUserId)) {
			log.warn("[im-command] execute rejected because wxUserId is empty, command={}", cmd);
			return replyService.failure(result, "NO_USER", "没有拿到微信用户ID，无法记账。");
		}
		if (cmd.isEmpty()) {
			log.warn("[im-command] execute rejected because command is empty, wxUserId={}", wxUserId);
			return replyService.failure(result, "EMPTY", "指令不能为空，发送“菜单”查看可用命令。");
		}

		ImUser user = null;
		ImUserContext context = null;
		try {
			user = getOrCreateUser(wxUserId);
			context = getOrCreateContext(user.getId());
			ImGroup defaultGroup = getOrCreateDefaultGroup(user);
			activateDefaultGroupIfNeeded(context, defaultGroup);
			log.info("[im-command] user context ready, wxUserId={}, userId={}, scopeType={}, groupId={}",
				wxUserId, user.getId(), resolveContextScope(context), context.getCurrentGroupId());

			// 与 dispatch 一致的分支顺序，直接执行对应处理逻辑
			if (cmd.startsWith("+")) {
				result.setRoute(EnumImCommandRoute.TXN_INCOME);
				return addTxn(result, user, context, cmd, rawCommand, true, commandMeta.shared);
			} else if (cmd.startsWith("-")) {
				result.setRoute(EnumImCommandRoute.TXN_EXPENSE);
				return addTxn(result, user, context, cmd, rawCommand, false, commandMeta.shared);
			} else if ("菜单".equals(cmd) || "menu".equalsIgnoreCase(cmd) || "帮助".equals(cmd)) {
				result.setRoute(EnumImCommandRoute.MENU);
				log.info("[im-command] menu replied, userId={}, scopeType={}, groupId={}", user.getId(), normalizeScope(context), context.getCurrentGroupId());
				return replyService.success(result, "帮助".equals(cmd) ? helpText(user, context) : menuText(user, context));
			} else if ("我的ID".equals(cmd) || "myid".equalsIgnoreCase(cmd)) {
				result.setRoute(EnumImCommandRoute.USER_NICKNAME_UPDATE);
				return myWechatId(result, user);
			} else if (isBudgetSetCommand(cmd)) {
				result.setRoute(EnumImCommandRoute.BUDGET_SET);
				return setBudget(result, user, cmd);
			} else if (isBudgetCloseCommand(cmd)) {
				result.setRoute(EnumImCommandRoute.BUDGET_SET);
				return closeBudget(result, user, cmd);
			} else if ("预算提醒".equals(cmd) || "本月预算".equals(cmd) || "预算剩余".equals(cmd)) {
				result.setRoute(EnumImCommandRoute.BUDGET_QUERY);
				return budgetOverview(result, user);
			} else if (cmd.startsWith("撤销")) {
				result.setRoute(EnumImCommandRoute.TXN_UNDO);
				return undoTxn(result, user, context, cmd, commandMeta.shared);
			} else if (isStatsCommand(cmd)) {
				result.setRoute(EnumImCommandRoute.STATS_PRIVATE);
				Integer scopeType = resolveStatsScopeType(cmd, commandMeta.shared);
				Long groupId = scopeType.equals(SCOPE_SHARED) ? context.getCurrentGroupId() : null;
				if (scopeType.equals(SCOPE_SHARED) && groupId == null) {
					return replyService.failure(result, "NO_GROUP", "当前不在共享账本中。发送“邀请记账 家庭账本”创建，或“加入记账 邀请码”加入。");
				}
				return stats(result, user, scopeType, groupId, cmd);
			} else if (isTxnListCommand(cmd)) {
				result.setRoute(EnumImCommandRoute.TXN_LIST);
				return listTxn(result, user, context, commandMeta.shared, cmd);
			} else if (cmd.startsWith("邀请记账") || cmd.startsWith("生成邀请码")) {
				result.setRoute(EnumImCommandRoute.GROUP_INVITE);
				return invite(result, user, context, cmd);
			} else if (cmd.startsWith("加入记账") || cmd.startsWith("加入 ")) {
				result.setRoute(EnumImCommandRoute.GROUP_JOIN);
				return joinGroup(result, user, context, cmd);
			}else if ("邀请成员".equals(cmd) || "成员".equals(cmd)) {
				result.setRoute(EnumImCommandRoute.GROUP_MEMBER_LIST);
				return memberList(result, user, context);
			} else if (cmd.startsWith("修改昵称") || cmd.startsWith("昵称 ")) {
				result.setRoute(EnumImCommandRoute.USER_NICKNAME_UPDATE);
				return updateNickname(result, user, cmd);
			} else if (cmd.startsWith("备注成员")) {
				result.setRoute(EnumImCommandRoute.GROUP_MEMBER_ALIAS);
				return aliasMember(result, user, context, cmd);
			} else if (cmd.startsWith("修改备注")) {
				result.setRoute(EnumImCommandRoute.GROUP_MEMBER_ALIAS);
				return aliasMember(result, user, context, cmd);
			} else if (cmd.startsWith("移除成员")) {
				result.setRoute(EnumImCommandRoute.GROUP_MEMBER_REMOVE);
				return removeMember(result, user, context, cmd);
			} else if ("退出".equals(cmd)) {
				result.setRoute(EnumImCommandRoute.GROUP_QUIT);
				return releaseToDefaultGroup(result, user, context);
			} else if ("退出邀请".equals(cmd) || "确认退出".equals(cmd)) {
				result.setRoute(EnumImCommandRoute.GROUP_QUIT);
				return quitGroup(result, user, context);
			} else if ("导出Excel".equalsIgnoreCase(cmd) || "导出".equals(cmd)
				|| cmd.startsWith("导出#") || cmd.toLowerCase().startsWith("导出excel#")) {
				result.setRoute(EnumImCommandRoute.EXPORT_EXCEL);
				return export(result, user, context, cmd, commandMeta.shared);
			} else if ("消费分析".equals(cmd)) {
				result.setRoute(EnumImCommandRoute.CONSUMPTION_ANALYSIS);
				return expenseAnalysis(result, user, context, commandMeta.shared);
			}
			result.setRoute(EnumImCommandRoute.AI_PARSE);
			return aiParse(result, user, context, cmd);
		} catch (Exception ex) {
			Long userId = user == null ? null : user.getId();
			Integer scopeType = resolveContextScope(context);
			Long groupId = context == null ? null : context.getCurrentGroupId();
			String routeName = result.getRoute() == null ? null : result.getRoute().name();
			log.error("[im-command] execute failed, wxUserId={}, userId={}, route={}, command={}", wxUserId, userId, routeName, cmd, ex);
			if (userId != null) {
				operationLogService.recordOperation(userId, scopeType, groupId, routeName, null, cmd, "FAIL", ex.getMessage());
			}
			return replyService.failure(result, "FAIL", "处理失败：" + ex.getMessage());
		}
	}

	@Override
	public ImCommandExecuteResponse ensureUser(String wxUserId, String nickName) {
		return ensureUser(wxUserId, nickName, null, null);
	}

	@Override
	public ImCommandExecuteResponse ensureUser(String wxUserId, String nickName, String botId, String botToken) {
		ImCommandExecuteResponse result = new ImCommandExecuteResponse();
		if (!StringUtils.hasText(wxUserId)) {
			log.warn("[im-command] ensureUser rejected because wxUserId is empty");
			return replyService.failure(result, "NO_USER", "微信用户ID不能为空。");
		}
		ImUser user = getOrCreateUser(wxUserId, nickName, botId, botToken);
		try {
			ImUserContext context = getOrCreateContext(user.getId());
			ImGroup defaultGroup = getOrCreateDefaultGroup(user);
			activateDefaultGroupIfNeeded(context, defaultGroup);
			log.info("[im-command] ensureUser done, wxUserId={}, userId={}, nickName={}, botId={}, botTokenPresent={}, scopeType={}, groupId={}",
				wxUserId, user.getId(), user.getNickName(), user.getBotId(), StringUtils.hasText(user.getBotToken()), resolveContextScope(context), context.getCurrentGroupId());
			return replyService.success(result, "用户已入库并初始化记账上下文。");
		} catch (Exception ex) {
			log.error("[im-command] ensureUser partial success, wxUserId={}, userId={}, reason={}",
				wxUserId, user.getId(), ex.getMessage(), ex);
			return replyService.success(result, "用户已入库，记账上下文初始化稍后自动重试。");
		}
	}

	private ImCommandExecuteResponse addTxn(ImCommandExecuteResponse result, ImUser user, ImUserContext context, String cmd, String rawCommand, boolean income, boolean sharedCommand) {
		Matcher matcher = TXN_PATTERN.matcher(cmd);
		if (!matcher.matches()) {
			return replyService.failure(result, "BAD_TXN", "记账格式不正确。示例：+100 工资，-25.5 午饭。");
		}
		BigDecimal amount = new BigDecimal(matcher.group(2)).setScale(2, RoundingMode.HALF_UP);
		String rawNote = matcher.group(3) == null ? (income ? "收入" : "支出") : matcher.group(3).trim();
		Date now = new Date();
		TxnDateParseResult dateParseResult = parseTxnDate(rawNote, now);
		String note = dateParseResult.note;
		String opId = nextOpId();

		ImTxn txn = new ImTxn();
		txn.setOpId(opId);
		txn.setUserId(user.getId());
		txn.setScopeType(resolveScopeType(sharedCommand));
		txn.setGroupId(txn.getScopeType().equals(SCOPE_SHARED) ? context.getCurrentGroupId() : null);
		txn.setTxnType(income ? TXN_INCOME : TXN_EXPENSE);
		txn.setAmount(amount);
		txn.setNote(note);
		txn.setTxnTime(dateParseResult.txnTime);
		txn.setSourceType(1);
		txn.setSourceText(StringUtils.hasText(rawCommand) ? rawCommand : cmd);
		txn.setIsDeleted(0);
		txn.setCreateTime(now);
		txn.setUpdateTime(now);
		txnService.save(txn);
		operationLogService.recordOperation(user.getId(), txn.getScopeType(), txn.getGroupId(), income ? "ADD_INCOME" : "ADD_EXPENSE", opId, rawCommand, "OK", null);
		log.info("[im-command] im_txn inserted, userId={}, opId={}, rawCommand={}, normalizedCommand={}, txnType={}, amount={}, note={}, scopeType={}, groupId={}",
			user.getId(), opId, rawCommand, cmd, txn.getTxnType(), amount, note, txn.getScopeType(), txn.getGroupId());

		String scopeName = txn.getScopeType().equals(SCOPE_SHARED) ? "共享账本" : "个人账本";
		String reply = "已记录" + (income ? "收入" : "支出") + " " + amount.toPlainString() + " 元\n"
			+ "备注：" + note + "\n"
			+ "账本：" + scopeName + "\n"
			+ "撤销可发送：撤销";
		if (!income) {
			String budgetTip = buildBudgetReminder(user);
			if (StringUtils.hasText(budgetTip)) {
				reply = reply + "\n" + budgetTip;
			}
		}
		ImCommandExecuteResponse success = replyService.success(result, reply);
		success.setReply(success.getReply()+"\n"+"> 【-1 备注 @公】一起记账中展示");
		return success;
	}

	private TxnDateParseResult parseTxnDate(String rawNote, Date now) {
		String note = StringUtils.hasText(rawNote) ? rawNote.trim() : "";
		if (!StringUtils.hasText(note)) {
			return new TxnDateParseResult(now, "备注");
		}
		String[] parts = note.split("\\s+");
		if (parts.length == 0) {
			return new TxnDateParseResult(now, note);
		}
		String tail = parts[parts.length - 1].trim();
		Date parsedDate = parseDateToken(tail, now);
		if (parsedDate == null) {
			return new TxnDateParseResult(now, note);
		}
		String noteWithoutDate = note.substring(0, note.length() - tail.length()).trim();
		if (!StringUtils.hasText(noteWithoutDate)) {
			noteWithoutDate = "支出";
		}
		return new TxnDateParseResult(parsedDate, noteWithoutDate);
	}

	private Date parseDateToken(String token, Date now) {
		if (!StringUtils.hasText(token)) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		if ("今天".equals(token)) {
			return now;
		}
		if ("昨天".equals(token)) {
			cal.add(Calendar.DAY_OF_MONTH, -1);
			return cal.getTime();
		}
		if ("前天".equals(token)) {
			cal.add(Calendar.DAY_OF_MONTH, -2);
			return cal.getTime();
		}

		Date fullDate = parseDateByPattern(token, "yyyy-MM-dd");
		if (fullDate == null) {
			fullDate = parseDateByPattern(token, "yyyy/MM/dd");
		}
		if (fullDate == null) {
			fullDate = parseDateByPattern(token, "MM-dd");
		}
		if (fullDate == null) {
			fullDate = parseDateByPattern(token, "MM/dd");
		}
		if (fullDate == null) {
			return null;
		}
		Calendar dateCal = Calendar.getInstance();
		dateCal.setTime(fullDate);
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTime(now);
		if (token.length() <= 5) {
			dateCal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR));
		}
		dateCal.set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY));
		dateCal.set(Calendar.MINUTE, nowCal.get(Calendar.MINUTE));
		dateCal.set(Calendar.SECOND, nowCal.get(Calendar.SECOND));
		dateCal.set(Calendar.MILLISECOND, nowCal.get(Calendar.MILLISECOND));
		return dateCal.getTime();
	}

	private Date parseDateByPattern(String text, String pattern) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(pattern);
			format.setLenient(false);
			return format.parse(text);
		} catch (Exception ex) {
			return null;
		}
	}

	private ImCommandExecuteResponse undoTxn(ImCommandExecuteResponse result, ImUser user, ImUserContext context, String cmd, boolean sharedCommand) {
		QueryWrapper<ImTxn> wrapper = new QueryWrapper<>();
		wrapper.eq("user_id", user.getId());
		wrapper.eq("is_deleted", 0);
		wrapper.orderByDesc("create_time");
		wrapper.last("LIMIT 1");
		ImTxn txn = txnService.getOne(wrapper);
		if (txn == null) {
			return replyService.failure(result, "BAD_UNDO", "没有可撤销的最近一笔记账。");
		}
		String opId = txn.getOpId();
		Date now = new Date();
		txn.setIsDeleted(1);
		txn.setDeletedAt(now);
		txn.setDeletedBy(user.getId());
		txn.setUpdateTime(now);
		txnService.updateById(txn);
		operationLogService.recordOperation(user.getId(), txn.getScopeType(), txn.getGroupId(), "UNDO", opId, cmd, "OK", null);
		log.info("[im-command] im_txn undone, userId={}, opId={}, deletedBy={}", user.getId(), opId, user.getId());
		String reply = "已撤销上一笔记账\n金额：" + txn.getAmount().toPlainString() + " 元，备注：" + safe(txn.getNote());
		if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_EXPENSE)) {
			String budgetTip = buildBudgetReminder(user);
			if (StringUtils.hasText(budgetTip)) {
				reply = reply + "\n" + budgetTip;
			}
		}
		return replyService.success(result, reply);
	}

	private ImCommandExecuteResponse stats(ImCommandExecuteResponse result, ImUser user, Integer scopeType, Long groupId, String cmd) {
		if ("账单".equals(cmd) || cmd.startsWith("账单#")) {
			return statsBillSnapshot(result, user, scopeType, groupId, cmd);
		}
		DateRange range = parseDateRange(cmd);
		QueryWrapper<ImTxn> wrapper = new QueryWrapper<>();
		wrapper.eq("scope_type", scopeType);
		if (scopeType.equals(SCOPE_PRIVATE)) {
			wrapper.eq("user_id", user.getId());
			wrapper.isNull("group_id");
		} else {
			wrapper.eq("group_id", groupId);
		}
		wrapper.eq("is_deleted", 0);
		wrapper.ge("txn_time", range.start);
		wrapper.lt("txn_time", range.end);
		wrapper.orderByDesc("txn_time");
		List<ImTxn> txns = txnService.list(wrapper);

		BigDecimal income = BigDecimal.ZERO;
		BigDecimal expense = BigDecimal.ZERO;
		int incomeCount = 0;
		int expenseCount = 0;
		int unknownCount = 0;
		for (ImTxn txn : txns) {
			BigDecimal amount = txn.getAmount() == null ? BigDecimal.ZERO : txn.getAmount();
			if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_INCOME)) {
				income = income.add(amount);
				incomeCount++;
			} else if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_EXPENSE)) {
				expense = expense.add(amount);
				expenseCount++;
			} else {
				unknownCount++;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append(scopeType.equals(SCOPE_SHARED) ? "共享账本" : "个人账本").append("统计\n");
		sb.append(range.label).append("\n");
		sb.append("收入：").append(money(income)).append(" 元（").append(incomeCount).append(" 笔）\n");
		sb.append("支出：").append(money(expense)).append(" 元（").append(expenseCount).append(" 笔）\n");
		sb.append("结余：").append(money(income.subtract(expense))).append(" 元\n");
		sb.append("流水：").append(txns.size()).append(" 笔");
		int count = Math.min(10, txns.size());
		for (int i = 0; i < count; i++) {
			ImTxn txn = txns.get(i);
			boolean txnIncome = txn.getTxnType() != null && txn.getTxnType().equals(TXN_INCOME);
			sb.append("\n").append(txnIncome ? "+" : "-")
				.append(money(txn.getAmount())).append(" ")
				.append(safe(txn.getNote())).append(" #").append(txn.getOpId());
		}
		operationLogService.recordOperation(user.getId(), scopeType, groupId, scopeType.equals(SCOPE_SHARED) ? "STATS_SHARED" : "STATS_PRIVATE", null, cmd, "OK", null);
		log.info("[im-command] stats queried, userId={}, scopeType={}, groupId={}, count={}, income={}, expense={}, incomeCount={}, expenseCount={}, unknownCount={}, rangeStart={}, rangeEnd={}",
			user.getId(), scopeType, groupId, txns.size(), money(income), money(expense), incomeCount, expenseCount, unknownCount, range.start, range.end);
		return replyService.success(result, sb.toString());
	}

	private ImCommandExecuteResponse statsBillSnapshot(ImCommandExecuteResponse result, ImUser user, Integer scopeType, Long groupId, String cmd) {
		DateRange range = resolveBillRange(cmd);
		StatsSummary summary = queryStatsSummary(user, scopeType, groupId, range);
		StringBuilder sb = new StringBuilder();
		sb.append(resolveBillTitle(cmd)).append("（").append(formatDateRangeText(range, isSingleDayBill(cmd))).append("）\n");
		sb.append("💰 收入：").append(money(summary.income)).append(" 元\n");
		sb.append("💸 消费：").append(money(summary.expense)).append(" 元");
		operationLogService.recordOperation(user.getId(), scopeType, groupId, scopeType.equals(SCOPE_SHARED) ? "STATS_SHARED" : "STATS_PRIVATE", null, cmd, "OK", null);
		ImCommandExecuteResponse response = replyService.success(result, sb.toString());
		response.setReply(response.getReply()+"\n"+"> 【账单#周】获取周统计\n【账单#月】获取月统计\n【账单#年】获取年统计");

		return response;
	}

	private DateRange resolveBillRange(String cmd) {
		if (!StringUtils.hasText(cmd) || "账单".equals(cmd)) {
			return parseDateRange("日统计");
		}
		if ("账单#周".equals(cmd)) {
			return parseDateRange("周统计");
		}
		if ("账单#月".equals(cmd)) {
			return parseDateRange("月统计");
		}
		if ("账单#年".equals(cmd)) {
			return parseDateRange("年统计");
		}
		return parseDateRange("日统计");
	}

	private String resolveBillTitle(String cmd) {
		if ("账单#周".equals(cmd)) {
			return "🗓️ 周账单";
		}
		if ("账单#月".equals(cmd)) {
			return "📆 月账单";
		}
		if ("账单#年".equals(cmd)) {
			return "🧾 年账单";
		}
		return "📅 今日账单";
	}

	private boolean isSingleDayBill(String cmd) {
		return !StringUtils.hasText(cmd) || "账单".equals(cmd);
	}

	private String formatDateRangeText(DateRange range, boolean singleDay) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String startText = dateFormat.format(range.start);
		if (singleDay) {
			return startText;
		}
		Calendar endCal = Calendar.getInstance();
		endCal.setTime(range.end);
		endCal.add(Calendar.MILLISECOND, -1);
		return startText + " ~ " + dateFormat.format(endCal.getTime());
	}

	private ImCommandExecuteResponse listTxn(ImCommandExecuteResponse result, ImUser user, ImUserContext context, boolean sharedCommand, String cmd) {
		Integer scopeType = resolveScopeType(sharedCommand);
		Long groupId = scopeType.equals(SCOPE_SHARED) ? context.getCurrentGroupId() : null;
		DateRange dateRange = resolveTxnListDateRange(cmd);
		if (dateRange != null) {
			return listTxnByDate(result, user, scopeType, groupId, cmd, dateRange);
		}
		if (isDailyRecordCommand(cmd)) {
			return listTxnDailySummary(result, user, scopeType, groupId, cmd);
		}
		int requestedPageNo = resolveTxnListPageNo(cmd);
		long totalCount;
		if (scopeType.equals(SCOPE_PRIVATE)) {
			totalCount = txnService.lambdaQuery()
				.eq(ImTxn::getScopeType, scopeType)
				.eq(ImTxn::getUserId, user.getId())
				.isNull(ImTxn::getGroupId)
				.eq(ImTxn::getIsDeleted, 0)
				.count();
		} else {
			totalCount = txnService.lambdaQuery()
				.eq(ImTxn::getScopeType, scopeType)
				.eq(ImTxn::getGroupId, groupId)
				.eq(ImTxn::getIsDeleted, 0)
				.count();
		}
		int totalPages = totalCount == 0 ? 1 : (int) ((totalCount + TXN_LIST_PAGE_SIZE - 1) / TXN_LIST_PAGE_SIZE);
		int pageNo = Math.min(requestedPageNo, totalPages);
		Page<ImTxn> page = new Page<>(pageNo, TXN_LIST_PAGE_SIZE);
		if (scopeType.equals(SCOPE_PRIVATE)) {
			txnService.lambdaQuery()
				.eq(ImTxn::getScopeType, scopeType)
				.eq(ImTxn::getUserId, user.getId())
				.isNull(ImTxn::getGroupId)
				.eq(ImTxn::getIsDeleted, 0)
				.orderByDesc(ImTxn::getTxnTime)
				.page(page);
		} else {
			txnService.lambdaQuery()
				.eq(ImTxn::getScopeType, scopeType)
				.eq(ImTxn::getGroupId, groupId)
				.eq(ImTxn::getIsDeleted, 0)
				.orderByDesc(ImTxn::getTxnTime)
				.page(page);
		}
		List<ImTxn> txns = page.getRecords();
		StringBuilder sb = new StringBuilder();
		sb.append("记账记录 第").append(pageNo).append("/").append(totalPages).append("页（每页").append(TXN_LIST_PAGE_SIZE).append("笔）\n");
		sb.append("本页共 ").append(txns.size()).append(" 笔");
		for (int i = 0; i < txns.size(); i++) {
			ImTxn txn = txns.get(i);
			boolean txnIncome = txn.getTxnType() != null && txn.getTxnType().equals(TXN_INCOME);
			int seqNo = (pageNo - 1) * TXN_LIST_PAGE_SIZE + i + 1;
			sb.append("\n【").append(seqNo).append("】")
				.append(txnIncome ? "+" : "-")
				.append(money(txn.getAmount())).append(" ")
				.append(safe(txn.getNote()));
		}
		if (txns.size() == TXN_LIST_PAGE_SIZE) {
			sb.append("\n下一页：列表#").append(pageNo + 1);
		}
		operationLogService.recordOperation(user.getId(), scopeType, groupId, "TXN_LIST", null, "最近账单", "OK", null);
		return replyService.success(result, sb.toString());
	}

	private ImCommandExecuteResponse listTxnDailySummary(ImCommandExecuteResponse result, ImUser user, Integer scopeType, Long groupId, String cmd) {
		List<ImTxn> allTxns;
		if (scopeType.equals(SCOPE_PRIVATE)) {
			allTxns = txnService.lambdaQuery()
				.eq(ImTxn::getScopeType, scopeType)
				.eq(ImTxn::getUserId, user.getId())
				.isNull(ImTxn::getGroupId)
				.eq(ImTxn::getIsDeleted, 0)
				.orderByDesc(ImTxn::getTxnTime)
				.list();
		} else {
			allTxns = txnService.lambdaQuery()
				.eq(ImTxn::getScopeType, scopeType)
				.eq(ImTxn::getGroupId, groupId)
				.eq(ImTxn::getIsDeleted, 0)
				.orderByDesc(ImTxn::getTxnTime)
				.list();
		}
		SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
		Map<String, DayTxnSummary> daySummaryMap = new LinkedHashMap<>();
		for (ImTxn txn : allTxns) {
			String dayKey = txn.getTxnTime() == null ? "-" : dayFormat.format(txn.getTxnTime());
			DayTxnSummary summary = daySummaryMap.computeIfAbsent(dayKey, DayTxnSummary::new);
			summary.txnCount++;
			BigDecimal amount = txn.getAmount() == null ? BigDecimal.ZERO : txn.getAmount();
			if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_INCOME)) {
				summary.income = summary.income.add(amount);
			} else if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_EXPENSE)) {
				summary.expense = summary.expense.add(amount);
			}
		}
		List<DayTxnSummary> summaries = new ArrayList<>(daySummaryMap.values());
		Integer detailIndex = resolveDailyRecordIndex(cmd);
		if (detailIndex != null) {
			if (detailIndex < 1 || detailIndex > summaries.size()) {
				return replyService.failure(result, "BAD_INDEX", "序号超出范围，请先发送“账单记录”查看可用序号。");
			}
			DayTxnSummary target = summaries.get(detailIndex - 1);
			Date parsedDate = parseDateByPattern(target.dayLabel, "yyyy-MM-dd");
			if (parsedDate == null) {
				return replyService.failure(result, "BAD_INDEX", "该序号对应日期解析失败，请改用“账单记录 yyyy-MM-dd”。");
			}
			Calendar start = Calendar.getInstance();
			start.setTime(parsedDate);
			start.set(Calendar.HOUR_OF_DAY, 0);
			start.set(Calendar.MINUTE, 0);
			start.set(Calendar.SECOND, 0);
			start.set(Calendar.MILLISECOND, 0);
			Calendar end = Calendar.getInstance();
			end.setTime(start.getTime());
			end.add(Calendar.DAY_OF_MONTH, 1);
			DateRange range = new DateRange(start.getTime(), end.getTime(), target.dayLabel);
			return listTxnByDate(result, user, scopeType, groupId, cmd, range);
		}
		int requestedPageNo = resolveTxnListPageNo(cmd);
		int totalPages = summaries.isEmpty() ? 1 : (int) ((summaries.size() + TXN_LIST_PAGE_SIZE - 1) / TXN_LIST_PAGE_SIZE);
		int pageNo = Math.min(requestedPageNo, totalPages);
		int fromIndex = Math.max(0, (pageNo - 1) * TXN_LIST_PAGE_SIZE);
		int toIndex = Math.min(summaries.size(), fromIndex + TXN_LIST_PAGE_SIZE);
		List<DayTxnSummary> pageItems = summaries.subList(fromIndex, toIndex);

		StringBuilder sb = new StringBuilder();
		sb.append("账单记录 第").append(pageNo).append("/").append(totalPages).append("页（按天统计）\n");
		for (int i = 0; i < pageItems.size(); i++) {
			DayTxnSummary item = pageItems.get(i);
			sb.append("\n【").append(fromIndex + i + 1).append("】")
				.append(item.dayLabel)
				.append(" 收入").append(money(item.income))
				.append(" / 支出").append(money(item.expense))
				.append(" / 共").append(item.txnCount).append("笔");
		}
		if (pageNo < totalPages) {
			sb.append("\n下一页：账单记录#").append(pageNo + 1);
		}
		operationLogService.recordOperation(user.getId(), scopeType, groupId, "TXN_LIST", null, cmd, "OK", "daily-summary");
		ImCommandExecuteResponse success = replyService.success(result, sb.toString());
		success.setReply(success.getReply()+"\n> 查看详情：账单记录1");
		return success;
	}

	private ImCommandExecuteResponse listTxnByDate(ImCommandExecuteResponse result, ImUser user, Integer scopeType, Long groupId, String cmd, DateRange range) {
		List<ImTxn> txns;
		if (scopeType.equals(SCOPE_PRIVATE)) {
			txns = txnService.lambdaQuery()
				.eq(ImTxn::getScopeType, scopeType)
				.eq(ImTxn::getUserId, user.getId())
				.isNull(ImTxn::getGroupId)
				.eq(ImTxn::getIsDeleted, 0)
				.ge(ImTxn::getTxnTime, range.start)
				.lt(ImTxn::getTxnTime, range.end)
				.orderByDesc(ImTxn::getTxnTime)
				.list();
		} else {
			txns = txnService.lambdaQuery()
				.eq(ImTxn::getScopeType, scopeType)
				.eq(ImTxn::getGroupId, groupId)
				.eq(ImTxn::getIsDeleted, 0)
				.ge(ImTxn::getTxnTime, range.start)
				.lt(ImTxn::getTxnTime, range.end)
				.orderByDesc(ImTxn::getTxnTime)
				.list();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("账单记录（").append(range.label).append("）\n");
		sb.append("共 ").append(txns.size()).append(" 笔");
		for (int i = 0; i < txns.size(); i++) {
			ImTxn txn = txns.get(i);
			boolean txnIncome = txn.getTxnType() != null && txn.getTxnType().equals(TXN_INCOME);
			sb.append("\n【").append(i + 1).append("】")
				.append(txnIncome ? "+" : "-")
				.append(money(txn.getAmount())).append(" ")
				.append(safe(txn.getNote()));
		}
		operationLogService.recordOperation(user.getId(), scopeType, groupId, "TXN_LIST", null, cmd, "OK", "date=" + range.label);
		return replyService.success(result, sb.toString());
	}

	private boolean isTxnListCommand(String cmd) {
		if (!StringUtils.hasText(cmd)) {
			return false;
		}
		String normalized = cmd.trim();
		return "最近账单".equals(normalized)
			|| "账单列表".equals(normalized)
			|| "查看账单".equals(normalized)
			|| normalized.startsWith("查看账单记录 ")
			|| "记账记录".equals(normalized)
			|| "列表".equals(normalized)
			|| "账单记录".equals(normalized)
			|| DAILY_RECORD_INDEX_PATTERN.matcher(normalized).matches()
			|| normalized.startsWith("列表 ")
			|| normalized.startsWith("账单记录 ")
			|| normalized.startsWith("列表#")
			|| normalized.startsWith("账单记录#");
	}

	private boolean isDailyRecordCommand(String cmd) {
		if (!StringUtils.hasText(cmd)) {
			return false;
		}
		String normalized = cmd.trim();
		return "账单记录".equals(normalized)
			|| normalized.startsWith("账单记录#")
			|| DAILY_RECORD_INDEX_PATTERN.matcher(normalized).matches();
	}

	private Integer resolveDailyRecordIndex(String cmd) {
		if (!StringUtils.hasText(cmd)) {
			return null;
		}
		Matcher matcher = DAILY_RECORD_INDEX_PATTERN.matcher(cmd.trim());
		if (!matcher.matches()) {
			return null;
		}
		try {
			return Integer.parseInt(matcher.group(1));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private DateRange resolveTxnListDateRange(String cmd) {
		if (!StringUtils.hasText(cmd)) {
			return null;
		}
		Matcher matcher = TXN_LIST_DATE_PATTERN.matcher(cmd.trim());
		if (!matcher.matches()) {
			return null;
		}
		Date parsedDate = parseDateToken(matcher.group(2).trim(), new Date());
		if (parsedDate == null) {
			return null;
		}
		Calendar start = Calendar.getInstance();
		start.setTime(parsedDate);
		start.set(Calendar.HOUR_OF_DAY, 0);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		Calendar end = Calendar.getInstance();
		end.setTime(start.getTime());
		end.add(Calendar.DAY_OF_MONTH, 1);
		return new DateRange(start.getTime(), end.getTime(), new SimpleDateFormat("yyyy-MM-dd").format(start.getTime()));
	}

	private int resolveTxnListPageNo(String cmd) {
		if (!StringUtils.hasText(cmd)) {
			return 1;
		}
		Matcher matcher = TXN_LIST_PAGE_PATTERN.matcher(cmd.trim());
		if (!matcher.matches()) {
			return 1;
		}
		String prefix = matcher.group(1);
		if (!"列表".equals(prefix) && !"账单记录".equals(prefix)) {
			return 1;
		}
		try {
			int pageNo = Integer.parseInt(matcher.group(2));
			return pageNo > 0 ? pageNo : 1;
		} catch (NumberFormatException ex) {
			return 1;
		}
	}

	private ImCommandExecuteResponse invite(ImCommandExecuteResponse result, ImUser user, ImUserContext context, String cmd) {
		String inviteeAlias = cmd.replace("邀请记账", "").replace("生成邀请码", "").trim();
		ImGroup group = ensureSharedSlot(user, context);
		if (group == null) {
			return replyService.failure(result, "NO_GROUP", "没有可用的共享账本。");
		}
		if (groupMemberService.getMemberRole(group.getId(), user.getId()) == null) {
			ImGroup defaultGroup = getOrCreateDefaultGroup(user);
			setSharedSlot(context, defaultGroup.getId());
			group = defaultGroup;
		}
		Calendar expires = Calendar.getInstance();
		expires.add(Calendar.MINUTE, 30);
		ImInviteCode code = inviteCodeService.createInviteCode(group.getId(), user.getId(), inviteeAlias, 1, expires.getTime());
		operationLogService.recordOperation(user.getId(), SCOPE_SHARED, group.getId(), "GROUP_INVITE", null, cmd, "OK", null);
		log.info("[im-command] invite created, userId={}, groupId={}, groupName={}, inviteCode={}, inviteeAlias={}, maxUse={}, expiresAt={}",
			user.getId(), group.getId(), group.getGroupName(), code.getInviteCode(), code.getInviteeAlias(), code.getMaxUse(), code.getExpiresAt());
		StringBuilder reply = new StringBuilder();
		reply.append("🎟️ 邀请码已生成\n");
		reply.append("🔑 邀请码：").append(code.getInviteCode()).append("\n");
		if (StringUtils.hasText(code.getInviteeAlias())) {
			reply.append("📝 备注：").append(code.getInviteeAlias()).append("\n");
		}
		reply.append("⏰ 有效期：30分钟\n");
		reply.append("👤 使用次数：1次");
		reply.append("\n> 对方发送【加入记账 ").append(code.getInviteCode()).append("】即可加入");
		return replyService.success(result, reply.toString());
	}

	private ImCommandExecuteResponse joinGroup(ImCommandExecuteResponse result, ImUser user, ImUserContext context, String cmd) {
		String inviteCode = cmd.replace("加入记账", "").replace("加入", "").trim().toUpperCase();
		if (!StringUtils.hasText(inviteCode)) {
			return replyService.failure(result, "BAD_INVITE", "请输入邀请码。示例：加入 UIJWSK。");
		}
		ImInviteCode code = inviteCodeService.getAvailableInviteCode(inviteCode, user.getId());
		ImGroup currentShared = ensureSharedSlot(user, context);
		if (currentShared != null && currentShared.getId().equals(code.getGroupId())
			&& groupMemberService.getMemberRole(currentShared.getId(), user.getId()) != null) {
			setSharedSlot(context, currentShared.getId());
			ImGroup ownerGroup = groupService.getById(code.getGroupId());
			String ownerName = "对方";
			if (ownerGroup != null && ownerGroup.getOwnerUserId() != null) {
				ImUser owner = userService.getById(ownerGroup.getOwnerUserId());
				ownerName = displayNickname(owner);
			}
			StringBuilder sameGroupReply = new StringBuilder();
			sameGroupReply.append("✅ 已在共享记账中\n");
			sameGroupReply.append("📒 你已经在").append(ownerName).append("的共享组里\n");
			sameGroupReply.append("🔄 已为你切换到共享模式");
			return replyService.success(result, sameGroupReply.toString());
		}
		Long previousGroupId = currentShared == null ? null : currentShared.getId();
		ImGroupMember member = groupMemberService.joinGroupByInviteCode(inviteCode, user.getId());
		deactivateOtherJoinedGroups(user, member.getGroupId());
		setSharedSlot(context, member.getGroupId());
		ImGroup group = groupService.getById(member.getGroupId());
		operationLogService.recordOperation(user.getId(), SCOPE_SHARED, member.getGroupId(), "GROUP_JOIN", null, cmd, "OK", null);
		log.info("[im-command] group joined, userId={}, groupId={}, inviteCode={}", user.getId(), member.getGroupId(), inviteCode);
		String ownerName = "-";
		if (group != null && group.getOwnerUserId() != null) {
			ImUser owner = userService.getById(group.getOwnerUserId());
			ownerName = displayNickname(owner);
		}
		StringBuilder reply = new StringBuilder();
		reply.append("✅ 加入成功\n");
		reply.append("👥 已加入一起记账\n");
		reply.append("🏷️ 邀请码：").append(inviteCode).append("\n");
		if (previousGroupId != null && !previousGroupId.equals(member.getGroupId())) {
			reply.append("🔄 已从旧共享组切换到新共享组\n");
		}
		reply.append("👤 共享组发起人：").append(ownerName).append("\n");
		reply.append("> 邀请码为一次性，使用后自动失效");
		return replyService.success(result, reply.toString());
	}

	private ImCommandExecuteResponse memberList(ImCommandExecuteResponse result, ImUser user, ImUserContext context) {
		ImGroup sharedGroup = ensureSharedSlot(user, context);
		if (sharedGroup == null) {
			return replyService.failure(result, "NO_GROUP", "当前没有可用的共享账本。");
		}
		List<ImGroupMember> members = groupService.getGroupMembers(sharedGroup.getId());
		StringBuilder sb = new StringBuilder("👥 成员列表");
		Map<Long, String> memberDisplayMap = new HashMap<>();
		Map<String, Integer> nameCountMap = new HashMap<>();
		for (ImGroupMember member : members) {
			ImUser memberUser = userService.getById(member.getUserId());
			String display = displayName(sharedGroup.getId(), user.getId(), member.getUserId(), memberUser);
			memberDisplayMap.put(member.getUserId(), display);
			nameCountMap.put(display, nameCountMap.getOrDefault(display, 0) + 1);
		}
		boolean hasDuplicatedName = false;
		Map<String, Integer> duplicateIndexMap = new HashMap<>();
		for (ImGroupMember member : members) {
			String displayName = memberDisplayMap.get(member.getUserId());
			boolean duplicatedName = StringUtils.hasText(displayName) && nameCountMap.getOrDefault(displayName, 0) > 1;
			hasDuplicatedName = hasDuplicatedName || duplicatedName;
			String finalName = displayName;
			if (duplicatedName) {
				int duplicateIndex = duplicateIndexMap.getOrDefault(displayName, 0);
				if (duplicateIndex > 0) {
					finalName = displayName + " -" + duplicateIndex;
				}
				duplicateIndexMap.put(displayName, duplicateIndex + 1);
			}
			if (member.getRoleType() != null && member.getRoleType() == 1) {
				sb.append("\n👑 创建者：").append(finalName);
			} else {
				sb.append("\n🙋 成员：").append(finalName);
			}
		}
		if (hasDuplicatedName) {
			sb.append("\n⚠️ 存在重名，已自动追加序号后缀区分");
		}
		log.info("[im-command] member list queried, userId={}, groupId={}, count={}", user.getId(), sharedGroup.getId(), members.size());
		operationLogService.recordOperation(user.getId(), SCOPE_SHARED, sharedGroup.getId(), "GROUP_MEMBER_LIST", null, "成员", "OK", null);
		return replyService.success(result, sb.toString().trim());
	}

	private ImCommandExecuteResponse updateNickname(ImCommandExecuteResponse result, ImUser user, String cmd) {
		String newName = cmd.replaceFirst("^修改昵称", "").replaceFirst("^昵称\\s+", "").trim();
		if (!StringUtils.hasText(newName)) {
			return replyService.failure(result, "BAD_NICKNAME", "格式：修改昵称 新昵称");
		}
		if (newName.length() > 40) {
			return replyService.failure(result, "BAD_NICKNAME", "昵称最多40个字符。");
		}
		user.setNickName(newName);
		user.setUpdateTime(new Date());
		userService.updateById(user);
		operationLogService.recordOperation(user.getId(), SCOPE_PRIVATE, null, "USER_NICKNAME_UPDATE", null, cmd, "OK", null);
		log.info("[im-command] nickname updated, userId={}, nickName={}", user.getId(), newName);
		return replyService.success(result, "昵称已更新为：" + newName);
	}

	private ImCommandExecuteResponse myWechatId(ImCommandExecuteResponse result, ImUser user) {
		String wxUserId = user == null ? "" : user.getWxUserId();
		if (!StringUtils.hasText(wxUserId)) {
			return replyService.failure(result, "NO_WECHAT_ID", "暂未获取到你的 wechatId。");
		}
		return replyService.success(result, "🆔 我的 wechatId\n" + wxUserId);
	}

	private boolean isBudgetSetCommand(String cmd) {
		return StringUtils.hasText(cmd) && BUDGET_PATTERN.matcher(cmd.trim()).matches();
	}

	private boolean isBudgetCloseCommand(String cmd) {
		return StringUtils.hasText(cmd) && BUDGET_CLOSE_PATTERN.matcher(cmd.trim()).matches();
	}

	private ImCommandExecuteResponse setBudget(ImCommandExecuteResponse result, ImUser user, String cmd) {
		Matcher matcher = BUDGET_PATTERN.matcher(cmd.trim());
		if (!matcher.matches()) {
			return replyService.failure(result, "BAD_BUDGET", "格式：预算 日100 / 预算 月3000 / 预算 年10000");
		}
		Integer periodType = resolveBudgetPeriodType(matcher.group(1));
		BigDecimal amount = new BigDecimal(matcher.group(2)).setScale(2, RoundingMode.HALF_UP);
		if (periodType == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			return replyService.failure(result, "BAD_BUDGET", "预算金额必须大于0。");
		}
		QueryWrapper<ImBudgetSetting> wrapper = new QueryWrapper<>();
		wrapper.eq("user_id", user.getId());
		wrapper.eq("period_type", periodType);
		ImBudgetSetting setting = budgetSettingService.getOne(wrapper);
		Date now = new Date();
		if (setting == null) {
			setting = new ImBudgetSetting();
			setting.setUserId(user.getId());
			setting.setPeriodType(periodType);
			setting.setBudgetAmount(amount);
			setting.setStatus(1);
			setting.setCreateTime(now);
			setting.setUpdateTime(now);
			budgetSettingService.save(setting);
		} else {
			setting.setBudgetAmount(amount);
			setting.setStatus(1);
			setting.setUpdateTime(now);
			budgetSettingService.updateById(setting);
		}
		operationLogService.recordOperation(user.getId(), SCOPE_PRIVATE, null, "BUDGET_SET", null, cmd, "OK", null);
		String periodLabel = budgetPeriodLabel(periodType);
		String overview = buildBudgetReminder(user);
		String reply = "✅ 预算已设置\n周期：" + periodLabel + "\n预算：" + money(amount) + " 元";
		if (StringUtils.hasText(overview)) {
			reply = reply + "\n" + overview;
		}
		return replyService.success(result, reply);
	}

	private ImCommandExecuteResponse closeBudget(ImCommandExecuteResponse result, ImUser user, String cmd) {
		Matcher matcher = BUDGET_CLOSE_PATTERN.matcher(cmd.trim());
		if (!matcher.matches()) {
			return replyService.failure(result, "BAD_BUDGET_CLOSE", "格式：关闭预算 日/月/年");
		}
		Integer periodType = resolveBudgetPeriodType(matcher.group(1));
		if (periodType == null) {
			return replyService.failure(result, "BAD_BUDGET_CLOSE", "仅支持：关闭预算 日/月/年");
		}
		QueryWrapper<ImBudgetSetting> wrapper = new QueryWrapper<>();
		wrapper.eq("user_id", user.getId());
		wrapper.eq("period_type", periodType);
		ImBudgetSetting setting = budgetSettingService.getOne(wrapper);
		if (setting == null || setting.getStatus() == null || setting.getStatus() != 1) {
			return replyService.failure(result, "BUDGET_NOT_FOUND", "该周期预算尚未开启，无需关闭。");
		}
		setting.setStatus(0);
		setting.setUpdateTime(new Date());
		budgetSettingService.updateById(setting);
		operationLogService.recordOperation(user.getId(), SCOPE_PRIVATE, null, "BUDGET_CLOSE", null, cmd, "OK", null);
		String periodLabel = budgetPeriodLabel(periodType);
		String overview = buildBudgetReminder(user);
		String reply = "✅ 已关闭预算\n周期：" + periodLabel;
		if (StringUtils.hasText(overview)) {
			reply = reply + "\n" + overview;
		}
		return replyService.success(result, reply);
	}

	private ImCommandExecuteResponse budgetOverview(ImCommandExecuteResponse result, ImUser user) {
		String overview = buildBudgetReminder(user);
		if (!StringUtils.hasText(overview)) {
			return replyService.success(result, "你还没有设置预算。\n可发送：预算 日100 / 预算 月3000 / 预算 年10000");
		}
		return replyService.success(result, overview);
	}

	private Integer resolveBudgetPeriodType(String periodText) {
		if ("日".equals(periodText)) {
			return BUDGET_DAY;
		}
		if ("月".equals(periodText)) {
			return BUDGET_MONTH;
		}
		if ("年".equals(periodText)) {
			return BUDGET_YEAR;
		}
		return null;
	}

	private String budgetPeriodLabel(Integer periodType) {
		if (periodType == null) {
			return "-";
		}
		if (periodType.equals(BUDGET_DAY)) {
			return "日预算";
		}
		if (periodType.equals(BUDGET_MONTH)) {
			return "月预算";
		}
		if (periodType.equals(BUDGET_YEAR)) {
			return "年预算";
		}
		return "-";
	}

	private String buildBudgetReminder(ImUser user) {
		if (user == null || user.getId() == null) {
			return "";
		}
		List<ImBudgetSetting> settings = budgetSettingService.lambdaQuery()
			.eq(ImBudgetSetting::getUserId, user.getId())
			.eq(ImBudgetSetting::getStatus, 1)
			.orderByAsc(ImBudgetSetting::getPeriodType)
			.list();
		if (settings == null || settings.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder("📉 预算概览");
		Date now = new Date();
		for (ImBudgetSetting setting : settings) {
			DateRange range = budgetRange(setting.getPeriodType(), now);
			if (range == null) {
				continue;
			}
			BigDecimal used = queryExpenseInRange(user.getId(), range);
			BigDecimal total = setting.getBudgetAmount() == null ? BigDecimal.ZERO : setting.getBudgetAmount().setScale(2, RoundingMode.HALF_UP);
			BigDecimal remain = total.subtract(used).setScale(2, RoundingMode.HALF_UP);
			sb.append("\n").append(budgetAlertEmoji(remain, total))
				.append(" ")
				.append(budgetPeriodLabel(setting.getPeriodType()))
				.append("：已用 ").append(money(used))
				.append(" / ").append(money(total))
				.append(" 元，剩余 ").append(money(remain)).append(" 元");
		}
		return sb.toString();
	}

	private String budgetAlertEmoji(BigDecimal remain, BigDecimal total) {
		if (remain == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
			return "💡";
		}
		if (remain.compareTo(BigDecimal.ZERO) < 0) {
			return "🚨";
		}
		BigDecimal ratio = remain.divide(total, 4, RoundingMode.HALF_UP);
		if (ratio.compareTo(new BigDecimal("0.20")) <= 0) {
			return "⚠️";
		}
		return "✅";
	}

	private BigDecimal queryExpenseInRange(Long userId, DateRange range) {
		QueryWrapper<ImTxn> wrapper = new QueryWrapper<>();
		wrapper.eq("user_id", userId);
		wrapper.eq("txn_type", TXN_EXPENSE);
		wrapper.eq("is_deleted", 0);
		wrapper.ge("txn_time", range.start);
		wrapper.lt("txn_time", range.end);
		List<ImTxn> txns = txnService.list(wrapper);
		BigDecimal total = BigDecimal.ZERO;
		for (ImTxn txn : txns) {
			if (txn != null && txn.getAmount() != null) {
				total = total.add(txn.getAmount());
			}
		}
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	private DateRange budgetRange(Integer periodType, Date now) {
		Calendar start = Calendar.getInstance();
		start.setTime(now);
		start.set(Calendar.HOUR_OF_DAY, 0);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		Calendar end = Calendar.getInstance();
		end.setTime(start.getTime());
		if (BUDGET_DAY == periodType) {
			end.add(Calendar.DAY_OF_MONTH, 1);
			return new DateRange(start.getTime(), end.getTime(), "日预算");
		}
		if (BUDGET_MONTH == periodType) {
			start.set(Calendar.DAY_OF_MONTH, 1);
			end.setTime(start.getTime());
			end.add(Calendar.MONTH, 1);
			return new DateRange(start.getTime(), end.getTime(), "月预算");
		}
		if (BUDGET_YEAR == periodType) {
			start.set(Calendar.DAY_OF_YEAR, 1);
			end.setTime(start.getTime());
			end.add(Calendar.YEAR, 1);
			return new DateRange(start.getTime(), end.getTime(), "年预算");
		}
		return null;
	}

	private ImCommandExecuteResponse aliasMember(ImCommandExecuteResponse result, ImUser user, ImUserContext context, String cmd) {
		ImGroup sharedGroup = ensureSharedSlot(user, context);
		if (sharedGroup == null) {
			return replyService.failure(result, "NO_GROUP", "当前没有可用的共享账本。");
		}
		String args = cmd.replaceFirst("^备注成员", "").replaceFirst("^修改备注", "").trim();
		String[] parts = args.split("\\s+", 2);
		if (parts.length < 2) {
			return replyService.failure(result, "BAD_ALIAS", "格式：修改备注 成员名称 备注名。先发送“成员”查看成员名称。");
		}
		Long targetUserId = resolveTargetMemberId(sharedGroup.getId(), user.getId(), parts[0].trim());
		if (targetUserId == null) {
			return replyService.failure(result, "BAD_ALIAS", "未找到对应成员，请发送“成员”确认名称后再试。");
		}
		if (groupMemberService.getMemberRole(sharedGroup.getId(), targetUserId) == null) {
			return replyService.failure(result, "NO_MEMBER", "该用户不在当前账本中。");
		}
		QueryWrapper<ImMemberAlias> wrapper = new QueryWrapper<>();
		wrapper.eq("group_id", sharedGroup.getId());
		wrapper.eq("owner_user_id", user.getId());
		wrapper.eq("target_user_id", targetUserId);
		ImMemberAlias alias = memberAliasService.getOne(wrapper);
		Date now = new Date();
		if (alias == null) {
			alias = new ImMemberAlias();
			alias.setGroupId(sharedGroup.getId());
			alias.setOwnerUserId(user.getId());
			alias.setTargetUserId(targetUserId);
			alias.setAliasName(parts[1].trim());
			alias.setCreateTime(now);
			alias.setUpdateTime(now);
			memberAliasService.save(alias);
		} else {
			alias.setAliasName(parts[1].trim());
			alias.setUpdateTime(now);
			memberAliasService.updateById(alias);
		}
		log.info("[im-command] member alias saved, userId={}, groupId={}, targetUserId={}, alias={}",
			user.getId(), sharedGroup.getId(), targetUserId, parts[1].trim());
		operationLogService.recordOperation(user.getId(), SCOPE_SHARED, sharedGroup.getId(), "GROUP_MEMBER_ALIAS", null, cmd, "OK", null);
		return replyService.success(result, "已修改备注： " + parts[0].trim() + " -> " + parts[1].trim());
	}

	private Long resolveTargetMemberId(Long groupId, Long ownerUserId, String keyword) {
		Long byId = parseUserId(keyword);
		if (byId != null) {
			return byId;
		}
		List<ImGroupMember> members = groupService.getGroupMembers(groupId);
		Long matchedUserId = null;
		for (ImGroupMember member : members) {
			ImUser memberUser = userService.getById(member.getUserId());
			String display = displayName(groupId, ownerUserId, member.getUserId(), memberUser);
			String nickName = memberUser == null ? "" : safe(memberUser.getNickName());
			String wxUserId = memberUser == null ? "" : safe(memberUser.getWxUserId());
			if (keyword.equals(display) || keyword.equals(nickName) || keyword.equals(wxUserId)) {
				if (matchedUserId != null && !matchedUserId.equals(member.getUserId())) {
					return null;
				}
				matchedUserId = member.getUserId();
			}
		}
		return matchedUserId;
	}

	private ImCommandExecuteResponse removeMember(ImCommandExecuteResponse result, ImUser user, ImUserContext context, String cmd) {
		ImGroup sharedGroup = ensureSharedSlot(user, context);
		if (sharedGroup == null) {
			return replyService.failure(result, "NO_GROUP", "当前没有可用的共享账本。");
		}
		if (!canManageGroup(sharedGroup.getId(), user.getId())) {
			return replyService.failure(result, "FORBIDDEN", "只有账本创建者可以移除成员。");
		}
		String arg = cmd.replace("移除成员", "").trim();
		if (!StringUtils.hasText(arg)) {
			return replyService.failure(result, "BAD_REMOVE", "格式：移除成员 成员ID。");
		}
		Long targetUserId = parseUserId(arg);
		if (targetUserId == null) {
			return replyService.failure(result, "BAD_REMOVE", "成员ID必须是数字。先发送“成员”查看成员ID。");
		}
		if (targetUserId.equals(user.getId())) {
			return replyService.failure(result, "BAD_REMOVE", "不能移除自己，创建者可发送“确认退出”解散账本。");
		}
		QueryWrapper<ImGroupMember> wrapper = new QueryWrapper<>();
		wrapper.eq("group_id", sharedGroup.getId());
		wrapper.eq("user_id", targetUserId);
		wrapper.eq("status", 1);
		ImGroupMember member = groupMemberService.getOne(wrapper);
		if (member == null) {
			return replyService.failure(result, "NO_MEMBER", "该用户不在当前账本中。");
		}
		member.setStatus(0);
		member.setLeftAt(new Date());
		groupMemberService.updateById(member);
		log.info("[im-command] member removed, operatorUserId={}, groupId={}, targetUserId={}", user.getId(), sharedGroup.getId(), targetUserId);
		operationLogService.recordOperation(user.getId(), SCOPE_SHARED, sharedGroup.getId(), "GROUP_MEMBER_REMOVE", null, cmd, "OK", null);
		return replyService.success(result, "已移除成员：" + targetUserId);
	}

	private ImCommandExecuteResponse quitGroup(ImCommandExecuteResponse result, ImUser user, ImUserContext context) {
		ImGroup sharedGroup = ensureSharedSlot(user, context);
		if (sharedGroup == null) {
			return replyService.failure(result, "NO_GROUP", "当前没有可用的共享账本。");
		}
		Long oldGroupId = sharedGroup.getId();
		Integer role = groupMemberService.getMemberRole(oldGroupId, user.getId());
		if (role != null && role == 1) {
			ImGroup defaultGroup = getOrCreateDefaultGroup(user);
			if (oldGroupId.equals(defaultGroup.getId())) {
				return replyService.failure(result, "DEFAULT_GROUP", "默认账本不能退出或解散。可以继续邀请别人一起记账，或加入其他人的账本。");
			}
			groupService.dissolveGroup(oldGroupId, user.getId());
			setSharedSlot(context, defaultGroup.getId());
			log.info("[im-command] group dissolved by owner, userId={}, groupId={}", user.getId(), oldGroupId);
			operationLogService.recordOperation(user.getId(), SCOPE_SHARED, oldGroupId, "GROUP_DISSOLVE", null, "确认退出", "OK", null);
			return replyService.success(result, "已解散共享账本，共享槽位已回到你的默认共享账本：" + defaultGroup.getGroupName());
		}
		groupMemberService.leaveGroup(oldGroupId, user.getId());
		ImGroup defaultGroup = getOrCreateDefaultGroup(user);
		setSharedSlot(context, defaultGroup.getId());
		log.info("[im-command] group left, userId={}, groupId={}", user.getId(), oldGroupId);
		operationLogService.recordOperation(user.getId(), SCOPE_SHARED, oldGroupId, "GROUP_QUIT", null, "退出账本", "OK", null);
		return replyService.success(result, "已退出共享账本，共享槽位已回到你的默认共享账本：" + defaultGroup.getGroupName() + "\n个人账本不受影响。");
	}

	private ImCommandExecuteResponse releaseToDefaultGroup(ImCommandExecuteResponse result, ImUser user, ImUserContext context) {
		ImGroup defaultGroup = getOrCreateDefaultGroup(user);
		if (defaultGroup == null || defaultGroup.getId() == null) {
			return replyService.failure(result, "NO_DEFAULT_GROUP", "未找到你的默认共享组，请稍后重试。");
		}
		Long currentGroupId = context == null ? null : context.getCurrentGroupId();
		if (defaultGroup.getId().equals(currentGroupId)) {
			return replyService.success(result, "✅ 当前已在你的默认共享组，无需退出。");
		}
		setSharedSlot(context, defaultGroup.getId());
		log.info("[im-command] shared slot released to default group, userId={}, defaultGroupId={}",
			user.getId(), defaultGroup.getId());
		operationLogService.recordOperation(user.getId(), SCOPE_SHARED, defaultGroup.getId(), "GROUP_RELEASE", null, "退出", "OK", null);
		return replyService.success(result, "✅ 已退出当前共享组，已恢复到你的默认共享组。");
	}

	private ImCommandExecuteResponse export(ImCommandExecuteResponse result, ImUser user, ImUserContext context, String cmd, boolean sharedCommand) {
		Date now = new Date();
		DateRange range = parseDateRange(cmd);
		Calendar cal = Calendar.getInstance();
		cal.setTime(range.start);
		Integer scopeType = resolveScopeType(sharedCommand);
		Long groupId = scopeType.equals(SCOPE_SHARED) ? context.getCurrentGroupId() : null;
		ImExportTask task = new ImExportTask();
		task.setTaskNo("EXP" + new SimpleDateFormat("yyyyMMddHHmmss").format(now) + RANDOM.nextInt(1000));
		task.setUserId(user.getId());
		task.setScopeType(scopeType);
		task.setGroupId(groupId);
		task.setYearVal(cal.get(Calendar.YEAR));
		task.setMonthVal(cal.get(Calendar.MONTH) + 1);
		task.setStatus(0);
		task.setProgress(0);
		task.setCreateTime(now);
		exportTaskService.save(task);

		try {
			QueryWrapper<ImTxn> wrapper = new QueryWrapper<>();
			wrapper.eq("scope_type", scopeType);
			if (scopeType.equals(SCOPE_PRIVATE)) {
				wrapper.eq("user_id", user.getId());
				wrapper.isNull("group_id");
			} else {
				wrapper.eq("group_id", groupId);
			}
			wrapper.eq("is_deleted", 0);
			wrapper.ge("txn_time", range.start);
			wrapper.lt("txn_time", range.end);
			wrapper.orderByAsc("txn_time");
			List<ImTxn> txns = txnService.list(wrapper);

			Path dir = Paths.get("target", "imoney-exports");
			Files.createDirectories(dir);
			String fileName = task.getTaskNo() + ".xlsx";
			Path file = dir.resolve(fileName).toAbsolutePath();
			Files.write(file, buildExcel(txns));

			task.setStatus(2);
			task.setProgress(100);
			task.setFileUrl(file.toString());
			task.setFinishedAt(new Date());
			exportTaskService.updateById(task);

			result.setFilePath(file.toString());
			result.setFileName(fileName);
			operationLogService.recordOperation(user.getId(), scopeType, groupId, "EXPORT_EXCEL", null, cmd, "OK", null);
			log.info("[im-command] export generated, userId={}, taskNo={}, filePath={}, count={}", user.getId(), task.getTaskNo(), file, txns.size());
			return replyService.success(result, "已生成导出文件：" + fileName + "\n范围：" + range.label + "\n流水：" + txns.size() + " 笔");
		} catch (Exception ex) {
			task.setStatus(3);
			task.setErrorMsg(ex.getMessage());
			task.setFinishedAt(new Date());
			exportTaskService.updateById(task);
			throw new RuntimeException("导出失败：" + ex.getMessage(), ex);
		}
	}

	private ImCommandExecuteResponse expenseAnalysis(ImCommandExecuteResponse result, ImUser user, ImUserContext context, boolean sharedCommand) {
		Integer scopeType = resolveScopeType(sharedCommand);
		Long groupId = scopeType.equals(SCOPE_SHARED) ? context.getCurrentGroupId() : null;
		if (scopeType.equals(SCOPE_SHARED) && groupId == null) {
			return replyService.failure(result, "NO_GROUP", "当前不在共享账本中。发送“邀请记账 家庭账本”创建，或“加入记账 邀请码”加入。");
		}
		DateRange range = parseDateRange("月统计");
		QueryWrapper<ImTxn> wrapper = new QueryWrapper<>();
		wrapper.eq("scope_type", scopeType);
		if (scopeType.equals(SCOPE_PRIVATE)) {
			wrapper.eq("user_id", user.getId());
			wrapper.isNull("group_id");
		} else {
			wrapper.eq("group_id", groupId);
		}
		wrapper.eq("is_deleted", 0);
		wrapper.ge("txn_time", range.start);
		wrapper.lt("txn_time", range.end);
		wrapper.orderByDesc("txn_time");
		List<ImTxn> txns = txnService.list(wrapper);

		List<ImTxn> expenseTxns = new ArrayList<>();
		BigDecimal totalExpense = BigDecimal.ZERO;
		BigDecimal totalIncome = BigDecimal.ZERO;
		Map<String, BigDecimal> noteExpenseMap = new HashMap<>();
		for (ImTxn txn : txns) {
			BigDecimal amount = txn.getAmount() == null ? BigDecimal.ZERO : txn.getAmount();
			if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_EXPENSE)) {
				expenseTxns.add(txn);
				totalExpense = totalExpense.add(amount);
				String noteKey = StringUtils.hasText(txn.getNote()) ? txn.getNote().trim() : "未备注";
				noteExpenseMap.put(noteKey, noteExpenseMap.getOrDefault(noteKey, BigDecimal.ZERO).add(amount));
			} else if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_INCOME)) {
				totalIncome = totalIncome.add(amount);
			}
		}
		if (expenseTxns.isEmpty()) {
			return replyService.success(result, "本月暂无支出流水，先记几笔账再来分析吧。");
		}

		List<Map.Entry<String, BigDecimal>> topNotes = new ArrayList<>(noteExpenseMap.entrySet());
		topNotes.sort((a, b) -> b.getValue().compareTo(a.getValue()));
		int topCount = Math.min(5, topNotes.size());
		Map<String, BigDecimal> topExpenseByNote = new LinkedHashMap<>();
		for (int i = 0; i < topCount; i++) {
			Map.Entry<String, BigDecimal> entry = topNotes.get(i);
			topExpenseByNote.put(entry.getKey(), entry.getValue());
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd");
		List<ImTxn> sortedExpense = new ArrayList<>(expenseTxns);
		sortedExpense.sort(Comparator.comparing(ImTxn::getAmount, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
		StringBuilder rawExpenseDetail = new StringBuilder();
		int detailCount = Math.min(20, sortedExpense.size());
		for (int i = 0; i < detailCount; i++) {
			ImTxn txn = sortedExpense.get(i);
			String day = txn.getTxnTime() == null ? "-" : dateFormat.format(txn.getTxnTime());
			rawExpenseDetail.append(day)
				.append(" ")
				.append(safe(txn.getNote()))
				.append(" ")
				.append(money(txn.getAmount()))
				.append("元\n");
		}

		StringBuilder topCategoryText = new StringBuilder();
		for (Map.Entry<String, BigDecimal> entry : topExpenseByNote.entrySet()) {
			topCategoryText.append("- ").append(entry.getKey()).append("：").append(money(entry.getValue())).append("元\n");
		}

		String prompt = "请基于以下记账数据做消费分析，给出中文结论。\n"
			+ "要求：\n"
			+ "1) 先给本月消费总览（不超过3行）\n"
			+ "2) 给出3条可执行建议（控制支出、优化结构、预算提醒）\n"
			+ "3) 语言简洁，适合微信消息阅读。\n\n"
			+ "统计范围：" + formatDateRangeText(range, false) + "\n"
			+ "收入总额：" + money(totalIncome) + "元\n"
			+ "支出总额：" + money(totalExpense) + "元\n"
			+ "支出笔数：" + expenseTxns.size() + "笔\n"
			+ "主要支出类别TOP5：\n" + topCategoryText
			+ "\n大额支出样本（最多20条）：\n" + rawExpenseDetail;
		String sysPrompt = "你是专业中文记账分析助手，只输出可读性强、可执行的结论。";
		String authKey = resolveZhipuAuthKey();
		if (!StringUtils.hasText(authKey)) {
			return replyService.failure(result, "AI_KEY_MISSING", "未配置智普AI Key，暂时无法使用消费分析。");
		}
		AiMessageResponse aiResp = aiChatService.chatCompletion(
			sysPrompt,
			prompt,
			EnumAiApiUrl.ZHIPU_CHAT_COMPLETIONS,
			zhipuModel,
			authKey
		);
		if (aiResp == null || aiResp.getSuccess() == null || !aiResp.getSuccess() || !StringUtils.hasText(aiResp.getContent())) {
			String reason = aiResp == null ? "调用失败" : safe(aiResp.getError());
			log.warn("[im-command] expense analysis ai failed, userId={}, scopeType={}, groupId={}, reason={}",
				user.getId(), scopeType, groupId, reason);
			return replyService.failure(result, "AI_ANALYSIS_FAIL", "消费分析暂时不可用，请稍后重试。");
		}
		StringBuilder reply = new StringBuilder();
		reply.append("📊 消费分析（").append(formatDateRangeText(range, false)).append("）\n");
		reply.append("💰 收入：").append(money(totalIncome)).append(" 元\n");
		reply.append("💸 支出：").append(money(totalExpense)).append(" 元\n\n");
		reply.append(aiResp.getContent().trim());
		operationLogService.recordOperation(user.getId(), scopeType, groupId, "CONSUMPTION_ANALYSIS", null, "消费分析", "OK", null);
		return replyService.success(result, reply.toString());
	}

	private String resolveZhipuAuthKey() {
		if (!StringUtils.hasText(zhipuApiKey)) {
			return "";
		}
		String key = zhipuApiKey.trim();
		return key.toLowerCase().startsWith("bearer ") ? key : "Bearer " + key;
	}

	private ImCommandExecuteResponse aiParse(ImCommandExecuteResponse result, ImUser user, ImUserContext context, String cmd) {
		List<String> parsedTxnCommands = parseTxnCommandsByAi(cmd);
		if (parsedTxnCommands.size() == 1) {
			String parsedTxnCommand = parsedTxnCommands.get(0);
			Integer inferredTxnType = inferTxnTypeByText(cmd);
			if (inferredTxnType != null) {
				if (inferredTxnType.equals(TXN_EXPENSE) && parsedTxnCommand.startsWith("+")) {
					parsedTxnCommand = "-" + parsedTxnCommand.substring(1);
				} else if (inferredTxnType.equals(TXN_INCOME) && parsedTxnCommand.startsWith("-")) {
					parsedTxnCommand = "+" + parsedTxnCommand.substring(1);
				}
			}
			boolean income = parsedTxnCommand.startsWith("+");
			result.setRoute(income ? EnumImCommandRoute.TXN_INCOME : EnumImCommandRoute.TXN_EXPENSE);
			operationLogService.recordOperation(user.getId(), normalizeScope(context), inSharedContext(context) ? context.getCurrentGroupId() : null,
				"AI_PARSE_MATCH", null, cmd, "OK", "parsed=" + parsedTxnCommand);
			return addTxn(result, user, context, parsedTxnCommand, cmd, income, false);
		}
		if (!parsedTxnCommands.isEmpty()) {
			int incomeCount = 0;
			int expenseCount = 0;
			BigDecimal incomeAmount = BigDecimal.ZERO;
			BigDecimal expenseAmount = BigDecimal.ZERO;
			StringBuilder details = new StringBuilder();
			List<String> segments = splitNaturalTxnSegments(cmd);
			for (String parsedCmd : parsedTxnCommands) {
				String adjustedCmd = parsedCmd;
				Integer inferredTxnType = inferTxnTypeByText(findBestMatchingSegment(parsedCmd, segments, cmd));
				if (inferredTxnType != null) {
					if (inferredTxnType.equals(TXN_EXPENSE) && adjustedCmd.startsWith("+")) {
						adjustedCmd = "-" + adjustedCmd.substring(1);
					} else if (inferredTxnType.equals(TXN_INCOME) && adjustedCmd.startsWith("-")) {
						adjustedCmd = "+" + adjustedCmd.substring(1);
					}
				}
				Matcher matcher = TXN_PATTERN.matcher(adjustedCmd);
				if (!matcher.matches()) {
					continue;
				}
				boolean income = "+".equals(matcher.group(1));
				BigDecimal amount = new BigDecimal(matcher.group(2)).setScale(2, RoundingMode.HALF_UP);
				String note = matcher.group(3) == null ? (income ? "收入" : "支出") : matcher.group(3).trim();
				addTxn(result, user, context, adjustedCmd, cmd, income, false);
				if (income) {
					incomeCount++;
					incomeAmount = incomeAmount.add(amount);
				} else {
					expenseCount++;
					expenseAmount = expenseAmount.add(amount);
				}
				details.append("\n").append(income ? "+" : "-")
					.append(money(amount)).append(" ").append(note);
			}
			if (incomeCount + expenseCount > 0) {
				result.setRoute(EnumImCommandRoute.TXN_LIST);
				operationLogService.recordOperation(user.getId(), normalizeScope(context), inSharedContext(context) ? context.getCurrentGroupId() : null,
					"AI_PARSE_BATCH", null, cmd, "OK", "count=" + (incomeCount + expenseCount));
				StringBuilder batchReply = new StringBuilder();
				batchReply.append("已为你批量记录 ").append(incomeCount + expenseCount).append(" 笔\n");
				batchReply.append("收入 ").append(incomeCount).append(" 笔，共 ").append(money(incomeAmount)).append(" 元\n");
				batchReply.append("支出 ").append(expenseCount).append(" 笔，共 ").append(money(expenseAmount)).append(" 元");
				batchReply.append(details);
				batchReply.append("\n\n如需继续分页查看，发送：列表#2");
				return replyService.success(result, batchReply.toString());
			}
		}
		ImAiParseLog parseLog = new ImAiParseLog();
		parseLog.setUserId(user.getId());
		parseLog.setScopeType(normalizeScope(context));
		parseLog.setGroupId(parseLog.getScopeType().equals(SCOPE_SHARED) ? context.getCurrentGroupId() : null);
		parseLog.setOriginText(cmd);
		parseLog.setConfirmStatus(0);
		parseLog.setCreateTime(new Date());
		aiParseLogService.save(parseLog);
		log.info("[im-command] ai parse log inserted, userId={}, aiParseLogId={}, text={}", user.getId(), parseLog.getId(), cmd);
		operationLogService.recordOperation(user.getId(), parseLog.getScopeType(), parseLog.getGroupId(), "AI_PARSE", null, cmd, "OK", "已记录为待解析");
		return replyService.success(result, "我还没识别出这条指令，已记录为待解析。\n发送“菜单”查看当前支持的命令。");
	}

	private List<String> parseTxnCommandsByAi(String text) {
		List<String> commands = new ArrayList<>();
		if (!StringUtils.hasText(text)) {
			return commands;
		}
		String authKey = resolveZhipuAuthKey();
		if (!StringUtils.hasText(authKey)) {
			return commands;
		}
		List<String> segments = splitNaturalTxnSegments(text);
		String sysPrompt = "你是记账指令解析器。只返回记账结果，不要解释。\n"
			+ "若能识别为记账，输出格式必须是：+金额 备注 或 -金额 备注（金额最多2位小数）。\n"
			+ "如果有多笔，按行输出多条，一行一条。\n"
			+ "必须逐笔拆分，不可合并多笔。\n"
			+ "示例输入：我今天赚了五十，吃饭花了二十\n"
			+ "示例输出：\n+50 今天赚了\n-20 吃饭\n"
			+ "若无法识别，输出：UNSUPPORTED";
		String userPrompt = "请将这句话转为记账命令：\n原文：" + text + "\n拆分参考：\n" + String.join("\n", segments);
		AiMessageResponse aiResp = aiChatService.chatCompletion(
			sysPrompt,
			userPrompt,
			EnumAiApiUrl.ZHIPU_CHAT_COMPLETIONS,
			zhipuModel,
			authKey
		);
		if (aiResp == null || aiResp.getSuccess() == null || !aiResp.getSuccess() || !StringUtils.hasText(aiResp.getContent())) {
			return commands;
		}
		String content = aiResp.getContent().trim();
		if (content.startsWith("```")) {
			content = content.replace("```", "").replace("text", "").trim();
		}
		String[] lines = content.split("\\r?\\n");
		for (String rawLine : lines) {
			String line = rawLine == null ? "" : rawLine.trim();
			if (!StringUtils.hasText(line) || "UNSUPPORTED".equalsIgnoreCase(line)) {
				continue;
			}
			if (TXN_PATTERN.matcher(line).matches()) {
				commands.add(line);
			}
		}
		return commands;
	}

	private Integer inferTxnTypeByText(String text) {
		if (!StringUtils.hasText(text)) {
			return null;
		}
		String lower = text.toLowerCase();
		for (String hint : EXPENSE_HINTS) {
			if (lower.contains(hint)) {
				return TXN_EXPENSE;
			}
		}
		for (String hint : INCOME_HINTS) {
			if (lower.contains(hint)) {
				return TXN_INCOME;
			}
		}
		return null;
	}

	private List<String> splitNaturalTxnSegments(String text) {
		List<String> segments = new ArrayList<>();
		if (!StringUtils.hasText(text)) {
			return segments;
		}
		String normalized = text.replace("，", "\n")
			.replace(",", "\n")
			.replace("。", "\n")
			.replace("；", "\n")
			.replace("然后", "\n")
			.replace("接着", "\n")
			.replace("再", "\n再")
			.replace("又", "\n又")
			.replace("晚上", "\n晚上")
			.replace("中午", "\n中午")
			.replace("早上", "\n早上");
		String[] rawParts = normalized.split("\\r?\\n");
		for (String rawPart : rawParts) {
			String part = rawPart == null ? "" : rawPart.trim();
			if (StringUtils.hasText(part)) {
				segments.add(part);
			}
		}
		if (segments.isEmpty()) {
			segments.add(text.trim());
		}
		return segments;
	}

	private String findBestMatchingSegment(String parsedCmd, List<String> segments, String fallback) {
		if (segments == null || segments.isEmpty() || !StringUtils.hasText(parsedCmd)) {
			return fallback;
		}
		Matcher matcher = TXN_PATTERN.matcher(parsedCmd);
		if (!matcher.matches()) {
			return fallback;
		}
		String note = matcher.group(3) == null ? "" : matcher.group(3).trim();
		if (!StringUtils.hasText(note)) {
			return fallback;
		}
		for (String segment : segments) {
			if (segment.contains(note) || note.contains(segment)) {
				return segment;
			}
		}
		return fallback;
	}

	private ImUser getOrCreateUser(String wxUserId) {
		return getOrCreateUser(wxUserId, wxUserId, null, null);
	}

	private ImUser getOrCreateUser(String wxUserId, String nickName, String botId, String botToken) {
		String normalizedBotId = limitLength(botId, BOT_ID_MAX_LENGTH, "botId", wxUserId);
		String normalizedBotToken = limitLength(botToken, BOT_TOKEN_MAX_LENGTH, "botToken", wxUserId);
		QueryWrapper<ImUser> wrapper = new QueryWrapper<>();
		wrapper.eq("wx_user_id", wxUserId);
		ImUser user = userService.getOne(wrapper);
		if (user != null) {
			boolean shouldUpdateBotInfo = false;
			if (StringUtils.hasText(normalizedBotId) && !normalizedBotId.equals(user.getBotId())) {
				user.setBotId(normalizedBotId);
				shouldUpdateBotInfo = true;
			}
			if (StringUtils.hasText(normalizedBotToken) && !normalizedBotToken.equals(user.getBotToken())) {
				user.setBotToken(normalizedBotToken);
				shouldUpdateBotInfo = true;
			}
			if (shouldRegenerateNickname(user.getNickName(), user.getWxUserId())) {
				user.setNickName(randomNickName());
				user.setUpdateTime(new Date());
				userService.updateById(user);
				log.info("[im-command] nickname regenerated, wxUserId={}, userId={}, nickName={}", wxUserId, user.getId(), user.getNickName());
			} else if (StringUtils.hasText(nickName) && !nickName.equals(wxUserId) && !nickName.equals(user.getNickName())) {
				user.setNickName(nickName);
				user.setUpdateTime(new Date());
				userService.updateById(user);
				log.info("[im-command] im_user updated, wxUserId={}, userId={}, nickName={}", wxUserId, user.getId(), nickName);
			} else if (shouldUpdateBotInfo) {
				user.setUpdateTime(new Date());
				userService.updateById(user);
				log.info("[im-command] im_user bot info updated, wxUserId={}, userId={}, botId={}, botTokenPresent={}",
					wxUserId, user.getId(), user.getBotId(), StringUtils.hasText(user.getBotToken()));
			}
			return user;
		}
		Date now = new Date();
		user = new ImUser();
		user.setWxUserId(wxUserId);
		user.setBotId(normalizedBotId);
		user.setBotToken(normalizedBotToken);
		user.setNickName(randomNickName());
		user.setStatus(1);
		user.setCreateTime(now);
		user.setUpdateTime(now);
		userService.save(user);
		log.info("[im-command] im_user inserted, wxUserId={}, userId={}, nickName={}, botId={}, botTokenPresent={}",
			wxUserId, user.getId(), user.getNickName(), user.getBotId(), StringUtils.hasText(user.getBotToken()));
		ImGroup defaultGroup = getOrCreateDefaultGroup(user);
		log.info("[im-command] default group ensured for new user, userId={}, groupId={}, groupName={}",
			user.getId(), defaultGroup.getId(), defaultGroup.getGroupName());
		return user;
	}

	private String limitLength(String value, int maxLength, String fieldName, String wxUserId) {
		if (!StringUtils.hasText(value)) {
			return value;
		}
		String trimmed = value.trim();
		if (trimmed.length() <= maxLength) {
			return trimmed;
		}
		log.warn("[im-command] {} too long and truncated, wxUserId={}, originalLength={}, maxLength={}",
			fieldName, wxUserId, trimmed.length(), maxLength);
		return trimmed.substring(0, maxLength);
	}

	private ImUserContext getOrCreateContext(Long userId) {
		QueryWrapper<ImUserContext> wrapper = new QueryWrapper<>();
		wrapper.eq("user_id", userId);
		ImUserContext context = userContextService.getOne(wrapper);
		if (context != null) {
			return context;
		}
		Date now = new Date();
		ImGroup defaultGroup = getOrCreateDefaultGroup(userService.getById(userId));
		context = new ImUserContext();
		context.setUserId(userId);
		context.setCurrentGroupId(null);
		context.setCreateTime(now);
		context.setUpdateTime(now);
		userContextService.save(context);
		log.info("[im-command] im_user_context inserted, userId={}, scopeType={}, groupId={}", userId, resolveContextScope(context), context.getCurrentGroupId());
		return context;
	}

	private String menuText(ImUser user, ImUserContext context) {
		String nickName = user == null ? "朋友" : displayNickname(user);
		return "我是iMoney你的专属会计\n\n"
			+ "| 👋你好，" + nickName + " | ㅤ |\n"
			+ "| :--- | :--- |\n"
			+ "| ⚡快速记账 | ㅤ |\n"
			+ "| +金额 备注 | -金额 备注 |\n"
			+ "| 修改昵称 新昵称 | ㅤ |\n"
			+ "| 📅账单查询 | ㅤ |\n"
			+ "| 账单 | 账单记录 |\n"
			+ "| 预算提醒 | 消费分析 |\n"
			+ "| 导出Excel | ㅤ |\n"
			+ "| 👫一起记账 | ㅤ |\n"
			+ "| 邀请记账 | 加入 邀请码 |\n"
			+ "| 成员 | 退出 |\n"
			+ "\n"
			+ "> 发送“帮助”可查看功能详细说明";
	}

	private String helpText(ImUser user, ImUserContext context) {
		String nickName = user == null ? "朋友" : displayNickname(user);
		return "# iMoney 帮助\n\n"
			+ "你好，**" + nickName + "** 👋\n\n"
			+ "🧾 记账相关\n"
			+ "- `+金额 备注`：新增收入。示例：`+50 早餐退款`\n"
			+ "- `-金额 备注`：新增支出。示例：`-20 午饭`\n"
			+ "- `-金额 备注 昨天`：补记历史账单，支持 `今天/昨天/前天/2026-04-28`\n"
			+ "- `撤销`：撤销最近一笔记账\n\n"
			+ "📊 账单查询\n"
			+ "- `账单`：今日收支汇总\n"
			+ "- `账单#周` / `账单#月` / `账单#年`：周期汇总\n"
			+ "- `账单记录`：按天统计（可翻页）\n"
			+ "- `账单记录#2`：按天统计第 2 页\n"
			+ "- `账单记录1`：按序号查看某天详情\n"
			+ "- `账单记录 2026-04-28`：按日期查看当天详情\n"
			+ "- `列表`：逐笔流水明细\n"
			+ "- `列表#2`：逐笔流水第 2 页\n\n"
			+ "🤖 智能能力\n"
			+ "- `消费分析`：AI 分析本月消费结构并给出建议\n"
			+ "- 自然语言记账：如“我今天赚了50，吃饭花了20”可自动拆分多笔\n\n"
			+ "💡 预算功能\n"
			+ "- `预算 日100` / `预算 月3000` / `预算 年10000`：设置预算\n"
			+ "- `预算提醒`（或 `本月预算` / `预算剩余`）：查看预算剩余与预警\n"
			+ "- `关闭预算 日`（或 `月/年`）：关闭对应周期预算\n\n"
			+ "👥 共享记账\n"
			+ "- `邀请记账`：生成邀请码\n"
			+ "- `加入记账 邀请码`：加入共享账本\n"
			+ "- `成员`：查看成员列表\n"
			+ "- `修改备注 成员名称 新备注`：给成员设置备注\n"
			+ "- `退出`：退出共享账本\n\n"
			+ "⚙️ 其它\n"
			+ "- `导出Excel`：导出当前账本数据\n";
	}

	private DateRange parseDateRange(String cmd) {
		String normalizedCmd = cmd == null ? "" : cmd.trim();
		Calendar start = Calendar.getInstance();
		Calendar end = Calendar.getInstance();
		if ("日统计".equals(normalizedCmd) || "今日统计".equals(normalizedCmd)) {
			start.set(Calendar.HOUR_OF_DAY, 0);
			start.set(Calendar.MINUTE, 0);
			start.set(Calendar.SECOND, 0);
			start.set(Calendar.MILLISECOND, 0);
			end.setTime(start.getTime());
			end.add(Calendar.DAY_OF_MONTH, 1);
			return new DateRange(start.getTime(), end.getTime(), new SimpleDateFormat("yyyy-MM-dd").format(start.getTime()));
		}
		if ("周统计".equals(normalizedCmd) || "本周统计".equals(normalizedCmd)) {
			start.setFirstDayOfWeek(Calendar.MONDAY);
			start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			start.set(Calendar.HOUR_OF_DAY, 0);
			start.set(Calendar.MINUTE, 0);
			start.set(Calendar.SECOND, 0);
			start.set(Calendar.MILLISECOND, 0);
			end.setTime(start.getTime());
			end.add(Calendar.DAY_OF_MONTH, 7);
			return new DateRange(start.getTime(), end.getTime(), "本周");
		}
		if ("年统计".equals(normalizedCmd) || "本年统计".equals(normalizedCmd)) {
			start.set(Calendar.DAY_OF_YEAR, 1);
			start.set(Calendar.HOUR_OF_DAY, 0);
			start.set(Calendar.MINUTE, 0);
			start.set(Calendar.SECOND, 0);
			start.set(Calendar.MILLISECOND, 0);
			end.setTime(start.getTime());
			end.add(Calendar.YEAR, 1);
			return new DateRange(start.getTime(), end.getTime(), String.valueOf(start.get(Calendar.YEAR)));
		}
		Matcher monthMatcher = Pattern.compile("#(\\d{4}[-/]\\d{1,2})").matcher(normalizedCmd);
		if (monthMatcher.find()) {
			Matcher m = MONTH_PATTERN.matcher(monthMatcher.group(1));
			if (m.matches()) {
				start.clear();
				start.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, 1, 0, 0, 0);
				end.setTime(start.getTime());
				end.add(Calendar.MONTH, 1);
				return new DateRange(start.getTime(), end.getTime(), m.group(1) + "-" + pad2(m.group(2)));
			}
		}
		start.set(Calendar.DAY_OF_MONTH, 1);
		start.set(Calendar.HOUR_OF_DAY, 0);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		end.setTime(start.getTime());
		end.add(Calendar.MONTH, 1);
		return new DateRange(start.getTime(), end.getTime(), new SimpleDateFormat("yyyy-MM").format(start.getTime()));
	}

	private StatsSummary queryStatsSummary(ImUser user, Integer scopeType, Long groupId, DateRange range) {
		QueryWrapper<ImTxn> wrapper = new QueryWrapper<>();
		wrapper.eq("scope_type", scopeType);
		if (scopeType.equals(SCOPE_PRIVATE)) {
			wrapper.eq("user_id", user.getId());
			wrapper.isNull("group_id");
		} else {
			wrapper.eq("group_id", groupId);
		}
		wrapper.eq("is_deleted", 0);
		wrapper.ge("txn_time", range.start);
		wrapper.lt("txn_time", range.end);
		List<ImTxn> txns = txnService.list(wrapper);
		StatsSummary summary = new StatsSummary();
		summary.txnCount = txns.size();
		for (ImTxn txn : txns) {
			BigDecimal amount = txn.getAmount() == null ? BigDecimal.ZERO : txn.getAmount();
			if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_INCOME)) {
				summary.income = summary.income.add(amount);
				summary.incomeCount++;
			} else if (txn.getTxnType() != null && txn.getTxnType().equals(TXN_EXPENSE)) {
				summary.expense = summary.expense.add(amount);
				summary.expenseCount++;
			}
		}
		return summary;
	}

	private boolean isStatsCommand(String cmd) {
		return "账单".equals(cmd) || cmd.startsWith("账单#") || "统计".equals(cmd);
	}

	private Integer resolveStatsScopeType(String cmd, boolean sharedCommand) {
		if ("#统计".equals(cmd) || cmd.startsWith("#统计#")) {
			return SCOPE_SHARED;
		}
		return resolveScopeType(sharedCommand);
	}

	private String normalizeCommand(String command) {
		if (command == null) {
			return "";
		}
		String text = command.trim();
		if (!StringUtils.hasText(text)) {
			return "";
		}
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '\u3000' || Character.isWhitespace(ch)) {
				sb.append(' ');
				continue;
			}
			if (ch >= '\uFF01' && ch <= '\uFF5E') {
				ch = (char) (ch - 0xFEE0);
			}
			if (ch == '\u2212' || ch == '\u2013' || ch == '\u2014' || ch == '\u2015' || ch == '\uFE63') {
				ch = '-';
			}
			sb.append(ch);
		}
		return sb.toString().trim().replaceAll("\\s+", " ");
	}

	private CommandMeta parseCommandMeta(String rawCommand) {
		String normalized = normalizeCommand(rawCommand);
		boolean shared = false;
		String cmd = normalized;
		if (StringUtils.hasText(cmd) && cmd.endsWith("@公")) {
			shared = true;
			cmd = normalizeCommand(cmd.substring(0, cmd.length() - 2));
		}
		return new CommandMeta(cmd, shared);
	}

	private Integer resolveScopeType(boolean sharedCommand) {
		return sharedCommand ? SCOPE_SHARED : SCOPE_PRIVATE;
	}

	private ImGroup getOrCreateDefaultGroup(ImUser user) {
		if (user == null || user.getId() == null) {
			return null;
		}
		QueryWrapper<ImGroup> wrapper = new QueryWrapper<>();
		wrapper.eq("owner_user_id", user.getId());
		wrapper.eq("status", 1);
		wrapper.orderByAsc("create_time");
		List<ImGroup> groups = groupService.list(wrapper);
		if (groups != null && !groups.isEmpty()) {
			return groups.get(0);
		}
		ImGroup group = groupService.createGroup(buildDefaultGroupName(user), user.getId());
		log.info("[im-command] default group created, userId={}, groupId={}, groupName={}",
			user.getId(), group.getId(), group.getGroupName());
		return group;
	}

	private void activateDefaultGroupIfNeeded(ImUserContext context, ImGroup defaultGroup) {
		if (context == null || defaultGroup == null) {
			return;
		}
		boolean needActivate = context.getCurrentGroupId() == null;
		if (!needActivate) {
			ImGroup currentGroup = groupService.getById(context.getCurrentGroupId());
			needActivate = currentGroup == null
				|| currentGroup.getStatus() == null
				|| currentGroup.getStatus() != 1
				|| groupMemberService.getMemberRole(currentGroup.getId(), context.getUserId()) == null;
		}
		if (!needActivate) {
			return;
		}
		context.setCurrentGroupId(defaultGroup.getId());
		context.setUpdateTime(new Date());
		userContextService.updateById(context);
		log.info("[im-command] shared slot switched to default group, userId={}, groupId={}, groupName={}",
			context.getUserId(), defaultGroup.getId(), defaultGroup.getGroupName());
	}

	private void switchToSharedContext(ImUserContext context, Long groupId) {
		setSharedSlot(context, groupId);
	}

	private void setSharedSlot(ImUserContext context, Long groupId) {
		context.setCurrentGroupId(groupId);
		context.setUpdateTime(new Date());
		userContextService.updateById(context);
	}

	private Integer resolveContextScope(ImUserContext context) {
		return context != null && context.getCurrentGroupId() != null ? SCOPE_SHARED : SCOPE_PRIVATE;
	}

	private Integer normalizeScope(ImUserContext context) {
		return resolveContextScope(context);
	}

	private boolean inSharedContext(ImUserContext context) {
		return resolveContextScope(context).equals(SCOPE_SHARED);
	}

	private ImGroup ensureSharedSlot(ImUser user, ImUserContext context) {
		ImGroup defaultGroup = getOrCreateDefaultGroup(user);
		activateDefaultGroupIfNeeded(context, defaultGroup);
		return context.getCurrentGroupId() == null ? defaultGroup : groupService.getById(context.getCurrentGroupId());
	}

	private void deactivateOtherJoinedGroups(ImUser user, Long activeGroupId) {
		QueryWrapper<ImGroupMember> wrapper = new QueryWrapper<>();
		wrapper.eq("user_id", user.getId());
		wrapper.eq("status", 1);
		List<ImGroupMember> members = groupMemberService.list(wrapper);
		Date now = new Date();
		for (ImGroupMember member : members) {
			if (member.getGroupId() == null || member.getGroupId().equals(activeGroupId)) {
				continue;
			}
			ImGroup group = groupService.getById(member.getGroupId());
			if (group != null && group.getOwnerUserId() != null && group.getOwnerUserId().equals(user.getId())) {
				continue;
			}
			member.setStatus(0);
			member.setLeftAt(now);
			groupMemberService.updateById(member);
			log.info("[im-command] old joined shared group deactivated, userId={}, groupId={}, activeGroupId={}",
				user.getId(), member.getGroupId(), activeGroupId);
		}
	}

	private String buildDefaultGroupName(ImUser user) {
		String baseName = user == null ? null : user.getNickName();
		if (!StringUtils.hasText(baseName) && user != null && StringUtils.hasText(user.getWxUserId())) {
			baseName = user.getWxUserId();
		}
		if (!StringUtils.hasText(baseName) && user != null && user.getId() != null) {
			baseName = "用户" + user.getId();
		}
		if (!StringUtils.hasText(baseName)) {
			baseName = "iMoney";
		}
		if (baseName.length() > 40) {
			baseName = baseName.substring(0, 40);
		}
		return baseName + DEFAULT_GROUP_SUFFIX;
	}

	private String randomNickName() {
		String adjective = NICK_ADJECTIVES[RANDOM.nextInt(NICK_ADJECTIVES.length)];
		String noun = NICK_NOUNS[RANDOM.nextInt(NICK_NOUNS.length)];
		return adjective + "的" + noun;
	}




	private boolean canManageGroup(Long groupId, Long userId) {
		if (groupId == null) {
			return false;
		}
		ImGroup group = groupService.getById(groupId);
		return group != null && group.getOwnerUserId() != null && group.getOwnerUserId().equals(userId);
	}

	private String displayName(Long groupId, Long ownerUserId, Long targetUserId, ImUser memberUser) {
		QueryWrapper<ImMemberAlias> wrapper = new QueryWrapper<>();
		wrapper.eq("group_id", groupId);
		wrapper.eq("owner_user_id", ownerUserId);
		wrapper.eq("target_user_id", targetUserId);
		ImMemberAlias alias = memberAliasService.getOne(wrapper);
		if (alias != null && StringUtils.hasText(alias.getAliasName())) {
			return alias.getAliasName();
		}
		return memberUser == null ? "用户" + targetUserId : displayNickname(memberUser);
	}

	private String displayNickname(ImUser user) {
		if (user == null) {
			return "朋友";
		}
		if (shouldRegenerateNickname(user.getNickName(), user.getWxUserId())) {
			user.setNickName(randomNickName());
			user.setUpdateTime(new Date());
			userService.updateById(user);
		}
		return safe(user.getNickName());
	}

	private boolean shouldRegenerateNickname(String nickname, String wxUserId) {
		if (!StringUtils.hasText(nickname)) {
			return true;
		}
		String lower = nickname.toLowerCase();
		if (lower.contains("@im.wechat")) {
			return true;
		}
		return StringUtils.hasText(wxUserId) && nickname.equals(wxUserId);
	}

	private String nextOpId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private String money(BigDecimal value) {
		return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private String safe(String value) {
		return StringUtils.hasText(value) ? value : "-";
	}

	private String pad2(String value) {
		return value.length() == 1 ? "0" + value : value;
	}

	private Long parseUserId(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return Long.valueOf(value.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private byte[] buildExcel(List<ImTxn> txns) throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Workbook workbook = new XSSFWorkbook();
		try {
			Sheet sheet = workbook.createSheet("账单");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("操作ID");
			header.createCell(1).setCellValue("时间");
			header.createCell(2).setCellValue("类型");
			header.createCell(3).setCellValue("金额");
			header.createCell(4).setCellValue("备注");
			header.createCell(5).setCellValue("原始消息");
			for (int i = 0; i < txns.size(); i++) {
				ImTxn txn = txns.get(i);
				Row row = sheet.createRow(i + 1);
				row.createCell(0).setCellValue(safe(txn.getOpId()));
				row.createCell(1).setCellValue(txn.getTxnTime() == null ? "" : format.format(txn.getTxnTime()));
				row.createCell(2).setCellValue(txn.getTxnType() != null && txn.getTxnType().equals(TXN_INCOME) ? "收入" : "支出");
				row.createCell(3).setCellValue(txn.getAmount() == null ? 0D : txn.getAmount().doubleValue());
				row.createCell(4).setCellValue(safe(txn.getNote()));
				row.createCell(5).setCellValue(safe(txn.getSourceText()));
			}
			for (int i = 0; i < 6; i++) {
				sheet.autoSizeColumn(i);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			return out.toByteArray();
		} finally {
			workbook.close();
		}
	}

	private static class DateRange {
		private final Date start;
		private final Date end;
		private final String label;

		private DateRange(Date start, Date end, String label) {
			this.start = start;
			this.end = end;
			this.label = label;
		}
	}

	private static class CommandMeta {
		private final String command;
		private final boolean shared;

		private CommandMeta(String command, boolean shared) {
			this.command = command;
			this.shared = shared;
		}
	}

	private static class StatsSummary {
		private BigDecimal income = BigDecimal.ZERO;
		private BigDecimal expense = BigDecimal.ZERO;
		private int incomeCount;
		private int expenseCount;
		private int txnCount;
	}

	private static class DayTxnSummary {
		private final String dayLabel;
		private BigDecimal income = BigDecimal.ZERO;
		private BigDecimal expense = BigDecimal.ZERO;
		private int txnCount;

		private DayTxnSummary(String dayLabel) {
			this.dayLabel = dayLabel;
		}
	}

	private static class TxnDateParseResult {
		private final Date txnTime;
		private final String note;

		private TxnDateParseResult(Date txnTime, String note) {
			this.txnTime = txnTime;
			this.note = note;
		}
	}
}

