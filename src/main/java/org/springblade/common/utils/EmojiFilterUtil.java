package org.springblade.common.utils;

import org.springblade.core.tool.utils.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiFilterUtil {

	//emoji表情过滤+长度过滤+特殊符号过滤
	public static String filterEmoji(String source) {
		if (StringUtil.isBlank(source)) {
			return source;
		}
		Pattern emoji = Pattern.compile("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]|[,\\.]|[，\\.]", Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
		Matcher emojiMatcher = emoji.matcher(source);
		if (emojiMatcher.find()) {
			source = emojiMatcher.replaceAll("");
		}
		if (source.length() > 30) {
			source = source.substring(0, 29);
		}
		return source;
	}

	public static void main(String[] args) {
		System.out.println(filterEmoji("911200001033670750_dbd797c5b，f504ef38,f9c5a2c84e59d45"));
	}
}

