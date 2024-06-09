package com.example.termp_new.openAi;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.termp_new.ResultActivity;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TextClassify {

    public static void WriteTextFile(ResultActivity activity, String contents,OpenAiCallback callback) {
        String question=contents+" 이 내용을 3줄 내외로 요약 해주세요. 존댓말을 사용해주세요";

        Gpt.getResponse(question, new VolleyCallback() {
            @Override
            public void onSuccess(String result_this) {
                activity.setResultGPT(result_this);
            }

            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(error);
                    }
                });
            }
        });
    }

    public static void process_text_Gpt(String text, ResultActivity activity) throws IOException, InterruptedException {
        if(text.isEmpty()){
            Toast.makeText(MyApplication.ApplicationContext(),"텍스트가 인식되지 않았습니다.",Toast.LENGTH_LONG).show();
        }
        else{
            WriteTextFile(activity,text,new OpenAiCallback(){
                public void onResponseReceived(String result) {

                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }
}
