/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.deprecation;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringSessionDescriptor;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;
import org.eclipse.jdt.internal.ui.refactoring.binary.BinaryRefactoringHistoryWizard;

/**
 * Helper class for refactorings used to resolve deprecations.
 * 
 * @since 3.2
 */
public final class DeprecationRefactorings {

	/** The script encoding */
	public static final String SCRIPT_ENCODING= "utf-8"; //$NON-NLS-1$

	/** The script folder */
	public static final String SCRIPT_FOLDER= ".deprecations"; //$NON-NLS-1$

	/** The script prefix */
	private static final String SCRIPT_PREFIX= "DEPRECATE_"; //$NON-NLS-1$

	/**
	 * Creates a refactoring script to inline the member.
	 * 
	 * @param binding
	 *            the binding representing the member
	 * @return a refactoring script, or <code>null</code>
	 */
	public static String createInlineDeprecationScript(final IBinding binding) {
		String script= null;
		RefactoringSessionDescriptor descriptor= null;
		if (binding instanceof IVariableBinding) {
			final IJavaElement element= binding.getJavaElement();
			if (element != null) {
				final InlineConstantRefactoring refactoring= new InlineConstantRefactoring((IField) element);
				if (refactoring.canEnableDeprecationResolving())
					descriptor= refactoring.createDeprecationResolution();
			}
		} else if (binding instanceof IMethodBinding) {
			final IJavaElement element= binding.getJavaElement();
			if (element != null) {
				final InlineMethodRefactoring refactoring= new InlineMethodRefactoring((IMethod) element);
				if (refactoring.canEnableDeprecationResolving())
					descriptor= refactoring.createDeprecationResolution();
			}
		}
		if (descriptor != null) {
			try {
				final ByteArrayOutputStream stream= new ByteArrayOutputStream(1024);
				RefactoringCore.getHistoryService().writeRefactoringSession(descriptor, stream, false);
				script= stream.toString(DeprecationRefactorings.SCRIPT_ENCODING);
			} catch (CoreException exception) {
				JavaPlugin.log(exception);
			} catch (UnsupportedEncodingException exception) {
				Assert.isTrue(false);
			}
		}
		return script;
	}

	/**
	 * Returns the refactoring script name associated with the variable binding.
	 * 
	 * @param binding
	 *            the variable binding
	 * @return the refactoring script name, or <code>null</code>
	 */
	private static String getFieldScriptName(final IVariableBinding binding) {
		Assert.isNotNull(binding);
		final IJavaElement element= binding.getDeclaringClass().getJavaElement();
		if (element instanceof IType) {
			final IType type= (IType) element;
			final StringBuffer buffer= new StringBuffer();
			buffer.append(SCRIPT_PREFIX);
			buffer.append(type.getFullyQualifiedName());
			buffer.append('.');
			buffer.append(binding.getName());
			buffer.append(".xml"); //$NON-NLS-1$
			return makePortable(buffer);
		}
		return null;
	}

	/**
	 * Returns the refactoring script name associated with the method binding.
	 * 
	 * @param binding
	 *            the method binding
	 * @return the refactoring script name, or <code>null</code>
	 */
	private static String getMethodScriptName(final IMethodBinding binding) {
		Assert.isNotNull(binding);
		final IJavaElement element= binding.getDeclaringClass().getJavaElement();
		if (element instanceof IType) {
			final IType type= (IType) element;
			final StringBuffer buffer= new StringBuffer();
			buffer.append(SCRIPT_PREFIX);
			buffer.append(type.getFullyQualifiedName());
			buffer.append('.');
			buffer.append(binding.getName());
			buffer.append('(');
			final ITypeBinding[] parameters= binding.getParameterTypes();
			for (int index= 0; index < parameters.length; index++) {
				if (index != 0)
					buffer.append(',');
				final IJavaElement javaElem= parameters[index].getJavaElement();
				if (javaElem instanceof IType)
					buffer.append(((IType) javaElem).getFullyQualifiedName());
				else if (javaElem instanceof ITypeParameter)
					buffer.append(((ITypeParameter) javaElem).getElementName());
				else
					buffer.append(parameters[index].getQualifiedName());
			}
			buffer.append(')');
			buffer.append(".xml"); //$NON-NLS-1$
			return makePortable(buffer);
		}
		return null;
	}

	/**
	 * Returns the binary package fragment root associated with the binding.
	 * 
	 * @param binding
	 *            the binding
	 * @return the binary package fragment root, or <code>null</code> if the
	 *         binding does not come from an archive
	 */
	public static IPackageFragmentRoot getPackageFragmentRoot(final IBinding binding) {
		IPackageFragmentRoot root= null;
		final IJavaElement element= binding.getJavaElement();
		if (element != null) {
			root= (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null && !root.isArchive())
				root= null;
		}
		return root;
	}

