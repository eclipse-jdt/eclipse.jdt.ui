/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.textmanipulation.enhanced;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.DocumentEvent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.Assert;

/**
 * A text edit describes an elementary text manipulation operation. Text edits
 * are executed by adding them to a <code>TextBufferEditor</code> and then
 * calling <code>perform</code> on the <code>TextBufferEditor</code>.
 * <p>
 * After a <code>TextEdit</code> has been added to a <code>TextBufferEditor</code>
 * the method <code>connect</code> is sent to the text edit. A <code>TextEdit</code>
 * is allowed to do some adjustments of the text range it is going to manipulate while inside
 * the hook <code>connect</code>.
 * 
 * @see TextBufferEditor
 */
public abstract class TextEdit {
	
	private TextEdit fParent;
	private List fChildren;
	private int fState;
	
	protected static final int CREATED= 0;
	protected static final int ADDED= 1;
	protected static final int CONNECTED= 2;

	protected TextEdit() {
		fState= CREATED;
	}

	public TextEdit getParent() {
		return fParent;
	}
	
	public void add(TextEdit edit) {
		Assert.isTrue(isCreated());
		edit.setParent(this);
		if (fChildren == null) {
			fChildren= new ArrayList(2);
			fChildren.add(edit);
		} else {
			int size= fChildren.size();
			TextRange editRange= edit.getTextRange();
			for (int i= 0; i < size; i++) {
				TextEdit element= (TextEdit)fChildren.get(i);
				TextRange elementRange= element.getTextRange();
				if (	elementRange.getOffset() > editRange.getOffset() || 
						(elementRange.getOffset() == editRange.getOffset() && editRange.getLength() < elementRange.getLength())) {
					fChildren.add(i, edit);
					return;
				}
			}
			fChildren.add(edit);
		}
	}

	/**
	 * Returns the <code>TextRange</code> that this text edit is going to
	 * manipulate. If this method is called before the <code>TextEdit</code>
	 * has been added to a <code>TextBufferEditor</code> it may return <code>
	 * null</code> or <code>TextRange.UNDEFINED</code> to indicate this situation.
	 * 
	 * @return the <code>TextRange</code>s this <code>TextEdit is going
	 * 	to manipulate
	 */
	public abstract TextRange getTextRange();
	
	/**
	 * Connects this text edit to the given <code>TextBufferEditor</code>. A text edit 
	 * must not keep a reference to the passed text buffer editor. It is guaranteed that 
	 * the buffer passed to <code>perform<code> is equal to the buffer managed by
	 * the given text buffer editor. But they don't have to be identical.
	 * <p>
	 * Note that this method <b>should only be called</b> by a <code>
	 * TextBufferEditor</code>.
	 *<p>
	 * This default implementation does nothing. Subclasses may override
	 * if needed.
	 *  
	 * @param buffer the text buffer this text edit will be applied to
	 */
	public void connect(TextBuffer buffer) throws CoreException {
		// does nothing
	}
	
	/**
	 * Performs the text edit. Note that this method <b>should only be called</b> 
	 * by a <code>TextBufferEditor</code>. 
	 * 
	 * @param buffer the actual buffer to manipulate
	 */
	public abstract void perform(TextBuffer buffer) throws CoreException;
	
	/**
	 * This method gets called after all <code>TextEdit</code>s added to a text buffer
	 * editor are executed. Implementors usually do some clean-up or release allocated 
	 * resources that are now longer needed.
	 * <p>
	 * Subclasses may extend this implementation.
	 * </p>
	 */
	public void performed() {
		// does nothing
	}
		
	/**
     * Creates and returns a copy of this object. The copy method should
     * be implemented in a way so that the copy can be added to a different 
     * <code>TextBufferEditor</code> without causing any harm to the object 
     * from which the copy has been created.
     * 
     * @return a copy of this object.
     */
	public final TextEdit copy() throws CoreException {
		TextEdit result= copy0();
		List list= new ArrayList(fChildren.size());
		for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
			TextEdit element= (TextEdit)iter.next();
			result.add(element.copy());
		}
		return result;
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
	
	protected abstract TextEdit copy0() throws CoreException;
	
