/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.textmanipulation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.text.DocumentEvent;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

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
 * @@ say something what happens if two insert are added at the same offset.
 * 
 * @see TextBufferEditor
 */
public abstract class TextEdit {

	private static class EmptyIterator implements Iterator {
		public boolean hasNext() {
			return false;
		}
		public Object next() {
			throw new NoSuchElementException();
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private static final Iterator EMPTY_ITERATOR= new EmptyIterator();
	private static final TextEdit[] EMPTY_ARRAY= new TextEdit[0];
	
	private TextEdit fParent;
	private List fChildren;
	private int fActiveState;
	
	/* package */ static final int INACTIVE= 0;
	/* package */ static final int ACTIVE= 1;
	/* package */ static final int SAME_AS_PARENT= 2;

	protected TextEdit() {
		fActiveState= SAME_AS_PARENT;
	}

	public TextEdit getParent() {
		return fParent;
	}
	
	public int size() {
		if (fChildren == null)
			return 0;
		return fChildren.size();
	}
	
	/**
	 * Adds the given text edit <code>edit</code> to this
	 * edit.
	 * 
	 * @param edit the text edit to be added
	 * @exception TextEditException is thrown if the given
	 *  edit can't be added to this edit. This happens if the
	 *  edit overlaps with one of its siblings or if the given
	 *  edit isn't fully covered by this edit.
	 */
	public void add(TextEdit edit) throws TextEditException {
		internalAdd(edit);
	}
	
	/**
	 * Adds all edits in <code>edits</code> to this edit.
	 * 
	 * @param edits the text edits to be added
	 * @exception TextEditException is thrown if one of the given
	 *  edits can't be added to this edit. This happens if one
	 *  edit overlaps with one of its siblings or if one edit
	 *  isn't fully covered by this edit.
	 */
	public void addAll(TextEdit[] edits) throws TextEditException {
		for (int i= 0; i < edits.length; i++) {
			internalAdd(edits[i]);
		}
	}
	
	public TextEdit remove(int index) {
		if (fChildren == null)
			return null;
		TextEdit result= (TextEdit) fChildren.remove(index);
		result.setParent(null);
		if (fChildren.isEmpty())
			fChildren= null;
		return result;
	}
	
	public TextEdit[] removeAll() {
		if (fChildren == null)
			return new TextEdit[0];
		int size= fChildren.size();
		TextEdit[] result= new TextEdit[size];
		for (int i= 0; i < size; i++) {
			result[i]= (TextEdit)fChildren.get(i);
			result[i].setParent(null);
		}
		fChildren= null;
		return result;
	}
	
	public TextEdit[] getChildren() {
		if (fChildren == null)
			return EMPTY_ARRAY;
		return (TextEdit[])fChildren.toArray(new TextEdit[fChildren.size()]);
	}
	
	public boolean isActive() {
		switch (fActiveState) {
			case INACTIVE:
				return false;
			case ACTIVE:
				return true;
			case SAME_AS_PARENT:
				TextEdit parent= getParent();
				if (parent == null)
					return false;
				return parent.isActive();
			default:
				return false;
		}
	}
	
	public void setActive(boolean active) {
		if (active)
			fActiveState= ACTIVE;
		else
			fActiveState= INACTIVE;
	}
	
	public void clearActive() {
		fActiveState= SAME_AS_PARENT;
	}
	
	/**
	 * Returns the children managed by this text edit collection.
	 * 
	 * @return the children of this composite text edit
	 */
	public Iterator iterator() {
		if (fChildren == null)
			return EMPTY_ITERATOR;
		return fChildren.iterator();
	}
	
	/**
	 * Returns <code>true</code> if this edit has children. Otherwise
	 * <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if this edit has children
	 */
	public boolean hasChildren() {
		return fChildren != null && ! fChildren.isEmpty();
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
	 * Connects this text edit to the given <code>TextBuffer</code>. 
	 * The passed buffer is the same instance passed to the perform 
	 * method. But the content of the buffer may differ.
	 * <p>
	 * Note that this method <b>should only be called</b> by the text
	 * edit framework and not by normal clients.
	 *<p>
	 * This default implementation does nothing. Subclasses may override
	 * if needed.
	 *  
	 * @param buffer the text buffer this text edit will be applied to
	 * @exception TextEditException if the edit isn't in a valid state
	 *  and can therefore not be connected to the given buffer.
	 */
	protected void connect(TextBuffer buffer) {
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
	/* package */ final TextEdit copy(TextEditCopier copier) {
		TextEdit result= copy0(copier);
		copier.addCopy(this, result);
		result.fActiveState= fActiveState;
		if (fChildren != null) {
			List children= new ArrayList(fChildren.size());
			for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
				TextEdit element= (TextEdit)iter.next();
				TextEdit copy= element.copy(copier);
				copy.fActiveState= element.fActiveState;
				copy.setParent(result);
				children.add(copy);
			}
			result.internalSetChildren(children);
		}
		executePostProcessCopy(copier);
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
	
	/* non Java-doc
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String name= getClass().getName();
		int index= name.lastIndexOf('.');
		if (index != -1) {
			name= name.substring(index + 1);
		}
		return "{" + name + "} " + getTextRange().toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static TextRange getTextRange(List edits) {
		int size= 0;
		if (edits == null || (size= edits.size()) == 0)
			return TextRange.UNDEFINED;
			
		int offset= Integer.MAX_VALUE;
		int end= Integer.MIN_VALUE;
		int deleted= 0;
		int undefined= 0;
		for (int i= 0; i < size; i++) {
			TextRange range= ((TextEdit)edits.get(i)).getTextRange();
			if (range.isDeleted()) {
				deleted++;
			} else if (range.isUndefined()) {
				undefined++;
			} else {
				offset= Math.min(offset, range.getOffset());
				end= Math.max(end, range.getExclusiveEnd());
			}
		}
		if (size == deleted) {
			return TextRange.DELETED;
		} else if (size == undefined) {
			return TextRange.UNDEFINED;
		} else {
			return TextRange.createFromStartAndExclusiveEnd(offset, end);
		}
	}	
	
	/**
	 * @deprecated Use copy0(Copier instead)
	 */
	protected final TextEdit copy0() {
		return null;
	}
	
	protected abstract TextEdit copy0(TextEditCopier copier);
	
	protected void postProcessCopy(TextEditCopier copier) {
	}
	
	protected TextRange getChildrenTextRange() {
		int size= fChildren != null ? fChildren.size() : 0;
		if (size == 0)
			return TextRange.UNDEFINED;
		return getTextRange(fChildren);
	}

	public void adjustOffset(int delta) {
		getTextRange().addToOffset(delta);
	}
	
	public void adjustLength(int delta) {
		getTextRange().addToLength(delta);
	}
	
	public void markAsDeleted() {
		getTextRange().markAsDeleted();
	}
	
	
	//---- Helpers -------------------------------------------------------------------------------------------
	
	/* package */ void executePostProcessCopy(TextEditCopier copier) {
		postProcessCopy(copier);
		if (fChildren != null) {
			for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
				((TextEdit)iter.next()).executePostProcessCopy(copier);
			}
		}
	}
	
	/* package */ int getNumberOfChildren() {
		List children= internalGetChildren();
		if (children == null)
			return 0;
		int result= children.size();
		for (Iterator iter= children.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			result+= edit.getNumberOfChildren();
		}
		return result;
	}	

	
	//---- Setter and Getters -------------------------------------------------------------------------------
	
	/* package */ void aboutToBeAdded(TextEdit parent) {
	}
	
	/* package */ void setParent(TextEdit parent) {
		if (parent != null)
			Assert.isTrue(fParent == null);
		fParent= parent;
	}
	
	/* package */ List internalGetChildren() {
		return fChildren;
	}
	
	/* package */ void internalSetChildren(List children) {
		fChildren= children;
	}
	
	/* package */ void internalAdd(TextEdit edit) throws TextEditException {
		edit.aboutToBeAdded(this);
		TextRange eRange= edit.getTextRange();
		if (eRange.isUndefined() || eRange.isDeleted())
			throw new TextEditException(this, edit, "Can't add undefined or deleted edit");
		TextRange range= getTextRange();
		if (!covers(range, eRange))
			throw new TextEditException(this, edit, TextManipulationMessages.getString("TextEdit.range_outside")); //$NON-NLS-1$
		if (fChildren == null) {
			fChildren= new ArrayList(2);
		}
		int index= getInsertionIndex(edit);
		fChildren.add(index, edit);
		edit.setParent(this);
	}
	
	/* package */ int getInsertionIndex(TextEdit edit) {
		TextRange range= edit.getTextRange();
		int size= fChildren.size();
		if (size == 0)
			return 0;
		int rStart= range.getOffset();
		int rEnd= range.getInclusiveEnd();
		for (int i= 0; i < size; i++) {
			TextRange other= ((TextEdit)fChildren.get(i)).getTextRange();
			int oStart= other.getOffset();
			int oEnd= other.getInclusiveEnd();
			// make sure that a duplicate insertion point at the same offet is inserted last 
			if (rStart > oEnd || (rStart == oStart && range.getLength() == 0 && other.getLength() == 0))
				continue;
			if (rEnd < oStart)
				return i;
			throw new TextEditException(this, edit, TextManipulationMessages.getString("TextEdit.overlapping")); //$NON-NLS-1$
		}
		return size;
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
	
	protected static IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, message, null);
	}
	
	protected static IStatus createOKStatus() {
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, TextManipulationMessages.getString("TextEdit.is_valid"), null); //$NON-NLS-1$
	}
	
