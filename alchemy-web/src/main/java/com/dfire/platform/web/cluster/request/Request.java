package com.dfire.platform.web.cluster.request;

/**
 * 需要发送给flink的请求，如提交job、取消job、获取job状态
 * 
 * @author congbai
 * @date 01/06/2018
 */
public interface Request {

    String getClusterName();

    boolean isTest();
}
