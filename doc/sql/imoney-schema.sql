-- iMoney 业务表结构（记账、圈子、流水、导出、AI 日志、预算等）
-- 适配 MySQL 8.x；与 blade-saber-mysql.sql 等基础库脚本分开执行

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) 用户表
CREATE TABLE IF NOT EXISTS `im_user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `wx_user_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '微信用户ID',
  `nick_name` VARCHAR(64) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='iMoney 用户表';

-- 2) 共享圈子表（用于“公费”记账）
CREATE TABLE IF NOT EXISTS `im_group` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `group_code` VARCHAR(32) NOT NULL UNIQUE COMMENT '共享圈子编码',
  `group_name` VARCHAR(64) NOT NULL COMMENT '共享圈子名称',
  `owner_user_id` BIGINT NOT NULL COMMENT '创建者用户ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用,0停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享圈子表';

-- 3) 共享圈子成员表
CREATE TABLE IF NOT EXISTS `im_group_member` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role_type` TINYINT NOT NULL DEFAULT 2 COMMENT '1创建者,2成员',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `left_at` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1在册,0已退出',
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_group_status` (`group_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享圈子成员表';

-- 4) 成员备注表（私有备注）
CREATE TABLE IF NOT EXISTS `im_member_alias` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL COMMENT '所属共享圈子ID',
  `owner_user_id` BIGINT NOT NULL COMMENT '设置备注的人',
  `target_user_id` BIGINT NOT NULL COMMENT '被备注的人',
  `alias_name` VARCHAR(64) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_alias` (`group_id`, `owner_user_id`, `target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员私有备注表';

-- 5) 邀请码表（用于加入共享圈子）
CREATE TABLE IF NOT EXISTS `im_invite_code` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `invite_code` VARCHAR(16) NOT NULL UNIQUE COMMENT '邀请码，如FY3K9',
  `group_id` BIGINT NOT NULL COMMENT '共享圈子ID',
  `inviter_user_id` BIGINT NOT NULL,
  `invitee_alias` VARCHAR(64) DEFAULT NULL COMMENT '邀请时给对方的备注',
  `max_use` INT NOT NULL DEFAULT 1,
  `used_count` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1可用,0失效',
  `expires_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `used_at` DATETIME DEFAULT NULL,
  KEY `idx_group` (`group_id`),
  KEY `idx_status_expire` (`status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享圈子邀请码表';

-- 6) 账单流水表（默认私有，指定共享后为公费）
CREATE TABLE IF NOT EXISTS `im_txn` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `op_id` VARCHAR(32) NOT NULL UNIQUE COMMENT '展示给用户的操作ID',
  `user_id` BIGINT NOT NULL COMMENT '记账操作人ID',
  `scope_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1私有,2共享',
  `group_id` BIGINT DEFAULT NULL COMMENT '共享圈子ID，私有时为空',
  `txn_type` TINYINT NOT NULL COMMENT '1支出,2收入',
  `amount` DECIMAL(18,2) NOT NULL,
  `note` VARCHAR(255) DEFAULT NULL,
  `txn_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `source_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1命令,2AI识别,3导入',
  `source_text` VARCHAR(500) DEFAULT NULL COMMENT '原始消息内容',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '1表示已撤销',
  `deleted_at` DATETIME DEFAULT NULL,
  `deleted_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `chk_txn_scope_group`
    CHECK (
      (`scope_type` = 1 AND `group_id` IS NULL) OR
      (`scope_type` = 2 AND `group_id` IS NOT NULL)
    ),
  KEY `idx_scope_group_time` (`scope_type`, `group_id`, `txn_time`),
  KEY `idx_user_time` (`user_id`, `txn_time`),
  KEY `idx_scope_deleted_time` (`scope_type`, `group_id`, `is_deleted`, `txn_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支流水表';

-- 7) 操作日志表
CREATE TABLE IF NOT EXISTS `im_operation_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `op_id` VARCHAR(32) NOT NULL COMMENT '当前操作ID',
  `ref_op_id` VARCHAR(32) DEFAULT NULL COMMENT '关联操作ID（如撤销目标）',
  `user_id` BIGINT NOT NULL,
  `scope_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1私有,2共享',
  `group_id` BIGINT DEFAULT NULL COMMENT '共享圈子ID',
  `op_type` VARCHAR(32) NOT NULL COMMENT '操作类型，如ADD_EXPENSE/UNDO/JOIN',
  `request_text` VARCHAR(500) DEFAULT NULL,
  `result_code` VARCHAR(32) NOT NULL DEFAULT 'OK',
  `result_msg` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `chk_op_scope_group`
    CHECK (
      (`scope_type` = 1 AND `group_id` IS NULL) OR
      (`scope_type` = 2 AND `group_id` IS NOT NULL)
    ),
  KEY `idx_ref_op` (`ref_op_id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_scope_group` (`scope_type`, `group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- 8) 导出任务表
CREATE TABLE IF NOT EXISTS `im_export_task` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `task_no` VARCHAR(32) NOT NULL UNIQUE,
  `user_id` BIGINT NOT NULL,
  `scope_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1私有,2共享',
  `group_id` BIGINT DEFAULT NULL COMMENT '共享圈子ID',
  `year_val` INT DEFAULT NULL,
  `month_val` INT DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0排队,1处理中,2成功,3失败',
  `progress` INT NOT NULL DEFAULT 0 COMMENT '进度百分比0-100',
  `file_url` VARCHAR(500) DEFAULT NULL,
  `error_msg` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` DATETIME DEFAULT NULL,
  CONSTRAINT `chk_export_scope_group`
    CHECK (
      (`scope_type` = 1 AND `group_id` IS NULL) OR
      (`scope_type` = 2 AND `group_id` IS NOT NULL)
    ),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_scope_group_status` (`scope_type`, `group_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Excel导出任务表';

-- 9) AI解析日志表
CREATE TABLE IF NOT EXISTS `im_ai_parse_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `scope_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1私有,2共享',
  `group_id` BIGINT DEFAULT NULL COMMENT '共享圈子ID',
  `origin_text` VARCHAR(500) NOT NULL,
  `suggest_type` TINYINT DEFAULT NULL COMMENT '1支出,2收入',
  `suggest_amount` DECIMAL(18,2) DEFAULT NULL,
  `suggest_note` VARCHAR(255) DEFAULT NULL,
  `confirm_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认,1已确认,2已忽略,3已重写',
  `related_op_id` VARCHAR(32) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirmed_at` DATETIME DEFAULT NULL,
  CONSTRAINT `chk_ai_scope_group`
    CHECK (
      (`scope_type` = 1 AND `group_id` IS NULL) OR
      (`scope_type` = 2 AND `group_id` IS NOT NULL)
    ),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_confirm` (`confirm_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI识别建议日志表';

-- 10) 用户上下文表（记录当前私有/共享模式）
CREATE TABLE IF NOT EXISTS `im_user_context` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `current_scope_type` TINYINT NOT NULL DEFAULT 1 COMMENT '当前记账模式：1私有,2共享',
  `current_group_id` BIGINT DEFAULT NULL COMMENT '当前共享圈子ID，私有模式为空',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user` (`user_id`),
  KEY `idx_scope_group` (`current_scope_type`, `current_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户当前记账上下文表';

-- 11) 邀请码使用记录表（谁在什么时候使用了邀请码）
CREATE TABLE IF NOT EXISTS `im_invite_code_use_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `invite_code_id` BIGINT NOT NULL COMMENT '邀请码主键ID',
  `invite_code` VARCHAR(16) NOT NULL COMMENT '邀请码快照',
  `group_id` BIGINT NOT NULL COMMENT '共享圈子ID',
  `used_by_user_id` BIGINT NOT NULL COMMENT '使用邀请码的用户ID',
  `result_code` VARCHAR(32) NOT NULL DEFAULT 'OK' COMMENT '使用结果编码，如OK/EXPIRED/INVALID',
  `result_msg` VARCHAR(255) DEFAULT NULL COMMENT '使用结果说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_invite_code_id` (`invite_code_id`),
  KEY `idx_group_user_time` (`group_id`, `used_by_user_id`, `create_time`),
  KEY `idx_result_code_time` (`result_code`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码使用明细日志表';

-- 12) 预算设置表（每用户每周期一条）
CREATE TABLE IF NOT EXISTS `im_budget_setting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `period_type` TINYINT NOT NULL COMMENT '预算周期:1日,2月,3年',
  `budget_amount` DECIMAL(18,2) NOT NULL COMMENT '预算金额',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_period` (`user_id`,`period_type`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户预算设置表';

SET FOREIGN_KEY_CHECKS = 1;
