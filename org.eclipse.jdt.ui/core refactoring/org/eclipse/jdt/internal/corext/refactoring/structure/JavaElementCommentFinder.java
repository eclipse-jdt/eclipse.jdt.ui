/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;


public class JavaElementCommentFinder {
	private JavaElementCommentFinder(){}
	
	/**
	 * returns <code>null</code> if there's no comment or member is not declared inside of a 
	 * <code>ICompilationUnit</code> or <code>IClassFile</code>
	 */
	public static String getCommentContent(IMember member) throws JavaModelException{
		if (member.getCompilationUnit() != null)			
			return getBufferContents(member.getCompilationUnit(), getCommentRange(member));
		else if (member.getClassFile() != null)
			return getBufferContents(member.getClassFile(), getCommentRange(member));
		else
			return null;
	}

	private static String getBufferContents(IOpenable element, ISourceRange range) throws JavaModelException{
		if (range == null)
			return null;
		return element.getBuffer().getText(range.getOffset(), range.getLength());
	}
	
	/**
	 * returns <code>null</code> if there's no comment or member is not declared inside of a 
	 * <code>ICompilationUnit</code> or <code>IClassFile</code>
	 */
	public static ISourceRange getCommentRange(IMember member) throws JavaModelException{
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(member.getSource().toCharArray());
		try {
			int firstToken= scanner.getNextToken();
			if (! TokenScanner.isComment(firstToken))
				return null;
			return new SourceRange(member.getSourceRange().getOffset() + scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() - scanner.getCurrentTokenStartPosition() + 1);
		} catch (InvalidInputException e) {
			return null;
		}
	}
}
