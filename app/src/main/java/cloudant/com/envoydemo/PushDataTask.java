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
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;

import java.net.URI;

/**
 * Created by tomblench on 26/07/2016.
 */

// TODO exception handling
public class PushDataTask extends AsyncTask<Void, Void, Void> {

    private String currentUser;
    private DatastoreManager dsm;
    private MainActivity mainActivity;
    private ProgressDialog pd;
    private String envoyDb;
    private String password;

    public PushDataTask(String currentUser,
                        String password,
                        DatastoreManager dsm,
                        MainActivity mainActivity,
                        String envoyDb) {
        this.currentUser = currentUser;
        this.password = password;
        this.dsm = dsm;
        this.mainActivity = mainActivity;
        this.envoyDb = envoyDb;
    }

    @Override
    protected void onPreExecute() {
        pd = mainActivity.doingStuff("Pushing data");
    }

    @Override
    protected Void doInBackground(Void... dontcare) {
        try {
            new DatastoreHelper<Void>(dsm, currentUser) {
                public Void performOnDatastore(Datastore d) throws Exception {
                    // envoy only supports basic auth, so switch it on
                    BasicAuthInterceptor bai = new BasicAuthInterceptor(currentUser + ":" + password);
                    Replicator r = ReplicatorBuilder.push().to(new URI(envoyDb)).from(ds).addRequestInterceptors(bai).build();
                    r.start();
                    while (r.getState() != Replicator.State.COMPLETE && r.getState() != Replicator.State.ERROR) {
                        ;
                    }
                    return null;
                }
            }.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void dontcare) {
        if (pd != null) {
            pd.dismiss();
        }
    }
}