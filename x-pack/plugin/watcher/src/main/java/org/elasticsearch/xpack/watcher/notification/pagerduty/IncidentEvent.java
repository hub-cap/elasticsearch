/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.pagerduty;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.core.watcher.watch.Payload;
import org.elasticsearch.xpack.watcher.common.http.HttpMethod;
import org.elasticsearch.xpack.watcher.common.http.HttpProxy;
import org.elasticsearch.xpack.watcher.common.http.HttpRequest;
import org.elasticsearch.xpack.watcher.common.http.Scheme;
import org.elasticsearch.xpack.watcher.common.text.TextTemplate;
import org.elasticsearch.xpack.watcher.common.text.TextTemplateEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Official documentation for this can be found at
 *
 * https://developer.pagerduty.com/documentation/howto/manually-trigger-an-incident/
 * https://developer.pagerduty.com/documentation/integration/events/trigger
 * https://developer.pagerduty.com/documentation/integration/events/acknowledge
 * https://developer.pagerduty.com/documentation/integration/events/resolve
 */
public class IncidentEvent implements ToXContentObject {

    static final String HOST = "events.pagerduty.com";
    static final String PATH = "/generic/2010-04-15/create_event.json";

    final String description;
    @Nullable final HttpProxy proxy;
    @Nullable final String incidentKey;
    @Nullable final String client;
    @Nullable final String clientUrl;
    @Nullable final String account;
    final String eventType;
    final boolean attachPayload;
    @Nullable final IncidentEventContext[] contexts;

    public IncidentEvent(String description, @Nullable String eventType, @Nullable String incidentKey, @Nullable String client,
                         @Nullable String clientUrl, @Nullable String account, boolean attachPayload,
                         @Nullable IncidentEventContext[] contexts, @Nullable HttpProxy proxy) {
        this.description = description;
        if (description == null) {
            throw new IllegalStateException("could not create pagerduty event. missing required [" +
                    Fields.DESCRIPTION.getPreferredName() + "] setting");
        }
        this.incidentKey = incidentKey;
        this.client = client;
        this.clientUrl = clientUrl;
        this.account = account;
        this.proxy = proxy;
        this.attachPayload = attachPayload;
        this.contexts = contexts;
        this.eventType = Strings.hasLength(eventType) ? eventType : "trigger";
    }

    public String getAccount() { return account; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IncidentEvent template = (IncidentEvent) o;
        return Objects.equals(description, template.description) &&
                Objects.equals(incidentKey, template.incidentKey) &&
                Objects.equals(client, template.client) &&
                Objects.equals(clientUrl, template.clientUrl) &&
                Objects.equals(attachPayload, template.attachPayload) &&
                Objects.equals(eventType, template.eventType) &&
                Objects.equals(account, template.account) &&
                Objects.equals(proxy, template.proxy) &&
                Arrays.equals(contexts, template.contexts);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(description, incidentKey, client, clientUrl, account, attachPayload, eventType, proxy);
        result = 31 * result + Arrays.hashCode(contexts);
        return result;
    }

    public HttpRequest createRequest(final String serviceKey, final Payload payload) throws IOException {
        return HttpRequest.builder(HOST, -1)
                .method(HttpMethod.POST)
                .scheme(Scheme.HTTPS)
                .path(PATH)
                .proxy(proxy)
                .jsonBody(new ToXContent() {
                    @Override
                    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                        builder.field(Fields.SERVICE_KEY.getPreferredName(), serviceKey);
                        builder.field(Fields.EVENT_TYPE.getPreferredName(), eventType);
                        builder.field(Fields.DESCRIPTION.getPreferredName(), description);
                        if (incidentKey != null) {
                            builder.field(Fields.INCIDENT_KEY.getPreferredName(), incidentKey);
                        }
                        if (client != null) {
                            builder.field(Fields.CLIENT.getPreferredName(), client);
                        }
                        if (clientUrl != null) {
                            builder.field(Fields.CLIENT_URL.getPreferredName(), clientUrl);
                        }
                        if (attachPayload) {
                            builder.startObject(Fields.DETAILS.getPreferredName());
                            builder.field(Fields.PAYLOAD.getPreferredName());
                            payload.toXContent(builder, params);
                            builder.endObject();
                        }
                        if (contexts != null && contexts.length > 0) {
                            builder.startArray(Fields.CONTEXTS.getPreferredName());
                            for (IncidentEventContext context : contexts) {
                                context.toXContent(builder, params);
                            }
                            builder.endArray();
                        }
                        return builder;
                    }
                })
                .build();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field(Fields.DESCRIPTION.getPreferredName(), description);
        if (eventType != null) {
            builder.field(Fields.EVENT_TYPE.getPreferredName(), eventType);
        }
        if (incidentKey != null) {
            builder.field(Fields.INCIDENT_KEY.getPreferredName(), incidentKey);
        }
        if (client != null) {
            builder.field(Fields.CLIENT.getPreferredName(), client);
        }
        if (clientUrl != null) {
            builder.field(Fields.CLIENT_URL.getPreferredName(), clientUrl);
        }
        if (account != null) {
            builder.field(Fields.ACCOUNT.getPreferredName(), account);
        }
        if (proxy != null) {
            proxy.toXContent(builder, params);
        }
        builder.field(Fields.ATTACH_PAYLOAD.getPreferredName(), attachPayload);
        if (contexts != null) {
            builder.startArray(Fields.CONTEXTS.getPreferredName());
            for (IncidentEventContext context : contexts) {
                context.toXContent(builder, params);
            }
            builder.endArray();
        }

