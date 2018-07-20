/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.slack.message;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.common.text.TextTemplate;
import org.elasticsearch.xpack.watcher.common.text.TextTemplateEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SlackMessage implements MessageElement {

    final String from;
    final String[] to;
    final String icon;
    final String text;
    final Attachment[] attachments;
    final DynamicAttachments dynamicAttachments;

    public SlackMessage(String from, String[] to, String icon, @Nullable String text, @Nullable Attachment[] attachments,
                        @Nullable DynamicAttachments dynamicAttachments) {
        if(text == null && attachments == null) {
            throw new IllegalArgumentException("Both text and attachments cannot be null.");
        }

        this.from = from;
        this.to = to;
        this.icon = icon;
        this.text = text;
        this.attachments = attachments;
        this.dynamicAttachments = dynamicAttachments;
    }

    public String getFrom() {
        return from;
    }

    public String[] getTo() {
        return to;
    }

    public String getIcon() {
        return icon;
    }

    public String getText() {
        return text;
    }

    public Attachment[] getAttachments() {
        return attachments;
    }

    public DynamicAttachments getDynamicAttachments() {
        return dynamicAttachments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SlackMessage that = (SlackMessage) o;
        return Objects.equals(from, that.from) &&
            Objects.equals(text, that.text) &&
            Objects.equals(icon, that.icon) &&
            Objects.equals(dynamicAttachments, that.dynamicAttachments) &&
            Arrays.equals(to, that.to) &&
            Arrays.equals(attachments, that.attachments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, text, icon, attachments, dynamicAttachments);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return toXContent(builder, params, true);
    }

    public XContentBuilder toXContent(XContentBuilder builder, Params params, boolean includeTargets) throws IOException {
        builder.startObject();
        if (from != null) {
            builder.field(XField.FROM.getPreferredName(), from);
        }
        if (includeTargets) {
            if (to != null) {
                builder.array(XField.TO.getPreferredName(), to);
            }
        }
        if (icon != null) {
            builder.field(XField.ICON.getPreferredName(), icon);
        }
        if (text != null) {
            builder.field(XField.TEXT.getPreferredName(), text);
        }
        if (attachments != null) {
            builder.startArray(XField.ATTACHMENTS.getPreferredName());
            for (Attachment attachment : attachments) {
                attachment.toXContent(builder, params);
            }
            builder.endArray();
        }
        if (dynamicAttachments != null) {
            builder.field(XField.DYNAMIC_ATTACHMENTS.getPreferredName(), dynamicAttachments, params);
        }
        return builder.endObject();
    }

    public static SlackMessage render(String watchId, String actionId, TextTemplateEngine engine, Map<String, Object> model,
                                   SlackMessageDefaults defaults, SlackMessage message) {
            String from = message.from != null ? engine.render(new TextTemplate(message.from), model) :
                    defaults.from != null ? defaults.from : watchId;
            String[] to = defaults.to;
            if (message.to != null) {
                to = new String[message.to.length];
                for (int i = 0; i < to.length; i++) {
                    to[i] = engine.render(new TextTemplate(message.to[i]), model);
                }
            }
            String text = message.text != null ? engine.render(new TextTemplate(message.text), model) : defaults.text;
            String icon = message.icon != null ? engine.render(new TextTemplate(message.icon), model) : defaults.icon;
            List<Attachment> attachments = null;
            if (message.attachments != null) {
                attachments = new ArrayList<>();
                for (Attachment attachment : message.attachments) {
                    attachments.add(Attachment.render(engine, model, defaults.attachment, attachment));
                }
            }
            if (message.dynamicAttachments != null) {
                if (attachments == null) {
                    attachments = new ArrayList<>();
                }
                attachments.addAll(DynamicAttachments.render(engine, model, defaults.attachment, message.dynamicAttachments));
            }
            if (attachments == null) {
                return new SlackMessage(from, to, icon, text, null, null);
            }
            return new SlackMessage(from, to, icon, text, attachments.toArray(new Attachment[attachments.size()]), null);
        }

    public static SlackMessage parse(XContentParser parser) throws IOException {
        Builder builder = new Builder();

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (XField.FROM.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    builder.setFrom(parser.text());
                } catch (ElasticsearchParseException pe) {
                    throw new ElasticsearchParseException("could not parse slack message. failed to parse [{}] field", pe,
                        XField.FROM.getPreferredName());
                }
            } else if (XField.TO.match(currentFieldName, parser.getDeprecationHandler())) {
                if (token == XContentParser.Token.START_ARRAY) {
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        try {
                            builder.addTo(parser.text());
                        } catch (ElasticsearchParseException pe) {
                            throw new ElasticsearchParseException("could not parse slack message. failed to parse [{}] field.", pe,
                                XField.TO.getPreferredName());
                        }
                    }
                } else {
                    try {
                        builder.addTo(parser.text());
                    } catch (ElasticsearchParseException pe) {
                        throw new ElasticsearchParseException("could not parse slack message. failed to parse [{}] field", pe,
                            XField.TO.getPreferredName());
                    }
                }
            } else if (XField.TEXT.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    builder.setText(parser.text());
                } catch (ElasticsearchParseException pe) {
                    throw new ElasticsearchParseException("could not parse slack message. failed to parse [{}] field", pe,
                        XField.TEXT.getPreferredName());
                }
            } else if (XField.ICON.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    builder.setIcon(parser.text());
                } catch (ElasticsearchParseException pe) {
                    throw new ElasticsearchParseException("could not parse slack message. failed to parse [{}] field.", pe,
                        XField.ICON.getPreferredName());
                }
            } else if (XField.ATTACHMENTS.match(currentFieldName, parser.getDeprecationHandler())) {
                if (token == XContentParser.Token.START_ARRAY) {
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        try {
                            builder.addAttachments(Attachment.parse(parser));
                        } catch (ElasticsearchParseException pe) {
                            throw new ElasticsearchParseException("could not parse slack message. failed to parse [{}] field.", pe,
                                XField.ATTACHMENTS.getPreferredName());
                        }
                    }
                } else {
                    try {
                        builder.addAttachments(Attachment.parse(parser));
                    } catch (ElasticsearchParseException pe) {
                        throw new ElasticsearchParseException("could not parse slack message. failed to parse [{}] field.", pe,
                            XField.ATTACHMENTS.getPreferredName());
                    }
                }
            } else if (XField.DYNAMIC_ATTACHMENTS.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    builder.setDynamicAttachments(DynamicAttachments.parse(parser));
                } catch (ElasticsearchParseException pe) {
                    throw new ElasticsearchParseException("could not parse slack message. failed to parse [{}] field.", pe,
                        XField.ICON.getPreferredName());
                }
            } else {
                throw new ElasticsearchParseException("could not parse slack message. unknown field [{}].", currentFieldName);
            }
        }

        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        String from;
        final List<String> to = new ArrayList<>();
        String text;
        String icon;
        final List<Attachment> attachments = new ArrayList<>();
        DynamicAttachments dynamicAttachments;

        private Builder() {
        }

        public Builder setFrom(String from) {
            this.from = from;
            return this;
        }

        public Builder addTo(String... to) {
            Collections.addAll(this.to, to);
            return this;
        }


        public Builder setText(String text) {
            this.text = text;
            return this;
        }

        public Builder setIcon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder addAttachments(Attachment... attachments) {
            Collections.addAll(this.attachments, attachments);
            return this;
        }

        public Builder addAttachments(Attachment.Builder... attachments) {
            for (Attachment.Builder attachment : attachments) {
                this.attachments.add(attachment.build());
            }
            return this;
        }

        public Builder setDynamicAttachments(DynamicAttachments dynamicAttachments) {
            this.dynamicAttachments = dynamicAttachments;
            return this;
        }

        public SlackMessage build() {
            String[] to = this.to.isEmpty() ? null : this.to.toArray(new String[this.to.size()]);
            Attachment[] attachments = this.attachments.isEmpty() ? null :
                this.attachments.toArray(new Attachment[this.attachments.size()]);
            return new SlackMessage(from, to, icon, text, attachments, dynamicAttachments);
        }
    }

    interface XField extends MessageElement.XField {
        ParseField FROM = new ParseField("from");
        ParseField TO = new ParseField("to");
        ParseField ICON = new ParseField("icon");
        ParseField ATTACHMENTS = new ParseField("attachments");
        ParseField DYNAMIC_ATTACHMENTS = new ParseField("dynamic_attachments");
    }
}
