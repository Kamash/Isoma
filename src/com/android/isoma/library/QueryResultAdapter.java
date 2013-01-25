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

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * A ListAdapter which uses QueryResult objects.
 * 
 * @author Alex Kuiper
 *
 * @param <T> the type of objects in the result.
 */
public abstract class QueryResultAdapter<T> extends BaseAdapter {

	QueryResult<T> result;
	
	public void setResult(QueryResult<T> result) {
		
		if ( this.result != null ) {
			this.result.close();
		}
		
		this.result = result;
		notifyDataSetChanged();
	}
	
	public void clear() {
		if ( this.result != null ) {
			result.close();
		}
		
		result = null;
		notifyDataSetChanged();
	}
	
	public int getCount() {
		if ( this.result == null ) {
			return 0;
		}
		
		return result.getSize();
	}
	
	public Object getItem(int position) {
		
		if ( result == null ) {
			return null;
		}
		
		return result.getItemAt(position);
	}
	
	public long getItemId(int position) {
		return position;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		return getView(position, result.getItemAt(position), convertView, parent);
	}
	
	public T getResultAt(int position) {
		
		if ( result == null ) {
			return null;
		}
		
		return result.getItemAt(position);
	}
	
	public abstract View getView( int index, T object, View convertView, ViewGroup parent );
	
}
