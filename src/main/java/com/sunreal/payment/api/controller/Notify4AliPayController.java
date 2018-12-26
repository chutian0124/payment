package com.sunreal.payment.api.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sunreal.payment.api.service.INotifyPayService;
import com.sunreal.payment.common.constant.PayConstant;
import com.sunreal.payment.common.util.JsonUtil;
import com.sunreal.payment.common.util.MyLog;
import com.sunreal.payment.dao.model.PayOrder;
import com.sunreal.payment.mgr.service.PayOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * @Description: 接收处理支付宝通知
 * @author dingzhiwei jmdhappy@126.com
 * @date 2017-07-05
 * @version V1.0
 * @Copyright: www.xxpay.org
 */
@RestController
public class Notify4AliPayController {

	private static final MyLog _log = MyLog.getLog(Notify4AliPayController.class);

	@Autowired
	private INotifyPayService notifyPayService;
	@Autowired
	private PayOrderService payOrderService;

	@GetMapping(value = "/notify/pay/aliPayNotifyRes.htm")
	@ResponseBody
	public ModelAndView aliPayReturnRes(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		_log.info("====== 开始接收支付宝支付同步回调 ======");
		_log.info(JsonUtil.object2Json(request.getParameterMap()));
        String payOrderId=request.getParameter("out_trade_no");
        PayOrder order=payOrderService.selectPayOrder(payOrderId);
        //ModelAndView mv = new ModelAndView("redirect:http://centee.wezoz.com/index.jsp");
        ModelAndView mv = new ModelAndView("redirect:"+order.getReturnUrl());
        _log.info("url:"+order.getReturnUrl());
        _log.info("====== 完成接收支付宝支付同步回调 ======");
        return mv;
    }
	/**
	 * 支付宝移动支付后台通知响应
	 * @param request
	 * @return
	 * @throws javax.servlet.ServletException
	 * @throws java.io.IOException
     */
	@PostMapping(value = "/notify/pay/aliPayNotifyRes.htm")
	@ResponseBody
	public String aliPayNotifyRes(HttpServletRequest request) throws ServletException, IOException {
		_log.info("====== 开始接收支付宝支付回调通知 ======");
		String notifyRes = doAliPayRes(request);
		_log.info("响应给支付宝:{}", notifyRes);
		_log.info("====== 完成接收支付宝支付回调通知 ======");
		return notifyRes;
	}

	public String doAliPayRes(HttpServletRequest request) throws ServletException, IOException {
		String logPrefix = "【支付宝支付回调通知】";
		//获取支付宝POST过来反馈信息
		Map<String,String> params = new HashMap<String,String>();
		Map requestParams = request.getParameterMap();
		for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			String[] values = (String[]) requestParams.get(name);
			String valueStr = "";
			for (int i = 0; i < values.length; i++) {
				valueStr = (i == values.length - 1) ? valueStr + values[i]
						: valueStr + values[i] + ",";
			}
			//乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
			//valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");
			params.put(name, valueStr);
		}
		_log.info("{}通知请求数据:reqStr={}", logPrefix, params);
		if(params.isEmpty()) {
			_log.error("{}请求参数为空", logPrefix);
			return PayConstant.RETURN_ALIPAY_VALUE_FAIL;
		}
		return notifyPayService.handleAliPayNotify(params);
	}

}
