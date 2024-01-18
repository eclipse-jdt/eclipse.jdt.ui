/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;


public class JavaMarkerAnnotation extends MarkerAnnotation implements IJavaAnnotation {

	public static final String ERROR_ANNOTATION_TYPE= ProblemLocation.ERROR_ANNOTATION_TYPE;
	public static final String WARNING_ANNOTATION_TYPE= "org.eclipse.jdt.ui.warning"; //$NON-NLS-1$
	public static final String INFO_ANNOTATION_TYPE= "org.eclipse.jdt.ui.info"; //$NON-NLS-1$
	public static final String TASK_ANNOTATION_TYPE= "org.eclipse.ui.workbench.texteditor.task"; //$NON-NLS-1$


	/**
	 * Tells whether the given marker can be treated as a Java annotation
	 * which will later be update by JDT Core problems.
	 *
	 * @param marker the marker
	 * @return <code>true</code> if the marker can be treated as a Java annotation
	 * @since 3.3.2
	 */
	static final boolean isJavaAnnotation(IMarker marker) {
		// Performance
		String markerType= MarkerUtilities.getMarkerType(marker);
		if (IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER.equals(markerType) ||
				IJavaModelMarker.TASK_MARKER.equals(markerType) ||
				IJavaModelMarker.TRANSIENT_PROBLEM.equals(markerType) ||
			IJavaModelMarker.BUILDPATH_PROBLEM_MARKER.equals(markerType))
			return true;


		return MarkerUtilities.isMarkerType(marker, IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
	}

	private IJavaAnnotation fOverlay;


	public JavaMarkerAnnotation(IMarker marker) {
		super(marker);
	}

	/*
	 * @see IJavaAnnotation#getArguments()
	 */
	@Override
	public String[] getArguments() {
		IMarker marker= getMarker();
		if (marker != null && marker.exists() && isProblem())
			return CorrectionEngine.getProblemArguments(marker);
		return null;
	}

	/*
	 * @see IJavaAnnotation#getId()
	 */
	@Override
	public int getId() {
		IMarker marker= getMarker();
		if (marker == null  || !marker.exists())
			return -1;

		if (isProblem())
			return marker.getAttribute(IJavaModelMarker.ID, -1);

//		if (TASK_ANNOTATION_TYPE.equals(getAnnotationType())) {
//			try {
//				if (marker.isSubtypeOf(IJavaModelMarker.TASK_MARKER)) {
//					return IProblem.Task;
//				}
//			} catch (CoreException e) {
//				JavaPlugin.log(e); // should no happen, we test for marker.exists
//			}
//		}

		return -1;
	}

	/*
	 * @see IJavaAnnotation#isProblem()
	 */
	@Override
	public boolean isProblem() {
		String type= getType();
		return INFO_ANNOTATION_TYPE.equals(type) || WARNING_ANNOTATION_TYPE.equals(type) || ERROR_ANNOTATION_TYPE.equals(type);
	}

	/**
	 * Overlays this annotation with the given javaAnnotation.
	 *
	 * @param javaAnnotation annotation that is overlaid by this annotation
	 */
	public void setOverlay(IJavaAnnotation javaAnnotation) {
		if (fOverlay != null)
			fOverlay.removeOverlaid(this);

		fOverlay= javaAnnotation;
		if (!isMarkedDeleted())
			markDeleted(fOverlay != null);

		if (fOverlay != null)
			fOverlay.addOverlaid(this);
	}

	/*
	 * @see IJavaAnnotation#hasOverlay()
	 */
	@Override
	public boolean hasOverlay() {
		return fOverlay != null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation#getOverlay()
	 */
	@Override
	public IJavaAnnotation getOverlay() {
		return fOverlay;
	}

	/*
	 * @see IJavaAnnotation#addOverlaid(IJavaAnnotation)
	 */
	@Override
	public void addOverlaid(IJavaAnnotation annotation) {
		// not supported
	}

	/*
	 * @see IJavaAnnotation#removeOverlaid(IJavaAnnotation)
	 */
	@Override
	public void removeOverlaid(IJavaAnnotation annotation) {
		// not supported
	}

	/*
	 * @see IJavaAnnotation#getOverlaidIterator()
	 */
	@Override
	public Iterator<IJavaAnnotation> getOverlaidIterator() {
		// not supported
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation#getCompilationUnit()
	 */
	@Override
	public ICompilationUnit getCompilationUnit() {
		IJavaElement element= JavaCore.create(getMarker().getResource());
		if (element instanceof ICompilationUnit) {
			return (ICompilationUnit)element;
		}
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation#getMarkerType()
	 */
	@Override
	public String getMarkerType() {
		IMarker marker= getMarker();
		if (marker == null  || !marker.exists())
			return null;

		return  MarkerUtilities.getMarkerType(getMarker());
	}
}
