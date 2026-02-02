package com.atjaa.myapplication

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.atjaa.myapplication.databinding.ActivityAdminMonitorUnlineListBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminMonitorUnlineListActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdminMonitorUnlineListBinding
    lateinit var ip: String
    lateinit var uuid: String

    private var monitorUnlineAppFragment = MonitorUnlineAppFragment()
    private var monitorUnlineAddFragment = MonitorUnlineAddFragment()
    private var activeFragment: Fragment = monitorUnlineAppFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminMonitorUnlineListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ip = intent.getStringExtra("ip").toString()
        uuid = intent.getStringExtra("uuid").toString()
        binding.txtIp.text = ip

        monitorUnlineAppFragment.updateContent(uuid)
        monitorUnlineAddFragment.updateContent(uuid)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, monitorUnlineAddFragment, "2").hide(monitorUnlineAddFragment)
            add(R.id.fragment_container, monitorUnlineAppFragment, "1")
        }.commit()

        var bottomNavView: BottomNavigationView = binding.bottomNavView
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_count -> {
                    switchFragment(monitorUnlineAppFragment)
                    true
                }

                R.id.menu_download -> {
                    switchFragment(monitorUnlineAddFragment)
                    true
                }

                else -> false
            }
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment != targetFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(targetFragment)
                .commit()
            activeFragment = targetFragment
        }
    }

    fun back(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }
}