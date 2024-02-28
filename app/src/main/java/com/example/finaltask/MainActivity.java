package com.example.finaltask;

import android.Manifest;
import android.content.Context;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;

import com.example.finaltask.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.finaltask.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        boolean isDarkThemeEnabled = sharedPreferences.getBoolean("dark_theme_enabled", false);
        // 修改主题
        if (isDarkThemeEnabled) {
            setTheme(R.style.Theme_myDark);
        } else {
            setTheme(R.style.Theme_myLight);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //隐藏acntionBar
        //Objects.requireNonNull(getSupportActionBar()).hide();
        // 获取主题中定义的颜色属性
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.ColorActionBar, typedValue, true);
        int actionBarColor = typedValue.data;
        // 获取 ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 设置 ActionBar 的背景颜色
            actionBar.setBackgroundDrawable(new ColorDrawable(actionBarColor));
        }
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_asr, R.id.navigation_tts, R.id.navigation_ocr)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}