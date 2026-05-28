package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.agent.SnmpAgent;
import com.ntc.arbiter.snmpservice.agent.StaticMOGroupExt;
import com.ntc.arbiter.snmpservice.config.AppConfig;
import com.ntc.arbiter.snmpservice.constants.Constants;
import com.ntc.arbiter.snmpservice.domain.*;
import io.vertx.core.Future;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class SnmpService {

  private static final Logger logger = LoggerFactory.getLogger(SnmpService.class);

  private final Map<String, ContextService> contextServices = new ConcurrentHashMap<>();

  protected final String companyId = AppConfig.getCompanyId();
  private final String system = AppConfig.getSystem();
  private final String trap = AppConfig.getTrap();
  private final String state = AppConfig.getState();
  private final String alert = AppConfig.getAlert();
  private final String stateTable = AppConfig.getStateTable();
  private final String severity = AppConfig.getSeverity();
  private final String available = AppConfig.getAvailable();
  private final String apiAccess = AppConfig.getApiAccess();
  private final String calcAccess = AppConfig.getCalcAccess();
  private final String uiAccess = AppConfig.getUiAccess();

  private String trapId;
  private String systemId;
  private String systemStateId;
  private String systemStateTableId;
  private String alertId;

  private final static int ON = 1;
  private final static int OFF = 0;

  private SnmpAgent agent;
  private StaticMOGroupExt group;

  private SnmpState apiState;
  private SnmpState calcState;
  private SnmpState uiState;

  private final MonitoringService monitoringService;

  public SnmpService(MonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  public void configureAgent() {
    configureAgent(AppConfig.getSnmpAgentAddress(), AppConfig.getSnmpClientAddress(), AppConfig.getSnmpClientUsers());
  }

  private void configureAgent(String agentAddress, String trapAddress, String users) {
    if (agentAddress == null || agentAddress.isEmpty()) {
      return;
    }
    systemId = companyId + '.' + system;
    systemStateId = systemId + '.' + state;
    systemStateTableId = systemId + '.' + stateTable;
    alertId = systemId + '.' + alert;
    trapId = companyId + '.' + trap;
    logger.info("systemId : " + systemId);
    logger.info("systemStateId : " + systemStateId);
    logger.info("systemStateTableId : " + systemStateTableId);
    logger.info("alertId : " + alertId);
    logger.info("trapId : " + trapId);

    try {
      agent = new SnmpAgent(agentAddress, trapAddress);
      if (users != null && !users.trim().isEmpty() && !"-".equals(users)) {
        for (String usrData : users.split("/")) {
          String[] usr = usrData.split(":");
          if (usr.length == 3) {
            agent.addUserSecurity(usr[0], usr[1], usr[2]);
          }
        }
      }
      agent.start();
      agent.unregisterManagedObject(agent.getSnmpv2MIB());
      List<VariableBinding> variables = new ArrayList<>();
      addCurrentDate(variables);
      addingVariables(variables);
      group = new StaticMOGroupExt(new OID(systemStateId), variables.toArray(new VariableBinding[0]));
      agent.registerManagedObject(group);
      agent.registerManagedObject(createStateTable());
    } catch (IOException e) {
      agent = null;
      String message = Constants.CREATE_AGENT_FAILURE;
      logger.error(message + ": " +  e.toString());
    }
  }

  private MOTable createStateTable() {
    OID tableRootOid = new OID(systemStateTableId + ".1");
    logger.info("tableRootOID : " + tableRootOid);
    MOTableSubIndex[] subIndexes = new MOTableSubIndex[]{new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER)};
    MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

    DefaultMOTable table = new DefaultMOTable(tableRootOid, indexDef, SnmpState.columns());
    MOMutableTableModel model = (MOMutableTableModel) table.getModel();

    apiState = new SnmpState(apiAccess, TargetType.API, ObjectState.UNKNOWN);
    model.addRow(apiState.row());

    calcState = new SnmpState(calcAccess, TargetType.CALC, ObjectState.UNKNOWN);
    model.addRow(calcState.row());

    uiState = new SnmpState(uiAccess, TargetType.UI, ObjectState.UNKNOWN);
    model.addRow(uiState.row());

    table.setVolatile(true);
    return table;
  }

  private OID getStateOID(String state) {
    return new OID(systemStateId + '.' + state);
  }

  private void addCurrentDate(List<VariableBinding> bind) {
    if (available != null) {
      OID systemAvailableOID = getStateOID(available);
      logger.info("systemAvailableOID : " + systemAvailableOID);
      bind.add(new VariableBinding(systemAvailableOID, new CurrentDate()));
    }
  }

  private void addingVariables(List<VariableBinding> bind) {
    if (apiAccess != null) {
      addServiceVariable(bind, apiAccess, AppConfig.getApiUrl(),
        Constants.API_TARGET_NAME, Constants.API_CONNECTION_RESTORED_VALUE,
        Constants.API_NO_ACCESS_VALUE, Constants.API_CONNECTION_RESTORED_MESSAGE,
        Constants.API_NO_ACCESS_MESSAGE, null);
    }
    if (calcAccess != null) {
      addServiceVariable(bind, calcAccess, AppConfig.getCalcUrl(),
        Constants.CALC_TARGET_NAME, Constants.CALC_CONNECTION_RESTORED_VALUE,
        Constants.CALC_NO_ACCESS_VALUE, Constants.CALC_CONNECTION_RESTORED_MESSAGE,
        Constants.CALC_NO_ACCESS_MESSAGE, null);
    }
    if (uiAccess != null) {
      String uiUrl = AppConfig.getUiUrl();
      String hostHeader = extractHostFromUrl(uiUrl);
      addServiceVariable(bind, uiAccess, uiUrl,
        Constants.UI_TARGET_NAME, Constants.UI_CONNECTION_RESTORED_VALUE,
        Constants.UI_NO_ACCESS_VALUE, Constants.UI_CONNECTION_RESTORED_MESSAGE,
        Constants.UI_NO_ACCESS_MESSAGE, hostHeader);
    }
  }

  private void addServiceVariable(List<VariableBinding> bind, String access, String url,
                                  String serviceName, String connectionRestoredValue,
                                  String noAccessValue, String connectionRestoredMessage,
                                  String noAccessMessage, String hostHeader) {
    Integer32 variable = new Integer32(ON);
    OID accessOID = getStateOID(access);
    logger.info(serviceName + " OID : " + accessOID);
    bind.add(new VariableBinding(accessOID, variable));

    ContextService.Builder builder = new ContextService.Builder()
      .setAccess(access)
      .setVariable(variable)
      .setServiceName(serviceName)
      .setUrl(url)
      .setConnectionRestoredValue(connectionRestoredValue)
      .setNoAccessValue(noAccessValue)
      .setConnectionRestoredMessage(connectionRestoredMessage)
      .setNoAccessMessage(noAccessMessage);

    if (hostHeader != null) {
      builder.setHostHeader(hostHeader);
    }

    contextServices.put(access, builder.build());
  }

  private String extractHostFromUrl(String url) {
    if (url == null || url.isEmpty()) {
      return null;
    }
    String withoutProtocol = url.replaceFirst("^https?://", "");
    int slashIndex = withoutProtocol.indexOf('/');
    if (slashIndex > 0) {
      withoutProtocol = withoutProtocol.substring(0, slashIndex);
    }
    int colonIndex = withoutProtocol.indexOf(':');
    if (colonIndex > 0) {
      withoutProtocol = withoutProtocol.substring(0, colonIndex);
    }
    return withoutProtocol;
  }

  public void checkServicesAlive() {
    if (group == null) {
      return;
    }

    for (ContextService contextService : contextServices.values()) {
      checkSingleServiceAlive(contextService);
    }
  }

  private void checkSingleServiceAlive(ContextService context) {
    Integer32 variable = (Integer32) group.get(getStateOID(context.getAccess()));
    if (variable == null) {
      return;
    }

    context.getCheckInProgress().set(true);

    Future<String> requestFuture = getRequestFuture(context);

    requestFuture.onComplete(rez -> {
      try {
        if (rez.succeeded()) {
          logger.info("Получен ответ от " + context.getServiceName() + ": " + rez.result());
          handleMonitoringValue(variable, context, true, context.getSuccessMessage());
        } else {
          handleMonitoringValue(variable, context, false, context.getFailureMessage());
        }
      } catch (Exception ex) {
        try {
          handleMonitoringValue(variable, context, false, context.getFailureMessage());
        } catch (Exception handleEx) {
          logger.error("Ошибка обработки: " + context.getServiceName() + ": " + ex.getMessage());
        }
      } finally {
        context.getCheckInProgress().set(false);
      }
    });
  }

  private Future<String> getRequestFuture(ContextService context) {
    String url = context.getUrl();

    if (Constants.UI_TARGET_NAME.equals(context.getServiceName()) && context.getHostHeader() != null) {
      return monitoringService.sendHeadRequest(url, context.getHostHeader());
    } else {
      return monitoringService.sendGetRequest(url);
    }
  }

  private void handleMonitoringValue(Integer32 value, ContextService context, boolean success, String logMessage) {
    Function<SnmpEvent.Severity, SnmpEvent> eventCreator = s -> SnmpEvent.createTrapEvent(trapId, alertId, s, success, context);

    SnmpState targetState = getTargetState(context.getServiceName());

    if (success) {
      if (targetState != null) {
        targetState.setState(ObjectState.OK);
      }
      if (value.getValue() == OFF) {
        SnmpEvent.Severity severityValue = SnmpEvent.Severity.CLEARED;
        logger.info(logMessage + " Value " + severityValue);
        sendTrapEvent(eventCreator.apply(severityValue));
      }
      value.setValue(ON);
    } else {
      logger.error(logMessage);
      if (targetState != null) {
        targetState.setState(ObjectState.CRITICAL_ERROR);
      }
      if (value.getValue() != OFF) {
        SnmpEvent.Severity severityValue = SnmpEvent.Severity.valueOf(severity);
        logger.info(logMessage + " Value " + severityValue);
        sendTrapEvent(eventCreator.apply(severityValue));
      }
      value.setValue(OFF);
    }
  }

  private SnmpState getTargetState(String serviceName) {
    if (Constants.API_TARGET_NAME.equals(serviceName)) {
      return apiState;
    } else if (Constants.CALC_TARGET_NAME.equals(serviceName)) {
      return calcState;
    } else if (Constants.UI_TARGET_NAME.equals(serviceName)) {
      return uiState;
    }
    return null;
  }

  void sendTrapEvent(SnmpEvent event) {
    try {
      agent.sendTrap(event);
      logger.info(Constants.SEND_TRAP);
    } catch (IOException e) {
      logger.error(String.format(Constants.SEND_TRAP_FAILED, e.getMessage()));
    }
  }

  public void destroy() {
    try {
      agent.stop();
    } catch (Exception ignored) {
    }
  }
}
