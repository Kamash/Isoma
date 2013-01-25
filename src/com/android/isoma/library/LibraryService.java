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
package com.android.isoma.library;

import java.io.IOException;

import nl.siegmann.epublib.domain.Book;


public interface LibraryService {
	
	public static final String BASE_LIB_PATH = "/sdcard/Isoma/Books/";
	
	/**
	 * Adds a new book to the library database, optionally copying it.
	 * 
	 * @param fileName
	 * @param book
	 * @param updateLastRead
	 * @param copyFile
	 * @throws IOException
	 */
	public void storeBook( String fileName, Book book, boolean updateLastRead, boolean copyFile ) throws IOException;
	
	public QueryResult<LibraryBook> findAllByLastRead();
	
	public QueryResult<LibraryBook> findAllByLastAdded();
	
	public QueryResult<LibraryBook> findAllByTitle();
	
	public QueryResult<LibraryBook> findAllByAuthor();
	
	public QueryResult<LibraryBook> findUnread();
	
	public QueryResult<LibraryBook> findMonte(String query);
	
	public LibraryBook getBook( String fileName );
	
	public boolean hasBook( String fileName );
	
	public void deleteBook( String fileName );
	
	public void close();
}
