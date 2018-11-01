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

package org.geometerplus.fbreader.fbreader;

import android.graphics.Color;
import android.util.Log;
import java.util.*;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.util.FixedTextSnippet;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile;
import org.geometerplus.zlibrary.core.fonts.FontEntry;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.view.SelectionCursor;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;

import org.geometerplus.zlibrary.text.view.*;
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection;

import org.geometerplus.fbreader.bookmodel.FBHyperlinkType;
import org.geometerplus.fbreader.fbreader.options.*;

public final class FBView extends ZLTextView {

    private String TAG = getClass().getSimpleName();
    private final FBReaderApp myReader;
    private final ViewOptions myViewOptions;
//	private final BookElementManager myBookElementManager;

    FBView(FBReaderApp reader) {
        super(reader);
        myReader = reader;
        myViewOptions = reader.ViewOptions;
//		myBookElementManager = new BookElementManager(this);
    }


    private int myStartY;
    private boolean myIsBrightnessAdjustmentInProgress;
    private int myStartBrightness;

    private TapZoneMap myZoneMap;

    private TapZoneMap getZoneMap() {
        final PageTurningOptions prefs = myReader.PageTurningOptions;
        String id = prefs.TapZoneMap.getValue();
        if ("".equals(id)) {
            id = prefs.Horizontal.getValue() ? "right_to_left" : "up";
        }
        if (myZoneMap == null || !id.equals(myZoneMap.Name)) {
            myZoneMap = TapZoneMap.zoneMap(id);
        }
        return myZoneMap;
    }

    private void onFingerSingleTapLastResort(int x, int y) {
        myReader.runAction(getZoneMap().getActionByCoordinates(
            x, y, getContextWidth(), getContextHeight(),
            isDoubleTapSupported() ? TapZoneMap.Tap.singleNotDoubleTap : TapZoneMap.Tap.singleTap
        ), x, y);
    }

    @Override
    public void onFingerSingleTap(int x, int y) {

        final ZLTextRegion hyperlinkRegion = findRegion(x, y, maxSelectionDistance(),
            ZLTextRegion.HyperlinkFilter);
        if (hyperlinkRegion != null) {
            outlineRegion(hyperlinkRegion);
            myReader.getViewWidget().reset();
            myReader.getViewWidget().repaint();
            myReader.runAction(ActionCode.PROCESS_HYPERLINK);
            return;
        }

        final ZLTextRegion bookRegion = findRegion(x, y, 0, ZLTextRegion.ExtensionFilter);
        if (bookRegion != null) {
            myReader.runAction(ActionCode.DISPLAY_BOOK_POPUP, bookRegion);
            return;
        }

        final ZLTextRegion videoRegion = findRegion(x, y, 0, ZLTextRegion.VideoFilter);
        if (videoRegion != null) {
            outlineRegion(videoRegion);
            myReader.getViewWidget().reset();
            myReader.getViewWidget().repaint();
            myReader
                .runAction(ActionCode.OPEN_VIDEO, (ZLTextVideoRegionSoul) videoRegion.getSoul());
            return;
        }

//        final ZLTextHighlighting highlighting = findHighlighting(x, y, maxSelectionDistance());
//		if (highlighting instanceof BookmarkHighlighting) {
//			myReader.runAction(
//				ActionCode.SELECTION_BOOKMARK,
//				((BookmarkHighlighting)highlighting).Bookmark
//			);
//			return;
//		}

        if (myReader.isActionEnabled(ActionCode.HIDE_TOAST)) {
            myReader.runAction(ActionCode.HIDE_TOAST);
            return;
        }

        myReader.runAction(ActionCode.SELECTION_CLEAR);

        //   点击区域
        int w = getContextWidth();

        if (x >= w / 3 && x <= w - w / 3) {
            myReader.runAction(ActionCode.SHOW_MENU);

            return;
        }
        onFingerSingleTapLastResort(x, y);

    }

    @Override
    public boolean isDoubleTapSupported() {
        return myReader.MiscOptions.EnableDoubleTap.getValue();
    }

