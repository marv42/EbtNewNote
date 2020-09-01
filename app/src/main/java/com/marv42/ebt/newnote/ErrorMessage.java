package com.marv42.ebt.newnote;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorMessage {
    public static final String ERROR = "ERROR";

    private Context mContext;

    ErrorMessage(Context context) {
        mContext = context;
    }

    String getErrorMessage(@NonNull String text) {
        if (! text.startsWith(ERROR))
            return text;
        text = text.substring(ERROR.length());

        Pattern pattern = Pattern.compile("R\\.string\\.(\\w+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1);
            int resId = mContext.getResources().getIdentifier(name, "string",
                    mContext.getPackageName());
            String target = matcher.group();
            text = text.replace(target, mContext.getString(resId));
        }
        return text;
    }
}
