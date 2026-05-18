package userInterface.opening;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nextgen.R;

import dataRepo.configs.AppConfig;
import dataRepo.configs.AppConfigsRepo;

/**
 * The entry point activity for the application.
 * <p>
 * This class handles the initial routing logic upon app startup. It checks the
 * {@link AppConfig} to determine if the application encountered a crash in the
 * previous session. If a crash is detected, it prepares to display a feedback
 * mechanism; otherwise, it navigates the user to the {@link OpeningActivity}
 * with a fade transition.
 */
public class LauncherActivity extends AppCompatActivity {
	
	/**
	 * Initializes the activity and determines the application's entry flow.
	 * <p>
	 * This method checks the persistent application configuration to see if the app
	 * crashed during its last session. If a crash is detected, it prepares to
	 * show a feedback screen. Otherwise, it navigates the user to {@link OpeningActivity}
	 * using a fade transition and clears the activity stack.
	 * </p>
	 *
	 */
	@Override public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AppConfig appConfig = AppConfigsRepo.getConfig();
		if (appConfig.hasAppCrashedRecently) {
			appConfig.hasAppCrashedRecently = false;
			appConfig.save();
			//todo: open app crashed activity.
		} else {
			Intent intent = new Intent(this, OpeningActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			overridePendingTransition(R.anim.anim_fade_enter, R.anim.anim_fade_exit);
			startActivity(intent);
			finish();
		}
	}
}
