/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ParentProvider;

public class AccessAnalyzer extends ParentProvider {
	
	private char[] fFieldIdentifier;
	private String fGetter;
	private String fSetter;
	private TextChange fChange;
	private RefactoringStatus fStatus;

	private static final String READ_ACCESS= "Encapsulate read access";
	private static final String WRITE_ACCESS= "Encapsulate write access";
		
	public AccessAnalyzer( SelfEncapsulateFieldRefactoring refactoring, FieldDeclaration field, TextChange change) {
		fFieldIdentifier= ASTUtil.getIdentifier(field.binding);
		Assert.isNotNull(fFieldIdentifier);
		fChange= change;
		Assert.isNotNull(fChange);
		fGetter= refactoring.getGetterName();
		fSetter= refactoring.getSetterName();
		fStatus= new RefactoringStatus();
	}

	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	public boolean visit(Assignment node, BlockScope scope) {
		if (node.lhs instanceof SingleNameReference) {
			if (getFieldBinding(node.lhs) != null) {	
				handleSingleWriteAccess(node, scope);
				return false;
			}
		} else if (node.lhs instanceof QualifiedNameReference) {
			QualifiedNameReference qnr= (QualifiedNameReference)node.lhs;
			return handleQualifiedWriteAccess(node, scope, qnr);
		}
		return true;
	}

	/*
	 * Handles write access of kind field= value;
	 */
	private void handleSingleWriteAccess(Assignment node, BlockScope scope) {
		if (checkParent(node))
			fChange.addTextEdit(WRITE_ACCESS, new EncapsulateWriteAccess(fSetter, node.lhs, node.expression));
		node.expression.traverse(this, scope);
	}
	 
	/*
	 * Handles write access of kind a.field= value
	 */
	private boolean handleQualifiedWriteAccess(Assignment node, BlockScope scope, QualifiedNameReference qnr) {
		FieldBinding binding;
		int index= qnr.otherBindings.length - 1;
		binding= qnr.otherBindings[index];
		if (considerFieldBinding(binding)) {
			if (checkParent(node)) {
				int offset= getOffset(qnr, index);
				fChange.addTextEdit(WRITE_ACCESS, new EncapsulateWriteAccess(
					fSetter, offset, node.expression.sourceStart - offset, node.expression));
			}
			node.expression.traverse(this, scope);
			return false;
		} else {
			return true;
		}
	}

	private boolean checkParent(Assignment assignment) {
		AstNode parent= getParent();
		if (parent instanceof Assignment) {
			// case t= a.text= "d";
			fStatus.addError("Cannot encapsulate write access. It is part of an assignment statement.");
			return false;
		} else if (parent instanceof AbstractVariableDeclaration) { 
			AbstractVariableDeclaration declaration= (AbstractVariableDeclaration)parent;
			if (declaration.initialization == assignment) {
				// caseString  t= a.text= "d";
				fStatus.addError("Cannot encapsulate write access. It is part of a variable declaration's initializer.");
				return false;
			}
		} else if (parent instanceof BinaryExpression) {
			// case if ((a.text= "d") == "d")
			fStatus.addError("Cannot encapsulate write access. It is part of an expression.");
			return false;
		} else if (parent instanceof MessageSend) {
			// case (a.text= "d").length();
			MessageSend messageSend= (MessageSend)parent;
			if (messageSend.receiver == assignment) {
				fStatus.addError("Cannot encapsulate write access. Is is part of a message send's receiver.");
				return false;
			}
		}
		return true;
	}
	
	public boolean visit(SingleNameReference node, BlockScope scope) {
		FieldBinding binding= getFieldBinding(node);
		if (binding == null)
			return true;
		
		fChange.addTextEdit(READ_ACCESS, new EncapsulateReadAccess(fGetter, node));
		return true;
	}
	
	public boolean visit(QualifiedNameReference node, BlockScope scope) {
		FieldBinding binding= getFieldBinding(node);
		if (binding != null)
			fChange.addTextEdit(READ_ACCESS, new EncapsulateReadAccess(fGetter, node.sourceStart, node.tokens[0].length));
	
		FieldBinding[] others= node.otherBindings;
		int start= node.sourceStart + node.tokens[0].length + 1;
		for (int i= 0; i < others.length; i++) {
			FieldBinding other= others[i];
			int length= node.tokens[i + 1].length;
			if (considerFieldBinding(other)) {
				fChange.addTextEdit(READ_ACCESS, new EncapsulateReadAccess(fGetter, start, length));
			}
			start+=  length + 1;
		}
		
		return true;
	}
	
	private FieldBinding getFieldBinding(Reference reference) {
		if (! (reference instanceof NameReference))
			return null;
		Binding binding= ((NameReference)reference).binding;
		if (!(binding instanceof FieldBinding))
			return null;
			
		FieldBinding result= (FieldBinding)binding;
		
		if (considerFieldBinding(result))
			return result;
			
		return null;
	}
	
	private boolean considerFieldBinding(FieldBinding binding) {
		if (binding.declaringClass == null) // case array.length
			return false;
		return CharOperation.equals(fFieldIdentifier, ASTUtil.getIdentifier(binding));
	}
	
	private int getOffset(QualifiedNameReference node, int index) {
		int offset= node.sourceStart;
		for (int i= 0; i <= index; i++) {		// first token belongs to node itself.
			offset+= node.tokens[index].length + 1;
		}
		return offset;
	}
}

