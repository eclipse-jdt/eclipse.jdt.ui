package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.jface.util.Assert;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.IContextInformation;


public class JavaCompletionProposal implements ICompletionProposal, ICompletionProposalExtension {

	private String fDisplayString;
	private String fReplacementString;
	private int fReplacementOffset;
	private int fReplacementLength;
	private int fCursorPosition;
	private Image fImage;
	private IContextInformation fContextInformation;
	private int fContextInformationPosition;
	private ProposalInfo fProposalInfo;
	private IImportDeclaration fImportDeclaration;
	private char[] fTriggerCharacters;

	/**
	 * Creates a new completion proposal based on the provided information.  The replacement string is
	 * considered being the display string too. All remaining fields are set to <code>null</code>.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition) {
		this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null, null);
	}

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal
	 * @param contentInformation the context information associated with this proposal
	 * @param optional import declaration to be added. Can be <code>null</code>. The underlying compilation unit
	 * is assumed to be compatible with the document passed in <code>apply</code>.
	 * @param additionalProposalInfo the additional information associated with this proposal or <code>null</code>
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, IImportDeclaration importDeclaration, ProposalInfo proposalInfo) {
		this(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,  contextInformation,  importDeclaration,  null, proposalInfo);
	}
	
	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal
	 * @param contentInformation the context information associated with this proposal
	 * @param optional import declaration to be added. Can be <code>null</code>. The underlying compilation unit
	 * is assumed to be compatible with the document passed in <code>apply</code>.
	 * @param triggerCharacters the set of characters which can trigger the application of this completion proposal
	 * @param additionalProposalInfo the additional information associated with this proposal or <code>null</code>
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, IImportDeclaration importDeclaration, char[] triggerCharacters, ProposalInfo proposalInfo) {
		Assert.isNotNull(replacementString);
		Assert.isTrue(replacementOffset >= 0);
		Assert.isTrue(replacementLength >= 0);
		Assert.isTrue(cursorPosition >= 0);
		
		fReplacementString= replacementString;
		fReplacementOffset= replacementOffset;
		fReplacementLength= replacementLength;
		fCursorPosition= cursorPosition;
		fImage= image;
		fDisplayString= displayString;
		fContextInformation= contextInformation;
		fContextInformationPosition= (fContextInformation != null ? fCursorPosition : -1);
		fImportDeclaration= importDeclaration;
		fTriggerCharacters= triggerCharacters;
		fProposalInfo= proposalInfo;
	}
	
	private void applyImport(IDocument document) {
		ICompilationUnit cu= (ICompilationUnit) JavaModelUtil.findElementOfKind(fImportDeclaration, IJavaElement.COMPILATION_UNIT);
		if (cu != null) {
			try {
				IType[] types= cu.getTypes();
				if (types.length == 0 || types[0].getSourceRange().getOffset() > fReplacementOffset) {
					// do not add import for code assist on import statements
					return;
				}

				int oldLen= document.getLength();
				
				ImportsStructure impStructure= new ImportsStructure(cu);
				impStructure.addImport(fImportDeclaration.getElementName());
				impStructure.create(document, null);
				
				fCursorPosition += document.getLength() - oldLen;
				
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}
	
	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger) {
		try {
			
			if (trigger == (char) 0)
				document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
			else {
				StringBuffer buffer= new StringBuffer(fReplacementString);
				buffer.insert(fCursorPosition, trigger);
				++fCursorPosition;
				document.replace(fReplacementOffset, fReplacementLength, buffer.toString());
			}
			
			if (fImportDeclaration != null) {
				applyImport(document);
			}
			
		} catch (BadLocationException x) {
			// ignore
		}	
	}
	
	/*
	 * @see ICompletionProposal#apply
	 */
	public void apply(IDocument document) {
		apply(document, (char) 0);
	}
	
	/*
	 * @see ICompletionProposal#getSelection
	 */
	public Point getSelection(IDocument document) {
		return new Point(fReplacementOffset + fCursorPosition, 0);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return fContextInformation;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		if (fDisplayString != null)
			return fDisplayString;
		return fReplacementString;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		if (fProposalInfo != null) {
			return fProposalInfo.getInfo();
		}
		return null;
	}
	
	/*
	 * @see ICompletionProposalExtension#getTriggerCharacters()
	 */
	public char[] getTriggerCharacters() {
		return fTriggerCharacters;
	}

	/*
	 * @see ICompletionProposalExtension#getContextInformationPosition()
	 */
	public int getContextInformationPosition() {
		return fReplacementOffset + fContextInformationPosition;
	}
}