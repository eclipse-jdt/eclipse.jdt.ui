/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.Iterator;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;

public class ASTFlattener extends GenericVisitor {

	protected StringBuffer fResult;

	public ASTFlattener() {
		fResult= new StringBuffer();
	}
	
	/**
	 * Returns the string accumulated in the visit.
	 *
	 * @return the serialized 
	 */
	public String getResult() {
		// convert to a string, but lose any extra space in the string buffer by copying
		return new String(fResult.toString());
	}
	
	/**
	 * Resets this printer so that it can be used again.
	 */
	public void reset() {
		fResult.setLength(0);
	}
	

	protected boolean visitNode(ASTNode node) {
		Assert.isTrue(false, "No implementation to flatten node: " + node.toString());  //$NON-NLS-1$
		return false;
	}
	
	/**
	 * Appends the text representation of the given modifier flags, followed by a single space.
	 * 
	 * @param modifiers the modifiers
	 * @param buf The <code>StringBuffer</code> to write the result to.
	 */
	public static void printModifiers(int modifiers, StringBuffer buf) {
		if (Modifier.isPublic(modifiers)) {
			buf.append("public "); //$NON-NLS-1$
		}
		if (Modifier.isProtected(modifiers)) {
			buf.append("protected "); //$NON-NLS-1$
		}
		if (Modifier.isPrivate(modifiers)) {
			buf.append("private "); //$NON-NLS-1$
		}
		if (Modifier.isStatic(modifiers)) {
			buf.append("static "); //$NON-NLS-1$
		}
		if (Modifier.isAbstract(modifiers)) {
			buf.append("abstract "); //$NON-NLS-1$
		}
		if (Modifier.isFinal(modifiers)) {
			buf.append("final "); //$NON-NLS-1$
		}
		if (Modifier.isSynchronized(modifiers)) {
			buf.append("synchronized "); //$NON-NLS-1$
		}
		if (Modifier.isVolatile(modifiers)) {
			buf.append("volatile "); //$NON-NLS-1$
		}
		if (Modifier.isNative(modifiers)) {
			buf.append("native "); //$NON-NLS-1$
		}
		if (Modifier.isStrictfp(modifiers)) {
			buf.append("strictfp "); //$NON-NLS-1$
		}
		if (Modifier.isTransient(modifiers)) {
			buf.append("transient "); //$NON-NLS-1$
		}
	}		
	
