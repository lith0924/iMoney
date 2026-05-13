package org.springblade.modules.business.imbudget.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.imbudget.entity.ImBudgetSetting;
import org.springblade.modules.business.imbudget.mapper.ImBudgetSettingMapper;
import org.springblade.modules.business.imbudget.service.IImBudgetSettingService;
import org.springframework.stereotype.Service;

@Service
public class ImBudgetSettingServiceImpl extends ServiceImpl<ImBudgetSettingMapper, ImBudgetSetting> implements IImBudgetSettingService {
}
