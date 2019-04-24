/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.enrich.rest;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.core.enrich.action.ListEnrichPolicyAction;

import java.io.IOException;

public class RestListEnrichPolicyAction extends BaseRestHandler {

    public RestListEnrichPolicyAction(final Settings settings, final RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.GET, "/_enrich/policy", this);
    }

    @Override
    public String getName() {
        return "list_enrich_policy";
    }

    @Override
    protected RestChannelConsumer prepareRequest(final RestRequest restRequest, final NodeClient client) throws IOException {
        final ListEnrichPolicyAction.Request request = new ListEnrichPolicyAction.Request();
        return channel -> client.execute(ListEnrichPolicyAction.INSTANCE, request, new RestToXContentListener<>(channel));
    }
}
