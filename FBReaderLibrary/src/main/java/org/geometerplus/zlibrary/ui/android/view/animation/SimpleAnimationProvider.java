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

package org.geometerplus.zlibrary.ui.android.view.animation;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;

abstract class SimpleAnimationProvider extends AnimationProvider {

    private String TAG = "Reader";
    private float mySpeedFactor;

    SimpleAnimationProvider(BitmapManager bitmapManager) {
        super(bitmapManager);
    }

    @Override
    public ZLViewEnums.PageIndex getPageToScrollTo(int x, int y) {
        if (myDirection == null) {
            return ZLViewEnums.PageIndex.current;
        }

        switch (myDirection) {
            case rightToLeft:
                return myStartX < x ? ZLViewEnums.PageIndex.previous : ZLViewEnums.PageIndex.next;
            case leftToRight:
                return myStartX < x ? ZLViewEnums.PageIndex.next : ZLViewEnums.PageIndex.previous;
            case up:
                return myStartY < y ? ZLViewEnums.PageIndex.previous : ZLViewEnums.PageIndex.next;
            case down:
                return myStartY < y ? ZLViewEnums.PageIndex.next : ZLViewEnums.PageIndex.previous;
        }
        return ZLViewEnums.PageIndex.current;
    }

    @Override
    protected void setupAnimatedScrollingStart(Integer x, Integer y) {
        if (x == null || y == null) {
            if (myDirection.IsHorizontal) {
                x = mySpeed < 0 ? myWidth : 0;
                y = 0;
            } else {
                x = 0;
                y = mySpeed < 0 ? myHeight : 0;
            }
        }
        myEndX = myStartX = x;
        myEndY = myStartY = y;
    }


    @Override
    protected void startAnimatedScrollingInternal(int speed) {
        mySpeedFactor = (float) Math.pow(1.5, 0.12 * speed);
        preDoStep(speed);
        doStep();
    }


    private Stack<Float> xSpeed = new Stack<>();
    private Stack<Float> xSpeedAfterHalf = new Stack<>();

    private int halfPercent = 50;

    private void preDoStep(int speed) {

        //当前距离和最后距离差为所有的距离。

        float bound;
        if (getMode() == Mode.AnimatedScrollingForward) {
            bound = myDirection.IsHorizontal ? myWidth : myHeight;
        } else {
            bound = 0;
        }
        int startPercent = getScrolledPercent();
        halfPercent = (100 - startPercent) / 2;

        int length = (int) (bound - startPercent * bound / 100);

        xSpeed.clear();
        xSpeedAfterHalf.clear();
        float half = length / 2;
        while (half >= speed) {
            half = half / mySpeedFactor;
            xSpeed.push(half);
        }
    }

    private float lastSp = MIN_STEP;
    private float getSpeed() {

        float sp = lastSp;

        if (isNotScrollerHalf()) {
            if (!xSpeed.isEmpty()) {
                sp = xSpeed.pop();
            }
            xSpeedAfterHalf.push(sp );

        } else {
            if (!xSpeedAfterHalf.isEmpty()) {
                sp = xSpeedAfterHalf.pop();
            }
        }

        lastSp = sp;
        return sp <= MIN_STEP ? MIN_STEP : sp;
    }


    private boolean isNotScrollerHalf() {
        return getScrolledPercent() < halfPercent;
    }

    private static final int MIN_STEP = 5;

    @Override
    public final void doStep() {
        if (!getMode().Auto) {
            return;
        }

        switch (myDirection) {
            case leftToRight:
                myEndX -= (int) mySpeed;
                break;
            case rightToLeft:
                myEndX += (int) mySpeed;
                break;
            case up:
                myEndY += (int) mySpeed;
                break;
            case down:
                myEndY -= (int) mySpeed;
                break;
        }
        final int bound;
        if (getMode() == Mode.AnimatedScrollingForward) {
            bound = myDirection.IsHorizontal ? myWidth : myHeight;
        } else {
            bound = 0;
        }

        if (mySpeed > 0) {

            if (getScrollingShift() >= bound) {
                if (myDirection.IsHorizontal) {
                    myEndX = myStartX + bound;
                } else {
                    myEndY = myStartY + bound;
                }
                terminate();
                return;
            }
        } else {

            if (getScrollingShift() <= -bound) {
                if (myDirection.IsHorizontal) {
                    myEndX = myStartX - bound;
                } else {
                    myEndY = myStartY - bound;
                }
                terminate();
                return;
            }
        }

        if (halfPercent <= 40) {
            mySpeed *= mySpeedFactor;
        } else {
            if (mySpeed > 0) {
                mySpeed += isNotScrollerHalf() ? getSpeed() : -getSpeed();
                mySpeed = Math.max(MIN_STEP, mySpeed);
            } else {
                mySpeed -= isNotScrollerHalf() ? getSpeed() : -getSpeed();
                mySpeed = Math.min(-MIN_STEP, mySpeed);
            }
        }
        Log.i(TAG, "doStep: " + mySpeed);
    }
}
