/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified in jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;

/**
 *
 */
public class ProblemLocationCore implements IProblemLocationCore {

	private final int fId;
	private final String[] fArguments;
	private final int fOffset;
	private final int fLength;
	private final boolean fIsError;
	private final String fMarkerType;

	public ProblemLocationCore(int offset, int length, int id, String[] arguments, boolean isError, String markerType) {
		fId= id;
		fArguments= arguments;
		fOffset= offset;
		fLength= length;
		fIsError= isError;
		fMarkerType= markerType;
	}

	public ProblemLocationCore(IProblem problem) {
		fId= problem.getID();
		fArguments= problem.getArguments();
		fOffset= problem.getSourceStart();
		fLength= problem.getSourceEnd() - fOffset + 1;
		fIsError= problem.isError();
		fMarkerType= problem instanceof CategorizedProblem ? ((CategorizedProblem) problem).getMarkerType() : IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER;
	}


	@Override
	public int getProblemId() {
		return fId;
	}

	@Override
	public String[] getProblemArguments() {
		return fArguments;
	}

	@Override
	public int getLength() {
		return fLength;
	}

	@Override
	public int getOffset() {
		return fOffset;
	}

	@Override
	public boolean isError() {
		return fIsError;
	}

	@Override
	public String getMarkerType() {
		return fMarkerType;
	}

	@Override
	public ASTNode getCoveringNode(CompilationUnit astRoot) {
		NodeFinder finder= new NodeFinder(astRoot, fOffset, fLength);
		return finder.getCoveringNode();
	}

	@Override
	public ASTNode getCoveredNode(CompilationUnit astRoot) {
		NodeFinder finder= new NodeFinder(astRoot, fOffset, fLength);
		return finder.getCoveredNode();
	}

	@Override
	public String toString() {
		StringBuilder buf= new StringBuilder();
		buf.append("Id: ").append(getErrorCode(fId)).append('\n'); //$NON-NLS-1$
		buf.append('[').append(fOffset).append(", ").append(fLength).append(']').append('\n'); //$NON-NLS-1$
		for (String arg : fArguments) {
			buf.append(arg);
			buf.append('\n');
		}
		return buf.toString();
	}

	private String getErrorCode(int code) {
		StringBuilder buf= new StringBuilder();

		if ((code & IProblem.TypeRelated) != 0) {
			buf.append("TypeRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.FieldRelated) != 0) {
			buf.append("FieldRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.ConstructorRelated) != 0) {
			buf.append("ConstructorRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.MethodRelated) != 0) {
			buf.append("MethodRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.ImportRelated) != 0) {
			buf.append("ImportRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.Internal) != 0) {
			buf.append("Internal + "); //$NON-NLS-1$
		}
		if ((code & IProblem.Syntax) != 0) {
			buf.append("Syntax + "); //$NON-NLS-1$
		}
		if ((code & IProblem.Javadoc) != 0) {
			buf.append("Javadoc + "); //$NON-NLS-1$
		}
		buf.append(code & IProblem.IgnoreCategoriesMask);

		return buf.toString();
	}


}
