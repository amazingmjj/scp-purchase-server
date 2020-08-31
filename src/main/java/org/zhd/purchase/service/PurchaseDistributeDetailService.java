package org.zhd.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xy.api.dpi.BaseService;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseDistributeDetailDto;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.*;
import org.zhd.purchase.mapper.PurchaseDistributeDetailMapper;
import org.xy.api.utils.DaoUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购配货明细业务层
 *
 * @author samy
 */
@Service
public class PurchaseDistributeDetailService implements BaseService<PurchaseDistributeDetailDto, Long> {

    @Autowired
    private PurchaseDistributeDetailMapper purchaseDistributeDetailMapper;

    @Autowired
    private PurchaseDistributeService purchaseDistributeService;

    @Autowired
    private CommMapper commMapper;

    @Override
    public PurchaseDistributeDetailDto saveOrUpdate(PurchaseDistributeDetailDto model) throws Exception {
        if (model.getParentCode() == null) {
            throw new Exception("采购配货编号不能为空");
        }
        PurchaseDistributeDetail obj = new PurchaseDistributeDetail();
        BeanUtils.copyProperties(model, obj);
        obj.setExecCount(obj.getCount().intValue());
        obj.setExecWeight(obj.getWeight());
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_Distribute_detail");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("PDByyyymmdd4", maxCode == null ? "0001" : maxCode);
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P002");
            purchaseDistributeDetailMapper.insert(obj);
        } else {
            // 更新
            purchaseDistributeDetailMapper.updateById(obj);
        }
        PurchaseDistributeDetailDto dto = new PurchaseDistributeDetailDto();
        BeanUtils.copyProperties(obj, dto);
        return dto;
    }

    @Override
    public BaseListDto<PurchaseDistributeDetailDto> selectPage(Map<String, Object> params) throws Exception {
        // FIXME DO SELECTPAGE ACTION
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchaseDistributeDetail> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchaseDistributeDetail.class);
        IPage<PurchaseDistributeDetail> pages = purchaseDistributeDetailMapper.selectPage((Page<PurchaseDistributeDetail>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchaseDistributeDetailDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseDistributeDetailDto>(list, (int) pages.getTotal());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        purchaseDistributeDetailMapper.deleteBatchIds(ids);
    }

    @Override
    public PurchaseDistributeDetailDto selectById(Long id) throws Exception {
        PurchaseDistributeDetail model = purchaseDistributeDetailMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchaseDistributeDetailDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchaseDistributeDetailDto dto = new PurchaseDistributeDetailDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    /**
     * 根据父类编号删除
     *
     * @param parentCode
     */
    public int delete(String parentCode) {
        QueryWrapper<PurchaseDistributeDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        return purchaseDistributeDetailMapper.delete(qw);
    }

    public BaseListDto<PurchaseDistributeDetailDto> selectByParentCode(String parentCode) {
        QueryWrapper<PurchaseDistributeDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        List<PurchaseDistributeDetailDto> list = purchaseDistributeDetailMapper.selectList(qw).stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseDistributeDetailDto>(list, list.size());
    }


    /**
     * 回写未执行量
     * @param changeList
     * @return
     */
    public int updateBackExec(List<PurchaseDistributeDetail> changeList) throws Exception {
        //回写明细
        Set<String> mainSet = new TreeSet<>();
        for (PurchaseDistributeDetail thisDetail:changeList){
            int result = updateBackDetailExec(thisDetail);
            if (result<0){
                throw new Exception("回写明细"+thisDetail.getCode()+"失败！");
            }

            //按主表分组
            mainSet.add(thisDetail.getParentCode());
        }

        String errorCode = purchaseDistributeService.updateBackMainExec(mainSet);
        if (StringUtil.isNotBlank(errorCode)){
            throw new Exception("回写主表"+errorCode+"失败！");
        }

        return 1;
    }

    /**
     * 回写明细未执行量
     * @param distributeDetail
     * @return
     */
    private int updateBackDetailExec(PurchaseDistributeDetail distributeDetail){
        QueryWrapper<PurchaseDistributeDetail> qw = new QueryWrapper<>();
        qw.eq("code",distributeDetail.getCode());
        qw.eq("parent_code",distributeDetail.getParentCode());
        PurchaseDistributeDetail oldDistributeDetail = purchaseDistributeDetailMapper.selectOne(qw);

        PurchaseDistributeDetail obj = new PurchaseDistributeDetail();
        obj.setExecCount(new BigDecimal(oldDistributeDetail.getExecCount())
                .subtract(new BigDecimal(distributeDetail.getChangeNum())).setScale(0,4).intValue());
        obj.setExecWeight(new BigDecimal(oldDistributeDetail.getExecWeight())
                .subtract(new BigDecimal(distributeDetail.getChangeWeight())).setScale(3,4).doubleValue());

        UpdateWrapper<PurchaseDistributeDetail> uw = new UpdateWrapper<>();
        uw.eq("code",distributeDetail.getCode());
        uw.eq("parent_code",distributeDetail.getParentCode());
        return purchaseDistributeDetailMapper.update(obj,uw);
    }


}