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
import org.xy.api.dto.purchase.PurchasePlanDetailDto;
import org.xy.api.dto.purchase.PurchasePlanDto;
import org.xy.api.dto.workflow.ProcessDto;
import org.xy.api.dto.workflow.ProcessTaskDto;
import org.xy.api.enums.ApiEnum;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchasePlan;
import org.zhd.purchase.mapper.PurchasePlanMapper;
import org.xy.api.utils.DaoUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购计划业务层
 *
 * @author samy
 */
@Service
@Slf4j
public class PurchasePlanService implements BaseService<PurchasePlanDto, Long> {

    @Autowired
    private PurchasePlanMapper purchasePlanMapper;

    @Autowired
    private CommMapper commMapper;

    @Autowired
    private PurchasePlanDetailService purchasePlanDetailService;

    @Autowired
    private ProcessApi processApi;

    @Override
    public PurchasePlanDto saveOrUpdate(PurchasePlanDto model) throws Exception {
        PurchasePlan obj = new PurchasePlan();
        String msg =checkMain(model);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        BeanUtils.copyProperties(model, obj);
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_plan");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("PQyyyymmdd3", maxCode == null ? "001" : maxCode);
            obj.setCode(uniqueCode);
            obj.setAuditStatus(0);
            obj.setBillTypeCode("P001");
            purchasePlanMapper.insert(obj);
        } else {
            // 更新
            purchasePlanMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchasePlanDto> selectPage(Map<String, Object> params) throws Exception {
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchasePlanDto> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchasePlanDto.class);
        IPage<PurchasePlanDto> pages = purchasePlanMapper.selectByPage((Page<PurchasePlan>) DaoUtil.queryPage(currentPage, pageSize), qw);
        return new BaseListDto<PurchasePlanDto>(pages.getRecords(), (int) pages.getTotal());

    }


    @Override
    public void delete(List<Long> ids) throws Exception {
        Boolean canDelete = true;
        for (int i = 0; i < ids.size(); i++) {
            PurchasePlan oldPlan = purchasePlanMapper.selectById(ids.get(i));
            if (null==oldPlan){
                throw new Exception("该记录不存在！");
            }

            if (oldPlan.getAuditStatus()>=2){
                throw new Exception("该记录已审核不能删除！");
            }

            int result = purchasePlanDetailService.delete(oldPlan.getCode());
            if (result < 0) {
                canDelete = false;
                try {
                    throw new Exception("delete purchase plan detail failure");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("delete purchase plan detail failure", e);
                }
                break;
            }
        }
        if (canDelete) {
            //删除审核流
            for (Long id : ids) {
                PurchasePlan oldPlan = purchasePlanMapper.selectById(id);
                deleteWorkflow(entity2Dto(oldPlan),null);
            }
            purchasePlanMapper.deleteBatchIds(ids);
        } else {
            throw new Exception("delete purchase plan failure");
        }
    }

    @Override
    public PurchasePlanDto selectById(Long id) throws Exception {
        PurchasePlan model = purchasePlanMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchasePlanDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }

        PurchasePlanDto dto = new PurchasePlanDto();
        BeanUtils.copyProperties(source, dto);

        if(StringUtil.isNotEmpty(dto.getCompanyCode())){
            dto.setCompanyName(commMapper.selectNameByCode(dto.getCompanyCode(),"t_company"));
        }
        if (StringUtil.isNotEmpty(dto.getEmployeeCode())){
            dto.setEmployeeName(commMapper.selectNameByCode(dto.getEmployeeCode(),"t_employee"));
        }
        if (StringUtil.isNotEmpty(dto.getOrgCode())){
            dto.setOrgName(commMapper.selectNameByCode(dto.getOrgCode(),"t_org"));
        }
        if (StringUtil.isNotEmpty(dto.getDptCode())){
            dto.setDptName(commMapper.selectNameByCode(dto.getDptCode(),"t_dpt"));
        }
        //FIXME
        dto.setAccountName("系统管理员");

