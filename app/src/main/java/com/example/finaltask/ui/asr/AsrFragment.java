package com.example.finaltask.ui.asr;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finaltask.R;

import com.example.finaltask.common.DemoException;
import com.example.finaltask.databinding.FragmentAsrBinding;
import com.example.finaltask.utils.PcmToWavUtil;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;


public class AsrFragment extends Fragment {
    private Button buttonPlay, buttonAsr, buttonCopy , buttonFileUpload;
    private ImageButton microphoneButton;
    private ImageView recordingIndicator;
    private MediaPlayer mediaPlayer;
    private FragmentAsrBinding binding;
    private View root;
    private TextView recordingTimeTextView , multiLineEditText;
    private static final int FILE_PICKER_REQUEST_CODE = 1;
    public AsrFragment() {
    }
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAsrBinding.inflate(inflater, container, false);
        init();  //初始化各种组件以及变量
        setRecording();
        setPlayMusic();
        setASR();
        copyFun();
        setFileUpload();
        return root;
    }
    private static final int PERMISSIONS_REQUEST_CODE_2 = 123;
    private String selectedFilePath;
    private void setFileUpload(){
        buttonFileUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndOpenFilePicker();
            }
        });
    }
    private void checkPermissionsAndOpenFilePicker() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), permissions, PERMISSIONS_REQUEST_CODE_2);
        } else {
            openFilePicker();
        }
    }
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*"); // Set the MIME type to audio/wav for WAV files
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE);
    }
    private String getFileExtension(String filePath) {
        if (filePath != null && filePath.lastIndexOf(".") != -1) {
            return filePath.substring(filePath.lastIndexOf("."));
        }
        return null;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(getActivity(), "需要文件读取权限才能选择音频文件", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            selectedFilePath = getPathFromUri(selectedFileUri);
            if (selectedFilePath != null) {
                Log.d("路径",selectedFilePath);
                String extension = getFileExtension(selectedFilePath);
                if(extension==null||!extension.equalsIgnoreCase(".wav")) {
                    Log.d("音频格式",extension);
                    Toast.makeText(getActivity(), "音频格式错误，需要WAV格式", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getActivity(), "上传成功！", Toast.LENGTH_SHORT).show();
                playAudioFromTempFile(selectedFilePath);
                currentAudioFilePath = selectedFilePath;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    //根据获取的Uri得到具体的路径
    private String getPathFromUri(Uri uri) {
        String filePath = null;
        String wholeID = DocumentsContract.getDocumentId(uri);
        String id = wholeID.split(":")[1];

        String[] projection = {MediaStore.Audio.Media.DATA};
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = {id};

        Cursor cursor = getActivity().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }

        return filePath;
    }
    private void copyFun(){
        buttonCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = multiLineEditText.getText().toString();
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
    private byte[] getFileContent(String filename) throws DemoException, IOException {
        File file = new File(filename);
        if (!file.canRead()) {
            System.err.println("文件不存在或者不可读: " + file.getAbsolutePath());
            throw new DemoException("file cannot read: " + file.getAbsolutePath());
        }
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] b = new byte[1024];
            // 定义一个输出流存储接收到的数据
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 开始接收数据
            int len = 0;
            while (true) {
                len = is.read(b);
                if (len == -1) {
                    // 数据读完
                    break;
                }
                byteArrayOutputStream.write(b, 0, len);
            }
            return byteArrayOutputStream.toByteArray();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    private void setASR(){
        buttonAsr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentAudioFilePath==null){
                    return;
                }
                byte[] content = new byte[0];
                try {
                    content = getFileContent(currentAudioFilePath);
                } catch (DemoException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String speech = base64Encode(content);
                String len = String.valueOf(content.length);
                realASR(speech,len);
            }
        });
    }
    private String base64Encode(byte[] content) {
        Base64.Encoder encoder = Base64.getEncoder(); // JDK 1.8  推荐方法
        return encoder.encodeToString(content);
    }
    private void realASR(String speech , String len){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 创建 URL 对象
                    URL url = new URL("https://vop.baidu.com/server_api");
                    // 打开连接
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    // 设置请求方法为 POST
                    connection.setRequestMethod("POST");
                    // 设置请求头
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    // 启用输入输出流
                    connection.setDoOutput(true);
                    // 构建请求体
                    String requestBody =  "{\"format\":\"wav\",\"rate\":16000,\"channel\":1,\"cuid\":\"Y3hddfwzu3wUuPTua3OQG03uxyTVmA2C\",\"token\":\"24.143e3a0892db4f950cd0aab9421c7446.2592000.1696401475.282335-38570675\",\"speech\":\""+speech+"\",\"len\":"+len+"}";
                    // 将请求体写入连接
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    // 获取响应代码
                    int responseCode = connection.getResponseCode();
                    // 读取响应数据
                    StringBuilder response = new StringBuilder();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream in = connection.getInputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            response.append(new String(buffer, 0, bytesRead));
                        }
                        in.close();
                    } else {
                        Log.e("HTTP Post Request", "Error response code: " + responseCode);
                    }
                    // 断开连接
                    connection.disconnect();
                    // 在这里处理响应结果
                    if (response != null) {
                        Log.d("HTTP Post Response", response.toString());
                        // 在子线程中更新 UI，例如显示结果或通知用户请求完成
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String jsonString = response.toString();
                                try {
                                    // 将 JSON 字符串解析为 JSON 对象
                                    JSONObject jsonObject = new JSONObject(jsonString);
                                    // 从 JSON 对象中获取 result 字段的值（result 是一个 JSON 数组）
                                    JSONArray resultArray = jsonObject.getJSONArray("result");
                                    // 如果 result 是一个数组，您可以遍历它，或者只获取第一个元素
                                    if (resultArray.length() > 0) {
                                        String resultValue = resultArray.getString(0);
                                        // 在这里使用 resultValue，它包含了 "北京科技馆，"
                                        Log.d("Result", resultValue);
                                        multiLineEditText.setText(resultValue);
                                        Toast.makeText(requireContext(), "识别成功！", Toast.LENGTH_SHORT ).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                // 在这里更新 UI，例如显示结果或通知用户请求完成
                            }
                        });
                    } else {
                        Log.e("HTTP Post Request", "Failed to make the request.");
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "识别出错！", Toast.LENGTH_SHORT ).show();
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void setPlayMusic(){
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer==null){
                    return;
                }
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                }
                else {
                    mediaPlayer.start();
                }
            }
        });
    }
    private String currentAudioFilePath;
    private CountDownTimer countDownTimer;
    private static final long RECORDING_DURATION_MS = 60000; // 60秒
    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(RECORDING_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 更新剩余时间
                long secondsRemaining = millisUntilFinished / 1000;
                recordingTimeTextView.setText(String.valueOf(60-secondsRemaining));
            }
            @Override
            public void onFinish() {
                // 录音时间已达到，停止录音
                stopRecordAudio();
                hideRecordingIndicator();
            }
        };
        countDownTimer.start();
    }
    private void stopCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        recordingTimeTextView.setText("0"); // 倒计时结束后，将TextView重置为60秒
    }
    private void setRecording() {
        microphoneButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    startRecordAudio();
                    startRecordAudio_Play();
                    showRecordingIndicator();
                    startCountdownTimer();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    stopRecordAudio();
                    stopRecordAudio_Play();
                    hideRecordingIndicator();
                    stopCountdownTimer();
                }
                return true;
            }
        });

    }
    // 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
    public static final int SAMPLE_RATE_INHZ = 16000;

    // 声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANNEL_CONFIG_Play = AudioFormat.CHANNEL_IN_STEREO;
    // 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord,audioRecord_Play;
    private Thread recordingAudioThread,recordingAudioThread_Play;
    private boolean isRecording = false , isRecording_Play = false;//mark if is recording
    protected void startRecordAudio_Play() {
        String audioCacheFilePath = getActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + "jerboa_audio_cache_play.pcm";
        try{
            // 获取最小录音缓存大小，
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG_Play, AUDIO_FORMAT);
            this.audioRecord_Play = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ, CHANNEL_CONFIG_Play, AUDIO_FORMAT, minBufferSize);
            // 开始录音
            this.isRecording_Play = true;
            audioRecord_Play.startRecording();

            // 创建数据流，将缓存导入数据流
            this.recordingAudioThread_Play = new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(audioCacheFilePath);
                    Log.i("录音", "audio cache pcm file path:" + audioCacheFilePath);
                    //  以防万一，看一下这个文件是不是存在，如果存在的话，先删除掉
                    if (file.exists()) {
                        file.delete();
                    }
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e("录音", "临时缓存文件未找到");
                    }
                    if (fos == null) {
                        return;
                    }
                    byte[] data = new byte[minBufferSize];
                    int read;
                    if (fos != null) {
                        while (isRecording_Play && !recordingAudioThread_Play.isInterrupted()) {
                            read = audioRecord_Play.read(data, 0, minBufferSize);
                            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                try {
                                    fos.write(data);
                                    Log.i("audioRecordTest", "写录音数据->" + read);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    try {
                        // 关闭数据流
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            this.recordingAudioThread_Play.start();
        }
        catch(IllegalStateException | SecurityException e){
            Log.w("录音","需要获取录音权限！");
            this.checkIfNeedRequestRunningPermission();
        }
        Log.d("外放路径",audioCacheFilePath);
        currentAudioFilePath_Play = audioCacheFilePath;
    }
    String currentAudioFilePath_Play = null;
    protected void stopRecordAudio_Play(){
        try {
            this.isRecording_Play = false;
            if (this.audioRecord_Play != null) {
                this.audioRecord_Play.stop();
                this.audioRecord_Play.release();
                this.audioRecord_Play = null;
                this.recordingAudioThread_Play.interrupt();
                this.recordingAudioThread_Play = null;
                String wav = getActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + "audio_cache_play.wav";
                PcmToWavUtil util = new PcmToWavUtil(CHANNEL_CONFIG_Play);
                util.pcmToWav(currentAudioFilePath_Play,wav);
                playAudioFromTempFile(wav);
            }
        }
        catch (Exception e){
            Log.w("录音",e.getLocalizedMessage());
        }
    }
    protected void startRecordAudio() {
        String audioCacheFilePath = getActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + "jerboa_audio_cache.pcm";
        try{
            // 获取最小录音缓存大小，
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);
            this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
            // 开始录音
            this.isRecording = true;
            audioRecord.startRecording();

            // 创建数据流，将缓存导入数据流
            this.recordingAudioThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(audioCacheFilePath);
                    Log.i("录音", "audio cache pcm file path:" + audioCacheFilePath);
                    /*
                     *  以防万一，看一下这个文件是不是存在，如果存在的话，先删除掉
                     */
                    if (file.exists()) {
                        file.delete();
                    }
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e("录音", "临时缓存文件未找到");
                    }
                    if (fos == null) {
                        return;
                    }
                    byte[] data = new byte[minBufferSize];
                    int read;
                    if (fos != null) {
                        while (isRecording && !recordingAudioThread.isInterrupted()) {
                            read = audioRecord.read(data, 0, minBufferSize);
                            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                try {
                                    fos.write(data);
                                    Log.i("audioRecordTest", "写录音数据->" + read);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    try {
                        // 关闭数据流
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            this.recordingAudioThread.start();
        }
        catch(IllegalStateException | SecurityException e){
            Log.w("录音","需要获取录音权限！");
            this.checkIfNeedRequestRunningPermission();
        }
        Log.d("传输路径",audioCacheFilePath);
        currentAudioFilePath = audioCacheFilePath;
    }
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    protected void checkIfNeedRequestRunningPermission(){
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_CODE);
        }
    }
    protected void stopRecordAudio(){
        try {
            this.isRecording = false;
            if (this.audioRecord != null) {
                this.audioRecord.stop();
                this.audioRecord.release();
                this.audioRecord = null;
                this.recordingAudioThread.interrupt();
                this.recordingAudioThread = null;
                String wav = getActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + "audio_cache.wav";
                PcmToWavUtil util = new PcmToWavUtil();
                util.pcmToWav(currentAudioFilePath,wav);
                currentAudioFilePath = wav;
//                playAudioFromTempFile(wav);
            }
        }
        catch (Exception e){
            Log.w("录音",e.getLocalizedMessage());
        }
    }
    private void playAudioFromTempFile(String audioFilePath) {
        Log.d("播放路径",audioFilePath);
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showRecordingIndicator() {
        recordingIndicator.setVisibility(View.VISIBLE);
        recordingTimeTextView.setVisibility(View.VISIBLE); // 设置为可见
    }
    private void hideRecordingIndicator() {
        recordingIndicator.setVisibility(View.INVISIBLE);
        recordingTimeTextView.setVisibility(View.INVISIBLE); // 设置为不可见
    }
    private void init(){
        root = binding.getRoot();
        microphoneButton = root.findViewById(R.id.microphoneButton);
        recordingIndicator = root.findViewById(R.id.recordingIndicator);
        buttonPlay = root.findViewById(R.id.buttonPlayMusic);
        recordingTimeTextView = root.findViewById(R.id. recordingTimeTextView);
        buttonAsr = root.findViewById(R.id.buttonAsr);
        buttonCopy = root.findViewById(R.id.buttonCopy);
        multiLineEditText = root.findViewById(R.id.multiLineEditText);
        buttonFileUpload = root.findViewById(R.id.buttonClear);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}