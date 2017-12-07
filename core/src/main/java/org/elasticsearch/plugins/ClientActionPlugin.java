/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.plugins;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.GenericAction;
import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;

import java.util.Collections;
import java.util.List;


/**
 * An additional extension point for {@link Plugin}s that extends Elasticsearch's scripting functionality. Implement it like this:
 * <pre>{@code
 *   {@literal @}Override
 *   public List<GenericAction<? extends ActionRequest, ? extends ActionResponse>> getClientActions() {
 *       return Arrays.asList(ReindexAction.INSTANCE,
 *                            UpdateByQueryAction.INSTANCE,
 *                            DeleteByQueryAction.INSTANCE,
 *                            RethrottleAction.INSTANCE);
 *   }
 * }</pre>
 */
public interface ClientActionPlugin {
    /**
     * Actions added by this plugin.
     */
    default List<GenericAction<? extends ActionRequest, ? extends ActionResponse>> getClientActions() {
        return Collections.emptyList();
    }

    /**
     * Returns parsers for {@link NamedWriteable} this plugin will use over the transport protocol.
     * @see NamedWriteableRegistry
     */
    default List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        return Collections.emptyList();
    }
}
