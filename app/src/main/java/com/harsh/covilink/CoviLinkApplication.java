package com.harsh.covilink;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class CoviLinkApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
} 