package com.ts.app.newenergy.view.custom;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.ts.app.newenergy.R;
import com.ts.app.newenergy.utils.Constants;
import com.ts.app.newenergy.utils.FontCache;
/**
 * CustomFontTextView class.
 *
 * @author huiluo
 * @version 1.0
 */
public class CustomFontTextView extends AppCompatTextView {
    public CustomFontTextView(Context context) {
        this(context, null);
    }
    public CustomFontTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    /**
     * initial attributes.
     *
     * @param context      the context
     * @param attrs        the AttributeSet
     * @param defStyleAttr the defStyleAttr
     */
    public CustomFontTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCustomFontTextView(context, attrs);
        setIncludeFontPadding(false);
    }
    private void initCustomFontTextView(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView);
        String fontName = typedArray.getString(R.styleable.CustomFontTextView_fontType);
        Typeface typeface = selectTypeFace(context, fontName);
        setTypeface(typeface);
        typedArray.recycle();
    }
    private Typeface selectTypeFace(Context context, String fontName) {
        if (!TextUtils.isEmpty(fontName)) {
            switch (fontName) {
                case Constants.FONT_ROBOTO_LIGHT:
                    return FontCache.getTypeface(context);
                case Constants.FONT_ROBOTO_REGULAR:
                    return FontCache.getTypeface(context);
                case Constants.FONT_SOURCEHSC_BOLD:
                    return FontCache.getTypeface(context);
                case Constants.FONT_SOURCEHSC_LIGHT:
                    return FontCache.getTypeface(context);
                case Constants.FONT_SOURCEHSC_MEDIUM:
                    return FontCache.getTypeface(context);
                case Constants.FONT_SOURCEHSC_NORMAL:
                    return FontCache.getTypeface(context);
                default:
                    return Typeface.DEFAULT;
            }
        }
        return Typeface.DEFAULT;
    }
}
