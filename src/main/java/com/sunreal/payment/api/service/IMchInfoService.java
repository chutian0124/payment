package com.sunreal.payment.api.service;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

/**
 * @author: dingzhiwei
 * @date: 17/9/8
 * @description:
 */
public interface IMchInfoService {

    Map selectMchInfo(String jsonParam);

    JSONObject getByMchId(String mchId);

}
