package com.sunreal.payment.api.service.impl;

import java.util.Date;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.sunreal.payment.api.service.BaseService;
import com.sunreal.payment.api.service.RefundOrderApiService;
import com.sunreal.payment.api.service.channel.alipay.AlipayConfig;
import com.sunreal.payment.common.constant.PayConstant;
import com.sunreal.payment.common.util.MyLog;
import com.sunreal.payment.common.util.MySeq;
import com.sunreal.payment.dao.mapper.RefundOrderMapper;
import com.sunreal.payment.dao.model.AlipayTradeRefundModel;
import com.sunreal.payment.dao.model.PayChannel;
import com.sunreal.payment.dao.model.PayOrder;
import com.sunreal.payment.dao.model.RefundOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 功能描述：//TODO
 *
 * @Author liy
 * @Date 2019/1/8
 */
@Service
public class RefundOrderApiServiceImpl extends BaseService implements RefundOrderApiService {
    private static final MyLog _log = MyLog.getLog(RefundOrderApiServiceImpl.class);

    @Autowired
    private AlipayConfig alipayConfig;
    @Autowired
    private RefundOrderMapper refundOrderMapper;


    /**
     * 支付宝执行退款操作
     * @param payOrderId
     * @param refundReason
     * @param terminalId
     * @return
     */
    @Override
    public String doAliRefundOrder(String payOrderId, String refundReason, String terminalId) {
        String result= PayConstant.RETURN_ALIPAY_VALUE_FAIL;
        try{
            PayOrder order= super.baseSelectPayOrder(payOrderId);
            String mchId = order.getMchId();
            String channelId = order.getChannelId();
            PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
            //新增退款订单信息
            RefundOrder refundOrder=new RefundOrder();
            refundOrder.setRefundOrderId(MySeq.getRefund());
            refundOrder.setPayOrderId(payOrderId);
            refundOrder.setChannelPayOrderNo(order.getChannelOrderNo());
            refundOrder.setMchId(order.getMchId());
            refundOrder.setMchRefundNo("");
            refundOrder.setChannelId(order.getChannelId());
            refundOrder.setPayAmount(order.getAmount());
            refundOrder.setCurrency(order.getCurrency());
            refundOrder.setStatus(PayConstant.REFUND_STATUS_INIT);
            refundOrder.setResult(PayConstant.REFUND_RESULT_INIT);
            refundOrder.setChannelMchId(order.getChannelMchId());
            refundOrder.setChannelOrderNo(order.getChannelOrderNo());
            refundOrder.setCreateTime(new Date());
            refundOrderMapper.insertSelective(refundOrder);
            //初始化阿里参数，请求退款接口
            alipayConfig.init(payChannel.getParam());
            AlipayClient client = new DefaultAlipayClient(alipayConfig.getUrl(), alipayConfig.getApp_id(), alipayConfig.getRsa_private_key(), AlipayConfig.FORMAT, AlipayConfig.CHARSET, alipayConfig.getAlipay_public_key(), AlipayConfig.SIGNTYPE);
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel alipayTradeRefundModel=new AlipayTradeRefundModel();
            alipayTradeRefundModel.setTrade_no(order.getChannelOrderNo());
            alipayTradeRefundModel.setOut_trade_no(order.getPayOrderId());
            alipayTradeRefundModel.setRefund_amount(Double.valueOf(order.getAmount())/100);
            alipayTradeRefundModel.setRefund_currency(PayConstant.CURRENCY);
            alipayTradeRefundModel.setRefund_reason(refundReason);
            alipayTradeRefundModel.setTerminal_id(terminalId);
            refundOrder.setRefundAmount(order.getAmount());
            request.setBizContent(JSON.toJSONString(alipayTradeRefundModel));
            AlipayTradeRefundResponse response = client.execute(request);
            if(response.isSuccess()){
                result=PayConstant.RETURN_ALIPAY_VALUE_SUCCESS;
            } else {
                result=PayConstant.RETURN_ALIPAY_VALUE_FAIL;
            }
            //更新本地订单状态
            order.setUpdateTime(response.getGmtRefundPay());
            order.setStatus(PayConstant.PAY_STATUS_REFUND);
            super.baseUpdateNotify(order);
            //refundOrder.setChannelUser(response.getBuyerLogonId());
            //refundOrder.setRefundSuccTime(response.getGmtRefundPay());
        }catch (AlipayApiException e){
            _log.error(e,"退款失败");
            result=PayConstant.RETURN_ALIPAY_VALUE_FAIL;
        }catch (Exception e){
            _log.error(e,"退款失败");
            result=PayConstant.RETURN_ALIPAY_VALUE_FAIL;
        }
        return result;
    }
}
