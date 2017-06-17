package com.example.android.comp7081_cameraapp.mydb;

/**
 * Created by HPangilinan on 24/05/2017.
 */

public class DataStorageImp implements IDataStore {

    private String state = null;

    @Override
    public void saveState(String state) {
        this.state = state;

    }

    @Override
    public String getState() {
        return state;
    }
}
