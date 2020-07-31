package de.tuberlin.mcc.simra.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.IconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.annotation.IncidentPopUpActivity;
import de.tuberlin.mcc.simra.app.annotation.MarkerFunct;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.Utils;

import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;

public class ShowRouteActivity extends BaseActivity {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ShowRouteActivity_LOG";
    private static final String EXTRA_RIDE_ID = "EXTRA_RIDE_ID";
    private static final String EXTRA_STATE = "EXTRA_STATE";
    ////~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Our ride
    public ExecutorService pool = Executors.newFixedThreadPool(6);
    public Drawable markerDefault;
    public Drawable markerNotYetAnnotated;
    public Drawable markerAutoGenerated;
    public Drawable editDoneCust;
    public int state;
    int start = 0;
    int end = 0;
    ImageButton backBtn;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // GEOCODER --> obtain GeoPoint from address
    TextView toolbarTxt;
    RelativeLayout saveButton;

    // Marker-icons for different types/states of events:
    // Automatically detected/custom; to be annotated/already annotated
    RelativeLayout exitButton;
    IconOverlay startFlagOverlay;
    IconOverlay finishFlagOverlay;
    MapEventsOverlay overlayEvents;
    boolean addCustomMarkerMode;
    MarkerFunct myMarkerFunct;

    int bike;
    int child;
    int trailer;
    int pLoc;
    int rideId;
    File gpsFile;
    Polyline route;
    Polyline editableRoute;
    RangeSlider privacySlider;
    int routeSize = 3;
    BoundingBox bBox;
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Map stuff, Overlays
    private MapView mMapView;
    private RelativeLayout addIncidentButtonMode;
    private RelativeLayout exitAddIncidentModeButton;
    private View loadingAnimationLayout;

    // New Variables
    private IncidentLog incidentLog;
    private DataLog dataLog;
    private DataLog originalDataLog;

    /**
     * Returns the longitudes of the southern- and northernmost points
     * as well as the latitudes of the western- and easternmost points
     *
     * @param pl
     * @return double Array {South, North, West, East}
     */
    private static BoundingBox getBoundingBox(Polyline pl) {

        // {North, East, South, West}
        List<GeoPoint> geoPoints = pl.getPoints();

        double[] border = {geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude(), geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude()};

        for (int i = 0; i < geoPoints.size(); i++) {
            // Check for south/north
            if (geoPoints.get(i).getLatitude() < border[2]) {
                border[2] = geoPoints.get(i).getLatitude();
            }
            if (geoPoints.get(i).getLatitude() > border[0]) {
                border[0] = geoPoints.get(i).getLatitude();
            }
            // Check for west/east
            if (geoPoints.get(i).getLongitude() < border[3]) {
                border[3] = geoPoints.get(i).getLongitude();
            }
            if (geoPoints.get(i).getLongitude() > border[1]) {
                border[1] = geoPoints.get(i).getLongitude();
            }
        }
        return new BoundingBox(border[0] + 0.001, border[1] + 0.001, border[2] - 0.001, border[3] - 0.001);
    }

    public static void startShowRouteActivity(int rideId, Integer state, Context context) {
        Intent intent = new Intent(context, ShowRouteActivity.class);
        intent.putExtra(EXTRA_RIDE_ID, rideId);
        intent.putExtra(EXTRA_STATE, state);
        context.startActivity(intent);

    }

