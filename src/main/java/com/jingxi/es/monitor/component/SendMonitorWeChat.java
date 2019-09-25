package com.jingxi.es.monitor.component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jingxi.es.monitor.service.ClusterStatusService;
import com.jingxi.es.monitor.service.ShardsService;
import com.jingxi.es.monitor.service.UpstreamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Calendar;


/**
 * @author JINGXI
 * @apiNote 采用企业微信webhook实现自动告警
 * @since 2019-09-18
 */

@Component
@Order(value = 1)
public class SendMonitorWeChat implements CommandLineRunner {


    private static int serverPort;
    private static String monitorAddress;
    private static String webHookUrl;
    private static Boolean standMonitor;
    private static Boolean activeMonitor;
    private static int monitorFrequency;
    private static Long activeDate;
    private static Long standbyDate;
    private static String esClusterName;
    private static String activeClusterState = "0";
    private static String standbyClusterState = "0";
    private static Boolean activeRedMonitor = true;
    private static Boolean activeYellowMonitor = true;
    private static Boolean standbyRedMonitor = true;
    private static Boolean standbyYellowMonitor = true;
    private static Boolean enableActiveAlerm = true;
    private static Boolean enableStandbyAlerm = true;

    private static Boolean enableActiveDelayedAlarm;
    private static Boolean enableStandbyDelayedAlarm;

    private static int standbyRedDelayedInterval;
    private static int standbyYellowDelayedInterval;

    private static int activeRedDelayedInterval;
    private static int activeYellowDelayedInterval;

    private static Calendar calendar = Calendar.getInstance();

    @Value("${es.cluster.name}")
    public void setEsClusterName(String esClusterName2) {
        esClusterName = esClusterName2;
    }

    @Value("${es.active.red.delayed.interval}")
    public void setActiveRedDelayedInterval(int activeRedDelayedInterval2) {
        activeRedDelayedInterval = activeRedDelayedInterval2;
    }

    @Value("${es.active.yellow.delayed.interval}")
    public void setActiveYellowDelayedInterval(int activeYellowDelayedInterval2) {
        activeYellowDelayedInterval = activeYellowDelayedInterval2;
    }


    @Value("${es.standby.red.delayed.interval}")
    public void setStandbyRedDelayedInterval(int standbyRedDelayedInterval2) {
        standbyRedDelayedInterval = standbyRedDelayedInterval2;
    }

    @Value("${es.standby.yellow.delayed.interval}")
    public void setStandbyYellowDelayedInterval(int standbyYellowDelayedInterval2) {
        standbyYellowDelayedInterval = standbyYellowDelayedInterval2;
    }


    @Value("${es.active.DelayedAlarm}")
    public void setEnableActiveDelayedAlarm(Boolean enableActiveDelayedAlarm2) {
        enableActiveDelayedAlarm = enableActiveDelayedAlarm2;
    }

    @Value("${es.standby.DelayedAlarm}")
    public void setEnableStandbyDelayedAlarm(Boolean enableStandbyDelayedAlarm2) {
        enableStandbyDelayedAlarm = enableStandbyDelayedAlarm2;
    }


    @Value("${server.port}")
    public void setServerPort(int serverPort2) {
        serverPort = serverPort2;
    }

    @Value("${monitor.address}")
    public void setMonitorAddress(String monitorAddress2) {
        monitorAddress = monitorAddress2;
    }


    @Value("${worker.vx.webhook}")
    public void setWebHookUrl(String webHookUrl2) {
        webHookUrl = webHookUrl2;
    }

    @Value("${es.standby.monitor}")
    public void setStandMonitor(Boolean standMonitor2) {
        standMonitor = standMonitor2;
    }

    @Value("${es.active.monitor}")
    public void setActiveMonitor(Boolean activeMonitor2) {
        activeMonitor = activeMonitor2;
    }

    @Value("${es.monitor.frequency}")
    public void setMonitorFrequency(int monitorFrequency2) {
        monitorFrequency = monitorFrequency2;
    }

