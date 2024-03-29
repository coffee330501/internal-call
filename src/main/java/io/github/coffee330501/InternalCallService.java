package io.github.coffee330501;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import io.github.coffee330501.config.InternalCallConfig;
import io.github.coffee330501.exception.InternalCallException;
import io.github.coffee330501.service.AbstractInformationTransmitter;
import io.github.coffee330501.service.InternalCallLogHandler;
import io.github.coffee330501.utils.RSAUtils;
import io.github.coffee330501.utils.SpringContextUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class InternalCallService {
    @Resource
    InternalCallConfig internalCallConfig;
    AbstractInformationTransmitter informationTransmitter;
    InternalCallLogHandler internalCallLogHandler;

    @PostConstruct
    public void init() {
        internalCallLogHandler = SpringContextUtil.getBean(InternalCallLogHandler.class);
        informationTransmitter = SpringContextUtil.getBean(AbstractInformationTransmitter.class);
    }


    public Object post(String url, Class clazz) {
        return post(url, clazz, new Object());
    }

    public Object post(String url, Class clazz, Object params) throws InternalCallException {
        InternalCallLogHandler.LogBuilder logBuilder = InternalCallLogHandler.createLogBuilder();
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpEntityEnclosingRequestBase requestBase = new HttpPost(url);
            // 签名
            String requestId = this.sign(requestBase);
            // 记录日志
            log(logBuilder, url, params, requestId);
            // 参数
            String paramStr = JSONObject.toJSONString(params);
            StringEntity stringEntity = new StringEntity(paramStr, ContentType.APPLICATION_JSON);
            requestBase.setEntity(stringEntity);
            // 发起请求
            HttpResponse response = client.execute(requestBase);
            // 处理结果
            String resultStr = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new InternalCallException(400, "请求失败");
            }
            SignatureResult signatureResult = JSONObject.parseObject(resultStr, new TypeReference<SignatureResult>() {
            });

            // 处理异常
            logBuilder.add("code", signatureResult.getCode());
            logBuilder.add("msg", signatureResult.getMsg());
            handleInternalCallException(signatureResult);

            Object data = signatureResult.getData();
            logBuilder.add("data", data);
            if (data == null) return null;

            if (data instanceof JSONArray) {
                return JSONArray.parseArray(JSONObject.toJSONString(data), clazz);
            }
            return JSONObject.parseObject(JSONObject.toJSONString(data), clazz);
        } catch (IOException e) {
            throw new InternalCallException(501, e.getMessage());
        } finally {
            if (internalCallLogHandler != null) internalCallLogHandler.log(logBuilder);
        }
    }

    private void log(InternalCallLogHandler.LogBuilder logBuilder, String url, Object params, String requestId) {
        logBuilder.add("url", url).add("params", params).add("requestId", requestId).add("type", "send");
        if (informationTransmitter != null) {
            Map<String, String> information = informationTransmitter.getInformation();
            logBuilder.add("internalInfo", information);
        }
    }

    /**
     * 处理异常返回
     */
    private void handleInternalCallException(SignatureResult<?> signatureResult) throws InternalCallException {
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
            throw new RuntimeException(signatureResult.getMsg());
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
        if (informationTransmitter != null) {
            Map<String, String> information = informationTransmitter.createInformation();
            Set<String> keys = information.keySet();
            for (String key : keys) {
                requestBase.addHeader(key, information.get(key));
            }
        }

        requestBase.addHeader("timestamp", String.valueOf(timestamp));
        requestBase.addHeader("requestId", requestId);
        requestBase.addHeader("sign", sign);
        return requestId;
    }
}