/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.ui.examples;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

/**
 *
 */
public class JavaElementLightweightDecorator extends LabelProvider implements ILightweightLabelDecorator {

	private class FileBufferListener implements IFileBufferListener {
		@Override
		public void bufferCreated(IFileBuffer buffer) {
			if (buffer.getLocation() != null)
				update(buffer.getLocation());
		}

		@Override
		public void bufferDisposed(IFileBuffer buffer) {
			if (buffer.getLocation() != null)
				update(buffer.getLocation());
		}

		@Override
		public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
		}

		@Override
		public void bufferContentReplaced(IFileBuffer buffer) {
		}

		@Override
		public void stateChanging(IFileBuffer buffer) {
		}

		@Override
		public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {
		}

		@Override
		public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
		}

		@Override
		public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
		}

		@Override
		public void underlyingFileDeleted(IFileBuffer buffer) {
		}

		@Override
		public void stateChangeFailed(IFileBuffer buffer) {}
	}

	private Font fBold;
	private FileBufferListener fListener;

	private UIJob fNotifierJob;

	private Set<IFile> fChangedResources;

	public JavaElementLightweightDecorator() {
		final FontRegistry fontRegistry= PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
		Display.getDefault().syncExec(() -> {
			fBold= fontRegistry.getBold(JFaceResources.DEFAULT_FONT);
		});
		fListener= new FileBufferListener();
		FileBuffers.getTextFileBufferManager().addFileBufferListener(fListener);

		fChangedResources= new HashSet<>();
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		IPath path= null;

		if (element instanceof IResource) {
			path= ((IResource) element).getFullPath();
		} else if (element instanceof IClassFile || element instanceof ICompilationUnit) {
			path= ((IJavaElement) element).getPath();
		}
		if (path != null) {
			if (FileBuffers.getTextFileBufferManager().getFileBuffer(path, LocationKind.NORMALIZE) != null) {
				decoration.setFont(fBold);
			}
		}
	}

	private void update(IPath location) {
		IFile file= FileBuffers.getWorkspaceFileAtLocation(location);
		if (file != null) {
			boolean hasChanges= false;
			synchronized (this) {
				hasChanges= fChangedResources.add(file);
			}
			if (hasChanges) {
				if (fNotifierJob == null) {
					fNotifierJob= new UIJob(Display.getDefault(), "Update Java test decorations") {
						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							runPendingUpdates();
							return Status.OK_STATUS;
						}
					};
					fNotifierJob.setSystem(true);
				}
				fNotifierJob.schedule();
			}
		}
	}

	private void runPendingUpdates() {
		Object[] resourceToUpdate= null;
		synchronized (this) {
			resourceToUpdate= fChangedResources.toArray();
			fChangedResources.clear();
		}
		if (resourceToUpdate.length > 0) {
			LabelProviderChangedEvent event= new LabelProviderChangedEvent(this, resourceToUpdate);
			fireLabelProviderChanged(event);
		}
	}

	@Override
	public void dispose() {
		FileBuffers.getTextFileBufferManager().removeFileBufferListener(fListener);
	}

}
