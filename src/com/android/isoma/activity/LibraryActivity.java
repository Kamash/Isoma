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

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

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

public class LibraryActivity extends RoboActivity implements OnItemClickListener  {
	
	@Inject 
	private LibraryService libraryService;
	
	@InjectView(R.id.librarySpinner)
	private Spinner spinner;
	
	@InjectView(R.id.libraryList)
	private ListView listView;
	
	@InjectResource(R.drawable.river_diary)
	private Drawable backupCover;
		
	public static enum Selections {
		 BY_LAST_READ, LAST_ADDED, UNREAD, BY_TITLE, BY_AUTHOR, FIND_MONTE;
	}	
	
	public BookAdapter bookAdapter;
		
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG);
	
	private ProgressDialog waitDialog;
	private ProgressDialog importDialog;	
	
	private SharedPreferences settings;
	
	private Selections lastSelection = Selections.LAST_ADDED;
	
	private static final Logger LOG = LoggerFactory.getLogger(LibraryActivity.class); 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_menu);
		
		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        // You can also assign the title programmatically by passing a
        // CharSequence or resource id.
        actionBar.setTitle(R.string.library_title);
        actionBar.setHomeAction(new IntentAction(this, EpubSite.createIntent(this), R.drawable.ic_menu_ilib));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.addAction(new SearchBookList());
        final Action settingsAction = new IntentAction(this, new Intent(this, PageTurnerPrefsActivity.class), R.drawable.ic_menu_settingis);
        actionBar.addAction(settingsAction);
		
		this.bookAdapter = new BookAdapter(this);
		this.listView.setAdapter(bookAdapter);
		this.listView.setOnItemClickListener(this);
		
		this.settings = PreferenceManager.getDefaultSharedPreferences(this); 
				
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
		            this, R.array.libraryQueries, android.R.layout.simple_spinner_item);
		
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new MenuSelectionListener());		
						
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		
		this.importDialog = new ProgressDialog(this);
		this.importDialog.setOwnerActivity(this);
		
		registerForContextMenu(this.listView);
		
		//Intent for the search
	    Intent intent = getIntent();
		//Intent
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) { 
			
			String query = intent.getStringExtra(SearchManager.QUERY);
			//SearchRecentSuggestions
	        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	               LibraryProvider.AUTHORITY, LibraryProvider.MODE);
	        suggestions.saveRecentQuery(query, null);		
		}
	  
		
	}
	private class SearchBookList extends AbstractAction {

        public SearchBookList() {
            super(R.drawable.action_search);
        }

        public void performAction(View view) {
            /*Action to call open the search dialog*/
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
				Intent intent = new Intent( LibraryActivity.this, BookDetailsActivity.class );
				intent.putExtra("book", selectedBook.getFileName());				
				startActivity(intent);					
				return true;
			}
		});
		
		MenuItem deleteItem = menu.add(R.string.delete);
		
		deleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				libraryService.deleteBook( selectedBook.getFileName() );
				new LoadBooksTask().execute(lastSelection);
				return true;					
			}
		});					
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		MenuItem prefs = menu.add(R.string.prefs);
		prefs.setIcon( getResources().getDrawable(R.drawable.ic_menu_settings) );
		
		prefs.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(LibraryActivity.this, PageTurnerPrefsActivity.class);
				startActivity(intent);
				
				return true;
			}
		});
		
		MenuItem item = menu.add(R.string.scan_books);
		item.setIcon( getResources().getDrawable(R.drawable.book_refresh) );
		
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");

				try {
					startActivityForResult(intent, 1);
				} catch (ActivityNotFoundException e) {
					
					String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
					 new ImportBooksTask().execute(new File(sdCardRoot));
				}
				return true;
			}
		});		
		
		return true;
	}	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

    	if ( resultCode == RESULT_OK && data != null) {
    		// obtain the filename
    		Uri fileUri = data.getData();
    		if (fileUri != null) {
    			String filePath = fileUri.getPath();
    			if (filePath != null) {
    				new ImportBooksTask().execute(new File(filePath));    				
    			}
    		}
    	}	
	}	
	
	@Override
	protected void onStop() {		
		this.libraryService.close();	
		this.waitDialog.dismiss();
		this.importDialog.dismiss();
		super.onStop();
	}
	
	@Override
	public void onBackPressed() {
		finish();			
	}	
	
	@Override
	protected void onPause() {
		
		this.bookAdapter.clear();
		this.libraryService.close();
		//We clear the list to free up memory.
		
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();				
		
		if ( spinner.getSelectedItemPosition() != this.lastSelection.ordinal() ) {
			spinner.setSelection(this.lastSelection.ordinal());
		} else {
			new LoadBooksTask().execute(this.lastSelection);
		}
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
	
	private class MenuSelectionListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
				long arg3) {
			
			Selections newSelections = Selections.values()[pos];
			
			lastSelection = newSelections;
			bookAdapter.clear();		
						
			new LoadBooksTask().execute(newSelections);
		}
		
		//Quick Action code
		
		public void onNothingSelected(AdapterView<?> arg0) {
						
		}
	}	
	
	private class ImportBooksTask extends AsyncTask<File, Integer, Void> implements OnCancelListener {	
		
		private List<String> errors = new ArrayList<String>();
		
		private static final int UPDATE_FOLDER = 1;
		private static final int UPDATE_IMPORT = 2;
		
		private int foldersScanned = 0;
		private int booksImported = 0;
		
		private boolean oldKeepScreenOn;
		private boolean keepRunning;	
		
		@Override
		protected void onPreExecute() {
			importDialog.setTitle(R.string.importing_books);
			importDialog.setMessage(getString(R.string.scanning_epub));
			importDialog.setOnCancelListener(this);
			importDialog.show();			
			
			this.keepRunning = true;
			
			this.oldKeepScreenOn = listView.getKeepScreenOn();
			listView.setKeepScreenOn(true);
		}		
		
		public void onCancel(DialogInterface dialog) {
			LOG.debug("User aborted import.");
			this.keepRunning = false;			
		}
		
		@Override
		protected Void doInBackground(File... params) {
			File parent = params[0];
			List<File> books = new ArrayList<File>();			
			findEpubsInFolder(parent, books);
			
			int total = books.size();
			int i = 0;			
	        
			while ( i < books.size() && keepRunning ) {
				
				File book = books.get(i);
				
				LOG.info("Importing: " + book.getAbsolutePath() );
				try {
					if ( ! libraryService.hasBook(book.getName() ) ) {
						importBook(book);
					}					
				} catch (OutOfMemoryError oom ) {
					errors.add(book.getName() + ": Out of memory.");
					return null;
				}
				
				i++;
				publishProgress(UPDATE_IMPORT, i, total);
				booksImported++;
			}
			
			return null;
		}
		
		private void findEpubsInFolder( File folder, List<File> items) {
			
			if ( folder == null || folder.getAbsolutePath().startsWith(LibraryService.BASE_LIB_PATH) ) {
				return;
			}			
			
			if ( ! keepRunning ) {
				return;
			}		
			
			if ( folder.isDirectory() && folder.listFiles() != null) {
				
				for (File child: folder.listFiles() ) {
					findEpubsInFolder(child, items); 
				}
				
				foldersScanned++;
				publishProgress(UPDATE_FOLDER, foldersScanned);
				
			} else {
				if ( folder.getName().endsWith(".epub") ) {
					items.add(folder);
				}
			}
		}
		
		private void importBook(File file) {
			try {
				
				if ( libraryService.hasBook(file.getName() ) ) {
					return;
				}
				String fileName = file.getAbsolutePath();
				// read epub file
		        EpubReader epubReader = new EpubReader();
		        
		        Log.d("Isoma","This is the file we are trying to import on line 492 "+ file);
		        
		       // Log.INFO("This is the file we are trying to import "+ file);				
				Book importedBook = epubReader.readEpubLazy(fileName, "UTF-8", 
						Arrays.asList(MediatypeService.mediatypes));	
				Log.d("Isoma","At line 500 the imported book to string is  "+ importedBook.toString());
				
				
				boolean copy = settings.getBoolean("copy_to_library", true);
	        	libraryService.storeBook(fileName, importedBook, false, copy);	        		        	
				
			} catch (Exception io ) {
				errors.add( file + ": " + io.getMessage() );
				LOG.error("Error while reading book: " + file, io); 
			}
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			
			if ( values[0] == UPDATE_IMPORT ) {
				String importing = String.format(getString(R.string.importing), values[1], values[2]);
				importDialog.setMessage(importing);
			} else {
				String message = String.format(getString(R.string.scan_folders), values[1]);
				importDialog.setMessage(message);
			}
		}
		
		@Override
		protected void onPostExecute(Void result) {
			
			importDialog.hide();			
			
			OnClickListener dismiss = new OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();						
				}
			};
			
			//If the user cancelled the import, don't bug him/her with alerts.
			if ( (! errors.isEmpty()) && keepRunning ) {
				AlertDialog.Builder builder = new AlertDialog.Builder(LibraryActivity.this);
				builder.setTitle(R.string.import_errors);
				
				builder.setItems( errors.toArray(new String[errors.size()]), null );				
				
				builder.setNeutralButton(android.R.string.ok, dismiss );
				
				builder.show();
			}
			
			listView.setKeepScreenOn(oldKeepScreenOn);
			
			if ( booksImported > 0 ) {			
				//Switch to the "recently added" view.
				if ( spinner.getSelectedItemPosition() == Selections.LAST_ADDED.ordinal() ) {
					new LoadBooksTask().execute(Selections.LAST_ADDED);
				} else {
					spinner.setSelection(Selections.LAST_ADDED.ordinal());
				}
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(LibraryActivity.this);
				builder.setTitle(R.string.no_books_found);
				builder.setMessage( getString(R.string.no_bks_fnd_text) );
				builder.setNeutralButton(android.R.string.ok, dismiss);
				
				builder.show();
			}
		}
	}
	
	private class LoadBooksTask extends AsyncTask<Selections, Integer, QueryResult<LibraryBook>> {		
		
		private Selections sel;
		
		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(R.string.loading_library);
			waitDialog.show();
		}
		
		@Override
		protected QueryResult<LibraryBook> doInBackground(Selections... params) {
			
			this.sel = params[0];
			
			switch ( sel ) {			
			case LAST_ADDED:
				return libraryService.findAllByLastAdded();
			case UNREAD:
				return libraryService.findUnread();
			case BY_TITLE:
				return libraryService.findAllByTitle();
			case BY_AUTHOR:
				return libraryService.findAllByAuthor();
			default:
				return libraryService.findAllByLastRead();
			}			
		}
		
		@Override
		protected void onPostExecute(QueryResult<LibraryBook> result) {
			bookAdapter.setResult(result);
			waitDialog.hide();			
			
			if ( sel == Selections.LAST_ADDED && result.getSize() == 0 ) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(LibraryActivity.this);
				builder.setTitle(R.string.no_books_found);
				builder.setMessage( getString(R.string.scan_bks_question) );
				builder.setPositiveButton(android.R.string.yes, new OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();	
						
						String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
						 new ImportBooksTask().execute(new File(sdCardRoot));
					}
				});
				
				builder.setNegativeButton(android.R.string.no, new OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();						
					}
				});				
				
				builder.show();
			}
		}
		
	}


	public static Intent createIntent(Context context) {
		 Intent i = new Intent(context, LibraryActivity.class);
	        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        return i;
	}
	
	
	
}
