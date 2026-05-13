package org.springblade.modules.business.imexporttask.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.imexporttask.entity.ImExportTask;
import org.springblade.modules.business.imexporttask.mapper.ImExportTaskMapper;
import org.springblade.modules.business.imexporttask.service.IImExportTaskService;
import org.springframework.stereotype.Service;

@Service
public class ImExportTaskServiceImpl extends ServiceImpl<ImExportTaskMapper, ImExportTask> implements IImExportTaskService {
}
