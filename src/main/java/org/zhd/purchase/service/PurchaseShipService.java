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
import org.xy.api.dto.purchase.PurchaseShipDetailDto;
import org.xy.api.dto.purchase.PurchaseShipDto;
import org.xy.api.dto.workflow.ProcessDto;
import org.xy.api.dto.workflow.ProcessTaskDto;
import org.xy.api.enums.ApiEnum;
import org.xy.api.mapper.CommMapper;
import org.xy.api.utils.StringUtil;
import org.zhd.purchase.entity.PurchaseShip;
import org.zhd.purchase.mapper.PurchaseShipMapper;
import org.xy.api.utils.DaoUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
* 采购发货集港业务层
*
* @author samy
*/
@Service
@Slf4j
public class PurchaseShipService implements BaseService<PurchaseShipDto, Long> {

    @Autowired
    private PurchaseShipMapper purchaseShipMapper;

    @Autowired
    private CommMapper commMapper;

    @Autowired
    private PurchaseShipDetailService purchaseShipDetailService;

    @Autowired
    private ProcessApi processApi;

    @Override
    public PurchaseShipDto saveOrUpdate(PurchaseShipDto model) throws Exception {
        PurchaseShip obj = new PurchaseShip();
        String msg =checkMain(model);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }

        BeanUtils.copyProperties(model, obj);
        if (model.getId() == null) {
            // 新增
            String maxCode = commMapper.maxCode("code", "t_purchase_ship");
            // FIXME 根据Feign相关接口获取业务单据编号规则
            String uniqueCode = DaoUtil.generateBillCode("Oyyyymmdd3", maxCode == null ? "001" : maxCode);
            //FIXME 默认值设定
            obj.setCode(uniqueCode);
            obj.setBillTypeCode("P006");
            obj.setExecCount(obj.getCount());
            obj.setExecWeight(obj.getWeight());
            obj.setFinishPay(1);
            // 默认未审核
            obj.setAuditStatus(0);
            purchaseShipMapper.insert(obj);
        } else {
            // 更新
            purchaseShipMapper.updateById(obj);
        }
        BeanUtils.copyProperties(obj, model);
        return model;
    }

    @Override
    public BaseListDto<PurchaseShipDto> selectPage(Map<String, Object> params) throws Exception {
        int currentPage = Integer.parseInt(params.getOrDefault("currentPage", 1).toString());
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", 10).toString());
        QueryWrapper<PurchaseShip> qw = new QueryWrapper<>();
        DaoUtil.parseGenericQueryWrapper(qw, params, PurchaseShip.class);
        IPage<PurchaseShip> pages = purchaseShipMapper.selectPage((Page<PurchaseShip>) DaoUtil.queryPage(currentPage, pageSize), qw);
        List<PurchaseShipDto> list = pages.getRecords().stream().map(entity -> entity2Dto(entity)).collect(Collectors.toList());
        return new BaseListDto<PurchaseShipDto>(list, (int) pages.getTotal());
    }

    @Override
    public void delete(List<Long> ids) throws Exception {
        Boolean canDelete = true;
        for (int i = 0; i < ids.size(); i++) {
            PurchaseShip oldShip = purchaseShipMapper.selectById(ids.get(i));
            if (null==oldShip){
                throw new Exception("该记录不存在！");
            }

            if (2==oldShip.getAuditStatus()){
                throw new Exception("该记录已审核不能删除！");
            }

            int result = purchaseShipDetailService.delete(oldShip.getCode());
            if (result < 0) {
                canDelete = false;
                try {
                    throw new Exception("delete purchase ship detail failure");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("delete purchase ship detail failure", e);
                }
                break;
            }
        }
        if (canDelete) {
            //删除审核流
            for (Long id : ids) {
                PurchaseShip oldShip = purchaseShipMapper.selectById(id);
                deleteWorkflow(entity2Dto(oldShip),null);
            }
            purchaseShipMapper.deleteBatchIds(ids);
        } else {
            throw new Exception("delete purchase ship failure");
        }
    }

    @Override
    public PurchaseShipDto selectById(Long id) throws Exception {
        PurchaseShip model = purchaseShipMapper.selectById(id);
        return entity2Dto(model);
    }

    @Override
    public PurchaseShipDto entity2Dto(Object source) {
        if (source == null) {
            return null;
        }
        // FIXME CHANGE VALUE IN FACT REQUIREMENT
        PurchaseShipDto dto = new PurchaseShipDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    public String checkMain(PurchaseShipDto model){
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
    public PurchaseShipDto audit(PurchaseShipDto model) throws Exception{
        String msg = checkAudit(model,true);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.audit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            PurchaseShip shipObj = new PurchaseShip();
            shipObj.setAuditDate(new Date());
            shipObj.setAuditName(model.getAuditName());
            shipObj.setAuditRemark(model.getAuditRemark());
            if (null != taskDtoList && taskDtoList.size() > 0) {
                //有人员列表表示可继续审核
                shipObj.setAuditStatus(3);
            }else {
                //已审核至归档
                shipObj.setAuditStatus(2);
            }
            UpdateWrapper<PurchaseShip> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseShipMapper.update(shipObj,ew);
            return entity2Dto(shipObj);
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
    public PurchaseShipDto unAudit(PurchaseShipDto model) throws Exception{
        String msg = checkAudit(model,false);
        if (StringUtil.isNotEmpty(msg)){
            throw new Exception(msg);
        }
        ProcessDto sendDto = convertProcessDto(model,null);
        ProcessDto backDto = processApi.unAudit(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()) {
            boolean start = backDto.isStartingPoint();
            PurchaseShip shipObj = new PurchaseShip();
            shipObj.setAuditDate(new Date());
            shipObj.setAuditName(model.getAuditName());
            shipObj.setAuditRemark(model.getAuditRemark());
            if (start) {
                //已到达第一个节点，设为弃审
                shipObj.setAuditStatus(-1);
            }else {
                //可继续审核
                shipObj.setAuditStatus(3);
            }
            UpdateWrapper<PurchaseShip> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseShipMapper.update(shipObj,ew);
            return entity2Dto(shipObj);
        }else {
            throw new Exception("审核失败！");
        }

    }


    private String checkAudit(PurchaseShipDto model,boolean ifAudit){
        String msg = "";
        PurchaseShip oldShip = purchaseShipMapper.selectById(model.getId());
        if (null==oldShip){
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
    public void startWorkflow(PurchaseShipDto model, List<PurchaseShipDetailDto> detailList) throws Exception{
        ProcessDto sendDto = convertProcessDto(model,detailList);
        ProcessDto backDto = processApi.startIfExists(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //有人员列表表示需要审核
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //回写审核状态
                PurchaseShip shipObj = new PurchaseShip();
                shipObj.setAuditStatus(1);
                shipObj.setWorkflowId(backDto.getProcessInstanceId());
                UpdateWrapper<PurchaseShip> ew = new UpdateWrapper<>();
                ew.eq("code",model.getCode());
                purchaseShipMapper.update(shipObj,ew);
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
    public void modifyWorkflow(PurchaseShipDto model, List<PurchaseShipDetailDto> detailList) throws Exception {
        ProcessDto sendDto = convertProcessDto(model, detailList);
        ProcessDto backDto = processApi.modify(sendDto);
        if (null!=backDto&&backDto.getReturn_code()== ApiEnum.SUCCESS.getValue()){
            List<ProcessTaskDto> taskDtoList = backDto.getProcessTaskList();
            //回写审核状态
            PurchaseShip shipObj = new PurchaseShip();
            if (null!=taskDtoList&&taskDtoList.size()>0){
                //未审
                shipObj.setAuditStatus(1);
                shipObj.setWorkflowId(backDto.getProcessInstanceId());
            }else{
                //无需审核
                shipObj.setAuditStatus(0);
                shipObj.setWorkflowId("");
            }
            UpdateWrapper<PurchaseShip> ew = new UpdateWrapper<>();
            ew.eq("code",model.getCode());
            purchaseShipMapper.update(shipObj,ew);
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
    public void deleteWorkflow(PurchaseShipDto model, List<PurchaseShipDetailDto> detailList) throws Exception{
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
    public ProcessDto convertProcessDto(PurchaseShipDto model, List<PurchaseShipDetailDto> DetailList) throws Exception {
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

    /**
     * 回写主表未执行量
     * @param mainSet
     * @return
     */
    public String updateBackMainExec(Set<String> mainSet){
        String errorCode = "";
        for (String parentCode : mainSet) {
            BaseListDto<PurchaseShipDetailDto> baseList = purchaseShipDetailService.selectByParentCode(parentCode);
            if (null!=baseList){
                List<PurchaseShipDetailDto> list = baseList.getList();

                int totalNum=0;
                double totalWeight=0d;
                for (PurchaseShipDetailDto thisDetail : list) {
                    totalNum += thisDetail.getExecCount();
                    totalWeight += thisDetail.getExecWeight();
                }

                PurchaseShip obj = new PurchaseShip();
                obj.setExecCount(totalNum);
                obj.setExecWeight(totalWeight);
                UpdateWrapper<PurchaseShip> uw = new UpdateWrapper<>();
                uw.eq("code",parentCode);
                int result = purchaseShipMapper.update(obj,uw);
                if(result<0){
                    errorCode = parentCode;
                }
            }
        }

        return errorCode;
    }
}