	//---- Edit processing --------------------------------------------------------------------------------
	
	protected void updateTextRange(int delta, List executedEdits) {
		for (Iterator iter= executedEdits.iterator(); iter.hasNext();) {
			((TextEdit)iter.next()).predecessorExecuted(delta);
		}
		adjustLength(delta);
		updateParents(delta);
	}
	
	/* package */ void execute(TextBuffer buffer, Updater updater, IProgressMonitor pm) throws CoreException {
		List children= internalGetChildren();
		pm.beginTask("", children != null ? children.size() + 1 : 1); //$NON-NLS-1$
		if (children != null) {
			for (int i= children.size() - 1; i >= 0; i--) {
				((TextEdit)children.get(i)).execute(buffer, updater, new SubProgressMonitor(pm, 1));
			}
		}
		if (isActive()) {
			try {
				updater.setActiveNode(this);
				perform(buffer);
			} finally {
				updater.setActiveNode(null);
			}
		}
		pm.worked(1);
	}
	
	/* package */ void childExecuted(int delta) {
		adjustLength(delta);
	}
	
	/* package */ void predecessorExecuted(int delta) {
		adjustOffset(delta);
	}
	
	/* package */ void markChildrenAsDeleted() {
		if (fChildren != null) {
			for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
				TextEdit edit= (TextEdit) iter.next();
				edit.markAsDeleted();
				edit.markChildrenAsDeleted();
			}
		}	
	}
	
	protected void updateParents(int delta) {
		TextEdit edit= getParent();
		while (edit != null) {
			edit.childExecuted(delta);
			edit= edit.getParent();
		}
	}
	
	private static boolean covers(TextRange thisRange, TextRange otherRange) {
		if (thisRange.getLength() == 0) {	// an insertion point can't cover anything
			return false;
		} else {
			return thisRange.getOffset() <= otherRange.getOffset() && otherRange.getExclusiveEnd() <= thisRange.getExclusiveEnd();
		}		
	}	
}

