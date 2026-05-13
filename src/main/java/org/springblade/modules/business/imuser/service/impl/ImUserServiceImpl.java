package org.springblade.modules.business.imuser.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.imuser.entity.ImUser;
import org.springblade.modules.business.imuser.mapper.ImUserMapper;
import org.springblade.modules.business.imuser.service.IImUserService;
import org.springframework.stereotype.Service;

@Service
public class ImUserServiceImpl extends ServiceImpl<ImUserMapper, ImUser> implements IImUserService {
}
