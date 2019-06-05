package com.dfire.platform.alchemy.descriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.descriptors.KafkaValidator;
import org.apache.flink.table.sources.tsextractors.ExistingField;
import org.apache.flink.table.sources.tsextractors.StreamRecordTimestamp;
import org.apache.flink.table.sources.tsextractors.TimestampExtractor;
import org.apache.flink.table.sources.wmstrategies.AscendingTimestamps;
import org.apache.flink.table.sources.wmstrategies.BoundedOutOfOrderTimestamps;
import org.apache.flink.table.sources.wmstrategies.PreserveWatermarks;
import org.apache.flink.table.sources.wmstrategies.WatermarkStrategy;
import org.apache.flink.table.typeutils.TypeStringUtils;
import org.apache.flink.types.Row;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.util.Assert;

import com.dfire.platform.alchemy.common.Constants;
import com.dfire.platform.alchemy.common.Field;
import com.dfire.platform.alchemy.common.TimeAttribute;
import com.dfire.platform.alchemy.common.Timestamps;
import com.dfire.platform.alchemy.common.Watermarks;
import com.dfire.platform.alchemy.connectors.kafka.AlchemyKafkaTableSource;
import com.dfire.platform.alchemy.util.PropertiesUtil;

/**
 * todo 事件事件、处理时间 --> 水位
 *
 * @author congbai
 * @date 03/06/2018
 */
public class KafkaConnectorDescriptor implements ConnectorDescriptor {

    private String topic;

    private String startupMode;

    private Map<String, String> specificOffsets;

    private Map<String, Object> properties;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getStartupMode() {
        return startupMode;
    }

    public void setStartupMode(String startupMode) {
        this.startupMode = startupMode;
    }

    public Map<String, String> getSpecificOffsets() {
        return specificOffsets;
    }

    public void setSpecificOffsets(Map<String, String> specificOffsets) {
        this.specificOffsets = specificOffsets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public void validate() throws Exception {
        Assert.notNull(topic, "kafka的topic不能为空");
        Assert.notNull(properties, "kafka的properties不能为空");
        Assert.notNull(PropertiesUtil.fromYamlMap(this.properties).get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG),
            "kafak的" + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG + "不能为空");

    }

    @Override
    public String type() {
        return Constants.CONNECTOR_TYPE_VALUE_KAFKA;
    }

    @Override
    public <T> T buildSource(List<Field> schema, FormatDescriptor format) throws Exception {
        return buildKafkaFlinkSource(schema, format);
    }

    private <T> T buildKafkaFlinkSource(List<Field> schema, FormatDescriptor format) throws Exception {
        AlchemyKafkaTableSource.Builder tableSourceBuilder = new AlchemyKafkaTableSource.Builder();
        TypeInformation<Row> returnType = createSchema(schema, tableSourceBuilder);
        DeserializationSchema<Row> deserializationSchema = format.transform(returnType);
        buildeProperties(tableSourceBuilder);
        tableSourceBuilder.withDeserializationSchema(deserializationSchema);
        return (T)tableSourceBuilder.build();
    }

    private <T> void buildeProperties(AlchemyKafkaTableSource.Builder builder) {
        builder.forTopic(this.topic);
        builder.withKafkaProperties(PropertiesUtil.fromYamlMap(this.properties));
        if (StringUtils.isNotEmpty(this.startupMode)) {
            switch (this.startupMode) {
                case KafkaValidator.CONNECTOR_STARTUP_MODE_VALUE_EARLIEST:
                    builder.fromEarliest();
                    break;

                case KafkaValidator.CONNECTOR_STARTUP_MODE_VALUE_LATEST:
                    builder.fromLatest();
                    break;

                case KafkaValidator.CONNECTOR_STARTUP_MODE_VALUE_GROUP_OFFSETS:
                    builder.fromGroupOffsets();
                    break;

                case KafkaValidator.CONNECTOR_STARTUP_MODE_VALUE_SPECIFIC_OFFSETS:
                    final Map<KafkaTopicPartition, Long> offsetMap = new HashMap<>();
                    for (Map.Entry<String, String> entry : this.specificOffsets.entrySet()) {
                        final KafkaTopicPartition topicPartition
                            = new KafkaTopicPartition(topic, Integer.parseInt(entry.getKey()));
                        offsetMap.put(topicPartition, Long.parseLong(entry.getValue()));
                    }
                    builder.fromSpecificOffsets(offsetMap);
                    break;
                default:

            }
        }
    }

    private <T> TypeInformation<Row> createSchema(List<Field> schema, AlchemyKafkaTableSource.Builder builder)
        throws ClassNotFoundException {
        if (CollectionUtils.isEmpty(schema)) {
            return null;
        }
        String[] columnNames = new String[schema.size()];
        TypeInformation[] columnTypes = new TypeInformation[schema.size()];
        for (int i = 0; i < schema.size(); i++) {
            columnNames[i] = schema.get(i).getName();
            TypeInformation typeInformation = TypeStringUtils.readTypeInfo(schema.get(i).getType());
            if (typeInformation == null) {
                throw new UnsupportedOperationException("Unsupported type:" + schema.get(i).getType());
            }
            columnTypes[i] = typeInformation;
            if (schema.get(i).isProctime()) {
                builder.withProctimeAttribute(schema.get(i).getName());
            } else {
                TimeAttribute timeAttribute = schema.get(i).getRowtime();
                if (timeAttribute == null) {
                    continue;
                }
                TimestampExtractor timestampExtractor
                    = Timestamps.Type.FIELD.getType().equals(timeAttribute.getTimestamps().getType())
                        ? new ExistingField(timeAttribute.getTimestamps().getFrom()) : new StreamRecordTimestamp();
                if (timeAttribute.getWatermarks() != null) {
                    WatermarkStrategy watermarkStrategy = null;
                    if (Watermarks.Type.PERIODIC_ASCENDING.getType().equals(timeAttribute.getWatermarks().getType())) {
                        watermarkStrategy = new AscendingTimestamps();
                    } else if (Watermarks.Type.PERIODIC_BOUNDED.getType()
                        .equals(timeAttribute.getWatermarks().getType())) {
                        watermarkStrategy = new BoundedOutOfOrderTimestamps(timeAttribute.getWatermarks().getDelay());
                    } else if (Watermarks.Type.FROM_SOURCE.getType().equals(timeAttribute.getWatermarks().getType())) {
                        watermarkStrategy = PreserveWatermarks.INSTANCE();
                    }
                    builder.withRowtimeAttribute(schema.get(i).getName(), timestampExtractor, watermarkStrategy);
                }
            }
        }
        TypeInformation<Row> returnType = new RowTypeInfo(columnTypes, columnNames);
        builder.withReturnType(returnType);
        builder.withSchema(new TableSchema(columnNames, columnTypes));
        return returnType;
    }
}