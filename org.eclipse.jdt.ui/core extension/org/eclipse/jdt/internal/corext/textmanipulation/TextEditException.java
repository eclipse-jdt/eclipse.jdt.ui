/*
 * Created on Aug 13, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.corext.textmanipulation;


public class TextEditException extends RuntimeException {
	
	/** 
	 * Constructs a new text edit exception without any detail
	 * message.
	 */
	public TextEditException() {
	}

	/** 
	 * Constructs a new text edit exception with the given detail
	 * message.
	 * 
	 * @param message the detail message
	 */
	public TextEditException(TextEdit parent, TextEdit child, String message) {
		super(message);
	}
}
