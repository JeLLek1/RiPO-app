package com.bronikowski.ripo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bronikowski.ripo.utils.CascadeClassifierWrapper
import com.bronikowski.ripo.utils.SavePhotoUtils
import com.bronikowski.ripo.utils.Yolov3Wrapper
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


class CameraActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val TAG = MainActivity::class.java.simpleName
    private val PERMISSIONS_READ_CAMERA = 1
    private val PERMISSION_WRITE_EXTERNAL_STORAGE = 2
    private val UPDATE_TIME_PERIOD = 60000
    private lateinit var cameraView: CameraBridgeViewBase
    private var soundEffects: Boolean = true
    private var detectionMethod : String = "LBP"
    private var permissionSavePhotos = false
    private var policeCascade: CascadeClassifierWrapper? = null
    private var firetruckCascade: CascadeClassifierWrapper? = null
    private var ambulanceCascade: CascadeClassifierWrapper? = null
    private var yolov3 : Yolov3Wrapper? = null
    private var lastTimeSave : Long = 0;

    var baseLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            super.onManagerConnected(status);

            Log.d(TAG, "callbacksuccess")
            when (status) {
                SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    when(detectionMethod){
                        "LBP" -> {
                            policeCascade = CascadeClassifierWrapper(R.raw.police_cascade, "policja", Scalar(0.0, 0.0, 255.0, 255.0), getApplicationContext())
                            firetruckCascade = CascadeClassifierWrapper(R.raw.fire_cascade, "straz", Scalar(255.0, 0.0, 0.0, 255.0), getApplicationContext())
                            ambulanceCascade = CascadeClassifierWrapper(R.raw.ambulance_cascade, "ambulans", Scalar(0.0, 255.0, 0.0, 255.0), getApplicationContext())
                        }
                        "HAAR" -> {
                            policeCascade = CascadeClassifierWrapper(R.raw.police_cascade_haar, "policja", Scalar(0.0, 0.0, 255.0, 255.0), getApplicationContext())
                            firetruckCascade = CascadeClassifierWrapper(R.raw.fire_cascade_haar, "straz", Scalar(255.0, 0.0, 0.0, 255.0), getApplicationContext())
                            ambulanceCascade = CascadeClassifierWrapper(R.raw.ambulance_cascade_haar, "ambulans", Scalar(0.0, 255.0, 0.0, 255.0), getApplicationContext())
                        }
                        "Yolov3" -> {
                            yolov3 = Yolov3Wrapper(R.raw.vehicles_yolo3_cfg,
                                    R.raw.vehicles_yolo3_weights,
                                    "vehicles", arrayListOf("ambulans", "policja", "straz"),
                                    arrayListOf(Scalar(0.0, 255.0, 0.0, 255.0),Scalar(0.0, 0.0, 255.0, 255.0), Scalar(255.0, 0.0, 0.0, 255.0)),
                                    getApplicationContext())
                        }
                    }

                    cameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraView = findViewById(R.id.camera_view)
        askForPermission()
        cameraView.setVisibility(SurfaceView.VISIBLE)
        cameraView.setCvCameraViewListener(this)
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK)

        soundEffects = intent.getBooleanExtra("soundEffects", true)
        detectionMethod = intent.getStringExtra("detectionMethod")
        Log.d(TAG, detectionMethod)
    }

    override fun onResume() {
        super.onResume()

        if (OpenCVLoader.initDebug()) {
            baseLoaderCallback.onManagerConnected((BaseLoaderCallback.SUCCESS));
            Log.d(TAG, "Wczytano OpenCV")
        }else{
            throw RuntimeException("Nie udało się wczytać OpenCV")
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat? {
        if(inputFrame == null) return null
        var mat: Mat = inputFrame.rgba()
        when (getResources().getConfiguration().orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                var tempMat = mat.t()
                mat.release()
                Core.flip(tempMat, mat, 1)
                tempMat.release()
            }
        }

        var isNew = false;
        when(detectionMethod){
            "LBP" -> {
                var mGray: Mat = Mat();
                Imgproc.cvtColor(mat, mGray, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.equalizeHist(mGray, mGray);
                val (mat1, isNewPolice) = policeCascade!!.applyDetection(mat, mGray);
                val (mat2, isNewFire) = firetruckCascade!!.applyDetection(mat1, mGray);
                val (mat3, isNewAmbulance) = ambulanceCascade!!.applyDetection(mat2, mGray);
                if(isNewPolice || isNewFire || isNewAmbulance){
                    handleNew(mat3)
                }
                return mat3
            }
            "HAAR" -> {
                var mGray: Mat = Mat();
                Imgproc.cvtColor(mat, mGray, Imgproc.COLOR_RGBA2GRAY);
                val (mat1, isNewPolice) = policeCascade!!.applyDetection(mat, mGray);
                val (mat2, isNewFire) = firetruckCascade!!.applyDetection(mat, mGray);
                val (mat3, isNewAmbulance) = ambulanceCascade!!.applyDetection(mat2, mGray);
                if(isNewPolice || isNewFire || isNewAmbulance){
                    handleNew(mat3)
                }
                return mat3
            }
            "Yolov3" -> {
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
                val (mat, isNew) = yolov3!!.applyDetection(mat);
                if(isNew){
                    handleNew(mat)
                }
                return mat;
            }
        }
        Log.d(TAG, "No detection")
        return mat;

    }

    private fun handleNew(mat: Mat){
        val now: Long = System.currentTimeMillis()
        if(now - lastTimeSave > UPDATE_TIME_PERIOD){
            lastTimeSave = now
            makeSound();
            SavePhotoUtils.saveImage(mat, getApplicationContext())
            Log.d(TAG, "Zapisywanie")
        }
    }

    private fun makeSound(){
        if(!soundEffects) return
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_READ_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                                grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    cameraView.setCameraPermissionGranted();
                }
            }
            PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() &&
                                grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    permissionSavePhotos = true;
                }
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun askForPermission(){
        when {
            hasPermissions(this, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                cameraView.setCameraPermissionGranted();
                permissionSavePhotos = true;
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.d(TAG, "Nie przyznano uprawnień na użycie kamery")
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                Log.d(TAG, "Nie przyznano uprawnień na zapis")
            }
            else -> {
                ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_READ_CAMERA
                )
            }
        }
    }
}