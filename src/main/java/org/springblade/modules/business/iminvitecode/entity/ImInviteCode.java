package org.springblade.modules.business.iminvitecode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_invite_code")
@ApiModel(value = "ImInviteCode对象", description = "共享圈子邀请码表")
public class ImInviteCode implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "邀请码，如FY3K9")
	private String inviteCode;

	@ApiModelProperty(value = "共享圈子ID")
	private Long groupId;

	@ApiModelProperty(value = "邀请人用户ID")
	private Long inviterUserId;

	@ApiModelProperty(value = "邀请时给对方的备注")
	private String inviteeAlias;

	@ApiModelProperty(value = "最大使用次数")
	private Integer maxUse;

	@ApiModelProperty(value = "已使用次数")
	private Integer usedCount;

	@ApiModelProperty(value = "状态:1可用,0失效")
	private Integer status;

	@ApiModelProperty(value = "过期时间")
	private Date expiresAt;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "使用时间")
	private Date usedAt;

}
