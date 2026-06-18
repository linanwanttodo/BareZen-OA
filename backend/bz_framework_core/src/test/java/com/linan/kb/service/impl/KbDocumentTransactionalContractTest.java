package com.linan.kb.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 知识文档多写路径事务契约测试
 *
 * <p>{@code createDocument} 先落文档再写版本快照，{@code updateDocument} 先写旧内容快照
 * 再更新文档，两者都是跨表双写。缺少事务时中途失败会留下"有正文无版本"或"版本链断裂"
 * 的永久脏数据，而版本链正是知识库模块的核心价值。本测试锁定这两个方法必须带
 * {@code @Transactional(rollbackFor = Exception.class)}。</p>
 *
 * <p>注意：本测试只锁定注解声明，不验证运行期回滚行为——代理语义需真库集成测试覆盖。</p>
 */
class KbDocumentTransactionalContractTest {

    private Transactional transactionalOf(String methodName) throws NoSuchMethodException {
        Method method = Arrays.stream(KbDocumentServiceImpl.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(methodName));
        return method.getAnnotation(Transactional.class);
    }

    @Test
    @DisplayName("新建文档的文档+版本双写在同一事务内")
    void createDocument_isTransactional() throws NoSuchMethodException {
        assertRollsBackOnException(transactionalOf("createDocument"), "createDocument");
    }

    @Test
    @DisplayName("更新文档的快照+更新双写在同一事务内")
    void updateDocument_isTransactional() throws NoSuchMethodException {
        assertRollsBackOnException(transactionalOf("updateDocument"), "updateDocument");
    }

    private void assertRollsBackOnException(Transactional transactional, String methodName) {
        assertNotNull(transactional, methodName + " 必须标注 @Transactional，否则双写中途失败会留下断裂的版本链");
        assertTrue(Arrays.asList(transactional.rollbackFor()).contains(Exception.class),
                methodName + " 必须声明 rollbackFor = Exception.class，受检异常默认不回滚");
    }
}
