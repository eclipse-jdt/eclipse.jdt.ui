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

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CommentFormatFix;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.internal.ui.fix.IMultiLineCleanUp.MultiLineCleanUpContext;

public class CommentFormatCleanUp extends AbstractCleanUp {
	
	public CommentFormatCleanUp(Map options) {
		super(options);
	}
	
	public CommentFormatCleanUp() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public CleanUpRequirements getRequirements() {
		boolean requiresChangedRegions= isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE) && isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		return new SaveActionRequirements(false, false, null, requiresChangedRegions);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CleanUpContext context) throws CoreException {
		ICompilationUnit compilationUnit= context.getCompilationUnit();
		if (compilationUnit == null)
			return null;
			
		if (!isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE))
			return null;
		
		HashMap preferences= new HashMap(compilationUnit.getJavaProject().getOptions(true));
		
		boolean singleLineComment= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT));
		boolean blockComment= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT));
		boolean javaDoc= DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT));
		
		IRegion[] regions;
		if (context instanceof MultiLineCleanUpContext) {
			regions= ((MultiLineCleanUpContext)context).getRegions();
		} else {
			regions= null;
		}

		return CommentFormatFix.createCleanUp(compilationUnit, regions, singleLineComment, blockComment, javaDoc, preferences);
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
}
