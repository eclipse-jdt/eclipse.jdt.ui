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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.corext.Assert;

/* package */ abstract class AbstractTransferEdit extends TextEdit {

	private TextRange fRange;
	
	/* package */ int fMode;
	/* package */ final static int UNDEFINED= 0;
	/* package */ final static int INSERT= 1;
	/* package */ final static int DELETE= 2;

	protected AbstractTransferEdit(TextRange range) {
		Assert.isNotNull(range);
		fRange= range;
	}

	/* non Java-doc
	 * @see TextEdit#getTextRange
	 */	
	public TextRange getTextRange() {
		return fRange;
	}
	
	/**
	 * Sets the text edit's range.
	 * <p>
	 * This method should only be called from within the <code>
	 * connect</code> method.
	 * 
	 * @param range the text edit's range.
	 */	
	protected final void setTextRange(TextRange range) {
		Assert.isTrue(range != null && !isConnected());
		fRange= range;
	}
	
	/* package */ void predecessorExecuted(List executedEdits, TextEdit last, int delta) {
		// The first element in the list of executed edits is the one that got
		// executed first. So we have to iterate from the back. Additionally we
		// have to ignore the child edits of this edit, which are the last edits
		// in the list of executed edits.
		for (int i= executedEdits.size() - 1 - getNumberOfChildren(); i >= 0; i--) {
			TextEdit edit= (TextEdit)executedEdits.get(i);
			edit.predecessorExecuted(delta);
			if (edit == last)
				return;
			
		}
	}
	
	/* package */ void move(List children, int delta) {
		if (children != null) {
			for (Iterator iter= children.iterator(); iter.hasNext();) {
				TextEdit element= (TextEdit)iter.next();
				element.adjustOffset(delta);
				move(element.getChildren(), delta);
			}
		}
	}	
}
