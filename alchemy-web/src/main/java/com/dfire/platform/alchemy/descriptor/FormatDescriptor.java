package com.dfire.platform.alchemy.descriptor;

import java.util.Map;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.json.JsonRowDeserializationSchema;
import org.apache.flink.types.Row;
import org.springframework.util.Assert;

import com.dfire.platform.alchemy.common.Constants;
import com.dfire.platform.alchemy.formats.grok.GrokRowDeserializationSchema;
import com.dfire.platform.alchemy.formats.hessian.HessianRowDeserializationSchema;
import com.dfire.platform.alchemy.formats.protostuff.ProtostuffRowDeserializationSchema;

/**
 * @author congbai
 * @date 2018/6/30
 */
public class FormatDescriptor implements CoreDescriptor {

    public static final String KEY_CLASS_NAME = "className";

    public static final String KEY_REGULAR = "regular";

    public static final String KEY_SCHEMA = "schema";

    private String type;

    private Map<String, Object> properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String type() {
        return this.type;
    }

    @Override
    public void validate() throws Exception {
        Assert.notNull(type, "序列化类型不能为空");
    }

    @Override
    public String getName() {
        return "format";
    }

    @Override
    public <T> T transform() throws Exception {
        throw new UnsupportedOperationException("transform without param is not allowed here");
    }

    @Override
    public <T, R> T transform(R param) throws Exception {
        if (param == null || !(param instanceof TypeInformation)) {
            throw new IllegalArgumentException("format param is invalid");
        }
        TypeInformation<Row> typeInformation = (TypeInformation<Row>)param;
        String type = this.type;
        if (Constants.TYPE_VALUE_FORMAT_HESSIAN.equalsIgnoreCase(type)) {
            Class clazz = findClazz();
            return (T)new HessianRowDeserializationSchema(typeInformation, clazz);
        } else if (Constants.TYPE_VALUE_FORMAT_PB.equalsIgnoreCase(type)) {
            Class clazz = findClazz();
            return (T)new ProtostuffRowDeserializationSchema(typeInformation, clazz);
        } else if (Constants.TYPE_VALUE_FORMAT_GROK.equalsIgnoreCase(type)) {
            return (T)new JsonRowDeserializationSchema(type);
        } else if (Constants.TYPE_VALUE_FORMAT_JSON.equalsIgnoreCase(type)) {
            String regular = findRegular();
            return (T)new GrokRowDeserializationSchema(typeInformation, regular);
        }
        throw new UnsupportedOperationException("UnKnow format,type:" + this.type);
    }

    private Class<?> findClazz() throws ClassNotFoundException {
        Assert.isTrue(((this.properties != null) && this.properties.get(KEY_CLASS_NAME) != null), "序列化的className不能为空");
        return Class.forName(this.properties.get(KEY_CLASS_NAME).toString());
    }

    private String findRegular() {
        Assert.isTrue(((this.properties != null) && this.properties.get(KEY_REGULAR) != null), "正则表达式不能为空");
        return this.properties.get(KEY_REGULAR).toString();
    }
}