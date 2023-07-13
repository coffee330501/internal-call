package io.github.coffee330501.autoconfigure;

import io.github.coffee330501.InternalCallService;
import io.github.coffee330501.aspect.InternalCallAspect;
import io.github.coffee330501.config.InternalCallConfig;
import io.github.coffee330501.utils.RedisUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(InternalCallConfig.class)
@EnableConfigurationProperties({InternalCallConfig.class})
@Import({InternalCallService.class, InternalCallAspect.class, RedisUtil.class})
public class InternalCallAutoConfigure {
}
