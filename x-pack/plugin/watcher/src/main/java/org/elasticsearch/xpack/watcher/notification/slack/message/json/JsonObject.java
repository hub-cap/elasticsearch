package org.elasticsearch.xpack.watcher.notification.slack.message.json;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonObject implements ToXContentObject {

    protected final Map<String, JsonValue> values;

    public JsonObject(Map<String, JsonValue> values) {
        this.values = values;
    }

    public JsonObject() {
        this.values = new HashMap<>();
    }

    public void put(String key, JsonValue value) {
        values.put(key, value);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        {
            for (Map.Entry<String, JsonValue> value : values.entrySet()) {
                builder.field(value.getKey());
                value.getValue().toXContent(builder, params);
            }
        }
        builder.endObject();

        return builder;
    }

    public static JsonObject parse(XContentParser parser) throws IOException {
        if (parser.currentToken() == null) {
            // this is the start of a new parse
            if (parser.nextToken() != XContentParser.Token.START_OBJECT) {
                throw new ElasticsearchParseException("Did not begin with START_OBJECT");
            }
        } else {
            if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
                throw new ElasticsearchParseException("Nested object did not begin with START_OBJECT");
            }
        }
        JsonObject jsonObject = new JsonObject();
        while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
            if (parser.currentToken() == XContentParser.Token.FIELD_NAME) {
                String name = parser.currentName();
                parser.nextToken();
                jsonObject.put(name, JsonValue.parse(parser));
            }
        }
        return jsonObject;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return values.equals(((JsonObject) obj).values);
    }
}
