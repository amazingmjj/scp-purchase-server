package org.zhd.purchase.controller;

import com.alibaba.fastjson.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchaseDistributeDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseDistributeDetailDto;
import org.xy.api.dto.purchase.PurchaseDistributeDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
* 采购配货 控制层
*
* @author samy
*/
@RestController
public class PurchaseDistributeController extends BaseController implements PurchaseDistributeDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchaseDistribute")
    public Map<String, Object> saveOrUpdate(PurchaseDistributeDto model) throws Exception {
        boolean ifAdd = model.getId() == null?true:false;
        String details = request.getParameter("details");
        System.out.println("detail:>>" + details);
        PurchaseDistributeDto dto = purchaseDistributeService.saveOrUpdate(model);
        System.out.println("dto id:>>" + dto.getId());
        List<PurchaseDistributeDetailDto> detailList = JSONArray.parseArray(details,PurchaseDistributeDetailDto.class);
        List<Long> ids = new ArrayList<>();
        List<PurchaseDistributeDetailDto> remainList = new ArrayList<>();
        for (PurchaseDistributeDetailDto detailDto : detailList){
            if (detailDto.getDataFlag() != null && detailDto.getDataFlag() == 2) {
                ids.add(detailDto.getId());
            }else {
                detailDto.setParentCode(dto.getCode());
                detailDto.setMemberCode(dto.getMemberCode());
                purchaseDistributeDetailService.saveOrUpdate(detailDto);
                remainList.add(detailDto);
            }
        }

        if (ids.size() > 0) {
            purchaseDistributeDetailService.delete(ids);
        }
        //处理审核
        if (ifAdd){
            purchaseDistributeService.startWorkflow(dto,remainList);
        }else {
            purchaseDistributeService.modifyWorkflow(dto,remainList);
        }
        return ApiUtil.responseCode();
    }

    /**
    * 当前页面 currentPage 1(第一页)
    * 页面行数 pageSize(页面行数)
    *
    * @param request
    * @return
    * @throws Exception
    */
    @PostMapping("api/purchase/purchaseDistribute/list")
    public BaseListDto<PurchaseDistributeDto> selectPage(HttpServletRequest request) throws Exception {
        return purchaseDistributeService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchaseDistributeService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchaseDistributeService.selectById(id));
    }

    @PostMapping("api/purchase/purchaseDistribute/audit")
    public Map<String, Object> audit(PurchaseDistributeDto model) throws Exception {
        PurchaseDistributeDto dto = purchaseDistributeService.audit(model);
        System.out.println("dto id:>>" + dto.getId());
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }

    @PostMapping("api/purchase/purchaseDistribute/unAudit")
    public Map<String, Object> unAudit(PurchaseDistributeDto model) throws Exception {
        PurchaseDistributeDto dto = purchaseDistributeService.unAudit(model);
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }
}