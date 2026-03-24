package org.schabi.newpipe.fragments.detail;

import static org.schabi.newpipe.extractor.stream.StreamExtractor.NO_AGE_LIMIT;
import static org.schabi.newpipe.util.Localization.getAppLocale;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.evernote.android.state.State;

import org.json.JSONArray;
import org.json.JSONObject;
import org.schabi.newpipe.DownloaderImpl;
import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.util.Localization;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DescriptionFragment extends BaseDescriptionFragment {

    private static final String TOURNESOL_API_BASE = "https://api.tournesol.app";
    private static final int TOURNESOL_SCORE_COLOR = Color.parseColor("#D1B65C");
    private static final int TOURNESOL_UNSAFE_COLOR = Color.parseColor("#E57373");
    private static final int YOUTUBE_SERVICE_ID = 0;

    private static final Map<String, Integer> CRITERIA_LABEL_MAP;
    static {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("reliability", R.string.tournesol_criteria_reliability);
        m.put("importance", R.string.tournesol_criteria_importance);
        m.put("engaging", R.string.tournesol_criteria_engaging);
        m.put("pedagogy", R.string.tournesol_criteria_pedagogy);
        m.put("layman_friendly", R.string.tournesol_criteria_layman_friendly);
        m.put("entertaining_relaxing", R.string.tournesol_criteria_entertaining_relaxing);
        m.put("diversity_inclusion", R.string.tournesol_criteria_diversity_inclusion);
        m.put("backfire_risk", R.string.tournesol_criteria_backfire_risk);
        m.put("better_habits", R.string.tournesol_criteria_better_habits);
        CRITERIA_LABEL_MAP = Collections.unmodifiableMap(m);
    }

    private final CompositeDisposable tournesolDisposables = new CompositeDisposable();

    @State
    StreamInfo streamInfo;

    public DescriptionFragment(final StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public DescriptionFragment() {
        // keep empty constructor for State when resuming fragment from memory
    }


    @Nullable
    @Override
    protected Description getDescription() {
        return streamInfo.getDescription();
    }

    @NonNull
    @Override
    protected StreamingService getService() {
        return streamInfo.getService();
    }

    @Override
    protected int getServiceId() {
        return streamInfo.getServiceId();
    }

    @NonNull
    @Override
    protected String getStreamUrl() {
        return streamInfo.getUrl();
    }

    @NonNull
    @Override
    public List<String> getTags() {
        return streamInfo.getTags();
    }

    @Override
    public void onDestroy() {
        tournesolDisposables.clear();
        super.onDestroy();
    }

    @Override
    protected void setupMetadata(final LayoutInflater inflater,
                                 final LinearLayout layout) {
        if (streamInfo != null && streamInfo.getUploadDate() != null) {
            binding.detailUploadDateView.setText(Localization
                    .localizeUploadDate(activity, streamInfo.getUploadDate().offsetDateTime()));
        } else {
            binding.detailUploadDateView.setVisibility(View.GONE);
        }

        if (streamInfo == null) {
            return;
        }

        fetchTournesolInfo();

        addMetadataItem(inflater, layout, false, R.string.metadata_category,
                streamInfo.getCategory());

        addMetadataItem(inflater, layout, false, R.string.metadata_licence,
                streamInfo.getLicence());

        addPrivacyMetadataItem(inflater, layout);

        if (streamInfo.getAgeLimit() != NO_AGE_LIMIT) {
            addMetadataItem(inflater, layout, false, R.string.metadata_age_limit,
                    String.valueOf(streamInfo.getAgeLimit()));
        }

        if (streamInfo.getLanguageInfo() != null) {
            addMetadataItem(inflater, layout, false, R.string.metadata_language,
                    streamInfo.getLanguageInfo().getDisplayLanguage(getAppLocale()));
        }

        addMetadataItem(inflater, layout, true, R.string.metadata_support,
                streamInfo.getSupportInfo());
        addMetadataItem(inflater, layout, true, R.string.metadata_host,
                streamInfo.getHost());

        addImagesMetadataItem(inflater, layout, R.string.metadata_thumbnails,
                streamInfo.getThumbnails());
        addImagesMetadataItem(inflater, layout, R.string.metadata_uploader_avatars,
                streamInfo.getUploaderAvatars());
        addImagesMetadataItem(inflater, layout, R.string.metadata_subchannel_avatars,
                streamInfo.getSubChannelAvatars());
    }

    private void fetchTournesolInfo() {
        if (binding == null) {
            return;
        }
        if (streamInfo.getServiceId() != YOUTUBE_SERVICE_ID) {
            return;
        }
        final String videoId = streamInfo.getId();
        if (videoId == null || videoId.isEmpty()) {
            return;
        }

        final String encodedUid;
        try {
            encodedUid = URLEncoder.encode("yt:" + videoId, "UTF-8");
        } catch (final Exception e) {
            return;
        }
        final String url = TOURNESOL_API_BASE + "/polls/videos/entities/" + encodedUid;

        tournesolDisposables.add(
                Single.fromCallable(() -> {
                    final OkHttpClient client = DownloaderImpl.getInstance().getClient();
                    final Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("Accept", "application/json")
                            .build();
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) {
                            return new JSONObject();
                        }
                        return new JSONObject(response.body().string());
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::displayTournesolInfo,
                        throwable -> {
                            // silently ignore - not all videos have Tournesol data
                        }
                )
        );
    }

    private void displayTournesolInfo(final JSONObject data) {
        if (data == null || !data.has("collective_rating")
                || binding == null || getContext() == null) {
            return;
        }

        final JSONObject collectiveRating = data.optJSONObject("collective_rating");
        if (collectiveRating == null) {
            return;
        }

        final double tournesolScore = collectiveRating.optDouble("tournesol_score", Double.NaN);
        if (Double.isNaN(tournesolScore)) {
            return;
        }

        final int nComparisons = collectiveRating.optInt("n_comparisons", 0);
        final int nContributors = collectiveRating.optInt("n_contributors", 0);

        // Set score value
        binding.tournesolScoreValue.setText(
                String.format(Locale.US, "%.1f", tournesolScore));

        // Set stats
        binding.tournesolStats.setText(
                getString(R.string.tournesol_detail_stats, nContributors, nComparisons));

        // Check unsafe status
        final JSONObject unsafe = collectiveRating.optJSONObject("unsafe");
        if (unsafe != null && unsafe.optBoolean("status", false)) {
            binding.tournesolUnsafeWarning.setText(R.string.tournesol_detail_unsafe_warning);
            binding.tournesolUnsafeWarning.setTextColor(TOURNESOL_UNSAFE_COLOR);
            binding.tournesolUnsafeWarning.setVisibility(View.VISIBLE);
        }

        // Criteria scores
        final JSONArray criteriaScores = collectiveRating.optJSONArray("criteria_scores");
        if (criteriaScores != null && criteriaScores.length() > 0) {
            displayCriteriaScores(criteriaScores);
        }

        // Show the section
        binding.tournesolInfoSection.setVisibility(View.VISIBLE);
    }

    private void displayCriteriaScores(final JSONArray criteriaScores) {
        // Parse and sort criteria by score descending
        final List<CriterionEntry> entries = new ArrayList<>();
        for (int i = 0; i < criteriaScores.length(); i++) {
            final JSONObject item = criteriaScores.optJSONObject(i);
            if (item == null) {
                continue;
            }
            final String criteria = item.optString("criteria", "");
            final double score = item.optDouble("score", 0);

            // Skip largely_recommended since it's the main score already shown
            if ("largely_recommended".equals(criteria)) {
                continue;
            }

            final Integer labelRes = CRITERIA_LABEL_MAP.get(criteria);
            final String label = labelRes != null
                    ? getString(labelRes)
                    : criteria.replace("_", " ");
            entries.add(new CriterionEntry(label, score));
        }

        // Sort by score descending
        entries.sort((a, b) -> Double.compare(b.score, a.score));

        // Create rows
        for (final CriterionEntry entry : entries) {
            binding.tournesolCriteriaContainer.addView(
                    createCriterionRow(entry.label, entry.score));
        }
    }

    private View createCriterionRow(final String label, final double score) {
        final LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dpToPx(3);
        row.setLayoutParams(rowParams);

        // Criterion label
        final TextView labelView = new TextView(requireContext());
        labelView.setText(label);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        final LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        row.addView(labelView);

        // Score value
        final TextView scoreView = new TextView(requireContext());
        scoreView.setText(String.format(Locale.US, "%.1f", score));
        scoreView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        scoreView.setTypeface(null, Typeface.BOLD);
        scoreView.setTextColor(score >= 0 ? TOURNESOL_SCORE_COLOR : TOURNESOL_UNSAFE_COLOR);
        row.addView(scoreView);

        return row;
    }

    private int dpToPx(final int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    private void addPrivacyMetadataItem(final LayoutInflater inflater, final LinearLayout layout) {
        if (streamInfo.getPrivacy() != null) {
            @StringRes final int contentRes;
            switch (streamInfo.getPrivacy()) {
                case PUBLIC:
                    contentRes = R.string.metadata_privacy_public;
                    break;
                case UNLISTED:
                    contentRes = R.string.metadata_privacy_unlisted;
                    break;
                case PRIVATE:
                    contentRes = R.string.metadata_privacy_private;
                    break;
                case INTERNAL:
                    contentRes = R.string.metadata_privacy_internal;
                    break;
                case OTHER:
                default:
                    contentRes = 0;
                    break;
            }

            if (contentRes != 0) {
                addMetadataItem(inflater, layout, false, R.string.metadata_privacy,
                        getString(contentRes));
            }
        }
    }

    private static class CriterionEntry {
        final String label;
        final double score;

        CriterionEntry(final String label, final double score) {
            this.label = label;
            this.score = score;
        }
    }
}
