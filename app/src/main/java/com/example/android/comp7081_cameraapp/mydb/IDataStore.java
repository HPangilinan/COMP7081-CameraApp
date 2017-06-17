package com.example.android.comp7081_cameraapp.mydb;

/**
 * Created by HPangilinan on 24/05/2017.
 */

public interface IDataStore {
    void saveState(String state);
    String getState();
}
