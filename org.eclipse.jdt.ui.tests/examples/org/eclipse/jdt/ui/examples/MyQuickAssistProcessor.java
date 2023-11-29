/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.tests.quickfix.QuickFixTest;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class MyQuickAssistProcessor implements IQuickAssistProcessor {

	private boolean getConvertProposal(IInvocationContext context, List<ChangeCorrectionProposal> result) {
		ASTNode node= context.getCoveringNode();
		if (!(node instanceof StringLiteral)) {
			return false;
		}
		if (result == null) {
			return true;
		}

		StringLiteral oldLiteral= (StringLiteral) node;

		AST ast= node.getAST();
		StringLiteral newLiteral= ast.newStringLiteral();
		newLiteral.setEscapedValue(toUpperCase(oldLiteral.getEscapedValue()));

		ASTRewrite rewrite= ASTRewrite.create(ast);
		rewrite.replace(oldLiteral, newLiteral, null);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		result.add(new ASTRewriteCorrectionProposal("To uppercase", context.getCompilationUnit(), rewrite, 10, image));
		return true;
	}

	private String toUpperCase(String escapedValue) {
		int length= escapedValue.length();
		StringBuilder buf= new StringBuilder(length);
		boolean inEscape= false;
		for (int i= 0; i < length; i++) {
			char ch= escapedValue.charAt(i);
			if (inEscape) {
				buf.append(ch);
				inEscape= false;
			} else if (ch == '\\') {
				buf.append(ch);
				inEscape= true;
			} else {
				buf.append(Character.toUpperCase(ch));
			}
		}
		return buf.toString();
	}

	private boolean getStringWrappedProposal(final IInvocationContext context, List<ChangeCorrectionProposal> result) throws CoreException {
		int selectionOffset= context.getSelectionOffset();
		int selectionLength= context.getSelectionLength();

		ITextFileBufferManager textFileBufferManager= FileBuffers.getTextFileBufferManager();
		IPath path= context.getCompilationUnit().getPath();

		try {
			textFileBufferManager.connect(path, LocationKind.NORMALIZE, null);
			IDocument document= textFileBufferManager.getTextFileBuffer(path, LocationKind.NORMALIZE).getDocument();
			int startLine= document.getLineOfOffset(selectionOffset);
			int endLine= document.getLineOfOffset(selectionOffset + selectionLength);


			if (startLine == endLine) {
				return false;
			}
			if (result == null) {
				return true;
			}
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			result.add(new ChangeCorrectionProposal("Wrap in buf.append() (to clipboard)", null, 3, image) {
				@Override
				protected void performChange(IEditorPart activeEditor, IDocument doc) throws CoreException {
					wrapAndCopyToClipboard(context, doc);
					super.performChange(activeEditor, doc);
				}
			});
		} catch (BadLocationException e) {
			return false;
		} finally {
			textFileBufferManager.disconnect(path, LocationKind.NORMALIZE, null);
		}
		return true;
	}

	private boolean getCreateQuickFixTestProposal(final IInvocationContext context, List<ChangeCorrectionProposal> result) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		if (context.getSelectionOffset() != 0 || context.getSelectionLength() != cu.getSourceRange().getLength()) {
			return false;
		}
		if (context.getASTRoot().getProblems().length != 1) {
			return false;
		}

		if (result == null) {
			return true;
		}

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		result.add(new ChangeCorrectionProposal("Create quick fix test", null, 3, image) {
			@Override
			protected void performChange(IEditorPart activeEditor, IDocument doc) throws CoreException {
				try {
					String content= QuickFixTest.getPreviewsInBufAppend(cu);
					if (content != null) {
						Clipboard clipboard= new Clipboard(Display.getCurrent());
						clipboard.setContents(new Object[] { content }, new Transfer[] { TextTransfer.getInstance() } );
						super.performChange(activeEditor, doc);
						return;
					}
				} catch (BadLocationException e) {
					// ignore
				}
 				MessageDialog.openError(activeEditor.getSite().getShell(), "Create quick fix test", "Could not create quick fix test");

			}
		});
		return true;
	}

	protected void wrapAndCopyToClipboard(IInvocationContext context, IDocument document) {
		StringBuilder buf= new StringBuilder();
		try {
			int selectionOffset= context.getSelectionOffset();
			int selectionLength= context.getSelectionLength();
			int startLine= document.getLineOfOffset(selectionOffset);
			int endLine= document.getLineOfOffset(selectionOffset + selectionLength);

			for (int i= startLine; i <= endLine; i++) {
				IRegion lineInfo= document.getLineInformation(i);
				String lineContent= document.get(lineInfo.getOffset(), lineInfo.getLength());
				buf.append("buf.append(\"");
				for (int k= 0; k < lineContent.length(); k++) {
					char ch= lineContent.charAt(k);
					switch (ch) {
					case '\t':
						buf.append("    "); // 4 spaces
						break;
					case '"':
					case '\\':
						buf.append('\\').append(ch);
						break;
					default:
						buf.append(ch);
						break;
					}
				}
				buf.append("\\n\");");
				if (i != endLine) {
					buf.append('\n');
				}
			}
		} catch (BadLocationException e) {
			// ignore
		}
		Clipboard clipboard= new Clipboard(Display.getCurrent());
		clipboard.setContents(new Object[] { buf.toString() }, new Transfer[] { TextTransfer.getInstance() } );

	}

	@Override
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		return getConvertProposal(context, null) || getStringWrappedProposal(context, null) || getCreateQuickFixTestProposal(context, null);
	}

	@Override
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		ArrayList<ChangeCorrectionProposal> resultingCollections= new ArrayList<>();
		getConvertProposal(context, resultingCollections);
		getStringWrappedProposal(context, resultingCollections);
		getCreateQuickFixTestProposal(context, resultingCollections);
		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

}
