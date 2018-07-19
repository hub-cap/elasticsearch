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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.core.watcher.common.xcontent.XContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class XContentSource implements ToXContent {

    private final BytesReference bytes;
    private final XContentType contentType;
    private Object data;


    /**
     * Constructs a new XContentSource out of the given bytes reference.
     */
    public XContentSource(BytesReference bytes, XContentType xContentType) throws ElasticsearchParseException {
        if (xContentType == null) {
            throw new IllegalArgumentException("xContentType must not be null");
        }
        this.bytes = bytes;
        this.contentType = xContentType;
    }

    /**
     * Constructs a new xcontent source from the bytes of the given xcontent builder
     */
    public XContentSource(XContentBuilder builder) {
        this(BytesReference.bytes(builder), builder.contentType());
    }


    /**
     * @return The bytes reference of the source
     */
    public BytesReference getBytes() {
        return bytes;
    }

    /**
     * @return The content-type
     */
    public XContentType getContentType() {
        return contentType;
    }

    /**
     * @return true if the top level value of the source is a map
     */
    public boolean isMap() {
        return data() instanceof Map;
    }

    /**
     * @return The source as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAsMap() {
        return (Map<String, Object>) data();
    }

    /**
     * @return true if the top level value of the source is a list
     */
    public boolean isList() {
        return data() instanceof List;
    }

    /**
     * @return The source as a list
     */
    @SuppressWarnings("unchecked")
    public List<Object> getAsList() {
        return (List<Object>) data();
    }

    /**
     * Extracts a value identified by the given path in the source.
     *
     * @param path a dot notation path to the requested value
     * @return The extracted value or {@code null} if no value is associated with the given path
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String path) {
        return (T) ObjectPath.eval(path, data());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        // EMPTY is safe here because we never use namedObject
        try (InputStream stream = bytes.streamInput();
             XContentParser parser = parser(NamedXContentRegistry.EMPTY, stream)) {
            parser.nextToken();
            builder.generator().copyCurrentStructure(parser);
            return builder;
        }
    }

    private Object data() {
        if (data == null) {
            // EMPTY is safe here because we never use namedObject
            try (InputStream stream = bytes.streamInput();
                 XContentParser parser = parser(NamedXContentRegistry.EMPTY, stream)) {
                data = XContentUtils.readValue(parser, parser.nextToken());
            } catch (IOException ex) {
                throw new ElasticsearchException("failed to read value", ex);
            }
        }
        return data;
    }

    public XContentParser parser(NamedXContentRegistry xContentRegistry, InputStream stream) throws IOException {
        return contentType.xContent().createParser(xContentRegistry, LoggingDeprecationHandler.INSTANCE, stream);
    }

}
