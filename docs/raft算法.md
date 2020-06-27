# Raft算法

> 本文是根据http://thesecretlivesofdata.com/raft/资料整理而成，相关截图均源于此网站。

# Raft算法是什么？

> Raft是一种分布式一致性协议(Protocol)。主要用于解决分布式环境下的一致性问题。其它的有Paxos，zab(zookeeper)等算法。我们可以理解为Raft是Paxos的简化版，更易于实现。
>

# CAP定理

CAP定理是由加州大学伯克利分校Eric Brewer教授提出来的，他指出WEB服务无法同时满足一下3个属性：

- 一致性(Consistency)
  -  客户端知道一系列的操作都会同时发生(生效) 
- 可用性(Availability) 
  -  每个操作都必须以可预期的响应结束 
- 分区容错性(Partition tolerance) 
  -  即使出现单个组件无法可用,操作依然可以完成 



# 单机数据一致性

比如只有以下一个节点（可以想像为数据库DB）：

![image-20200627071214333](images/raft算法/image-20200627071214333.png)

此时，假如有一个客户端写入数据8到服务端：

![image-20200627071717642](images/raft算法/image-20200627071717642.png)

最终结果，很容易做到一致性，因为就只有一台服务器：

![image-20200627071822198](images/raft算法/image-20200627071822198.png)

# 分布式一致性问题

但如果我们有多台服务器的话，比如3台，上面的操作就会变成如下结果：

![image-20200627071909573](images/raft算法/image-20200627071909573.png)

那么我们完成上面操作时，只有1台服务器有数据，另外两台还没有数据。如果需要保持一致性，那就么需要将数数据8同步到另外两台机，这就是分布式下的一致问题。

# Raft是怎么实现分布式一致性问题？

## 节点角色

> 我们需要一些状态来描述每一个节点，在不同的状态下做不同的事情。

- Leader

  - 主节点，一个集群中只有一台主节点，负责写。

  ![image-20200627073607134](images/raft算法/image-20200627073607134.png)

- Follower

  - 从节点，一个集群中可以有N个从节点，负责读操作。
  - 用以下节点表示：

  ![image-20200627072707368](images/raft算法/image-20200627072707368.png)

- Candidate

  - 候选节点，集群中投票选举时Leader时的节点角色。

  ![image-20200627072932427](images/raft算法/image-20200627072932427.png)

##　1-系统启动后

> 系统启动后，每个节点都是Follower节点。此时，开始election timeout计时。

![image-20200627072800077](images/raft算法/image-20200627072800077.png)

## 2-候选节点

> 当一个集群中没有Leader节点时，即Leader和Followers之间没有心跳，过一段时间后Followers会变成Candidate角色，准备发起投票。具体时间一般是固定间隔(比如150ms)加一个随机数，一般建议在150~300ms之间。保证不同节点发起投票的时间不一致，从而防止投票失败。

![image-20200627072910570](images/raft算法/image-20200627072910570.png)

## 3-启动(选主)

> 简单正常的选主，详细的说明后面会单独的章节描述。

候选节点发起投票操作（绿色小圆点）：

> 具体过程是选投自己一票，然后请求其它节点给自己投票。

![image-20200627073227995](images/raft算法/image-20200627073227995.png)

收到其它节点的票

![image-20200627073534749](images/raft算法/image-20200627073534749.png)

最终投票结果：如果节点收到大多数的节点同意票时，就会成为leader节点(大多数是指超过一半 )

![image-20200627073549929](images/raft算法/image-20200627073549929.png)

> 以上整个过程叫Leader选举

## 4-写操作

### 4.1 写

![image-20200627073855211](images/raft算法/image-20200627073855211.png)

> ”SET 5“即对应的指令操作，日志记录的内容。

节点选主成功后，所有的写操作均使用Leader节点，从节点只有读操作。如上图所示，客户端向主节点写入了一个5，此时主节点会将5写入**日志**(每一个变更操作均会写入日志)。此时节点的值还没有更新，因为日志没有**提交(uncommitted)**。

> 我的理解这里的日志是一个抽象概念，可以是真正的文件日志，或者DB,或者内存Map等等。只要能保存数据即可。通过实现使用文件日志，比如mysql的binlog等。

### 4-2主从复制(*Log Replication*)

![image-20200627082458060](images/raft算法/image-20200627082458060.png)

当5写入主节点后，指令还未提交。因为Leader节点的值还未更新，只是将操作记录到主节点的日志中。此时，需要主节将指令复制到对应的从节点：

![image-20200627082526811](images/raft算法/image-20200627082526811.png)

在主节点将指令复制到对应的从节点后，所有的节点中均存在`SET 5`指令，但还未提交。

![image-20200627083003956](images/raft算法/image-20200627083003956.png)

