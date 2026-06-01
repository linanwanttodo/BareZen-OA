package com.linan.system.service.impl;

import com.linan.core.gen.ColumnInfo;
import com.linan.core.gen.GenConstants;
import com.linan.core.gen.TableInfo;
import com.linan.system.service.IGenCodeGenerationService;
import com.linan.system.service.IGenTableQueryService;
import com.linan.system.service.IGenTypeMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
        String pkg = "com.linan." + table.getModuleName() + ".entity";
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.baomidou.mybatisplus.annotation.TableId;\n");
        sb.append("import com.baomidou.mybatisplus.annotation.TableName;\n");
        sb.append("import com.baomidou.mybatisplus.annotation.TableLogic;\n");
        sb.append("import lombok.Data;\n\n");

        // 收集需要 import 的 Java 类型
        String imports = table.getColumns().stream()
                .map(ColumnInfo::getJavaType)
                .filter(t -> t.startsWith("java."))
                .distinct()
                .sorted()
                .map(t -> "import " + t + ";\n")
                .collect(Collectors.joining());
        sb.append(imports);

        if (!imports.isEmpty()) sb.append("\n");

        sb.append("/**\n");
        sb.append(" * ").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append("\n");
        sb.append(" */\n");
        sb.append("@Data\n");
        sb.append("@TableName(\"").append(table.getTableName()).append("\")\n");
        sb.append("public class ").append(table.getClassName()).append(" {\n\n");

        for (ColumnInfo col : table.getColumns()) {
            sb.append("    /** ").append(col.getColumnComment() != null ? col.getColumnComment() : col.getColumnName()).append(" */\n");
            if (col.isPk()) {
                sb.append("    @TableId\n");
            }
            sb.append("    private ").append(col.getJavaType()).append(" ").append(col.getJavaField()).append(";\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String generateMapperCode(TableInfo table) {
        String pkg = "com.linan." + table.getModuleName() + ".mapper";
        String entityPkg = "com.linan." + table.getModuleName() + ".entity." + table.getClassName();
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n");
        sb.append("import ").append(entityPkg).append(";\n");
        sb.append("import org.apache.ibatis.annotations.Mapper;\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append(" Mapper\n");
        sb.append(" */\n");
        sb.append("@Mapper\n");
        sb.append("public interface ").append(table.getClassName()).append("Mapper extends BaseMapper<").append(table.getClassName()).append("> {\n");
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String generateServiceInterfaceCode(TableInfo table) {
        String pkg = "com.linan." + table.getModuleName() + ".service";
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.baomidou.mybatisplus.extension.service.IService;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".entity.").append(table.getClassName()).append(";\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append(" 服务接口\n");
        sb.append(" */\n");
        sb.append("public interface I").append(table.getClassName()).append("Service extends IService<").append(table.getClassName()).append("> {\n");
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String generateServiceImplCode(TableInfo table) {
        String pkg = "com.linan." + table.getModuleName() + ".service.impl";
        String entityClass = table.getClassName();
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".entity.").append(entityClass).append(";\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".mapper.").append(entityClass).append("Mapper;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".service.I").append(entityClass).append("Service;\n");
        sb.append("import lombok.RequiredArgsConstructor;\n");
        sb.append("import org.springframework.stereotype.Service;\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append(" 服务实现\n");
        sb.append(" */\n");
        sb.append("@Service\n");
        sb.append("@RequiredArgsConstructor\n");
        sb.append("public class ").append(entityClass).append("ServiceImpl extends ServiceImpl<").append(entityClass).append("Mapper, ").append(entityClass).append("> implements I").append(entityClass).append("Service {\n");
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String generateControllerCode(TableInfo table) {
        String pkg = "com.linan." + table.getModuleName() + ".controller";
        String entityClass = table.getClassName();
        String classVar = table.getClassVar();
        String mapping = "/api/" + table.getModuleName() + "/v1/" + classVar + "s";
        String perm = table.getPermPrefix();

        // 收集可查询字段（like 和 eq）
        String queryFields = table.getColumns().stream()
                .filter(c -> !GenConstants.BASE_FIELDS.contains(c.getJavaField()))
                .filter(c -> c.isQuery())
                .map(c -> "        // " + c.getColumnComment() + " - " + c.getQueryType())
                .collect(Collectors.joining("\n"));

        // 收集列表显示的字段名
        String listFields = table.getColumns().stream()
                .filter(c -> c.isList())
                .map(ColumnInfo::getJavaField)
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;\n");
        sb.append("import com.baomidou.mybatisplus.extension.plugins.pagination.Page;\n");
        sb.append("import com.linan.core.common.api.Result;\n");
        sb.append("import com.linan.core.common.annotation.OperLog;\n");
        sb.append("import com.linan.core.security.RequirePermission;\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".entity.").append(entityClass).append(";\n");
        sb.append("import com.linan.").append(table.getModuleName()).append(".service.I").append(entityClass).append("Service;\n");
        sb.append("import io.swagger.v3.oas.annotations.Operation;\n");
        sb.append("import io.swagger.v3.oas.annotations.tags.Tag;\n");
        sb.append("import lombok.RequiredArgsConstructor;\n");
        sb.append("import org.springframework.web.bind.annotation.*;\n\n");

        sb.append("/**\n");
        sb.append(" * ").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append(" 管理\n");
        sb.append(" */\n");
        sb.append("@Tag(name = \"").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append("\")\n");
        sb.append("@RestController\n");
        sb.append("@RequestMapping(\"").append(mapping).append("\")\n");
        sb.append("@RequiredArgsConstructor\n");
        sb.append("public class ").append(entityClass).append("Controller {\n\n");

        sb.append("    private final I").append(entityClass).append("Service ").append(classVar).append("Service;\n\n");

        // 分页查询
        sb.append("    @GetMapping(\"/page\")\n");
        sb.append("    @Operation(summary = \"分页查询" + (table.getTableComment() != null ? table.getTableComment() : table.getTableName()) + "\")\n");
        sb.append("    @RequirePermission(\"").append(perm).append(":list\")\n");
        sb.append("    public Result<Page<").append(entityClass).append(">> page(").append(entityClass).append(" ").append(classVar).append(", @RequestParam(defaultValue = \"1\") Integer pageNum, @RequestParam(defaultValue = \"10\") Integer pageSize) {\n");
        sb.append("        LambdaQueryWrapper<").append(entityClass).append("> wrapper = new LambdaQueryWrapper<>();\n");
        if (!queryFields.isEmpty()) {
            sb.append(queryFields).append("\n");
        }
        sb.append("        wrapper.orderByDesc(").append(entityClass).append("::getId);\n");
        sb.append("        return Result.success(").append(classVar).append("Service.page(new Page<>(pageNum, pageSize), wrapper));\n");
        sb.append("    }\n\n");

        // 详情
        sb.append("    @GetMapping(\"/{id}\")\n");
        sb.append("    @Operation(summary = \"查询" + (table.getTableComment() != null ? table.getTableComment() : "详情") + "\")\n");
        sb.append("    @RequirePermission(\"").append(perm).append(":list\")\n");
        sb.append("    public Result<").append(entityClass).append("> get(@PathVariable Long id) {\n");
        sb.append("        return Result.success(").append(classVar).append("Service.getById(id));\n");
        sb.append("    }\n\n");

        // 新增
        sb.append("    @PostMapping\n");
        sb.append("    @Operation(summary = \"新增" + (table.getTableComment() != null ? table.getTableComment() : "") + "\")\n");
        sb.append("    @RequirePermission(\"").append(perm).append(":add\")\n");
        sb.append("    @OperLog(title = \"").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append("\", businessType = 1)\n");
        sb.append("    public Result<Void> add(@RequestBody ").append(entityClass).append(" ").append(classVar).append(") {\n");
        sb.append("        ").append(classVar).append("Service.save(").append(classVar).append(");\n");
        sb.append("        return Result.success();\n");
        sb.append("    }\n\n");

        // 修改
        sb.append("    @PutMapping\n");
        sb.append("    @Operation(summary = \"修改" + (table.getTableComment() != null ? table.getTableComment() : "") + "\")\n");
        sb.append("    @RequirePermission(\"").append(perm).append(":edit\")\n");
        sb.append("    @OperLog(title = \"").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append("\", businessType = 2)\n");
        sb.append("    public Result<Void> update(@RequestBody ").append(entityClass).append(" ").append(classVar).append(") {\n");
        sb.append("        ").append(classVar).append("Service.updateById(").append(classVar).append(");\n");
        sb.append("        return Result.success();\n");
        sb.append("    }\n\n");

        // 删除
        sb.append("    @DeleteMapping(\"/{id}\")\n");
        sb.append("    @Operation(summary = \"删除" + (table.getTableComment() != null ? table.getTableComment() : "") + "\")\n");
        sb.append("    @RequirePermission(\"").append(perm).append(":delete\")\n");
        sb.append("    @OperLog(title = \"").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append("\", businessType = 3)\n");
        sb.append("    public Result<Void> delete(@PathVariable Long id) {\n");
        sb.append("        ").append(classVar).append("Service.removeById(id);\n");
        sb.append("        return Result.success();\n");
        sb.append("    }\n");

        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String generateVueCode(TableInfo table) {
        String classVar = table.getClassVar();
        String className = table.getClassName();
        String perm = table.getPermPrefix();

        // 收集列表显示的字段
        var listColumns = table.getColumns().stream()
                .filter(ColumnInfo::isList)
                .collect(Collectors.toList());

        // 收集可搜索字段
        var queryColumns = table.getColumns().stream()
                .filter(ColumnInfo::isQuery)
                .collect(Collectors.toList());

        // 收集可编辑字段
        var editColumns = table.getColumns().stream()
                .filter(c -> c.isInsert() || c.isEdit())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("<template>\n");
        sb.append("  <div class=\"page-container\">\n");
        sb.append("    <!-- 搜索栏 -->\n");
        sb.append("    <a-card class=\"mb-4\">\n");
        sb.append("      <a-form :model=\"searchForm\" layout=\"inline\" @submit-success=\"handleSearch\">\n");

        for (ColumnInfo col : queryColumns) {
            String field = col.getJavaField();
            if ("String".equals(col.getJavaType())) {
                sb.append("        <a-form-item field=\"").append(field).append("\" label=\"").append(col.getColumnComment()).append("\">\n");
                sb.append("          <a-input v-model=\"searchForm.").append(field).append("\" placeholder=\"请输入\" allow-clear style=\"width: 200px\" />\n");
                sb.append("        </a-form-item>\n");
            }
        }

        sb.append("        <a-form-item>\n");
        sb.append("          <a-space>\n");
        sb.append("            <a-button type=\"primary\" html-type=\"submit\">{{ $t('common.search') }}</a-button>\n");
        sb.append("            <a-button @click=\"handleReset\">{{ $t('common.reset') }}</a-button>\n");
        sb.append("          </a-space>\n");
        sb.append("        </a-form-item>\n");
        sb.append("      </a-form>\n");
        sb.append("    </a-card>\n\n");

        sb.append("    <!-- 表格 -->\n");
        sb.append("    <a-card>\n");
        sb.append("      <template #title>\n");
        sb.append("        <div class=\"flex items-center justify-between w-full\">\n");
        sb.append("          <span>").append(table.getTableComment() != null ? table.getTableComment() : table.getTableName()).append("</span>\n");
        sb.append("          <a-button v-permission=\"\\\"").append(perm).append(":add\\\"\" type=\"primary\" @click=\"openCreate\">\n");
        sb.append("            <template #icon><icon-plus /></template>\n");
        sb.append("            {{ $t('common.add') }}\n");
        sb.append("          </a-button>\n");
        sb.append("        </div>\n");
        sb.append("      </template>\n\n");

        sb.append("      <a-table :data=\"tableData\" :pagination=\"false\" row-key=\"id\" :loading=\"loading\">\n");
        sb.append("        <template #columns>\n");

        for (ColumnInfo col : listColumns) {
            String field = col.getJavaField();
            if (GenConstants.BASE_FIELDS.contains(field)) continue;
            String comment = col.getColumnComment() != null ? col.getColumnComment() : field;
            sb.append("          <a-table-column title=\"").append(comment).append("\" data-index=\"").append(field).append("\"");
            if ("LocalDateTime".equals(col.getJavaType().replace("java.time.", ""))) {
                sb.append(" :width=\"170\"");
            }
            sb.append(" />\n");
        }

        sb.append("          <a-table-column title=\"{{ $t('common.operate') }}\" :width=\"200\" align=\"right\">\n");
        sb.append("            <template #cell=\"{ record }\">\n");
        sb.append("              <a-space>\n");
        sb.append("                <a-button size=\"small\" @click=\"openEdit(record)\">{{ $t('common.edit') }}</a-button>\n");
        sb.append("                <a-popconfirm :content=\"$t('common.deleteConfirm')\" @ok=\"handleDelete(record)\">\n");
        sb.append("                  <a-button v-permission=\"\\\"").append(perm).append(":delete\\\"\" size=\"small\" status=\"danger\">{{ $t('common.delete') }}</a-button>\n");
        sb.append("                </a-popconfirm>\n");
        sb.append("              </a-space>\n");
        sb.append("            </template>\n");
        sb.append("          </a-table-column>\n");
        sb.append("        </template>\n");
        sb.append("      </a-table>\n\n");

        sb.append("      <div v-if=\"pagination.total > 0\" class=\"mt-4 flex justify-end\">\n");
        sb.append("        <a-pagination v-model:current=\"pagination.current\" :total=\"pagination.total\" :page-size=\"pagination.pageSize\" @change=\"loadData\" />\n");
        sb.append("      </div>\n");
        sb.append("    </a-card>\n\n");

        sb.append("    <!-- 新增/编辑弹窗 -->\n");
        sb.append("    <a-modal v-model:visible=\"formVisible\" :title=\"formTitle\" @ok=\"handleSubmit\" @cancel=\"formVisible = false\">\n");
        sb.append("      <a-form :model=\"form\" auto-label-width>\n");
        for (ColumnInfo col : editColumns) {
            String field = col.getJavaField();
            if (GenConstants.BASE_FIELDS.contains(field)) continue;
            String comment = col.getColumnComment() != null ? col.getColumnComment() : field;
            String htmlType = col.getHtmlType();

            sb.append("        <a-form-item field=\"").append(field).append("\" label=\"").append(comment).append("\"");
            if (col.isRequired()) {
                sb.append(" required");
            }
            sb.append(">\n");

            if ("datetime".equals(htmlType)) {
                sb.append("          <a-date-picker v-model=\"form.").append(field).append("\" show-time style=\"width: 100%\" />\n");
            } else if ("textarea".equals(htmlType) || "editor".equals(htmlType)) {
                sb.append("          <a-textarea v-model=\"form.").append(field).append("\" placeholder=\"请输入").append(comment).append("\" />\n");
            } else {
                sb.append("          <a-input v-model=\"form.").append(field).append("\" placeholder=\"请输入").append(comment).append("\" />\n");
            }
            sb.append("        </a-form-item>\n");
        }
        sb.append("      </a-form>\n");
        sb.append("    </a-modal>\n");
        sb.append("  </div>\n");
        sb.append("</template>\n\n");

        sb.append("<script setup lang=\"ts\">\n");
        sb.append("import { ref, reactive, computed, onMounted } from 'vue'\n");
        sb.append("import { Message } from '@arco-design/web-vue'\n");
        sb.append("import { useI18n } from 'vue-i18n'\n\n");
        sb.append("const { t } = useI18n()\n\n");

        sb.append("const loading = ref(false)\n");
        sb.append("const tableData = ref<any[]>([])\n");
        sb.append("const pagination = reactive({ current: 1, pageSize: 10, total: 0 })\n");
        sb.append("const searchForm = reactive({})\n\n");

        sb.append("/* 数据加载 */\n");
        sb.append("onMounted(() => loadData())\n\n");
        sb.append("async function loadData() {\n");
        sb.append("  loading.value = true\n");
        sb.append("  try {\n");
        sb.append("    const res = await getPageApi({ ...searchForm, pageNum: pagination.current, pageSize: pagination.pageSize })\n");
        sb.append("    tableData.value = res.data.records || []\n");
        sb.append("    pagination.total = res.data.total\n");
        sb.append("  } finally {\n");
        sb.append("    loading.value = false\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        sb.append("function handleSearch() { pagination.current = 1; loadData() }\n");
        sb.append("function handleReset() { Object.keys(searchForm).forEach(k => delete searchForm[k]); pagination.current = 1; loadData() }\n\n");

        sb.append("/* 新增/编辑 */\n");
        sb.append("const formVisible = ref(false)\n");
        sb.append("const form = reactive<Record<string, any>>({})\n");
        sb.append("const editingId = ref<number | null>(null)\n");
        sb.append("const formTitle = computed(() => editingId.value ? '编辑' : '新增')\n\n");

        sb.append("function openCreate() {\n");
        sb.append("  editingId.value = null\n");
        sb.append("  Object.keys(form).forEach(k => delete form[k])\n");
        sb.append("  formVisible.value = true\n");
        sb.append("}\n\n");

        sb.append("function openEdit(record: any) {\n");
        sb.append("  editingId.value = record.id\n");
        sb.append("  Object.assign(form, record)\n");
        sb.append("  formVisible.value = true\n");
        sb.append("}\n\n");

        sb.append("async function handleSubmit() {\n");
        sb.append("  if (editingId.value) {\n");
        sb.append("    await updateApi(form)\n");
        sb.append("  } else {\n");
        sb.append("    await addApi(form)\n");
        sb.append("  }\n");
        sb.append("  formVisible.value = false\n");
        sb.append("  Message.success(t('common.success'))\n");
        sb.append("  loadData()\n");
        sb.append("}\n\n");

        sb.append("async function handleDelete(record: any) {\n");
        sb.append("  await deleteApi(record.id)\n");
        sb.append("  Message.success(t('common.success'))\n");
        sb.append("  loadData()\n");
        sb.append("}\n");
        sb.append("</script>\n");

        return sb.toString();
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
}