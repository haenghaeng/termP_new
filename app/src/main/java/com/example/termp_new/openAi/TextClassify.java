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
    // 디렉토리 경로
    final static String foldername =

            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .getAbsolutePath() +"/File";
    // 파일 이름
    final static String filename = "file.txt";
    public static void WriteTextFile(Activity activity, TextView textView, String foldername, String filename, String contents,OpenAiCallback callback) throws IOException, InterruptedException {
        //textView= textView.findViewById(R.id.textView);;
        String question=contents+" 이 내용을 3줄 내외로 요약 해 주세요. 존댓말을 사용해 주세요.";

        Gpt.getResponse(question, new VolleyCallback() {
            @Override
            public void onSuccess(String result_this) {
                //textView.setText(result_this);
                //result=result_this;
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
            Toast.makeText(activity,"No text detected",Toast.LENGTH_LONG).show();
        }
        else{
            for(FirebaseVisionText.TextBlock block:firebaseVisionText.getTextBlocks()){
                String text=block.getText();
                //textView.setText(text);
                // 파일 작성시간 (현재시간으로)
                String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                // 파일 내용 생성
                String contents = "파일 내용 :\n"+text+" \n파일 작성 시간 : "+now+"\n";
                // 파일 없으면 생성 후 파일안에 내용 저장.
                WriteTextFile(activity,textView, foldername, filename, contents,new OpenAiCallback(){
                    public void onResponseReceived(String result) {
                        // Write to file after receiving the OpenAI response
                        try {
                            // foldername 경로의 파일 객체 생성 (디렉토리를 가리키고 있고, 해당 경로에 디렉토리가 없어도 File 객체 생성됨.)
                            File dir = new File (foldername);
                            if(!dir.exists()){
                                // 디렉토리 생성.
                                // 만들고자 하는 디렉토리의 상위 디렉토리가 존재하지 않을 경우, 생성 불가
                                dir.mkdir();
                            }
                            //디렉토리 폴더가 없으면 생성함

                            // 문자열을 바이트배열로 변환해서 파일에 저장한다.

                            // 생성한 FileOutputStream 객체를 통해 파일을 생성, 내용 작성한다.
                            // 기존 파일에 내용을 추가 할려면 두번째 인자로 true를 적어 준다. true를 추가해도 없으면 만든다.
                            FileOutputStream fos = new FileOutputStream(foldername+"/"+filename);
                            // 문자열을 바이트배열로 변환해서 파일에 저장한다.
                            fos.write((contents+"\n\n").getBytes());
                            fos.write(("summarized text:\n\n"+result).getBytes());
                            // 파일 닫기.
                            fos.close();
                            result="";
                            Toast.makeText(activity,"txt파일이 생성되었습니다.",Toast.LENGTH_LONG).show();
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(String error) {

                    }
                });
            }
        }
    }
}
