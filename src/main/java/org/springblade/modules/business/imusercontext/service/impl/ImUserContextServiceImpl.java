package org.springblade.modules.business.imusercontext.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.imusercontext.entity.ImUserContext;
import org.springblade.modules.business.imusercontext.mapper.ImUserContextMapper;
import org.springblade.modules.business.imusercontext.service.IImUserContextService;
import org.springframework.stereotype.Service;

@Service
public class ImUserContextServiceImpl extends ServiceImpl<ImUserContextMapper, ImUserContext> implements IImUserContextService {
}
