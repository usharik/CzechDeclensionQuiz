package com.usharik.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.usharik.app.R;

// Custom layout that wraps child views to a new line
public class FlowLayout extends ViewGroup {

    private int marginHorizontal;
    private int marginVertical;

    public FlowLayout(Context context) {
        super(context);
        init();
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() { // Specify the margins for the children
        marginHorizontal = getResources().getDimensionPixelSize(R.dimen.activity_half_horizontal_margin);
        marginVertical = getResources().getDimensionPixelSize(R.dimen.activity_half_vertical_margin);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        int lowestBottom = 0;
        int lineHeight = 0;
        int myWidth = resolveSize(100, widthMeasureSpec);
        int wantedHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            child.measure(getChildMeasureSpec(widthMeasureSpec, 0, child.getLayoutParams().width),
                    getChildMeasureSpec(heightMeasureSpec, 0, child.getLayoutParams().height));
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            lineHeight = Math.max(childHeight, lineHeight);

            if (childWidth + childLeft + getPaddingRight() > myWidth) { // Wrap this line
                childLeft = getPaddingLeft();
                childTop = marginVertical + lowestBottom; // Spaced below the previous lowest point
                lineHeight = childHeight;
            }
            childLeft += childWidth + marginHorizontal;

            if (childHeight + childTop > lowestBottom) { // New lowest point
                lowestBottom = childHeight + childTop;
            }
        }

        wantedHeight += childTop + lineHeight + getPaddingBottom();
        setMeasuredDimension(myWidth, resolveSize(wantedHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        int lowestBottom = 0;
        int myWidth = right - left;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (childWidth + childLeft + getPaddingRight() > myWidth) { // Wrap this line
                childLeft = getPaddingLeft();
                childTop = marginVertical + lowestBottom; // Spaced below the previous lowest point
            }
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            childLeft += childWidth + marginHorizontal;

            if (childHeight + childTop > lowestBottom) { // New lowest point
                lowestBottom = childHeight + childTop;
            }
        }
    }
}
