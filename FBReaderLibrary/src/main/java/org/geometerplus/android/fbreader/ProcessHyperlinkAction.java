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

package org.geometerplus.android.fbreader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.zlibrary.text.view.*;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType;

public class ProcessHyperlinkAction extends FBAction {


    Activity BaseActivity;

    public ProcessHyperlinkAction(Activity baseActivity, FBReaderApp fbreader) {
        super(fbreader);
        BaseActivity = baseActivity;
    }

    @Override
    public boolean isEnabled() {
        return Reader.getTextView().getOutlinedRegion() != null;
    }

    @Override
    protected void run(Object... params) {

        final ZLTextRegion region = Reader.getTextView().getOutlinedRegion();
        if (region == null) {
            return;
        }

        final ZLTextRegion.Soul soul = region.getSoul();
        if (soul instanceof ZLTextHyperlinkRegionSoul) {
            Reader.getTextView().hideOutline();
            Reader.getViewWidget().repaint();
            final ZLTextHyperlink hyperlink = ((ZLTextHyperlinkRegionSoul) soul).Hyperlink;
            switch (hyperlink.Type) {
                case FBHyperlinkType.EXTERNAL:
                    openInBrowser(hyperlink.Id);
                    break;
                case FBHyperlinkType.INTERNAL:
                case FBHyperlinkType.FOOTNOTE: {
                    Reader.tryOpenFootnote(hyperlink.Id);

                    break;
                }
            }
        } else if (soul instanceof ZLTextImageRegionSoul) {
            Reader.getTextView().hideOutline();
            Reader.getViewWidget().repaint();
            final String url = ((ZLTextImageRegionSoul) soul).ImageElement.URL;

        } else if (soul instanceof ZLTextWordRegionSoul) {


        }
    }

    private void openInBrowser(  String url) {

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            BaseActivity.startActivity(intent);
        } catch (Exception e) {
            Log.i(TAG, "openInBrowser: error" +url);
        }

    }
}
