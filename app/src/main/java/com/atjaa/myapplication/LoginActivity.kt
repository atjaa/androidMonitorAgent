package com.atjaa.myapplication

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import com.atjaa.myapplication.databinding.ActivityLoginBinding
import es.dmoral.toasty.Toasty

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var pdView = binding.pdValue
        pdView.doOnTextChanged { text, start, before, count ->
            if ((text?.length ?: 0) > 20) {
                binding.pdInput.error = "Length out of range"
            }
        }
    }

    fun login(view: View) {
        var pd = binding.pdValue
        if ("1234".equals(pd.text.toString())) {
            Toasty.success(this, "login success").show()
            val intent = Intent(this, MonitorActivity::class.java)
            startActivity(intent)

        } else if ("7758521".equals(pd.text.toString())) {
            // 进入到管理页面
            val intent = Intent(this, AdminMonitorActivity::class.java)
            startActivity(intent)
        } else {
            Toasty.error(this, "Invalid password").show()
        }
    }


}