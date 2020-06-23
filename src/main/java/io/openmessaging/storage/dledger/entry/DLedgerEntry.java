/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openmessaging.storage.dledger.entry;

/**
 * DLedger的实体(消息)格式定义
 * header:
 *   4: magic 魔数
 *   4: entrysize 大小
 *   8: entry index
 *   8: entry term
 *   8: pos
 *   4: channel
 *   4: chain crc
 *   4: body crc
 *   4: body size
 * body: 具体的body，大小为body size
 */
public class DLedgerEntry {
    /**
     * POS的偏移量，前面4个字段分别为magic,entrysize,entryindex,entryterm
     */
    public final static int POS_OFFSET = 4 + 4 + 8 + 8;
    /**
     * header大小
     */
    public final static int HEADER_SIZE = POS_OFFSET + 8 + 4 + 4 + 4;
    /**
     * Body的大小偏移量
     */
    public final static int BODY_OFFSET = HEADER_SIZE + 4;
    private int magic;
    private int size;
    private long index;
    private long term;
    private long pos;       //used to validate data
    private int channel;    //reserved
    private int chainCrc;   //like the block chain, this crc indicates any modification before this entry.
    private int bodyCrc;    //the crc of the body
    private byte[] body;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getMagic() {
        return magic;
    }

    public void setMagic(int magic) {
        this.magic = magic;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public int getChainCrc() {
        return chainCrc;
    }

    public void setChainCrc(int chainCrc) {
        this.chainCrc = chainCrc;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int getBodyCrc() {
        return bodyCrc;
    }

    public void setBodyCrc(int bodyCrc) {
        this.bodyCrc = bodyCrc;
    }

    public int computSizeInBytes() {
        size = HEADER_SIZE + 4 + body.length;
        return size;
    }

    public long getPos() {
        return pos;
    }

    public void setPos(long pos) {
        this.pos = pos;
    }

    @Override
    public boolean equals(Object entry) {
        if (entry == null || !(entry instanceof DLedgerEntry)) {
            return false;
        }
        DLedgerEntry other = (DLedgerEntry) entry;
        if (this.size != other.size
            || this.magic != other.magic
            || this.index != other.index
            || this.term != other.term
            || this.channel != other.channel
            || this.pos != other.pos) {
            return false;
        }
        if (body == null) {
            return other.body == null;
        }

        if (other.body == null) {
            return false;
        }
        if (body.length != other.body.length) {
            return false;
        }
        for (int i = 0; i < body.length; i++) {
            if (body[i] != other.body[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int h = 1;
        h = prime * h + size;
        h = prime * h + magic;
        h = prime * h + (int) index;
        h = prime * h + (int) term;
        h = prime * h + channel;
        h = prime * h + (int) pos;
        if (body != null) {
            for (int i = 0; i < body.length; i++) {
                h = prime * h + body[i];
            }
        } else {
            h = prime * h;
        }
        return h;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }
}
