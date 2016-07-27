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

import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreException;
import com.cloudant.sync.datastore.DatastoreManager;

/**
 * Created by tomblench on 26/07/2016.
 *
 * Helper to do the following:
 * - Open datastore for a given user name (each datastore is named {user}-db)
 * - Execute the performOnDatastore method which the implementer over-rides
 * - Automatically close datastore either on successful execution or exception
 */
abstract public class DatastoreHelper<R> {

    Datastore ds;

    public DatastoreHelper(DatastoreManager dsm, String user) throws DatastoreException {
        ds = dsm.openDatastore(user+"-db");
    }

    public R run() throws Exception {
        try (AutoCloseableDatastore acds = new AutoCloseableDatastore(ds)) {
            return performOnDatastore(acds.ds);
        } catch (Exception e) {
            throw e;
        }
    }

    abstract R performOnDatastore(Datastore d) throws Exception;

    private class AutoCloseableDatastore implements AutoCloseable {

        final public Datastore ds;

        public AutoCloseableDatastore(Datastore ds) {
            this.ds = ds;
        }

        @Override
        public void close() {
            ds.close();
        }

    }

}
