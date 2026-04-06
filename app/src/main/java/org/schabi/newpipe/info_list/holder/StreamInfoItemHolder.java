package org.schabi.newpipe.info_list.holder;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.info_list.InfoItemBuilder;
import org.schabi.newpipe.local.history.HistoryRecordManager;
import org.schabi.newpipe.util.Localization;
import org.schabi.newpipe.util.TournesolHelper;

/*
 * Created by Christian Schabesberger on 01.08.16.
 * <p>
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * StreamInfoItemHolder.java is part of NewPipe.
 * </p>
 * <p>
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 * <p>
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p?
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with NewPipe. If not, see <http://www.gnu.org/licenses/>.
 * </p>
 */

public class StreamInfoItemHolder extends StreamMiniInfoItemHolder {
    public final TextView itemAdditionalDetails;
    private final TextView itemInlineBestArrow;
    private final ImageView itemInlineBestIcon;
    private final TextView itemInlineWorstArrow;
    private final ImageView itemInlineWorstIcon;

    public StreamInfoItemHolder(final InfoItemBuilder infoItemBuilder, final ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_stream_item, parent);
    }

    public StreamInfoItemHolder(final InfoItemBuilder infoItemBuilder, final int layoutId,
                                final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
        itemAdditionalDetails = itemView.findViewById(R.id.itemAdditionalDetails);
        itemInlineBestArrow = itemView.findViewById(R.id.itemInlineBestArrow);
        itemInlineBestIcon = itemView.findViewById(R.id.itemInlineBestIcon);
        itemInlineWorstArrow = itemView.findViewById(R.id.itemInlineWorstArrow);
        itemInlineWorstIcon = itemView.findViewById(R.id.itemInlineWorstIcon);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem,
                               final HistoryRecordManager historyRecordManager) {
        super.updateFromItem(infoItem, historyRecordManager);

        if (!(infoItem instanceof StreamInfoItem)) {
            return;
        }
        final StreamInfoItem item = (StreamInfoItem) infoItem;

        final SpannableStringBuilder details = buildCompactDetailLine(item);

        if (details.length() > 0) {
            itemAdditionalDetails.setText(details);
            itemAdditionalDetails.setVisibility(View.VISIBLE);
        } else {
            itemAdditionalDetails.setVisibility(View.GONE);
        }

        bindInlineCriteria(item);
    }

    private SpannableStringBuilder buildCompactDetailLine(final StreamInfoItem item) {
        final Context context = itemBuilder.getContext();
        final int iconSize = (int) itemAdditionalDetails.getTextSize();
        final int textColor = itemAdditionalDetails.getCurrentTextColor();
        final SpannableStringBuilder sb = new SpannableStringBuilder();

        // Tournesol score: logo icon (or plant emoji if insufficient) + score (accent yellow)
        final Long tournesolScore = item.getTournesolScore();
        if (tournesolScore != null) {
            final boolean hasInsufficientReason = TournesolHelper.hasInsufficientReason(
                    item.getTournesolUnsafeReasons());
            if (hasInsufficientReason) {
                sb.append("\uD83C\uDF31\u2009");
                final int textStart = sb.length();
                sb.append(Long.toString(tournesolScore));
                sb.setSpan(new ForegroundColorSpan(0xFFD1B65C),
                        textStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                appendIconAndText(sb, context, R.drawable.logo_small, iconSize,
                        Long.toString(tournesolScore), null, 0xFFD1B65C);
            }
        }

        // Views: eye icon + compact count
        if (item.getViewCount() >= 0) {
            final String viewText;
            if (item.getStreamType().equals(StreamType.AUDIO_LIVE_STREAM)) {
                viewText = Localization.listeningCount(context, item.getViewCount());
            } else if (item.getStreamType().equals(StreamType.LIVE_STREAM)) {
                viewText = Localization.shortWatchingCount(context, item.getViewCount());
            } else {
                viewText = Localization.shortCount(context, item.getViewCount());
            }
            appendIconAndText(sb, context, R.drawable.ic_visibility_on, iconSize,
                    viewText, textColor);
        }

        // Date: clock icon + compact relative time
        final String uploadDate = Localization.compactRelativeTimeOrTextual(
                item.getUploadDate(), item.getTextualUploadDate());
        if (!TextUtils.isEmpty(uploadDate)) {
            if (sb.length() > 0) {
                sb.append("  ");
            }
            appendIconAndText(sb, context, R.drawable.ic_watch_later, iconSize,
                    uploadDate, textColor);
        }

        // Votes: balance scale emoji + count
        final int nComparisons = item.getTournesolNComparisons();
        if (nComparisons >= 0) {
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append("\u2696\uFE0F\u2009");
            sb.append(String.valueOf(nComparisons));
        }

        return sb;
    }

    private static void appendIconAndText(final SpannableStringBuilder sb,
                                          final Context context,
                                          @DrawableRes final int iconRes,
                                          final int iconSize,
                                          final String text,
                                          final int tintColor) {
        appendIconAndText(sb, context, iconRes, iconSize, text, (Integer) tintColor, null);
    }

    private static void appendIconAndText(final SpannableStringBuilder sb,
                                          final Context context,
                                          @DrawableRes final int iconRes,
                                          final int iconSize,
                                          final String text,
                                          @Nullable final Integer iconTint,
                                          @Nullable final Integer textColor) {
        final Drawable icon = ContextCompat.getDrawable(context, iconRes);
        if (icon != null) {
            icon.mutate();
            if (iconTint != null) {
                icon.setTint(iconTint);
            }
            icon.setBounds(0, 0, iconSize, iconSize);
            sb.append(" ");
            sb.setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE),
                    sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append("\u2009");
        }
        final int textStart = sb.length();
        sb.append(text);
        if (textColor != null) {
            sb.setSpan(new ForegroundColorSpan(textColor),
                    textStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void bindInlineCriteria(final StreamInfoItem item) {
        final String bestCriteria = item.getTournesolBestCriteria();
        final String worstCriteria = item.getTournesolWorstCriteria();
        final Integer bestIconRes = getCriteriaIcon(bestCriteria);
        final Integer worstIconRes = getCriteriaIcon(worstCriteria);

        // Inline best criteria (right of uploader line)
        if (itemInlineBestArrow != null && itemInlineBestIcon != null) {
            if (bestIconRes != null) {
                itemInlineBestArrow.setVisibility(View.VISIBLE);
                itemInlineBestIcon.setImageResource(bestIconRes);
                itemInlineBestIcon.setVisibility(View.VISIBLE);
            } else {
                itemInlineBestArrow.setVisibility(View.GONE);
                itemInlineBestIcon.setVisibility(View.GONE);
            }
        }

        // Inline worst criteria (right of additional details line)
        if (itemInlineWorstArrow != null && itemInlineWorstIcon != null) {
            if (worstIconRes != null) {
                itemInlineWorstArrow.setVisibility(View.VISIBLE);
                itemInlineWorstIcon.setImageResource(worstIconRes);
                itemInlineWorstIcon.setVisibility(View.VISIBLE);
            } else {
                itemInlineWorstArrow.setVisibility(View.GONE);
                itemInlineWorstIcon.setVisibility(View.GONE);
            }
        }
    }

    @Nullable
    @DrawableRes
    private static Integer getCriteriaIcon(@Nullable final String criteria) {
        if (criteria == null) {
            return null;
        }
        switch (criteria) {
            case "reliability": return R.drawable.reliability;
            case "pedagogy": return R.drawable.pedagogy;
            case "importance": return R.drawable.importance;
            case "layman_friendly": return R.drawable.layman_friendly;
            case "entertaining_relaxing": return R.drawable.entertaining_relaxing;
            case "engaging": return R.drawable.engaging;
            case "diversity_inclusion": return R.drawable.diversity_inclusion;
            case "better_habits": return R.drawable.better_habits;
            case "backfire_risk": return R.drawable.backfire_risk;
            default: return null;
        }
    }
}
