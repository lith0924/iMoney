package org.springblade.modules.business.imilink.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.exception.SessionExpiredException;
import com.github.wechat.ilink.sdk.core.listener.OnLoginListener;
import com.github.wechat.ilink.sdk.core.listener.OnMessageListener;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.modules.business.imcommand.service.IImCommandService;
import org.springblade.modules.business.imcommand.dto.ImCommandExecuteResponse;
import org.springblade.modules.business.imuser.entity.ImUser;
import org.springblade.modules.business.imuser.service.IImUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IlinkBotService {

	private static final Logger log = LoggerFactory.getLogger(IlinkBotService.class);
	private static final String DEFAULT_CLIENT_ID = "default";
	private static final AtomicLong CLIENT_SEQ = new AtomicLong();

	private final IImCommandService commandService;
	private final IImUserService userService;
	private final ConcurrentMap<String, ClientSession> sessions = new ConcurrentHashMap<>();

	@Value("${imoney.ilink.poll-delay-ms:1000}")
	private long pollDelayMs;
	@Value("${imoney.ilink.connect-timeout-ms:35000}")
	private long connectTimeoutMs;
	@Value("${imoney.ilink.read-timeout-ms:35000}")
	private long readTimeoutMs;
	@Value("${imoney.ilink.write-timeout-ms:35000}")
	private long writeTimeoutMs;
	@Value("${imoney.ilink.http-max-retries:3}")
	private int httpMaxRetries;
	@Value("${imoney.ilink.retry-base-delay-ms:1000}")
	private long retryBaseDelayMs;
	@Value("${imoney.ilink.retry-max-delay-ms:10000}")
	private long retryMaxDelayMs;
	@Value("${imoney.ilink.heartbeat-enabled:true}")
	private boolean heartbeatEnabled;
	@Value("${imoney.ilink.heartbeat-interval-ms:30000}")
	private long heartbeatIntervalMs;
	@Value("${imoney.ilink.channel-version:1.0.0}")
	private String channelVersion;
	@Value("${imoney.ilink.login-wait-seconds:60}")
	private long loginWaitSeconds;
	@Value("${imoney.ilink.restore-enabled:true}")
	private boolean restoreEnabled;
	@Value("${imoney.ilink.default-base-url:https://ilinkai.weixin.qq.com}")
	private String defaultBaseUrl;

	public IlinkBotService(IImCommandService commandService, IImUserService userService) {
		this.commandService = commandService;
		this.userService = userService;
	}

	@PostConstruct
	public void restoreClientsOnStartup() {
		if (!restoreEnabled) {
			log.info("[ilink] restore on startup disabled by config");
			return;
		}
		try {
			List<ImUser> users = userService.list();
			if (users == null || users.isEmpty()) {
				log.info("[ilink] no users found for restore");
				return;
			}
			int restored = 0;
			for (ImUser user : users) {
				if (user == null
					|| user.getId() == null
					|| !StringUtils.hasText(user.getWxUserId())
					|| !StringUtils.hasText(user.getBotId())
					|| !StringUtils.hasText(user.getBotToken())) {
					continue;
				}
				String clientId = restoredClientId(user);
				ClientSession session = getOrCreateSession(clientId);
				synchronized (session.lock) {
					try {
						closeSession(session);
						LoginContext context = new LoginContext(
							user.getBotToken().trim(),
							user.getWxUserId().trim(),
							user.getBotId().trim(),
							defaultBaseUrl
						);
						ILinkClient restoredClient = buildClient(session, context);
						session.client = restoredClient;
						session.lastQrCode = null;
						session.lastError = null;
						startPollingLoop(session, restoredClient);
						restored++;
						log.info("[ilink] client restored from db, clientId={}, userId={}, botId={}",
							session.clientId, user.getWxUserId(), user.getBotId());
					} catch (Exception ex) {
						session.lastError = ex.getMessage();
						log.error("[ilink] restore client failed, clientId={}, userId={}, botId={}, reason={}",
							clientId, user.getWxUserId(), user.getBotId(), ex.getMessage(), ex);
					}
				}
			}
			log.info("[ilink] restore on startup completed, totalUsers={}, restored={}", users.size(), restored);
		} catch (Exception ex) {
			log.error("[ilink] restore on startup aborted (ilink 接口仍可用): {}", ex.toString(), ex);
		}
	}

	public Map<String, Object> startLogin() {
		return startLogin(null);
	}

	public Map<String, Object> startLogin(String clientId) {
		boolean explicitClientId = StringUtils.hasText(clientId);
		ClientSession session = explicitClientId ? getOrCreateSession(clientId) : createNewSession();
		synchronized (session.lock) {
			Map<String, Object> data = new HashMap<>(10);
			ILinkClient current = session.client;
			if (explicitClientId && current != null && current.isLoggedIn()) {
				recordLoggedInUser(session.clientId, current.getLoginContext());
				startPollingLoop(session, current);
				log.info("[ilink] startLogin reused logged-in client, clientId={}, status={}", session.clientId, current.getConnectionStatus());
				data.putAll(status(session));
				data.put("reused", true);
				data.put("message", "ilink 客户端已登录，轮询会按 SDK 登录状态自动进行。");
				return data;
			}
			if (explicitClientId && current != null && current.getLoginFuture() != null && !current.getLoginFuture().isDone()) {
				log.info("[ilink] startLogin reused pending login, clientId={}, status={}", session.clientId, current.getConnectionStatus());
				data.putAll(status(session));
				data.put("reused", true);
				data.put("qrCode", session.lastQrCode);
				data.put("message", "该客户端已有登录流程进行中，请继续扫码。");
				return data;
			}

			closeSession(session);
			log.info("[ilink] creating client and starting QR login, clientId={}", session.clientId);
			ILinkClient nextClient = buildClient(session, null);
			session.client = nextClient;
			session.lastQrCode = nextClient.executeLogin();
			session.lastError = null;
			log.info("[ilink] QR login started, clientId={}, qrCodeLength={}", session.clientId, session.lastQrCode == null ? 0 : session.lastQrCode.length());
			data.put("clientId", session.clientId);
			data.put("started", true);
			data.put("loggedIn", false);
			data.put("qrCode", session.lastQrCode);
			data.put("message", "请使用微信扫码登录；登录成功后该客户端会自动开始拉取消息。");
			return data;
		}
	}

	public Map<String, Object> waitLogin() {
		return waitLogin(DEFAULT_CLIENT_ID);
	}

	public Map<String, Object> waitLogin(String clientId) {
		ClientSession session = getSession(clientId);
		if (session == null) {
			return missingSession(clientId);
		}
		ILinkClient current = session.client;
		Map<String, Object> status = new HashMap<>(10);
		status.put("clientId", session.clientId);
		if (current == null || current.getLoginFuture() == null) {
			status.put("loggedIn", false);
			status.put("message", "尚未开始登录，请先调用 /login/start 获取二维码。");
			return status;
		}
		try {
			LoginContext context = current.getLoginFuture().get(loginWaitSeconds, TimeUnit.SECONDS);
			status.put("loggedIn", context != null);
			if (context != null) {
				recordLoggedInUser(session.clientId, context);
				startPollingLoop(session, current);
				log.info("[ilink] waitLogin success, clientId={}, botId={}, userId={}, baseUrl={}",
					session.clientId, context.getBotId(), context.getUserId(), context.getBaseUrl());
				status.put("botId", context.getBotId());
				status.put("userId", context.getUserId());
				status.put("baseUrl", context.getBaseUrl());
			}
			return status;
		} catch (Exception ex) {
			session.lastError = ex.getMessage();
			log.error("[ilink] waitLogin failed, clientId={}", session.clientId, ex);
			status.put("loggedIn", false);
			status.put("message", ex.getMessage());
			return status;
		}
	}

	public Map<String, Object> status() {
		return statusAll();
	}

	public Map<String, Object> status(String clientId) {
		if (StringUtils.hasText(clientId)) {
			ClientSession session = getSession(clientId);
			return session == null ? missingSession(clientId) : status(session);
		}
		return statusAll();
	}

	private Map<String, Object> statusAll() {
		Map<String, Object> status = new HashMap<>(8);
		List<Map<String, Object>> sessionStatuses = new ArrayList<>();
		for (ClientSession session : sessions.values()) {
			sessionStatuses.add(status(session));
		}
		status.put("clientCount", sessions.size());
		status.put("sessions", sessionStatuses);
		status.put("message", sessions.isEmpty() ? "暂无 ilink 客户端会话。" : "ok");
		return status;
	}

	public Map<String, Object> pollOnce() {
		return pollOnce(DEFAULT_CLIENT_ID);
	}

	public Map<String, Object> pollOnce(String clientId) {
		ClientSession session = getSession(clientId);
		if (session == null) {
			return missingSession(clientId);
		}
		return pollOnce(session);
	}

	public Map<String, Object> stop(String clientId) {
		ClientSession session = getSession(clientId);
		if (session == null) {
			return missingSession(clientId);
		}
		closeSession(session);
		sessions.remove(session.clientId);
		Map<String, Object> result = new HashMap<>(4);
		result.put("clientId", session.clientId);
		result.put("success", true);
		result.put("message", "ilink 客户端已停止。");
		log.info("[ilink] client stopped, clientId={}", session.clientId);
		return result;
	}

	public void stop() {
		stop(DEFAULT_CLIENT_ID);
	}

	public Map<String, Object> stopAll() {
		List<String> clientIds = new ArrayList<>(sessions.keySet());
		for (String clientId : clientIds) {
			ClientSession session = sessions.get(clientId);
			if (session != null) {
				closeSession(session);
				sessions.remove(clientId);
			}
		}
		Map<String, Object> result = new HashMap<>(4);
		result.put("success", true);
		result.put("stoppedCount", clientIds.size());
		result.put("clientIds", clientIds);
		log.info("[ilink] all clients stopped, count={}", clientIds.size());
		return result;
	}

	@PreDestroy
	public void destroy() {
		stopAll();
	}

	private ILinkClient buildClient(ClientSession session, LoginContext loginContext) {
		ILinkConfig config = ILinkConfig.builder()
			.connectTimeoutMs(connectTimeoutMs)
			.readTimeoutMs(readTimeoutMs)
			.writeTimeoutMs(writeTimeoutMs)
			.httpMaxRetries(httpMaxRetries)
			.retryBaseDelayMs(retryBaseDelayMs)
			.retryMaxDelayMs(retryMaxDelayMs)
			.heartbeatEnabled(heartbeatEnabled)
			.heartbeatIntervalMs(heartbeatIntervalMs)
			.channelVersion(channelVersion)
			.build();

		final ILinkClient[] holder = new ILinkClient[1];
		ILinkClient builtClient = ILinkClient.builder()
			.config(config)
			.loginContext(loginContext)
			.onLogin(new OnLoginListener() {
				@Override
				public void onLoginSuccess(LoginContext context) {
					log.info("[ilink] login success, clientId={}, botId={}, userId={}, baseUrl={}",
						session.clientId, context.getBotId(), context.getUserId(), context.getBaseUrl());
					recordLoggedInUser(session.clientId, context);
					startPollingLoop(session, holder[0]);
				}

				@Override
				public void onLoginFailure(Throwable throwable) {
					session.lastError = throwable.getMessage();
					log.error("[ilink] login failed, clientId={}", session.clientId, throwable);
				}
			})
			.onMessage(new OnMessageListener() {
				@Override
				public void onMessages(List<WeixinMessage> messages) {
					handleMessages(session, holder[0], messages);
				}
			})
			.build();
		holder[0] = builtClient;
		return builtClient;
	}

	private String restoredClientId(ImUser user) {
		return "db-user-" + user.getId();
	}

	private Map<String, Object> pollOnce(ClientSession session) {
		Map<String, Object> result = new HashMap<>(6);
		result.put("clientId", session.clientId);
		ILinkClient current = session.client;
		if (current == null || !isClientReady(current)) {
			log.debug("[ilink] poll skipped, clientId={}, clientExists={}, loggedIn={}",
				session.clientId, current != null, current != null && isClientReady(current));
			result.put("success", false);
			result.put("message", "ilink 客户端未登录。");
			return result;
		}
		try {
			log.debug("[ilink] poll start, clientId={}, connectionStatus={}, loginStatus={}",
				session.clientId, current.getConnectionStatus(), current.getLoginStatus().getStatus());
			List<WeixinMessage> messages = current.getUpdates();
			result.put("success", true);
			result.put("count", messages == null ? 0 : messages.size());
			log.info("[ilink] poll done, clientId={}, messageCount={}", session.clientId, messages == null ? 0 : messages.size());
			return result;
		} catch (SessionExpiredException ex) {
			session.lastError = ex.getMessage();
			log.warn("[ilink] poll session expired, clientId={}, stopping session and waiting relogin", session.clientId);
			result.put("success", false);
			result.put("message", "session expired，需要重新登录。");
			closeSession(session);
			return result;
		} catch (Exception ex) {
			session.lastError = ex.getMessage();
			log.error("[ilink] poll failed, clientId={}", session.clientId, ex);
			result.put("success", false);
			result.put("message", ex.getMessage());
			return result;
		}
	}

	private void recordLoggedInUser(String clientId, LoginContext context) {
		if (context == null || !StringUtils.hasText(context.getUserId())) {
			log.warn("[ilink] login context has no userId, skip user persistence, clientId={}", clientId);
			return;
		}
		try {
			String botToken = resolveBotToken(context);
			log.info("[ilink] persisting login user start, clientId={}, userId={}, botId={}, botTokenPresent={}",
				clientId, context.getUserId(), context.getBotId(), StringUtils.hasText(botToken));
			ImCommandExecuteResponse result = commandService.ensureUser(context.getUserId(), context.getUserId(), context.getBotId(), botToken);
			log.info("[ilink] login user persisted, clientId={}, userId={}, botId={}, botTokenPresent={}, result={}",
				clientId, context.getUserId(), context.getBotId(), StringUtils.hasText(botToken), result);
		} catch (Exception ex) {
			log.error("[ilink] login user persistence failed, clientId={}, userId={}, botId={}, reason={}",
				clientId, context.getUserId(), context.getBotId(), ex.getMessage(), ex);
		}
	}

	private String resolveBotToken(LoginContext context) {
		if (context == null) {
			return null;
		}
		String[] methodNames = new String[]{"getBotToken", "getToken"};
		for (String methodName : methodNames) {
			try {
				Method method = context.getClass().getMethod(methodName);
				Object value = method.invoke(context);
				if (value instanceof String && StringUtils.hasText((String) value)) {
					return (String) value;
				}
			} catch (Exception ex) {
				log.debug("[ilink] login context does not expose method {}, clientClass={}", methodName, context.getClass().getName());
			}
		}
		return null;
	}

	private void handleMessages(ClientSession session, ILinkClient current, List<WeixinMessage> messages) {
		if (messages == null || messages.isEmpty()) {
			log.debug("[ilink] onMessage called with empty messages, clientId={}", session.clientId);
			return;
		}
		if (current == null) {
			log.warn("[ilink] onMessage ignored because client is null, clientId={}, messageCount={}", session.clientId, messages.size());
			return;
		}
		if (session.client != current) {
			log.warn("[ilink] onMessage ignored because client instance is stale, clientId={}, messageCount={}", session.clientId, messages.size());
			return;
		}
		LoginContext loginContext = current.getLoginContext();
		log.info("[ilink] onMessage received, clientId={}, messageCount={}, botUserId={}",
			session.clientId, messages.size(), loginContext == null ? null : loginContext.getUserId());
		for (WeixinMessage message : messages) {
			if (message == null || !StringUtils.hasText(message.getFrom_user_id())) {
				log.warn("[ilink] message skipped because from_user_id is empty, clientId={}, message={}", session.clientId, message);
				continue;
			}
			String commandUserId = message.getFrom_user_id();
			log.info("[ilink] handling message, clientId={}, messageId={}, fromUserId={}, toUserId={}, commandUserId={}, itemCount={}, contextTokenPresent={}",
				session.clientId,
				message.getMessage_id(),
				message.getFrom_user_id(),
				message.getTo_user_id(),
				commandUserId,
				message.getItem_list() == null ? 0 : message.getItem_list().size(),
				StringUtils.hasText(message.getContext_token()));
			log.info("[ilink] raw message, clientId={}, message={}", session.clientId, JSON.toJSONString(message));
			try {
				handleTextItems(session, current, message, commandUserId);
			} catch (Exception ex) {
				log.error("[ilink] handleTextItems crashed, clientId={}, messageId={}, fromUserId={}",
					session.clientId, message == null ? null : message.getMessage_id(), commandUserId, ex);
			}
		}
	}

	/**
	 * 从条目中取出与「发文字」等价的指令串：text_item，或语音的识别结果 voice_item.text。
	 */
	private String resolveCommandTextFromItem(MessageItem item) {
		if (item == null) {
			return null;
		}
		if (item.getText_item() != null && StringUtils.hasText(item.getText_item().getText())) {
			return item.getText_item().getText().trim();
		}
		if (item.getVoice_item() != null && StringUtils.hasText(item.getVoice_item().getText())) {
			return item.getVoice_item().getText().trim();
		}
		return null;
	}

	/**
	 * 与 {@link #resolveCommandTextFromItem} 相同语义，兼容 item 为 Map / 字段名与模型略有差异的 JSON。
	 */
	private String resolveCommandTextFromJsonFallback(Object rawItem) {
		if (rawItem == null) {
			return null;
		}
		try {
			JSONObject o = JSON.parseObject(JSON.toJSONString(rawItem));
			JSONObject vi = o.getJSONObject("voice_item");
			if (vi != null) {
				for (String key : new String[]{"text", "transcript", "transcription", "asr_text", "voice_text"}) {
					String v = vi.getString(key);
					if (StringUtils.hasText(v)) {
						return v.trim();
					}
				}
			}
			JSONObject ti = o.getJSONObject("text_item");
			if (ti != null) {
				String v = ti.getString("text");
				if (StringUtils.hasText(v)) {
					return v.trim();
				}
			}
		} catch (Exception ex) {
			log.warn("[ilink] json fallback extract command text failed, rawClass={}, err={}",
				rawItem.getClass().getName(), ex.toString());
		}
		return null;
	}

	private MessageItem coerceMessageItem(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof MessageItem) {
			return (MessageItem) raw;
		}
		try {
			return JSON.parseObject(JSON.toJSONString(raw), MessageItem.class);
		} catch (Exception ex) {
			log.warn("[ilink] coerce MessageItem failed, rawClass={}, err={}", raw.getClass().getName(), ex.toString());
			return null;
		}
	}

	private String commandTextSource(MessageItem item) {
		if (item == null) {
			return "unknown";
		}
		if (item.getText_item() != null && StringUtils.hasText(item.getText_item().getText())) {
			return "text";
		}
		if (item.getVoice_item() != null && StringUtils.hasText(item.getVoice_item().getText())) {
			return "voice_asr";
		}
		return "unknown";
	}

	private void handleTextItems(ClientSession session, ILinkClient current, WeixinMessage message, String commandUserId) {
		List<?> items = message.getItem_list();
		if (items == null || items.isEmpty()) {
			log.info("[ilink] message has no items, clientId={}, messageId={}, messageType={}",
				session.clientId, message.getMessage_id(), message.getMessage_type());
			return;
		}
		if (!StringUtils.hasText(commandUserId)) {
			log.warn("[ilink] cannot resolve command user, clientId={}, messageId={}, fromUserId={}, toUserId={}",
				session.clientId, message.getMessage_id(), message.getFrom_user_id(), message.getTo_user_id());
			return;
		}
		for (int i = 0; i < items.size(); i++) {
			Object raw = items.get(i);
			MessageItem item = coerceMessageItem(raw);
			String text = resolveCommandTextFromItem(item);
			String source = null;
			if (StringUtils.hasText(text)) {
				source = commandTextSource(item);
			} else {
				text = resolveCommandTextFromJsonFallback(raw);
				if (StringUtils.hasText(text)) {
					source = "json_fallback";
				}
			}
			if (!StringUtils.hasText(text)) {
				log.debug("[ilink] item skipped (no text/voice transcript), clientId={}, messageId={}, index={}",
					session.clientId, message.getMessage_id(), i);
				continue;
			}
			text = text.trim();
			if (isRecentlySent(session, commandUserId, text)) {
				log.info("[ilink] own reply skipped, clientId={}, commandUserId={}, messageId={}, text={}",
					session.clientId, commandUserId, message.getMessage_id(), text);
				continue;
			}
			log.info("[ilink] command received, clientId={}, commandUserId={}, messageId={}, source={}, text={}",
				session.clientId, commandUserId, message.getMessage_id(),
				source != null ? source : "unknown", text);
			ImCommandExecuteResponse commandResult = commandService.execute(commandUserId, text);
			String reply = commandResult.getReply();
			if (!StringUtils.hasText(reply)) {
				reply = "已收到。";
			}
			log.info("[ilink] command executed, clientId={}, route={}, success={}, replyLength={}, filePath={}",
				session.clientId,
				commandResult.getRoute(),
				commandResult.isSuccess(),
				reply.length(),
				commandResult.getFilePath());
			try {
				current.sendText(commandUserId, reply);
				rememberSent(session, commandUserId, reply);
				log.info("[ilink] text reply sent, clientId={}, toUserId={}, sourceMessageId={}",
					session.clientId, commandUserId, message.getMessage_id());
				sendCommandFileIfPresent(session, current, commandUserId, commandResult);
			} catch (IOException ex) {
				session.lastError = ex.getMessage();
				log.error("[ilink] send reply failed, clientId={}, commandUserId={}, text={}", session.clientId, commandUserId, text, ex);
			}
		}
	}

	private void sendCommandFileIfPresent(ClientSession session, ILinkClient current, String toUserId, ImCommandExecuteResponse commandResult) throws IOException {
		String filePathValue = commandResult.getFilePath();
		if (!StringUtils.hasText(filePathValue)) {
			return;
		}
		Path file = Paths.get(filePathValue);
		if (!Files.exists(file)) {
			log.warn("[ilink] command file not found, clientId={}, filePath={}", session.clientId, file);
			return;
		}
		String fileNameValue = commandResult.getFileName();
		String fileName = !StringUtils.hasText(fileNameValue)
			? file.getFileName().toString()
			: fileNameValue;
		current.sendFile(toUserId, Files.readAllBytes(file), fileName, null);
		log.info("[ilink] file reply sent, clientId={}, toUserId={}, fileName={}, filePath={}", session.clientId, toUserId, fileName, file);
	}

	private void rememberSent(ClientSession session, String toUserId, String text) {
		cleanupRecentSent(session);
		session.recentSentTexts.put(sentKey(toUserId, text), System.currentTimeMillis());
	}

	private boolean isRecentlySent(ClientSession session, String toUserId, String text) {
		cleanupRecentSent(session);
		return session.recentSentTexts.containsKey(sentKey(toUserId, text));
	}

	private String sentKey(String toUserId, String text) {
		return toUserId + "\n" + text;
	}

	private void cleanupRecentSent(ClientSession session) {
		long expireBefore = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
		Iterator<Map.Entry<String, Long>> iterator = session.recentSentTexts.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue() < expireBefore) {
				iterator.remove();
			}
		}
	}

	private void startPollingLoop(ClientSession session, ILinkClient loginClient) {
		if (session == null || loginClient == null) {
			return;
		}
		if (session.polling.compareAndSet(false, true)) {
			Thread thread = new Thread(() -> {
				log.info("[ilink] polling loop started, clientId={}, delayMs={}", session.clientId, pollDelayMs);
				while (session.polling.get()) {
					ILinkClient current = session.client;
					if (current == null || current != loginClient) {
						log.info("[ilink] polling loop exits because client changed or closed, clientId={}", session.clientId);
						break;
					}
					if (isClientReady(current)) {
						pollOnce(session);
					} else {
						log.debug("[ilink] polling loop waiting for login, clientId={}, connectionStatus={}, loginStatus={}",
							session.clientId, current.getConnectionStatus(), current.getLoginStatus().getStatus());
					}
					try {
						Thread.sleep(Math.max(200L, pollDelayMs));
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				session.polling.set(false);
				if (session.pollThread == Thread.currentThread()) {
					session.pollThread = null;
				}
				log.info("[ilink] polling loop stopped, clientId={}", session.clientId);
			}, "imoney-ilink-poll-" + safeThreadName(session.clientId));
			thread.setDaemon(true);
			session.pollThread = thread;
			thread.start();
		} else {
			log.debug("[ilink] polling loop already running, clientId={}", session.clientId);
		}
	}

	private void closeSession(ClientSession session) {
		if (session == null) {
			return;
		}
		synchronized (session.lock) {
			session.polling.set(false);
			if (session.pollThread != null) {
				session.pollThread.interrupt();
				session.pollThread = null;
			}
			if (session.client != null) {
				try {
					session.client.close();
				} catch (Exception ex) {
					log.warn("[ilink] close client failed, clientId={}", session.clientId, ex);
				}
				session.client = null;
			}
			session.lastQrCode = null;
			session.recentSentTexts.clear();
		}
	}

	private ClientSession getOrCreateSession(String clientId) {
		String resolvedClientId = normalizeClientId(clientId);
		return sessions.computeIfAbsent(resolvedClientId, ClientSession::new);
	}

	private ClientSession getSession(String clientId) {
		if (StringUtils.hasText(clientId)) {
			return sessions.get(normalizeClientId(clientId));
		}
		ClientSession defaultSession = sessions.get(DEFAULT_CLIENT_ID);
		if (defaultSession != null) {
			return defaultSession;
		}
		if (sessions.size() == 1) {
			return sessions.values().iterator().next();
		}
		return null;
	}

	private ClientSession createNewSession() {
		while (true) {
			String clientId = nextClientId();
			ClientSession session = new ClientSession(clientId);
			if (sessions.putIfAbsent(clientId, session) == null) {
				return session;
			}
		}
	}

	private String nextClientId() {
		return "ilink-" + System.currentTimeMillis() + "-" + CLIENT_SEQ.incrementAndGet();
	}

	private String normalizeClientId(String clientId) {
		String value = clientId == null ? "" : clientId.trim();
		return StringUtils.hasText(value) ? value : DEFAULT_CLIENT_ID;
	}

	private String safeThreadName(String clientId) {
		return normalizeClientId(clientId).replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private Map<String, Object> status(ClientSession session) {
		Map<String, Object> status = new HashMap<>(12);
		ILinkClient current = session.client;
		status.put("clientId", session.clientId);
		status.put("started", current != null);
		status.put("loggedIn", current != null && isClientReady(current));
		status.put("polling", session.polling.get());
		status.put("pollThreadAlive", session.pollThread != null && session.pollThread.isAlive());
		status.put("connectionStatus", current == null ? "NONE" : String.valueOf(current.getConnectionStatus()));
		status.put("loginStatus", current == null || current.getLoginStatus() == null ? "NONE" : String.valueOf(current.getLoginStatus().getStatus()));
		status.put("lastError", session.lastError);
		status.put("qrCodePresent", StringUtils.hasText(session.lastQrCode));
		if (current != null && current.getLoginContext() != null) {
			status.put("botId", current.getLoginContext().getBotId());
			status.put("userId", current.getLoginContext().getUserId());
			status.put("baseUrl", current.getLoginContext().getBaseUrl());
		}
		return status;
	}

	private boolean isClientReady(ILinkClient client) {
		return client != null && (client.isLoggedIn() || client.getLoginContext() != null);
	}

	private Map<String, Object> missingSession(String clientId) {
		Map<String, Object> result = new HashMap<>(6);
		result.put("clientId", normalizeClientId(clientId));
		result.put("success", false);
		result.put("started", false);
		result.put("loggedIn", false);
		result.put("message", !StringUtils.hasText(clientId) && sessions.size() > 1
			? "当前存在多个 ilink 客户端，请传入 login/start 返回的 clientId。"
			: "该 ilink 客户端尚未创建，请先调用 /login/start。");
		return result;
	}

	private static class ClientSession {
		private final String clientId;
		private final Object lock = new Object();
		private final AtomicBoolean polling = new AtomicBoolean(false);
		private final ConcurrentMap<String, Long> recentSentTexts = new ConcurrentHashMap<>();
		private volatile ILinkClient client;
		private volatile String lastQrCode;
		private volatile String lastError;
		private volatile Thread pollThread;

		private ClientSession(String clientId) {
			this.clientId = clientId;
		}
	}
}
