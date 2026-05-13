package org.springblade.common.utils;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PageUtil {
	@ApiModelProperty("当前页")
	private Integer current;

	@ApiModelProperty("最大页")
	private Integer size;
}
