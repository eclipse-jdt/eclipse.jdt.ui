/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.undo.snapshot.IResourceSnapshot;
import org.eclipse.core.resources.undo.snapshot.ResourceSnapshotFactory;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

public class DeletePackageFragmentRootChange extends AbstractDeleteChange {

	private final String fHandle;
	private final IPackageFragmentRootManipulationQuery fUpdateClasspathQuery;

	public DeletePackageFragmentRootChange(IPackageFragmentRoot root, boolean isExecuteChange,
			IPackageFragmentRootManipulationQuery updateClasspathQuery) {
		Assert.isNotNull(root);
		Assert.isTrue(! root.isExternal());
		fHandle= root.getHandleIdentifier();
		fUpdateClasspathQuery= updateClasspathQuery;

		if (isExecuteChange) {
			// don't check for read-only resources since we already
			// prompt the user via a dialog to confirm deletion of
			// read only resource. The change is currently not used
			// as
			setValidationMethod(VALIDATE_NOT_DIRTY);
		} else {
			setValidationMethod(VALIDATE_NOT_DIRTY | VALIDATE_NOT_READ_ONLY);
		}
	}

	@Override
	public String getName() {
		String rootName= JavaElementLabelsCore.getElementLabel(getRoot(), JavaElementLabelsCore.ALL_DEFAULT);
		return Messages.format(RefactoringCoreMessages.DeletePackageFragmentRootChange_delete, rootName);
	}

	@Override
	public Object getModifiedElement() {
		return getRoot();
	}

	@Override
	protected IResource getModifiedResource() {
		return getRoot().getResource();
	}

	private IPackageFragmentRoot getRoot(){
		return (IPackageFragmentRoot)JavaCore.create(fHandle);
	}

	@Override
	protected Change doDelete(IProgressMonitor pm) throws CoreException {
		if (! confirmDeleteIfReferenced())
			return new NullChange();
		int resourceUpdateFlags= IResource.KEEP_HISTORY;
		int jCoreUpdateFlags= IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH | IPackageFragmentRoot.OTHER_REFERRING_PROJECTS_CLASSPATH;

		pm.beginTask("", 2); //$NON-NLS-1$
		IPackageFragmentRoot root= getRoot();
		IResource rootResource= root.getResource();
		CompositeChange result= new CompositeChange(getName());

		IResourceSnapshot<IResource> rootDescription = ResourceSnapshotFactory.fromResource(rootResource);
		HashMap<IFile, String> classpathFilesContents= new HashMap<>();
		for (IJavaProject javaProject : JavaElementUtil.getReferencingProjects(root)) {
			IFile classpathFile= javaProject.getProject().getFile(".classpath"); //$NON-NLS-1$
			if (classpathFile.exists()) {
				classpathFilesContents.put(classpathFile, getFileContents(classpathFile));
			}
		}

		root.delete(resourceUpdateFlags, jCoreUpdateFlags, SubMonitor.convert(pm, 1));

		rootDescription.recordStateFromHistory(SubMonitor.convert(pm, 1));
		for (Entry<IFile, String> entry : classpathFilesContents.entrySet()) {
			IFile file= entry.getKey();
			String contents= entry.getValue();
			//Restore time stamps? This should probably be some sort of UndoTextFileChange.
			TextFileChange classpathUndo= new TextFileChange(Messages.format(RefactoringCoreMessages.DeletePackageFragmentRootChange_restore_file, BasicElementLabels.getPathLabel(file.getFullPath(), true)), file);
			classpathUndo.setEdit(new ReplaceEdit(0, getFileLength(file), contents));
			result.add(classpathUndo);
		}
		result.add(new UndoDeleteResourceChange(rootDescription));

		pm.done();
		return result;
	}

	private static String getFileContents(IFile file) throws CoreException {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath path= file.getFullPath();
		manager.connect(path, LocationKind.IFILE, new NullProgressMonitor());
		try {
			return manager.getTextFileBuffer(path, LocationKind.IFILE).getDocument().get();
		} finally {
			manager.disconnect(path, LocationKind.IFILE, new NullProgressMonitor());
		}
	}

	private static int getFileLength(IFile file) throws CoreException {
		// Cannot use file buffers here, since they are not yet in sync at this point.
		try (InputStream contents= file.getContents();
			InputStreamReader reader= createReader(file, contents)) {
			return (int) reader.skip(Integer.MAX_VALUE);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaManipulationPlugin.getPluginId(), e.getMessage(), e));
		}
	}

	private static InputStreamReader createReader(IFile file, InputStream contents) throws CoreException {
		try {
			return new InputStreamReader(contents, file.getCharset());
		} catch (UnsupportedEncodingException e) {
			JavaManipulationPlugin.log(e);
			return new InputStreamReader(contents);
		}
	}

	private boolean confirmDeleteIfReferenced() throws JavaModelException {
		IPackageFragmentRoot root= getRoot();
		if (!root.isArchive() && !root.isExternal()) //for source folders, you don't ask, just do it
			return true;
		if (fUpdateClasspathQuery == null)
			return true;
		IJavaProject[] referencingProjects= JavaElementUtil.getReferencingProjects(getRoot());
		if (referencingProjects.length <= 1)
			return true;
		return fUpdateClasspathQuery.confirmManipulation(getRoot(), referencingProjects);
	}
}
