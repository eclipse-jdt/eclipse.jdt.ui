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
package org.eclipse.jdt.internal.ui.text.correction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.fix.AbstractSerialVersionOperation;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Proposal for a hashed serial version id.
 * 
 * @since 3.1
 */
public final class SerialVersionHashOperation extends AbstractSerialVersionOperation {

	/** The serial support jar */
	private static final String SERIAL_SUPPORT_JAR= "serialsupport.jar"; //$NON-NLS-1$

	/**
	 * Computes the class path entries which are on the user class path and are
	 * explicitely put on the boot class path.
	 * 
	 * @param project
	 *            the project to compute the classpath for
	 * @return the computed classpath. May be empty, but not null.
	 * @throws CoreException
	 *             if the project's class path cannot be computed
	 */
	public static String[] computeUserAndBootClasspath(final IJavaProject project) throws CoreException {
		final IRuntimeClasspathEntry[] unresolved= JavaRuntime.computeUnresolvedRuntimeClasspath(project);
		final List resolved= new ArrayList(unresolved.length);
		for (int index= 0; index < unresolved.length; index++) {
			final IRuntimeClasspathEntry entry= unresolved[index];
			final int property= entry.getClasspathProperty();
			if (property == IRuntimeClasspathEntry.USER_CLASSES || property == IRuntimeClasspathEntry.BOOTSTRAP_CLASSES) {
				final IRuntimeClasspathEntry[] entries= JavaRuntime.resolveRuntimeClasspathEntry(entry, project);
				for (int offset= 0; offset < entries.length; offset++) {
					final String location= entries[offset].getLocation();
					if (location != null)
						resolved.add(location);
				}
			}
		}
		return (String[]) resolved.toArray(new String[resolved.size()]);
	}
	
	public static long[] calculateSerialVersionIds(String[] qualifiedNames, IJavaProject project, final IProgressMonitor monitor) throws CoreException, IOException {
		final String[] entries= computeUserAndBootClasspath(project);
		final IRuntimeClasspathEntry[] classpath= new IRuntimeClasspathEntry[entries.length + 2];
		classpath[0]= JavaRuntime.newRuntimeContainerClasspathEntry(new Path(JavaRuntime.JRE_CONTAINER), IRuntimeClasspathEntry.STANDARD_CLASSES, project);
		classpath[1]= JavaRuntime.newArchiveRuntimeClasspathEntry(Path.fromOSString(Platform.asLocalURL(JavaPlugin.getDefault().getBundle().getEntry(SERIAL_SUPPORT_JAR)).getFile()));
		for (int index= 2; index < classpath.length; index++)
			classpath[index]= JavaRuntime.newArchiveRuntimeClasspathEntry(Path.fromOSString(entries[index - 2]));
		return SerialVersionComputationHelper.computeSerialIDs(classpath, project, qualifiedNames, monitor);
	}
	

