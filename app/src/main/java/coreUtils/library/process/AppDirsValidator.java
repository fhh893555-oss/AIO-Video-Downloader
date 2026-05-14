package coreUtils.library.process;

import static coreUtils.library.strings.StringHelper.removeDuplicateSlashes;

import java.io.File;

import coreUtils.base.StaticAppInfo;
import coreUtils.library.storage.FileStorageUtility;

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

            String subfolderName = StaticAppInfo.APP_DOWNLOAD_FOLDER_NAME;
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
                    StaticAppInfo.APP_DOWNLOAD_ARCHIVE_FOLDER,
                    StaticAppInfo.APP_DOWNLOAD_SOUND_FOLDER,
                    StaticAppInfo.APP_DOWNLOAD_VIDEO_FOLDER,
                    StaticAppInfo.APP_DOWNLOAD_IMAGES_FOLDER,
                    StaticAppInfo.APP_DOWNLOAD_PROGRAM_FOLDER,
                    StaticAppInfo.APP_DOWNLOAD_DOCUMENTS_FOLDER,
                    StaticAppInfo.APP_DOWNLOAD_OTHERS_FOLDER
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