	/**
	 * Returns a refactoring history suitable to fix deprecated references to
	 * the specified member.
	 * 
	 * @param binding
	 *            the binding associated with the member
	 * @return a refactoring history, or <code>null</code>
	 */
	public static RefactoringHistory getRefactoringHistory(final IBinding binding) {
		RefactoringHistory history= null;
		final String name= getRefactoringScriptName(binding);
		if (name != null) {
			final IPackageFragmentRoot root= getPackageFragmentRoot(binding);
			if (root != null)
				history= getRefactoringHistory(root, name);
			if (history == null) {
				IFile file= null;
				final IJavaElement element= binding.getJavaElement();
				if (element != null) {
					final IJavaProject project= element.getJavaProject();
					if (project != null)
						file= project.getProject().getFolder(DeprecationRefactorings.SCRIPT_FOLDER).getFile(name);
				}
				if (file != null && file.exists())
					history= getRefactoringHistory(file);
			}
		}
		return history;
	}

	/**
	 * Retrieves a refactoring history from the specified file.
	 * 
	 * @param file
	 *            the file
	 * @return the refactoring history, or <code>null</code>
	 */
	public static RefactoringHistory getRefactoringHistory(final IFile file) {
		Assert.isNotNull(file);
		InputStream stream= null;
		try {
			stream= new BufferedInputStream(file.getContents(true));
			return RefactoringCore.getHistoryService().readRefactoringHistory(stream, JavaRefactoringDescriptor.DEPRECATION_RESOLVING);
		} catch (CoreException exception) {
			JavaPlugin.log(exception);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException exception) {
					// Do nothing
				}
			}
		}
		return null;
	}

	/**
	 * Retrieves a refactoring history with the given name from the specified
	 * binary package fragment root.
	 * 
	 * @param root
	 *            the binary package fragment root
	 * @param name
	 *            the name of the history
	 * @return the refactoring history, or <code>null</code>
	 */
	private static RefactoringHistory getRefactoringHistory(final IPackageFragmentRoot root, final String name) {
		Assert.isNotNull(root);
		Assert.isNotNull(name);
		try {
			final URI uri= BinaryRefactoringHistoryWizard.getLocationURI(root.getRawClasspathEntry());
			if (uri != null) {
				final File file= new File(uri);
				if (file.exists()) {
					ZipFile zip= null;
					try {
						zip= new ZipFile(file, ZipFile.OPEN_READ);
						ZipEntry entry= zip.getEntry(JarPackagerUtil.getDeprecationEntry(name));
						if (entry != null) {
							InputStream stream= null;
							try {
								stream= zip.getInputStream(entry);
								return RefactoringCore.getHistoryService().readRefactoringHistory(stream, JavaRefactoringDescriptor.DEPRECATION_RESOLVING);
							} catch (CoreException exception) {
								JavaPlugin.log(exception);
							} finally {
								if (stream != null) {
									try {
										stream.close();
									} catch (IOException exception) {
										// Do nothing
									}
								}
							}
						}
					} catch (IOException exception) {
						// Just leave it
					}
				}
			}
		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
		}
		return null;
	}

	/**
	 * Returns the refactoring script file for the given project and name.
	 * 
	 * @param project
	 *            the project
	 * @param name
	 *            the name
	 * @return the script file
	 */
	public static IFile getRefactoringScriptFile(final IJavaProject project, final String name) {
		return project.getProject().getFolder(SCRIPT_FOLDER).getFile(name);
	}

	/**
	 * Returns the name of the refactoring script associated with the member.
	 * 
	 * @param binding
	 *            the binding representing the member
	 * @return the refactoring script name
	 */
	public static String getRefactoringScriptName(final IBinding binding) {
		String fileName= null;
		if (binding instanceof IVariableBinding)
			fileName= getFieldScriptName((IVariableBinding) binding);
		else if (binding instanceof IMethodBinding) {
			fileName= getMethodScriptName((IMethodBinding) binding);
		}
		return fileName;
	}

	/**
	 * Makes the file name in the string buffer portable on all file systems.
	 * 
	 * @param buffer
	 *            the buffer
	 * @return the portable file name
	 */
	private static String makePortable(final StringBuffer buffer) {
		for (int index= 0; index < buffer.length(); index++) {
			final char character= buffer.charAt(index);
			switch (character) {
				case ',':
				case '<':
				case '>':
				case ':':
				case '/':
				case '\\':
				case '|':
					buffer.setCharAt(index, '_');
			}
		}
		return buffer.toString();
	}

	/**
	 * Creates a new deprecation refactorings.
	 */
	private DeprecationRefactorings() {
		// Not for instantiation
	}
}