        return dto;
    }

    public String checkMain(PurchasePlanDto model){
        String msg ="";
        //FIXME 字段校验
        msg = StringUtil.isEmpty(model.getMemberCode())?msg+"集团编号不能为空,":msg;

        msg = (null!=model.getAuditStatus()&&model.getAuditStatus()>=2)?msg+"单据已审核,":msg;

        if(StringUtil.isNotEmpty(msg)){
            msg=msg.substring(0,msg.length()-1);
        }
        return msg;
    }


    /**
     * 审核
     * @param model
     * @return
     * @throws Exception
     */
    public PurchasePlanDto audit(PurchasePlanDto model) throws Exception{
        String msg = checkAudit(model,true);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.audit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            PurchasePlan planObj = new PurchasePlan();
            planObj.setAuditDate(new Date());
            planObj.setAuditName(model.getAuditName());
            planObj.setAuditRemark(model.getAuditRemark());
            if (null != taskDtoList && taskDtoList.size() > 0) {
                //有人员列表表示可继续审核
                planObj.setAuditStatus(3);
            }else {
                //已审核至归档
                planObj.setAuditStatus(2);
            }
            UpdateWrapper<PurchasePlan> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchasePlanMapper.update(planObj,ew);
            return entity2Dto(planObj);
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
    public PurchasePlanDto unAudit(PurchasePlanDto model) throws Exception{
        String msg = checkAudit(model,false);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.unAudit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            boolean start = backDto.isStartingPoint();
            PurchasePlan planObj = new PurchasePlan();
            planObj.setAuditDate(new Date());
            planObj.setAuditName(model.getAuditName());
            planObj.setAuditRemark(model.getAuditRemark());
            if (start) {
                //已到达第一个节点，设为弃审
                planObj.setAuditStatus(-1);
            }else {
                //可继续审核
                planObj.setAuditStatus(3);
            }
            UpdateWrapper<PurchasePlan> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchasePlanMapper.update(planObj,ew);
            return entity2Dto(planObj);
        }else {
            throw new Exception("审核失败！");
        }

    }


    private String checkAudit(PurchasePlanDto model,boolean ifAudit){
        String msg = "";
        PurchasePlan oldPlan = purchasePlanMapper.selectById(model.getId());
        if (null==oldPlan){
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
    public void startWorkflow(PurchasePlanDto model, List<PurchasePlanDetailDto> detailList) throws Exception{
        ProcessDto sendDto = convertProcessDto(model,detailList);
        ProcessDto backDto = processApi.startIfExists(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //有人员列表表示需要审核
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //回写审核状态
                PurchasePlan planObj = new PurchasePlan();
                planObj.setAuditStatus(1);
                planObj.setWorkflowId(backDto.getProcessInstanceId());
                UpdateWrapper<PurchasePlan> ew = new UpdateWrapper<>();
                ew.eq("code",model.getCode());
                purchasePlanMapper.update(planObj,ew);
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
    public void modifyWorkflow(PurchasePlanDto model, List<PurchasePlanDetailDto> detailList) throws Exception {
        ProcessDto sendDto = convertProcessDto(model, detailList);
        ProcessDto backDto = processApi.modify(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //回写审核状态
            PurchasePlan planObj = new PurchasePlan();
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //未审
                planObj.setAuditStatus(1);
                planObj.setWorkflowId(backDto.getProcessInstanceId());
            }else{
                //无需审核
                planObj.setAuditStatus(0);
                planObj.setWorkflowId("");
            }
            UpdateWrapper<PurchasePlan> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchasePlanMapper.update(planObj,ew);
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
    public void deleteWorkflow(PurchasePlanDto model, List<PurchasePlanDetailDto> detailList) throws Exception{
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
    public ProcessDto convertProcessDto(PurchasePlanDto model, List<PurchasePlanDetailDto> DetailList) throws Exception {
        ProcessDto returnDto = new ProcessDto();
        returnDto.setProcessKey(model.getBillTypeCode());

        String workflowId =  StringUtil.isNotEmpty(model.getWorkflowId())?model.getWorkflowId():"";
        returnDto.setProcessInstanceId(workflowId);

        returnDto.setProcessInstanceName("采购计划");

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


}