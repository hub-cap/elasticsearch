/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.rest.action;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.core.watcher.client.WatcherClient;
import org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchRequest;
import org.elasticsearch.xpack.watcher.rest.WatcherRestHandler;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;

public class RestDeleteWatchAction extends WatcherRestHandler {
    public RestDeleteWatchAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(DELETE, URI_BASE + "/watch/{id}", this);
    }

    @Override
    public String getName() {
        return "xpack_watcher_delete_watch_action";
    }

    @Override
    protected RestChannelConsumer doPrepareRequest(final RestRequest request, WatcherClient client) {
        DeleteWatchRequest deleteWatchRequest = new DeleteWatchRequest(request.param("id"));
        return channel -> client.deleteWatch(deleteWatchRequest, new RestToXContentListener<>(channel));
    }
}
