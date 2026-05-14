package com.ntc.arbiter.snmpservice.domain;

import static com.ntc.arbiter.snmpservice.agent.SnmpUtils.getResourceString;

/**
 * Тип объекта, с которым произошла проблема
 */
public enum TargetType {
    SUPERVISOR {
        @Override
        public String toString() {
            return getResourceString("snmp.target-type.supervisor");
        }
    },
    DATABASE {
        @Override
        public String toString() {
            return getResourceString("snmp.target-type.database");
        }
    },
    API {
        @Override
        public String toString() {
            return getResourceString("snmp.target-type.api");
        }
    },
    ALARM { //todo для сообщений мониторинга
        @Override
        public String toString() {
            return getResourceString("snmp.trap-message.target-type.alarm");
        }
    },
    LDAP {
        @Override
        public String toString() {
            return getResourceString("snmp.target-type.ldap");
        }
    }
}
