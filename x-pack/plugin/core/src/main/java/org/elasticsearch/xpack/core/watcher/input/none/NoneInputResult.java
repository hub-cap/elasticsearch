/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.watcher.input.none;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.core.watcher.input.InputResult;
import org.elasticsearch.xpack.core.watcher.watch.Payload;

import java.io.IOException;

public class NoneInputResult extends InputResult {

    public static final NoneInputResult INSTANCE = new NoneInputResult();

    private NoneInputResult() {
        super(NoneInput.TYPE, Payload.EMPTY);
    }

    @Override
    protected XContentBuilder typeXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        return builder;
    }
}
