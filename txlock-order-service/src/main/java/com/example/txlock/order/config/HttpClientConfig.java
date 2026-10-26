package com.example.txlock.order.config;

import com.example.txlock.order.client.RemoteOrderApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient remoteRestClient(DemoProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.remote().connectTimeoutMs());
        requestFactory.setReadTimeout(properties.remote().readTimeoutMs());
        return RestClient.builder()
                .baseUrl(properties.remote().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public RemoteOrderApi remoteOrderApi(RestClient remoteRestClient) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(remoteRestClient))
                .build();
        return factory.createClient(RemoteOrderApi.class);
    }
}
