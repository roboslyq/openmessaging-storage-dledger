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

package io.openmessaging.storage.dledger;

import com.beust.jcommander.Parameter;
import io.openmessaging.storage.dledger.store.file.DLedgerMmapFileStore;
import java.io.File;

/**
 * 1、DLedger配置: 默认是为RocketMQ提供实现，所以很多配置项与RocketMQ保持一致。
 * 2、DLedger无缝的整合RocketMQ主要体现在以下三个方面：
 *  (1)启动时，对CommitLog进行相关的恢复
 *  (2)消息追加PutMessage时，
 *  (3)消息读取
 */
public class DLedgerConfig {

    public static final String MEMORY = "MEMORY";
    public static final String FILE = "FILE";
    /**
     * 分组:如果与RocketMQ整合，建议与 broker 配置属性 brokerName 保持一致。
     */
    @Parameter(names = {"--group", "-g"}, description = "Group of this server")
    private String group = "default";
    /**
     *  leader 节点的 id 名称，示例配置：n0，其配置要求第二个字符后必须是数字。
     */
    @Parameter(names = {"--id", "-i"}, description = "Self id of this server")
    private String selfId = "n0";
    /**
     * DLeger Group 中所有的节点信息，其配置示例 n0-127.0.0.1:40911;n1-127.0.0.1:40912;n2-127.0.0.1:40913。
     * 多个节点使用分号隔开。
     */
    @Parameter(names = {"--peers", "-p"}, description = "Peer info of this server")
    private String peers = "n0-localhost:20911";
    /**
     * 设置 DLedger 的日志文件的根目录，取自 borker 配件文件中的 storePathRootDir ，即 RocketMQ 的数据存储根路径。
     */
    @Parameter(names = {"--store-base-dir", "-s"}, description = "The base store dir of this server")
    private String storeBaseDir = File.separator + "tmp" + File.separator + "dledgerstore";


    @Parameter(names = {"--peer-push-throttle-point"}, description = "When the follower is behind the leader more than this value, it will trigger the throttle")
    private int peerPushThrottlePoint = 300 * 1024 * 1024;

    @Parameter(names = {"--peer-push-quotas"}, description = "The quotas of the pusher")
    private int peerPushQuota = 20 * 1024 * 1024;
    /**
     * DLedger 存储类型，固定为 基于文件的存储模式。
     */
    private String storeType = FILE; //FILE, MEMORY
    /**
     * 文件存储路径
     */
    private String dataStorePath;
    /**
     * 最大并发量
     */
    private int maxPendingRequestsNum = 10000;
    /**
     * 等待集群中其它节点ack最大毫秒数，超过当前值即超时
     */
    private int maxWaitAckTimeMs = 2500;

    private int maxPushTimeOutMs = 1000;
    /**
     * 是否开始自动选主
     */
    private boolean enableLeaderElector = true;
    /**
     * 心跳间隔
     */
    private int heartBeatTimeIntervalMs = 2000;
    /**
     * 心跳最大失败次数
     */
    private int maxHeartBeatLeak = 3;
    /**
     * 最小投票间隔
     */
    private int minVoteIntervalMs = 300;
    /**
     * 最大投票间隔
     */
    private int maxVoteIntervalMs = 1000;
    /**
     *  DLedger 日志文件保留时长，取自 broker 配置文件中的 fileReservedHours，默认为 72h。
     */
    private int fileReservedHours = 72;
    /**
     *  DLedger 日志文件的删除时间，取自 broker 配置文件中的 deleteWhen，默认为凌晨 4点。
     */
    private String deleteWhen = "04";

    private float diskSpaceRatioToCheckExpired = Float.parseFloat(System.getProperty("dledger.disk.ratio.check", "0.70"));
    private float diskSpaceRatioToForceClean = Float.parseFloat(System.getProperty("dledger.disk.ratio.clean", "0.85"));
    /**
     * 是否开启强制删除文件功能：默认为true
     */
    private boolean enableDiskForceClean = true;
    /**
     * 文件刷盘周期
     */
    private long flushFileInterval = 10;
    /**
     * TODO
     */
    private long checkPointInterval = 3000;
    /**
     * 设置 DLedger 的单个日志文件的大小，取自 broker 配置文件中的 - mapedFileSizeCommitLog，即与 commitlog 文件的单个文件大小一致。
     * 默认是1G
     */
    private int mappedFileSizeForEntryData = 1024 * 1024 * 1024;
    /**
     * 索引 文件大小
     */
    private int mappedFileSizeForEntryIndex = DLedgerMmapFileStore.INDEX_UNIT_SIZE * 5 * 1024 * 1024;
    /**
     * 是否将Entry推送到follwer(主从同步)
     */
    private boolean enablePushToFollower = true;

