package com.sunreal.payment.api.service;

/**
 * 功能描述：//TODO
 *
 * @Author liy
 * @Date 2019/1/8
 */
public interface RefundOrderApiService {
    String doAliRefundOrder(String payOrderId, String refundReason, String terminalId);
}
