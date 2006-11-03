/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class CommentFormatCleanUp extends AbstractCleanUp {
		
	public CommentFormatCleanUp(Map options) {
		super(options);
	}
	
	public CommentFormatCleanUp() {
		super();
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		boolean formatComment= isEnabled(CleanUpConstants.FORMAT_COMMENT);
		return CommentFormatFix.createCleanUp(compilationUnit,
				formatComment && isEnabled(CleanUpConstants.FORMAT_SINGLE_LINE_COMMENT),
				formatComment && isEnabled(CleanUpConstants.FORMAT_MULTI_LINE_COMMENT),
				formatComment && isEnabled(CleanUpConstants.FORMAT_JAVADOC));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return null;
	}

	public Map getRequiredOptions() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isEnabled(CleanUpConstants.FORMAT_COMMENT) && isEnabled(CleanUpConstants.FORMAT_MULTI_LINE_COMMENT))
			result.add(MultiFixMessages.CommentFormatCleanUp_multiLineComments);
		if (isEnabled(CleanUpConstants.FORMAT_COMMENT) && isEnabled(CleanUpConstants.FORMAT_SINGLE_LINE_COMMENT))
			result.add(MultiFixMessages.CommentFormatCleanUp_singleLineComments);
		if (isEnabled(CleanUpConstants.FORMAT_COMMENT) && isEnabled(CleanUpConstants.FORMAT_JAVADOC))
			result.add(MultiFixMessages.CommentFormatCleanUp_javadocComments);
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n"); //$NON-NLS-1$
		buf.append(" *A Javadoc comment\n"); //$NON-NLS-1$
		buf.append("* @param i\n"); //$NON-NLS-1$
		buf.append(" */\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("/*\n"); //$NON-NLS-1$
		buf.append("*A multi line comment\n"); //$NON-NLS-1$
		buf.append("*/\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("//A single line comment\n"); //$NON-NLS-1$
		return CommentFormatFix.format(buf.toString(),
				isEnabled(CleanUpConstants.FORMAT_COMMENT) && isEnabled(CleanUpConstants.FORMAT_SINGLE_LINE_COMMENT),
				isEnabled(CleanUpConstants.FORMAT_COMMENT) && isEnabled(CleanUpConstants.FORMAT_MULTI_LINE_COMMENT),
				isEnabled(CleanUpConstants.FORMAT_COMMENT) && isEnabled(CleanUpConstants.FORMAT_JAVADOC));
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		return -1;
	}

    public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
	    return false;
    }
    
}