package com.ntc.arbiter.snmpservice.domain;

import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.AbstractVariable;
import org.snmp4j.smi.OID;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CurrentDate extends AbstractVariable {

    private final static SimpleDateFormat form = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    long currTime = 0;
    private byte[] valArr = new byte[0];

    public byte[] getValue() {
        if (new Date().getTime() - currTime > 500) {
            currTime = new Date().getTime();
            valArr = form.format(new Date()).getBytes();
        }
        return valArr;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int compareTo(Object o) {
        return -1;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public int getBERLength() {
        byte[] vl = this.getValue();
        return vl.length + BER.getBERLengthOfLength(vl.length) + 1;
    }

    @Override
    public void decodeBER(BERInputStream inputStream) throws IOException {
    }

    @Override
    public void encodeBER(OutputStream outputStream) throws IOException {
        BER.encodeString(outputStream, (byte) 4, this.getValue());
    }

    @Override
    public int getSyntax() {
        return 4;
    }

    @Override
    public String toString() {
        return form.format(new Date());
    }

    @Override
    public int toInt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long toLong() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object clone() {
        return new CurrentDate();
    }

    public final int length() {
        return this.getValue().length;
    }

    public final byte get(int index) {
        return this.getValue()[index];
    }

    @Override
    public OID toSubIndex(boolean impliedLength) {
        int offset = 0;
        int[] subIndex;
        if (!impliedLength) {
            subIndex = new int[this.length() + 1];
            subIndex[offset++] = this.length();
        } else {
            subIndex = new int[this.length()];
        }

        for (int i = 0; i < this.length(); ++i) {
            subIndex[offset + i] = this.get(i) & 255;
        }

        return new OID(subIndex);
    }

    @Override
    public void fromSubIndex(OID oid, boolean impliedLength) {
    }
}

