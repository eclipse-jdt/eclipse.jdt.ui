/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;

public final class AutoOrganizingTextEdit extends TextEdit {

	private List fElements;

	public AutoOrganizingTextEdit() {
		super();
		fElements= new ArrayList(2);
	}
	
	private AutoOrganizingTextEdit(List elements) {
		fElements= elements;
	}

	/* non Java-doc
	 * @see TextEdit#add
	 */	
	public void add(TextEdit edit) {
		Assert.isTrue(isUnconnected());
		fElements.add(edit);
	}
	
	/* non Java-doc
	 * @see TextEdit#hasChildren
	 */	
	public boolean hasChildren() {
		if (fElements != null && isUnconnected())
			return !fElements.isEmpty();
		return super.hasChildren();
	}

	/* non Java-doc
	 * @see TextEdit#copy0
	 */	
	protected TextEdit copy0(TextEditCopier copier) {
		List newElements= new ArrayList(fElements.size());
		for (Iterator iter= fElements.iterator(); iter.hasNext();) {
			newElements.add(((TextEdit)iter.next()).copy(copier));
		}		
		return new AutoOrganizingTextEdit(newElements);
	}

	/* non Java-doc
	 * @see TextEdit#getTextRange
	 */	
	public final TextRange getTextRange() {
		TextRange result= getChildrenTextRange();
		if (result == null || result.isUndefined())
			return new TextRange(0,0);
		return result;
	}

	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public void perform(TextBuffer buffer) throws CoreException {
		// do nothing
	}

	/* non Java-doc
	 * @see TextEdit#iterator
	 */	
	public Iterator iterator() {
		if (fElements != null && isUnconnected())
			return fElements.iterator();
		return super.iterator();
	}
	
	/* non Java-doc
	 * @see TextEdit#adjustOffset
	 */	
	protected void adjustOffset(int delta) {
		// do nothing since this edit doesn't manage its own TextRange
	}
	
	/* non Java-doc
	 * @see TextEdit#adjustLength
	 */	
	protected void adjustLength(int delta) {
		// do nothing since this edit doesn't manage its own TextRange
	}
	
	/* package */ void executePostProcessCopy(TextEditCopier copier) {
		if (fElements != null && isUnconnected()) {
			postProcessCopy(copier);
			for (Iterator iter= fElements.iterator(); iter.hasNext();) {
				((TextEdit)iter.next()).executePostProcessCopy(copier);
			}
		} else {
			super.executePostProcessCopy(copier);
		}
	}
	
	/* package */ void executeConnect(TextBuffer buffer) throws CoreException {
		Assert.isTrue(isUnconnected());
		setLifeCycle(ADDED);
		int size= fElements.size();
		for (int i= size - 1; i >= 0; i--) {
			((TextEdit)fElements.get(i)).executeConnect(buffer);
		}
		setLifeCycle(UNCONNECTED);
		for (int i= 0; i < size; i++) {
			TextEdit edit= ((TextEdit)fElements.get(i));
			edit.setLifeCycleDeep(UNCONNECTED);
			insert(this, edit);
		}
		fElements= null;
			
		connect(buffer);
		sortAndConnect(this);
	}
	
	private static void insert(TextEdit parent, TextEdit edit) {
		List children= parent.getChildren();
		if (children == null) {
			parent.internalAdd(edit);
			return;
		}
		children= new ArrayList(children); // clone the list
		// Can be optimize using binary search
		for (Iterator iter= children.iterator(); iter.hasNext();) {
			TextEdit child= (TextEdit)iter.next();
			if (child.getTextRange().covers(edit.getTextRange())) {
				insert(child, edit);
				return;
			}
		}
		for (int i= children.size() - 1; i >= 0; i--) {
			TextEdit child= (TextEdit)children.get(i);
			if (edit.getTextRange().covers(child.getTextRange())) {
				parent.remove(i);
				edit.internalAdd(child);
				parent.internalAdd(edit);
				for (int r= i  - 1; r >= 0; r--) {
					TextEdit child2= (TextEdit) children.get(r);
					if (edit.getTextRange().covers(child2.getTextRange())) {
						parent.remove(r);
						edit.internalAdd(child2);
					}
				}
				return;
			}
		}
		parent.internalAdd(edit);
	}
	
	private static void sortAndConnect(TextEdit edit) {
		List children= edit.getChildren();
		if (children != null) {
			for (Iterator iter= children.iterator(); iter.hasNext();) {
				sortAndConnect((TextEdit)iter.next());
			}
		}
		edit.sortChildren();
		edit.setLifeCycle(CONNECTED);
	}
}
