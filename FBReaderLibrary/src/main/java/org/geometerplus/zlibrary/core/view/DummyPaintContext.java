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

package org.geometerplus.zlibrary.core.view;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import java.util.List;

import org.geometerplus.fbreader.fbreader.options.ViewOptions;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.fonts.FontEntry;
import org.geometerplus.zlibrary.core.image.ZLImageData;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.view.SelectionCursor.Which;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;

final class DummyPaintContext extends ZLPaintContext {

    Paint paint = new Paint();

    DummyPaintContext() {
        super(new SystemInfo() {
            @Override
            public String tempDirectory() {
                return "";
            }

            @Override
            public String networkCacheDirectory() {
                return "";
            }
        });
        ViewOptions options = new ViewOptions();
        paint
            .setTextSize(options.getTextStyleCollection().getBaseStyle().FontSizeOption.getValue());
    }


    @Override
    public void clear(ZLFile wallpaperFile, FillMode mode) {
    }

    @Override
    public void clear(ZLColor color) {
    }

    @Override
    public ZLColor getBackgroundColor() {
        return new ZLColor(0, 0, 0);
    }

    @Override
    protected void setFontInternal(List<FontEntry> entries, int size, boolean bold, boolean italic,
        boolean underline, boolean strikeThrought) {
    }

    @Override
    public void setTextColor(ZLColor color) {
    }

    @Override
    public void setLineColor(ZLColor color) {
    }

    @Override
    public void setLineWidth(int width) {
    }

    @Override
    public void setFillColor(ZLColor color, int alpha) {
    }

    @Override
    public int getWidth() {
        return ZLibrary.Instance().getWidthInPixels();
    }

    @Override
    public int getHeight() {
        //这个就是计算页码的坑，第一次的时候由于没有获取到view宽高导致
        int h = ZLibrary.Instance().getHeightInPixels();
        final ZLView.FooterArea footer = ZLApplication.Instance().getCurrentView().getFooterArea();
        // 减去 footer区域
        h = footer != null ? h - footer.getHeight() : h;
        //  减去  (状态栏)
         h = h- ZLibrary.Instance().getStatusBarHeight();
        return h;

    }



    @Override
    public int getStringWidth(char[] string, int offset, int length) {
        boolean containsSoftHyphen = false;
        for (int i = offset; i < offset + length; ++i) {
            if (string[i] == (char) 0xAD) {
                containsSoftHyphen = true;
                break;
            }
        }
        if (!containsSoftHyphen) {
            return (int) (paint.measureText(new String(string, offset, length)) + 0.5f);
        } else {
            final char[] corrected = new char[length];
            int len = 0;
            for (int o = offset; o < offset + length; ++o) {
                final char chr = string[o];
                if (chr != (char) 0xAD) {
                    corrected[len++] = chr;
                }
            }
            return (int) (paint.measureText(corrected, 0, len) + 0.5f);
        }
    }


    @Override
    protected int getSpaceWidthInternal() {
        return (int) (paint.measureText(" ", 0, 1) + 0.5f);
    }

    @Override
    protected int getCharHeightInternal(char chr) {
        final Rect r = new Rect();
        final char[] txt = new char[]{chr};
        paint.getTextBounds(txt, 0, 1, r);
        return r.bottom - r.top;
    }

    @Override
    protected int getStringHeightInternal() {
        return (int) (paint.getTextSize() + 0.5f);
    }

    @Override
    protected int getDescentInternal() {
        return (int) (paint.descent() + 0.5f);
    }


    @Override
    public void drawString(int x, int y, char[] string, int offset, int length) {
    }

    @Override
    public Size imageSize(ZLImageData image, Size maxSize, ScalingType scaling) {
        final Bitmap bitmap = ((ZLAndroidImageData) image).getBitmap(maxSize, scaling);
        return (bitmap != null && !bitmap.isRecycled())
            ? new Size(bitmap.getWidth(), bitmap.getHeight()) : null;
    }

    @Override
    public void drawImage(int x, int y, ZLImageData image, Size maxSize, ScalingType scaling,
        ColorAdjustingMode adjustingMode) {
    }

    @Override
    public void drawLine(int x0, int y0, int x1, int y1) {
    }

    @Override
    public void fillRectangle(int x0, int y0, int x1, int y1) {
    }

    @Override
    public void fillPolygon(int[] xs, int[] ys) {
    }

    @Override
    public void drawPolygonalLine(int[] xs, int[] ys) {
    }

    @Override
    public void drawOutline(int[] xs, int[] ys) {
    }

    @Override
    public void fillCircle(int x, int y, int radius) {
    }

    @Override
    public void fillCursor(int x, int y, int radius, Which type) {

    }
}
