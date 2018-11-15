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

package org.geometerplus.fbreader.fbreader.options;

import java.util.*;

import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.options.*;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;

public class ColorProfile {
	public static final String DAY = "light";
	public static final String NIGHT = "night";

	private static final ArrayList<String> ourNames = new ArrayList<String>();
	private static final HashMap<String,ColorProfile> ourProfiles = new HashMap<String,ColorProfile>();

	public static List<String> names() {
		if (ourNames.isEmpty()) {
			final int size = new ZLIntegerOption("Colors", "NumberOfSchemes", 0).getValue();
			if (size == 0) {
				ourNames.add(DAY);
				ourNames.add(NIGHT);
			} else {
				for (int i = 0; i < size; ++i) {
					ourNames.add(new ZLStringOption("Colors", "Scheme" + i, "").getValue());
				}
			}
		}
		return Collections.unmodifiableList(ourNames);
	}

	public static ColorProfile get(String name) {
		ColorProfile profile = ourProfiles.get(name);
		if (profile == null) {
			profile = new ColorProfile(name);
			ourProfiles.put(name, profile);
		}
		return profile;
	}

	public final String Name;

	public final ZLStringOption WallpaperOption;

	public final ZLEnumOption<ZLPaintContext.FillMode> FillModeOption;
	//阅读器背景色
	public final ZLColorOption BackgroundOption;
	//选中的颜色
	public final ZLColorOption SelectionBackgroundOption;
	public final ZLColorOption SelectionForegroundOption;
	//标记的颜色
	public final ZLColorOption HighlightingForegroundOption;
	public final ZLColorOption HighlightingBackgroundOption;
	//阅读器文字
	// 0 0 0  - 148 148 153
	public final ZLColorOption RegularTextOption;
	//链接的颜色
	public final ZLColorOption HyperlinkTextOption;

	public final ZLColorOption VisitedHyperlinkTextOption;
	public final ZLColorOption FooterFillOption;
	public final ZLColorOption FooterNGBackgroundOption;
	//页脚文字颜色
	public final ZLColorOption FooterNGForegroundOption;
	public final ZLColorOption FooterNGForegroundUnreadOption;

	private ColorProfile(String name, ColorProfile base) {
		this(name);
		WallpaperOption.setValue(base.WallpaperOption.getValue());

		FillModeOption.setValue(base.FillModeOption.getValue());
		BackgroundOption.setValue(base.BackgroundOption.getValue());
		SelectionBackgroundOption.setValue(base.SelectionBackgroundOption.getValue());
		SelectionForegroundOption.setValue(base.SelectionForegroundOption.getValue());
		HighlightingForegroundOption.setValue(base.HighlightingForegroundOption.getValue());
		HighlightingBackgroundOption.setValue(base.HighlightingBackgroundOption.getValue());
		RegularTextOption.setValue(base.RegularTextOption.getValue());
		HyperlinkTextOption.setValue(base.HyperlinkTextOption.getValue());
		VisitedHyperlinkTextOption.setValue(base.VisitedHyperlinkTextOption.getValue());
		FooterFillOption.setValue(base.FooterFillOption.getValue());
		FooterNGBackgroundOption.setValue(base.FooterNGBackgroundOption.getValue());
		FooterNGForegroundOption.setValue(base.FooterNGForegroundOption.getValue());
		FooterNGForegroundUnreadOption.setValue(base.FooterNGForegroundUnreadOption.getValue());
	}

	private static ZLColorOption createOption(String profileName, String optionName, int r, int g, int b) {
		return new ZLColorOption("Colors", profileName + ':' + optionName, new ZLColor(r, g, b));
	}

	private static ZLColorOption createNullOption(String profileName, String optionName) {
		return new ZLColorOption("Colors", profileName + ':' + optionName, null);
	}

	private ColorProfile(String name) {
		Name = name;
		if (NIGHT.equals(name)) {
			WallpaperOption =
				new ZLStringOption("Colors", name + ":Wallpaper", "");
			FillModeOption =
				new ZLEnumOption<>("Colors", name + ":FillMode", ZLPaintContext.FillMode.tile);
			BackgroundOption =
				createOption(name, "Background", 35, 35, 38);
			SelectionBackgroundOption =
				createOption(name, "SelectionBackground", 0, 158, 157);
			SelectionForegroundOption =
				createNullOption(name, "SelectionForeground");
			HighlightingBackgroundOption =
				createOption(name, "Highlighting", 96, 96, 128);
			HighlightingForegroundOption =
				createNullOption(name, "HighlightingForeground");
			RegularTextOption =
				createOption(name, "Text", 148, 148, 153);
			HyperlinkTextOption =
				createOption(name, "Hyperlink", 60, 142, 224);
			VisitedHyperlinkTextOption =
				createOption(name, "VisitedHyperlink", 200, 139, 255);
			FooterFillOption =
				createOption(name, "FooterFillOption", 85, 85, 85);
			FooterNGBackgroundOption =
				createOption(name, "FooterNGBackgroundOption", 68, 68, 68);
			FooterNGForegroundOption =
				createOption(name, "FooterNGForegroundOption", 73, 73, 77);
			FooterNGForegroundUnreadOption =
				createOption(name, "FooterNGForegroundUnreadOption", 119, 119, 119);
		} else {
			WallpaperOption =
				new ZLStringOption("Colors", name + ":Wallpaper", "");//wallpapers/sepia.jpg
			FillModeOption =
				new ZLEnumOption<ZLPaintContext.FillMode>("Colors", name + ":FillMode", ZLPaintContext.FillMode.tile);
			BackgroundOption =
				createOption(name, "Background", 247, 248, 250);
			SelectionBackgroundOption =
//				createOption(name, "SelectionBackground", 82, 131, 194);
                createOption(name, "SelectionBackground", 0, 158, 157);
			SelectionForegroundOption =
				createNullOption(name, "SelectionForeground");
			HighlightingBackgroundOption =
				createOption(name, "Highlighting", 255, 192, 128);
			HighlightingForegroundOption =
				createNullOption(name, "HighlightingForeground");
			RegularTextOption =
				createOption(name, "Text", 0, 0, 0);
			HyperlinkTextOption =
				createOption(name, "Hyperlink", 60, 139, 255);
			VisitedHyperlinkTextOption =
				createOption(name, "VisitedHyperlink", 200, 139, 255);
			FooterFillOption =
				createOption(name, "FooterFillOption", 170, 170, 170);
			FooterNGBackgroundOption =
				createOption(name, "FooterNGBackgroundOption", 68, 68, 68);
			FooterNGForegroundOption =
				createOption(name, "FooterNGForegroundOption", 173, 173, 173);
			FooterNGForegroundUnreadOption =
				createOption(name, "FooterNGForegroundUnreadOption", 119, 119, 119);
		}
	}
}
