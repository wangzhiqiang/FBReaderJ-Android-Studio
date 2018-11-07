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
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.ui.android.R;

public class SelectionShowPanelAction extends FBAction {


    private Activity context;

    public SelectionShowPanelAction(Activity baseActivity, FBReaderApp fbreader) {
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


    private void showSelectionPanel() {
        final FBView view = Reader.getTextView();
        int x = view.getSelectionStartX();
        int y = view.getSelectionStartY();

        if (null == popupWindow) {

            popupWindow = new PopupWindow(context);
            View pop = View.inflate(context, R.layout.selection_panel, null);
            ColorDrawable dw = new ColorDrawable(0x00000000);
            popupWindow.setBackgroundDrawable(dw);
            popupWindow.setContentView(pop);
            pop.findViewById(R.id.select_popup_copy)
                .setOnClickListener(v -> {
                        Reader.runAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD);
                        hide();


                        TextSnippet snippet = view.getSelectedSnippet();

                    Log.i(TAG, "showSelectionPanel: "+snippet.getText());

                        ClipboardManager cmb = (ClipboardManager) context
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                        cmb.setText(snippet.getText());

                        Reader.runAction(ActionCode.SELECTION_CLEAR);


                    }
                );

            pop.findViewById(R.id.select_popup_dict)
                .setOnClickListener(v -> {
                    Reader.runAction(ActionCode.SELECTION_TRANSLATE);
                    hide();

                    //TODO 跳转字典

                    Reader.runAction(ActionCode.SELECTION_CLEAR);
                });

        }

        Log.i(TAG, "showSelectionPanel: x:" + x + " y:" + y);

        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

        popupWindow.setOutsideTouchable(true);
        View contentView = context.findViewById(android.R.id.content);

        popupWindow.showAtLocation(contentView, Gravity.NO_GRAVITY, x, y - dip2px(context, 45));

    }


    private int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);

    }

    private void hide() {
        if (null != popupWindow && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }
}
