package com.sunreal.payment.api.controller;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.sunreal.payment.api.service.IMchInfoService;
import com.sunreal.payment.api.service.IPayOrderService;
import com.sunreal.payment.api.service.RefundOrderApiService;
import com.sunreal.payment.common.constant.PayConstant;
import com.sunreal.payment.common.util.MyLog;
import com.sunreal.payment.common.util.RpcUtil;
import com.sunreal.payment.common.util.XXPayUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能描述：退款接口
 *
 * @Author liy
 * @Date 2019/1/7
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class RefundOrderApiController {
    private final static MyLog _log = MyLog.getLog(RefundOrderApiController.class);



    @Autowired
    private IMchInfoService mchInfoService;
    @Autowired
    private RefundOrderApiService refundOrderApiService;
    @Autowired
    private IPayOrderService payOrderService;


    /**
     * 查询支付订单接口:
     * 1)先验证接口参数以及签名信息
     * 2)根据参数查询订单
     * 3)返回订单数据
     * @param params
     * @return
     */
    @RequestMapping(value = "/api/pay/refund_order")
    public String craetePayOrder(@RequestParam String params) {
        JSONObject po = JSONObject.parseObject(params);
        return craetePayOrder(po);
    }

    /**
     * 退款方法
     * @param params
     * @return
     */
    @RequestMapping(value = "/api/pay/refund_order", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String craetePayOrder(@RequestBody JSONObject params) {
        _log.info("###### 开始接收商户退款请求请求 ######");
        String logPrefix = "【商户退款】";
        try{
            JSONObject payContext = new JSONObject();
            // 验证参数有效性
            String errorMessage = validateParams(params, payContext);
            if (!"success".equalsIgnoreCase(errorMessage)) {
                _log.warn(errorMessage);
                return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, errorMessage, null, null));
            }
            _log.debug("请求参数及签名校验通过");
            String payOrderId = params.getString("payOrderId"); 	// 订单ID
            String refundReason = params.getString("refundReason"); 	// 退款原因
            String terminalId = params.getString("terminalId"); 	// 操作端
            Map<String, Object> p = new HashMap<String, Object>();
            p.put("payOrderId",payOrderId);
            String jsonParam = RpcUtil.createBaseParam(p);
            Map order = payOrderService.selectPayOrder(jsonParam);
            if(order.get("status").equals(PayConstant.PAY_STATUS_INIT)||order.get("status").equals(PayConstant.PAY_STATUS_PAYING)){
                _log.warn("该订单还未支付无法进行退款，订单编号："+payOrderId);
                return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "该订单还未支付无法进行退款，订单编号："+payOrderId, null, null));
            }else if(order.get("status").equals(PayConstant.PAY_STATUS_REFUND)){
                _log.warn("该订单已进行退款无法进行退款，订单编号："+payOrderId);
                return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "该订单已进行退款无法进行退款，订单编号："+payOrderId, null, null));
            }
            String channelId = order.get("channelId").toString();
            String result = "";
            switch (channelId) {
                case PayConstant.PAY_CHANNEL_WX_APP:
                    break;
                case PayConstant.PAY_CHANNEL_WX_JSAPI:
                    break;
                case PayConstant.PAY_CHANNEL_WX_NATIVE:
                    break;
                case PayConstant.PAY_CHANNEL_WX_MWEB:
                    break;
                case PayConstant.PAY_CHANNEL_ALIPAY_MOBILE:
                    result = refundOrderApiService.doAliRefundOrder(payOrderId, refundReason, terminalId);
                    break;
                case PayConstant.PAY_CHANNEL_ALIPAY_PC:
                    result = refundOrderApiService.doAliRefundOrder(payOrderId, refundReason, terminalId);
                    break;
                case PayConstant.PAY_CHANNEL_ALIPAY_WAP:
                    result = refundOrderApiService.doAliRefundOrder(payOrderId, refundReason, terminalId);
                    break;
                case PayConstant.PAY_CHANNEL_ALIPAY_QR:
                    result = refundOrderApiService.doAliRefundOrder(payOrderId, refundReason, terminalId);
                    break;
                default:
                    result = XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "不支持的支付渠道类型[channelId=" + channelId + "]", null, null));
            }
            Map<String, Object> retMap = XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
            retMap.put("result", result);
            return XXPayUtil.makeRetData(retMap, payContext.getString("resKey"));
        }catch (Exception e){
            _log.error("退款失败：",e);
        }
        return null;
    }


    /**
     * 验证查询订单请求参数,参数通过返回JSONObject对象,否则返回错误文本信息
     * @param params
     * @return
     */
    private String validateParams(JSONObject params, JSONObject payContext) {
        // 验证请求参数,参数有问题返回错误提示
        String errorMessage;
        // 支付参数
        String payOrderId = params.getString("payOrderId"); 			    // 商户ID
        String mchId = params.getString("mchId"); 			    // 商户ID

        String sign = params.getString("sign"); 				// 签名

        // 验证请求参数有效性（必选项）
        if(StringUtils.isBlank(payOrderId)) {
            errorMessage = "request params[mchId] error.";
            return errorMessage;
        }
        // 签名信息
        if (StringUtils.isEmpty(sign)) {
            errorMessage = "request params[sign] error.";
            return errorMessage;
        }

        // 查询商户信息
        JSONObject mchInfo = mchInfoService.getByMchId(mchId);
        if(mchInfo == null) {
            errorMessage = "Can't found mchInfo[mchId="+mchId+"] record in db.";
            return errorMessage;
        }
        if(mchInfo.getByte("state") != 1) {
            errorMessage = "mchInfo not available [mchId="+mchId+"] record in db.";
            return errorMessage;
        }

        String reqKey = mchInfo.getString("reqKey");
        if (StringUtils.isBlank(reqKey)) {
            errorMessage = "reqKey is null[mchId="+mchId+"] record in db.";
            return errorMessage;
        }
        payContext.put("resKey", mchInfo.getString("resKey"));

        // 验证签名数据
        boolean verifyFlag = XXPayUtil.verifyPaySign(params, reqKey);
        if(!verifyFlag) {
            errorMessage = "支付中心验证签名失败";
            return errorMessage;
        }

        return "success";
    }
}
