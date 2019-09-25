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
 * @apiNote 查询集群具体的异常分片以及分片异常之前所在的节点
 * @since 2019-09-19
 */
@Service
public class ShardsService {

    private static String nginxUrl;
    private static String standBynginxUrl;

    @Value("${es.active.url}")
    public void setNginxUrl(String nginxUrl2) {
        nginxUrl = nginxUrl2;
    }

    @Value("${es.standby.url}")
    public void setstandBynginxUrl(String standBynginxUrl2) {
        standBynginxUrl = standBynginxUrl2;
    }


    private static JSONObject resultInfo = new JSONObject();
    private static JSONObject previousNodeInfo;
    private static JSONObject previousStandByNodeInfo;
    private static JSONArray exceptionIndex = new JSONArray();

    public static JSONObject getShardsStatus(String clusterType) {
        RestTemplate restFul = InitRestTemplate.getRestTemplate();
        //TODO 日志
        String shardsStatus;
        try {
            if (!clusterType.equals("standby")) {
                shardsStatus = restFul.getForObject("http://" + nginxUrl + "/_cat/shards?h=index,shard,state,prirep,ip,node", String.class);
            } else {
                shardsStatus = restFul.getForObject("http://" + standBynginxUrl + "/_cat/shards?h=index,shard,state,prirep,ip,node", String.class);
            }
        } catch (RestClientException e) {
            resultInfo.put("info", "连接集群超时");
            resultInfo.put("status", 5000);
            return resultInfo;
        }
        JSONObject currentNodeInfo = new JSONObject();
        assert shardsStatus != null;
        //将分片数据封装成json

        int code = 0;
        //每次请求确保异常分片只有一个
        exceptionIndex.clear();

        String[] shards = shardsStatus.trim().split("[\n\r]");
        for (String shard : shards) {
            String[] shardInfo = shard.trim().split("\\s+");
            if (shardInfo.length >= 6) {
                if (shardInfo[2].trim().equals("STARTED") || shardInfo[2].trim().equals("RELOCATING")) {
                    JSONObject detail = new JSONObject(true);
                    detail.put("shard", shardInfo[1]);
                    detail.put("status", shardInfo[2]);
                    detail.put("prirep", shardInfo[3]);
                    detail.put("host", shardInfo[4]);
                    detail.put("node", shardInfo[5]);
                    if (currentNodeInfo.containsKey(shardInfo[0])) {
                        JSONArray existData = currentNodeInfo.getJSONArray(shardInfo[0]);
                        existData.add(detail);
                        currentNodeInfo.put(shardInfo[0], existData);
                    } else {
                        JSONArray detailInfo = new JSONArray();
                        detailInfo.add(detail);
                        currentNodeInfo.put(shardInfo[0], detailInfo);
                    }
                } else {
                    code++;
                    JSONObject exceptionShard = new JSONObject(true);

                    //如果分片状态不为start 并且分片为主分片，则告警
//                    if(shardInfo[5].trim().equals("p")){
                    JSONObject info = new JSONObject();
                    info.put("host", shardInfo[4]);
                    info.put("node", shardInfo[5]);
                    exceptionShard.put("index", shardInfo[0]);
                    exceptionShard.put("shard", shardInfo[1]);
                    exceptionShard.put("status", shardInfo[2]);
                    exceptionShard.put("shardPosition", info);
                    exceptionIndex.add(exceptionShard);
                }
//                }
            } else {
                code++;
                if (shardInfo.length >= 4) {
                    //监控主分片
//                    if (shardInfo[3].trim().equals("p")) {
                    JSONObject exceptionShard = new JSONObject(true);
                    JSONObject queryNodeInfo = previousNodeInfo;
                    if (clusterType.equals("standby")) queryNodeInfo = previousStandByNodeInfo;
                    if (queryNodeInfo == null) {
                        exceptionShard.put("index", shardInfo[0]);
                        exceptionShard.put("shard", shardInfo[1]);
                        exceptionShard.put("status", shardInfo[2]);
                        exceptionShard.put("shardPosition", "分片位置未缓存");
                        exceptionIndex.add(exceptionShard);
                    } else {
                        JSONArray standIndex = queryNodeInfo.getJSONArray(shardInfo[0]);
                        exceptionShard.put("index", shardInfo[0]);
                        exceptionShard.put("shard", shardInfo[1]);
                        exceptionShard.put("status", shardInfo[2]);
                        for (int i = 0; i < standIndex.size(); i++) {
                            JSONObject info = standIndex.getJSONObject(i);
                            if (info.getString("shard").equals(shardInfo[1])) {
                                info.remove("status");
                                exceptionShard.put("shardPosition", info);
                                break;
                            } else {
                                exceptionShard.put("shardPosition", "无此分片位置");
                            }
                        }
                        exceptionIndex.add(exceptionShard);
                    }
                }
            }
        }
//        }

        if (code != 0) {
            resultInfo.put("info", exceptionIndex);
            resultInfo.put("status", 5001);
            return resultInfo;
        } else {
            if (!clusterType.equals("standby")) {
                previousNodeInfo = currentNodeInfo;
            } else {
                previousStandByNodeInfo = currentNodeInfo;
            }
            exceptionIndex.clear();
            resultInfo.put("info", "分片状态正常");
            resultInfo.put("status", 2000);
            return resultInfo;
        }
    }


}
