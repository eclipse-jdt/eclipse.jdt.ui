/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     jens.lukowski@gmx.de - contributed code to convert prefix and postfix 
 *       expressions into a combination of setter and getter calls.
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.sef;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Analyzer to find all references to the field and to determine how to convert 
 * them into setter or getter calls.
 */
class AccessAnalyzer extends ASTVisitor {

	private ICompilationUnit fCUnit;
	private IVariableBinding fFieldBinding;
	private ITypeBinding fDeclaringClassBinding;	
	private String fGetter;
	private String fSetter;
	private OldASTRewrite fRewriter;
	private List fGroupDescriptions;
	private RefactoringStatus fStatus;
	private boolean fSetterMustReturnValue;
	private boolean fEncapsulateDeclaringClass;
	private boolean fIsFieldFinal;

	private static final String READ_ACCESS= RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.encapsulate_read_access"); //$NON-NLS-1$
	private static final String WRITE_ACCESS= RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.encapsulate_write_access"); //$NON-NLS-1$
	private static final String PREFIX_ACCESS= RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.encapsulate_prefix_access"); //$NON-NLS-1$
	private static final String POSTFIX_ACCESS= RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.encapsulate_postfix_access"); //$NON-NLS-1$
		
	public AccessAnalyzer(SelfEncapsulateFieldRefactoring refactoring, ICompilationUnit unit, IVariableBinding field, ITypeBinding declaringClass, OldASTRewrite rewriter) {
		Assert.isNotNull(refactoring);
		Assert.isNotNull(unit);
		Assert.isNotNull(field);
		Assert.isNotNull(declaringClass);
		Assert.isNotNull(rewriter);
		fCUnit= unit;
		fFieldBinding= field;
		fDeclaringClassBinding= declaringClass;
		fRewriter= rewriter;
		fGroupDescriptions= new ArrayList();
		fGetter= refactoring.getGetterName();
		fSetter= refactoring.getSetterName();
		fEncapsulateDeclaringClass= refactoring.getEncapsulateDeclaringClass();
		try {
			fIsFieldFinal= Flags.isFinal(refactoring.getField().getFlags());
		} catch (JavaModelException e) {
			// assume non final field
		}
		fStatus= new RefactoringStatus();
	}

	public boolean getSetterMustReturnValue() {
		return fSetterMustReturnValue;
	}

	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	public List getGroupDescriptions() {
		return fGroupDescriptions;
	}
	
	public boolean visit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		if (!considerBinding(resolveBinding(lhs), lhs))
			return true;
			
