/*
 * Copyright (C) 2011 Alex Kuiper <http://www.nightwhistler.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.isoma.htmlspanner.handlers;

import org.htmlcleaner.TagNode;

import com.isoma.htmlspanner.TagNodeHandler;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

/**
 * Applies bold formatting.
 * 
 * @author Alex Kuiper
 *
 */
public class BoldHandler extends TagNodeHandler {

	@Override
	public void handleTagNode(TagNode node,
			SpannableStringBuilder builder, int start, int end) {
		builder.setSpan(new StyleSpan(Typeface.BOLD), start, end,
				Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}
