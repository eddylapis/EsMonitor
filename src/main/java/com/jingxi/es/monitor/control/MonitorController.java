package com.jingxi.es.monitor.control;

import com.alibaba.fastjson.JSONObject;
import com.jingxi.es.monitor.service.ClusterStatusService;
import com.jingxi.es.monitor.service.ShardsService;
import com.jingxi.es.monitor.service.UpstreamService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

/**
 * @author JINGXI
 * @since 2019-09-18
 */

@RestController
public class MonitorController {

    @RequestMapping(value = "/upstream/status", method = RequestMethod.GET)
    @ApiOperation(value = "查询集群负载均衡节点的状态", notes = "返回值为status为2000则状态正常，5001负载均衡节点异常，5000则连接集群超时")
    public JSONObject getUpstreamNodeStatus() {
        return UpstreamService.getUpstreamStatus();
    }

    @RequestMapping(value = "/datanode/status/{type}", method = RequestMethod.GET)
    @ApiOperation(value = "查询集群数据节点的状态", notes = "返回值为0则状态正常。否则状态异常，并返回异常节点信息")
    public String getDataNodeStatus(@PathVariable String type) {
        return "0";
    }

    @RequestMapping(value = "/shards/status/{type}", method = RequestMethod.GET)
    @ApiOperation(value = "查询集群分片的状态", notes = "返回值status为2000则状态正常，5001索引分片状态异常（目前只监控未分配分片），5000则连接集群超时")
    public JSONObject getShardsStatus(@PathVariable String type) {
        return ShardsService.getShardsStatus(type);
    }

    @RequestMapping(value = "/cluster/status/{type}", method = RequestMethod.GET)
    @ApiOperation(value = "查询集群的状态", notes = "返回值status为2000则状态正常，5001集群状态异常，5000则连接集群超时")
    public JSONObject getClusterStatus(@PathVariable String type) {
        return ClusterStatusService.getClusterStatus(type);
    }


}
