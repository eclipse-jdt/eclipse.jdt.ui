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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;

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

	/**
	 * Create a new text edit. Parent is initialized to <code>
	 * null<code> and the edit doesn't have any children.
	 */
	protected TextEdit() {
	}
	
	/**
	 * Copy constrcutor
	 * 
	 * @param source the source to copy form
	 */
	protected TextEdit(TextEdit source) {
		// do nothing. Parent and childs are
		// populated by the EditCopier class
	}

	//---- parent and children management -----------------------------
	
	/**
	 * Returns the edit's parent. The method returns <code>null</code> 
	 * if this edit hasn't been add to another edit.
	 * 
	 * @return the edit's parent
	 */
	public final TextEdit getParent() {
		return fParent;
	}
	
	/**
	 * Adds the given edit <code>child</code> to this edit.
	 * 
	 * @param child the child edit to add
	 * @exception <code>EditException<code> is thrown if the child
	 *  edit can't be added to this edit. This is the case if the child 
	 *  overlaps with one of its siblings or if the child edit's region
	 *  isn't fully covered by this edit.
	 */
	public final void add(TextEdit child) throws IllegalEditException {
		internalAdd(child);
	}
	
	/**
	 * Adds all edits in <code>edits</code> to this edit.
	 * 
	 * @param edits the text edits to add
	 * @exception <code>EditException</code> is thrown if one of 
	 *  the given edits can't be added to this edit.
	 * 
	 * @see #add(TextEdit)
	 */
	public final void addAll(TextEdit[] edits) throws IllegalEditException {
		for (int i= 0; i < edits.length; i++) {
			internalAdd(edits[i]);
		}
	}
	
	/**
	 * Removes the edit specified by the given index from the list
	 * of children. Returns the child edit that was removed from
	 * the list of children. The parent of the returned edit is
	 * set to <code>null</code>.
	 * 
	 * @param index the index of the edit to remove
	 * @return the removed edit
	 * @exception <code>IndexOutOfBoundsException</code> if the index 
	 *  is out of range
	 */
	public final TextEdit remove(int index) {
		if (fChildren == null)
			throw new IndexOutOfBoundsException("Index: " + index + " Size: 0");  //$NON-NLS-1$//$NON-NLS-2$
		TextEdit result= (TextEdit)fChildren.remove(index);
		result.setParent(null);
		if (fChildren.isEmpty())
			fChildren= null;
		return result;
	}
	
	/**
	 * Removes all edits from the list of child edits. Returns the 
	 * removed child edits. The parent of the removed edits is set
	 * to <code>null</code>.
	 * 
	 * @return an array of the removed edits
	 */
	public final TextEdit[] removeAll() {
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
	
	/**
	 * Returns <code>true</code> if this edit has children. Otherwise
	 * <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if this edit has children; otherwise
	 *  <code>false</code> is returned
	 */
	public boolean hasChildren() {
		return fChildren != null && ! fChildren.isEmpty();
	}

	/**
	 * Returns the edit's children. If the edit doesn't have any 
	 * children an empty array is returned.
	 * 
	 * @return the edit's children
	 */
	public TextEdit[] getChildren() {
		if (fChildren == null)
			return EMPTY_ARRAY;
		return (TextEdit[])fChildren.toArray(new TextEdit[fChildren.size()]);
	}
	
	/**
	 * Returns an iterator over the child edits.
	 * 
	 * @return an iterator over the child edits
	 */
	public Iterator iterator() {
		if (fChildren == null)
			return EMPTY_ITERATOR;
		return fChildren.iterator();
	}
	
	//---- equals and hashcode ---------------------------------------------

	/**
	 * The <code>Edit</code> implementation of this <code>Object</code>
	 * method uses object identity (==).
	 * 
	 * @param obj the other object
	 * @return <code>true</code> iff <code>this == obj</code>; otherwise
	 *  <code>false</code> is returned
	 * 
	 * @see Object#equals(java.lang.Object)
	 */
//	public final boolean equals(Object obj) {
//		return this == obj; // equivalent to Object.equals
//	}
	
	/**
	 * The <code>Edit</code> implementation of this <code>Object</code>
	 * method calls uses <code>Object#hashCode()</code> to compute its
	 * hash code.
	 * 
	 * @return the object's hash code value
	 * 
	 * @see Object#hashCode()
	 */
//	public final int hashCode() {
//		return super.hashCode();
//	}
	
	//---- Region management -----------------------------------------------

	/**
	 * Returns the range that this edit is manipulating. The returned
	 * <code>IRegion</code> contains the edit's offset and length at
	 * the point in time when this call is made. Any subsequent changes
	 * to the edit's offset and length aren't reflected in the returned
	 * region object.
	 * 
	 * @return the manipulated range
	 */
	public abstract TextRange getTextRange();
	
	public int getOffset() {
		return getTextRange().getOffset();
	}
	
	public int getLength() {
		return getTextRange().getLength();
	}
	
	//---- Execution -------------------------------------------------------
	
	/**
	 * Connects this edit to the given <code>IDocument</code>. The passed 
	 * document is the same instance passed to the perform method.
	 * <p>
	 * Note that this method <b>should only be called</b> by the edit
	 * framework and not by normal clients.
	 *<p>
	 * This default implementation does nothing. Subclasses may override
	 * if needed.
	 *  
	 * @param document the document this edit will be applied to
	 * @exception EditException if the edit isn't in a valid state
	 *  and can therefore not be connected to the given document.
	 */
	protected void connect(IDocument document) throws IllegalEditException {
		// does nothing
	}
	
	/**
	 * Performs the text edit. Note that this method <b>should only be called</b> 
	 * by a <code>EditProcessor</code>. 
	 * 
	 * @param document the actual document to manipulate
	 */
	public abstract void perform(IDocument document) throws PerformEditException;
	
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
	
	//---- Copying -------------------------------------------------------------
		
	/**
	 * Creates and returns a copy of this edit. The copy method should be 
	 * implemented in a way so that the copy can be added to a different 
	 * <code>EditProcessor</code> without causing any harm to original 
	 * edit.
	 * <p>
	 * Implementers of this method should use the copy constructor <code>
	 * Edit#Edit(Edit source) to initialize the edit part of the copy.
	 * <p>
	 * This method <b>should not be called</b> from outside the framework.
	 * Please use <code>EditCopier</code> to create a copy of a edit tree.
	 * 
	 * @return a copy of this edit.
	 * @see EditCopier
	 */
	protected abstract TextEdit copy0();
	
	public abstract boolean matches(Object other);
	
	public boolean matchesSubtree(Object obj) {
		if (!matches(obj))
			return false;
		TextEdit other= (TextEdit)obj;
		int size= fChildren != null ? fChildren.size() : 0;
		int otherSize= other.fChildren != null ? other.fChildren.size() : 0;
		if (size != otherSize)
			return false;
		if (size == 0 && otherSize == 0)
			return true;
		for (Iterator iter1= fChildren.iterator(), iter2= other.fChildren.iterator(); iter1.hasNext();) {
			TextEdit thisChild= (TextEdit)iter1.next();
			TextEdit otherChild= (TextEdit)iter2.next();
			if (other != otherChild.getParent())
				return false;
			if (! thisChild.matchesSubtree(otherChild))
				return false;
		}
		return true;
	}
	
	protected TextEdit createPlaceholder() {
		TextRange range= getTextRange();
		return new MultiTextEdit(range.getOffset(), range.getLength());
	}
	
	protected void postProcessCopy(TextEditCopier copier) {
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
	
	/* package */ void performReplace(IDocument document, TextRange range, String text) throws PerformEditException {
		try {
			document.replace(range.getOffset(), range.getLength(), text);
		} catch (BadLocationException e) {
			throw new PerformEditException(this, e.getMessage(), e);
		}
	}
	
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
	
	/* package */ void internalAdd(TextEdit edit) throws IllegalEditException {
		edit.aboutToBeAdded(this);
		TextRange eRange= edit.getTextRange();
		if (eRange.isUndefined() || eRange.isDeleted())
			throw new IllegalEditException(this, edit, "Can't add undefined or deleted edit");
		TextRange range= getTextRange();
		if (!Regions.covers(range, eRange))
			throw new IllegalEditException(this, edit, TextManipulationMessages.getString("TextEdit.range_outside")); //$NON-NLS-1$
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
			throw new IllegalEditException(this, edit, TextManipulationMessages.getString("TextEdit.overlapping")); //$NON-NLS-1$
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
	
	/* package */ void execute(IDocument document, Updater updater, IProgressMonitor pm) throws PerformEditException {
		List children= internalGetChildren();
		pm.beginTask("", children != null ? children.size() + 1 : 1); //$NON-NLS-1$
		if (children != null) {
			for (int i= children.size() - 1; i >= 0; i--) {
				((TextEdit)children.get(i)).execute(document, updater, new SubProgressMonitor(pm, 1));
			}
		}
		try {
			updater.setActiveNode(this);
			perform(document);
		} finally {
			updater.setActiveNode(null);
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
}

