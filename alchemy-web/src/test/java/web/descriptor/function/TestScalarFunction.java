package web.descriptor.function;

import org.apache.commons.lang3.StringUtils;

import com.dfire.platform.alchemy.function.StreamScalarFunction;

/**
 * @author congbai
 * @date 07/06/2018
 */
public class TestScalarFunction implements StreamScalarFunction<String> {
    @Override
    public String invoke(Object... args) {
        return StringUtils.join(args);
    }

}
