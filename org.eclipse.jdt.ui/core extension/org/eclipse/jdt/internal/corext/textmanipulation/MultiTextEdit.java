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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;

public class MultiTextEdit extends TextEdit {

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
	public final void perform(TextBuffer buffer) throws CoreException {
		// do nothing.
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0(TextEditCopier copier) {
		Assert.isTrue(MultiTextEdit.class == getClass(), "Subclasses must reimplement copy0"); //$NON-NLS-1$
		return new MultiTextEdit();
	}

	/* non Java-doc
	 * @see TextEdit#adjustOffset
	 */	
	public void adjustOffset(int delta) {
		// do nothing since this edit doesn't manage its own TextRange
	}
	
	/* non Java-doc
	 * @see TextEdit#adjustLength
	 */	
	public void adjustLength(int delta) {
		// do nothing since this edit doesn't manage its own TextRange
	}
}
