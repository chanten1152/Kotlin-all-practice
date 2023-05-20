package jp.techacademy.takatoshi.tenkou

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun total(first: Int, last: Int): Int {
        var sum = 0
        for (i in first..last)  {
            sum += i
        }

        return sum
    }

}