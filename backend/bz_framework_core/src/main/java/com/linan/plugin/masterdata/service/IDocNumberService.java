package com.linan.plugin.masterdata.service;

/**
 * 单据号服务
 *
 * <p>按 biz_key 发放形如 SUP20261124-0001 的单据号：行锁（SELECT ... FOR UPDATE）
 * + 乐观护栏更新双重保证并发不重号；跨日按规则日期段格式重置序号。
 * 供采购/销售等后续单据模块复用。</p>
 *
 * @author BareZen Team
 */
public interface IDocNumberService {

    /**
     * 取下一个单据号
     *
     * @param bizKey 业务键（如 SUP/CUS/MAT）
     * @return 单据号，如 SUP20261124-0001
     */
    String nextNumber(String bizKey);
}
