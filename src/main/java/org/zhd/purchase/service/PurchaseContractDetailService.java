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
import org.xy.api.dto.purchase.PurchaseContractDetailDto;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchaseContractDetail;
import org.zhd.purchase.entity.PurchasePayApplyDetail;
import org.zhd.purchase.mapper.PurchaseContractDetailMapper;
import org.xy.api.utils.DaoUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购合同明细业务层
 *
 * @author samy
 */
@Service
public class PurchaseContractDetailService implements BaseService<PurchaseContractDetailDto, Long> {

    @Autowired
    private PurchaseContractDetailMapper purchaseContractDetailMapper;
    @Autowired
    private PurchaseContractService purchaseContractService;
    @Autowired
    private PurchasePayApplyDetailService purchasePayApplyDetailService;
    @Autowired
    private CommMapper commMapper;

    @Override
    public PurchaseContractDetailDto saveOrUpdate(PurchaseContractDetailDto model) throws Exception {
        if (model.getParentCode() == null) {
            throw new Exception("采购合同编号不能为空");
        }
        PurchaseContractDetail obj = new PurchaseContractDetail();
        BeanUtils.copyProperties(model, obj);
        obj.setExecCount(obj.getCount().intValue());
        obj.setExecWeight(obj.getWeight());
        obj.setExecAssistWeight(obj.getAssistWeight());
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_contract_detail");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("CByyyymmdd4", maxCode == null ? "0001" : maxCode);
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P005");
            obj.setFinishBill(1);
            obj.setFinishGoods(1);
            obj.setFinishPay(1);
            purchaseContractDetailMapper.insert(obj);
        } else {
            // 更新
            purchaseContractDetailMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchaseContractDetailDto> selectPage(Map<String, Object> params) throws Exception {
        // FIXME DO SELECTPAGE ACTION
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchaseContractDetail> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchaseContractDetail.class);
        IPage<PurchaseContractDetail> pages = purchaseContractDetailMapper.selectPage((Page<PurchaseContractDetail>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchaseContractDetailDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseContractDetailDto>(list, (int) pages.getTotal());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        purchaseContractDetailMapper.deleteBatchIds(ids);
    }

    @Override
    public PurchaseContractDetailDto selectById(Long id) throws Exception {
        PurchaseContractDetail model = purchaseContractDetailMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchaseContractDetailDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchaseContractDetailDto dto = new PurchaseContractDetailDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    /**
     * 根据父类编号删除
     *
     * @param parentCode
     */
    public int delete(String parentCode) {
        QueryWrapper<PurchaseContractDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        return purchaseContractDetailMapper.delete(qw);
    }

    public BaseListDto<PurchaseContractDetailDto> selectByParentCode(String parentCode) {
        QueryWrapper<PurchaseContractDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        List<PurchaseContractDetailDto> list = purchaseContractDetailMapper.selectList(qw).stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseContractDetailDto>(list, list.size());
    }

    /**
     * 回写上级
     * @param detailList
     * @return
     * @throws Exception
     */
    public int updateBackPayApply(List<PurchaseContractDetailDto> detailList) throws Exception {
        List<PurchasePayApplyDetail> backList = new ArrayList<>();
        for (PurchaseContractDetailDto detailDto : detailList) {
            if (StringUtil.isNotBlank(detailDto.getOuterCode())&&detailDto.getOuterCode().startsWith("PQPAB")){
                PurchasePayApplyDetail backObj = new PurchasePayApplyDetail();
                backObj.setCode(detailDto.getOuterCode());
                backObj.setParentCode(detailDto.getOuterParentCode());
                if (detailDto.getDataFlag() != null && detailDto.getDataFlag() == 0) {
                    //新增则数量全部回写
                    backObj.setChangeNum(detailDto.getCount());
                    backObj.setChangeWeight(detailDto.getWeight());
                }else if(detailDto.getDataFlag() != null && detailDto.getDataFlag() == 2){
                    //删除则数量全部撤销
                    backObj.setChangeNum(-detailDto.getCount());
                    backObj.setChangeWeight(-detailDto.getWeight());
                }else {
                    PurchaseContractDetail oldDetail = purchaseContractDetailMapper.selectById(detailDto.getId());
                    backObj.setChangeNum(detailDto.getCount()-oldDetail.getCount());
                    backObj.setChangeWeight(detailDto.getWeight()-oldDetail.getWeight());
                }
                backList.add(backObj);
            }
        }

        return purchasePayApplyDetailService.updateBackExec(backList);
    }

    /**
     * 回写未执行量接口
     * @param changeDtoList
     * @return
     * @throws Exception
     */
    public int updateBackExecInterface(List<PurchaseContractDetailDto> changeDtoList) throws Exception{
        List<PurchaseContractDetail> detailList = new ArrayList<>();
        for (PurchaseContractDetailDto thisDto : changeDtoList) {
            PurchaseContractDetail retDetail = new PurchaseContractDetail();
            BeanUtils.copyProperties(thisDto,retDetail);
            detailList.add(retDetail);
        }
        return updateBackExec(detailList);
    }


    /**
     * 回写未执行量
     * @param changeList
     * @return
     */
    public int updateBackExec(List<PurchaseContractDetail> changeList) throws Exception {
        //回写明细
        Set<String> mainSet = new TreeSet<>();
        for (PurchaseContractDetail thisDetail:changeList){
            int result = updateBackDetailExec(thisDetail);
            if (result<0){
                throw new Exception("回写明细"+thisDetail.getCode()+"失败！");
            }

            //按主表分组
            mainSet.add(thisDetail.getParentCode());
        }

        String errorCode = purchaseContractService.updateBackMainExec(mainSet);
        if (StringUtil.isNotBlank(errorCode)){
            throw new Exception("回写主表"+errorCode+"失败！");
        }

        return 1;
    }

    /**
     * 回写明细未执行量
     * @param contractDetail
     * @return
     */
    private int updateBackDetailExec(PurchaseContractDetail contractDetail){
        QueryWrapper<PurchaseContractDetail> qw = new QueryWrapper<>();
        qw.eq("code",contractDetail.getCode());
        qw.eq("parent_code",contractDetail.getParentCode());
        PurchaseContractDetail oldContractDetail = purchaseContractDetailMapper.selectOne(qw);

        PurchaseContractDetail obj = new PurchaseContractDetail();
        obj.setExecCount(new BigDecimal(oldContractDetail.getExecCount())
                .subtract(new BigDecimal(contractDetail.getChangeNum())).setScale(0,4).intValue());
        obj.setExecWeight(new BigDecimal(oldContractDetail.getExecWeight())
                .subtract(new BigDecimal(contractDetail.getChangeWeight())).setScale(3,4).doubleValue());
        obj.setExecAssistWeight(new BigDecimal(oldContractDetail.getExecAssistWeight())
                .subtract(new BigDecimal(contractDetail.getChangeAssistWeight())).setScale(3,4).doubleValue());

        UpdateWrapper<PurchaseContractDetail> uw = new UpdateWrapper<>();
        uw.eq("code",contractDetail.getCode());
        uw.eq("parent_code",contractDetail.getParentCode());
        return purchaseContractDetailMapper.update(obj,uw);
    }
}