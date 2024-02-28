package com.example.finaltask.ui.ocr;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

import com.example.finaltask.MainActivity;
import com.example.finaltask.R;
import com.example.finaltask.databinding.FragmentOcrBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class OcrFragment extends Fragment {
    private final static int CAMERA_CODE = 1 , GALLERY_CODE = 2;
    private FragmentOcrBinding binding;
    private ImageView imageView;
    private Button buttonCopy;
    private View root;
    private LinearLayout camera,gallery;
    private EditText text;
    private ImageButton cameraButton , galleryButton;
    private String base64Str ;
    private String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic";   //api地址，高精度版
    private String apiKey = "24.be507776026e858c4552ac3302fa0ea4.2592000.1696063716.282335-38560831"; // API密钥  AccessToken

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOcrBinding.inflate(inflater, container, false);
        init();  // 初始化，绑定组件
        clickCamera();
        clickGalley();
        copyFun();
        return root;
    }
    private void openCamera() {
        // 启动相机程序
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_CODE);
    }
    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_CODE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_CODE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap bitmap = (Bitmap) extras.get("data");
                imageView.setImageBitmap(bitmap);
                base64Str = bitmapToBase64(bitmap);
                onOCR();
            }
        }
        else if(requestCode == GALLERY_CODE&& resultCode == Activity.RESULT_OK && data != null){
            Uri imageUri = data.getData();
            imageView.setImageURI(imageUri);
            base64Str = uriToBase64(imageUri);
            onOCR();
        }
    }
    //点击相机触发事件
    private void clickCamera(){
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
    }
    private void clickGalley(){
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
    }
    //bitmap转base64格式，以便于OCR发送格式
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    //URI格式转base64格式，用于OCR
    public String uriToBase64(Uri uri) {
        try {
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    //执行图片识别功能
    private void onOCR() {
        if (base64Str == null) {
            Toast.makeText(requireContext(), "图片格式转换错误！", Toast.LENGTH_SHORT ).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String imgParam = URLEncoder.encode(base64Str, "UTF-8");
                    String param = "image=" + imgParam;
                    URL realUrl = new URL(url + "?access_token=" + apiKey);
                    HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Charset", "UTF-8");
                    PrintWriter out = new PrintWriter(connection.getOutputStream());
                    out.print(param);
                    out.flush();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder resultBuilder = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        resultBuilder.append(line);
                    }
                    in.close();
                    out.close();
                    connection.disconnect();
                    final String result = resultBuilder.toString();
                    // 在子线程中更新 UI，例如显示结果或通知用户请求完成
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("图片返回值", result);
                            // 在这里更新 UI，例如显示结果或通知用户请求完成
                            text.setText(jsonToString(result));
                            Toast.makeText(requireContext(), "识别成功！", Toast.LENGTH_SHORT ).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    final String errorMessage = "OCR网络请求错误：" + e.getMessage();
                    // 在子线程中更新 UI，例如显示错误信息
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
    private String jsonToString(String jsonString){
        try {
            JSONObject jsonObject = new JSONObject(jsonString); // jsonString 是你的 JSON 数据
            JSONArray wordsResultArray = jsonObject.getJSONArray("words_result");

            StringBuilder wordsBuilder = new StringBuilder();
            for (int i = 0; i < wordsResultArray.length(); i++) {
                JSONObject wordsObject = wordsResultArray.getJSONObject(i);
                String words = wordsObject.getString("words");
                wordsBuilder.append(words).append(" ");
            }
            String concatenatedWords = wordsBuilder.toString().trim();
            Log.d("Concatenated Words", concatenatedWords);
            return concatenatedWords;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void copyFun(){
        buttonCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = text.getText().toString();
                // 获取Activity的上下文
                Context context = requireActivity();
                // 获取系统剪贴板服务
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                // 创建一个剪贴板数据项
                ClipData clip = ClipData.newPlainText("label", value);
                // 将数据项放入剪贴板
                clipboard.setPrimaryClip(clip);
                // 提示用户已复制到剪贴板
                Toast.makeText(context, "文本已复制到剪贴板", Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void init(){
        base64Str = null;
        root = binding.getRoot();
        imageView = root.findViewById(R.id.placeholderImageView);
        camera = root.findViewById(R.id.linearLayout2);
        gallery = root.findViewById(R.id.linearLayout);
        text = root.findViewById(R.id.multiLineEditText);
        cameraButton = root.findViewById(R.id.cameraButton);
        galleryButton = root.findViewById(R.id.galleryButton);
        buttonCopy = root.findViewById(R.id.buttonCopy);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}