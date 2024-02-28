package com.example.finaltask.ui.tts;

import android.content.ContentValues;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.finaltask.R;
import com.example.finaltask.databinding.FragmentTtsBinding;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TtsFragment extends Fragment {
    private FragmentTtsBinding binding;
    private View root;
    private SeekBar seekBarSpeed,seekBarShow;
    private TextView textSpeedChange,multiLineEditText,textMusic;
    private Spinner spinnerVoice;
    private Button buttonClear , buttonTTS ,buttonPlay ,buttonDonload;
    private boolean musicSet;
    private byte[] musicFile;
    private MediaPlayer mediaPlayer ;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTtsBinding.inflate(inflater, container, false);
        init();
        setSeekBarSpeedChange();
        setSpinnerVoiceData();
        setClear();
        setTts();
        setPlayOrPause();
        setDonloadMusic();
        return root;
    }
    private void setDonloadMusic(){
        buttonDonload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(musicFile==null){
                    Toast.makeText(requireContext(), "现在没有语音文件！", Toast.LENGTH_SHORT ).show();
                    return;
                }
                String filename = "语音合成"+UUID.randomUUID()+".mp3"; // 文件名
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename); // 文件名
                values.put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg"); // MIME类型，根据实际情况设置
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS); // 文件保存的相对路径

                Uri uri = getActivity().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                try {
                    OutputStream os = getActivity().getContentResolver().openOutputStream(uri);
                    os.write(musicFile); // 写入文件内容
                    os.close();
                    // 文件保存成功
                    Toast.makeText(requireContext(), "文件保存成功！", Toast.LENGTH_SHORT ).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    // 处理文件保存失败的情况
                    Toast.makeText(requireContext(), "文件保存失败！", Toast.LENGTH_SHORT ).show();
                }


