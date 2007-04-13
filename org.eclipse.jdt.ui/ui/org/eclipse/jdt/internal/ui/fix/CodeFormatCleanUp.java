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
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class CodeFormatCleanUp extends AbstractCleanUp {
	
	public CodeFormatCleanUp() {
		super();
	}
	
	public CodeFormatCleanUp(Map options) {
		super(options);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean requireAST(ICompilationUnit unit) throws CoreException {
		return false;
	}
	
	public IFix createFix(ICompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		boolean removeWhitespaces= isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		return CodeFormatFix.createCleanUp(compilationUnit, isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE), removeWhitespaces && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL), removeWhitespaces && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY));
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
		ArrayList result= new ArrayList();
		if (isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE))
			result.add(MultiFixMessages.CodeFormatCleanUp_description);
		
		if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES)) {
			if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL)) {
				result.add(MultiFixMessages.CodeFormatCleanUp_RemoveTrailingAll_description);
			} else if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY)) {
				result.add(MultiFixMessages.CodeFormatCleanUp_RemoveTrailingNoEmpty_description);
			}
		}
		
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		buf.append("public class Engine {\n"); //$NON-NLS-1$
		buf.append("  public void start() {}\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES) && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL)) {
			buf.append("\n"); //$NON-NLS-1$
		} else {
			buf.append("    \n"); //$NON-NLS-1$
		}
		if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES)) {
			buf.append("    public\n"); //$NON-NLS-1$
		} else {
			buf.append("    public \n"); //$NON-NLS-1$
		}
		buf.append("        void stop() {\n"); //$NON-NLS-1$
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		
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