/*
 * Created on Aug 13, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.corext.textmanipulation;

/**
 * Thrown to indicate that an edit got added to a parent edit
 * but the child edit somehow conflicts with the parent or
 * one of it siblings.
 * 
 * @see TextEdit#add(TextEdit)
 */
public class IllegalEditException extends IllegalArgumentException {
	
	private TextEdit fParent;
	private TextEdit fChild;
	
	/** 
	 * Constructs a new illegal edit exception with the given detail
	 * message.
	 * 
	 * @param parent the parent edit
	 * @param child the child edit
	 * @param message the detail message
	 */
	public IllegalEditException(TextEdit parent, TextEdit child, String message) {
		super(message);
		fParent= parent;
		fChild= child;
	}
}