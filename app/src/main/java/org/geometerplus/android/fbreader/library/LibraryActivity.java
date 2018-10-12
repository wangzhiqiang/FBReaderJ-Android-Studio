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

package org.geometerplus.android.fbreader.library;


import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.ListView;


import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.library.*;
import org.geometerplus.fbreader.tree.FBTree;

import org.geometerplus.android.util.*;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.fbreader.tree.TreeActivity;


//书库列表页面 多级
public class LibraryActivity extends TreeActivity<LibraryTree> implements

	View.OnCreateContextMenuListener, IBookCollection.Listener<Book> {

	static final String START_SEARCH_ACTION = "action.fbreader.library.start-search";

	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private volatile RootTree myRootTree;
	private Book mySelectedBook;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);



		mySelectedBook = FBReaderIntents.getBookExtra(getIntent(), myCollection);

		new LibraryTreeAdapter(this);

		getListView().setTextFilterEnabled(true);
		getListView().setOnCreateContextMenuListener(this);

		deleteRootTree();

		myCollection.bindToService(this, new Runnable() {
			@Override
			public void run() {
				setProgressBarIndeterminateVisibility(!myCollection.status().IsComplete);
				myRootTree = new RootTree(myCollection, PluginCollection.Instance(Paths.systemInfo(LibraryActivity.this)));
				myCollection.addListener(LibraryActivity.this);
				init(getIntent());
			}
		});
	}


	@Override
	protected LibraryTree getTreeByKey(FBTree.Key key) {
		return key != null ? myRootTree.getLibraryTree(key) : myRootTree;
	}

	private synchronized void deleteRootTree() {
		if (myRootTree != null) {
			myCollection.removeListener(this);
			myCollection.unbind();
			myRootTree = null;
		}
	}

	@Override
	protected void onDestroy() {
		deleteRootTree();
		super.onDestroy();
	}

	@Override
	public boolean isTreeSelected(FBTree tree) {
		final LibraryTree lTree = (LibraryTree)tree;
		return lTree.isSelectable() && lTree.containsBook(mySelectedBook);
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position, long rowId) {
		final LibraryTree tree = (LibraryTree)getTreeAdapter().getItem(position);
		if (tree instanceof ExternalViewTree) {
			runOrInstallExternalView(true);
		} else {
			final Book book = tree.getBook();
			if (book != null) {
				showBookInfo(book);
			} else {
				openTree(tree);
			}
		}
	}

	//
	// show BookInfoActivity
	//
	private void showBookInfo(Book book) {
		final Intent intent = new Intent(getApplicationContext(), BookInfoActivity.class);
		FBReaderIntents.putBookExtra(intent, book);
		OrientationUtil.startActivity(this, intent);
	}




	private void runOrInstallExternalView(boolean install) {
		try {
			startActivity(new Intent(FBReaderIntents.Action.EXTERNAL_LIBRARY));
			finish();
		} catch (ActivityNotFoundException e) {
			if (install) {
				PackageUtil.installFromMarket(this, "org.fbreader.plugin.library");
			}
		}
	}


	@Override
	public void onBookEvent(BookEvent event, Book book) {
		if (getCurrentTree().onBookEvent(event, book)) {
			getTreeAdapter().replaceAll(getCurrentTree().subtrees(), true);
		}
	}

	@Override
	public void onBuildEvent(IBookCollection.Status status) {
		setProgressBarIndeterminateVisibility(!status.IsComplete);
	}
}
