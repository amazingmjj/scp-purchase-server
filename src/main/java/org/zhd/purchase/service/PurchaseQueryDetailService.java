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
import org.xy.api.dto.purchase.PurchaseQueryDetailDto;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.*;
import org.zhd.purchase.entity.PurchaseQueryDetail;
import org.zhd.purchase.mapper.PurchaseQueryDetailMapper;
import org.xy.api.utils.DaoUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购询价明细业务层
 *
 * @author samy
 */
@Service
public class PurchaseQueryDetailService implements BaseService<PurchaseQueryDetailDto, Long> {

    @Autowired
    private PurchaseQueryDetailMapper purchaseQueryDetailMapper;

    @Autowired
    private PurchaseQueryService purchaseQueryService;

    @Autowired
    private PurchaseDistributeDetailService purchaseDistributeDetailService;

    @Autowired
    private CommMapper commMapper;

    @Override
    public PurchaseQueryDetailDto saveOrUpdate(PurchaseQueryDetailDto model) throws Exception {
        if (model.getParentCode() == null) {
            throw new Exception("采购询价编号不能为空");
        }
        PurchaseQueryDetail obj = new PurchaseQueryDetail();
        BeanUtils.copyProperties(model, obj);
        obj.setExecCount(obj.getCount());
        obj.setExecWeight(obj.getWeight());
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_query_detail");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("PQDByyyymmdd4", maxCode == null ? "0001" : maxCode);
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P003");
            purchaseQueryDetailMapper.insert(obj);
        } else {
            // 更新
            purchaseQueryDetailMapper.updateById(obj);
        }
        PurchaseQueryDetailDto dto = new PurchaseQueryDetailDto();
        BeanUtils.copyProperties(obj, dto);
        return dto;
    }

    @Override
    public BaseListDto<PurchaseQueryDetailDto> selectPage(Map<String, Object> params) throws Exception {
        // FIXME DO SELECTPAGE ACTION
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchaseQueryDetail> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchaseQueryDetail.class);
        IPage<PurchaseQueryDetail> pages = purchaseQueryDetailMapper.selectPage((Page<PurchaseQueryDetail>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchaseQueryDetailDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseQueryDetailDto>(list, (int) pages.getTotal());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        purchaseQueryDetailMapper.deleteBatchIds(ids);
    }

    @Override
    public PurchaseQueryDetailDto selectById(Long id) throws Exception {
        PurchaseQueryDetail model = purchaseQueryDetailMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchaseQueryDetailDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchaseQueryDetailDto dto = new PurchaseQueryDetailDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    /**
     * 根据父类编号删除
     *
     * @param parentCode
     */
    public int delete(String parentCode) {
        QueryWrapper<PurchaseQueryDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        return purchaseQueryDetailMapper.delete(qw);
    }

    public BaseListDto<PurchaseQueryDetailDto> selectByParentCode(String parentCode) {
        QueryWrapper<PurchaseQueryDetail> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        List<PurchaseQueryDetailDto> list = purchaseQueryDetailMapper.selectList(qw).stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseQueryDetailDto>(list, list.size());
    }

    /**
     * 回写上级
     * @param detailList
     * @return
     * @throws Exception
     */
    public int updateBackDistribute(List<PurchaseQueryDetailDto> detailList) throws Exception {
        List<PurchaseDistributeDetail> backList = new ArrayList<>();
        for (PurchaseQueryDetailDto detailDto : detailList) {
            if (StringUtil.isNotBlank(detailDto.getOuterCode())){
                PurchaseDistributeDetail backObj = new PurchaseDistributeDetail();
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
                    PurchaseQueryDetail oldDetail = purchaseQueryDetailMapper.selectById(detailDto.getId());
                    backObj.setChangeNum(detailDto.getCount()-oldDetail.getCount());
                    backObj.setChangeWeight(detailDto.getWeight()-oldDetail.getWeight());
                }
                backList.add(backObj);
            }
        }

        return purchaseDistributeDetailService.updateBackExec(backList);
    }

    /**
     * 回写未执行量
     * @param changeList
     * @return
     */
    public int updateBackExec(List<PurchaseQueryDetail> changeList) throws Exception {
        //回写明细
        Set<String> mainSet = new TreeSet<>();
        for (PurchaseQueryDetail thisDetail:changeList){
            int result = updateBackDetailExec(thisDetail);
            if (result<0){
                throw new Exception("回写明细"+thisDetail.getCode()+"失败！");
            }

            //按主表分组
            mainSet.add(thisDetail.getParentCode());
        }

        String errorCode = purchaseQueryService.updateBackMainExec(mainSet);
        if (StringUtil.isNotBlank(errorCode)){
            throw new Exception("回写主表"+errorCode+"失败！");
        }

        return 1;
    }

    /**
     * 回写明细未执行量
     * @param queryDetail
     * @return
     */
    private int updateBackDetailExec(PurchaseQueryDetail queryDetail){
        QueryWrapper<PurchaseQueryDetail> qw = new QueryWrapper<>();
        qw.eq("code",queryDetail.getCode());
        qw.eq("parent_code",queryDetail.getParentCode());
        PurchaseQueryDetail oldQueryDetail = purchaseQueryDetailMapper.selectOne(qw);

        PurchaseQueryDetail obj = new PurchaseQueryDetail();
        obj.setExecCount(new BigDecimal(oldQueryDetail.getExecCount())
                .subtract(new BigDecimal(queryDetail.getChangeNum())).setScale(0,4).intValue());
        obj.setExecWeight(new BigDecimal(oldQueryDetail.getExecWeight())
                .subtract(new BigDecimal(queryDetail.getChangeWeight())).setScale(3,4).doubleValue());

        UpdateWrapper<PurchaseQueryDetail> uw = new UpdateWrapper<>();
        uw.eq("code",queryDetail.getCode());
        uw.eq("parent_code",queryDetail.getParentCode());
        return purchaseQueryDetailMapper.update(obj,uw);
    }


}