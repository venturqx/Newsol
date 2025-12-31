package org.schabi.newpipe.fragments.list.kiosk;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.R;
import org.schabi.newpipe.error.ErrorInfo;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.util.KioskTranslator;
import org.schabi.newpipe.util.ServiceHelper;
import org.schabi.newpipe.util.TournesolHelper;

import java.util.List;

public class DefaultKioskFragment extends KioskFragment {

    @Nullable
    private TournesolFilterController tournesolFilterController;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (serviceId < 0) {
            updateSelectedDefaultKiosk();
        }
    }

    @Override
    protected void initViews(final View rootView, final Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        if ("Tournesol".equals(kioskId)) {
            ensureTournesolController(rootView);
            applyTournesolFilters(tournesolFilterController.getCurrentLanguages(),
                    tournesolFilterController.getCurrentDateKey(), false);
        } else {
            hideTournesolHeader(rootView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean shouldReload = false;
        final android.content.SharedPreferences prefs =
                androidx.preference.PreferenceManager
                        .getDefaultSharedPreferences(requireContext());
        final String recommendationAlgorithmKey = getString(R.string.recommendation_algorithm_key);
        final String currentRecommendationAlgorithm =
                prefs.getString(recommendationAlgorithmKey, "youtube");

        if (serviceId != ServiceHelper.getSelectedServiceId(requireContext())
                || !currentRecommendationAlgorithm.equals(lastRecommendationAlgorithm)) {
            updateSelectedDefaultKiosk();
            currentInfo = null;
            if (currentWorker != null) {
                currentWorker.dispose();
            }
            shouldReload = true;
        }

        final View rootView = getView();
        if ("Tournesol".equals(kioskId)) {
            if (rootView != null) {
                ensureTournesolController(rootView);
                applyTournesolFilters(tournesolFilterController.getCurrentLanguages(),
                        tournesolFilterController.getCurrentDateKey(), shouldReload);
            }
            if (tournesolFilterController != null) {
                tournesolFilterController.onResume();
            }
        } else {
            if (tournesolFilterController != null) {
                tournesolFilterController.onDestroyView();
                tournesolFilterController = null;
            }
            if (rootView != null) {
                hideTournesolHeader(rootView);
            }
            if (shouldReload) {
                reloadContent();
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (tournesolFilterController != null) {
            tournesolFilterController.onDestroyView();
            tournesolFilterController = null;
        }
        super.onDestroyView();
    }

    private String lastRecommendationAlgorithm;

    private void updateSelectedDefaultKiosk() {
        try {
            serviceId = ServiceHelper.getSelectedServiceId(requireContext());

            String kioskId = null;
            String url = null;
            String kioskTranslatedName = null;

            final android.content.SharedPreferences prefs =
                    androidx.preference.PreferenceManager
                            .getDefaultSharedPreferences(requireContext());
            final String recommendationAlgorithmKey =
                    getString(R.string.recommendation_algorithm_key);
            final String recommendationAlgorithm =
                    prefs.getString(recommendationAlgorithmKey, "youtube");

            lastRecommendationAlgorithm = recommendationAlgorithm;

            final KioskList kioskList = NewPipe.getService(serviceId).getKioskList();

            if (serviceId == ServiceList.YouTube.getServiceId()) {
                if ("tournesol".equals(recommendationAlgorithm)) {
                    kioskId = "Tournesol";
                    url = "Tournesol";
                    kioskTranslatedName = KioskTranslator
                            .getTranslatedKioskName(kioskId, requireContext());
                } else if (!"youtube".equals(recommendationAlgorithm)
                        && recommendationAlgorithm != null
                        && !recommendationAlgorithm.isEmpty()) {
                    kioskId = recommendationAlgorithm;
                    try {
                        url = kioskList.getListLinkHandlerFactoryByType(kioskId)
                                .fromId(kioskId).getUrl();
                    } catch (final Exception e) {
                        kioskId = kioskList.getDefaultKioskId();
                        url = kioskList.getListLinkHandlerFactoryByType(kioskId)
                                .fromId(kioskId).getUrl();
                    }
                    kioskTranslatedName = KioskTranslator
                            .getTranslatedKioskName(kioskId, requireContext());
                } else {
                    kioskId = kioskList.getDefaultKioskId();
                    url = kioskList.getListLinkHandlerFactoryByType(kioskId)
                            .fromId(kioskId).getUrl();
                    kioskTranslatedName = KioskTranslator
                            .getTranslatedKioskName(kioskId, requireContext());
                }
            } else {
                kioskId = kioskList.getDefaultKioskId();
                url = kioskList.getListLinkHandlerFactoryByType(kioskId).fromId(kioskId).getUrl();
                kioskTranslatedName = KioskTranslator
                        .getTranslatedKioskName(kioskId, requireContext());
            }

            this.kioskId = kioskId;
            this.url = url;
            this.name = kioskTranslatedName;
            this.kioskTranslatedName = kioskTranslatedName;

            setInitialData(serviceId, url, kioskTranslatedName);

            currentInfo = null;
            currentNextPage = null;
        } catch (final ExtractionException e) {
            showError(new ErrorInfo(e, UserAction.REQUESTED_KIOSK,
                    "Loading default kiosk for selected service"));
        }
    }

    private void ensureTournesolController(@NonNull final View rootView) {
        if (tournesolFilterController == null) {
            tournesolFilterController =
                    new TournesolFilterController(this, this::onTournesolFiltersChanged);
        }
        tournesolFilterController.init(rootView);
    }

    private void onTournesolFiltersChanged(@NonNull final List<String> languages,
                                           @NonNull final String dateKey) {
        applyTournesolFilters(languages, dateKey, true);
    }

    private void applyTournesolFilters(@NonNull final List<String> languages,
                                       @NonNull final String dateKey,
                                       final boolean reload) {
        url = TournesolHelper.INSTANCE.buildTournesolUrl(languages, dateKey);
        if (!reload) {
            return;
        }
        currentInfo = null;
        currentNextPage = null;
        if (currentWorker != null) {
            currentWorker.dispose();
        }
        reloadContent();
    }

    private void hideTournesolHeader(@NonNull final View rootView) {
        final View headerContainer = rootView.findViewById(R.id.kiosk_header_container);
        if (headerContainer != null) {
            headerContainer.setVisibility(View.GONE);
        }
    }
}
