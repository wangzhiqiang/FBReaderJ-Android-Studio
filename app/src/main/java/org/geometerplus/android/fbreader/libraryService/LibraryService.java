/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.libraryService;

import java.util.*;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.FileObserver;

import org.geometerplus.zlibrary.core.options.Config;

import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;


import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.*;

import org.geometerplus.android.fbreader.api.FBReaderIntents;

public class LibraryService extends Service {
	private static SQLiteBooksDatabase ourDatabase;
	private static final Object ourDatabaseLock = new Object();


	public final class LibraryImplementation extends LibraryInterface.Stub {
		private final BooksDatabase myDatabase;
		private final List<FileObserver> myFileObservers = new LinkedList<FileObserver>();
		private BookCollection myCollection;

		LibraryImplementation(BooksDatabase db) {
			myDatabase = db;
			myCollection = new BookCollection(
				Paths.systemInfo(LibraryService.this), myDatabase, Paths.bookPath()
			);
			reset(true);
		}

		@Override
		public void reset(final boolean force) {
			Config.Instance().runOnConnect(new Runnable() {
				@Override
				public void run() {
					resetInternal(force);
				}
			});
		}

		private void resetInternal(boolean force) {
			final List<String> bookDirectories = Paths.bookPath();
			if (!force &&
				myCollection.status() != BookCollection.Status.NotStarted &&
				bookDirectories.equals(myCollection.BookDirectories)
			) {
				return;
			}

			deactivate();
			myFileObservers.clear();

			myCollection = new BookCollection(
				Paths.systemInfo(LibraryService.this), myDatabase, bookDirectories
			);
//


			myCollection.addListener(new BookCollection.Listener<DbBook>() {
				@Override
				public void onBookEvent(BookEvent event, DbBook book) {
					final Intent intent = new Intent(FBReaderIntents.Event.LIBRARY_BOOK);
					intent.putExtra("type", event.toString());
					intent.putExtra("book", SerializerUtil.serialize(book));
					sendBroadcast(intent);
				}

				@Override
				public void onBuildEvent(BookCollection.Status status) {
					final Intent intent = new Intent(FBReaderIntents.Event.LIBRARY_BUILD);
					intent.putExtra("type", status.toString());
					sendBroadcast(intent);
				}
			});
			myCollection.startBuild();
		}

		public void deactivate() {
			for (FileObserver observer : myFileObservers) {
				observer.stopWatching();
			}
		}

		@Override
		public String status() {
			return myCollection.status().toString();
		}

		@Override
		public int size() {
			return myCollection.size();
		}

		@Override
		public List<String> books(String query) {
			return SerializerUtil.serializeBookList(
				myCollection.books(SerializerUtil.deserializeBookQuery(query))
			);
		}

		@Override
		public boolean hasBooks(String query) {
			return myCollection.hasBooks(SerializerUtil.deserializeBookQuery(query).Filter);
		}

		@Override
		public List<String> recentBooks() {
			return recentlyOpenedBooks(12);
		}

		@Override
		public List<String> recentlyOpenedBooks(int count) {
			return SerializerUtil.serializeBookList(myCollection.recentlyOpenedBooks(count));
		}

		@Override
		public List<String> recentlyAddedBooks(int count) {
			return SerializerUtil.serializeBookList(myCollection.recentlyAddedBooks(count));
		}

		@Override
		public String getRecentBook(int index) {
			return SerializerUtil.serialize(myCollection.getRecentBook(index));
		}

		@Override
		public String getBookByFile(String path) {
			return SerializerUtil.serialize(myCollection.getBookByFile(path));
		}

		@Override
		public String getBookById(long id) {
			return SerializerUtil.serialize(myCollection.getBookById(id));
		}

		@Override
		public String getBookByUid(String type, String id) {
			return SerializerUtil.serialize(myCollection.getBookByUid(new UID(type, id)));
		}

		@Override
		public String getBookByHash(String hash) {
			return SerializerUtil.serialize(myCollection.getBookByHash(hash));
		}

		@Override
		public List<String> authors() {
			final List<Author> authors = myCollection.authors();
			final List<String> strings = new ArrayList<String>(authors.size());
			for (Author a : authors) {
				strings.add(Util.authorToString(a));
			}
			return strings;
		}

		@Override
		public boolean hasSeries() {
			return myCollection.hasSeries();
		}

		@Override
		public List<String> series() {
			return myCollection.series();
		}

		@Override
		public List<String> tags() {
			final List<Tag> tags = myCollection.tags();
			final List<String> strings = new ArrayList<String>(tags.size());
			for (Tag t : tags) {
				strings.add(Util.tagToString(t));
			}
			return strings;
		}

		@Override
		public List<String> titles(String query) {
			return myCollection.titles(SerializerUtil.deserializeBookQuery(query));
		}

		@Override
		public List<String> firstTitleLetters() {
			return myCollection.firstTitleLetters();
		}

		@Override
		public boolean saveBook(String book) {
			return myCollection.saveBook(SerializerUtil.deserializeBook(book, myCollection));
		}

		@Override
		public boolean canRemoveBook(String book, boolean deleteFromDisk) {
			return myCollection.canRemoveBook(SerializerUtil.deserializeBook(book, myCollection), deleteFromDisk);
		}

		@Override
		public void removeBook(String book, boolean deleteFromDisk) {
			myCollection.removeBook(SerializerUtil.deserializeBook(book, myCollection), deleteFromDisk);
		}

		@Override
		public void addToRecentlyOpened(String book) {
			myCollection.addToRecentlyOpened(SerializerUtil.deserializeBook(book, myCollection));
		}

		@Override
		public void removeFromRecentlyOpened(String book) {
			myCollection.removeFromRecentlyOpened(SerializerUtil.deserializeBook(book, myCollection));
		}