    public MapView getmMapView() {
        return mMapView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started");
        setContentView(R.layout.activity_show_route);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Toolbar
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_showRoute);
        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> finish());

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        mMapView = findViewById(R.id.showRouteMap);
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        MapController mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(ZOOM_LEVEL);
        TextView copyrightTxt = findViewById(R.id.copyright_text);
        copyrightTxt.setMovementMethod(LinkMovementMethod.getInstance());

        if (!getIntent().hasExtra(EXTRA_RIDE_ID)) {
            throw new RuntimeException("Extra: " + EXTRA_RIDE_ID + " not defined.");
        }
        rideId = getIntent().getIntExtra(EXTRA_RIDE_ID, 0);
        state = getIntent().getIntExtra(EXTRA_STATE, MetaData.STATE.JUST_RECORDED);
        new LoadOriginalDataLogTask().execute();

        addIncidentButtonMode = findViewById(R.id.addIncidentModeButton);
        exitAddIncidentModeButton = findViewById(R.id.exitAddIncidentModeButton);
        exitAddIncidentModeButton.setVisibility(View.GONE);
        loadingAnimationLayout = findViewById(R.id.loadingAnimationLayout);

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);

        gpsFile = IOUtils.Files.getGPSLogFile(rideId, false, this);

        bike = SharedPref.Settings.Ride.BikeType.getBikeType(this);
        child = SharedPref.Settings.Ride.ChildOnBoard.getValue(this);
        trailer = SharedPref.Settings.Ride.BikeWithTrailer.getValue(this);
        pLoc = SharedPref.Settings.Ride.PhoneLocation.getPhoneLocation(this);

        privacySlider = findViewById(R.id.routePrivacySlider);
        privacySlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {
                // Remove Icons for better visibility of the track
                mMapView.getOverlays().remove(startFlagOverlay);
                mMapView.getOverlays().remove(finishFlagOverlay);
            }

            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                start = Math.round(slider.getValues().get(0));
                end = Math.round(slider.getValues().get(1));
                if (end > originalDataLog.rideAnalysisData.route.getPoints().size()) {
                    end = routeSize;
                }
                new RideUpdateTask(true, true).execute();
            }
        });
        privacySlider.addOnChangeListener((slider, changeListener, touchChangeListener) -> {
            if (editableRoute != null) {
                editableRoute.setPoints(originalDataLog.rideAnalysisData.route.getPoints().subList(Math.round(slider.getValues().get(0)), Math.round(slider.getValues().get(1))));
            }
            mMapView.invalidate();
        });

        TextView privacySliderDescription = findViewById(R.id.privacySliderDescription);
        LinearLayout privacySliderLinearLayout = findViewById(R.id.privacySliderLinearLayout);
        saveButton = findViewById(R.id.saveIncident);
        exitButton = findViewById(R.id.exitShowRoute);

        if (state < MetaData.STATE.SYNCED) {
            addIncidentButtonMode.setVisibility(View.VISIBLE);
            exitButton.setVisibility(View.INVISIBLE);
            if (!IOUtils.isDirectoryEmpty(IOUtils.Directories.getPictureCacheDirectoryPath())) {
                EvaluateClosePassActivity.startEvaluateClosePassActivity(rideId, this);
            }
        } else {
            addIncidentButtonMode.setVisibility(View.GONE);
            privacySliderLinearLayout.setVisibility(View.INVISIBLE);
            privacySliderDescription.setVisibility(View.INVISIBLE);
            saveButton.setVisibility(View.INVISIBLE);
            exitButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    exitButton.setElevation(0.0f);
                    exitButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    exitButton.setElevation(2 * ShowRouteActivity.this.getResources().getDisplayMetrics().density);
                    exitButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            });
            exitButton.setOnClickListener(v -> finish());
        }


        // Functionality for 'edit mode', i.e. the mode in which users can put their own incidents
        // onto the map
        addIncidentButtonMode.setOnClickListener((View v) -> {
            addIncidentButtonMode.setVisibility(View.INVISIBLE);
            exitAddIncidentModeButton.setVisibility(View.VISIBLE);
            addCustomMarkerMode = true;
        });

        exitAddIncidentModeButton.setOnClickListener((View v) -> {
            addIncidentButtonMode.setVisibility(View.VISIBLE);
            exitAddIncidentModeButton.setVisibility(View.INVISIBLE);
            addCustomMarkerMode = false;
        });

        if (state < MetaData.STATE.SYNCED) {
            fireRideSettingsDialog();
        } else {
            new RideUpdateTask(false, false).execute();
        }
        Log.d(TAG, "onCreate() finished");

    }

    private void refreshRoute(int rideId, boolean updateBoundaries, boolean calculateEvents) throws IOException {

        if (updateBoundaries && dataLog != null) {
            long startTime = this.originalDataLog.onlyGPSDataLogEntries.get(start).timestamp;
            long endTime = this.originalDataLog.onlyGPSDataLogEntries.get(end).timestamp;
            this.dataLog = DataLog.loadDataLog(rideId, startTime, endTime, this);
            this.incidentLog = IncidentLog.filterIncidentLog(IncidentLog.mergeIncidentLogs(this.incidentLog, IncidentLog.loadIncidentLog(rideId, startTime, endTime, this)), startTime, endTime);
        } else {
            this.dataLog = DataLog.loadDataLog(rideId, this);
            this.incidentLog = IncidentLog.loadIncidentLog(rideId, this);
        }
        if (!this.incidentLog.hasAutoGeneratedIncidents()) {
            List<IncidentLogEntry> autoGeneratedIncidents = Utils.findAccEvents(rideId, this);
            for (IncidentLogEntry autoGeneratedIncident : autoGeneratedIncidents) {
                this.incidentLog.updateOrAddIncident(autoGeneratedIncident);
            }
        }

        if (route != null) {
            mMapView.getOverlayManager().remove(route);
        }
        route = this.dataLog.rideAnalysisData.route;
        route.setWidth(8f);
        route.getPaint().setStrokeCap(Paint.Cap.ROUND);
        mMapView.getOverlayManager().add(route);


        // Get a bounding box of the route so the view can be moved to it and the zoom can be
        // set accordingly
        runOnUiThread(() -> {
            bBox = getBoundingBox(route);
            zoomToBBox(bBox);
            mMapView.invalidate();
        });

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // (3): CenterMap
        ImageButton centerMap = findViewById(R.id.bounding_box_center_button);

        centerMap.setOnClickListener(v -> {
            Log.d(TAG, "boundingBoxCenterMap clicked ");
            zoomToBBox(bBox);
        });

        // Set the icons for event markers
        // (1) Automatically recognized, not yet annotated
        markerDefault = getResources().getDrawable(R.drawable.edit_event_blue, null);

        // (2) Custom, not yet annotated
        markerNotYetAnnotated = getResources().getDrawable(R.drawable.edit_event_green, null);

        // (3) Automatically recognized, annotated
        markerAutoGenerated = getResources().getDrawable(R.drawable.edited_event_blue, null);

        // (4) Custom, not yet annotated
        editDoneCust = getResources().getDrawable(R.drawable.edited_event_green, null);


        // Create an instance of MarkerFunct-class which provides all functionality related to
        // incident markers
        Log.d(TAG, "creating MarkerFunct object");
        runOnUiThread(() -> {
            if (myMarkerFunct != null) {
                myMarkerFunct.deleteAllMarkers();
            }
            myMarkerFunct = new MarkerFunct(ShowRouteActivity.this, dataLog, incidentLog);
            myMarkerFunct.updateMarkers(this.incidentLog, ShowRouteActivity.this);
        });

        addCustomMarkerMode = false;

        Log.d(TAG, "setting up mReceive");

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                if (addCustomMarkerMode) {
                    // Call custom marker adding functionality which enables the user to put
                    // markers on the map

                    myMarkerFunct.addCustomMarker(p);
                    exitAddIncidentModeButton.performClick();
                    return true;
                } else {
                    InfoWindow.closeAllInfoWindowsOn(mMapView);
                    return false;
                }

            }

            @Override
            public boolean longPressHelper(GeoPoint p) {

                if (addCustomMarkerMode) {
                    // Call custom marker adding functionality which enables the user to put
                    // markers on the map
                    myMarkerFunct.addCustomMarker(p);
                    exitAddIncidentModeButton.performClick();
                    return true;

                }
                return false;
            }
        };

        routeSize = route.getPoints().size();
        if (routeSize < 2) {
            routeSize = 2;
        }
        runOnUiThread(() -> {
            Drawable startFlag = ShowRouteActivity.this.getResources().getDrawable(R.drawable.startblack, null);
            Drawable finishFlag = ShowRouteActivity.this.getResources().getDrawable(R.drawable.racingflagblack, null);
            GeoPoint startFlagPoint = dataLog.rideAnalysisData.route.getPoints().get(0);
            GeoPoint finishFlagPoint = dataLog.rideAnalysisData.route.getPoints().get(dataLog.rideAnalysisData.route.getPoints().size() - 1);

            startFlagOverlay = new IconOverlay(startFlagPoint, startFlag);
            finishFlagOverlay = new IconOverlay(finishFlagPoint, finishFlag);
            mMapView.getOverlays().add(startFlagOverlay);
            mMapView.getOverlays().add(finishFlagOverlay);
        });


        saveButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                saveButton.setElevation(0.0f);
                saveButton.setBackground(getDrawable(R.drawable.button_pressed));
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                saveButton.setElevation(2 * ShowRouteActivity.this.getResources().getDisplayMetrics().density);
                saveButton.setBackground(getDrawable(R.drawable.button_unpressed));
            }
            return false;
        });

        saveButton.setOnClickListener((View v) -> saveChanges());

        overlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
        runOnUiThread(() -> {
            mMapView.getOverlays().add(overlayEvents);
            mMapView.invalidate();
        });

    }

    private void saveChanges() {
        // Save incidents
        IncidentLog.saveIncidentLog(incidentLog, this);
        // Save new Route
        DataLog.saveDataLog(dataLog, this);
        // Update MetaData
        MetaDataEntry metaDataEntry = MetaData.getMetaDataEntryForRide(rideId, this);
        metaDataEntry.startTime = dataLog.startTime;
        metaDataEntry.endTime = dataLog.endTime;
        metaDataEntry.distance = dataLog.rideAnalysisData.distance;
        metaDataEntry.waitedTime = dataLog.rideAnalysisData.waitedTime;
        metaDataEntry.numberOfIncidents = incidentLog.getIncidents().size();
        metaDataEntry.numberOfScaryIncidents = IncidentLog.getScaryIncidents(incidentLog).size();
        metaDataEntry.region = lookUpIntSharedPrefs("Region", 0, "Profile", this);
        metaDataEntry.state = MetaData.STATE.ANNOTATED;
        MetaData.updateOrAddMetaDataEntryForRide(metaDataEntry, this);

        Toast.makeText(this, getString(R.string.savedRide), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == IncidentPopUpActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                IncidentLogEntry incidentLogEntry = (IncidentLogEntry) intent.getSerializableExtra(IncidentPopUpActivity.EXTRA_INCIDENT);
                incidentLog.updateOrAddIncident(incidentLogEntry);
                myMarkerFunct.setMarker(incidentLogEntry);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //*****************************************************************
        // Shutdown pool and await termination to make sure the program
        // doesn't continue without the relevant work being completed
        try {
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException | NullPointerException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Zoom automatically to the bounding box.
     *
     * @param bBox
     */
    public void zoomToBBox(BoundingBox bBox) {
        // Usually the command in the if body should suffice
        // but osmdroid is buggy and we need the else part to fix it.
        if ((mMapView.getIntrinsicScreenRect(null).bottom - mMapView.getIntrinsicScreenRect(null).top) > 0) {
            mMapView.zoomToBoundingBox(bBox, false);
        } else {
            ViewTreeObserver vto = mMapView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    mMapView.zoomToBoundingBox(bBox, false);
                    ViewTreeObserver vto2 = mMapView.getViewTreeObserver();
                    vto2.removeOnGlobalLayoutListener(this);
                }
            });
        }

        mMapView.setMinZoomLevel(7.0);
        if (mMapView.getMaxZoomLevel() > 19.0) {
            mMapView.setMaxZoomLevel(19.0);
        }
    }

    public void fireRideSettingsDialog() {
        Log.d(TAG, "fireRideSettingsDialog()");

        // Create a alert dialog builder.
        final AlertDialog.Builder builder = new AlertDialog.Builder(ShowRouteActivity.this);

        // Get custom login form view.
        View settingsView = getLayoutInflater().inflate(R.layout.activity_ride_settings, null);

        // Set above view in alert dialog.
        builder.setView(settingsView);

        // Bike Type and Phone Location Spinners

        Spinner bikeTypeSpinner = settingsView.findViewById(R.id.bikeTypeSpinnerRideSettings);
        Spinner phoneLocationSpinner = settingsView.findViewById(R.id.locationTypeSpinnerRideSettings);

        CheckBox childCheckBoxButton = settingsView.findViewById(R.id.childCheckBoxRideSettings);
        CheckBox trailerCheckBoxButton = settingsView.findViewById(R.id.trailerCheckBoxRideSettings);

        CheckBox rememberMyChoiceCheckBox = settingsView.findViewById(R.id.rememberMyChoice);

        bikeTypeSpinner.setSelection(bike);
        phoneLocationSpinner.setSelection(pLoc);

        // Load previous child and trailer settings

        if (child == 1) {
            childCheckBoxButton.setChecked(true);
        }

        if (trailer == 1) {
            trailerCheckBoxButton.setChecked(true);
        }
        AlertDialog alertDialog = builder.create();
        // doneButton click listener.
        MaterialButton doneButton = settingsView.findViewById(R.id.done_button);

        doneButton.setOnClickListener(view -> {
            try {
                bike = bikeTypeSpinner.getSelectedItemPosition();
                pLoc = phoneLocationSpinner.getSelectedItemPosition();
                if (childCheckBoxButton.isChecked()) {
                    child = 1;
                } else {
                    child = 0;
                }
                if (trailerCheckBoxButton.isChecked()) {
                    trailer = 1;
                } else {
                    trailer = 0;
                }
                if (rememberMyChoiceCheckBox.isChecked()) {
                    SharedPref.Settings.Ride.PhoneLocation.setPhoneLocation(pLoc, ShowRouteActivity.this);
                    SharedPref.Settings.Ride.ChildOnBoard.setChildOnBoardByValue(child, ShowRouteActivity.this);
                    SharedPref.Settings.Ride.BikeWithTrailer.setTrailerByValue(trailer, ShowRouteActivity.this);
                    SharedPref.Settings.Ride.BikeType.setBikeType(bike, ShowRouteActivity.this);
                }
                // Close Alert Dialog.
                alertDialog.cancel();
                if (state == 0) {
                    new RideUpdateTask(false, true).execute();
                } else {
                    new RideUpdateTask(false, false).execute();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        builder.setCancelable(false);
        alertDialog.show();
    }

    private class RideUpdateTask extends AsyncTask {

        private boolean updateBoundaries;
        private boolean calculateEvents;

        private RideUpdateTask(boolean updateBoundaries, boolean calculateEvents) {
            this.updateBoundaries = updateBoundaries;
            this.calculateEvents = calculateEvents;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d(TAG, "doInBackground()");
            try {
                refreshRoute(rideId, updateBoundaries, calculateEvents);
            } catch (IOException e) {
                e.printStackTrace();
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingAnimationLayout.setVisibility(View.VISIBLE);

        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            loadingAnimationLayout.setVisibility(View.GONE);
            if (updateBoundaries) {
                InfoWindow.closeAllInfoWindowsOn(mMapView);
            }
        }
    }

    private class LoadOriginalDataLogTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            originalDataLog = DataLog.loadDataLog(rideId, ShowRouteActivity.this);
            Polyline originalRoute = originalDataLog.rideAnalysisData.route;
            incidentLog = IncidentLog.loadIncidentLog(rideId, ShowRouteActivity.this);
            if (editableRoute != null) {
                mMapView.getOverlayManager().remove(editableRoute);
            }
            editableRoute = new Polyline();
            editableRoute.setPoints(originalRoute.getPoints());
            editableRoute.setWidth(40.0f);
            editableRoute.getPaint().setColor(getColor(R.color.colorPrimaryDark));
            editableRoute.getPaint().setStrokeCap(Paint.Cap.ROUND);
            mMapView.getOverlayManager().add(editableRoute);
            runOnUiThread(() -> {
                privacySlider.setValues(0F, (float) originalRoute.getPoints().size());
                privacySlider.setValueTo(originalRoute.getPoints().size());
                privacySlider.setValueFrom(0F);
            });
            return null;
        }
    }
}