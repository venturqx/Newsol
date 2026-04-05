package org.schabi.newpipe.fragments.detail;

import static org.schabi.newpipe.extractor.stream.StreamExtractor.NO_AGE_LIMIT;
import static org.schabi.newpipe.util.Localization.getAppLocale;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
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
import org.schabi.newpipe.util.TournesolScoreCache;

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
    private static final int TOURNESOL_SCORE_COLOR = Color.parseColor("#FFCA1D");
    private static final int TOURNESOL_UNSAFE_COLOR = Color.parseColor("#E57373");
    private static final int YOUTUBE_SERVICE_ID = 0;

    private static final Map<String, Integer> CRITERIA_LABEL_MAP;
    static {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("reliability", R.string.tournesol_criteria_reliability);
        m.put("pedagogy", R.string.tournesol_criteria_pedagogy);
        m.put("importance", R.string.tournesol_criteria_importance);
        m.put("layman_friendly", R.string.tournesol_criteria_layman_friendly);
        m.put("entertaining_relaxing", R.string.tournesol_criteria_entertaining_relaxing);
        m.put("engaging", R.string.tournesol_criteria_engaging);
        m.put("diversity_inclusion", R.string.tournesol_criteria_diversity_inclusion);
        m.put("better_habits", R.string.tournesol_criteria_better_habits);
        m.put("backfire_risk", R.string.tournesol_criteria_backfire_risk);
        CRITERIA_LABEL_MAP = Collections.unmodifiableMap(m);
    }

    private static final Map<String, Integer> CRITERIA_ICON_MAP;
    static {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("reliability", R.drawable.reliability);
        m.put("pedagogy", R.drawable.pedagogy);
        m.put("importance", R.drawable.importance);
        m.put("layman_friendly", R.drawable.layman_friendly);
        m.put("entertaining_relaxing", R.drawable.entertaining_relaxing);
        m.put("engaging", R.drawable.engaging);
        m.put("diversity_inclusion", R.drawable.diversity_inclusion);
        m.put("better_habits", R.drawable.better_habits);
        m.put("backfire_risk", R.drawable.backfire_risk);
        CRITERIA_ICON_MAP = Collections.unmodifiableMap(m);
    }

    private static final Map<String, Integer> CRITERIA_DESCRIPTION_MAP;
    static {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("reliability", R.string.compare_criteria_desc_reliability);
        m.put("pedagogy", R.string.compare_criteria_desc_pedagogy);
        m.put("importance", R.string.compare_criteria_desc_importance);
        m.put("layman_friendly", R.string.compare_criteria_desc_layman_friendly);
        m.put("entertaining_relaxing", R.string.compare_criteria_desc_entertaining_relaxing);
        m.put("engaging", R.string.compare_criteria_desc_engaging);
        m.put("diversity_inclusion", R.string.compare_criteria_desc_diversity_inclusion);
        m.put("better_habits", R.string.compare_criteria_desc_better_habits);
        m.put("backfire_risk", R.string.compare_criteria_desc_backfire_risk);
        CRITERIA_DESCRIPTION_MAP = Collections.unmodifiableMap(m);
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
        fetchTournesolDistribution();

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

        // Cache the score so the video detail header can use it without re-fetching
        TournesolScoreCache.INSTANCE.put(streamInfo.getOriginalUrl(),
                Math.round(tournesolScore));

        final int nComparisons = collectiveRating.optInt("n_comparisons", 0);
        final int nContributors = collectiveRating.optInt("n_contributors", 0);

        // Set score value
        binding.tournesolScoreValue.setText(
                String.format(Locale.US, "%.1f", tournesolScore));

        // Set stats
        binding.tournesolContributors.setText(
                getString(R.string.tournesol_detail_contributors, nContributors));
        binding.tournesolComparisons.setText(
                getString(R.string.tournesol_detail_comparisons, nComparisons));

        // Check unsafe status – show plant emoji instead of tournesol logo
        final JSONObject unsafe = collectiveRating.optJSONObject("unsafe");
        if (unsafe != null && unsafe.optBoolean("status", false)) {
            binding.tournesolLogo.setVisibility(View.GONE);
            binding.tournesolUnsafeEmoji.setVisibility(View.VISIBLE);
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
            entries.add(new CriterionEntry(criteria, label, score));
        }

        // Sort by fixed criteria order (matching compare view)
        final List<String> keyOrder = new ArrayList<>(CRITERIA_LABEL_MAP.keySet());
        entries.sort((a, b) -> {
            int ia = keyOrder.indexOf(a.id);
            int ib = keyOrder.indexOf(b.id);
            if (ia < 0) {
                ia = Integer.MAX_VALUE;
            }
            if (ib < 0) {
                ib = Integer.MAX_VALUE;
            }
            return Integer.compare(ia, ib);
        });

        // Add lollipop chart
        if (!entries.isEmpty()) {
            createLollipopChart(entries);
        }
    }



    private List<android.graphics.drawable.Drawable> loadRadarIcons(
            final List<CriterionEntry> entries) {
        final List<android.graphics.drawable.Drawable> icons = new ArrayList<>();
        final int iconSizePx = dpToPx(32);
        for (final CriterionEntry entry : entries) {
            final Integer iconRes = CRITERIA_ICON_MAP.get(entry.id);
            if (iconRes != null) {
                final android.graphics.drawable.Drawable d =
                        androidx.core.content.ContextCompat.getDrawable(
                                requireContext(), iconRes);
                if (d != null) {
                    d.setBounds(0, 0, iconSizePx, iconSizePx);
                }
                icons.add(d);
            } else {
                icons.add(null);
            }
        }
        return icons;
    }

    private static final Map<String, Integer> CRITERIA_COLOR_MAP;
    static {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("reliability", Color.parseColor("#4F77DD"));
        m.put("pedagogy", Color.parseColor("#C28BED"));
        m.put("importance", Color.parseColor("#DC8A5D"));
        m.put("layman_friendly", Color.parseColor("#4BB061"));
        m.put("entertaining_relaxing", Color.parseColor("#D8B36D"));
        m.put("engaging", Color.parseColor("#DFC642"));
        m.put("diversity_inclusion", Color.parseColor("#76C6CB"));
        m.put("better_habits", Color.parseColor("#9DD654"));
        m.put("backfire_risk", Color.parseColor("#D37A80"));
        CRITERIA_COLOR_MAP = Collections.unmodifiableMap(m);
    }

    private void createLollipopChart(final List<CriterionEntry> entries) {
        final List<android.graphics.drawable.Drawable> icons = loadRadarIcons(entries);
        final int[] colors = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            final Integer c = CRITERIA_COLOR_MAP.get(entries.get(i).id);
            colors[i] = c != null ? c : Color.WHITE;
        }

        // Replace FrameLayout with a vertical LinearLayout to hold chart + description
        final LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(Gravity.CENTER_HORIZONTAL);
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);

        final LollipopChartView lollipopView = new LollipopChartView(
                requireContext(), entries, icons, colors, dpToPx(18));

        final int glowPadding = dpToPx(8);
        final int chartWidth = dpToPx(340);
        final int chartHeight = dpToPx(260);
        final LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                chartWidth + glowPadding * 2, chartHeight + glowPadding * 2);
        lollipopView.setLayoutParams(chartParams);
        lollipopView.setPadding(glowPadding, glowPadding, glowPadding, glowPadding);

        // Description row: icon + text
        final LinearLayout descRow = new LinearLayout(requireContext());
        descRow.setOrientation(LinearLayout.HORIZONTAL);
        descRow.setGravity(Gravity.CENTER_VERTICAL);
        descRow.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), 0);
        descRow.setVisibility(View.GONE);

        final ImageView descIcon = new ImageView(requireContext());
        final int iconSize = dpToPx(28);
        descIcon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));

        final TextView descText = new TextView(requireContext());
        final LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(dpToPx(10));
        descText.setLayoutParams(textParams);
        descText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        descText.setTextColor(Color.parseColor("#B8FFFFFF"));
        descText.setMaxLines(4);

        descRow.addView(descIcon);
        descRow.addView(descText);

        wrapper.addView(lollipopView);
        wrapper.addView(descRow);

        // Handle dimension clicks
        lollipopView.setOnDimensionClickListener(index -> {
            final CriterionEntry entry = entries.get(index);
            final Integer iconRes = CRITERIA_ICON_MAP.get(entry.id);
            final Integer descRes = CRITERIA_DESCRIPTION_MAP.get(entry.id);
            if (iconRes != null && descRes != null) {
                descIcon.setImageResource(iconRes);
                final Integer labelRes = CRITERIA_LABEL_MAP.get(entry.id);
                final String title = labelRes != null ? getString(labelRes) : entry.label;
                final String full = title + "\n" + getString(descRes);
                final SpannableString spannable = new SpannableString(full);
                spannable.setSpan(new StyleSpan(Typeface.BOLD),
                        0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                descText.setText(spannable);
                descRow.setVisibility(View.VISIBLE);
            }
        });

        binding.tournesolLollipopContainer.removeAllViews();
        binding.tournesolLollipopContainer.addView(wrapper);
        binding.tournesolLollipopContainer.setVisibility(View.VISIBLE);
    }

    private void fetchTournesolDistribution() {
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
        final String url = TOURNESOL_API_BASE + "/polls/videos/entities/" + encodedUid
                + "/criteria_scores_distributions";

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
                        this::displayDistribution,
                        throwable -> { /* silently ignore */ }
                )
        );
    }

    private void displayDistribution(final JSONObject data) {
        if (data == null || binding == null || getContext() == null) {
            return;
        }
        final JSONArray distributions = data.optJSONArray("criteria_scores_distributions");
        if (distributions == null) {
            return;
        }

        JSONArray binsJson = null;
        JSONArray distributionJson = null;
        for (int i = 0; i < distributions.length(); i++) {
            final JSONObject entry = distributions.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            if ("largely_recommended".equals(entry.optString("criteria"))) {
                binsJson = entry.optJSONArray("bins");
                distributionJson = entry.optJSONArray("distribution");
                break;
            }
        }

        if (binsJson == null || distributionJson == null || distributionJson.length() == 0) {
            return;
        }

        final int[] bins = new int[binsJson.length()];
        for (int i = 0; i < binsJson.length(); i++) {
            bins[i] = binsJson.optInt(i);
        }
        final int[] distribution = new int[distributionJson.length()];
        for (int i = 0; i < distributionJson.length(); i++) {
            distribution[i] = distributionJson.optInt(i);
        }

        binding.tournesolDistributionContainer.addView(createDistributionChart(bins, distribution));
        binding.tournesolDistributionContainer.setVisibility(View.VISIBLE);
    }

    private static final int CHART_HEIGHT_DP = 28;

    private View createDistributionChart(final int[] bins, final int[] distribution) {
        int maxCount = 0;
        for (final int count : distribution) {
            maxCount = Math.max(maxCount, count);
        }
        if (maxCount == 0) {
            return new View(requireContext());
        }
        final int finalMaxCount = maxCount;
        final int chartHeightPx = dpToPx(CHART_HEIGHT_DP);
        final float strokePx = dpToPx(2) / 3f;

        // Custom view — draws a smooth Catmull-Rom curve with filled area + zero separator
        final View curveView = new View(requireContext()) {
            private final Paint curvePaint = buildCurvePaint();
            private final Paint fillPaint = buildFillPaint();
            private final Paint separatorPaint = buildSeparatorPaint();

            private Paint buildCurvePaint() {
                final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(TOURNESOL_SCORE_COLOR);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(strokePx);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeJoin(Paint.Join.ROUND);
                return p;
            }

            private Paint buildFillPaint() {
                final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(Color.argb(55, 0xFF, 0xCA, 0x1D));
                p.setStyle(Paint.Style.FILL);
                return p;
            }

            private Paint buildSeparatorPaint() {
                final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(Color.argb(140, 0xFF, 0xFF, 0xFF));
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(strokePx * 0.6f);
                return p;
            }

            @Override
            protected void onDraw(final Canvas canvas) {
                final int w = getWidth();
                final int h = getHeight();
                if (w == 0 || h == 0 || distribution.length == 0) {
                    return;
                }

                final int n = distribution.length;
                final float xMin = bins[0];
                final float xRange = bins[bins.length - 1] - xMin;
                final float topPad = h * 0.12f;

                // Pre-smooth with [0.25, 0.5, 0.25] weighted average
                final float[] smoothed = new float[n];
                smoothed[0] = distribution[0];
                smoothed[n - 1] = distribution[n - 1];
                for (int i = 1; i < n - 1; i++) {
                    smoothed[i] = distribution[i - 1] * 0.25f
                            + distribution[i] * 0.5f
                            + distribution[i + 1] * 0.25f;
                }

                final float[] xs = new float[n];
                final float[] ys = new float[n];
                for (int i = 0; i < n; i++) {
                    final float binCenter = (bins[i] + bins[i + 1]) / 2f;
                    xs[i] = (binCenter - xMin) / xRange * w;
                    ys[i] = topPad + (1f - smoothed[i] / (float) finalMaxCount)
                            * (h - topPad);
                }

                // Fritsch-Carlson monotone cubic interpolation
                // Step 1: compute slopes (deltas) and secants
                final float[] dx = new float[n - 1];
                final float[] dy = new float[n - 1];
                final float[] slopes = new float[n - 1];
                for (int i = 0; i < n - 1; i++) {
                    dx[i] = xs[i + 1] - xs[i];
                    dy[i] = ys[i + 1] - ys[i];
                    slopes[i] = dx[i] == 0 ? 0 : dy[i] / dx[i];
                }

                // Step 2: compute tangents at each point
                final float[] m = new float[n];
                m[0] = slopes[0];
                m[n - 1] = slopes[n - 2];
                for (int i = 1; i < n - 1; i++) {
                    if (slopes[i - 1] * slopes[i] <= 0) {
                        // Sign change or zero — flat tangent prevents overshoot
                        m[i] = 0;
                    } else {
                        m[i] = (slopes[i - 1] + slopes[i]) / 2f;
                    }
                }

                // Step 3: enforce monotonicity (Fritsch-Carlson conditions)
                for (int i = 0; i < n - 1; i++) {
                    if (slopes[i] == 0) {
                        m[i] = 0;
                        m[i + 1] = 0;
                    } else {
                        final float alpha = m[i] / slopes[i];
                        final float beta = m[i + 1] / slopes[i];
                        // Restrict to circle of radius 3 to ensure monotonicity
                        final float mag = alpha * alpha + beta * beta;
                        if (mag > 9f) {
                            final float s = 3f / (float) Math.sqrt(mag);
                            m[i] = s * alpha * slopes[i];
                            m[i + 1] = s * beta * slopes[i];
                        }
                    }
                }

                // Step 4: build cubic bezier path from Hermite tangents
                final Path path = new Path();
                path.moveTo(xs[0], ys[0]);
                for (int i = 0; i < n - 1; i++) {
                    final float seg = dx[i] / 3f;
                    path.cubicTo(
                            xs[i] + seg, ys[i] + m[i] * seg,
                            xs[i + 1] - seg, ys[i + 1] - m[i + 1] * seg,
                            xs[i + 1], ys[i + 1]);
                }

                // Fill under curve
                final Path fill = new Path(path);
                fill.lineTo(xs[n - 1], h);
                fill.lineTo(xs[0], h);
                fill.close();
                canvas.drawPath(fill, fillPaint);

                // Curve line
                canvas.drawPath(path, curvePaint);

                // Vertical separator at score = 0
                final float zeroX = (0f - xMin) / xRange * w;
                canvas.drawLine(zeroX, 0f, zeroX, (float) h, separatorPaint);
            }
        };
        curveView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, chartHeightPx));

        // Axis labels: bins[0], 0, bins[last]
        return curveView;
    }

    private int dpToPx(final int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    private int dpToPx(final float dp) {
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

    interface OnDimensionClickListener {
        void onDimensionClick(int index);
    }

    private static class LollipopChartView extends View {
        private final List<CriterionEntry> entries;
        private final List<android.graphics.drawable.Drawable> icons;
        private final int[] colors;
        private final int iconSizePx;
        private OnDimensionClickListener dimensionClickListener;
        private int selectedIndex = -1;

        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint zeroLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint selectedGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Cached hit targets for touch detection
        private final float[] circleCxArray;
        private final float[] circleCyArray;
        private float circleRadiusCached;

        LollipopChartView(final android.content.Context context,
                          final List<CriterionEntry> entries,
                          final List<android.graphics.drawable.Drawable> icons,
                          final int[] colors,
                          final int iconSizePx) {
            super(context);
            this.entries = entries;
            this.icons = icons;
            this.colors = colors;
            this.iconSizePx = iconSizePx;
            this.circleCxArray = new float[entries.size()];
            this.circleCyArray = new float[entries.size()];

            barPaint.setStyle(Paint.Style.FILL);
            barPaint.setStrokeCap(Paint.Cap.BUTT);

            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(Color.parseColor("#0F0F0F"));

            circleStrokePaint.setStyle(Paint.Style.STROKE);
            circleStrokePaint.setStrokeWidth(dp(3.5f));

            scorePaint.setTextSize(dp(10f));
            scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            scorePaint.setTextAlign(Paint.Align.CENTER);
            scorePaint.setColor(Color.WHITE);

            zeroLinePaint.setStyle(Paint.Style.STROKE);
            zeroLinePaint.setColor(Color.parseColor("#40FFFFFF"));
            zeroLinePaint.setStrokeWidth(dp(1f));

            selectedGlowPaint.setStyle(Paint.Style.STROKE);
            selectedGlowPaint.setStrokeWidth(dp(4f));

            setClickable(true);
        }

        void setOnDimensionClickListener(final OnDimensionClickListener listener) {
            this.dimensionClickListener = listener;
        }

        private int dp(final float dpVal) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, dpVal,
                    getResources().getDisplayMetrics());
        }

        @Override
        public boolean onTouchEvent(final MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                final float tx = event.getX();
                final float ty = event.getY();
                final float touchRadius = circleRadiusCached + dp(8f);
                for (int i = 0; i < entries.size(); i++) {
                    final float dx = tx - circleCxArray[i];
                    final float dy = ty - circleCyArray[i];
                    if (dx * dx + dy * dy <= touchRadius * touchRadius) {
                        selectedIndex = (selectedIndex == i) ? -1 : i;
                        invalidate();
                        if (dimensionClickListener != null && selectedIndex >= 0) {
                            dimensionClickListener.onDimensionClick(selectedIndex);
                        }
                        return true;
                    }
                }
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            super.onDraw(canvas);
            final int n = entries.size();
            if (n == 0) {
                return;
            }

            final float w = getWidth();
            final float h = getHeight();
            final float circleRadius = dp(14f);
            circleRadiusCached = circleRadius;
            final float scoreTextHeight = dp(14f);
            final float topPadding = scoreTextHeight + dp(2f);
            final float bottomPadding = scoreTextHeight + dp(2f);
            final float barWidth = dp(12f);

            // The vertical area for bars: from topPadding+circleRadius to
            // h-bottomPadding-circleRadius
            final float drawTop = topPadding + circleRadius;
            final float drawBottom = h - bottomPadding - circleRadius;

            // Find min/max scores; range always includes zero
            double minScore = 0;
            double maxScore = 0;
            for (final CriterionEntry entry : entries) {
                if (entry.score < minScore) {
                    minScore = entry.score;
                }
                if (entry.score > maxScore) {
                    maxScore = entry.score;
                }
            }
            double totalRange = maxScore - minScore;
            if (totalRange < 1) {
                totalRange = 1;
            }

            // Position zero line proportionally within the draw area
            final float zeroY = (float) (drawTop
                    + (maxScore / totalRange) * (drawBottom - drawTop));

            // Draw zero line
            final float slotWidth = w / n;
            canvas.drawLine(slotWidth * 0.3f, zeroY,
                    w - slotWidth * 0.3f, zeroY, zeroLinePaint);

            for (int i = 0; i < n; i++) {
                final CriterionEntry entry = entries.get(i);
                final int color = colors[i];
                final float cx = slotWidth * (i + 0.5f);

                // Map score to vertical position within drawTop..drawBottom
                final float barEndY = (float) (drawTop
                        + ((maxScore - entry.score) / totalRange)
                        * (drawBottom - drawTop));

                // Cache positions for touch detection
                circleCxArray[i] = cx;
                circleCyArray[i] = barEndY;

                // Draw bar (from zero to barEnd)
                barPaint.setColor(color);
                barPaint.setStrokeWidth(barWidth);
                canvas.drawLine(cx, zeroY, cx, barEndY, barPaint);

                // Draw selected glow ring
                if (i == selectedIndex) {
                    selectedGlowPaint.setColor(color);
                    selectedGlowPaint.setAlpha(160);
                    canvas.drawCircle(cx, barEndY, circleRadius + dp(3f),
                            selectedGlowPaint);
                }

                // Draw circle at end of bar: border only, background fill
                canvas.drawCircle(cx, barEndY, circleRadius, circlePaint);
                circleStrokePaint.setColor(color);
                canvas.drawCircle(cx, barEndY, circleRadius, circleStrokePaint);

                // Draw icon inside circle (keep original icon color)
                final android.graphics.drawable.Drawable icon =
                        i < icons.size() ? icons.get(i) : null;
                if (icon != null) {
                    final int strokeInset = dp(3.5f) / 2;
                    int halfIconW = iconSizePx / 2 - strokeInset;
                    int halfIconH = iconSizePx / 2 - strokeInset;
                    float iconOffsetY = 0;
                    final String entryId = entry.id;
                    // layman_friendly icon is slightly too large / stretched vertically
                    if ("layman_friendly".equals(entryId)) {
                        halfIconW = (int) (halfIconW * 0.88f);
                        halfIconH = (int) (halfIconH * 0.88f);
                    }
                    // backfire_risk icon content sits slightly high in its viewport
                    if ("backfire_risk".equals(entryId)) {
                        iconOffsetY = dp(1f);
                    }
                    final float iy = barEndY + iconOffsetY;
                    icon.setBounds(
                            (int) (cx - halfIconW), (int) (iy - halfIconH),
                            (int) (cx + halfIconW), (int) (iy + halfIconH));
                    icon.setTintList(null);
                    icon.draw(canvas);
                }

                // Draw score text above/below the circle
                final String scoreText = String.format(Locale.US, "%.0f",
                        entry.score);
                scorePaint.setColor(color);
                if (entry.score >= 0) {
                    // Score above the circle
                    canvas.drawText(scoreText, cx,
                            barEndY - circleRadius - dp(6f), scorePaint);
                } else {
                    // Score below the circle
                    canvas.drawText(scoreText, cx,
                            barEndY + circleRadius + scoreTextHeight + dp(1f),
                            scorePaint);
                }
            }
        }
    }


    private static class CriterionEntry {
        final String id;
        final String label;
        final double score;

        CriterionEntry(final String id, final String label, final double score) {
            this.id = id;
            this.label = label;
            this.score = score;
        }
    }
}
