package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.agent.SnmpAgent;
import com.ntc.arbiter.snmpservice.agent.StaticMOGroupExt;
import com.ntc.arbiter.snmpservice.config.AppConfig;
import com.ntc.arbiter.snmpservice.domain.SnmpEvent;
import com.ntc.arbiter.snmpservice.domain.SnmpState;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
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
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
class SnmpServiceTest {

  private static final String TEST_COMPANY_ID = "1.3.6.1.4.1";
  private static final String TEST_SYSTEM = "1";
  private static final String TEST_STATE = "2";
  private static final String TEST_STATE_TABLE = "3";
  private static final String TEST_ALERT = "4";
  private static final String TEST_TRAP = "5";
  private static final String TEST_SEVERITY = "CRITICAL";
  private static final String TEST_AVAILABLE = "1";
  private static final String TEST_API_ACCESS = "2";
  private static final String TEST_CALC_ACCESS = "3";

  @Mock
  private WebClient webClient;

  private Vertx vertx;
  private SnmpService snmpService;
  private SnmpAgent mockAgent;
  private StaticMOGroupExt mockGroup;
  private MockedStatic<AppConfig> mockedAppConfig;
  private MockedStatic<WebClient> mockedWebClient;

  private MonitoringService monitoringService;

  @Mock
  private HttpRequest<Buffer> mockHttpRequest;

