package com.ntc.arbiter.snmpservice.agent;

import com.ntc.arbiter.snmpservice.constants.Constants;
import org.snmp4j.smi.OctetString;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

/**
 * Утилитная функциональность
 */
public class SnmpUtils {


    private static final String UNDEFINED_HOST_NAME = "snmp.host-name.undefined";

    /**
     * Получить по ключу сообщение из ресурсов
     * @param key ключ сообщения
     * @return сообщение из ресурсов
     */
    public static String getResourceString(String key){
        return ResourceBundle.getBundle(Constants.BUNDLE_NAME).getString(key);
    }

    /**
     * Вернуть текущий адрес хоста
     * @return адрес хоста
     */
    public static String currentHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return  getResourceString(UNDEFINED_HOST_NAME);
        }
    }

    /**
     * Сконвертировать обычную строку в строку SNMP
     * @param string строка
     * @return строка SNMP
     */
    public static OctetString convertToOctetString(String string){
        return new OctetString(string != null ? string.getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }
}
