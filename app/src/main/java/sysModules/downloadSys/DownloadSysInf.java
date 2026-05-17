package sysModules.downloadSys;

import dataRepo.downloads.DownloadInfo;
import io.objectbox.query.LazyList;

public interface DownloadSysInf {
    boolean isDownloadSystemInitialize();
    LazyList<DownloadInfo> getAllDownloadRecordsFromRepo();
    LazyList<DownloadInfo> getAllFinishedDownloadRecordsFromRepo();
}
