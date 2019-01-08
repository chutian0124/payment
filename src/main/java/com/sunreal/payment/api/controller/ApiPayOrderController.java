package com.sunreal.payment.api.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.utils.qrcode.QrcodeUtils;
import com.sunreal.payment.api.service.IMchInfoService;
import com.sunreal.payment.api.service.IPayChannelService;
import com.sunreal.payment.api.service.IPayOrderService;
import com.sunreal.payment.api.service.channel.wechat.WxPayProperties;
import com.sunreal.payment.common.constant.PayConstant;
import com.sunreal.payment.common.util.AmountUtil;
import com.sunreal.payment.common.util.JsonUtil;
import com.sunreal.payment.common.util.MyLog;
import com.sunreal.payment.common.util.MySeq;
import com.sunreal.payment.common.util.RpcUtil;
import com.sunreal.payment.common.util.XXPayUtil;

/**
 * @author dingzhiwei jmdhappy@126.com
 * @version V1.0
 * @Description: 支付订单, 包括:统一下单,订单查询,补单等接口
 * @date 2017-07-05
 * @Copyright: www.xxpay.org
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class ApiPayOrderController {

    private final MyLog _log = MyLog.getLog(ApiPayOrderController.class);

    @Autowired
    private IPayOrderService payOrderService;

    @Autowired
    private IPayChannelService payChannelService;

    @Autowired
    private IMchInfoService mchInfoService;

    @Resource
    private WxPayProperties wxPayProperties;

    @RequestMapping(value = "/api/pay/create_order", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String craetePayOrder(@RequestBody JSONObject params) {
        _log.info("###### 开始接收商户统一下单请求 ######");
        String logPrefix = "【商户统一下单】";
        try {
            JSONObject payContext = new JSONObject();
            JSONObject payOrder = null;
            // 验证参数有效性,如果通过了返回是jsonobject，没通过返回的是String，错误信息
            Object object = validateParams(params, payContext);
            if (object instanceof String) {
                _log.info("{}参数校验不通过:{}", logPrefix, object);
                return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, object.toString(), null, null));
            }
            if (object instanceof JSONObject) payOrder = (JSONObject) object;
            if (payOrder == null)
                return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付中心下单失败", null, null));
            Map result = payOrderService.createPayOrder(payOrder);
            _log.info("{}创建支付订单,结果:{}", logPrefix, result);
            String count = RpcUtil.mkRet(result);
            if (count == null || Integer.valueOf(count) != 1) {
                String rpcRetMsg = result.get("rpcRetMsg") == null ? "支付中心下单失败！" : result.get("rpcRetMsg").toString();
                String resCode = result.get("rpcRetCode") == null ? "" : result.get("rpcRetCode").toString();
                return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, rpcRetMsg, resCode, null));
            } else {
                String jsonParam = RpcUtil.createBaseParam(JsonUtil.getObjectFromJson(payOrder.toJSONString(), Map.class));
                Map createdOrder = payOrderService.selectPayOrderByMchIdAndMchOrderNo(jsonParam);
                Map<String, Object> retMap = XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
                Map bizResult = JsonUtil.getObjectFromJson(createdOrder.get("bizResult").toString(), Map.class);
                retMap.put("result", bizResult.get("payOrderId"));
                return XXPayUtil.makeRetData(retMap, payContext.getString("resKey"));
            }
        } catch (Exception e) {
            _log.error(e, "");
            return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付中心系统异常", null, null));
        }
    }

    @RequestMapping(value = "/api/pay/{id}")
    public void payOrder(@PathVariable(value = "id") String orderid, HttpServletResponse httpResponse) throws IOException {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("payOrderId", orderid);
        String jsonParam = RpcUtil.createBaseParam(p);
        Map order = JsonUtil.getObjectFromJson(payOrderService.selectPayOrder(jsonParam).get("bizResult").toString(), Map.class);
        String channelId = order.get("channelId").toString();
        String mchId = order.get("mchId").toString();
        JSONObject mchInfo = mchInfoService.getByMchId(mchId);
        JSONObject payOrder = JsonUtil.getJSONObjectFromObj(order);
        String resKey = (String) mchInfo.get("resKey");
        String result = "";
        switch (channelId) {
            case PayConstant.PAY_CHANNEL_WX_APP:
                result = payOrderService.doWxPayReq(PayConstant.WxConstant.TRADE_TYPE_APP, payOrder, resKey);
                break;
            case PayConstant.PAY_CHANNEL_WX_JSAPI:
                result = payOrderService.doWxPayReq(PayConstant.WxConstant.TRADE_TYPE_JSPAI, payOrder, resKey);
                break;
            case PayConstant.PAY_CHANNEL_WX_NATIVE:
                result = payOrderService.doWxPayReq(PayConstant.WxConstant.TRADE_TYPE_NATIVE, payOrder, resKey);
                break;
            case PayConstant.PAY_CHANNEL_WX_MWEB:
                result = payOrderService.doWxPayReq(PayConstant.WxConstant.TRADE_TYPE_MWEB, payOrder, resKey);
                break;
            case PayConstant.PAY_CHANNEL_ALIPAY_MOBILE:
                result = payOrderService.doAliPayReq(channelId, payOrder, resKey);
                break;
            case PayConstant.PAY_CHANNEL_ALIPAY_PC:
                result = payOrderService.doAliPayReq(channelId, payOrder, resKey);
                break;
            case PayConstant.PAY_CHANNEL_ALIPAY_WAP:
                result = payOrderService.doAliPayReq(channelId, payOrder, resKey);
                break;
            case PayConstant.PAY_CHANNEL_ALIPAY_QR:
                result = payOrderService.doAliPayReq(channelId, payOrder, resKey);
                break;
            default:
                result = XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "不支持的支付渠道类型[channelId=" + channelId + "]", null, null));
        }
        Map resultmap = JsonUtil.getObjectFromJson(result, Map.class);
        if (resultmap.get("payUrl") != null) {
            httpResponse.setContentType("text/html;charset=UTF-8");
            httpResponse.getWriter().write(resultmap.get("payUrl").toString());//直接将完整的表单html输出到页面、
            httpResponse.getWriter().flush();
            httpResponse.getWriter().close();
        } else if (resultmap.get("codeUrl") != null)  {
            Object subject = order.get("subject");
            Object body = order.get("body");
            Object returnUrl = order.get("returnUrl");
            String amount = AmountUtil.convertCent2Dollar(order.get("amount").toString());
            System.out.println(amount);
            String codeUrl = resultmap.get("codeUrl").toString();
            String prepayId = resultmap.get("prepayId").toString();
            System.out.println("微信返回的prepayId："+prepayId);
            httpResponse.setContentType("text/html;charset=UTF-8");
            StringBuffer url = new StringBuffer("<!DOCTYPE html>");
            url.append("<html>\n");
            url.append("<head>\n");
            url.append("    <meta charset=\"utf-8\">\n");
            url.append("    <meta value=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0\" />\n");
            url.append("    <title>").append(subject).append("</title>\n");
            url.append("    <link rel=\"stylesheet\" href=\"/css/wxpay.css\">\n");
            url.append("</head>\n");
            url.append("<body>\n");
            url.append("    <div class=\"main\">\n");
            url.append("        <div class=\"header\">\n");
            url.append("            <div class=\"logo wd\">\n");
            url.append("                <img src=\"/images/logo.png\" alt=\"\">\n");
            url.append("            </div>\n");
            url.append("            <h3 class=\" title\">").append(subject).append("</h3>\n");
            url.append("        </div>\n");
            url.append("        <div class=\"content wd\">\n");
            url.append("            <div class=\"code-box\">\n");
            url.append("                <div class=\"code-title\">\n");
            url.append("                    <h4>订单总计：<span>¥<b>").append(amount).append("元</b></span></h4>\n");
            url.append("                    <p>").append(body).append("</p>\n");
            url.append("                </div>\n");
            url.append("                <div class=\"code-img\">\n");
            url.append("                    <img src=\"/api/createQrcode?qrvalue=").append(codeUrl).append("\" alt=\"\">\n");
            url.append("                </div>\n");
            url.append("                <div class='code-foot'>\n");
            url.append("                    <img src=\"/images/wei.png\" alt=\"\">\n");
            url.append("                    <span>请扫码完成微信支付</span>\n");
            url.append("                </div>\n");
            url.append("            </div>\n");
            url.append("        </div>\n");
            url.append("    </div>\n");
            url.append("</body>\n");
            url.append("<script src=\"/js/jquery-1.12.4.min.js\"></script>\n");
            url.append("<script>\n");
            url.append("    var orderid ='").append(orderid).append("';\n");
            url.append("    var mchId ='").append(mchId).append("';\n");
            url.append("    var returnUrl='").append(returnUrl).append("';\n");
            url.append("    var id = 0;\n");
            url.append("    var url = '/api/pay/isOrderFinish';\n");
            url.append("    $(function(){\n");
            url.append("        id= setInterval(load,1000);\n");
            url.append("    });\n");
            url.append("    function load() {\n");
            url.append("       $.ajax({\n");
            url.append("           url: url,\n");
            url.append("           type: 'post',\n");
            url.append("           contentType:'application/json',\n");
            url.append("           data: JSON.stringify({\n");
            url.append("                 \"payOrderId\":\"").append(orderid).append("\"").append(",\n");
            url.append("                 \"mchId\":\"").append(mchId).append("\"\n");
            url.append("           }),\n");
            url.append("           success: function (data) {\n");
            url.append("                 if(data == 'Y'){\n");
            url.append("                        clearInterval(id);\n");
            url.append("                        window.location = \"").append(returnUrl).append("\";\n");
            url.append("                 }\n");
            url.append("           },\n");
            url.append("           error: function (err) {\n");
            url.append("                 console.log(err);\n");
            url.append("           }\n");
            url.append("     });\n");
            url.append("  }\n");
            url.append("</script>\n");
            url.append("</html>\n");
            httpResponse.getWriter().write(url.toString());//直接将完整的表单html输出到页面、
            httpResponse.getWriter().flush();
            httpResponse.getWriter().close();
        }
    }

    @RequestMapping(value = "/api/createQrcode")
    public void createQrcode(String qrvalue, HttpServletResponse httpResponse) throws IOException {
        File logofile = new File(wxPayProperties.getLogoFilePath());
        byte[] buf = QrcodeUtils.createQrcode(qrvalue, logofile);
        ServletOutputStream out = httpResponse.getOutputStream();
        out.write(buf);
        out.flush();
        out.close();
    }

    /**
     * 统一下单接口:
     * 1)先验证接口参数以及签名信息
     * 2)验证通过创建支付订单
     * 3)根据商户选择渠道,调用支付服务进行下单
     * 4)返回下单数据
     *
     * @param params
     * @return
     */
    @RequestMapping(value = "/api/pay/create_order")
    public String payOrder(@RequestParam String params) {
        JSONObject po = JSONObject.parseObject(params);
        return craetePayOrder(po);
    }

    /**
     * 验证创建订单请求参数,参数通过返回JSONObject对象,否则返回错误文本信息
     *
     * @param params
     * @return
     */
    private Object validateParams(JSONObject params, JSONObject payContext) {
        // 验证请求参数,参数有问题返回错误提示
        String errorMessage;
        // 支付参数
        String mchId = params.getString("mchId");                // 商户ID
        String mchOrderNo = params.getString("mchOrderNo");    // 商户订单号
        String channelId = params.getString("channelId");        // 渠道ID
        String amount = params.getString("amount");            // 支付金额（单位分）
        String currency = params.getString("currency");         // 币种
        String clientIp = params.getString("clientIp");            // 客户端IP
        String device = params.getString("device");            // 设备
        String extra = params.getString("extra");                // 特定渠道发起时额外参数
        String param1 = params.getString("param1");            // 扩展参数1
        String param2 = params.getString("param2");            // 扩展参数2
        String notifyUrl = params.getString("notifyUrl");        // 支付结果回调URL
        String sign = params.getString("sign");                // 签名
        String subject = params.getString("subject");            // 商品主题
        String body = params.getString("body");                    // 商品描述信息
        String returnUrl = params.getString("returnUrl");        // 支付结果同步回调地址
        String originalOrderId = params.getString("originalOrderId");        // 原始订单id
        // 验证请求参数有效性（必选项）
        if (StringUtils.isBlank(mchId)) {
            errorMessage = "request params[mchId] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(mchOrderNo)) {
            errorMessage = "request params[mchOrderNo] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(channelId)) {
            errorMessage = "request params[channelId] error.";
            return errorMessage;
        }
        if (!NumberUtils.isNumber(amount)) {
            errorMessage = "request params[amount] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(currency)) {
            errorMessage = "request params[currency] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(notifyUrl)) {
            errorMessage = "request params[notifyUrl] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(subject)) {
            errorMessage = "request params[subject] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(body)) {
            errorMessage = "request params[body] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(returnUrl)) {
            errorMessage = "request params[returnUrl] error.";
            return errorMessage;
        }
        if (StringUtils.isBlank(originalOrderId)) {
            errorMessage = "request params[originalOrderId] error.";
            return errorMessage;
        }
        // 根据不同渠道,判断extra参数
        if (PayConstant.PAY_CHANNEL_WX_JSAPI.equalsIgnoreCase(channelId)) {
            if (StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            JSONObject extraObject = JSON.parseObject(extra);
            String openId = extraObject.getString("openId");
            if (StringUtils.isBlank(openId)) {
                errorMessage = "request params[extra.openId] error.";
                return errorMessage;
            }
        } else if (PayConstant.PAY_CHANNEL_WX_NATIVE.equalsIgnoreCase(channelId)) {
            if (StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            JSONObject extraObject = JSON.parseObject(extra);
            String productId = extraObject.getString("productId");
            if (StringUtils.isBlank(productId)) {
                errorMessage = "request params[extra.productId] error.";
                return errorMessage;
            }
        } else if (PayConstant.PAY_CHANNEL_WX_MWEB.equalsIgnoreCase(channelId)) {
            if (StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            JSONObject extraObject = JSON.parseObject(extra);
            String productId = extraObject.getString("sceneInfo");
            if (StringUtils.isBlank(productId)) {
                errorMessage = "request params[extra.sceneInfo] error.";
                return errorMessage;
            }
            if (StringUtils.isBlank(clientIp)) {
                errorMessage = "request params[clientIp] error.";
                return errorMessage;
            }
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

        // 查询商户对应的支付渠道
        JSONObject payChannel = payChannelService.getByMchIdAndChannelId(mchId, channelId);
        if (payChannel == null) {
            errorMessage = "Can't found payChannel[channelId=" + channelId + ",mchId=" + mchId + "] record in db.";
            return errorMessage;
        }
        if (payChannel.getByte("state") != 1) {
            errorMessage = "channel not available [channelId=" + channelId + ",mchId=" + mchId + "]";
            return errorMessage;
        }

        // 验证签名数据
        boolean verifyFlag = XXPayUtil.verifyPaySign(params, reqKey);
        if (!verifyFlag) {
            errorMessage = "Verify XX pay sign failed.";
            return errorMessage;
        }
        // 验证参数通过,返回JSONObject对象
        JSONObject payOrder = new JSONObject();
        payOrder.put("payOrderId", MySeq.getPay());
        payOrder.put("mchId", mchId);
        payOrder.put("mchOrderNo", mchOrderNo);
        payOrder.put("channelId", channelId);
        payOrder.put("amount", Long.parseLong(amount));
        payOrder.put("currency", currency);
        payOrder.put("clientIp", clientIp);
        payOrder.put("device", device);
        payOrder.put("subject", subject);
        payOrder.put("body", body);
        payOrder.put("extra", extra);
        payOrder.put("channelMchId", payChannel.getString("channelMchId"));
        payOrder.put("param1", param1);
        payOrder.put("param2", param2);
        payOrder.put("notifyUrl", notifyUrl);
        payOrder.put("returnUrl", returnUrl);
        payOrder.put("originalOrderId", originalOrderId);
        return payOrder;
    }

}
