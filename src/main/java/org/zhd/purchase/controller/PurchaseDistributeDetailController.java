package org.zhd.purchase.controller;

import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchaseDistributeDetailDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseDistributeDetailDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
* 采购配货明细 控制层
*
* @author samy
*/
@RestController
public class PurchaseDistributeDetailController extends BaseController implements PurchaseDistributeDetailDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchaseDistributeDetail")
    public Map<String, Object> saveOrUpdate(PurchaseDistributeDetailDto model) throws Exception {
        // FIXME DO SAVEORUPDATE ACTION
        return ApiUtil.responseDto(purchaseDistributeDetailService.saveOrUpdate(model));
    }

    /**
    * 当前页面 currentPage 1(第一页)
    * 页面行数 pageSize(页面行数)
    *
    * @param request
    * @return
    * @throws Exception
    */
    @PostMapping("api/purchase/purchaseDistributeDetail/list")
    public BaseListDto<PurchaseDistributeDetailDto> selectPage(HttpServletRequest request) throws Exception {
        return purchaseDistributeDetailService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchaseDistributeDetailService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchaseDistributeDetailService.selectById(id));
    }

    @GetMapping("api/purchase/purchaseDistributeDetail/parentCode")
    public BaseListDto<PurchaseDistributeDetailDto> selectParentPage(String parentCode) {
        return purchaseDistributeDetailService.selectByParentCode(parentCode);
    }
}