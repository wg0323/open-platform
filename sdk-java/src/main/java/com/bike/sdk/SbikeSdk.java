package com.bike.sdk;

import com.google.gson.Gson;
import com.bike.sdk.invoker.ApiClient;
import com.bike.sdk.invoker.JSON;

import java.time.LocalDateTime;

/**
 * SDK 入口：传入 appKey、appSecret 与服务地址即可，签名由拦截器自动完成。
 */
public final class SbikeSdk {

    private SbikeSdk() {
    }

    /**
     * 创建已装配签名的 API 客户端。
     *
     * @param appKey    平台分配的接入方标识
     * @param appSecret 平台分配的密钥
     * @param baseUrl   服务地址，如 http://localhost:8080
     * @return API 客户端
     */
    public static ApiClient create(String appKey, String appSecret, String baseUrl) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        apiClient.setHttpClient(apiClient.getHttpClient().newBuilder()
                .addInterceptor(new HmacSigningInterceptor(appKey, appSecret))
                .build());
        JSON json = apiClient.getJSON();
        Gson gson = json.getGson().newBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        json.setGson(gson);
        return apiClient;
    }
}
