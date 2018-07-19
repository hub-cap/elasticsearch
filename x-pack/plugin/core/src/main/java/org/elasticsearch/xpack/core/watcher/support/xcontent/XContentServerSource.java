/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.watcher.support.xcontent;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;

/**
 * Encapsulates the xcontent source
 */
public class XContentServerSource extends XContentSource {


    /**
     * Constructs a new XContentServerSource out of the given bytes reference.
     */
    public XContentServerSource(BytesReference bytes, XContentType xContentType) throws ElasticsearchParseException {
        super(bytes, xContentType);
    }

    /**
     * Constructs a new xcontent source from the bytes of the given xcontent builder
     */
    public XContentServerSource(XContentBuilder builder) {
        super(BytesReference.bytes(builder), builder.contentType());
    }


    public static XContentServerSource readFrom(StreamInput in) throws IOException {
        return new XContentServerSource(in.readBytesReference(), in.readEnum(XContentType.class));
    }

    public static void writeTo(XContentServerSource source, StreamOutput out) throws IOException {
        out.writeBytesReference(source.getBytes());
        out.writeEnum(source.getContentType());
    }


}
