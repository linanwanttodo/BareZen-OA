package com.linan.core.common.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.core.common.api.Result;
import org.springframework.web.bind.annotation.*;

/**
 * Controller 通用基类
 *
 * 提供通用的 CRUD 接口实现。子类 Controller 继承此基类后，
 * 自动获得以下接口，无需重复编写：
 * - GET    /list       分页查询
 * - GET    /{id}       根据ID查询
 * - POST   /           新增
 * - PUT    /           更新
 * - DELETE /{id}       删除
 *
 * 使用方式：
 * <pre>
 * @RestController
 * @RequestMapping("/api/system/v1/users")
 * public class UserController extends BaseController< User, UserService> {
 *     // 自动继承增删改查接口，只需添加自定义业务接口
 * }
 * </pre>
 *
 * @param <T> 实体类型（如 User）
 * @param <S> Service 类型（如 UserService，需继承 IService<T>）
 */
public abstract class BaseController<T, S extends IService<T>> {

    /**
     * 子类通过此方法提供自己的 Service 实例。
     *
     * @return Service 实例
     */
    protected abstract S getService();

    /**
     * 分页查询
     *
     * 请求：GET /{controller-path}/page?pageNum=1&pageSize=10
     * 响应：{"code":200,"msg":"操作成功","data":{"total":100,"records":[...]}}
     */
    @GetMapping("/page")
    public Result<IPage<T>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<T> page = new Page<>(pageNum, pageSize);
        IPage<T> result = getService().page(page);
        return Result.success(result);
    }

    /**
     * 根据ID查询
     *
     * 请求：GET /{controller-path}/{id}
     * 响应：{"code":200,"msg":"操作成功","data":{...}}
     */
    @GetMapping("/{id}")
    public Result<T> getById(@PathVariable Long id) {
        T entity = getService().getById(id);
        if (entity == null) {
            return Result.error("数据不存在");
        }
        return Result.success(entity);
    }

    /**
     * 新增前钩子 —— 子类可重写此方法在保存前执行额外处理（如参数补充、权限校验）。
     * 返回非 null 的 Result 将中断保存流程并直接返回该结果。
     */
    protected Result<Void> preSave(T entity) {
        return null; // 默认不做额外处理
    }

    /**
     * 新增后钩子 —— 子类可重写此方法在保存成功后执行额外处理（如关联数据写入）。
     */
    protected void postSave(T entity) {
        // 默认不做额外处理
    }

    /**
     * 新增
     *
     * 请求：POST /{controller-path}  Body: {...}
     * 响应：{"code":200,"msg":"操作成功","data":null}
     */
    @PostMapping
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
     * 更新
     *
     * 请求：PUT /{controller-path}  Body: {...}
     * 响应：{"code":200,"msg":"操作成功","data":null}
     */
    @PutMapping
    public Result<Void> update(@jakarta.validation.Valid @RequestBody T entity) {
        boolean ok = getService().updateById(entity);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除
     *
     * 请求：DELETE /{controller-path}/{id}
     * 响应：{"code":200,"msg":"操作成功","data":null}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = getService().removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}