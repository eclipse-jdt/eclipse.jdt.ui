/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.core.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;

/* package */ class LocalTypeAnalyzer {
	
	private List fTypeDeclarationsBefore= new ArrayList(2);
	private List fTypeDeclarationsSelected= new ArrayList(2);
	private String fBeforeTypeReferenced;
	private String fSelectedTypeReferenced;

	//---- Analyzing statements ----------------------------------------------------------------

	public void visit(SingleNameReference singleNameReference, BlockScope scope, int mode) {
		if (!(singleNameReference.binding instanceof LocalTypeBinding))
			return;
		processLocalTypeBinding((LocalTypeBinding)singleNameReference.binding, mode);
	}
	
	public void visitLocalTypeDeclaration(TypeDeclaration declaration, BlockScope scope, int mode) {
		switch (mode) {
			case StatementAnalyzer.BEFORE:
				fTypeDeclarationsBefore.add(declaration);
				break;
			case StatementAnalyzer.SELECTED:
				fTypeDeclarationsSelected.add(declaration);
				break;
		}
	}
	
	public void visitTypeReference(TypeReference reference, BlockScope scope, int mode) {
		if (!(reference.binding instanceof LocalTypeBinding))
			return;
		processLocalTypeBinding((LocalTypeBinding)reference.binding, mode);
	}
	
	private void processLocalTypeBinding(LocalTypeBinding binding, int mode) {
		switch (mode) {
			case StatementAnalyzer.SELECTED:
				if (fBeforeTypeReferenced != null)
					break;
				if (checkBinding(fTypeDeclarationsBefore, binding))
					fBeforeTypeReferenced= "Selected block references a local type declared outside the selection";
				break;
			case StatementAnalyzer.AFTER:
				if (fSelectedTypeReferenced != null)
					break;
				if (checkBinding(fTypeDeclarationsSelected, binding))
					fSelectedTypeReferenced= "A local type declared in the selected block is referenced outside the selection";
				break;
		}
	}
	
	private boolean checkBinding(List declarations, LocalTypeBinding binding) {
		for (Iterator iter= declarations.iterator(); iter.hasNext();) {
			TypeDeclaration declaration= (TypeDeclaration)iter.next();
			if (declaration.binding == binding) {
				return true;
			}
		}
		return false;
	}
	
	//---- Precondition checking ----------------------------------------------------------------

	public void checkActivation(RefactoringStatus status) {
		if (fBeforeTypeReferenced != null)
			status.addError(fBeforeTypeReferenced);
		if (fSelectedTypeReferenced != null)
			status.addError(fSelectedTypeReferenced);
	}
	
}