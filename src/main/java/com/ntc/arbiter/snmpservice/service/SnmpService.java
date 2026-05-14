package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.agent.SnmpAgent;
import com.ntc.arbiter.snmpservice.agent.StaticMOGroupExt;
import com.ntc.arbiter.snmpservice.config.AppConfig;
import com.ntc.arbiter.snmpservice.config.SupervisorAppConfig;
import com.ntc.arbiter.snmpservice.domain.*;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

//import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SnmpService {

    //private static final Logger logger = LoggerFactory.create(SnmpService.class);

    //@Value("${iso.org.dod.internet.private.enterprises.ntc}")
    protected String companyId;
    //@Value("${ntc.snmp.system}")
    private String system;
    //@Value("${ntc.snmp.trap}")
    private String trap;
    //@Value("${ntc.snmp.state}")
    private String state;
    //@Value("${ntc.snmp.alert}")
    private String alert;
    //@Value("${ntc.snmp.stateTable}")
    private String stateTable;
    //@Value("${ntc.snmp.severity}")
    private String severity;

    //@Value("${ntc.system.available}")
    private String available;
    //@Value("${ntc.system.api.alive}")
    private String apiAccess;
    //@Value("${ntc.system.sql.alive}")
    private String sqlAccess;
    //@Value("${ntc.system.ldap.alive}")
    private String ldapAccess;


    private String trapId;
    private String systemId;
    private String systemStateId;
    private String systemStateTableId;
    private String alertId;

    private final static int ON = 1;
    private final static int OFF = 0;

    private SnmpAgent agent;
    private StaticMOGroupExt group;

    private SnmpState supervisorState;
    private SnmpState apiState;
    private SnmpState sqlState;
    private SnmpState ldapState;

    private final AppConfig config;
    //private final MonitoringService monitoringService;

    private boolean checkApiInProgress = false;

    /**
     * Время последнего обращения к службе Супервизора со стороны службы api
     */
    private volatile long lastApiAccessTime = new Date().getTime();

