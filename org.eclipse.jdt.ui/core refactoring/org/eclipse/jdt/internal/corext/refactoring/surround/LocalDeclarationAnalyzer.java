/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;

public class LocalDeclarationAnalyzer extends AbstractSyntaxTreeVisitorAdapter {

	private Selection fSelection;
	private List fAffectedLocals;

	public static LocalDeclaration[] perform(AbstractMethodDeclaration method, Selection selection, ClassScope scope) {
		LocalDeclarationAnalyzer analyzer= new LocalDeclarationAnalyzer(selection);
		method.traverse(analyzer, scope);
		return (LocalDeclaration[]) analyzer.fAffectedLocals.toArray(new LocalDeclaration[analyzer.fAffectedLocals.size()]);
	}

	private LocalDeclarationAnalyzer(Selection selection) {
		fSelection= selection;
		fAffectedLocals= new ArrayList(1);
	}
	
	public boolean visit(SingleNameReference node, BlockScope scope) {
		if (!considerNode(node) || !(node.binding instanceof LocalVariableBinding))
			return false;
		handleReferenceToLocal((LocalVariableBinding)node.binding);
		return true;
	}	
	
	public boolean visit(QualifiedNameReference node, BlockScope scope) {
		if (!considerNode(node) || !(node.binding instanceof LocalVariableBinding))
			return false;
		handleReferenceToLocal((LocalVariableBinding)node.binding);
		return true;
	}

	private boolean considerNode(AstNode node) {
		return fSelection.getVisitSelectionMode(node) == Selection.AFTER;
	}
	
	private void handleReferenceToLocal(LocalVariableBinding reference) {
		LocalDeclaration declaration= reference.declaration;
		if (fSelection.covers(declaration.declarationSourceStart, declaration.declarationSourceEnd))
			addLocalDeclaration(declaration);
	}
	
	private void addLocalDeclaration(LocalDeclaration declaration) {
		if (!fAffectedLocals.contains(declaration))
			fAffectedLocals.add(declaration);
	}
}
