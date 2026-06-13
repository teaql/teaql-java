package io.teaql.core.flux;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import io.teaql.core.RequestHolder;
import io.teaql.core.ResponseHolder;
import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.UserContextInitializer;
import reactor.core.publisher.Flux;

public class FluxInitializer implements UserContextInitializer, PriorityOrdered {
    @Override
    public boolean support(Object request) {
        return request instanceof ServerWebExchange;
    }

    @Override
    public void init(UserContext userContext, Object request) {
        if (request instanceof ServerWebExchange exchange) {
            ServerHttpRequest serverHttpRequest = exchange.getRequest();
            ServerHttpResponse serverHttpResponse = exchange.getResponse();
            userContext.put(
                    DefaultUserContext.REQUEST_HOLDER,
                    new RequestHolder() {

                        @Override
                        public String method() {
                            return serverHttpRequest.getMethod().name();
                        }

                        @Override
                        public String getHeader(String name) {
                            return serverHttpRequest.getHeaders().getFirst(name);
                        }

                        @Override
                        public List<String> getHeaderNames() {
                            return new ArrayList<>(serverHttpRequest.getHeaders().keySet());
                        }

                        @Override
                        public byte[] getPart(String name) {
                            Flux<DataBuffer> data =
                                    exchange.getMultipartData().map(i -> i.getFirst(name).content()).block();
                            return DataBufferUtils.join(data)
                                    .map(
                                            dataBuffer -> {
                                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                                dataBuffer.read(bytes);
                                                DataBufferUtils.release(dataBuffer);
                                                return bytes;
                                            })
                                    .block();
                        }

                        @Override
                        public List<String> getParameterNames() {
                            return new ArrayList<>(serverHttpRequest.getQueryParams().keySet());
                        }

                        @Override
                        public String getParameter(String name) {
                            String queryParam = serverHttpRequest.getQueryParams().getFirst(name);
                            if (queryParam != null) {
                                return queryParam;
                            }
                            return exchange.getFormData().map(i -> i.getFirst(name)).block();
                        }

                        @Override
                        public byte[] getBodyBytes() {
                            Flux<DataBuffer> body = serverHttpRequest.getBody();
                            return DataBufferUtils.join(body)
                                    .map(
                                            dataBuffer -> {
                                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                                dataBuffer.read(bytes);
                                                DataBufferUtils.release(dataBuffer);
                                                return bytes;
                                            })
                                    .block();
                        }

                        @Override
                        public String requestUri() {
                            return serverHttpRequest.getPath().pathWithinApplication().value();
                        }

                        @Override
                        public String getRemoteAddress() {
                            return serverHttpRequest.getRemoteAddress().getHostString();
                        }
                    });

            userContext.put(
                    DefaultUserContext.RESPONSE_HOLDER,
                    new ResponseHolder() {
                        @Override
                        public void setHeader(String name, String value) {
                            serverHttpResponse.getHeaders().add(name, value);
                        }

                        @Override
                        public String getHeader(String name) {
                            return serverHttpResponse.getHeaders().getFirst(name);
                        }
                    });
        }
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