//    public SnmpService(SupervisorAppConfig config, MonitoringService monitoringService) {
//        this.config = config;
//        this.monitoringService = monitoringService;
//    }

  public SnmpService(AppConfig config) {
    this.config = config;
  }

  //@PostConstruct
    public void configureAgent() {
        configureAgent(config.getSnmpAgentAddress(), config.getSnmpClientAddress(), config.getSnmpClientUsers());
    }

    private void configureAgent(String agentAddress, String trapAddress, String users) {
        if (agentAddress == null || agentAddress.isEmpty()) {
            return;
        }
        systemId = companyId + '.' + system; //идентификатор системы
        systemStateId = systemId + '.' + state; //идентификатор состояния системы
        systemStateTableId = systemId + '.' + stateTable; //идентификатор состояния системы
        alertId = systemId + '.' + alert; //идентификатор атрибутов trap-сообщения
        trapId = companyId + '.' + trap; //идентификатор trap-сообщения
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
            //logger.log(Level.WARNING, "snmp.create-agent.failure", e.toString());
        }
    }

    private MOTable createStateTable() {
        OID tableRootOid = new OID(systemStateTableId + ".1");
        MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER) };
        MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

        DefaultMOTable table = new DefaultMOTable(tableRootOid, indexDef, SnmpState.columns());
        MOMutableTableModel model = (MOMutableTableModel) table.getModel();

        supervisorState = new SnmpState(available, TargetType.SUPERVISOR, ObjectState.OK);
        apiState = new SnmpState(apiAccess, TargetType.API, ObjectState.UNKNOWN);
        sqlState = new SnmpState(sqlAccess, TargetType.DATABASE, ObjectState.UNKNOWN);
        ldapState = new SnmpState(ldapAccess, TargetType.LDAP, ObjectState.UNKNOWN);

        model.addRow(supervisorState.row());
        model.addRow(apiState.row());
        model.addRow(sqlState.row());
        model.addRow(ldapState.row());

        table.setVolatile(true);
        return table;
    }

    private OID getStateOID(String state) {
        return new OID(systemStateId + '.' + state);
    }

    private void addCurrentDate(List<VariableBinding> bind) {
        if (available != null) {
            bind.add(new VariableBinding(getStateOID(available), new CurrentDate()));
        }
    }

    private void addingVariables(List<VariableBinding> bind) {
        if (apiAccess != null) {
            bind.add(new VariableBinding(getStateOID(apiAccess), new Integer32(ON)));
        }
        if (sqlAccess != null) {
            bind.add(new VariableBinding(getStateOID(sqlAccess), new Integer32(ON)));
        }
        if (ldapAccess != null) {
            bind.add(new VariableBinding(getStateOID(ldapAccess), new Integer32(ON)));
        }

        //todo добавить сюда остальные параметры
    }

    //@Scheduled(cron = "${snmp.check.api.cron}")
    public void checkApiAlive() {
        if (group == null || apiAccess == null || sqlAccess == null || ldapAccess == null || checkApiInProgress) {
            return;
        }
        Integer32 apiAccessValue = (Integer32) group.get(getStateOID(apiAccess));
        Integer32 sqlAccessValue = (Integer32) group.get(getStateOID(sqlAccess));
        Integer32 ldapAccessValue = (Integer32) group.get(getStateOID(ldapAccess));
        if (apiAccessValue == null || sqlAccessValue == null) {
            return;
        }
        checkApiInProgress = true;
        try {
//            MonitoringState monitoringState = monitoringService.currentState();
//            if (monitoringState != null) { // в противном случае мониторинг выключен
//                boolean hasSqlAccess = monitoringState.isSqlAccess();
//                handleMonitoringValue(apiAccessValue, apiAccess, true, "snmp.api.connection.restored"); //если доступа не было - восстановился
//                handleMonitoringValue(sqlAccessValue, sqlAccess, hasSqlAccess, hasSqlAccess ? "snmp.sql.connection.restored":"snmp.sql.no-access");
//                boolean hasLdapAccess = monitoringState.isLdapAccess();
//                handleMonitoringValue(ldapAccessValue, ldapAccess, hasLdapAccess, hasLdapAccess ? "snmp.ldap.connection.restored":"snmp.ldap.no-access");
//            }
        } catch (Exception ex) {
            try {
                handleMonitoringValue(apiAccessValue, apiAccess, false, "snmp.api.no-access");
            } catch(Exception handleEx) {
                //logger.severe(handleEx.toString());
            }
        } finally {
            checkApiInProgress = false;
        }
    }

    private void handleMonitoringValue(Integer32 value, String valueKey, boolean success, String logMessage) {
        supervisorState.setState(ObjectState.OK); //всегда ОК, так как отвечаем на запрос, только обновляем время
        boolean isApi = valueKey.equalsIgnoreCase(apiAccess);
        boolean isSql = valueKey.equalsIgnoreCase(sqlAccess);
        Function<SnmpEvent.Severity, SnmpEvent> eventCreator = isApi ?
                (s -> SnmpEvent.createApiEvent(trapId, alertId, s, success)) :
                (isSql ? (s -> SnmpEvent.createDatabaseEvent(trapId, alertId, s, success)) :
                        (s -> SnmpEvent.createLdapEvent(trapId, alertId, s, success)));

        if (success) {
            (isApi ? apiState : (isSql ? sqlState : ldapState)).setState(ObjectState.OK);
            if (value.getValue() == OFF) {
                //logger.info(logMessage);
                SnmpEvent.Severity severityValue = SnmpEvent.Severity.CLEARED;
                //logger.log(Level.INFO, "snmp.create-trap", new Object[] {valueKey, severityValue.toString()});
                sendTrapEvent(eventCreator.apply(severityValue));
            }
            value.setValue(ON);
        } else {
            //logger.severe(logMessage);
            (isApi ? apiState : (isSql ? sqlState : ldapState)).setState(ObjectState.CRITICAL_ERROR);
            if (value.getValue() != OFF) {
                SnmpEvent.Severity severityValue = SnmpEvent.Severity.valueOf(severity);
                //logger.log(Level.INFO, "snmp.create-trap", new Object[] {logMessage, severityValue.toString()});
                sendTrapEvent(eventCreator.apply(severityValue));
            }
            value.setValue(OFF);
        }
    }

    /**
     * Зафиксировать получение запроса от службы api
     */
    public void updateApiLastAccessTime() {
        lastApiAccessTime = new Date().getTime();
    }

    private void sendTrapEvent(SnmpEvent event) {
        try {
            agent.sendTrap(event);
            //logger.info("snmp.send-trap");
        } catch (IOException e) {
            //logger.log(Level.SEVERE, "snmp.send-trap.failed", e.toString());
        }
    }

    //@Override
    public void destroy() {
        try {
            agent.stop();
        } catch (Exception ignored) {}
    }
}
