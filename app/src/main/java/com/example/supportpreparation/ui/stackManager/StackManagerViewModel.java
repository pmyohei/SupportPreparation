package com.example.supportpreparation.ui.stackManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class StackManagerViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public StackManagerViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}