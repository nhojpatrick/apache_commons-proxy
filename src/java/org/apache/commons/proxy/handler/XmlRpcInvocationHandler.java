/* $Id$
 *
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.proxy.handler;

import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.commons.proxy.exception.InvocationHandlerException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Vector;

/**
 * @author James Carman
 * @version 1.0
 */
public class XmlRpcInvocationHandler implements InvocationHandler
{
    private final XmlRpcHandler handler;
    private final String handlerName;

    public XmlRpcInvocationHandler( XmlRpcHandler handler, String handlerName )
    {
        this.handler = handler;
        this.handlerName = handlerName;
    }

    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
    {
        final Object returnValue = handler.execute( handlerName + "." + method.getName(), toArgumentVector( args ) );
        if( returnValue instanceof XmlRpcException )
        {
            throw new InvocationHandlerException( "Unable to execute XML-RPC call.", ( XmlRpcException )returnValue );

        }
        return returnValue;
    }

    private Vector toArgumentVector( Object[] args )
    {
        final Vector v = new Vector();
        for( int i = 0; i < args.length; i++ )
        {
            Object arg = args[i];
            v.addElement( arg );
        }
        return v;
    }
}