package io.zhudy.duic.config

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.zhudy.duic.Config
import io.zhudy.kitty.core.jackson.KittyModule
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Duration
import java.util.*

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServerProperties::class)
class BasicConfiguration {

    companion object {

        @Bean
        fun kotlinPropertyConfigurer() = PropertySourcesPlaceholderConfigurer().apply {
            setPlaceholderPrefix("%{")
            setTrimValues(true)
            setIgnoreUnresolvablePlaceholders(true)
        }
    }

    @Bean
    fun kittyModule() = KittyModule()

    @Bean(Config.BEAN_NAME)
    @ConfigurationProperties(prefix = "duic")
    fun config(serverProperties: ServerProperties): Config {
        return Config
    }

    @Bean
    fun jackson2ObjectMapperBuilderCustomizer() = Jackson2ObjectMapperBuilderCustomizer {
        it.timeZone(TimeZone.getDefault())
    }

    @Bean
    fun bCryptPasswordEncoder() = BCryptPasswordEncoder()

    @Bean
    @ConditionalOnExpression("\${duic.concurrent.request-limit-for-period:-1} >= 1")
    fun rateLimiter(config: Config): RateLimiter {
        val c = RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(100))
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(config.concurrent.requestLimitForPeriod)
                .build()
        return RateLimiter.of("web-rate-limiter", c)
    }
}