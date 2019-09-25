ES读写集群告警监控
---

# 1. 概述
> 该服务监控ES的读写集群状态，通过企业微信机器人自动推送告警信息。
>
> 该系统自动区分读写集群，注意线上读写集群的命名xx-xx-a/xx-xx-b
>
> a为写集群，b为读集群，也可以自行修改源代码SendMonitorWeChat.java

# 2. 支持功能
1. 支持集群状态监控(red，green，未分配分片数量，活跃分片比例)。
2. 支持未分配分片详细位置监控。
3. 支持nginx负载均衡节点的状态监控。
4. 支持企业微信机器人告警。
5. 支持集群状态恢复机器人提醒。
6. 支持延时告警功能（如果集群red时间超过设定值则告警，避免误报）。
7. 支持跳过指定时间告警，避免业务误报。
8. 支持restful接口方式查看集群状态。


# 3. 配置监控
**1. application-idc.properties文件属性介绍**

\#服务端口
>server.port = 8888

\#集群名称
>es.cluster.name = MyEs

\#监控服务主机
>monitor.address = 172.0.0.1

\#nginx代理地址
>es.nginx.url = 172.0.0.1:9200    

\#es主节点
>es.active.url = 172.0.0.1:9200

\#es备用节点
>es.standby.url = 172.0.0.1:9200

\#是否启主节点告警
>es.active.monitor = true

\#是否启用备用节点告警
>es.standby.monitor = true

\#是否开启集群状态延时判断告警
>es.active.DelayedAlarm = false

>es.active.red.delayed.interval = 0

>es.active.yellow.delayed.interval = 0

>es.standby.DelayedAlarm = true 

>es.standby.red.delayed.interval = 1800

>es.standby.yellow.delayed.interval = 2700

\#告警间隔时间 = 监控频率 * 告警频率

\#适当减小监控频率避免不能及时发现集群告警问题，

\#适当增加告警频率让相同的告警间隔扩大，避免频繁提醒

\#监控频率 /s
>es.monitor.frequency = 15

\#告警频率
>es.alarm.frequency = 200

\#告警机器人的请求地址（需要自己申请）
>worker.vx.webhook = https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxxxx 


# 4. 打包上线

>... 

# 5. API使用方法

>   返回值status
>
>   2000则状态正常
>
>   5001负载均衡节点异常
>
>   5000则连接集群超时
>
>   5002等待集群修复
>
>   2001集群状态恢复

**localhost/upstream/status/{type}**

\#type 为active与standby
>   查询集群负载均衡节点的状态" 
>

**localhost/shards/status/{type}**

\#type 为active与standby

> 查询集群分片的状态

**loaclhost/cluster/status/{type}**

\#type 为active与standby

> 查询集群的状态

**localhost/swagger-ui.html**

>API可视化

# 6. 群机器人配置说明
>https://work.weixin.qq.com/api/doc#90000/90136/91770