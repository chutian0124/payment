package com.sunreal.payment.api.service.impl;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import com.sunreal.payment.api.service.BaseService;
import com.sunreal.payment.api.service.IMchInfoService;
import com.sunreal.payment.common.domain.BaseParam;
import com.sunreal.payment.common.enumm.RetEnum;
import com.sunreal.payment.common.util.JsonUtil;
import com.sunreal.payment.common.util.MyLog;
import com.sunreal.payment.common.util.ObjectValidUtil;
import com.sunreal.payment.common.util.RpcUtil;
import com.sunreal.payment.dao.model.MchInfo;

/**
 * @author: dingzhiwei
 * @date: 17/9/8
 * @description:
 */
@Service
public class MchInfoServiceImpl extends BaseService implements IMchInfoService {

    private static final MyLog _log = MyLog.getLog(MchInfoServiceImpl.class);

    @Override
    public Map selectMchInfo(String jsonParam) {
        BaseParam baseParam = JsonUtil.getObjectFromJson(jsonParam, BaseParam.class);
        Map<String, Object> bizParamMap = baseParam.getBizParamMap();
        if (ObjectValidUtil.isInvalid(bizParamMap)) {
            _log.warn("查询商户信息失败, {}. jsonParam={}", RetEnum.RET_PARAM_NOT_FOUND.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_NOT_FOUND);
        }
        String mchId = baseParam.isNullValue("mchId") ? null : bizParamMap.get("mchId").toString();
        if (ObjectValidUtil.isInvalid(mchId)) {
            _log.warn("查询商户信息失败, {}. jsonParam={}", RetEnum.RET_PARAM_INVALID.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_INVALID);
        }
        MchInfo mchInfo = super.baseSelectMchInfo(mchId);
        if(mchInfo == null) return RpcUtil.createFailResult(baseParam, RetEnum.RET_BIZ_DATA_NOT_EXISTS);
        String jsonResult = JsonUtil.object2Json(mchInfo);
        return RpcUtil.createBizResult(baseParam, jsonResult);
    }

    public JSONObject getByMchId(String mchId) {
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("mchId", mchId);
        String jsonParam = RpcUtil.createBaseParam(paramMap);
        Map<String, Object> result = selectMchInfo(jsonParam);
        String s = RpcUtil.mkRet(result);
        if(s==null) return null;
        return JSONObject.parseObject(s);
    }
}