//                    Toast.makeText(requireContext(), "文件保存成功！", Toast.LENGTH_SHORT ).show();
//                    Toast.makeText(requireContext(), "文件保存失败！", Toast.LENGTH_SHORT ).show();

            }
        });
    }
    private void setSeekBarSpeedChange(){
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 步长为1，直接显示当前进度到TextView中
                textSpeedChange.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 用户开始拖动SeekBar时的回调
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 用户停止拖动SeekBar时的回调
            }
        });
    }
    private void setSpinnerVoiceData(){
        List<String> voiceList = new ArrayList<>();
        voiceList.add("度小美");  //0
        voiceList.add("度小宇");  //1
        voiceList.add("度丫丫");  //4
        voiceList.add("度逍遥");  //3
        voiceList.add("度小鹿");  //5118
        voiceList.add("度博文");  //106
        voiceList.add("度小童");  //110
        voiceList.add("度小萌");  //111
        voiceList.add("度米朵");  //103
        voiceList.add("度小娇");  //5
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.simple_spinner_item,R.id.spinner_item_text,voiceList);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinnerVoice.setAdapter(adapter);
    }
    private void  setClear(){
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                multiLineEditText.setText("");
            }
        });
    }
    private void setTts(){
        buttonTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String spd = String.valueOf(seekBarSpeed.getProgress()); //语速
                int voicePer = (int) spinnerVoice.getSelectedItemId();
                int per = 0;
                switch (voicePer){
                    case 1:per =1;break;
                    case 2:per =4;break;
                    case 3:per =3;break;
                    case 4:per =5118;break;
                    case 5:per =106;break;
                    case 6:per =110;break;
                    case 7:per =111;break;
                    case 8:per =103;break;
                    case 9:per =5;break;
                }
                String text = multiLineEditText.getText().toString();
                if(text.length()>59){
                    Toast.makeText(requireContext(), "字数不能超过59个！", Toast.LENGTH_SHORT ).show();
                    return;
                }
                ttsFun(text,spd,String.valueOf(per));
            }
        });
    }
    private void ttsFun(String text , String speed , String perCode) {
        // 执行后台任务
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 后台任务代码，例如网络请求等
                    String tex = text;
                    String cuid = "ycMTXmZpd2Ml7yZCcItIZyVcpdXb52PQ";
                    String lan = "zh";  //中文
                    String spd = speed;  //音速
                    String pit = "5";  //音调
                    String vol = "5";  //音量
                    String per = perCode;  //音源
                    String aue = "3";  //格式，3为mp3，6为wav
                    String token = "24.ae23c6fdfaf845b6776a3cd555c47887.2592000.1696164391.282335-38570675";  //密钥
                    String url = "https://tsn.baidu.com/text2audio";
                    String query = "tex=" + URLEncoder.encode(tex, "UTF-8") + "&tok=" + token + "&cuid=" + cuid + "&ctp=1&lan=" + lan
                            + "&spd=" + spd + "&pit=" + pit + "&vol=" + vol + "&per=" + per + "&aue=" + aue;
                    HttpURLConnection connection = null;
                    try {
                        URL apiUrl = new URL(url);
                        connection = (HttpURLConnection) apiUrl.openConnection();
                        // 设置请求方法为POST
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        // 设置请求头
                        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        connection.setRequestProperty("Accept", "*/*");
                        // 写入请求体
                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = query.getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            try (InputStream is = connection.getInputStream()) {
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                                }
                                byte[] audioData = byteArrayOutputStream.toByteArray();
                                // 创建一个临时文件保存音频数据
                                File tempAudioFile = File.createTempFile("temp_audio", ".mp3", getActivity().getCacheDir());
                                FileOutputStream fos = new FileOutputStream(tempAudioFile);
                                fos.write(audioData);
                                fos.close();
                                musicFile = audioData;
                                music(tempAudioFile);   //执行音乐监听时间等
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.err.println("HTTP请求失败，响应码：" + responseCode);
                            Toast.makeText(requireContext(), "语音合成成功！", Toast.LENGTH_SHORT ).show();
                        }

                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "语音合成失败！", Toast.LENGTH_SHORT ).show();
                }
            }
        }).start();
    }
    private void music(File tempAudioFile) throws IOException {
        // 设置 MediaPlayer 的监听器，用于更新播放进度
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // 获取音频的总时长
                long duration = mediaPlayer.getDuration();
                // 更新SeekBar的最大值为音频的总时长
                seekBarShow.setMax((int) duration);
                Toast.makeText(requireContext(), "语音合成成功！", Toast.LENGTH_SHORT ).show();
                // 设置SeekBar的ChangeListener，用于拖动SeekBar时控制播放进度
                seekBarShow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            // 用户拖动SeekBar时，将MediaPlayer的播放进度更新到拖动位置
                            mediaPlayer.seekTo(progress);
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // 用户开始拖动SeekBar时，暂停MediaPlayer
                        mediaPlayer.pause();
                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // 用户停止拖动SeekBar时，继续播放MediaPlayer
                        mediaPlayer.start();
                    }
                });
                // 启动一个定时器，用于更新SeekBar和TextView
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        // 获取当前的播放位置
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        // 更新SeekBar的进度
                        seekBarShow.setProgress(currentPosition);
                        // 在UI线程中更新TextView
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 将播放时间格式化为分钟:秒的形式，更新到TextView
                                String formattedTime = String.format("%02d:%02d",
                                        TimeUnit.MILLISECONDS.toMinutes(currentPosition),
                                        TimeUnit.MILLISECONDS.toSeconds(currentPosition) -
                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentPosition)));
                                textMusic.setText(formattedTime);
                            }
                        });
                    }
                }, 0, 1000); // 每隔1秒更新一次
                // 开始播放音频
                mediaPlayer.start();
            }
        });
        mediaPlayer.reset();
        // 设置 MediaPlayer 的数据源
        mediaPlayer.setDataSource(tempAudioFile.getAbsolutePath());
        musicSet = true;
        // 异步准备 MediaPlayer
        mediaPlayer.prepareAsync();
    }
    private void setPlayOrPause(){
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!musicSet){
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
    private void init(){
        root = binding.getRoot();
        seekBarSpeed = root.findViewById(R.id.seekBarSpeed);
        textSpeedChange = root.findViewById(R.id.speedChange);
        spinnerVoice = root.findViewById(R.id.spinnerVoice);
        multiLineEditText= root.findViewById(R.id.multiLineEditText);
        buttonClear = root.findViewById(R.id.buttonClear);
        buttonTTS = root.findViewById(R.id.buttonTTS);
        seekBarShow = root.findViewById(R.id.seekBarShow);
        textMusic = root.findViewById(R.id.textMusic);
        musicFile=  null;
        mediaPlayer = new MediaPlayer();
        buttonPlay = root.findViewById(R.id.buttonPlay);
        musicSet = false;
        buttonDonload = root.findViewById(R.id.buttonDonload);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}
