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
import android.util.Log;
import java.util.Arrays;
import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class SwitchProfileAction extends FBAction {
	private String myProfileName;

	public  SwitchProfileAction( FBReaderApp fbreader, String profileName) {
		super(fbreader);
		myProfileName = profileName;
	}

	@Override
	public boolean isVisible() {
		return !myProfileName.equals(Reader.ViewOptions.ColorProfileName.getValue());
	}

	@Override
	protected void run(Object... params) {

		Log.i(TAG, "run: "+Arrays.toString(params));

		Reader.ViewOptions.ColorProfileName.setValue(myProfileName);
		Reader.getViewWidget().reset();
		Reader.getViewWidget().repaint();
	}
}
