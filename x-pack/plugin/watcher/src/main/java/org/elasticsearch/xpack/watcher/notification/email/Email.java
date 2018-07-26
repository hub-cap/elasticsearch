/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.email;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.common.text.TextTemplate;
import org.elasticsearch.xpack.watcher.common.text.TextTemplateEngine;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static java.util.Collections.unmodifiableMap;

public class Email implements ToXContentObject {

    final String id;
    final String from;
    final List<String> replyTo;
    final String priority;
    final DateTime sentDate;
    final List<String> to;
    final List<String> cc;
    final List<String> bcc;
    final String subject;
    final String textBody;
    final String htmlBody;
    final Map<String, Attachment> attachments;

    public Email(String id, String from, List<String> replyTo, String priority, DateTime sentDate,
                 List<String> to, List<String> cc, List<String> bcc, String subject, String textBody, String htmlBody,
                 Map<String, Attachment> attachments) {

        this.id = id;
        this.from = from;
        this.replyTo = replyTo;
        this.priority = priority;
        this.sentDate = sentDate != null ? sentDate : new DateTime(DateTimeZone.UTC);
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
        this.textBody = textBody;
        this.htmlBody = htmlBody;
        this.attachments = attachments;
    }

    public String id() {
        return id;
    }

    public String from() {
        return from;
    }

    public List<String> replyTo() {
        return replyTo;
    }

    public String priority() {
        return priority;
    }

    public DateTime sentDate() {
        return sentDate;
    }

    public List<String> to() {
        return to;
    }

    public List<String> cc() {
        return cc;
    }

    public List<String> bcc() {
        return bcc;
    }

    public String subject() {
        return subject;
    }

    public String textBody() {
        return textBody;
    }

    public String htmlBody() {
        return htmlBody;
    }

    public Map<String, Attachment> attachments() {
        return attachments;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        xContentBody(builder, params);
        return builder.endObject();
    }

