package com.linan.core.common.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import org.springframework.web.bind.annotation.*;

/**
 * Controller 通用基类（受保护 CRUD）
 *
 * <p>提供通用 CRUD 实现，但默认不暴露 HTTP 路由。
 * 子类必须显式 {@code @Override} 并加 {@code @RequirePermission} 后才能对外开放，
 * 避免继承即匿名开放接口的安全风险。</p>
 *
 * <p>使用方式：</p>
 * <pre>
 * {@code
 * @RestController
 * @RequestMapping("/api/system/v1/notices")
 * public class SysNoticeController extends BaseController&lt;SysNotice, ISysNoticeService&gt; {
 *     @Override
 *     @RequirePermission("system:notice:list")
 *     @GetMapping("/page")
 *     public Result&lt;IPage&lt;SysNotice&gt;&gt; list(Integer pageNum, Integer pageSize) {
 *         return super.list(pageNum, pageSize);
 *     }
 * }
 * }
 * </pre>
 *
 * @param <T> 实体类型
 * @param <S> Service 类型（需继承 IService&lt;T&gt;）
 */
public abstract class BaseController<T, S extends IService<T>> {

    /**
     * 子类通过此方法提供自己的 Service 实例。
     */
    protected abstract S getService();

    /**
     * 分页查询（子类需显式映射并加权限注解）
     */
    public Result<PageResult<T>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<T> page = new Page<>(pageNum, pageSize);
        IPage<T> result = getService().page(page);
        return Result.page(result);
    }

    /**
     * 根据 ID 查询（子类需显式映射并加权限注解）
     */
    public Result<T> getById(@PathVariable Long id) {
        T entity = getService().getById(id);
        if (entity == null) {
            return Result.error("数据不存在");
        }
        return Result.success(entity);
    }

    /**
     * 新增前钩子 —— 返回非 null 的 Result 将中断保存。
     */
    protected Result<Void> preSave(T entity) {
        return null;
    }

    /**
     * 新增后钩子
     */
    protected void postSave(T entity) {
        // no-op
    }

    /**
     * 新增（子类需显式映射并加权限注解）
     */
    public Result<Void> save(@jakarta.validation.Valid @RequestBody T entity) {
        Result<Void> preResult = preSave(entity);
        if (preResult != null) {
            return preResult;
        }
        boolean ok = getService().save(entity);
        if (ok) {
            postSave(entity);
            return Result.success(null);
        }
        return Result.error("新增失败");
    }

    /**
     * 更新（子类需显式映射并加权限注解）
     */
    public Result<Void> update(@jakarta.validation.Valid @RequestBody T entity) {
        boolean ok = getService().updateById(entity);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除（子类需显式映射并加权限注解）
     */
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = getService().removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}