    @Parameter(names = {"--preferred-leader-id"}, description = "Preferred LeaderId")
    private String preferredLeaderId;
    private long maxLeadershipTransferWaitIndex = 1000;
    private int minTakeLeadershipVoteIntervalMs =  30;
    private int maxTakeLeadershipVoteIntervalMs =  100;

    private boolean isEnableBatchPush = false;
    private int maxBatchPushSize = 4 * 1024;


    public String getDefaultPath() {
        return storeBaseDir + File.separator + "dledger-" + selfId;
    }

    public String getDataStorePath() {
        if (dataStorePath == null) {
            return getDefaultPath() + File.separator + "data";
        }
        return dataStorePath;
    }

    public void setDataStorePath(String dataStorePath) {
        this.dataStorePath = dataStorePath;
    }

    public String getIndexStorePath() {
        return getDefaultPath() + File.separator + "index";
    }

    public int getMappedFileSizeForEntryData() {
        return mappedFileSizeForEntryData;
    }

    public void setMappedFileSizeForEntryData(int mappedFileSizeForEntryData) {
        this.mappedFileSizeForEntryData = mappedFileSizeForEntryData;
    }

    public int getMappedFileSizeForEntryIndex() {
        return mappedFileSizeForEntryIndex;
    }

    public void setMappedFileSizeForEntryIndex(int mappedFileSizeForEntryIndex) {
        this.mappedFileSizeForEntryIndex = mappedFileSizeForEntryIndex;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSelfId() {
        return selfId;
    }

    public void setSelfId(String selfId) {
        this.selfId = selfId;
    }

    public String getPeers() {
        return peers;
    }

    public void setPeers(String peers) {
        this.peers = peers;
    }

    public String getStoreBaseDir() {
        return storeBaseDir;
    }

    public void setStoreBaseDir(String storeBaseDir) {
        this.storeBaseDir = storeBaseDir;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public boolean isEnableLeaderElector() {
        return enableLeaderElector;
    }

    public void setEnableLeaderElector(boolean enableLeaderElector) {
        this.enableLeaderElector = enableLeaderElector;
    }

    //for builder semantic
    public DLedgerConfig group(String group) {
        this.group = group;
        return this;
    }

    public DLedgerConfig selfId(String selfId) {
        this.selfId = selfId;
        return this;
    }

    public DLedgerConfig peers(String peers) {
        this.peers = peers;
        return this;
    }

    public DLedgerConfig storeBaseDir(String dir) {
        this.storeBaseDir = dir;
        return this;
    }

    public boolean isEnablePushToFollower() {
        return enablePushToFollower;
    }

    public void setEnablePushToFollower(boolean enablePushToFollower) {
        this.enablePushToFollower = enablePushToFollower;
    }

    public int getMaxPendingRequestsNum() {
        return maxPendingRequestsNum;
    }

    public void setMaxPendingRequestsNum(int maxPendingRequestsNum) {
        this.maxPendingRequestsNum = maxPendingRequestsNum;
    }

    public int getMaxWaitAckTimeMs() {
        return maxWaitAckTimeMs;
    }

    public void setMaxWaitAckTimeMs(int maxWaitAckTimeMs) {
        this.maxWaitAckTimeMs = maxWaitAckTimeMs;
    }

    public int getMaxPushTimeOutMs() {
        return maxPushTimeOutMs;
    }

    public void setMaxPushTimeOutMs(int maxPushTimeOutMs) {
        this.maxPushTimeOutMs = maxPushTimeOutMs;
    }

    public int getHeartBeatTimeIntervalMs() {
        return heartBeatTimeIntervalMs;
    }

    public void setHeartBeatTimeIntervalMs(int heartBeatTimeIntervalMs) {
        this.heartBeatTimeIntervalMs = heartBeatTimeIntervalMs;
    }

    public int getMinVoteIntervalMs() {
        return minVoteIntervalMs;
    }

    public void setMinVoteIntervalMs(int minVoteIntervalMs) {
        this.minVoteIntervalMs = minVoteIntervalMs;
    }

    public int getMaxVoteIntervalMs() {
        return maxVoteIntervalMs;
    }

    public void setMaxVoteIntervalMs(int maxVoteIntervalMs) {
        this.maxVoteIntervalMs = maxVoteIntervalMs;
    }

    public String getDeleteWhen() {
        return deleteWhen;
    }

    public void setDeleteWhen(String deleteWhen) {
        this.deleteWhen = deleteWhen;
    }

    public float getDiskSpaceRatioToCheckExpired() {
        return diskSpaceRatioToCheckExpired;
    }

    public void setDiskSpaceRatioToCheckExpired(float diskSpaceRatioToCheckExpired) {
        this.diskSpaceRatioToCheckExpired = diskSpaceRatioToCheckExpired;
    }

    public float getDiskSpaceRatioToForceClean() {
        if (diskSpaceRatioToForceClean < 0.50f) {
            return 0.50f;
        } else {
            return diskSpaceRatioToForceClean;
        }
    }

    public void setDiskSpaceRatioToForceClean(float diskSpaceRatioToForceClean) {
        this.diskSpaceRatioToForceClean = diskSpaceRatioToForceClean;
    }

    public float getDiskFullRatio() {
        float ratio = diskSpaceRatioToForceClean + 0.05f;
        if (ratio > 0.95f) {
            return 0.95f;
        }
        return ratio;
    }

    public int getFileReservedHours() {
        return fileReservedHours;
    }

    public void setFileReservedHours(int fileReservedHours) {
        this.fileReservedHours = fileReservedHours;
    }

    public long getFlushFileInterval() {
        return flushFileInterval;
    }

    public void setFlushFileInterval(long flushFileInterval) {
        this.flushFileInterval = flushFileInterval;
    }

    public boolean isEnableDiskForceClean() {
        return enableDiskForceClean;
    }

    public void setEnableDiskForceClean(boolean enableDiskForceClean) {
        this.enableDiskForceClean = enableDiskForceClean;
    }

    public int getMaxHeartBeatLeak() {
        return maxHeartBeatLeak;
    }

    public void setMaxHeartBeatLeak(int maxHeartBeatLeak) {
        this.maxHeartBeatLeak = maxHeartBeatLeak;
    }

    public int getPeerPushThrottlePoint() {
        return peerPushThrottlePoint;
    }

    public void setPeerPushThrottlePoint(int peerPushThrottlePoint) {
        this.peerPushThrottlePoint = peerPushThrottlePoint;
    }

    public int getPeerPushQuota() {
        return peerPushQuota;
    }

    public void setPeerPushQuota(int peerPushQuota) {
        this.peerPushQuota = peerPushQuota;
    }

    public long getCheckPointInterval() {
        return checkPointInterval;
    }

    public void setCheckPointInterval(long checkPointInterval) {
        this.checkPointInterval = checkPointInterval;
    }

    public String getPreferredLeaderId() {
        return preferredLeaderId;
    }

    public void setPreferredLeaderId(String preferredLeaderId) {
        this.preferredLeaderId = preferredLeaderId;
    }

    public long getMaxLeadershipTransferWaitIndex() {
        return maxLeadershipTransferWaitIndex;
    }

    public void setMaxLeadershipTransferWaitIndex(long maxLeadershipTransferWaitIndex) {
        this.maxLeadershipTransferWaitIndex = maxLeadershipTransferWaitIndex;
    }

    public int getMinTakeLeadershipVoteIntervalMs() {
        return minTakeLeadershipVoteIntervalMs;
    }

    public void setMinTakeLeadershipVoteIntervalMs(int minTakeLeadershipVoteIntervalMs) {
        this.minTakeLeadershipVoteIntervalMs = minTakeLeadershipVoteIntervalMs;
    }

    public int getMaxTakeLeadershipVoteIntervalMs() {
        return maxTakeLeadershipVoteIntervalMs;
    }

    public void setMaxTakeLeadershipVoteIntervalMs(int maxTakeLeadershipVoteIntervalMs) {
        this.maxTakeLeadershipVoteIntervalMs = maxTakeLeadershipVoteIntervalMs;
    }

    public boolean isEnableBatchPush() {
        return isEnableBatchPush;
    }

    public void setEnableBatchPush(boolean enableBatchPush) {
        isEnableBatchPush = enableBatchPush;
    }

    public int getMaxBatchPushSize() {
        return maxBatchPushSize;
    }

    public void setMaxBatchPushSize(int maxBatchPushSize) {
        this.maxBatchPushSize = maxBatchPushSize;
    }
}