    @Override
    public void run(String... args) throws Exception {

        //对分片位置进行缓存
        if (activeMonitor)ShardsService.getShardsStatus("active");
        if (standMonitor) ShardsService.getShardsStatus("standby");


        //微信机器人协议地址
        RestTemplate restFul = InitRestTemplate.getRestTemplate();

        //包装推送的数据
        JSONObject reqData = new JSONObject();
        JSONObject data = new JSONObject();

        //循环检测集群状态,按分钟
        while (true) {

            Long currentTime = System.currentTimeMillis();
            JSONObject clusterStatus = new JSONObject();
            JSONObject clusterStandByStatus = new JSONObject();
            if (standMonitor) clusterStandByStatus = ClusterStatusService.getClusterStatus("standby");

            //获取主节点信息
            if (activeMonitor)  clusterStatus = ClusterStatusService.getClusterStatus("active");

            //获取nginx节点信息
            JSONObject nginxStatus = UpstreamService.getUpstreamStatus();

            assert nginxStatus != null;
            assert clusterStatus != null;

            //获取当前时间
            int hour = calendar.get(Calendar.HOUR);

            //判断是否开启当前集群的延时告警
            if (activeMonitor && enableActiveDelayedAlarm) {
                if (clusterStatus.getJSONObject("info").getString("status").equals("red")) {
                    enableActiveAlerm = false;
                    activeYellowMonitor = true;
                    if (!activeClusterState.equals("red")) {
                        //更新时间
                        activeDate = System.currentTimeMillis();
                        clusterStatus.put("status", 5002);
                    } else {
                        if ((currentTime - activeDate) >= activeRedDelayedInterval * 1001 && activeRedMonitor) {
                            activeRedMonitor = false;
                            clusterStatus.put("status", 5001);
                        }

                    }
                    activeClusterState = "red";
                } else if (clusterStatus.getJSONObject("info").getString("status").equals("yellow")) {
                    activeRedMonitor = true;
                    enableActiveAlerm = false;
                    if (!activeClusterState.equals("yellow")) {
                        //更新时间
                        activeDate = System.currentTimeMillis();
                        clusterStatus.put("status", 5002);
                    } else {
                        if ((currentTime - activeDate) >= activeYellowDelayedInterval * 1001 && activeYellowMonitor) {
                            activeYellowMonitor = false;
                            clusterStatus.put("status", 5001);
                        }

                    }
                    activeClusterState = "yellow";
                } else {
                    activeClusterState = "green";
                    if (!activeRedMonitor || !activeYellowMonitor) {
                        enableActiveAlerm = true;
                        activeRedMonitor = true;
                        activeYellowMonitor = true;
                    }
                }
            }

            //读集群
            if(standMonitor && (hour<=5 || hour >=9)){
                if (enableStandbyDelayedAlarm) {
                    if (clusterStandByStatus.getJSONObject("info").getString("status").equals("red")) {
                        enableStandbyAlerm = false;
                        standbyYellowMonitor = true;
                        if (!standbyClusterState.equals("red")) {
                            //更新时间
                            standbyDate = System.currentTimeMillis();
                            clusterStandByStatus.put("status", 5002);
                        } else {
                            if ((currentTime - standbyDate) >= standbyRedDelayedInterval * 1000 && standbyRedMonitor) {
                                standbyRedMonitor = false;
                                clusterStandByStatus.put("status", 5001);
                            }

                        }
                        standbyClusterState = "red";
                    } else if (clusterStandByStatus.getJSONObject("info").getString("status").equals("yellow")) {
                        standbyRedMonitor = true;
                        enableStandbyAlerm = false;
                        if (!standbyClusterState.equals("yellow")) {
                            //更新时间
                            standbyDate = System.currentTimeMillis();
                            clusterStandByStatus.put("status", 5002);
                        } else {
                            if ((currentTime - standbyDate) >= standbyYellowDelayedInterval * 1000 && standbyYellowMonitor) {
                                standbyYellowMonitor = false;
                                clusterStandByStatus.put("status", 5001);
                            }

                        }
                        standbyClusterState = "yellow";
                    } else {
                        standbyClusterState = "green";
                        if (!standbyRedMonitor || !standbyYellowMonitor) {
                            enableStandbyAlerm = true;
                            standbyRedMonitor = true;
                            standbyYellowMonitor = true;
                        }
                    }
                }
            }else {
                enableStandbyAlerm = false;
                standbyRedMonitor = true;
                standbyYellowMonitor = true;
            }

            //监控线上读写集群
            if (activeMonitor && clusterStatus.getIntValue("status") == 5001 && (!activeRedMonitor || !activeYellowMonitor || enableActiveAlerm)) {
                JSONObject clusterInfo = clusterStatus.getJSONObject("info");
                String clusterType = "Read";
                String clusterName = clusterInfo.getString("cluster_name");
                String clusterState = clusterInfo.getString("status");
                Float activeShardsPercentAsNumber = clusterInfo.getFloat("active_shards_percent_as_number");
                int unassignedShards = clusterInfo.getIntValue("unassigned_shards");
                int initializingShards = clusterInfo.getIntValue("initializing_shards");
                String monitorColor = "warning";
                if (clusterState.equals("green")) monitorColor = "info";
                if (clusterState.equals("red")) monitorColor = "comment";
                if (!clusterName.toLowerCase().split("_")[2].equals("b")) clusterType = "Write";
                if (!clusterState.equals("green")) {
                    data.put("content", "**[ "+esClusterName+" ES " + clusterType + " Cluster Status Exception ]** \n" +
                            ">集群状态: <font color=" + monitorColor + ">" + clusterState + "</font> \n" +
                            ">未分配分片数量: " + unassignedShards + " \n" +
                            ">初始化分片数量: " + initializingShards + " \n" +
                            ">活跃分片比例: " + activeShardsPercentAsNumber + "% \n" +
                            //测试
                            ">[分片详细信息](" + monitorAddress + ":" + serverPort + "/shards/status/active)");
                    reqData.put("msgtype", "markdown");
                    reqData.put("markdown", data);

                } else {
                    data.put("content", "**[ "+esClusterName+" ES " + clusterType + " Cluster Recovery Health ]** \n" +
                            ">集群状态: <font color=" + monitorColor + ">" + clusterState + "</font> \n" +
                            ">活跃分片比例: " + activeShardsPercentAsNumber + "%");
                    reqData.put("msgtype", "markdown");
                    reqData.put("markdown", data);
                }

                //发送信息
                sendToWorkVx(reqData, restFul);

            }

            if (standMonitor && clusterStandByStatus.getIntValue("status") == 5001 && (!standbyRedMonitor || !standbyYellowMonitor || enableStandbyAlerm)) {
                JSONObject clusterInfo = clusterStandByStatus.getJSONObject("info");
                reqData.clear();
                data.clear();
                String clusterType = "Write";
                String clusterName = clusterInfo.getString("cluster_name");
                String clusterState = clusterInfo.getString("status");
                Float activeShardsPercentAsNumber = clusterInfo.getFloat("active_shards_percent_as_number");
                int unassignedShards = clusterInfo.getIntValue("unassigned_shards");
                int initializingShards = clusterInfo.getIntValue("initializing_shards");
                String monitorColor = "info";
                if (clusterState.equals("red")) monitorColor = "comment";
                if (clusterState.equals("yellow")) monitorColor = "warning";
                if (clusterName.toLowerCase().split("_")[2].equals("b")) clusterType = "Read";
                if (!clusterState.equals("green")) {
                    data.put("content", "**[ "+esClusterName+" ES " + clusterType + " Cluster Status Exception ]** \n" +
                            ">集群状态: <font color=" + monitorColor + ">" + clusterState + "</font> \n" +
                            ">未分配分片数量: " + unassignedShards + " \n" +
                            ">初始化分片数量: " + initializingShards + " \n" +
                            ">活跃分片比例: " + activeShardsPercentAsNumber + "% \n" +
                            ">[分片详细信息](" + monitorAddress + ":" + serverPort + "/shards/status/standby)");
                    reqData.put("msgtype", "markdown");
                    reqData.put("markdown", data);

                } else {
                    data.put("content", "**[ "+esClusterName+" ES " + clusterType + " Cluster Recovery Health ]** \n" +
                            ">集群状态: <font color=" + monitorColor + ">" + clusterState + "</font> \n" +
                            ">活跃分片比例: " + activeShardsPercentAsNumber + "%");
                    reqData.put("msgtype", "markdown");
                    reqData.put("markdown", data);
                }

                //发送信息
                sendToWorkVx(reqData, restFul);
            }

            //监控Nginx负载均衡节点
            if (nginxStatus.getIntValue("status") == 5001) {
                JSONArray info = nginxStatus.getJSONArray("info");
                reqData.clear();
                data.clear();
                if (info.size() != 0) {
                    StringBuilder exceptionNode = new StringBuilder("[ ");

                    for (int i = 0; i < info.size(); i++) {
                        exceptionNode.append(info.getJSONObject(i).get("hostName"));
                        exceptionNode.append("-");
                        exceptionNode.append(info.getJSONObject(i).get("status"));
                        exceptionNode.append(" ");
                    }
                    exceptionNode.append(" ]");

                    data.put("content", "**[ "+esClusterName+" ES Cluster Nginx Upstream Status Exception ]** \n" +
                            ">异常Client节点: <font color= \"warning\">" + exceptionNode + "</font>");
                    reqData.put("msgtype", "markdown");
                    reqData.put("markdown", data);
                } else {
                    data.put("content", "**[ "+esClusterName+" ES Cluster Nginx Upstream Status Recovery Health ]** \n" +
                            ">Client: <font color= \"info\">green</font>");
                    reqData.put("msgtype", "markdown");
                    reqData.put("markdown", data);
                }
                //告警
                sendToWorkVx(reqData, restFul);
            }
            //监测频率
            Thread.sleep(monitorFrequency * 1000);
        }


    }

    //向企业微信推送告警
    private void sendToWorkVx(JSONObject reqData, RestTemplate restFul) throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> entity = new HttpEntity<>(reqData, headers);
        try {
            //向webHook推送消息
            restFul.exchange(webHookUrl, HttpMethod.POST, entity, JSONObject.class);
        } catch (RestClientException e) {
            //TODO 日志
            System.out.println("超时异常" + e);
        }
    }

}
