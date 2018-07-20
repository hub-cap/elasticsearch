/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.slack.message;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.core.watcher.support.xcontent.WatcherParams;
import org.elasticsearch.xpack.watcher.common.http.HttpRequest;
import org.elasticsearch.xpack.watcher.common.http.HttpResponse;
import org.elasticsearch.xpack.watcher.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.notification.slack.SentMessages;
import org.elasticsearch.xpack.watcher.test.MockTextTemplateEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class SlackMessageTests extends ESTestCase {

    public void testToXContent() throws Exception {
        String from = randomBoolean() ? null : randomAlphaOfLength(10);
        String[] to = rarely() ? null : new String[randomIntBetween(0, 2)];
        if (to != null) {
            for (int i = 0; i < to.length; i++) {
                to[i] = randomAlphaOfLength(10);
            }
        }
        String icon = randomBoolean() ? null : randomAlphaOfLength(10);
        String text = randomBoolean() ? null : randomAlphaOfLength(50);
        Attachment[] attachments = (text != null && randomBoolean()) ? null : new Attachment[randomIntBetween(0, 2)];
        if (attachments != null) {
            for (int i = 0; i < attachments.length; i++) {
                String fallback = randomBoolean() ? null : randomAlphaOfLength(10);
                String color = randomBoolean() ? null : randomAlphaOfLength(10);
                String pretext = randomBoolean() ? null : randomAlphaOfLength(10);
                String authorName = randomBoolean() ? null : randomAlphaOfLength(10);
                String authorLink = authorName == null || randomBoolean() ? null : randomAlphaOfLength(10);
                String authorIcon = authorName == null || randomBoolean() ? null : randomAlphaOfLength(10);
                String title = randomBoolean() ? null : randomAlphaOfLength(10);
                String titleLink = title == null ||randomBoolean() ? null : randomAlphaOfLength(10);
                String attachmentText = randomBoolean() ? null : randomAlphaOfLength(10);
                Field[] fields = randomBoolean() ? null : new Field[randomIntBetween(0, 2)];
                if (fields != null) {
                    for (int j = 0; j < fields.length; j++) {
                        fields[j] = new Field(randomAlphaOfLength(10), randomAlphaOfLength(10), randomBoolean());
                    }
                }
                String imageUrl = randomBoolean() ? null : randomAlphaOfLength(10);
                String thumbUrl = randomBoolean() ? null : randomAlphaOfLength(10);
                String[] markdownFields = randomBoolean() ? null : new String[]{"pretext"};
                List<Action> actions = new ArrayList<>();
                if (randomBoolean()) {
                    actions.add(new Action("primary", "action_name", "button", "action_text", "https://elastic.co"));
                }
                attachments[i] = new Attachment(fallback, color, pretext, authorName, authorLink, authorIcon, title, titleLink,
                        attachmentText, fields, imageUrl, thumbUrl, markdownFields, actions);
            }
        }

        SlackMessage expected = new SlackMessage(from, to,  icon, text, attachments, null);

        boolean includeTarget = randomBoolean();

        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        writeFieldIfNotNull(builder, "from", from);
        if (includeTarget) {
            writeFieldIfNotNull(builder, "to", to);
        }
        writeFieldIfNotNull(builder, "icon", icon);
        writeFieldIfNotNull(builder, "text", text);
        if (attachments != null) {
            builder.startArray("attachments");
            for (Attachment attachment : attachments) {
                builder.startObject();
                writeFieldIfNotNull(builder, "fallback", attachment.fallback);
                writeFieldIfNotNull(builder, "color", attachment.color);
                writeFieldIfNotNull(builder, "pretext", attachment.pretext);
                writeFieldIfNotNull(builder, "author_name", attachment.authorName);
                writeFieldIfNotNull(builder, "author_link", attachment.authorLink);
                writeFieldIfNotNull(builder, "author_icon", attachment.authorIcon);
                writeFieldIfNotNull(builder, "title", attachment.title);
                writeFieldIfNotNull(builder, "title_link", attachment.titleLink);
                writeFieldIfNotNull(builder, "text", attachment.text);
                if (attachment.fields != null) {
                    builder.startArray("fields");
                    for (Field field : attachment.fields) {
                        builder.startObject();
                        builder.field("title", field.title);
                        builder.field("value", field.value);
                        builder.field("short", field.isShort);
                        builder.endObject();
                    }
                    builder.endArray();
                }
                if (attachment.actions.isEmpty() == false) {
                    builder.startArray("actions");
                    for (Action action : attachment.actions) {
                        action.toXContent(builder, ToXContent.EMPTY_PARAMS);
                    }
                    builder.endArray();
                }
                builder.endObject();
            }
            builder.endArray();
        }
        builder.endObject();

        builder = jsonBuilder();
        if (includeTarget && randomBoolean()) {
            expected.toXContent(builder, ToXContent.EMPTY_PARAMS);
        } else {
            expected.toXContent(builder, ToXContent.EMPTY_PARAMS, includeTarget);
        }

        XContentParser parser = createParser(builder);
        parser.nextToken();

        from = null;
        to = null;
        icon = null;
        text = null;
        attachments = null;

        String currentFieldName = null;
        XContentParser.Token token = parser.currentToken();
        assertThat(token, is(XContentParser.Token.START_OBJECT));
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if ("from".equals(currentFieldName)) {
                from = parser.text();
            } else if ("to".equals(currentFieldName)) {
                List<String> list = new ArrayList<>();
                while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                    list.add(parser.text());
                }
                to = list.toArray(new String[list.size()]);
            } else if ("icon".equals(currentFieldName)) {
                icon = parser.text();
            } else if ("text".equals(currentFieldName)) {
                text = parser.text();
            } else if ("attachments".equals(currentFieldName)) {
                List<Attachment> list = new ArrayList<>();
                while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                    assertThat(token, is(XContentParser.Token.START_OBJECT));
                    String fallback = null;
                    String color = null;
                    String pretext = null;
                    String authorName = null;
                    String authorLink = null;
                    String authorIcon = null;
                    String title = null;
                    String titleLink = null;
                    String attachmentText = null;
                    Field[] fields = null;
                    String imageUrl = null;
                    String thumbUrl = null;
                    String[] markdownSupportedFields = null;
                    List<Action> actions = new ArrayList<>();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                        if (token == XContentParser.Token.FIELD_NAME) {
                            currentFieldName = parser.currentName();
                        } else if ("fallback".equals(currentFieldName)) {
                            fallback = parser.text();
                        } else if ("color".equals(currentFieldName)) {
                            color = parser.text();
                        } else if ("pretext".equals(currentFieldName)) {
                            pretext = parser.text();
                        } else if ("author_name".equals(currentFieldName)) {
                            authorName = parser.text();
                        } else if ("author_link".equals(currentFieldName)) {
                            authorLink = parser.text();
                        } else if ("author_icon".equals(currentFieldName)) {
                            authorIcon = parser.text();
                        } else if ("title".equals(currentFieldName)) {
                            title = parser.text();
                        } else if ("title_link".equals(currentFieldName)) {
                            titleLink = parser.text();
                        } else if ("text".equals(currentFieldName)) {
                            attachmentText = parser.text();
                        } else if ("fields".equals(currentFieldName)) {
                            List<Field> fieldList = new ArrayList<>();
                            while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                                assertThat(token, is(XContentParser.Token.START_OBJECT));
                                String fieldTitle = null;
                                String fieldValue = null;
                                boolean isShort = false;
                                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                                    if (token == XContentParser.Token.FIELD_NAME) {
                                        currentFieldName = parser.currentName();
                                    } else if ("title".equals(currentFieldName)) {
                                        fieldTitle = parser.text();
                                    } else if ("value".equals(currentFieldName)) {
                                        fieldValue = parser.text();
                                    } else if ("short".equals(currentFieldName)) {
                                        isShort = parser.booleanValue();
                                    }
                                }
                                fieldList.add(new Field(fieldTitle, fieldValue, isShort));
                            }
                            fields = fieldList.toArray(new Field[fieldList.size()]);
                        } else if ("actions".equals(currentFieldName)) {
                            MockTextTemplateEngine engine = new MockTextTemplateEngine();
                            while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                                Action action = new Action();
                                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                                    if (token == XContentParser.Token.FIELD_NAME) {
                                        currentFieldName = parser.currentName();
                                    } else if (token.isValue()) {
                                        switch (currentFieldName) {
                                            case "url":
                                                action.setUrl(parser.text());
                                                break;
                                            case "name":
                                                action.setName(parser.text());
                                                break;
                                            case "style":
                                                action.setStyle(parser.text());
                                                break;
                                            case "text":
                                                action.setText(parser.text());
                                                break;
                                            case "type":
                                                action.setType(parser.text());
                                                break;
                                        }
                                    }

                                }
                                actions.add(Action.render(engine, Collections.emptyMap(), action));
                            }
                        } else if ("image_url".equals(currentFieldName)) {
                            imageUrl = parser.text();
                        } else if ("thumb_url".equals(currentFieldName)) {
                            thumbUrl = parser.text();
                        } else if ("mrkdwn_in".equals(currentFieldName)) {
                            List<String> data = new ArrayList<>();
                            while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                                data.add(parser.text());
                            }
                            markdownSupportedFields = data.toArray(new String[]{});
                        }
                    }
                    list.add(new Attachment(fallback, color, pretext, authorName, authorLink, authorIcon, title, titleLink,
                            attachmentText, fields, imageUrl, thumbUrl, markdownSupportedFields, actions));
                }
                attachments = list.toArray(new Attachment[list.size()]);
            }
        }

        if (!includeTarget) {
            assertThat(to, nullValue());
            to = expected.to;
        }

        SlackMessage actual = new SlackMessage(from, to, icon, text, attachments, null);

        assertThat(actual, equalTo(expected));
    }

    public void testTemplateParse() throws Exception {
        ToXContent.Params params = ToXContent.EMPTY_PARAMS;
        XContentBuilder jsonBuilder = jsonBuilder();
        jsonBuilder.startObject();

        String from = null;
        if (randomBoolean()) {
            from = randomAlphaOfLength(200);
            jsonBuilder.field("from", from);
        }
        String[] to = null;
        if (randomBoolean()) {
            jsonBuilder.startArray("to");
            to = new String[randomIntBetween(1, 3)];
            for (int i = 0; i < to.length; i++) {
                to[i] = randomAlphaOfLength(10);
                jsonBuilder.value(to[i]);
            }
            jsonBuilder.endArray();
        }
        String text = null;
        if (randomBoolean()) {
            text = randomAlphaOfLength(200);
            jsonBuilder.field("text", text);
        }
        String icon = null;
        if (randomBoolean()) {
            icon = randomAlphaOfLength(10);
            jsonBuilder.field("icon", icon);
        }
        Attachment[] attachments = null;
        if (text == null || randomBoolean()) {
            jsonBuilder.startArray("attachments");
            attachments = new Attachment[randomIntBetween(1, 3)];
            for (int i = 0; i < attachments.length; i++) {
                jsonBuilder.startObject();
                String fallback = null;
                if (randomBoolean()) {
                    fallback = randomAlphaOfLength(200);
                    jsonBuilder.field("fallback", fallback);
                }
                String color = null;
                if (randomBoolean()) {
                    color = randomAlphaOfLength(200);
                    jsonBuilder.field("color", color);
                }
                String pretext = null;
                if (randomBoolean()) {
                    pretext = randomAlphaOfLength(200);
                    jsonBuilder.field("pretext", pretext);
                }
                String authorName = null;
                String authorLink = null;
                String authorIcon = null;
                if (randomBoolean()) {
                    authorName = randomAlphaOfLength(200);
                    jsonBuilder.field("author_name", authorName);
                    if (randomBoolean()) {
                        authorLink = randomAlphaOfLength(200);
                        jsonBuilder.field("author_link", authorLink);
                    }
                    if (randomBoolean()) {
                        authorIcon = randomAlphaOfLength(200);
                        jsonBuilder.field("author_icon", authorIcon);
                    }
                }
                String title = null;
                String titleLink = null;
                if (randomBoolean()) {
                    title = randomAlphaOfLength(200);
                    jsonBuilder.field("title", title);
                    if (randomBoolean()) {
                        titleLink = randomAlphaOfLength(200);
                        jsonBuilder.field("title_link", titleLink);
                    }
                }
                String attachmentText = null;
                if (randomBoolean()) {
                    attachmentText = randomAlphaOfLength(200);
                    jsonBuilder.field("text", attachmentText);
                }
                String imageUrl = null;
                if (randomBoolean()) {
                    imageUrl = randomAlphaOfLength(200);
                    jsonBuilder.field("image_url", imageUrl);
                }
                String thumbUrl = null;
                if (randomBoolean()) {
                    thumbUrl = randomAlphaOfLength(200);
                    jsonBuilder.field("thumb_url", thumbUrl);
                }
                Field[] fields = null;
                if (randomBoolean()) {
                    jsonBuilder.startArray("fields");
                    fields = new Field[randomIntBetween(1,3)];
                    for (int j = 0; j < fields.length; j++) {
                        jsonBuilder.startObject();
                        String fieldTitle = randomAlphaOfLength(50);
                        jsonBuilder.field("title", fieldTitle);
                        String fieldValue = randomAlphaOfLength(50);
                        jsonBuilder.field("value", fieldValue);
                        boolean isShort = randomBoolean();
                        jsonBuilder.field("short", isShort);
                        fields[j] = new Field(fieldTitle, fieldValue, isShort);
                        jsonBuilder.endObject();
                    }
                    jsonBuilder.endArray();
                }
                String[] markdownSupportedFields = null;
                if (randomBoolean()) {
                    jsonBuilder.startArray("mrkdwn_in");
                    jsonBuilder.value("pretext");
                    jsonBuilder.endArray();
                    markdownSupportedFields = new String[] { "pretext" };
                }
                List<Action> actions = new ArrayList<>();
                if (randomBoolean()) {
                    jsonBuilder.startArray("actions");
                    jsonBuilder.startObject();
                    jsonBuilder.field("type", "button");
                    jsonBuilder.field("text", "My text");
                    jsonBuilder.field("url", "https://elastic.co");
                    String style = randomFrom("primary", "danger");
                    jsonBuilder.field("style", style);
                    jsonBuilder.field("name", "somebuttonparty");
                    jsonBuilder.endObject();
                    jsonBuilder.endArray();
                    Action action = new Action();
                    action.setName("somebuttonparty");
                    action.setStyle(style);
                    action.setText("My text");
                    action.setType("button");
                    action.setUrl("https://elastic.co");
                    actions.add(action);
                }
                jsonBuilder.endObject();
                attachments[i] = new Attachment(fallback, color, pretext, authorName, authorLink, authorIcon, title,
                        titleLink, attachmentText, fields, imageUrl, thumbUrl, markdownSupportedFields, actions);
            }
            jsonBuilder.endArray();
        }
        jsonBuilder.endObject();

        XContentParser parser = createParser(jsonBuilder);
        parser.nextToken();
        assertThat(parser.currentToken(), is(XContentParser.Token.START_OBJECT));

        SlackMessage template = SlackMessage.parse(parser);
        assertThat(template, notNullValue());
        assertThat(template.from, is(from));
        if (to == null) {
            assertThat(template.to, nullValue());
        } else {
            assertThat(template.to, arrayContaining(to));
        }
        assertThat(template.icon, is(icon));
        assertThat(template.text, is(text));
        if (attachments == null) {
            assertThat(template.attachments, nullValue());
        } else {
            for (int i = 0; i < attachments.length; i++) {
                assertThat(template.attachments[i], is(attachments[i]));
            }
        }
    }

    public void testTemplateParseSelfGenerated() throws Exception {
        SlackMessage template = createRandomTemplate();

        XContentBuilder jsonBuilder = jsonBuilder();
        template.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);

        XContentParser parser = createParser(jsonBuilder);
        parser.nextToken();

        SlackMessage parsed = SlackMessage.parse(parser);

        assertThat(parsed, equalTo(template));
    }

    public void testTemplateRender() throws Exception {
        Settings settings = SlackMessageDefaultsTests.randomSettings();
        SlackMessageDefaults defaults = new SlackMessageDefaults(settings);
        SlackMessage.Builder templateBuilder = SlackMessage.builder();

        if (randomBoolean()) {
            templateBuilder.setFrom(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            int count = randomIntBetween(0, 3);
            for (int i = 0; i < count; i++) {
                templateBuilder.addTo(randomAlphaOfLength(10));
            }
        }
        if (randomBoolean()) {
            templateBuilder.setIcon(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            templateBuilder.setText(randomAlphaOfLength(10));
        }
        if (templateBuilder.text == null || randomBoolean()) {
            // ensure at least one attachment in the event the text is null
            int minimumAttachments = templateBuilder.text == null ? 1 : 0;
            int count = randomIntBetween(minimumAttachments, 3);
            for (int i = 0; i < count; i++) {
                Attachment.Builder attachmentBuilder = createRandomAttachmentTemplateBuilder();
                templateBuilder.addAttachments(attachmentBuilder);
            }
        }

        // relies on the fact that all the templates we use are inline templates without param place holders
        TextTemplateEngine engine = new MockTextTemplateEngine();

        SlackMessage template = templateBuilder.build();

        SlackMessage message = SlackMessage.render("_w1", "_a1", engine, Collections.emptyMap(), defaults, template);
        assertThat(message, notNullValue());
        if (template.from != null) {
            assertThat(message.from, is(template.from));
        } else {
            assertThat(message.from, is(defaults.from != null ? defaults.from : "_w1"));
        }
        if (template.to == null) {
            assertThat(message.to, is(defaults.to));
        } else {
            String[] expected = new String[message.to.length];
            for (int i = 0; i < expected.length; i++) {
                expected[i] = template.to[i];
            }
            assertThat(message.to, arrayContaining(expected));
        }
        assertThat(message.icon, is(template.icon != null ? template.icon : defaults.icon));
        assertThat(message.text, is(template.text != null ? template.text : defaults.text));
        if (template.attachments == null) {
            assertThat(message.attachments, nullValue());
        } else {
            for (int i = 0; i < template.attachments.length; i++) {
                Attachment attachmentTemplate = template.attachments[i];
                Attachment attachment = message.attachments[i];
                assertThat(attachment.authorName, is(attachmentTemplate.authorName != null ? attachmentTemplate.authorName
                        : defaults.attachment.authorName));
                assertThat(attachment.authorLink, is(attachmentTemplate.authorLink != null ? attachmentTemplate.authorLink
                        : defaults.attachment.authorLink));
                assertThat(attachment.authorIcon, is(attachmentTemplate.authorIcon != null ? attachmentTemplate.authorIcon
                        : defaults.attachment.authorIcon));
                assertThat(attachment.color, is(attachmentTemplate.color != null ? attachmentTemplate.color
                        : defaults.attachment.color));
                assertThat(attachment.fallback, is(attachmentTemplate.fallback != null ? attachmentTemplate.fallback
                        : defaults.attachment.fallback));
                assertThat(attachment.imageUrl, is(attachmentTemplate.imageUrl != null ? attachmentTemplate.imageUrl
                        : defaults.attachment.imageUrl));
                assertThat(attachment.pretext, is(attachmentTemplate.pretext != null ? attachmentTemplate.pretext
                        : defaults.attachment.pretext));
                assertThat(attachment.thumbUrl, is(attachmentTemplate.thumbUrl != null ? attachmentTemplate.thumbUrl
                        : defaults.attachment.thumbUrl));
                assertThat(attachment.title, is(attachmentTemplate.title != null ? attachmentTemplate.title
                        : defaults.attachment.title));
                assertThat(attachment.titleLink, is(attachmentTemplate.titleLink != null ? attachmentTemplate.titleLink
                        : defaults.attachment.titleLink));
                assertThat(attachment.text, is(attachmentTemplate.text != null ? attachmentTemplate.text
                        : defaults.attachment.text));
                if (attachmentTemplate.fields == null) {
                    assertThat(attachment.fields, nullValue());
                } else {
                    for (int j = 0; j < attachmentTemplate.fields.length; j++) {
                        Field fieldTemplate = attachmentTemplate.fields[j];
                        Field field = attachment.fields[j];
                        assertThat(field.title,
                                is(fieldTemplate.title != null ? fieldTemplate.title: defaults.attachment.field.title));
                        assertThat(field.value,
                                is(fieldTemplate.value != null ? fieldTemplate.value : defaults.attachment.field.value));
                        assertThat(field.isShort,
                                is(fieldTemplate.isShort));
                    }
                }
                if (attachmentTemplate.markdownSupportedFields == null) {
                    assertThat(attachment.markdownSupportedFields, nullValue());
                } else {
                    for (int j = 0; j < attachmentTemplate.markdownSupportedFields.length; j++) {
                        String[] templateMarkdownSupportFields = Arrays.stream(attachmentTemplate.markdownSupportedFields)
                                .toArray(String[]::new);

                        assertThat(attachment.markdownSupportedFields, arrayContainingInAnyOrder(templateMarkdownSupportFields));
                    }
                }
            }
        }
    }

    // the url path contains sensitive information, which should not be exposed
    public void testUrlPathIsFiltered() throws Exception {
        HttpResponse response = new HttpResponse(500);
        String path = randomAlphaOfLength(20);
        HttpRequest request = HttpRequest.builder("localhost", 1234).path(path).build();
        SlackMessage slackMessage = new SlackMessage("from", new String[] {"to"}, "icon", "text", null, null);
        SentMessages sentMessages = new SentMessages("foo",
                Arrays.asList(SentMessages.SentMessage.responded("recipient", slackMessage, request, response)));

        try (XContentBuilder builder = jsonBuilder()) {
            WatcherParams params = WatcherParams.builder().hideSecrets(false).build();
            sentMessages.toXContent(builder, params);
            assertThat(Strings.toString(builder), containsString(path));

            try (XContentParser parser = builder.contentType().xContent()
                    .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                            Strings.toString(builder))) {
                parser.map();
            }
        }
        try (XContentBuilder builder = jsonBuilder()) {
            sentMessages.toXContent(builder, ToXContent.EMPTY_PARAMS);
            assertThat(Strings.toString(builder), not(containsString(path)));

            try (XContentParser parser = builder.contentType().xContent()
                    .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                            Strings.toString(builder))) {
                parser.map();
            }
        }
    }

    public void testCanHaveNullText()  throws Exception {
        SlackMessage slackMessage = new SlackMessage("from", new String[] {"to"}, "icon", null, new Attachment[1], null);
        assertNull(slackMessage.getText());
        assertNotNull(slackMessage.getAttachments());
    }

    public void testCanHaveNullAttachments()  throws Exception {
        SlackMessage slackMessage = new SlackMessage("from", new String[] {"to"}, "icon", "text", null, null);
        assertNotNull(slackMessage.getText());
        assertNull(slackMessage.getAttachments());
    }

    public void testCannotHaveNullAttachmentsAndNullText() throws Exception {
        expectThrows(IllegalArgumentException.class, () -> new SlackMessage("from", new String[]{"to"}, "icon", null, null, null));
    }

    private static void writeFieldIfNotNull(XContentBuilder builder, String field, Object value) throws IOException {
        if (value != null) {
            builder.field(field, value);
        }
    }

    public static SlackMessage createRandomTemplate() {
        SlackMessage.Builder templateBuilder = SlackMessage.builder();

        if (randomBoolean()) {
            templateBuilder.setFrom(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            int count = randomIntBetween(0, 3);
            for (int i = 0; i < count; i++) {
                templateBuilder.addTo(randomAlphaOfLength(10));
            }
        }
        if (randomBoolean()) {
            templateBuilder.setIcon(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            templateBuilder.setText(randomAlphaOfLength(10));
        }
        if (templateBuilder.text == null || randomBoolean()) {
            // ensure at least one attachment in the event the text is null
            int minimumAttachments = templateBuilder.text == null ? 1 : 0;
            int count = randomIntBetween(minimumAttachments, 3);
            for (int i = 0; i < count; i++) {
                Attachment.Builder attachmentBuilder = createRandomAttachmentTemplateBuilder();
                templateBuilder.addAttachments(attachmentBuilder);
            }
        }

        return templateBuilder.build();
    }

    private static Attachment.Builder createRandomAttachmentTemplateBuilder() {
        Attachment.Builder attachmentBuilder = Attachment.builder();
        if (randomBoolean()) {
            attachmentBuilder.setAuthorName(randomAlphaOfLength(10));
            if (randomBoolean()) {
                attachmentBuilder.setAuthorIcon(randomAlphaOfLength(10));
            }
            if (randomBoolean()) {
                attachmentBuilder.setAuthorLink(randomAlphaOfLength(10));
            }
        }
        if (randomBoolean()) {
            attachmentBuilder.setColor(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            attachmentBuilder.setFallback(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            attachmentBuilder.setImageUrl(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            attachmentBuilder.setPretext(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            attachmentBuilder.setThumbUrl(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            attachmentBuilder.setTitle(randomAlphaOfLength(10));
            if (randomBoolean()) {
                attachmentBuilder.setTitleLink(randomAlphaOfLength(10));
            }
        }
        if (randomBoolean()) {
            attachmentBuilder.setText(randomAlphaOfLength(10));
        }
        if (randomBoolean()) {
            int fieldCount = randomIntBetween(0, 3);
            for (int j = 0; j < fieldCount; j++) {
                attachmentBuilder.addField(randomAlphaOfLength(10), randomAlphaOfLength(10), randomBoolean());
            }
        }
        if (randomBoolean()) {
            attachmentBuilder.addMarkdownField(randomAlphaOfLength(10));
            if (randomBoolean()) {
                attachmentBuilder.addMarkdownField(randomAlphaOfLength(10));
            }
        }

        return attachmentBuilder;
    }
}
