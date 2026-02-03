package com.atjaa.myapplication.service

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder

/**
 * 认证服务：让系统识别你的账号
 */
class AuthenticatorService : Service() {

    override fun onBind(intent: Intent) = object : AbstractAccountAuthenticator(this) {
        override fun editProperties(r: AccountAuthenticatorResponse?, s: String?) = null
        override fun addAccount(r: AccountAuthenticatorResponse?, s: String?, s2: String?, a: Array<out String>?, o: Bundle?) = null
        override fun confirmCredentials(r: AccountAuthenticatorResponse?, a: Account?, o: Bundle?) = null
        override fun getAuthToken(r: AccountAuthenticatorResponse?, a: Account?, s: String?, o: Bundle?) = null
        override fun getAuthTokenLabel(s: String?) = null
        override fun updateCredentials(r: AccountAuthenticatorResponse?, a: Account?, s: String?, o: Bundle?) = null
        override fun hasFeatures(r: AccountAuthenticatorResponse?, a: Account?, a2: Array<out String>?) = null
    }.iBinder
}