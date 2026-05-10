package com.linan.system.service.impl;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.TableInfo;
import com.linan.system.service.IGenCodeGenerationService;
import com.linan.system.service.IGenTableQueryService;
import com.linan.system.service.IGenTypeMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成 - 代码生成服务实现
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class GenCodeGenerationServiceImpl implements IGenCodeGenerationService {

    private final IGenTableQueryService tableQueryService;
    private final IGenTypeMappingService typeMappingService;

    @Override
    public Map<String, String> generateCode(String tableName) {
        TableInfo table = tableQueryService.selectTableByName(tableName);
        if (table == null) {
            throw new IllegalArgumentException("表不存在: " + tableName);
        }

        // 填充衍生字段
        enrichTableInfo(table);

        Map<String, String> codeMap = new HashMap<>();
        codeMap.put("entity.java", generateEntityCode(table));
        codeMap.put("mapper.java", generateMapperCode(table));
        codeMap.put("service.java", generateServiceInterfaceCode(table));
        codeMap.put("serviceImpl.java", generateServiceImplCode(table));
        codeMap.put("controller.java", generateControllerCode(table));
        codeMap.put("index.vue", generateVueCode(table));

        return codeMap;
    }

    @Override
    public String generateEntityCode(TableInfo table) {
        // TODO: 实现实体类代码生成
        return generatePlaceholder("Entity", table);
    }

    @Override
    public String generateMapperCode(TableInfo table) {
        // TODO: 实现Mapper接口代码生成
        return generatePlaceholder("Mapper", table);
    }

    @Override
    public String generateServiceInterfaceCode(TableInfo table) {
        // TODO: 实现Service接口代码生成
        return generatePlaceholder("Service", table);
    }

    @Override
    public String generateServiceImplCode(TableInfo table) {
        // TODO: 实现Service实现类代码生成
        return generatePlaceholder("ServiceImpl", table);
    }

    @Override
    public String generateControllerCode(TableInfo table) {
        // TODO: 实现Controller代码生成
        return generatePlaceholder("Controller", table);
    }

    @Override
    public String generateVueCode(TableInfo table) {
        // TODO: 实现Vue页面代码生成
        return generatePlaceholder("Vue", table);
    }

    /**
     * 填充表的衍生字段
     */
    private void enrichTableInfo(TableInfo table) {
        String className = typeMappingService.tableToClassName(table.getTableName());
        table.setClassName(className);
        table.setClassVar(Character.toLowerCase(className.charAt(0)) + className.substring(1));
        table.setModuleName(typeMappingService.resolveModuleName(table.getTableName()));
        table.setPermPrefix(typeMappingService.resolvePermPrefix(table.getTableName()));

        // 填充字段的Java类型和字段名
        for (ColumnInfo column : table.getColumns()) {
            column.setJavaType(typeMappingService.mapToJavaType(column.getDataType()));
            column.setJavaField(typeMappingService.columnToField(column.getColumnName()));
            column.setHtmlType(typeMappingService.resolveHtmlType(column));
            if (column.getDictType() == null || column.getDictType().isEmpty()) {
                column.setDictType(typeMappingService.resolveDictType(column.getColumnName()));
            }
        }
    }

    /**
     * 生成占位符代码（实际实现需要集成模板引擎）
     */
    private String generatePlaceholder(String type, TableInfo table) {
        return "// TODO: " + type + " code generation for table: " + table.getTableName() + "\n" +
               "// This is a placeholder - implement with template engine like FreeMarker or Velocity";
    }
}