package com.usharik.app.widget;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.View;

public class CustomDragShadowBuilder extends View.DragShadowBuilder {

    private Point scaleFactor;
    private float scale;

    public CustomDragShadowBuilder(View v, float scale) {
        super(v);
        this.scale = scale;
    }

    @Override
    public void onProvideShadowMetrics (Point size, Point touch) {
        int width = Math.round(getView().getWidth() * scale);
        int height = Math.round(getView().getHeight() * scale);
        size.set(width, height);
        scaleFactor = size;
        touch.set(width / 2, height / 2);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        canvas.scale(scaleFactor.x/(float)getView().getWidth(), scaleFactor.y/(float)getView().getHeight());
        getView().draw(canvas);
    }

}
