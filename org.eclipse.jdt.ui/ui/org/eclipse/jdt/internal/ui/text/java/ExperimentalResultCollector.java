package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
  
import org.eclipse.jface.text.ITextViewer;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ExperimentalResultCollector extends ResultCollector {
	
	private ITextViewer fViewer;
		
	protected JavaCompletionProposal createMethodCallCompletion(char[] declaringTypeName, char[] name, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypeName, char[] completionName, int modifiers, int start, int end, int relevance) {
		JavaCompletionProposal original= super.createMethodCallCompletion(declaringTypeName, name, parameterTypeNames, parameterNames, returnTypeName, completionName, modifiers, start, end, relevance);
		
		// XXX hack to handle empty code completion
		if ((completionName.length == 0) || ((completionName.length == 1) && completionName[0] == ')'))
			return original;
	
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

	/**
	 * Sets the viewer.
	 * @param viewer The viewer to set
	 */
	public void setViewer(ITextViewer viewer) {
		fViewer= viewer;
	}

}
