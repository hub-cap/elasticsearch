package org.elasticsearch.xpack.watcher.notification.slack.message.json;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

public class JsonValue implements ToXContentObject {
    private enum Type { PRIMITIVE, ARRAY, OBJECT }
    private final Type type;
    protected final Object value;

    public JsonValue(String value) {
        this.type = Type.PRIMITIVE;
        this.value = value;
    }

    public JsonValue(JsonArray value) {
        this.type = Type.ARRAY;
        this.value = value;
    }

    public JsonValue(JsonObject value) {
        this.type = Type.OBJECT;
        this.value = value;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        switch (type) {
            case PRIMITIVE: {
                builder.value(value);
                break;
            }
            case ARRAY: {
                ((JsonArray) value).toXContent(builder, params);
                break;
            }
            case OBJECT: {
                ((JsonObject) value).toXContent(builder, params);
                break;
            }
        }

        return builder;
    }

    public static JsonValue parse(XContentParser parser) throws IOException {
        XContentParser.Token token = parser.currentToken();
        if (XContentParser.Token.VALUE_STRING == token) {
            return new JsonValue(parser.text());
        } else if (XContentParser.Token.START_OBJECT == token) {
            return new JsonValue(JsonObject.parse(parser));
        } else if (XContentParser.Token.START_ARRAY == token) {
            return new JsonValue(JsonArray.parse(parser));
        }
        throw new ElasticsearchParseException("Could not parse entity");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return value.equals(((JsonValue) obj).value);
    }
}
