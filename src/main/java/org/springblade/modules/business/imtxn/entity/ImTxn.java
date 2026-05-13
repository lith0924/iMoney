package org.springblade.modules.business.imtxn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

@Data
@TableName("im_txn")
@ApiModel(value = "ImTxn对象", description = "收支流水表")
public class ImTxn implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "展示给用户的操作ID")
	private String opId;

	@ApiModelProperty(value = "记账操作人ID")
	private Long userId;

	@ApiModelProperty(value = "作用域:1私有,2共享")
	private Integer scopeType;

	@ApiModelProperty(value = "共享圈子ID，私有时为空")
	private Long groupId;

	@ApiModelProperty(value = "类型:1支出,2收入")
	private Integer txnType;

	@ApiModelProperty(value = "金额")
	private BigDecimal amount;

	@ApiModelProperty(value = "备注")
	private String note;

	@ApiModelProperty(value = "交易时间")
	private Date txnTime;

	@ApiModelProperty(value = "来源:1命令,2AI识别,3导入")
	private Integer sourceType;

	@ApiModelProperty(value = "原始消息内容")
	private String sourceText;

	@ApiModelProperty(value = "1表示已撤销")
	private Integer isDeleted;

	@ApiModelProperty(value = "撤销时间")
	private Date deletedAt;

	@ApiModelProperty(value = "撤销人ID")
	private Long deletedBy;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "更新时间")
	private Date updateTime;

}
