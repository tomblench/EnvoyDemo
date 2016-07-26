package cloudant.com.envoydemo;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreException;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DocumentException;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

//    private String usersDb = "https://buthermseldentakettlybit:533cf03374b915386a70596bf63b75bd7b587467@tomblench.cloudant.com/envoyusers";
    private String envoyDb = "http://10.0.2.2:8001/db";
    private Map<String, String> users;
    private DatastoreManager dsm;
    private MapAdaptor mapAdaptor;
    private String currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
  //      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);



//        setSupportActionBar(toolbar);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        File path = this.getApplicationContext().getDir(
                "my-data",
                Context.MODE_PRIVATE
        );

        dsm = new DatastoreManager(path);



        populateUsers();



    }

    @Override
    public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker) {

        Datastore ds = null;
        TextView tv = (TextView) findViewById(R.id.textView);
        try {


            String id = this.mapAdaptor.IdForMarker(marker);
            ds = dsm.openDatastore(currentUser + "-db");
            Map data = ds.getDocument(id).asMap();
            tv.setText((String) data.get("comments"));
        } catch (Exception e) {
            tv.setText(e.getMessage());
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.mapAdaptor = new MapAdaptor(map);
        map.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onLogin(View v) {
        try {
            // get selected user
            Spinner s = (Spinner)findViewById(R.id.spinner);
            String user = (String)s.getSelectedItem();
            pullData(user);
            currentUser = user;
        } catch (Exception e) {
            System.err.println("ex "+e);
        }
    }


    public void populateUsers() {
        users = new HashMap<String, String>();
        users.put("rita", "password");
        users.put("sue", "password");
        users.put("bob", "password");
        // sync to dropdown
        Spinner s = (Spinner)findViewById(R.id.spinner);
        List<String> names = new ArrayList<String>(users.keySet());
        ArrayAdapter<String> aa = new ArrayAdapter<String>(s.getContext(), R.layout.support_simple_spinner_dropdown_item, names);
        s.setAdapter(aa);
    }

    void populateTable(String user) throws DatastoreException, DocumentException {

        TextView tv = (TextView)findViewById(R.id.textView);
        /*
        ListView lv = (ListView)findViewById(R.id.listView);


        Datastore ds = dsm.openDatastore(user+"-db");
        ArrayList<String> data = new ArrayList<>();

        for(String id : ds.getAllDocumentIds()) {
            DocumentRevision d = ds.getDocument(id);
            data.add(new String(d.getBody().asBytes()));
        }

        ArrayAdapter<String> aa = new ArrayAdapter<String>(lv.getContext(), android.R.layout.simple_list_item_1, data);
        lv.setAdapter(aa);
        ds.close();
*/

    }

    public void pullData(String user) throws DocumentException, DatastoreException, URISyntaxException {
        Datastore ds = dsm.openDatastore(user+"-db");
        BasicAuthInterceptor bai = new BasicAuthInterceptor(user+":"+users.get(user));
        Replicator r = ReplicatorBuilder.pull().from(new URI(envoyDb)).to(ds).addRequestInterceptors(bai).build();
        r.start();
        while(r.getState() != Replicator.State.COMPLETE && r.getState() != Replicator.State.ERROR) {
            ;
        }
        ds.close();
        //System.out.println("size of ds "+ds.getDocumentCount());
        //populateTable(user);
        mapAdaptor.populateMap(dsm, user);



    }


}
