package com.example.termp_new.openAi;

public interface OpenAiCallback {
    void onResponseReceived(String result);
    void onError(String error);
}
