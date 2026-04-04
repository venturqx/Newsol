package org.schabi.newpipe.fragments.detail;

import static org.schabi.newpipe.extractor.stream.StreamExtractor.NO_AGE_LIMIT;
import static org.schabi.newpipe.util.Localization.getAppLocale;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.FrameLayout;
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

        // Measure the widest label and widest score so columns align
        final TextPaint labelPaint = new TextPaint();
        labelPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 12,
                requireContext().getResources().getDisplayMetrics()));
        final TextPaint scorePaint = new TextPaint();
        scorePaint.setTextSize(labelPaint.getTextSize());
        scorePaint.setTypeface(Typeface.DEFAULT_BOLD);
        int maxLabelWidth = 0;
        int maxScoreWidth = 0;
        for (final CriterionEntry entry : entries) {
            final int lw = (int) Math.ceil(labelPaint.measureText(entry.label));
            if (lw > maxLabelWidth) {
                maxLabelWidth = lw;
            }
            final String scoreText = String.format(Locale.US, "%.1f", entry.score);
            final int sw = (int) Math.ceil(scorePaint.measureText(scoreText));
            if (sw > maxScoreWidth) {
                maxScoreWidth = sw;
            }
        }

        // Create rows
        for (final CriterionEntry entry : entries) {
            binding.tournesolCriteriaContainer.addView(
                    createCriterionRow(entry.id, entry.label, entry.score,
                            maxLabelWidth, maxScoreWidth));
        }

        // Add lollipop chart below the bars
        if (!entries.isEmpty()) {
            createLollipopChart(entries);
        }

        // Add radar chart below the bars
        if (!entries.isEmpty()) {
            createRadarChart(entries);
        }
    }

    private static final int BAR_HEIGHT_DP = 10;
    private static final int BAR_CORNER_RADIUS_DP = 4;
    private static final int BAR_BORDER_WIDTH_DP = 2;
    private static final double BAR_MAX_SCORE = 50.0;

    private View createCriterionRow(final String id, final String label,
                                     final double score, final int labelWidth,
                                     final int scoreWidth) {
        final LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dpToPx(3);
        row.setLayoutParams(rowParams);

        // Criterion label — fixed width (widest label) so bar area is uniform
        final TextView labelView = new TextView(requireContext());
        labelView.setText(label);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                labelWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(labelView);

        // Bar container — fills remaining space, bar is RIGHT-aligned inside
        final LinearLayout barContainer = new LinearLayout(requireContext());
        barContainer.setOrientation(LinearLayout.HORIZONTAL);
        barContainer.setGravity(Gravity.CENTER_VERTICAL);
        final LinearLayout.LayoutParams barContainerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        barContainerParams.leftMargin = dpToPx(4);
        barContainerParams.rightMargin = dpToPx(4);
        barContainer.setLayoutParams(barContainerParams);
        row.addView(barContainer);

        final int scoreColor = score >= 0 ? TOURNESOL_SCORE_COLOR : TOURNESOL_UNSAFE_COLOR;
        final int barHeight = dpToPx(BAR_HEIGHT_DP);
        final float r = dpToPx(BAR_CORNER_RADIUS_DP);
        final double clamped = Math.max(0, Math.min(BAR_MAX_SCORE, score));

        // Left spacer — pushes bar+border to the right
        if (clamped < BAR_MAX_SCORE) {
            final View spacer = new View(requireContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(
                    0, barHeight, (float) (BAR_MAX_SCORE - clamped)));
            barContainer.addView(spacer);
        }

        // Bar — grows leftward (rounded left end, flat right end against border)
        if (clamped > 0) {
            final View bar = new View(requireContext());
            final GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(TOURNESOL_SCORE_COLOR);
            // Rounded left, flat right
            drawable.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, r, r});
            bar.setBackground(drawable);
            bar.setLayoutParams(new LinearLayout.LayoutParams(
                    0, barHeight, (float) clamped));
            barContainer.addView(bar);
        }

        // Right border "|" — flat edge, always visible
        final View rightBorder = new View(requireContext());
        rightBorder.setBackgroundColor(scoreColor);
        rightBorder.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(BAR_BORDER_WIDTH_DP), barHeight));
        barContainer.addView(rightBorder);

        // Score value — fixed width (widest score), right-aligned
        final TextView scoreView = new TextView(requireContext());
        scoreView.setText(String.format(Locale.US, "%.1f", score));
        scoreView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        scoreView.setTypeface(null, Typeface.BOLD);
        scoreView.setTextColor(scoreColor);
        scoreView.setGravity(Gravity.END);
        scoreView.setLayoutParams(new LinearLayout.LayoutParams(
                scoreWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(scoreView);

        // Criterion icon
        final Integer iconRes = CRITERIA_ICON_MAP.get(id);
        if (iconRes != null) {
            final ImageView icon = new ImageView(requireContext());
            icon.setImageResource(iconRes);
            final int iconSize = dpToPx(14);
            final LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    iconSize, iconSize);
            iconParams.leftMargin = dpToPx(3);
            icon.setLayoutParams(iconParams);
            row.addView(icon);
        }

        return row;
    }

    private static final double RADAR_MIN_SCORE = -100.0;
    private static final double RADAR_MAX_SCORE = 100.0;
    private static final double RADAR_MID_SCORE = 0.0;

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

    private void createRadarChart(final List<CriterionEntry> entries) {
        final List<android.graphics.drawable.Drawable> icons = loadRadarIcons(entries);
        final View radarView = new RadarChartView(
                requireContext(), entries, icons, dpToPx(32));

        final int chartSize = dpToPx(340);
        radarView.setLayoutParams(new FrameLayout.LayoutParams(
                chartSize, chartSize, Gravity.CENTER));

        binding.tournesolRadarContainer.removeAllViews();
        binding.tournesolRadarContainer.addView(radarView);
        binding.tournesolRadarContainer.setVisibility(View.VISIBLE);
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
        final View lollipopView = new LollipopChartView(
                requireContext(), entries, icons, colors, dpToPx(24));

        final int chartWidth = dpToPx(340);
        final int chartHeight = dpToPx(260);
        lollipopView.setLayoutParams(new FrameLayout.LayoutParams(
                chartWidth, chartHeight, Gravity.CENTER));

        binding.tournesolLollipopContainer.removeAllViews();
        binding.tournesolLollipopContainer.addView(lollipopView);
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

    private static class LollipopChartView extends View {
        private final List<CriterionEntry> entries;
        private final List<android.graphics.drawable.Drawable> icons;
        private final int[] colors;
        private final int iconSizePx;

        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint zeroLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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

            barPaint.setStyle(Paint.Style.FILL);
            barPaint.setStrokeCap(Paint.Cap.ROUND);

            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(Color.parseColor("#0F0F0F"));

            circleStrokePaint.setStyle(Paint.Style.STROKE);
            circleStrokePaint.setStrokeWidth(dp(2.5f));

            scorePaint.setTextSize(dp(10f));
            scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            scorePaint.setTextAlign(Paint.Align.CENTER);
            scorePaint.setColor(Color.WHITE);

            zeroLinePaint.setStyle(Paint.Style.STROKE);
            zeroLinePaint.setColor(Color.parseColor("#40FFFFFF"));
            zeroLinePaint.setStrokeWidth(dp(1f));
        }

        private int dp(final float dpVal) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, dpVal,
                    getResources().getDisplayMetrics());
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
            final float circleRadius = dp(18f);
            final float scoreTextHeight = dp(14f);
            final float topPadding = scoreTextHeight + dp(2f);
            final float bottomPadding = scoreTextHeight + dp(2f);
            final float barWidth = dp(8f);

            // The vertical area for bars: from topPadding+circleRadius to
            // h-bottomPadding-circleRadius
            final float drawTop = topPadding + circleRadius;
            final float drawBottom = h - bottomPadding - circleRadius;
            final float zeroY = (drawTop + drawBottom) / 2f;

            // Draw zero line
            final float slotWidth = w / n;
            canvas.drawLine(slotWidth * 0.3f, zeroY,
                    w - slotWidth * 0.3f, zeroY, zeroLinePaint);

            // Find max absolute score for scaling
            double maxAbs = 1;
            for (final CriterionEntry entry : entries) {
                final double abs = Math.abs(entry.score);
                if (abs > maxAbs) {
                    maxAbs = abs;
                }
            }

            for (int i = 0; i < n; i++) {
                final CriterionEntry entry = entries.get(i);
                final int color = colors[i];
                final float cx = slotWidth * (i + 0.5f);

                // Normalized score: how far from zero line
                final float norm = (float) (entry.score / maxAbs);
                // barEnd: negative score goes down, positive goes up
                final float halfRange = (drawBottom - drawTop) / 2f;
                final float barEndY = zeroY - norm * halfRange;

                // Draw bar (from zero to barEnd)
                // Offset start by half barWidth so the round cap doesn't overflow Y=0
                barPaint.setColor(color);
                barPaint.setAlpha(180);
                barPaint.setStrokeWidth(barWidth);
                final float halfBar = barWidth / 2f;
                final float barStartY = norm >= 0
                        ? zeroY - halfBar : zeroY + halfBar;
                canvas.drawLine(cx, barStartY, cx, barEndY, barPaint);

                // Draw circle at end of bar: border only, background fill
                canvas.drawCircle(cx, barEndY, circleRadius, circlePaint);
                circleStrokePaint.setColor(color);
                canvas.drawCircle(cx, barEndY, circleRadius, circleStrokePaint);

                // Draw icon inside circle (keep original icon color)
                final android.graphics.drawable.Drawable icon =
                        i < icons.size() ? icons.get(i) : null;
                if (icon != null) {
                    final int halfIcon = iconSizePx / 2;
                    icon.setBounds(
                            (int) (cx - halfIcon), (int) (barEndY - halfIcon),
                            (int) (cx + halfIcon), (int) (barEndY + halfIcon));
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
                            barEndY - circleRadius - dp(3f), scorePaint);
                } else {
                    // Score below the circle
                    canvas.drawText(scoreText, cx,
                            barEndY + circleRadius + scoreTextHeight - dp(2f),
                            scorePaint);
                }
            }
        }
    }

    private static class RadarChartView extends View {
        private final List<CriterionEntry> entries;
        private final List<android.graphics.drawable.Drawable> icons;
        private final int iconSizePx;

        // Zone colors
        private static final int COLOR_BAD_ZONE = Color.parseColor("#30E53935");
        private static final int COLOR_BAD_ZONE_CENTER = Color.parseColor("#50B71C1C");
        private static final int COLOR_GOOD_ZONE = Color.parseColor("#1843A047");
        private static final int COLOR_GOOD_ZONE_EDGE = Color.parseColor("#0843A047");
        private static final int COLOR_MID_RING = Color.parseColor("#AAFFFFFF");
        private static final int COLOR_GOLD = Color.parseColor("#FFCA1D");
        private static final int COLOR_GOLD_BRIGHT = Color.parseColor("#FFD54F");
        private static final int COLOR_RED = Color.parseColor("#EF5350");
        private static final int COLOR_RED_DARK = Color.parseColor("#C62828");
        private static final int COLOR_GREEN_ACCENT = Color.parseColor("#66BB6A");

        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridPaintOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint midRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint midRingGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint badZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint goodZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotBadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotBadOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotGlowBadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint valueBadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint zoneLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path dataPath = new Path();
        private final Path gridPath = new Path();
        private final Path zonePath = new Path();

        RadarChartView(final android.content.Context context,
                       final List<CriterionEntry> entries,
                       final List<android.graphics.drawable.Drawable> icons,
                       final int iconSizePx) {
            super(context);
            this.entries = entries;
            this.icons = icons;
            this.iconSizePx = iconSizePx;
            initPaints();
        }

        private int dp(final float dpVal) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, dpVal,
                    getResources().getDisplayMetrics());
        }

        private float dpF(final float dpVal) {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, dpVal,
                    getResources().getDisplayMetrics());
        }

        private void initPaints() {
            // Grid lines - very subtle
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setColor(Color.parseColor("#15FFFFFF"));
            gridPaint.setStrokeWidth(dpF(0.75f));

            gridPaintOuter.setStyle(Paint.Style.STROKE);
            gridPaintOuter.setColor(Color.parseColor("#30FFFFFF"));
            gridPaintOuter.setStrokeWidth(dpF(1.2f));

            // Axis lines - barely visible
            axisPaint.setStyle(Paint.Style.STROKE);
            axisPaint.setColor(Color.parseColor("#0DFFFFFF"));
            axisPaint.setStrokeWidth(dpF(0.5f));

            // Data fill
            fillPaint.setStyle(Paint.Style.FILL);

            // Data stroke - gold line
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(COLOR_GOLD);
            strokePaint.setStrokeWidth(dpF(2f));
            strokePaint.setStrokeJoin(Paint.Join.ROUND);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);

            // Glow behind data stroke
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setColor(Color.parseColor("#40FFCA1D"));
            glowPaint.setStrokeWidth(dpF(5));
            glowPaint.setStrokeJoin(Paint.Join.ROUND);
            glowPaint.setMaskFilter(new BlurMaskFilter(
                    dpF(10), BlurMaskFilter.Blur.NORMAL));

            // Good dots - gold with white outline
            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(COLOR_GOLD_BRIGHT);

            dotOutlinePaint.setStyle(Paint.Style.STROKE);
            dotOutlinePaint.setColor(Color.parseColor("#DDFFFFFF"));
            dotOutlinePaint.setStrokeWidth(dpF(1.5f));

            dotGlowPaint.setStyle(Paint.Style.FILL);
            dotGlowPaint.setColor(Color.parseColor("#66FFCA1D"));
            dotGlowPaint.setMaskFilter(new BlurMaskFilter(
                    dpF(8), BlurMaskFilter.Blur.NORMAL));

            // Bad dots - red with outline
            dotBadPaint.setStyle(Paint.Style.FILL);
            dotBadPaint.setColor(COLOR_RED);

            dotBadOutlinePaint.setStyle(Paint.Style.STROKE);
            dotBadOutlinePaint.setColor(Color.parseColor("#AAFFFFFF"));
            dotBadOutlinePaint.setStrokeWidth(dpF(1.5f));

            dotGlowBadPaint.setStyle(Paint.Style.FILL);
            dotGlowBadPaint.setColor(Color.parseColor("#66E53935"));
            dotGlowBadPaint.setMaskFilter(new BlurMaskFilter(
                    dpF(8), BlurMaskFilter.Blur.NORMAL));

            // Value labels
            valuePaint.setTextSize(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 12,
                    getResources().getDisplayMetrics()));
            valuePaint.setColor(Color.parseColor("#EEFFFFFF"));
            valuePaint.setTextAlign(Paint.Align.CENTER);
            valuePaint.setTypeface(Typeface.create("sans-serif-medium",
                    Typeface.NORMAL));

            valueBadPaint.setTextSize(valuePaint.getTextSize());
            valueBadPaint.setColor(COLOR_RED);
            valueBadPaint.setTextAlign(Paint.Align.CENTER);
            valueBadPaint.setTypeface(Typeface.create("sans-serif-medium",
                    Typeface.NORMAL));

            // Mid ring (score = 0 boundary) - dashed, prominent
            midRingPaint.setStyle(Paint.Style.STROKE);
            midRingPaint.setColor(COLOR_MID_RING);
            midRingPaint.setStrokeWidth(dpF(1.2f));
            midRingPaint.setPathEffect(new DashPathEffect(
                    new float[]{dpF(4), dpF(3)}, 0));

            midRingGlowPaint.setStyle(Paint.Style.STROKE);
            midRingGlowPaint.setColor(Color.parseColor("#22FFFFFF"));
            midRingGlowPaint.setStrokeWidth(dpF(4));
            midRingGlowPaint.setMaskFilter(new BlurMaskFilter(
                    dpF(3), BlurMaskFilter.Blur.NORMAL));

            // Background
            bgPaint.setStyle(Paint.Style.FILL);

            // Bad zone fill
            badZonePaint.setStyle(Paint.Style.FILL);

            // Good zone fill
            goodZonePaint.setStyle(Paint.Style.FILL);

            // Zone labels ("BAD" / "GOOD")
            zoneLabelPaint.setTextSize(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 9,
                    getResources().getDisplayMetrics()));
            zoneLabelPaint.setTextAlign(Paint.Align.CENTER);
            zoneLabelPaint.setTypeface(Typeface.create("sans-serif",
                    Typeface.BOLD));
            zoneLabelPaint.setLetterSpacing(0.15f);
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            super.onDraw(canvas);
            final int n = entries.size();
            if (n < 3) {
                return;
            }

            setLayerType(LAYER_TYPE_SOFTWARE, null);

            final float w = getWidth();
            final float h = getHeight();
            final float cx = w / 2f;
            final float cy = h / 2f;
            final float outerMargin = dp(52);
            final float radius = Math.min(cx, cy) - outerMargin;
            final double angleStep = 2.0 * Math.PI / n;
            final double startAngle = -Math.PI / 2.0;

            drawBackground(canvas, cx, cy, radius);
            drawZones(canvas, cx, cy, radius, n, angleStep, startAngle);
            drawGrid(canvas, cx, cy, radius, n, angleStep, startAngle);
            drawAxes(canvas, cx, cy, radius, n, angleStep, startAngle);
            drawMidRing(canvas, cx, cy, radius, n, angleStep, startAngle);
            drawDataArea(canvas, cx, cy, radius, n, angleStep, startAngle);
            drawDotsAndLabels(canvas, cx, cy, radius, n, angleStep, startAngle);
        }

        private void drawBackground(final Canvas canvas,
                                     final float cx, final float cy,
                                     final float radius) {
            // Subtle dark radial glow
            bgPaint.setShader(new RadialGradient(cx, cy, radius * 1.3f,
                    Color.parseColor("#0AFFFFFF"), Color.TRANSPARENT,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius * 1.3f, bgPaint);
        }

        private void drawZones(final Canvas canvas,
                                final float cx, final float cy,
                                final float radius, final int n,
                                final double angleStep,
                                final double startAngle) {
            final float midNorm = (float) ((RADAR_MID_SCORE - RADAR_MIN_SCORE)
                    / (RADAR_MAX_SCORE - RADAR_MIN_SCORE));
            final float midR = radius * midNorm;

            // Bad zone: center to midpoint - red gradient
            badZonePaint.setShader(new RadialGradient(cx, cy, midR,
                    COLOR_BAD_ZONE_CENTER, COLOR_BAD_ZONE,
                    Shader.TileMode.CLAMP));
            zonePath.reset();
            for (int i = 0; i < n; i++) {
                final double angle = startAngle + i * angleStep;
                final float x = cx + (float) (midR * Math.cos(angle));
                final float y = cy + (float) (midR * Math.sin(angle));
                if (i == 0) {
                    zonePath.moveTo(x, y);
                } else {
                    zonePath.lineTo(x, y);
                }
            }
            zonePath.close();
            canvas.drawPath(zonePath, badZonePaint);

            // Good zone: midpoint to outer edge - green tint
            // Draw as full polygon minus inner polygon using clip
            goodZonePaint.setShader(new RadialGradient(cx, cy, radius,
                    COLOR_GOOD_ZONE_EDGE, COLOR_GOOD_ZONE,
                    Shader.TileMode.CLAMP));
            // Build outer polygon path
            final Path outerPath = new Path();
            for (int i = 0; i < n; i++) {
                final double angle = startAngle + i * angleStep;
                final float x = cx + (float) (radius * Math.cos(angle));
                final float y = cy + (float) (radius * Math.sin(angle));
                if (i == 0) {
                    outerPath.moveTo(x, y);
                } else {
                    outerPath.lineTo(x, y);
                }
            }
            outerPath.close();
            canvas.save();
            canvas.clipPath(outerPath);
            // Draw a big rect with good zone paint, then restore
            final Path innerClip = new Path();
            for (int i = 0; i < n; i++) {
                final double angle = startAngle + i * angleStep;
                final float x = cx + (float) (midR * Math.cos(angle));
                final float y = cy + (float) (midR * Math.sin(angle));
                if (i == 0) {
                    innerClip.moveTo(x, y);
                } else {
                    innerClip.lineTo(x, y);
                }
            }
            innerClip.close();
            // Draw the full outer, the inner clip will be subtracted
            // by drawing good zone over entire outer polygon
            canvas.drawPath(outerPath, goodZonePaint);
            canvas.restore();
        }

        private void drawGrid(final Canvas canvas,
                               final float cx, final float cy,
                               final float radius, final int n,
                               final double angleStep,
                               final double startAngle) {
            final int gridLevels = 5;
            for (int level = 1; level <= gridLevels; level++) {
                final float r = radius * level / gridLevels;
                gridPath.reset();
                for (int i = 0; i < n; i++) {
                    final double angle = startAngle + i * angleStep;
                    final float x = cx + (float) (r * Math.cos(angle));
                    final float y = cy + (float) (r * Math.sin(angle));
                    if (i == 0) {
                        gridPath.moveTo(x, y);
                    } else {
                        gridPath.lineTo(x, y);
                    }
                }
                gridPath.close();
                canvas.drawPath(gridPath,
                        level == gridLevels ? gridPaintOuter : gridPaint);
            }
        }

        private void drawAxes(final Canvas canvas,
                               final float cx, final float cy,
                               final float radius, final int n,
                               final double angleStep,
                               final double startAngle) {
            for (int i = 0; i < n; i++) {
                final double angle = startAngle + i * angleStep;
                final float x = cx + (float) (radius * Math.cos(angle));
                final float y = cy + (float) (radius * Math.sin(angle));
                canvas.drawLine(cx, cy, x, y, axisPaint);
            }
        }

        private void drawMidRing(final Canvas canvas,
                                  final float cx, final float cy,
                                  final float radius, final int n,
                                  final double angleStep,
                                  final double startAngle) {
            final float midNorm = (float) ((RADAR_MID_SCORE - RADAR_MIN_SCORE)
                    / (RADAR_MAX_SCORE - RADAR_MIN_SCORE));
            final float midR = radius * midNorm;

            // Glow behind the ring
            gridPath.reset();
            for (int i = 0; i < n; i++) {
                final double angle = startAngle + i * angleStep;
                final float x = cx + (float) (midR * Math.cos(angle));
                final float y = cy + (float) (midR * Math.sin(angle));
                if (i == 0) {
                    gridPath.moveTo(x, y);
                } else {
                    gridPath.lineTo(x, y);
                }
            }
            gridPath.close();
            canvas.drawPath(gridPath, midRingGlowPaint);
            canvas.drawPath(gridPath, midRingPaint);

            // Zone labels along one axis
            final float badLabelR = midR * 0.5f;
            zoneLabelPaint.setColor(Color.parseColor("#66EF5350"));
            canvas.drawText("BAD", cx, cy + badLabelR
                    + zoneLabelPaint.getTextSize() / 3f, zoneLabelPaint);

            final float goodLabelR = midR + (radius - midR) * 0.5f;
            zoneLabelPaint.setColor(Color.parseColor("#5566BB6A"));
            canvas.drawText("GOOD", cx, cy + goodLabelR
                    + zoneLabelPaint.getTextSize() / 3f, zoneLabelPaint);
        }

        private void drawDataArea(final Canvas canvas,
                                   final float cx, final float cy,
                                   final float radius, final int n,
                                   final double angleStep,
                                   final double startAngle) {
            dataPath.reset();
            for (int i = 0; i < n; i++) {
                final double angle = startAngle + i * angleStep;
                final double score = entries.get(i).score;
                final double norm = Math.max(0, Math.min(1.0,
                        (score - RADAR_MIN_SCORE)
                                / (RADAR_MAX_SCORE - RADAR_MIN_SCORE)));
                final float r = (float) (radius * norm);
                final float x = cx + (float) (r * Math.cos(angle));
                final float y = cy + (float) (r * Math.sin(angle));
                if (i == 0) {
                    dataPath.moveTo(x, y);
                } else {
                    dataPath.lineTo(x, y);
                }
            }
            dataPath.close();

            // Fill with gradient - gold tones
            fillPaint.setShader(new RadialGradient(cx, cy, radius,
                    Color.parseColor("#33FFCA1D"),
                    Color.parseColor("#0DFFCA1D"),
                    Shader.TileMode.CLAMP));
            canvas.drawPath(dataPath, fillPaint);

            // Glow then stroke
            canvas.drawPath(dataPath, glowPaint);
            canvas.drawPath(dataPath, strokePaint);
        }

        private void drawDotsAndLabels(final Canvas canvas,
                                        final float cx, final float cy,
                                        final float radius, final int n,
                                        final double angleStep,
                                        final double startAngle) {
            final int halfIcon = iconSizePx / 2;
            for (int i = 0; i < n; i++) {
                final double angle = startAngle + i * angleStep;
                final double score = entries.get(i).score;
                final double norm = Math.max(0, Math.min(1.0,
                        (score - RADAR_MIN_SCORE)
                                / (RADAR_MAX_SCORE - RADAR_MIN_SCORE)));
                final float r = (float) (radius * norm);
                final float dx = cx + (float) (r * Math.cos(angle));
                final float dy = cy + (float) (r * Math.sin(angle));

                final boolean isBad = score < RADAR_MID_SCORE;

                // Glow -> dot -> outline
                canvas.drawCircle(dx, dy, dpF(7),
                        isBad ? dotGlowBadPaint : dotGlowPaint);
                canvas.drawCircle(dx, dy, dpF(3.5f),
                        isBad ? dotBadPaint : dotPaint);
                canvas.drawCircle(dx, dy, dpF(3.5f),
                        isBad ? dotBadOutlinePaint : dotOutlinePaint);

                // Icon
                final float iconR = radius + dpF(20);
                final float ix = cx + (float) (iconR * Math.cos(angle));
                final float iy = cy + (float) (iconR * Math.sin(angle));

                final android.graphics.drawable.Drawable icon =
                        i < icons.size() ? icons.get(i) : null;
                if (icon != null) {
                    canvas.save();
                    canvas.translate(ix - halfIcon, iy - halfIcon);
                    icon.draw(canvas);
                    canvas.restore();
                }

                // Score value
                final float valR = radius + dpF(40);
                final float vx = cx + (float) (valR * Math.cos(angle));
                final float vy = cy + (float) (valR * Math.sin(angle));

                canvas.drawText(
                        String.format(Locale.US, "%.1f", score),
                        vx, vy + valuePaint.getTextSize() / 3f,
                        isBad ? valueBadPaint : valuePaint);
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
