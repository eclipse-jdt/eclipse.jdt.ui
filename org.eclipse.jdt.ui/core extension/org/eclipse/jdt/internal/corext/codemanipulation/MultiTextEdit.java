/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.utils.Assert;
import org.eclipse.core.runtime.CoreException;

public class MultiTextEdit {

	private List fChildren;

	/**
	 * Creates a new composite text edit.
	 */
	public MultiTextEdit() {
		fChildren= new ArrayList(3);
	}

	private MultiTextEdit(List children) {
		fChildren= new ArrayList(children.size());
		for (Iterator iter= children.iterator(); iter.hasNext();) {
			fChildren.add(((TextEdit)iter.next()).copy());
		}
	}
	
	/**
	 * Returns the children managed by this text edit collection.
	 * 
	 * @return the children of this composite text edit
	 */
	public TextEdit[] getChildren() {
		return (TextEdit[]) fChildren.toArray(new TextEdit[fChildren.size()]);
	}
	
	/**
	 * Adds all <code>TextEdits</code> managed by the given multt text edit.
	 * 
	 * @param edit the multi text edit to be added.
	 */
	public void addAll(MultiTextEdit edit) {
		fChildren.addAll(edit.fChildren);
	}
	
	/**
	 * Adds a text edit.
	 * 
	 * @param edit the text edit to be added
	 */
	public void add(TextEdit edit) {
		Assert.isNotNull(edit);
		fChildren.add(edit);
	}
	
	/**
     * Creates and returns a copy of this text edit collection. The copy method should
     * be implemented in a way so that the copy can be added to a different  <code>
     * TextBuffer</code> without causing any harm to the object from which the copy 
     * has been created.
     * 
     * @return a copy of this object.
     */
	public MultiTextEdit copy() {
		return new MultiTextEdit(fChildren);
	}
	
	/**
	 * Returns the <code>TextRange</code> that this text edit is going to
	 * manipulate. If this method is called before the <code>MultiTextEdit</code>
	 * has been added to a <code>TextBufferEditor</code> it may return <code>
	 * null</code> to indicate this situation.
	 * 
	 * @return the <code>TextRange</code>s this <code>TextEdit is going
	 * 	to manipulate
	 */
	public TextRange getTextRange() {
		int size= fChildren.size();
		if (size == 0)
			return new TextRange(0,0);
		TextRange range= ((TextEdit)fChildren.get(0)).getTextRange();
		int start= range.getOffset();
		int end= range.getEnd();
		for (int i= 1; i < size; i++) {
			range= ((TextEdit)fChildren.get(i)).getTextRange();
			start= Math.min(start, range.getOffset());
			end= Math.max(end, range.getEnd());
		}
		return new TextRange(start, end - start + 1);
	}
	
	/**
	 * Returns the element modified by this text edit. The method
	 * may return <code>null</code> if the modification isn't related to a
	 * element or if the content of the modified text buffer doesn't
	 * follow any syntax.
	 * <p>
	 * This default implementation returns <code>null</code>
	 * 
	 * @return the element modified by this text edit
	 */
	public Object getModifiedElement() {
		return null;
	}	
}

