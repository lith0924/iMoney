package org.springblade.modules.business.imexporttask.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_export_task")
@ApiModel(value = "ImExportTask对象", description = "Excel导出任务表")
public class ImExportTask implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(value = "主键")
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@ApiModelProperty(value = "任务编号")
	private String taskNo;

	@ApiModelProperty(value = "用户ID")
	private Long userId;

	@ApiModelProperty(value = "作用域:1私有,2共享")
	private Integer scopeType;

	@ApiModelProperty(value = "共享圈子ID")
	private Long groupId;

	@ApiModelProperty(value = "年份")
	private Integer yearVal;

	@ApiModelProperty(value = "月份")
	private Integer monthVal;

	@ApiModelProperty(value = "状态:0排队,1处理中,2成功,3失败")
	private Integer status;

	@ApiModelProperty(value = "进度百分比0-100")
	private Integer progress;

	@ApiModelProperty(value = "导出文件地址")
	private String fileUrl;

	@ApiModelProperty(value = "错误信息")
	private String errorMsg;

	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	@ApiModelProperty(value = "完成时间")
	private Date finishedAt;

}
