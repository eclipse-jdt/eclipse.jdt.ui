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
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ExperimentalResultCollector extends ResultCollector {

	/** The text viewer. */
	private ITextViewer fViewer;

	private static boolean appendArguments(ITextViewer viewer, int offset) {
		
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION))
			return true;

		if (viewer == null)
			return true;
							
		try {
			IDocument document= viewer.getDocument();		
			IRegion region= document.getLineInformationOfOffset(offset);
			String line= document.get(region.getOffset(), region.getLength());
			
			int index= offset - region.getOffset();
			while (index != line.length() && Character.isUnicodeIdentifierPart(line.charAt(index)))
				++index;
			
			if (index == line.length())
				return true;
				
			return line.charAt(index) != '(';
		
		} catch (BadLocationException e) {
			return true;
		}
	}
	
	/**
	 * Creates a proposal that includes a best guess for each parameter. Best guesses are computed by the 
	 * {@link ParameterGuesser} when the {@link org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)}
	 * method is called.
	 */
	protected JavaCompletionProposal createMethodCallCompletion(char[] declaringTypeName, char[] name,
		char[][] parameterTypePackageNames, char[][] parameterTypeNames, char[][] parameterNames,
		char[] returnTypeName, char[] completionName, int modifiers, int start, int end, int relevance)
	{		
		JavaCompletionProposal original= super.createMethodCallCompletion(declaringTypeName, name,
			parameterTypePackageNames, parameterTypeNames, parameterNames, returnTypeName,
			completionName, modifiers, start, end, relevance);
		
		// handle empty code completion
		if ((completionName.length == 0) || ((completionName.length == 1) && completionName[0] == ')'))
			return original;
		
		// use original code for 0-argument methods
		if (parameterNames.length == 0)
			return original;			

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();

		if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS)) {
			return new ParameterGuessingProposal(
				new StringBuffer().append(name).append('(').toString(), start, end - start, original.getImage(), original.getDisplayString(), fViewer, relevance,
				name, parameterTypePackageNames, parameterTypeNames, parameterNames, 
				fCodeAssistOffset, fCompilationUnit);
				
		} else {
			int count;
			int[] offsets;
			int[] lengths;
	
			StringBuffer buffer= new StringBuffer();	
			buffer.append(name);
			
			if (appendArguments(fViewer, start)) {				
				count= parameterNames.length;
				offsets= new int[count];
				lengths= new int[count];
				
				buffer.append('(');
				for (int i= 0; i != count; i++) {
					if (i != 0)
						buffer.append(", "); //$NON-NLS-1$
						
					offsets[i]= buffer.length();
					buffer.append(parameterNames[i]);
					lengths[i]= buffer.length() - offsets[i];
				}
				buffer.append(')');

			} else {
				count= 0;
				offsets= new int[0];
				lengths= new int[0];				
			}
			
			return new ExperimentalProposal(buffer.toString(), start, end - start, original.getImage(), original.getDisplayString(), offsets, lengths, fViewer, relevance);
		}
	}

	/**
	 * Copied from super class
	 */
	public void setViewer(ITextViewer viewer) {
		fViewer= viewer;
	}
}
