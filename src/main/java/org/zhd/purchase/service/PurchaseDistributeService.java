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
import org.springframework.transaction.annotation.Transactional;
import org.xy.api.dpi.BaseService;
import org.xy.api.dpi.workflow.ProcessApi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseDistributeDetailDto;
import org.xy.api.dto.purchase.PurchaseDistributeDto;
import org.xy.api.dto.workflow.ProcessDto;
import org.xy.api.dto.workflow.ProcessTaskDto;
import org.xy.api.enums.ApiEnum;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchaseDistribute;
import org.zhd.purchase.mapper.PurchaseDistributeMapper;
import org.xy.api.utils.DaoUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
* 采购配货业务层
*
* @author samy
*/
@Service
@Slf4j
public class PurchaseDistributeService implements BaseService<PurchaseDistributeDto, Long> {

    @Autowired
    private PurchaseDistributeMapper purchaseDistributeMapper;

    @Autowired
    private PurchaseDistributeDetailService purchaseDistributeDetailService;

    @Autowired
    private CommMapper commMapper;

    @Autowired
    private ProcessApi processApi;

    @Override
    public PurchaseDistributeDto saveOrUpdate(PurchaseDistributeDto model) throws Exception {
        PurchaseDistribute obj = new PurchaseDistribute();
        String msg =checkMain(model);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }

        BeanUtils.copyProperties(model, obj);
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_Distribute");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("PDyyyymmdd3", maxCode == null ? "001" : maxCode);
            //FIXME 默认值设定
            obj.setExecCount(obj.getCount());
            obj.setExecWeight(obj.getWeight());
            obj.setCode(uniqueCode);
            obj.setAuditStatus(0);
            obj.setBillTypeCode("P002");
            purchaseDistributeMapper.insert(obj);
        } else {
            // 更新
            purchaseDistributeMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchaseDistributeDto> selectPage(Map<String, Object> params) throws Exception {
        // FIXME DO SELECTPAGE ACTION
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchaseDistribute> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchaseDistribute.class);
        IPage<PurchaseDistribute> pages = purchaseDistributeMapper.selectPage((Page<PurchaseDistribute>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchaseDistributeDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseDistributeDto>(list, (int) pages.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) throws Exception {
        Boolean canDelete = true;
        for (int i = 0; i < ids.size(); i++) {
            PurchaseDistribute oldDistribute = purchaseDistributeMapper.selectById(ids.get(i));
            if (null==oldDistribute){
                throw new Exception("该记录不存在！");
            }

            if (oldDistribute.getAuditStatus()>=2){
                throw new Exception("该记录已审核不能删除！");
            }

            int result = purchaseDistributeDetailService.delete(oldDistribute.getCode());
            if (result < 0) {
                canDelete = false;
                try {
                    throw new Exception("delete purchase Distribute detail failure");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("delete purchase Distribute detail failure", e);
                }
                break;
            }
        }
        if (canDelete) {
            //删除审核流
            for (Long id : ids) {
                PurchaseDistribute oldDistribute = purchaseDistributeMapper.selectById(id);
                deleteWorkflow(entity2Dto(oldDistribute),null);
            }
            purchaseDistributeMapper.deleteBatchIds(ids);
        } else {
            throw new Exception("delete purchase Distribute failure");
        }
    }

    @Override
    public PurchaseDistributeDto selectById(Long id) throws Exception {
        PurchaseDistribute model = purchaseDistributeMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchaseDistributeDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchaseDistributeDto dto = new PurchaseDistributeDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    public String checkMain(PurchaseDistributeDto model){
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
    public PurchaseDistributeDto audit(PurchaseDistributeDto model) throws Exception{
        String msg = checkAudit(model,true);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.audit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            PurchaseDistribute planObj = new PurchaseDistribute();
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
            UpdateWrapper<PurchaseDistribute> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseDistributeMapper.update(planObj,ew);
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
    public PurchaseDistributeDto unAudit(PurchaseDistributeDto model) throws Exception{
        String msg = checkAudit(model,false);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.unAudit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            boolean start = backDto.isStartingPoint();
            PurchaseDistribute planObj = new PurchaseDistribute();
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
            UpdateWrapper<PurchaseDistribute> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseDistributeMapper.update(planObj,ew);
            return entity2Dto(planObj);
        }else {
            throw new Exception("审核失败！");
        }

    }

    private String checkAudit(PurchaseDistributeDto model,boolean ifAudit){
        String msg = "";
        PurchaseDistribute oldPlan = purchaseDistributeMapper.selectById(model.getId());
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
    public void startWorkflow(PurchaseDistributeDto model, List<PurchaseDistributeDetailDto> detailList) throws Exception{
        ProcessDto sendDto = convertProcessDto(model,detailList);
        ProcessDto backDto = processApi.startIfExists(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //有人员列表表示需要审核
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //回写审核状态
                PurchaseDistribute planObj = new PurchaseDistribute();
                planObj.setAuditStatus(1);
                planObj.setWorkflowId(backDto.getProcessInstanceId());
                UpdateWrapper<PurchaseDistribute> ew = new UpdateWrapper<>();
                ew.eq("code",model.getCode());
                purchaseDistributeMapper.update(planObj,ew);
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
    public void modifyWorkflow(PurchaseDistributeDto model, List<PurchaseDistributeDetailDto> detailList) throws Exception {
        ProcessDto sendDto = convertProcessDto(model, detailList);
        ProcessDto backDto = processApi.modify(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //回写审核状态
            PurchaseDistribute planObj = new PurchaseDistribute();
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //未审
                planObj.setAuditStatus(1);
                planObj.setWorkflowId(backDto.getProcessInstanceId());
            }else{
                //无需审核
                planObj.setAuditStatus(0);
                planObj.setWorkflowId("");
            }
            UpdateWrapper<PurchaseDistribute> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseDistributeMapper.update(planObj,ew);
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
    public void deleteWorkflow(PurchaseDistributeDto model, List<PurchaseDistributeDetailDto> detailList) throws Exception{
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
    public ProcessDto convertProcessDto(PurchaseDistributeDto model, List<PurchaseDistributeDetailDto> DetailList) throws Exception {
        ProcessDto returnDto = new ProcessDto();
        returnDto.setProcessKey(model.getBillTypeCode());

        String workflowId =  StringUtil.isNotEmpty(model.getWorkflowId())?model.getWorkflowId():"";
        returnDto.setProcessInstanceId(workflowId);

        returnDto.setProcessInstanceName("采购配货");

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
            BaseListDto<PurchaseDistributeDetailDto> baseList = purchaseDistributeDetailService.selectByParentCode(parentCode);
            if (null!=baseList){
                List<PurchaseDistributeDetailDto> list = baseList.getList();

                int totalNum=0;
                double totalWeight=0d;
                for (PurchaseDistributeDetailDto thisDetail : list) {
                    totalNum += thisDetail.getExecCount();
                    totalWeight += thisDetail.getExecWeight();
                }

                PurchaseDistribute obj = new PurchaseDistribute();
                obj.setExecCount(totalNum);
                obj.setExecWeight(totalWeight);
                UpdateWrapper<PurchaseDistribute> uw = new UpdateWrapper<>();
                uw.eq("code",parentCode);
                int result = purchaseDistributeMapper.update(obj,uw);
                if(result<0){
                    errorCode = parentCode;
                }
            }
        }

        return errorCode;
    }
}