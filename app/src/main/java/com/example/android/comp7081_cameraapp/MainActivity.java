package com.example.android.comp7081_cameraapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity
        implements Camera.OnZoomChangeListener
{
    final String LOG_TAG = "Camera Direct Access" ;

    public final static String ACTIVITY_TITLE_EXTRA = "cameraapp.extra.ACTIVITY_TITLE";
    public final static String SELECTIONS_EXTRA = "cameraapp.extra.SELECTIONS";
    public final static String SELECTED_INDEX_EXTRA = "cameraapp.extra.SELECTED_INDEX";

    final String CAMERA_SIZE_DISPLAY_FORMAT = "%d x %d";
    final String SELECTED_CAMERA_ID_KEY = "_selectedCameraId";
    final int CAMERA_ID_NOT_SET = -1;
    final int NOT_SET = -1;

    final int PICTURE_SIZE_SELECTION_REQUEST_CODE = 2100;
    final int FILE_THUMBNAIL_REQUEST_CODE = 2200;

    int _frontFacingCameraId = CAMERA_ID_NOT_SET;
    int _backFacingCameraId = CAMERA_ID_NOT_SET;

    boolean _hasCamera = false;
    boolean _hasFrontCamera = false;

    int _selectedCameraId  = CAMERA_ID_NOT_SET;
    Camera _selectedCamera;

    Camera.Parameters _cameraParameters = null;
    List<Camera.Size> _supportedPictureSizes = null;
    Camera.Size _selectedPictureSize = null;

    int _currentZoom = NOT_SET;
    int _maxZoom = NOT_SET;
    boolean _isSmoothZoomSupported = false;
    boolean _isZoomSupported = false;

    String _lastMediaFilePath = null;
    Uri _lastMediaFileUri = null;
    String localityForFilename = "NA";
    String TAGForFilename = "";

    String test = "Some test string for VCS";

    String[] _fileNamesInPhotoDirectory = null;

    private EditText txtCityFilter = null;
    private EditText txtDateFilter = null;
    private EditText txtTAGFilter = null;
    private EditText txtTAG = null;

    private EditText txtFromDateFilter = null;
    private EditText txtToDateFilter = null;

    public static String strCityFilter = null;
    public static String strDateFilter = null;
    public static String strTAGFilter = null;

    public static String strFromDateFilter = null;
    public static String strToDateFilter = null;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        PackageManager pm = getPackageManager();
        _hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        _hasFrontCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

        if(!_hasCamera)
            showNoCameraDialog();

        if (savedInstanceState != null)
            _selectedCameraId = savedInstanceState.getInt(SELECTED_CAMERA_ID_KEY, CAMERA_ID_NOT_SET);

        setupCameraControlsEventHandlers();
        manageCameraControls(false);

        txtCityFilter = (EditText) findViewById(R.id.editTextCityFilter);
        txtDateFilter = (EditText) findViewById(R.id.editTextDateFilter);
        txtTAGFilter = (EditText) findViewById(R.id.editTagFilter);

        txtTAG = (EditText) findViewById(R.id.editTAG);

        txtFromDateFilter = (EditText) findViewById(R.id.editTextFromDateFilter);
        txtToDateFilter = (EditText) findViewById(R.id.editTextToDateFilter);
    }

    public void onSubmitBtnClick(View v) {

        strCityFilter = txtCityFilter.getText().toString().toUpperCase();
        strDateFilter = txtDateFilter.getText().toString().toUpperCase();
        strTAGFilter = txtTAGFilter.getText().toString().toUpperCase();

        strFromDateFilter = txtFromDateFilter.getText().toString().toUpperCase();
        strToDateFilter = txtToDateFilter.getText().toString().toUpperCase();
    }

    public void onSubmitTAGBtnClick(View v) {

        TAGForFilename = txtTAG.getText().toString().toUpperCase();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SELECTED_CAMERA_ID_KEY, _selectedCameraId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater() ;
        inflater.inflate(R.menu.main_menu, menu) ;

        if(!_hasCamera)
            disableCameraMenuItems(menu);
        else if(!_hasFrontCamera)
            disableFrontCameraMenuItems(menu);

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        releaseSelectedCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openSelectedCamera();
    }

    public void onMenuOpenBackCamera(MenuItem item) {
        logMenuChoice(item);

        _selectedCameraId = getBackFacingCameraId();
        openSelectedCamera();

    }

    public void onMenuOpenFrontCamera(MenuItem item) {
        logMenuChoice(item);

        _selectedCameraId = getFrontFacingCameraId();
        openSelectedCamera();
    }

    public void onMenuCloseCamera(MenuItem item) {
        logMenuChoice(item);

        releaseSelectedCamera();
        _selectedCameraId = CAMERA_ID_NOT_SET;

    }

    public void onMenuShowThumbnailForExistingPicture(MenuItem item) {

        File photoDirectory = CameraHelper.getPhotoDirectory();
        _fileNamesInPhotoDirectory = photoDirectory.list();

        // Show list in selection activity
        Intent intent = new Intent(this, SelectionActivity.class);
        intent.putExtra(ACTIVITY_TITLE_EXTRA, "Select picture for thumbnail");
        intent.putExtra(SELECTIONS_EXTRA, _fileNamesInPhotoDirectory);
        startActivityForResult(intent, FILE_THUMBNAIL_REQUEST_CODE);
    }

    public void showThumbnailAlertDialog(Bitmap bitmap, String filename) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(filename);
        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }) ;
        if (bitmap != null) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View layoutView = inflater.inflate(R.layout.thumbnail_dialog, null);
            ImageView thumbnailView = (ImageView) layoutView.findViewById(R.id.thumbnail);
            thumbnailView.setImageBitmap(bitmap);
            builder.setView(layoutView);
        }
        else {
            builder.setMessage("Bitmap contained a NULL value");
        }
        builder.show();
    }

    public void onExit(MenuItem item) {
        logMenuChoice(item);

        releaseSelectedCamera();

        finish();
    }

    private void logMenuChoice(MenuItem item) {
        CharSequence menuTitle = item.getTitle();
        Log.d(LOG_TAG, "Menu item selected:" + menuTitle);
    }

    void showNoCameraDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Camera");
        builder.setMessage(
                "Device does not have required camera support. " +
                        "Some features will not be available.");
        builder.setPositiveButton("Continue", null);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    void disableCameraMenuItems(Menu menu) {
        menu.findItem(R.id.menuOpenBackCamera).setEnabled(false);
        menu.findItem(R.id.menuOpenFrontCamera).setEnabled(false);
        menu.findItem(R.id.menuCloseCamera).setEnabled(false);
    }

    void disableFrontCameraMenuItems(Menu menu) {
        menu.findItem(R.id.menuOpenFrontCamera).setEnabled(false);
    }

    int getFacingCameraId(int facing) {
        int cameraId = CAMERA_ID_NOT_SET;

        int nCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for(int cameraInfoId=0; cameraInfoId < nCameras; cameraInfoId++) {
            Camera.getCameraInfo(cameraInfoId, cameraInfo);
            if(cameraInfo.facing == facing) {
                cameraId = cameraInfoId;
                break;
            }

        }
        return cameraId;
    }

    int getFrontFacingCameraId() {
        if(_frontFacingCameraId == CAMERA_ID_NOT_SET)
            _frontFacingCameraId = getFacingCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);

        return _frontFacingCameraId;
    }

    int getBackFacingCameraId() {
        if(_backFacingCameraId == CAMERA_ID_NOT_SET)
            _backFacingCameraId = getFacingCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);

        return _backFacingCameraId;
    }

    void openSelectedCamera() {
        String message = null;

        releaseSelectedCamera();
        if(_selectedCameraId != CAMERA_ID_NOT_SET) {
            try {
                _selectedCamera = Camera.open(_selectedCameraId);
                //message = String.format("Opened Camera ID: %d", _selectedCameraId);

                CameraPreview cameraPreview =
                        (CameraPreview) findViewById(R.id.cameraPreview);
                cameraPreview.connectCamera(_selectedCamera, _selectedCameraId);

                _cameraParameters = _selectedCamera.getParameters();

                _supportedPictureSizes = _cameraParameters.getSupportedPictureSizes();

                if(_selectedPictureSize == null)
                    _selectedPictureSize = _cameraParameters.getPictureSize();

                _isZoomSupported = _cameraParameters.isZoomSupported();
                if (_isZoomSupported) {
                    _currentZoom = _cameraParameters.getZoom();
                    _maxZoom = _cameraParameters.getMaxZoom();
                }

                _isSmoothZoomSupported = _cameraParameters.isSmoothZoomSupported();
                if (_isSmoothZoomSupported)
                    _selectedCamera.setZoomChangeListener(this);

            } catch (Exception ex) {
                message = "Unable to open camera: " + ex.getMessage();
                Log.e(LOG_TAG, message);

            }

            manageCameraControls(true);
        }

        if(message != null)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    void releaseSelectedCamera() {
        if(_selectedCamera != null) {
            CameraPreview cameraPreview =
                    (CameraPreview) findViewById(R.id.cameraPreview);
            cameraPreview.releaseCamera();

            manageCameraControls(false);

            _selectedCamera.release();
            _selectedCamera = null;
        }
    }

    private void manageCameraControls(boolean enable) {

        Button takePictureButton = (Button) findViewById(R.id.takePictureButton);
        takePictureButton.setEnabled(enable);
        Button selectPictureSizeButton = (Button) findViewById(R.id.selectPictureSizeButton);
        selectPictureSizeButton.setEnabled(enable);

        displaySelectedPictureSize(enable ? _selectedPictureSize : null);
        manageCameraZoomControls(enable);
    }

    void manageCameraZoomControls(boolean enable) {
        // Only enable zoom controls if the selected camera supports zooming
        boolean zoomEnable = enable && _isZoomSupported;
        ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoomControls);
        zoomControls.setIsZoomInEnabled(zoomEnable);
        zoomControls.setIsZoomOutEnabled(zoomEnable);

    }

    private void displaySelectedPictureSize(Camera.Size size) {
        String display = size == null ? "" :
                String.format(CAMERA_SIZE_DISPLAY_FORMAT, size.width,  size.height);

        TextView textView = (TextView) findViewById(R.id.selectedPictureSizeTextView);
        textView.setText(display);
    }

    private void setupCameraControlsEventHandlers() {

        // **** Take Picture Button ****
        Button takePictureButton = (Button) findViewById(R.id.takePictureButton);
        takePictureButton.setOnClickListener( new View.OnClickListener() {
            public void onClick(View view) {
                // Take Picture Button Click Handler
                takePicture();
            }
        });

        // **** Select Picture Size Button ****
        Button selectPictureSizeButton = (Button) findViewById(R.id.selectPictureSizeButton);
        selectPictureSizeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                selectPictureSizeButtonClicked();
            }
        });

        ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoomControls);
        // **** Zoom In Button ****
        zoomControls.setOnZoomInClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        // Zoom In Button Click Handler
                        zoomIn();
                    }
                }
        );
        // **** Zoom Out Button ****
        zoomControls.setOnZoomOutClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        // Zoom Out Button Click Handler
                        zoomOut();
                    }
                }
        );

    }

    void zoomIn() {
        if(_currentZoom < _maxZoom) {
            _currentZoom++;
            _cameraParameters.setZoom(_currentZoom);
            _selectedCamera.setParameters(_cameraParameters);
        }
    }

    void zoomOut() {
        if(_currentZoom > 0) {
            _currentZoom--;
            _cameraParameters.setZoom(_currentZoom);
            _selectedCamera.setParameters(_cameraParameters);
        }
    }

    public void onZoomChange(int zoomValue, boolean stopped, Camera camera) {
        if(stopped)
            manageCameraZoomControls(true);

        _currentZoom = zoomValue;
    }

    void takePicture() {
        if(_cameraParameters != null) {
            _cameraParameters.setPictureSize(_selectedPictureSize.width, _selectedPictureSize.height);

            int rotation = CameraHelper.getDisplayOrientationForCamera(this, _selectedCameraId);
            _cameraParameters.setRotation(rotation);

            configureCameraGpsParameters(_cameraParameters);

            _selectedCamera.setParameters(_cameraParameters);
        }

        _selectedCamera.takePicture(null, null, new Camera.PictureCallback() {
            public void onPictureTaken(byte[] bytes, Camera camera) {
                onPictureJpeg(bytes, camera);
            }
        });
    }

    void onPictureJpeg(byte[] bytes, Camera camera) {
        String userMessage = null;
        int i = bytes.length;
        Log.d(LOG_TAG, String.format("bytes = %d", i));

        File f = CameraHelper.generatePhotoFileWithTimeStampAndLocale(localityForFilename, TAGForFilename );

        try {
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(f));
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
            userMessage = "Picture saved as " + f.getName();

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error accessing photo output file:" + e.getMessage());
            userMessage = "Error saving photo";
        }

        if (userMessage != null)
            Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();

        doScanFile(f.toString());

        _selectedCamera.startPreview();

    }

    void doScanFile(String fileName) {
        String[] filesToScan = {fileName};

        MediaScannerConnection.scanFile(this, filesToScan, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String filePath, Uri uri) {
                        mediaFileScanComplete(filePath, uri);
                    }
                });
    }

    void mediaFileScanComplete(String mediaFilePath, Uri mediaFileUri) {
        Log.d(LOG_TAG, String.format("File=%s | Uri=%s", mediaFilePath, mediaFilePath));

        _lastMediaFilePath = mediaFilePath;
        _lastMediaFileUri = mediaFileUri;
    }

    private void configureCameraGpsParameters(Camera.Parameters cameraParameters) {

        // Find the best location
        LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        Location location = bestLocation;

        if (location != null) {
            double altitude = location.hasAltitude() ? location.getAltitude() : 0;
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            long gpsTime = location.getTime();

            Geocoder gcd = new Geocoder(this, Locale.getDefault());

            List<Address> addresses = null;
            try {

                addresses = gcd.getFromLocation(latitude, longitude, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addresses.size() >0)
            {
                localityForFilename = addresses.get(0).getLocality();
                localityForFilename = localityForFilename.toUpperCase();
                Toast.makeText(this, "Current Locality: " + localityForFilename, Toast.LENGTH_SHORT).show();
            }


            // Set GPS values into parameters
            cameraParameters.setGpsAltitude(altitude);
            cameraParameters.setGpsLatitude(latitude);
            cameraParameters.setGpsLongitude(longitude);
            cameraParameters.setGpsTimestamp(gpsTime);

        } else {
            // Clear GPS parameters
            cameraParameters.removeGpsData();

        }

    }

    void selectPictureSizeButtonClicked() {
        // Create as array of strings of the form 320x240
        String[] pictureSizesAsString = new String[_supportedPictureSizes.size()];
        int index = 0;
        for(Camera.Size pictureSize:_supportedPictureSizes)
            pictureSizesAsString[index++] =
                    String.format(CAMERA_SIZE_DISPLAY_FORMAT, pictureSize.width, pictureSize.height);

        // Show list in selection activity
        Intent intent = new Intent(this, SelectionActivity.class);
        intent.putExtra(ACTIVITY_TITLE_EXTRA, "Select picture size");
        intent.putExtra(SELECTIONS_EXTRA, pictureSizesAsString);
        startActivityForResult(intent, PICTURE_SIZE_SELECTION_REQUEST_CODE);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int index = -1;
        if (resultCode == RESULT_OK) {
            switch(requestCode) {
                case PICTURE_SIZE_SELECTION_REQUEST_CODE:
                    index = data.getIntExtra(SELECTED_INDEX_EXTRA, NOT_SET);
                    if(index != NOT_SET)
                        _selectedPictureSize = _supportedPictureSizes.get(index);
                    break;
                case FILE_THUMBNAIL_REQUEST_CODE:
                    index = data.getIntExtra(SELECTED_INDEX_EXTRA, NOT_SET);
                    if(index != NOT_SET) {
                        String selectedFileName = _fileNamesInPhotoDirectory[index];
                        showThumbnailForFileName(selectedFileName);
                    }
                    break;
                default:
                    break;
            }
        }

    }

    void showThumbnailForFileName(String fileName) {
        final String[] QUERY_COLUMNS = {MediaStore.Images.Media._ID};
        final String QUERY_ORDER_BY = MediaStore.Images.Media._ID;
        final String QUERY_WHERE = MediaStore.Images.Media.DATA + " like ? ";

        Bitmap thumbnail = null;

        File photoDirectory = CameraHelper.getPhotoDirectory();
        File filePath = new File(photoDirectory, fileName);

        String[] queryValues = {filePath.toString()};
        Cursor imageCursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, QUERY_COLUMNS,
                QUERY_WHERE, queryValues, null);

        if(imageCursor.moveToFirst()) {
            thumbnail= BitmapFactory.decodeFile(filePath.toString());
            showThumbnailAlertDialog(thumbnail, fileName);
        }
    }

}
