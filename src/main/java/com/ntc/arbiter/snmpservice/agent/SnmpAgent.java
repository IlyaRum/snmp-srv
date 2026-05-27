package com.ntc.arbiter.snmpservice.agent;

import com.ntc.arbiter.snmpservice.domain.SnmpEvent;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import org.snmp4j.*;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.*;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Базовый snmp агент
 */
public class SnmpAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(SnmpAgent.class);

    private static final String UDP_PROTOCOL = "udp";

    private String address;
    private String community = "public";
    private String trapAddress;
    private String trapCommunity = "public";
    private List<UsmUser> users = new ArrayList<>();

    private long agentStartTime = System.currentTimeMillis();

    public static OctetString getStr(String value) {
        return new OctetString(value);
    }

    public SnmpAgent(String address, String trapAddress) throws IOException {
        // These files does not exist and are not used but has to be specified Read snmp4j docs for more info
        super(new File("conf.agent"), null, new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
        parseAddress(address);
        parseTrapAddress(trapAddress);
    }

    private void parseAddress(String address) {
        String[] addr = address.split("\\|");
        this.address = addr[0];
        this.community = addr.length == 2 ? addr[1] : this.community;
    }

    private void parseTrapAddress(String address) {
        String[] addr = address.split("\\|");
        this.trapAddress = addr[0];
        this.trapCommunity = addr.length == 2 ? addr[1] : this.trapCommunity;
    }

    public void addUserSecurity(String login, String password, String privacy) {
        UsmUser user = new UsmUser(getStr(login), AuthMD5.ID, getStr(password), PrivDES.ID, getStr(privacy));
        users.add(user);
    }

    @Override
    protected void registerManagedObjects() {
    }

    /**
     * Регистрации управляемых объектов в которых хранятся переменные
     */
    public void registerManagedObject(ManagedObject mo) {
        try {
            server.register(mo, null);
        } catch (DuplicateRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void unregisterManagedObject(MOGroup moGroup) {
        moGroup.unregisterMOs(server, getContext(moGroup));
    }

    @Override
    protected void addNotificationTargets(SnmpTargetMIB targetMIB, SnmpNotificationMIB notificationMIB) {
    }

    @Override
    protected void addViews(VacmMIB vacm) {
        //группа v2
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, getStr("cpublic"), getStr("v1v2group"), StorageType.nonVolatile);
        //todo реализовать аутентификацию v2
        /*for (SNMPV2Authentication snmpv2Authentication : communitySecurityList) {
            vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(snmpv2Authentication.getSecurityName()),
                    new OctetString("v1v2group"), StorageType.nonVolatile);
        }*/
        //группа v3
        for (UsmUser user : users) {//пользователи в конфиге
            vacm.addGroup(SecurityModel.SECURITY_MODEL_USM, user.getSecurityName(), getStr("v3group"), StorageType.nonVolatile);
        }

        //доступ v2
        vacm.addAccess(getStr("v1v2group"), getStr(""), SecurityModel.SECURITY_MODEL_SNMPv2c, SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT,
                getStr("fullReadView"), getStr("fullWriteView"), getStr("fullNotifyView"), StorageType.nonVolatile);

        vacm.addAccess(getStr("v3group"), getStr(""), SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_PRIV, MutableVACM.VACM_MATCH_EXACT,
                getStr("fullReadView"), getStr("fullWriteView"), getStr("fullNotifyView"), StorageType.nonVolatile);

        vacm.addViewTreeFamily(getStr("fullReadView"), new OID("1.3"), getStr(""), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacm.addViewTreeFamily(getStr("fullWriteView"), new OID("1.3"), getStr(""), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacm.addViewTreeFamily(getStr("fullNotifyView"),new OID("1.3"), getStr(""),VacmMIB.vacmViewIncluded,StorageType.nonVolatile);
    }

    /**
     * Добавление пользователей для версии SNMP v.3
     */
    protected void addUsmUser(USM usm) {
        for (UsmUser user : users) {
            usm.addUser(user.getSecurityName(), user);
        }
    }

    /**
     * Регистрация групп в сообществах для доступа
     * Поумолчанию только public для SNMP v2c
     */
    protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[]{
                getStr(this.community), // community name
                getStr("cpublic"), // security name
                getAgent().getContextEngineID(), // local engine ID
                getStr(""), // default context name
                getStr(""), // transport tag
                new Integer32(StorageType.nonVolatile), // storage type
                new Integer32(RowStatus.active) // row status
        };
        MOTableRow row = communityMIB.getSnmpCommunityEntry().createRow(getStr("public2public").toSubIndex(true), com2sec);
        communityMIB.getSnmpCommunityEntry().addRow(row);
    }

    @Override
    protected void initMessageDispatcher() {
        dispatcher = new MessageDispatcherImpl();
        usm = new USM(SecurityProtocols.getInstance(), agent.getContextEngineID(), updateEngineBoots());
        mpv3 = new MPv3(usm);
        SecurityModels.getInstance().addSecurityModel(this.usm);
        SecurityProtocols.getInstance().addDefaultProtocols();
        //dispatcher.addMessageProcessingModel(new MPv1());
        dispatcher.addMessageProcessingModel(new MPv2c());
        dispatcher.addMessageProcessingModel(mpv3);
        initSnmpSession();
    }

    protected void initTransportMappings() throws IOException {
        transportMappings = new TransportMapping[1];
        Address addr = GenericAddress.parse(address);
        TransportMapping tm = TransportMappings.getInstance().createTransportMapping(addr);
        transportMappings[0] = tm;
    }

    /**
     * Метод инициализации, нужен для запуска агента
     * @throws IOException
     */
    public void start() throws IOException {
        init();
        addShutdownHook();
        getServer().addContext(getStr("public"));
        finishInit();
        run();
        sendColdStartNotification();
    }

    protected void unregisterManagedObjects() {
    }

    /**
     * @param snmpEvent параметры события
     * @throws IOException
     */
    public void sendTrap(SnmpEvent snmpEvent) throws IOException {
        PDU pdu = snmpEvent.createTrapMessage(System.currentTimeMillis() - agentStartTime);
        //todo ScopedPDU trapV3 = new ScopedPDU();

        CommunityTarget target = new CommunityTarget();
        target.setVersion(SnmpConstants.version2c);
        target.setCommunity(getStr(trapCommunity));
        target.setAddress(GenericAddress.parse(trapAddress));

        Snmp snmp = new Snmp(trapAddress.toLowerCase().startsWith(UDP_PROTOCOL) ? new DefaultUdpTransportMapping() : new DefaultTcpTransportMapping());
        try {
            snmp.send(pdu, target);
        } catch (Exception ex) {
            logger.error(ex.toString());
        } finally {
            try {
                snmp.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