    public void xContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field(Field.ID.getPreferredName(), id);
        if (from != null) {
            builder.field(Field.FROM.getPreferredName(), from);
        }
        if (replyTo != null) {
            builder.field(Field.REPLY_TO.getPreferredName(), replyTo);
        }
        if (priority != null) {
            builder.field(Field.PRIORITY.getPreferredName(), priority);
        }
        builder.timeField(Field.SENT_DATE.getPreferredName(), sentDate);
        if (to != null) {
            builder.field(Field.TO.getPreferredName(), to);
        }
        if (cc != null) {
            builder.field(Field.CC.getPreferredName(), cc);
        }
        if (bcc != null) {
            builder.field(Field.BCC.getPreferredName(), bcc);
        }
        builder.field(Field.SUBJECT.getPreferredName(), subject);
        if (textBody != null || htmlBody != null) {
            builder.startObject(Field.BODY.getPreferredName());
            if (textBody != null) {
                builder.field(Field.BODY_TEXT.getPreferredName(), textBody);
            }
            if (htmlBody != null) {
                builder.field(Field.BODY_HTML.getPreferredName(), htmlBody);
            }
            builder.endObject();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Email email = (Email) o;

        if (!id.equals(email.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Email.Builder render(TextTemplateEngine engine, Map<String, Object> model, HtmlSanitizer htmlSanitizer,
                                Map<String, Attachment> attachments, Email email) throws AddressException {
        Email.Builder builder = Email.builder();
        if (email.from != null) {
            builder.from(engine.render(new TextTemplate(email.from), model));
        }
        if (email.replyTo != null) {
            List<String> addresses = templatesToAddressList(engine, email.replyTo, model);
            builder.replyTo(addresses);
        }
        if (email.priority != null) {
            builder.priority(Email.Priority.resolve(engine.render(new TextTemplate(email.priority), model)));
        }
        if (email.to != null) {
            List<String> addresses = templatesToAddressList(engine, email.to, model);
            builder.to(addresses);
        }
        if (email.cc != null) {
            List<String> addresses = templatesToAddressList(engine, email.cc, model);
            builder.cc(addresses);
        }
        if (email.bcc != null) {
            List<String> addresses = templatesToAddressList(engine, email.bcc, model);
            builder.bcc(addresses);
        }
        if (email.subject != null) {
            builder.subject(engine.render(new TextTemplate(email.subject), model));
        }
        if (email.textBody != null) {
            builder.textBody(engine.render(new TextTemplate(email.textBody), model));
        }
        if (attachments != null) {
            for (Attachment attachment : attachments.values()) {
                builder.attach(attachment);
            }
        }
        if (email.htmlBody != null) {
            String renderedHtml = engine.render(new TextTemplate(email.htmlBody), model);
            renderedHtml = htmlSanitizer.sanitize(renderedHtml);
            builder.htmlBody(renderedHtml);
        }
        return builder;
    }

    private static List<String> templatesToAddressList(TextTemplateEngine engine, List<String> items,
                                                            Map<String, Object> model) throws AddressException {
        List<String> addresses = new ArrayList<>(items.size());
        for (String item : items) {
            Email.AddressList.parse(engine.render(new TextTemplate(item), model)).forEach(addresses::add);
        }
        return addresses;
    }

    public static class Builder {

        private String id;
        private String from;
        private List<String> replyTo;
        private String priority;
        private DateTime sentDate;
        private List<String> to;
        private List<String> cc;
        private List<String> bcc;
        private String subject;
        private String textBody;
        private String htmlBody;
        private Map<String, Attachment> attachments = new HashMap<>();

        private Builder() {
        }

        public Builder copyFrom(Email email) {
            id = email.id;
            from = email.from;
            replyTo = email.replyTo;
            priority = email.priority;
            sentDate = email.sentDate;
            to = email.to;
            cc = email.cc;
            bcc = email.bcc;
            subject = email.subject;
            textBody = email.textBody;
            htmlBody = email.htmlBody;
            attachments.putAll(email.attachments);
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder replyTo(List<String> replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        public Builder replyTo(String replyTo) {
            this.replyTo = Collections.singletonList(replyTo);
            return this;
        }

        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public Builder sentDate(DateTime sentDate) {
            this.sentDate = sentDate;
            return this;
        }

        public Builder to(String to) {
            this.to = Collections.singletonList(to);
            return this;
        }

        public Builder to(List<String> to) {
            this.to = to;
            return this;
        }

        public List<String> to() {
            return to;
        }

        public Builder cc(List<String> cc) {
            this.cc = cc;
            return this;
        }

        public Builder cc(String to) {
            this.cc = Collections.singletonList(to);
            return this;
        }


        public Builder bcc(List<String> bcc) {
            this.bcc = bcc;
            return this;
        }

        public Builder bcc(String to) {
            this.bcc = Collections.singletonList(to);
            return this;
        }


        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder textBody(String text) {
            this.textBody = text;
            return this;
        }

        public Builder htmlBody(String html) {
            this.htmlBody = html;
            return this;
        }

        public Builder attach(Attachment attachment) {
            if (attachments == null) {
                throw new IllegalStateException("Email has already been built!");
            }
            attachments.put(attachment.id(), attachment);
            return this;
        }

        /**
         * Build the email. Note that adding items to attachments or inlines
         * after this is called is incorrect.
         */
        public Email build() {
            assert id != null : "email id should not be null"; //fuck.
            Email email = new Email(id, from, replyTo, priority, sentDate, to, cc, bcc, subject, textBody, htmlBody,
                    unmodifiableMap(attachments));
            attachments = null;
            return email;
        }
    }

    public enum Priority {

        HIGHEST(1),
        HIGH(2),
        NORMAL(3),
        LOW(4),
        LOWEST(5);

        static final String HEADER = "X-Priority";

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public static void applyTo(MimeMessage message, String priority) throws MessagingException {
            message.setHeader(HEADER, String.valueOf(resolve(priority, null).value));
        }

        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static String resolve(String name) {
            Priority priority = resolve(name, null);
            if (priority == null) {
                throw new IllegalArgumentException("[" + name + "] is not a valid email priority");
            }
            return priority.name();
        }

        public static Priority resolve(String name, Priority defaultPriority) {
            if (name == null) {
                return defaultPriority;
            }
            switch (name.toLowerCase(Locale.ROOT)) {
                case "highest": return HIGHEST;
                case "high":    return HIGH;
                case "normal":  return NORMAL;
                case "low":     return LOW;
                case "lowest":  return LOWEST;
                default:
                    return defaultPriority;
            }
        }

        public static String parse(Settings settings, String name) {
            String value = settings.get(name);
            if (value == null) {
                return null;
            }
            return resolve(value);
        }
    }

    public static class Address {

        public static final ParseField ADDRESS_NAME_FIELD = new ParseField("name");
        public static final ParseField ADDRESS_EMAIL_FIELD = new ParseField("email");

        public static String parse(String field, XContentParser.Token token, XContentParser parser) throws IOException {
            if (token == XContentParser.Token.VALUE_STRING) {
                String text = parser.text();
                try {
                    return new InternetAddress(parser.text()).toString();
                } catch (AddressException ae) {
                    String msg = "could not parse [" + text + "] in field [" + field + "] as address. address must be RFC822 encoded";
                    throw new ElasticsearchParseException(msg, ae);
                }
            }

            if (token == XContentParser.Token.START_OBJECT) {
                String email = null;
                String name = null;
                String currentFieldName = null;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token == XContentParser.Token.VALUE_STRING) {
                        if (ADDRESS_EMAIL_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            email = parser.text();
                        } else if (ADDRESS_NAME_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            name = parser.text();
                        } else {
                            throw new ElasticsearchParseException("could not parse [" + field + "] object as address. unknown address " +
                                    "field [" + currentFieldName + "]");
                        }
                    }
                }
                if (email == null) {
                    String msg = "could not parse [" + field + "] as address. address object must define an [email] field";
                    throw new ElasticsearchParseException(msg);
                }
                try {
                    return name != null ? new InternetAddress(email, name).toString() : new InternetAddress(email).toString();
                } catch (AddressException ae) {
                    throw new ElasticsearchParseException("could not parse [" + field + "] as address", ae);
                }

            }
            throw new ElasticsearchParseException("could not parse [{}] as address. address must either be a string (RFC822 encoded) or " +
                    "an object specifying the address [name] and [email]", field);
        }

        public static String parse(Settings settings, String name) {
            String value = settings.get(name);
            try {
                return value != null ? new InternetAddress(value).toString() : null;
            } catch (AddressException ae) {
                throw new IllegalArgumentException("[" + value + "] is not a valid RFC822 email address", ae);
            }
        }
    }

    public static class AddressList {

        public static List<String> parse(String text) throws AddressException {
            InternetAddress[] addresses = InternetAddress.parse(text);
            List<String> list = new ArrayList<>(addresses.length);
            for (InternetAddress address : addresses) {
                list.add(new InternetAddress(address.toUnicodeString()).toString());
            }
            return list;
        }

        public static List<String> parse(Settings settings, String name) {
            List<String> addresses = settings.getAsList(name);
            if (addresses == null || addresses.isEmpty()) {
                return null;
            }
            try {
                List<String> list = new ArrayList<>(addresses.size());
                for (String address : addresses) {
                    list.add(new InternetAddress(address).toString());
                }
                return list;
            } catch (AddressException ae) {
                throw new IllegalArgumentException("[" + settings.get(name) + "] is not a valid list of RFC822 email addresses", ae);
            }
        }

        public static List<String> parse(String field, XContentParser.Token token, XContentParser parser) throws IOException {
            if (token == XContentParser.Token.VALUE_STRING) {
                String text = parser.text();
                try {
                    return parse(parser.text());
                } catch (AddressException ae) {
                    throw new ElasticsearchParseException("could not parse field [" + field + "] with value [" + text + "] as address " +
                            "list. address(es) must be RFC822 encoded", ae);
                }
            }
            if (token == XContentParser.Token.START_ARRAY) {
                List<String> addresses = new ArrayList<>();
                while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                    addresses.add(Address.parse(field, token, parser));
                }
                return addresses;
            }
            throw new ElasticsearchParseException("could not parse [" + field + "] as address list. field must either be a string " +
                    "(comma-separated list of RFC822 encoded addresses) or an array of objects representing addresses");
        }
    }

    public interface Field {
        ParseField ID = new ParseField("id");
        ParseField FROM = new ParseField("from");
        ParseField REPLY_TO = new ParseField("reply_to");
        ParseField PRIORITY = new ParseField("priority");
        ParseField SENT_DATE = new ParseField("sent_date");
        ParseField TO = new ParseField("to");
        ParseField CC = new ParseField("cc");
        ParseField BCC = new ParseField("bcc");
        ParseField SUBJECT = new ParseField("subject");
        ParseField BODY = new ParseField("body");
        ParseField BODY_TEXT = new ParseField("text");
        ParseField BODY_HTML = new ParseField("html");
    }

}