上图，主节点收到从节点的响应，说可以提交，然后主节点先提交自己本地“事务”，将值更新为5。

![image-20200627083052915](images/raft算法/image-20200627083052915.png)

> 主节点提交指令"SET 5",提交后主节点的值为5。

![image-20200627083325406](images/raft算法/image-20200627083325406.png)

然后主节点向从节点发起提交指令命令（此处通常是通过借助心跳指令完成同步，不需要单独的实现，当然也可以单独实现主从同步，不借助于心跳）。最后，所以节点的的值均为5.

![image-20200627083544868](images/raft算法/image-20200627083544868.png)

## 5-选主(Leader Election)

### 5-1 计时器

> In Raft there are two timeout settings which control elections. The election timeout is randomized to be between 150ms and 300ms.

![image-20200627083808883](images/raft算法/image-20200627083808883.png)

如果上图所示，正常启动之后，会有两个计时器控制着选举：**选举超时(election timeout)**和**心跳频率(heartbeat timeout)**。

- 选举超时
  - 指当follower多长时间没有收到leader的心跳，就主观认为leader已经宕机，然后将自己的角色由follwer改变成candidate，准备发起投票选举。这个超时时间默认是随机的150ms到300ms。为什么随机呢？防止大家同进超时，发起投票，从而导致投票失败。如果超时时间不一样，总有一个follower先发起投票，进行成为Leader。
- 心跳超时
  - 即Leader向从服务器发起心跳间隔，心跳间隔小于election timeout。

当在election time时间内，如果Follower未收到Leader的心跳信息，Follower会将自己的角色从Follower转换成Candidate，发起新的一轮投票选举。

### 5-2 Candidate并自己投票

假如节点C一直没收到Leader的心跳，那就会在`election timeout`时间后，将角色从Followers修改为`candidate`,准备发起投票。

此时，节点A和B还是倒计时。因为election timeout是随机的，总有一个节点会先结束，改变角色，发起新的投票。此时Term会加1。即新的一轮投票，并且自己先投自己一票，然后向节点A和B发起拉票。

![image-20200627084451794](images/raft算法/image-20200627084451794.png)

### 5-3 请求其它节点投票

![image-20200627084719090](images/raft算法/image-20200627084719090.png)

在节点C给自己投票后，向节点A和B请求投息一票，发起拉票请求。

![image-20200627084831237](images/raft算法/image-20200627084831237.png)

此时，节点A和B收到C的拉票，如果此自己还没有开始投票，检查自己的Term是0，小于C的拉票Term=1，因为同意C的投票，返回给C1票。并且从新开发倒计时(election timeout)。

![image-20200627085053850](images/raft算法/image-20200627085053850.png)

C收到A和B的投票后，当选为Leader节点。

### 5-4 心跳

#### 5-4-1 Leader发起心跳

![image-20200627085230826](images/raft算法/image-20200627085230826.png)

当C成为Leader后，开始向A和B发起心跳检测，A和B每次收到心跳后，都会重置election timeout。并且返回给Leader响应。

>  具体心跳的时间间隔叫`heartbeat timeout`。如果在heartbeat timeout时间Leader还没发出heartbeat,则可以主观认为Leader已经宕机。

#### 5-4-2 Follower响应心跳

![image-20200627085505563](images/raft算法/image-20200627085505563.png)

如果Followers不返回给Leader响应，那么Leader可以认为Followers已经宕机（具体判断规则可以根据实际场景自定义），然后将已经宕机的Followers踢出集群。

> 正常运行时，5-4-1和5-4-2一直不断的重复进行，直到有节点成为Candidate（可能是Leader宕机，也可以是Follower自己网络出问题）

### 5-5 Leader宕机

![image-20200627090906059](images/raft算法/image-20200627090906059.png)

如果Leader C宕机，那么A和B收不到C的心跳信息，此时election timeout会超时，比如A先结束超时。

![image-20200627093133948](images/raft算法/image-20200627093133948.png)

A在election timeout后，自己成为candicate,发起投票选举。此时B没有收到C的心跳信息，并且自己当前的Term是1，小于A的Term=2，所以同意A的投票。

![image-20200627093257202](images/raft算法/image-20200627093257202.png)

此时，A收到B的投票后，成为Leader。A会向B和C发起心跳

![image-20200627093323997](images/raft算法/image-20200627093323997.png)

此时，只有B响应给A的心跳，因为C已经宕机。此时，集群只有两个节点，A和B.

![image-20200627093358289](images/raft算法/image-20200627093358289.png)

### 5-6 Leader恢复



## 6 选举脑裂(split vote)

> 如果A和B同时election timeout,那么肯定会同时发起投票，这时，怎么处理呢?

![image-20200627093751259](images/raft算法/image-20200627093751259.png)

