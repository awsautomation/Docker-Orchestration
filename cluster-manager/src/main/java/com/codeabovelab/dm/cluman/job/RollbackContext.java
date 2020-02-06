
package com.codeabovelab.dm.cluman.job;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 */
public class RollbackContext {
    //see ScopedProxyUtils.getTargetBeanName
    private static final String SCOPED_TARGET = "scopedTarget.";
    private final ListableBeanFactory beanFactory;
    private final JobsManager manager;
    private final JobContext jobContext;

    RollbackContext(ListableBeanFactory beanFactory, JobsManager manager, JobContext jobContext) {
        this.beanFactory = beanFactory;
        this.manager = manager;
        this.jobContext = jobContext;
    }

    public JobsManager getManager() {
        return manager;
    }

    public JobContext getContext() {
        return jobContext;
    }

    public <T> T getBean(Class<T> type) {
        return beanFactory.getBean(type);
    }

    public <T> T getBean(Class<T> type, String name) {
        return beanFactory.getBean(name, type);
    }


    /**
     * A {@link #setBean(Object, Class)} variant, for cases when bean definition point to implementation type.
     * Equal code: <code>setBean(bean, bean.getClass())</code>
     * @param bean
     */
    public void setBean(final Object bean) {
        setBean(bean, bean.getClass());
    }
    /**
     * Put specified bean into context
     * @param type type of bean, note that it not always same as bean type, usual it super
     *             type of bean, for example: bean is a 'class SomeImpl' but type is 'interface Some'
     * @param bean instance of bean
     */
    public void setBean(final Object bean, Class<?> type) {
        Assert.notNull(type, "type is null");
        Assert.notNull(bean, "bean is null");
        String name = resolveName(type);
        Scope scope = beanFactory.findAnnotationOnBean(name, Scope.class);
        String scopeName = (String) AnnotationUtils.getValue(scope);
        ScopeBeans beans ;
        if(JobScopeIteration.SCOPE_NAME.equals(scopeName)) {
            beans = JobScopeIteration.getBeans();
        } else {
            beans = jobContext.getScopeBeans();
        }
        beans.putBean(name, bean);
    }

    private String resolveName(Class<?> type) {
        String[] names = beanFactory.getBeanNamesForType(type);
        Assert.isTrue(names.length != 0, "Can not find bean names for " + type);
        String name = null;
        for(String potentialName: names) {
            if(isScopedVariant(name, potentialName)) {
                // we prefer name with scopeTarget prefix
                name = potentialName;
                continue;
            }
            if(name != null) {
                if(isScopedVariant(potentialName, name)) {
                    // if potentialName a variant of current + prefix, then we simply skip it
                    continue;
                }
                throw new IllegalStateException("Can not resolve appropriate bean: " + name + " and " + potentialName);
            }
            name = potentialName;
        }
        return name;
    }

    /**
     * Check that second value is a scoped variant (differ only by prefix) of first argument.
     * @param original name
     * @param scoped name
     * @return true if '{@link #SCOPED_TARGET} + original  == scoped'
     */
    private boolean isScopedVariant(String original, String scoped) {
        if(original == null || scoped == null) {
            return false;
        }
        return scoped.startsWith(SCOPED_TARGET) && scoped.regionMatches(SCOPED_TARGET.length(), original, 0, original.length());
    }
}
