/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.email;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

public class EmailTests extends ESTestCase {
    public void testEmailParserSelfGenerated() throws Exception {
        String id = "test-id";
        String from = randomFrom("from@from.com", null);
        List<String> addresses = new ArrayList<>();
        for( int i = 0; i < randomIntBetween(1, 5); ++i){
            addresses.add("address" + i + "@test.com");
        }
        List<String> replyTo = randomFrom(addresses, null);
        Email.Priority priority = randomFrom(Email.Priority.values());
        DateTime sentDate = new DateTime(randomInt(), DateTimeZone.UTC);
        List<String> to = randomFrom(addresses, null);
        List<String> cc = randomFrom(addresses, null);
        List<String> bcc = randomFrom(addresses, null);
        String subject = randomFrom("Random Subject", "", null);
        String textBody = randomFrom("Random Body", "", null);
        String htmlBody = randomFrom("<hr /><b>BODY</b><hr />", "", null);
        Map<String, Attachment> attachments = null;

        Email email = new Email(id, from, replyTo, priority.name(), sentDate, to, cc, bcc, subject, textBody, htmlBody, attachments);

        XContentBuilder builder = XContentFactory.jsonBuilder();
        email.toXContent(builder, ToXContent.EMPTY_PARAMS);

        XContentParser parser = createParser(builder);
        parser.nextToken();

        Email parsedEmail = Email.parse(parser);

        assertThat(email.id, equalTo(parsedEmail.id));
        assertThat(email.from, equalTo(parsedEmail.from));
        assertThat(email.replyTo, equalTo(parsedEmail.replyTo));
        assertThat(email.priority, equalTo(parsedEmail.priority));
        assertThat(email.sentDate, equalTo(parsedEmail.sentDate));
        assertThat(email.to, equalTo(parsedEmail.to));
        assertThat(email.cc, equalTo(parsedEmail.cc));
        assertThat(email.bcc, equalTo(parsedEmail.bcc));
        assertThat(email.subject, equalTo(parsedEmail.subject));
        assertThat(email.textBody, equalTo(parsedEmail.textBody));
        assertThat(email.htmlBody, equalTo(parsedEmail.htmlBody));
    }

}
