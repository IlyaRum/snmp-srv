package com.ntc.arbiter.snmpservice.domain;

import static com.ntc.arbiter.snmpservice.agent.SnmpUtils.getResourceString;

/**
 * Состояние объекта наблюдения
 */
public enum ObjectState {
    UNKNOWN(1) {
        @Override
        public String toString() {
            return getResourceString("snmp.object-state.unknown");
        }
    },
    OK(2) {
        @Override
        public String toString() {
            return getResourceString("snmp.object-state.ok");
        }
    },
    MINOR_ERROR(3) {
        @Override
        public String toString() {
            return getResourceString("snmp.object-state.minor-error");
        }
    },
    MAJOR_ERROR(4) {
        @Override
        public String toString() {
            return getResourceString("snmp.object-state.major-error");
        }
    },
    CRITICAL_ERROR(5) {
        @Override
        public String toString() {
            return getResourceString("snmp.object-state.critical-error");
        }
    };

    private final int value;

    ObjectState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
