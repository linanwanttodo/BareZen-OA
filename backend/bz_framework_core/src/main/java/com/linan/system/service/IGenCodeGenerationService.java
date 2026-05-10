package com.linan.system.service;

import com.linan.core.gen.TableInfo;

import java.util.Map;

/**
 * 代码生成 - 代码生成服务接口
 *
 * 负责根据表信息生成各种代码文件
 *
 * @author BareZen Team
 */
public interface IGenCodeGenerationService {

    /**
     * 生成代码，返回 Map<文件名, 代码内容>
     *
     * @param tableName 表名
     * @return 代码文件映射
     */
    Map<String, String> generateCode(String tableName);

    /**
     * 生成Java实体类代码
     *
     * @param table 表信息
     * @return Java代码
     */
    String generateEntityCode(TableInfo table);

    /**
     * 生成Mapper接口代码
     *
     * @param table 表信息
     * @return Mapper代码
     */
    String generateMapperCode(TableInfo table);

    /**
     * 生成Service接口代码
     *
     * @param table 表信息
     * @return Service接口代码
     */
    String generateServiceInterfaceCode(TableInfo table);

    /**
     * 生成Service实现类代码
     *
     * @param table 表信息
     * @return Service实现代码
     */
    String generateServiceImplCode(TableInfo table);

    /**
     * 生成Controller代码
     *
     * @param table 表信息
     * @return Controller代码
     */
    String generateControllerCode(TableInfo table);

    /**
     * 生成Vue页面代码
     *
     * @param table 表信息
     * @return Vue代码
     */
    String generateVueCode(TableInfo table);
}