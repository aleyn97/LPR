package com.pcl.lpr.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.pcl.lpr.R;

/**
 * @auther : Aleyn
 * time   : 2019/05/05
 */
public class ViewFinderView extends View {

    //相机遮罩框外面的线，阴影区域，滚动线
    private Paint border, area, line;
    //相机遮罩框中间透明区域
    public Rect center;
    //屏幕大小
    private int screenHeight, screenWidth;
    //滚动线的起始点
    private int startX, startY, endX, endY;
    //滚动线向下滚动标识
    private boolean isDown = true;
    //滚动线速度
    private static final int SPEED = 6;
    //中间区域宽高（dp），
    public int centerHeight;
    public int centerWidth;


    public ViewFinderView(Context context) {
        super(context, null);
    }

    public ViewFinderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //setAlpha一定要在setStyle后面，否则不起作用
        border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.BLUE);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(5f);
        border.setAlpha(10);

        area = new Paint(Paint.ANTI_ALIAS_FLAG);
        area.setStyle(Paint.Style.FILL);
        area.setColor(Color.BLACK);
        area.setAlpha(180);

        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        centerHeight = 500;
        centerWidth = screenWidth - 100;
        center = getCenterRect(centerHeight, centerWidth);

        line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setColor(Color.GREEN);
        //设置滚动线的起始点
        startX = center.left;
        startY = center.top;
        endX = center.right;
        endY = center.top;
    }

    /**
     * 根据尺寸获取中间区大小
     *
     * @param height
     * @param width
     * @return
     */
    private Rect getCenterRect(int height, int width) {
        Rect rect = new Rect();
        int left = (this.screenWidth - width) / 2;
        int top = (this.screenHeight - height) / 2;
        rect.set(left, top, left + width, top + height);
        return rect;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制四周阴影区域（上下左右），注意+1和-1，不设置不显示边框
        canvas.drawRect(0, 0, screenWidth, center.top - 1, area);
        canvas.drawRect(0, center.bottom + 1, screenWidth, screenHeight, area);
        canvas.drawRect(0, center.top - 1, center.left - 1, center.bottom + 1, area);
        canvas.drawRect(center.right + 1, center.top - 1, screenWidth, center.bottom + 1, area);

        canvas.drawRect(center, border);

        //滚动线
        if (isDown) {
            startY = endY += SPEED;
            if (startY >= center.bottom)
                isDown = false;
        } else {
            startY = endY -= SPEED;
            if (startY <= center.top)
                isDown = true;
        }
        canvas.drawLine(startX, startY, endX, endY, line);
        postInvalidate();
    }

}
