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

import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Proposal for a hashed serial version id.
 * 
 * @since 3.1
 */
public final class SerialVersionHashProposal extends AbstractSerialVersionProposal {

	/**
	 * Class loader that doesn't delegate to parent. Additionally findClass
	 * has to be public.
	 */
	public final class SerialVersionClassLoader extends URLClassLoader {
		public SerialVersionClassLoader(final URL[] urls) {
			super(urls, null);
		}
		public final Class findClass(final String name) throws ClassNotFoundException {
			return super.findClass(name);
		}
	}

	/** The jar file extension */
	private static final String EXTENSION_JAR= ".jar"; //$NON-NLS-1$

	/** The file url protocol */
	private static final String PROTOCOL_FILE= "file:"; //$NON-NLS-1$

	/** The serial version id */
	private long fSerialVersionId= SERIAL_VALUE;

	/**
	 * Creates a new serial version hash proposal.
	 * 
	 * @param unit
	 *        the compilation unit
	 * @param node
	 *        the originally selected node
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
			// Do nothing
		} catch (InterruptedException exception) {
			// Do nothing
		}
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public final String getAdditionalProposalInfo() {
		return CorrectionMessages.getString("SerialVersionHashProposal.message.generated.info"); //$NON-NLS-1$
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

		final List list= new ArrayList();
		final String name= computeTypeName();

		try {
			final ICompilationUnit unit= getCompilationUnit();
			final IJavaProject project= unit.getJavaProject();

			project.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

			monitor.beginTask(CorrectionMessages.getString("SerialVersionHashProposal.progress.title"), 8); //$NON-NLS-1$
			monitor.worked(1);

			addClassPathEntries(project, new HashSet(), list);

			monitor.worked(1);
			if (monitor.isCanceled())
				return null;

			final URL[] urls= new URL[list.size()];

			String path= null;
			for (int index= 0; index < list.size(); index++) {

				path= (String) list.get(index);
				if (!path.endsWith(EXTENSION_JAR))
					path+= IPath.SEPARATOR;

				urls[index]= new URL(PROTOCOL_FILE + path);
			}

			monitor.worked(1);
			if (monitor.isCanceled())
				return null;

			final SerialVersionClassLoader loader= new SerialVersionClassLoader(urls);

			monitor.worked(1);
			if (monitor.isCanceled())
				return null;

			final Class clazz= loader.findClass(name);

			monitor.worked(1);
			if (monitor.isCanceled())
				return null;

			final ObjectStreamClass stream= ObjectStreamClass.lookup(clazz);

			monitor.worked(1);
			if (monitor.isCanceled())
				return null;

			if (stream != null)
				fSerialVersionId= stream.getSerialVersionUID();

			monitor.worked(1);

		} catch (Throwable throwable) {
			monitor.done();
			displayErrorMessage(throwable);

			return null;
		} finally {
			monitor.done();
		}
		return getAST().newNumberLiteral(fSerialVersionId + LONG_SUFFIX);
	}

	/**
	 * Returns the qualified type name of the class declaration.
	 * 
	 * @return the qualified type name of the class
	 */
	private String computeTypeName() {
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

	/**
	 * Adds the class path entries of the specified project to the path entry list.
	 * 
	 * @param project
	 *        the project to add its class path entries
	 * @param projects
	 *        the set of projects whose class path entries have already been added
	 * @param paths
	 *        the path entry list to add the entries to
	 * 
	 * @throws JavaModelException
	 *         if the class path for the project could not be resolved
	 */
	private static void addClassPathEntries(final IJavaProject project, final Set projects, final List paths) throws JavaModelException {
		Assert.isNotNull(project);
		Assert.isNotNull(projects);
		Assert.isNotNull(paths);

		projects.add(project);

		final IClasspathEntry[] entries= project.getResolvedClasspath(true);
		final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();

		int kind= 0;
		IClasspathEntry entry= null;

		IPath path= null;
		IProject resource= null;
		IJavaProject reference= null;

		for (int index= 0; index < entries.length; index++) {
			entry= entries[index];
			kind= entry.getEntryKind();

			if (kind == IClasspathEntry.CPE_LIBRARY) {
				final IPath library= entry.getPath();

				final IPath location= ResourcesPlugin.getWorkspace().getRoot().getFile(library).getLocation();
				if (location != null)
					addClassPathEntry(paths, location.toString());
				else
					addClassPathEntry(paths, library.toString());
			} else if (kind == IClasspathEntry.CPE_SOURCE)
				addClassPathEntry(paths, project.getProject().getLocation().makeAbsolute().toString() + project.getOutputLocation().removeFirstSegments(1).makeAbsolute().toString());
			else if (kind == IClasspathEntry.CPE_PROJECT) {
				path= entry.getPath();

				resource= root.getProject(path.toString());
				if (resource != null && resource.exists()) {
					reference= JavaCore.create(resource);
					if (reference != null && !projects.contains(reference))
						addClassPathEntries(reference, projects, paths);
				}
			} else
				Assert.isTrue(false);
		}
	}

	/**
	 * Adds a class path entry described by the indicated path into the path entry list.
	 * 
	 * @param paths
	 *        the path entry list to add the path to
	 * @param path
	 *        the path of the class path entry
	 */
	private static void addClassPathEntry(final List paths, final String path) {
		Assert.isNotNull(paths);
		Assert.isNotNull(path);

		if (!paths.contains(path))
			paths.add(path);
	}

	/**
	 * Displays an appropriate error message if the class could not be loaded.
	 * 
	 * @param throwable
	 *        the throwable object to display
	 */
	private static void displayErrorMessage(final Throwable throwable) {
		final Display display= PlatformUI.getWorkbench().getDisplay();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public final void run() {
					if (display != null && !display.isDisposed()) {
						final Shell shell= display.getActiveShell();
						if (shell != null && !shell.isDisposed())
							MessageDialog.openError(shell, CorrectionMessages.getString("SerialVersionHashProposal.dialog.error.caption"), CorrectionMessages.getFormattedString("SerialVersionHashProposal.dialog.error.message", throwable.getLocalizedMessage())); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			});
		}
	}	
}