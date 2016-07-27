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

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.event.Subscribe;
import com.cloudant.sync.notifications.ReplicationCompleted;
import com.cloudant.sync.notifications.ReplicationErrored;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

/**
 * Created by tomblench on 26/07/2016.
 */

public class PullDataTask extends AsyncTask<Void, Void, AsyncResult<Void>> {

    private String currentUser;
    private DatastoreManager dsm;
    private MainActivity mainActivity;
    private ProgressDialog pd;
    private String envoyDb;
    private String password;
    private CountDownLatch latch;
    private AsyncResult<Void> result;

    public PullDataTask(String currentUser,
                        String password,
                        DatastoreManager dsm,
                        MainActivity mainActivity,
                        String envoyDb) {
        this.currentUser = currentUser;
        this.password = password;
        this.dsm = dsm;
        this.mainActivity = mainActivity;
        this.envoyDb = envoyDb;
        this.latch = new CountDownLatch(1);
    }

    @Override
    protected void onPreExecute() {
        pd = mainActivity.doingStuff("Pulling data");
    }

    @Override
    protected AsyncResult<Void> doInBackground(Void... dontcare) {
        try {
            return new DatastoreHelper<AsyncResult<Void>>(dsm, currentUser) {
                public AsyncResult<Void> performOnDatastore(Datastore d) throws Exception {
                    // envoy only supports basic auth, so switch it on
                    BasicAuthInterceptor bai = new BasicAuthInterceptor(currentUser + ":" + password);
                    Replicator r = ReplicatorBuilder.pull().from(new URI(envoyDb)).to(ds).addRequestInterceptors(bai).build();
                    r.getEventBus().register(PullDataTask.this);
                    r.start();
                    latch.await();
                    return result;
                }
            }.run();
        } catch (Exception e) {
            return new AsyncResult<Void>(e);
        }
    }

    @Override
    protected void onPostExecute(AsyncResult<Void> result) {
        if (pd != null) {
            pd.dismiss();
        }
        if (result.exception != null ) {
            mainActivity.errorDialog(result.exception);
        }
    }

    @Subscribe
    public void onReplicationErrored(ReplicationErrored re) {
        result = new AsyncResult<Void>(re.errorInfo.getException());
        latch.countDown();
    }

    @Subscribe
    public void onReplicationCompleted(ReplicationCompleted rc) {
        result = new AsyncResult<Void>((Void)null);
        latch.countDown();
    }

}
