/*
 * Copyright (C) 2012 @ilabAfrica
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
package com.android.isoma;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;

    public ImageAdapter(Context c) {
        mContext = c;
    }

    
	public int getCount() {
        return mThumbIds.length;
    }

    
	public Object getItem(int position) {
        return null;
    }

    
	public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
  
	public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(15,15,15,15);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(mThumbIds[position]);
        return imageView;
    }

    // references to our images
    private Integer[] mThumbIds = {
            R.drawable.ic_menu_library, R.drawable.ic_menu_archive,
            R.drawable.ic_menu_navigate, R.drawable.ic_menu_settings,
            R.drawable.ic_menu_help, R.drawable.ic_menu_more,
                
    };
}
