/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;

public class InlineMethodFixCore implements IProposableFix, ICleanUpFixCore {

	private final String fName;
	private final ICompilationUnit fCompilationUnit;
	private final InlineMethodRefactoring fRefactoring;

	private InlineMethodFixCore(String name, CompilationUnit compilationUnit, InlineMethodRefactoring refactoring) {
		this.fName= name;
		this.fCompilationUnit= (ICompilationUnit)compilationUnit.getJavaElement();
		this.fRefactoring= refactoring;
	}

	public static InlineMethodFixCore create(String name, CompilationUnit compilationUnit, MethodInvocation methodInvocation) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(cu, compilationUnit,
				methodInvocation.getStartPosition(), methodInvocation.getLength());
		try {
			RefactoringStatus status= refactoring.checkAllConditions(new NullProgressMonitor());
			if (!status.isOK()) {
				return null;
			}
		} catch (OperationCanceledException | CoreException e) {
			return null;
		}
		InlineMethodFixCore fix= new InlineMethodFixCore(name, compilationUnit, refactoring);
		return fix;
	}
	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		CompositeChange change= (CompositeChange)fRefactoring.createChange(progressMonitor);
		CompilationUnitChange compilationUnitChange= new CompilationUnitChange(fName, fCompilationUnit);
		Change[] changes= change.getChildren();
		if (changes.length == 1 && changes[0] instanceof TextChange textChange) {
			compilationUnitChange.setEdit(textChange.getEdit());
			return compilationUnitChange;
		}
		return null;
	}

	@Override
	public String getDisplayString() {
		return fName;
	}

	@Override
	public String getAdditionalProposalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IStatus getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

}
