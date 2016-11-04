package com.sw926.imagefileselector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.Fragment
import android.text.TextUtils
import java.io.File


internal class ImagePickHelper(private val mContext: Context) {

    private var TAG = "ImagePickHelper"

    var errorCallback: ((ErrorResult) -> Unit)? = null
    var successCallback: ((String) -> Unit)? = null
    private var fragment: Fragment? = null
    private var activity: Activity? = null
    var mRequestCode = -1

    var mType = "image/*"

    fun selectImage(fragment: Fragment, requestCode: Int) {
        AppLogger.i(TAG, "start select image from fragment")
        mRequestCode = requestCode
        this.fragment = fragment
        this.activity = null
        this.fragment?.let {
            if (PermissionsHelper.checkAndRequestPermission(it, mRequestCode)) {
                AppLogger.i(TAG, "permission already grant")
                it.startActivityForResult(createIntent(), mRequestCode)
            }
        }
    }

    fun selectorImage(activity: Activity, requestCode: Int) {
        AppLogger.i(TAG, "start select image from activity")
        mRequestCode = requestCode
        this.activity = activity
        this.fragment = null
        this.activity?.let {
            if (PermissionsHelper.checkAndRequestPermission(it, mRequestCode)) {
                AppLogger.i(TAG, "permission already grant")
                it.startActivityForResult(createIntent(), mRequestCode)
            }
        }
    }

    fun startSelect() {
        AppLogger.i(TAG, "start system gallery activity")
        activity?.let {
            it.startActivityForResult(createIntent(), mRequestCode)
            return
        }

        fragment?.let {
            it.startActivityForResult(createIntent(), mRequestCode)
            return
        }
        AppLogger.e(TAG, "activity or fragment is null")
        errorCallback?.invoke(ErrorResult.error)
    }

    private fun createIntent(): Intent {
        val intent: Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        } else {
            intent = Intent(Intent.ACTION_GET_CONTENT)
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mType
        return intent
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == mRequestCode) {
            if (resultCode == Activity.RESULT_CANCELED) {
                AppLogger.i(TAG, "canceled select image")
                errorCallback?.invoke(ErrorResult.canceled)
            } else if (resultCode == Activity.RESULT_OK) {
                if (intent == null) {
                    AppLogger.e(TAG, "select image error, intent null")
                    errorCallback?.invoke(ErrorResult.error)
                } else {
                    val uri = intent.data
                    val path: String? = Compatibility.getPath(mContext, uri)
                    if (!TextUtils.isEmpty(path) && File(path).exists()) {
                        AppLogger.i(TAG, "select image success: $path")
                        successCallback?.invoke(path!!)
                    } else {
                        AppLogger.e(TAG, "select image file path $path is error or not exists")
                        errorCallback?.invoke(ErrorResult.error)
                    }
                }

            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, @Suppress("UNUSED_PARAMETER") permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == mRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSelect()
            } else {
                errorCallback?.invoke(ErrorResult.permissionDenied)
            }

        }
    }

}