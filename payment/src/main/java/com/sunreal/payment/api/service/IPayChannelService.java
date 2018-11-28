package com.sunreal.payment.api.service;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;

/**
 * @author: dingzhiwei
 * @date: 17/9/8
 * @description:
 */
public interface IPayChannelService {

    Map selectPayChannel(String jsonParam);

    JSONObject getByMchIdAndChannelId(String mchId, String channelId);
}
