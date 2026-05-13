package org.springblade.modules.business.immemberalias.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_member_alias")
@ApiModel(value = "ImMemberAlias对象", description = "成员私有备注表")
public class ImMemberAlias implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "所属共享圈子ID")
	private Long groupId;

	@ApiModelProperty(value = "设置备注的人")
	private Long ownerUserId;

	@ApiModelProperty(value = "被备注的人")
	private Long targetUserId;

	@ApiModelProperty(value = "备注名称")
	private String aliasName;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "更新时间")
	private Date updateTime;

}
