package org.zhd.purchase.controller;

import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchaseQueryDetailDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseQueryDetailDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
 * 采购询价明细 控制层
 *
 * @author samy
 */
@RestController
public class PurchaseQueryDetailController extends BaseController implements PurchaseQueryDetailDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchaseQueryDetail")
    public Map<String, Object> saveOrUpdate(PurchaseQueryDetailDto model) throws Exception {
        // FIXME DO SAVEORUPDATE ACTION
        return ApiUtil.responseDto(purchaseQueryDetailService.saveOrUpdate(model));
    }

    /**
     * 当前页面 currentPage 1(第一页)
     * 页面行数 pageSize(页面行数)
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("api/purchase/purchaseQueryDetail/list")
    public BaseListDto<PurchaseQueryDetailDto> selectPage(HttpServletRequest request) throws Exception {
        return purchaseQueryDetailService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchaseQueryDetailService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchaseQueryDetailService.selectById(id));
    }

    @GetMapping("api/purchase/purchaseQueryDetail/parentCode")
    public BaseListDto<PurchaseQueryDetailDto> selectParentPage(String parentCode) {
        return purchaseQueryDetailService.selectByParentCode(parentCode);
    }
}