package userInterface.appUpdater;

import android.content.Intent;
import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityUpdater1Binding;

import java.io.Serializable;

import javax.annotation.Nullable;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.openingSplash.OpeningActivity;

public class AppUpdaterActivity extends BaseActivity<ActivityUpdater1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String KEY_INTENT_RECEIVED_KEY = "KEY_INTENT_RECEIVED_KEY";
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityUpdater1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityUpdater1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
	
	}
	
	/**
	 * Extracts the UpdateInfo object from the activity's intent extras.
	 * <p>
	 * This method retrieves the serialized UpdateInfo that was passed to this activity
	 * via the intent's extra bundle using {@link #KEY_INTENT_RECEIVED_KEY} as the key.
	 * It performs type safety checks to ensure the extracted object is properly
	 * typed as an UpdateInfo instance before returning it.
	 * </p>
	 *
	 * <p><b>Usage Context:</b>
	 * Typically called when this activity is launched from {@link OpeningActivity}
	 * after an update is detected. The UpdateInfo contains version details and the
	 * APK download URL for the available update.
	 * </p>
	 *
	 * @return the UpdateInfo object extracted from the intent, or {@code null} if
	 *         the intent contains no serializable extra under the expected key,
	 *         or if the extra is not an instance of UpdateInfo
	 */
	private @Nullable UpdateInfo getUpdateInfoPackageFromIntent() {
		Intent intent = getIntent();
		Serializable packageExtra =
			intent.getSerializableExtra(KEY_INTENT_RECEIVED_KEY);
		
		if (packageExtra != null) {
			if (!(packageExtra instanceof UpdateInfo)) {return null;}
			return (UpdateInfo) packageExtra;
		} else {
			return null;
		}
	}
}
