package org.springblade.modules.business.iminvitecodeuselog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_invite_code_use_log")
@ApiModel(value = "ImInviteCodeUseLog对象", description = "邀请码使用明细日志表")
public class ImInviteCodeUseLog implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "邀请码主键ID")
	private Long inviteCodeId;

	@ApiModelProperty(value = "邀请码快照")
	private String inviteCode;

	@ApiModelProperty(value = "共享圈子ID")
	private Long groupId;

	@ApiModelProperty(value = "使用邀请码的用户ID")
	private Long usedByUserId;

	@ApiModelProperty(value = "使用结果编码，如OK/EXPIRED/INVALID")
	private String resultCode;

	@ApiModelProperty(value = "使用结果说明")
	private String resultMsg;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

}
