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
package org.elasticsearch.xpack.core.watcher.input.none;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.core.watcher.input.Input;

import java.io.IOException;

public class NoneInput implements Input {

    public static final String TYPE = "none";
    public static final NoneInput INSTANCE = new NoneInput();

    private NoneInput() {
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().endObject();
    }

    public static Builder builder() {
        return Builder.INSTANCE;
    }

    public static class Builder implements Input.Builder<NoneInput> {

        private static final Builder INSTANCE = new Builder();

        private Builder() {
        }

        @Override
        public NoneInput build() {
            return NoneInput.INSTANCE;
        }
    }
}
