package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.IOException;
import java.io.Reader;


/**
 * Provides a set of convenience methods for creating HTML pages.
 */
public class HTMLPrinter {
			
	private HTMLPrinter() {
	}
	
	private static String replace(String text, char c, String s) {
				
		int previous= 0;
		int current= text.indexOf(c, previous);
		
		if (current == -1)
			return text;
		
		StringBuffer buffer= new StringBuffer();	
		while (current > -1) {
			buffer.append(text.substring(previous, current));
			buffer.append(s);
			previous= current + 1;
			current= text.indexOf(c, previous);
		}
		buffer.append(text.substring(previous));
		
		return buffer.toString();
	}
	
	public static String convertToHTMLContent(String content) {
		return content;
//		content= replace(content, '<', "&lt;");
//		return replace(content, '>', "&gt;");
	}
	
	public static String read(Reader rd) {
		
		StringBuffer buffer= new StringBuffer();
		char[] readBuffer= new char[2048];
		
		try {
			int n= rd.read(readBuffer);
			while (n > 0) {
				buffer.append(readBuffer, 0, n);
				n= rd.read(readBuffer);
			}
			return buffer.toString();
		} catch (IOException x) {
		}
		
		return null;
	}

	public static void insertPageProlog(StringBuffer buffer, int position) {
		buffer.insert(position, "<html><body text=\"#000000\" bgcolor=\"#FFFF88\"><font size=-1>");
	}
	
	public static void addPageProlog(StringBuffer buffer) {
		insertPageProlog(buffer, buffer.length());
	}
	
	public static void addPageEpilog(StringBuffer buffer) {
		buffer.append("</font></body></html>");
	}
	
	public static void startBulletList(StringBuffer buffer) {
		buffer.append("<ul>");
	}
	
	public static void endBulletList(StringBuffer buffer) {
		buffer.append("</ul>");
	}
	
	public static void addBullet(StringBuffer buffer, String bullet) {
		if (bullet != null) {
			buffer.append("<li>");
			buffer.append(convertToHTMLContent(bullet));
			buffer.append("</li>");
		}
	}
	
	public static void addSmallHeader(StringBuffer buffer, String header) {
		if (header != null) {
			buffer.append("<h5>");
			buffer.append(convertToHTMLContent(header));
			buffer.append("</h5>");
		}
	}
	
	public static void addParagraph(StringBuffer buffer, String paragraph) {
		if (paragraph != null) {
			buffer.append("<p>");
			buffer.append(convertToHTMLContent(paragraph));
			buffer.append("</p>");
		}
	}
	
	public static void addParagraph(StringBuffer buffer, Reader paragraphReader) {
		if (paragraphReader != null)
			addParagraph(buffer, read(paragraphReader));
	}
}