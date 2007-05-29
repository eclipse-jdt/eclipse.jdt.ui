/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

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
	
	public IFix createFix(ICompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		if (!isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE))
			return null;
		
		HashMap preferences= new HashMap(compilationUnit.getJavaProject().getOptions(true));
		
		boolean singleLineComment= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT));
		boolean blockComment= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT));
		boolean javaDoc= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT));

		return CommentFormatFix.createCleanUp(compilationUnit, singleLineComment, blockComment, javaDoc, preferences);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean requireAST(ICompilationUnit unit) throws CoreException {
		return false;
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
		return null;
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n"); //$NON-NLS-1$
		buf.append(" *A Javadoc comment\n"); //$NON-NLS-1$
		buf.append("* @since 2007\n"); //$NON-NLS-1$
		buf.append(" */\n"); //$NON-NLS-1$
		
		return buf.toString();
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
