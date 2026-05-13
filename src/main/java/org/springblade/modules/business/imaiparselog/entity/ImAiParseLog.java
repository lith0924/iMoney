package org.springblade.modules.business.imaiparselog.entity;

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
@TableName("im_ai_parse_log")
@ApiModel(value = "ImAiParseLog对象", description = "AI识别建议日志表")
public class ImAiParseLog implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "用户ID")
	private Long userId;

	@ApiModelProperty(value = "作用域:1私有,2共享")
	private Integer scopeType;

	@ApiModelProperty(value = "共享圈子ID")
	private Long groupId;

	@ApiModelProperty(value = "原始文本")
	private String originText;

	@ApiModelProperty(value = "建议类型:1支出,2收入")
	private Integer suggestType;

	@ApiModelProperty(value = "建议金额")
	private BigDecimal suggestAmount;

	@ApiModelProperty(value = "建议备注")
	private String suggestNote;

	@ApiModelProperty(value = "确认状态:0待确认,1已确认,2已忽略,3已重写")
	private Integer confirmStatus;

	@ApiModelProperty(value = "关联操作ID")
	private String relatedOpId;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "确认时间")
	private Date confirmedAt;

}
