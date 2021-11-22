package com.sixshop.payment.service;

import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PaymentService {

    /* pay request page */
    @GetMapping(value = "/{siteLink}/payment/request/{pgType}/{customerNo}/{orderNo}")
    public String displayShopPayRequestPage(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response,
                                            @RequestParam Map<String, String> params, @PathVariable String siteLink,
                                            @PathVariable String pgType, @PathVariable int customerNo, @PathVariable int orderNo) {

        Site site = designService.getSiteBySiteLink(siteLink);
        Member owner = memberService.getMemberByMemberNo(site.getMemberNo());

        if (shopOrderService.isNotAvailable(owner)) {
            return CommonUtil.redirectToErrorPage(modelMap, BACK_TO_PREV_PAGE,
                    OrderRestrictedType.find(owner.getPlan()).getErrorCode());
        }

        params.put("pgType", pgType);
        params.put("customerNo", String.valueOf(customerNo));
        params.put("orderNo", String.valueOf(orderNo));

        modelMap.put("domainCheck", displayService.checkDomainOwnership(request, modelMap));

        String process = "";

        if (StringUtils.equals(pgType, "noPayMethod") || StringUtils.equals(pgType, "withoutBank")) {
            process = "success";
        }

        process = request";
    }

    /* pay success page */
    @RequestMapping(value = "/{siteLink}/payment/success/{pgType}/{customerNo}/{orderNo}", method = {RequestMethod.POST,
            RequestMethod.GET})
    public String displayPGSuccess(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response,
                                   @RequestParam Map<String, String> params, @PathVariable String siteLink,
                                   @PathVariable String pgType, @PathVariable int customerNo, @PathVariable String orderNo,
                                   @RequestParam(required = false, defaultValue = "0") String successProcess) {
        params.put("pgType", pgType);
        params.put("customerNo", String.valueOf(customerNo));
        params.put("orderNo", orderNo);
        params.put("successProcess", successProcess);
        return handlePaymentProcess(modelMap, request, response, params, siteLink, SUCCESS);
    }

    private String handlePaymentProcess(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response,
                                        Map<String, String> params, String siteLink, String process) {
        int customerNo = Integer
                .parseInt(StringUtils.isEmpty(params.get("customerNo")) ? "0" : params.get("customerNo"));
        int orderNo = Integer.parseInt(StringUtils.isEmpty(params.get("orderNo")) ? "0" : params.get("orderNo"));

        String pgType = params.get("pgType");
        String successProcess = params.get("successProcess");
        BrowserDetectUtil.putUserDevice(modelMap, request);
        BrowserDetectUtil.putIsIOS(modelMap, request);

        Site site = designService.getSiteBySiteLink(siteLink, "published");
        Member owner = memberService.getMemberByMemberNo(site.getMemberNo());
        int ownerMemberNo = owner.getMemberNo();
        ShopOrder shopOrder = shopPaymentService.getShopOrderByOrderNo(ownerMemberNo, orderNo);
        ShopCustomer shopCustomer = shopCustomerService.getShopCustomerByCustomerNo(ownerMemberNo, customerNo);

        modelMap.put("serverDomain", StringUtils.replace(serverDomain, "https://", "http://"));
        modelMap.put("favicon", (!StringUtils.equals(owner.getPlan(), "trial") ? site.getFavicon()
                : "/resources/images/common/favicon.ico"));

        addonService.putAddonParameter(modelMap, params, addonService.getInstalledAddonsAllByMemberNoAndPlan(owner));
        if (shopOrder.getOrderSequence() != 0 && !StringUtils.equals(successProcess, "done")) {
            CommonUtil.clearModelMapExceptLoginSession(modelMap);
            displayService.getAndPutSiteDomainAddress(request, modelMap, owner.getMemberNo());

            return CommonUtil
                    .redirectToErrorPage(modelMap, JsonUtils.GO_TO_HOME_URL, "ALREADY_FINISHED_ORDER", site.getLocale());
        }

        String result = "";
        if (StringUtils.equals(process, "request")) {
            //get order Sequence
            int orderSequence = shopPaymentService.getLastSequence(ownerMemberNo) + 1;
            shopOrder.setOrderSequence(orderSequence);

            ShopSettingPayment shopSettingPayment = shopSettingService.getShopSettingPaymentByMemberNo(ownerMemberNo);
            modelMap.put("shopSettingPayment", shopSettingPayment);

            DecimalFormat df = new DecimalFormat("#,###");

            String orderOptions = shopOrder.getOrderOptions();
            JSONObject jsonObject = JsonUtils.parse(orderOptions);

            BigDecimal taxFree = BigDecimal.ZERO;
            BigDecimal tax = BigDecimal.ZERO;
            if (jsonObject.get("taxFree") != null) {
                taxFree = new BigDecimal(String.valueOf(jsonObject.get("taxFree")));
            }
            if (jsonObject.get("tax") != null) {
                tax = new BigDecimal(String.valueOf(jsonObject.get("tax")));
            }
            request.setAttribute("orderPrice", df.format(shopOrder.getOrderPrice()));

            if (StringUtils.equals(pgType, "nicepay")) {
                String merchantKey = "";
                String merchantId = "";
                int vBankExpireDays = 7;
                if (null != shopSettingPayment) {
                    merchantKey = (StringUtils.equals(shopSettingPayment.getVerifiedPg(), "no")
                            ? "33F49GnCMS1mFYlGXisbUDzVf2ATWCl9k3R++d5hDd3Frmuos/XLx8XhXpe+LDYAbpGKZYSwtlyyLOtS/8aD7A=="
                            : shopSettingPayment.getMerchantKey());
                    merchantId = (StringUtils.equals(shopSettingPayment.getVerifiedPg(), "no") ? "nictest00m"
                            : shopSettingPayment.getMerchantId());
                    vBankExpireDays = shopSettingPayment.getvBankExpireDays();

                }
                request.setAttribute("intOrderPrice", shopOrder.getOrderPrice().intValue());
                request.setAttribute("merchantKey", merchantKey);
                request.setAttribute("merchantId", merchantId);

                //calculate tax free amount

                // tax 항상 버림
                BigDecimal floorTax = tax.setScale(0, BigDecimal.ROUND_FLOOR);
                String[] orderCartList = shopOrder.getOrderCartList().split(",");
                //goodsCnt
                request.setAttribute("goodsCnt", orderCartList.length);
                modelMap.put("supplyAmt",
                        shopOrder.getOrderPrice().subtract(floorTax).subtract(taxFree).max(BigDecimal.ZERO)
                                .setScale(0, BigDecimal.ROUND_HALF_UP));

                modelMap.put("vBankExpireDay", LocalDateTime.now().plusDays(vBankExpireDays).format(
                        DateTimeFormatter.ofPattern("uuuuMMdd")));
                modelMap.put("goodsVat", floorTax.intValue());
                modelMap.put("taxFreeAmt", taxFree.intValue());
            } else if ((StringUtils.equals(pgType, "inicisPay"))) {
                String merchantId = "";
                int vBankExpireDays = 7;
                if (null != shopSettingPayment) {
                    merchantId = shopSettingPayment.getMerchantId();
                    vBankExpireDays = shopSettingPayment.getvBankExpireDays();
                }

                displayService.getAndPutSiteDomainAddress(request, modelMap, owner.getMemberNo());

                request.setAttribute("intOrderPrice", shopOrder.getOrderPrice().intValue());
                request.setAttribute("signKey", INICIS_SIGN_KEY);
                request.setAttribute("mid", merchantId);
                request.setAttribute("oid", shopOrder.getOrderUniqueNo());

                String[] orderCartList = shopOrder.getOrderCartList().split(",");
                //goodsCnt
                request.setAttribute("goodsCnt", orderCartList.length);

                modelMap.put("vBankExpireDay", LocalDateTime.now().plusDays(vBankExpireDays).format(
                        DateTimeFormatter.ofPattern("uuuuMMdd")));
                modelMap.put("tax", tax.setScale(0, BigDecimal.ROUND_FLOOR));
                modelMap.put("taxfree", taxFree.intValue());
            }

            //휴대폰 결제용 주문제목
            String orderTitle = shopOrder.getOrderTitle();
            if (StringUtils.equals(shopOrder.getPayMethod(), "CELLPHONE")) {
                StringBuilder sb = new StringBuilder();
                int curLen = 0;
                String curChar;
                int maxLen = 49;

                for (int i = 0; i < orderTitle.length(); i++) {
                    curChar = orderTitle.substring(i, i + 1);
                    curLen += curChar.getBytes().length;
                    if (curLen > maxLen) {
                        break;
                    } else {
                        sb.append(curChar);
                    }
                }
                orderTitle = sb.toString();
            }

            modelMap.put("orderTitle", orderTitle);

            String orderPhoneNumber = "";
            String[] orderPhoneList;
            if (customerNo == 0) {
                shopOrder.setCustomerNo(0);
                orderPhoneList = shopOrder.getGuestOrderPhone().split(",");
            } else {
                orderPhoneList = shopCustomer.getCustomerPhone().split(",");
            }
            for (int i = 0; i < orderPhoneList.length; i++) {
                orderPhoneNumber += (i > 0 ? "-" : "") + orderPhoneList[i].replaceAll("\"", "");
            }
            modelMap.put("orderPhoneNumber", orderPhoneNumber);
        } else if (StringUtils.equals(process, SUCCESS)) {
            params.put("siteLink", siteLink);
            if (StringUtils.equals(pgType, "withoutBank") || StringUtils.equals(pgType, "noPayMethod")) {
                result = shopPaymentService.handlePaymentSuccess(request, modelMap, params);
            } else if (StringUtils.equals(pgType, "nicepay")) {
                result = shopPaymentService.checkNicePaySuccess(modelMap, request, response, params);
            } else if (StringUtils.equals(pgType, "inicisPay")) {
                if (!StringUtils.equals(successProcess, "done")) {
                    result = shopPaymentService.checkInicisPaySuccess(modelMap, request, response, params);
                } else {
                    if (shopOrder.getOrderSequence() != 0) {
                        result = SUCCESS;
                    } else {
                        result = shopOrder.getOrderError();
                    }
                }
            } else if (StringUtils.equals(pgType, "kakaopay")) {
                if (!StringUtils.equals(successProcess, "done")) {
                    result = shopPaymentService.checkPaymentSuccess(request, modelMap, params, pgType, shopOrder);
                } else {
                    if (shopOrder.getOrderSequence() != 0) {
                        result = SUCCESS;
                    } else {
                        result = shopOrder.getOrderError();
                    }
                }
            } else if (StringUtils.equals(pgType, "payletter")) {
                request.setAttribute("pgType", pgType);
                if (StringUtils.equals(successProcess, "done")) {
                    if (shopOrder.getOrderSequence() != 0) {
                        result = SUCCESS;
                    } else {
                        result = shopOrder.getOrderError();
                    }
                } else {
                    result = payletterService.checkPaymentSuccess(request, modelMap, params, shopOrder);
                }
            } else if (StringUtils.equals(pgType, "toss")) {
                request.setAttribute("pgType", pgType);
                if (!StringUtils.equals(successProcess, "done")) {
                    result = tossPaymentService
                            .checkPaymentSuccess(request, modelMap, params, shopOrder.getOrderUniqueNo());
                } else {
                    if (shopOrder.getOrderSequence() != 0) {
                        result = SUCCESS;
                    } else {
                        result = shopOrder.getOrderError();
                    }
                }
            }
        }

        PageWithProps pageWithProps = new PageWithProps();
        pageWithProps.setSiteLink(site.getSiteLink());
        pageWithProps.setPageOptions("{}");
        pageWithProps.setPageContents("{}");
        pageWithProps.setPageType("paymentPage");
        params.put("pageLink", "paymentPage");

        displayService.putResourcePath(modelMap, request);
        displayService.putShopFilesAndOthers(modelMap, request, params, site, owner);
        designService.handleDesignProduct(modelMap, request, response, params, site, owner);

        modelMap.put("pageWithProps", pageWithProps);
        modelMap.put("orderPrice", shopOrder.getOrderPrice());
        modelMap.put("site", site);
        modelMap.put("siteURI", displayService.getSiteURI(request, site));
        modelMap.put("ownerMemberNo", ownerMemberNo);
        modelMap.put("sitePlan", owner.getPlan());
        modelMap.put("owner", owner);
        modelMap.put("customerNo", customerNo);
        modelMap.put("shopOrder", shopOrder);
        if (customerNo != 0) {
            modelMap.put("shopCustomer", shopCustomer);
        }
        modelMap.put("pgType", pgType);
        modelMap.put("group", "payment");
        modelMap.put("nowPage", "error");

        String payMethod = shopOrder.getPayMethod();
        String payMethodText = CommonUtil.getParameterText(payMethod, "english", "korean");
        modelMap.put("payMethodText", payMethodText);

        if (StringUtils.equals(process, "request")) {
            if (StringUtils.equals(pgType, "toss") || StringUtils.equals(pgType, "kakaopay") || StringUtils
                    .equals(pgType, "payletter")) {
                return "/6shop/pgModule/payRequest";
            } else if (StringUtils.equals(BrowserDetectUtil.getDevice(request), "mobile") && (
                    StringUtils.equals(pgType, "nicepay") || StringUtils.equals(pgType, "inicisPay"))) {
                if (StringUtils.equals(pgType, "inicisPay")) {
                    Optional.ofNullable(modelMap.get("shopCustomer")).map(x -> (ShopCustomer) x)
                            .map(ShopCustomer::getCustomerName)
                            .map(SixshopStringUtils::removeSpecialCharacters)
                            .ifPresent(s -> modelMap.put("plainCustomerName", s));
                    Optional.ofNullable(shopOrder)
                            .map(ShopOrder::getGuestOrderName)
                            .map(SixshopStringUtils::removeSpecialCharacters)
                            .ifPresent(s -> modelMap.put("plainGuestName", s));
                    Optional.ofNullable(modelMap.get("orderTitle")).map(String::valueOf)
                            .map(SixshopStringUtils::removeSpecialCharacters)
                            .ifPresent(s -> modelMap.put("plainGoodsName", s));
                }
                pageWithProps.setPageLink(pgType + "MobilePayRequest");
                displayService.getAndPutSiteDomainAddress(request, modelMap, ownerMemberNo);
                return "/6shop/pgModule/" + pgType + "/mobile/payRequest";
            } else {
                pageWithProps.setPageLink(pgType + "PayRequest");
                return "/6shop/pgModule/" + pgType + "/payRequest";
            }
        } else if (StringUtils.equals(process, SUCCESS)) {
            if (StringUtils.equals(result, SUCCESS)) {
                ShopOrder paySuccessShopOrder = shopPaymentService.getShopOrderByOrderNo(ownerMemberNo, orderNo);

                displayService.getAndPutSiteDomainAddress(request, modelMap, ownerMemberNo);

                if (StringUtils.isNotEmpty(paySuccessShopOrder.getOrderBankExpireDate())) {
                    String orderBankExpireDate = paySuccessShopOrder.getOrderBankExpireDate();
                    String bankExpireDateText = orderBankExpireDate.substring(0, 10).replaceAll("-", ".");
                    paySuccessShopOrder.setBankExpireDateText(bankExpireDateText);
                }

                //modelMap put
                Currency currency = site.getCurrency();

                modelMap.put("paySuccessShopOrder", paySuccessShopOrder);
                modelMap.put("paySuccessPrice", currency.format(paySuccessShopOrder.getOrderPrice()));
                modelMap.put("payCurrency", currency);

                List<ShopOrderInfo> shopOrderInfoList = shopPaymentService
                        .getShopOrderInfosByOrderNo(ownerMemberNo, orderNo);
                List<JSONObject> orderProductDetailList = new ArrayList<>();
                for (ShopOrderInfo shopOrderInfo : shopOrderInfoList) {
                    JSONObject json = new JSONObject();
                    String productName = shopOrderInfo.getOrderProductName();
                    if (StringUtils.isNotEmpty(shopOrderInfo.getOrderProductOptionName())) {
                        productName =
                                productName + " - " + shopOrderInfo.getOrderProductOptionName().replaceAll("<br>", " / ");
                    }
                    int productQuantity = shopOrderInfo.getOrderQuantity();
                    String productSKU = shopOrderInfo.getOrderProductSKU();
                    if (StringUtils.isEmpty(productSKU)) {
                        productSKU = "";
                    }
                    ShopProduct shopProduct = shopProductService
                            .getShopProductByMemberNoAndProductNo(shopOrderInfo.getMemberNo(),
                                    shopOrderInfo.getProductNo());
                    String productAddress = shopProduct.getProductAddress();
                    BigDecimal listPrice = shopProduct.getProductPrice();
                    BigDecimal sellPrice = shopProduct.getProductDiscountPrice();
                    if (sellPrice.compareTo(BigDecimal.ZERO) == -1) {
                        sellPrice = listPrice;
                    }

                    if (StringUtils.equals(shopProduct.getProductType(), ShopProduct.QUANTITY_LIMIT)) {
                        quantityLimitEventService.add(shopProduct.getProductNo(), orderNo);
                    }

                    JsonUtils.put(json, "string_productSKU", productSKU);
                    JsonUtils.put(json, "string_productName", productName);
                    JsonUtils.put(json, "string_productAddress", productAddress);
                    JsonUtils.put(json, "double_productPrice", sellPrice);
                    JsonUtils.put(json, "double_productListPrice", listPrice);
                    JsonUtils.put(json, "int_productQuantity", productQuantity);

                    orderProductDetailList.add(json);
                }
                modelMap.put("orderProductDetailList", orderProductDetailList);

                AddonCodeInjection addonCodeInjection = (AddonCodeInjection) modelMap.get("addonCodeInjection");
                modelMap.remove("addonCodeInjection");
                designService
                        .convertCustomCodeCustomerVarsForPaymentCompletePage(addonCodeInjection, paySuccessShopOrder,
                                orderProductDetailList);
                modelMap.put("customCodePaymentCompleteHead", addonCodeInjection.getPaymentCompleteHead());
                modelMap.put("customCodePaymentCompleteBody", addonCodeInjection.getPaymentCompleteBody());

                // 쿠폰, 할인코드, 적립금 레디스 초기화
                redisTemplate.delete("customer:" + customerNo + ":coupon");
                redisTemplate.delete("customer:" + customerNo + ":promotion");
                redisTemplate.delete("customer:" + customerNo + ":point");

                if (StringUtils.equals(pgType, "nicepay") || StringUtils.equals(pgType, "inicisPay")) {
                    if (StringUtils.equals(BrowserDetectUtil.getDevice(request), "mobile")) {
                        if (StringUtils.equals(payMethod, "BANK") && StringUtils.equals(pgType, "inicisPay")
                                && !StringUtils.equals(successProcess, "done")) {
                            request.setAttribute("bankSuccess", result);
                            return "/6shop/pgModule/inicisPay/mobile/bankCheck";
                        } else {
                            pageWithProps.setPageLink(pgType + "MobilePaySuccess");
                            return "/6shop/pgModule/nicepay/mobile/paySuccess";
                        }
                    } else {
                        pageWithProps.setPageLink(pgType + "PaySuccess");
                        return "/6shop/pgModule/nicepay/paySuccess";
                    }
                } else if (StringUtils.equals(pgType, "withoutBank")) {
                    pageWithProps.setPageLink(pgType + "PaySuccess");
                    return "/6shop/pgModule/withoutBank/paySuccess";
                } else if (StringUtils.equals(pgType, "toss") || StringUtils.equals(pgType, "kakaopay") || StringUtils
                        .equals(pgType, "payletter")) {
                    pageWithProps.setPageLink(pgType + "PaySuccess");
                    return "/6shop/pgModule/paySuccess";
                } else {
                    pageWithProps.setPageLink(pgType + "PaySuccess");
                    return "/6shop/pgModule/withoutPay/paySuccess";
                }
            } else {
                displayService.getAndPutSiteDomainAddress(request, modelMap, owner.getMemberNo());
                modelMap.put("errorMsg", result);
                shopOrder.setOrderError(result);
                shopPaymentService.updateShopOrderError(shopOrder);
                return "/6shop/pgModule/paymentError";
            }
        } else {
            return "redirect:" + displayService.getSiteURI(request, site);
        }
    }
}
