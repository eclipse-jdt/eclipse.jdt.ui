/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.ui.IEditorInput;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.core.manipulation.search.BreakContinueTargetFinder;
import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder.OccurrenceLocation;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.CollectionsUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


/**
 * Java element hyperlink detector.
 *
 * @since 3.1
 */
public class JavaElementHyperlinkDetector extends AbstractHyperlinkDetector {

	/* cache for the last result from codeSelect(..) */
	private static ITypeRoot fLastInput;

	private static long fLastModStamp;

	private static IRegion fLastWordRegion;

	private static IJavaElement[] fLastElements;

	/*
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		ITextEditor textEditor= getAdapter(ITextEditor.class);
		if (region == null || !(textEditor instanceof JavaEditor))
			return null;

		IAction openAction= textEditor.getAction("OpenEditor"); //$NON-NLS-1$
		if (!(openAction instanceof SelectionDispatchAction))
			return null;

		int offset= region.getOffset();

		ITypeRoot input= EditorUtility.getEditorInputJavaElement(textEditor, false);
		if (input == null)
			return null;

		try {
			IDocumentProvider documentProvider= textEditor.getDocumentProvider();
			IEditorInput editorInput= textEditor.getEditorInput();
			IDocument document= documentProvider.getDocument(editorInput);
			IRegion wordRegion= JavaWordFinder.findWord(document, offset);
			if (wordRegion == null || wordRegion.getLength() == 0)
				return null;

			if (isInheritDoc(document, wordRegion) && getClass() != JavaElementHyperlinkDetector.class)
				return null;

			if (JavaElementHyperlinkDetector.class == getClass() && findBreakOrContinueTarget(input, region) != null)
				return new IHyperlink[] { new JavaElementHyperlink(wordRegion, (SelectionDispatchAction) openAction, null, false) };

			if (JavaElementHyperlinkDetector.class == getClass() && findSwitchCaseTarget(input, region) != null)
				return new IHyperlink[] { new JavaElementHyperlink(wordRegion, (SelectionDispatchAction) openAction, null, false) };

			if (JavaElementHyperlinkDetector.class == getClass() && findEnumConstructorTarget(input, region) != null) {
				return new IHyperlink[] { new JavaElementHyperlink(wordRegion, (SelectionDispatchAction) openAction, null, false) };
			}

			IJavaElement[] elements;
			long modStamp= documentProvider.getModificationStamp(editorInput);
			if (input.equals(fLastInput) && modStamp == fLastModStamp && wordRegion.equals(fLastWordRegion)) {
				elements= fLastElements;
			} else {
				elements= ((ICodeAssist) input).codeSelect(wordRegion.getOffset(), wordRegion.getLength());
				elements= selectOpenableElements(elements);
				fLastInput= input;
				fLastModStamp= modStamp;
				fLastWordRegion= wordRegion;
				fLastElements= elements;
			}
			if (elements.length == 0)
				return null;

			ArrayList<IHyperlink> links= new ArrayList<>(elements.length);
			for (IJavaElement element : elements) {
				addHyperlinks(links, wordRegion, (SelectionDispatchAction) openAction, element, elements.length > 1, (JavaEditor) textEditor);
			}
			if (links.isEmpty())
				return null;

			return CollectionsUtil.toArray(links, IHyperlink.class);

		} catch (JavaModelException e) {
			return null;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		fLastElements= null;
		fLastInput= null;
		fLastWordRegion= null;
	}

	/**
	 * Returns whether the word is "inheritDoc".
	 *
	 * @param document the document
	 * @param wordRegion the word region
	 * @return <code>true</code> iff the word is "inheritDoc"
	 * @since 3.7
	 */
	private static boolean isInheritDoc(IDocument document, IRegion wordRegion) {
		try {
			String word= document.get(wordRegion.getOffset(), wordRegion.getLength());
			return "inheritDoc".equals(word); //$NON-NLS-1$
		} catch (BadLocationException e) {
			return false;
		}
	}

	/**
	 * Creates and adds Java element hyperlinks.
	 *
	 * @param hyperlinksCollector the list to which hyperlinks should be added
	 * @param wordRegion the region of the link
	 * @param openAction the action to use to open the Java elements
	 * @param element the Java element to open
	 * @param qualify <code>true</code> if the hyperlink text should show a qualified name for
	 *            element
	 * @param editor the active Java editor
	 *
	 * @since 3.5
	 */
	protected void addHyperlinks(List<IHyperlink> hyperlinksCollector, IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		hyperlinksCollector.add(new JavaElementHyperlink(wordRegion, openAction, element, qualify));
	}


	/**
	 * Selects the openable elements out of the given ones.
	 *
	 * @param elements the elements to filter
	 * @return the openable elements
	 * @since 3.4
	 */
	private IJavaElement[] selectOpenableElements(IJavaElement[] elements) {
		List<IJavaElement> result= new ArrayList<>(elements.length);
		for (IJavaElement element : elements) {
			switch (element.getElementType()) {
				case IJavaElement.PACKAGE_DECLARATION:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.JAVA_MODEL:
					break;
				default:
					result.add(element);
					break;
			}
		}
		return result.toArray(new IJavaElement[result.size()]);
	}

