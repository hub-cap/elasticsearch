package org.elasticsearch.xpack.watcher.notification.slack.message.json;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonArray implements ToXContentObject {

    private final List<JsonValue> values;

    public JsonArray(List<JsonValue> values) {
        this.values = values;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startArray();
        {
            for (JsonValue value : values) {
                value.toXContent(builder, params);
            }
        }
        builder.endArray();
        return builder;
    }

    public static JsonArray parse(XContentParser parser) throws IOException {
        if (parser.currentToken() != XContentParser.Token.START_ARRAY) {
            throw new ElasticsearchParseException("Nested object did not begin with START_ARRAY");
        }
        List<JsonValue> jsonValues = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            jsonValues.add(JsonValue.parse(parser));
        }
        return new JsonArray(jsonValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return values.equals(((JsonArray) obj).values);
    }
}
