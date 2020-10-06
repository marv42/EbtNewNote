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
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

import static android.view.View.GONE;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static androidx.core.text.HtmlCompat.fromHtml;
import static com.marv42.ebt.newnote.SubmittedFragment.DENOMINATION_IMAGE;

public class MyExpandableListAdapter extends SimpleExpandableListAdapter {

    private LayoutInflater layoutInflater;
    private List<? extends Map<String, ?> > groupData;
    private String[] groupFrom;
    private int[] groupTo;
    private List<? extends List<? extends Map<String, ?>>> childData;
    private String[] childFrom;
    private int[] childTo;

    MyExpandableListAdapter(LayoutInflater layoutInflater,
                            List<? extends Map<String, ?> > groupData,
                            String[] groupFrom,
                            int[] groupTo,
                            List<? extends List<? extends Map<String, ?>>> childData,
                            String[] childFrom,
                            int[] childTo) {
        super(layoutInflater.getContext(), groupData, R.layout.list_parents, groupFrom, groupTo,
                childData, R.layout.list_children, childFrom, childTo);
        this.layoutInflater = layoutInflater;
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
        return v;
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

    private void loadDenominationImage(ImageView viewById, String data) {
        if (viewById != null)
            Picasso.get().load(data).into(viewById);
    }

    private void setTextFromHtml(TextView viewById, String data) {
        if (viewById != null) {
            final Spanned text = fromHtml(data, FROM_HTML_MODE_COMPACT);
            viewById.setText(text);
            if (TextUtils.isEmpty(data))
                viewById.setVisibility(GONE);
        }
    }
}
