package org.zhd.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xy.api.dpi.BaseService;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchasePlanDetailDto;
import org.xy.api.mapper.CommMapper;
import org.zhd.purchase.entity.PurchasePlanDetail;
import org.zhd.purchase.mapper.PurchasePlanDetailMapper;
import org.xy.api.utils.DaoUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购计划明细业务层
 *
 * @author samy
 */
@Service
public class PurchasePlanDetailService implements BaseService<PurchasePlanDetailDto, Long> {

    @Autowired
    private PurchasePlanDetailMapper purchasePlanDetailMapper;
    @Autowired
    private CommMapper commMapper;

    @Override
    public PurchasePlanDetailDto saveOrUpdate(PurchasePlanDetailDto model) throws Exception {
        if (model.getParentCode() == null) {
            throw new Exception("采购计划编号不能为空");
        }
        PurchasePlanDetail obj = new PurchasePlanDetail();
        BeanUtils.copyProperties(model, obj);
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_plan_detail");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("PQDyyyymmdd4", maxCode == null ? "0001" : maxCode);
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P001");
            purchasePlanDetailMapper.insert(obj);
        } else {
            // 更新
            purchasePlanDetailMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchasePlanDetailDto> selectPage(Map<String, Object> params) throws Exception {
        // FIXME DO SELECTPAGE ACTION
        int currentPage = (int) params.getOrDefault("currentPage", 1);
        int pageSize = (int) params.getOrDefault("pageSize", 10);
        QueryWrapper<PurchasePlanDetail> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchasePlanDetail.class);
        IPage<PurchasePlanDetail> pages = purchasePlanDetailMapper.selectPage((Page<PurchasePlanDetail>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchasePlanDetailDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchasePlanDetailDto>(list, (int) pages.getTotal());
    }

    public BaseListDto<PurchasePlanDetailDto> selectByParentCode(String parentCode) {
        QueryWrapper<PurchasePlanDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        List<PurchasePlanDetailDto> list = purchasePlanDetailMapper.selectList(qw).stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchasePlanDetailDto>(list, list.size());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        purchasePlanDetailMapper.deleteBatchIds(ids);
    }

    @Override
    public PurchasePlanDetailDto selectById(Long id) throws Exception {
        PurchasePlanDetail model = purchasePlanDetailMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchasePlanDetailDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchasePlanDetailDto dto = new PurchasePlanDetailDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    /**
     * 根据父类编号删除
     *
     * @param parentCode
     */
    public int delete(String parentCode) {
        QueryWrapper<PurchasePlanDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        return purchasePlanDetailMapper.delete(qw);
    }
}