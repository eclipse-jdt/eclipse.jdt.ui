/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.unittest.junit.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.unittest.junit.JUnitTestPlugin;

/**
 * Abstract Action for opening a Java editor.
 */
public abstract class OpenEditorAction extends Action {
	protected final Shell shell;
	protected final ITestRunSession testSession;
	protected String fClassName;
	private final boolean fActivate;

	protected OpenEditorAction(Shell shell, String testClassName, ITestRunSession session) {
		this(shell, testClassName, true, session);
	}

	public OpenEditorAction(Shell shell, String className, boolean activate, ITestRunSession session) {
		super(JUnitMessages.OpenEditorAction_action_label);
		this.fClassName = className;
		this.shell = shell;
		this.fActivate = activate;
		this.testSession = session;
	}

	@Override
	public void run() {
		IEditorPart editor = null;
		try {
			IJavaElement element = findElement(
					JUnitLaunchConfigurationConstants.getJavaProject(testSession.getLaunch().getLaunchConfiguration()),
					fClassName);
			if (element == null) {
				MessageDialog.openError(getShell(), JUnitMessages.OpenEditorAction_error_cannotopen_title,
						JUnitMessages.OpenEditorAction_error_cannotopen_message);
				return;
			}
			editor = JavaUI.openInEditor(element, fActivate, false);
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), JUnitMessages.OpenEditorAction_error_dialog_title,
					JUnitMessages.OpenEditorAction_error_dialog_message, e.getStatus());
			return;
		}
		if (!(editor instanceof ITextEditor)) {
			MessageDialog.openError(getShell(), JUnitMessages.OpenEditorAction_error_dialog_title,
					JUnitMessages.OpenEditorAction_error_dialog_message);
			return;
		}
		reveal((ITextEditor) editor);
	}

	protected Shell getShell() {
		return shell;
	}

	protected String getClassName() {
		return fClassName;
	}

	protected abstract IJavaElement findElement(IJavaProject project, String className) throws CoreException;

	protected abstract void reveal(ITextEditor editor);

	protected final IType findType(final IJavaProject project, String className) {
		final IType[] result = { null };
		final String dottedName = className.replace('$', '.'); // for nested classes...
		if (project == null) {
			return null;
		}
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				try {
					result[0] = internalFindType(project, dottedName, new HashSet<IJavaProject>(), monitor);
					if (result[0] == null) {
						int lastDot = dottedName.lastIndexOf('.');
						TypeNameMatchRequestor nameMatchRequestor = new TypeNameMatchRequestor() {
							@Override
							public void acceptTypeNameMatch(TypeNameMatch match) {
								result[0] = match.getType();
							}
						};
						new SearchEngine().searchAllTypeNames(
								lastDot >= 0 ? dottedName.substring(0, lastDot).toCharArray() : null,
								SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE,
								(lastDot >= 0 ? dottedName.substring(lastDot + 1) : dottedName).toCharArray(),
								SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, IJavaSearchConstants.TYPE,
								SearchEngine.createWorkspaceScope(), nameMatchRequestor,
								IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
					}
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			JUnitTestPlugin.log(e);
		} catch (InterruptedException e) {
			// user cancelled
		}
		return result[0];
	}

	private IType internalFindType(IJavaProject project, String className, Set<IJavaProject> visitedProjects,
			IProgressMonitor monitor) throws JavaModelException {
		try {
			if (visitedProjects.contains(project))
				return null;
			monitor.beginTask("", 2); //$NON-NLS-1$
			IType type = project.findType(className, SubMonitor.convert(monitor, 1));
			if (type != null)
				return type;
			// fix for bug 87492: visit required projects explicitly to also find not
			// exported types
			visitedProjects.add(project);
			IJavaModel javaModel = project.getJavaModel();
			String[] requiredProjectNames = project.getRequiredProjectNames();
			IProgressMonitor reqMonitor = SubMonitor.convert(monitor, 1);
			reqMonitor.beginTask("", requiredProjectNames.length); //$NON-NLS-1$
			for (String requiredProjectName : requiredProjectNames) {
				IJavaProject requiredProject = javaModel.getJavaProject(requiredProjectName);
				if (requiredProject.exists()) {
					type = internalFindType(requiredProject, className, visitedProjects,
							SubMonitor.convert(reqMonitor, 1));
					if (type != null)
						return type;
				}
			}
			return null;
		} finally {
			monitor.done();
		}
	}

}
