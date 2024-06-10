package com.example.termp_new.openAi;

import static java.util.Collections.emptyList;

import android.graphics.Bitmap;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.termp_new.ResultActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

public class Image_to_text {
    public static void Write_Text(Bitmap bitmap, ResultActivity activity){
        if(bitmap==null){
            Toast.makeText(MyApplication.ApplicationContext(),"올바른 그림 형식이 아닙니다.",Toast.LENGTH_LONG).show();
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
                                    activity.setResultText(firebaseVisionText);
                                }
                            });
        }
    }
}


