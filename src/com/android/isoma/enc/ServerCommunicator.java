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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

public class ServerCommunicator {
public static final int timeout=30000;
public static String url;
private static HttpClient httpclient;
public static String executePost(ArrayList<NameValuePair> postParameters){
	String ServerResponse = null;
	HttpClient httpClient=getHttpClient();

	ResponseHandler<String> response=new BasicResponseHandler();
	HttpPost request = new HttpPost(url); 
	try {
		request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		 
        request.setEntity(new UrlEncodedFormEntity(postParameters, HTTP.UTF_8));

		
	    ServerResponse=httpClient.execute(request,response);
	    
	} catch (UnsupportedEncodingException e) {
		
		e.printStackTrace();
	} catch (ClientProtocolException e) {
		
		e.printStackTrace();
	} catch (IOException e) {
		
		e.printStackTrace();
	}

return ServerResponse;
}
public static void setURL(String URL){
	url=URL;
}
private static HttpClient getHttpClient(){
	httpclient=new DefaultHttpClient();
	final HttpParams params = httpclient.getParams(); 

	HttpConnectionParams.setConnectionTimeout(params, timeout); 

	HttpConnectionParams.setSoTimeout(params, timeout); 

	ConnManagerParams.setTimeout(params, timeout); 

	return httpclient;
}


}
