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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.Gravity;
import android.view.MenuItem;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
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
	ProgressDialog mProgressDialog,progDialog,loadingProgress=null;
	public String url="";

   	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.epubview);
	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setJavaScriptEnabled(true);
	    mWebView.getSettings().setAppCacheEnabled(true);
	    mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
	    
	    getResources();
	    
	    mProgressDialog = new ProgressDialog(this);
	    mProgressDialog.setMessage("Downloading book");
	    mProgressDialog.setIndeterminate(false);
	    mProgressDialog.setMax(100);
	    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    
	    loadingProgress = new ProgressDialog(this);
	    loadingProgress.setMessage("Loading..");
	    loadingProgress.setIndeterminate(false);
	    loadingProgress.setMax(100);
	    loadingProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		
	    mWebView.setDownloadListener(new DownloadListener() {
	    	
	        public void onDownloadStart(final String url, String userAgent,
	                String contentDisposition, String mimetype,
	                long contentLength) {
	          
	        	DownloadFilesTask downloadFile = new DownloadFilesTask();
	        	downloadFile.execute(url);
	    			
	    		}
	    
	    });
   	}   
   	
   	public static Intent createIntent(Context context) {
        Intent i = new Intent(context, EpubSite.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }
   	
    @Override
	public void onResume() {
        super.onResume();
        new LoadSite().execute();
    }
    /**
     * 
     * @author admin
     * Task to load the mobile website
     *
     */
    public class LoadSite extends AsyncTask<Void, Void, Boolean> {
    	
    	@Override
		protected void onPreExecute() {
    		
    		progDialog=ProgressDialog.show(EpubSite.this,"Isoma website", "Loading..",false,false,new DialogInterface.OnCancelListener(){
                public void onCancel(DialogInterface dialog) {
                    LoadSite.this.cancel(true);
                    finish();
                }
            });	
    
 		 
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
        		
        		url=getResources().getString(R.string.server_url);
        		
        		mWebView.loadUrl("http"+url);
            	mWebView.setWebViewClient(new HelloWebViewClient());
            	mWebView.setWebChromeClient(new WebChromeClient() {
            		   @Override
                public void onProgressChanged(WebView view, int newProgress) {

                super.onProgressChanged(view, newProgress);
                loadingProgress.setProgress(newProgress);
            		  
            	// hide the progress bar if the loading is complete

            	 if (newProgress == 100) {
            		 loadingProgress.dismiss();	   
            	 } else{
            		 loadingProgress.show();
            	 }

             }});
            	
              	WebSettings ws=mWebView.getSettings();
                ws.setJavaScriptEnabled(true);
                sendPhoneData spd=new sendPhoneData(EpubSite.this,mWebView);
                mWebView.addJavascriptInterface(spd, "Android");
            	            
        			}
        	    }.start();
                return true;           
            }
            return false;
			
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
    /**
     * 
     * @author admin
     * Task to download book from the mobile website
     *
     */
    
    private class DownloadFilesTask extends AsyncTask<String, Integer, String> {
    	
		@Override
		protected String doInBackground(String... url) {
			File rootDir = Environment.getExternalStorageDirectory();
			//get the filename from the URL
			    int  last = url[0].lastIndexOf("/");
	   			String oldfilename=url[0].substring(last+1);

	   		//handle the spaces in the filename	
	   		   String filename=oldfilename.replaceAll("%20"," ");
	   			
	        URL u;
			try {
				u = new URL(url[0]);
			    
	            HttpURLConnection c = (HttpURLConnection) u.openConnection();
	            c.setRequestMethod("GET");
	            c.setDoOutput(true);
	            c.connect();
	            int lengthOfFile = c.getContentLength();
	           
	         FileOutputStream fileoutput = new FileOutputStream(new File(rootDir, filename));
	         
	         InputStream in = c.getInputStream();

	         byte[] buffer = new byte[1024];
	         int bufferLength=0;
	         int downloadedSize=0;
	        
	         while ( (bufferLength = in.read(buffer)) > 0 ) {  
	        	 
	             fileoutput.write(buffer, 0, bufferLength);  
	             downloadedSize += bufferLength;  
	             publishProgress((int) (downloadedSize * 100 / lengthOfFile));
	         } 
	         
	         fileoutput.close();
	         
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        mProgressDialog.show();
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        mProgressDialog.setProgress(progress[0]);
	    }
	    @Override
		protected void onPostExecute(String result) {
	    	super.onPostExecute(result);
	    	mProgressDialog.dismiss();
	    }   
    	
    }

   
	private class HelloWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        view.loadUrl(url);
            return true;
	    }   
	}
	
    }

