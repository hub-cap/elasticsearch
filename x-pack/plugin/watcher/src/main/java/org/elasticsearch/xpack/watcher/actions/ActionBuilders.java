/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.actions;

import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.xpack.watcher.common.http.HttpRequestTemplate;
import org.elasticsearch.xpack.watcher.common.text.TextTemplate;
import org.elasticsearch.xpack.watcher.notification.email.EmailTemplate;
import org.elasticsearch.xpack.watcher.notification.pagerduty.IncidentEvent;
import org.elasticsearch.xpack.watcher.notification.slack.message.SlackMessage;
import org.elasticsearch.xpack.watcher.actions.email.EmailAction;
import org.elasticsearch.xpack.watcher.actions.hipchat.HipChatAction;
import org.elasticsearch.xpack.watcher.actions.index.IndexAction;
import org.elasticsearch.xpack.watcher.actions.jira.JiraAction;
import org.elasticsearch.xpack.watcher.actions.logging.LoggingAction;
import org.elasticsearch.xpack.watcher.actions.pagerduty.PagerDutyAction;
import org.elasticsearch.xpack.watcher.actions.slack.SlackAction;
import org.elasticsearch.xpack.watcher.actions.webhook.WebhookAction;

import java.util.Map;

public final class ActionBuilders {

    private ActionBuilders() {
    }

    public static EmailAction.Builder emailAction(EmailTemplate.Builder email) {
        return emailAction(email.build());
    }

    public static EmailAction.Builder emailAction(EmailTemplate email) {
        return EmailAction.builder(email);
    }

    public static IndexAction.Builder indexAction(String index, String type) {
        return IndexAction.builder(index, type);
    }

    public static JiraAction.Builder jiraAction(String account, MapBuilder<String, Object> fields) {
        return jiraAction(account, fields.immutableMap());
    }

    public static JiraAction.Builder jiraAction(String account, Map<String, Object> fields) {
        return JiraAction.builder(account, fields);
    }

    public static WebhookAction.Builder webhookAction(HttpRequestTemplate.Builder httpRequest) {
        return webhookAction(httpRequest.build());
    }

    public static WebhookAction.Builder webhookAction(HttpRequestTemplate httpRequest) {
        return WebhookAction.builder(httpRequest);
    }

    public static LoggingAction.Builder loggingAction(String text) {
        return loggingAction(new TextTemplate(text));
    }

    public static LoggingAction.Builder loggingAction(TextTemplate text) {
        return LoggingAction.builder(text);
    }

    public static HipChatAction.Builder hipchatAction(String body) {
        return hipchatAction(null, body);
    }

    public static HipChatAction.Builder hipchatAction(String account, String body) {
        return HipChatAction.builder(account, body);
    }

    public static SlackAction.Builder slackAction(String account, SlackMessage.Builder message) {
        return slackAction(account, message.build());
    }

    public static SlackAction.Builder slackAction(String account, SlackMessage message) {
        return SlackAction.builder(account, message);
    }

    public static PagerDutyAction.Builder triggerPagerDutyAction(String account, String description) {
        return pagerDutyAction(IncidentEvent.templateBuilder(description).setAccount(account));
    }

    public static PagerDutyAction.Builder pagerDutyAction(IncidentEvent.Builder event) {
        return PagerDutyAction.builder(event.build());
    }

    public static PagerDutyAction.Builder pagerDutyAction(IncidentEvent event) {
        return PagerDutyAction.builder(event);
    }
}
