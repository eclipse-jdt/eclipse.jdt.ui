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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;
import org.eclipse.jdt.internal.ui.refactoring.binary.BinaryRefactoringHistoryWizard;

/**
 * Helper class for refactorings used to resolve deprecations.
 * 
 * @since 3.2
 */
public final class DeprecationRefactorings {

	/** The script prefix */
	private static final String SCRIPT_PREFIX= "DEPRECATE_"; //$NON-NLS-1$

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
	public static RefactoringHistory getRefactoringHistory(final IPackageFragmentRoot root, final String name) {
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
	 * Returns the refactoring script name associated with the method binding.
	 * 
	 * @param binding
	 *            the method binding
	 * @return the refactoring script name, or <code>null</code>
	 */
	public static String getRefactoringScriptName(final IMethodBinding binding) {
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
	 * Returns the refactoring script name associated with the variable binding.
	 * 
	 * @param binding
	 *            the variable binding
	 * @return the refactoring script name, or <code>null</code>
	 */
	public static String getRefactoringScriptName(final IVariableBinding binding) {
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
