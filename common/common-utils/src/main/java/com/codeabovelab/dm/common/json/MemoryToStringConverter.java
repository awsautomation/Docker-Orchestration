
package com.codeabovelab.dm.common.json;

import com.codeabovelab.dm.common.utils.DataSize;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;


class MemoryToStringConverter implements Converter {
    @Override
    public Object convert(Object value) {
        return DataSize.toString((Long) value);
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return typeFactory.constructType(Long.class);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return typeFactory.constructType(String.class);
    }
}
