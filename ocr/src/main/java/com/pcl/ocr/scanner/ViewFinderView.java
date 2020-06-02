package com.pcl.ocr.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.pcl.ocr.utils.CameraUtils;

import static com.pcl.ocr.utils.CameraUtils.MAX_FRAME_HEIGHT;

/**
 * @auther : Aleyn
 * time   : 2019/05/05
 */
final class ViewFinderView extends View {

    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int POINT_SIZE = 6;

    private final Paint paint;
    private Bitmap resultBitmap;

    private int animationDelay = 0;
    private Bitmap laserLineBitmap;

    private int resultColor = Scanner.color.RESULT_VIEW;//扫描成功后扫描框以外区域白色
    private int laserLineTop;// 扫描线最顶端位置

    private int laserLineHeight;//扫描线默认高度
    private int frameCornerWidth;//扫描框4角宽
    private int frameCornerLength;//扫描框4角高
    private Rect frameRect;//扫描框大小
    private int tipTextSize;//提示文字大小
    private int tipTextMargin;//提示文字与扫描框距离
    private boolean isDown = true; //滚动线向下滚动标识
    // 屏幕分辨率
    private Point screenResolution;
    private ScannerOptions scannerOptions;

    public ViewFinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    void setScannerOptions(ScannerOptions scannerOptions) {
        this.scannerOptions = scannerOptions;
        laserLineHeight = dp2px(scannerOptions.getLaserLineHeight());
        frameCornerWidth = dp2px(scannerOptions.getFrameCornerWidth());
        frameCornerLength = dp2px(scannerOptions.getFrameCornerLength());

        tipTextSize = Scanner.sp2px(getContext(), scannerOptions.getTipTextSize());
        tipTextMargin = dp2px(scannerOptions.getTipTextToFrameMargin());
        screenResolution = CameraUtils.getScreenResolution(getContext());
        getFramingRect();//取扫描框
    }

    private int dp2px(int dp) {
        return Scanner.dp2px(getContext(), dp);
    }

    Rect getFramingRect() {
        if (frameRect == null) {
            int height;
            int width = 9 * screenResolution.x / 10;
            height = width / 3;
            if (height > MAX_FRAME_HEIGHT) height = MAX_FRAME_HEIGHT;
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            int top = dp2px(scannerOptions.getFrameTopMargin());
            if (top == 0)
                top = topOffset - getStatusBarHeight();
            else {
                top += getStatusBarHeight();
            }
            frameRect = new Rect(leftOffset, top, leftOffset + width, top + height);
        }
        return frameRect;
    }

    Point getScreenResolution() {
        return screenResolution;
    }

