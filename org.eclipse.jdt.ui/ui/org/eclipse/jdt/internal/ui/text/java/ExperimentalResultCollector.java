/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ExperimentalResultCollector extends ResultCollector {

	/** The text viewer. */
	private ITextViewer fViewer;
	
	/**
	 * Creates a proposal that includes a best guess for each parameter. Best guesses are computed by the 
	 * {@link ParameterGuessingEngine} when the {@link org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)}
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

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();

		if (preferenceStore.getBoolean(ContentAssistPreference.GUESS_METHOD_ARGUMENTS)) {
			return new ParameterGuessingProposal(
				start, end - start, original.getImage(), original.getDisplayString(), fViewer, relevance,
				name, parameterTypePackageNames, parameterTypeNames, parameterNames, 
				fCodeAssistOffset, fCompilationUnit);
				
		} else {
			int count= parameterNames.length;
			int[] offsets= new int[count];
			int[] lengths= new int[count];
	
			StringBuffer buffer= new StringBuffer();	
			buffer.append(name);
			buffer.append('(');
			for (int i= 0; i != count; i++) {
				if (i != 0)
					buffer.append(", "); //$NON-NLS-1$
					
				offsets[i]= buffer.length();
				buffer.append(parameterNames[i]);
				lengths[i]= buffer.length() - offsets[i];
			}
			buffer.append(')');
	
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