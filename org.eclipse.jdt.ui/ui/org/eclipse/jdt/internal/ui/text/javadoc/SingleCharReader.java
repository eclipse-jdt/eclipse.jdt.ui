package org.eclipse.jdt.internal.ui.text.javadoc;

import java.io.IOException;
import java.io.Reader;

public abstract class SingleCharReader extends Reader {
	
	/**
	 * @see Reader#read(char)
	 */
	public abstract int read() throws IOException;

	/**
	 * @see Reader#read(char[],int,int)
	 */
	public int read(char cbuf[], int off, int len) throws IOException {
		int end= off + len;
		for (int i= off; i < end; i++) {
			char ch= (char)read();
			if (ch == -1) {
				return i - off;
			}
			cbuf[i]= ch;
		}
		return len;
	}		
	
	/**
	 * @see Reader#ready()
	 */		
    	public boolean ready() throws IOException {
		return true;
	}
	
	/**
	 * Gets the comment as a String
	 */
	public String getString() throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch;
		while ((ch= read()) != -1) {
			buf.append((char)ch);
		}
		return buf.toString();
	}
}