//
// Created by HUY QUANG on 10/13/2020.
//

#ifndef GRAYIMAGE_BITMAP_UTILS_H
#define GRAYIMAGE_BITMAP_UTILS_H

#include <android/bitmap.h>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

extern "C" {
    /**
     * Bitmap Rotation matrix
     * @param env JNI Environmental Science
     * @param bitmap Bitmap object
     * @param mat Picture matrix
     * @param needPremultiplyAlpha Does it multiply transparency?
     */
    void bitmap2Mat(JNIEnv *env, jobject bitmap, Mat *mat, bool needPremultiplyAlpha = false);

    /**
    * Matrix to Bitmap
    * @param env JNI Environmental Science
    * @param mat Picture matrix
    * @param bitmap Bitmap object
    * @param needPremultiplyAlpha Does it multiply transparency?
    */
    void mat2Bitmap(JNIEnv *env, Mat mat, jobject bitmap, bool needPremultiplyAlpha = false);

    /**
    *
    * Create Bitmap
    * @param env JNI Environmental Science
    * @param src matrix
    * @param config Bitmap To configure
    * @return Bitmap object
    */
    jobject createBitmap(JNIEnv *env, Mat src, jobject config);
}

#endif //GRAYIMAGE_BITMAP_UTILS_H