        return builder.endObject();
    }

    public static Builder templateBuilder(String description) {
        return new Builder(description);
    }

    public static IncidentEvent render(String watchId, String actionId, TextTemplateEngine engine, Map<String, Object> model,
                                IncidentEventDefaults defaults, IncidentEvent event) {
        String description = event.description != null ? engine.render(new TextTemplate(event.description), model) : defaults.description;
        String incidentKey = event.incidentKey != null ? engine.render(new TextTemplate(event.incidentKey), model) :
            defaults.incidentKey != null ? defaults.incidentKey : watchId;
        String client = event.client != null ? engine.render(new TextTemplate(event.client), model) : defaults.client;
        String clientUrl = event.clientUrl != null ? engine.render(new TextTemplate(event.clientUrl), model) : defaults.clientUrl;
        String eventType = event.eventType != null ? engine.render(new TextTemplate(event.eventType), model) : defaults.eventType;
        boolean attachPayload = event.attachPayload;
        IncidentEventContext[] contexts = null;
        if (event.contexts != null) {
            contexts = new IncidentEventContext[event.contexts.length];
            for (int i = 0; i < event.contexts.length; i++) {
                contexts[i] = IncidentEventContext.render(engine, model, defaults, event.contexts[i]);
            }
        }
        return new IncidentEvent(description, eventType, incidentKey, client, clientUrl, event.account, attachPayload, contexts,
            event.proxy);
    }

    public static IncidentEvent parse(String watchId, String actionId, XContentParser parser) throws IOException {
        String incidentKey = null;
        String description = null;
        String client = null;
        String clientUrl = null;
        String eventType = null;
        String account = null;
        HttpProxy proxy = null;
        boolean attachPayload = false;
        IncidentEventContext[] contexts = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (Fields.INCIDENT_KEY.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    incidentKey = parser.text();
                } catch (ElasticsearchParseException e) {
                    throw new ElasticsearchParseException("could not parse pager duty event template. failed to parse field [{}]",
                        Fields.INCIDENT_KEY.getPreferredName());
                }
            } else if (Fields.DESCRIPTION.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    description = parser.text();
                } catch (ElasticsearchParseException e) {
                    throw new ElasticsearchParseException("could not parse pager duty event template. failed to parse field [{}]",
                        Fields.DESCRIPTION.getPreferredName());
                }
            } else if (Fields.CLIENT.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    client = parser.text();
                } catch (ElasticsearchParseException e) {
                    throw new ElasticsearchParseException("could not parse pager duty event template. failed to parse field [{}]",
                        Fields.CLIENT.getPreferredName());
                }
            } else if (Fields.CLIENT_URL.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    clientUrl = parser.text();
                } catch (ElasticsearchParseException e) {
                    throw new ElasticsearchParseException("could not parse pager duty event template. failed to parse field [{}]",
                        Fields.CLIENT_URL.getPreferredName());
                }
            } else if (Fields.ACCOUNT.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    account = parser.text();
                } catch (ElasticsearchParseException e) {
                    throw new ElasticsearchParseException("could not parse pager duty event template. failed to parse field [{}]",
                        Fields.CLIENT_URL.getPreferredName());
                }
            } else if (Fields.PROXY.match(currentFieldName, parser.getDeprecationHandler())) {
                proxy = HttpProxy.parse(parser);
            } else if (Fields.EVENT_TYPE.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    eventType = parser.text();
                } catch (ElasticsearchParseException e) {
                    throw new ElasticsearchParseException("could not parse pager duty event template. failed to parse field [{}]",
                        Fields.EVENT_TYPE.getPreferredName());
                }
            } else if (Fields.ATTACH_PAYLOAD.match(currentFieldName, parser.getDeprecationHandler())) {
                if (token == XContentParser.Token.VALUE_BOOLEAN) {
                    attachPayload = parser.booleanValue();
                } else {
                    throw new ElasticsearchParseException("could not parse pager duty event template. failed to parse field [{}], " +
                        "expected a boolean value but found [{}] instead", Fields.ATTACH_PAYLOAD.getPreferredName(), token);
                }
            } else if (Fields.CONTEXTS.match(currentFieldName, parser.getDeprecationHandler())
                || Fields.CONTEXT_DEPRECATED.match(currentFieldName, parser.getDeprecationHandler())) {
                if (token == XContentParser.Token.START_ARRAY) {
                    List<IncidentEventContext> list = new ArrayList<>();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        try {
                            list.add(IncidentEventContext.parse(parser));
                        } catch (ElasticsearchParseException e) {
                            throw new ElasticsearchParseException("could not parse pager duty event template. failed to parse field " +
                                "[{}]", parser.currentName());
                        }
                    }
                    contexts = list.toArray(new IncidentEventContext[list.size()]);
                }
            } else {
                throw new ElasticsearchParseException("could not parse pager duty event template. unexpected field [{}]",
                    currentFieldName);
            }
        }
        return new IncidentEvent(description, eventType, incidentKey, client, clientUrl, account, attachPayload, contexts, proxy);
    }

    public static class Builder {

        final String description;
        String incidentKey;
        String client;
        String clientUrl;
        String eventType;
        String account;
        HttpProxy proxy;
        boolean attachPayload;
        List<IncidentEventContext> contexts = new ArrayList<>();

        public Builder(String description) {
            this.description = description;
        }

        public Builder setIncidentKey(String incidentKey) {
            this.incidentKey = incidentKey;
            return this;
        }

        public Builder setClient(String client) {
            this.client = client;
            return this;
        }

        public Builder setClientUrl(String clientUrl) {
            this.clientUrl = clientUrl;
            return this;
        }

        public Builder setEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder setAccount(String account) {
            this.account= account;
            return this;
        }

        public Builder setAttachPayload(boolean attachPayload) {
            this.attachPayload = attachPayload;
            return this;
        }

        public Builder setProxy(HttpProxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder addContext(IncidentEventContext context) {
            this.contexts.add(context);
            return this;
        }

        public IncidentEvent build() {
            IncidentEventContext[] contexts = this.contexts.isEmpty() ? null :
                this.contexts.toArray(new IncidentEventContext[this.contexts.size()]);
            return new IncidentEvent(description, eventType, incidentKey, client, clientUrl, account, attachPayload, contexts, proxy);
        }
    }

    interface Fields {

        ParseField TYPE = new ParseField("type");
        ParseField EVENT_TYPE = new ParseField("event_type");

        ParseField ACCOUNT = new ParseField("account");
        ParseField PROXY = new ParseField("proxy");
        ParseField DESCRIPTION = new ParseField("description");
        ParseField INCIDENT_KEY = new ParseField("incident_key");
        ParseField CLIENT = new ParseField("client");
        ParseField CLIENT_URL = new ParseField("client_url");
        ParseField ATTACH_PAYLOAD = new ParseField("attach_payload");
        ParseField CONTEXTS = new ParseField("contexts");
        // this field exists because in versions prior 6.0 we accidentally used context instead of contexts and thus the correct data
        // was never picked up on the pagerduty side
        // we need to keep this for BWC
        ParseField CONTEXT_DEPRECATED = new ParseField("context");

        ParseField SERVICE_KEY = new ParseField("service_key");
        ParseField PAYLOAD = new ParseField("payload");
        ParseField DETAILS = new ParseField("details");
    }
}
