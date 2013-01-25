package com.android.isoma.library;

import android.content.SearchRecentSuggestionsProvider;

public class LibraryProvider extends SearchRecentSuggestionsProvider {

	 String TAG = "Isoma";

	    public static String AUTHORITY = "com.android.isoma.library.LibraryProvider";
	    public final static int MODE = DATABASE_MODE_QUERIES;

	    public LibraryProvider() {
	        setupSuggestions(AUTHORITY, MODE);
	    }

}
