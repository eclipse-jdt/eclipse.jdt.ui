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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Proposal for a hashed serial version id.
 */
public class SerialVersionHashProposal extends SerialVersionDefaultProposal {

	/** The class file extension */
	private static final String CLASS_FILE_EXTENSION= ".class"; //$NON-NLS-1$

	/** The key separator */
	private static final char KEY_SEPARATOR= '/';

	/** The name separator */
	private static final char NAME_SEPARATOR= '.';

	/** The path separator */
	private static final char PATH_SEPARATOR= '/';

	/** The file url protocol */
	private static final String PROTOCOL_FILE= "file://"; //$NON-NLS-1$

	/** Has code been generated for the compilation unit? */
	private boolean fCodeGenerated= false;

	/** The compilation unit to operate on */
	protected final ICompilationUnit fCompilationUnit;

	/** The serial version id */
	protected long fSUID= SERIAL_VALUE;

	/**
	 * Creates a new serial version hash proposal.
	 * 
	 * @param label
	 *        the label of this proposal
	 * @param unit
	 *        the compilation unit
	 * @param node
	 *        the originally selected node
	 */
	public SerialVersionHashProposal(final String label, final ICompilationUnit unit, final ASTNode node) {
		super(label, unit, node);

		Assert.isNotNull(unit);

		fCompilationUnit= unit;

		final IPackageFragmentRoot root= (IPackageFragmentRoot) fCompilationUnit.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (root != null) {

			try {

				final IClasspathEntry entry= root.getRawClasspathEntry();
				if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {

					IPath path= entry.getOutputLocation();
					if (path == null)
						path= fCompilationUnit.getJavaProject().getOutputLocation();

					path= path.append(getClassFilePath());

					final IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(path);
					if (file != null && file.exists())
						fCodeGenerated= true;
				}

			} catch (JavaModelException exception) {
				// Do nothing
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.SerialVersionDefaultProposal#addLinkedPositions(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, org.eclipse.jdt.core.dom.VariableDeclarationFragment)
	 */
	protected void addLinkedPositions(final ASTRewrite rewrite, final VariableDeclarationFragment fragment) {
		// Do nothing
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.SerialVersionDefaultProposal#computeDefaultExpression()
	 */
	protected Expression computeDefaultExpression() {

		fSUID= SERIAL_VALUE;

		if (fCodeGenerated) {

			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

				public final void run() {

					final String name= getQualifiedName();

					try {

						final List list= new ArrayList();
						addClassPathEntries(fCompilationUnit.getJavaProject(), new HashSet(), list);

						final URL[] urls= new URL[list.size()];

						String path= null;
						for (int index= 0; index < list.size(); index++) {

							path= (String) list.get(index);
							urls[index]= new URL(PROTOCOL_FILE + path);
						}

						final ObjectStreamClass stream= ObjectStreamClass.lookup(Class.forName(name, false, new URLClassLoader(urls)));
						if (stream != null)
							fSUID= stream.getSerialVersionUID();

					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					} catch (MalformedURLException exception) {
						JavaPlugin.log(exception);
					} catch (ClassNotFoundException exception) {
						// Do nothing
					}
				}
			});
		}
		return fNode.getAST().newNumberLiteral(fSUID + LONG_SUFFIX);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {

		String result= null;
		if (fCodeGenerated)
			result= CorrectionMessages.getString("SerialVersionHashProposal.message.generated.info"); //$NON-NLS-1$
		else
			result= CorrectionMessages.getString("SerialVersionHashProposal.message.compiled.warning"); //$NON-NLS-1$

		return result;
	}

	/**
	 * Returns the class file path of the class being fixed.
	 * 
	 * @return the class file path of the class
	 */
	private String getClassFilePath() {

		final ITypeBinding binding= getTypeBinding();
		if (binding != null) {

			// TODO: Should not rely on format of ITypeBinding#getKey().

			final StringBuffer buffer= new StringBuffer(binding.getKey());
			for (int index= 0; index < buffer.length(); index++) {

				if (buffer.charAt(index) == KEY_SEPARATOR)
					buffer.setCharAt(index, PATH_SEPARATOR);
			}
			return buffer.toString() + CLASS_FILE_EXTENSION;
		}
		return null;
	}

	/**
	 * Returns the qualified name of the class being fixed.
	 * 
	 * @return the qualified name of the class
	 */
	private String getQualifiedName() {

		final ITypeBinding binding= getTypeBinding();
		if (binding != null) {

			// TODO: Should not rely on format of ITypeBinding#getKey().

			final StringBuffer buffer= new StringBuffer(binding.getKey());
			for (int index= 0; index < buffer.length(); index++) {

				if (buffer.charAt(index) == KEY_SEPARATOR)
					buffer.setCharAt(index, NAME_SEPARATOR);
			}
			return buffer.toString();
		}
		return null;
	}

	/**
	 * Returns the type binding of the class being fixed.
	 * 
	 * @return the type binding of the class
	 */
	private ITypeBinding getTypeBinding() {

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
		return binding;
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

			if (kind == IClasspathEntry.CPE_LIBRARY)
				addClassPathEntry(paths, entry.getPath().toString());
			else if (kind == IClasspathEntry.CPE_SOURCE)
				addClassPathEntry(paths, project.getProject().getLocation().makeAbsolute().toString() + project.getOutputLocation().removeFirstSegments(1).makeAbsolute().toFile().toString() + PATH_SEPARATOR);
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
}