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


import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import java.util.Date;
import org.geometerplus.android.fbreader.config.Config;
import org.geometerplus.zlibrary.core.application.*;
import org.geometerplus.zlibrary.core.util.*;

import org.geometerplus.zlibrary.text.view.*;

import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.bookmodel.*;
import org.geometerplus.fbreader.fbreader.options.*;
import org.geometerplus.fbreader.formats.*;
import org.json.JSONObject;

public final class FBReaderApp extends ZLApplication {

    private String TAG = getClass().getSimpleName();

    public final MiscOptions MiscOptions = new MiscOptions();
    public final ImageOptions ImageOptions = new ImageOptions();
    public final ViewOptions ViewOptions = new ViewOptions();
    public final PageTurningOptions PageTurningOptions = new PageTurningOptions();
//    public final SyncOptions SyncOptions = new SyncOptions();

    private final ZLKeyBindings myBindings = new ZLKeyBindings();

    public final FBView BookTextView;
    public volatile BookModel Model;

    public FBReaderApp(SystemInfo systemInfo) {
        super(systemInfo);

        //clean选择部分
        addAction(ActionCode.SELECTION_CLEAR, new SelectionClearAction(this));

//        addAction(ActionCode.TURN_PAGE_FORWARD, new TurnPageAction(this, true));
//        addAction(ActionCode.TURN_PAGE_BACK, new TurnPageAction(this, false));


        addAction(ActionCode.INCREASE_FONT, new ChangeFontSizeAction(this, +2));
        addAction(ActionCode.DECREASE_FONT, new ChangeFontSizeAction(this, -2));
        BookTextView = new FBView(this);
        setView(BookTextView);
    }


    public void openBook(Book book, final Bookmark bookmark, Runnable postAction) {

        if (null != Model && Model.Book.equals(book)) {

            Log.i(TAG, "not open Current Book: " + book);
            return;
        }

        final Book bookToOpen = book;
        bookToOpen.addNewLabel(Book.READ_LABEL);

        final SynchronousExecutor executor = createExecutor("loadingBook");
        executor.execute(() -> openBookInternal(bookToOpen, bookmark, false), postAction);
    }


    @Override
    public ZLKeyBindings keyBindings() {
        return myBindings;
    }

    public FBView getTextView() {
        return (FBView) getCurrentView();
    }


    public void clearTextCaches() {
        BookTextView.clearCaches();
    }


    private synchronized void openBookInternal(final Book book, Bookmark bookmark, boolean force) {
//
        BookTextView.setModel(null);
        clearTextCaches();
        Model = null;
        System.gc();

        final PluginCollection pluginCollection = PluginCollection.Instance(SystemInfo);
        final FormatPlugin plugin;
        try {
            plugin = BookUtil.getPlugin(pluginCollection, book);
        } catch (BookReadingException e) {
            e.printStackTrace();
            return;
        }

        try {
            Model = BookModel.createModel(book, plugin);
            BookTextView.setModel(Model.getTextModel());

            //  跳书签
            if (null != bookmark) {

                BookTextView.gotoPosition(bookmark);

            }else {
                //TODO   应该单独处理，不使用config
                String p = Config.Instance().getConfig(book.getPath());
                if(!TextUtils.isEmpty(p)){
                    ZLTextFixedPosition pos = new ZLTextFixedPosition(Integer.valueOf(p),0,0);
                    BookTextView.gotoPosition(pos);
                }
            }
            setView(BookTextView);

        } catch (BookReadingException e) {
            e.printStackTrace();
        }

        getViewWidget().reset();
        getViewWidget().repaint();

    }



    @Override
    public void onWindowClosing() {
        storePosition();
    }

    //链接跳转的
    public void tryOpenFootnote(String id) {
        if (Model != null) {
            final BookModel.Label label = Model.getLabel(id);
            if (label != null) {

                BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
                setView(BookTextView);

                getViewWidget().repaint();
                storePosition();
            }
        }
    }

    public void storePosition() {
        final Book bk = Model != null ? Model.Book : null;
        if (bk != null  && BookTextView != null) {
            final ZLTextFixedPosition position = new ZLTextFixedPosition(BookTextView.getStartCursor());
//            if (!myStoredPosition.equals(position)) {
//                myStoredPosition = position;
//                savePosition();
//            }


            Config.Instance().setConfig(bk.getPath(), ""+position.getParagraphIndex());
        }
    }




}
