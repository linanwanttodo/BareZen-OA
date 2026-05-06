package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.system.entity.SysUser;
import com.linan.system.mapper.SysUserDeptMapper;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.mapper.SysUserRoleMapper;
import com.linan.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SysUserServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private SysUserDeptMapper userDeptMapper;

    private SysUserServiceImpl userService;

    private SysUser validUser;

    @BeforeEach
    void setUp() {
        // 构造器注入（与生产代码一致）：传入 userRoleMapper / userDeptMapper，
        // baseMapper（继承自 MyBatis-Plus ServiceImpl）仍需反射注入
        userService = new SysUserServiceImpl(userRoleMapper, userDeptMapper);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        validUser = new SysUser();
        validUser.setUserName("testuser");
        validUser.setNickName("测试用户");
        validUser.setPassword("password123");
        validUser.setPhone("13800138000");
    }

    @Nested
    @DisplayName("getUserByUserName()")
    class GetUserByUserName {

        @Test
        @DisplayName("null 用户名返回 null")
        void nullUserName_returnsNull() {
            assertNull(userService.getUserByUserName(null));
            assertNull(userService.getUserByUserName("   "));
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("有效用户名返回用户对象")
        void validUserName_returnsUser() {
            when(userMapper.selectOne(any())).thenReturn(validUser);

            SysUser result = userService.getUserByUserName("testuser");

            assertNotNull(result);
            assertEquals("testuser", result.getUserName());
            verify(userMapper).selectOne(any());
        }

        @Test
        @DisplayName("用户名自动 trim")
        void userNameIsTrimmed() {
            when(userMapper.selectOne(any())).thenReturn(validUser);

            SysUser result = userService.getUserByUserName("  testuser  ");

            assertNotNull(result);
            assertEquals("testuser", result.getUserName());
        }
    }

    @Nested
    @DisplayName("addUser()")
    class AddUser {

        @Test
        @DisplayName("null 用户抛出异常")
        void nullUser_throwsException() {
            assertThrows(BusinessException.class, () -> userService.addUser(null));
            verifyNoInteractions(userMapper, userRoleMapper, userDeptMapper);
        }

        @Test
        @DisplayName("空用户名抛出异常")
        void emptyUserName_throwsException() {
            validUser.setUserName("");
            assertThrows(BusinessException.class, () -> userService.addUser(validUser));
        }

        @Test
        @DisplayName("用户名已存在抛出异常")
        void duplicateUserName_throwsException() {
            when(userMapper.selectCount(any())).thenReturn(1L);
            assertThrows(BusinessException.class, () -> userService.addUser(validUser));
        }
    }

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        @Test
        @DisplayName("空密码返回错误消息，不触达数据库")
        void blankPassword_rejected() {
            assertEquals("新密码不能为空", userService.resetPassword(1L, null));
            assertEquals("新密码不能为空", userService.resetPassword(1L, ""));
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("弱密码返回策略消息")
        void weakPassword_rejected() {
            assertNotNull(userService.resetPassword(1L, "abc"));
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("用户不存在返回错误消息")
        void missingUser_rejected() {
            when(userMapper.selectById(1L)).thenReturn(null);

            assertEquals("用户不存在", userService.resetPassword(1L, "Strong#Pass1"));
        }

        @Test
        @DisplayName("重置成功返回 null 并更新加密密码（不覆盖其他字段）")
        void success_returnsNull() {
            SysUser existing = new SysUser();
            existing.setId(1L);
            existing.setUserName("admin");
            existing.setPassword("$2a$10$old");
            when(userMapper.selectById(1L)).thenReturn(existing);
            when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

            assertNull(userService.resetPassword(1L, "Strong#Pass1"));

            ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
            verify(userMapper).updateById(captor.capture());
            // 密码已加密且不覆盖用户名等业务字段
            assertEquals(1L, captor.getValue().getId());
            assertTrue(captor.getValue().getPassword().startsWith("$2a$"));
            assertEquals("admin", captor.getValue().getUserName());
        }
    }
}
