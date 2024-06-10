package com.example.termp_new.openAi;


import android.widget.Toast;
import com.example.termp_new.ResultActivity;
import java.io.IOException;

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

    public static void process_text_Gpt(String text, ResultActivity activity){
        if(text.isEmpty()){
            Toast.makeText(activity,"텍스트를 요약할 수 없었습니다.",Toast.LENGTH_LONG).show();
            activity.setResultGPT("텍스트를 요약할 수 없었습니다.");
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
