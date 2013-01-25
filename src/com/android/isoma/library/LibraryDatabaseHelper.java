package com.android.isoma.library;

import java.util.Date;

import roboguice.inject.ContextScoped;

import com.google.inject.Inject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

@ContextScoped
public class LibraryDatabaseHelper extends SQLiteOpenHelper {
	
	public enum Field { file_name, title, a_first_name, a_last_name,
		date_added, date_last_read, description, cover_image
	}	
	
	private SQLiteDatabase database;
	
	public enum Order { ASC, DESC };
	
	private static final String CREATE_TABLE =
		"create table lib_books ( file_name text primary key, title text, " +
		"a_first_name text, a_last_name text, date_added integer, " +
		"date_last_read integer, description text, cover_image blob );";
	
	private static final String CREATE_TABLE_FTS =
		"create virtual table lib_books_fts using fts3( file_name, title, " +
		"a_first_name , a_last_name, date_added, " +
		"date_last_read , description, cover_image);";
	
	private static final String DROP_TABLE = "drop table lib_books;";
	private static final String DROP_TABLE_FTS = "drop table lib_books_fts;";
	
	private static final String DB_NAME = "Isoma";
	private static final int VERSION = 3;

	@Inject
	public LibraryDatabaseHelper(Context context) {
		super(context, DB_NAME, null, VERSION);		
	}
	
	private synchronized SQLiteDatabase getDataBase() {
		if ( this.database == null ) {
			this.database = getWritableDatabase();
		}
		
		return this.database;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
		db.execSQL(CREATE_TABLE_FTS);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//Nothing to do yet :)		
		db.execSQL(DROP_TABLE);
		db.execSQL(DROP_TABLE_FTS);
		db.execSQL(CREATE_TABLE);
		db.execSQL(CREATE_TABLE_FTS);
		
	}	
	
	public void delete( String fileName ) {
		
		String[] args = { fileName };
		
		getDataBase().delete("lib_books", Field.file_name + " = ?", args );		
	}	
	
	@Override
	public void close() {
		if ( this.database != null ) {
			database.close();
			this.database = null;
		}
	}
	
	public void updateLastRead( String fileName ) {
		
		String whereClause = Field.file_name.toString() + " like ?";
		String[] args = { "%" + fileName };
		
		ContentValues content = new ContentValues();
		content.put( Field.date_last_read.toString(), new Date().getTime() );
		
		getDataBase().update("lib_books", content, whereClause, args);		
	}	
	
	public void storeNewBook(String fileName, String authorFirstName,
			String authorLastName, String title, String description,
			byte[] coverImage, boolean setLastRead) {
		
				
		ContentValues content = new ContentValues();
				
		content.put(Field.title.toString(), title );
		content.put(Field.a_first_name.toString(), authorFirstName );
		content.put(Field.a_last_name.toString(), authorLastName );
		content.put(Field.cover_image.toString(), coverImage );
		content.put(Field.description.toString(), description );
		
		if ( setLastRead ) {
			content.put(Field.date_last_read.toString(), new Date().getTime() );
		}		
			
		content.put(Field.file_name.toString(), fileName );
		content.put(Field.date_added.toString(), new Date().getTime() );			
			
		getDataBase().insert("lib_books", null, content);
	}
	
	public boolean hasBook( String fileName ) {
		Field[] fields = { Field.file_name };
		String[] args = { "%" + fileName };
		
		String whereClause = Field.file_name.toString() + " like ?";
		
		Cursor findBook = getDataBase().query( "lib_books", fieldsAsString(fields), whereClause,
				args, null, null, null );
		
		boolean result =  findBook.getCount() != 0;
		findBook.close();
		
		return result;
	}	
	
	public QueryResult<LibraryBook> findByField( Field fieldName, String fieldValue,
			Field orderField, Order ordering) {
						
		String[] args = { fieldValue };
		String whereClause;
		
		if ( fieldValue == null ) {
			whereClause = fieldName.toString() + " is null";
			args = null;
		} else {
			whereClause = fieldName.toString() + " = ?";			
		}
		
		Cursor cursor = getDataBase().query("lib_books", fieldsAsString(Field.values()), 
				whereClause, args, null, null,
				orderField + " " + ordering  );		
		
		return new LibraryBookResult(cursor);
	}
	
	public QueryResult<LibraryBook> findAllOrderedBy( Field fieldName, Order order ) {
						
		Cursor cursor = getDataBase().query("lib_books", fieldsAsString(Field.values()), 
				fieldName != null ? fieldName.toString() + " is not null" : null,
			    new String[0], null, null,
				fieldName != null ? fieldName.toString() + " " + order.toString() : null );		
		
		return new LibraryBookResult(cursor);
	}	
	
	public QueryResult<LibraryBook> findMonte(String query) {
		//Field[] searchFields = { Field.title,Field.a_first_name, Field.a_last_name };
		
	//query = "monte";
		
		Cursor cursor = getDataBase().query(false, "lib_books", fieldsAsString(Field.values()),Field.a_first_name + " LIKE" + "'%" + query + "%' OR " +Field.title + " LIKE" + "'%" + query + "%' OR "+Field.a_last_name + " LIKE" + "'%" + query + "%'",null,null,null, null, null); 
		//startManagingCursor(cursor);
		
		if(cursor!=null && cursor.getCount()>0){
			//sth	
			/*Context LibraryActivity = null;
			//Toast.makeText(LibraryDatabaseHelper.this, "There are no records for the search query " + query.toString(), Toast.LENGTH_LONG).show();
			 Toast toast=Toast.makeText(LibraryActivity, "There are no records for the search query " + query.toString(), Toast.LENGTH_LONG);
	            toast.setGravity(Gravity.CENTER, 0, 0);
	            toast.show();*/
		}
		
		return new LibraryBookResult(cursor);
	}
	private class LibraryBookResult extends QueryResult<LibraryBook> {
		
		public LibraryBookResult(Cursor cursor) {
			super(cursor);
		}
		
		@Override
		public LibraryBook convertRow(Cursor cursor) {
			
			LibraryBook newBook = new LibraryBook();
			
			newBook.setAuthor(new Author( 
					cursor.getString(Field.a_first_name.ordinal()), 
					cursor.getString(Field.a_last_name.ordinal())));
			
			newBook.setTitle( cursor.getString(Field.title.ordinal()));
			
			newBook.setDescription(cursor.getString(Field.description.ordinal()));
			
			try {
				newBook.setAddedToLibrary(new Date(cursor.getLong(Field.date_added.ordinal())));
			} catch (RuntimeException r){}
			
			try {
				newBook.setLastRead(new Date(cursor.getLong(Field.date_last_read.ordinal())));
			} catch (RuntimeException r){}
			
			newBook.setCoverImage( cursor.getBlob(Field.cover_image.ordinal() ) );			
			newBook.setFileName( cursor.getString(Field.file_name.ordinal()));
			
			return newBook;
		}
	}	
	
	//public void createOrUpdateBook( 
	
	private static String[] fieldsAsString(Field[] values) {		
		
		String[] fieldsAsString = new String[values.length];
		for ( int i=0; i < values.length; i++ ) {
			fieldsAsString[i] = values[i].toString();
		}
		
		return fieldsAsString;
	}
	
}