	/**
	 * Finds the target for break or continue node.
	 *
	 * @param input the editor input
	 * @param region the region
	 * @return the break or continue target location or <code>null</code> if none
	 * @since 3.7
	 */
	public static OccurrenceLocation findBreakOrContinueTarget(ITypeRoot input, IRegion region) {
		CompilationUnit astRoot= SharedASTProviderCore.getAST(input, SharedASTProviderCore.WAIT_NO, null);
		if (astRoot == null)
			return null;

		ASTNode node= NodeFinder.perform(astRoot, region.getOffset(), region.getLength());
		ASTNode breakOrContinueNode= null;
		boolean labelSelected= false;
		if (node instanceof SimpleName) {
			SimpleName simpleName= (SimpleName) node;
			StructuralPropertyDescriptor location= simpleName.getLocationInParent();
			if (location == ContinueStatement.LABEL_PROPERTY || location == BreakStatement.LABEL_PROPERTY) {
				breakOrContinueNode= simpleName.getParent();
				labelSelected= true;
			}
		} else if (node instanceof ContinueStatement || node instanceof BreakStatement)
			breakOrContinueNode= node;

		if (breakOrContinueNode == null)
			return null;

		BreakContinueTargetFinder finder= new BreakContinueTargetFinder();
		if (finder.initialize(astRoot, breakOrContinueNode) == null) {
			OccurrenceLocation[] locations= finder.getOccurrences();
			if (locations != null) {
				if (breakOrContinueNode instanceof BreakStatement && !labelSelected)
					return locations[locations.length - 1]; // points to the end of target statement
				return locations[0]; // points to the beginning of target statement
			}
		}
		return null;
	}

	/**
	 * Finds the target for switch-case node.
	 *
	 * @param input the editor input
	 * @param region the region
	 * @return the switch-case target location or <code>null</code> if none
	 * @since 3.14
	 */
	public static OccurrenceLocation findSwitchCaseTarget(ITypeRoot input, IRegion region) {
		CompilationUnit astRoot= SharedASTProviderCore.getAST(input, SharedASTProviderCore.WAIT_NO, null);
		if (astRoot == null) {
			return null;
		}

		ASTNode node= NodeFinder.perform(astRoot, region.getOffset(), region.getLength());
		if (!(node instanceof SwitchCase)) {
			return null;
		}
		SwitchCase caseNode= (SwitchCase) node;

		ASTNode parent= caseNode.getParent();
		ASTNode switchNode;
		if (parent instanceof SwitchStatement
				|| parent instanceof SwitchExpression) {
			switchNode= parent;
		} else {
			return null;
		}

		String description= Messages.format(org.eclipse.jdt.internal.core.manipulation.search.SearchMessages.BreakContinueTargetFinder_occurrence_description, BasicElementLabels.getJavaElementName(ASTNodes.asString(caseNode)));
		return new OccurrenceLocation(switchNode.getStartPosition(), 6, 0, description); // '6' is the length of 'switch'
	}

	/**
	 * Finds the optional enum constructor target for ENUM_CONSTANT_DECLARATION node.
	 *
	 * @param input the editor input
	 * @param region the region
	 * @return the ENUM_CONSTANT_DECLARATION target location or <code>null</code> if none
	 * @since 3.22
	 */
	public static OccurrenceLocation findEnumConstructorTarget(ITypeRoot input, IRegion region) {
		CompilationUnit astRoot= SharedASTProviderCore.getAST(input, SharedASTProviderCore.WAIT_NO, null);
		return findEnumConstructorTarget(astRoot, region);
	}

	public static OccurrenceLocation findEnumConstructorTarget(CompilationUnit astRoot, IRegion region) {
		if (astRoot == null) {
			return null;
		}

		ASTNode node= NodeFinder.perform(astRoot, region.getOffset(), region.getLength());
		if (node == null
				|| node.getParent() == null
				|| node.getParent().getNodeType() != ASTNode.ENUM_CONSTANT_DECLARATION) {
			return null;
		}
		EnumConstantDeclaration enumNode= (EnumConstantDeclaration) node.getParent();
		EnumConstructorTargetFinder finder= new EnumConstructorTargetFinder();
		if (finder.initialize(astRoot, enumNode)) {
			OccurrenceLocation location= finder.getOccurrence();
			if (location != null) {
				return location;
			}
		}
		String description= Messages.format(SearchMessages.EnumConstructorTargetFinder_description, BasicElementLabels.getJavaElementName(ASTNodes.asString(node)));
		return new OccurrenceLocation(enumNode.getStartPosition(), node.getLength(), 0, description);
	}
}
