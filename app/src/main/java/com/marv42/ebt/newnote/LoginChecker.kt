/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.marv42.ebt.newnote.data.LoginInfo
import com.marv42.ebt.newnote.exceptions.CallResponseException
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.HttpCallException
import kotlinx.coroutines.*

class LoginChecker(private val app: ThisApp, private val apiCaller: ApiCaller) {

    private val scope = MainScope()

    fun execute() {
        scope.executeAsyncTask(
           onPreExecute = {
            onPreExecute()
        }, doInBackground = {
            doInBackground()
        }, onPostExecute = {
            onPostExecute(it)
        })
    }

    private fun onPreExecute() {
        Toast.makeText(app, R.string.trying_login, LENGTH_LONG).show()
    }

    private fun doInBackground(): String {
        return try {
            tryToLogIn()
        } catch (e: HttpCallException) {
            ErrorMessage.ERROR + e.message
        } catch (e: CallResponseException) {
            ErrorMessage.ERROR + e.message
        }
    }

    @Throws(HttpCallException::class, CallResponseException::class)
    private fun tryToLogIn(): String {
        val loginInfo = apiCaller.callLogin()
        return setPreferences(loginInfo)
    }

    private fun setPreferences(loginInfo: LoginInfo): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(app)
        val editor = preferences.edit()
        val loginValuesOkKey = app.getString(R.string.pref_login_values_ok_key)
        if (loginInfo.sessionId.isEmpty()) {
            editor.putBoolean(loginValuesOkKey, false).apply()
            return ErrorMessage.ERROR + app.getString(R.string.wrong_login_info)
        }
        editor.putBoolean(loginValuesOkKey, true).apply()
        setLocationInfo(loginInfo, editor)
        return """${app.getString(R.string.logged_in)}
${app.getString(R.string.hello)} ${loginInfo.userName}"""
    }

    private fun setLocationInfo(loginInfo: LoginInfo, editor: SharedPreferences.Editor) {
        editor.putString(app.getString(R.string.pref_country_key), loginInfo.locationValues.country)
                .putString(app.getString(R.string.pref_city_key), loginInfo.locationValues.city)
                .putString(app.getString(R.string.pref_postal_code_key), loginInfo.locationValues.postalCode).apply()
    }

    private fun onPostExecute(result: String) {
        var text = result
        if (text.startsWith(ErrorMessage.ERROR))
            text = ErrorMessage(app).getErrorMessage(text)
        Toast.makeText(app, text, LENGTH_LONG).show()
    }

    companion object {
        @JvmStatic
        fun checkLoginInfo(activity: FragmentActivity) {
            val app = activity.application
            if (!PreferenceManager.getDefaultSharedPreferences(app).getBoolean(app.getString(R.string.pref_login_values_ok_key), false)) {
                AlertDialog.Builder(activity).setTitle(app.getString(R.string.invalid_login))
                        .setMessage(app.getString(R.string.wrong_login_info) + "." + app.getString(R.string.redirect_to_settings) + ".")
                        .setPositiveButton(app.getString(R.string.ok)
                        ) { dialog: DialogInterface, _: Int ->
                            activity.startActivity(Intent(activity.applicationContext,
                                    SettingsActivity::class.java))
                            dialog.dismiss()
                        }
                        .show()
            }
        }
    }
}