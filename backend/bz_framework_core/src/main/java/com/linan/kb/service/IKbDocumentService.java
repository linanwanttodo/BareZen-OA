package com.linan.kb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.kb.entity.KbDocument;

/**
 * 知识文档服务接口
 *
 * <p>版本快照、作者兜底、浏览量自增、删除双轨校验等业务规则下沉于此，Controller 仅委托。</p>
 */
public interface IKbDocumentService extends IService<KbDocument> {

    /**
     * 新增文档（首版即生成版本快照 v1，作者由服务端按当前登录用户填充）
     *
     * @param doc 文档实体（含标题/分类/内容）
     * @return 是否成功
     */
    boolean createDocument(KbDocument doc);

    /**
     * 更新文档（更新前先写当前内容版本快照，保证历史可回溯）
     *
     * @param doc 文档实体（含 id 与待更新字段）
     * @return 是否成功
     */
    boolean updateDocument(KbDocument doc);

    /**
     * 删除文档（作者本人 OR 持删除权限，双轨校验）
     *
     * @param id     文档 ID
     * @param userId 当前用户 ID
     * @return 是否成功
     */
    boolean deleteDocument(Long id, Long userId);

    /**
     * 浏览文档：返回文档并将浏览量原子 +1
     *
     * @param id 文档 ID
     * @return 文档（浏览量已自增）
     */
    KbDocument viewDocument(Long id);
}