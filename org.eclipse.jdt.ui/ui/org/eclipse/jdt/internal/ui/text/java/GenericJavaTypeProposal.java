/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * An experimental proposal.
 */
public final class GenericJavaTypeProposal extends JavaTypeCompletionProposal {

	private IRegion fSelectedRegion; // initialized by apply()
//	private final CompletionContext fContext;
	private final CompletionProposal fProposal;

	public GenericJavaTypeProposal(CompletionProposal typeProposal, CompletionContext context, int offset, int length, ICompilationUnit cu, Image image, String displayString) {
		super(String.valueOf(typeProposal.getCompletion()), cu, offset, length, image, displayString, typeProposal.getRelevance(), String.valueOf(Signature.getSignatureSimpleName(typeProposal.getSignature())), String.valueOf(Signature.getSignatureQualifier(typeProposal.getSignature())));
		fProposal= typeProposal;
//		fContext= context;
	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		
		CharSequence[] typeArgumentNames= computeTypeArgumentNames();
		
		if (typeArgumentNames.length == 0 || !shouldAppendArguments(document, offset)) {
			// not a parameterized type || already followed by generic signature
			super.apply(document, trigger, offset);
			return;
		}
		
		int[] offsets= new int[typeArgumentNames.length];
		int[] lengths= new int[typeArgumentNames.length];
		StringBuffer buffer= createParameterList(typeArgumentNames, offsets, lengths);

		// set the generic type as replacement string
		super.setReplacementString(buffer.toString());
		// add import & remove package, update replacement offset
		super.apply(document, trigger, offset);

		if (fTextViewer != null) {
			String replacementString= getReplacementString();
			int delta= buffer.length() - replacementString.length(); // due to using an import instead of package
			for (int i= 0; i < offsets.length; i++) {
				offsets[i]-= delta;
			}
			installLinkedMode(document, offsets, lengths);
		}
	}
	
	private CharSequence[] computeTypeArgumentNames() throws IllegalArgumentException {
		char[] signature= fProposal.getSignature();
		char[][] typeArguments= Signature.getTypeArguments(signature);
		String[] typeArgumentNames;
		// TODO add context awareness
		if (typeArguments.length > 0) {
			typeArgumentNames= new String[typeArguments.length];
			for (int i= 0; i < typeArguments.length; i++) {
				typeArgumentNames[i]= String.valueOf(Signature.getSignatureSimpleName(typeArguments[i]));
			}
		} else {
			// jdt-core bug XXX
			// type proposals from binary types carry the erased signature
			// so, if we don't get any type arguments, compute the type parameters
			// for the type from the java model.
			ITypeParameter[] parameters= computeTypeParameters();
			typeArgumentNames= new String[parameters.length];
			for (int i= 0; i < parameters.length; i++) {
				typeArgumentNames[i]= parameters[i].getElementName();
			}
		}
		return typeArgumentNames;
	}

	private ITypeParameter[] computeTypeParameters() throws IllegalArgumentException {
		// check for binary types which are not reported as parameterized
		if (fCompilationUnit != null) {
			String fullType= SignatureUtil.stripSignatureToFQN(String.valueOf(fProposal.getSignature()));
			try {
				IType type= fCompilationUnit.getJavaProject().findType(fullType);
				if (type != null) {
					return type.getTypeParameters();
				}
			} catch (JavaModelException e) {
				// ignore and return no type parameters
			}
		}
		return new ITypeParameter[0];
	}
	
	private boolean shouldAppendArguments(IDocument document, int offset) {
		try {
			IRegion region= document.getLineInformationOfOffset(offset);
			String line= document.get(region.getOffset(), region.getLength());
			
			int index= offset - region.getOffset();
			while (index != line.length() && Character.isUnicodeIdentifierPart(line.charAt(index)))
				++index;
			
			if (index == line.length())
				return true;
				
			char ch= line.charAt(index);
			return ch != '<';
		
		} catch (BadLocationException e) {
			return true;
		}
	}
	
	private StringBuffer createParameterList(CharSequence[] typeArgumentNames, int[] offsets, int[] lengths) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(getReplacementString());
		buffer.append('<');
		for (int i= 0; i != typeArgumentNames.length; i++) {
			if (i != 0)
				buffer.append(", "); //$NON-NLS-1$ // TODO respect formatter prefs
				
			offsets[i]= buffer.length();
			buffer.append(typeArgumentNames[i]);
			lengths[i]= buffer.length() - offsets[i];
		}
		buffer.append('>');
		return buffer;
	}

	private void installLinkedMode(IDocument document, int[] offsets, int[] lengths) {
		int replacementOffset= getReplacementOffset();
		String replacementString= getReplacementString();
		try {
			LinkedModeModel model= new LinkedModeModel();
			for (int i= 0; i != offsets.length; i++) {
				LinkedPositionGroup group= new LinkedPositionGroup();
				group.addPosition(new LinkedPosition(document, replacementOffset + offsets[i], lengths[i], LinkedPositionGroup.NO_STOP));
				model.addGroup(group);
			}
			
			model.forceInstall();
			JavaEditor editor= getJavaEditor();
			if (editor != null) {
				model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
			}
			
			LinkedModeUI ui= new EditorLinkedModeUI(model, fTextViewer);
			ui.setExitPosition(fTextViewer, replacementOffset + replacementString.length(), 0, Integer.MAX_VALUE);
			ui.setDoContextInfo(true);
			ui.enter();

			fSelectedRegion= ui.getSelectedRegion();

		} catch (BadLocationException e) {
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

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return super.getSelection(document);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	private void openErrorDialog(BadLocationException e) {
		Shell shell= fTextViewer.getTextWidget().getShell();
		MessageDialog.openError(shell, JavaTextMessages.getString("ExperimentalProposal.error.msg"), e.getMessage()); //$NON-NLS-1$
	}	

}
