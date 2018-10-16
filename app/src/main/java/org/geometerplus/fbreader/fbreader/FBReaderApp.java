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

import java.util.*;

import org.fbreader.util.ComparisonUtil;

import org.geometerplus.zlibrary.core.application.*;
import org.geometerplus.zlibrary.core.drm.FileEncryptionInfo;
import org.geometerplus.zlibrary.core.drm.EncryptionMethod;
import org.geometerplus.zlibrary.core.util.*;

import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.*;

import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.bookmodel.*;
import org.geometerplus.fbreader.fbreader.options.*;
import org.geometerplus.fbreader.formats.*;
//import org.geometerplus.fbreader.network.sync.SyncData;
import org.geometerplus.fbreader.util.*;

public final class FBReaderApp extends ZLApplication {



    public final MiscOptions MiscOptions = new MiscOptions();
    public final ImageOptions ImageOptions = new ImageOptions();
    public final ViewOptions ViewOptions = new ViewOptions();
    public final PageTurningOptions PageTurningOptions = new PageTurningOptions();
//    public final SyncOptions SyncOptions = new SyncOptions();

    private final ZLKeyBindings myBindings = new ZLKeyBindings();

    public final FBView BookTextView;
    public final FBView FootnoteView;
    private String myFootnoteModelId;

    public volatile BookModel Model;
    public volatile Book ExternalBook;

    private ZLTextPosition myJumpEndPosition;
    private Date myJumpTimeStamp;

//    public final IBookCollection<Book> Collection;

//	private final SyncData mySyncData = new SyncData();

    public FBReaderApp(SystemInfo systemInfo) {
        super(systemInfo);

//        Collection = collection;

//        collection.addListener(new IBookCollection.Listener<Book>() {
//            @Override
//            public void onBookEvent(BookEvent event, Book book) {
//                switch (event) {
//                    case BookmarkStyleChanged:
//                    case BookmarksUpdated:
//                        if (Model != null && (book == null || collection
//                            .sameBook(book, Model.Book))) {
//                            if (BookTextView.getModel() != null) {
////                                setBookmarkHighlightings(BookTextView, null);
//                            }
//                            if (FootnoteView.getModel() != null && myFootnoteModelId != null) {
////                                setBookmarkHighlightings(FootnoteView, myFootnoteModelId);
//                            }
//                        }
//                        break;
//                    case Updated:
////                        onBookUpdated(book);
//                        break;
//                }
//            }
//
//            @Override
//            public void onBuildEvent(IBookCollection.Status status) {
//            }
//        });

        addAction(ActionCode.EXIT, new ExitAction(this));

        BookTextView = new FBView(this);
        FootnoteView = new FBView(this);

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
        FootnoteView.clearCaches();
    }



    private void setFootnoteModel(String modelId) {
        final ZLTextModel model = Model.getFootnoteModel(modelId);
        FootnoteView.setModel(model);
        if (model != null) {
            myFootnoteModelId = modelId;
//            setBookmarkHighlightings(FootnoteView, modelId);
        }
    }

    private synchronized void openBookInternal(final Book book, Bookmark bookmark, boolean force) {
//        if (!force && Model != null && Collection.sameBook(book, Model.Book)) {
//            if (bookmark != null) {
//                gotoBookmark(bookmark, false);
//            }
//            return;
//        }
//
//        hideActivePopup();
//        storePosition();

        BookTextView.setModel(null);
        FootnoteView.setModel(null);
        clearTextCaches();
        Model = null;
        ExternalBook = null;
        System.gc();
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
//            Collection.saveBook(book);
            ZLTextHyphenator.Instance().load(book.getLanguage());
            BookTextView.setModel(Model.getTextModel());
//            setBookmarkHighlightings(BookTextView, null);
            gotoStoredPosition();
            if (bookmark == null) {
                setView(BookTextView);
            } else {
                gotoBookmark(bookmark, false);
            }
//
        } catch (BookReadingException e) {
            processException(e);
        }

