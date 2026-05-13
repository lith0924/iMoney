package org.springblade.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public class PageMethods {
	public static IPage TransportPage(Object obj) {
		IPage<Object> page = new Page<>();
		JSONObject jsonObject = JSONArray.parseObject(String.valueOf(JSON.toJSON(obj)));
		Object current = jsonObject.get("current");
		if (Verify.notCheck(current)) {
			throw new RuntimeException("请传入当前页!");
		}
		Object size = jsonObject.get("size");
		if (Verify.notCheck(size)) {
			throw new RuntimeException("请传入最大页!");
		}
		page.setCurrent(Long.valueOf(String.valueOf(current)));
		page.setSize(Long.valueOf(String.valueOf(size)));
		return page;
	}
}