    /**
     * 获取状态栏高度
     *
     * @return
     */
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = frameRect;//取扫描框
        //取屏幕预览
        if (frame == null) {
            return;
        }
        //全屏不绘制扫描框以外4个区域
        if (!scannerOptions.isScanFullScreen()) {
            drawMask(canvas, frame);
        }
        // 如果有二维码结果的Bitmap，在扫取景框内绘制不透明的result Bitmap
        if (resultBitmap != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            if (!scannerOptions.isFrameHide()) drawFrame(canvas, frame);//绘制扫描框
            if (!scannerOptions.isFrameCornerHide()) drawFrameCorner(canvas, frame);//绘制扫描框4角
            drawText(canvas, frame);// 画扫描框下面的字

            //全屏移动扫描线
            if (scannerOptions.isLaserMoveFullScreen()) {
                moveLaserSpeedFullScreen(screenResolution);//计算全屏移动位置
                drawLaserLineFullScreen(canvas, screenResolution);//绘制全屏扫描线
            } else {
                if (!scannerOptions.isFrameLineHide()) {
                    drawLaserLine(canvas, frame);//绘制扫描框内扫描线
                    moveLaserSpeed(frame);//计算扫描框内移动位置
                }
            }
            if (scannerOptions.getViewfinderCallback() != null) {
                scannerOptions.getViewfinderCallback().onDraw(this, canvas, frame);
            }
        }
    }

    private void moveLaserSpeed(Rect frame) {
        //初始化扫描线起始点为扫描框顶部位置
        if (laserLineTop == 0) {
            laserLineTop = frame.top;
        }
        int laserMoveSpeed = scannerOptions.getLaserLineMoveSpeed();
        // 每次刷新界面，扫描线往下移动 LASER_VELOCITY
        /*laserLineTop += laserMoveSpeed;
        if (laserLineTop < frame.bottom) {
            laserLineTop = frame.top;
        } else if (laserLineTop >= frame.bottom) {

        }*/
        if (isDown) {
            laserLineTop += laserMoveSpeed;
            if (laserLineTop >= frame.bottom)
                isDown = false;
        } else {
            laserLineTop -= laserMoveSpeed;
            if (laserLineTop <= frame.top)
                isDown = true;
        }
        if (animationDelay == 0) {
            animationDelay = (int) ((1.0f * 1000 * laserMoveSpeed) / (frame.bottom - frame.top));
        }

        // 只刷新扫描框的内容，其他地方不刷新
        postInvalidateDelayed(animationDelay, frame.left - POINT_SIZE, frame.top - POINT_SIZE
                , frame.right + POINT_SIZE, frame.bottom + POINT_SIZE);
    }

    private void moveLaserSpeedFullScreen(Point point) {
        //初始化扫描线起始点为顶部位置
        int laserMoveSpeed = scannerOptions.getLaserLineMoveSpeed();
        // 每次刷新界面，扫描线往下移动 LASER_VELOCITY
        laserLineTop += laserMoveSpeed;
        if (laserLineTop >= point.y) {
            laserLineTop = 0;
        }
        if (animationDelay == 0) {
            animationDelay = (int) ((1.0f * 1000 * laserMoveSpeed) / point.y);
        }
        postInvalidateDelayed(animationDelay);
    }

    /**
     * 画扫描框外区域
     */
    private void drawMask(Canvas canvas, Rect frame) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        paint.setColor(resultBitmap != null ? resultColor : scannerOptions.getFrameOutsideColor());
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    /**
     * 绘制提示文字
     */
    private void drawText(Canvas canvas, Rect frame) {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(scannerOptions.getTipTextColor());
        textPaint.setTextSize(tipTextSize);

        float x = frame.left;//文字开始位置
        //根据 drawTextGravityBottom 文字在扫描框上方还是上文，默认下方
        float y = !scannerOptions.isTipTextToFrameTop() ? frame.bottom + tipTextMargin
                : frame.top - tipTextMargin;

        StaticLayout staticLayout = new StaticLayout(scannerOptions.getTipText(), textPaint, frame.width()
                , Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * 绘制扫描框4角
     */
    private void drawFrameCorner(Canvas canvas, Rect frame) {
        paint.setColor(scannerOptions.getFrameCornerColor());
        paint.setStyle(Paint.Style.FILL);
        if (scannerOptions.isFrameCornerInside()) {
            // 左上角，左
            canvas.drawRect(frame.left, frame.top, frame.left + frameCornerWidth, frame.top + frameCornerLength, paint);
            // 左上角，上
            canvas.drawRect(frame.left, frame.top, frame.left + frameCornerLength, frame.top + frameCornerWidth, paint);
            // 右上角，右
            canvas.drawRect(frame.right - frameCornerWidth, frame.top, frame.right, frame.top + frameCornerLength, paint);
            // 右上角，上
            canvas.drawRect(frame.right - frameCornerLength, frame.top, frame.right, frame.top + frameCornerWidth, paint);
            // 左下角，左
            canvas.drawRect(frame.left, frame.bottom - frameCornerLength, frame.left + frameCornerWidth, frame.bottom, paint);
            // 左下角，下
            canvas.drawRect(frame.left, frame.bottom - frameCornerWidth, frame.left + frameCornerLength, frame.bottom, paint);
            // 右下角，右
            canvas.drawRect(frame.right - frameCornerWidth, frame.bottom - frameCornerLength, frame.right, frame.bottom, paint);
            // 右下角，下
            canvas.drawRect(frame.right - frameCornerLength, frame.bottom - frameCornerWidth, frame.right, frame.bottom, paint);
        } else {
            // 左上角
            canvas.drawRect(frame.left - frameCornerWidth, frame.top, frame.left, frame.top + frameCornerLength, paint);
            canvas.drawRect(frame.left - frameCornerWidth, frame.top - frameCornerWidth, frame.left + frameCornerLength, frame.top, paint);
            // 右上角
            canvas.drawRect(frame.right, frame.top, frame.right + frameCornerWidth, frame.top + frameCornerLength, paint);
            canvas.drawRect(frame.right - frameCornerLength, frame.top - frameCornerWidth, frame.right + frameCornerWidth, frame.top, paint);
            // 左下角
            canvas.drawRect(frame.left - frameCornerWidth, frame.bottom - frameCornerLength, frame.left, frame.bottom, paint);
            canvas.drawRect(frame.left - frameCornerWidth, frame.bottom, frame.left + frameCornerLength, frame.bottom + frameCornerWidth, paint);
            // 右下角
            canvas.drawRect(frame.right, frame.bottom - frameCornerLength, frame.right + frameCornerWidth, frame.bottom, paint);
            canvas.drawRect(frame.right - frameCornerLength, frame.bottom, frame.right + frameCornerWidth, frame.bottom + frameCornerWidth, paint);
        }
    }

    /**
     * 画扫描框
     */
    private void drawFrame(Canvas canvas, Rect frame) {
        paint.setColor(scannerOptions.getFrameStrokeColor());//扫描边框色
        paint.setStrokeWidth(scannerOptions.getFrameStrokeWidth());
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(frame, paint);
    }

    /**
     * 画扫描线
     */
    private void drawLaserLine(Canvas canvas, Rect frame) {
        if (scannerOptions.getLaserStyle() == ScannerOptions.LaserStyle.COLOR_LINE) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(scannerOptions.getLaserLineColor());// 设置扫描线颜色
            canvas.drawRect(frame.left, laserLineTop, frame.right
                    , laserLineTop + laserLineHeight, paint);
        } else {
            if (laserLineBitmap == null)//图片资源文件转为 Bitmap
                laserLineBitmap = BitmapFactory.decodeResource(getResources(), scannerOptions.getLaserLineResId());
            int height = laserLineBitmap.getHeight();//取原图高
            //网格图片
            if (scannerOptions.getLaserStyle() == ScannerOptions.LaserStyle.RES_GRID) {
                RectF dstRectF = new RectF(frame.left, frame.top, frame.right, laserLineTop);
                Rect srcRect = new Rect(0, (int) (height - dstRectF.height())
                        , laserLineBitmap.getWidth(), height);
                canvas.drawBitmap(laserLineBitmap, srcRect, dstRectF, paint);
            }
            //线条图片
            else {
                //如果没有设置线条高度，则用图片原始高度
                if (laserLineHeight == dp2px(ScannerOptions.DEFAULT_LASER_LINE_HEIGHT)) {
                    laserLineHeight = laserLineBitmap.getHeight() / 2;
                }
                Rect laserRect = new Rect(frame.left, laserLineTop, frame.right
                        , laserLineTop + laserLineHeight);
                canvas.drawBitmap(laserLineBitmap, null, laserRect, paint);
            }
        }
    }


    /**
     * 画全屏宽扫描线
     */
    private void drawLaserLineFullScreen(Canvas canvas, Point point) {
        if (scannerOptions.getLaserStyle() == ScannerOptions.LaserStyle.COLOR_LINE) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(scannerOptions.getLaserLineColor());// 设置扫描线颜色
            canvas.drawRect(0, laserLineTop, point.x, laserLineTop + laserLineHeight, paint);
        } else {
            if (laserLineBitmap == null)//图片资源文件转为 Bitmap
                laserLineBitmap = BitmapFactory.decodeResource(getResources(), scannerOptions.getLaserLineResId());
            int height = laserLineBitmap.getHeight();//取原图高
            //网格图片
            if (scannerOptions.getLaserStyle() == ScannerOptions.LaserStyle.RES_GRID) {
                int dstRectFTop = 0;
                if (laserLineTop >= height) {
                    dstRectFTop = laserLineTop - height;
                }
                RectF dstRectF = new RectF(0, dstRectFTop, point.x, laserLineTop);
                Rect srcRect = new Rect(0, (int) (height - dstRectF.height()), laserLineBitmap.getWidth(), height);
                canvas.drawBitmap(laserLineBitmap, srcRect, dstRectF, paint);
            }
            //线条图片
            else {
                //如果没有设置线条高度，则用图片原始高度
                if (laserLineHeight == dp2px(ScannerOptions.DEFAULT_LASER_LINE_HEIGHT)) {
                    laserLineHeight = laserLineBitmap.getHeight() / 2;
                }
                Rect laserRect = new Rect(0, laserLineTop, point.x, laserLineTop + laserLineHeight);
                canvas.drawBitmap(laserLineBitmap, null, laserRect, paint);
            }
        }
    }
}
