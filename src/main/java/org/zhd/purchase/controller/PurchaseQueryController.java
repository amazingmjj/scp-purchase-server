package org.zhd.purchase.controller;

import com.alibaba.fastjson.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchaseQueryDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseQueryDetailDto;
import org.xy.api.dto.purchase.PurchaseQueryDto;
import org.xy.api.enums.ApiEnum;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
 * 采购询价 控制层
 *
 * @author samy
 */
@RestController
public class PurchaseQueryController extends BaseController implements PurchaseQueryDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchaseQuery")
    public Map<String, Object> saveOrUpdate(PurchaseQueryDto model) throws Exception {
        boolean ifAdd = model.getId() == null?true:false;
        String details = request.getParameter("details");
        System.out.println("detail:>>" + details);
        PurchaseQueryDto dto = purchaseQueryService.saveOrUpdate(model);
        System.out.println("dto id:>>" + dto.getId());

        List<PurchaseQueryDetailDto> detailList = JSONArray.parseArray(details, PurchaseQueryDetailDto.class);
        //回写引单
        int back = purchaseQueryDetailService.updateBackDistribute(detailList);
        if (back<0){
            throw new Exception("回写上级单据失败！");
        }

        // 保存明细
        List<Long> ids = new ArrayList<>();
        List<PurchaseQueryDetailDto> remainList = new ArrayList<>();
        for (PurchaseQueryDetailDto detailDto : detailList) {
            if (detailDto.getDataFlag() != null && detailDto.getDataFlag() == 2) {
                ids.add(detailDto.getId());
            } else {
                detailDto.setParentCode(dto.getCode());
                detailDto.setMemberCode(dto.getMemberCode());
                purchaseQueryDetailService.saveOrUpdate(detailDto);
                remainList.add(detailDto);
            }
        }
        if (ids.size() > 0) {
            purchaseQueryDetailService.delete(ids);
        }
        //处理审核
        if (ifAdd){
            purchaseQueryService.startWorkflow(dto,remainList);
        }else {
            purchaseQueryService.modifyWorkflow(dto,remainList);
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
    @PostMapping("api/purchase/purchaseQuery/list")
    public BaseListDto<PurchaseQueryDto> selectPage(HttpServletRequest request) throws Exception {
        return purchaseQueryService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) {
        try {
            purchaseQueryService.delete(ids);
            return ApiUtil.responseCode();
        } catch (Exception e){
            return ApiUtil.responseCode(null,ApiEnum.FAILURE,e.getMessage());
        }
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchaseQueryService.selectById(id));
    }

    @PostMapping("api/purchase/purchaseQuery/audit")
    public Map<String, Object> audit(PurchaseQueryDto model) throws Exception {
        PurchaseQueryDto dto = purchaseQueryService.audit(model);
        System.out.println("dto id:>>" + dto.getId());
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }

    @PostMapping("api/purchase/purchaseQuery/unAudit")
    public Map<String, Object> unAudit(PurchaseQueryDto model) throws Exception {
        PurchaseQueryDto dto = purchaseQueryService.unAudit(model);
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }
}