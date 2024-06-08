package com.example.termp_new.openAi

import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

public class Gpt {
    companion object {
        var api_key="your_api_key"
        var url = "https://api.openai.com/v1/chat/completions"

        @JvmStatic
        fun getResponse(query: String, callback: VolleyCallback) {
            // creating a queue for request queue.
            val queue: RequestQueue = Volley.newRequestQueue(MyApplication.ApplicationContext())
            // creating a json object on below line.
            val jsonObject = JSONObject()
            // adding params to json object.
            jsonObject.put("model", "gpt-3.5-turbo")

            // Create the messages array
            val messagesArray = JSONArray()
            val messageObject = JSONObject()
            messageObject.put("role", "user")
            messageObject.put("content", query)
            messagesArray.put(messageObject)

            // Add messages array to the json object
            jsonObject.put("messages", messagesArray)

            jsonObject.put("temperature", 0)
            jsonObject.put("max_tokens", 100)
            jsonObject.put("top_p", 1)
            jsonObject.put("frequency_penalty", 0.0)
            jsonObject.put("presence_penalty", 0.0)

            // on below line making json object request.
            val postRequest: JsonObjectRequest =
                object : JsonObjectRequest(Method.POST, url, jsonObject,
                    Response.Listener { response ->
                        try {
                            // on below line getting response message and calling success callback.
                            val responseMsg: String =
                                response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                            callback.onSuccess(responseMsg)
                        } catch (e: Exception) {
                            callback.onError("Parsing error: " + e.message)
                        }
                    },
                    // adding on error listener
                    Response.ErrorListener { error ->
                        Log.e("TAGAPI", "Error is : " + error.message + "\n" + error)
                        callback.onError("Volley error: " + error.message)
                    }) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val params: MutableMap<String, String> = HashMap()
                        // adding headers on below line.
                        params["Content-Type"] = "application/json"
                        params["Authorization"] = "Bearer "+api_key
                        return params
                    }
                }

            // on below line adding retry policy for our request.
            postRequest.setRetryPolicy(object : RetryPolicy {
                override fun getCurrentTimeout(): Int {
                    return 5000
                }

                override fun getCurrentRetryCount(): Int {
                    return 3
                }

                @Throws(VolleyError::class)
                override fun retry(error: VolleyError) {
                    if (error.networkResponse?.statusCode == 429) {
                        Thread.sleep(2000) // Wait for 2 seconds before retrying
                    } else {
                        throw error
                    }
                }
            })
            // on below line adding our request to queue.
            queue.add(postRequest)
        }
    }
}