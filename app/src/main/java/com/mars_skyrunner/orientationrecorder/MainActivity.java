package com.mars_skyrunner.orientationrecorder;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private TextView orientationTextView;
    Snackbar snackbar;
    private String LOG_TAG = MainActivity.class.getSimpleName();
    int orientationValue;
    File storageDir;
    String csvString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientationTextView = (TextView) findViewById(R.id.textview_first);
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
                    snackbar.show();

                }
            }
        });
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


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something here if sensor accuracy changes.
            // You must implement this callback in your code.
        }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(MainActivity.this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(MainActivity.this, magneticField,
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
        orientationTextView.setText(orientationValue+ ":"+getOrientationLabel(orientationValue));

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

    private String getOrientationLabel(int orientationValue) {

        switch (orientationValue){
              case 0:
                return "NORTH";
              case 1:
                return "NORTHEAST";
              case 2:
                return "EAST";
              case 3:
                return "SOUTHEAST";
              case 4:
                return "WEST";
              case 5:
                return "SOUTHWEST";
              case 6:
                return "NORTHWEST";
              case 7:
                return "SOUTH";
        }

        return null;
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

}
