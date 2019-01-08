package com.sunreal.payment.api.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.sunreal.payment.api.service.INotifyPayService;
import com.sunreal.payment.common.util.JsonUtil;
import com.sunreal.payment.common.util.MyLog;
import com.sunreal.payment.dao.model.PayOrder;
import com.sunreal.payment.mgr.service.PayOrderService;

/**
 * @author dingzhiwei jmdhappy@126.com
 * @version V1.0
 * @Description: 接收处理微信通知
 * @date 2017-07-05
 * @Copyright: www.xxpay.org
 */
@RestController
public class Notify4WxPayController {

    private static final MyLog _log = MyLog.getLog(Notify4WxPayController.class);

    @Autowired
    private INotifyPayService notifyPayService;
    @Autowired
    private PayOrderService payOrderService;

    /**
     * 微信支付(统一下单接口)同步通知响应
     *
     * @param request
     * @return
     *
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    @GetMapping("/notify/pay/wxPayNotifyRes.htm")
    @ResponseBody
    public ModelAndView wxPayReturnRes(HttpServletRequest request) throws ServletException, IOException {
        _log.info("====== 开始接收微信支付同步回调 ======");
        _log.info(JsonUtil.object2Json(request.getParameterMap()));
        String payOrderId = request.getParameter("out_trade_no");
        PayOrder order = payOrderService.selectPayOrder(payOrderId);
        ModelAndView mv = new ModelAndView("redirect:" + order.getReturnUrl());
        _log.info("url:" + order.getReturnUrl());
        _log.info("====== 完成接收微信支付同步回调 ======");
        return mv;
    }

    /**
     * 微信支付(统一下单接口)后台通知响应
     *
     * @param request
     * @return
     *
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    @PostMapping("/notify/pay/wxPayNotifyRes.htm")
    @ResponseBody
    public String wxPayNotifyRes(HttpServletRequest request) throws ServletException, IOException {
        _log.info("====== 开始接收微信支付回调通知 ======");
        String notifyRes = doWxPayRes(request);
        _log.info("响应给微信:{}", notifyRes);
        _log.info("====== 完成接收微信支付回调通知 ======");
        return notifyRes;
    }

    public String doWxPayRes(HttpServletRequest request) throws ServletException, IOException {
        String logPrefix = "【微信支付回调通知】";
        String xmlResult = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
        _log.info("{}通知请求数据:reqStr={}", logPrefix, xmlResult);
        return notifyPayService.handleWxPayNotify(xmlResult);
    }

}
