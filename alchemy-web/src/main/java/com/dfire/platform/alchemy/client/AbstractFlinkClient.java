package com.dfire.platform.alchemy.client;

import static com.dfire.platform.alchemy.client.request.SqlSubmitFlinkRequest.CONFIG_KEY_DELAY_BETWEEN_ATTEMPTS;
import static com.dfire.platform.alchemy.client.request.SqlSubmitFlinkRequest.CONFIG_KEY_DELAY_INTERVAL;
import static com.dfire.platform.alchemy.client.request.SqlSubmitFlinkRequest.CONFIG_KEY_FAILURE_INTERVAL;
import static com.dfire.platform.alchemy.client.request.SqlSubmitFlinkRequest.CONFIG_KEY_FAILURE_RATE;
import static com.dfire.platform.alchemy.client.request.SqlSubmitFlinkRequest.CONFIG_KEY_RESTART_ATTEMPTS;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobSubmissionResult;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.JobWithJars;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.optimizer.DataStatistics;
import org.apache.flink.optimizer.Optimizer;
import org.apache.flink.optimizer.costs.DefaultCostEstimator;
import org.apache.flink.optimizer.plan.FlinkPlan;
import org.apache.flink.runtime.jobgraph.JobStatus;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.flink.runtime.messages.Acknowledge;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.functions.AggregateFunction;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.functions.TableFunction;
import org.apache.flink.table.shaded.org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.table.sources.TableSource;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import com.dfire.platform.alchemy.api.common.Alias;
import com.dfire.platform.alchemy.api.util.SideParser;
import com.dfire.platform.alchemy.client.request.CancelFlinkRequest;
import com.dfire.platform.alchemy.client.request.JarSubmitFlinkRequest;
import com.dfire.platform.alchemy.client.request.JobStatusRequest;
import com.dfire.platform.alchemy.client.request.RescaleFlinkRequest;
import com.dfire.platform.alchemy.client.request.SavepointFlinkRequest;
import com.dfire.platform.alchemy.client.request.SqlSubmitFlinkRequest;
import com.dfire.platform.alchemy.client.response.JobStatusResponse;
import com.dfire.platform.alchemy.client.response.Response;
import com.dfire.platform.alchemy.client.response.SubmitFlinkResponse;
import com.dfire.platform.alchemy.common.Constants;
import com.dfire.platform.alchemy.common.ResultMessage;
import com.dfire.platform.alchemy.descriptor.SinkDescriptor;
import com.dfire.platform.alchemy.descriptor.SourceDescriptor;
import com.dfire.platform.alchemy.domain.enumeration.TableType;
import com.dfire.platform.alchemy.function.BaseFunction;
import com.dfire.platform.alchemy.service.util.SqlParseUtil;
import com.dfire.platform.alchemy.util.AlchemyProperties;
import com.dfire.platform.alchemy.util.FileUtil;
import com.dfire.platform.alchemy.util.JarArgUtil;
import com.dfire.platform.alchemy.util.MavenJarUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author congbai
 * @date 2019/6/10
 */
public abstract class AbstractFlinkClient implements FlinkClient {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * 集群额外依赖的公共包
     */
    private final List<String> avgs;

    public AbstractFlinkClient(List<String> avgs) {
        this.avgs = avgs;
    }

    public Response cancel(ClusterClient clusterClient, CancelFlinkRequest request) throws Exception {
        if(StringUtils.isEmpty(request.getJobID())){
            return new Response("the job is not submit yet");
        }
        boolean savePoint = request.getSavePoint() != null && request.getSavePoint().booleanValue();
        if (savePoint) {
            String path = clusterClient.cancelWithSavepoint(JobID.fromHexString(request.getJobID()),
                request.getSavepointDirectory());
            return new Response(true, path);
        } else {
            clusterClient.cancel(JobID.fromHexString(request.getJobID()));
            return new Response(true);
        }
    }

    public Response rescale(ClusterClient clusterClient, RescaleFlinkRequest request) throws Exception {
        if(StringUtils.isEmpty(request.getJobID())){
            return new Response("the job is not submit yet");
        }
        CompletableFuture<Acknowledge> future
            = clusterClient.rescaleJob(JobID.fromHexString(request.getJobID()), request.getNewParallelism());;
        future.get();
        return new Response(true);
    }

