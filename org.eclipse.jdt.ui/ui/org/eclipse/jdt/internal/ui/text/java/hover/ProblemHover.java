/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.Annotation;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.preferences.JavadocProblemsPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.ProblemSeveritiesPreferencePage;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * This annotation hover shows the description of the
 * selected java annotation.
 *
 * XXX: Currently this problem hover only works for
 *		Java and spelling problems.
 *		see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=62081
 *
 * @since 3.0
 */
public class ProblemHover extends AbstractAnnotationHover {
	
	/**
	 * Action to configure the problem severity of a compiler option.
	 * 
	 * @since 3.4
	 */
	private static final class ConfigureProblemSeverityAction extends Action {

		private final IJavaProject fProject;
		private final String fOptionId;
		private final boolean fIsJavadocOption;

		public ConfigureProblemSeverityAction(IJavaProject project, String optionId, boolean isJavadocOption) {
			super();
			fProject= project;
			fOptionId= optionId;
			fIsJavadocOption= isJavadocOption;
			setImageDescriptor(JavaPluginImages.DESC_ELCL_CONFIGURE_PROBLEM_SEVERITIES);
			setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CONFIGURE_PROBLEM_SEVERITIES);
			setToolTipText(JavaHoverMessages.ProblemHover_action_configureProblemSeverity);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			if (fIsJavadocOption) {
				Map data= new HashMap();
				data.put(JavadocProblemsPreferencePage.DATA_SELECT_OPTION_KEY, fOptionId);
				data.put(JavadocProblemsPreferencePage.DATA_SELECT_OPTION_QUALIFIER, JavaCore.PLUGIN_ID);

				PreferencesUtil.createPropertyDialogOn(shell, fProject, JavadocProblemsPreferencePage.PROP_ID, null, data).open();
			} else {
				Map data= new HashMap();
				data.put(ProblemSeveritiesPreferencePage.DATA_SELECT_OPTION_KEY, fOptionId);
				data.put(ProblemSeveritiesPreferencePage.DATA_SELECT_OPTION_QUALIFIER, JavaCore.PLUGIN_ID);

				PreferencesUtil.createPropertyDialogOn(shell, fProject, ProblemSeveritiesPreferencePage.PROP_ID, null, data).open();
			}
		}
	}
	
	protected static class ProblemInfo extends AnnotationInfo {

		public ProblemInfo(Annotation annotation, Position position, ITextViewer textViewer) {
			super(annotation, position, textViewer);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover.AnnotationInfo#getCompletionProposals()
		 */
		public ICompletionProposal[] getCompletionProposals() {
			ProblemLocation location= new ProblemLocation(position.getOffset(), position.getLength(), (IJavaAnnotation) annotation);
			ICompilationUnit cu= ((IJavaAnnotation) annotation).getCompilationUnit();

			IInvocationContext context= new AssistContext(cu, location.getOffset(), location.getLength());
			if (!SpellingAnnotation.TYPE.equals(annotation.getType()) && !hasProblem(context.getASTRoot().getProblems(), location))
				return new ICompletionProposal[0];

			ArrayList proposals= new ArrayList();
			JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
			Collections.sort(proposals, new CompletionProposalComparator());

			return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
		}

		private static boolean hasProblem(IProblem[] problems, IProblemLocation location) {
			for (int i= 0; i < problems.length; i++) {
				IProblem problem= problems[i];
				if (problem.getID() == location.getProblemId() && problem.getSourceStart() == location.getOffset())
					return true;
			}
			return false;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover.AnnotationInfo#fillToolBar(org.eclipse.jface.action.ToolBarManager)
		 */
		public void fillToolBar(ToolBarManager manager) {
			super.fillToolBar(manager);

			IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;

			String optionId= JavaCore.getOptionForConfigurableSeverity(javaAnnotation.getId());
			if (optionId != null) {
				IJavaProject javaProject= javaAnnotation.getCompilationUnit().getJavaProject();
				boolean isJavadocProblem= (javaAnnotation.getId() & IProblem.Javadoc) != 0;
				ConfigureProblemSeverityAction problemSeverityAction= new ConfigureProblemSeverityAction(javaProject, optionId, isJavadocProblem);
				manager.add(problemSeverityAction);
			}
		}

	}

	public ProblemHover() {
		super(false);
	}

	protected AnnotationInfo createAnnotationInfo(Annotation annotation, Position position, ITextViewer textViewer) {
		return new ProblemInfo(annotation, position, textViewer);
	}
}
