package coreUtils.library.process;

import static coreUtils.library.strings.StringHelper.removeDuplicateSlashes;

import com.nextgen.R;

import java.io.File;

import coreUtils.library.storage.FileStorageUtility;
import coreUtils.library.strings.StringHelper;

public final class AppDirsValidator {
    private static final LoggerUtils logger = LoggerUtils.from(AppDirsValidator.class);
    private static final ThreadTask<Boolean, Boolean> validatorTask = new ThreadTask<>();

    private AppDirsValidator() {}

    public static void performValidation() {
        validatorTask.setBackgroundTask(progressCallback -> {
            logger.debug("Performing app dirs validation");

            File publicDownloadFolder = FileStorageUtility.getPublicDownloadFolder();
            String storageRoot = publicDownloadFolder.getAbsolutePath();
            logger.debug("Getting device public download path: " + storageRoot);

            if (storageRoot.isEmpty()) {
                logger.error("Failed to get device public download path");
                return false;
            }

            String subfolderName = StringHelper.getText(R.string.title_default_app_folder);
            String rootAppDirPath = removeDuplicateSlashes(storageRoot + "/" + subfolderName);

            File rootAppDir = new File(rootAppDirPath);
            boolean rootValid;

            if (rootAppDir.exists()) {
                rootValid = rootAppDir.isDirectory();
                logger.debug("Root folder already exists");

            } else {
                rootValid = rootAppDir.mkdirs();
                logger.debug("Root folder created: " + rootValid);
            }

            if (!rootValid) {
                logger.error("Failed to create root app directory");
                return false;
            }

            String[] subFolders = {
                    StringHelper.getText(R.string.title_tubeaio_archives),
                    StringHelper.getText(R.string.title_tubeaio_sounds),
                    StringHelper.getText(R.string.title_tubeaio_videos),
                    StringHelper.getText(R.string.title_tubeaio_images),
                    StringHelper.getText(R.string.title_tubeaio_programs),
                    StringHelper.getText(R.string.title_tubeaio_documents),
                    StringHelper.getText(R.string.title_tubeaio_others)
            };

            boolean allSubFoldersValid = true;
            for (String folderName : subFolders) {
                File subDir = new File(rootAppDir, folderName);
                boolean valid;

                if (subDir.exists()) {
                    valid = subDir.isDirectory();
                    logger.debug("Subfolder already exists: " + folderName);

                } else {
                    valid = subDir.mkdirs();
                    logger.debug("Created subfolder: " + folderName + " = " + valid);
                }

                if (!valid) {
                    logger.error("Failed to validate subfolder: " + folderName);
                    allSubFoldersValid = false;
                }
            }

            return allSubFoldersValid;
        });

        validatorTask.setResultTask(result -> {
            logger.debug("App dirs validation result: " + result);
        });

        validatorTask.start();
    }
}
