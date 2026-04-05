package org.schabi.newpipe.info_list.holder;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import android.util.Log;

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
    private final LinearLayout itemTournesolDetails;
    private final TextView itemTournesolSocialProof;
    private final TextView itemTournesolBestArrow;
    private final ImageView itemTournesolBestIcon;
    private final TextView itemTournesolWorstArrow;
    private final ImageView itemTournesolWorstIcon;

    public StreamInfoItemHolder(final InfoItemBuilder infoItemBuilder, final ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_stream_item, parent);
    }

    public StreamInfoItemHolder(final InfoItemBuilder infoItemBuilder, final int layoutId,
                                final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
        itemAdditionalDetails = itemView.findViewById(R.id.itemAdditionalDetails);
        itemTournesolDetails = itemView.findViewById(R.id.itemTournesolDetails);
        itemTournesolSocialProof = itemView.findViewById(R.id.itemTournesolSocialProof);
        itemTournesolBestArrow = itemView.findViewById(R.id.itemTournesolBestArrow);
        itemTournesolBestIcon = itemView.findViewById(R.id.itemTournesolBestIcon);
        itemTournesolWorstArrow = itemView.findViewById(R.id.itemTournesolWorstArrow);
        itemTournesolWorstIcon = itemView.findViewById(R.id.itemTournesolWorstIcon);
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
        itemAdditionalDetails.setText(details);
        itemAdditionalDetails.setVisibility(
                TextUtils.isEmpty(details) ? View.GONE : View.VISIBLE);

        bindTournesolDetails(item);
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

    private void bindTournesolDetails(final StreamInfoItem item) {
        if (itemTournesolDetails == null) {
            return;
        }

        final int nContributors = item.getTournesolNContributors();
        final int nComparisons = item.getTournesolNComparisons();
        final String bestCriteria = item.getTournesolBestCriteria();
        final String worstCriteria = item.getTournesolWorstCriteria();
        Log.d("TournesolDetails", "name=" + item.getName()
                + " nContrib=" + nContributors + " nComp=" + nComparisons
                + " best=" + bestCriteria + " worst=" + worstCriteria);

        final boolean hasContributors = nContributors >= 0;
        final boolean hasComparisons = nComparisons >= 0;
        final Integer bestIconRes = getCriteriaIcon(bestCriteria);
        final Integer worstIconRes = getCriteriaIcon(worstCriteria);

        if (!hasContributors && !hasComparisons && bestIconRes == null && worstIconRes == null) {
            itemTournesolDetails.setVisibility(View.GONE);
            return;
        }

        // Build social proof text
        final StringBuilder sb = new StringBuilder();
        if (hasContributors) {
            sb.append("\uD83D\uDC65 ").append(nContributors);
        }
        if (hasComparisons) {
            if (sb.length() > 0) {
                sb.append(" \u00B7 ");
            }
            sb.append("\u2696\uFE0F ").append(nComparisons);
        }
        itemTournesolSocialProof.setText(sb.toString());
        itemTournesolSocialProof.setVisibility(
                sb.length() > 0 ? View.VISIBLE : View.GONE);

        // Best criteria
        if (bestIconRes != null) {
            itemTournesolBestArrow.setText(
                    (sb.length() > 0 ? " \u00B7 " : "") + "\u25B2");
            itemTournesolBestArrow.setVisibility(View.VISIBLE);
            itemTournesolBestIcon.setImageResource(bestIconRes);
            itemTournesolBestIcon.setVisibility(View.VISIBLE);
        } else {
            itemTournesolBestArrow.setVisibility(View.GONE);
            itemTournesolBestIcon.setVisibility(View.GONE);
        }

        // Worst criteria
        if (worstIconRes != null) {
            itemTournesolWorstArrow.setText(" \u25BC");
            itemTournesolWorstArrow.setVisibility(View.VISIBLE);
            itemTournesolWorstIcon.setImageResource(worstIconRes);
            itemTournesolWorstIcon.setVisibility(View.VISIBLE);
        } else {
            itemTournesolWorstArrow.setVisibility(View.GONE);
            itemTournesolWorstIcon.setVisibility(View.GONE);
        }

        itemTournesolDetails.setVisibility(View.VISIBLE);
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