	/*
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		fResult.append('{');
		for (Iterator it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
			BodyDeclaration b = (BodyDeclaration) it.next();
			b.accept(this);
		}
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		node.getArray().accept(this);
		fResult.append('[');
		node.getIndex().accept(this);
		fResult.append(']');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		fResult.append("new "); //$NON-NLS-1$
		ArrayType at = node.getType();
		int dims = at.getDimensions();
		Type elementType = at.getElementType();
		elementType.accept(this);
		for (Iterator it = node.dimensions().iterator(); it.hasNext(); ) {
			fResult.append('[');
			Expression e = (Expression) it.next();
			e.accept(this);
			fResult.append(']');
			dims--;
		}
		// add empty "[]" for each extra array dimension
		for (int i= 0; i < dims; i++) {
			fResult.append("[]"); //$NON-NLS-1$
		}
		if (node.getInitializer() != null) {
			fResult.append('=');
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		fResult.append('{');
		for (Iterator it = node.expressions().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		node.getComponentType().accept(this);
		fResult.append("[]"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		fResult.append("assert "); //$NON-NLS-1$
		node.getExpression().accept(this);
		if (node.getMessage() != null) {
			fResult.append(':');
			node.getMessage().accept(this);
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		node.getLeftHandSide().accept(this);
		fResult.append(node.getOperator().toString());
		node.getRightHandSide().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		fResult.append('{');
		for (Iterator it = node.statements().iterator(); it.hasNext(); ) {
			Statement s = (Statement) it.next();
			s.accept(this);
		}
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		if (node.booleanValue() == true) {
			fResult.append("true"); //$NON-NLS-1$
		} else {
			fResult.append("false"); //$NON-NLS-1$
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		fResult.append("break"); //$NON-NLS-1$
		if (node.getLabel() != null) {
			fResult.append(' ');
			node.getLabel().accept(this);
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CastExpression)
	 */
	public boolean visit(CastExpression node) {
		fResult.append('(');
		node.getType().accept(this);
		fResult.append(')');
		node.getExpression().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		fResult.append("catch ("); //$NON-NLS-1$
		node.getException().accept(this);
		fResult.append(')');
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		fResult.append(node.getEscapedValue());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			fResult.append('.');
		}
		fResult.append("new "); //$NON-NLS-1$
		node.getName().accept(this);
		fResult.append('(');
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append(')');
		if (node.getAnonymousClassDeclaration() != null) {
			node.getAnonymousClassDeclaration().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CompilationUnit)
	 */
	public boolean visit(CompilationUnit node) {
		if (node.getPackage() != null) {
			node.getPackage().accept(this);
		}
		for (Iterator it = node.imports().iterator(); it.hasNext(); ) {
			ImportDeclaration d = (ImportDeclaration) it.next();
			d.accept(this);
		}
		for (Iterator it = node.types().iterator(); it.hasNext(); ) {
			TypeDeclaration d = (TypeDeclaration) it.next();
			d.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		node.getExpression().accept(this);
		fResult.append('?');
		node.getThenExpression().accept(this);
		fResult.append(':');
		node.getElseExpression().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		fResult.append("this("); //$NON-NLS-1$
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append(");"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		fResult.append("continue"); //$NON-NLS-1$
		if (node.getLabel() != null) {
			fResult.append(' ');
			node.getLabel().accept(this);
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		fResult.append("do "); //$NON-NLS-1$
		node.getBody().accept(this);
		fResult.append(" while ("); //$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(");"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		node.getExpression().accept(this);
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		node.getExpression().accept(this);
		fResult.append('.');
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		printModifiers(node.getModifiers(), fResult);
		node.getType().accept(this);
		fResult.append(' ');
		for (Iterator it = node.fragments().iterator(); it.hasNext(); ) {
			VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		fResult.append("for ("); //$NON-NLS-1$
		for (Iterator it = node.initializers().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
		}
		fResult.append(';');
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
		}
		fResult.append(';');
		for (Iterator it = node.updaters().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
		}
		fResult.append(')');
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		fResult.append("if ("); //$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(')');
		node.getThenStatement().accept(this);
		if (node.getElseStatement() != null) {
			fResult.append(" else "); //$NON-NLS-1$
			node.getElseStatement().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		fResult.append("import "); //$NON-NLS-1$
		node.getName().accept(this);
		if (node.isOnDemand()) {
			fResult.append(".*"); //$NON-NLS-1$
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		node.getLeftOperand().accept(this);
		fResult.append(' ');
		fResult.append(node.getOperator().toString());
		fResult.append(' ');
		node.getRightOperand().accept(this);
		for (Iterator it = node.extendedOperands().iterator(); it.hasNext(); ) {
			fResult.append(node.getOperator().toString());
			Expression e = (Expression) it.next();
			e.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		node.getLeftOperand().accept(this);
		fResult.append(" instanceof "); //$NON-NLS-1$
		node.getRightOperand().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		printModifiers(node.getModifiers(), fResult);
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(Javadoc node) {
		fResult.append(node.getComment());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		node.getLabel().accept(this);
		fResult.append(": "); //$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		printModifiers(node.getModifiers(), fResult);
		if (!node.isConstructor()) {
			node.getReturnType().accept(this);
			fResult.append(' ');
		}
		node.getName().accept(this);
		fResult.append('(');
		for (Iterator it = node.parameters().iterator(); it.hasNext(); ) {
			SingleVariableDeclaration v = (SingleVariableDeclaration) it.next();
			v.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append(')');
		for (int i = 0; i < node.getExtraDimensions(); i++) {
			fResult.append("[]"); //$NON-NLS-1$
		}		
		if (!node.thrownExceptions().isEmpty()) {
			fResult.append(" throws "); //$NON-NLS-1$
			for (Iterator it = node.thrownExceptions().iterator(); it.hasNext(); ) {
				Name n = (Name) it.next();
				n.accept(this);
				if (it.hasNext()) {
					fResult.append(',');
				}
			}
			fResult.append(' ');
		}
		if (node.getBody() == null) {
			fResult.append(';');
		} else {
			node.getBody().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			fResult.append('.');
		}
		node.getName().accept(this);
		fResult.append('(');
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append(')');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		fResult.append("null"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		fResult.append(node.getToken());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		fResult.append("package "); //$NON-NLS-1$
		node.getName().accept(this);
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		fResult.append('(');
		node.getExpression().accept(this);
		fResult.append(')');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		node.getOperand().accept(this);
		fResult.append(node.getOperator().toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		fResult.append(node.getOperator().toString());
		node.getOperand().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		fResult.append(node.getPrimitiveTypeCode().toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		node.getQualifier().accept(this);
		fResult.append('.');
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		fResult.append("return"); //$NON-NLS-1$
		if (node.getExpression() != null) {
			fResult.append(' ');
			node.getExpression().accept(this);
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		fResult.append(node.getIdentifier());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		return true;
	}

	/*
	 * @see ASTVisitor#visit(SingleVariableDeclaration)
	 */
	public boolean visit(SingleVariableDeclaration node) {
		printModifiers(node.getModifiers(), fResult);
		node.getType().accept(this);
		fResult.append(' ');
		node.getName().accept(this);
		for (int i = 0; i < node.getExtraDimensions(); i++) {
			fResult.append("[]"); //$NON-NLS-1$
		}			
		if (node.getInitializer() != null) {
			fResult.append('=');
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		fResult.append(node.getEscapedValue());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			fResult.append('.');
		}
		fResult.append("super("); //$NON-NLS-1$
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append(");"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			fResult.append('.');
		}
		fResult.append("super."); //$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			fResult.append('.');
		}
		fResult.append("super."); //$NON-NLS-1$
		node.getName().accept(this);
		fResult.append('(');
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append(')');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		Expression expression= node.getExpression();
		if (expression == null) {
			fResult.append("default"); //$NON-NLS-1$
		} else {
			fResult.append("case "); //$NON-NLS-1$
			expression.accept(this);
		}
		fResult.append(':');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		fResult.append("switch ("); //$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(')');
		fResult.append('{');
		for (Iterator it = node.statements().iterator(); it.hasNext(); ) {
			Statement s = (Statement) it.next();
			s.accept(this);
		}
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SynchronizedStatement)
	 */
	public boolean visit(SynchronizedStatement node) {
		fResult.append("synchronized ("); //$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(')');
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			fResult.append('.');
		}
		fResult.append("this"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		fResult.append("throw "); //$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		fResult.append("try "); //$NON-NLS-1$
		node.getBody().accept(this);
		fResult.append(' ');
		for (Iterator it = node.catchClauses().iterator(); it.hasNext(); ) {
			CatchClause cc = (CatchClause) it.next();
			cc.accept(this);
		}
		if (node.getFinally() != null) {
			fResult.append(" finally "); //$NON-NLS-1$
			node.getFinally().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		printModifiers(node.getModifiers(), fResult);
		fResult.append(node.isInterface() ? "interface " : "class "); //$NON-NLS-1$ //$NON-NLS-2$
		node.getName().accept(this);
		fResult.append(' ');
		if (node.getSuperclass() != null) {
			fResult.append("extends "); //$NON-NLS-1$
			node.getSuperclass().accept(this);
			fResult.append(' ');
		}
		if (!node.superInterfaces().isEmpty()) {
			fResult.append(node.isInterface() ? "extends " : "implements "); //$NON-NLS-1$ //$NON-NLS-2$
			for (Iterator it = node.superInterfaces().iterator(); it.hasNext(); ) {
				Name n = (Name) it.next();
				n.accept(this);
				if (it.hasNext()) {
					fResult.append(',');
				}
			}
			fResult.append(' ');
		}
		fResult.append('{');
		for (Iterator it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
			BodyDeclaration d = (BodyDeclaration) it.next();
			d.accept(this);
		}
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		node.getTypeDeclaration().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		node.getType().accept(this);
		fResult.append(".class"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		printModifiers(node.getModifiers(), fResult);
		node.getType().accept(this);
		fResult.append(' ');
		for (Iterator it = node.fragments().iterator(); it.hasNext(); ) {
			VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		node.getName().accept(this);
		for (int i = 0; i < node.getExtraDimensions(); i++) {
			fResult.append("[]"); //$NON-NLS-1$
		}
		if (node.getInitializer() != null) {
			fResult.append('=');
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		printModifiers(node.getModifiers(), fResult);
		node.getType().accept(this);
		fResult.append(' ');
		for (Iterator it = node.fragments().iterator(); it.hasNext(); ) {
			VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				fResult.append(',');
			}
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		fResult.append("while ("); //$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(')');
		node.getBody().accept(this);
		return false;
	}	
}
