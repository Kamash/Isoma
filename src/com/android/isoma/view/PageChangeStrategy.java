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
package com.android.isoma.view;

import android.text.Spanned;

public interface PageChangeStrategy {

	/**
	 * Loads the given section of text.
	 * 
	 * This will be a whole "file" from an epub.
	 * 
	 * @param text
	 */
	public void loadText( Spanned text );
	
	/**
	 * Returns the text-offset of the top-left character on the screen.
	 * 
	 * @return
	 */
	public int getPosition();
	
	/**
	 * Tells this strategy to move the window so the specified
	 * position ends up on the top line of the windows.
	 * 
	 * @param pos
	 */
	public void setPosition( int pos );	
	
	/**
	 * Move the view one page up.
	 */
	public void pageUp();
	
	/**
	 * Move the view one page down.
	 */
	public void pageDown();
	
	/** Simple way to differentiate without instanceof **/
	public boolean isScrolling();
	
	/**
	 * Clears all text held in this strategy's buffer.
	 */
	public void clearText();
	
	/**
	 * Clears the stored position in this strategy.
	 */
	public void clearStoredPosition();
	
	/**
	 * Updates all fields to reflect a new configuration.
	 */
	public void updatePosition();
	
	/**
	 * Clears both the buffer and stored position.
	 */
	public void reset();
	
	/**
	 * Gets the text held in this strategy's buffer.
	 * 
	 * @return the text
	 */
	public Spanned getText();
	
}
