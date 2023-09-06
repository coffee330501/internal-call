package io.github.coffee330501;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import io.github.coffee330501.service.SenderIdHandler;
import io.github.coffee330501.service.SenderIdSelector;
import io.github.coffee330501.config.InternalCallConfig;
import io.github.coffee330501.exception.InternalCallException;
import io.github.coffee330501.utils.LogUtils;
import io.github.coffee330501.utils.RSAUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Resource;
import java.util.Date;

public class InternalCallService {
    @Resource
    InternalCallConfig internalCallConfig;
    @Resource
    SenderIdSelector senderIdSelector;


    public Object post(String url, Class clazz) throws Exception {
        return post(url, clazz, new Object());
    }

    public Object post(String url, Class clazz, Object params) throws Exception {
        LogUtils.info(url, params);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEntityEnclosingRequestBase requestBase = new HttpPost(url);
            // 签名
            String requestId = this.sign(requestBase);
            // 参数
            String paramStr = JSONObject.toJSONString(params);
            StringEntity stringEntity = new StringEntity(paramStr, ContentType.APPLICATION_JSON);
            requestBase.setEntity(stringEntity);
            // 发起请求
            HttpResponse response = client.execute(requestBase);
            // 处理结果
            String resultStr = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                LogUtils.error(url, params, requestId, resultStr);
                throw new InternalCallException(400, "请求失败");
            }
            SignatureResult signatureResult = JSONObject.parseObject(resultStr, new TypeReference<SignatureResult>() {
            });

            // 处理异常
            handleInternalCallException(signatureResult);

            Object data = signatureResult.getData();
            if (data == null) return null;

            if (data instanceof JSONArray) {
                return JSONArray.parseArray(JSONObject.toJSONString(data), clazz);
            }
            return JSONObject.parseObject(JSONObject.toJSONString(data), clazz);
        }
    }

    /**
     * 处理异常返回
     *
     * @param signatureResult SignatureResult
     * @throws Exception e
     */
    private void handleInternalCallException(SignatureResult signatureResult) throws Exception {
        // 验签问题
        if (signatureResult.getCode() == 400) {
            throw new InternalCallException(400, signatureResult.getMsg());
        }
        // 业务异常
        if (signatureResult.getCode() == 500) {
            throw new InternalCallException(500, signatureResult.getMsg());
        }
        // 系统异常
        if (signatureResult.getCode() == 501) {
            LogUtils.error(signatureResult.getMsg());
            throw new Exception(signatureResult.getMsg());
        }
    }


    /**
     * 对请求签名
     *
     * @param requestBase HttpEntityEnclosingRequestBase
     */
    private String sign(HttpEntityEnclosingRequestBase requestBase) {
        // UUID
        String requestId = IdUtil.randomUUID();
        // 时间戳
        long timestamp = new Date().getTime();
        // 签名
        String content = "requestId=" + requestId + "&" + "timestamp=" + timestamp;
        String sign = RSAUtils.signByPrivateKey(content, internalCallConfig.getPrivateKey());
        // 当前用户信息
        String userId = senderIdSelector.getUserId();
        String userTableName = senderIdSelector.getUserTableName();

        requestBase.addHeader("timestamp", String.valueOf(timestamp));
        requestBase.addHeader("requestId", requestId);
        requestBase.addHeader("sign", sign);
        requestBase.addHeader("userId", userId);
        requestBase.addHeader("userTableName", userTableName);
        return requestId;
    }
}