package org.springblade.modules.system.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springblade.common.utils.PageUtil;

/**
 * @Description: 业务文件
 * @author: 鹿现策
 * @create: 2023-01-22 17:24:17
 **/


@Data
@ApiModel(value = "SysFile", description = "分页param")
public class SysFilePageParam extends PageUtil {

}

