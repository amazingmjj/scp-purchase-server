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
import org.xy.api.dto.purchase.PurchaseShipDetailDto;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchaseContractDetail;
import org.zhd.purchase.entity.PurchaseShipDetail;
import org.zhd.purchase.mapper.PurchaseShipDetailMapper;
import org.xy.api.utils.DaoUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
* 采购发货集港明细业务层
*
* @author samy
*/
@Service
public class PurchaseShipDetailService implements BaseService<PurchaseShipDetailDto, Long> {

    @Autowired
    private PurchaseShipDetailMapper purchaseShipDetailMapper;
    @Autowired
    private PurchaseShipService purchaseShipService;
    @Autowired
    private PurchaseContractDetailService purchaseContractDetailService;
    @Autowired
    private CommMapper commMapper;

    @Override
    public PurchaseShipDetailDto saveOrUpdate(PurchaseShipDetailDto model) throws Exception {
        if (model.getParentCode() == null) {
            throw new Exception("采购合同编号不能为空");
        }
        PurchaseShipDetail obj = new PurchaseShipDetail();
        BeanUtils.copyProperties(model, obj);
        obj.setExecCount(obj.getCount().intValue());
        obj.setExecWeight(obj.getWeight());
        obj.setExecAssistWeight(obj.getAssistWeight());
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_ship_detail");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("OByyyymmdd4", maxCode == null ? "0001" : maxCode);
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P006");
            obj.setFinishBill(1);
            obj.setFinishGoods(1);
            obj.setFinishPay(1);
            purchaseShipDetailMapper.insert(obj);
        } else {
            // 更新
            purchaseShipDetailMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchaseShipDetailDto> selectPage(Map<String, Object> params) throws Exception {
        // FIXME DO SELECTPAGE ACTION
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchaseShipDetail> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchaseShipDetail.class);
        IPage<PurchaseShipDetail> pages = purchaseShipDetailMapper.selectPage((Page<PurchaseShipDetail>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchaseShipDetailDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseShipDetailDto>(list, (int) pages.getTotal());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        purchaseShipDetailMapper.deleteBatchIds(ids);
    }

    @Override
    public PurchaseShipDetailDto selectById(Long id) throws Exception {
        PurchaseShipDetail model = purchaseShipDetailMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchaseShipDetailDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchaseShipDetailDto dto = new PurchaseShipDetailDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    /**
     * 根据父类编号删除
     *
     * @param parentCode
     */
    public int delete(String parentCode) {
        QueryWrapper<PurchaseShipDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        return purchaseShipDetailMapper.delete(qw);
    }

    public BaseListDto<PurchaseShipDetailDto> selectByParentCode(String parentCode) {
        QueryWrapper<PurchaseShipDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        List<PurchaseShipDetailDto> list = purchaseShipDetailMapper.selectList(qw).stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseShipDetailDto>(list, list.size());
    }

    /**
     * 回写上级
     * @param detailList
     * @return
     * @throws Exception
     */
    public int updateBackContract(List<PurchaseShipDetailDto> detailList) throws Exception {
        List<PurchaseContractDetail> backList = new ArrayList<>();
        for (PurchaseShipDetailDto detailDto : detailList) {
            if (StringUtil.isNotBlank(detailDto.getOuterCode())&&detailDto.getOuterCode().startsWith("CB")){
                PurchaseContractDetail backObj = new PurchaseContractDetail();
                backObj.setCode(detailDto.getOuterCode());
                backObj.setParentCode(detailDto.getOuterParentCode());
                if (detailDto.getDataFlag() != null && detailDto.getDataFlag() == 0) {
                    //新增则数量全部回写
                    backObj.setChangeNum(detailDto.getCount());
                    backObj.setChangeWeight(detailDto.getWeight());
                    backObj.setChangeAssistWeight(detailDto.getAssistWeight());
                }else if(detailDto.getDataFlag() != null && detailDto.getDataFlag() == 2){
                    //删除则数量全部撤销
                    backObj.setChangeNum(-detailDto.getCount());
                    backObj.setChangeWeight(-detailDto.getWeight());
                    backObj.setChangeAssistWeight(-detailDto.getAssistWeight());
                }else {
                    PurchaseShipDetail oldDetail = purchaseShipDetailMapper.selectById(detailDto.getId());
                    backObj.setChangeNum(detailDto.getCount()-oldDetail.getCount());
                    backObj.setChangeWeight(detailDto.getWeight()-oldDetail.getWeight());
                    backObj.setChangeAssistWeight(detailDto.getAssistWeight()-oldDetail.getAssistWeight());
                }
                backList.add(backObj);
            }
        }

        return purchaseContractDetailService.updateBackExec(backList);
    }

    /**
     * 回写未执行量接口
     * @param changeDtoList
     * @return
     * @throws Exception
     */
    public int updateBackExecInterface(List<PurchaseShipDetailDto> changeDtoList) throws Exception{
        List<PurchaseShipDetail> detailList = new ArrayList<>();
        for (PurchaseShipDetailDto thisDto : changeDtoList) {
            PurchaseShipDetail retDetail = new PurchaseShipDetail();
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
    public int updateBackExec(List<PurchaseShipDetail> changeList) throws Exception {
        //回写明细
        Set<String> mainSet = new TreeSet<>();
        for (PurchaseShipDetail thisDetail:changeList){
            int result = updateBackDetailExec(thisDetail);
            if (result<0){
                throw new Exception("回写明细"+thisDetail.getCode()+"失败！");
            }

            //按主表分组
            mainSet.add(thisDetail.getParentCode());
        }

        String errorCode = purchaseShipService.updateBackMainExec(mainSet);
        if (StringUtil.isNotBlank(errorCode)){
            throw new Exception("回写主表"+errorCode+"失败！");
        }

        return 1;
    }

    /**
     * 回写明细未执行量
     * @param shipDetail
     * @return
     */
    private int updateBackDetailExec(PurchaseShipDetail shipDetail){
        QueryWrapper<PurchaseShipDetail> qw = new QueryWrapper<>();
        qw.eq("code",shipDetail.getCode());
        qw.eq("parent_code",shipDetail.getParentCode());
        PurchaseShipDetail oldShipDetail = purchaseShipDetailMapper.selectOne(qw);

        PurchaseShipDetail obj = new PurchaseShipDetail();
        obj.setExecCount(new BigDecimal(oldShipDetail.getExecCount())
                .subtract(new BigDecimal(shipDetail.getChangeNum())).setScale(0,4).intValue());
        obj.setExecWeight(new BigDecimal(oldShipDetail.getExecWeight())
                .subtract(new BigDecimal(shipDetail.getChangeWeight())).setScale(3,4).doubleValue());
        obj.setExecAssistWeight(new BigDecimal(oldShipDetail.getExecAssistWeight())
                .subtract(new BigDecimal(shipDetail.getChangeAssistWeight())).setScale(3,4).doubleValue());

        UpdateWrapper<PurchaseShipDetail> uw = new UpdateWrapper<>();
        uw.eq("code",shipDetail.getCode());
        uw.eq("parent_code",shipDetail.getParentCode());
        return purchaseShipDetailMapper.update(obj,uw);
    }
}