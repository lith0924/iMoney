package org.springblade.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.system.entity.SysFile;
import org.springblade.modules.system.vo.SysFilePageParam;

import java.util.List;

/**
 * @Description: 业务文件
 * @author: 鹿现策
 * @create: 2023-01-22 17:24:17
 **/


@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
	IPage<SysFile> selectByPage(IPage<SysFile> iPage, @Param("param") SysFilePageParam param);

	SysFile selectById(Long id);

	List<SysFile> selectFileList(Long id);

	int deleteSysFileById(Long id);

	boolean batchInsert(List<SysFile> sysFileList);
}
