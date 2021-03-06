/*
 * Copyright (C) 2013 @ilabAfrica
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.isoma.EpubSite;
import com.android.isoma.R;
import com.android.isoma.library.LibraryBook;
import com.android.isoma.library.LibraryProvider;
import com.android.isoma.library.LibraryService;
import com.android.isoma.library.QueryResult;
import com.android.isoma.library.QueryResultAdapter;
import com.google.inject.Inject;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class SearchDetailsActivity extends RoboActivity implements OnItemClickListener  {
	
	@Inject 
	private LibraryService libraryService;
	
	@InjectView(R.id.searchList)
	private ListView listView;
	
	@InjectResource(R.drawable.river_diary)
	private Drawable backupCover;
	
	@InjectView(R.id.searchTerm) 
	private TextView searchValue;
		
	private static enum Selections {
		 BY_LAST_READ, LAST_ADDED, UNREAD, BY_TITLE, BY_AUTHOR;
	}	
	
	public BookAdapter bookAdapter;
	
		
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG);
	
	private ProgressDialog waitDialog;
	private ProgressDialog importDialog;	
	
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(SearchDetailsActivity.class); 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_book);
		
		ActionBar actionBar = (ActionBar) findViewById(R.id.search_actionbar);
        // You can also assign the title programmatically by passing a
        // CharSequence or resource id.
        actionBar.setTitle(R.string.search_title);
        actionBar.setHomeAction(new IntentAction(this, EpubSite.createIntent(this), R.drawable.ic_title_home_default));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.addAction(new SearchBookList());
        final Action settingsAction = new IntentAction(this, new Intent(this, PageTurnerPrefsActivity.class), R.drawable.ic_menu_settingis);
        actionBar.addAction(settingsAction);
		
		this.bookAdapter = new BookAdapter(this);
		this.listView.setAdapter(bookAdapter);
		this.listView.setOnItemClickListener(this);
		
								
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		
		this.importDialog = new ProgressDialog(this);
		this.importDialog.setOwnerActivity(this);
		
		registerForContextMenu(this.listView);
		
		/*Based on example here 
		https://code.google.com/p/androidsearchexample/downloads/list*/	
		
		//Intent for the search
	    Intent intent = getIntent();
		//Intent
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) { 
			
			String query = intent.getStringExtra(SearchManager.QUERY);
			//SearchRecentSuggestions
	        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	               LibraryProvider.AUTHORITY, LibraryProvider.MODE);
	        suggestions.saveRecentQuery(query, null);			
			//Start a query 
	       libraryService.findMonte(query);
			//Start task to display the books
	       new LoadBooksTask().execute();
		}
	}

	private class SearchBookList extends AbstractAction {

        public SearchBookList() {
            super(R.drawable.action_search);
        }

        public void performAction(View view) {
            /*Start the search dialog*/
        	onSearchRequested();
        }
    }
	
	
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
		
		LibraryBook book = this.bookAdapter.getResultAt(pos);
		Intent intent = new Intent(this, ReadingActivity.class);
		
		intent.setData( Uri.parse(book.getFileName()));
		this.setResult(RESULT_OK, intent);
				
		startActivityIfNeeded(intent, 99);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final LibraryBook selectedBook = bookAdapter.getResultAt(info.position);
		
		MenuItem detailsItem = menu.add( R.string.view_details);
		
		detailsItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent( SearchDetailsActivity.this, BookDetailsActivity.class );
				intent.putExtra("book", selectedBook.getFileName());				
				startActivity(intent);					
				return true;
			}
		});
		
		MenuItem deleteItem = menu.add(R.string.delete);
		
		deleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				libraryService.deleteBook( selectedBook.getFileName() );
				//new LoadBooksTask().execute(lastSelection);
				return true;					
			}
		});						
	}	
	
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        return true;	
    }
	  @Override
		public boolean onOptionsItemSelected(MenuItem item) {
		        switch (item.getItemId()) {
		            case R.id.ClearYes:
		            	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
		            			LibraryProvider.AUTHORITY, LibraryProvider.MODE);
		            	suggestions.clearHistory();
		            	Toast.makeText(this, "Suggestions cleared!", Toast.LENGTH_SHORT).show();
		                return true;
		            case R.id.ClearNo:
		                Toast.makeText(this, "Canceled!", Toast.LENGTH_SHORT).show();
		                return true;
		            // Generic catch all for all the other menu resources
		            default:
		                // Don't toast text when a submenu is clicked
		                if (!item.hasSubMenu()) {
		                    Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
		                    return true;
		                }
		                break;
		        }
		        return false;
		    }
	
	@Override
	protected void onStop() {		
		this.libraryService.close();	
		this.waitDialog.dismiss();
		this.importDialog.dismiss();
		super.onStop();
	}
	
	/**
	 * The idea is to make it seamless for the user. Implementing finish will
	 * or might cause the search dialog to appear multiple times.
	 * This just takes you back to the library activity.
	 * @author work
	 *
	 */
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, LibraryActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP| Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}	
	
	@Override
	protected void onPause() {
		
		this.bookAdapter.clear();
		this.libraryService.close();
		//We clear the list to free up memory.
		
		super.onPause();
	}
	
	/**
	 * Based on example found here:
	 * http://www.vogella.de/articles/AndroidListView/article.html
	 * 
	 * @author work
	 *
	 */
	private class BookAdapter extends QueryResultAdapter<LibraryBook> {	
		
		private Context context;
		
		public BookAdapter(Context context) {
			this.context = context;
		}		
		
		
		@Override
		public View getView(int index, LibraryBook book, View convertView,
				ViewGroup parent) {
			
			View rowView;
			
			if ( convertView == null ) {			
				LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.book_row, parent, false);
			} else {
				rowView = convertView;
			}
			
			TextView titleView = (TextView) rowView.findViewById(R.id.bookTitle);
			//titleView.setTypeface(tf,R.style.LibView);
		
			TextView authorView = (TextView) rowView.findViewById(R.id.bookAuthor);
			
			TextView dateView = (TextView) rowView.findViewById(R.id.addedToLibrary);
			
			ImageView imageView = (ImageView) rowView.findViewById(R.id.bookCover);
						
			String authorText = String.format(getString(R.string.book_by),
					book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName() );
			
			authorView.setText(authorText);
			titleView.setText(book.getTitle());
			
			String dateText = String.format(getString(R.string.added_to_lib),
					DATE_FORMAT.format(book.getAddedToLibrary()));
			dateView.setText( dateText );
			
			if ( book.getCoverImage() != null ) {
				byte[] cover = book.getCoverImage();
				imageView.setImageBitmap( BitmapFactory.decodeByteArray(cover, 0, cover.length ));
			} else {
				imageView.setImageDrawable(backupCover);
			}			
			return rowView;
		}		
	}
	
	private class LoadBooksTask extends AsyncTask<Selections, Integer, QueryResult<LibraryBook>> {		
		
		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(R.string.loading_library);
			waitDialog.show();
		}
		
		protected QueryResult<LibraryBook> doInBackground(Selections... params) {
			//Intent to get the search input
				Intent intent = getIntent();
				//Assign the input of the search dialog then adapt it to the library service.
				String query = intent.getStringExtra(SearchManager.QUERY);	
				//Log.d("Isoma", "The item queried is " + query);
				searchValue.setText("You searched: " + query+" ");
				
				return libraryService.findMonte(query);							
		}
		
		protected void onPostExecute(QueryResult<LibraryBook> result) {
			bookAdapter.setResult(result);
			waitDialog.hide();
			waitDialog.dismiss();
		}
		
	}
}
	
	

