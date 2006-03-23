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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.DeleteFileChange;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * Change to create a new deprecation fix.
 * 
 * @since 3.2
 */
public final class CreateDeprecationFixChange extends JDTChange {

	/** The label of the element to be deprecated */
	private final String fLabel;

	/** The refactoring script path */
	private final IPath fPath;

	/** The refactoring script */
	private final String fScript;

	/**
	 * Creates a new deprecation fix change.
	 * 
	 * @param path
	 *            the path of the script file
	 * @param script
	 *            the deprecation script
	 */
	public CreateDeprecationFixChange(final IPath path, final String script, final String label) {
		Assert.isNotNull(path);
		Assert.isLegal(script != null && !"".equals(script)); //$NON-NLS-1$
		Assert.isNotNull(label);
		fPath= path;
		fScript= script;
		fLabel= label;
	}

	/**
	 * Creates the script file from the specified input stream.
	 * 
	 * @param file
	 *            the script file to create
	 * @param stream
	 *            the input stream
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while creating the file
	 */
	private void createFile(final IFile file, final InputStream stream, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$
			createFolder(file.getParent(), new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			if (file.exists())
				file.delete(true, false, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			file.create(stream, false, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the container if necessary.
	 * 
	 * @param container
	 *            the container to create
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while creating the container
	 */
	private void createFolder(final IContainer container, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			if (container instanceof IFolder) {
				final IFolder folder= (IFolder) container;
				createFolder(folder.getParent(), new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				if (!folder.exists())
					folder.create(false, true, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getModifiedElement() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return Messages.format(RefactoringCoreMessages.CreateDeprecationScriptChange_name, fLabel);
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus isValid(final IProgressMonitor monitor) throws CoreException {
		final RefactoringStatus status= new RefactoringStatus();
		final IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
		final URI uri= file.getLocationURI();
		if (uri == null)
			status.addFatalError(Messages.format(RefactoringCoreMessages.CreateDeprecationScriptChange_unknown_location, file.getFullPath().toString()));
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	public Change perform(final IProgressMonitor monitor) throws CoreException {
		InputStream stream= null;
		try {
			monitor.beginTask(RefactoringCoreMessages.CreateDeprecationScriptChange_performing_change, 2);
			final IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
			try {
				stream= new ByteArrayInputStream(fScript.getBytes(DeprecationRefactorings.SCRIPT_ENCODING));
				createFile(file, stream, monitor);
				file.setCharset(DeprecationRefactorings.SCRIPT_ENCODING, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				return new DeleteFileChange(file);
			} catch (UnsupportedEncodingException exception) {
				throw new JavaModelException(exception, IJavaModelStatusConstants.IO_EXCEPTION);
			}
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException exception) {
				throw new JavaModelException(exception, IJavaModelStatusConstants.IO_EXCEPTION);
			} finally {
				monitor.done();
			}
		}
	}
}