    @Override
    public void onFingerDoubleTap(int x, int y) {
        myReader.runAction(ActionCode.HIDE_TOAST);

        myReader.runAction(getZoneMap().getActionByCoordinates(
            x, y, getContextWidth(), getContextHeight(), TapZoneMap.Tap.doubleTap
        ), x, y);
    }

    @Override
    public void onFingerPress(int x, int y) {
        myReader.runAction(ActionCode.HIDE_TOAST);

        final float maxDist = ZLibrary.Instance().getDisplayDPI() / 4;
        final SelectionCursor.Which cursor = findSelectionCursor(x, y, maxDist * maxDist);
        if (cursor != null) {
            myReader.runAction(ActionCode.SELECTION_HIDE_PANEL);
            moveSelectionCursorTo(cursor, x, y);
            return;
        }

        if (myReader.MiscOptions.AllowScreenBrightnessAdjustment.getValue()
            && x < getContextWidth() / 10) {
            myIsBrightnessAdjustmentInProgress = true;
            myStartY = y;
            myStartBrightness = myReader.getViewWidget().getScreenBrightness();
            return;
        }

        startManualScrolling(x, y);
        myReader.runAction(ActionCode.SELECTION_CLEAR);

    }

    private boolean isFlickScrollingEnabled() {
        final PageTurningOptions.FingerScrollingType fingerScrolling =
            myReader.PageTurningOptions.FingerScrolling.getValue();
        return
            fingerScrolling == PageTurningOptions.FingerScrollingType.byFlick ||
                fingerScrolling == PageTurningOptions.FingerScrollingType.byTapAndFlick;
    }

    private void startManualScrolling(int x, int y) {
        if (!isFlickScrollingEnabled()) {
            return;
        }

        final boolean horizontal = myReader.PageTurningOptions.Horizontal.getValue();
        final Direction direction = horizontal ? Direction.rightToLeft : Direction.up;
        myReader.getViewWidget().startManualScrolling(x, y, direction);

        //这里可以记录上次打开的位置
//		Log.w(TAG, "startManualScrolling: X:"+x+" Y:"+y+"   POS:  "+ getStartCursor());

    }

    @Override
    public void onFingerMove(int x, int y) {
        final SelectionCursor.Which cursor = getSelectionCursorInMovement();
        if (cursor != null) {
            moveSelectionCursorTo(cursor, x, y);
            return;
        }

        synchronized (this) {
            if (myIsBrightnessAdjustmentInProgress) {
                if (x >= getContextWidth() / 5) {
                    myIsBrightnessAdjustmentInProgress = false;
                    startManualScrolling(x, y);
                } else {
                    final int delta =
                        (myStartBrightness + 30) * (myStartY - y) / getContextHeight();
                    myReader.getViewWidget().setScreenBrightness(myStartBrightness + delta);
                    return;
                }
            }

            if (isFlickScrollingEnabled()) {
                myReader.getViewWidget().scrollManuallyTo(x, y);
            }
        }
    }

    @Override
    public void onFingerRelease(int x, int y) {
        final SelectionCursor.Which cursor = getSelectionCursorInMovement();
        if (cursor != null) {
            releaseSelectionCursor();
        } else if (myIsBrightnessAdjustmentInProgress) {
            myIsBrightnessAdjustmentInProgress = false;
        } else if (isFlickScrollingEnabled()) {
            myReader.getViewWidget().startAnimatedScrolling(
                x, y, myReader.PageTurningOptions.AnimationSpeed.getValue()
            );
        }

    }

