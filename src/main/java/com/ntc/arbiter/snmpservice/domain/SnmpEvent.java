package com.ntc.arbiter.snmpservice.domain;

import com.ntc.arbiter.snmpservice.service.ContextService;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;
import com.ntc.arbiter.snmpservice.agent.SnmpUtils;

import java.util.Date;

import static com.ntc.arbiter.snmpservice.agent.SnmpUtils.convertToOctetString;
import static com.ntc.arbiter.snmpservice.agent.SnmpUtils.getResourceString;

/**
 * Событие уведомления Trap-сообщений
 */
public class SnmpEvent {

    private static final String ACCESS_RULE = getResourceString("snmp.trap-message.rule-name.access-rule");

    private static final String TARGET_NAME = "1.0";
    private static final String TARGET_TYPE = "2.0";
    private static final String HOST_NAME = "3.0";
    private static final String METRIC_NAME = "4.0";
    private static final String KEY_NAME = "5.0";
    private static final String KEY_VALUE = "6.0";
    private static final String TIMESTAMP = "7.0";
    private static final String SEVERITY = "8.0";
    private static final String MESSAGE = "9.0";
    private static final String RULE_NAME = "10.0";
    private static final String RULE_OWNER = "11.0";
    private static final String METRIC_VALUE = "12.0";
    private static final String CONTEXT = "13.0";

    /**
     * Строка идентификатора trap-сообщения
     */
    private String trapId;

    /**
     * Строка идентификатора alert-атрибута
     */
    private String alertId;

    /**
     * Имя объекта мониторинга, с которым произошла проблема
     */
    private String targetName;

    /**
     *  Тип объекта, с которым произошла проблема
     */
    private TargetType targetType;

    /**
     *  Хост, на котором зафиксирована проблема
     */
    private String hostName;

    /**
     * Группа метрик мониторинга
     */
    private MetricName metricName;

    /**
     * Имя метрики в группе
     */
    private KeyName keyName;

    /**
     * Значение метрики
     */
    private String keyValue;

    /**
     * Дата/время локальное
     */
    private String timestamp;

    /**
     * Критичность
     */
    private Severity severity;

    /**
     * Краткое аварийное сообщение
     */
    private String message;

    /**
     * Имя правила обработки аварии
     */
    private String ruleName;

    /**
     * Имя для того, кто создал правило
     */
    private String ruleOwner;

    /**
     * Значение для дополнительного поля технических метрик
     */
    private String metricValue;

    /**
     * Значение для дополнительной текстовой информации
     */
    private String context;

    private SnmpEvent(String trapId, String alertId) {
        this.trapId = trapId;
        this.alertId = alertId;
    }

    public static SnmpEvent createTrapEvent(String trapId, String alertId, Severity severity, boolean success, ContextService context){
      SnmpEvent snmpEvent = new SnmpEvent(trapId, alertId);
      snmpEvent.targetName = context.getServiceName();
      snmpEvent.targetType = TargetType.API;
      snmpEvent.metricName = MetricName.API_CONTROL;
      snmpEvent.keyName = KeyName.API_WORK;
      snmpEvent.keyValue = success ? context.getConnectionRestoredValue() : context.getNoAccessValue();
      snmpEvent.severity = severity;
      snmpEvent.message = success ? context.getConnectionRestoredMessage() : context.getNoAccessMessage();
      snmpEvent.ruleName = ACCESS_RULE;
      fillCommonInfo(snmpEvent);
      return snmpEvent;
    }

    private static void fillCommonInfo(SnmpEvent snmpEvent) {
        snmpEvent.hostName = SnmpUtils.currentHostName();
        snmpEvent.timestamp = (new Date()).toString();
        snmpEvent.ruleName = ACCESS_RULE;
        snmpEvent.ruleOwner = RuleOwner.ADMIN.toString();
        snmpEvent.metricValue = "";
        snmpEvent.context = "";
    }

    private OID getAlertOID(String id) {
        return new OID(alertId + '.' + id);
    }

    public PDU createTrapMessage(long uptimeMs) {
        PDU trap = new PDU();
        trap.setType(PDU.TRAP);

        trap.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(trapId)));
        trap.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(uptimeMs / 10)));
        trap.add(new VariableBinding(SnmpConstants.sysDescr, convertToOctetString(targetName)));
        trap.add(new VariableBinding(getAlertOID(TARGET_NAME), convertToOctetString(targetName)));
        trap.add(new VariableBinding(getAlertOID(TARGET_TYPE), convertToOctetString(targetType.toString())));
        trap.add(new VariableBinding(getAlertOID(HOST_NAME), convertToOctetString(hostName)));
        trap.add(new VariableBinding(getAlertOID(METRIC_NAME), convertToOctetString(metricName.toString())));
        trap.add(new VariableBinding(getAlertOID(KEY_NAME), convertToOctetString(keyName.toString())));
        trap.add(new VariableBinding(getAlertOID(KEY_VALUE), convertToOctetString(keyValue)));
        trap.add(new VariableBinding(getAlertOID(TIMESTAMP), convertToOctetString(timestamp)));
        trap.add(new VariableBinding(getAlertOID(SEVERITY), new Integer32(severity.getValue())));
        trap.add(new VariableBinding(getAlertOID(MESSAGE), convertToOctetString(message)));
        trap.add(new VariableBinding(getAlertOID(RULE_NAME), convertToOctetString(ruleName)));
        trap.add(new VariableBinding(getAlertOID(RULE_OWNER), convertToOctetString(ruleOwner)));
        trap.add(new VariableBinding(getAlertOID(METRIC_VALUE), convertToOctetString(metricValue)));
        trap.add(new VariableBinding(getAlertOID(CONTEXT), convertToOctetString(context)));

        return trap;
    }

    /**
     * Группа метрик мониторинга
     */
    public enum MetricName {
        API_CONTROL {
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.metric-name.api-control");
            }
        },
        ALARM_CONTROL { //todo для сообщений мониторинга
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.metric-name.alarm-control");
            }
        }
    }

    /**
     * Имя метрики в группе
     */
    public enum KeyName {
        API_WORK {
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.key-name.api-work");
            }
        },
        ALARM_OCCUR { //todo для сообщений мониторинга
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.key-name.alarm-occur");
            }
        }
    }

    /**
     * Критичность
     */
    public enum Severity {
        INFORMATION(1) {
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.severity.information");
            }
        },
        CLEARED(2) {
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.severity.cleared");
            }
        },
        MINOR(3) {
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.severity.minor");
            }
        },
        MAJOR(4) {
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.severity.major");
            }
        },
        CRITICAL(5) {
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.severity.critical");
            }
        };

        private final int value;

        Severity(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Кто создал правило
     */
    public enum RuleOwner{
        ADMIN {
            @Override
            public String toString() {
                return getResourceString("snmp.trap-message.rule-owner.admin");
            }
        }
    }
}
