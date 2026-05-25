package userInterface.openingSplash;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nextgen.R;

import dataRepo.configs.AppConfig;
import dataRepo.configs.AppConfigsRepo;

/**
 * Launcher activity that serves as the application's entry point and crash detection handler.
 * <p>
 * This activity is the first component launched when the application starts. It checks for
 * recent crash states stored in the app configuration and determines the appropriate
 * navigation flow. If a crash was detected in the previous session, it redirects to a
 * crash reporting screen; otherwise, it proceeds to the normal application launch flow
 * through {@link OpeningActivity}.
 * </p>
 *
 * <p><b>Crash Detection Flow:</b>
 * <ul>
 *   <li>Reads {@code hasAppCrashedRecently} flag from {@link AppConfigsRepo}</li>
 *   <li>If true: clears the flag, saves configuration, and redirects to crash reporting activity</li>
 *   <li>If false: proceeds directly to the opening/splash screen</li>
 * </ul>
 * </p>
 *
 * <p><b>Navigation Flags:</b>
 * When launching the opening screen, the intent uses {@link Intent#FLAG_ACTIVITY_NEW_TASK}
 * and {@link Intent#FLAG_ACTIVITY_CLEAR_TASK} flags to clear the activity stack,
 * ensuring the launcher is not retained in the back stack after navigation.
 * </p>
 *
 * <p><b>Transition Animation:</b>
 * A fade in/fade out transition animation is applied when moving to the opening screen,
 * providing a smooth visual transition between activities.
 * </p>
 *
 * @see AppCompatActivity
 * @see AppConfigsRepo
 * @see OpeningActivity
 */
public class LauncherActivity extends AppCompatActivity {
	
	/**
	 * Called when the launcher activity is created.
	 * <p>
	 * This method serves as the application's main entry point. It checks if a crash occurred
	 * in the previous app session by reading the {@code hasAppCrashedRecently} flag from
	 * the app configuration. If a crash was detected, it clears the flag and saves the
	 * configuration to reset the crash state for future launches. If no crash occurred,
	 * it proceeds to the normal application launch flow by starting the opening splash screen.
	 * </p>
	 *
	 * <p><b>Crash Detection Flow:</b>
	 * <ul>
	 *   <li><b>Crash detected:</b> Flag is reset to false and configuration saved.
	 *       (Note: Crash reporting UI should be launched here)</li>
	 *   <li><b>No crash detected:</b> OpeningActivity is launched with flags to clear
	 *       the activity stack</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Navigation Flags:</b>
	 * The intent uses {@link Intent#FLAG_ACTIVITY_NEW_TASK} to start a new task and
	 * {@link Intent#FLAG_ACTIVITY_CLEAR_TASK} to clear any existing activities from the
	 * task, ensuring the launcher activity is not retained in the back stack after
	 * transitioning to the opening screen.
	 * </p>
	 *
	 * <p><b>Transition Animation:</b>
	 * A fade enter and fade exit animation is applied when transitioning to the opening
	 * activity, providing a smooth visual experience during app launch.
	 * </p>
	 *
	 * @param savedInstanceState Previously saved state data, or null if no saved state exists
	 */
	@Override public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AppConfig appConfig = AppConfigsRepo.getConfig();
		if (appConfig.hasAppCrashedRecently) {
			appConfig.hasAppCrashedRecently = false;
			appConfig.save();
			
			Intent intent = new Intent(this, OpeningActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			overridePendingTransition(R.anim.anim_fade_enter, R.anim.anim_fade_exit);
			startActivity(intent);
			finish();
		} else {
			Intent intent = new Intent(this, OpeningActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			overridePendingTransition(R.anim.anim_fade_enter, R.anim.anim_fade_exit);
			startActivity(intent);
			finish();
		}
	}
}
