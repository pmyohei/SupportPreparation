package com.example.supportpreparation.ui.groupManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GroupManagerViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public GroupManagerViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is notifications fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}