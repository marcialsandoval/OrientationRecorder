package com.mars_skyrunner.orientationrecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class OrientationActivity extends AppCompatActivity implements SensorEventListener {

    private static final int WRITE_PERMISSION_RQST = 100;
    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private TextView orientationTextView;
    private TextView outcomeTextView;
    Snackbar snackbar;
    private String LOG_TAG = OrientationActivity.class.getSimpleName();
    int orientationValue;
    File storageDir;
    String csvString = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientationTextView = (TextView) findViewById(R.id.textview_first);
        outcomeTextView = (TextView) findViewById(R.id.output);
        orientationTextView.setText("No orientation");
        storageDir = getOutputDirectory("OrientationRecorder");

        FloatingActionButton fab = findViewById(R.id.fab);
        snackbar = Snackbar.make(fab, "Recording orientation...", Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(snackbar.isShown()){

                    snackbar.dismiss();
                    createCSVFile();
                    csvString = "";

                }else{

                    //Checks for granted write external Permission
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(OrientationActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_RQST);

                    }else{

                        snackbar.show();

                    }
                }
            }
        });

    }


    /**
     * if there is no SD card, create new directory objects to make directory on device
     */
    private File getOutputDirectory(String folderName) {

        File directory = null;

        if (Environment.getExternalStorageState() == null) {
            //create new file directory object
            directory = new File(Environment.getDataDirectory()
                    + "/"+folderName+"/");

            // if no directory exists, create new directory
            if (!directory.exists()) {
                directory.mkdir();
            }

            // if phone DOES have sd card
        } else if (Environment.getExternalStorageState() != null) {

            // search for directory on SD card
            directory = new File(Environment.getExternalStorageDirectory()
                    + "/"+folderName+"/");

            // if no directory exists, create new directory
            if (!directory.exists()) {
                directory.mkdir();
            }
        }// end of SD card checking

        return directory;

    }


    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(OrientationActivity.this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(OrientationActivity.this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
        orientationTextView.setText("No orientation");
    }

    private void createCSVFile() {

        String csvPath = storageDir.getPath() + File.separator + "orientations.csv";
        Log.i(LOG_TAG,"csvPath:" + csvPath);
        try {
            FileWriter fw = new FileWriter(csvPath,true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(csvString);
            bw.flush();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "FileWriter IOException: " + e.toString());
        }

        Log.w(LOG_TAG, "createCSVFile  : Datapoint Exported Successfully.");
    }


    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);

            updateOrientationAngles();

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);

            updateOrientationAngles();
        }

    }


    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "mRotationMatrix" now has up-to-date information.
        SensorManager.getOrientation(rotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
        orientationValue = gerOrientationValue();
        outcomeTextView.setText(getOrientationLabel(orientationValue));
        orientationTextView.setText("id: " + orientationValue);

        if(snackbar.isShown()){
            String line =   accelerometerReading[0] + "," + accelerometerReading[1] + "," + accelerometerReading[2] + "," +
                    magnetometerReading[0] + "," + magnetometerReading[1] + "," + magnetometerReading[2] + "," +
                    mOrientationAngles[0] + "," + mOrientationAngles[1] + "," + mOrientationAngles[2] + "," +
                    rotationMatrix[0] + "," + rotationMatrix[1] + "," + rotationMatrix[2] + "," +
                    rotationMatrix[3] + "," + rotationMatrix[4] + "," + rotationMatrix[5] + "," +
                    rotationMatrix[6] + "," + rotationMatrix[7] + "," + rotationMatrix[8] ;

            String sample = line + "," + orientationValue + "\n";

            csvString += sample;
        }
    }

    private int gerOrientationValue() {
                /*
        Azimuth, angle of rotation about the -z axis.
        This value represents the angle between the device's y axis and the magnetic north pole.
        When facing north, this angle is 0, when facing south, this angle is π.
        Likewise, when facing east, this angle is π/2, and when facing west, this angle is -π/2.
        The range of values is -π to π.
        Device axis
        https://developer.android.com/images/axis_device.png
        Globe axis
        https://developer.android.com/images/axis_globe.png
        * */

        float azimuth = (float) mOrientationAngles[0]; // orientation

        int orientation = 1000;
        float tolerance = (float) Math.PI / 8;

        float NORTH = 0f;
        float MIN_NORTH = (NORTH - tolerance);
        float MAX_NORTH = (NORTH + tolerance);

        float SOUTH = (float) Math.PI;
        float MIN_SOUTH = (SOUTH - tolerance);

        float EAST = (float) Math.PI / 2;
        float MIN_EAST = (EAST - tolerance);
        float MAX_EAST = (EAST + tolerance);

        float WEST = -EAST;
        float MIN_WEST = (WEST - tolerance);
        float MAX_WEST = (WEST + tolerance);

        if (azimuth >= MIN_NORTH && azimuth <= MAX_NORTH) {//NORTH
            orientation = 0;

        } else {

            if (azimuth > MAX_NORTH && azimuth < MIN_EAST) {//NORTHEAST
                orientation = 1;

            } else {

                if (azimuth >= MIN_EAST && azimuth <= MAX_EAST) {//EAST
                    orientation = 2;

                } else {

                    if (azimuth > MAX_EAST && azimuth < MIN_SOUTH) {//SOUTHEAST
                        orientation = 3;

                    } else {

                        if (azimuth >= MIN_WEST && azimuth <= MAX_WEST) {//WEST
                            orientation = 4;

                        } else {

                            if (azimuth < MIN_WEST && azimuth > -MIN_SOUTH) {//SOUTHWEST
                                orientation = 5;

                            } else {

                                if (azimuth > MAX_WEST && azimuth < MIN_NORTH) {//NORTHWEST
                                    orientation = 6;

                                } else {
                                    if (azimuth > MIN_SOUTH || azimuth < -MIN_SOUTH) {//SOUTH
                                        orientation = 7;

                                    }

                                }


                            }

                        }
                    }

                }

            }


        }

        return orientation;
    }


    private String getOrientationLabel(int orientationValue) {

        switch (orientationValue){
            case 0:
                return "N";//NORTH
            case 1:
                return "NE";//NORTHEAST
            case 2:
                return "E";//EAST
            case 3:
                return "SE";//SOUTHEAST
            case 4:
                return "W";//WEST
            case 5:
                return "SW";//SOUTHWEST
            case 6:
                return "NW";//NORTHWEST
            case 7:
                return "S";//SOUTH
        }

        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults != null) {

            switch (requestCode) {

                case WRITE_PERMISSION_RQST:

                    if(grantResults[0] == PackageManager.PERMISSION_GRANTED){

                        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                            Log.i(LOG_TAG, "All permissions granted" );
                            snackbar.show();

                        }else{
                            Toast.makeText(getApplicationContext(),"Write external storage permission necessary.",Toast.LENGTH_SHORT).show();
                        }

                    }

                    break;


            }


        }

    }

}
