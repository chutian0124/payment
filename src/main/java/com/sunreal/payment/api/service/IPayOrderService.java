package com.sunreal.payment.api.service;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

/**
 * @author: dingzhiwei
 * @date: 17/9/8
 * @description:
 */
public interface IPayOrderService {

    Map createPayOrder(String jsonParam);

    Map selectPayOrder(String jsonParam);

    Map selectPayOrderByMchIdAndPayOrderId(String jsonParam);

    Map selectPayOrderByMchIdAndMchOrderNo(String jsonParam);

    Map updateStatus4Ing(String jsonParam);

    Map updateStatus4Success(String jsonParam);

    Map updateStatus4Complete(String jsonParam);

    Map updateNotify(String jsonParam);

    Map createPayOrder(JSONObject payOrder);

    JSONObject queryPayOrder(String mchId, String payOrderId, String mchOrderNo, String executeNotify);

    String doWxPayReq(String tradeType, JSONObject payOrder, String resKey);

    String doAliPayReq(String channelId, JSONObject payOrder, String resKey);

}
