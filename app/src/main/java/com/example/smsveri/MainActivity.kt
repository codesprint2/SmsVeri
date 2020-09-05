package com.example.smsveri

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.smsveri.comm.ApiClient
import com.example.smsveri.model.SmsCallback
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "MainActivity"
private const val SERVER_URL = "http://157.230.59.18:8080"
private const val RESOLVE_HINT = 100
class MainActivity : AppCompatActivity() {

    private val mApiClient = Retrofit.Builder()
        .baseUrl(SERVER_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiClient::class.java)
    private val mFilter = IntentFilter("com.google.android.gms.auth.api.phone.SMS_RETRIEVED")
    private val mSmsBroadcastReceiver = object :  BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (SmsRetriever.SMS_RETRIEVED_ACTION  == it.action) {
                    val extras = it.extras
                    val status = extras?.get(SmsRetriever.EXTRA_STATUS) as Status?
                    when(status?.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            val smsMessage = extras?.get(SmsRetriever.EXTRA_SMS_MESSAGE) as String?
                            if (smsMessage != null) {
                                Log.d(TAG, "received message $smsMessage")
                                var code = smsMessage.substring(smsMessage.indexOf(":") + 1).trim()
                                if (code.length > 5) {
                                    code = code.substring(0, 6)
                                    tv_verify_code.text = getString(R.string.verification_code, code)
                                } else {
                                    tv_verify_code.text = getString(R.string.verification_code, "n/a")
                                }
                            }
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            Log.e(TAG, "receiver timeout")
                            tv_verify_code.text = getString(R.string.verification_code, "timeout")
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESOLVE_HINT) {
            if (resultCode == RESULT_OK) {
                val credential : Credential? = data?.getParcelableExtra(Credential.EXTRA_KEY)
                Log.d(TAG, credential?.id ?: "Phone number not available")
                credential?.let {
                    requestVerificationCode(it.id)
                    startSmsRetriever()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerReceiver(mSmsBroadcastReceiver, mFilter)
    }

    override fun onStart() {
        super.onStart()

        tv_sms_number.setOnClickListener {
            requestHint()
        }
    }

    private fun requestHint() {
        val hintRequest = HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build()
        val credentialsClient = Credentials.getClient(this)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        startIntentSenderForResult(intent.intentSender, RESOLVE_HINT, null, 0, 0, 0)
    }

    private fun requestVerificationCode(callbackNo: String) {
        mApiClient.sendMeCode(SmsCallback(callbackNo)).enqueue(object : Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e(TAG, "Failure - verification code request")
            }

            override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                Log.e(TAG, "Success - verification code request")
            }
        })
    }

    private fun startSmsRetriever() {
        val client = SmsRetriever.getClient(this)

        val task = client.startSmsRetriever()
        task.addOnSuccessListener {
            Log.d(TAG, "Successfully started retriever")
        }
        task.addOnFailureListener {
            Log.e(TAG, "Failed to start retriever")
        }
    }
}
