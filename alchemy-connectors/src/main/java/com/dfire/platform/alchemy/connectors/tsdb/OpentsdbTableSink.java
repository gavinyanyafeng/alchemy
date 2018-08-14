package com.dfire.platform.alchemy.connectors.tsdb;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.table.sinks.AppendStreamTableSink;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;

/**
 * @author congbai
 * @date 2018/7/10
 */
public class OpentsdbTableSink implements AppendStreamTableSink<Row> {

    private final OpentsdbProperties opentsdbProperties;

    private final String code;

    private String[] fieldNames;

    private TypeInformation[] fieldTypes;

    public OpentsdbTableSink(OpentsdbProperties opentsdbProperties, String code) {
        this.opentsdbProperties = Preconditions.checkNotNull(opentsdbProperties, "opentsdbProperties");;
        this.code = code;
    }

    @Override
    public String[] getFieldNames() {
        return fieldNames;
    }

    @Override
    public TypeInformation<?>[] getFieldTypes() {
        return fieldTypes;
    }

    @Override
    public TypeInformation<Row> getOutputType() {
        return new RowTypeInfo(this.fieldTypes);
    }

    @Override
    public TableSink<Row> configure(String[] fieldNames, TypeInformation<?>[] fieldTypes) {
        OpentsdbTableSink copy = new OpentsdbTableSink(this.opentsdbProperties, this.code);
        copy.fieldNames = Preconditions.checkNotNull(fieldNames, "fieldNames");
        copy.fieldTypes = Preconditions.checkNotNull(fieldTypes, "fieldTypes");
        Preconditions.checkArgument(fieldNames.length == fieldTypes.length,
            "Number of provided field names and types does not match.");
        return copy;
    }

    @Override
    public void emitDataStream(DataStream<Row> dataStream) {
        RichSinkFunction richSinkFunction = createTsdbRich();
        dataStream.addSink(richSinkFunction);
    }

    private RichSinkFunction createTsdbRich() {
        return new OpentsdbSinkFunction(this.opentsdbProperties, this.code);
    }
}
