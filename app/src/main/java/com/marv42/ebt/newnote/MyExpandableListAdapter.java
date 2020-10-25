/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TableRow;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static android.view.View.GONE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static androidx.core.text.HtmlCompat.fromHtml;
import static com.marv42.ebt.newnote.ResultsFragment.DENOMINATION_IMAGE;

public class MyExpandableListAdapter extends SimpleExpandableListAdapter {

    public static final int DENOMINATION_IMAGE_WIDTH = 55;
    public static final int DENOMINATION_IMAGE_MARGIN = 10;
    private final LayoutInflater layoutInflater;
    private final boolean showImages;
    private final List<? extends Map<String, ?>> groupData;
    private final String[] groupFrom;
    private final int[] groupTo;
    private final List<? extends List<? extends Map<String, ?>>> childData;
    private final String[] childFrom;
    private final int[] childTo;

    MyExpandableListAdapter(LayoutInflater layoutInflater, boolean showImages,
                            List<? extends Map<String, ?>> groupData,
                            String[] groupFrom,
                            int[] groupTo,
                            List<? extends List<? extends Map<String, ?>>> childData,
                            String[] childFrom,
                            int[] childTo) {
        super(layoutInflater.getContext(), groupData, R.layout.list_parents, groupFrom, groupTo,
                childData, R.layout.list_children, childFrom, childTo);
        this.layoutInflater = layoutInflater;
        this.showImages = showImages;
        this.groupData = groupData;
        this.groupFrom = groupFrom;
        this.groupTo = groupTo;
        this.childData = childData;
        this.childFrom = childFrom;
        this.childTo = childTo;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View v = layoutInflater.inflate(R.layout.list_parents, parent, false);
        bindView(v, groupData.get(groupPosition), groupFrom, groupTo);
        checkShowImages(v);
        return v;
    }

    private void checkShowImages(View view) {
        if (showImages) {
            ImageView denominationImage = view.findViewById(R.id.list_denomination_image);
            TableRow.LayoutParams params = getLayoutParams();
            denominationImage.setLayoutParams(params);
        }
    }

    @NotNull
    private TableRow.LayoutParams getLayoutParams() {
        TableRow.LayoutParams params = new TableRow.LayoutParams(DENOMINATION_IMAGE_WIDTH, MATCH_PARENT);
        final int margin = getMargin();
        params.setMargins(margin, margin, margin, margin);
        return params;
    }

    private int getMargin() {
        // https://developer.android.com/training/multiscreen/screendensities#dips-pels
        final float scale = layoutInflater.getContext().getResources().getDisplayMetrics().density;
        return (int) (DENOMINATION_IMAGE_MARGIN * scale);
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        View v = layoutInflater.inflate(R.layout.list_children, parent, false);
        bindView(v, childData.get(groupPosition).get(childPosition), childFrom, childTo);
        return v;
    }

    private void bindView(View view, Map<String, ?> data, String[] from, int[] to) {
        for (int i = 0; i < to.length; ++i) {
            String dataFromI = (String) data.get(from[i]);
            if (from[i].equals(DENOMINATION_IMAGE))
                loadDenominationImage(view.findViewById(to[i]), dataFromI);
            else
                setTextFromHtml(view.findViewById(to[i]), dataFromI);
        }
    }

    private void loadDenominationImage(ImageView view, String data) {
        if (view != null)
            Picasso.get().load(data).into(view);
    }

    private void setTextFromHtml(TextView view, String data) {
        if (view != null) {
            final Spanned text = fromHtml(data, FROM_HTML_MODE_COMPACT);
            view.setText(text);
            if (TextUtils.isEmpty(data))
                view.setVisibility(GONE);
        }
    }
}
