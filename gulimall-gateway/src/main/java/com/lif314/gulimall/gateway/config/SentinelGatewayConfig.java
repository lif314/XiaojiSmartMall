package com.lif314.gulimall.gateway.config;


import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.lif314.common.exception.BizCodeEnum;
import com.lif314.common.utils.R;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class SentinelGatewayConfig {

    // TODO Web Flux 响应式编程
    public SentinelGatewayConfig(){
        GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
            // 网关限流规则
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange serverWebExchange, Throwable throwable) {
                R error =  R.error(BizCodeEnum.TOO_MANY_REQUESTS.getCode(), BizCodeEnum.TOO_MANY_REQUESTS.getMsg());
                String errJson = JSON.toJSONString(error);
                Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(errJson), String.class);
                return body;
            }
        });
    }

}
