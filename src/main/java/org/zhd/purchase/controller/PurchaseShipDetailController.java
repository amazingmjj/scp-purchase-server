package org.zhd.purchase.controller;

import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchaseShipDetailDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseShipDetailDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
* 采购发货集港明细 控制层
*
* @author samy
*/
@RestController
public class PurchaseShipDetailController extends BaseController implements PurchaseShipDetailDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchaseShipDetail")
    public Map<String, Object> saveOrUpdate(PurchaseShipDetailDto model) throws Exception {
        return ApiUtil.responseDto(purchaseShipDetailService.saveOrUpdate(model));
    }

    /**
    * 当前页面 currentPage 1(第一页)
    * 页面行数 pageSize(页面行数)
    *
    * @param request
    * @return
    * @throws Exception
    */
    @PostMapping("api/purchase/purchaseShipDetail/list")
    public BaseListDto<PurchaseShipDetailDto> selectPage(HttpServletRequest request) throws Exception {
        return purchaseShipDetailService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchaseShipDetailService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchaseShipDetailService.selectById(id));
    }

    @GetMapping("api/purchase/purchaseShipDetail/parentCode")
    public BaseListDto<PurchaseShipDetailDto> selectParentPage(String parentCode) {
        return purchaseShipDetailService.selectByParentCode(parentCode);
    }

    @Override
    public void updateBackExec(List<PurchaseShipDetailDto> changeDtoList) throws Exception{
        purchaseShipDetailService.updateBackExecInterface(changeDtoList);
    }
}