package com.example.termp_new.openAi;


import static com.example.termp_new.openAi.TextClassify.process_text;

import android.app.Activity;
import android.graphics.Bitmap;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;

public class Image_to_text {
    public static void Write_Text(Bitmap bitmap,Activity activity, TextView textView){
        if(bitmap==null){
            Toast.makeText(MyApplication.ApplicationContext(),"Bitmap is null",Toast.LENGTH_LONG).show();
        }
        else{
            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            Task<FirebaseVisionText> result =
                    detector.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    try {
                                        process_text(activity, textView, firebaseVisionText);
                                    } catch (IOException | InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
        }
    }
}
