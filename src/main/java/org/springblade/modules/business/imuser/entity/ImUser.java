package org.springblade.modules.business.imuser.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_user")
@ApiModel(value = "ImUser对象", description = "iMoney 用户表")
public class ImUser implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "微信用户ID")
	private String wxUserId;

	@ApiModelProperty(value = "botID")
	private String botId;

	@ApiModelProperty(value = "botToken")
	private String botToken;

	@ApiModelProperty(value = "昵称")
	private String nickName;

	@ApiModelProperty(value = "状态:1启用,0停用")
	private Integer status;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "更新时间")
	private Date updateTime;

}
