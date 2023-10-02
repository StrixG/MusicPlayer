package com.obrekht.musicplayer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.obrekht.musicplayer.databinding.ActivityMainBinding
import com.obrekht.musicplayer.utils.viewBinding

class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}
