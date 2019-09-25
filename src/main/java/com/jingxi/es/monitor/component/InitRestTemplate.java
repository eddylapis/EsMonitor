package com.jingxi.es.monitor.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import javax.annotation.PostConstruct;
/**
 * @author JINGXI
 * @since 2019-09-18
 *
 */
@Component
public class InitRestTemplate {

    @Autowired
    private RestTemplate restProxy;
    private static InitRestTemplate initRestTemplate;

    @PostConstruct
    public void init() {
        initRestTemplate = this;
        initRestTemplate.restProxy = this.restProxy;
    }

    public static RestTemplate getRestTemplate(){
        return initRestTemplate.restProxy;
    }


}