    public Response savepoint(ClusterClient clusterClient, SavepointFlinkRequest request) throws Exception {
        if(StringUtils.isEmpty(request.getJobID())){
            return new Response("the job is not submit yet");
        }
        CompletableFuture<String> future
            = clusterClient.triggerSavepoint(JobID.fromHexString(request.getJobID()), request.getSavepointDirectory());
        return new Response(true, future.get());
    }

    public JobStatusResponse status(ClusterClient clusterClient, JobStatusRequest request) throws Exception {
        if(StringUtils.isEmpty(request.getJobID())){
            return new JobStatusResponse("the job is not submit yet");
        }
        CompletableFuture<JobStatus> jobStatusCompletableFuture
            = clusterClient.getJobStatus(JobID.fromHexString(request.getJobID()));
        // jobStatusCompletableFuture.
        switch (jobStatusCompletableFuture.get()) {
            case CREATED:
                return new JobStatusResponse(true, com.dfire.platform.alchemy.domain.enumeration.JobStatus.SUBMIT);
            case RESTARTING:
                break;
            case RUNNING:
                return new JobStatusResponse(true, com.dfire.platform.alchemy.domain.enumeration.JobStatus.RUNNING);
            case FAILING:
            case FAILED:
                return new JobStatusResponse(true, com.dfire.platform.alchemy.domain.enumeration.JobStatus.FAILED);
            case CANCELLING:
            case CANCELED:
                return new JobStatusResponse(true, com.dfire.platform.alchemy.domain.enumeration.JobStatus.CANCELED);
            case FINISHED:
                return new JobStatusResponse(true, com.dfire.platform.alchemy.domain.enumeration.JobStatus.FINISHED);
            case SUSPENDED:
            case RECONCILING:
            default:
                // nothing to do
        }
        return new JobStatusResponse(null);
    }

    public SubmitFlinkResponse submitJar(ClusterClient clusterClient, JarSubmitFlinkRequest request) throws Exception {
        if (request.isTest()) {
            throw new UnsupportedOperationException();
        }
        LOGGER.trace("start submit jar request,entryClass:{}", request.getEntryClass());
        try {
            File file = MavenJarUtil.forAvg(request.getAvg()).getJarFile();
            List<String> programArgs = JarArgUtil.tokenizeArguments(request.getProgramArgs());
            PackagedProgram program = new PackagedProgram(file, request.getEntryClass(),
                programArgs.toArray(new String[programArgs.size()]));
            ClassLoader classLoader = null;
            try {
                classLoader = program.getUserCodeClassLoader();
            } catch (Exception e) {
                LOGGER.warn(e.getMessage());
            }

            Optimizer optimizer = new Optimizer(new DataStatistics(), new DefaultCostEstimator(), new Configuration());
            FlinkPlan plan = ClusterClient.getOptimizedPlan(optimizer, program, request.getParallelism());
            // Savepoint restore settings
            SavepointRestoreSettings savepointSettings = SavepointRestoreSettings.none();
            String savepointPath = request.getSavepointPath();
            if (StringUtils.isNotEmpty(savepointPath)) {
                Boolean allowNonRestoredOpt = request.getAllowNonRestoredState();
                boolean allowNonRestoredState = allowNonRestoredOpt != null && allowNonRestoredOpt.booleanValue();
                savepointSettings = SavepointRestoreSettings.forPath(savepointPath, allowNonRestoredState);
            }
            // set up the execution environment
            List<URL> jarFiles = FileUtil.createPath(file);
            JobSubmissionResult submissionResult
                = clusterClient.run(plan, jarFiles, Collections.emptyList(), classLoader, savepointSettings);
            LOGGER.trace(" submit jar request sucess,jobId:{}", submissionResult.getJobID());
            return new SubmitFlinkResponse(true, submissionResult.getJobID().toString());
        } catch (Exception e) {
            String term = e.getMessage() == null ? "." : (": " + e.getMessage());
            LOGGER.error(" submit jar request fail", e);
            return new SubmitFlinkResponse(term);
        }
    }

