/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *		Andrew McCullough - initial API and implementation
 *		IBM Corporation  - general improvement and bug fixes, partial reimplementation
 *******************************************************************************/
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
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.LinkedEnvironment;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedUIControl;
import org.eclipse.jface.text.link.ProposalPosition;

import org.eclipse.ui.texteditor.link.EditorHistoryUpdater;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.template.java.JavaTemplateMessages;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * This is a {@link org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal} which includes templates 
 * that represent the best guess completion for each parameter of a method.
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
	private ICompletionProposal[][] fChoices; // initialized by guessParameters()
		
	/**
	 * Creates a template proposal with a template and its context.
	 * @param image     the icon of the proposal.
	 */		
	public ParameterGuessingProposal(
		String replacementString, int replacementOffset, int replacementLength, Image image,
	    String displayString, ITextViewer viewer, int relevance,
		char[] name,  char[][] paramaterTypePackageNames, char[][] parameterTypeNames, char[][] parameterNames,  
		int codeAssistOffset, ICompilationUnit compilationUnit)
	{
		super(replacementString, replacementOffset, replacementLength, image, displayString, relevance); //$NON-NLS-1$		

		fName= name;
		fParamaterTypePackageNames= paramaterTypePackageNames;
		fParameterTypeNames= parameterTypeNames;
		fParameterNames= parameterNames;
		fViewer= viewer;
		fCodeAssistOffset= codeAssistOffset;
		fCompilationUnit= compilationUnit;
	}
 
 	private boolean appendArguments(IDocument document, int offset) {
		
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION) ^ fToggleEating)
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
			int[] positionOffsets;
			int[] positionLengths;
			String replacementString;
			int baseOffset= getReplacementOffset();
			
			if (appendArguments(document, offset)) {
				parameterCount= fParameterNames.length;
				positionOffsets= new int[parameterCount];
				positionLengths= new int[parameterCount];

				replacementString= computeGuessingCompletion(baseOffset, positionOffsets, positionLengths, document);
				
			} else {
				parameterCount= 0;
				positionOffsets= new int[0];
				positionLengths= new int[0];
				
				replacementString= new String(fName);
			}

			setReplacementString(replacementString);
		
			super.apply(document, trigger, offset);

			if (parameterCount > 0) {
				LinkedEnvironment environment= new LinkedEnvironment();
				for (int i= 0; i != parameterCount; i++) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					int positionOffset= baseOffset + positionOffsets[i];
					if (fChoices[i].length < 2) {
						group.createPosition(document, positionOffset, positionLengths[i]);
					} else {
						group.addPosition(new ProposalPosition(document, positionOffset, positionLengths[i], LinkedPositionGroup.NO_STOP, fChoices[i]));
					}
					environment.addGroup(group);
				}
				
				environment.forceInstall();
				
				LinkedUIControl editor= new LinkedUIControl(environment, fViewer);
				editor.setPositionListener(new EditorHistoryUpdater());
				editor.setExitPosition(fViewer, baseOffset + replacementString.length(), 0, Integer.MAX_VALUE);
				editor.setExitPolicy(new ExitPolicy(')'));
				editor.setCyclingMode(LinkedUIControl.CYCLE_WHEN_NO_PARENT);
				editor.setDoContextInfo(true);
				editor.enter();
				fSelectedRegion= editor.getSelectedRegion();
				
			} else {
				fSelectedRegion= new Region(baseOffset + replacementString.length(), 0);
			}

		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
			openErrorDialog(e);

		} catch (JavaModelException e) {
			JavaPlugin.log(e);	
			openErrorDialog(e);			
		}
	}
	
	private ICompletionProposal[][] guessParameters(int offset, IDocument document) throws JavaModelException {
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

		fChoices= new ICompletionProposal[fParameterNames.length][];

		if (fCompilationUnit == null) {
			for (int i= 0; i != fParameterNames.length; i++) {
				String name= new String(fParameterNames[i]);
				int length= name.length();
				fChoices[i]= new ICompletionProposal[] {new CompletionProposal(name, offset, length, length)};
				offset+= length + 2;
			}
			return fChoices;

		} else {
			
			synchronized (fCompilationUnit) {
				fCompilationUnit.reconcile();
			}
			
			ParameterGuesser guesser= new ParameterGuesser(fCodeAssistOffset, fCompilationUnit);
			for (int i= fParameterNames.length - 1; i >= 0; i--) {
				String paramName= new String(fParameterNames[i]);
				ICompletionProposal[] parameter= guesser.parameterProposals(
					new String(fParamaterTypePackageNames[i]),
					new String(fParameterTypeNames[i]),
					paramName,
					offset,
					document);

				int paramLength= paramName.length();
				fChoices[i]= (parameter == null) ? new ICompletionProposal[] {new CompletionProposal(paramName, offset, paramLength, paramLength)} : parameter;
				offset+= paramLength + 2;
			}

			return fChoices;
		}		
	}
	
	/**
	 * Creates the completion string. Offsets and Lengths are set to the offsets and lengths
	 * of the parameters.
	 */
	private String computeGuessingCompletion(int startOffset, int[] offsets, int[] lengths, IDocument document) throws JavaModelException {
	
		StringBuffer buffer= new StringBuffer();
		buffer.append(fName);
		buffer.append('(');

		fChoices= guessParameters(startOffset+buffer.length(), document);
		for (int i= 0; i < fChoices.length; i++) {
			if (i != 0)
				buffer.append(", "); //$NON-NLS-1$
			offsets[i]= buffer.length();
			ICompletionProposal[] proposals= fChoices[i];
			String display= proposals.length > 0 ? proposals[0].getDisplayString() : new String(fParameterNames[i]);
			buffer.append(display);
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
		MessageDialog.openError(shell, JavaTemplateMessages.getString("TemplateEvaluator.error.title"), e.getMessage()); //$NON-NLS-1$
	}	

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getDisplayString() + super.toString();
	}

}
