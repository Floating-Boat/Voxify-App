package com.example.finaltask.ui.home;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.finaltask.MainActivity;
import com.example.finaltask.R;
import com.example.finaltask.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private Switch switchThemeButton;
    private SharedPreferences sharedPreferences;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        sharedPreferences = requireContext().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        switchThemeButton = root.findViewById(R.id.themeSwitch);
        switchThemeButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean currentTheme = sharedPreferences.getBoolean("dark_theme_enabled", false);
                if (isChecked != currentTheme) {
                    // Save the theme choice to SharedPreferences
                    sharedPreferences.edit().putBoolean("dark_theme_enabled", isChecked).apply();
                    switchThemeButton.setText("黑夜");
                    // Recreate the activity to apply the new theme
                    getActivity().recreate();
                }
            }
        });
        // 获取主题中的ColorActionBar属性值
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = requireContext().getTheme();
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        int colorActionBar = typedValue.data;
        // 根据ColorActionBar属性值设置Switch状态
        if (colorActionBar == -16676986) {
            switchThemeButton.setChecked(true);
            switchThemeButton.setText("黑夜");
        }
        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}