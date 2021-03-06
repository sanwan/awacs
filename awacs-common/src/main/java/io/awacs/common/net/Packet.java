/**
 * Copyright 2016-2017 AWACS Project.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awacs.common.net;

import io.awacs.common.IllegalPacketException;

import java.util.Arrays;

/**
 * Byte /      0        |       1       |       2       |       3       |
 * /               |               |               |               |
 * +---------------+---------------+---------------+---------------+
 * 0|             Magic             |    Version    |      Key      |
 * +---------------+---------------+---------------+---------------+
 * 4|                            SEQ-ID                             |
 * +---------------+---------------+---------------+---------------+
 * 8|    compress   |               |        NAMESPACE_LEN          |
 * +---------------+---------------+---------------+---------------+
 * 12|                            BODY_LEN                           |
 * +---------------+---------------+---------------+---------------+
 * Total 16 bytes
 * Created by pixyonly on 02/09/2017.
 */
public class Packet {

    public static final int MAX_PACKET_SIZE = 1 << 20;

    public static final byte MAGIC_H = 0x5c;

    public static final byte MAGIC_L = 0x4e;

    public static final byte VERSION = 0x02;

    private String namespace;

    private byte[] body;

    private byte key;

    private long reqId;

    public Packet(String namespace, byte key, byte[] body) {
        this.namespace = namespace;
        this.key = key;
        this.body = body;
    }

    public String getNamespace() {
        return namespace;
    }

    public byte[] getBody() {
        return body;
    }

    public int size() {
        return body.length + namespace.getBytes().length + 16;
    }

    public byte key() {
        return this.key;
    }

    public byte[] serialize() {
        byte[] nb = namespace.getBytes();
        int length = 16 + body.length + nb.length;
        byte[] payload = new byte[length];

        payload[0] = 0x5c;
        payload[1] = 0x4e;
        payload[2] = VERSION;
        payload[3] = key;

        //4,5,6,7,8,9 ignore

        //namespace len
        payload[10] = (byte) ((nb.length & 0x0000ff00) >> 8);
        payload[11] = (byte) (nb.length & 0x000000ff);

        //body len
        payload[12] = (byte) (body.length >> 24);
        payload[13] = (byte) ((body.length & 0x00ff0000) >> 16);
        payload[14] = (byte) (body.length >> 8);
        payload[15] = (byte) (body.length & 0x000000ff);

        System.arraycopy(nb, 0, payload, 16, nb.length);
        System.arraycopy(body, 0, payload, 16 + nb.length, body.length);
        return payload;
    }

    public static Packet parse(byte[] header, byte[] next) throws IllegalPacketException {
        if (header[0] != MAGIC_H || header[1] != MAGIC_L || header[2] != VERSION) {
            throw new IllegalPacketException();
        }
        //key
        byte k = header[3];
        //compression ignore
        byte compression = header[8];
        int namespaceLen = (Byte.toUnsignedInt(header[10]) << 8) |
                (Byte.toUnsignedInt(header[11]));
        int bodyLen = (Byte.toUnsignedInt(header[12]) << 24) |
                (Byte.toUnsignedInt(header[13]) << 16) |
                (Byte.toUnsignedInt(header[14]) << 8) |
                Byte.toUnsignedInt(header[15]);
        String namespace = new String(next, 0, namespaceLen);
        byte[] body = Arrays.copyOfRange(next, namespaceLen, next.length);
        return new Packet(namespace, k, body);
    }

    public static int namespaceLength(byte[] payload) {
        return (Byte.toUnsignedInt(payload[10]) << 8) | (Byte.toUnsignedInt(payload[11]));
    }

    public static int bodyLength(byte[] payload) {
        return (Byte.toUnsignedInt(payload[12]) << 24) | (Byte.toUnsignedInt(payload[13]) << 16) |
                (Byte.toUnsignedInt(payload[14]) << 8) | Byte.toUnsignedInt(payload[15]);
    }

    @Override
    public String toString() {
        return "Packet{namespace=" + namespace +
                ", key=" + key +
                ", body=" + body +
                '}';
    }
}
