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
package org.elasticsearch.protocol.xpack.watcher;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Objects;

public class DeleteWatchResponse {

    @SuppressWarnings("unchecked")
    private static final ConstructingObjectParser<DeleteWatchResponse, Void> PARSER =
        new ConstructingObjectParser<>(DeleteWatchResponse.class.getName(), true,
            (args) -> new DeleteWatchResponse((String) args[0], (long) args[1], (boolean) args[2]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("_id"));
        PARSER.declareLong(ConstructingObjectParser.constructorArg(), new ParseField("_version"));
        PARSER.declareBoolean(ConstructingObjectParser.constructorArg(), new ParseField("found"));
    }

    private final String id;
    private final long version;
    private final boolean found;

    public DeleteWatchResponse(String id, long version, boolean found) {
        this.id = id;
        this.version = version;
        this.found = found;
    }

    public String getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public boolean isFound() {
        return found;
    }

    public static DeleteWatchResponse fromXContent(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeleteWatchResponse that = (DeleteWatchResponse) o;

        return Objects.equals(id, that.id) && Objects.equals(version, that.version) && Objects.equals(found, that.found);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, found);
    }

}
