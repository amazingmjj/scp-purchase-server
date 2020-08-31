package org.zhd.purchase.controller;

import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchasePlanDetailDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchasePlanDetailDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 采购计划明细 控制层
 *
 * @author samy
 */
@RestController
public class PurchasePlanDetailController extends BaseController implements PurchasePlanDetailDpi {

    public Map<String, Object> saveOrUpdate(PurchasePlanDetailDto model) throws Exception {
        // FIXME DO SAVEORUPDATE ACTION
        return ApiUtil.responseDto(purchasePlanDetailService.saveOrUpdate(model));
    }

    @PostMapping("api/purchase/purchasePlanDetail/list")
    public BaseListDto<PurchasePlanDetailDto> selectPage(HttpServletRequest request) throws Exception {
        return purchasePlanDetailService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchasePlanDetailService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchasePlanDetailService.selectById(id));
    }

    @GetMapping("api/purchase/purchasePlanDetail/parentCode")
    public BaseListDto<PurchasePlanDetailDto> selectParentPage(String parentCode) {
        return purchasePlanDetailService.selectByParentCode(parentCode);
    }
}