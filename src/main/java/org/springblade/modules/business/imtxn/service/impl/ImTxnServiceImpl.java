package org.springblade.modules.business.imtxn.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.imtxn.entity.ImTxn;
import org.springblade.modules.business.imtxn.mapper.ImTxnMapper;
import org.springblade.modules.business.imtxn.service.IImTxnService;
import org.springframework.stereotype.Service;

@Service
public class ImTxnServiceImpl extends ServiceImpl<ImTxnMapper, ImTxn> implements IImTxnService {
}