如上图所示，B和C同时election timeout,同时发起投票，并且B投票信息先于D的投票到达A节点，D的投票信息先于B的投票信息到达A节点。即，此时B和D手中各有两票(自己手中有一票)

![image-20200627093911794](images/raft算法/image-20200627093911794.png)

![image-20200627094059593](images/raft算法/image-20200627094059593.png)

![image-20200627094137762](images/raft算法/image-20200627094137762.png)

当B和C各有2票，并且得不到更多票时怎么办？B和C收到的票数没有超过大多数(majority of votes)票，因此不会成为Leader，即AB,C,D节点都收不到Leader的心跳。

此时，D的election timer快结束了，会发起新的一轮投票，Term=5的投票。(如果此时有节点有C同时超时，那么继续下一轮投票即可)。此时，A,B,D中的Term都是4，D发起新的南村Term是5，因此会获得相应票数。

![image-20200627094237747](images/raft算法/image-20200627094237747.png)

最终，D成为Leader，并且Term是5。

![image-20200627094445829](images/raft算法/image-20200627094445829.png)

![image-20200627094725933](images/raft算法/image-20200627094725933.png)

## 7-主从复制（Log Replication)

> 1、系统正常运行时，我们的数据变化操作(册改增)都需要Leader执行。Followers只能执行查询操作，因为大多数据场景，查询量远远大于写的量。
>
> 2、当客户执行操作时，服务端的heart beat一直在不断的进行，相互不干扰。
>
> 3、具体实现可以优化调整

![image-20200627094904306](images/raft算法/image-20200627094904306.png)

当Leader执行了写的操作时，需要将数据同步到Followers，保证数据的一致性。这个过程就叫Log replication

### 7-1 客户端写

![image-20200627095146168](images/raft算法/image-20200627095146168.png)

如上图所示，客户端向Leader发起写操作。Leader C先将操作记录到Log，还没提交。

![image-20200627095442100](images/raft算法/image-20200627095442100.png)



### 7-2 Replication

在下一个心跳时，Leader会将"SET　５"这个Log发送给Follower　Ａ和Ｂ。

![image-20200627095531997](images/raft算法/image-20200627095531997.png)

Ａ和Ｂ收到同步Leader c的同步信息，将“SET 5”写到自己的本地Log中，并且响应给Leader c可以提交。

![image-20200627095644812](images/raft算法/image-20200627095644812.png)

Leader C判断，大多数可以提交(超过半数)，给客户端响应成功

![image-20200627095903303](images/raft算法/image-20200627095903303.png)

并且在下一次心跳时，发起提交指令

![image-20200627095925691](images/raft算法/image-20200627095925691.png)

比如再执行“ADD 2”命令，所有节点的结果会是7

![image-20200627100032438](images/raft算法/image-20200627100032438.png)

## 8-网络分裂(分区)

> 简单理解 ，网络分区就是因为故障，将一个独立的网络分割成了多个小网络，各小网络之间不能连通。常见，当路由器或者交换机出现问题，或者网口down掉时，都可能发生网络分区。

Raft可以处理网络分裂。

### 8-1 正常情况

![image-20200627100638521](images/raft算法/image-20200627100638521.png)

假如，A和B，与C,D,E形成了两个网络分区。

![image-20200627100756424](images/raft算法/image-20200627100756424.png)

### 8-2 双Leader

此时，C,D,E收不到Leader B的心跳信息，会发起新的一轮投票(Term = 2)，比如D最先发起投票，因为此时有3个节点，超过总节点数5个(A,B,C,D,E)的一半，因此，可以投票成功，D会成为新的Leader。如下衅所示：

![image-20200627100821912](images/raft算法/image-20200627100821912.png)

这时，就会出现两个Leader同时存在的情况。

### 8-3 客户端写

![image-20200627101231649](images/raft算法/image-20200627101231649.png)

假如，此时再增加一个客户端进行写操作，此时客户端会更新两个Leader。

比如先更新Leader B。此时，B能收到的ACK只有A,没有超过半数，因为写失败。B的写日志未处于提交状态。

![image-20200627101321524](images/raft算法/image-20200627101321524.png)

假如另一个客户端写入8到另一个Leader D。因为有C和E投票，因此超过半数，可以正常提交，最终写入成功。

![image-20200627101513087](images/raft算法/image-20200627101513087.png)

### 8-4 网络恢复

![image-20200627101630534](images/raft算法/image-20200627101630534.png)

![image-20200627101659842](images/raft算法/image-20200627101659842.png)

此时，节点B会收到更高的Term的信息(Leader D节点发送的心跳信息)，因此自己将Leader角色改变为Follower，并且A和B都会回滚未提交的操作，然后从新的Leader D中进行日志同步，从而达到整个集群的最终一致性。

![image-20200627101956763](images/raft算法/image-20200627101956763.png)