package com.example.todoapp.api;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HolidayApiTests {
    @Test
    void connectionFailureReturnsEmpty200WithUnavailableHeader() {
        assertUnavailable(new ResourceAccessException("Connection failed", new ConnectException()));
    }

    @Test
    void timeoutReturnsEmpty200WithUnavailableHeader() {
        assertUnavailable(new ResourceAccessException("Read timed out", new SocketTimeoutException()));
    }

    private void assertUnavailable(ResourceAccessException failure) {
        HolidayClient client = new HolidayClient();
        RestClient restClient = mock(RestClient.class);
        ReflectionTestUtils.setField(client, "restClient", restClient);
        when(restClient.get()).thenThrow(failure);

        var response = new HolidayApiController(client).holidays(null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of(), response.getBody());
        assertEquals("true", response.getHeaders().getFirst("X-Holidays-Unavailable"));
    }

    @Test
    void successfulResponseKeepsDateFilteringAndOmitsUnavailableHeader() {
        HolidayClient client = mock(HolidayClient.class);
        when(client.fetchHolidays()).thenReturn(new HolidayClient.HolidayResult(
                Map.of("2026-01-01", "元日", "2026-02-11", "建国記念の日"), false));

        var response = new HolidayApiController(client).holidays(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("2026-01-01", "元日"), response.getBody());
        assertNull(response.getHeaders().getFirst("X-Holidays-Unavailable"));
    }
}
