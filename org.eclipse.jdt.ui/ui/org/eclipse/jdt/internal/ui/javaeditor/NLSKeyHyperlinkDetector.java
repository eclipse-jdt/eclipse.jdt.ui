/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHintHelper;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * NLS hyperlink detector.
 *
 * @since 3.1
 */
public class NLSKeyHyperlinkDetector implements IHyperlinkDetector {

	private ITextEditor fTextEditor;


	/**
	 * Creates a new NLS hyperlink detector.
	 *
	 * @param editor the editor in which to detect the hyperlink
	 */
	public NLSKeyHyperlinkDetector(ITextEditor editor) {
		Assert.isNotNull(editor);
		fTextEditor= editor;
	}

	/*
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		if (region == null || fTextEditor == null || canShowMultipleHyperlinks)
			return null;

		IEditorSite site= fTextEditor.getEditorSite();
		if (site == null)
			return null;

		IJavaElement javaElement= getInputJavaElement(fTextEditor);
		if (javaElement == null)
			return null;

		CompilationUnit ast= JavaPlugin.getDefault().getASTProvider().getAST(javaElement, ASTProvider.WAIT_NO, null);
		if (ast == null)
			return null;

		ASTNode node= NodeFinder.perform(ast, region.getOffset(), 1);
		if (!(node instanceof StringLiteral)  && !(node instanceof SimpleName))
			return null;
		
		if (node.getLocationInParent() == QualifiedName.QUALIFIER_PROPERTY)
			return null;

		IRegion nlsKeyRegion= new Region(node.getStartPosition(), node.getLength());
		AccessorClassReference ref= NLSHintHelper.getAccessorClassReference(ast, nlsKeyRegion);
		if (ref == null)
			return null;
		String keyName= null;
		if (node instanceof StringLiteral) {
			keyName= ((StringLiteral)node).getLiteralValue();
		} else {
			keyName= ((SimpleName)node).getIdentifier();
		}
		if (keyName != null)
			return new IHyperlink[] {new NLSKeyHyperlink(nlsKeyRegion, keyName, ref, fTextEditor)};

		return null;
	}

	private IJavaElement getInputJavaElement(ITextEditor editor) {
		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)editorInput).getClassFile();

		if (editor instanceof CompilationUnitEditor)
			return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput);

		return null;
	}

}
