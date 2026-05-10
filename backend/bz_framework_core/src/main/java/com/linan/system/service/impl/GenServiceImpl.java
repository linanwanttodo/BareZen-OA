package com.linan.system.service.impl;

import com.linan.core.gen.TableInfo;
import com.linan.system.service.IGenCodeGenerationService;
import com.linan.system.service.IGenService;
import com.linan.system.service.IGenTableQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 代码生成服务实现（门面模式）
 *
 * <p>委托给专门的服务处理不同职责，保持本类的简洁性。</p>
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class GenServiceImpl implements IGenService {

    private final IGenTableQueryService tableQueryService;
    private final IGenCodeGenerationService codeGenerationService;

    @Override
    public List<TableInfo> selectTableList(String tableName) {
        return tableQueryService.selectTableList(tableName);
    }

    @Override
    public TableInfo selectTableByName(String tableName) {
        return tableQueryService.selectTableByName(tableName);
    }

    @Override
    public Map<String, String> generateCode(String tableName) {
        return codeGenerationService.generateCode(tableName);
    }
}