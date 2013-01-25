/*
 * Copyright (C) 2011 Alex Kuiper
 * 
 * This file is part of Isoma
 *
 * Isoma is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Isoma is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Isoma.  If not, see <http://www.gnu.org/licenses/>.*
 */
package com.android.isoma.activity;

import java.text.DateFormat;
import java.util.Date;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.isoma.IsomaActionActivity;
import com.android.isoma.R;
import com.android.isoma.library.LibraryBook;
import com.android.isoma.library.LibraryService;
import com.google.inject.Inject;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class BookDetailsActivity extends RoboActivity {
	
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG);
	
	@InjectView(R.id.coverImage)
	private ImageView coverView;
	
	@InjectExtra("book")
	private String book;
	
	@InjectView(R.id.titleField) 
	private TextView titleView;
	
	@InjectView(R.id.authorField)
	private TextView authorView;
	
	@InjectView(R.id.lastRead)
	private TextView lastRead;
	
	@InjectView(R.id.addedToLibrary)
	private TextView added;
	
	@InjectView(R.id.bookDescription)
	private TextView descriptionView;
	
	@Inject
	private LibraryService libraryService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.book_details);
		
		ActionBar detailsActionBar = (ActionBar) findViewById(R.id.detailsActionbar);
        // You can also assign the title programmatically by passing a
        // CharSequence or resource id.
		detailsActionBar.setTitle(R.string.book_details);
		detailsActionBar.setHomeAction(new IntentAction(this, IsomaActionActivity.createIntent(this), R.drawable.ic_title_home_default));
		detailsActionBar.setDisplayHomeAsUpEnabled(true);
        final Action bookSettingsAction = new IntentAction(this, new Intent(this, PageTurnerPrefsActivity.class), R.drawable.ic_menu_settingis);
        detailsActionBar.addAction(bookSettingsAction);
        
		
		LibraryBook libraryBook = libraryService.getBook(this.book);
		
		if ( libraryBook != null ) {

			if ( libraryBook.getCoverImage() != null ) {			
				coverView.setImageBitmap( BitmapFactory.decodeByteArray(libraryBook.getCoverImage(),
						0, libraryBook.getCoverImage().length));
			} else {			
				coverView.setImageDrawable( getResources().getDrawable(R.drawable.river_diary));
			}

			titleView.setText(libraryBook.getTitle());
			String authorText = String.format( getString(R.string.book_by),
					 libraryBook.getAuthor().getFirstName() + " " 
					 + libraryBook.getAuthor().getLastName() );
			authorView.setText( authorText );

			if (libraryBook.getLastRead() != null && ! libraryBook.getLastRead().equals(new Date(0))) {
				String lastReadText = String.format(getString(R.string.last_read),
						DATE_FORMAT.format(libraryBook.getLastRead()));
				lastRead.setText( lastReadText );
			}

			String addedText = String.format( getString(R.string.added_to_lib),
					DATE_FORMAT.format(libraryBook.getAddedToLibrary()));
			added.setText( addedText );
			descriptionView.setText(libraryBook.getDescription());
		}
	}
	
}
