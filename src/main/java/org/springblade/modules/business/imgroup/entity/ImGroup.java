package org.springblade.modules.business.imgroup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_group")
@ApiModel(value = "ImGroup对象", description = "共享圈子表")
public class ImGroup implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "共享圈子编码")
	private String groupCode;

	@ApiModelProperty(value = "共享圈子名称")
	private String groupName;

	@ApiModelProperty(value = "创建者用户ID")
	private Long ownerUserId;

	@ApiModelProperty(value = "状态:1启用,0停用")
	private Integer status;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "更新时间")
	private Date updateTime;

}
