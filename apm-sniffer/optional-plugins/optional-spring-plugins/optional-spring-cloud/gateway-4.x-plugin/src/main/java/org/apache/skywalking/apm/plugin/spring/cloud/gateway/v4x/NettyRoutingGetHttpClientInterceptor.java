/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v4x;

import io.netty.handler.codec.http.HttpMethod;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.spring.cloud.gateway.v4x.define.EnhanceObjectCache;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientConfig;

import java.lang.reflect.Method;

/**
 * This class intercept <code>uri</code> method to get the url of downstream service
 */
public class NettyRoutingGetHttpClientInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            MethodInterceptResult result) throws Throwable {
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
            Object ret) throws Throwable {
        ServerWebExchange exchange = (ServerWebExchange) allArguments[1];
        ServerHttpRequest request = exchange.getRequest();

        final HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().name());
        //复制一个 避免线程冲突
        HttpClient retDup = (HttpClient)((HttpClient)ret).request(httpMethod);
        Object configuration = retDup.configuration();

//        if (retDup instanceof EnhancedInstance) {
            EnhanceObjectCache enhanceObjectCache = (EnhanceObjectCache) ((EnhancedInstance) configuration)
                    .getSkyWalkingDynamicField();
            EnhancedInstance enhancedInstance = getInstance(allArguments[1]);
//            if (enhancedInstance != null && enhancedInstance.getSkyWalkingDynamicField() != null) {
                enhanceObjectCache.setSnapshot((ContextSnapshot) enhancedInstance.getSkyWalkingDynamicField());
//            }

//        }
        return retDup;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
            Class<?>[] argumentsTypes, Throwable t) {
    }

    private EnhancedInstance getInstance(Object o) {
        EnhancedInstance instance = null;
        if (o instanceof EnhancedInstance) {
            instance = (EnhancedInstance) o;
        } else if (o instanceof ServerWebExchangeDecorator) {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) o).getDelegate();
            return getInstance(delegate);
        }
        return instance;
    }
}
