package org.halalz.kahftube.network

import org.halalz.kahftube.constants.ApiConstant

/**
 * Created by Asif Ahmed on 30/1/24.
 */

class ApiService {
    fun registerApi(
        name: String,
        email: String,
        callback: RequestListener
    ) {
        /*
        val url = ApiConstant.REGISTER

        val jsonBody = """
        {
          "name": "$name",
          "email": "$email"
        }
        """.trimIndent()

        // Make the network call using your network service
        try {
            val response = NetworkService().makePostRequest(url, jsonBody, callback)
            //val response = NetworkService().makeGetRequest(url, callback)
            // Handle the response, e.g., update UI or log the response
            println("Response: $response")
        } catch (e: Exception) {
            // Handle exceptions, e.g., network error
            println("Error: ${e.message}")
        }*/

        val params = HashMap<String, @JvmSuppressWildcards Any?>()
        params["name"] = name
        params["email"] = email

        NetworkManager().postRequest(url = ApiConstant.REGISTER, params = params, listener = callback)
    }

    fun loginApi(
        email: String,
        callback: RequestListener
    ) {
        /*
        val url = ApiConstant.LOGIN

        val jsonBody = """
        {
          "email": "$email"
        }
        """.trimIndent()

        // Make the network call using your network service
        try {
            val response = NetworkService().makePostRequest(url, jsonBody, callback)
            //val response = NetworkService().makeGetRequest(url, callback)
            // Handle the response, e.g., update UI or log the response
            println("Response: $response")
        } catch (e: Exception) {
            // Handle exceptions, e.g., network error
            println("Error: ${e.message}")
        }*/
        val params = HashMap<String, @JvmSuppressWildcards Any?>()
        params["email"] = email

        NetworkManager().postRequest(url = ApiConstant.LOGIN, params = params, listener = callback)
    }
}
