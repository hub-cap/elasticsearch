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
package org.elasticsearch.xpack.core.watcher.condition;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public class AlwaysCondition implements Condition {
    public static final String TYPE = "always";
    public static final Condition INSTANCE = new AlwaysCondition();

    protected AlwaysCondition() { }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AlwaysCondition;
    }

    @Override
    public int hashCode() {
        // All instances has to produce the same hashCode because they are all equal
        return 0;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().endObject();
    }
}
