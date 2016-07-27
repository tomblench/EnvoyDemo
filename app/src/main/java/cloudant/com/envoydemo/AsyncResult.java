/*
 *  Copyright (c) 2016 IBM Corp. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *   License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 */

package cloudant.com.envoydemo;

/**
 * Created by tomblench on 27/07/2016.
 *
 * Represent the result of an async task, or the exception that was thrown during execution
 */
public class AsyncResult <T> {

    public final T result;
    public final Throwable exception;

    public AsyncResult(T result) {
        this.result = result;
        this.exception = null;
    }

    public AsyncResult(Throwable exception) {
        this.result = null;
        this.exception = exception;
    }

}
