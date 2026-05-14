package com.ntc.arbiter.snmpservice.domain;

import org.snmp4j.agent.mo.DefaultMOMutableRow2PC;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import com.ntc.arbiter.snmpservice.agent.SnmpUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ntc.arbiter.snmpservice.agent.SnmpUtils.convertToOctetString;
import static com.ntc.arbiter.snmpservice.agent.SnmpUtils.getResourceString;

/**
 * Объект состояния для таблицы
 */
public class SnmpState {

    private static final String TARGET_NAME_PREFIX = "snmp.target-name.";

    /**
     * Идентификатор состояния
     */
    private String id;

    /**
     * Имя объекта
     */
    private String targetName;

    /**
     *  Тип объекта
     */
    private TargetType targetType;

    /**
     *  Хост
     */
    private String hostName;

    /**
     * Дата/время локальное
     */
    private Instant dateTime;

    /**
     * Состояние объекта
     */
    private ObjectState objectState;

    /**
     * Краткое аварийное сообщение
     */
    private String message;

    /**
     * Значение для дополнительной текстовой информации
     */
    private String context;

    private Variable[] variables = new Variable[7];

    private MOTableRow row;

    public SnmpState(String id, TargetType targetType, ObjectState objectState) {
        this.id = id;
        this.targetType = targetType;
        this.objectState = objectState;
    }

    public synchronized MOTableRow row() {
        if (row != null) {
            return row;
        }
        variables[0] = convertToOctetString(getResourceString(TARGET_NAME_PREFIX + targetType.name().toLowerCase()));
        variables[1] = convertToOctetString(targetType.toString());
        variables[2] = convertToOctetString(SnmpUtils.currentHostName());
        setState(objectState);
        variables[6] = convertToOctetString(null);
        row = new DefaultMOMutableRow2PC(new OID(id), variables);
        return row;
    }

    public synchronized void setState(ObjectState state) {
        variables[3] = convertToOctetString((new Date()).toString());
        variables[4] = new Integer32(state.getValue());
        variables[5] = convertToOctetString(state.toString());
    }

    public static MOColumn[] columns() {
        List<MOColumn> columns = new ArrayList<>();
        columns.add(new MOColumn(columns.size() + 1, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY)); //0 objectTargetName
        columns.add(new MOColumn(columns.size() + 1, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY)); //1 objectTargetType
        columns.add(new MOColumn(columns.size() + 1, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY)); //2 objectHostName
        columns.add(new MOColumn(columns.size() + 1, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY)); //3 objectDateAndTime
        columns.add(new MOColumn(columns.size() + 1, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY));    //4 objectState
        columns.add(new MOColumn(columns.size() + 1, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY)); //5 objectMessage
        columns.add(new MOColumn(columns.size() + 1, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY)); //6 objectContext
        return columns.toArray(new MOColumn[0]);
    }
}
