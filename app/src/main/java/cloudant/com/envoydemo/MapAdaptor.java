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
import com.cloudant.sync.datastore.DocumentException;
import com.cloudant.sync.datastore.DocumentRevision;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tomblench on 25/07/2016.
 */
public class MapAdaptor   {

    private GoogleMap googleMap;
    private Map<Marker, String> markers; //marker -> id

    public MapAdaptor(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.markers = new HashMap();
    }


    public String idForMarker(Marker m) {
        return markers.get(m);
    }

    public void populateMap(Datastore ds) throws DatastoreException, DocumentException {

        googleMap.clear();
        markers.clear();

        // for finding the bounding box
        double maxLat = -180;
        double minLat = 180;
        double maxLon = -180;
        double minLon = 180;

        // find all locations and populate with map with markers for each location
        for(String id : ds.getAllDocumentIds()) {
            DocumentRevision d = ds.getDocument(id);
            Map<String, Object> data = d.getBody().asMap();
            if (data.containsKey("customer_location")) {

                Map<String, Double> location = (Map<String, Double>)data.get("customer_location");
                double lat = location.get("lat");
                double lon = location.get("lon");
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
                Marker m = addPoint(lat, lon, (String)data.get("customer_name"), (String)data.get("comments"));
                markers.put(m, id);
            }
        }

        // zoom to bounds of markers
        LatLngBounds bounds = new LatLngBounds(new LatLng(minLat, minLon), new LatLng(maxLat, maxLon));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 1));
    }

    public Marker addPoint(double lat, double lon, String title, String comments) {
        return googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .snippet(comments)
                .title(title));
    }

}
