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

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.equalTo;

public class DeleteWatchResponseTests extends ESTestCase {

    private org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchResponse createTestInstance() {
        String id = randomAlphaOfLength(10);
        long version = randomLongBetween(1, 10);
        boolean found = randomBoolean();
        return new org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchResponse(id, version, found);
    }

    protected DeleteWatchResponse doParseInstance(XContentParser parser) throws IOException {
        return DeleteWatchResponse.fromXContent(parser);
    }

    public void testServerToClient() throws IOException {
        // using a server side class, construct the XContent
        org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchResponse expected = createTestInstance();
        XContentBuilder builder = jsonBuilder();
        expected.toXContent(builder, ToXContent.EMPTY_PARAMS);
        XContentParser parser = createParser(builder);

        // using the parser created from the server side builder, parse the client side response
        DeleteWatchResponse actual = doParseInstance(parser);

        // since the classes are not the same, we must check the internal state of the things being set when the server
        // side class is instantiated
        assertThat(expected.getId(), equalTo(actual.getId()));
        assertThat(expected.getVersion(), equalTo(actual.getVersion()));
        assertThat(expected.isFound(), equalTo(actual.isFound()));
    }
}
