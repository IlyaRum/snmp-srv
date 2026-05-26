package com.ntc.arbiter.snmpservice.constants;

import static com.ntc.arbiter.snmpservice.agent.SnmpUtils.getResourceString;

public class Constants {

  public static final String BUNDLE_NAME = "snmp.messages";

  public static final String API_TARGET_NAME = getResourceString("snmp.target-name.api");
  public static final String API_NO_ACCESS_VALUE = getResourceString("snmp.trap-message.key-value.api-no-access");
  public static final String API_CONNECTION_RESTORED_VALUE = getResourceString("snmp.trap-message.key-value.api.connection.restored");
  public static final String API_NO_ACCESS_MESSAGE = getResourceString("snmp.trap-message.alert-message.api-no-access");
  public static final String API_CONNECTION_RESTORED_MESSAGE = getResourceString("snmp.trap-message.alert-message.api.connection.restored");

  public static final String CALC_TARGET_NAME = getResourceString("snmp.target-name.calc");
  public static final String CALC_NO_ACCESS_VALUE = getResourceString("snmp.trap-message.key-value.calc-no-access");
  public static final String CALC_CONNECTION_RESTORED_VALUE = getResourceString("snmp.trap-message.key-value.calc.connection.restored");
  public static final String CALC_NO_ACCESS_MESSAGE = getResourceString("snmp.trap-message.alert-message.calc-no-access");
  public static final String CALC_CONNECTION_RESTORED_MESSAGE = getResourceString("snmp.trap-message.alert-message.calc.connection.restored");

  public static final String SEND_TRAP = getResourceString("snmp.send-trap");
  public static final String SEND_TRAP_FAILED = getResourceString("snmp.send-trap.failed");
  public static final String CREATE_AGENT_FAILURE = getResourceString("snmp.create-agent.failure");

}
