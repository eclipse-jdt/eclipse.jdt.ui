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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CodeFormatCleanUp extends AbstractCleanUp {
	
	public CodeFormatCleanUp() {
		super();
	}
	
	public CodeFormatCleanUp(Map options) {
		super(options);
	}
	
	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return CodeFormatFix.createCleanUp(compilationUnit, isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE));
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
		if (isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE))
			return new String[] {MultiFixMessages.CodeFormatCleanUp_description};
		
		return null;
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		buf.append("package org.model;\n"); //$NON-NLS-1$
		buf.append("public class Engine {\n"); //$NON-NLS-1$
		buf.append("  public void start() {}\n"); //$NON-NLS-1$
		buf.append("    public \n"); //$NON-NLS-1$
		buf.append("        void stop() {\n"); //$NON-NLS-1$
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		
		String original= buf.toString();
		if (!isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE))
			return original;
		
		HashMap preferences= new HashMap(JavaCore.getOptions());
		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, original, 0, original.length(), 0, "\n", preferences); //$NON-NLS-1$
		if (edit == null)
			return original;
		
		IDocument doc= new Document(original);
		try {
			edit.apply(doc);
		} catch (MalformedTreeException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		return doc.get();
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