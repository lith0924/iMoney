package org.springblade.modules.business.imgroupmember.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_group_member")
@ApiModel(value = "ImGroupMember对象", description = "共享圈子成员表")
public class ImGroupMember implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "共享圈子ID")
	private Long groupId;

	@ApiModelProperty(value = "用户ID")
	private Long userId;

	@ApiModelProperty(value = "角色类型:1创建者,2成员")
	private Integer roleType;

	@ApiModelProperty(value = "加入时间")
	private Date joinedAt;

	@ApiModelProperty(value = "退出时间")
	private Date leftAt;

	@ApiModelProperty(value = "状态:1在册,0已退出")
	private Integer status;

}
