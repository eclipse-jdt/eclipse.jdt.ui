/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.PlatformUI;

import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Proposal for a hashed serial version id.
 * 
 * @since 3.1
 */
public final class SerialVersionHashProposal extends AbstractSerialVersionProposal {

	/** The launch configuration type */
	public static final String LAUNCH_CONFIG_TYPE= "org.eclipse.jdt.ui.serial.support"; //$NON-NLS-1$

	/** The serial support class */
	public static final String SERIAL_SUPPORT_CLASS= "org.eclipse.jdt.internal.ui.text.correction.SerialVersionComputer"; //$NON-NLS-1$

	/** The serial support jar */
	public static final String SERIAL_SUPPORT_JAR= "serialsupport.jar"; //$NON-NLS-1$

	/**
	 * Displays an appropriate error message for a specific problem.
	 * 
	 * @param message The message to display
	 */
	protected static void displayErrorMessage(final String message) {
		final Display display= PlatformUI.getWorkbench().getDisplay();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(new Runnable() {

				public final void run() {
					if (display != null && !display.isDisposed()) {
						final Shell shell= display.getActiveShell();
						if (shell != null && !shell.isDisposed())
							MessageDialog.openError(shell, CorrectionMessages.getString("SerialVersionHashProposal.dialog.error.caption"), CorrectionMessages.getFormattedString("SerialVersionHashProposal.dialog.error.message", message)); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			});
		}
	}

	/**
	 * Displays an appropriate error message for a specific problem.
	 * 
	 * @param throwable the throwable object to display
	 */
	protected static void displayErrorMessage(final Throwable throwable) {
		displayErrorMessage(throwable.getLocalizedMessage());
	}

	/**
	 * Creates a new serial version hash proposal.
	 * 
	 * @param unit the compilation unit
	 * @param node the originally selected node
	 */
	public SerialVersionHashProposal(final ICompilationUnit unit, final ASTNode node) {
		super(CorrectionMessages.getString("SerialVersionSubProcessor.createhashed.description"), unit, node); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractSerialVersionProposal#addInitializer(org.eclipse.jdt.core.dom.VariableDeclarationFragment)
	 */
	protected final void addInitializer(final VariableDeclarationFragment fragment) {
		Assert.isNotNull(fragment);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

				public final void run(final IProgressMonitor monitor) {
					Assert.isNotNull(monitor);
					fragment.setInitializer(computeDefaultExpression(monitor));
				}
			});
		} catch (InvocationTargetException exception) {
			JavaPlugin.log(exception);
		} catch (InterruptedException exception) {
			// Do nothing
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractSerialVersionProposal#addLinkedPositions(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, org.eclipse.jdt.core.dom.VariableDeclarationFragment)
	 */
	protected final void addLinkedPositions(final ASTRewrite rewrite, final VariableDeclarationFragment fragment) {
		// Do nothing
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractSerialVersionProposal#computeDefaultExpression(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected final Expression computeDefaultExpression(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		long serialVersionID= SERIAL_VALUE;
		ILaunchConfiguration configuration= null;
		try {
			monitor.beginTask(CorrectionMessages.getString("SerialVersionHashProposal.computing.id"), 4); //$NON-NLS-1$
			final IJavaProject project= getCompilationUnit().getJavaProject();
			final ILaunchConfigurationWorkingCopy copy= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(LAUNCH_CONFIG_TYPE).newInstance(null, LAUNCH_CONFIG_TYPE + System.currentTimeMillis());
			monitor.worked(1);
			copy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, SERIAL_SUPPORT_CLASS);
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, getQualifiedName());
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getElementName());
			final String[] entries= JavaRuntime.computeDefaultRuntimeClassPath(project);
			final IRuntimeClasspathEntry[] classpath= new IRuntimeClasspathEntry[entries.length + 2];
			monitor.worked(1);
			classpath[0]= JavaRuntime.newRuntimeContainerClasspathEntry(new Path(JavaRuntime.JRE_CONTAINER), IRuntimeClasspathEntry.STANDARD_CLASSES, project);
			classpath[1]= JavaRuntime.newArchiveRuntimeClasspathEntry(new Path(Platform.asLocalURL(JavaPlugin.getDefault().getBundle().getEntry(SERIAL_SUPPORT_JAR)).getFile()));
			for (int index= 2; index < classpath.length; index++)
				classpath[index]= JavaRuntime.newArchiveRuntimeClasspathEntry(new Path(entries[index - 2]));
			final List mementos= new ArrayList(classpath.length);
			IRuntimeClasspathEntry entry= null;
			for (int index= 0; index < classpath.length; index++) {
				entry= classpath[index];
				mementos.add(entry.getMemento());
			}
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, mementos);
			configuration= copy.doSave();
			monitor.worked(1);
			final ILaunchConfigurationDelegate delegate= configuration.getType().getDelegate(ILaunchManager.RUN_MODE);
			if (delegate instanceof SerialVersionLaunchConfigurationDelegate) {
				final SerialVersionLaunchConfigurationDelegate extension= (SerialVersionLaunchConfigurationDelegate) delegate;
				configuration.launch(ILaunchManager.RUN_MODE, new SubProgressMonitor(monitor, 1), true);
				monitor.worked(1);
				serialVersionID= extension.getSerialVersionID();
				final String message= extension.getErrorMessage();
				if (message != null && message.length() > 0)
					displayErrorMessage(message);
			} else
				displayErrorMessage(CorrectionMessages.getString("SerialVersionHashProposal.wrong.launch.delegate")); //$NON-NLS-1$
		} catch (CoreException exception) {
			displayErrorMessage(exception);
		} catch (IOException exception) {
			displayErrorMessage(exception);
		} finally {
			try {
				if (configuration != null)
					configuration.delete();
			} catch (CoreException exception) {
				JavaPlugin.log(exception);
			}
			monitor.done();
		}
		return getAST().newNumberLiteral(serialVersionID + LONG_SUFFIX);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public final String getAdditionalProposalInfo() {
		return CorrectionMessages.getString("SerialVersionHashProposal.message.generated.info"); //$NON-NLS-1$
	}

	/**
	 * Returns the qualified type name of the class declaration.
	 * 
	 * @return the qualified type name of the class
	 */
	protected String getQualifiedName() {
		final ASTNode parent= getDeclarationNode();
		ITypeBinding binding= null;
		if (parent instanceof TypeDeclaration) {
			final TypeDeclaration declaration= (TypeDeclaration) parent;
			binding= declaration.resolveBinding();
		} else if (parent instanceof AnonymousClassDeclaration) {
			final AnonymousClassDeclaration declaration= (AnonymousClassDeclaration) parent;
			final ClassInstanceCreation creation= (ClassInstanceCreation) declaration.getParent();
			binding= creation.resolveTypeBinding();
		}
		if (binding != null)
			return binding.getBinaryName();
		return null;
	}
}