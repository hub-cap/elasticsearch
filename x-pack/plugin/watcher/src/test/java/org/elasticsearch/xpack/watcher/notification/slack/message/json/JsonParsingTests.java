package org.elasticsearch.xpack.watcher.notification.slack.message.json;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

public class JsonParsingTests extends ESTestCase {
    public void testSerialize() throws Exception {
        /*
        {
          "account": "monitoring",
          "message": {
            "from": "Elasticsearch Watcher",
            "to": [
              "#channel"
            ],
            "text": "*Something bad happened*",
            "attachments": [
              {
                "color": "warning",
                "title": "Something bad happened",
                "text": "Something bad happened",
                "callback_id": "ack_alert",
                "actions": [
                  {
                    "name": "acknowledge",
                    "value": "acknowledge"
                  }
                ]
              }
            ]
          }
        }
         */


        XContentBuilder builder = XContentFactory.jsonBuilder();
        ToXContent.Params params = ToXContent.EMPTY_PARAMS;

        JsonObject actionObject = new JsonObject();
        actionObject.put("name", new JsonValue("acknowledge"));
        actionObject.put("type", new JsonValue("button"));
        JsonArray actionArray = new JsonArray(Collections.singletonList(new JsonValue(actionObject)));

        JsonObject attachments = new JsonObject();
        attachments.put("attachments", new JsonValue(actionArray));

        attachments.toXContent(builder, params);
        assertThat(Strings.toString(attachments), equalTo("{\"attachments\":[{\"name\":\"acknowledge\",\"type\":\"button\"}]}"));
    }

    public void testSimpleParserValue() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        ToXContent.Params params = ToXContent.EMPTY_PARAMS;

        JsonObject simpleObject = new JsonObject();
        JsonValue value = new JsonValue("value");
        simpleObject.put("key", value);
        simpleObject.toXContent(builder, params);

        JsonObject parsed = JsonObject.parse(createParser(builder));
        assertThat(parsed.values.size(), equalTo(1));
        Map.Entry<String, JsonValue> parsedValue = parsed.values.entrySet().iterator().next();
        assertThat(parsedValue.getKey(), equalTo("key"));
        assertThat(parsedValue.getValue(), equalTo(value));
    }

    public void testNestedParseValue() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        ToXContent.Params params = ToXContent.EMPTY_PARAMS;

        JsonObject simpleObject = new JsonObject();
        JsonValue value = new JsonValue("value");
        simpleObject.put("key", value);

        JsonObject nestedObject = new JsonObject();
        JsonValue nested = new JsonValue(simpleObject);
        nestedObject.put("nested", nested);
        nestedObject.toXContent(builder, params);

        JsonObject parsed = JsonObject.parse(createParser(builder));
        assertThat(parsed.values.size(), equalTo(1));
        Map.Entry<String, JsonValue> outerParsedValue = parsed.values.entrySet().iterator().next();
        assertThat(outerParsedValue.getKey(), equalTo("nested"));
        assertThat(outerParsedValue.getValue(), equalTo(nested));

        JsonObject innerParsedValue = (JsonObject) outerParsedValue.getValue().value;
        assertThat(innerParsedValue, equalTo(simpleObject));
    }

    public void testSimpleArrayParserValue() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        ToXContent.Params params = ToXContent.EMPTY_PARAMS;

        JsonObject list = new JsonObject();
        JsonArray listValues = new JsonArray(Arrays.asList(new JsonValue("a"), new JsonValue("b")));
        list.put("list", new JsonValue(listValues));
        list.toXContent(builder, params);

        JsonObject parsed = JsonObject.parse(createParser(builder));
        Map.Entry<String, JsonValue> parsedValue = parsed.values.entrySet().iterator().next();
        assertThat(parsedValue.getKey(), equalTo("list"));

        JsonArray innerParsedValue = (JsonArray) parsedValue.getValue().value;
        assertThat(innerParsedValue, equalTo(listValues));
    }

    public void testComplexArrayParserValue() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        ToXContent.Params params = ToXContent.EMPTY_PARAMS;

        JsonObject simpleObject = new JsonObject();
        JsonValue value = new JsonValue("value");
        simpleObject.put("key", value);

        JsonObject nestedObject = new JsonObject();
        JsonValue nested = new JsonValue(simpleObject);
        nestedObject.put("nested", nested);

        JsonObject list = new JsonObject();
        JsonArray listValues = new JsonArray(Arrays.asList(new JsonValue("a"), new JsonValue("b"), new JsonValue(nestedObject)));
        list.put("list", new JsonValue(listValues));
        list.toXContent(builder, params);

        JsonObject parsed = JsonObject.parse(createParser(builder));
        Map.Entry<String, JsonValue> parsedValue = parsed.values.entrySet().iterator().next();
        assertThat(parsedValue.getKey(), equalTo("list"));

        JsonArray innerParsedValue = (JsonArray) parsedValue.getValue().value;
        assertThat(innerParsedValue, equalTo(listValues));
    }
}
