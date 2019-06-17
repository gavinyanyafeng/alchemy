package com.dfire.platform.alchemy.common;

/**
 * @author congbai
 * @date 06/06/2018
 */
public final class Constants {

    // 数据传输时间
    public static final Integer SO_TIMEOUT = 15000;

    // 创建连接时间
    public static final int CONNECTION_TIMEOUT = 5000;

    // 设置从连接池获取连接的超时时间
    public static final int CONNECTION_REQUEST_TIMEOUT = 3000;

    // 连接存活时间
    public static final int KEEP_ALIVE = 15000;

    public static final String FILE_PATH = System.getProperty("user.home") + "/upload/alchemy/";

    public static final String GLOBAL_FILE_NAME = "global.jar";

    public static final String BIND_PREFIX = "";

    public static final String DESCRIPTOR_TYPE_KEY = "type";

    public static final String TYPE_VALUE_JAR = "jar";

    public static final String TYPE_VALUE_SOURCE = "source";

    public static final String TYPE_VALUE_TABLE = "table";

    public static final String TYPE_VALUE_UDF = "udf";

    public static final String TYPE_VALUE_FORMAT_JSON = "json";

    public static final String TYPE_VALUE_FORMAT_HESSIAN = "hessian";

    public static final String TYPE_VALUE_FORMAT_PB = "protostuff";

    public static final String TYPE_VALUE_FORMAT_GROK = "grok";

    public static final String CONNECTOR_TYPE_VALUE_KAFKA = "kafka-0.10";

    public static final String CONNECTOR_TYPE_VALUE_MYSQL = "mysql";

    public static final String CONNECTOR_TYPE_VALUE_ROCKETMQ = "rocketMQ";

    public static final String CONNECTOR_TYPE_VALUE_CSV = "csv";

    public static final String SINK_TYPE_VALUE_REDIS = "redis";

    public static final String SINK_TYPE_VALUE_ES = "elasticsearch5";

    public static final String SINK_TYPE_VALUE_HBASE = "hbase";

    public static final String SINK_TYPE_VALUE_KAFKA = "kafka-0.10";

    public static final String SINK_TYPE_VALUE_MYSQL = "mysql";

    public static final String SINK_TYPE_VALUE_ROCKETMQ = "rocketMQ";

    public static final String SINK_TYPE_VALUE_OPENTSDB = "tsdb";

    public static final String SINK_TYPE_VALUE_PRINT = "print";

    public static final String SINK_TYPE_VALUE_DUBBO = "dubbo";

    public static final String SINK_TYPE_VALUE_FILESYSTEM = "file";

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_.@A-Za-z0-9-]*$";

    public static final String SYSTEM_ACCOUNT = "system";
    public static final String ANONYMOUS_USER = "anonymoususer";
    public static final String DEFAULT_LANGUAGE = "en";

    /**
     * 重试次数
     */
    public static final int RESTART_ATTEMPTS = 3;

    /**
     * 重试间隔
     */
    public static final long DELAY_BETWEEN_ATTEMPTS = 10000;


    /**
     * Maximum number of restarts in given interval {@code failureInterval} before failing a job
     */
    public static final int FAILURE_RATE = 1;

    /**
     * Time interval for failures
     */
    public static final long FAILURE_INTERVAL = 60000;

    /**
     * Delay in-between restart attempts
     */
    public static final long DELAY_INTERVAL = 30000;

    /**
     * redis默认连接数
     */
    public static final int REDIS_DEFAULT_TOTAL = 10;

    /**
     * redis最大连接数
     */
    public static final int REDIS_MAX_TOTAL = 50;

    /**
     * redis默认队列
     */
    public static final int REDIS_DEFAULT_QUEUE_SIZE = 1000;

    /**
     * redis最大队列
     */
    public static final int REDIS_MAX_QUEUE_SIZE = 10000;

    /**
     * redis默认消费线程
     */
    public static final int REDIS_DEFAULT_THREAD_SIZE = 5;

    /**
     * redis最大消费线程
     */
    public static final int REDIS_MAX_THREAD_SIZE = 20;

    public static final String SUBMIT_LOCK_PREFIX = "al:submit:lock:";

    public static final int SUBMIT_LOCK_TIME = 300;

    public static final String STATUS_KEY = "al:status";

    public static final int STATUS_TIME = 24 * 60 * 60;

    public static final String STATUS_LOCK_PREFIX = "al:status:lock:";

    public static final int STATUS_LOCK_TIME = 120;

    public static final int PAGE_SIZE = 100;

    public static final String DEFAULT_FLINK_CLUSTER = "flink_default";
}
