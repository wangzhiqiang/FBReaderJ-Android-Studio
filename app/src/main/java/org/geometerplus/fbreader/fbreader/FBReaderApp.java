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


import org.geometerplus.zlibrary.core.application.*;
import org.geometerplus.zlibrary.core.drm.FileEncryptionInfo;
import org.geometerplus.zlibrary.core.drm.EncryptionMethod;
import org.geometerplus.zlibrary.core.util.*;

import org.geometerplus.zlibrary.text.view.*;

import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.bookmodel.*;
import org.geometerplus.fbreader.fbreader.options.*;
import org.geometerplus.fbreader.formats.*;

public final class FBReaderApp extends ZLApplication {


    public final MiscOptions MiscOptions = new MiscOptions();
    public final ImageOptions ImageOptions = new ImageOptions();
    public final ViewOptions ViewOptions = new ViewOptions();
    public final PageTurningOptions PageTurningOptions = new PageTurningOptions();
//    public final SyncOptions SyncOptions = new SyncOptions();

    private final ZLKeyBindings myBindings = new ZLKeyBindings();

    public final FBView BookTextView;
    public volatile BookModel Model;
    public volatile Book ExternalBook;

    public FBReaderApp(SystemInfo systemInfo) {
        super(systemInfo);

        BookTextView = new FBView(this);
        setView(BookTextView);
    }


    public void openBook(Book book, final Bookmark bookmark, Runnable postAction) {

        final Book bookToOpen = book;
        bookToOpen.addNewLabel(Book.READ_LABEL);

        final SynchronousExecutor executor = createExecutor("loadingBook");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                openBookInternal(bookToOpen, bookmark, false);
            }
        }, postAction);
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
        ExternalBook = null;
        System.gc();

        final PluginCollection pluginCollection = PluginCollection.Instance(SystemInfo);
        final FormatPlugin plugin;
        try {
            plugin = BookUtil.getPlugin(pluginCollection, book);
        } catch (BookReadingException e) {
            processException(e);
            return;
        }

        try {
            Model = BookModel.createModel(book, plugin);
            BookTextView.setModel(Model.getTextModel());

            //TODO 跳书签
            if(null == bookmark) {
                BookTextView.gotoPosition(new ZLTextFixedPosition(3,0,0));

            }
            setView(BookTextView);

        } catch (BookReadingException e) {
            processException(e);
        }

        getViewWidget().reset();
        getViewWidget().repaint();

//        for (FileEncryptionInfo info : plugin.readEncryptionInfos(book)) {
//            if (info != null && !EncryptionMethod.isSupported(info.Method)) {
//                showErrorMessage("unsupportedEncryptionMethod", book.getPath());
//                break;
//            }
//        }
    }

    public void showBookTextView() {
        setView(BookTextView);
    }

    @Override
    public void onWindowClosing() {
//        storePosition();
    }



}
