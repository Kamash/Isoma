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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;


import android.content.Context;

public class ProcessData {

public String decryption(byte [] encdata,Context context) throws DataLengthException, IllegalStateException, InvalidCipherTextException, FileNotFoundException, IOException{
	
	DeviceCommunicator dc=new DeviceCommunicator(context);
	String key=dc.createKey();
   	Encryption enc=new Encryption(key);

    return uncompress(enc.decrypt(encdata));
}
public String uncompress(byte[] input){
	Inflater decompresser = new Inflater();
	 decompresser.setInput(input, 0, input.length);
	 byte[] result = new byte[(int) (input.length*2.5)];
	 int resultLength = 0;
	try {
		resultLength = decompresser.inflate(result);
	} catch (DataFormatException e) {
		
		e.printStackTrace();
	}
	 decompresser.end();
	 // Decode the bytes into a String
	 String outputString = null;
	try {
		outputString = new String(result, 0, resultLength, "UTF-8");
	} catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
     return outputString;
}


public String convertByteArrayToString(byte [] byteArray) {
    
   String value = null;
	try {
		value = new String(byteArray,"UTF8");
	} catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}   
    return value;
}

}
