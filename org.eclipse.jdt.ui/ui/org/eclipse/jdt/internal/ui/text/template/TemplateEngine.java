/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.Assert;

public class TemplateEngine {

	private static final char ARGUMENTS_BEGIN= '(';
	private static final char ARGUMENTS_END= ')';
	private static final char HTML_TAG_BEGIN= '<';
	private static final char HTML_TAG_END= '>';
	private static final char JAVADOC_TAG_BEGIN= '@';

	private String fPartitionType;
	
	private ArrayList fProposals= new ArrayList();

	public TemplateEngine(String partitionType) {
		Assert.isNotNull(partitionType);
		fPartitionType= new String(partitionType);
	}

	/**
	 * Empties the collector.
	 * 
	 * @param viewer the text viewer  
	 * @param unit   the compilation unit (may be <code>null</code>)
	 */
	public void reset() {
		fProposals.clear();
	}

	/**
	 * Returns the array of matching templates.
	 */
	public ICompletionProposal[] getResults() {
		return (ICompletionProposal[]) fProposals.toArray(new ICompletionProposal[fProposals.size()]);
	}

	/**
	 * Inspects the context of the compilation unit around <code>completionPosition</code>
	 * and feeds the collector with proposals.
	 * @param viewer             the text viewer
	 * @param completionPosition the context position in the document of the text viewer
	 * @param unit               the compilation unit (may be <code>null</code>)
	 */
	public void complete(ITextViewer viewer, int completionPosition, ICompilationUnit sourceUnit)
		throws JavaModelException
	{
		Assert.isNotNull(viewer);

		IDocument document= viewer.getDocument();
		String source= document.get();

		// template key
		int end = completionPosition;
		int start= guessStart(source, end, fPartitionType);				
		String key= source.substring(start, end);

		TemplateContext context= new TemplateContext(viewer, start, end, sourceUnit);

		Template[] templates= TemplateSet.getInstance().getMatchingTemplates(key, fPartitionType);

		for (int i= 0; i != templates.length; i++) {
			TemplateProposal proposal= new TemplateProposal(templates[i], context);
			fProposals.add(proposal);
		}
	}

	private static final int guessStart(String source, int end, String partitionType) {
		int start= end;

		if (partitionType.equals(TemplateContext.JAVA)) {				
			while ((start != 0) && Character.isUnicodeIdentifierPart(source.charAt(start - 1)))
				start--;
			
			if ((start != 0) && Character.isUnicodeIdentifierStart(source.charAt(start - 1)))
				start--;
				
		} else if (partitionType.equals(TemplateContext.JAVADOC)) {

			// javadoc tag
			if ((start != 0) && (source.charAt(start - 1) == HTML_TAG_END))
				start--;

			while ((start != 0) && Character.isUnicodeIdentifierPart(source.charAt(start - 1)))
				start--;
			
			if ((start != 0) && Character.isUnicodeIdentifierStart(source.charAt(start - 1)))
				start--;

			// include html and javadoc tags
			if ((start != 0) &&
				(source.charAt(start - 1) == HTML_TAG_BEGIN) ||
				(source.charAt(start - 1) == JAVADOC_TAG_BEGIN))				
			{
				start--;
			}
				
		}

		return start;		
	}	

}

