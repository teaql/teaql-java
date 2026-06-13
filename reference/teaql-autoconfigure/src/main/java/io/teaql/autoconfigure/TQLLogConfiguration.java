package io.teaql.autoconfigure;

import io.teaql.core.UserContext;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import io.teaql.core.utils.MapUtil;
import io.teaql.core.utils.URLDecoder;

import io.teaql.autoconfigure.log.LogConfiguration;
import io.teaql.autoconfigure.log.RequestLogger;
import io.teaql.autoconfigure.log.UserTraceIdInitializer;

@Configuration
public class TQLLogConfiguration {
    @Bean
    public LogConfiguration logConfig() {
        return new LogConfiguration();
    }

    @Bean
    public RequestLogger requestLogger() {
        return new RequestLogger();
    }

    @Bean
    public UserTraceIdInitializer userTraceIdInitializer() {
        return new UserTraceIdInitializer();
    }

    @Controller
    public static class LogController {

        @GetMapping("/logConfig/enableGlobalMarker/{markerName}/")
        @ResponseBody
        public Object enableGlobalMarker(
                @TQLContext UserContext ctx, @PathVariable("markerName") String markerName) {
            ctx.getBean(LogConfiguration.class).enableGlobalMarker(markerName);
            return MapUtil.of("success", true);
        }

        @GetMapping("/logConfig/disableGlobalMarker/{markerName}/")
        @ResponseBody
        public Object disableGlobalMarker(
                @TQLContext UserContext ctx, @PathVariable("markerName") String markerName) {
            ctx.getBean(LogConfiguration.class).disableGlobalMarker(markerName);
            return MapUtil.of("success", true);
        }

        @GetMapping("/logConfig/addDeniedUrl/{url}/")
        @ResponseBody
        public Object addDeniedUrl(@TQLContext UserContext ctx, @PathVariable("url") String url) {
            ctx.getBean(LogConfiguration.class)
                    .addDeniedUrl(URLDecoder.decode(url, StandardCharsets.UTF_8));
            return MapUtil.of("success", true);
        }

        @GetMapping("/logConfig/removeDeniedUrl/{url}/")
        @ResponseBody
        public Object removeDeniedUrl(@TQLContext UserContext ctx, @PathVariable("url") String url) {
            ctx.getBean(LogConfiguration.class)
                    .addDeniedUrl(URLDecoder.decode(url, StandardCharsets.UTF_8));
            return MapUtil.of("success", true);
        }

        @GetMapping("/logConfig/enableUserMarker/{markerNames}/")
        @ResponseBody
        public Object enableUserMarker(
                @TQLContext UserContext ctx, @PathVariable("markerNames") String markerNames) {
            ctx.getBean(LogConfiguration.class).enableUserMarker(ctx, markerNames);
            return MapUtil.of("success", true);
        }

        @GetMapping("/logConfig/disableUserMarker/{markerNames}/")
        @ResponseBody
        public Object disableUserMarker(
                @TQLContext UserContext ctx, @PathVariable("markerNames") String markerNames) {
            ctx.getBean(LogConfiguration.class).disableUserMarker(ctx, markerNames);
            return MapUtil.of("success", true);
        }

        @GetMapping("/logConfig/enableAll/")
        @ResponseBody
        public Object enableAll(@TQLContext UserContext ctx) {
            ctx.getBean(LogConfiguration.class).enableAll(ctx);
            return MapUtil.of("success", true);
        }

        @GetMapping("/logConfig/reset/")
        @ResponseBody
        public Object reset(@TQLContext UserContext ctx) {
            ctx.getBean(LogConfiguration.class).enableAll(ctx);
            return MapUtil.of("success", true);
        }
    }
}
