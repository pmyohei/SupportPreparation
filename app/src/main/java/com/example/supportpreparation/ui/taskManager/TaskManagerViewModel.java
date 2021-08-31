package com.example.supportpreparation.ui.taskManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TaskManagerViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public TaskManagerViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}