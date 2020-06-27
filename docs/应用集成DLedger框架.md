# 应用集成DLedger框架

1、关于`DLedger`相关知识，请点击[dledger](https://github.com/openmessaging/openmessaging-storage-dledger)

2、此次集成，以spring boot项目为例

# 项目结构

![image-20200627194048649](images/应用集成DLedger框架/image-20200627194048649.png)

1、DemoApplication

​	Spring boot项目的启动类

2、DLedgerConfigForSpring

​	DLedger相关配置类

3、application-nx.properies

​	针对不同节点的配置类，要模块高可用，我们定义了三个节点的配置

# 引入依赖

```xml
 <dependency>
     <groupId>io.openmessaging.storage</groupId>
     <artifactId>dledger</artifactId>
     <version>0.1</version>
</dependency>
```

# 定义配置

- application-n0.properties

  ```properties
  server.port = 8080
  dledger.peers = n0-localhost:30001;n1-localhost:30002;n2-localhost:30002
  dledger.group = test_group
  dledger.selfid = n0
  ```

- application-n1.properties

  ```properties
  server.port = 8081
  dledger.peers = n0-localhost:30001;n1-localhost:30002;n2-localhost:30002
  dledger.group = test_group
  dledger.selfid = n1
  ```

- application-n2.properties

  ```properties
  server.port = 8082
  dledger.peers = n0-localhost:30001;n1-localhost:30002;n2-localhost:30002
  dledger.group = test_group
  dledger.selfid = n2
  ```

# 配置类定义

`DLedgerConfigForSpring`

```java
package com.example.demo.config;

import io.openmessaging.storage.dledger.DLedgerConfig;
import io.openmessaging.storage.dledger.DLedgerServer;
import io.openmessaging.storage.dledger.client.DLedgerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * @author roboslyq
 * @Desc
 * @create 2020-06-26 23:18
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "dledger")
public class DLedgerConfigForSpring {
    public static final String TEST_BASE = File.separator + "tmp" + File.separator + "dledgerteststore";

    String peers;

    String group;

    String selfId;

    @Bean
    public DLedgerServer initDLedgerServer(){
        DLedgerConfig config = new DLedgerConfig();
        config.setStoreBaseDir(TEST_BASE + File.separator + group);
        config.group(group).selfId(selfId).peers(peers);
        config.setStoreType(DLedgerConfig.MEMORY);
        DLedgerServer dLedgerServer = new DLedgerServer(config);
        dLedgerServer.startup();
        return dLedgerServer;
    }

    @Bean
    public DLedgerClient launchClient() {
        DLedgerClient dLedgerClient = new DLedgerClient(group, peers);
        dLedgerClient.startup();
        return dLedgerClient;
    }

    public String getPeers() {
        return peers;
    }

    public void setPeers(String peers) {
        this.peers = peers;
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
}

```

# 启动类定义

```java
package com.example.demo;

import io.openmessaging.storage.dledger.DLedgerServer;
import io.openmessaging.storage.dledger.MemberState;
import io.openmessaging.storage.dledger.client.DLedgerClient;
import io.openmessaging.storage.dledger.entry.DLedgerEntry;
import io.openmessaging.storage.dledger.protocol.AppendEntryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class DemoApplication {
	private static int serverPort = 0;
    
	@Autowired
    DLedgerServer dLedgerServer;
    
	@Autowired
    DLedgerClient dLedgerClient;
	@GetMapping("/status")
    MemberState status(){
		return dLedgerServer.getMemberState();
	}
    @GetMapping("/getdata")
	public String getData(Long index){
	   return new String(dLedgerServer.getdLedgerStore().get(index).getBody());
    }
    @GetMapping("/setdata")
    public AppendEntryResponse setData(Long index){
        AppendEntryResponse appendEntryResponse = dLedgerClient.append(("HelloSingleServerInMemory" + index).getBytes());
        return appendEntryResponse;
    }
    public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(DemoApplication.class);
		ConfigurableApplicationContext context =builder.build().run(args);
		serverPort = Integer.parseInt(context.getEnvironment().getProperty("server.port"));
		System.out.println(serverPort);
	}

}

```

> 启动类定义了3个控制器，分别用来获取当前节点状态，存值及取值

# 启动测试

设置`--spring.profiles.active=n0`参数，以激活不同的应用。注意勾选`Allow parallel run`，多进程启动。

![image-20200627194635499](images/应用集成DLedger框架/image-20200627194635499.png)

启动之后结果如下：

![image-20200627194809891](images/应用集成DLedger框架/image-20200627194809891.png)

# 高可用测试

使用`http://localhost:8080/status`测试每个节点的信息，每个节点的port不一样，分别是8080，8081和8082。

`8080`

```json
{
"dLedgerConfig": {
"group": "test_group",
"selfId": "n0",
"peers": "n0-localhost:30001;n1-localhost:30002;n2-localhost:30002",
"storeBaseDir": "\\tmp\\dledgerteststore\\test_group",
"peerPushThrottlePoint": 314572800,
"peerPushQuota": 20971520,
"storeType": "MEMORY",
"dataStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n0\\data",
"maxPendingRequestsNum": 10000,
"maxWaitAckTimeMs": 2500,
"maxPushTimeOutMs": 1000,
"enableLeaderElector": true,
"heartBeatTimeIntervalMs": 2000,
"maxHeartBeatLeak": 3,
"minVoteIntervalMs": 300,
"maxVoteIntervalMs": 1000,
"fileReservedHours": 72,
"deleteWhen": "04",
"diskSpaceRatioToCheckExpired": 0.7,
"diskSpaceRatioToForceClean": 0.85,
"enableDiskForceClean": true,
"flushFileInterval": 10,
"checkPointInterval": 3000,
"mappedFileSizeForEntryData": 1073741824,
"mappedFileSizeForEntryIndex": 167772160,
"enablePushToFollower": true,
"indexStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n0\\index",
"defaultPath": "\\tmp\\dledgerteststore\\test_group\\dledger-n0",
"diskFullRatio": 0.90000004
},
"defaultLock": {
"holdCount": 0,
"queueLength": 0,
"fair": false,
"heldByCurrentThread": false,
"locked": false
},
"group": "test_group",
"selfId": "n0",
"role": "FOLLOWER",
"leaderId": "n1",
"ledgerEndIndex": -1,
"ledgerEndTerm": -1,
"peerMap": {
"n0": "localhost:30001",
"n1": "localhost:30002",
"n2": "localhost:30002"
},
"candidate": false,
"leader": false,
"leaderAddr": "localhost:30002",
"selfAddr": "localhost:30001",
"follower": true
}
```

> 注意"role": "FOLLOWER",表明当前节点是FOLLOWER角色

`8081`

```json
{
"dLedgerConfig": {
"group": "test_group",
"selfId": "n1",
"peers": "n0-localhost:30001;n1-localhost:30002;n2-localhost:30002",
"storeBaseDir": "\\tmp\\dledgerteststore\\test_group",
"peerPushThrottlePoint": 314572800,
"peerPushQuota": 20971520,
"storeType": "MEMORY",
"dataStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n1\\data",
"maxPendingRequestsNum": 10000,
"maxWaitAckTimeMs": 2500,
"maxPushTimeOutMs": 1000,
"enableLeaderElector": true,
"heartBeatTimeIntervalMs": 2000,
"maxHeartBeatLeak": 3,
"minVoteIntervalMs": 300,
"maxVoteIntervalMs": 1000,
"fileReservedHours": 72,
"deleteWhen": "04",
"diskSpaceRatioToCheckExpired": 0.7,
"diskSpaceRatioToForceClean": 0.85,
"enableDiskForceClean": true,
"flushFileInterval": 10,
"checkPointInterval": 3000,
"mappedFileSizeForEntryData": 1073741824,
"mappedFileSizeForEntryIndex": 167772160,
"enablePushToFollower": true,
"defaultPath": "\\tmp\\dledgerteststore\\test_group\\dledger-n1",
"indexStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n1\\index",
"diskFullRatio": 0.90000004
},
"defaultLock": {
"holdCount": 0,
"queueLength": 0,
"fair": false,
"heldByCurrentThread": false,
"locked": false
},
"group": "test_group",
"selfId": "n1",
"role": "LEADER",
"leaderId": "n1",
"ledgerEndIndex": -1,
"ledgerEndTerm": -1,
"peerMap": {
"n0": "localhost:30001",
"n1": "localhost:30002",
"n2": "localhost:30002"
},
"leaderAddr": "localhost:30002",
"leader": true,
"follower": false,
"selfAddr": "localhost:30002",
"candidate": false
}
```

> 注意"role": "LEADER",表明当前节点是LEADER角色

`8082`

```json
{
"dLedgerConfig": {
"group": "test_group",
"selfId": "n2",
"peers": "n0-localhost:30000;n1-localhost:30001;n2-localhost:30002",
"storeBaseDir": "\\tmp\\dledgerteststore\\test_group",
"peerPushThrottlePoint": 314572800,
"peerPushQuota": 20971520,
"storeType": "MEMORY",
"dataStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2\\data",
"maxPendingRequestsNum": 10000,
"maxWaitAckTimeMs": 2500,
"maxPushTimeOutMs": 1000,
"enableLeaderElector": true,
"heartBeatTimeIntervalMs": 2000,
"maxHeartBeatLeak": 3,
"minVoteIntervalMs": 300,
"maxVoteIntervalMs": 1000,
"fileReservedHours": 72,
"deleteWhen": "04",
"diskSpaceRatioToCheckExpired": 0.7,
"diskSpaceRatioToForceClean": 0.85,
"enableDiskForceClean": true,
"flushFileInterval": 10,
"checkPointInterval": 3000,
"mappedFileSizeForEntryData": 1073741824,
"mappedFileSizeForEntryIndex": 167772160,
"enablePushToFollower": true,
"indexStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2\\index",
"defaultPath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2",
"diskFullRatio": 0.90000004
},
"defaultLock": {
"holdCount": 0,
"queueLength": 0,
"fair": false,
"heldByCurrentThread": false,
"locked": false
},
"group": "test_group",
"selfId": "n2",
"role": "FOLLOWER",
"leaderId": "n1",
"ledgerEndIndex": -1,
"ledgerEndTerm": -1,
"peerMap": {
"n0": "localhost:30000",
"n1": "localhost:30001",
"n2": "localhost:30002"
},
"candidate": false,
"selfAddr": "localhost:30002",
"leaderAddr": "localhost:30001",
"follower": true,
"leader": false
}
```

## 模拟Leader宕机

在Leader宕机一段时间后，8082的状态成为了Candidate,准备投票选举。

```json
{
"dLedgerConfig": {
"group": "test_group",
"selfId": "n2",
"peers": "n0-localhost:30000;n1-localhost:30001;n2-localhost:30002",
"storeBaseDir": "\\tmp\\dledgerteststore\\test_group",
"peerPushThrottlePoint": 314572800,
"peerPushQuota": 20971520,
"storeType": "MEMORY",
"dataStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2\\data",
"maxPendingRequestsNum": 10000,
"maxWaitAckTimeMs": 2500,
"maxPushTimeOutMs": 1000,
"enableLeaderElector": true,
"heartBeatTimeIntervalMs": 2000,
"maxHeartBeatLeak": 3,
"minVoteIntervalMs": 300,
"maxVoteIntervalMs": 1000,
"fileReservedHours": 72,
"deleteWhen": "04",
"diskSpaceRatioToCheckExpired": 0.7,
"diskSpaceRatioToForceClean": 0.85,
"enableDiskForceClean": true,
"flushFileInterval": 10,
"checkPointInterval": 3000,
"mappedFileSizeForEntryData": 1073741824,
"mappedFileSizeForEntryIndex": 167772160,
"enablePushToFollower": true,
"indexStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2\\index",
"defaultPath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2",
"diskFullRatio": 0.90000004
},
"defaultLock": {
"holdCount": 0,
"queueLength": 0,
"fair": false,
"heldByCurrentThread": false,
"locked": false
},
"group": "test_group",
"selfId": "n2",
"role": "CANDIDATE",
"leaderId": null,
"ledgerEndIndex": -1,
"ledgerEndTerm": -1,
"peerMap": {
"n0": "localhost:30000",
"n1": "localhost:30001",
"n2": "localhost:30002"
},
"candidate": true,
"selfAddr": "localhost:30002",
"leaderAddr": null,
"follower": false,
"leader": false
}
```

再过一小段时间，如果投票成功，Role即改变为Leader

```json
{
"dLedgerConfig": {
"group": "test_group",
"selfId": "n2",
"peers": "n0-localhost:30000;n1-localhost:30001;n2-localhost:30002",
"storeBaseDir": "\\tmp\\dledgerteststore\\test_group",
"peerPushThrottlePoint": 314572800,
"peerPushQuota": 20971520,
"storeType": "MEMORY",
"dataStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2\\data",
"maxPendingRequestsNum": 10000,
"maxWaitAckTimeMs": 2500,
"maxPushTimeOutMs": 1000,
"enableLeaderElector": true,
"heartBeatTimeIntervalMs": 2000,
"maxHeartBeatLeak": 3,
"minVoteIntervalMs": 300,
"maxVoteIntervalMs": 1000,
"fileReservedHours": 72,
"deleteWhen": "04",
"diskSpaceRatioToCheckExpired": 0.7,
"diskSpaceRatioToForceClean": 0.85,
"enableDiskForceClean": true,
"flushFileInterval": 10,
"checkPointInterval": 3000,
"mappedFileSizeForEntryData": 1073741824,
"mappedFileSizeForEntryIndex": 167772160,
"enablePushToFollower": true,
"indexStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2\\index",
"defaultPath": "\\tmp\\dledgerteststore\\test_group\\dledger-n2",
"diskFullRatio": 0.90000004
},
"defaultLock": {
"holdCount": 0,
"queueLength": 0,
"fair": false,
"heldByCurrentThread": false,
"locked": false
},
"group": "test_group",
"selfId": "n2",
"role": "LEADER",
"leaderId": "n2",
"ledgerEndIndex": -1,
"ledgerEndTerm": -1,
"peerMap": {
"n0": "localhost:30000",
"n1": "localhost:30001",
"n2": "localhost:30002"
},
"candidate": false,
"selfAddr": "localhost:30002",
"leaderAddr": "localhost:30002",
"follower": false,
"leader": true
}
```

> 新的Leader

## Leader恢复

重启8081端口(已经宕机的Leader完成恢复)

```json
"dLedgerConfig": {
"group": "test_group",
"selfId": "n1",
"peers": "n0-localhost:30000;n1-localhost:30001;n2-localhost:30002",
"storeBaseDir": "\\tmp\\dledgerteststore\\test_group",
"peerPushThrottlePoint": 314572800,
"peerPushQuota": 20971520,
"storeType": "MEMORY",
"dataStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n1\\data",
"maxPendingRequestsNum": 10000,
"maxWaitAckTimeMs": 2500,
"maxPushTimeOutMs": 1000,
"enableLeaderElector": true,
"heartBeatTimeIntervalMs": 2000,
"maxHeartBeatLeak": 3,
"minVoteIntervalMs": 300,
"maxVoteIntervalMs": 1000,
"fileReservedHours": 72,
"deleteWhen": "04",
"diskSpaceRatioToCheckExpired": 0.7,
"diskSpaceRatioToForceClean": 0.85,
"enableDiskForceClean": true,
"flushFileInterval": 10,
"checkPointInterval": 3000,
"mappedFileSizeForEntryData": 1073741824,
"mappedFileSizeForEntryIndex": 167772160,
"enablePushToFollower": true,
"indexStorePath": "\\tmp\\dledgerteststore\\test_group\\dledger-n1\\index",
"defaultPath": "\\tmp\\dledgerteststore\\test_group\\dledger-n1",
"diskFullRatio": 0.90000004
},
"defaultLock": {
"holdCount": 0,
"queueLength": 0,
"fair": false,
"heldByCurrentThread": false,
"locked": false
},
"group": "test_group",
"selfId": "n1",
"role": "FOLLOWER",
"leaderId": "n2",
"ledgerEndIndex": -1,
"ledgerEndTerm": -1,
"peerMap": {
"n0": "localhost:30000",
"n1": "localhost:30001",
"n2": "localhost:30002"
},
"candidate": false,
"leader": false,
"follower": true,
"selfAddr": "localhost:30001",
"leaderAddr": "localhost:30002"
}
```

> 恢复之后的Leader变成了Followers，集群保证高可用

# 主从复制测试

## 主写

使用路径“http://localhost:8082/setdata?index=2”进行测试。改变后面的index值，可以进行不同的数据测试。

响应结果：

```json
{
"group": "test_group",
"remoteId": null,
"localId": null,
"code": 200,
"leaderId": "n2",
"term": 9,
"index": 0,
"pos": 0
}
```

## 从读

![image-20200627200350118](images/应用集成DLedger框架/image-20200627200350118.png)

当从读取一个不存在index时，会报如下错误：

![image-20200627200628541](images/应用集成DLedger框架/image-20200627200628541.png)

当index过大，那么就需要主多写入一些数据

![image-20200627200714397](images/应用集成DLedger框架/image-20200627200714397.png)

然后从在读

![image-20200627200748669](images/应用集成DLedger框架/image-20200627200748669.png)

到此，DLedger已经完成整合。