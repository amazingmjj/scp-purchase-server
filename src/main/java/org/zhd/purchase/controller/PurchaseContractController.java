package org.zhd.purchase.controller;

import com.alibaba.fastjson.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchaseContractDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseContractDetailDto;
import org.xy.api.dto.purchase.PurchaseContractDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
* 采购合同 控制层
*
* @author samy
*/
@RestController
public class PurchaseContractController extends BaseController implements PurchaseContractDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchaseContract")
    public Map<String, Object> saveOrUpdate(PurchaseContractDto model) throws Exception {
        boolean ifAdd = model.getId() == null?true:false;
        String details = request.getParameter("details");
        System.out.println("detail:>>" + details);
        PurchaseContractDto dto = purchaseContractService.saveOrUpdate(model);
        System.out.println("dto id:>>" + dto.getId());

        List<PurchaseContractDetailDto> detailList = JSONArray.parseArray(details,PurchaseContractDetailDto.class);
        //回写引单--采购付款申请
        int back = purchaseContractDetailService.updateBackPayApply(detailList);
        if (back<0){
            throw new Exception("回写上级单据失败！");
        }

        //保存明细
        List<Long> ids = new ArrayList<>();
        List<PurchaseContractDetailDto> remainList = new ArrayList<>();
        for (PurchaseContractDetailDto detailDto : detailList){
            if (detailDto.getDataFlag() != null && detailDto.getDataFlag() == 2) {
                ids.add(detailDto.getId());
            }else {
                detailDto.setParentCode(dto.getCode());
                detailDto.setMemberCode(dto.getMemberCode());
                purchaseContractDetailService.saveOrUpdate(detailDto);
                remainList.add(detailDto);
            }
        }

        if (ids.size() > 0) {
            purchaseContractDetailService.delete(ids);
        }
        //处理审核
        if (ifAdd){
            purchaseContractService.startWorkflow(dto,remainList);
        }else {
            purchaseContractService.modifyWorkflow(dto,remainList);
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
    @PostMapping("api/purchase/purchaseContract/list")
    public BaseListDto<PurchaseContractDto> selectPage(HttpServletRequest request) throws Exception {
        return purchaseContractService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchaseContractService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchaseContractService.selectById(id));
    }

    @PostMapping("api/purchase/purchaseContract/audit")
    public Map<String, Object> audit(PurchaseContractDto model) throws Exception {
        PurchaseContractDto dto = purchaseContractService.audit(model);
        System.out.println("dto id:>>" + dto.getId());
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }

    @PostMapping("api/purchase/purchaseContract/unAudit")
    public Map<String, Object> unAudit(PurchaseContractDto model) throws Exception {
        PurchaseContractDto dto = purchaseContractService.unAudit(model);
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }
}