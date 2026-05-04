package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.config.AppConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpTonPayAdapterClient implements TonPayAdapterClient {

    private final RestTemplate restTemplate;
    private final AppConfig appConfig;

    public HttpTonPayAdapterClient(RestTemplate restTemplate, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
    }

    @Override
    public TonPayCreateTransferResponse createTransfer(TonPayCreateTransferRequest request) {
        String adapterUrl = appConfig.getTonpay().getAdapterUrl();
        if (adapterUrl == null || adapterUrl.isBlank()) {
            throw new IllegalStateException("TON Pay adapter URL не настроен (app.tonpay.adapter-url)");
        }

        String url = adapterUrl.replaceAll("/$", "") + "/api/tonpay/create-transfer";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = appConfig.getTonpay().getAdapterToken();
        if (token != null && !token.isBlank()) {
            headers.set("X-Service-Token", token);
        }
        headers.set("X-TonPay-Chain", appConfig.getTonpay().getChain());

        try {
            TonPayCreateTransferResponse response = restTemplate.postForObject(
                    url,
                    new HttpEntity<>(request, headers),
                    TonPayCreateTransferResponse.class
            );
            if (response == null || response.getMessage() == null || response.getReference() == null
                    || response.getReference().isBlank()) {
                throw new IllegalStateException("TON Pay adapter вернул неполный ответ");
            }
            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Ошибка вызова TON Pay adapter: " + e.getMessage(), e);
        }
    }
}
