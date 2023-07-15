package io.github.coffee330501;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
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
import java.util.HashMap;

public class InternalCallService {
    @Resource
    InternalCallConfig internalCallConfig;


    public <T> T post(String url, Class<T> clazz) throws InternalCallException {
        return post(url, clazz, new Object());
    }

    public <T> T post(String url, Class<T> clazz, Object params) throws InternalCallException {
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
                throw new InternalCallException("请求失败");
            }
            SignatureResult<T> signatureResult = JSONObject.parseObject(resultStr, new TypeReference<SignatureResult<T>>() {
            });
            if (signatureResult.getCode() == 400) {
                throw new InternalCallException(signatureResult.getMsg());
            }
            if (signatureResult.getCode() == 500) {
                LogUtils.error(url, params, requestId, signatureResult.getMsg());
                throw new InternalCallException("系统异常");
            }
            T data = signatureResult.getData();
            return JSONObject.parseObject(JSONObject.toJSONString(data), clazz);
        } catch (Exception e) {
            LogUtils.error(e, url, params);
            throw new InternalCallException("请求失败");
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

        requestBase.addHeader("timestamp", String.valueOf(timestamp));
        requestBase.addHeader("requestId", requestId);
        requestBase.addHeader("sign", sign);
        return requestId;
    }
}