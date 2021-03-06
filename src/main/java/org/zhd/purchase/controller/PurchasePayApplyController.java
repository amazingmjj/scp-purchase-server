package org.zhd.purchase.controller;

import com.alibaba.fastjson.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchasePayApplyDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchasePayApplyDetailDto;
import org.xy.api.dto.purchase.PurchasePayApplyDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
 * 采购付款申请 控制层
 *
 * @author samy
 */
@RestController
public class PurchasePayApplyController extends BaseController implements PurchasePayApplyDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchasePayApply")
    public Map<String, Object> saveOrUpdate(PurchasePayApplyDto model) throws Exception {
        boolean ifAdd = model.getId() == null?true:false;
        String details = request.getParameter("details");
        System.out.println("detail:>>" + details);
        PurchasePayApplyDto dto = purchasePayApplyService.saveOrUpdate(model);
        System.out.println("dto id:>>" + dto.getId());

        List<PurchasePayApplyDetailDto> detailList = JSONArray.parseArray(details, PurchasePayApplyDetailDto.class);
        //回写引单
        int back = purchasePayApplyDetailService.updateBackQuery(detailList);
        if (back<0){
            throw new Exception("回写上级单据失败！");
        }
        //保存明细
        List<Long> ids = new ArrayList<>();
        List<PurchasePayApplyDetailDto> remainList = new ArrayList<>();
        for (PurchasePayApplyDetailDto detailDto : detailList) {
            if (detailDto.getDataFlag() != null && detailDto.getDataFlag() == 2) {
                ids.add(detailDto.getId());
            } else {
                detailDto.setParentCode(dto.getCode());
                detailDto.setMemberCode(dto.getMemberCode());
                purchasePayApplyDetailService.saveOrUpdate(detailDto);
                remainList.add(detailDto);
            }
        }
        if (ids.size() > 0) {
            purchasePayApplyDetailService.delete(ids);
        }
        //处理审核
        if (ifAdd){
            purchasePayApplyService.startWorkflow(dto,remainList);
        }else {
            purchasePayApplyService.modifyWorkflow(dto,remainList);
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
    @PostMapping("api/purchase/purchasePayApply/list")
    public BaseListDto<PurchasePayApplyDto> selectPage(HttpServletRequest request) throws Exception {
        return purchasePayApplyService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchasePayApplyService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchasePayApplyService.selectById(id));
    }

    @PostMapping("api/purchase/purchasePayApply/audit")
    public Map<String, Object> audit(PurchasePayApplyDto model) throws Exception {
        PurchasePayApplyDto dto = purchasePayApplyService.audit(model);
        System.out.println("dto id:>>" + dto.getId());
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }

    @PostMapping("api/purchase/purchasePayApply/unAudit")
    public Map<String, Object> unAudit(PurchasePayApplyDto model) throws Exception {
        PurchasePayApplyDto dto = purchasePayApplyService.unAudit(model);
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }
}