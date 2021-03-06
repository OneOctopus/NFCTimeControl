/*
 * Copyright (c) 2016. OneOctopus www.oneoctopus.es
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.naroh.nfctimecontrol.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import com.naroh.nfctimecontrol.R;
import com.naroh.nfctimecontrol.data.PlacesDAO;
import com.naroh.nfctimecontrol.helpers.SPHelper;
import com.naroh.nfctimecontrol.other.Constants;


public class MainActivityFragment extends Fragment implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {
    private SupportMapFragment mapView;
    private Location loc;
    private CardView instructions;
    private TextView action;
    private CardView card;
    private TextView placeName;
    private TextView placeTime;

    public MainActivityFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment UnfollowsFragment.
     */
    public static MainActivityFragment newInstance() {
        return new MainActivityFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_view);
        getLocationPermissions();

        instructions = (CardView) view.findViewById(R.id.instructions);
        action = (TextView) view.findViewById(R.id.action);
        card = (CardView) view.findViewById(R.id.cardview);
        placeName = (TextView) view.findViewById(R.id.place_name);
        placeTime = (TextView) view.findViewById(R.id.place_time);
    }

    /**
     * Check if the application has the permission to access the user location since
     * Marshmallow requires each permission to be explicitly approved.
     */
    private void getLocationPermissions() {

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {

            requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.PERMISSION_LOCATION);
        } else
            getLastKnownLocation();
    }

    /**
     * Returns the user response about the permissions.
     *
     * @param requestCode the code that identifies the permission request
     * @param permissions the permissions that are referred in the request
     * @param grantResults results of the request to the user
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                getLastKnownLocation();
            else
                disableLocation();
        }


    }

    private void disableLocation() {

    }

    private void getLastKnownLocation() {

        try {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            List<String> providers = locationManager.getProviders(true);
            Location bestLocation = null;
            for (String provider : providers) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location
                    bestLocation = l;
                }
            }

            loc = bestLocation;

            mapView.getMapAsync(this);
        } catch (SecurityException sec) {
            loc = null;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setBuildingsEnabled(true);
        googleMap.setIndoorEnabled(true);
        googleMap.getUiSettings().setAllGesturesEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        if(loc != null) {
            //noinspection MissingPermission - already checked
            googleMap.setMyLocationEnabled(true);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(loc.getLatitude(), loc.getLongitude())));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        }

    }

    public void showUserFeedbackNoTags() {
        PlacesDAO db = new PlacesDAO(getActivity());
        if(SPHelper.getBoolean(getActivity(), "first_time", true) && db.isEmpty()) {
            card.setVisibility(View.GONE);
            instructions.setVisibility(View.VISIBLE);
            action.setText(R.string.create_place_start);
        }else{

            List<String> openChecks = db.getOpenChecks();

            if(openChecks != null){
                StringBuilder placeNames = new StringBuilder();

                for(String place : openChecks) {
                    placeNames.append(place);
                    placeNames.append(" ");
                }

                instructions.setVisibility(View.GONE);
                card.setVisibility(View.VISIBLE);
                placeName.setText(placeNames.toString());
                try {
                    placeTime.setText(String.format(getString(R.string.for_hours_minutes), db.getTimeInOpenCheck(openChecks.get(0))/60, db.getTimeInOpenCheck(openChecks.get(0))%60));
                }catch (SQLException e){
                    e.printStackTrace();
                }
            } else{
                card.setVisibility(View.GONE);
                action.setText(getString(R.string.scan_a_tag_nto_check_in));
                instructions.setVisibility(View.VISIBLE);
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showUserFeedbackNoTags();
    }

}
