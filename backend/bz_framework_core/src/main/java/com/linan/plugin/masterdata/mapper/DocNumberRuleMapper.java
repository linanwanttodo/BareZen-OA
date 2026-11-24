package com.linan.plugin.masterdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.plugin.masterdata.entity.DocNumberRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 单据号规则 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface DocNumberRuleMapper extends BaseMapper<DocNumberRule> {

    /**
     * 悲观锁查询规则行（SELECT ... FOR UPDATE）
     *
     * <p>发号事务内先锁定规则行，使同一 bizKey 的并发发号串行化，
     * 后到者等待锁释放后读到最新序号，保证不重号。</p>
     */
    @Select("SELECT * FROM sys_doc_number_rule WHERE biz_key = #{bizKey} AND del_flag = 0 FOR UPDATE")
    DocNumberRule selectByBizKeyForUpdate(@Param("bizKey") String bizKey);

    /**
     * 乐观护栏更新序号：仅当规则行仍处于发号事务读到的状态时生效。
     *
     * <p>以读到的 (current_seq, last_date) 二元组为前置条件（&lt;=&gt; 为 MySQL
     * NULL 安全等值，覆盖种子行 last_date 为 NULL 的场景），日期段切换时
     * current_seq 残留上一段序号、last_date 为旧日期串，二者一同比对才能
     * 正确判定「行未被并发改动」。行锁失效的场景（如只读副本、非 InnoDB）
     * 下两事务同时通过读检查，后提交者的 WHERE 不再匹配，updated=0，
     * 由服务层抛错拒绝重复发号。</p>
     *
     * @return 影响行数（0 表示并发冲突或规则已不存在）
     */
    @Update("UPDATE sys_doc_number_rule SET current_seq = #{nextSeq}, last_date = #{dateStr} "
            + "WHERE id = #{id} AND current_seq <=> #{readSeq} AND last_date <=> #{readDate} AND del_flag = 0")
    int updateSeq(@Param("id") Long id,
                  @Param("readSeq") long readSeq,
                  @Param("readDate") String readDate,
                  @Param("nextSeq") long nextSeq,
                  @Param("dateStr") String dateStr);
}
