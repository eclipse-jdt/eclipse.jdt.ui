/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.InclusivePositionUpdater;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

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
	private IPositionUpdater fUpdater;
		
	/**
	 * Creates a template proposal with a template and its context.
	 * @param replacementString
	 * @param replacementOffset
	 * @param replacementLength
	 * @param image     the icon of the proposal.
	 * @param displayString
	 * @param viewer
	 * @param relevance
	 * @param name
	 * @param paramaterTypePackageNames
	 * @param parameterTypeNames
	 * @param parameterNames
	 * @param codeAssistOffset
	 * @param compilationUnit
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
			Position[] positions;
			String replacementString;
			int baseOffset= getReplacementOffset();
			
			if (appendArguments(document, offset)) {
				parameterCount= fParameterNames.length;
				positionOffsets= new int[parameterCount];
				positionLengths= new int[parameterCount];
				positions= new Position[parameterCount];
				

				replacementString= computeGuessingCompletion(baseOffset, positionOffsets, positionLengths, document, positions);
				
			} else {
				parameterCount= 0;
				positionOffsets= new int[0];
				positionLengths= new int[0];
				positions= new Position[0];
				
				replacementString= new String(fName);
			}

			setReplacementString(replacementString);
		
			super.apply(document, trigger, offset);

			if (parameterCount > 0 && fViewer != null) {
				LinkedModeModel model= new LinkedModeModel();
				for (int i= 0; i != parameterCount; i++) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					int positionOffset= baseOffset + positionOffsets[i];
					if (fChoices[i].length < 2) {
						group.addPosition(new LinkedPosition(document, positionOffset, positionLengths[i], LinkedPositionGroup.NO_STOP));
					} else {
						ensurePositionCategoryInstalled(document, model);
						document.addPosition(getCategory(), positions[i]);
						group.addPosition(new ProposalPosition(document, positionOffset, positionLengths[i], LinkedPositionGroup.NO_STOP, fChoices[i]));
					}
					model.addGroup(group);
				}
				
				model.forceInstall();
				JavaEditor editor= getJavaEditor();
				if (editor != null) {
					model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
				}
								
				LinkedModeUI ui= new EditorLinkedModeUI(model, fViewer);
				ui.setExitPosition(fViewer, baseOffset + replacementString.length(), 0, Integer.MAX_VALUE);
				ui.setExitPolicy(new ExitPolicy(')'));
				ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
				ui.setDoContextInfo(true);
				ui.enter();
				fSelectedRegion= ui.getSelectedRegion();
				
			} else {
				fSelectedRegion= new Region(baseOffset + replacementString.length(), 0);
			}

		} catch (BadLocationException e) {
			ensurePositionCategoryRemoved(document);
			JavaPlugin.log(e);	
			openErrorDialog(e);
		} catch (JavaModelException e) {
			ensurePositionCategoryRemoved(document);
			JavaPlugin.log(e);	
			openErrorDialog(e);			
		} catch (BadPositionCategoryException e) {
			ensurePositionCategoryRemoved(document);
			JavaPlugin.log(e);	
			openErrorDialog(e);			
		}
	}
	
	/**
	 * Returns the currently active java editor, or <code>null</code> if it 
	 * cannot be determined.
	 * 
	 * @return  the currently active java editor, or <code>null</code>
	 */
	private JavaEditor getJavaEditor() {
		IEditorPart part= JavaPlugin.getActivePage().getActiveEditor();
		if (part instanceof JavaEditor)
			return (JavaEditor) part;
		else
			return null;
	}

	private ICompletionProposal[][] guessParameters(int offset, IDocument document, Position[] positions) throws JavaModelException {
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
				fChoices[i]= new JavaCompletionProposal[] {new JavaCompletionProposal(name, offset, length, null, name, 0)};
				offset+= length + 2;
			}
			return fChoices;

		} else {
			
			synchronized (fCompilationUnit) {
				fCompilationUnit.reconcile(ICompilationUnit.NO_AST, false, null, null);
			}
			
			ParameterGuesser guesser= new ParameterGuesser(fCodeAssistOffset, fCompilationUnit);
			for (int i= fParameterNames.length - 1; i >= 0; i--) {
				positions[i]= new Position(0,0);
				String paramName= new String(fParameterNames[i]);
				ICompletionProposal[] parameters= guesser.parameterProposals(
					new String(fParamaterTypePackageNames[i]),
					new String(fParameterTypeNames[i]),
					paramName,
					positions[i],
					document);

				int paramLength= paramName.length();
				if (parameters == null)
					fChoices[i]= new ICompletionProposal[] {new JavaCompletionProposal(paramName, offset, paramLength, null, paramName, 0)};
				else {
					fChoices[i]= parameters;
				}
				offset+= paramLength + 2;
			}

			return fChoices;
		}		
	}
	
	/**
	 * Creates the completion string. Offsets and Lengths are set to the offsets and lengths
	 * of the parameters.
	 */
	private String computeGuessingCompletion(int startOffset, int[] offsets, int[] lengths, IDocument document, Position[] positions) throws JavaModelException {
	
		StringBuffer buffer= new StringBuffer();
		buffer.append(fName);
		buffer.append('(');

		fChoices= guessParameters(startOffset+buffer.length(), document, positions);
		for (int i= 0; i < fChoices.length; i++) {
			if (i != 0)
				buffer.append(", "); //$NON-NLS-1$
			offsets[i]= buffer.length();
			ICompletionProposal[] proposals= fChoices[i];
			String display= proposals.length > 0 ? proposals[0].getDisplayString() : new String(fParameterNames[i]);
			buffer.append(display);
			lengths[i]= buffer.length() - offsets[i];			
			if (proposals.length > 1) {
				positions[i].offset= startOffset + offsets[i];
				positions[i].length= lengths[i];
			}
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
		MessageDialog.openError(shell, JavaTextMessages.getString("ParameterGuessingProposal.error.msg"), e.getMessage()); //$NON-NLS-1$
	}	

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getDisplayString() + super.toString();
	}

	private void ensurePositionCategoryInstalled(final IDocument document, LinkedModeModel model) {
		if (!document.containsPositionCategory(getCategory())) {
			document.addPositionCategory(getCategory());
			fUpdater= new InclusivePositionUpdater(getCategory());
			document.addPositionUpdater(fUpdater);
			
			model.addLinkingListener(new ILinkedModeListener() {

				/*
				 * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
				 */
				public void left(LinkedModeModel environment, int flags) {
					ensurePositionCategoryRemoved(document);
				}

				public void suspend(LinkedModeModel environment) {}
				public void resume(LinkedModeModel environment, int flags) {}
			});
		}
	}

	private void ensurePositionCategoryRemoved(IDocument document) {
		if (document.containsPositionCategory(getCategory())) {
			try {
				document.removePositionCategory(getCategory());
			} catch (BadPositionCategoryException e) {
				// ignore
			}
			document.removePositionUpdater(fUpdater);
		}
	}
	
	private String getCategory() {
		return "ParameterGuessingProposal_" + toString(); //$NON-NLS-1$
	}

}
