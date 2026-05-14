package com.ntc.arbiter.snmpservice.agent;

import org.snmp4j.agent.mo.ext.StaticMOGroup;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import java.util.HashMap;
import java.util.Map;

public class StaticMOGroupExt extends StaticMOGroup {
    Map<OID, Variable> mapVar = new HashMap<>();

    public StaticMOGroupExt(OID root, VariableBinding[] vbs) {
        super(root, vbs);
        for (VariableBinding vb : vbs) {
            mapVar.put(vb.getOid(), vb.getVariable());
        }
    }

    public Variable get(OID oid) {
        return mapVar.get(oid);
    }
}
