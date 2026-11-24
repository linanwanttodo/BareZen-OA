package com.linan.plugin.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.plugin.masterdata.entity.MaterialInfo;
import com.linan.plugin.masterdata.mapper.MaterialInfoMapper;
import com.linan.plugin.masterdata.service.IMaterialInfoService;
import org.springframework.stereotype.Service;

/**
 * 物料信息服务实现
 *
 * @author BareZen Team
 */
@Service
public class MaterialInfoServiceImpl extends ServiceImpl<MaterialInfoMapper, MaterialInfo>
        implements IMaterialInfoService {

    @Override
    public MaterialInfo createMaterial(MaterialInfo material) {
        validate(material);
        if (existsByCode(material.getMaterialCode(), null)) {
            throw new BusinessException("物料编码已存在: " + material.getMaterialCode());
        }
        if (material.getStatus() == null || material.getStatus().isBlank()) {
            material.setStatus("0");
        }
        save(material);
        return material;
    }

    @Override
    public MaterialInfo updateMaterial(MaterialInfo material) {
        if (material.getId() == null) {
            throw new BusinessException("物料ID不能为空");
        }
        if (getById(material.getId()) == null) {
            throw new BusinessException("物料不存在");
        }
        validate(material);
        if (existsByCode(material.getMaterialCode(), material.getId())) {
            throw new BusinessException("物料编码已存在: " + material.getMaterialCode());
        }
        updateById(material);
        return material;
    }

    private void validate(MaterialInfo material) {
        if (material.getMaterialCode() == null || material.getMaterialCode().isBlank()) {
            throw new BusinessException("物料编码不能为空");
        }
        if (material.getMaterialName() == null || material.getMaterialName().isBlank()) {
            throw new BusinessException("物料名称不能为空");
        }
    }

    private boolean existsByCode(String code, Long excludeId) {
        return count(new LambdaQueryWrapper<MaterialInfo>()
                .eq(MaterialInfo::getMaterialCode, code)
                .ne(excludeId != null, MaterialInfo::getId, excludeId)) > 0;
    }
}
