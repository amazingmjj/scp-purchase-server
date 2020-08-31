package org.zhd.purchase.controller;

import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchasePayApplyDetailDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchasePayApplyDetailDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
 * 采购付款申请明细 控制层
 *
 * @author samy
 */
@RestController
public class PurchasePayApplyDetailController extends BaseController implements PurchasePayApplyDetailDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchasePayApplyDetail")
    public Map<String, Object> saveOrUpdate(PurchasePayApplyDetailDto model) throws Exception {
        // FIXME DO SAVEORUPDATE ACTION
        return ApiUtil.responseDto(purchasePayApplyDetailService.saveOrUpdate(model));
    }

    /**
     * 当前页面 currentPage 1(第一页)
     * 页面行数 pageSize(页面行数)
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("api/purchase/purchasePayApplyDetail/list")
    public BaseListDto<PurchasePayApplyDetailDto> selectPage(HttpServletRequest request) throws Exception {
        return purchasePayApplyDetailService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchasePayApplyDetailService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchasePayApplyDetailService.selectById(id));
    }

    @GetMapping("api/purchase/purchasePayApplyDetail/parentCode")
    public BaseListDto<PurchasePayApplyDetailDto> selectParentPage(String parentCode) {
        return purchasePayApplyDetailService.selectByParentCode(parentCode);
    }
}