		checkParent(node);
		if (!fIsFieldFinal) {
			// Write access.
			AST ast= node.getAST();
			MethodInvocation invocation= ast.newMethodInvocation();
			invocation.setName(ast.newSimpleName(fSetter));
			Expression receiver= getReceiver(lhs);
			if (receiver != null)
				invocation.setExpression((Expression)fRewriter.createCopyTarget(receiver));
			List arguments= invocation.arguments();
			if (node.getOperator() == Assignment.Operator.ASSIGN) {
				arguments.add(fRewriter.createCopyTarget(node.getRightHandSide()));
			} else {
				// This is the compound assignment case: field+= 10;
				boolean needsParentheses= ASTNodes.needsParentheses(node.getRightHandSide());
				InfixExpression exp= ast.newInfixExpression();
				exp.setOperator(ASTNodes.convertToInfixOperator(node.getOperator()));
				MethodInvocation getter= ast.newMethodInvocation();
				getter.setName(ast.newSimpleName(fGetter));
				if (receiver != null)
					getter.setExpression((Expression)fRewriter.createCopyTarget(receiver));
				exp.setLeftOperand(getter);
				Expression rhs= (Expression)fRewriter.createCopyTarget(node.getRightHandSide());
				if (needsParentheses) {
					ParenthesizedExpression p= ast.newParenthesizedExpression();
					p.setExpression(rhs);
					rhs= p;
				}
				exp.setRightOperand(rhs);
				arguments.add(exp);
			}
			fRewriter.replace(node, invocation, createGroupDescription(WRITE_ACCESS));
		}
		node.getRightHandSide().accept(this);
		return false;
	}

	public boolean visit(SimpleName node) {
		if (!node.isDeclaration() && considerBinding(node.resolveBinding(), node)) {
			fRewriter.replace(
				node, 
				fRewriter.createStringPlaceholder(fGetter + "()", ASTNode.METHOD_INVOCATION), //$NON-NLS-1$
				createGroupDescription(READ_ACCESS));
		}
		return true;
	}
	
	public boolean visit(PrefixExpression node) {
		Expression operand= node.getOperand();
		if (!considerBinding(resolveBinding(operand), operand))
			return true;
		
		PrefixExpression.Operator operator= node.getOperator();	
		if (operator != PrefixExpression.Operator.INCREMENT && operator != PrefixExpression.Operator.DECREMENT)
			return true;
			
		checkParent(node);
		
		fRewriter.replace(node, 
			createInvocation(node.getAST(), node.getOperand(), node.getOperator().toString()), 
			createGroupDescription(PREFIX_ACCESS));
		return false;
	}
	
	public boolean visit(PostfixExpression node) {
		Expression operand= node.getOperand();
		if (!considerBinding(resolveBinding(operand), operand))
			return true;

		ASTNode parent= node.getParent();
		if (!(parent instanceof ExpressionStatement)) {
			fStatus.addError(RefactoringCoreMessages.getString("SelfEncapsulateField.AccessAnalyzer.cannot_convert_postfix_expression"),  //$NON-NLS-1$
				JavaStatusContext.create(fCUnit, new SourceRange(node)));
			return false;
		}
		fRewriter.replace(node, 
			createInvocation(node.getAST(), node.getOperand(), node.getOperator().toString()), 
			createGroupDescription(POSTFIX_ACCESS));
		return false;
	}
	
	private boolean considerBinding(IBinding binding, ASTNode node) {
		boolean result= Bindings.equals(fFieldBinding, binding);
		if (!result || fEncapsulateDeclaringClass)
			return result;
			
		if (binding instanceof IVariableBinding) {
			TypeDeclaration type= (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
			if (type != null) {
				ITypeBinding declaringType= type.resolveBinding();
				return !Bindings.equals(fDeclaringClassBinding, declaringType);
			}
		}
		return true;
	}
	
	private void checkParent(ASTNode node) {
		ASTNode parent= node.getParent();
		if (!(parent instanceof ExpressionStatement))
			fSetterMustReturnValue= true;
	}
		
	private IBinding resolveBinding(Expression expression) {
		if (expression instanceof SimpleName)
			return ((SimpleName)expression).resolveBinding();
		else if (expression instanceof QualifiedName)
			return ((QualifiedName)expression).resolveBinding();
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).getName().resolveBinding();
		return null;
	}
	
	private Expression getReceiver(Expression expression) {
		int type= expression.getNodeType();
		switch(type) {
			case ASTNode.SIMPLE_NAME:
				return null;
			case ASTNode.QUALIFIED_NAME:
				return ((QualifiedName)expression).getQualifier();
			case ASTNode.FIELD_ACCESS:
				return ((FieldAccess)expression).getExpression();
		}
		return null;
	}
		
	private MethodInvocation createInvocation(AST ast, Expression operand, String operator) {
		Expression receiver= getReceiver(operand);
		MethodInvocation invocation= ast.newMethodInvocation();
		invocation.setName(ast.newSimpleName(fSetter));
		if (receiver != null)
			invocation.setExpression((Expression)fRewriter.createCopyTarget(receiver));
		InfixExpression argument= ast.newInfixExpression();
		invocation.arguments().add(argument);
		if ("++".equals(operator)) { //$NON-NLS-1$
			argument.setOperator(InfixExpression.Operator.PLUS);
		} else if ("--".equals(operator)) { //$NON-NLS-1$
			argument.setOperator(InfixExpression.Operator.MINUS);
		} else {
			Assert.isTrue(false, "Should not happen"); //$NON-NLS-1$
		}
		MethodInvocation getter= ast.newMethodInvocation();
		getter.setName(ast.newSimpleName(fGetter));
		if (receiver != null)
			getter.setExpression((Expression)fRewriter.createCopyTarget(receiver));
		argument.setLeftOperand(getter);
		argument.setRightOperand(ast.newNumberLiteral("1")); //$NON-NLS-1$

		return invocation;
	}
	
	private TextEditGroup createGroupDescription(String name) {
		TextEditGroup result= new TextEditGroup(name);
		fGroupDescriptions.add(result);
		return result;
	}
}

