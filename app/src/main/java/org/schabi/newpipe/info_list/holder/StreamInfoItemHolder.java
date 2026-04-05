package org.schabi.newpipe.info_list.holder;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.info_list.InfoItemBuilder;
import org.schabi.newpipe.local.history.HistoryRecordManager;
import org.schabi.newpipe.util.Localization;

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

        String details = getStreamInfoDetailLine(item);
        if (item.getTournesolScore() != null && !TextUtils.isEmpty(details)) {
            details = "\u2022 " + details;
        }

        final String socialProof = buildSocialProofText(item);
        if (!TextUtils.isEmpty(socialProof)) {
            if (!TextUtils.isEmpty(details)) {
                details = details + " \u2022 " + socialProof;
            } else {
                details = socialProof;
            }
        }

        itemAdditionalDetails.setText(details);
        itemAdditionalDetails.setVisibility(
                TextUtils.isEmpty(details) ? View.GONE : View.VISIBLE);

        bindInlineCriteria(item);
    }

    private String getStreamInfoDetailLine(final StreamInfoItem infoItem) {
        String viewsAndDate = "";
        if (infoItem.getViewCount() >= 0) {
            if (infoItem.getStreamType().equals(StreamType.AUDIO_LIVE_STREAM)) {
                viewsAndDate = Localization
                        .listeningCount(itemBuilder.getContext(), infoItem.getViewCount());
            } else if (infoItem.getStreamType().equals(StreamType.LIVE_STREAM)) {
                viewsAndDate = Localization
                        .shortWatchingCount(itemBuilder.getContext(), infoItem.getViewCount());
            } else {
                viewsAndDate = Localization
                        .shortViewCount(itemBuilder.getContext(), infoItem.getViewCount());
            }
        }

        final String uploadDate = Localization.relativeTimeOrTextual(itemBuilder.getContext(),
                infoItem.getUploadDate(),
                infoItem.getTextualUploadDate());
        if (!TextUtils.isEmpty(uploadDate)) {
            if (viewsAndDate.isEmpty()) {
                return uploadDate;
            }

            return Localization.concatenateStrings(viewsAndDate, uploadDate);
        }

        return viewsAndDate;
    }

    private String buildSocialProofText(final StreamInfoItem item) {
        final int nComparisons = item.getTournesolNComparisons();
        if (nComparisons >= 0) {
            return nComparisons + " votes";
        }
        return "";
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
