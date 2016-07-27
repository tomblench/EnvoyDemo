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
import android.util.Pair;

import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DocumentRevision;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by tomblench on 26/07/2016.
 */

public class PopulateMapTask extends AsyncTask<Void, Void, AsyncResult<List<Pair<String,MarkerOptions>>>> {

    private MapAdaptor ma;
    private String currentUser;
    private DatastoreManager dsm;
    private MainActivity mainActivity;
    private ProgressDialog pd;

    public PopulateMapTask(MainActivity mainActivity, String user, DatastoreManager dsm, MapAdaptor ma) {
        this.ma = ma;
        this.dsm = dsm;
        this.currentUser = user;
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onPreExecute() {
        pd = mainActivity.doingStuff("Populating data");
    }

    @Override
    protected AsyncResult<List<Pair<String,MarkerOptions>>> doInBackground(Void... dontcare) {
        try {
            return new DatastoreHelper<AsyncResult<List<Pair<String,MarkerOptions>>>>(dsm, currentUser) {
                public AsyncResult<List<Pair<String,MarkerOptions>>> performOnDatastore(Datastore ds) throws Exception {
                    // find all locations and populate with map with markers for each location
                    final List<Pair<String,MarkerOptions>> markers = new ArrayList<>();
                    for (String id : ds.getAllDocumentIds()) {
                        DocumentRevision d = ds.getDocument(id);
                        Map<String, Object> data = d.getBody().asMap();
                        if (data.containsKey("customer_location")) {
                            Map<String, Double> location = (Map<String, Double>) data.get("customer_location");
                            double lat = location.get("lat");
                            double lon = location.get("lon");
                            MarkerOptions mo = newPoint(lat, lon, (String) data.get("customer_name"), (String) data.get("comments"), id);
                            Pair<String, MarkerOptions> pair = new Pair<>(id, mo);
                            markers.add(pair);
                        }
                    }
                    return new AsyncResult<List<Pair<String, MarkerOptions>>>(markers);
                }
            }.run();
        } catch (Exception e) {
            return new AsyncResult<List<Pair<String, MarkerOptions>>>(e);
        }
    }

    public MarkerOptions newPoint(double lat, double lon, String title, String comments, String id) {
        MarkerOptions mo = new MarkerOptions()
                .position(new LatLng(lat, lon))
                .snippet(comments)
                .title(title);
        return mo;
    }

    @Override
    protected void onPostExecute(AsyncResult<List<Pair<String,MarkerOptions>>> result) {
        if (pd != null) {
            pd.dismiss();
        }
        if (result.exception != null) {
            mainActivity.errorDialog(result.exception.getMessage());
        } else {
            ma.addPoints(result.result);
        }
    }

}