  @BeforeEach
  void setUp() throws Exception {
    mockedAppConfig = Mockito.mockStatic(AppConfig.class);
    mockedWebClient = Mockito.mockStatic(WebClient.class);
    mockedAppConfig.when(AppConfig::isTrustAll).thenReturn(true);
    mockedAppConfig.when(AppConfig::getSnmpAgentAddress).thenReturn("udp:0.0.0.0/1610");
    mockedAppConfig.when(AppConfig::getSnmpClientAddress).thenReturn("udp:127.0.0.1/162");
    mockedAppConfig.when(AppConfig::getSnmpClientUsers).thenReturn("user:userPassword:userGroup");

    mockedWebClient.when(() -> WebClient.wrap(any())).thenReturn(webClient);
    vertx = Vertx.vertx();

    monitoringService = mock(MonitoringService.class);
    snmpService = spy(new SnmpService(monitoringService));

    mockAgent = mock(SnmpAgent.class);
    mockGroup = mock(StaticMOGroupExt.class);

    setPrivateField(snmpService, "agent", mockAgent);
    setPrivateField(snmpService, "group", mockGroup);
    setPrivateField(snmpService, "companyId", TEST_COMPANY_ID);
    setPrivateField(snmpService, "system", TEST_SYSTEM);
    setPrivateField(snmpService, "state", TEST_STATE);
    setPrivateField(snmpService, "stateTable", TEST_STATE_TABLE);
    setPrivateField(snmpService, "alert", TEST_ALERT);
    setPrivateField(snmpService, "trap", TEST_TRAP);
    setPrivateField(snmpService, "severity", TEST_SEVERITY);
    setPrivateField(snmpService, "available", TEST_AVAILABLE);
    setPrivateField(snmpService, "apiAccess", TEST_API_ACCESS);
    setPrivateField(snmpService, "calcAccess", TEST_CALC_ACCESS);
    setPrivateField(snmpService, "systemId", TEST_COMPANY_ID + "." + TEST_SYSTEM);
    setPrivateField(snmpService, "systemStateId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE);
    setPrivateField(snmpService, "systemStateTableId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE_TABLE);
    setPrivateField(snmpService, "alertId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_ALERT);
    setPrivateField(snmpService, "trapId", TEST_COMPANY_ID + "." + TEST_TRAP);

    mockedAppConfig.when(AppConfig::getCompanyId).thenReturn(TEST_COMPANY_ID);
    mockedAppConfig.when(AppConfig::getSystem).thenReturn(TEST_SYSTEM);
    mockedAppConfig.when(AppConfig::getTrap).thenReturn(TEST_TRAP);
    mockedAppConfig.when(AppConfig::getState).thenReturn(TEST_STATE);
    mockedAppConfig.when(AppConfig::getAlert).thenReturn(TEST_ALERT);
    mockedAppConfig.when(AppConfig::getStateTable).thenReturn(TEST_STATE_TABLE);
    mockedAppConfig.when(AppConfig::getSeverity).thenReturn(TEST_SEVERITY);
    mockedAppConfig.when(AppConfig::getAvailable).thenReturn(TEST_AVAILABLE);
    mockedAppConfig.when(AppConfig::getApiAccess).thenReturn(TEST_API_ACCESS);
    mockedAppConfig.when(AppConfig::getCalcAccess).thenReturn(TEST_CALC_ACCESS);

    SnmpState apiState = new SnmpState(TEST_API_ACCESS,
      com.ntc.arbiter.snmpservice.domain.TargetType.API,
      com.ntc.arbiter.snmpservice.domain.ObjectState.UNKNOWN);
    setPrivateField(snmpService, "apiState", apiState);

    SnmpState calcState = new SnmpState(TEST_CALC_ACCESS,
      com.ntc.arbiter.snmpservice.domain.TargetType.API,
      com.ntc.arbiter.snmpservice.domain.ObjectState.UNKNOWN);
    setPrivateField(snmpService, "calcState", calcState);
  }

  @AfterEach
  void tearDown() {
    if (vertx != null) {
      vertx.close();
    }
    if (mockedAppConfig != null) {
      mockedAppConfig.close();
    }
    if (mockedWebClient != null) {
      mockedWebClient.close();
    }
  }

  @Test
  void testConfigureAgent_EmptyAddress() throws Exception {
    mockedAppConfig.when(AppConfig::getSnmpAgentAddress).thenReturn(null);
    mockedAppConfig.when(AppConfig::getSnmpClientAddress).thenReturn("udp:127.0.0.1/162");
    mockedAppConfig.when(AppConfig::getSnmpClientUsers).thenReturn("user:userPassword:userGroup");

    MonitoringService realMonitoringService = mock(MonitoringService.class);
    SnmpService realService = new SnmpService(realMonitoringService);

    realService.configureAgent();

    Field agentField = SnmpService.class.getDeclaredField("agent");
    agentField.setAccessible(true);
    SnmpAgent agent = (SnmpAgent) agentField.get(realService);
    assertNull(agent, "Agent should be null when SNMP agent address is empty");
  }

  @Test
  void testCheckServicesAlive_GroupIsNull() {
    try {
      setPrivateField(snmpService, "group", null);
      assertDoesNotThrow(() -> snmpService.checkServicesAlive());
    } catch (Exception e) {
      fail("Не должно быть исключения: " + e.getMessage());
    }
  }

  @Test
  void testCheckSingleServiceAlive_Success() throws Exception {
    String testUrl = "http://api.test";
    Integer32 variable = new Integer32(0);
    when(mockGroup.get(any(OID.class))).thenReturn(variable);
    MonitoringService mockMonitoringService = mock(MonitoringService.class);

    io.vertx.core.Future<String> successFuture = io.vertx.core.Future.succeededFuture("OK");
    when(mockMonitoringService.sendRequest(eq(testUrl))).thenReturn(successFuture);

    SnmpService testService = spy(new SnmpService(mockMonitoringService));

    setPrivateField(testService, "agent", mockAgent);
    setPrivateField(testService, "group", mockGroup);
    setPrivateField(testService, "companyId", TEST_COMPANY_ID);
    setPrivateField(testService, "system", TEST_SYSTEM);
    setPrivateField(testService, "state", TEST_STATE);
    setPrivateField(testService, "stateTable", TEST_STATE_TABLE);
    setPrivateField(testService, "alert", TEST_ALERT);
    setPrivateField(testService, "trap", TEST_TRAP);
    setPrivateField(testService, "severity", TEST_SEVERITY);
    setPrivateField(testService, "available", TEST_AVAILABLE);
    setPrivateField(testService, "apiAccess", TEST_API_ACCESS);
    setPrivateField(testService, "calcAccess", TEST_CALC_ACCESS);
    setPrivateField(testService, "systemId", TEST_COMPANY_ID + "." + TEST_SYSTEM);
    setPrivateField(testService, "systemStateId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE);
    setPrivateField(testService, "systemStateTableId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE_TABLE);
    setPrivateField(testService, "alertId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_ALERT);
    setPrivateField(testService, "trapId", TEST_COMPANY_ID + "." + TEST_TRAP);

    Object contextService = createMockContextService(TEST_API_ACCESS, testUrl, variable);
    addContextToService(testService, TEST_API_ACCESS, contextService);

    SnmpState apiState = new SnmpState(TEST_API_ACCESS,
      com.ntc.arbiter.snmpservice.domain.TargetType.API,
      com.ntc.arbiter.snmpservice.domain.ObjectState.UNKNOWN);
    setPrivateField(testService, "apiState", apiState);

    CountDownLatch latch = new CountDownLatch(1);

    doAnswer(invocation -> {
      latch.countDown();
      return null;
    }).when(testService).sendTrapEvent(any(SnmpEvent.class));

    testService.checkServicesAlive();

    assertTrue(latch.await(5, TimeUnit.SECONDS));

    verify(mockMonitoringService, times(1)).sendRequest(eq(testUrl));
  }

  @Test
  void testCheckSingleServiceAlive_Failure() throws Exception {
    String testUrl = "http://api.test";
    Integer32 variable = new Integer32(1);
    when(mockGroup.get(any(OID.class))).thenReturn(variable);

    MonitoringService mockMonitoringService = mock(MonitoringService.class);

    io.vertx.core.Future<String> failureFuture = io.vertx.core.Future.failedFuture("Connection refused");
    when(mockMonitoringService.sendRequest(eq(testUrl))).thenReturn(failureFuture);

    SnmpService testService = spy(new SnmpService(mockMonitoringService));

    setPrivateField(testService, "agent", mockAgent);
    setPrivateField(testService, "group", mockGroup);
    setPrivateField(testService, "companyId", TEST_COMPANY_ID);
    setPrivateField(testService, "system", TEST_SYSTEM);
    setPrivateField(testService, "state", TEST_STATE);
    setPrivateField(testService, "stateTable", TEST_STATE_TABLE);
    setPrivateField(testService, "alert", TEST_ALERT);
    setPrivateField(testService, "trap", TEST_TRAP);
    setPrivateField(testService, "severity", TEST_SEVERITY);
    setPrivateField(testService, "available", TEST_AVAILABLE);
    setPrivateField(testService, "apiAccess", TEST_API_ACCESS);
    setPrivateField(testService, "calcAccess", TEST_CALC_ACCESS);
    setPrivateField(testService, "systemId", TEST_COMPANY_ID + "." + TEST_SYSTEM);
    setPrivateField(testService, "systemStateId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE);
    setPrivateField(testService, "systemStateTableId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE_TABLE);
    setPrivateField(testService, "alertId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_ALERT);
    setPrivateField(testService, "trapId", TEST_COMPANY_ID + "." + TEST_TRAP);

    Object contextService = createMockContextService(TEST_API_ACCESS, testUrl, variable);
    addContextToService(testService, TEST_API_ACCESS, contextService);

    SnmpState apiState = new SnmpState(TEST_API_ACCESS,
      com.ntc.arbiter.snmpservice.domain.TargetType.API,
      com.ntc.arbiter.snmpservice.domain.ObjectState.UNKNOWN);
    setPrivateField(testService, "apiState", apiState);

    SnmpState calcState = new SnmpState(TEST_CALC_ACCESS,
      com.ntc.arbiter.snmpservice.domain.TargetType.API,
      com.ntc.arbiter.snmpservice.domain.ObjectState.UNKNOWN);
    setPrivateField(testService, "calcState", calcState);

    CountDownLatch latch = new CountDownLatch(1);

    doAnswer(invocation -> {
      latch.countDown();
      return null;
    }).when(testService).sendTrapEvent(any(SnmpEvent.class));

    testService.checkServicesAlive();

    assertTrue(latch.await(5, TimeUnit.SECONDS));

    verify(mockMonitoringService, times(1)).sendRequest(eq(testUrl));
    verify(testService, times(1)).sendTrapEvent(any(SnmpEvent.class));
    assertEquals(0, variable.getValue());
  }

  @Test
  void testHandleMonitoringValue_RestoredConnection() throws Exception {
    Integer32 variable = new Integer32(0);
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test", variable);
    addContextToService(snmpService, TEST_API_ACCESS, contextService);

    java.lang.reflect.Method method = SnmpService.class.getDeclaredMethod(
      "handleMonitoringValue",
      Integer32.class,
      contextService.getClass(),
      boolean.class,
      String.class
    );
    method.setAccessible(true);

    method.invoke(snmpService, variable, contextService, true, "Connection restored");

    assertEquals(1, variable.getValue());
    verify(mockAgent, times(1)).sendTrap(any(SnmpEvent.class));
  }

  @Test
  void testHandleMonitoringValue_ConnectionLost() throws Exception {
    Integer32 variable = new Integer32(1);
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test", variable);
    addContextToService(snmpService, TEST_API_ACCESS, contextService);

    java.lang.reflect.Method method = SnmpService.class.getDeclaredMethod(
      "handleMonitoringValue",
      Integer32.class,
      contextService.getClass(),
      boolean.class,
      String.class
    );
    method.setAccessible(true);

    method.invoke(snmpService, variable, contextService, false, "Connection lost");

    assertEquals(0, variable.getValue());
    verify(mockAgent, times(1)).sendTrap(any(SnmpEvent.class));
  }

  @Test
  void testHandleMonitoringValue_NoChangeNeeded() throws Exception {
    Integer32 variable = new Integer32(1);
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test", variable);
    addContextToService(snmpService, TEST_API_ACCESS, contextService);

    java.lang.reflect.Method method = SnmpService.class.getDeclaredMethod(
      "handleMonitoringValue",
      Integer32.class,
      contextService.getClass(),
      boolean.class,
      String.class
    );
    method.setAccessible(true);

    method.invoke(snmpService, variable, contextService, true, "Already OK");

    assertEquals(1, variable.getValue());
    verify(mockAgent, never()).sendTrap(any(SnmpEvent.class));
  }

  @Test
  void testSendTrapEvent_Success() throws Exception {
    Integer32 variable = new Integer32(1);
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test", variable);
    addContextToService(snmpService, TEST_API_ACCESS, contextService);

    SnmpEvent event = SnmpEvent.createTrapEvent(
      TEST_COMPANY_ID + "." + TEST_TRAP,
      TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_ALERT,
      SnmpEvent.Severity.CRITICAL,
      false,
      (ContextService) contextService
    );

    snmpService.sendTrapEvent(event);

    verify(mockAgent, times(1)).sendTrap(event);
  }

  @Test
  void testSendTrapEvent_Failure() throws Exception {
    Integer32 variable = new Integer32(1);
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test", variable);
    addContextToService(snmpService, TEST_API_ACCESS, contextService);

    SnmpEvent event = SnmpEvent.createTrapEvent(
      TEST_COMPANY_ID + "." + TEST_TRAP,
      TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_ALERT,
      SnmpEvent.Severity.CRITICAL,
      false,
      (ContextService) contextService
    );

    doThrow(new java.io.IOException("Send failed")).when(mockAgent).sendTrap(any(SnmpEvent.class));

    java.lang.reflect.Method method = SnmpService.class.getDeclaredMethod("sendTrapEvent", SnmpEvent.class);
    method.setAccessible(true);

    assertDoesNotThrow(() -> method.invoke(snmpService, event));
    verify(mockAgent, times(1)).sendTrap(event);
  }

  @Test
  void testDestroy() {
    snmpService.destroy();

    verify(mockAgent, times(1)).stop();
  }

  @Test
  void testDestroy_WithException() throws Exception {
    doThrow(new RuntimeException("Stop failed")).when(mockAgent).stop();

    assertDoesNotThrow(() -> snmpService.destroy());

    verify(mockAgent, times(1)).stop();
  }

  @Test
  void testGetStateOID() throws Exception {
    java.lang.reflect.Method method = SnmpService.class.getDeclaredMethod("getStateOID", String.class);
    method.setAccessible(true);

    OID result = (OID) method.invoke(snmpService, TEST_API_ACCESS);

    String expected = TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE + "." + TEST_API_ACCESS;
    assertEquals(expected, result.toString());
  }

  @Test
  void testCreateStateTable() throws Exception {
    SnmpService realService = new SnmpService(monitoringService);

    setPrivateField(realService, "companyId", TEST_COMPANY_ID);
    setPrivateField(realService, "system", TEST_SYSTEM);
    setPrivateField(realService, "systemStateTableId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE_TABLE);
    setPrivateField(realService, "stateTable", TEST_STATE_TABLE);
    setPrivateField(realService, "apiAccess", TEST_API_ACCESS);
    setPrivateField(realService, "calcAccess", TEST_CALC_ACCESS);

    java.lang.reflect.Method method = SnmpService.class.getDeclaredMethod("createStateTable");
    method.setAccessible(true);

    Object table = method.invoke(realService);

    assertNotNull(table);
    assertTrue(table instanceof org.snmp4j.agent.mo.MOTable);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Object createMockContextService(String access, String url, Integer32 variable) throws Exception {
    Class<?> contextClass = Class.forName("com.ntc.arbiter.snmpservice.service.ContextService");
    Object contextMock = mock(contextClass);

    when(contextMock.getClass().getMethod("getAccess").invoke(contextMock)).thenReturn(access);
    when(contextMock.getClass().getMethod("getUrl").invoke(contextMock)).thenReturn(url);
    when(contextMock.getClass().getMethod("getServiceName").invoke(contextMock)).thenReturn("TestService");
    when(contextMock.getClass().getMethod("getSuccessMessage").invoke(contextMock)).thenReturn("Connection restored");
    when(contextMock.getClass().getMethod("getFailureMessage").invoke(contextMock)).thenReturn("Connection lost");

    java.util.concurrent.atomic.AtomicBoolean checkInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);
    when(contextMock.getClass().getMethod("getCheckInProgress").invoke(contextMock)).thenReturn(checkInProgress);

    return contextMock;
  }

  private void addContextToService(SnmpService service, String key, Object context) throws Exception {
    Field contextField = SnmpService.class.getDeclaredField("contextServices");
    contextField.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> contextMap = (java.util.Map<String, Object>) contextField.get(service);
    contextMap.put(key, context);
  }
}
