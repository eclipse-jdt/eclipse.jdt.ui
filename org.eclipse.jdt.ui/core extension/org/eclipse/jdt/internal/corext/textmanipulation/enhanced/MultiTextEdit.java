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
package org.eclipse.jdt.internal.corext.textmanipulation.enhanced;

import org.eclipse.core.runtime.CoreException;

public final class MultiTextEdit extends TextEdit {

	/* non Java-doc
	 * @see TextEdit#getTextRange
	 */	
	public TextRange getTextRange() {
		return getChildrenTextRange();
	}

	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public void perform(TextBuffer buffer) throws CoreException {
		// do nothing.
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() throws CoreException {
		return new MultiTextEdit();
	}
}
