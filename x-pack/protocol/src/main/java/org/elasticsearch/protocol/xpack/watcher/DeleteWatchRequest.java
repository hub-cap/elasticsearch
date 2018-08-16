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
package org.elasticsearch.protocol.xpack.watcher;

import org.elasticsearch.protocol.xpack.common.Validatable;
import org.elasticsearch.protocol.xpack.common.ValidationException;

public class DeleteWatchRequest implements Validatable {
    private final String id;

    public DeleteWatchRequest(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public ValidationException validate() {
        ValidationException validationException = new ValidationException();
        if (id == null){
            validationException.addValidationError("watch id is missing");
        } else if (PutWatchRequest.isValidId(id) == false) {
            validationException.addValidationError("watch id contains whitespace");
        }
        return validationException;
    }
}
