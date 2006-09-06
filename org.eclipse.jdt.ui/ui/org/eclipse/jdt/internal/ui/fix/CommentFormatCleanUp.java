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

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class CommentFormatCleanUp extends AbstractCleanUp {
	
	/**
	 * Format single line comment in java code.
	 */
	public static final int SINGLE_LINE_COMMENT= 1;
	
	/**
	 * Format multi line comment in java code.
	 */
	public static final int MULTI_LINE_COMMENT= 2;
	
	/**
	 * Format java doc comments
	 */
	public static final int JAVA_DOC= 4;
	
	private static final int DEFAULT_FLAG= 0;
	private static final String SECTION_NAME= "CleanUp_CommentFormat"; //$NON-NLS-1$
	
	public CommentFormatCleanUp(int flag) {
		super(flag);
	}

	public CommentFormatCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}
	
	public CommentFormatCleanUp(Map options) {
		super(options);
	}
	
	public CommentFormatCleanUp() {
		super();
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return CommentFormatFix.createCleanUp(compilationUnit, 
				isFlag(SINGLE_LINE_COMMENT),
				isFlag(MULTI_LINE_COMMENT),
				isFlag(JAVA_DOC));
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

	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(getSection(settings, SECTION_NAME));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isFlag(MULTI_LINE_COMMENT))
			result.add(MultiFixMessages.CommentFormatCleanUp_multiLineComments);
		if (isFlag(SINGLE_LINE_COMMENT))
			result.add(MultiFixMessages.CommentFormatCleanUp_singleLineComments);
		if (isFlag(JAVA_DOC))
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
				isFlag(SINGLE_LINE_COMMENT),
				isFlag(MULTI_LINE_COMMENT),
				isFlag(JAVA_DOC));
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		return -1;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDefaultFlag() {
		return DEFAULT_FLAG;
	}

    public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
	    return false;
    }
    
    protected int createFlag(Map options) {
	    int flag= 0;
		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.FORMAT_COMMENT))) {
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.FORMAT_SINGLE_LINE_COMMENT))) {
				flag|= SINGLE_LINE_COMMENT;
			}
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.FORMAT_MULTI_LINE_COMMENT))) {
				flag|= MULTI_LINE_COMMENT;
			}
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.FORMAT_JAVADOC))) {
				flag|= JAVA_DOC;
			}
		}
		return flag;
    }
}