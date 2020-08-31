package org.zhd.purchase.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.zhd.purchase.service.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author juny
 */
@Controller
public class BaseController {
    @Autowired
    protected HttpServletRequest request;
    @Autowired
    protected PurchasePlanService purchasePlanService;
    @Autowired
    protected PurchasePlanDetailService purchasePlanDetailService;
    @Autowired
    protected PurchaseContractService purchaseContractService;
    @Autowired
    protected PurchaseContractDetailService purchaseContractDetailService;
    @Autowired
    protected PurchaseQueryService purchaseQueryService;
    @Autowired
    protected PurchaseQueryDetailService purchaseQueryDetailService;
    @Autowired
    protected PurchasePayApplyService purchasePayApplyService;
    @Autowired
    protected PurchasePayApplyDetailService purchasePayApplyDetailService;
    @Autowired
    protected PurchaseDistributeService purchaseDistributeService;
    @Autowired
    protected PurchaseDistributeDetailService purchaseDistributeDetailService;
    @Autowired
    protected PurchaseShipService purchaseShipService;
    @Autowired
    protected PurchaseShipDetailService purchaseShipDetailService;
}
