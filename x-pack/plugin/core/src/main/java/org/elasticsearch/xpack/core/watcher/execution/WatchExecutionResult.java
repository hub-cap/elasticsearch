/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.watcher.execution;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.core.watcher.actions.ActionWrapperResult;
import org.elasticsearch.xpack.core.watcher.condition.ConditionResult;
import org.elasticsearch.xpack.core.watcher.input.InputResult;
import org.elasticsearch.xpack.core.watcher.support.WatcherDateTimeUtils;
import org.elasticsearch.xpack.core.watcher.transform.Transform;
import org.elasticsearch.xpack.core.watcher.transform.TransformResult;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;

public class WatchExecutionResult implements ToXContentObject {

    private final DateTime executionTime;
    private final long executionDurationMs;
    @Nullable private final InputResult inputResult;
    @Nullable private final ConditionResult conditionResult;
    @Nullable private final TransformResult transformResult;
    private final Map<String, ActionWrapperResult> actionsResults;

    public WatchExecutionResult(WatchExecutionContext context, long executionDurationMs) {
        this(context.executionTime(), executionDurationMs, context.inputResult(), context.conditionResult(), context.transformResult(),
                context.actionsResults());
    }

    private WatchExecutionResult(DateTime executionTime, long executionDurationMs, InputResult inputResult,
                                 ConditionResult conditionResult, @Nullable TransformResult transformResult,
                                 Map<String, ActionWrapperResult> actionsResults) {
        this.executionTime = executionTime;
        this.inputResult = inputResult;
        this.conditionResult = conditionResult;
        this.transformResult = transformResult;
        this.actionsResults = actionsResults;
        this.executionDurationMs = executionDurationMs;
    }

    public DateTime executionTime() {
        return executionTime;
    }

    public long executionDurationMs() {
        return executionDurationMs;
    }

    public InputResult inputResult() {
        return inputResult;
    }

    public ConditionResult conditionResult() {
        return conditionResult;
    }

    public TransformResult transformResult() {
        return transformResult;
    }

    public Map<String, ActionWrapperResult> actionsResults() {
        return actionsResults;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();

        WatcherDateTimeUtils.writeDate(Field.EXECUTION_TIME.getPreferredName(), builder, executionTime);
        builder.field(Field.EXECUTION_DURATION.getPreferredName(), executionDurationMs);

        if (inputResult != null) {
            builder.field(Field.INPUT.getPreferredName(), inputResult, params);
        }
        if (conditionResult != null) {
            builder.field(Field.CONDITION.getPreferredName(), conditionResult, params);
        }
        if (transformResult != null) {
            builder.field(Transform.TRANSFORM.getPreferredName(), transformResult, params);
        }
        builder.startArray(Field.ACTIONS.getPreferredName());
        for (ActionWrapperResult result : actionsResults.values()) {
            result.toXContent(builder, params);
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    public interface Field {
        ParseField EXECUTION_TIME = new ParseField("execution_time");
        ParseField EXECUTION_DURATION = new ParseField("execution_duration");
        ParseField INPUT = new ParseField("input");
        ParseField CONDITION = new ParseField("condition");
        ParseField ACTIONS = new ParseField("actions");
        ParseField TYPE = new ParseField("type");
    }
}
