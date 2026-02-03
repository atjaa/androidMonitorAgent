package com.atjaa.myapplication.service

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri

/**
 * 占位 Provider
 */
class StubProvider : ContentProvider() {
    override fun onCreate() = true
    override fun query(u: Uri, p: Array<out String>?, s: String?, sa: Array<out String>?, o: String?) = null
    override fun getType(u: Uri) = null
    override fun insert(u: Uri, v: ContentValues?) = null
    override fun delete(u: Uri, s: String?, sa: Array<out String>?) = 0
    override fun update(u: Uri, v: ContentValues?, s: String?, sa: Array<out String>?) = 0
}