		@Override
		public List<String> labels() {
			return myCollection.labels();
		}

		@Override
		public PositionWithTimestamp getStoredPosition(long bookId) {
			final ZLTextPosition position = myCollection.getStoredPosition(bookId);
			return position != null ? new PositionWithTimestamp(position) : null;
		}

		@Override
		public void storePosition(long bookId, PositionWithTimestamp pos) {
			if (pos == null) {
				return;
			}
			myCollection.storePosition(bookId, new ZLTextFixedPosition.WithTimestamp(
				pos.ParagraphIndex, pos.ElementIndex, pos.CharIndex, pos.Timestamp
			));
		}

		@Override
		public boolean isHyperlinkVisited(String book, String linkId) {
			return myCollection.isHyperlinkVisited(SerializerUtil.deserializeBook(book, myCollection), linkId);
		}

		@Override
		public void markHyperlinkAsVisited(String book, String linkId) {
			myCollection.markHyperlinkAsVisited(SerializerUtil.deserializeBook(book, myCollection), linkId);
		}

		@Override
		public String getCoverUrl(String path) {
			return path;
			//DataUtil.buildUrl(DataConnection, "cover", path);
		}

		@Override
		public String getDescription(String book) {
			return BookUtil.getAnnotation(SerializerUtil.deserializeBook(book, myCollection), myCollection.PluginCollection);
		}

		@Override
		public Bitmap getCover(final String bookString, final int maxWidth, final int maxHeight, boolean[] delayed) {
			// this method kept for compatibility
			delayed[0] = false;
			return null;
		}

		private Bitmap getResizedBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
			if (maxWidth <= 0 || maxHeight <= 0) {
				return null;
			}

			final int bWidth = bitmap.getWidth();
			final int bHeight = bitmap.getHeight();
			if (maxWidth > bWidth && maxHeight > bHeight) {
				return null;
			}

			final int w, h;
			if (bWidth * maxHeight > bHeight * maxWidth) {
				w = maxWidth;
				h = Math.max(1, (int)(bHeight * (w + .5f) / bWidth));
			} else {
				h = maxHeight;
				w = Math.max(1, (int)(bWidth * (h + .5f) / bHeight));
			}
			if (2 * w <= bWidth && 2 * h <= bHeight) {
				return bitmap;
			}
			return Bitmap.createScaledBitmap(bitmap, w, h, false);
		}

		@Override
		public List<String> bookmarks(String query) {
			return SerializerUtil.serializeBookmarkList(myCollection.bookmarks(
				SerializerUtil.deserializeBookmarkQuery(query, myCollection)
			));
		}

		@Override
		public String saveBookmark(String serialized) {
			final Bookmark bookmark = SerializerUtil.deserializeBookmark(serialized);
			myCollection.saveBookmark(bookmark);
			return SerializerUtil.serialize(bookmark);
		}

		@Override
		public void deleteBookmark(String serialized) {
			myCollection.deleteBookmark(SerializerUtil.deserializeBookmark(serialized));
		}

		@Override
		public List<String> deletedBookmarkUids() {
			return myCollection.deletedBookmarkUids();
		}

		@Override
		public void purgeBookmarks(List<String> uids) {
			myCollection.purgeBookmarks(uids);
		}

		@Override
		public String getHighlightingStyle(int styleId) {
			return SerializerUtil.serialize(myCollection.getHighlightingStyle(styleId));
		}

		@Override
		public List<String> highlightingStyles() {
			return SerializerUtil.serializeStyleList(myCollection.highlightingStyles());
		}

		@Override
		public void saveHighlightingStyle(String style) {
			myCollection.saveHighlightingStyle(SerializerUtil.deserializeStyle(style));
		}

		@Override
		public int getDefaultHighlightingStyleId() {
			return myCollection.getDefaultHighlightingStyleId();
		}

		@Override
		public void setDefaultHighlightingStyleId(int styleId) {
			myCollection.setDefaultHighlightingStyleId(styleId);
		}

		@Override
		public void rescan(String path) {
			myCollection.rescan(path);
		}

		@Override
		public String getHash(String book, boolean force) {
			return myCollection.getHash(SerializerUtil.deserializeBook(book, myCollection), force);
		}

		@Override
		public void setHash(String book, String hash) {
			myCollection.setHash(SerializerUtil.deserializeBook(book, myCollection), hash);
		}

		@Override
		public List<String> formats() {
			final List<IBookCollection.FormatDescriptor> descriptors = myCollection.formats();
			final List<String> serialized = new ArrayList<String>(descriptors.size());
			for (IBookCollection.FormatDescriptor d : descriptors) {
				serialized.add(Util.formatDescriptorToString(d));
			}
			return serialized;
		}

		@Override
		public boolean setActiveFormats(List<String> formatIds) {
			if (myCollection.setActiveFormats(formatIds)) {
				reset(true);
				return true;
			} else {
				return false;
			}
		}
	}

	private volatile LibraryImplementation myLibrary;

	@Override
	public void onStart(Intent intent, int startId) {
		onStartCommand(intent, 0, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return myLibrary;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		synchronized (ourDatabaseLock) {
			if (ourDatabase == null) {
				ourDatabase = new SQLiteBooksDatabase(LibraryService.this);
			}
		}
		myLibrary = new LibraryImplementation(ourDatabase);

//		bindService(
//			new Intent(this, DataService.class),
//			DataConnection,
//			DataService.BIND_AUTO_CREATE
//		);
	}

	@Override
	public void onDestroy() {
//		unbindService(DataConnection);

		if (myLibrary != null) {
			final LibraryImplementation l = myLibrary;
			myLibrary = null;
			l.deactivate();
		}
		super.onDestroy();
	}
}
