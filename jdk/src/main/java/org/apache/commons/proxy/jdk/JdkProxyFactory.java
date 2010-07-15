package org.apache.commons.proxy.jdk;

import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.Invoker;
import org.apache.commons.proxy.ObjectProvider;
import org.apache.commons.proxy.ProxyUtils;
import org.apache.commons.proxy.impl.AbstractProxyFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JdkProxyFactory extends AbstractProxyFactory
{    
//**********************************************************************************************************************
// ProxyFactory Implementation
//**********************************************************************************************************************

    
    /**
     * Creates a proxy which delegates to the object provided by <code>delegateProvider</code>.
     *
     * @param classLoader      the class loader to use when generating the proxy
     * @param delegateProvider the delegate provider
     * @param proxyClasses     the interfaces that the proxy should implement
     * @return a proxy which delegates to the object provided by the target <code>delegateProvider>
     */
    public Object createDelegatorProxy( ClassLoader classLoader, ObjectProvider delegateProvider,
                                        Class... proxyClasses )
    {
        return Proxy.newProxyInstance(classLoader, proxyClasses,
                                      new DelegatorInvocationHandler(delegateProvider));
    }

    /**
     * Creates a proxy which passes through a {@link Interceptor interceptor} before eventually reaching the
     * <code>target</code> object.
     *
     * @param classLoader  the class loader to use when generating the proxy
     * @param target       the target object
     * @param interceptor  the method interceptor
     * @param proxyClasses the interfaces that the proxy should implement.
     * @return a proxy which passes through a {@link Interceptor interceptor} before eventually reaching the
     *         <code>target</code> object.
     */
    public Object createInterceptorProxy( ClassLoader classLoader, Object target, Interceptor interceptor,
                                          Class... proxyClasses )
    {
        return Proxy
                .newProxyInstance(classLoader, proxyClasses, new InterceptorInvocationHandler(target, interceptor));
    }

    /**
     * Creates a proxy which uses the provided {@link Invoker} to handle all method invocations.
     *
     * @param classLoader  the class loader to use when generating the proxy
     * @param invoker      the invoker
     * @param proxyClasses the interfaces that the proxy should implement
     * @return a proxy which uses the provided {@link Invoker} to handle all method invocations
     */
    public Object createInvokerProxy( ClassLoader classLoader, Invoker invoker,
                                      Class... proxyClasses )
    {
        return Proxy.newProxyInstance(classLoader, proxyClasses, new InvokerInvocationHandler(invoker));
    }

//**********************************************************************************************************************
// Inner Classes
//**********************************************************************************************************************

    private abstract static class AbstractInvocationHandler implements InvocationHandler, Serializable
    {
        public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
        {
            if( isHashCode(method) )
            {
                return System.identityHashCode(proxy);
            }
            else if( isEqualsMethod(method) )
            {
                return proxy == args[0];
            }
            else
            {
                return invokeImpl(proxy, method, args);
            }
        }

        protected abstract Object invokeImpl( Object proxy, Method method, Object[] args ) throws Throwable;
    }

    private static class DelegatorInvocationHandler extends AbstractInvocationHandler
    {
        private final ObjectProvider delegateProvider;

        protected DelegatorInvocationHandler( ObjectProvider delegateProvider )
        {
            this.delegateProvider = delegateProvider;
        }

        public Object invokeImpl( Object proxy, Method method, Object[] args ) throws Throwable
        {
            try
            {
                return method.invoke(delegateProvider.getObject(), args);
            }
            catch( InvocationTargetException e )
            {
                throw e.getTargetException();
            }
        }
    }

    private static class InterceptorInvocationHandler extends AbstractInvocationHandler
    {
        private final Object target;
        private final Interceptor methodInterceptor;

        public InterceptorInvocationHandler( Object target, Interceptor methodInterceptor )
        {
            this.target = target;
            this.methodInterceptor = methodInterceptor;
        }

        public Object invokeImpl( Object proxy, Method method, Object[] args ) throws Throwable
        {
            final ReflectionInvocation invocation = new ReflectionInvocation(target, method, args);
            return methodInterceptor.intercept(invocation);
        }
    }

    private static class InvokerInvocationHandler extends AbstractInvocationHandler
    {
        private final Invoker invoker;

        public InvokerInvocationHandler( Invoker invoker )
        {
            this.invoker = invoker;
        }

        public Object invokeImpl( Object proxy, Method method, Object[] args ) throws Throwable
        {
            return invoker.invoke(proxy, method, args);
        }
    }

    private static class ReflectionInvocation implements Invocation, Serializable
    {
        private final Method method;
        private final Object[] arguments;
        private final Object target;

        public ReflectionInvocation( Object target, Method method, Object[] arguments )
        {
            this.method = method;
            this.arguments = ( arguments == null ? ProxyUtils.EMPTY_ARGUMENTS : arguments );
            this.target = target;
        }

        public Object[] getArguments()
        {
            return arguments;
        }

        public Method getMethod()
        {
            return method;
        }

        public Object getProxy()
        {
            return target;
        }

        public Object proceed() throws Throwable
        {
            try
            {
                return method.invoke(target, arguments);
            }
            catch( InvocationTargetException e )
            {
                throw e.getTargetException();
            }
        }
    }
}
