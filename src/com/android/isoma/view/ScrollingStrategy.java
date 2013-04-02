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



import android.text.Layout;
import android.text.Spanned;
import android.widget.TextView;


public class ScrollingStrategy implements PageChangeStrategy {
	
	private BookView bookView;
	
	private TextView childView;
	private int storedPosition;
	
	private Spanned text;
	
	public ScrollingStrategy(BookView bookView) {
		this.bookView = bookView;
		this.childView = bookView.getInnerView();		
	}

	public int getPosition() {
		if ( childView.getText().length() == 0 ) {
			return storedPosition;
		} else {
			int yPos = bookView.getScrollY();
		
			return findTextOffset(findClosestLineBottom(yPos));
		}
	}
	
	public void loadText(Spanned text) {		
		childView.setText(text);
		this.text = text;
		updatePosition();
	}
	
	public void pageDown() {
		this.scroll( bookView.getHeight() - 2 * bookView.getVerticalMargin());
	}
	
	public void pageUp() {
		this.scroll( (bookView.getHeight() - 2* bookView.getVerticalMargin() ) * -1);
	}
	
	public void setPosition(int pos) {
		this.storedPosition = pos;
		updatePosition();
	}
	
	public void clearText() {
		this.childView.setText("");	
		this.text = null;
	}
	
	public void updatePosition() {
		if ( this.storedPosition == -1 || this.childView.getText().length() == 0 ) {			
			return; //Hopefully come back later
		} else {
			
			Layout layout = this.childView.getLayout();
			
			if ( layout != null ) {
				int pos = Math.max(0, this.storedPosition);
				int line = layout.getLineForOffset(pos);
				
				if ( line > 0 ) {
					int newPos = layout.getLineBottom(line -1);
					bookView.scrollTo(0, newPos);
				} else {
					bookView.scrollTo(0, 0);
				}
			}						 
		}
	}
	
	public Spanned getText() {
		return text;
	}
	
	public void reset() {
		this.storedPosition = -1;		
	}
	
	public void clearStoredPosition() {
		this.storedPosition = -1;	
	}
	
	private void scroll( int delta ) {
		
		int currentPos = bookView.getScrollY();
		
		int newPos = currentPos + delta;
		
		bookView.scrollTo(0, findClosestLineBottom(newPos));
		
		if ( bookView.getScrollY() == currentPos ) {
						
			if ( delta < 0 ) {				
				if (! bookView.getSpine().navigateBack() ) {					
					return;
				}
			} else {				
				if ( ! bookView.getSpine().navigateForward() ) {
					return;
				}
			}
			
			this.childView.setText("");
			
			if ( delta > 0 ) {
				bookView.scrollTo(0,0);
				this.storedPosition = -1;
			} else {
				bookView.scrollTo(0, bookView.getHeight());
				
				//We scrolled back up, so we want the very bottom of the text.
				this.storedPosition = Integer.MAX_VALUE;
			}	
			
			bookView.loadText();
		}
	}
	
	private int findClosestLineBottom( int ypos ) {
				
		Layout layout = this.childView.getLayout();
		
		if ( layout == null ) {
			return ypos;
		}
		
		int currentLine = layout.getLineForVertical(ypos);
		
		//System.out.println("Returning line " + currentLine + " for ypos " + ypos);
		
		if ( currentLine > 0 ) {
			int height = layout.getLineBottom(currentLine -1);
			return height;
		} else {
			return 0;
		}		
	}
	
	private int findTextOffset(int ypos) {
		
		Layout layout = this.childView.getLayout();
		if ( layout == null ) {
			return 0;
		}
		
		return layout.getLineStart(layout.getLineForVertical(ypos));		
	}
	
	public boolean isScrolling() {
		return true;
	}

	public boolean isAtEnd() {
		int ypos = bookView.getScrollY() + bookView.getHeight();
		
		Layout layout = this.childView.getLayout();
		if ( layout == null ) {
			return false;
		}
		
		int line = layout.getLineForVertical(ypos);
		return line == layout.getLineCount() -1;
	}
	
}
