/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.buildpath;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IMarker;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

public class ClasspathVariableMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	private final static IMarkerResolution[] NO_RESOLUTION = new IMarkerResolution[0];

	/*
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		int id = marker.getAttribute(IJavaModelMarker.ID, -1);
		if (id == IJavaModelStatusConstants.DEPRECATED_VARIABLE) {
			String[] arguments= CorrectionEngine.getProblemArguments(marker);
			if (arguments == null || arguments.length == 0)
				return false;
			if (arguments[0].startsWith(JUnitPlugin.JUNIT_HOME + IPath.SEPARATOR)
					|| arguments[0].startsWith(JUnitPlugin.JUNIT_SRC_HOME + IPath.SEPARATOR))
				return true;
		}
		return false;
	}
	
	/*
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (!hasResolutions(marker)) {
			return NO_RESOLUTION;
		}
		
		String[] arguments= CorrectionEngine.getProblemArguments(marker);
		final IPath path= new Path(arguments[0]);
		final IJavaProject project= getJavaProject(marker);
		
		return new IMarkerResolution2[] {
				new IMarkerResolution2() {
					public Image getImage() {
						return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					}
		
					public String getLabel() {
						return JUnitMessages.ClasspathVariableMarkerResolutionGenerator_use_JUnit3;
					}
		
					public String getDescription() {
						return JUnitMessages.ClasspathVariableMarkerResolutionGenerator_use_JUnit3_desc;
					}
		
					public void run(IMarker nonsenseArgument) {
						IClasspathEntry[] entries;
						try {
							entries= project.getRawClasspath();
							int idx= indexOfClasspath(entries, path);
							if (idx == -1) {
								return;
							}
							entries[idx]= BuildPathSupport.getJUnit3ClasspathEntry();
							
							setClasspath(project, entries, new BusyIndicatorRunnableContext());
							
						} catch (JavaModelException e) {
							JUnitPlugin.log(e);
						}
					}
				}
		};
	}

	private IJavaProject getJavaProject(IMarker marker) {
		return JavaCore.create(marker.getResource().getProject());
	}

	private int indexOfClasspath(IClasspathEntry[] entries, IPath path) {
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry curr= entries[i];
			if (curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE && curr.getPath().equals(path)) {
				return i;
			}
		}
		return -1;
	}
	
	private static void setClasspath(final IJavaProject project, final IClasspathEntry[] entries, IRunnableContext context) throws JavaModelException {
		/*
		 * @see org.eclipse.jdt.internal.junit.ui.JUnitAddLibraryProposal#addToClasspath()
		 */
		try {
			context.run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						project.setRawClasspath(entries, monitor);
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			JUnitPlugin.log(e);
		} catch (InterruptedException e) {
			// that's OK
		}

	}
	
}
