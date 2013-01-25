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

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class Encryption {
	
	    private KeyParameter key;
	    
	   
		public Encryption(String key){
	    	
	       this.key=new KeyParameter(key.getBytes());
	      
	      
	    }
	
	    public byte[] callCipher(BufferedBlockCipher cipher,byte[]data) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
	    	int size=cipher.getOutputSize(data.length);
	    	byte[]result=new byte[size];
	    	int olen=cipher.processBytes(data,0,data.length,result, 0);
	    	
				 int last=cipher.doFinal(result, olen);
				
		   		
			final byte[] plain = new byte[olen + last];
		    System.arraycopy(result, 0, plain, 0, plain.length);
		   // cipher.reset();
		    return plain;
		

	    }
	    public synchronized byte[] encrypt( byte[] data ) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
	    	byte[]iv=null;
	    	if( data == null || data.length == 0 ){
	            return new byte[0];
	        }
	    	BufferedBlockCipher encryptcipher=new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()),new ZeroBytePadding());
		    
	     
	    	String source = "00000000";
				iv = source.getBytes();
			
			
	        CipherParameters ivAndKey = new ParametersWithIV(key, iv);
	        encryptcipher.init(true,ivAndKey);
	        
	       
	        return callCipher(encryptcipher, data );
	    }
	    public byte[] encryptString( String data ) throws DataLengthException, IllegalStateException, InvalidCipherTextException{
	        if( data == null || data.length() == 0 ){
	            return new byte[0];
	        }
	        
	        return encrypt(data.getBytes());
	    }
	    public synchronized byte[] decrypt( byte[] data ) throws DataLengthException, IllegalStateException, InvalidCipherTextException{
	    	byte[]iv=null;
	        if( data == null || data.length == 0 ){
	            return new byte[0];
	        }
	        
	        BufferedBlockCipher decryptcipher=new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()),new ZeroBytePadding());
	        
	        
	        String source = "00000000";
				iv = source.getBytes();
			
	        CipherParameters ivAndKey = new ParametersWithIV(key, iv);
	        
	       
	        decryptcipher.init(false, ivAndKey);
	        
            return callCipher(decryptcipher, data );
	    }
	    public String decryptString( String data ) throws DataLengthException, IllegalStateException, InvalidCipherTextException{
	        if( data == null || data.length() == 0 ){
	            return "";
	        }
	        
	        return new String( decrypt( data.getBytes()));
	    }
	    
}
