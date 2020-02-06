
package com.codeabovelab.dm.cluman.job;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;

/**
 */
class JobParametersDeserializer extends JsonDeserializer {

    private static JobsManager jobsManager;

    static void setJobsManager(JobsManager jobsManager) {
        // the ugly hack, because we need access to jobs types in runtime
        JobParametersDeserializer.jobsManager = jobsManager;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        if(jobsManager == null) {
            throw new IllegalStateException("This deserializer need a jobsManager instance, see 'JobParametersDeserializer.setJobsManager'");
        }
        final JsonStreamContext jsc = p.getParsingContext();
        String paramName = null;
        JsonStreamContext parent = jsc;
        while(parent != null) {
            paramName = parent.getCurrentName();
            if(paramName != null) {
                break;
            }
            parent = parent.getParent();
        }
        if(parent == null) {
            throw new NullPointerException("Something wrong: we can not find our parent object, " +
              "may be you use this deserializer on custom object?");
        }
        JobParameters.Builder r = (JobParameters.Builder) parent.getParent().getCurrentValue();
        String jobType = r.getType();
        JobDescription desc = jobsManager.getDescription(jobType);
        JobParameterDescription param = desc.getParameters().get(paramName);
        TypeFactory typeFactory = ctx.getTypeFactory();
        JavaType type;
        if(param == null) {
            type = typeFactory.constructType(Object.class);
        } else {
            type = typeFactory.constructType(param.getType());
        }
        JsonDeserializer<Object> deser = ctx.findNonContextualValueDeserializer(type);
        try {
            return deser.deserialize(p, ctx);
        } catch (Exception e) {
            throw new RuntimeException("Can not deserialize '" + jobType + "." + paramName + "' job parameter ", e );
        }
    }
}
