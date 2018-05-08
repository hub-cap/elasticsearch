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

package org.elasticsearch.client.documentation;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.LatchedActionListener;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse;
import org.elasticsearch.client.ESRestHighLevelClientTestCase;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;

/**
 * This class is used to generate the Java Cluster API documentation.
 * You need to wrap your code between two tags like:
 * // tag::example
 * // end::example
 *
 * Where example is your tag name.
 *
 * Then in the documentation, you can extract what is between tag and end tags with
 * ["source","java",subs="attributes,callouts,macros"]
 * --------------------------------------------------
 * include-tagged::{doc-tests}/SnapshotClientDocumentationIT.java[example]
 * --------------------------------------------------
 *
 * The column width of the code block is 84. If the code contains a line longer
 * than 84, the line will be cut and a horizontal scroll bar will be displayed.
 * (the code indentation of the tag is not included in the width)
 */
public class SnapshotClientDocumentationIT extends ESRestHighLevelClientTestCase {

    private static final String testRepository = "test_repository";

    public void testSnapshotPutRepository() throws IOException {
        RestHighLevelClient client = highLevelClient();

        // tag::put-repository-request
        PutRepositoryRequest request = new PutRepositoryRequest();
        // end::put-repository-request

        // tag::put-repository-create-settings
        Settings settings = Settings.builder()
            .put("location", ".")
            .put("compress", true)
            .build(); // <1>
        // end::put-repository-create-settings

        // tag::put-repository-request-name
        request.name(testRepository); // <1>
        // end::put-repository-request-name
        // tag::put-repository-request-type
        request.type("fs"); // <1>
        // end::put-repository-request-type
        // tag::put-repository-request-settings
        request.settings(settings); // <1>
        // end::put-repository-request-settings
        // tag::put-repository-request-masterTimeout
        request.masterNodeTimeout(TimeValue.timeValueMinutes(1)); // <1>
        request.masterNodeTimeout("1m"); // <2>
        // end::put-repository-request-masterTimeout
        // tag::put-repository-request-timeout
        request.timeout(TimeValue.timeValueMinutes(1)); // <1>
        request.timeout("1m"); // <2>
        // end::put-repository-request-timeout
        // tag::put-repository-request-verify
        request.verify(true); // <1>
        // end::put-repository-request-verify

        // tag::put-repository-execute
        PutRepositoryResponse response = client.snapshot().putRepository(request);
        // end::get-repository-execute

        // tag::put-repository-response
        boolean acknowledged = response.isAcknowledged();
        // end::put-repository-response
        assertTrue(acknowledged);
    }

    public void testSnapshotGetRepository() throws IOException {
        RestHighLevelClient client = highLevelClient();

        createTestRepositories();

        // tag::get-repository-request
        GetRepositoriesRequest request = new GetRepositoriesRequest();
        // end::get-repository-request

        // tag::get-repository-request-repositories
        String [] repositories = new String[] { testRepository };
        request.repositories(repositories); // <1>
        // end::get-repository-request-repositories
        // tag::get-repository-request-local
        request.local(true); // <1>
        // end::get-repository-request-local
        // tag::get-repository-request-masterTimeout
        request.masterNodeTimeout(TimeValue.timeValueMinutes(1)); // <1>
        request.masterNodeTimeout("1m"); // <2>
        // end::get-repository-request-masterTimeout

        // tag::get-repository-execute
        GetRepositoriesResponse response = client.snapshot().getRepositories(request);
        // end::get-repository-execute

        // tag::get-repository-response
        List<RepositoryMetaData> repositoryMetaDataResponse = response.repositories();
        // end::get-repository-response
        assertThat(1, equalTo(repositoryMetaDataResponse.size()));
        assertThat(testRepository, equalTo(repositoryMetaDataResponse.get(0).name()));
    }

    public void testSnapshotGetRepositoryAsync() throws InterruptedException {
        RestHighLevelClient client = highLevelClient();
        {
            GetRepositoriesRequest request = new GetRepositoriesRequest();

            // tag::get-repository-execute-listener
            ActionListener<GetRepositoriesResponse> listener =
                    new ActionListener<GetRepositoriesResponse>() {
                @Override
                public void onResponse(GetRepositoriesResponse getRepositoriesResponse) {
                    // <1>
                }

                @Override
                public void onFailure(Exception e) {
                    // <2>
                }
            };
            // end::get-repository-execute-listener

            // Replace the empty listener by a blocking listener in test
            final CountDownLatch latch = new CountDownLatch(1);
            listener = new LatchedActionListener<>(listener, latch);

            // tag::get-repository-execute-async
            client.snapshot().getRepositoriesAsync(request, listener); // <1>
            // end::get-repository-execute-async

            assertTrue(latch.await(30L, TimeUnit.SECONDS));
        }

    }

    private final void createTestRepositories() throws IOException {
        PutRepositoryRequest request = new PutRepositoryRequest(testRepository);
        request.type("fs");
        request.settings("{\"location\": \".\"}", XContentType.JSON);
        assertTrue(highLevelClient().snapshot().putRepository(request).isAcknowledged());
    }
}
