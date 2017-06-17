package com.example.android.comp7081_cameraapp;

import com.example.android.comp7081_cameraapp.mydb.DataStorageImp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void dataStorage() throws Exception {
        DataStorageImp db = new DataStorageImp();
        db.saveState("Testing");
        assertEquals("Testing!!!", db.getState());
    }
}

