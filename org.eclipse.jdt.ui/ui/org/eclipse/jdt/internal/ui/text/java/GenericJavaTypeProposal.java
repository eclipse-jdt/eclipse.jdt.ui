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
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * An experimental proposal.
 */
public class GenericJavaTypeProposal extends JavaTypeCompletionProposal {

	private IRegion fSelectedRegion; // initialized by apply()
//	private final CompletionContext fContext;
	private final CompletionProposal fProposal;

	public GenericJavaTypeProposal(CompletionProposal typeProposal, int offset, int length, ICompilationUnit cu, Image image, String displayString, ITextViewer viewer) {
		super(String.valueOf(typeProposal.getCompletion()), cu, offset, length, image, displayString, typeProposal.getRelevance(), String.valueOf(Signature.getSignatureSimpleName(typeProposal.getSignature())), String.valueOf(Signature.getSignatureQualifier(typeProposal.getSignature())), viewer);
		fProposal= typeProposal;
//		fContext= context;
	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		
		char[] signature= fProposal.getSignature();
		char[][] typeArguments= Signature.getTypeArguments(signature);
		char[][] typeArgumentNames= null;
		
//		final Map bindings= new HashMap();
//		fContext.getExpectedTypesKeys();
//		if (expectedTypesKeys != null && expectedTypesKeys.length > 0) {
//			final String[] keys= new String[expectedTypesKeys.length];
//			for (int i= 0; i < keys.length; i++) {
//				keys[i]= String.valueOf(expectedTypesKeys[i]);
//			}
//			
//			final ASTParser parser= ASTParser.newParser(AST.JLS3);
//			parser.setProject(fCompilationUnit.getJavaProject());
//			parser.setResolveBindings(true);
//			
//			ASTRequestor requestor= new ASTRequestor() {
//
//				public void acceptBinding(String bindingKey,IBinding binding) {
//					bindings.put(bindingKey, binding);
//				}
//			};
//			parser.createASTs(new ICompilationUnit[0], keys, requestor, null);
//		}
//		
		
		// TODO add context awareness
		if (typeArguments.length == 0) {
			// check for binary types which are not reported as parameterized
			if (fCompilationUnit != null) {
				String fullType= SignatureUtil.stripSignatureToFQN(String.valueOf(signature));
				try {
					IType type= fCompilationUnit.getJavaProject().findType(fullType);
					if (type != null) {
						String[] typeParameterSignatures= type.getTypeParameterSignatures();
						if (typeParameterSignatures.length > 0) {
							// we've got a parameterized type!
							// TODO do parameter guessing based on type signatures
							// for now: just add parameters
							typeArgumentNames= new char[typeParameterSignatures.length][];
							for (int i= 0; i < typeParameterSignatures.length; i++) {
								typeArgumentNames[i]= Signature.getTypeVariable(typeParameterSignatures[i].toCharArray());
							}
						}
					}
				} catch (JavaModelException e) {
					// ignore and return the original proposal
				}
			}
			
		} else {
			typeArgumentNames= new char[typeArguments.length][];
			for (int i= 0; i < typeArguments.length; i++) {
				typeArgumentNames[i]= Signature.getSignatureSimpleName(typeArguments[i]);
			}
		}
		
		if (typeArgumentNames == null) {
			// not a parameterized type
			super.apply(document, trigger, offset);
			return;
		}
		
		int count;
		int[] offsets, lengths;
		StringBuffer buffer= new StringBuffer();
		buffer.append(getReplacementString());
		
		if (appendArguments(fTextViewer, offset)) {				
			count= typeArgumentNames.length;
			offsets= new int[count];
			lengths= new int[count];
			
			buffer.append('<');
			for (int i= 0; i != count; i++) {
				if (i != 0)
					buffer.append(", "); //$NON-NLS-1$
					
				offsets[i]= buffer.length();
				buffer.append(typeArgumentNames[i]);
				lengths[i]= buffer.length() - offsets[i];
			}
			buffer.append('>');

		} else {
			count= 0;
			offsets= new int[0];
			lengths= new int[0];				
		}
		

		// set the generic type as replacement string
		super.setReplacementString(buffer.toString());
		// add import & remove package, update replacement offset
		super.apply(document, trigger, offset);

		int replacementOffset= getReplacementOffset();
		String replacementString= getReplacementString();
		int offsetSubtraction= buffer.length() - replacementString.length(); // due to using an import instead of package

		if (offsets.length > 0 && fTextViewer != null) {
			try {
				LinkedModeModel model= new LinkedModeModel();
				for (int i= 0; i != offsets.length; i++) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					group.addPosition(new LinkedPosition(document, replacementOffset + offsets[i] - offsetSubtraction, lengths[i], LinkedPositionGroup.NO_STOP));
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
		} else
			fSelectedRegion= new Region(replacementOffset + replacementString.length(), 0);
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
				
			char ch= line.charAt(index);
			return ch != '<';
		
		} catch (BadLocationException e) {
			return true;
		}
	}
	
}
