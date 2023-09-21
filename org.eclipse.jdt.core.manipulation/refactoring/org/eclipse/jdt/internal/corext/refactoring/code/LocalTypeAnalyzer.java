/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

public class LocalTypeAnalyzer extends ASTVisitor {

	private Selection fSelection;
	private List<AbstractTypeDeclaration> fTypeDeclarationsBefore= new ArrayList<>(2);
	private List<AbstractTypeDeclaration> fTypeDeclarationsSelected= new ArrayList<>(2);
	private String fBeforeTypeReferenced;
	private String fSelectedTypeReferenced;

	//---- Analyzing statements ----------------------------------------------------------------

	public static RefactoringStatus perform(BodyDeclaration declaration, Selection selection) {
		LocalTypeAnalyzer analyzer= new LocalTypeAnalyzer(selection);
		declaration.accept(analyzer);
		RefactoringStatus result= new RefactoringStatus();
		analyzer.check(result);
		return result;
	}

	private LocalTypeAnalyzer(Selection selection) {
		fSelection= selection;
	}

	@Override
	public boolean visit(SimpleName node) {
		if (node.isDeclaration())
			return true;
		IBinding binding= node.resolveBinding();
		if (binding instanceof ITypeBinding)
			processLocalTypeBinding((ITypeBinding) binding, fSelection.getVisitSelectionMode(node));

		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		return visitType(node);
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		return visitType(node);
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		return visitType(node);
	}

	private boolean visitType(AbstractTypeDeclaration node) {
		int mode= fSelection.getVisitSelectionMode(node);
		switch (mode) {
			case Selection.BEFORE:
				fTypeDeclarationsBefore.add(node);
				break;
			case Selection.SELECTED:
				fTypeDeclarationsSelected.add(node);
				break;
		}
		return true;
	}

	private void processLocalTypeBinding(ITypeBinding binding, int mode) {
		switch (mode) {
			case Selection.SELECTED:
				if (fBeforeTypeReferenced != null)
					break;
				if (checkBinding(fTypeDeclarationsBefore, binding))
					fBeforeTypeReferenced= RefactoringCoreMessages.LocalTypeAnalyzer_local_type_from_outside;
				break;
			case Selection.AFTER:
				if (fSelectedTypeReferenced != null)
					break;
				if (checkBinding(fTypeDeclarationsSelected, binding))
					fSelectedTypeReferenced= RefactoringCoreMessages.LocalTypeAnalyzer_local_type_referenced_outside;
				break;
		}
	}

	private boolean checkBinding(List<AbstractTypeDeclaration> declarations, ITypeBinding binding) {
		for (AbstractTypeDeclaration declaration : declarations) {
			if (declaration.resolveBinding() == binding) {
				return true;
			}
		}
		return false;
	}

	private void check(RefactoringStatus status) {
		if (fBeforeTypeReferenced != null)
			status.addFatalError(fBeforeTypeReferenced);
		if (fSelectedTypeReferenced != null)
			status.addFatalError(fSelectedTypeReferenced);
	}
}
