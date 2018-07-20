/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.slack.message;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.watcher.common.text.TextTemplate;
import org.elasticsearch.xpack.watcher.common.text.TextTemplateEngine;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class Action implements MessageElement {

    static final ObjectParser<Action, Void> ACTION_PARSER = new ObjectParser<>("action", Action::new);
    static {
        ACTION_PARSER.declareField(Action::setType, (p, c) -> p.text(), new ParseField("type"), ValueType.STRING);
        ACTION_PARSER.declareField(Action::setUrl, (p, c) -> p.text(), new ParseField("url"), ValueType.STRING);
        ACTION_PARSER.declareField(Action::setText, (p, c) -> p.text(), new ParseField("text"), ValueType.STRING);
        ACTION_PARSER.declareField(Action::setStyle, (p, c) -> p.text(), new ParseField("style"), ValueType.STRING);
        ACTION_PARSER.declareField(Action::setName, (p, c) -> p.text(), new ParseField("name"), ValueType.STRING);
    }

    private static final ParseField URL = new ParseField("url");
    private static final ParseField TYPE = new ParseField("type");
    private static final ParseField TEXT = new ParseField("text");
    private static final ParseField STYLE = new ParseField("style");
    private static final ParseField NAME = new ParseField("name");

    private String style;
    private String name;
    private String type;
    private String text;
    private String url;

    public Action() {
    }

    public Action(String style, String name, String type, String text, String url) {
        this.style = style;
        this.name = name;
        this.type = type;
        this.text = text;
        this.url = url;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Action template = (Action) o;

        return Objects.equals(style, template.style) && Objects.equals(type, template.type) && Objects.equals(url, template.url)
                && Objects.equals(text, template.text) && Objects.equals(name, template.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(style, type, url, name, text);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
                .field(NAME.getPreferredName(), name)
                .field(STYLE.getPreferredName(), style)
                .field(TYPE.getPreferredName(), type)
                .field(TEXT.getPreferredName(), text)
                .field(URL.getPreferredName(), url)
                .endObject();
    }

    public static Action render(TextTemplateEngine engine, Map<String, Object> model, Action action) {
        String style = engine.render(new TextTemplate(action.style), model);
        String type = engine.render(new TextTemplate(action.type), model);
        String url = engine.render(new TextTemplate(action.url), model);
        String name = engine.render(new TextTemplate(action.name), model);
        String text = engine.render(new TextTemplate(action.text), model);
        return new Action(style, name, type, text, url);
    }
}