	/**
	 * Displays an appropriate error message for a specific problem.
	 * 
	 * @param message
	 *            The message to display
	 */
	private static void displayErrorMessage(final String message) {
		final Display display= PlatformUI.getWorkbench().getDisplay();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(new Runnable() {

				public final void run() {
					if (!display.isDisposed()) {
						final Shell shell= display.getActiveShell();
						if (shell != null && !shell.isDisposed())
							MessageDialog.openError(shell, CorrectionMessages.SerialVersionHashProposal_dialog_error_caption, Messages.format(CorrectionMessages.SerialVersionHashProposal_dialog_error_message, message));
					}
				}
			});
		}
	}

	/**
	 * Displays an appropriate error message for a specific problem.
	 * 
	 * @param throwable
	 *            the throwable object to display
	 */
	private static void displayErrorMessage(final Throwable throwable) {
		displayErrorMessage(throwable.getLocalizedMessage());
	}

	/**
	 * Displays a dialog with a question as message.
	 * 
	 * @param title
	 *            The title to display
	 * @param message
	 *            The message to display
	 */
	private static boolean displayYesNoMessage(final String title, final String message) {
		final boolean[] result= { true};
		final Display display= PlatformUI.getWorkbench().getDisplay();
		if (display != null && !display.isDisposed()) {
			display.syncExec(new Runnable() {

				public final void run() {
					if (!display.isDisposed()) {
						final Shell shell= display.getActiveShell();
						if (shell != null && !shell.isDisposed())
							result[0]= MessageDialog.openQuestion(shell, title, message);
					}
				}
			});
		}
		return result[0];
	}

	private final ICompilationUnit fCompilationUnit;
	
	public SerialVersionHashOperation(ICompilationUnit unit, ASTNode[] nodes) {
		super(unit, nodes);
		fCompilationUnit= unit;
	}

	/**
	 * {@inheritDoc}
	 * @throws CoreException 
	 */
	protected boolean addInitializer(final VariableDeclarationFragment fragment, final ASTNode declarationNode) throws CoreException {
		Assert.isNotNull(fragment);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

				public final void run(final IProgressMonitor monitor) throws InterruptedException {
					Assert.isNotNull(monitor);
					String id= computeId(declarationNode, monitor);
					fragment.setInitializer(fragment.getAST().newNumberLiteral(id));
				}
			});
		} catch (InvocationTargetException exception) {
			JavaPlugin.log(exception);
		} catch (InterruptedException exception) {
			// Do nothing
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void addLinkedPositions(ASTRewrite rewrite, VariableDeclarationFragment fragment, List positionGroups) {
		//Do nothing
	}

	private String computeId(final ASTNode declarationNode, final IProgressMonitor monitor) throws InterruptedException {
		Assert.isNotNull(monitor);
		long serialVersionID= SERIAL_VALUE;
		try {
			monitor.beginTask(CorrectionMessages.SerialVersionHashProposal_computing_id, 200);
			final IJavaProject project= fCompilationUnit.getJavaProject();
			final IPath path= fCompilationUnit.getResource().getFullPath();
			try {
				FileBuffers.getTextFileBufferManager().connect(path, new SubProgressMonitor(monitor, 10));
				if (monitor.isCanceled())
					throw new InterruptedException();
				
				final ITextFileBuffer buffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path);
				if (buffer.isDirty() && buffer.isStateValidated() && buffer.isCommitable() && displayYesNoMessage(CorrectionMessages.SerialVersionHashProposal_save_caption, CorrectionMessages.SerialVersionHashProposal_save_message))
					buffer.commit(new SubProgressMonitor(monitor, 20), true);
				else
					monitor.worked(20);
				
				if (monitor.isCanceled())
					throw new InterruptedException();
			} finally {
				FileBuffers.getTextFileBufferManager().disconnect(path, new SubProgressMonitor(monitor, 10));
			}
			project.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(monitor, 60));
			if (monitor.isCanceled())
				throw new InterruptedException();
			
			long[] ids= calculateSerialVersionIds(new String[] {getQualifiedName(declarationNode)}, project, new SubProgressMonitor(monitor, 100));
			if (ids.length == 1)
				serialVersionID= ids[0];
		} catch (CoreException exception) {
			displayErrorMessage(exception);
		} catch (IOException exception) {
			displayErrorMessage(exception);
		} finally {
			monitor.done();
		}
		return serialVersionID + LONG_SUFFIX;
	}

	/**
	 * Returns the qualified type name of the class declaration.
	 * 
	 * @return the qualified type name of the class
	 */
	private String getQualifiedName(final ASTNode parent) {
		ITypeBinding binding= null;
		if (parent instanceof AbstractTypeDeclaration) {
			final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) parent;
			binding= declaration.resolveBinding();
		} else if (parent instanceof AnonymousClassDeclaration) {
			final AnonymousClassDeclaration declaration= (AnonymousClassDeclaration) parent;
			final ClassInstanceCreation creation= (ClassInstanceCreation) declaration.getParent();
			binding= creation.resolveTypeBinding();
		} else if (parent instanceof ParameterizedType) {
			final ParameterizedType type= (ParameterizedType) parent;
			binding= type.resolveBinding();
		}
		if (binding != null)
			return binding.getBinaryName();
		return null;
	}
}
