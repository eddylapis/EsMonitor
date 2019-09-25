package com.jingxi.es.monitor.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jingxi.es.monitor.component.InitRestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author JINGXI
 * @apiNote 查询nginx负载均衡池中节点的状态
 * @since 2019-09-18
 */
@Service
public class UpstreamService {

    private static String nginxUrl;
    private static int alarmFrequency;

    @Value("${es.nginx.url}")
    public void setNginxUrl(String nginxUrl2) {
        nginxUrl = nginxUrl2;
    }

    @Value("${es.alarm.frequency}")
    public void setAlarmFrequency(int alarmFrequency2) {
        alarmFrequency = alarmFrequency2;
    }


    private static JSONObject resultInfo = new JSONObject(true);
    //控制报警的频率
    private static int alertFrequency = alarmFrequency * 1024;


    public static JSONObject getUpstreamStatus() {

        RestTemplate restFul = InitRestTemplate.getRestTemplate();
        resultInfo.clear();

        //默认返回值
        resultInfo.put("info", "负载均衡节点状态正常");
        resultInfo.put("status", 2000);

        //TODO 日志
        JSONArray exceptionStatus = new JSONArray();
        JSONObject upStreamStatus = null;
        try {
            upStreamStatus = restFul.getForObject("http://" + nginxUrl + "/estatus", JSONObject.class);
        } catch (RestClientException ignored) {
            resultInfo.put("info", "集群连接超时");
            resultInfo.put("status", 5000);
            return resultInfo;
        }

        assert upStreamStatus != null;
        JSONObject servers = upStreamStatus.getJSONObject("servers");
        int totalServers = servers.getIntValue("total");

        JSONArray server = servers.getJSONArray("server");
        for (int i = 0; i < totalServers; i++) {
            String status = server.getJSONObject(i).getString("status");
            if (!status.trim().equals("up")) {
                JSONObject exceptionState = new JSONObject();
                String hostName = server.getJSONObject(i).getString("name");
                exceptionState.put("hostName", hostName);
                exceptionState.put("status", status);
                exceptionStatus.add(exceptionState);
            }
        }
        if (exceptionStatus.size() == 0) {
            //给定初始值
            if (alertFrequency == 0) alertFrequency = alarmFrequency * 1024;
            if (alertFrequency != alarmFrequency * 1024) {
                resultInfo.put("info", exceptionStatus);
                resultInfo.put("status", 5001);
            }
            alertFrequency = alarmFrequency * 1024;
        } else {
            if (alertFrequency % alarmFrequency == 0) {
                alertFrequency--;
                resultInfo.put("info", exceptionStatus);
                resultInfo.put("status", 5001);
            } else {
                resultInfo.put("info", "等待集群修复");
                resultInfo.put("status", 5002);
            }
        }
        return resultInfo;
    }
}
