package com.websarva.wings.android.asynchandlersample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        private const val DEBUG_TAG = "AsyncSample"
        private const val WEATHERINFO_URL = "https://api.openweathermap.org/data/2.5/weather?lang=ja"
        private const val APP_ID = "84c24117ac7cbebfe1b2825e73829b62"
    }
    private var _list: MutableList<MutableMap<String, String>> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _list = createList()

        val lvCityList = findViewById<ListView>(R.id.lvCityList)
        val from = arrayOf("name")
        val to = intArrayOf(android.R.id.text1)
        val adapter = SimpleAdapter(this@MainActivity, _list, android.R.layout.simple_list_item_1, from, to)
        lvCityList.adapter = adapter
        lvCityList.onItemClickListener = ListItemClickListener()
    }

    private fun createList(): MutableList<MutableMap<String, String>> {
        var list: MutableList<MutableMap<String, String>> = mutableListOf()

        var city = mutableMapOf("name" to "大阪", "q" to "Osaka")
        list.add(city)
        city = mutableMapOf("name" to "神戸", "q" to "Kobe")
        list.add(city)
        city = mutableMapOf("name" to "仙台", "q" to "Sendai")
        list.add(city)
        city = mutableMapOf("name" to "福岡", "q" to "Fukuoka")
        list.add(city)
        city = mutableMapOf("name" to "佐賀", "q" to "Saga")
        list.add(city)
        city = mutableMapOf("name" to "北海道", "q" to "Hokkaido")
        list.add(city)
        city = mutableMapOf("name" to "広島", "q" to "Hiroshima")
        list.add(city)
        return list
    }

    @UiThread
    private fun addMsg(msg: String) {
        val tvWeatherDesc = findViewById<TextView>(R.id.tvWeatherDesc)
        var msgNow = tvWeatherDesc.text
        if(!msgNow.equals("")) {
            msgNow = "${msgNow}¥n"
        }
        msgNow = "${msgNow}${msg}"
        tvWeatherDesc.text = msgNow
    }

    @UiThread
    private fun receiveWeatherInfo(urlFull: String) {
        val tvWeatherTelop = findViewById<TextView>(R.id.tvWeatherTelpo)
        tvWeatherTelop.text = ""
        val tvWeatherDesc = findViewById<TextView>(R.id.tvWeatherDesc)
        tvWeatherDesc.text = ""
        val handler = HandlerCompat.createAsync(mainLooper)
        val backgroundReceiver = WeatherInfoBackgroundReceiver(handler, urlFull)
        val executeService = Executors.newSingleThreadExecutor()
        executeService.submit(backgroundReceiver)
    }
    private inner class WeatherInfoBackgroundReceiver(handler: Handler, url: String): Runnable {
        private val _handler = handler
        private val _url = url
        @WorkerThread
        override fun run() {
            var progressUpdate = ProgressUpdateExecutor("バックグラウンド処理開始")
            _handler.post(progressUpdate)

            var result = ""
            val url = URL(_url)
            val con = url.openConnection() as HttpURLConnection
            con.connectTimeout = 100
            con.readTimeout = 100
            con.requestMethod = "GET"
            try {
                progressUpdate = ProgressUpdateExecutor("Webアクセス開始。")
                _handler.post(progressUpdate)
                con.connect()
                val stream = con.inputStream
                result = is2String(stream)
                stream.close()

                progressUpdate = ProgressUpdateExecutor("Webアクセス終了。")
                _handler.post(progressUpdate)
            }
            catch(ex: SocketTimeoutException) {
                Log.w(DEBUG_TAG, "通信タイムアウト", ex)
            }
            con.disconnect()
            progressUpdate = ProgressUpdateExecutor("バックグラウンド処理終了。")
            _handler.post(progressUpdate)

            val postExector = WeatherInfoReceivePostExecutor(result)
            _handler.post(postExector)
        }

        private inner class ProgressUpdateExecutor(msg: String): Runnable {
            private val _msg = msg

            @UiThread
            override fun run() {
                addMsg(_msg)
            }
        }

        private fun is2String(stream: InputStream): String {
            val sb = StringBuilder()
            val reader = BufferedReader(
                InputStreamReader(
                stream, StandardCharsets.UTF_8)
            )
            var line = reader.readLine()
            while(line != null) {
                sb.append(line)
                line = reader.readLine()
            }
            reader.close()
            return sb.toString()
        }
    }

    private inner class WeatherInfoReceivePostExecutor(result: String): Runnable {
        private val _result = result
        @UiThread
        override fun run() {
            val rootJSON = JSONObject(_result)
            val cityName = rootJSON.getString("name")

            val coordJSON = rootJSON.getJSONObject("coord")
            val latitude = coordJSON.getString("lat")
            val longitude = coordJSON.getString("lon")
            val weatherJSONArray = rootJSON.getJSONArray("weather")
            val weatherJSON = weatherJSONArray.getJSONObject(0)
            val weather = weatherJSON.getString("description")
            val telop = "${cityName}の天気"
            val desc = "現在は${weather}です。¥n井戸は${latitude}度で軽度は${longitude}です。"
            val tvWeatherTelop = findViewById<TextView>(R.id.tvWeatherTelpo)
            val tvWeatherDesc = findViewById<TextView>(R.id.tvWeatherDesc)
            tvWeatherTelop.text = telop
            tvWeatherDesc.text = desc
        }
    }

    private inner class ListItemClickListener: AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val item = _list.get(position)
            val q = item.get("q")
            q?.let {
                val urlFull = "$WEATHERINFO_URL&q=$q&appid=$APP_ID"
                receiveWeatherInfo(urlFull)
            }
        }
    }
}