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
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

/**
 * Changes the visibility of a <code>IMember</code> instance.
 */
public final class ChangeVisibilityEdit extends SimpleTextEdit {

	private final IMember fMember;
	private final int fVisibility;

	/**
	 * Create a new text edit that changes the visibility of the given member.
	 * 
	 * @param member the member for which the visibility is to be changed
	 * @param visibility the new visibility
	 * @see JdtFlags
	 */		
	public ChangeVisibilityEdit(IMember member, int visibility) {
		JdtFlags.assertVisibility(visibility);
		fMember= member;
		Assert.isNotNull(fMember);
		fVisibility= visibility;
	}
	
	/* non Java-doc
	 * @see TextEdit#copy0()
	 */
	protected TextEdit copy0(TextEditCopier copier) {
		return new ChangeVisibilityEdit(fMember, fVisibility);
	}
	
	/* non Java-doc
	 * @see TextEdit#getModifiedElement
	 */
	public Object getModifiedElement() {
		return fMember;
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBuffer buffer) throws CoreException {
		int offset= fMember.getSourceRange().getOffset();
		int length= 0;
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(buffer.getContent(offset, fMember.getSourceRange().getLength()).toCharArray());
		int token= 0;
		try {
			while((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				if (token == ITerminalSymbols.TokenNamepublic || token == ITerminalSymbols.TokenNameprotected || token == ITerminalSymbols.TokenNameprivate) {
					offset+= scanner.getCurrentTokenStartPosition();
					length= scanner.getCurrentTokenEndPosition() - scanner.getCurrentTokenStartPosition() + 1;
					break;
				}
			}
		} catch (InvalidInputException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
		}
		setTextRange(new TextRange(offset, length));
		setText(getVisibilityString(length));
		super.connect(buffer);
	}	
	
	private String getVisibilityString(int tokenLength){
		String text= JdtFlags.getVisibilityString(fVisibility);
		if (tokenLength == 0)
			text+= " "; //$NON-NLS-1$
		return text;
	}
}

