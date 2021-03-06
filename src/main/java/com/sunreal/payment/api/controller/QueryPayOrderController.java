package com.sunreal.payment.api.controller;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sunreal.payment.api.service.IMchInfoService;
import com.sunreal.payment.api.service.IPayOrderService;
import com.sunreal.payment.common.constant.PayConstant;
import com.sunreal.payment.common.util.MyLog;
import com.sunreal.payment.common.util.XXPayUtil;
import com.sunreal.payment.dao.model.PayOrder;

/**
 * @author dingzhiwei jmdhappy@126.com
 * @version V1.0
 * @Description: 支付订单查询
 * @date 2017-08-31
 * @Copyright: www.xxpay.org
 */
@RestController
public class QueryPayOrderController {

    private final MyLog _log = MyLog.getLog(QueryPayOrderController.class);

    @Autowired
    private IPayOrderService payOrderService;

    @Autowired
    private IMchInfoService mchInfoService;

    @RequestMapping(value = "/api/pay/isOrderFinished")
    public String queryPayOrder(@RequestParam String mchId, String payOrderId) {
        _log.info("查询参数，mchId:{}" + mchId);
        _log.info("查询参数，payOrderId:{}" + payOrderId);
        JSONObject payOrder = payOrderService.queryPayOrder(mchId, payOrderId, null, "false");
        _log.info("查询支付订单,结果:{}", payOrder);
        if (payOrder != null && payOrder.get("status") != null && (Integer) payOrder.get("status") > 1) {
            return "Y";
        } else {
            return "N";
        }
    }
    @RequestMapping(value = "/api/pay/isOrderFinish")
    public String queryPayOrders(@RequestBody PayOrder order) {
        _log.info("查询参数，mchId:{}" + order.getMchId());
        _log.info("查询参数，payOrderId:{}" + order.getPayOrderId());
        JSONObject payOrder = payOrderService.queryPayOrder(order.getMchId(), order.getPayOrderId(), null, "false");
        _log.info("查询支付订单,结果:{}", payOrder);
        if (payOrder != null && payOrder.get("status") != null && (Integer) payOrder.get("status") > 1) {
            return "Y";
        } else {
            return "N";
        }
    }

    /**
     * 查询支付订单接口:
     * 1)先验证接口参数以及签名信息
     * 2)根据参数查询订单
     * 3)返回订单数据
     *
     * @param params
     * @return
     */
    @RequestMapping(value = "/api/pay/query_order")
    public String queryPayOrder(@RequestParam String params) {
        JSONObject po = JSONObject.parseObject(params);
        return queryPayOrder(po);
    }

    @RequestMapping(value = "/api/pay/query_order", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String queryPayOrder(@RequestBody JSONObject params) {
        _log.info("###### 开始接收商户查询支付订单请求 ######");
        String logPrefix = "【商户支付订单查询】";
        try {
            JSONObject payContext = new JSONObject();
            // 验证参数有效性
            String errorMessage = validateParams(params, payContext);
            if (!"success".equalsIgnoreCase(errorMessage)) {
                _log.warn(errorMessage);
                return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, errorMessage, null, null));
            }
            _log.debug("请求参数及签名校验通过");
            String mchId = params.getString("mchId");                // 商户ID
            String mchOrderNo = params.getString("mchOrderNo");    // 商户订单号
            String payOrderId = params.getString("payOrderId");    // 支付订单号
            String executeNotify = params.getString("executeNotify");   // 是否执行回调
            JSONObject payOrder = payOrderService.queryPayOrder(mchId, payOrderId, mchOrderNo, executeNotify);
            _log.info("{}查询支付订单,结果:{}", logPrefix, payOrder);
            if (payOrder == null) {
                return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付订单不存在", null, null));
            }
            Map<String, Object> map = XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
            map.put("result", payOrder);
            _log.info("###### 商户查询订单处理完成 ######");
            return XXPayUtil.makeRetData(map, payContext.getString("resKey"));
        } catch (Exception e) {
            _log.error(e, "");
            return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付中心系统异常", null, null));
        }
    }

    /**
     * 验证创建订单请求参数,参数通过返回JSONObject对象,否则返回错误文本信息
     *
     * @param params
     * @return
     */
    private String validateParams(JSONObject params, JSONObject payContext) {
        // 验证请求参数,参数有问题返回错误提示
        String errorMessage;
        // 支付参数
        String mchId = params.getString("mchId");                // 商户ID
        String mchOrderNo = params.getString("mchOrderNo");    // 商户订单号
        String payOrderId = params.getString("payOrderId");    // 支付订单号

        String sign = params.getString("sign");                // 签名

        // 验证请求参数有效性（必选项）
        if (StringUtils.isBlank(mchId)) {
            errorMessage = "request params[mchId] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(mchOrderNo) && StringUtils.isBlank(payOrderId)) {
            errorMessage = "request params[mchOrderNo or payOrderId] error.";
            return errorMessage;
        }

        // 签名信息
        if (StringUtils.isEmpty(sign)) {
            errorMessage = "request params[sign] error.";
            return errorMessage;
        }

        // 查询商户信息
        JSONObject mchInfo = mchInfoService.getByMchId(mchId);
        if (mchInfo == null) {
            errorMessage = "Can't found mchInfo[mchId=" + mchId + "] record in db.";
            return errorMessage;
        }
        if (mchInfo.getByte("state") != 1) {
            errorMessage = "mchInfo not available [mchId=" + mchId + "] record in db.";
            return errorMessage;
        }

        String reqKey = mchInfo.getString("reqKey");
        if (StringUtils.isBlank(reqKey)) {
            errorMessage = "reqKey is null[mchId=" + mchId + "] record in db.";
            return errorMessage;
        }
        payContext.put("resKey", mchInfo.getString("resKey"));

        // 验证签名数据
        boolean verifyFlag = XXPayUtil.verifyPaySign(params, reqKey);
        if (!verifyFlag) {
            errorMessage = "Verify XX pay sign failed.";
            return errorMessage;
        }

        return "success";
    }

}
