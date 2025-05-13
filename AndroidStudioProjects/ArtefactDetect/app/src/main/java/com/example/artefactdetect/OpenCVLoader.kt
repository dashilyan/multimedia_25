//package com.example.artefactdetect
//
//import android.content.Context
//import org.opencv.android.BaseLoaderCallback
//import org.opencv.android.LoaderCallbackInterface
//import org.opencv.android.OpenCVLoader
//
//object OpenCVLoader {
//    private var initialized = false
//    private lateinit var loaderCallback: BaseLoaderCallback
//
//    fun init(context: Context, onInit: (Boolean) -> Unit = {}) {
//        if (initialized) {
//            onInit(true)
//            return
//        }
//
//        loaderCallback = object : BaseLoaderCallback(context) {
//            override fun onManagerConnected(status: Int) {
//                when (status) {
//                    LoaderCallbackInterface.SUCCESS -> {
//                        initialized = true
//                        onInit(true)
//                    }
//                    else -> {
//                        super.onManagerConnected(status)
//                        onInit(false)
//                    }
//                }
//            }
//        }
//
//        if (OpenCVLoader.initDebug()) {
//            initialized = true
//            onInit(true)
//        } else {
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, loaderCallback)
//        }
//    }
//
//    fun isInitialized(): Boolean = initialized
//}