/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.nls.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {

	//no instances
	private Utils(){
	}
	
	/**
	 * Returns null if an error occurred.
	 * closes the stream 
	 */
	public static String readString(InputStream is) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is));

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);
			
			return buffer.toString();
			
		} catch (IOException ex) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
				}
			}
		}
		return null;
	}
}