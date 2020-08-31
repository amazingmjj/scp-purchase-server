package org.zhd.purchase.controller;

import org.springframework.web.bind.annotation.*;
import org.xy.api.dpi.purchase.PurchaseContractDetailDpi;
import org.xy.api.dto.BaseListDto;
import org.xy.api.dto.purchase.PurchaseContractDetailDto;
import org.xy.api.utils.ApiUtil;
import org.xy.api.utils.DaoUtil;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

/**
* 采购合同明细 控制层
*
* @author samy
*/
@RestController
public class PurchaseContractDetailController extends BaseController implements PurchaseContractDetailDpi {

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("api/purchase/purchaseContractDetail")
    public Map<String, Object> saveOrUpdate(PurchaseContractDetailDto model) throws Exception {
        return ApiUtil.responseDto(purchaseContractDetailService.saveOrUpdate(model));
    }

    /**
    * 当前页面 currentPage 1(第一页)
    * 页面行数 pageSize(页面行数)
    *
    * @param request
    * @return
    * @throws Exception
    */
    @PostMapping("api/purchase/purchaseContractDetail/list")
    public BaseListDto<PurchaseContractDetailDto> selectPage(HttpServletRequest request) throws Exception {
        return purchaseContractDetailService.selectPage(DaoUtil.requestMap2Map(request));
    }

    @Override
    public Map<String, Object> delete(List<Long> ids) throws Exception {
        purchaseContractDetailService.delete(ids);
        return ApiUtil.responseCode();
    }

    @Override
    public Map<String, Object> selectById(Long id) throws Exception {
        return ApiUtil.responseDto(purchaseContractDetailService.selectById(id));
    }

    @GetMapping("api/purchase/purchaseContractDetail/parentCode")
    public BaseListDto<PurchaseContractDetailDto> selectParentPage(String parentCode) {
        return purchaseContractDetailService.selectByParentCode(parentCode);
    }

    @Override
    public void updateBackExec(List<PurchaseContractDetailDto> changeDtoList) throws Exception{
        purchaseContractDetailService.updateBackExecInterface(changeDtoList);
    }
}