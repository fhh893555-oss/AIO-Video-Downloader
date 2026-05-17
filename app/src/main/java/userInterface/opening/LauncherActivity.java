package userInterface.opening;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nextgen.R;

import dataRepo.configs.AppConfig;
import dataRepo.configs.AppConfigsRepo;

public class LauncherActivity extends AppCompatActivity {

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppConfig appConfig = AppConfigsRepo.getConfig();
        if (appConfig.hasAppCrashedRecently) {
            //todo: open feedback screen.
            appConfig.hasAppCrashedRecently = false;
            appConfig.save();
        } else {
            Intent intent = new Intent(this, OpeningActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.anim_fade_enter, R.anim.anim_fade_exit);
            finish();
        }
    }
}
