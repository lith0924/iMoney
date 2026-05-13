package org.springblade.modules.business.imusercontext.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_user_context")
@ApiModel(value = "ImUserContext对象", description = "用户当前记账上下文表")
public class ImUserContext implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "用户ID")
	private Long userId;

	@ApiModelProperty(value = "当前共享圈子ID，私有模式为空")
	private Long currentGroupId;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "更新时间")
	private Date updateTime;

}
