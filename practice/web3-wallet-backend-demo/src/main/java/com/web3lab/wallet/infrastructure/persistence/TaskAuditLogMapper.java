package com.web3lab.wallet.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3lab.wallet.domain.task.TaskAuditLog;

/**
 * 任务审计日志持久化 Mapper。
 */
public interface TaskAuditLogMapper extends BaseMapper<TaskAuditLog> {
}
