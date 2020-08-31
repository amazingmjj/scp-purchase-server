package org.zhd.purchase.controller;

import com.alibaba.fastjson.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchasePlanDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchasePlanDetailDto;
import org.xy.api.dto.purchase.PurchasePlanDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;
import org.xy.api.utils.TimeUtil;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
 * 采购计划 控制层
 *
 * @author samy
 */
@RestController
public class PurchasePlanController extends BaseController implements PurchasePlanDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchasePlan")
    public Map<String, Object> saveOrUpdate(PurchasePlanDto model) throws Exception {
        boolean ifAdd = model.getId() == null?true:false;
        Date planMonth = TimeUtil.str2Date(request.getParameter("planMonthStr"), "yyyy-MM");
        model.setPlanMonth(planMonth);
        String details = request.getParameter("details");
        System.out.println("detail:>>" + details);
        PurchasePlanDto dto = purchasePlanService.saveOrUpdate(model);
        System.out.println("dto id:>>" + dto.getId());
        //保存明细
        List<PurchasePlanDetailDto> detailList = JSONArray.parseArray(details, PurchasePlanDetailDto.class);
        List<Long> ids = new ArrayList<>();
        List<PurchasePlanDetailDto> remainList = new ArrayList<>();
        for (PurchasePlanDetailDto detailDto : detailList) {
            if (detailDto.getDataFlag() != null && detailDto.getDataFlag() == 2) {
                ids.add(detailDto.getId());
            } else {
                detailDto.setParentCode(dto.getCode());
                detailDto.setMemberCode(dto.getMemberCode());
                purchasePlanDetailService.saveOrUpdate(detailDto);
                remainList.add(detailDto);
            }
        }
        if (ids.size() > 0) {
            purchasePlanDetailService.delete(ids);
        }
        //处理审核
        if (ifAdd){
            purchasePlanService.startWorkflow(dto,remainList);
        }else {
            purchasePlanService.modifyWorkflow(dto,remainList);
        }
        return ApiUtil.responseCode();
    }

    @PostMapping("api/purchase/purchasePlan/list")
    public BaseListDto<PurchasePlanDto> selectPage(HttpServletRequest request) throws Exception {
        return purchasePlanService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchasePlanService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchasePlanService.selectById(id));
    }

    @PostMapping("api/purchase/purchasePlan/audit")
    public Map<String, Object> audit(PurchasePlanDto model) throws Exception {
        PurchasePlanDto dto = purchasePlanService.audit(model);
        System.out.println("dto id:>>" + dto.getId());
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }

    @PostMapping("api/purchase/purchasePlan/unAudit")
    public Map<String, Object> unAudit(PurchasePlanDto model) throws Exception {
        PurchasePlanDto dto = purchasePlanService.unAudit(model);
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }
}