package userInterface.fragments.homepage.promoSection;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import dataRepo.promo.PromoAdInfo;
import dataRepo.promo.PromoAdsCloud;
import dataRepo.user.AppUser;
import dataRepo.user.AppUserRepo;
import userInterface.main.MainViewModel;

public class PromoAdsProvider {
    private final LoggerUtils logger = LoggerUtils.from(MainViewModel.class);
    private final MutableLiveData<List<PromoAdInfo>> promoAdsLiveData = new MutableLiveData<>();
    private boolean promoLoadStarted = false;

    public LiveData<List<PromoAdInfo>> getPromoHeaderAdsLiveData() {
        List<PromoAdInfo> current = promoAdsLiveData.getValue();
        if ((current == null || current.isEmpty()) && !promoLoadStarted) {
            promoLoadStarted = true;
            loadPromoAdsFromServer();
        }
        return promoAdsLiveData;
    }

    private void loadPromoAdsFromServer() {
        ThreadTask<List<PromoAdInfo>, List<PromoAdInfo>> loadAdsTask;
        loadAdsTask = new ThreadTask<>();
        loadAdsTask.setMaxExecutionTimeMs(5_000);
        loadAdsTask.setBackgroundTask(progressCallback -> {
            try {
                AppUser user = AppUserRepo.getUser();
                if (user == null) return new ArrayList<>();
                String deviceId = user.userDeviceId;
                if (deviceId == null || deviceId.isEmpty()) {
                    return new ArrayList<>();
                }

                PromoAdsCloud cloud = new PromoAdsCloud();
                return cloud.getPromoAds(deviceId);
            } catch (Exception error) {
                logger.error("Failed to load promo ads", error);
                return new ArrayList<>();
            }
        });

        loadAdsTask.setResultTask(result -> {
            promoAdsLiveData.setValue(result);
            promoLoadStarted = false;
        });

        loadAdsTask.setErrorTask(error -> {
            promoAdsLiveData.setValue(new ArrayList<>());
            promoLoadStarted = false;
        });

        loadAdsTask.start();
    }

    public void refreshPromoHeaderAds() {
        promoLoadStarted = false;
        loadPromoAdsFromServer();
    }

    public List<String> getPromoHeaderAdImages() {
        List<String> imageUrls = new ArrayList<>();
        List<PromoAdInfo> ads = promoAdsLiveData.getValue();
        if (ads == null) return imageUrls;
        for (PromoAdInfo ad : ads) {
            if (ad != null &&
                    ad.imageUrl != null &&
                    !ad.imageUrl.isEmpty()) {
                imageUrls.add(ad.imageUrl);
            }
        }

        return imageUrls;
    }

    public List<PromoAdInfo> getLastPromoHeaderAds() {
        List<PromoAdInfo> list = promoAdsLiveData.getValue();
        return list != null ? list : new ArrayList<>();
    }
}
