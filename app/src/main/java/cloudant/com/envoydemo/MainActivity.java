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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DocumentBodyFactory;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private String envoyDb = "http://10.0.2.2:8001/db";
    private Map<String, String> users;
    private DatastoreManager dsm;
    private MapAdaptor mapAdaptor;
    private String currentUser;
    private boolean loggedIn = false;
    private String selectedId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup the google map
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // setup our datastore manager
        File path = this.getApplicationContext().getDir("my-data", Context.MODE_PRIVATE);
        dsm = new DatastoreManager(path);

        populateUsers();
    }

    @Override
    public boolean onMarkerClick(final com.google.android.gms.maps.model.Marker marker) {

        final TextView tv = (TextView) findViewById(R.id.textView);
        final CheckBox cb = (CheckBox) findViewById(R.id.checkBox);
        try {
            new DatastoreHelper<Void>(dsm, currentUser) {
                public Void performOnDatastore(Datastore d) throws Exception {
                    selectedId = mapAdaptor.idForMarker(marker);
                    Map data = ds.getDocument(selectedId).getBody().asMap();
                    tv.setText((String) data.get("comments"));
                    cb.setChecked((Boolean)data.get("completed"));
                    return null;
                }
            }.run();
        } catch (Exception e) {
            errorDialog(e.getMessage());
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.mapAdaptor = new MapAdaptor(map);
        map.setOnMarkerClickListener(this);
    }

    public void onLoginLogout(View v) {
        if (loggedIn) {
            onLogout(v);
        } else {
            onLogin(v);
        }
        loggedIn = !loggedIn;
        Button b = (Button) findViewById(R.id.button);
        b.setText(loggedIn ? "Logout" : "Login");
    }

    public void onLogin(final View v) {
        try {
            final TextView tv = (TextView) findViewById(R.id.textView);
            final CheckBox cb = (CheckBox) findViewById(R.id.checkBox);
            tv.setText("");
            cb.setChecked(false);
            // get selected user
            Spinner s = (Spinner) findViewById(R.id.spinner);
            currentUser = (String) s.getSelectedItem();
            pullData(v);
        } catch (Exception e) {
            errorDialog(e.getMessage());
        }
    }

    public void onLogout(View v) {
        try {
            // get selected user
            Spinner s = (Spinner)findViewById(R.id.spinner);
            currentUser = (String)s.getSelectedItem();
            pushData();
            final TextView tv = (TextView) findViewById(R.id.textView);
            final CheckBox cb = (CheckBox) findViewById(R.id.checkBox);
            tv.setText("");
            cb.setChecked(false);
            mapAdaptor.clearMap();
        } catch (Exception e) {
            errorDialog(e.getMessage());
        }
    }

    public void onSave(View v) {
        final TextView tv = (TextView) findViewById(R.id.textView);
        final CheckBox cb = (CheckBox) findViewById(R.id.checkBox);
        try {
            new DatastoreHelper<Void>(dsm, currentUser) {
                public Void performOnDatastore(Datastore d) throws Exception {
                    DocumentRevision toUpdate = ds.getDocument(selectedId);
                    Map data = toUpdate.getBody().asMap();
                    data.put("comments", tv.getText().toString());
                    data.put("completed", cb.isChecked());
                    toUpdate.setBody(DocumentBodyFactory.create(data));
                    ds.updateDocumentFromRevision(toUpdate);
                    return null;
                }
            }.run();
        } catch (Exception e) {
            errorDialog(e.getMessage());
        }

    }

    public void populateUsers() {
        // populate username/password pairs
        users = new HashMap<>();
        users.put("rita", "password");
        users.put("sue", "password");
        users.put("bob", "password");
        // sync to dropdown
        Spinner s = (Spinner)findViewById(R.id.spinner);
        List<String> names = new ArrayList<>(users.keySet());
        ArrayAdapter<String> aa = new ArrayAdapter<>(s.getContext(), R.layout.support_simple_spinner_dropdown_item, names);
        s.setAdapter(aa);
    }

    // TODO don't run on gui thread
    public void pullData(final View v) {

        // pull any new data from server and display results on map
        try {
            new DatastoreHelper<Void>(dsm, currentUser) {
                public Void performOnDatastore(Datastore d) throws Exception {
                    // envoy only supports basic auth, so switch it on
                    BasicAuthInterceptor bai = new BasicAuthInterceptor(currentUser + ":" + users.get(currentUser));
                    Replicator r = ReplicatorBuilder.pull().from(new URI(envoyDb)).to(ds).addRequestInterceptors(bai).build();
                    r.start();
                    while (r.getState() != Replicator.State.COMPLETE && r.getState() != Replicator.State.ERROR) {
                        ;
                    }
                    mapAdaptor.populateMap(d);
                    return null;
                }
            }.run();
        } catch (Exception e) {
            errorDialog(e.getMessage());
        }
    }

    // TODO don't run on gui thread
    public void pushData() {

        // push data (should we clear map?)
        try {
            new DatastoreHelper<Void>(dsm, currentUser) {
                public Void performOnDatastore(Datastore d) throws Exception {
                    // envoy only supports basic auth, so switch it on
                    BasicAuthInterceptor bai = new BasicAuthInterceptor(currentUser + ":" + users.get(currentUser));
                    Replicator r = ReplicatorBuilder.push().to(new URI(envoyDb)).from(ds).addRequestInterceptors(bai).build();
                    r.start();
                    while (r.getState() != Replicator.State.COMPLETE && r.getState() != Replicator.State.ERROR) {
                        ;
                    }
                    return null;
                }
            }.run();
        } catch (Exception e) {
            errorDialog(e.getMessage());
        }
    }

    private void errorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle("Error")
                .setPositiveButton(getString(R.string.accept), null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private AlertDialog doingStuff(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle("Doing Stuff...")
                .setPositiveButton(getString(R.string.accept), null);
        AlertDialog dialog = builder.create();
        return dialog;
    }


}
