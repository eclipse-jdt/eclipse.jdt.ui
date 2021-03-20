/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.Collator;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaDocContextType;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.text.template.contentassist.SurroundWithTemplateProposal;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;


/**
 * Quick template processor.
 */
public class QuickTemplateProcessor implements IQuickAssistProcessor {

	private static final Pattern $_LINE_SELECTION_PATTERN= Pattern.compile("\\$\\{(.*:)?" + GlobalTemplateVariables.LineSelection.NAME + "(\\(.*\\))?\\}"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final Pattern $_WORD_SELECTION_PATTERN= Pattern.compile("\\$\\{(.*:)?" + GlobalTemplateVariables.WordSelection.NAME + "(\\(.*\\))?\\}"); //$NON-NLS-1$ //$NON-NLS-2$

	public QuickTemplateProcessor() {
	}

	@Override
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		IDocument document= getDocument(cu);

		int offset= context.getSelectionOffset();
		int length= context.getSelectionLength();
		if (length == 0) {
			return false;
		}

		try {
			int startLine= document.getLineOfOffset(offset);
			int endLine= document.getLineOfOffset(offset + length);
			IRegion region= document.getLineInformation(endLine);
			return startLine  < endLine || length > 0 && offset == region.getOffset() && length == region.getLength();
		} catch (BadLocationException e) {
			return false;
		}
	}

	@Override
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		if (locations != null && locations.length > 0) {
			return new IJavaCompletionProposal[0];
		}

		try {
			int offset= context.getSelectionOffset();
			int length= context.getSelectionLength();
			if (length == 0) {
				return null;
			}

			ICompilationUnit cu= context.getCompilationUnit();
			IDocument document= getDocument(cu);

			String contentType= TextUtilities.getContentType(document, IJavaPartitions.JAVA_PARTITIONING, offset, true);

			String contextId;
			if (IJavaPartitions.JAVA_DOC.equals(contentType)) {
				contextId= JavaDocContextType.ID;
			} else {
				// TODO: Should compute location: member/statement
				contextId= JavaContextType.ID_ALL;
			}

			// test if selection is either a full line or spans over multiple lines
			int startLine= document.getLineOfOffset(offset);
			int endLine= document.getLineOfOffset(offset + length);

			if (JavaContextType.ID_ALL.equals(contextId)) {
				IRegion endLineRegion= document.getLineInformation(endLine);
				//if end position is at start of line, set it back to the previous line's end
				if (endLine > startLine && endLineRegion.getOffset() == offset + length) {
					endLine--;
					endLineRegion= document.getLineInformation(endLine);
					length= endLineRegion.getOffset() + endLineRegion.getLength() - offset;
				}
				if (startLine == endLine) {
					if (length == 0 || offset != endLineRegion.getOffset() || length != endLineRegion.getLength()) {
						AssistContext invocationContext= new AssistContext(cu, offset, length);
						if (!SurroundWith.isApplicable(invocationContext))
							return null;
					}
				} else {
					// expand selection
					offset= document.getLineOffset(startLine);
					length= endLineRegion.getOffset() + endLineRegion.getLength() - offset;
				}
			}

			ArrayList<IJavaCompletionProposal> resultingCollections= new ArrayList<>();
			collectSurroundTemplates(document, cu, offset, length, resultingCollections, contextId);
			sort(resultingCollections);
			return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	private void sort(ArrayList<IJavaCompletionProposal> proposals) {
		Collections.sort(proposals, (p1, p2) -> Collator.getInstance().compare(p1.getDisplayString(), p2.getDisplayString()));
	}

	private IDocument getDocument(ICompilationUnit cu) throws JavaModelException {
		IFile file= (IFile) cu.getResource();
		IDocument document= JavaUI.getDocumentProvider().getDocument(new FileEditorInput(file));
		if (document == null) {
			return new Document(cu.getSource()); // only used by test cases
		}
		return document;
	}

	private void collectSurroundTemplates(IDocument document, ICompilationUnit cu, int offset, int length, Collection<IJavaCompletionProposal> result, String contextId) throws BadLocationException, CoreException {
		CompilationUnitContextType contextType= (CompilationUnitContextType) JavaPlugin.getDefault().getTemplateContextRegistry().getContextType(contextId);
		CompilationUnitContext context= (CompilationUnitContext) contextType.createContext(document, offset, length, cu);
		context.setVariable("selection", document.get(offset, length)); //$NON-NLS-1$
		context.setForceEvaluation(true);

		int start= context.getStart();
		int end= context.getEnd();
		IRegion region= new Region(start, end - start);

		AssistContext invocationContext= new AssistContext(cu, start, end - start);
		ASTNode[] selectedNodes= SurroundWith.getValidSelectedNodes(invocationContext);

		Template[] templates= JavaPlugin.getDefault().getTemplateStore().getTemplates();
		for (Template currentTemplate : templates) {
			if (canEvaluate(context, currentTemplate)) {

				if (selectedNodes != null) {
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					TemplateProposal proposal= new SurroundWithTemplateProposal(cu, currentTemplate, context, region, image, selectedNodes);
					String[] arg= new String[] { currentTemplate.getName(), currentTemplate.getDescription() };
					String decorated= Messages.format(CorrectionMessages.QuickTemplateProcessor_surround_label, arg);
					proposal.setDisplayString(StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, new StyledString(currentTemplate.getName())));
					result.add(proposal);
				} else {
					TemplateProposal proposal= new TemplateProposal(currentTemplate, context, region, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)) {
						@Override
						public boolean validate(IDocument doc, int off, DocumentEvent event) {
							return false;
						}
					};
					String[] arg= new String[] { currentTemplate.getName(), currentTemplate.getDescription() };
					String decorated= Messages.format(CorrectionMessages.QuickTemplateProcessor_surround_label, arg);
					proposal.setDisplayString(StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, new StyledString(currentTemplate.getName())));
					result.add(proposal);
				}
			}
		}
	}

	private boolean canEvaluate(TemplateContext context, Template template) {
		String contextId= context.getContextType().getId();
		Matcher wordSelectionMatcher= $_WORD_SELECTION_PATTERN.matcher(template.getPattern());
		Matcher lineSelectionMatcher= $_LINE_SELECTION_PATTERN.matcher(template.getPattern());
		if (JavaDocContextType.ID.equals(contextId)) {
			if (!template.matches("", contextId) || !lineSelectionMatcher.find() && !wordSelectionMatcher.find()) //$NON-NLS-1$
				return false;
		} else {
			if (template.matches("", JavaDocContextType.ID) || !lineSelectionMatcher.find() //$NON-NLS-1$
					|| template.matches("", JavaContextType.ID_EMPTY)) //$NON-NLS-1$
				return false;
		}
		TemplateContextType contextType= JavaPlugin.getDefault().getTemplateContextRegistry().getContextType(template.getContextTypeId());
		return contextType instanceof CompilationUnitContextType;
	}

}
