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
import org.xy.api.dto.purchase.PurchasePayApplyDetailDto;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchasePayApply;
import org.zhd.purchase.entity.PurchasePayApplyDetail;
import org.zhd.purchase.entity.PurchaseQueryDetail;
import org.zhd.purchase.mapper.PurchasePayApplyDetailMapper;
import org.xy.api.utils.DaoUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购付款申请明细业务层
 *
 * @author samy
 */
@Service
public class PurchasePayApplyDetailService implements BaseService<PurchasePayApplyDetailDto, Long> {

    @Autowired
    private PurchasePayApplyDetailMapper purchasePayApplyDetailMapper;
    @Autowired
    private PurchasePayApplyService purchasePayApplyService;
    @Autowired
    private PurchaseQueryDetailService purchaseQueryDetailService;
    @Autowired
    private CommMapper commMapper;

    @Override
    public PurchasePayApplyDetailDto saveOrUpdate(PurchasePayApplyDetailDto model) throws Exception {
        // FIXME DO SAVE ACTION
        if (model.getParentCode() == null) {
            throw new Exception("采购付款申请编号不能为空");
        }
        PurchasePayApplyDetail obj = new PurchasePayApplyDetail();
        BeanUtils.copyProperties(model, obj);
        obj.setExecCount(obj.getCount());
        obj.setExecWeight(obj.getWeight());
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_pay_apply_detail");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("PQPAByyyymmdd4", maxCode == null ? "0001" : maxCode);
            obj.setCode(uniqueCode);
            // FIXME 修改成正确的值
            obj.setBillTypeCode("P004");
            purchasePayApplyDetailMapper.insert(obj);
        } else {
            // 更新
            purchasePayApplyDetailMapper.updateById(obj);
        }
        PurchasePayApplyDetailDto dto = new PurchasePayApplyDetailDto();
        BeanUtils.copyProperties(obj, dto);
        return dto;
    }

    @Override
    public BaseListDto<PurchasePayApplyDetailDto> selectPage(Map<String, Object> params) throws Exception {
        // FIXME DO SELECTPAGE ACTION
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchasePayApplyDetail> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchasePayApplyDetail.class);
        IPage<PurchasePayApplyDetail> pages = purchasePayApplyDetailMapper.selectPage((Page<PurchasePayApplyDetail>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchasePayApplyDetailDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchasePayApplyDetailDto>(list, (int) pages.getTotal());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        purchasePayApplyDetailMapper.deleteBatchIds(ids);
    }

    @Override
    public PurchasePayApplyDetailDto selectById(Long id) throws Exception {
        PurchasePayApplyDetail model = purchasePayApplyDetailMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchasePayApplyDetailDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchasePayApplyDetailDto dto = new PurchasePayApplyDetailDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    /**
     * 根据父类编号删除
     *
     * @param parentCode
     */
    public int delete(String parentCode) {
        QueryWrapper<PurchasePayApplyDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        return purchasePayApplyDetailMapper.delete(qw);
    }

    public BaseListDto<PurchasePayApplyDetailDto> selectByParentCode(String parentCode) {
        QueryWrapper<PurchasePayApplyDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        List<PurchasePayApplyDetailDto> list = purchasePayApplyDetailMapper.selectList(qw).stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchasePayApplyDetailDto>(list, list.size());
    }

    /**
     * 回写上级
     * @param detailList
     * @return
     * @throws Exception
     */
    public int updateBackQuery(List<PurchasePayApplyDetailDto> detailList) throws Exception {
        List<PurchaseQueryDetail> backList = new ArrayList<>();
        for (PurchasePayApplyDetailDto detailDto : detailList) {
            if (StringUtil.isNotBlank(detailDto.getOuterCode())){
                PurchaseQueryDetail backObj = new PurchaseQueryDetail();
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
                    PurchasePayApplyDetail oldDetail = purchasePayApplyDetailMapper.selectById(detailDto.getId());
                    backObj.setChangeNum(detailDto.getCount()-oldDetail.getCount());
                    backObj.setChangeWeight(detailDto.getWeight()-oldDetail.getWeight());
                }
                backList.add(backObj);
            }
        }

        return purchaseQueryDetailService.updateBackExec(backList);
    }

    /**
     * 回写未执行量
     * @param changeList
     * @return
     */
    public int updateBackExec(List<PurchasePayApplyDetail> changeList) throws Exception {
        //回写明细
        Set<String> mainSet = new TreeSet<>();
        for (PurchasePayApplyDetail thisDetail:changeList){
            int result = updateBackDetailExec(thisDetail);
            if (result<0){
                throw new Exception("回写明细"+thisDetail.getCode()+"失败！");
            }

            //按主表分组
            mainSet.add(thisDetail.getParentCode());
        }

        String errorCode = purchasePayApplyService.updateBackMainExec(mainSet);
        if (StringUtil.isNotBlank(errorCode)){
            throw new Exception("回写主表"+errorCode+"失败！");
        }

        return 1;
    }

    /**
     * 回写明细未执行量
     * @param payApplyDetail
     * @return
     */
    private int updateBackDetailExec(PurchasePayApplyDetail payApplyDetail){
        QueryWrapper<PurchasePayApplyDetail> qw = new QueryWrapper<>();
        qw.eq("code",payApplyDetail.getCode());
        qw.eq("parent_code",payApplyDetail.getParentCode());
        PurchasePayApplyDetail oldPayApplyDetail = purchasePayApplyDetailMapper.selectOne(qw);

        PurchasePayApplyDetail obj = new PurchasePayApplyDetail();
        obj.setExecCount(new BigDecimal(oldPayApplyDetail.getExecCount())
                .subtract(new BigDecimal(payApplyDetail.getChangeNum())).setScale(0,4).intValue());
        obj.setExecWeight(new BigDecimal(oldPayApplyDetail.getExecWeight())
                .subtract(new BigDecimal(payApplyDetail.getChangeWeight())).setScale(3,4).doubleValue());

        UpdateWrapper<PurchasePayApplyDetail> uw = new UpdateWrapper<>();
        uw.eq("code",payApplyDetail.getCode());
        uw.eq("parent_code",payApplyDetail.getParentCode());
        return purchasePayApplyDetailMapper.update(obj,uw);
    }


}