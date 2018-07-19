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
package org.elasticsearch.xpack.core.watcher.support.xcontent;

import org.elasticsearch.common.xcontent.ToXContent;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.elasticsearch.xpack.core.watcher.watch.WatchField.INCLUDE_STATUS_KEY;

public class WatcherParams extends ToXContent.DelegatingMapParams {

    public static final WatcherParams HIDE_SECRETS = WatcherParams.builder().hideSecrets(true).build();

    private static final String HIDE_SECRETS_KEY = "hide_secrets";
    private static final String HIDE_HEADERS = "hide_headers";
    private static final String DEBUG_KEY = "debug";

    public static boolean hideSecrets(ToXContent.Params params) {
        return wrap(params).hideSecrets();
    }

    public static boolean debug(ToXContent.Params params) {
        return wrap(params).debug();
    }

    public static boolean hideHeaders(ToXContent.Params params) {
        return wrap(params).hideHeaders();
    }

    private WatcherParams(Map<String, String> params, ToXContent.Params delegate) {
        super(params, delegate);
    }

    private boolean hideSecrets() {
        return paramAsBoolean(HIDE_SECRETS_KEY, true);
    }

    private boolean debug() {
        return paramAsBoolean(DEBUG_KEY, false);
    }

    private boolean hideHeaders() {
        return paramAsBoolean(HIDE_HEADERS, true);
    }

    public static WatcherParams wrap(ToXContent.Params params) {
        return params instanceof WatcherParams ?
                (WatcherParams) params :
                new WatcherParams(emptyMap(), params);
    }

    public static Builder builder() {
        return builder(ToXContent.EMPTY_PARAMS);
    }

    public static Builder builder(ToXContent.Params delegate) {
        return new Builder(delegate);
    }

    public static class Builder {

        private final ToXContent.Params delegate;
        private final Map<String, String> params = new HashMap<>();

        private Builder(ToXContent.Params delegate) {
            this.delegate = delegate;
        }

        public Builder hideSecrets(boolean hideSecrets) {
            params.put(HIDE_SECRETS_KEY, String.valueOf(hideSecrets));
            return this;
        }

        public Builder hideHeaders(boolean hideHeaders) {
            params.put(HIDE_HEADERS, String.valueOf(hideHeaders));
            return this;
        }

        public Builder debug(boolean debug) {
            params.put(DEBUG_KEY, String.valueOf(debug));
            return this;
        }

        public Builder includeStatus(boolean includeStatus) {
            params.put(INCLUDE_STATUS_KEY, String.valueOf(includeStatus));
            return this;
        }

        public Builder put(String key, Object value) {
            params.put(key, String.valueOf(value));
            return this;
        }

        public WatcherParams build() {
            return new WatcherParams(unmodifiableMap(new HashMap<>(params)), delegate);
        }
    }
}
