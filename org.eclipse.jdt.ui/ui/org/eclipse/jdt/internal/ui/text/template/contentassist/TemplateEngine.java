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
 *     Nicolaj Hoess <nicohoess@gmail.com> - Make some internal methods accessible to help Postfix Code Completion plug-in - https://bugs.eclipse.org/433500
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 486899, 486903
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.template.contentassist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType;
import org.eclipse.jdt.internal.corext.template.java.SWTContextType;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class TemplateEngine {

	private static final Pattern $_LINE_SELECTION_PATTERN= Pattern.compile("\\$\\{(.*:)?" + GlobalTemplateVariables.LineSelection.NAME + "(\\(.*\\))?\\}"); //$NON-NLS-1$ //$NON-NLS-2$

	private static final Pattern $_WORD_SELECTION_PATTERN= Pattern.compile("\\$\\{(.*:)?" + GlobalTemplateVariables.WordSelection.NAME + "(\\(.*\\))?\\}"); //$NON-NLS-1$ //$NON-NLS-2$

	private static String Switch_Name = "switch"; //$NON-NLS-1$
	private static String Switch_Default = "switch case statement"; //$NON-NLS-1$

	private static String NEW_RECORD_TEMPLATE_NAME= "new_record"; //$NON-NLS-1$

	/** The context type. */
	private TemplateContextType fContextType;
	/** The result proposals. */
	private ArrayList<TemplateProposal> fProposals= new ArrayList<>();
	/** Positions created on the key documents to remove in reset. */
	private final Map<IDocument, Position> fPositions= new HashMap<>();

	/**
	 * Creates the template engine for the given <code>contextType</code>.
	 * <p>
	 * The <code>JavaPlugin.getDefault().getTemplateContextRegistry()</code>
	 * defines the supported context types.</p>
	 *
	 * @param contextType the context type
	 */
	public TemplateEngine(TemplateContextType contextType) {
		Assert.isNotNull(contextType);
		fContextType= contextType;
	}

	/**
	 * Empties the collector.
	 */
	public void reset() {
		fProposals.clear();
		for (Entry<IDocument, Position> entry : fPositions.entrySet()) {
			IDocument doc= entry.getKey();
			Position position= entry.getValue();
			doc.removePosition(position);
		}
		fPositions.clear();
	}

	/**
	 * Returns the array of matching templates.
	 *
	 * @return the template proposals
	 */
	public TemplateProposal[] getResults() {
		return fProposals.toArray(new TemplateProposal[fProposals.size()]);
	}

	/**
	 * Inspects the context of the compilation unit around <code>completionPosition</code>
	 * and feeds the collector with proposals.
	 * @param viewer the text viewer
	 * @param completionPosition the context position in the document of the text viewer
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 *
	 * @deprecated Use {@link #complete(ITextViewer, Point, int, ICompilationUnit)} instead.
	 */
	@Deprecated
	public void complete(ITextViewer viewer, int completionPosition, ICompilationUnit compilationUnit) {
		if (!(fContextType instanceof CompilationUnitContextType)) {
			return;
		}
		complete(viewer, viewer.getSelectedRange(), completionPosition, compilationUnit);
	}

	/**
	 * Inspects the context of the compilation unit around <code>completionPosition</code>
	 * and feeds the collector with proposals.
	 * @param viewer the text viewer
	 * @param selectedRange the selected range
	 * @param completionPosition the context position in the document of the text viewer
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 */
	public void complete(ITextViewer viewer, Point selectedRange, int completionPosition, ICompilationUnit compilationUnit) {
	    IDocument document= viewer.getDocument();

		if (!(fContextType instanceof CompilationUnitContextType)) {
			return;
		}

		Position position= new Position(completionPosition, selectedRange.y);

		// remember selected text
		String selectedText= null;
		if (selectedRange.y != 0) {
			try {
				selectedText= document.get(selectedRange.x, selectedRange.y);
				document.addPosition(position);
				fPositions.put(document, position);
			} catch (BadLocationException e) {}
		}

		CompilationUnitContext context= (CompilationUnitContext) ((CompilationUnitContextType) fContextType).createContext(document, position, compilationUnit);
		context.setVariable("selection", selectedText); //$NON-NLS-1$
		int start= context.getStart();
		int end= context.getEnd();
		IRegion region= new Region(start, end - start);

		Template[] templates= JavaPlugin.getDefault().getTemplateStore().getTemplates();
		boolean needsCheck= !isJava12OrHigherProject(compilationUnit);
		if (selectedRange.y == 0) {
			for (Template template : templates) {
				if (canEvaluate(context, template, needsCheck)) {
					fProposals.add(new TemplateProposal(template, context, region, getImage()));
				}
			}
		} else {

			if (context.getKey().length() == 0) {
				context.setForceEvaluation(true);
			}

			boolean multipleLinesSelected= areMultipleLinesSelected(document, selectedRange);

			for (Template template : templates) {
				if (canEvaluate(context, template, needsCheck))
				{
					Matcher wordSelectionMatcher= $_WORD_SELECTION_PATTERN.matcher(template.getPattern());
					Matcher lineSelectionMatcher= $_LINE_SELECTION_PATTERN.matcher(template.getPattern());
					if ((!multipleLinesSelected && wordSelectionMatcher.find()) || (multipleLinesSelected && lineSelectionMatcher.find())) {
						fProposals.add(new TemplateProposal(template, context, region, getImage()));
					}
				}
			}
		}
	}

	protected TemplateContextType getContextType() {
		return fContextType;
	}

	protected ArrayList<TemplateProposal> getProposals() {
		return fProposals;
	}

	protected Image getImage() {
		if (fContextType instanceof SWTContextType) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SWT_TEMPLATE);
		} else {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE);
		}
	}

	private boolean isJava12OrHigherProject(ICompilationUnit compUnit) {
		if (compUnit != null) {
			IJavaProject javaProject= compUnit.getJavaProject();
			return JavaModelUtil.is12OrHigher(javaProject);
		}
		return false;
	}

	private boolean isTemplateAllowed(Template template, CompilationUnitContext context) {
		if (Switch_Name.equals(template.getName())) {
			if (Switch_Default.equals(template.getDescription())) {
				return true;
			}
			return false;
		}
		if (NEW_RECORD_TEMPLATE_NAME.equals(template.getName()) && JavaModelUtil.is16OrHigher(context.getJavaProject())) {
			return true;
		}
		return true;
	}

	private boolean canEvaluate(CompilationUnitContext context, Template template, boolean needsCheck) {
		if (!needsCheck) {
			return context.canEvaluate(template);
		}
		if (isTemplateAllowed(template, context)) {
			return context.canEvaluate(template);
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if one line is completely selected or if multiple lines are selected.
	 * Being completely selected means that all characters except the new line characters are
	 * selected.
	 *
	 * @param document the document
	 * @param selectedRange the range
	 * @return <code>true</code> if one or multiple lines are selected
	 */
	private boolean areMultipleLinesSelected(IDocument document, Point selectedRange) {
		if (document == null || selectedRange == null) {
			return false;
		}

		if (selectedRange.y == 0) {
			return false;
		}

		try {
			int startLine= document.getLineOfOffset(selectedRange.x);
			int endLine= document.getLineOfOffset(selectedRange.x + selectedRange.y);
			IRegion line= document.getLineInformation(startLine);
			return startLine != endLine || (selectedRange.x == line.getOffset() && selectedRange.y == line.getLength());

		} catch (BadLocationException x) {
			return false;
		}
	}
}
