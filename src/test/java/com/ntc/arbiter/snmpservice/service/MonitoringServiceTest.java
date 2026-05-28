package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class MonitoringServiceTest {

  @Mock
  private WebClient webClient;

  @Mock
  private HttpRequest<Buffer> httpRequest;

  @Mock
  private HttpResponse<Buffer> httpResponse;

  private MonitoringService monitoringService;
  private MockedStatic<AppConfig> mockedAppConfig;

  @BeforeEach
  void setUp() {
    mockedAppConfig = Mockito.mockStatic(AppConfig.class);
    monitoringService = new MonitoringService(webClient) {
    };
  }

  @AfterEach
  void tearDown() {
    if (mockedAppConfig != null) {
      mockedAppConfig.close();
    }
  }

  @Test
  void sendGetRequest_Success_ShouldReturnResponseBody(VertxTestContext testContext) {
    String url = "http://example.com/api/test";
    String expectedResponse = "{\"status\":\"ok\"}";

    when(webClient.getAbs(url)).thenReturn(httpRequest);
    when(httpRequest.putHeader("Content-Type", "application/json")).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsString()).thenReturn(expectedResponse);

    monitoringService.sendGetRequest(url)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(expectedResponse, response);
        verify(webClient).getAbs(url);
        verify(httpRequest).putHeader("Content-Type", "application/json");
        verify(httpRequest).timeout(30000);
        verify(httpRequest).send();
        testContext.completeNow();
      })));
  }

  @Test
  void sendGetRequest_WithHttpError_ShouldFail(VertxTestContext testContext) {
    String url = "http://example.com/api/test";
    int errorCode = 404;

    when(webClient.getAbs(url)).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(errorCode);

    monitoringService.sendGetRequest(url)
      .onComplete(testContext.failing(throwable -> testContext.verify(() -> {
        assertTrue(throwable.getMessage().contains("HTTP error: " + errorCode));
        testContext.completeNow();
      })));
  }

  @Test
  void sendGetRequest_WithNetworkError_ShouldFail(VertxTestContext testContext) {
    String url = "http://example.com/api/test";
    String errorMessage = "Connection refused";

    when(webClient.getAbs(url)).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.failedFuture(errorMessage));

    monitoringService.sendGetRequest(url)
      .onComplete(testContext.failing(throwable -> testContext.verify(() -> {
        assertEquals(errorMessage, throwable.getMessage());
        testContext.completeNow();
      })));
  }

  @Test
  void sendGetRequest_WithRedirectStatus_ShouldBeSuccess(VertxTestContext testContext) {
    String url = "http://example.com/api/test";

    when(webClient.getAbs(url)).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(302);

    monitoringService.sendGetRequest(url)
      .onComplete(testContext.failing(throwable  -> testContext.verify(() -> {
        assertTrue(throwable.getMessage().contains("HTTP error: 302"));
        testContext.completeNow();
      })));
  }

  @Test
  void sendHeadRequest_SuccessWithStatus200_ShouldReturnOkMessage(VertxTestContext testContext) {
    String url = "https://127.0.0.0";
    String hostHeader = "msk-arbitr-dev01.ntcees.ru";
    String expectedResponse = "HTTP/1.1 200 OK";

    when(webClient.headAbs(url)).thenReturn(httpRequest);
    when(httpRequest.putHeader("Host", hostHeader)).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);

    monitoringService.sendHeadRequest(url, hostHeader)
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(expectedResponse, response);
        verify(webClient).headAbs(url);
        verify(httpRequest).putHeader("Host", hostHeader);
        verify(httpRequest).timeout(30000);
        testContext.completeNow();
      })));
  }

  @Test
  void sendHeadRequest_WithNon200Status_ShouldFail(VertxTestContext testContext) {
    String url = "https://127.0.0.0";
    String hostHeader = "msk-arbitr-dev01.ntcees.ru";
    int errorCode = 500;

    when(webClient.headAbs(url)).thenReturn(httpRequest);
    when(httpRequest.putHeader("Host", hostHeader)).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(errorCode);

    monitoringService.sendHeadRequest(url, hostHeader)
      .onComplete(testContext.failing(throwable -> testContext.verify(() -> {
        assertTrue(throwable.getMessage().contains("HTTP error: " + errorCode));
        testContext.completeNow();
      })));
  }

  @Test
  void sendHeadRequest_WithNetworkError_ShouldFail(VertxTestContext testContext) {
    String url = "https://127.0.0.0";
    String hostHeader = "msk-arbitr-dev01.ntcees.ru";
    String errorMessage = "SSL handshake failed";

    when(webClient.headAbs(url)).thenReturn(httpRequest);
    when(httpRequest.putHeader("Host", hostHeader)).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.failedFuture(errorMessage));

    monitoringService.sendHeadRequest(url, hostHeader)
      .onComplete(testContext.failing(throwable -> testContext.verify(() -> {
        assertEquals(errorMessage, throwable.getMessage());
        testContext.completeNow();
      })));
  }

  @Test
  void sendHeadRequest_WithTimeout_ShouldFail(VertxTestContext testContext) {
    String url = "https://127.0.0.0";
    String hostHeader = "msk-arbitr-dev01.ntcees.ru";
    String timeoutMessage = "Timeout";

    when(webClient.headAbs(url)).thenReturn(httpRequest);
    when(httpRequest.putHeader("Host", hostHeader)).thenReturn(httpRequest);
    when(httpRequest.timeout(anyLong())).thenReturn(httpRequest);
    when(httpRequest.send()).thenReturn(Future.failedFuture(timeoutMessage));

    monitoringService.sendHeadRequest(url, hostHeader)
      .onComplete(testContext.failing(throwable -> testContext.verify(() -> {
        assertEquals(timeoutMessage, throwable.getMessage());
        testContext.completeNow();
      })));
  }
}
