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
import org.xy.api.dto.purchase.PurchaseContractDetailDto;
import org.xy.api.dto.purchase.PurchaseContractDto;
import org.xy.api.dto.workflow.ProcessDto;
import org.xy.api.dto.workflow.ProcessTaskDto;
import org.xy.api.enums.ApiEnum;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchaseContract;
import org.zhd.purchase.mapper.PurchaseContractMapper;
import org.xy.api.utils.DaoUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
* 采购合同业务层
*
* @author samy
*/
@Service
@Slf4j
public class PurchaseContractService implements BaseService<PurchaseContractDto, Long> {

    @Autowired
    private PurchaseContractMapper purchaseContractMapper;

    @Autowired
    private CommMapper commMapper;

    @Autowired
    private PurchaseContractDetailService purchaseContractDetailService;

    @Autowired
    private ProcessApi processApi;

    @Override
    @GlobalTransactional(name = "scp-create-purchaseContract",rollbackFor = Exception.class)
    public PurchaseContractDto saveOrUpdate(PurchaseContractDto model) throws Exception {
        PurchaseContract obj = new PurchaseContract();
        String msg =checkMain(model);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }

        BeanUtils.copyProperties(model, obj);
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_contract");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("Cyyyymmdd3", maxCode == null ? "001" : maxCode);
            //FIXME 默认值设定
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P005");
            obj.setExecCount(obj.getCount());
            obj.setExecWeight(obj.getWeight());
            obj.setFinishPay(1);
            // 默认无需审核
            obj.setAuditStatus(0);
            purchaseContractMapper.insert(obj);
        } else {
            // 更新
            purchaseContractMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchaseContractDto> selectPage(Map<String, Object> params) throws Exception {
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchaseContract> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchaseContract.class);
        IPage<PurchaseContract> pages = purchaseContractMapper.selectPage((Page<PurchaseContract>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchaseContractDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseContractDto>(list, (int) pages.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) throws Exception {
        Boolean canDelete = true;
        for (int i = 0; i < ids.size(); i++) {
            PurchaseContract oldContract = purchaseContractMapper.selectById(ids.get(i));
            if (null==oldContract){
                throw new Exception("该记录不存在！");
            }

            if (oldContract.getAuditStatus()>=2){
                throw new Exception("该记录已审核不能删除！");
            }

            int result = purchaseContractDetailService.delete(oldContract.getCode());
            if (result < 0) {
                canDelete = false;
                try {
                    throw new Exception("delete purchase contract detail failure");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("delete purchase contract detail failure", e);
                }
                break;
            }
        }
        if (canDelete) {
            //删除审核流
            for (Long id : ids) {
                PurchaseContract oldContract = purchaseContractMapper.selectById(id);
                deleteWorkflow(entity2Dto(oldContract),null);
            }
            purchaseContractMapper.deleteBatchIds(ids);
        } else {
            throw new Exception("delete purchase contract failure");
        }
    }

    @Override
    public PurchaseContractDto selectById(Long id) throws Exception {
        PurchaseContract model = purchaseContractMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchaseContractDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchaseContractDto dto = new PurchaseContractDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    public String checkMain(PurchaseContractDto model){
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
    public PurchaseContractDto audit(PurchaseContractDto model) throws Exception{
        String msg = checkAudit(model,true);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.audit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            PurchaseContract contractObj = new PurchaseContract();
            contractObj.setAuditDate(new Date());
            contractObj.setAuditName(model.getAuditName());
            contractObj.setAuditRemark(model.getAuditRemark());
            if (null != taskDtoList && taskDtoList.size() > 0) {
                //有人员列表表示可继续审核
                contractObj.setAuditStatus(3);
            }else {
                //已审核至归档
                contractObj.setAuditStatus(2);
            }
            UpdateWrapper<PurchaseContract> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseContractMapper.update(contractObj,ew);
            return entity2Dto(contractObj);
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
    public PurchaseContractDto unAudit(PurchaseContractDto model) throws Exception{
        String msg = checkAudit(model,false);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.unAudit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            boolean start = backDto.isStartingPoint();
            PurchaseContract contractObj = new PurchaseContract();
            contractObj.setAuditDate(new Date());
            contractObj.setAuditName(model.getAuditName());
            contractObj.setAuditRemark(model.getAuditRemark());
            if (start) {
                //已到达第一个节点，设为弃审
                contractObj.setAuditStatus(-1);
            }else {
                //可继续审核
                contractObj.setAuditStatus(3);
            }
            UpdateWrapper<PurchaseContract> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseContractMapper.update(contractObj,ew);
            return entity2Dto(contractObj);
        }else {
            throw new Exception("审核失败！");
        }

    }


    private String checkAudit(PurchaseContractDto model,boolean ifAudit){
        String msg = "";
        PurchaseContract oldContract = purchaseContractMapper.selectById(model.getId());
        if (null==oldContract){
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
    public void startWorkflow(PurchaseContractDto model, List<PurchaseContractDetailDto> detailList) throws Exception{
        ProcessDto sendDto = convertProcessDto(model,detailList);
        ProcessDto backDto = processApi.startIfExists(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //有人员列表表示需要审核
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //回写审核状态
                PurchaseContract contractObj = new PurchaseContract();
                contractObj.setAuditStatus(1);
                contractObj.setWorkflowId(backDto.getProcessInstanceId());
                UpdateWrapper<PurchaseContract> ew = new UpdateWrapper<>();
                ew.eq("code",model.getCode());
                purchaseContractMapper.update(contractObj,ew);
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
    public void modifyWorkflow(PurchaseContractDto model, List<PurchaseContractDetailDto> detailList) throws Exception {
        ProcessDto sendDto = convertProcessDto(model, detailList);
        ProcessDto backDto = processApi.modify(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //回写审核状态
            PurchaseContract contractObj = new PurchaseContract();
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //未审
                contractObj.setAuditStatus(1);
                contractObj.setWorkflowId(backDto.getProcessInstanceId());
            }else{
                //无需审核
                contractObj.setAuditStatus(0);
                contractObj.setWorkflowId("");
            }
            UpdateWrapper<PurchaseContract> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseContractMapper.update(contractObj,ew);
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
    public void deleteWorkflow(PurchaseContractDto model, List<PurchaseContractDetailDto> detailList) throws Exception{
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
    public ProcessDto convertProcessDto(PurchaseContractDto model, List<PurchaseContractDetailDto> DetailList) throws Exception {
        ProcessDto returnDto = new ProcessDto();
        returnDto.setProcessKey(model.getBillTypeCode());

        String workflowId =  StringUtil.isNotEmpty(model.getWorkflowId())?model.getWorkflowId():"";
        returnDto.setProcessInstanceId(workflowId);

        returnDto.setProcessInstanceName("采购合同");

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
            BaseListDto<PurchaseContractDetailDto> baseList = purchaseContractDetailService.selectByParentCode(parentCode);
            if (null!=baseList){
                List<PurchaseContractDetailDto> list = baseList.getList();

                int totalNum=0;
                double totalWeight=0d;
                for (PurchaseContractDetailDto thisDetail : list) {
                    totalNum += thisDetail.getExecCount();
                    totalWeight += thisDetail.getExecWeight();
                }

                PurchaseContract obj = new PurchaseContract();
                obj.setExecCount(totalNum);
                obj.setExecWeight(totalWeight);
                UpdateWrapper<PurchaseContract> uw = new UpdateWrapper<>();
                uw.eq("code",parentCode);
                int result = purchaseContractMapper.update(obj,uw);
                if(result<0){
                    errorCode = parentCode;
                }
            }
        }

        return errorCode;
    }
}