    @Override
    public boolean onFingerLongPress(int x, int y) {
        myReader.runAction(ActionCode.HIDE_TOAST);

        final ZLTextRegion region = findRegion(x, y, maxSelectionDistance(),
            ZLTextRegion.AnyRegionFilter);
        if (region != null) {
            final ZLTextRegion.Soul soul = region.getSoul();
            boolean doSelectRegion = false;
            if (soul instanceof ZLTextWordRegionSoul) {
                switch (myReader.MiscOptions.WordTappingAction.getValue()) {
                    case startSelecting:
                        myReader.runAction(ActionCode.SELECTION_HIDE_PANEL);
                        initSelection(x, y);
                        final SelectionCursor.Which cursor = findSelectionCursor(x, y);
                        if (cursor != null) {
                            moveSelectionCursorTo(cursor, x, y);
                        }
                        return true;
                    case selectSingleWord:
                    case openDictionary:
                        doSelectRegion = true;
                        break;
                }
            } else if (soul instanceof ZLTextImageRegionSoul) {
                doSelectRegion =
                    myReader.ImageOptions.TapAction.getValue() !=
                        ImageOptions.TapActionEnum.doNothing;
            } else if (soul instanceof ZLTextHyperlinkRegionSoul) {
                doSelectRegion = true;
            }

            if (doSelectRegion) {
                outlineRegion(region);
                myReader.getViewWidget().reset();
                myReader.getViewWidget().repaint();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onFingerMoveAfterLongPress(int x, int y) {
        final SelectionCursor.Which cursor = getSelectionCursorInMovement();
        if (cursor != null) {
            moveSelectionCursorTo(cursor, x, y);
            return;
        }

        ZLTextRegion region = getOutlinedRegion();
        if (region != null) {
            ZLTextRegion.Soul soul = region.getSoul();
            if (soul instanceof ZLTextHyperlinkRegionSoul ||
                soul instanceof ZLTextWordRegionSoul) {
                if (myReader.MiscOptions.WordTappingAction.getValue() !=
                    MiscOptions.WordTappingActionEnum.doNothing) {
                    region = findRegion(x, y, maxSelectionDistance(), ZLTextRegion.AnyRegionFilter);
                    if (region != null) {
                        soul = region.getSoul();
                        if (soul instanceof ZLTextHyperlinkRegionSoul
                            || soul instanceof ZLTextWordRegionSoul) {
                            outlineRegion(region);
                            myReader.getViewWidget().reset();
                            myReader.getViewWidget().repaint();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onFingerReleaseAfterLongPress(int x, int y) {
        final SelectionCursor.Which cursor = getSelectionCursorInMovement();
        if (cursor != null) {
            releaseSelectionCursor();
            return;
        }

        final ZLTextRegion region = getOutlinedRegion();
        if (region != null) {
            final ZLTextRegion.Soul soul = region.getSoul();

            boolean doRunAction = false;
            if (soul instanceof ZLTextWordRegionSoul) {
                doRunAction =
                    myReader.MiscOptions.WordTappingAction.getValue() ==
                        MiscOptions.WordTappingActionEnum.openDictionary;
            } else if (soul instanceof ZLTextImageRegionSoul) {
                doRunAction =
                    myReader.ImageOptions.TapAction.getValue() ==
                        ImageOptions.TapActionEnum.openImageView;
            }

            if (doRunAction) {
                myReader.runAction(ActionCode.PROCESS_HYPERLINK);
            }
        }
    }

    @Override
    public void onFingerEventCancelled() {
        final SelectionCursor.Which cursor = getSelectionCursorInMovement();
        if (cursor != null) {
            releaseSelectionCursor();
        }
    }

    @Override
    public boolean onTrackballRotated(int diffX, int diffY) {
        if (diffX == 0 && diffY == 0) {
            return true;
        }

        final Direction direction = (diffY != 0) ?
            (diffY > 0 ? Direction.down : Direction.up) :
            (diffX > 0 ? Direction.leftToRight : Direction.rightToLeft);

        new MoveCursorAction(myReader, direction).run();
        return true;
    }

    @Override
    public ZLTextStyleCollection getTextStyleCollection() {
        return myViewOptions.getTextStyleCollection();
    }

    @Override
    public ImageFitting getImageFitting() {
        return myReader.ImageOptions.FitToScreen.getValue();
    }

    @Override
    public int getLeftMargin() {
        return myViewOptions.LeftMargin.getValue();
    }

    @Override
    public int getRightMargin() {
        return myViewOptions.RightMargin.getValue();
    }

    @Override
    public int getTopMargin() {
        return myViewOptions.TopMargin.getValue();
    }

    @Override
    public int getBottomMargin() {
        return myViewOptions.BottomMargin.getValue();
    }

    @Override
    public int getSpaceBetweenColumns() {
        return myViewOptions.SpaceBetweenColumns.getValue();
    }

    @Override
    public boolean twoColumnView() {
        return getContextHeight() <= getContextWidth() && myViewOptions.TwoColumnView.getValue();
    }

    @Override
    public ZLFile getWallpaperFile() {
        final String filePath = myViewOptions.getColorProfile().WallpaperOption.getValue();
        if ("".equals(filePath)) {
            return null;
        }

        final ZLFile file = ZLFile.createFileByPath(filePath);
        if (file == null || !file.exists()) {
            return null;
        }
        return file;
    }

    @Override
    public ZLPaintContext.FillMode getFillMode() {
        return getWallpaperFile() instanceof ZLResourceFile
            ? ZLPaintContext.FillMode.tileMirror
            : myViewOptions.getColorProfile().FillModeOption.getValue();
    }

    @Override
    public ZLColor getBackgroundColor() {
        return myViewOptions.getColorProfile().BackgroundOption.getValue();
    }

    @Override
    public ZLColor getSelectionBackgroundColor() {
        return myViewOptions.getColorProfile().SelectionBackgroundOption.getValue();
    }

    @Override
    public ZLColor getSelectionForegroundColor() {
        return myViewOptions.getColorProfile().SelectionForegroundOption.getValue();
    }

    @Override
    public ZLColor getTextColor(ZLTextHyperlink hyperlink) {
        final ColorProfile profile = myViewOptions.getColorProfile();
        switch (hyperlink.Type) {
            default:
            case FBHyperlinkType.NONE:
                return profile.RegularTextOption.getValue();
            case FBHyperlinkType.INTERNAL:
//			case FBHyperlinkType.FOOTNOTE:
//				return myReader.Collection.isHyperlinkVisited(myReader.getCurrentBook(), hyperlink.Id)
//					? profile.VisitedHyperlinkTextOption.getValue()
//					: profile.HyperlinkTextOption.getValue();
            case FBHyperlinkType.EXTERNAL:
                return profile.HyperlinkTextOption.getValue();
        }
    }

    @Override
    public ZLColor getHighlightingBackgroundColor() {
        return myViewOptions.getColorProfile().HighlightingBackgroundOption.getValue();
    }

    @Override
    public ZLColor getHighlightingForegroundColor() {
        return myViewOptions.getColorProfile().HighlightingForegroundOption.getValue();
    }


    @Override
    protected void releaseSelectionCursor() {
        super.releaseSelectionCursor();
        if (getCountOfSelectedWords() > 0) {
            myReader.runAction(ActionCode.SELECTION_SHOW_PANEL);
        }
    }

    public TextSnippet getSelectedSnippet() {
        final ZLTextPosition start = getSelectionStartPosition();
        final ZLTextPosition end = getSelectionEndPosition();
        if (start == null || end == null) {
            return null;
        }
        final TextBuildTraverser traverser = new TextBuildTraverser(this);
        traverser.traverse(start, end);
        return new FixedTextSnippet(start, end, traverser.getText());
    }


    public int getCountOfSelectedWords() {
        final WordCountTraverser traverser = new WordCountTraverser(this);
        if (!isSelectionEmpty()) {
            traverser.traverse(getSelectionStartPosition(), getSelectionEndPosition());
        }
        return traverser.getCount();
    }

    public static final int SCROLLBAR_SHOW_AS_FOOTER = 3;

    @Override
    public int scrollbarType() {
        return myViewOptions.ScrollbarType.getValue();
    }


    private abstract class Footer implements FooterArea {


        public int getHeight() {
            return myViewOptions.FooterHeight.getValue();
        }


        protected String buildInfoString(PagePosition pagePosition, String separator) {
            final StringBuilder info = new StringBuilder();
            final FooterOptions footerOptions = myViewOptions.getFooterOptions();

            if (footerOptions.showProgressAsPages()) {
                maybeAddSeparator(info, separator);
                info.append(pagePosition.Current);
                info.append("/");
                info.append(pagePosition.Total);
            }
            return info.toString();
        }

        private void maybeAddSeparator(StringBuilder info, String separator) {
            if (info.length() > 0) {
                info.append(separator);
            }
        }

        private List<FontEntry> myFontEntry;
        private Map<String, Integer> myHeightMap = new HashMap<>();
        private Map<String, Integer> myCharHeightMap = new HashMap<>();

        protected synchronized int setFont(ZLPaintContext context, int height, boolean bold) {
            final String family = myViewOptions.getFooterOptions().Font.getValue();
            if (myFontEntry == null || !family.equals(myFontEntry.get(0).Family)) {
                myFontEntry = Collections.singletonList(FontEntry.systemEntry(family));
            }
            final String key = family + (bold ? "N" : "B") + height;
            final Integer cached = myHeightMap.get(key);
            if (cached != null) {
                context.setFont(myFontEntry, cached, bold, false, false, false);
                final Integer charHeight = myCharHeightMap.get(key);
                return charHeight != null ? charHeight : height;
            } else {
                int h = height + 2;
                int charHeight = height;
                final int max = height < 9 ? height - 1 : height - 2;
                for (; h > 5; --h) {
                    context.setFont(myFontEntry, h, bold, false, false, false);
                    charHeight = context.getCharHeight('H');
                    if (charHeight <= max) {
                        break;
                    }
                }
                myHeightMap.put(key, h);
                myCharHeightMap.put(key, charHeight);
                return charHeight;
            }
        }
    }

    private class FooterOldStyle extends Footer {

        public synchronized void paint(ZLPaintContext context) {
            final ZLFile wallpaper = getWallpaperFile();
            if (wallpaper != null) {
                context.clear(wallpaper, getFillMode());
            } else {
                context.clear(getBackgroundColor());
            }

            final BookModel model = myReader.Model;
            if (model == null) {
                return;
            }

            final ZLColor fgColor = getTextColor(ZLTextHyperlink.NO_LINK);

            //200<<24
            int c = (-939524096) | (fgColor.Red << 16) | (fgColor.Green << 8) | fgColor.Blue;

            final int right = context.getWidth() - getRightMargin();
            final int height = getHeight();
            setFont(context,  height /3*2, false);

            final PagePosition pagePosition = FBView.this.pagePosition();
            final String infoString = buildInfoString(pagePosition, " ");
            final int infoWidth = context.getStringWidth(infoString);
            context.setTextColor(new ZLColor(c));
            context.drawString(right - infoWidth, height/3*2, infoString);


        }
    }


    private Footer myFooter;

    @Override
    public Footer getFooterArea() {
        if (myFooter == null) {
            myFooter = new FooterOldStyle();
        }
        return myFooter;
    }

    @Override
    public Animation getAnimationType() {
        return myReader.PageTurningOptions.Animation.getValue();
    }

    @Override
    protected ZLPaintContext.ColorAdjustingMode getAdjustingModeForImages() {

        //TODO 这里决定是否处理图片背景
        if (myReader.ImageOptions.MatchBackground.getValue()) {
            if (ColorProfile.DAY.equals(myViewOptions.getColorProfile().Name)) {
                return ZLPaintContext.ColorAdjustingMode.DARKEN_TO_BACKGROUND;
            } else {
                return ZLPaintContext.ColorAdjustingMode.LIGHTEN_TO_BACKGROUND;
            }
        } else {
            return ZLPaintContext.ColorAdjustingMode.NONE;
        }
    }

    @Override
    public synchronized void onScrollingFinished(PageIndex pageIndex) {
        super.onScrollingFinished(pageIndex);
        myReader.storePosition();
    }

    @Override
    protected ExtensionElementManager getExtensionManager() {
//		return myBookElementManager;
        return null;
    }
}
