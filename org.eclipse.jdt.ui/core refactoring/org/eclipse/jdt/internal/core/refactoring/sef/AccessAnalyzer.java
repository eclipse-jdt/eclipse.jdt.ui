/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.sef;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.internal.core.refactoring.util.ASTUtil;

public class AccessAnalyzer extends AbstractSyntaxTreeVisitorAdapter {
	
	private char[] fFieldIdentifier;
	private String fGetter;
	private String fSetter;
	private ITextBufferChange fChange;
	
	private static final String READ_ACCESS= "Encapsulate read access";
	
	public AccessAnalyzer( SelfEncapsulateFieldRefactoring refactoring, FieldDeclaration field, ITextBufferChange change) {
		fFieldIdentifier= ASTUtil.getIdentifier(field.binding);
		Assert.isNotNull(fFieldIdentifier);
		fChange= change;
		Assert.isNotNull(fChange);
		fGetter= refactoring.getGetterName();
		fSetter= refactoring.getSetterName();
	}

	public boolean visit(Assignment node, BlockScope scope) {
		FieldBinding binding= getFieldBinding(node.lhs);
		if (binding == null)
			return true;
		
		fChange.addSimpleTextChange(new EncapsulateWriteAccess(fSetter, node.lhs, node.expression));
		node.expression.traverse(this, scope);
		return false;
	}
	
	public boolean visit(SingleNameReference node, BlockScope scope) {
		FieldBinding binding= getFieldBinding(node);
		if (binding == null)
			return true;
		
		fChange.addSimpleTextChange(new EncapsulateReadAccess(fGetter, node));
		return true;
	}
	
	public boolean visit(QualifiedNameReference node, BlockScope scope) {
		FieldBinding binding= getFieldBinding(node);
		if (binding != null)
			fChange.addSimpleTextChange(new EncapsulateReadAccess(fGetter, node.sourceStart, node.tokens[0].length));
	
		FieldBinding[] others= node.otherBindings;
		int start= node.sourceStart + node.tokens[0].length + 1;
		for (int i= 0; i < others.length; i++) {
			FieldBinding other= others[i];
			int length= node.tokens[i + 1].length;
			if (CharOperation.equals(fFieldIdentifier, ASTUtil.getIdentifier(other))) {
				fChange.addSimpleTextChange(new EncapsulateReadAccess(fGetter, start, length));
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
		
		if (CharOperation.equals(fFieldIdentifier, ASTUtil.getIdentifier(result)))
			return result;
			
		return null;
	}
}

