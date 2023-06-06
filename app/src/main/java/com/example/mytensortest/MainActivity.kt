package com.example.mytensortest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mytensortest.ui.DetectorFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val fragment = DetectorFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(
                    R.id.fragment_container, fragment
                )
                .commit()
        }
    }
}