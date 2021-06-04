package com.bronikowski.ripo.utils

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import com.bronikowski.ripo.MainActivity
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.File

data class DetectedObject(val leftTop: Point, val rightBottom: Point, val center: Point, val confidence: Float)

class Yolov3Wrapper {
    private val DELTA: Float = 100f;
    private val MINCONFIDENCE = 0.7f
    private val TAG = MainActivity::class.java.simpleName
    private var configFile : File? = null;
    private var modelWeightsFile : File? = null;
    private var net: Net? = null;
    private var _names : ArrayList<String>
    private var _colors : ArrayList<Scalar>
    private var _lastDetected : ArrayList<ArrayList<DetectedObject>>
    constructor(@RawRes cfgRawId: Int, @RawRes weightsRawId: Int, name: String, names: ArrayList<String>, colors: ArrayList<Scalar>, context: Context){
        _names = names;
        _colors = colors;
        _lastDetected = ArrayList<ArrayList<DetectedObject>>(_names.size);
        for(classId: Int in _names.indices){
            _lastDetected.add(ArrayList<DetectedObject>())
        }
        if(configFile==null){
            configFile = SaveFileUtils.saveFile(cfgRawId, "yolo-" + name + "-conf", "yolo-" + name + "-conf.conf", context)
        }
        if(modelWeightsFile==null){
            modelWeightsFile = SaveFileUtils.saveFile(weightsRawId, "yolo-" + name + "-weights", "yolo-" + name + "-weights.weights", context)
        }
        if(configFile!=null && modelWeightsFile!=null) {

            net = Dnn.readNetFromDarknet(configFile!!.getAbsolutePath(), modelWeightsFile!!.getAbsolutePath())
        }else{
            Log.e(TAG, "Failed to load dnn module")
        }
    }

    private fun findNear(objectList: ArrayList<DetectedObject>, detectedObject: DetectedObject): Boolean{
        var foundNear = false;
        for(objectItem in objectList){
            if((detectedObject.center.x >= objectItem.center.x - DELTA || detectedObject.center.x <= objectItem.center.x + DELTA) &&
                    (detectedObject.center.y >= objectItem.center.y - DELTA || detectedObject.center.y <= objectItem.center.y + DELTA)){
                foundNear = true;
            }
        }
        return foundNear;
    }

    public fun applyDetection(mat: Mat) : Pair<Mat, Boolean>{
        if(net == null || net!!.empty()) return Pair(mat, false);
        var newRect: Boolean = false
        val blob = Dnn.blobFromImage(mat, 1/255.0, Size(256.0, 256.0), Scalar(0.0), false, false)
        val result: List<Mat> = ArrayList()
        val outBlobNames = net!!.unconnectedOutLayersNames
        net!!.setInput(blob);
        net!!.forward(result, outBlobNames)

        val detectedObjects = ArrayList<ArrayList<DetectedObject>>(_names.size);
        for(classId: Int in _names.indices){
            detectedObjects.add(ArrayList<DetectedObject>())
        }
        for (i in result.indices) {
            val level = result[i]
            for (j in 0 until level.rows()) {
                val row = level.row(j)
                val scores = row.colRange(5, level.cols())
                val mm = Core.minMaxLoc(scores)
                val classIdPoint = mm.maxLoc
                val confidence = mm.maxVal.toFloat()
                if (confidence > MINCONFIDENCE) {
                    val centerX = (row[0, 0][0] * mat.cols())
                    val centerY = (row[0, 1][0] * mat.rows())
                    val width = (row[0, 2][0] * mat.cols())
                    val height = (row[0, 3][0] * mat.rows())
                    val classId: Int = classIdPoint.x.toInt()
                    val detectedObject = DetectedObject(Point((centerX - width * 0.5), (centerY - height * 0.5)),
                            Point((centerX + width * 0.5), (centerY + height * 0.5)),
                            Point(centerX, centerY),
                            confidence
                        )
                    if(_names.size >= classId && !findNear(detectedObjects[classId], detectedObject)){
                        detectedObjects[classId].add(detectedObject);
                    }
                }
            }
        }
        for (classId: Int in detectedObjects.indices) {
            for(detectedObject in detectedObjects[classId]){
                Imgproc.rectangle(mat,
                        detectedObject.leftTop,
                        detectedObject.rightBottom,
                        _colors[classId],
                        3)
                var textPos = detectedObject.leftTop
                textPos.y = textPos.y - 5.0;
                if(textPos.y>0){
                    Imgproc.putText(mat, _names[classId]+": "+detectedObject.confidence.toString(), textPos, Imgproc.FONT_HERSHEY_COMPLEX_SMALL, 1.0, _colors[classId])
                }
            }
        }
        var isNew = true;
        var objectDetected = false;
        for (classId: Int in detectedObjects.indices) {
            if(detectedObjects[classId].size>0){
                objectDetected = true;
            }
            for(detectedObject in detectedObjects[classId]){
                if(findNear(_lastDetected[classId], detectedObject)){
                    isNew = false
                    break
                }
            }
            if(!isNew){
                break
            }
        }
        _lastDetected = ArrayList(detectedObjects)
        return Pair(mat, isNew && objectDetected);
    }
}