# DLedger详解

> 是什么？为了解决MQ中的高可用（主从模式，主从复制，自动选主）。实现了Raft协议的Java library实现。主要是通过CommitLog来实现。封装了对日志的基本操作。



> 解决什么问题？
>
> 1、 多副本存储实现（CommitLog）
>
> 2、Leader 选主
>
> 3、日志追加流程
>
> 4、日志复制(传播)

# 常见类

- DLedgerServer 
  - 服务类，一个服务节点的抽象
- DLedgerStore
  - 存储类
- DLedgerRpcService
  - RPC类
  - 具体实现有
    - DLedgerClientRpcNettyService

- DLedgerEntryPusher
  - 主从复制实现
- DLedgerStore
  - 存储实现
- MemberState
  - 状态机
- EntryHandler
  - 从节点使用，当接收主从复制的信息
- QuorumAckChecker
  - 主节点使用，日志复制投票器，一个日志写请求只有得到集群内的的大多数节点的响应，日志才会被提交
- EntryDispatcher 
  - 主从复制：主节点日志请求转发器，向从节点复制消息

## 常见类

- DLedgerServer
  - DLedger节点服务（每一个节点即一个服务 ，有leader,follower和candicate三种角色）

  如何启动一个DLedgerServer？

  ```java
  protected synchronized DLedgerServer launchServer(String group, String peers, String selfId, String leaderId, String storeType) {
      // 配置DLedgerConfig
          DLedgerConfig config = new DLedgerConfig();
      // 设置group名称，Id,及集群中其它节点
          config.group(group).selfId(selfId).peers(peers);
      // 设置工作路径
          config.setStoreBaseDir(FileTestUtil.TEST_BASE + File.separator + group);
      // 设置存储类型
          config.setStoreType(storeType);
      // 设置MappedFile单个文件大小，默认1G
          config.setMappedFileSizeForEntryData(10 * 1024 * 1024);
      // 设置是否开启自动选主
          config.setEnableLeaderElector(false);
      // 设置是否删除文件
          config.setEnableDiskForceClean(false);
      // 设置磁盘使用超过率（超过之后开始删除文件）
          config.setDiskSpaceRatioToForceClean(0.90f);
      // 根据配置创建DLedgerServer
          DLedgerServer dLedgerServer = new DLedgerServer(config);
      // 获取集群中所有的成员状态
          MemberState memberState = dLedgerServer.getMemberState();
          memberState.setCurrTermForTest(0);
      // 关闭了自动选主，所以如果自己的selfId等于leaderId，则选自己为Leader
          if (selfId.equals(leaderId)) {
              memberState.changeToLeader(0);
          } else {
              memberState.changeToFollower(0, leaderId);
          }
  	// 启动DLedgerServer
          dLedgerServer.startup();
          return dLedgerServer;
      }
  ```

- DLedgerConfig
  - 配置文件
- DLedgerEntryPusher
  - 主从同步
- DLedgerLeaderElector
  - 选主
- DLedgerRpcService
  - DLedger的节点（服务）之间的RPC通讯，底层使用Netty ,默认实现是DLedgerRpcNettyService
- DLedgerRpcNettyService
  - 实现了DLedgerRpcService
- MemberState
  - 每个节点的状态成员状态机
- DLedgerEntry
  - DLedger消息格式封装
- DLedgerEntryCoder
  - 消息编码器



# 存储

- DLedgerStore
  - DLedgerMmapFileStore
    - 文件存储实现
  - DLedgerMemoryStore
    - 内存存储实现