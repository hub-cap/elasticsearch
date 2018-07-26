/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.notification.email;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.test.MockTextTemplateEngine;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class EmailTests extends ESTestCase {

    public void testParsingMultipleEmailAddresses() throws Exception {
        Email template = Email.builder()
            .from("sender@example.org")
            .to("to1@example.org, to2@example.org")
            .cc("cc1@example.org, cc2@example.org")
            .bcc("bcc1@example.org, bcc2@example.org")
            .textBody("blah")
            .build();

        Email email = Email.render(new MockTextTemplateEngine(), emptyMap(), null, emptyMap(), template).id("foo").build();

        assertThat(email.to.size(), is(2));
        assertThat(email.to, containsInAnyOrder("to1@example.org", "to2@example.org"));
        assertThat(email.cc.size(), is(2));
        assertThat(email.cc, containsInAnyOrder("cc1@example.org", "cc2@example.org"));
        assertThat(email.bcc.size(), is(2));
        assertThat(email.bcc, containsInAnyOrder("bcc1@example.org", "bcc2@example.org"));
    }
}
