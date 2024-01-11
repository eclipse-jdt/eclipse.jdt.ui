/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to extend ProblemLocationCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;

public class ProblemLocation extends ProblemLocationCore implements IProblemLocation {
	public static final String ERROR_ANNOTATION_TYPE= "org.eclipse.jdt.ui.error"; //$NON-NLS-1$

	public ProblemLocation(int offset, int length, IJavaAnnotation annotation) {
		super(offset, length, annotation.getId(),
				annotation.getArguments() != null ? annotation.getArguments() : new String[0],
						ERROR_ANNOTATION_TYPE.equals(annotation.getType()),
						annotation.getMarkerType() != null ? annotation.getMarkerType() : IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
	}

	public ProblemLocation(IProblemLocationCore core) {
		super(core.getOffset(), core.getLength(), core.getProblemId(), core.getProblemArguments(), core.isError(), core.getMarkerType());
	}

	public ProblemLocation(int offset, int length, int id, String[] arguments, boolean isError, String markerType) {
		super(offset, length, id, arguments, isError, markerType);
	}

	public ProblemLocation(IProblem problem) {
		super(problem);
	}


}
