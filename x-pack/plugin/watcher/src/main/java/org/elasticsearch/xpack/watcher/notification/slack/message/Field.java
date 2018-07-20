/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.slack.message;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.common.text.TextTemplate;
import org.elasticsearch.xpack.watcher.common.text.TextTemplateEngine;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

class Field implements MessageElement {

    final String title;
    final String value;
    final boolean isShort;

    Field(String title, String value, boolean isShort) {
        this.title = title;
        this.value = value;
        this.isShort = isShort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Field field = (Field) o;

        if (isShort != field.isShort) return false;
        if (!title.equals(field.title)) return false;
        return value.equals(field.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, value, isShort);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
                .field(XField.TITLE.getPreferredName(), title)
                .field(XField.VALUE.getPreferredName(), value)
                .field(XField.SHORT.getPreferredName(), isShort)
                .endObject();
    }

    public static Field render(TextTemplateEngine engine, Map<String, Object> model,
                        SlackMessageDefaults.AttachmentDefaults.FieldDefaults defaults, Field field) {
        String title = field.title != null ? engine.render(new TextTemplate(field.title), model) : defaults.title;
        String value = field.value != null ? engine.render(new TextTemplate(field.value), model) : defaults.value;
        boolean isShort = field.isShort;
        return new Field(title, value, isShort);
    }

    public static Field parse(XContentParser parser) throws IOException {

        String title = null;
        String value = null;
        boolean isShort = false;

        XContentParser.Token token = null;
        String currentFieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (XField.TITLE.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    title = parser.text();
                } catch (ElasticsearchParseException pe) {
                    throw new ElasticsearchParseException("could not parse message attachment field. failed to parse [{}] field", pe,
                        XField.TITLE);
                }
            } else if (XField.VALUE.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    value = parser.text();
                } catch (ElasticsearchParseException pe) {
                    throw new ElasticsearchParseException("could not parse message attachment field. failed to parse [{}] field", pe,
                        XField.VALUE);
                }
            } else if (XField.SHORT.match(currentFieldName, parser.getDeprecationHandler())) {
                if (token == XContentParser.Token.VALUE_BOOLEAN) {
                    isShort = parser.booleanValue();
                } else {
                    throw new ElasticsearchParseException("could not parse message attachment field. expected a boolean value for " +
                        "[{}] field, but found [{}]", XField.SHORT, token);
                }
            } else {
                throw new ElasticsearchParseException("could not parse message attachment field. unexpected field [{}]",
                    currentFieldName);
            }
        }

        if (title == null) {
            throw new ElasticsearchParseException("could not parse message attachment field. missing required [{}] field",
                XField.TITLE);
        }
        if (value == null) {
            throw new ElasticsearchParseException("could not parse message attachment field. missing required [{}] field",
                XField.VALUE);
        }
        return new Field(title, value, isShort);
    }

    interface XField extends MessageElement.XField {
        ParseField VALUE = new ParseField("value");
        ParseField SHORT = new ParseField("short");
    }
}
