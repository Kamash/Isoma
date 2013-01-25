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

import com.android.isoma.activity.LibraryActivity;
import com.android.isoma.activity.PageTurnerPrefsActivity;
import com.markupartist.android.widget.ActionBar;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class IsomaActionActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_main);

        final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        //actionBar.setHomeAction(new IntentAction(this, createIntent(this), R.drawable.ic_title_home_demo));
        actionBar.setTitle("Home");

      /*  final Action shareAction = new IntentAction(this, createShareIntent(), R.drawable.ic_menu_library);
        actionBar.addAction(shareAction);
        final Action otherAction = new IntentAction(this, new Intent(this, ReadActivity.class), R.drawable.ic_menu_edit);
        actionBar.addAction(otherAction);*/
        

    }

    public static Intent createIntent(Context context) {
        Intent i = new Intent(context, IsomaActionActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }

    @SuppressWarnings("unused")
	private Intent createShareIntent() {
         final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Shared from the ActionBar widget.");
        return Intent.createChooser(intent, "Share");
    }
    
    /**
     * Handle the click on the home button.
     * 
     * @param v View
     * @return void
     */

    public void onClickHome (View v)
    {
        goHome (this);
    }

 

    /**
     * Handle the click on the About button.
     * 
     * @param v View
     * @return void
     */

   /* public void onClickAbout (View v)
    {
        startActivity (new Intent(getApplicationContext(), LibraryActivity.class));
    }*/

    /**
     * Handle the click of a Feature button.
     * 
     * @param v View
     * @return void
     */

    public void onClickFeature (View v)
    {
        int id = v.getId ();
        switch (id) {
          case R.id.home_btn_feature1 :
               startActivity (new Intent(getApplicationContext(), LibraryActivity.class));
               break;
          /*case R.id.home_btn_feature2 :
               startActivity (new Intent(getApplicationContext(), MenuInflateFromXml.class));
               break;*/
          case R.id.home_btn_feature3 :
        	  startActivity (new Intent(getApplicationContext(), EpubSite.class));
               break;
          case R.id.home_btn_feature4 :
               startActivity (new Intent(getApplicationContext(), PageTurnerPrefsActivity.class));
               break;
          /*  case R.id.home_btn_feature5 :
               startActivity (new Intent(getApplicationContext(), ReaderActivity.class));
               break;
        case R.id.home_btn_feature6 :
               startActivity (new Intent(getApplicationContext(), F6Activity.class));
               break;*/
          default: 
        	   break;
        }
    }
    /**
     * Go back to the home activity.
     * 
     * @param context Context
     * @return void
     */

    public void goHome(Context context) 
    {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity (intent);
    }
}