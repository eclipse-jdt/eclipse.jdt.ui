/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.template.TemplateMessages;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;

/**
 * This is a {@link org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal} which includes templates 
 * that represent the best guess completion for each parameter of a method.
 * 
 * @author Andrew McCullough
 */
public class ParameterGuessingProposal extends JavaCompletionProposal {

	private final char[] fName;
	private final char[][] fParameterNames;
	private final char[][] fParamaterTypePackageNames;
	private final char[][] fParameterTypeNames;
	private final int fCodeAssistOffset;
	private final ICompilationUnit fCompilationUnit;
	private final ITextViewer fViewer;
	private IRegion fSelectedRegion; // initialized by apply()
		
	/**
	 * Creates a template proposal with a template and its context.
	 * @param template  the template
	 * @param context   the context in which the template was requested.
	 * @param image     the icon of the proposal.
	 */		
	public ParameterGuessingProposal(
		int replacementOffset, int replacementLength, Image image,
	    String displayString, ITextViewer viewer, int relevance,
		char[] name,  char[][] paramaterTypePackageNames, char[][] parameterTypeNames, char[][] parameterNames,  
		int codeAssistOffset, ICompilationUnit compilationUnit)
	{
		// replacementString is set in apply()
		super("", replacementOffset, replacementLength, image, displayString, relevance);		

		fName= name;
		fParamaterTypePackageNames= paramaterTypePackageNames;
		fParameterTypeNames= parameterTypeNames;
		fParameterNames= parameterNames;
		fViewer= viewer;
		fCodeAssistOffset= codeAssistOffset;
		fCompilationUnit= compilationUnit;
	}
 
 	private static boolean appendArguments(IDocument document, int offset) {
		
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		if (preferenceStore.getBoolean(ContentAssistPreference.INSERT_COMPLETION))
			return true;
							
		try {
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
 
	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {

		try {
			int parameterCount= fParameterNames.length;
			int[] positionOffsets= new int[parameterCount];
			int[] positionLengths= new int[parameterCount];
			String replacementString;
			
			if (appendArguments(document, offset)) {
				parameterCount= fParameterNames.length;
				positionOffsets= new int[parameterCount];
				positionLengths= new int[parameterCount];

				replacementString= computeGuessingCompletion(positionOffsets, positionLengths);
				
			} else {
				parameterCount= 0;
				positionOffsets= new int[0];
				positionLengths= new int[0];
				
				replacementString= new String(fName);
			}		

			setReplacementString(replacementString);
		
			super.apply(document, trigger, offset);

			int replacementOffset= getReplacementOffset();

			if (LinkedPositionManager.hasActiveManager(document)) {
				fSelectedRegion= (positionOffsets.length == 0)
					? new Region(replacementOffset + replacementString.length(), 0)
					: new Region(replacementOffset + positionOffsets[0], positionLengths[0]);
			
			} else {
				LinkedPositionManager manager= new LinkedPositionManager(document);
				for (int i= 0; i != positionOffsets.length; i++)
					manager.addPosition(replacementOffset + positionOffsets[i], positionLengths[i]);
				
				LinkedPositionUI editor= new LinkedPositionUI(fViewer, manager);
				editor.setFinalCaretOffset(replacementOffset + replacementString.length());
				editor.enter();
	
				fSelectedRegion= editor.getSelectedRegion();	
			}

		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
			openErrorDialog(e);

		} catch (JavaModelException e) {
			JavaPlugin.log(e);	
			openErrorDialog(e);			
		}
	}
	
	private String[] guessParameters() throws JavaModelException {
		// find matches in reverse order.  Do this because people tend to declare the variable meant for the last
		// parameter last.  That is, local variables for the last parameter in the method completion are more
		// likely to be closer to the point of codecompletion. As an example consider a "delegation" completion:
		// 
		// 		public void myMethod(int param1, int param2, int param3) {
		// 			someOtherObject.yourMethod(param1, param2, param3);
		//		}
		// 
		// The other consideration is giving preference to variables that have not previously been used in this
		// code completion (which avoids "someOtherObject.yourMethod(param1, param1, param1)";

		String[] parameters= new String[fParameterNames.length];

		if (fCompilationUnit == null) {
			for (int i= 0; i != fParameterNames.length; i++)
				parameters[i]= new String(fParameterNames[i]);
			return parameters;

		} else {

			ParameterGuesser guesser= new ParameterGuesser(fCodeAssistOffset, fCompilationUnit);
			for (int i= fParameterNames.length - 1; i >= 0; i--) {
				String parameter= guesser.guessParameterName(
					new String(fParamaterTypePackageNames[i]),
					new String(fParameterTypeNames[i]),
					new String(fParameterNames[i]));

				parameters[i]= (parameter == null) ? new String(fParameterNames[i]) : parameter;
			}

			return parameters;
		}		
	}
	
	/**
	 * Creates the completion string. Offsets and Lengths are set to the offsets and lengths
	 * of the parameters.
	 */
	private String computeGuessingCompletion(int[] offsets, int[] lengths) throws JavaModelException {
	
		StringBuffer buffer= new StringBuffer();
		buffer.append(fName);
		buffer.append('(');

		String[] parameters= guessParameters();
		for (int i= 0; i < parameters.length; i++) {
			if (i != 0)
				buffer.append(", ");
			offsets[i]= buffer.length();
			buffer.append(parameters[i]);
			lengths[i]= buffer.length() - offsets[i];			
		}

		buffer.append(')');

		return buffer.toString();
	}
	
	
	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return new Point(getReplacementOffset(), 0);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	private void openErrorDialog(Exception e) {
		Shell shell= fViewer.getTextWidget().getShell();
		MessageDialog.openError(shell, TemplateMessages.getString("TemplateEvaluator.error.title"), e.getMessage()); //$NON-NLS-1$
	}	

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getDisplayString() + super.toString();
	}

}