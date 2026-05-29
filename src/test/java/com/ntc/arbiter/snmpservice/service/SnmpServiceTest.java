package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.agent.SnmpAgent;
import com.ntc.arbiter.snmpservice.agent.StaticMOGroupExt;
import com.ntc.arbiter.snmpservice.config.AppConfig;
import com.ntc.arbiter.snmpservice.domain.ObjectState;
import com.ntc.arbiter.snmpservice.domain.SnmpEvent;
import com.ntc.arbiter.snmpservice.domain.SnmpState;
import com.ntc.arbiter.snmpservice.domain.TargetType;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  private static final String TEST_UI_ACCESS = "4";

  private Vertx vertx;
  private SnmpService snmpService;
  private SnmpAgent mockAgent;
  private StaticMOGroupExt mockGroup;
  private MockedStatic<AppConfig> mockedAppConfig;
  private MonitoringService monitoringService;

  @BeforeEach
  void setUp() throws Exception {
    mockedAppConfig = Mockito.mockStatic(AppConfig.class);
    vertx = Vertx.vertx();

    monitoringService = mock(MonitoringService.class);
    snmpService = spy(new SnmpService(monitoringService));

    mockAgent = mock(SnmpAgent.class);
    mockGroup = mock(StaticMOGroupExt.class);

    initPrivateFields(snmpService);


    SnmpState apiState = new SnmpState(TEST_API_ACCESS,
      TargetType.API,
      ObjectState.UNKNOWN);
    setPrivateField(snmpService, "apiState", apiState);

    SnmpState calcState = new SnmpState(TEST_CALC_ACCESS,
      TargetType.CALC,
      ObjectState.UNKNOWN);
    setPrivateField(snmpService, "calcState", calcState);

    SnmpState uiState = new SnmpState(TEST_UI_ACCESS,
      TargetType.UI,
      ObjectState.UNKNOWN);
    setPrivateField(snmpService, "uiState", uiState);
  }

  @AfterEach
  void tearDown() {
    if (vertx != null) {
      vertx.close();
    }
    if (mockedAppConfig != null) {
      mockedAppConfig.close();
    }
  }

  @Test
  void testConfigureAgent_EmptyAddress() throws Exception {
    MonitoringService realMonitoringService = mock(MonitoringService.class);
    SnmpService realService = new SnmpService(realMonitoringService);

    realService.configureAgent();

    Field agentField = SnmpService.class.getDeclaredField("agent");
    agentField.setAccessible(true);
    SnmpAgent agent = (SnmpAgent) agentField.get(realService);
    assertNull(agent, "Agent should be null when SNMP agent address is empty");
  }

  @Test
  void testConfigureAgent_SuccessfulInitialization() throws Exception {
    SnmpService spyService = spy(snmpService);
    SnmpAgent mockAgentLocal = mock(SnmpAgent.class);
    doNothing().when(mockAgentLocal).start();
    doNothing().when(mockAgentLocal).unregisterManagedObject(any());
    doNothing().when(mockAgentLocal).registerManagedObject(any(StaticMOGroupExt.class));
    doNothing().when(mockAgentLocal).registerManagedObject(any(org.snmp4j.agent.mo.MOTable.class));

    doReturn(mockAgentLocal).when(spyService).createSnmpAgent(anyString(), anyString());

    Method method = SnmpService.class.getDeclaredMethod("configureAgent", String.class, String.class, String.class);
    method.setAccessible(true);

    initPrivateFields(spyService);

    method.invoke(spyService, "0.0.0.0/161", "0.0.0.0/162", "user:userPassword:userGroup");

    verify(mockAgentLocal, times(1)).start();
    verify(mockAgentLocal, times(1)).unregisterManagedObject(any());
    verify(mockAgentLocal, times(2)).registerManagedObject(any());
    verify(mockAgentLocal, times(1)).addUserSecurity("user", "userPassword", "userGroup");
  }

  @Test
  void testConfigureAgent_WhenUsersNull_ShouldNotAddUsers() throws Exception {
    SnmpService spyService = spy(snmpService);
    SnmpAgent mockAgentLocal = mock(SnmpAgent.class);
    doNothing().when(mockAgentLocal).start();
    doNothing().when(mockAgentLocal).unregisterManagedObject(any());
    doNothing().when(mockAgentLocal).registerManagedObject(any(StaticMOGroupExt.class));
    doNothing().when(mockAgentLocal).registerManagedObject(any(org.snmp4j.agent.mo.MOTable.class));

    doReturn(mockAgentLocal).when(spyService).createSnmpAgent(anyString(), anyString());

    Method method = SnmpService.class.getDeclaredMethod("configureAgent", String.class, String.class, String.class);
    method.setAccessible(true);

    initPrivateFields(spyService);

    method.invoke(spyService, "0.0.0.0/161", "0.0.0.0/162", (String) null);

    verify(mockAgentLocal, times(1)).start();
    verify(mockAgentLocal, never()).addUserSecurity(anyString(), anyString(), anyString());
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
    when(mockMonitoringService.sendGetRequest(eq(testUrl))).thenReturn(successFuture);

    SnmpService testService = spy(new SnmpService(mockMonitoringService));

    initPrivateFields(testService);

    Object contextService = createMockContextService(TEST_API_ACCESS, testUrl);
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

    verify(mockMonitoringService, times(1)).sendGetRequest(eq(testUrl));
  }

  @Test
  void testCheckSingleServiceAlive_Failure() throws Exception {
    String testUrl = "http://api.test";
    Integer32 variable = new Integer32(1);
    when(mockGroup.get(any(OID.class))).thenReturn(variable);

    MonitoringService mockMonitoringService = mock(MonitoringService.class);

    io.vertx.core.Future<String> failureFuture = io.vertx.core.Future.failedFuture("Connection refused");
    when(mockMonitoringService.sendGetRequest(eq(testUrl))).thenReturn(failureFuture);

    SnmpService testService = spy(new SnmpService(mockMonitoringService));

    initPrivateFields(testService);

    Object contextService = createMockContextService(TEST_API_ACCESS, testUrl);
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

    verify(mockMonitoringService, times(1)).sendGetRequest(eq(testUrl));
    verify(testService, times(1)).sendTrapEvent(any(SnmpEvent.class));
    assertEquals(0, variable.getValue());
  }

  @Test
  void testHandleMonitoringValue_RestoredConnection() throws Exception {
    Integer32 variable = new Integer32(0);
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test");
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
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test");
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
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test");
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
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test");
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
    Object contextService = createMockContextService(TEST_API_ACCESS, "http://api.test");
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
  void testDestroy_WithException() {
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

    initPrivateFields(realService);

    java.lang.reflect.Method method = SnmpService.class.getDeclaredMethod("createStateTable");
    method.setAccessible(true);

    Object table = method.invoke(realService);

    assertNotNull(table);
    assertInstanceOf(MOTable.class, table);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Object createMockContextService(String access, String url) throws Exception {
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

  private void initPrivateFields(SnmpService snmpService) throws Exception {
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
    setPrivateField(snmpService, "uiAccess", TEST_UI_ACCESS);
    setPrivateField(snmpService, "systemId", TEST_COMPANY_ID + "." + TEST_SYSTEM);
    setPrivateField(snmpService, "systemStateId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE);
    setPrivateField(snmpService, "systemStateTableId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_STATE_TABLE);
    setPrivateField(snmpService, "alertId", TEST_COMPANY_ID + "." + TEST_SYSTEM + "." + TEST_ALERT);
    setPrivateField(snmpService, "trapId", TEST_COMPANY_ID + "." + TEST_TRAP);
  }
}
