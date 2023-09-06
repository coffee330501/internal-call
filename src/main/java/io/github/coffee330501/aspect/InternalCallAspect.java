package io.github.coffee330501.aspect;


import io.github.coffee330501.annotation.BusinessExceptionTag;
import io.github.coffee330501.annotation.Internal;
import io.github.coffee330501.config.InternalCallConfig;
import io.github.coffee330501.exception.InternalCallException;
import io.github.coffee330501.service.SenderIdHandler;
import io.github.coffee330501.utils.RSAUtils;
import io.github.coffee330501.utils.RedisUtil;
import io.github.coffee330501.utils.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Objects;

@Aspect
@Slf4j
public class InternalCallAspect {
    @Resource(name = "interCallRedisUtil")
    RedisUtil redisUtil;
    @Resource
    SenderIdHandler senderIdHandler;
    @Resource
    InternalCallConfig internalCallConfig;

    @Pointcut("@within(io.github.coffee330501.annotation.InternalController)")
    public void withinInternalController() {
    }

    @Pointcut("@annotation(io.github.coffee330501.annotation.Internal)")
    public void internalCallPointCut() {
    }

    @Around("withinInternalController()")
    public Object addInternalAnnotationToMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        if (!method.isAnnotationPresent(Internal.class)) {
            return this.internalCallAround(joinPoint);
        }
        return joinPoint.proceed(joinPoint.getArgs());
    }

    @Around("internalCallPointCut()")
    public Object internalCallAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 从header中获取签名内容
        HttpServletRequest request = ((ServletRequestAttributes) Objects
                .requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getRequest();
        String sign = request.getHeader("sign");
        String requestId = request.getHeader("requestId");
        String timestampStr = request.getHeader("timestamp");
        if (sign == null || requestId == null || timestampStr == null) {
            return SignatureUtil.errorByClient("Signature parameter is empty");
        }

        String userId = request.getHeader("userId");
        String userTableName = request.getHeader("userTableName");
        senderIdHandler.handle(userId, userTableName);

        try {
            // 验签
            String publicKey = internalCallConfig.getPublicKey();
            if (StringUtils.isEmpty(publicKey)) log.error("Internal call publicKey is empty!");

            long timestamp = Long.parseLong(timestampStr);
            if (!RSAUtils.verifySignByPublicKey(getSignContent(timestamp, requestId), sign, publicKey)) {
                throw new InternalCallException(400, "Internal call verification failed");
            }
            // 检查请求是否重复、过期
            requestValidate(timestamp, requestId);
            // 调用方法返回结果
            Object[] args = joinPoint.getArgs();
            Object result = joinPoint.proceed(args);
            return SignatureUtil.success(result);
        } catch (InternalCallException e) {
            if (e.getCode() == 400) return SignatureUtil.errorByClient(e.getMessage());
            return SignatureUtil.errorByBusiness(e.getMessage());
        } catch (Exception e) {
            BusinessExceptionTag annotation = e.getClass().getAnnotation(BusinessExceptionTag.class);
            if (annotation == null) return SignatureUtil.errorBySystem(e.getMessage());
            return SignatureUtil.errorByBusiness(e.getMessage());
        }
    }

    /**
     * 获取被签名的内容
     *
     * @return 被签名的内容
     */
    private String getSignContent(Long timestamp, String requestId) {
        return "requestId=" + requestId + "&" + "timestamp=" + timestamp;
    }

    /**
     * 验证请求有效性
     *
     * @param timestamp 请求发起时间戳
     * @param requestId 请求ID
     * @throws InternalCallException 内部调用异常
     */
    private void requestValidate(long timestamp, String requestId) throws InternalCallException {
        long time = new Date().getTime();
        long diff = time - timestamp;
        if (diff > 10 * 1000) {
            throw new InternalCallException(400, "Request Expiration");
        }
        boolean set = redisUtil.setNx(requestId, 10);
        if (!set) {
            throw new InternalCallException(400, "Duplicate Request");
        }
    }
}
