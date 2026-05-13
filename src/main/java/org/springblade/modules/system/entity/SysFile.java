package org.springblade.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_file")
@ApiModel(value = "业务文件实体类", description = "业务文件实体类")
@JsonSerialize(nullsUsing = NullSerializer.class)
public class SysFile {
	@ApiModelProperty("")
	private Long id;

	@ApiModelProperty("父级id")
	private Long parentId;

	@ApiModelProperty("标识")
	private String biz;

	@ApiModelProperty("文件类型")
	private String type;

	@ApiModelProperty("文件字典类型")
	private Integer typeNum;

	@ApiModelProperty("文件名")
	private String fileName;

	@ApiModelProperty("新文件名")
	private String newFileName;

	@ApiModelProperty("文件路径")
	private String filePath;

	@ApiModelProperty("文件大小")
	private String size;

	@ApiModelProperty("视频时长")
	private String videoTime;

	@ApiModelProperty("office文件转为png的文件")
	private String filePngPath;

	@ApiModelProperty("删除状态（0，正常，1已删除）")
	private Integer isDeleted;

	@ApiModelProperty("创建人")
	private Long createUser;

	@ApiModelProperty("创建时间")
	private Date createTime;

	@ApiModelProperty("更新人")
	private Long updateUser;

	@ApiModelProperty("更新时间")
	private Date updateTime;

	@ApiModelProperty("名称")
	@TableField(exist = false)
	private String name;

}
