package org.zhd.purchase.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xy.api.dpi.BaseService;
import org.xy.api.dpi.workflow.ProcessApi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseQueryDetailDto;
import org.xy.api.dto.purchase.PurchaseQueryDto;
import org.xy.api.dto.workflow.ProcessDto;
import org.xy.api.dto.workflow.ProcessTaskDto;
import org.xy.api.enums.ApiEnum;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchaseQuery;
import org.zhd.purchase.mapper.PurchaseQueryMapper;
import org.xy.api.utils.DaoUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购询价业务层
 *
 * @author samy
 */
@Service
@Slf4j
public class PurchaseQueryService implements BaseService<PurchaseQueryDto, Long> {

    @Autowired
    private PurchaseQueryMapper purchaseQueryMapper;

    @Autowired
    private CommMapper commMapper;

    @Autowired
    private PurchaseQueryDetailService purchaseQueryDetailService;

    @Autowired
    private ProcessApi processApi;

    @Override
    public PurchaseQueryDto saveOrUpdate(PurchaseQueryDto model) throws Exception {
        PurchaseQuery obj = new PurchaseQuery();
        if (model.getMemberCode() == null) {
            throw new Exception("集团编号不能为空");
        }

        if (null!=model.getAuditStatus()&&model.getAuditStatus()>=2){
            throw new Exception("单据已审核");
        }

        BeanUtils.copyProperties(model, obj);
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_query");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("PQQyyyymmdd3", maxCode == null ? "001" : maxCode);
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P003");
            obj.setExecCount(obj.getCount());
            obj.setExecWeight(obj.getWeight());
            // 默认无需审核
            obj.setAuditStatus(0);
            // FIXME 处理审核逻辑
            purchaseQueryMapper.insert(obj);
        } else {
            // 更新
            purchaseQueryMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchaseQueryDto> selectPage(Map<String, Object> params) throws Exception {
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchaseQuery> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchaseQuery.class);
        IPage<PurchaseQuery> pages = purchaseQueryMapper.selectPage((Page<PurchaseQuery>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchaseQueryDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseQueryDto>(list, (int) pages.getTotal());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        Boolean canDelete = true;
        for (int i = 0; i < ids.size(); i++) {
            PurchaseQuery oldQuery = purchaseQueryMapper.selectById(ids.get(i));
            if (null==oldQuery){
                throw new Exception("该记录不存在！");
            }

            if (oldQuery.getAuditStatus()>=2){
                throw new Exception("该记录已审核不能删除！");
            }
            int result = purchaseQueryDetailService.delete(oldQuery.getCode());
            if (result < 0) {
                canDelete = false;
                try {
                    throw new Exception("delete purchase query detail failure");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("delete purchase query detail failure", e);
                }
                break;
            }
        }
        if (canDelete) {
            //删除审核流
            for (Long id : ids) {
                PurchaseQuery oldQuery = purchaseQueryMapper.selectById(id);
                deleteWorkflow(entity2Dto(oldQuery),null);
            }
            purchaseQueryMapper.deleteBatchIds(ids);
        } else {
            throw new Exception("delete purchase query failure");
        }
    }

    @Override
    public PurchaseQueryDto selectById(Long id) throws Exception {
        PurchaseQuery model = purchaseQueryMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchaseQueryDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchaseQueryDto dto = new PurchaseQueryDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    /**
     * 审核
     * @param model
     * @return
     * @throws Exception
     */
    public PurchaseQueryDto audit(PurchaseQueryDto model) throws Exception{
        String msg = checkAudit(model,true);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.audit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            PurchaseQuery queryObj = new PurchaseQuery();
            queryObj.setAuditDate(new Date());
            queryObj.setAuditName(model.getAuditName());
            queryObj.setAuditRemark(model.getAuditRemark());
            if (null != taskDtoList && taskDtoList.size() > 0) {
                //有人员列表表示可继续审核
                queryObj.setAuditStatus(3);
            }else {
                //已审核至归档
                queryObj.setAuditStatus(2);
            }
            UpdateWrapper<PurchaseQuery> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseQueryMapper.update(queryObj,ew);
            return entity2Dto(queryObj);
        }else {
            throw new Exception("审核失败！");
        }
    }

    /**
     * 弃审
     * @param model
     * @return
     * @throws Exception
     */
    public PurchaseQueryDto unAudit(PurchaseQueryDto model) throws Exception{
        String msg = checkAudit(model,false);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.unAudit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            boolean start = backDto.isStartingPoint();
            PurchaseQuery queryObj = new PurchaseQuery();
            queryObj.setAuditDate(new Date());
            queryObj.setAuditName(model.getAuditName());
            queryObj.setAuditRemark(model.getAuditRemark());
            if (start) {
                //已到达第一个节点，设为弃审
                queryObj.setAuditStatus(-1);
            }else {
                //可继续审核
                queryObj.setAuditStatus(3);
            }
            UpdateWrapper<PurchaseQuery> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseQueryMapper.update(queryObj,ew);
            return entity2Dto(queryObj);
        }else {
            throw new Exception("审核失败！");
        }

    }


    private String checkAudit(PurchaseQueryDto model,boolean ifAudit){
        String msg = "";
        PurchaseQuery oldQuery = purchaseQueryMapper.selectById(model.getId());
        if (null==oldQuery){
            msg = "该记录不存在!";
        }else if(model.getAuditStatus().intValue()==0){
            msg += "该记录无需审核";
        }else if (ifAudit&&model.getAuditStatus().intValue()==2){
            msg += "该记录已审核";
        }else if (!ifAudit&&(model.getAuditStatus().intValue()==-1||model.getAuditStatus().intValue()==1)){
            msg += "该记录未审核";
        }
        msg = StringUtil.isNotEmpty(msg)?msg.substring(0,msg.length()-1):msg;

        return msg;
    }


    /**
     * 启动审核流
     * @param model
     * @param detailList
     * @throws Exception
     */
    public void startWorkflow(PurchaseQueryDto model, List<PurchaseQueryDetailDto> detailList) throws Exception{
        ProcessDto sendDto = convertProcessDto(model,detailList);
        ProcessDto backDto = processApi.startIfExists(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //有人员列表表示需要审核
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //回写审核状态
                PurchaseQuery queryObj = new PurchaseQuery();
                queryObj.setAuditStatus(1);
                queryObj.setWorkflowId(backDto.getProcessInstanceId());
                UpdateWrapper<PurchaseQuery> ew = new UpdateWrapper<>();
                ew.eq("code",model.getCode());
                purchaseQueryMapper.update(queryObj,ew);
            }
        }else {
            throw new Exception("启动审核流失败！");
        }
    }