    public SubmitFlinkResponse submitSql(ClusterClient clusterClient, SqlSubmitFlinkRequest request) throws Exception {
        LOGGER.trace("start submit sql request,jobName:{},sql:{}", request.getJobName(), request.getSqls().toArray());
        if (CollectionUtils.isEmpty(request.getSources())) {
            return new SubmitFlinkResponse(ResultMessage.SOURCE_EMPTY.getMsg());
        }
        if (CollectionUtils.isEmpty(request.getSinks())) {
            return new SubmitFlinkResponse(ResultMessage.SINK_EMPTY.getMsg());
        }
        if (CollectionUtils.isEmpty(request.getSqls())) {
            return new SubmitFlinkResponse(ResultMessage.SQL_EMPTY.getMsg());
        }
        final StreamExecutionEnvironment execEnv = StreamExecutionEnvironment.createLocalEnvironment();
        StreamTableEnvironment env = StreamTableEnvironment.getTableEnvironment(execEnv);
        List<URL> urls = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(request.getAvgs())) {
            // 先加入外部依赖，方便发现函数的class
            urls.addAll(FileUtil.createPath(request.getAvgs()));
        }
        Map<String, SourceDescriptor> sideSources = Maps.newHashMap();
        Map<String, TableSource> tableSources = Maps.newHashMap();
        setBaseInfo(execEnv, request);
        registerSource(env, request, urls, tableSources, sideSources);
        registerFunction(env, request, urls);
        for (String sql : request.getSqls()) {
            String sink = SqlParseUtil.parseSinkName(sql);
            Table table = registerSql(env, sql, tableSources, sideSources);
            registerSink(table, request, sink, urls);
        }
        if (request.isTest()) {
            execEnv.execute(request.getJobName());
            return new SubmitFlinkResponse(true, "");
        }
        StreamGraph streamGraph = execEnv.getStreamGraph();
        streamGraph.setJobName(request.getJobName());
        urls.addAll(createGlobalPath(this.getAvgs()));
        ClassLoader usercodeClassLoader
            = JobWithJars.buildUserCodeClassLoader(urls, Collections.emptyList(), getClass().getClassLoader());
        try {
            JobSubmissionResult submissionResult
                = clusterClient.run(streamGraph, urls, Collections.emptyList(), usercodeClassLoader);
            LOGGER.trace(" submit sql request success,jobId:{}", submissionResult.getJobID());
            return new SubmitFlinkResponse(true, submissionResult.getJobID().toString());
        } catch (Exception e) {
            String term = e.getMessage() == null ? "." : (": " + e.getMessage());
            LOGGER.error(" submit sql request fail", e);
            return new SubmitFlinkResponse(term);
        }
    }

    private void registerSink(Table table, SqlSubmitFlinkRequest request, String sink, List<URL> urls)
        throws Exception {
        if (request.getSinks() == null) {
            throw new IllegalArgumentException("table sink can'be null");
        }
        SinkDescriptor sinkDescriptor = findSink(request.getSinks(), sink);
        TableSink tableSink = sinkDescriptor.transform();
        table.writeToSink(tableSink);
        addUrl(sinkDescriptor.type(), urls);
    }

    private SinkDescriptor findSink(List<SinkDescriptor> sinks, String sink) {
        for (SinkDescriptor sinkDescriptor : sinks) {
            if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(sinkDescriptor.getName(), sink)) {
                return sinkDescriptor;
            }
        }
        throw new IllegalArgumentException("can’t find the sink:" + sink);
    }

    private Table registerSql(StreamTableEnvironment env, String sql, Map<String, TableSource> tableSources,
        Map<String, SourceDescriptor> sideSources) throws Exception {
        if (sideSources.isEmpty()) {
            return env.sqlQuery(sql);
        }
        Deque<SqlNode> deque = SideParser.parse(sql);
        SqlNode last;
        SqlSelect modifyNode = null;
        SqlNode fullNode = deque.peekFirst();
        while ((last = deque.pollLast()) != null) {
            if (modifyNode != null) {
                SideParser.rewrite(last, modifyNode);
                modifyNode = null;
            }
            if (last.getKind() == SqlKind.SELECT) {
                SqlSelect sqlSelect = (SqlSelect)last;
                SqlNode selectFrom = sqlSelect.getFrom();
                if (SqlKind.JOIN != selectFrom.getKind()) {
                    continue;
                }
                SqlJoin sqlJoin = (SqlJoin)selectFrom;
                Alias sideAlias = SideParser.getTableName(sqlJoin.getRight());
                Alias leftAlias = SideParser.getTableName(sqlJoin.getLeft());
                if (isSide(sideSources.keySet(), leftAlias.getTable())) {
                    throw new UnsupportedOperationException("side table must be right table");
                }
                if (!isSide(sideSources.keySet(), sideAlias.getTable())) {
                    continue;
                }
                DataStream<Row> dataStream = SideStream.buildStream(env, sqlSelect, leftAlias, sideAlias,
                    sideSources.get(sideAlias.getTable()));
                Alias newTable = new Alias(leftAlias.getTable() + "_" + sideAlias.getTable(),
                    leftAlias.getAlias() + "_" + sideAlias.getAlias());
                if (!env.isRegistered(newTable.getTable())) {
                    env.registerDataStream(newTable.getTable(), dataStream);
                }
                SqlSelect newSelect
                    = SideParser.newSelect(sqlSelect, newTable.getTable(), newTable.getAlias(), false, true);
                modifyNode = newSelect;
            }
        }
        if (modifyNode != null) {
            return env.sqlQuery(modifyNode.toString());
        } else {
            return env.sqlQuery(fullNode.toString());
        }

    }

    private boolean isSide(Set<String> keySet, String table) {
        for (String side : keySet) {
            if (side.equalsIgnoreCase(table)) {
                return true;
            }
        }
        return false;
    }

    private void registerFunction(StreamTableEnvironment env, SqlSubmitFlinkRequest request, List<URL> urls) {
        // 加载公共function
        List<String> functionNames = Lists.newArrayList();
        loadFunction(env, functionNames, ServiceLoader.load(BaseFunction.class));
        if (CollectionUtils.isNotEmpty(request.getUdfs())) {
            request.getUdfs().forEach(udfDescriptor -> {
                try {
                    loadUrl(udfDescriptor.getAvg(), urls);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        if (request.getUdfs() == null) {
            return;
        }
        // 加载自定义函数
        URLClassLoader urlClassLoader
            = new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader());
        request.getUdfs().forEach(udfDescriptor -> {
            try {

                Object udf = udfDescriptor.transform(urlClassLoader);
                register(env, udfDescriptor.getName(), udf);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }

    private void loadFunction(StreamTableEnvironment env, List<String> functionNames,
        ServiceLoader<BaseFunction> serviceLoader) {
        Iterator<BaseFunction> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            BaseFunction function = iterator.next();
            if (org.apache.commons.lang3.StringUtils.isEmpty(function.getFunctionName())
                || functionNames.contains(function.getFunctionName())) {
                continue;
            }
            functionNames.add(function.getFunctionName());
            register(env, function.getFunctionName(), function);
        }
    }

    private void register(StreamTableEnvironment env, String name, Object function) {
        if (function instanceof TableFunction) {
            env.registerFunction(name, (TableFunction)function);
        } else if (function instanceof AggregateFunction) {
            env.registerFunction(name, (AggregateFunction)function);
        } else if (function instanceof ScalarFunction) {
            env.registerFunction(name, (ScalarFunction)function);
        } else {
            throw new RuntimeException("Unknown UDF {} was found." + name);
        }
    }

    private void registerSource(StreamTableEnvironment env, SqlSubmitFlinkRequest request, List<URL> urls,
        Map<String, TableSource> tableSources, Map<String, SourceDescriptor> sideSources) {
        request.getSources().forEach(consumer -> {
            try {
                TableType tableType = consumer.getTableType();
                switch (tableType) {
                    case SIDE:
                        sideSources.put(consumer.getName(), consumer);
                        break;
                    case VIEW:
                        String sql = consumer.getSql();
                        Table table = registerSql(env, sql, tableSources, sideSources);
                        env.registerTable(consumer.getName(), table);
                        break;
                    case TABLE:
                        TableSource tableSource = consumer.transform();
                        addUrl(consumer.getConnectorDescriptor().type(), urls);
                        if (consumer.getFormat() != null) {
                            addUrl(consumer.getFormat().type(), urls);
                        }
                        env.registerTableSource(consumer.getName(), tableSource);
                        tableSources.put(consumer.getName(), tableSource);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknow tableType" + tableType);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setBaseInfo(StreamExecutionEnvironment execEnv, SqlSubmitFlinkRequest request) {
        execEnv.setParallelism(request.getParallelism());
        if (request.getMaxParallelism() != null) {
            execEnv.setMaxParallelism(request.getMaxParallelism());
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(request.getTimeCharacteristic())) {
            execEnv.setStreamTimeCharacteristic(TimeCharacteristic.valueOf(request.getTimeCharacteristic()));
        } else {
            execEnv.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
        }
        if (request.getBufferTimeout() != null) {
            execEnv.setBufferTimeout(request.getBufferTimeout());
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(request.getRestartStrategies())) {
            String strategies = request.getRestartStrategies();
            com.dfire.platform.alchemy.common.RestartStrategies restartStrategies
                = com.dfire.platform.alchemy.common.RestartStrategies.valueOf(strategies.toUpperCase());
            Map<String, Object> restartParams = request.getRestartParams();
            switch (restartStrategies) {
                case NO:
                    execEnv.setRestartStrategy(RestartStrategies.noRestart());
                    break;
                case FIXED:
                    Integer restartAttempts = restartParams == null ? Constants.RESTART_ATTEMPTS
                        : Integer.valueOf(restartParams.get(CONFIG_KEY_RESTART_ATTEMPTS).toString());
                    Long delayBetweenAttempts = restartParams == null ? Constants.DELAY_BETWEEN_ATTEMPTS
                        : Long.valueOf(restartParams.get(CONFIG_KEY_DELAY_BETWEEN_ATTEMPTS).toString());
                    execEnv
                        .setRestartStrategy(RestartStrategies.fixedDelayRestart(restartAttempts, delayBetweenAttempts));
                    break;
                case FAILURE:
                    Integer failureRate = restartParams == null ? Constants.FAILURE_RATE
                        : Integer.valueOf(restartParams.get(CONFIG_KEY_FAILURE_RATE).toString());
                    Long failureInterval = restartParams == null ? Constants.FAILURE_INTERVAL
                        : Long.valueOf(restartParams.get(CONFIG_KEY_FAILURE_INTERVAL).toString());
                    Long delayInterval = restartParams == null ? Constants.DELAY_INTERVAL
                        : Long.valueOf(restartParams.get(CONFIG_KEY_DELAY_INTERVAL).toString());
                    execEnv.setRestartStrategy(RestartStrategies.failureRateRestart(failureRate,
                        Time.of(failureInterval, TimeUnit.MILLISECONDS),
                        Time.of(delayInterval, TimeUnit.MILLISECONDS)));
                    break;
                case FALLBACK:
                    execEnv.setRestartStrategy(RestartStrategies.fallBackRestart());
                    break;
                default:
            }
        }
        if (request.getCheckpointCfg() != null) {
            CheckpointConfig checkpointConfig = execEnv.getCheckpointConfig();
            BeanUtils.copyProperties(request.getCheckpointCfg(), checkpointConfig);
        }

    }

    private void loadUrl(String avg, List<URL> urls) throws MalformedURLException {
        if (org.apache.commons.lang3.StringUtils.isEmpty(avg)) {
            return;
        }
        URL url = MavenJarUtil.forAvg(avg).getJarFile().getAbsoluteFile().toURI().toURL();
        if (!urls.contains(url)) {
            urls.add(url);
        }
    }

    private void addUrl(String name, List<URL> urls) throws MalformedURLException {
        if (org.apache.commons.lang3.StringUtils.isEmpty(name)) {
            return;
        }
        String avg = AlchemyProperties.get(name);
        if (org.apache.commons.lang3.StringUtils.isEmpty(avg)) {
            LOGGER.info("{} is not exist  in alchemy properties", name);
            return;
        }
        URL url = MavenJarUtil.forAvg(avg).getJarFile().getAbsoluteFile().toURI().toURL();
        if (!urls.contains(url)) {
            urls.add(url);
        }
    }

    private List<URL> createGlobalPath(List<String> avgs) throws MalformedURLException {
        return FileUtil.createPath(avgs);
    }

    public List<String> getAvgs() {
        return avgs;
    }
}
