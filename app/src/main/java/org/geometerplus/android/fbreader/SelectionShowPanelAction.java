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

package org.geometerplus.android.fbreader;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import co.anybooks.ui.BookReadActivity;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.R;

public class SelectionShowPanelAction extends FBAction {


    private Activity context;

    public SelectionShowPanelAction(BookReadActivity baseActivity, FBReaderApp fbreader) {
        super(fbreader);
        context = baseActivity;
    }

    @Override
    public boolean isEnabled() {
        return !Reader.getTextView().isSelectionEmpty();
    }

    @Override
    protected void run(Object... params) {
        showSelectionPanel();
    }

    private PopupWindow popupWindow;

    public void showSelectionPanel() {
        final ZLTextView view = Reader.getTextView();
        int start = view.getSelectionStartY();
        int end = view.getSelectionEndY();

        if (null == popupWindow) {
            popupWindow = new PopupWindow(context);
            View contentView = View.inflate(context, R.layout.selection_panel, null);
            popupWindow.setContentView(contentView);
        }

        Log.i(TAG, "showSelectionPanel: x:" + start + " y:" + end);

        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

        popupWindow
            .showAtLocation(context.findViewById(android.R.id.content), Gravity.CENTER_HORIZONTAL
                , start, end);

    }
}
