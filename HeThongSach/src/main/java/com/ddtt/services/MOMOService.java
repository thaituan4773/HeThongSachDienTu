package com.ddtt.services;

import com.ddtt.utils.MOMOUtils;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import jakarta.inject.Singleton;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.reactivestreams.Publisher;

@Singleton
public class MOMOService {

    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String endpoint;
    private final String ipnUrl;
    private final String redirectUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public MOMOService(
            @Value("${app.momo.partnerCode}") String partnerCode,
            @Value("${app.momo.accessKey}") String accessKey,
            @Value("${app.momo.secretKey}") String secretKey,
            @Value("${app.momo.endpoint}") @Client String endpoint,
            @Value("${app.momo.ipnUrl}") String ipnUrl,
            @Value("${app.momo.redirectUrl}") String redirectUrl,
            HttpClient httpClient,
            ObjectMapper mapper
    ) {
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
        this.ipnUrl = ipnUrl;
        this.redirectUrl = redirectUrl;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    public Publisher<Map<String, Object>> createPaymentUrl(UUID transactionId, int accountId, int coinAmount, long coinPrice, String packName)
            throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        String requestType = "captureWallet";
        String extraData = "";
        String lang = "vi";
        String orderInfo = "Mua gói " + packName + " (" + coinAmount + ") trị giá " + coinPrice + " VND";

        String rawData = new StringBuilder()
                .append("accessKey=").append(accessKey).append("&")
                .append("amount=").append(coinPrice).append("&")
                .append("extraData=").append(extraData).append("&")
                .append("ipnUrl=").append(ipnUrl).append("&")
                .append("orderId=").append(transactionId).append("&")
                .append("orderInfo=").append(orderInfo).append("&")
                .append("partnerCode=").append(partnerCode).append("&")
                .append("redirectUrl=").append(redirectUrl).append("&")
                .append("requestId=").append(transactionId).append("&")
                .append("requestType=").append(requestType)
                .toString();

        String signature = MOMOUtils.sign(secretKey, rawData);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("accessKey", accessKey);
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("requestType", requestType);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("redirectUrl", redirectUrl);
        requestBody.put("orderId", transactionId);
        requestBody.put("amount", coinPrice);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("requestId", transactionId);
        requestBody.put("extraData", extraData);
        requestBody.put("lang", lang);
        requestBody.put("signature", signature);

        HttpRequest<Map<String, Object>> request = HttpRequest.POST(endpoint, requestBody)
                .contentType(MediaType.APPLICATION_JSON_TYPE);

        return Publishers.map(
                httpClient.retrieve(request, String.class),
                body -> {
                    try {
                        return mapper.readValue(body, Map.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Không thể parse MoMo response");
                    }
                }
        );
    }
    
    private String getString(Map<String, Object> payload, String key) {
    Object val = payload.get(key);
    return val == null ? "" : val.toString();
}

    public boolean verifyResponseSignature(Map<String, Object> payload) throws InvalidKeyException, NoSuchAlgorithmException {
        Function<String, String> v = key -> Objects.toString(payload.get(key), "");
        String rawData = new StringBuilder()
            .append("accessKey=").append(accessKey)
            .append("&amount=").append(v.apply("amount"))
            .append("&extraData=").append(v.apply("extraData"))
            .append("&message=").append(v.apply("message"))
            .append("&orderId=").append(v.apply("orderId"))
            .append("&orderInfo=").append(v.apply("orderInfo"))
            .append("&orderType=").append(v.apply("orderType"))
            .append("&partnerCode=").append(partnerCode)
            .append("&payType=").append(v.apply("payType"))
            .append("&requestId=").append(v.apply("requestId"))
            .append("&responseTime=").append(v.apply("responseTime"))
            .append("&resultCode=").append(v.apply("resultCode"))
            .append("&transId=").append(v.apply("transId"))
            .toString();
        String expectedSignature = MOMOUtils.sign(secretKey, rawData);
        String momoSignature = (String) payload.get("signature");;
        return expectedSignature.equals(momoSignature);
    }
}