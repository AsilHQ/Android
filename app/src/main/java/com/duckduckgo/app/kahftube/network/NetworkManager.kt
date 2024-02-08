package org.halalz.kahftube.network

import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import org.halalz.kahftube.constants.ApiConstant
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import timber.log.Timber

class NetworkManager {

    companion object {

        const val TEMP_AUTHORIZATION: String = ""

        val retrofitClient: Api = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ApiConstant.BASE_URL)
            .client(
                OkHttpClient.Builder().addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        //.addHeader("Authorization", "Bearer $TEMP_AUTHORIZATION")
                        .build()
                    chain.proceed(request)
                }.build(),
            )
            //.client(oktHttpClient.build())
            .build().create(Api::class.java)
    }

    fun getRequest(
        url: String,
        listener: RequestListener
    ) {
        val accessToken = TEMP_AUTHORIZATION

        if (accessToken.isEmpty()) {
            retrofitClient.getData(url).enqueue(APICallBack(listener = listener))
        } else {
            retrofitClient.getData(accessToken, url).enqueue(APICallBack(listener = listener))
        }
    }

    fun postRequest(
        url: String,
        params: Map<String, @JvmSuppressWildcards Any?>,
        listener: RequestListener
    ) {
        val accessToken = TEMP_AUTHORIZATION

        if (accessToken.isEmpty()) {
            retrofitClient.postData(url, params)
                .enqueue(APICallBack(params = params, listener = listener))
        } else {
            retrofitClient.postData(accessToken, url, params)
                .enqueue(APICallBack(params = params, listener = listener))
        }
    }

    fun deleteRequest(
        url: String,
        params: Map<String, @JvmSuppressWildcards Any?>,
        listener: RequestListener
    ) {
        val accessToken = TEMP_AUTHORIZATION

        if (accessToken.isEmpty()) {
            retrofitClient.deleteData(url, params)
                .enqueue(APICallBack(params = params, listener = listener))
        } else {
            retrofitClient.deleteData(accessToken, url, params)
                .enqueue(APICallBack(params = params, listener = listener))
        }
    }

    fun putRequest(
        url: String,
        params: Map<String, @JvmSuppressWildcards Any?>,
        listener: RequestListener
    ) {
        val accessToken = TEMP_AUTHORIZATION

        if (accessToken.isEmpty()) {
            retrofitClient.putData(url, params)
                .enqueue(APICallBack(params = params, listener = listener))
        } else {
            retrofitClient.putData(accessToken, url, params)
                .enqueue(APICallBack(params = params, listener = listener))
        }
    }
}

interface Api {
    @POST
    @Headers("Content-Type: application/json")
    fun postData(
        @Url url: String,
        @Body params: Map<String, @JvmSuppressWildcards Any?>
    ): Call<JsonElement?>

    @POST
    @Headers("Content-Type: application/json")
    fun postData(
        @Header(NetworkManager.TEMP_AUTHORIZATION) authorization: String,
        @Url url: String,
        @Body params: Map<String, @JvmSuppressWildcards Any?>
    ): Call<JsonElement?>

    @GET
    @Headers("Content-Type: application/json")
    fun getData(
        @Header(NetworkManager.TEMP_AUTHORIZATION) authorization: String,
        @Url url: String?
    ): Call<JsonElement?>

    @GET
    fun getData(
        @Url url: String?
    ): Call<JsonElement?>

    @HTTP(method = "DELETE", hasBody = true)
    @Headers("Content-Type: application/json")
    fun deleteData(
        @Url url: String,
        @Body params: Map<String, @JvmSuppressWildcards Any?>
    ): Call<JsonElement?>

    @HTTP(method = "DELETE", hasBody = true)
    @Headers("Content-Type: application/json")
    fun deleteData(
        @Header(NetworkManager.TEMP_AUTHORIZATION) authorization: String,
        @Url url: String,
        @Body params: Map<String, @JvmSuppressWildcards Any?>
    ): Call<JsonElement?>

    @HTTP(method = "PUT", hasBody = true)
    @Headers("Content-Type: application/json")
    fun putData(
        @Url url: String,
        @Body params: Map<String, @JvmSuppressWildcards Any?>
    ): Call<JsonElement?>

    @HTTP(method = "PUT", hasBody = true)
    @Headers("Content-Type: application/json")
    fun putData(
        @Header(NetworkManager.TEMP_AUTHORIZATION) authorization: String,
        @Url url: String,
        @Body params: Map<String, @JvmSuppressWildcards Any?>
    ): Call<JsonElement?>
}

class APICallBack<T>(
    private val params: Map<String, @JvmSuppressWildcards Any?>? = null,
    private val listener: RequestListener
) : Callback<T> {
    private val tag: String = this.javaClass.simpleName

    override fun onResponse(
        call: Call<T>,
        response: Response<T>
    ) {
        try {
            Timber.d("accessToken: " + NetworkManager.TEMP_AUTHORIZATION)

            Timber.d("url: (method: " + call.request().method + "): " + call.request().url)
            Timber.d("headers(): " + call.request().headers)
            params?.let { Timber.d("params: $params") }
            Timber.d("onResponse: $response")
            //Log.d("NetworkManager", "onResponse: $response")
            //Log.d("NetworkManager", "onResponse: ${response.body()}")

            if (response.code() == 200) {
                Timber.d("onSuccess: " + response.body())
                //Log.d("NetworkManager", "onSuccess: " + response.body())
                listener.onSuccess(response.body().toString())
            } else {
                response.errorBody()?.let {

                    var message: String
                    val errorBody = it.string()
                    Timber.e("onFailure: errorBody: $errorBody")

                    try {
                        val body = JSONObject(errorBody)
                        message = body.getString("message")
                        val code = body.getInt("code")


                        when (ErrorCodeEnum.valueOf(code)) {
                            ErrorCodeEnum.OK -> {}
                            ErrorCodeEnum.Canceled -> {}
                            ErrorCodeEnum.Unknown -> {}
                            ErrorCodeEnum.InvalidArgument -> {}
                            ErrorCodeEnum.DeadlineExceeded -> {}
                            ErrorCodeEnum.NotFound -> {}
                            ErrorCodeEnum.AlreadyExists -> {}
                            ErrorCodeEnum.PermissionDenied -> {}
                            ErrorCodeEnum.ResourceExhausted -> {}
                            ErrorCodeEnum.FailedPrecondition -> {}
                            ErrorCodeEnum.Aborted -> {}
                            ErrorCodeEnum.OutOfRange -> {}
                            ErrorCodeEnum.Unimplemented -> {}
                            ErrorCodeEnum.Internal -> {}
                            ErrorCodeEnum.Unavailable -> {}
                            ErrorCodeEnum.DataLoss -> {}
                            ErrorCodeEnum.Unauthenticated -> {}
                            null -> {}
                        }
                    } catch (e: Exception) {
                        message = errorBody
                    }
                    listener.onError(message)
                }
            }
        } catch (e: Exception) {
            Timber.e("onFailure: (catch): $e")
            listener.onError(e.toString())
        }
    }

    override fun onFailure(
        call: Call<T>,
        t: Throwable
    ) {
        if (t is NoConnectivityException) {
            // show No Connectivity message to user or do whatever you want.
            Timber.e("onFailure: NoConnectivityException: " + t.message)
        }
        Timber.e("url: (method: " + call.request().method + "): " + call.request().url)
        Timber.e("onFailure: " + t.message)
        listener.onError(t.message.toString())
    }
}

@JvmSuppressWildcards
interface RequestListener {
    fun onSuccess(response: String)
    fun onError(error: String)
}
