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


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import com.android.isoma.enc.sendPhoneData;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class EpubSite extends Activity{
	WebView mWebView;
	EditText report;
	Button btnRead;
	MenuItem readBook;
	final static int MENU_READ = 0;
	final static int MENU_HELP = 1;
	static String noma;
	File cacheDir;
	ProgressDialog dialog;
	 ProgressDialog progDialog=null;

   	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.epubview);
	    mWebView = (WebView) findViewById(R.id.webview);
	   
	    mWebView.setDownloadListener(new DownloadListener() {
	    	
	        public void onDownloadStart(final String url, String userAgent,
	                String contentDisposition, String mimetype,
	                long contentLength) {
	          
	           dialog = ProgressDialog.show(EpubSite.this,
	    				"Isoma", "Downloading book. Please wait....",
	    				true);
	    		new Thread() {
	    			@Override
	    			public void run() {
	    			savefile(url);
	    	  dialog.dismiss();
	    			}
			}.start();

	        }
	    });
   	}   
    @Override
	public void onResume() {
        super.onResume();
        new LoadSite().execute();
    }
    public class LoadSite extends AsyncTask<Void, Void, Boolean> {
    	
    	@Override
		protected void onPreExecute() {
    		progDialog=ProgressDialog.show(EpubSite.this,"Isoma website", "Opening page, please wait...",false,false);	
    		 
    		Toast toa=Toast.makeText(EpubSite.this, "Checking internet connection", Toast.LENGTH_SHORT); 
    		toa.setGravity(Gravity.CENTER, 0, 0);
    		toa.show(); 
        }
    	
    	@Override
        protected Boolean doInBackground(Void... params) {

            // TODO Auto-generated method stub
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
             
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
        	    new Thread() {
        			@Override
        			public void run() {
        		 mWebView.loadUrl("Url to your site");
            	 mWebView.setWebViewClient(new HelloWebViewClient());
                sendPhoneData spd=new sendPhoneData(EpubSite.this,mWebView);
                mWebView.addJavascriptInterface(spd, "Android");
        			}
        	    }.start();
                return true;           
            }
            return false;
			//return null;
        }  
    	@Override
		protected void onPostExecute(Boolean result) {
       
    		progDialog.show();
    		
            if (result == true) { 
            	
            Handler handler = new Handler();
        	handler.post(new Runnable(){
         	   public void run(){
         	      progDialog.dismiss();
         	   }
         	});
        	
            Toast toast=Toast.makeText(EpubSite.this, "Done loading", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        
            }

            if (result == false) {
            	Handler handler = new Handler();
            	handler.post(new Runnable(){
            	   public void run(){
            	      progDialog.dismiss();
            	   }
            	});

            Toast toas=Toast.makeText(EpubSite.this, "Sorry. It seems there is a problem "+"\n"+"with your internet connection", Toast.LENGTH_LONG);
            toas.setGravity(Gravity.CENTER, 0, 0);
            toas.show();
           }
            return;
      }
        }
    

    

   	private void savefile(String url1){
   		File rootDir = Environment.getExternalStorageDirectory();
   		
		//get the filename from the URL
		    int  last = url1.lastIndexOf("/");
   			String oldfilename=url1.substring(last+1);

   		//handle the spaces in the filename	
   		   String filename=oldfilename.replaceAll("%20"," ");  			
        URL u;
		try {
			u = new URL(url1);
		
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setDoOutput(true);
            c.connect();
           
         FileOutputStream fileoutput = new FileOutputStream(new File(rootDir, filename));
         
         InputStream in = c.getInputStream();

         byte[] buffer = new byte[1024];
         int bufferLength=0;
         int downloadedSize=0;
         while ( (bufferLength = in.read(buffer)) > 0 ) {  
             fileoutput.write(buffer, 0, bufferLength);  
             downloadedSize += bufferLength;  
         } 
         
         fileoutput.close();
         
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
   	}
   
	private class HelloWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        view.loadUrl(url);
        
            return true;
	    }
	    
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.viewer_menu, menu);
        return true;
	}
	  @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	            // For "Title only": Examples of matching an ID with one assigned in
	            //                   the XML
	            case R.id.ViewerHelp:
	                Toast.makeText(this, "Oops, it seems the file was not loaded!", Toast.LENGTH_SHORT).show();
	                return true;
	            case R.id.ReadYes:
	            	Toast.makeText(this, "See you!", Toast.LENGTH_SHORT).show();
	            	finish();
	                return true;
	            case R.id.ReadNo:
	                Toast.makeText(this, "Phew!", Toast.LENGTH_SHORT).show();
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
	

    }

