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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IMarker;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.PreferencePageSupport;
import org.eclipse.jdt.internal.ui.preferences.UserLibraryPreferencePage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;

public class UserLibraryMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {
	
	private final static IMarkerResolution[] NO_RESOLUTION = new IMarkerResolution[0];

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		int id = marker.getAttribute(IJavaModelMarker.ID, -1);
		if (id == IJavaModelStatusConstants.CP_CONTAINER_PATH_UNBOUND) {
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		final Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (!hasResolutions(marker) || shell == null) {
			return NO_RESOLUTION;
		}
		
		
		ArrayList resolutions= new ArrayList();
		
		String[] arguments= CorrectionEngine.getProblemArguments(marker);
		final IPath path= new Path(arguments[0]);
		final IJavaProject project= getJavaProject(marker);
		
		if (path.segment(0).equals(JavaCore.USER_LIBRARY_CONTAINER_ID)) {
			String label= NewWizardMessages.getString("UserLibraryMarkerResolutionGenerator.changetouserlib.label"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME);
			resolutions.add(new UserLibraryMarkerResolution(label, image) {
				public void run(IMarker m) {
					changeToExistingLibrary(shell, path, false, project);
				}
			});
			if (path.segmentCount() == 2) {
				String label2= NewWizardMessages.getFormattedString("UserLibraryMarkerResolutionGenerator.createuserlib.label", path.segment(1)); //$NON-NLS-1$
				Image image2= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
				resolutions.add(new UserLibraryMarkerResolution(label2, image2) {
					public void run(IMarker m) {
						createUserLibrary(shell, path, project);
					}
				});
			}
		}
		String label= NewWizardMessages.getString("UserLibraryMarkerResolutionGenerator.changetoother"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME);
		resolutions.add(new UserLibraryMarkerResolution(label, image) {
			public void run(IMarker m) {
				changeToExistingLibrary(shell, path, true, project);
			}
		});
		return (IMarkerResolution[]) resolutions.toArray(new IMarkerResolution[resolutions.size()]);
	}

	protected void changeToExistingLibrary(Shell shell, IPath path, boolean isNew, final IJavaProject project) {
		try {
			IClasspathEntry[] entries= project.getRawClasspath();
			int idx= indexOfClasspath(entries, path);
			if (idx == -1) {
				return;
			}
			IClasspathEntry[] res;
			if (isNew) {
				res= BuildPathDialogAccess.chooseContainerEntries(shell, project, entries);
				if (res == null) {
					return;
				}
			} else {
				IClasspathEntry resEntry= BuildPathDialogAccess.configureContainerEntry(shell, entries[idx], project, entries);
				if (resEntry == null) {
					return;
				}
				res= new IClasspathEntry[] { resEntry };
			}
			final IClasspathEntry[] newEntries= new IClasspathEntry[entries.length - 1 + res.length];
			System.arraycopy(entries, 0, newEntries, 0, idx);
			System.arraycopy(res, 0, newEntries, idx, res.length);
			System.arraycopy(entries, idx + 1, newEntries, idx + res.length, entries.length - idx - 1);
			
			IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
			if (context == null) {
				context= PlatformUI.getWorkbench().getProgressService();
			}
			context.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						project.setRawClasspath(newEntries, project.getOutputLocation(), monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (JavaModelException e) {
			String title= NewWizardMessages.getString("UserLibraryMarkerResolutionGenerator.error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("UserLibraryMarkerResolutionGenerator.error.creationfailed.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.getString("UserLibraryMarkerResolutionGenerator.error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("UserLibraryMarkerResolutionGenerator.error.applyingfailed.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InterruptedException e) {
			// user cancelled
		}
	}
	
	private int indexOfClasspath(IClasspathEntry[] entries, IPath path) {
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry curr= entries[i];
			if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER && curr.getPath().equals(path)) {
				return i;
			}
		}
		return -1;
	}
	
	protected void createUserLibrary(final Shell shell, IPath unboundPath, IJavaProject project) {
		final String name= unboundPath.segment(1);
		UserLibraryPreferencePage page= new UserLibraryPreferencePage(name, true);
		PreferencePageSupport.showPreferencePage(shell, UserLibraryPreferencePage.ID, page);
	}

	private IJavaProject getJavaProject(IMarker marker) {
		return JavaCore.create(marker.getResource().getProject());
	}

	/**
	 * Library quick fix base class
	 */
	private static abstract class UserLibraryMarkerResolution implements IMarkerResolution, IMarkerResolution2 {
		
		private String fLabel;
		private Image fImage;
		
		public UserLibraryMarkerResolution(String label, Image image) {
			fLabel= label;
			fImage= image;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution#getLabel()
		 */
		public String getLabel() {
			return fLabel;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
		 */
		public String getDescription() {
			return null;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution2#getImage()
		 */
		public Image getImage() {
			return fImage;
		}
	}


	

}
