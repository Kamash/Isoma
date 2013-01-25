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
package com.android.isoma.enc;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.webkit.WebView;

public class sendPhoneData {
Context context;
private static String URL="your server url";
private ArrayList<NameValuePair> postParameters; 
String Response;
WebView webview;
 
public sendPhoneData(Context c,WebView m){
	
	 webview=m;
	 context=c;
	 Response=null;
 }
 
 public void communicatePhoneData(String username,String sessid){
 	int usernameSize=username.length();
 	
 	if(usernameSize>0){
 	DeviceCommunicator devcom=new DeviceCommunicator(context);
 	String data=devcom.createKey();
 	postParameters = new ArrayList<NameValuePair>(2);
	    postParameters.add(new BasicNameValuePair("phonedata",data));
	    postParameters.add(new BasicNameValuePair("username",username));
	    postParameters.add(new BasicNameValuePair("session",sessid));
	
	   try {
		ServerCommunicator.setURL("your url to process phone data");
	    Response= ServerCommunicator.executePost(postParameters);
		
	    
	   } catch (Exception e) {
		 
		e.printStackTrace();
	   }
 	}
 	if(Response!=null){
 		createDialog("Alert",Response);
 	}

 }
 public String getResponse(){
	 return Response;
 }
 private void createDialog(String title,String content) {
 	Builder ad = new AlertDialog.Builder(context);
 	 
     ad.setPositiveButton("OK", null);

     ad.setTitle(title);

     ad.setMessage(content);

     ad.create();

     ad.show();
		
	}
}
