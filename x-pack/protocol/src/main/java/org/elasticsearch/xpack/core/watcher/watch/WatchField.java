/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.xpack.core.watcher.watch;

import org.elasticsearch.common.ParseField;

public final class WatchField {
    public static final String INCLUDE_STATUS_KEY = "include_status";
    public static final ParseField TRIGGER = new ParseField("trigger");
    public static final ParseField INPUT = new ParseField("input");
    public static final ParseField CONDITION = new ParseField("condition");
    public static final ParseField ACTIONS = new ParseField("actions");
    public static final ParseField TRANSFORM = new ParseField("transform");
    public static final ParseField THROTTLE_PERIOD = new ParseField("throttle_period_in_millis");
    public static final ParseField THROTTLE_PERIOD_HUMAN = new ParseField("throttle_period");
    public static final ParseField METADATA = new ParseField("metadata");

    private WatchField() {}
}
