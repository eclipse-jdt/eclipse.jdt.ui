/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.LocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class LocalTypeAnalyzer extends AbstractSyntaxTreeVisitorAdapter {

	private Selection fSelection;
	private List fTypeDeclarationsBefore= new ArrayList(2);
	private List fTypeDeclarationsSelected= new ArrayList(2);
	private String fBeforeTypeReferenced;
	private String fSelectedTypeReferenced;

	//---- Analyzing statements ----------------------------------------------------------------
	
	public static RefactoringStatus perform(AbstractMethodDeclaration method, ClassScope scope, Selection selection) {
		LocalTypeAnalyzer analyzer= new LocalTypeAnalyzer(selection);
		method.traverse(analyzer, scope);
		RefactoringStatus result= new RefactoringStatus();
		analyzer.check(result);
		return result;
	}
	
	private LocalTypeAnalyzer(Selection selection) {
		fSelection= selection;
	}
	
	public boolean visit(SingleNameReference node, BlockScope scope) {
		if (node.binding instanceof LocalTypeBinding)
			processLocalTypeBinding((LocalTypeBinding)node.binding, fSelection.getVisitSelectionMode(node));

		return true;
	}
	
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return visitTypeReference(arrayTypeReference, scope);
	}

	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		return visitTypeReference(arrayQualifiedTypeReference, scope);
	}

	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		return visitTypeReference(singleTypeReference, scope);
	}

	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		return visitTypeReference(qualifiedTypeReference, scope);
	}

	public boolean visit(LocalTypeDeclaration node, BlockScope scope) {
		visitLocalTypeDeclaration(node, scope);
		return true;
	}
	
	private boolean visitTypeReference(TypeReference node, BlockScope scope) {
		if (node.binding instanceof LocalTypeBinding)
			processLocalTypeBinding((LocalTypeBinding)node.binding, fSelection.getVisitSelectionMode(node));
			
		return true;
	}
	
	private void visitLocalTypeDeclaration(TypeDeclaration node, BlockScope scope) {
		int mode= fSelection.getVisitSelectionMode(node);
		switch (mode) {
			case Selection.BEFORE:
				fTypeDeclarationsBefore.add(node);
				break;
			case Selection.SELECTED:
				fTypeDeclarationsSelected.add(node);
				break;
		}
	}
	
	private void processLocalTypeBinding(LocalTypeBinding binding, int mode) {
		switch (mode) {
			case Selection.SELECTED:
				if (fBeforeTypeReferenced != null)
					break;
				if (checkBinding(fTypeDeclarationsBefore, binding))
					fBeforeTypeReferenced= RefactoringCoreMessages.getString("LocalTypeAnalyzer.local_type_from_outside"); //$NON-NLS-1$
				break;
			case Selection.AFTER:
				if (fSelectedTypeReferenced != null)
					break;
				if (checkBinding(fTypeDeclarationsSelected, binding))
					fSelectedTypeReferenced= RefactoringCoreMessages.getString("LocalTypeAnalyzer.local_type_referenced_outside"); //$NON-NLS-1$
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
	
	private void check(RefactoringStatus status) {
		if (fBeforeTypeReferenced != null)
			status.addFatalError(fBeforeTypeReferenced);
		if (fSelectedTypeReferenced != null)
			status.addFatalError(fSelectedTypeReferenced);
	}	
}