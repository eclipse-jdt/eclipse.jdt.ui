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
package org.eclipse.jdt.internal.corext.textmanipulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.jface.text.DocumentEvent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

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
 * @see TextBufferEditor
 */
public abstract class TextEdit {

	private static class Sorter implements Comparator {
		public int compare(Object arg1, Object arg2) {
			TextRange r1= ((TextEdit)arg1).getTextRange();
			TextRange r2= ((TextEdit)arg2).getTextRange();
			int o1= r1.getOffset();
			int o2= r2.getOffset();
			if (o1 < o2)
				return -1;
			if (o1 > o2)
				return 1;
			
			int l1= r1.getLength();
			int l2= r2.getLength();
			
			if (l1 < l2)
				return -1;
			if (l1 > l2)
				return 1;
				
			return 0;
		}
	}
	
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
	
	private TextEdit fParent;
	private List fChildren;
	private int fLifeCycle;
	private int fActiveState;
	
	protected static final int UNCONNECTED= 0;
	protected static final int ADDED= 1;
	protected static final int CONNECTED= 2;

	/* package */ static final int INACTIVE= 0;
	/* package */ static final int ACTIVE= 1;
	/* package */ static final int SAME_AS_PARENT= 2;

	protected TextEdit() {
		fLifeCycle= UNCONNECTED;
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
	
	public void add(TextEdit edit) {
		internalAdd(edit);
	}
	
	public void addAll(TextEdit[] edits) {
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
	 * @deprecated Use connect(TextBuffer buffer) instead
	 */
	public final void connect(TextBufferEditor editor) throws CoreException {
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
			for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
				TextEdit element= (TextEdit)iter.next();
				result.add(element.copy(copier));
			}
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

	protected void adjustOffset(int delta) {
		getTextRange().addToOffset(delta);
	}
	
	protected void adjustLength(int delta) {
		getTextRange().addToLength(delta);
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
	
	/* package */ void setLifeCycle(int state) {
		fLifeCycle= state;
	}
	
	/* package */ boolean isUnconnected() {
		return fLifeCycle == UNCONNECTED;
	}
	
	/* package */ boolean isAdded() {
		return fLifeCycle == ADDED;
	}
	
	/* package */ boolean isConnected() {
		return fLifeCycle == CONNECTED;
	}
	
	/* package */ void internalAdd(TextEdit edit) {
		Assert.isTrue(isUnconnected());
		edit.setParent(this);
		if (fChildren == null) {
			fChildren= new ArrayList(2);
		}
		fChildren.add(edit);
	}
	
	/* package */ void setLifeCycleDeep(int state) {
		setLifeCycle(state);
		if (fChildren != null) {
			for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
				((TextEdit)iter.next()).setLifeCycleDeep(state);
				
			}
		}
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
	
	/* package */ IStatus checkEdit(int bufferLength) {
		TextRange range= getTextRange();
		if (range.getExclusiveEnd() > bufferLength)
			return createErrorStatus(TextManipulationMessages.getString("TextEdit.offset_greater")); //$NON-NLS-1$
		boolean isInsertionPoint= range.isInsertionPoint();
		TextRange cRange= getChildrenTextRange();
		if (!cRange.isUndefined() && (cRange.getOffset() < range.getOffset() || cRange.getExclusiveEnd() > range.getExclusiveEnd())) {
			return createErrorStatus(TextManipulationMessages.getString("TextEdit.range_outside")); //$NON-NLS-1$
		}
		TextRange last= null;
		if (fChildren != null) {
			for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
				TextEdit element= (TextEdit)iter.next();
				TextRange eRange= element.getTextRange();
				
				if (!(isInsertionPoint && eRange.isInsertionPoint()) && last != null && last.getInclusiveEnd() >= eRange.getOffset()) {
					return createErrorStatus(TextManipulationMessages.getString("TextEdit.overlapping")); //$NON-NLS-1$
				}
				IStatus s= element.checkEdit(bufferLength);
				if (!s.isOK())
					return s;
				last= eRange;
			}
		}
		return createOKStatus();
	}
	
	protected static IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, message, null);
	}
	
	protected static IStatus createOKStatus() {
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, TextManipulationMessages.getString("TextEdit.is_valid"), null); //$NON-NLS-1$
	}
	
	//---- Edit processing --------------------------------------------------------------------------------
	
	/* package */ void executeConnect(TextBuffer buffer) throws CoreException {
		Assert.isTrue(isUnconnected());
		fLifeCycle= ADDED;
		List children= getChildren();
		if (children != null) {
			for (int i= children.size() - 1; i >= 0; i--) {
				((TextEdit)children.get(i)).executeConnect(buffer);
			}
		}
		connect(buffer);
		fLifeCycle= CONNECTED;
		sortChildren();
	}
	
	/* package */ void execute(TextBuffer buffer, Updater updater, IProgressMonitor pm) throws CoreException {
		List children= getChildren();
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
				// System.out.println(toString());
			} finally {
				updater.setActiveNode(null);
			}
		}
		pm.worked(1);
	}
	
	/* package */ void updateTextRange(int delta, List executedEdits) {
		for (Iterator iter= executedEdits.iterator(); iter.hasNext();) {
			((TextEdit)iter.next()).predecessorExecuted(delta);
		}
		adjustLength(delta);
		updateParents(delta);
	}
	
	/* package */ void childExecuted(int delta) {
		adjustLength(delta);
	}
	
	/* package */ void predecessorExecuted(int delta) {
		adjustOffset(delta);
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
	
	/* package */ void sortChildren() {
		if (fChildren != null)
			Collections.sort(fChildren, new Sorter());
	}	
}

