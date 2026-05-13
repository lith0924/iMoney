package org.springblade.modules.business.imbudget.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("im_budget_setting")
@ApiModel(value = "ImBudgetSetting对象", description = "用户预算设置表")
public class ImBudgetSetting implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "用户ID")
	private Long userId;

	@ApiModelProperty(value = "预算周期:1日,2月,3年")
	private Integer periodType;

	@ApiModelProperty(value = "预算金额")
	private BigDecimal budgetAmount;

	@ApiModelProperty(value = "状态:1启用,0停用")
	private Integer status;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "更新时间")
	private Date updateTime;
}
