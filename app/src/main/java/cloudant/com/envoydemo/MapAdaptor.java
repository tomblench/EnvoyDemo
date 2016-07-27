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

import android.os.AsyncTask;
import android.util.Pair;

import com.cloudant.sync.datastore.DatastoreManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tomblench on 25/07/2016.
 */
public class MapAdaptor {

    private GoogleMap googleMap;
    private Map<Marker, String> markers; //marker -> id

    public MapAdaptor(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.markers = new HashMap();
    }

    public String idForMarker(Marker m) {
        return markers.get(m);
    }

    public void clearMap() {
        googleMap.clear();
        LatLng centre = new LatLng(0,0);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centre, 2.0f));
    }

    public void populateMap(MainActivity ma, String user, DatastoreManager dsm) {
        AsyncTask<Void,Void,AsyncResult<List<Pair<String,MarkerOptions>>>> task = new PopulateMapTask(ma, user, dsm, this);
        task.execute();
    }

    public void addPoints(List<Pair<String,MarkerOptions>> mos) {
        googleMap.clear();
        markers.clear();

        // for finding the bounding box
        double maxLat = -180;
        double minLat = 180;
        double maxLon = -180;
        double minLon = 180;

        for (Pair<String, MarkerOptions> mo : mos) {
            double lat = mo.second.getPosition().latitude;
            double lon = mo.second.getPosition().longitude;
            if (lat > maxLat) {
                maxLat = lat;
            } else if (lat < minLat) {
                minLat = lat;
            }
            if (lon > maxLon) {
                maxLon = lon;
            } else if (lon < minLon) {
                minLon = lon;
            }
            Marker m = googleMap.addMarker(mo.second);
            markers.put(m, mo.first);
        }

        // zoom to bounds of markers
        LatLngBounds bounds = new LatLngBounds(new LatLng(minLat, minLon), new LatLng(maxLat, maxLon));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 1));

    }


}
