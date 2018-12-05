package web.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.flink.runtime.jobmanager.HighAvailabilityMode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import com.dfire.platform.alchemy.web.bind.BindPropertiesFactory;
import com.dfire.platform.alchemy.web.cluster.ClusterInfo;
import com.dfire.platform.alchemy.web.cluster.FlinkCluster;
import com.dfire.platform.alchemy.web.cluster.request.SqlSubmitFlinkRequest;
import com.dfire.platform.alchemy.web.cluster.response.Response;
import com.dfire.platform.alchemy.web.common.Constants;

/**
 * @author congbai
 * @date 06/06/2018
 */
public class FlinkClusterTest {

    private FlinkCluster cluster;

    @Before
    public void before() {
        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setName("test");
        clusterInfo.setClusterId("daily-default-8");
        clusterInfo.setAddress("10.1.21.95");
        clusterInfo.setMode(HighAvailabilityMode.ZOOKEEPER.toString().toLowerCase());
        clusterInfo.setPort(6123);
        clusterInfo.setAvg("com.dfire.platform:alchemy-connectors:0.0.1-SNAPSHOT");
        clusterInfo.setStoragePath("hdfs://sunset004.daily.2dfire.info:8020/flink/ha/real");
        clusterInfo.setZookeeperQuorum("10.1.22.21:2181,10.1.22.22:2181,10.1.22.23:2181");
        this.cluster = new FlinkCluster();
        cluster.start(clusterInfo);
    }

    @Test
    public void sendJar() {

    }

    @Test
    public void sendScalarSql() throws Exception {
        SqlSubmitFlinkRequest sqlSubmitRequest = createSqlRequest(
            "select scalarF(id) as id,CURRENT_DATE as createTime from kafka_table_test", "flinkClusterTest-ScalarSQL");
        Response resp = this.cluster.send(sqlSubmitRequest);
        assert resp.isSuccess();
    }

    @Test
    public void sendTableSql() throws Exception {
        SqlSubmitFlinkRequest sqlSubmitRequest = createSqlRequest(
            "SELECT s as body,id FROM kafka_table_test, LATERAL TABLE(tableF(id,999999988889)) as T(s)",
            "flinkClusterTest-TableSQL");
        Response resp = this.cluster.send(sqlSubmitRequest);
        assert resp.isSuccess();
    }

    @Test
    public void sendAggreSql() throws Exception {
        SqlSubmitFlinkRequest sqlSubmitRequest = createSqlRequest(
            "select aggreF(id) as id from kafka_table_test GROUP BY HOP(ptime, INTERVAL '10' SECOND, INTERVAL '1' SECOND)",
            "flinkClusterTest-AggreSQL");
        Response resp = this.cluster.send(sqlSubmitRequest);
        assert resp.isSuccess();
    }

    @Test
    public void rocketmq() throws Exception {
        SqlSubmitFlinkRequest sqlSubmitRequest
            = createSqlRequest("SELECT entityId,goodId, count(id) as cnt \n" + "FROM rocketmq_table_test\n"
                + "GROUP BY HOP(rideTime, INTERVAL '1' MINUTE,INTERVAL '1' MINUTE), entityId,goodId HAVING count(id) >1", "flinkClusterTest-rocketmq");
        sqlSubmitRequest.setTest(true);
        Response resp = this.cluster.send(sqlSubmitRequest);
        assert resp.isSuccess();
    }

    private SqlSubmitFlinkRequest createSqlRequest(String sql, String jobName) throws Exception {
        File file = ResourceUtils.getFile("classpath:config.yaml");
        SqlSubmitFlinkRequest sqlSubmitFlinkRequest = new SqlSubmitFlinkRequest();
        BindPropertiesFactory.bindProperties(sqlSubmitFlinkRequest, Constants.BIND_PREFIX, new FileInputStream(file));
        List<String> codes = new ArrayList<>();
        codes.add(createScalarUdfs());
        codes.add(createTableUdfs());
        codes.add(createAggreUdfs());
        codes.add(createHbaseCode());
        sqlSubmitFlinkRequest.getTable().setCodes(codes);
        sqlSubmitFlinkRequest.getTable().setSql(sql);
        sqlSubmitFlinkRequest.setJobName(jobName);
        // sqlSubmitFlinkRequest.setTest(true);
        return sqlSubmitFlinkRequest;
    }

    private String createScalarUdfs() {
        return "import com.dfire.platform.alchemy.api.function.StreamScalarFunction;\n" + "\n" + "/**\n"
            + " * @author congbai\n" + " * @date 06/06/2018\n" + " */\n"
            + "public class TestFunction implements StreamScalarFunction<String> {\n" + "\n" + "    @Override\n"
            + "    public  String invoke(Object... args) {\n" + "        String result=2222;\n"
            + "        return  String.valueOf(result);\n" + "    }\n" + "}\n";
    }

    private String createTableUdfs() {
        return "import com.dfire.platform.alchemy.api.function.StreamTableFunction;\n" + "\n" + "/**\n"
            + " * @author congbai\n" + " * @date 06/06/2018\n" + " */\n"
            + "public class TestTableFunction extends StreamTableFunction<String> {\n" + "\n" + "\n" + "    @Override\n"
            + "    public void invoke(Object... args) {\n" + "        for(Object arg:args){\n"
            + "            collect(String.valueOf(arg));\n" + "        }\n" + "    }\n" + "}\n";
    }

    private String createAggreUdfs() {
        return "import java.util.ArrayList;\n" + "import java.util.List;\n" + "\n"
            + "import com.dfire.platform.alchemy.api.function.StreamAggregateFunction;\n" + "\n" + "/**\n"
            + " * @author congbai\n" + " * @date 06/06/2018\n" + " */\n"
            + "public class TestAggreFunction implements StreamAggregateFunction<String, List, Integer> {\n" + "\n"
            + "    @Override\n" + "    public List createAccumulator() {\n" + "        return new ArrayList();\n"
            + "    }\n" + "\n" + "    @Override\n" + "    public void accumulate(List accumulator, String value) {\n"
            + "        accumulator.add(value);\n" + "    }\n" + "\n" + "    @Override\n"
            + "    public Integer getValue(List accumulator) {\n" + "        return accumulator.size();\n" + "    }\n"
            + "}\n";
    }

    private String createHbaseCode() {
        return "import com.dfire.platform.alchemy.api.sink.HbaseInvoker;\n" +
            "\n" +
            "/**\n" +
            " * @author congbai\n" +
            " * @date 07/06/2018\n" +
            " */\n" +
            "public class TestHbaseInvoke implements HbaseInvoker {\n" +
            "    @Override\n" +
            "    public String getRowKey(Object[] rows) {\n" +
            "        return String.valueOf(rows[0]);\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getFamily(Object[] rows) {\n" +
            "        return \"s\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getQualifier(Object[] rows) {\n" +
            "        return String.valueOf(rows[0]);\n" +
            "    }\n" +
            "}\n";
    }

}
