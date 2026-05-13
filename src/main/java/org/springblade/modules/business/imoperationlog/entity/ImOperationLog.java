package org.springblade.modules.business.imoperationlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_operation_log")
@ApiModel(value = "ImOperationLog对象", description = "操作审计日志表")
public class ImOperationLog implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "当前操作ID")
	private String opId;

	@ApiModelProperty(value = "关联操作ID（如撤销目标）")
	private String refOpId;

	@ApiModelProperty(value = "用户ID")
	private Long userId;

	@ApiModelProperty(value = "作用域:1私有,2共享")
	private Integer scopeType;

	@ApiModelProperty(value = "共享圈子ID")
	private Long groupId;

	@ApiModelProperty(value = "操作类型，如ADD_EXPENSE/UNDO/JOIN")
	private String opType;

	@ApiModelProperty(value = "请求文本")
	private String requestText;

	@ApiModelProperty(value = "结果编码")
	private String resultCode;

	@ApiModelProperty(value = "结果说明")
	private String resultMsg;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

}
