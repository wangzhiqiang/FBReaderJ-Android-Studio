/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.ui.android.view;

import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;

import org.fbreader.util.Boolean3;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLApplication.ZLAction;
import org.geometerplus.zlibrary.core.application.ZLKeyBindings;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;

import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.view.animation.*;

import org.geometerplus.fbreader.Paths;

public class ZLAndroidWidget extends MainView implements ZLViewWidget, View.OnLongClickListener

{

    public final ExecutorService PrepareService = Executors.newSingleThreadExecutor();

    private final Paint myPaint = new Paint();

    private final BitmapManagerImpl myBitmapManager = new BitmapManagerImpl(this);
    //	private Bitmap myFooterBitmap;
    private final SystemInfo mySystemInfo;

    public ZLAndroidWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mySystemInfo = Paths.systemInfo(context);
        init();
    }

    public ZLAndroidWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        mySystemInfo = Paths.systemInfo(context);
        init();
    }

    public ZLAndroidWidget(Context context) {
        super(context);
        mySystemInfo = Paths.systemInfo(context);
        init();
    }

    private GestureDetector mGestureDetector;

    private void init() {
        // next line prevent ignoring first onKeyDown DPad event
        // after any dialog was closed
        setFocusableInTouchMode(true);
        setDrawingCacheEnabled(false);
        setOnLongClickListener(this);

        mGestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {

            PageTurningOptions options = new PageTurningOptions();
            float fling = 600;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {

                Boolean3 b = Boolean3.UNDEFINED;
                if (velocityX < -fling) {
                    b = Boolean3.TRUE;
                }
                if (velocityX > fling) {
                    b = Boolean3.FALSE;
                }
                if (b != Boolean3.UNDEFINED) {

                    startAnimatedScrolling(
                        b == Boolean3.TRUE ? FBView.PageIndex.next : FBView.PageIndex.previous,
                        options.Horizontal.getValue()
                            ? FBView.Direction.rightToLeft : FBView.Direction.up,
                        options.AnimationSpeed.getValue()
                    );

                    myPendingPress = false;
                    if (null != myPendingShortClickRunnable) {
                        removeCallbacks(myPendingShortClickRunnable);
                    }
                    myPendingShortClickRunnable = null;

                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        getAnimationProvider().terminate();
        if (myScreenIsTouched) {
            final ZLView view = ZLApplication.Instance().getCurrentView();
            myScreenIsTouched = false;
            view.onScrollingFinished(ZLView.PageIndex.current);
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
//		final Context context = getContext();
//		if (context instanceof FBReader) {
//			((FBReader)context).createWakeLock();
//		} else {
//			System.err.println("A surprise: view's context is not an FBReader");
//		}
        super.onDraw(canvas);

//		final int w = getWidth();
//		final int h = getMainAreaHeight();

        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        myBitmapManager.setSize(getWidth(), getMainAreaHeight());
        if (getAnimationProvider().inProgress()) {
            onDrawInScrolling(canvas);
        } else {
            onDrawStatic(canvas);
            ZLApplication.Instance().onRepaintFinished();
        }
    }

    private AnimationProvider myAnimationProvider;
    private ZLView.Animation myAnimationType;

    private AnimationProvider getAnimationProvider() {
        final ZLView.Animation type = ZLApplication.Instance().getCurrentView().getAnimationType();
        if (myAnimationProvider == null || myAnimationType != type) {
            myAnimationType = type;
            //这里是切换翻页动画的地方
            switch (type) {
                case none:
                    myAnimationProvider = new NoneAnimationProvider(myBitmapManager);
                    break;
                case curl:
                    myAnimationProvider = new CurlAnimationProvider(myBitmapManager);
                    break;
                case slide:
                    myAnimationProvider = new SlideAnimationProvider(myBitmapManager);
                    break;
                case slideOldStyle:
                    myAnimationProvider = new SlideOldStyleAnimationProvider(myBitmapManager);
                    break;
                case shift:
                    myAnimationProvider = new ShiftAnimationProvider(myBitmapManager);
                    break;
            }
        }
        return myAnimationProvider;
    }

    private void onDrawInScrolling(Canvas canvas) {
        final ZLView view = ZLApplication.Instance().getCurrentView();

        final AnimationProvider animator = getAnimationProvider();
        final AnimationProvider.Mode oldMode = animator.getMode();
        animator.doStep();
        if (animator.inProgress()) {
            animator.draw(canvas);
            if (animator.getMode().Auto) {
                postInvalidate();
            }
            drawFooter(canvas, animator);
        } else {
            switch (oldMode) {
                case AnimatedScrollingForward: {
                    final ZLView.PageIndex index = animator.getPageToScrollTo();
                    myBitmapManager.shift(index == ZLView.PageIndex.next);
                    view.onScrollingFinished(index);
                    ZLApplication.Instance().onRepaintFinished();
                    break;
                }
                case AnimatedScrollingBackward:
                    view.onScrollingFinished(ZLView.PageIndex.current);
                    break;
                default:
                    break;
            }
//            Log.i("Reader", "onDrawInScrolling: "+oldMode);

            onDrawStatic(canvas);
            //通知翻页了
            try {
                FBReaderApp app = (FBReaderApp) FBReaderApp.Instance();
                if (null == app) {
                    app = new FBReaderApp(Paths.systemInfo(getContext()));
                }
                app.runAction(ActionCode.ACTION_PAGE_SCROLL);
            } catch (Exception e) {
                //TODO 不发出消息
            }

        }
    }

    @Override
    public void reset() {
        myBitmapManager.reset();
    }

    @Override
    public void repaint() {
        postInvalidate();
    }

    @Override
    public void startManualScrolling(int x, int y, ZLView.Direction direction) {
        final AnimationProvider animator = getAnimationProvider();
        animator.setup(direction, getWidth(), getMainAreaHeight(), myColorLevel);
        animator.startManualScrolling(x, y);
    }

    @Override
    public void scrollManuallyTo(int x, int y) {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        final AnimationProvider animator = getAnimationProvider();
        if (view.canScroll(animator.getPageToScrollTo(x, y))) {
            animator.scrollTo(x, y);
            postInvalidate();
        }
    }

    @Override
    public void startAnimatedScrolling(ZLView.PageIndex pageIndex, int x, int y,
        ZLView.Direction direction, int speed) {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        if (pageIndex == ZLView.PageIndex.current || !view.canScroll(pageIndex)) {
            return;
        }
        final AnimationProvider animator = getAnimationProvider();
        animator.setup(direction, getWidth(), getMainAreaHeight(), myColorLevel);
        animator.startAnimatedScrolling(pageIndex, x, y, speed);
        if (animator.getMode().Auto) {
            postInvalidate();
        }
    }

    @Override
    public void startAnimatedScrolling(ZLView.PageIndex pageIndex, ZLView.Direction direction,
        int speed) {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        if (pageIndex == ZLView.PageIndex.current || !view.canScroll(pageIndex)) {
            return;
        }
        final AnimationProvider animator = getAnimationProvider();
        animator.setup(direction, getWidth(), getMainAreaHeight(), myColorLevel);
        animator.startAnimatedScrolling(pageIndex, null, null, speed);
        if (animator.getMode().Auto) {
            postInvalidate();
        }
    }

    @Override
    public void startAnimatedScrolling(int x, int y, int speed) {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        final AnimationProvider animator = getAnimationProvider();
        if (!view.canScroll(animator.getPageToScrollTo(x, y))) {
            animator.terminate();
            return;
        }
        animator.startAnimatedScrolling(x, y, speed);
        postInvalidate();
    }

    void drawOnBitmap(Bitmap bitmap, ZLView.PageIndex index) {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        if (view == null) {
            return;
        }

        final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
            mySystemInfo,
            new Canvas(bitmap),
            new ZLAndroidPaintContext.Geometry(
                getWidth(),
                getHeight(),
                getWidth(),
                getMainAreaHeight(),
                0,
                0
            ),
            view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
        );
        view.paint(context, index);
    }

    private Bitmap myFooterBitmap;

    private void drawFooter(Canvas canvas, AnimationProvider animator) {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        final ZLView.FooterArea footer = view.getFooterArea();

        if (footer == null) {
            myFooterBitmap = null;
            return;
        }

        if (myFooterBitmap != null &&
            (myFooterBitmap.getWidth() != getWidth() ||
                myFooterBitmap.getHeight() != footer.getHeight())) {
            myFooterBitmap = null;
        }
        if (myFooterBitmap == null) {
            myFooterBitmap = Bitmap
                .createBitmap(getWidth(), footer.getHeight(), Bitmap.Config.RGB_565);
        }
        final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
            mySystemInfo,
            new Canvas(myFooterBitmap),
            new ZLAndroidPaintContext.Geometry(
                getWidth(),
                getHeight(),
                getWidth(),
                footer.getHeight(),
                0,
                getMainAreaHeight()
            ),
            view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
        );
        footer.paint(context);
        final int voffset = getHeight() - footer.getHeight();
        if (animator != null) {
            animator.drawFooterBitmap(canvas, myFooterBitmap, voffset);
        } else {
            canvas.drawBitmap(myFooterBitmap, 0, voffset, myPaint);
        }
    }

    private void onDrawStatic(final Canvas canvas) {
        canvas.drawBitmap(myBitmapManager.getBitmap(ZLView.PageIndex.current), 0, 0, myPaint);
        drawFooter(canvas, null);
        post(() -> PrepareService.execute(() -> {
            final ZLView view = ZLApplication.Instance().getCurrentView();
            final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
                mySystemInfo,
                canvas,
                new ZLAndroidPaintContext.Geometry(
                    getWidth(),
                    getHeight(),
                    getWidth(),
                    getMainAreaHeight(),
                    0,
                    0
                ),
                view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
            );
            view.preparePage(context, ZLView.PageIndex.next);
        }));
    }


    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
        } else {
            ZLApplication.Instance().getCurrentView()
                .onTrackballRotated((int) (10 * event.getX()), (int) (10 * event.getY()));
        }
        return true;
    }

    private class LongClickRunnable implements Runnable {

        @Override
        public void run() {
            if (performLongClick()) {
                myLongClickPerformed = true;
            }
        }
    }

    private volatile LongClickRunnable myPendingLongClickRunnable;
    private volatile boolean myLongClickPerformed;

    private void postLongClickRunnable() {
        myLongClickPerformed = false;
        myPendingPress = false;
        if (myPendingLongClickRunnable == null) {
            myPendingLongClickRunnable = new LongClickRunnable();
        }
        postDelayed(myPendingLongClickRunnable, ViewConfiguration.getLongPressTimeout());
    }

    private class ShortClickRunnable implements Runnable {

        @Override
        public void run() {
            final ZLView view = ZLApplication.Instance().getCurrentView();
//            view.onFingerSingleTap(myPressedX, myPressedY);
            myPendingPress = false;
            myPendingShortClickRunnable = null;
        }
    }

    private volatile ShortClickRunnable myPendingShortClickRunnable;

    private volatile boolean myPendingPress;
    private volatile boolean myPendingDoubleTap;
    private int myPressedX, myPressedY;
    private boolean myScreenIsTouched;


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isPopupWindowStatus()) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                FBReaderApp.Instance().runAction(ActionCode.SHOW_MENU, false);
            }
            return true;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        final ZLView view = ZLApplication.Instance().getCurrentView();

        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
                myPendingDoubleTap = false;
                myPendingPress = false;
                myScreenIsTouched = false;
                myLongClickPerformed = false;
                if (myPendingShortClickRunnable != null) {
                    removeCallbacks(myPendingShortClickRunnable);
                    myPendingShortClickRunnable = null;
                }
                if (myPendingLongClickRunnable != null) {
                    removeCallbacks(myPendingLongClickRunnable);
                    myPendingLongClickRunnable = null;
                }
                view.onFingerEventCancelled();
                break;
            case MotionEvent.ACTION_UP:
                if (myPendingDoubleTap) {
                    view.onFingerDoubleTap(x, y);
                } else if (myLongClickPerformed) {
                    view.onFingerReleaseAfterLongPress(x, y);
                } else {
                    if (myPendingLongClickRunnable != null) {
                        removeCallbacks(myPendingLongClickRunnable);
                        myPendingLongClickRunnable = null;
                    }
                    if (myPendingPress) {
                        if (view.isDoubleTapSupported()) {
                            if (myPendingShortClickRunnable == null) {
                                myPendingShortClickRunnable = new ShortClickRunnable();
                            }
                            postDelayed(myPendingShortClickRunnable,
                                ViewConfiguration.getDoubleTapTimeout());
                        } else {
                            view.onFingerSingleTap(x, y);
                        }
                    } else {
                        view.onFingerRelease(x, y);
                    }
                }
                myPendingDoubleTap = false;
                myPendingPress = false;
                myScreenIsTouched = false;

                break;
            case MotionEvent.ACTION_DOWN:

                if (myPendingShortClickRunnable != null) {
                    removeCallbacks(myPendingShortClickRunnable);
                    myPendingShortClickRunnable = null;
                    myPendingDoubleTap = true;
                } else {
                    postLongClickRunnable();
                    myPendingPress = true;
                }
                myScreenIsTouched = true;
                myPressedX = x;
                myPressedY = y;
                break;
            case MotionEvent.ACTION_MOVE: {
                final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                final boolean isAMove =
                    Math.abs(myPressedX - x) > slop || Math.abs(myPressedY - y) > slop;
                if (isAMove) {
                    myPendingDoubleTap = false;
                }
                if (myLongClickPerformed) {
                    view.onFingerMoveAfterLongPress(x, y);
                } else {
                    if (myPendingPress) {
                        if (isAMove) {
                            if (myPendingShortClickRunnable != null) {
                                removeCallbacks(myPendingShortClickRunnable);
                                myPendingShortClickRunnable = null;
                            }
                            if (myPendingLongClickRunnable != null) {
                                removeCallbacks(myPendingLongClickRunnable);
                            }
                            view.onFingerPress(myPressedX, myPressedY);
                            myPendingPress = false;
                        }
                    }
                    if (!myPendingPress) {
                        view.onFingerMove(x, y);
                    }
                }
                break;

            }
            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onLongClick(View v) {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        return view.onFingerLongPress(myPressedX, myPressedY);
    }

    private int myKeyUnderTracking = -1;
    private long myTrackingStartTime;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final ZLApplication application = ZLApplication.Instance();
        final ZLKeyBindings bindings = application.keyBindings();

        if (bindings.hasBinding(keyCode, true) ||
            bindings.hasBinding(keyCode, false)) {
            if (myKeyUnderTracking != -1) {
                if (myKeyUnderTracking == keyCode) {
                    return true;
                } else {
                    myKeyUnderTracking = -1;
                }
            }
            if (bindings.hasBinding(keyCode, true)) {
                myKeyUnderTracking = keyCode;
                myTrackingStartTime = System.currentTimeMillis();
                return true;
            } else {
                return application.runActionByKey(keyCode, false);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (myKeyUnderTracking != -1) {
            if (myKeyUnderTracking == keyCode) {
                final boolean longPress = System.currentTimeMillis() >
                    myTrackingStartTime + ViewConfiguration.getLongPressTimeout();
                ZLApplication.Instance().runActionByKey(keyCode, longPress);
            }
            myKeyUnderTracking = -1;
            return true;
        } else {
            final ZLKeyBindings bindings = ZLApplication.Instance().keyBindings();
            return
                bindings.hasBinding(keyCode, false) ||
                    bindings.hasBinding(keyCode, true);
        }
    }

    @Override
    protected int computeVerticalScrollExtent() {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        if (!view.isScrollbarShown()) {
            return 0;
        }
        final AnimationProvider animator = getAnimationProvider();
        if (animator.inProgress()) {
            final int from = view.getScrollbarThumbLength(ZLView.PageIndex.current);
            final int to = view.getScrollbarThumbLength(animator.getPageToScrollTo());
            final int percent = animator.getScrolledPercent();
            return (from * (100 - percent) + to * percent) / 100;
        } else {
            return view.getScrollbarThumbLength(ZLView.PageIndex.current);
        }
    }

    @Override
    protected int computeVerticalScrollOffset() {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        if (!view.isScrollbarShown()) {
            return 0;
        }
        final AnimationProvider animator = getAnimationProvider();
        if (animator.inProgress()) {
            final int from = view.getScrollbarThumbPosition(ZLView.PageIndex.current);
            final int to = view.getScrollbarThumbPosition(animator.getPageToScrollTo());
            final int percent = animator.getScrolledPercent();
            return (from * (100 - percent) + to * percent) / 100;
        } else {
            return view.getScrollbarThumbPosition(ZLView.PageIndex.current);
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        final ZLView view = ZLApplication.Instance().getCurrentView();
        if (!view.isScrollbarShown()) {
            return 0;
        }
        return view.getScrollbarFullSize();
    }

    private int getMainAreaHeight() {
        final ZLView.FooterArea footer = ZLApplication.Instance().getCurrentView().getFooterArea();
        return footer != null ? getHeight() - footer.getHeight() : getHeight();
    }

    @Override
    protected void updateColorLevel() {
        ViewUtil.setColorLevel(myPaint, myColorLevel);
    }

    private boolean popupWindowStatus = false;


    public boolean isPopupWindowStatus() {
        return popupWindowStatus;
    }

    @Override
    public void setPopupWindowStatus(boolean popupWindowStatus) {
        this.popupWindowStatus = popupWindowStatus;
    }
}
