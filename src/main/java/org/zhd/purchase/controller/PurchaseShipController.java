package org.zhd.purchase.controller;

import com.alibaba.fastjson.JSONArray;
import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchaseShipDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.*;
import org.xy.api.enums.ApiEnum;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
* 采购发货集港 控制层
*
* @author samy
*/
@RestController
public class PurchaseShipController extends BaseController implements PurchaseShipDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchaseShip")
    public Map<String, Object> saveOrUpdate(PurchaseShipDto model) throws Exception {
        boolean ifAdd = model.getId() == null?true:false;
        String details = request.getParameter("details");
        System.out.println("detail:>>" + details);
        PurchaseShipDto dto = purchaseShipService.saveOrUpdate(model);
        System.out.println("dto id:>>" + dto.getId());

        List<PurchaseShipDetailDto> detailList = JSONArray.parseArray(details,PurchaseShipDetailDto.class);
        //回写引单--采购合同
        int back = purchaseShipDetailService.updateBackContract(detailList);
        if (back<0){
            throw new Exception("回写上级单据失败！");
        }

        //保存明细
        List<Long> ids = new ArrayList<>();
        List<PurchaseShipDetailDto> remainList = new ArrayList<>();
        for (PurchaseShipDetailDto detailDto : detailList){
            if (detailDto.getDataFlag() != null && detailDto.getDataFlag() == 2) {
                ids.add(detailDto.getId());
            }else {
                detailDto.setParentCode(dto.getCode());
                detailDto.setMemberCode(dto.getMemberCode());
                purchaseShipDetailService.saveOrUpdate(detailDto);
                remainList.add(detailDto);
            }
        }

        if (ids.size() > 0) {
            purchaseShipDetailService.delete(ids);
        }
        //处理审核
        if (ifAdd){
            purchaseShipService.startWorkflow(dto,remainList);
        }else {
            purchaseShipService.modifyWorkflow(dto,remainList);
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
    @PostMapping("api/purchase/purchaseShip/list")
    public BaseListDto<PurchaseShipDto> selectPage(HttpServletRequest request) throws Exception {
        return purchaseShipService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchaseShipService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchaseShipService.selectById(id));
    }

    @PostMapping("api/purchase/purchaseShip/audit")
    public Map<String, Object> audit(PurchaseShipDto model) throws Exception {
        PurchaseShipDto dto = purchaseShipService.audit(model);
        System.out.println("dto id:>>" + dto.getId());
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }

    @PostMapping("api/purchase/purchaseShip/unAudit")
    public Map<String, Object> unAudit(PurchaseShipDto model) throws Exception {
        PurchaseShipDto dto = purchaseShipService.unAudit(model);
        Map<String,Object> returnMap = ApiUtil.responseCode();
        returnMap.put("auditStatus",dto.getAuditStatus());
        return returnMap;
    }
}