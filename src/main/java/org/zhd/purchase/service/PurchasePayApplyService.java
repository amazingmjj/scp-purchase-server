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
import org.xy.api.dto.purchase.*;
import org.xy.api.dto.workflow.ProcessDto;
import org.xy.api.dto.workflow.ProcessTaskDto;
import org.xy.api.enums.ApiEnum;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchasePayApply;
import org.zhd.purchase.entity.PurchasePayApplyDetail;
import org.zhd.purchase.entity.PurchasePlan;
import org.zhd.purchase.entity.PurchaseQuery;
import org.zhd.purchase.mapper.PurchasePayApplyMapper;
import org.xy.api.utils.DaoUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购付款申请业务层
 *
 * @author samy
 */
@Service
@Slf4j
public class PurchasePayApplyService implements BaseService<PurchasePayApplyDto, Long> {

    @Autowired
    private PurchasePayApplyMapper purchasePayApplyMapper;

    @Autowired
    private CommMapper commMapper;

    @Autowired
    private PurchasePayApplyDetailService purchasePayApplyDetailService;

    @Autowired
    private ProcessApi processApi;

    @Override
    public PurchasePayApplyDto saveOrUpdate(PurchasePayApplyDto model) throws Exception {
        PurchasePayApply obj = new PurchasePayApply();
        if (model.getMemberCode() == null) {
            throw new Exception("集团编号不能为空");
        }
        if (null!=model.getAuditStatus()&&model.getAuditStatus()>=2){
            throw new Exception("单据已审核");
        }
        BeanUtils.copyProperties(model, obj);
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_pay_apply");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("PQPAyyyymmdd3", maxCode == null ? "001" : maxCode);
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P004");
            obj.setExecCount(obj.getCount());
            obj.setExecWeight(obj.getWeight());
            // 默认无需审核
            obj.setAuditStatus(0);
            // FIXME 处理审核逻辑
            purchasePayApplyMapper.insert(obj);
        } else {
            // 更新
            purchasePayApplyMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchasePayApplyDto> selectPage(Map<String, Object> params) throws Exception {
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchasePayApply> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchasePayApply.class);
        IPage<PurchasePayApply> pages = purchasePayApplyMapper.selectPage((Page<PurchasePayApply>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchasePayApplyDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchasePayApplyDto>(list, (int) pages.getTotal());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        Boolean canDelete = true;
        for (int i = 0; i < ids.size(); i++) {
            PurchasePayApply oldPayApply = purchasePayApplyMapper.selectById(ids.get(i));
            if (null==oldPayApply){
                throw new Exception("该记录不存在！");
            }

            if (2==oldPayApply.getAuditStatus()){
                throw new Exception("该记录已审核不能删除！");
            }
            int result = purchasePayApplyDetailService.delete(oldPayApply.getCode());
            if (result < 0) {
                canDelete = false;
                try {
                    throw new Exception("delete purchase pay apply detail failure");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("delete purchase pay apply detail query failure", e);
                }
                break;
            }
        }
        if (canDelete) {
            //删除审核流
            for (Long id : ids) {
                PurchasePayApply oldPayApply = purchasePayApplyMapper.selectById(id);
                deleteWorkflow(entity2Dto(oldPayApply),null);
            }
            purchasePayApplyMapper.deleteBatchIds(ids);
        } else {
            throw new Exception("delete purchase pay apply detail failure");
        }
    }

    @Override
    public PurchasePayApplyDto selectById(Long id) throws Exception {
        PurchasePayApply model = purchasePayApplyMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchasePayApplyDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchasePayApplyDto dto = new PurchasePayApplyDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    /**
     * 审核
     * @param model
     * @return
     * @throws Exception
     */
    public PurchasePayApplyDto audit(PurchasePayApplyDto model) throws Exception{
        String msg = checkAudit(model,true);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.audit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            PurchasePayApply payApplyObj = new PurchasePayApply();
            payApplyObj.setAuditDate(new Date());
            payApplyObj.setAuditName(model.getAuditName());
            payApplyObj.setAuditRemark(model.getAuditRemark());
            if (null != taskDtoList && taskDtoList.size() > 0) {
                //有人员列表表示可继续审核
                payApplyObj.setAuditStatus(3);
            }else {
                //已审核至归档
                payApplyObj.setAuditStatus(2);
            }
            UpdateWrapper<PurchasePayApply> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchasePayApplyMapper.update(payApplyObj,ew);
            return entity2Dto(payApplyObj);
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
    public PurchasePayApplyDto unAudit(PurchasePayApplyDto model) throws Exception{
        String msg = checkAudit(model,false);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.unAudit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            boolean start = backDto.isStartingPoint();
            PurchasePayApply payApplyObj = new PurchasePayApply();
            payApplyObj.setAuditDate(new Date());
            payApplyObj.setAuditName(model.getAuditName());
            payApplyObj.setAuditRemark(model.getAuditRemark());
            if (start) {
                //已到达第一个节点，设为弃审
                payApplyObj.setAuditStatus(-1);
            }else {
                //可继续审核
                payApplyObj.setAuditStatus(3);
            }
            UpdateWrapper<PurchasePayApply> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchasePayApplyMapper.update(payApplyObj,ew);
            return entity2Dto(payApplyObj);
        }else {
            throw new Exception("审核失败！");
        }

    }


    private String checkAudit(PurchasePayApplyDto model,boolean ifAudit){
        String msg = "";
        PurchasePayApply oldPayApply = purchasePayApplyMapper.selectById(model.getId());
        if (null==oldPayApply){
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
    public void startWorkflow(PurchasePayApplyDto model, List<PurchasePayApplyDetailDto> detailList) throws Exception{
        ProcessDto sendDto = convertProcessDto(model,detailList);
        ProcessDto backDto = processApi.startIfExists(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //有人员列表表示需要审核
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //回写审核状态
                PurchasePayApply payApplyObj = new PurchasePayApply();
                payApplyObj.setAuditStatus(1);
                payApplyObj.setWorkflowId(backDto.getProcessInstanceId());
                UpdateWrapper<PurchasePayApply> ew = new UpdateWrapper<>();
                ew.eq("code",model.getCode());
                purchasePayApplyMapper.update(payApplyObj,ew);
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
    public void modifyWorkflow(PurchasePayApplyDto model, List<PurchasePayApplyDetailDto> detailList) throws Exception {
        ProcessDto sendDto = convertProcessDto(model, detailList);
        ProcessDto backDto = processApi.modify(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //回写审核状态
            PurchasePayApply payApplyObj = new PurchasePayApply();
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //未审
                payApplyObj.setAuditStatus(1);
                payApplyObj.setWorkflowId(backDto.getProcessInstanceId());
            }else{
                //无需审核
                payApplyObj.setAuditStatus(0);
                payApplyObj.setWorkflowId("");
            }
            UpdateWrapper<PurchasePayApply> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchasePayApplyMapper.update(payApplyObj,ew);
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
    public void deleteWorkflow(PurchasePayApplyDto model, List<PurchasePayApplyDetailDto> detailList) throws Exception{
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
    public ProcessDto convertProcessDto(PurchasePayApplyDto model, List<PurchasePayApplyDetailDto> DetailList) throws Exception {
        ProcessDto returnDto = new ProcessDto();
        returnDto.setProcessKey(model.getBillTypeCode());

        String workflowId =  StringUtil.isNotEmpty(model.getWorkflowId())?model.getWorkflowId():"";
        returnDto.setProcessInstanceId(workflowId);

        returnDto.setProcessInstanceName("付款申请");

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
            BaseListDto<PurchasePayApplyDetailDto> baseList = purchasePayApplyDetailService.selectByParentCode(parentCode);
            if (null!=baseList){
                List<PurchasePayApplyDetailDto> list = baseList.getList();

                int totalNum=0;
                double totalWeight=0d;
                for (PurchasePayApplyDetailDto thisDetail : list) {
                    totalNum += thisDetail.getExecCount();
                    totalWeight += thisDetail.getExecWeight();
                }

                PurchasePayApply obj = new PurchasePayApply();
                obj.setExecCount(totalNum);
                obj.setExecWeight(totalWeight);
                UpdateWrapper<PurchasePayApply> uw = new UpdateWrapper<>();
                uw.eq("code",parentCode);
                int result = purchasePayApplyMapper.update(obj,uw);
                if(result<0){
                    errorCode = parentCode;
                }

            }
        }

        return errorCode;
    }
}