package com.example.todoapp.api;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HolidayClient {
    private final RestClient restClient;

    public HolidayClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public record HolidayResult(Map<String, String> holidays, boolean unavailable) {}

    public HolidayResult fetchHolidays() {
        try {
            Map<String, String> holidays = restClient.get()
                .uri("https://holidays-jp.github.io/api/v1/date.json")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, String>>() {});
            return holidays == null
                    ? new HolidayResult(Map.of(), true)
                    : new HolidayResult(holidays, false);
        } catch (RestClientException exception) {
            return new HolidayResult(Map.of(), true);
        }
    }
}