        getViewWidget().reset();
        getViewWidget().repaint();

        for (FileEncryptionInfo info : plugin.readEncryptionInfos(book)) {
            if (info != null && !EncryptionMethod.isSupported(info.Method)) {
                showErrorMessage("unsupportedEncryptionMethod", book.getPath());
                break;
            }
        }
    }

    private List<Bookmark> invisibleBookmarks() {
//        final List<Bookmark> bookmarks = Collection.bookmarks(
//            new BookmarkQuery(Model.Book, false, 10)
//        );
//        Collections.sort(bookmarks, new Bookmark.ByTimeComparator());
//        return bookmarks;
        return null;
    }



    private void gotoBookmark(Bookmark bookmark, boolean exactly) {
        final String modelId = bookmark.ModelId;
        if (modelId == null) {
//            addInvisibleBookmark();
            if (exactly) {
                BookTextView.gotoPosition(bookmark);

            }
            setView(BookTextView);
        } else {
            setFootnoteModel(modelId);
            if (exactly) {
                FootnoteView.gotoPosition(bookmark);
            }
            setView(FootnoteView);
        }
        getViewWidget().repaint();
        storePosition();
    }

    public void showBookTextView() {
        setView(BookTextView);
    }

    @Override
    public void onWindowClosing() {
        storePosition();
    }

    private class PositionSaver implements Runnable {

        private final Book myBook;
        private final ZLTextPosition myPosition;
        private final RationalNumber myProgress;

        PositionSaver(Book book, ZLTextPosition position, RationalNumber progress) {
            myBook = book;
            myPosition = position;
            myProgress = progress;
        }

        @Override
        public void run() {
//            Collection.storePosition(myBook.getId(), myPosition);
            myBook.setProgress(myProgress);
//            Collection.saveBook(myBook);
        }
    }

    private class SaverThread extends Thread {

        private final List<Runnable> myTasks =
            Collections.synchronizedList(new LinkedList<Runnable>());

        SaverThread() {
            setPriority(MIN_PRIORITY);
        }

        void add(Runnable task) {
            myTasks.add(task);
        }

        @Override
        public void run() {
            while (true) {
                synchronized (myTasks) {
                    while (!myTasks.isEmpty()) {
                        myTasks.remove(0).run();
                    }
                }
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                }
            }
        }
    }


    private final SaverThread mySaverThread = new SaverThread();
    private volatile ZLTextPosition myStoredPosition;
    private volatile Book myStoredPositionBook;

    private ZLTextFixedPosition getStoredPosition(Book book) {
//		final ZLTextFixedPosition.WithTimestamp fromServer =
//			mySyncData.getAndCleanPosition(Collection.getHash(book, true));
//        final ZLTextFixedPosition.WithTimestamp local =
//            Collection.getStoredPosition(book.getId());

        return null;
//
    }

    private void gotoStoredPosition() {
        myStoredPositionBook = Model != null ? Model.Book : null;
        if (myStoredPositionBook == null) {
            return;
        }
        myStoredPosition = getStoredPosition(myStoredPositionBook);
        BookTextView.gotoPosition(myStoredPosition);
        savePosition();
    }

    public void storePosition() {
        final Book bk = Model != null ? Model.Book : null;
        if (bk != null && bk == myStoredPositionBook && myStoredPosition != null
            && BookTextView != null) {
            final ZLTextPosition position = new ZLTextFixedPosition(BookTextView.getStartCursor());
            if (!myStoredPosition.equals(position)) {
                myStoredPosition = position;
                savePosition();
            }
        }
    }

    private void savePosition() {
        final RationalNumber progress = BookTextView.getProgress();
        synchronized (mySaverThread) {
            if (!mySaverThread.isAlive()) {
                mySaverThread.start();
            }
            mySaverThread.add(new PositionSaver(myStoredPositionBook, myStoredPosition, progress));
        }
    }

}
