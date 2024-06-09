package com.example.termp_new.openAi;

import android.app.Activity;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TextClassify {

    static String result="";

    public static void WriteTextFile(Activity activity, TextView textView, String contents,OpenAiCallback callback) throws IOException, InterruptedException {
        //textView= textView.findViewById(R.id.textView);;
        String question=contents+" 이 내용을 3줄 내외로 요약 해 주세요. 존댓말을 사용해 주세요.";

        Gpt.getResponse(question, new VolleyCallback() {
            @Override
            public void onSuccess(String result_this) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(result_this);
                        result=result_this;
                        callback.onResponseReceived(result);
                    }
                });
            }

            @Override
            public void onError(String error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(error);
                        callback.onError(error);
                    }
                });
            }
        });


    }


    static void process_text(Activity activity, TextView textView, FirebaseVisionText firebaseVisionText) throws IOException, InterruptedException {
        List<FirebaseVisionText.TextBlock> blocks=firebaseVisionText.getTextBlocks();
        if(blocks.size()==0){
            Toast.makeText(activity,"텍스트가 인식되지 않았습니다.",Toast.LENGTH_LONG).show();
        }
        else{
            for(FirebaseVisionText.TextBlock block:firebaseVisionText.getTextBlocks()){
                String text=block.getText();
                textView.setText(text);
            }
        }
    }
    public static void process_text_Gpt(Activity activity, TextView textView) throws IOException, InterruptedException {
        String text=textView.getText().toString();
        if(text.length()==0){
            Toast.makeText(activity,"텍스트가 인식되지 않았습니다.",Toast.LENGTH_LONG).show();
        }
        else{
            WriteTextFile(activity,textView,text,new OpenAiCallback(){
                public void onResponseReceived(String result) {
                    // Write to file after receiving the OpenAI response
                    textView.setText(result);
                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }
}
