package cloudant.com.envoydemo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
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
        try {
            new DatastoreHelper<Void>(dsm, currentUser) {
                public Void performOnDatastore(Datastore d) throws Exception {
                    String id = mapAdaptor.idForMarker(marker);
                    Map data = ds.getDocument(id).asMap();
                    tv.setText((String) data.get("comments"));
                    return null;
                }
            }.run();
        } catch (Exception e) {
            // TODO
            ;
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.mapAdaptor = new MapAdaptor(map);
        map.setOnMarkerClickListener(this);
    }

    public void onLogin(View v) {
        try {
            // get selected user
            Spinner s = (Spinner)findViewById(R.id.spinner);
            String user = (String)s.getSelectedItem();
            currentUser = user;
            pullData();
        } catch (Exception e) {
            System.err.println("ex "+e);
        }
    }

    public void populateUsers() {
        // populate
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

    public void pullData() {

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
            // TODO
            ;
        }
    }

}