	protected TextRange getChildrenTextRange() {
		int size= -1;
		if (fChildren == null || (size= fChildren.size())== 0) {
			return TextRange.UNDEFINED;
		} else {
			int offset= ((TextEdit)fChildren.get(0)).getTextRange().getOffset();
			int end= ((TextEdit)fChildren.get(size - 1)).getTextRange().getExclusiveEnd();
			return TextRange.createFromStartAndExclusiveEnd(offset, end);
		}
	}
	
	//---- Setter and Getters -------------------------------------------------------------------------------
	
	/* package */ void setParent(TextEdit parent) {
		if (parent != null)
			Assert.isTrue(fParent == null);
		fParent= parent;
	}
	
	/* package */ List getChildren() {
		return fChildren;
	}
	
	/* package */ void setChildren(List children) {
		fChildren= children;
	}
	
	/* package */ void setState(int state) {
		fState= state;
	}
	
	/* package */ boolean isCreated() {
		return fState == CREATED;
	}
	
	/* package */ boolean isAdded() {
		return fState == ADDED;
	}
	
	/* package */ boolean isConnected() {
		return fState == CONNECTED;
	}
	
	//---- Validation methods -------------------------------------------------------------------------------
	
	/* package */ void checkRange(DocumentEvent event) {
		TextRange range= getTextRange();
		int eventOffset= event.getOffset();
		int eventLength= event.getLength();
		int eventEnd = eventOffset + eventLength - 1;
		// "Edit changes text that lies outside its defined range"
		Assert.isTrue(range.getOffset() <= eventOffset && eventEnd <= range.getInclusiveEnd());
	}
	
	/* package */ boolean checkEdit(int bufferLength) {
		TextRange range= getTextRange();
		if (range.getExclusiveEnd() > bufferLength)
			return false;
		boolean isInsertionPoint= range.isInsertionPoint();
		TextRange cRange= getChildrenTextRange();
		if (!cRange.isUndefined() && (cRange.getOffset() < range.getOffset() || cRange.getExclusiveEnd() > range.getExclusiveEnd())) {
			return false;
		}
		TextRange last= null;
		if (fChildren != null) {
			for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
				TextEdit element= (TextEdit)iter.next();
				TextRange eRange= element.getTextRange();
				
				if (!(isInsertionPoint && eRange.isInsertionPoint()) && last != null && last.getInclusiveEnd() >= eRange.getOffset()) {
					return false;
				}
				if (!element.checkEdit(bufferLength))
					return false;
				last= eRange;
			}
		}
		return true;
	}
	
	//---- Edit processing --------------------------------------------------------------------------------
	
	/* package */ void executeConnect(TextBuffer buffer) throws CoreException {
		fState= ADDED;
		List children= getChildren();
		if (children != null) {
			for (int i= children.size() - 1; i >= 0; i--) {
				((TextEdit)children.get(i)).executeConnect(buffer);
			}
		}
		connect(buffer);
		fState= CONNECTED;
	}
	
	/* package */ void execute(TextBuffer buffer, Updater updater, IProgressMonitor pm) throws CoreException {
		List children= getChildren();
		pm.beginTask("", children != null ? children.size() + 1 : 1);
		if (children != null) {
			for (int i= children.size() - 1; i >= 0; i--) {
				((TextEdit)children.get(i)).execute(buffer, updater, new SubProgressMonitor(pm, 1));
			}
		}
		try {
			updater.setActiveNode(this);
			perform(buffer);
			pm.worked(1);
		} finally {
			updater.setActiveNode(null);
		}
	}
	
	/* package */ void updateTextRange(int delta, List executedEdits) {
		for (Iterator iter= executedEdits.iterator(); iter.hasNext();) {
			((TextEdit)iter.next()).predecessorExecuted(delta);
		}
		getTextRange().adjustLength(delta);
		updateParents(delta);
	}
	
	/* package */ void childExecuted(int delta) {
		getTextRange().adjustLength(delta);
	}
	
	/* package */ void predecessorExecuted(int delta) {
		getTextRange().adjustOffset(delta);
	}
	
	/* package */ void markAsDeleted(List children) {
		if (children != null) {
			for (Iterator iter= children.iterator(); iter.hasNext();) {
				TextEdit element= (TextEdit) iter.next();
				element.getTextRange().markAsDeleted();
				markAsDeleted(element.getChildren());
			}
		}
	}
	
	/* package */ void updateParents(int delta) {
		TextEdit edit= getParent();
		while (edit != null) {
			edit.childExecuted(delta);
			edit= edit.getParent();
		}
	}	
}