    /**
     * 更新审核流
     * @param model
     * @param detailList
     * @throws Exception
     */
    public void modifyWorkflow(PurchaseQueryDto model, List<PurchaseQueryDetailDto> detailList) throws Exception {
        ProcessDto sendDto = convertProcessDto(model, detailList);
        ProcessDto backDto = processApi.modify(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //回写审核状态
            PurchaseQuery queryObj = new PurchaseQuery();
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //未审
                queryObj.setAuditStatus(1);
                queryObj.setWorkflowId(backDto.getProcessInstanceId());
            }else{
                //无需审核
                queryObj.setAuditStatus(0);
                queryObj.setWorkflowId("");
            }
            UpdateWrapper<PurchaseQuery> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseQueryMapper.update(queryObj,ew);
        }else {
            throw new Exception("更新审核流失败！");
        }
    }

    /**
     * 删除审核流
     * @param model
     * @param detailList
     * @throws Exception
     */
    public void deleteWorkflow(PurchaseQueryDto model, List<PurchaseQueryDetailDto> detailList) throws Exception{
        if (StringUtil.isNotEmpty(model.getWorkflowId())){
            ProcessDto sendDto = convertProcessDto(model,detailList);
            ProcessDto backDto = processApi.delete(sendDto);
            if (null==backDto||backDto.getReturn_code()!= ApiEnum.SUCCESS.getValue()){
                throw new Exception("删除审核流失败！");
            }
        }
    }

    /**
     * 封装审核数据
     * @param model
     * @param DetailList
     * @return
     * @throws Exception
     */
    public ProcessDto convertProcessDto(PurchaseQueryDto model, List<PurchaseQueryDetailDto> DetailList) throws Exception {
        ProcessDto returnDto = new ProcessDto();
        returnDto.setProcessKey(model.getBillTypeCode());

        String workflowId =  StringUtil.isNotEmpty(model.getWorkflowId())?model.getWorkflowId():"";
        returnDto.setProcessInstanceId(workflowId);

        returnDto.setProcessInstanceName("采购询价");

        Map<String,Object> valueMap = new HashMap<>();
        Class clazz = model.getClass();
        for (Field field :clazz.getDeclaredFields()){
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value = field.get(model);
            valueMap.put(fieldName, value);
        }
        if (null!=DetailList&&DetailList.size()>0){
            valueMap.put("details", JSON.toJSONString(DetailList));
        }
        returnDto.setVariables(valueMap);

        returnDto.setUserCode(StringUtil.isNotEmpty(model.getAuditCode())?
                model.getAuditCode():model.getAccountCode());
        returnDto.setUserName(StringUtil.isNotEmpty(model.getAuditName())?
                model.getAuditName():model.getAccountName());

        returnDto.setComment(model.getAuditRemark());

        //来源1：ERP
        returnDto.setSource(1);

        return returnDto;
    }

    /**
     * 回写主表未执行量
     * @param mainSet
     * @return
     */
    public String updateBackMainExec(Set<String> mainSet){
        String errorCode = "";
        for (String parentCode : mainSet) {
            BaseListDto<PurchaseQueryDetailDto> baseList = purchaseQueryDetailService.selectByParentCode(parentCode);
            if (null!=baseList){
                List<PurchaseQueryDetailDto> list = baseList.getList();

                int totalNum=0;
                double totalWeight=0d;
                for (PurchaseQueryDetailDto thisDetail : list) {
                    totalNum += thisDetail.getExecCount();
                    totalWeight += thisDetail.getExecWeight();
                }

                PurchaseQuery obj = new PurchaseQuery();
                obj.setExecCount(totalNum);
                obj.setExecWeight(totalWeight);
                UpdateWrapper<PurchaseQuery> uw = new UpdateWrapper<>();
                uw.eq("code",parentCode);
                int result = purchaseQueryMapper.update(obj,uw);
                if(result<0){
                    errorCode = parentCode;
                }
            }
        }

        return errorCode;
    }
}