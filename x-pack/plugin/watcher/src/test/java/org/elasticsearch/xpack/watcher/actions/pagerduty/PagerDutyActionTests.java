/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.actions.pagerduty;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.core.watcher.actions.Action;
import org.elasticsearch.xpack.core.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.core.watcher.execution.Wid;
import org.elasticsearch.xpack.core.watcher.watch.Payload;
import org.elasticsearch.xpack.watcher.common.http.HttpProxy;
import org.elasticsearch.xpack.watcher.common.http.HttpRequest;
import org.elasticsearch.xpack.watcher.common.http.HttpResponse;
import org.elasticsearch.xpack.watcher.common.text.TextTemplate;
import org.elasticsearch.xpack.watcher.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.notification.pagerduty.IncidentEvent;
import org.elasticsearch.xpack.watcher.notification.pagerduty.IncidentEventContext;
import org.elasticsearch.xpack.watcher.notification.pagerduty.IncidentEventDefaults;
import org.elasticsearch.xpack.watcher.notification.pagerduty.PagerDutyAccount;
import org.elasticsearch.xpack.watcher.notification.pagerduty.PagerDutyService;
import org.elasticsearch.xpack.watcher.notification.pagerduty.SentEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.watcher.actions.ActionBuilders.pagerDutyAction;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.mockExecutionContextBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PagerDutyActionTests extends ESTestCase {

    private PagerDutyService service;

    @Before
    public void init() throws Exception {
        service = mock(PagerDutyService.class);
    }

    public void testExecute() throws Exception {
        final String accountName = "account1";

        TextTemplateEngine templateEngine = mock(TextTemplateEngine.class);

        String description = new String("_description");
        IncidentEvent.Builder eventBuilder = new IncidentEvent.Builder(description);
        boolean attachPayload = randomBoolean();
        eventBuilder.setAttachPayload(attachPayload);
        eventBuilder.setAccount(accountName);
        IncidentEvent eventTemplate = eventBuilder.build();

        PagerDutyAction action = new PagerDutyAction(eventTemplate);
        ExecutablePagerDutyAction executable = new ExecutablePagerDutyAction(action, logger, service, templateEngine);

        Map<String, Object> data = new HashMap<>();
        Payload payload = new Payload.Simple(data);

        Map<String, Object> metadata = MapBuilder.<String, Object>newMapBuilder().put("_key", "_val").map();

        DateTime now = DateTime.now(DateTimeZone.UTC);

        Wid wid = new Wid(randomAlphaOfLength(5), now);
        WatchExecutionContext ctx = mockExecutionContextBuilder(wid.watchId())
                .wid(wid)
                .payload(payload)
                .time(wid.watchId(), now)
                .metadata(metadata)
                .buildMock();

        Map<String, Object> ctxModel = new HashMap<>();
        ctxModel.put("id", ctx.id().value());
        ctxModel.put("watch_id", wid.watchId());
        ctxModel.put("payload", data);
        ctxModel.put("metadata", metadata);
        ctxModel.put("execution_time", now);
        Map<String, Object> triggerModel = new HashMap<>();
        triggerModel.put("triggered_time", now);
        triggerModel.put("scheduled_time", now);
        ctxModel.put("trigger", triggerModel);
        ctxModel.put("vars", Collections.emptyMap());
        Map<String, Object> expectedModel = new HashMap<>();
        expectedModel.put("ctx", ctxModel);

        when(templateEngine.render(new TextTemplate(description), expectedModel)).thenReturn(description);

        IncidentEvent event = new IncidentEvent(description, null, wid.watchId(), null, null, accountName, attachPayload,
                null, null);
        PagerDutyAccount account = mock(PagerDutyAccount.class);
        when(account.getDefaults()).thenReturn(new IncidentEventDefaults(Settings.EMPTY));
        HttpResponse response = mock(HttpResponse.class);
        when(response.status()).thenReturn(200);
        HttpRequest request = mock(HttpRequest.class);
        SentEvent sentEvent = SentEvent.responded(event, request, response);
        when(account.send(event, payload)).thenReturn(sentEvent);
        when(service.getAccount(accountName)).thenReturn(account);

        Action.Result result = executable.execute("_id", ctx, payload);

        assertThat(result, notNullValue());
        assertThat(result, instanceOf(PagerDutyAction.Result.Executed.class));
        assertThat(result.status(), equalTo(Action.Result.Status.SUCCESS));
        assertThat(((PagerDutyAction.Result.Executed) result).sentEvent(), sameInstance(sentEvent));
    }

    public void testParser() throws Exception {

        XContentBuilder builder = jsonBuilder().startObject();

        String accountName = randomAlphaOfLength(10);
        builder.field("account", accountName);

        String incidentKey = null;
        if (randomBoolean()) {
            incidentKey = "_incident_key";
            builder.field("incident_key", incidentKey);
        }

        String description = "_description";
        builder.field("description", description);

        String client = null;
        if (randomBoolean()) {
            client = "_client";
            builder.field("client", client);
        }

        String clientUrl = null;
        if (randomBoolean()) {
            clientUrl = "_client_url";
            builder.field("client_url", clientUrl);
        }

        String eventType = null;
        if (randomBoolean()) {
            eventType = randomFrom("trigger", "resolve", "acknowledge");
            builder.field("event_type", eventType);
        }

        boolean attachPayload = randomBoolean();
        if (attachPayload) {
            builder.field("attach_payload", attachPayload);
        }

        HttpProxy proxy = null;
        if (randomBoolean()) {
            proxy = new HttpProxy("localhost", 8080);
            proxy.toXContent(builder, ToXContent.EMPTY_PARAMS);
        }

        IncidentEventContext[] contexts = null;
        if (randomBoolean()) {
            contexts = new IncidentEventContext[] {
                    IncidentEventContext.link("_href", "_text"),
                    IncidentEventContext.image("_src", "_href", "_alt")
            };
            String fieldName = randomBoolean() ? "contexts" : "context";
            builder.array(fieldName, (Object) contexts);
        }

        builder.endObject();

        BytesReference bytes = BytesReference.bytes(builder);
        logger.info("pagerduty action json [{}]", bytes.utf8ToString());
        XContentParser parser = createParser(JsonXContent.jsonXContent, bytes);
        parser.nextToken();

        PagerDutyAction action = PagerDutyAction.parse("_watch", "_action", parser);

        assertThat(action, notNullValue());
        assertThat(action.event.getAccount(), is(accountName));
        assertThat(action.event, notNullValue());
        assertThat(action.event, instanceOf(IncidentEvent.class));
        assertThat(action.event, is(new IncidentEvent(description, eventType, incidentKey, client, clientUrl, accountName,
                attachPayload, contexts, proxy)));
    }

    public void testParserSelfGenerated() throws Exception {
        IncidentEvent.Builder event = IncidentEvent.templateBuilder(randomAlphaOfLength(50));

        if (randomBoolean()) {
            event.setIncidentKey(randomAlphaOfLength(50));
        }
        if (randomBoolean()) {
            event.setClient(randomAlphaOfLength(50));
        }
        if (randomBoolean()) {
            event.setClientUrl(randomAlphaOfLength(50));
        }
        if (randomBoolean()) {
            event.setAttachPayload(randomBoolean());
        }
        if (randomBoolean()) {
            event.addContext(IncidentEventContext.link("_href", "_text"));
        }
        if (randomBoolean()) {
            event.addContext(IncidentEventContext.image("_src", "_href","_alt"));
        }
        if (randomBoolean()) {
            event.setEventType(randomAlphaOfLength(50));
        }
        if (randomBoolean()) {
            event.setAccount(randomAlphaOfLength(50));
        }
        if (randomBoolean()) {
            event.setProxy(new HttpProxy("localhost", 8080));
        }

        PagerDutyAction action = pagerDutyAction(event).build();
        XContentBuilder jsonBuilder = jsonBuilder();
        action.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
        XContentParser parser = createParser(jsonBuilder);
        parser.nextToken();

        PagerDutyAction parsedAction = PagerDutyAction.parse("_w1", "_a1", parser);
        assertThat(parsedAction, notNullValue());
        assertThat(parsedAction, is(action));
    }

    public void testParserInvalid() throws Exception {
        try {
            XContentBuilder builder = jsonBuilder().startObject().field("unknown_field", "value").endObject();
            XContentParser parser = createParser(builder);
            parser.nextToken();
            PagerDutyAction.parse("_watch", "_action", parser);
            fail("Expected ElasticsearchParseException but did not happen");
        } catch (ElasticsearchParseException e) {

        }
    }
}
