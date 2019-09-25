package com.jingxi.es.monitor.service;

import com.alibaba.fastjson.JSONObject;
import com.jingxi.es.monitor.component.InitRestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author JINGXI
 * @apiNote 监控集群整体状态
 * @since 2019-09-19
 */
@Service
public class ClusterStatusService {

    private static String nginxUrl;
    private static String standBynginxUrl;
    private static int alarmFrequency;


    @Value("${es.active.url}")
    public void setNginxUrl(String nginxUrl2) {
        nginxUrl = nginxUrl2;
    }

    @Value("${es.standby.url}")
    public void setstandBynginxUrl(String standBynginxUrl2) {
        standBynginxUrl = standBynginxUrl2;
    }

    @Value("${es.alarm.frequency}")
    public void setAlarmFrequency2(int alarmFrequency2) {
        alarmFrequency = alarmFrequency2;
    }


    //控制报警的频率
    private static int alertFrequency = alarmFrequency * 1024;
    private static int alertStandByFrequency = alarmFrequency * 1024;

    public static JSONObject getClusterStatus(String clusterType) {
        JSONObject resultInfo = new JSONObject(true);
        resultInfo.put("status", 2000);
        RestTemplate restFul = InitRestTemplate.getRestTemplate();
        //TODO 日志
        JSONObject clusterStatus = null;
        try {
            if (!clusterType.equals("standby")) {
                clusterStatus = restFul.getForObject("http://" + nginxUrl + "/_cluster/health", JSONObject.class);
            } else {
                clusterStatus = restFul.getForObject("http://" + standBynginxUrl + "/_cluster/health", JSONObject.class);
            }

        } catch (RestClientException e) {
            resultInfo.put("info", "连接集群超时");
            resultInfo.put("status", 5000);
            return resultInfo;
        }

        assert clusterStatus != null;


        // 监控读写集群
        if (clusterStatus.getString("status").trim().equals("red")) {

            if (!clusterType.equals("standby")) {
                if (alertFrequency > alarmFrequency * 1024) alertFrequency = alarmFrequency * 1024;
                if (alertFrequency % alarmFrequency == 0) {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5001);
                } else {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5002);
                }
                alertFrequency--;
            } else {
                if (alertStandByFrequency > alarmFrequency * 1024){ alertStandByFrequency = alarmFrequency * 1024;}
                if (alertStandByFrequency % alarmFrequency == 0) {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5001);
                } else {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5002);
                }
                alertStandByFrequency--;
            }
        } else if (clusterStatus.getString("status").trim().equals("yellow")) {

            if (!clusterType.equals("standby")) {
                if (alertFrequency < alarmFrequency * 1024) alertFrequency = alarmFrequency * 1024;
                if (alertFrequency % alarmFrequency == 0) {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5001);
                } else {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5002);
                }
                alertFrequency++;
            } else {
                if (alertStandByFrequency < alarmFrequency * 1024){ alertStandByFrequency = alarmFrequency * 1024;}
                if (alertStandByFrequency % alarmFrequency == 0) {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5001);
                } else {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5002);
                }
                alertStandByFrequency++;
            }

        } else {
            //给定初始值

            if (!clusterType.equals("standby")) {
                if (alertFrequency == 0) alertFrequency = alarmFrequency * 1024;
                if (alertFrequency != alarmFrequency * 1024) {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5001);
                }else {
                    resultInfo.put("info", clusterStatus);
                }
                alertFrequency = alarmFrequency * 1024;
            } else {
                if (alertStandByFrequency == 0) alertStandByFrequency = alarmFrequency * 1024;
                if (alertStandByFrequency != alarmFrequency * 1024) {
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status", 5001);
                }else{
                    resultInfo.put("info", clusterStatus);
                    resultInfo.put("status",2000);
                }
                alertStandByFrequency = alarmFrequency * 1024;
            }
        }
        return resultInfo;
    }


}
