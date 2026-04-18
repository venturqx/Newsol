package org.schabi.newpipe.fragments.list.kiosk;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.R;
import org.schabi.newpipe.error.ErrorInfo;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.NewPipe;
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
                    tournesolFilterController.getCurrentDateKey(),
                    tournesolFilterController.getCurrentIncludeLowScoreVideos(),
                    tournesolFilterController.getCurrentDurationMinSeconds(),
                    tournesolFilterController.getCurrentDurationMaxSeconds(),
                    tournesolFilterController.getCurrentWeightLargelyRecommended(),
                    tournesolFilterController.getCurrentWeightReliability(),
                    tournesolFilterController.getCurrentWeightImportance(),
                    tournesolFilterController.getCurrentWeightPedagogy(),
                    tournesolFilterController.getCurrentWeightLaymanFriendly(),
                    tournesolFilterController.getCurrentWeightEntertainingRelaxing(),
                    tournesolFilterController.getCurrentWeightEngaging(),
                    tournesolFilterController.getCurrentWeightDiversityInclusion(),
                    tournesolFilterController.getCurrentWeightBetterHabits(),
                    tournesolFilterController.getCurrentWeightBackfireRisk(),
                    false);
        } else {
            hideTournesolHeader(rootView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serviceId != ServiceHelper.getSelectedServiceId(requireContext())) {
            if (currentWorker != null) {
                currentWorker.dispose();
            }
            updateSelectedDefaultKiosk();
            reloadContent();
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

    private void ensureTournesolController(@NonNull final View rootView) {
        if (tournesolFilterController == null) {
            tournesolFilterController =
                    new TournesolFilterController(this, this::onTournesolFiltersChanged);
        }
        tournesolFilterController.init(rootView);
    }

    private void onTournesolFiltersChanged(@NonNull final List<String> languages,
                                           @NonNull final String dateKey,
                                           final boolean includeLowScoreVideos,
                                           final int durationMinSeconds,
                                           final int durationMaxSeconds,
                                           final int weightLargelyRecommended,
                                           final int weightReliability,
                                           final int weightImportance,
                                           final int weightPedagogy,
                                           final int weightLaymanFriendly,
                                           final int weightEntertainingRelaxing,
                                           final int weightEngaging,
                                           final int weightDiversityInclusion,
                                           final int weightBetterHabits,
                                           final int weightBackfireRisk) {
        applyTournesolFilters(languages, dateKey, includeLowScoreVideos,
                durationMinSeconds, durationMaxSeconds,
                weightLargelyRecommended, weightReliability, weightImportance,
                weightPedagogy, weightLaymanFriendly, weightEntertainingRelaxing,
                weightEngaging, weightDiversityInclusion, weightBetterHabits,
                weightBackfireRisk, true);
    }

    private void applyTournesolFilters(@NonNull final List<String> languages,
                                       @NonNull final String dateKey,
                                       final boolean includeLowScoreVideos,
                                       final int durationMinSeconds,
                                       final int durationMaxSeconds,
                                       final int weightLargelyRecommended,
                                       final int weightReliability,
                                       final int weightImportance,
                                       final int weightPedagogy,
                                       final int weightLaymanFriendly,
                                       final int weightEntertainingRelaxing,
                                       final int weightEngaging,
                                       final int weightDiversityInclusion,
                                       final int weightBetterHabits,
                                       final int weightBackfireRisk,
                                       final boolean reload) {
        url = TournesolHelper.INSTANCE.buildTournesolUrl(languages, dateKey,
                includeLowScoreVideos, durationMinSeconds, durationMaxSeconds,
                weightLargelyRecommended, weightReliability, weightImportance,
                weightPedagogy, weightLaymanFriendly, weightEntertainingRelaxing,
                weightEngaging, weightDiversityInclusion, weightBetterHabits,
                weightBackfireRisk);
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

    private void updateSelectedDefaultKiosk() {
        try {
            serviceId = ServiceHelper.getSelectedServiceId(requireContext());

            final KioskList kioskList = NewPipe.getService(serviceId).getKioskList();
            kioskId = kioskList.getDefaultKioskId();
            url = kioskList.getListLinkHandlerFactoryByType(kioskId).fromId(kioskId).getUrl();

            kioskTranslatedName = KioskTranslator.getTranslatedKioskName(kioskId, requireContext());
            name = kioskTranslatedName;

            currentInfo = null;
            currentNextPage = null;
        } catch (final ExtractionException e) {
            showError(new ErrorInfo(e, UserAction.REQUESTED_KIOSK,
                    "Loading default kiosk for selected service"));
        }
    }
}
