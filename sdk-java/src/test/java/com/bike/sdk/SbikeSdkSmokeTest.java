package com.bike.sdk;

import com.bike.sdk.api.FenceApi;
import com.bike.sdk.api.OrderApi;
import com.bike.sdk.api.PositionApi;
import com.bike.sdk.invoker.ApiClient;
import com.bike.sdk.invoker.ApiException;
import com.bike.sdk.model.ApiResultOrderVo;
import com.bike.sdk.model.ApiResultPageResultFenceVo;
import com.bike.sdk.model.OrderCreateRequest;
import com.bike.sdk.model.PositionCreateRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SDK 冒烟测试：需要服务端已启动，运行参数 -Dsbike.smoke=true -Dsbike.baseUrl=http://localhost:8080。
 */
class SbikeSdkSmokeTest {

    private static final String APP_KEY = "sbike_demo";
    private static final String APP_SECRET = "SbikeDemoSecret2026x9f8e7d6c5b4a3210";
    private static final String CODE_OK = "0";
    private static final String CODE_ORDER_EXISTS = "BIZ-40901";

    private static ApiClient apiClient;

    @BeforeAll
    static void setUp() {
        Assumptions.assumeTrue(Boolean.getBoolean("sbike.smoke"), "未开启冒烟测试");
        String baseUrl = System.getProperty("sbike.baseUrl", "http://localhost:8080");
        apiClient = SbikeSdk.create(APP_KEY, APP_SECRET, baseUrl);
    }

    @Test
    void listFencesShouldReturnSeededData() throws ApiException {
        ApiResultPageResultFenceVo result = new FenceApi(apiClient).listFences(1L, 10L, null);

        assertEquals(CODE_OK, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().getTotal() > 0);
    }

    @Test
    void createOrderTwiceWithSameIdempotencyKeyShouldReturnOrderExists() throws ApiException {
        OrderCreateRequest request = new OrderCreateRequest()
                .areaCode("110101")
                .sn("BK0001")
                .orderId("SDK" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .startTime(LocalDateTime.now().withNano(0))
                .endTime(LocalDateTime.now().withNano(0).plusMinutes(30))
                .amount(new BigDecimal("1.50"));
        String idempotencyKey = UUID.randomUUID().toString();
        OrderApi orderApi = new OrderApi(apiClient);

        ApiResultOrderVo first = orderApi.createOrder(idempotencyKey, request);
        ApiResultOrderVo second = orderApi.createOrder(idempotencyKey, request);

        assertEquals(CODE_OK, first.getCode());
        assertEquals(CODE_ORDER_EXISTS, second.getCode());
    }

    @Test
    void uploadPositionShouldSucceed() throws ApiException {
        PositionCreateRequest request = new PositionCreateRequest()
                .areaCode("110101")
                .sn("SDK" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .lng(new BigDecimal("116.418"))
                .lat(new BigDecimal("39.908"))
                .positionTime(LocalDateTime.now().withNano(0))
                .status(1)
                .busiStatus(0);

        assertEquals(CODE_OK, new PositionApi(apiClient).uploadPosition(request).getCode());
    }
}
