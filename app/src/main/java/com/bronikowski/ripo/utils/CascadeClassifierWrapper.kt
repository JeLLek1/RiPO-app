package com.bronikowski.ripo.utils

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bronikowski.ripo.R
import org.opencv.core.Scalar
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.Context
import androidx.annotation.RawRes
import com.bronikowski.ripo.MainActivity
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class CascadeClassifierWrapper {
    private val TAG = MainActivity::class.java.simpleName
    private var file: File? = null;
    private var cascade: CascadeClassifier? = null;
    private var _color: Scalar;
    private var _name: String
    private var oldRect : Array<Rect>? = null

    constructor(@RawRes rawId: Int, name: String, color: Scalar, context: Context){
        _color = color
        _name = name
        if(file==null){
            file = SaveFileUtils.saveFile(rawId, "cascade-"+name, "cascade-"+name+".xml", context)
        }
        if(file!=null) {
            cascade = CascadeClassifier(file!!.getAbsolutePath())
            if (cascade!!.empty()) {
                Log.e(TAG, "Failed to load cascade classifier")
                cascade = null
            } else Log.i(TAG, "Loaded cascade classifier from " + file!!.getAbsolutePath())
        }
    }

    public fun applyDetection(mat: Mat, matGray: Mat) : Pair<Mat, Boolean>{
        var newRect: Boolean = false
        if(cascade != null){
            val rect: MatOfRect = MatOfRect();
            cascade!!.detectMultiScale(matGray, rect, 1.1, 3, 0, Size(200.0, 200.0), Size());
            val rectArray: Array<Rect> = rect.toArray()

            if(rectArray.size >0 && oldRect == null){
                newRect = true;
            }

            for (i in 0 until rectArray.size) {
                var textPos: Point = rectArray.get(i).tl()
                textPos.y = textPos.y - 5.0;
                if(textPos.y>0){
                    Imgproc.putText(mat, _name, textPos, Imgproc.FONT_HERSHEY_COMPLEX_SMALL, 1.0, _color)
                }
                Imgproc.rectangle(mat, rectArray.get(i).tl(), rectArray.get(i).br(), _color, 3)

                val middle: Point = Point((rectArray.get(i).x + rectArray.get(i).br().x)*0.5, (rectArray.get(i).y + rectArray.get(i).br().y)*0.5)
                if(oldRect != null){
                    for(i in 0 until oldRect!!.size){
                        Log.e(TAG, "Test: xTop: "+oldRect!!.get(i).x.toString()+" xBottom: "+oldRect!!.get(i).br().x.toString())
                        if(middle.x > oldRect!!.get(i).x && middle.x < oldRect!!.get(i).br().x && middle.y > oldRect!!.get(i).y && middle.y < oldRect!!.get(i).br().y){
                            newRect = true;
                        }
                    }
                }
            }
            oldRect = rectArray
        }
        return Pair(mat, newRect)
    }
}