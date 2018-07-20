/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.hipchat;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HipChatMessage implements ToXContentObject {

    final String body;
    @Nullable final String[] rooms;
    @Nullable final String[] users;
    @Nullable final String from;
    @Nullable final Format format;
    @Nullable final Color color;
    @Nullable final Boolean notify;

    public HipChatMessage(String body, String[] rooms, String[] users, String from, Format format, Color color, Boolean notify) {
        this.body = body;
        this.rooms = rooms;
        this.users = users;
        this.from = from;
        this.format = format;
        this.color = color;
        this.notify = notify;
    }

    public String getBody() {
        return body;
    }

    public String[] getRooms() {
        return rooms;
    }

    @Nullable
    public String[] getUsers() {
        return users;
    }

    @Nullable
    public String getFrom() {
        return from;
    }

    @Nullable
    public Format getFormat() {
        return format;
    }

    @Nullable
    public Color getColor() {
        return color;
    }

    @Nullable
    public Boolean getNotify() {
        return notify;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HipChatMessage that = (HipChatMessage) o;
        return Objects.equals(body, that.body) &&
               Objects.deepEquals(rooms, that.rooms) &&
               Objects.deepEquals(users, that.users) &&
               Objects.equals(from, that.from) &&
               Objects.equals(format, that.format) &&
               Objects.equals(color, that.color) &&
               Objects.equals(notify, that.notify);
    }

    @Override
    public int hashCode() {
        int result = body.hashCode();
        result = 31 * result + (rooms != null ? Arrays.hashCode(rooms) : 0);
        result = 31 * result + (users != null ? Arrays.hashCode(users) : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (notify != null ? notify.hashCode() : 0);
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return toXContent(builder, params, true);
    }

    public XContentBuilder toXContent(XContentBuilder builder, Params params, boolean includeTargets) throws IOException {
        builder.startObject();
        if (from != null) {
            builder.field(Field.FROM.getPreferredName(), from);
        }
        if (includeTargets) {
            if (rooms != null && rooms.length > 0) {
                builder.array(Field.ROOM.getPreferredName(), rooms);
            }
            if (users != null && users.length > 0) {
                builder.array(Field.USER.getPreferredName(), users);
            }
        }
        builder.field(Field.BODY.getPreferredName(), body);
        if (format != null) {
            builder.field(Field.FORMAT.getPreferredName(), format.value());
        }
        if (color != null) {
            builder.field(Field.COLOR.getPreferredName(), color.value());
        }
        if (notify != null) {
            builder.field(Field.NOTIFY.getPreferredName(), notify);
        }
        return builder.endObject();
    }

    public static HipChatMessage parse(XContentParser parser) throws IOException {
        String body = null;
        String[] rooms = null;
        String[] users = null;
        String from = null;
        Color color = null;
        boolean notify = false;
        HipChatMessage.Format messageFormat = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (Field.FROM.match(currentFieldName, parser.getDeprecationHandler())) {
                from = parser.text();
            } else if (Field.ROOM.match(currentFieldName, parser.getDeprecationHandler())) {
                List<String> templates = new ArrayList<>();
                if (token == XContentParser.Token.START_ARRAY) {
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        try {
                            templates.add(parser.text());
                        } catch (ElasticsearchParseException epe) {
                            throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", epe,
                                Field.ROOM.getPreferredName());
                        }
                    }
                } else {
                    try {
                        templates.add(parser.text());
                    } catch (ElasticsearchParseException epe) {
                        throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", epe,
                            Field.ROOM.getPreferredName());
                    }
                }
                rooms = templates.toArray(new String[templates.size()]);
            } else if (Field.USER.match(currentFieldName, parser.getDeprecationHandler())) {
                List<String> templates = new ArrayList<>();
                if (token == XContentParser.Token.START_ARRAY) {
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        try {
                            templates.add(parser.text());
                        } catch (ElasticsearchParseException epe) {
                            throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", epe,
                                Field.USER.getPreferredName());
                        }
                    }
                } else {
                    try {
                        templates.add(parser.text());
                    } catch (ElasticsearchParseException epe) {
                        throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", epe,
                            Field.USER.getPreferredName());
                    }
                }
                users = templates.toArray(new String[templates.size()]);
            } else if (Field.COLOR.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    color = Color.resolve(parser.text(), null);
                } catch (ElasticsearchParseException | IllegalArgumentException e) {
                    throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", e,
                        Field.COLOR.getPreferredName());
                }
            } else if (Field.NOTIFY.match(currentFieldName, parser.getDeprecationHandler())) {
                if (token == XContentParser.Token.VALUE_BOOLEAN) {
                    notify = parser.booleanValue();
                } else {
                    throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field, expected a " +
                        "boolean value but found [{}]", Field.NOTIFY.getPreferredName(), token);
                }
            } else if (Field.BODY.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    body = parser.text();
                } catch (ElasticsearchParseException pe) {
                    throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", pe,
                        Field.BODY.getPreferredName());
                }
            } else if (Field.FORMAT.match(currentFieldName, parser.getDeprecationHandler())) {
                try {
                    messageFormat = HipChatMessage.Format.parse(parser);
                } catch (IllegalArgumentException ilae) {
                    throw new ElasticsearchParseException("failed to parse hipchat message. failed to parse [{}] field", ilae,
                        Field.FORMAT.getPreferredName());
                }
            } else {
                throw new ElasticsearchParseException("failed to parse hipchat message. unexpected field [{}]", currentFieldName);
            }
        }

        if (body == null) {
            throw new ElasticsearchParseException("failed to parse hipchat message. missing required [{}] field",
                Field.BODY.getPreferredName());
        }

        return new HipChatMessage(body, rooms, users, from, messageFormat, color, notify);
    }

    public static class Builder {

        final String body;
        final List<String> rooms = new ArrayList<>();
        final List<String> users = new ArrayList<>();
        @Nullable String from;
        @Nullable Format format;
        @Nullable Color color;
        @Nullable boolean notify;

        public Builder(String body) {
            this.body = body;
        }

        public Builder addRooms(String... rooms) {
            this.rooms.addAll(Arrays.asList(rooms));
            return this;
        }

        public Builder addUsers(String... users) {
            this.users.addAll(Arrays.asList(users));
            return this;
        }

        public Builder setFrom(String from) {
            this.from = from;
            return this;
        }

        public Builder setFormat(Format format) {
            this.format = format;
            return this;
        }

        public Builder setColor(Color color) {
            this.color = color;
            return this;
        }

        public Builder setNotify(boolean notify) {
            this.notify = notify;
            return this;
        }

        public HipChatMessage build() {
            return new HipChatMessage(
                body,
                rooms.isEmpty() ? null : rooms.toArray(new String[rooms.size()]),
                users.isEmpty() ? null : users.toArray(new String[users.size()]),
                from,
                format,
                color,
                notify);
        }
    }

    public enum Color {
        YELLOW, GREEN, RED, PURPLE, GRAY, RANDOM;

        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Color parse(XContentParser parser) throws IOException {
            return Color.valueOf(parser.text().toUpperCase(Locale.ROOT));
        }

        public static Color resolve(String value, Color defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            return Color.valueOf(value.toUpperCase(Locale.ROOT));
        }

        public static Color resolve(Settings settings, String setting, Color defaultValue) {
            return resolve(settings.get(setting), defaultValue);
        }

        public static boolean validate(String value) {
            try {
                Color.valueOf(value.toUpperCase(Locale.ROOT));
                return true;
            } catch (IllegalArgumentException ilae) {
                return false;
            }
        }
    }

    public enum Format {

        TEXT,
        HTML;

        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Format parse(XContentParser parser) throws IOException {
            return Format.valueOf(parser.text().toUpperCase(Locale.ROOT));
        }

        public static Format resolve(String value, Format defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            return Format.valueOf(value.toUpperCase(Locale.ROOT));
        }

        public static Format resolve(Settings settings, String setting, Format defaultValue) {
            return resolve(settings.get(setting), defaultValue);
        }

        public static boolean validate(String value) {
            try {
                Format.valueOf(value.toUpperCase(Locale.ROOT));
                return true;
            } catch (IllegalArgumentException ilae) {
                return false;
            }
        }
    }

    public interface Field {
        ParseField ROOM = new ParseField("room");
        ParseField USER = new ParseField("user");
        ParseField BODY = new ParseField("body");
        ParseField FROM = new ParseField("from");
        ParseField COLOR = new ParseField("color");
        ParseField NOTIFY = new ParseField("notify");
        ParseField FORMAT = new ParseField("format");
    }
}
