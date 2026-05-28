package com.ntc.arbiter.snmpservice.domain;

import static com.ntc.arbiter.snmpservice.agent.SnmpUtils.getResourceString;

/**
 * Тип объекта, с которым произошла проблема
 */
public enum TargetType {
    API {
        @Override
        public String toString() {
            return getResourceString("snmp.target-type.api");
        }
    },
    CALC {
      @Override
      public String toString() {
        return getResourceString("snmp.target-type.calc");
      }
    },
    UI {
      @Override
      public String toString() {
        return getResourceString("snmp.target-type.ui");
      }
    },
    ALARM { //todo для сообщений мониторинга
        @Override
        public String toString() {
            return getResourceString("snmp.trap-message.target-type.alarm");
        }
    }
}
