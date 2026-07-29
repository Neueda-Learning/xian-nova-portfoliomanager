package org.xian.protfoliomanage.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PriceClientServiceTest {

    @Test
    void getCurrentPriceReturnsNullForBlankTicker() {
        PriceClientService service = new PriceClientService(RestClient.builder(), new ObjectMapper(), "http://localhost");

        assertNull(service.getCurrentPrice("   "));
    }

    @Test
    void getCurrentPriceReturnsOneForCash() {
        PriceClientService service = new PriceClientService(RestClient.builder(), new ObjectMapper(), "http://localhost");

        assertEquals(BigDecimal.ONE, service.getCurrentPrice(" cash "));
    }

    @Test
    void fetchLatestQuoteReturnsSyntheticQuoteForCash() {
        PriceClientService service = new PriceClientService(RestClient.builder(), new ObjectMapper(), "http://localhost");

        PriceClientService.PriceQuote quote = service.fetchLatestQuote("cash");

        assertNotNull(quote);
        assertEquals("CASH", quote.ticker());
        assertEquals(BigDecimal.ONE, quote.latestPrice());
    }

    @Test
    void fetchLatestQuoteParsesNestedCloseArray() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PriceClientService service = new PriceClientService(builder, new ObjectMapper(), "http://localhost");

        server.expect(requestTo("http://localhost?ticker=AAPL"))
                .andRespond(withSuccess("{\"price_data\":{\"close\":[100.10,101.25]}}", MediaType.APPLICATION_JSON));

        PriceClientService.PriceQuote quote = service.fetchLatestQuote("aapl");

        assertNotNull(quote);
        assertEquals("AAPL", quote.ticker());
        assertEquals(new BigDecimal("101.25"), quote.latestPrice());
        server.verify();
    }

    @Test
    void getCurrentPriceUsesCacheWithinWindow() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PriceClientService service = new PriceClientService(builder, new ObjectMapper(), "http://localhost");

        server.expect(requestTo("http://localhost?ticker=MSFT"))
                .andRespond(withSuccess("{\"close\":[300.50]}", MediaType.APPLICATION_JSON));

        BigDecimal first = service.getCurrentPrice("msft");
        BigDecimal second = service.getCurrentPrice("MSFT");

        assertEquals(0, new BigDecimal("300.50").compareTo(first));
        assertEquals(first, second);
        server.verify();
    }

    @Test
    void fetchLatestQuoteReturnsNullWhenNoParsablePriceExists() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PriceClientService service = new PriceClientService(builder, new ObjectMapper(), "http://localhost");

        server.expect(requestTo("http://localhost?ticker=FAIL"))
                .andRespond(withSuccess("{\"close\":[]}", MediaType.APPLICATION_JSON));

        PriceClientService.PriceQuote quote = service.fetchLatestQuote("FAIL");

        assertNull(quote);
        server.verify();
    }

    @Test
    void getPriceForDateReturnsHistoricalCloseByPurchaseDate() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PriceClientService service = new PriceClientService(builder, new ObjectMapper(), "http://localhost");

        server.expect(requestTo("http://localhost?ticker=AAPL"))
                .andRespond(withSuccess("{\"price_data\":{\"close\":[10.00,11.00,12.00,13.00]}}", MediaType.APPLICATION_JSON));

        BigDecimal price = service.getPriceForDate("AAPL", LocalDate.now().minusDays(2));

        assertEquals(0, new BigDecimal("11.00").compareTo(price));
        server.verify();
    }

    @Test
    void getPriceForDateUsesLatestWhenPurchaseDateIsTodayOrLater() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PriceClientService service = new PriceClientService(builder, new ObjectMapper(), "http://localhost");

        server.expect(requestTo("http://localhost?ticker=TSLA"))
                .andRespond(withSuccess("{\"close\":[200.00,220.00]}\n", MediaType.APPLICATION_JSON));

        BigDecimal price = service.getPriceForDate("TSLA", LocalDate.now().plusDays(1));

        assertEquals(0, new BigDecimal("220.00").compareTo(price));
        server.verify();
    }
}

