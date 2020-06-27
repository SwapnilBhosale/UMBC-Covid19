package edu.umbc.covid19.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import edu.umbc.covid19.MainActivity;
import edu.umbc.covid19.MainActivity1;
import edu.umbc.covid19.R;
import edu.umbc.covid19.PrefManager;
import edu.umbc.covid19.onboarding.OnboardingActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int DURATION = 3000;
    private static final int REQ_ONBOARDING = 123;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        prefManager = new PrefManager(this);
        new Handler().postDelayed(() -> {

            PrefManager prefManager = new PrefManager(getApplicationContext());
            boolean onboardingCompleted = prefManager.getIsOnboardingCompleted();
            if (onboardingCompleted) {
                showHomeActivity();
            } else {
                startActivityForResult(new Intent(this, OnboardingActivity.class), REQ_ONBOARDING);
            }

        }, DURATION);
    }

    @Override
    public void onBackPressed() {

    }

    private void showHomeActivity(){
        final Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ONBOARDING) {
            if (resultCode == RESULT_OK) {
                prefManager.setIsOnboardingCompleted(true);
                showHomeActivity();
            } else {
                finish();
            }
        }
    }
}