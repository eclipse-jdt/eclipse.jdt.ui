/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.util.ArrayList;

import java.util.List;
import java.util.StringTokenizer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class TemplateEngine {

	private static final char ARGUMENTS_BEGIN= '(';
	private static final char ARGUMENTS_END= ')';
	private static final char HTML_TAG_BEGIN= '<';
	private static final char HTML_TAG_END= '>';
	private static final char JAVADOC_TAG_BEGIN= '@';
	
	/**
	 * Partition types.
	 */
	public static String JAVA= "java"; // $NON-NLS-1$
	public static String JAVADOC= "javadoc"; // $NON-NLS-1$

	private String fPartitionType;
	private ArrayList fExactProposals= new ArrayList();
	private ArrayList fNotExactProposals= new ArrayList();

	public TemplateEngine(String partitionType) {
		Assert.isNotNull(partitionType);
		fPartitionType= new String(partitionType);
	}

	/**
	 * Empties the collector.
	 */
	public void reset() {
		fExactProposals.clear();
		fNotExactProposals.clear();		
	}

	/**
	 * Returns an array of templates matching exactly.
	 */
	public ICompletionProposal[] getExactResults() {
		return (ICompletionProposal[]) fExactProposals.toArray(new ICompletionProposal[fExactProposals.size()]);
	}
	
	/**
	 * Returns an array of templates matching not exactly.
	 */
	public ICompletionProposal[] getNotExactResults() {
		return (ICompletionProposal[]) fNotExactProposals.toArray(new ICompletionProposal[fNotExactProposals.size()]);
	}

	/**
	 * Inspects the context of the compilation unit around <code>completionPosition</code>
	 * and feeds the collector with proposals.
	 * @param collector          the collector for template proposals.
	 * @param sourceUnit         the compilation unit.
	 * @param completionPosition the context position in the compilation unit.
	 */
	public void complete(ICompilationUnit sourceUnit, int completionPosition)
		throws JavaModelException
	{
		String source= sourceUnit.getSource();

		// inspect context
		int end = completionPosition;
		int start= guessStart(source, end, fPartitionType);

		// handle optional argument
		String request= source.substring(start, end);
		int index= request.indexOf(ARGUMENTS_BEGIN);

		String key;
		String[] arguments;
		if (index == -1) {
			key= request;
			arguments= null;
		} else {
			key= request.substring(0, index);
			
			String allArguments= request.substring(index + 1, request.length() - 1);			
			List list= new ArrayList();
			StringTokenizer tokenizer= new StringTokenizer(allArguments, ","); // $NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token= tokenizer.nextToken().trim();
				list.add(token);
			}			
			arguments= (String[]) list.toArray(new String[list.size()]);			
		}

		// match context with template
		Template[] templates= TemplateSet.getInstance().getMatchingTemplates(key, fPartitionType);

		for (int i= 0; i != templates.length; i++) {
			TemplateProposal proposal= new TemplateProposal(templates[i], arguments, start, end);			

			if (templates[i].getName().equals(key)) {
				fExactProposals.add(proposal);
			} else {
				fNotExactProposals.add(proposal);
			}
		}
	}

	private static final int guessStart(String source, int end, String partitionType) {
		int start= end;

		if (partitionType.equals(JAVA)) {
			
			// optional arguments
			if ((start != 0) && (source.charAt(start - 1) == ARGUMENTS_END)) {
				start--;
				
				while ((start != 0) && (source.charAt(start - 1) != ARGUMENTS_BEGIN))
					start--;
				start--;
					
				if ((start != 0) && Character.isWhitespace(source.charAt(start - 1)))
					start--;
			}				

			while ((start != 0) && Character.isUnicodeIdentifierPart(source.charAt(start - 1)))
				start--;
			
			if ((start != 0) && Character.isUnicodeIdentifierStart(source.charAt(start - 1)))
				start--;
				
		} else if (partitionType.equals(JAVADOC)) {

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

