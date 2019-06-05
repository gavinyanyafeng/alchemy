package com.dfire.platform.alchemy.descriptor;

import com.dfire.platform.alchemy.domain.Sink;

/**
 * @author congbai
 * @date 03/06/2018
 */
public abstract class SinkDescriptor implements CoreDescriptor {

    public static SinkDescriptor from(Sink sink) {
        SinkDescriptor descriptor
            = DescriptorFactory.me.find(sink.getType().toString().toLowerCase(), SinkDescriptor.class);
        if (descriptor == null) {
            throw new UnsupportedOperationException("Unknow sink type:" + sink.getType());
        }
        return descriptor;
    }
}