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

import java.util.List;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;

public class ASTRewriteFlattener extends GenericVisitor {

	public static String asString(ASTNode node, RewriteEventStore store) {
		ASTRewriteFlattener flattener= new ASTRewriteFlattener(store);
		node.accept(flattener);
		return flattener.getResult();
	}
	
	protected StringBuffer fResult;
	private RewriteEventStore fStore;

	public ASTRewriteFlattener(RewriteEventStore store) {
		fStore= store;
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
	
	protected List getChildList(ASTNode parent, int childProperty) {
		return (List) getAttribute(parent, childProperty);
	}
	
	protected ASTNode getChildNode(ASTNode parent, int childProperty) {
		return (ASTNode) getAttribute(parent, childProperty);
	}
	
	protected int getIntAttribute(ASTNode parent, int childProperty) {
		return ((Integer) getAttribute(parent, childProperty)).intValue();
	}
	
	protected boolean getBooleanAttribute(ASTNode parent, int childProperty) {
		return ((Boolean) getAttribute(parent, childProperty)).booleanValue();
	}
	
	protected Object getAttribute(ASTNode parent, int childProperty) {
		return fStore.getNewValue(parent, childProperty);
	}
	
	protected void visitList(ASTNode parent, int childProperty, String separator) {
		List list= getChildList(parent, childProperty);
		for (int i= 0; i < list.size(); i++) {
			if (separator != null && i > 0) {
				fResult.append(separator);
			}
			((ASTNode) list.get(i)).accept(this);
		}
	}
	
	protected void visitList(ASTNode parent, int childProperty, String separator, String lead) {
		List list= getChildList(parent, childProperty);
		if (!list.isEmpty()) {
			fResult.append(lead);
			for (int i= 0; i < list.size(); i++) {
				if (separator != null && i > 0) {
					fResult.append(separator);
				}
				((ASTNode) list.get(i)).accept(this);
			}
		}
	}
	
	
	/*
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		fResult.append('{');
		visitList(node, ASTNodeConstants.BODY_DECLARATIONS, null);
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		getChildNode(node, ASTNodeConstants.ARRAY).accept(this);
		fResult.append('[');
		getChildNode(node, ASTNodeConstants.INDEX).accept(this);
		fResult.append(']');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		fResult.append("new "); //$NON-NLS-1$
		ArrayType arrayType= (ArrayType) getChildNode(node, ASTNodeConstants.TYPE);
		
		// get the element type and count dimensions
		Type elementType= (Type) getChildNode(arrayType, ASTNodeConstants.COMPONENT_TYPE);
		int dimensions= 1; // always include this array type
		while (elementType.isArrayType()) {
			dimensions++;
			elementType = (Type) getChildNode(elementType, ASTNodeConstants.COMPONENT_TYPE);
		}
		
		elementType.accept(this);
		
		List list= getChildList(node, ASTNodeConstants.DIMENSIONS);
		for (int i= 0; i < list.size(); i++) {
			fResult.append('[');
			((ASTNode) list.get(i)).accept(this);
			fResult.append(']');
			dimensions--;
		}
		
		// add empty "[]" for each extra array dimension
		for (int i= 0; i < dimensions; i++) {
			fResult.append("[]"); //$NON-NLS-1$
		}
		ASTNode initializer= getChildNode(node, ASTNodeConstants.INITIALIZER);
		if (initializer != null) {
			fResult.append('=');
			getChildNode(node, ASTNodeConstants.INITIALIZER).accept(this);
		}
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		fResult.append('{');
		visitList(node, ASTNodeConstants.EXPRESSIONS, String.valueOf(','));
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		getChildNode(node, ASTNodeConstants.COMPONENT_TYPE).accept(this);
		fResult.append("[]"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		fResult.append("assert "); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		
		ASTNode message= getChildNode(node, ASTNodeConstants.MESSAGE);
		if (message != null) {
			fResult.append(':');
			message.accept(this);
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		getChildNode(node, ASTNodeConstants.LEFT_HAND_SIDE).accept(this);
		fResult.append(getAttribute(node, ASTNodeConstants.OPERATOR).toString());
		getChildNode(node, ASTNodeConstants.RIGHT_HAND_SIDE).accept(this);
		return false;
	}



	/*
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		fResult.append('{');
		visitList(node, ASTNodeConstants.STATEMENTS, null);
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
		ASTNode label= getChildNode(node, ASTNodeConstants.LABEL);
		if (label != null) {
			fResult.append(' ');
			label.accept(this);
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CastExpression)
	 */
	public boolean visit(CastExpression node) {
		fResult.append('(');
		getChildNode(node, ASTNodeConstants.TYPE).accept(this);
		fResult.append(')');
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		fResult.append("catch ("); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.EXCEPTION).accept(this);
		fResult.append(')');
		getChildNode(node, ASTNodeConstants.BODY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		fResult.append(getAttribute(node, ASTNodeConstants.ESCAPED_VALUE));
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		ASTNode expression= getChildNode(node, ASTNodeConstants.EXPRESSION);
		if (expression != null) {
			expression.accept(this);
			fResult.append('.');
		}
		fResult.append("new "); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		fResult.append('(');
		visitList(node, ASTNodeConstants.ARGUMENTS, String.valueOf(','));
		fResult.append(')');
		ASTNode decl= getChildNode(node, ASTNodeConstants.ANONYMOUS_CLASS_DECLARATION);
		if (decl != null) {
			decl.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CompilationUnit)
	 */
	public boolean visit(CompilationUnit node) {
		ASTNode pack= getChildNode(node, ASTNodeConstants.PACKAGE);
		if (pack != null) {
			pack.accept(this);
		}
		visitList(node, ASTNodeConstants.IMPORTS, null);
		visitList(node, ASTNodeConstants.TYPES, null);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append('?');
		getChildNode(node, ASTNodeConstants.THEN_EXPRESSION).accept(this);
		fResult.append(':');
		getChildNode(node, ASTNodeConstants.ELSE_EXPRESSION).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		fResult.append("this("); //$NON-NLS-1$
		visitList(node, ASTNodeConstants.ARGUMENTS, String.valueOf(','));
		fResult.append(");"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		fResult.append("continue"); //$NON-NLS-1$
		ASTNode label= getChildNode(node, ASTNodeConstants.LABEL);
		if (label != null) {
			fResult.append(' ');
			label.accept(this);
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		fResult.append("do "); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.BODY).accept(this);
		fResult.append(" while ("); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
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
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append('.');
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		ASTNode javadoc= getChildNode(node, ASTNodeConstants.JAVADOC);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		printModifiers(getIntAttribute(node, ASTNodeConstants.MODIFIERS), fResult);
		getChildNode(node, ASTNodeConstants.TYPE).accept(this);
		fResult.append(' ');
		visitList(node, ASTNodeConstants.FRAGMENTS, String.valueOf(','));
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		fResult.append("for ("); //$NON-NLS-1$
		visitList(node, ASTNodeConstants.INITIALIZERS, null);
		fResult.append(';');
		ASTNode expression= getChildNode(node, ASTNodeConstants.EXPRESSION);
		if (expression != null) {
			expression.accept(this);
		}
		fResult.append(';');
		visitList(node, ASTNodeConstants.UPDATERS, null);
		fResult.append(')');
		getChildNode(node, ASTNodeConstants.BODY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		fResult.append("if ("); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append(')');
		getChildNode(node, ASTNodeConstants.THEN_STATEMENT).accept(this);
		ASTNode elseStatement= getChildNode(node, ASTNodeConstants.ELSE_STATEMENT);
		if (elseStatement != null) {
			fResult.append(" else "); //$NON-NLS-1$
			elseStatement.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		fResult.append("import "); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		if (getBooleanAttribute(node, ASTNodeConstants.IS_ON_DEMAND)) {
			fResult.append(".*"); //$NON-NLS-1$
		}
		fResult.append(';');
		return false;
	}



	/*
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		getChildNode(node, ASTNodeConstants.LEFT_OPERAND).accept(this);
		fResult.append(' ');
		String operator= getAttribute(node, ASTNodeConstants.OPERATOR).toString();
		
		fResult.append(operator);
		fResult.append(' ');
		getChildNode(node, ASTNodeConstants.RIGHT_OPERAND).accept(this);
		
		List list= getChildList(node, ASTNodeConstants.EXTENDED_OPERANDS);
		for (int i= 0; i < list.size(); i++) {
			fResult.append(operator);
			((ASTNode) list.get(i)).accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		getChildNode(node, ASTNodeConstants.LEFT_OPERAND).accept(this);
		fResult.append(" instanceof "); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.RIGHT_OPERAND).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		ASTNode javadoc= getChildNode(node, ASTNodeConstants.JAVADOC);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		printModifiers(getIntAttribute(node, ASTNodeConstants.MODIFIERS), fResult);
		getChildNode(node, ASTNodeConstants.BODY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(Javadoc node) {
		fResult.append("/**"); //$NON-NLS-1$
		List list= getChildList(node, ASTNodeConstants.TAGS);
		for (int i= 0; i < list.size(); i++) {
			fResult.append("\n * "); //$NON-NLS-1$
			((ASTNode) list.get(i)).accept(this);
		}
		fResult.append("\n */"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		getChildNode(node, ASTNodeConstants.LABEL).accept(this);
		fResult.append(": "); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.BODY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		ASTNode javadoc= getChildNode(node, ASTNodeConstants.JAVADOC);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		printModifiers(getIntAttribute(node, ASTNodeConstants.MODIFIERS), fResult);
		if (!getBooleanAttribute(node, ASTNodeConstants.IS_CONSTRUCTOR)) {
			getChildNode(node, ASTNodeConstants.RETURN_TYPE).accept(this);
			fResult.append(' ');
		}
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		fResult.append('(');
		visitList(node, ASTNodeConstants.PARAMETERS, String.valueOf(','));
		fResult.append(')');
		int extraDims= getIntAttribute(node, ASTNodeConstants.EXTRA_DIMENSIONS);
		for (int i = 0; i < extraDims; i++) {
			fResult.append("[]"); //$NON-NLS-1$
		}		
		visitList(node, ASTNodeConstants.THROWN_EXCEPTIONS, String.valueOf(','), " throws "); //$NON-NLS-1$
		ASTNode body= getChildNode(node, ASTNodeConstants.BODY);
		if (body == null) {
			fResult.append(';');
		} else {
			body.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		ASTNode expression= getChildNode(node, ASTNodeConstants.EXPRESSION);
		if (expression != null) {
			expression.accept(this);
			fResult.append('.');
		}
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		fResult.append('(');
		visitList(node, ASTNodeConstants.ARGUMENTS, String.valueOf(','));
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
		fResult.append(getAttribute(node, ASTNodeConstants.TOKEN).toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		fResult.append("package "); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		fResult.append('(');
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append(')');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		getChildNode(node, ASTNodeConstants.OPERAND).accept(this);
		fResult.append(getAttribute(node, ASTNodeConstants.OPERATOR).toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		fResult.append(getAttribute(node, ASTNodeConstants.OPERATOR).toString());
		getChildNode(node, ASTNodeConstants.OPERAND).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		fResult.append(getAttribute(node, ASTNodeConstants.PRIMITIVE_TYPE_CODE).toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		getChildNode(node, ASTNodeConstants.QUALIFIER).accept(this);
		fResult.append('.');
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		fResult.append("return"); //$NON-NLS-1$
		ASTNode expression= getChildNode(node, ASTNodeConstants.EXPRESSION);
		if (expression != null) {
			fResult.append(' ');
			expression.accept(this);
		}
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		fResult.append(getAttribute(node, ASTNodeConstants.IDENTIFIER));
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
		printModifiers(getIntAttribute(node, ASTNodeConstants.MODIFIERS), fResult);
		getChildNode(node, ASTNodeConstants.TYPE).accept(this);
		fResult.append(' ');
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		int extraDimensions= getIntAttribute(node, ASTNodeConstants.EXTRA_DIMENSIONS);
		for (int i = 0; i < extraDimensions; i++) {
			fResult.append("[]"); //$NON-NLS-1$
		}			
		ASTNode initializer= getChildNode(node, ASTNodeConstants.INITIALIZER);
		if (initializer != null) {
			fResult.append('=');
			initializer.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		fResult.append(getAttribute(node, ASTNodeConstants.ESCAPED_VALUE));
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		ASTNode expression= getChildNode(node, ASTNodeConstants.EXPRESSION);
		if (expression != null) {
			expression.accept(this);
			fResult.append('.');
		}
		fResult.append("super("); //$NON-NLS-1$
		visitList(node, ASTNodeConstants.ARGUMENTS, String.valueOf(','));
		fResult.append(");"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		ASTNode qualifier= getChildNode(node, ASTNodeConstants.QUALIFIER);
		if (qualifier != null) {
			qualifier.accept(this);
			fResult.append('.');
		}
		fResult.append("super."); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		ASTNode qualifier= getChildNode(node, ASTNodeConstants.QUALIFIER);
		if (qualifier != null) {
			qualifier.accept(this);
			fResult.append('.');
		}
		fResult.append("super."); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		fResult.append('(');
		visitList(node, ASTNodeConstants.ARGUMENTS, String.valueOf(','));
		fResult.append(')');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		ASTNode expression= getChildNode(node, ASTNodeConstants.EXPRESSION);
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
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append(')');
		fResult.append('{');
		visitList(node, ASTNodeConstants.STATEMENTS, null);
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SynchronizedStatement)
	 */
	public boolean visit(SynchronizedStatement node) {
		fResult.append("synchronized ("); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append(')');
		getChildNode(node, ASTNodeConstants.BODY).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		ASTNode qualifier= getChildNode(node, ASTNodeConstants.QUALIFIER);
		if (qualifier != null) {
			qualifier.accept(this);
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
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		fResult.append("try "); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.BODY).accept(this);
		fResult.append(' ');
		visitList(node, ASTNodeConstants.CATCH_CLAUSES, null);
		ASTNode finallyClause= getChildNode(node, ASTNodeConstants.FINALLY);
		if (finallyClause != null) {
			fResult.append(" finally "); //$NON-NLS-1$
			finallyClause.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		ASTNode javadoc= getChildNode(node, ASTNodeConstants.JAVADOC);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		printModifiers(getIntAttribute(node, ASTNodeConstants.MODIFIERS), fResult);
		
		boolean isInterface= getBooleanAttribute(node, ASTNodeConstants.IS_INTERFACE);
		fResult.append(isInterface ? "interface " : "class "); //$NON-NLS-1$ //$NON-NLS-2$
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		fResult.append(' ');
		ASTNode superclass= getChildNode(node, ASTNodeConstants.SUPERCLASS);
		if (superclass != null) {
			fResult.append("extends "); //$NON-NLS-1$
			superclass.accept(this);
			fResult.append(' ');
		}
		
		String lead= isInterface ? "extends " : "implements ";  //$NON-NLS-1$//$NON-NLS-2$
		visitList(node, ASTNodeConstants.SUPER_INTERFACES, String.valueOf(','), lead);
		fResult.append('{');
		visitList(node, ASTNodeConstants.BODY_DECLARATIONS, null);
		fResult.append('}');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		getChildNode(node, ASTNodeConstants.TYPE_DECLARATION).accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		getChildNode(node, ASTNodeConstants.TYPE).accept(this);
		fResult.append(".class"); //$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		printModifiers(getIntAttribute(node, ASTNodeConstants.MODIFIERS), fResult);
		getChildNode(node, ASTNodeConstants.TYPE).accept(this);
		fResult.append(' ');
		visitList(node, ASTNodeConstants.FRAGMENTS, String.valueOf(','));
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		int extraDimensions= getIntAttribute(node, ASTNodeConstants.EXTRA_DIMENSIONS);
		for (int i = 0; i < extraDimensions; i++) {
			fResult.append("[]"); //$NON-NLS-1$
		}
		ASTNode initializer= getChildNode(node, ASTNodeConstants.INITIALIZER);
		if (initializer != null) {
			fResult.append('=');
			initializer.accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		printModifiers(getIntAttribute(node, ASTNodeConstants.MODIFIERS), fResult);
		getChildNode(node, ASTNodeConstants.TYPE).accept(this);
		fResult.append(' ');
		visitList(node, ASTNodeConstants.FRAGMENTS, String.valueOf(','));
		fResult.append(';');
		return false;
	}

	/*
	 * @see ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		fResult.append("while ("); //$NON-NLS-1$
		getChildNode(node, ASTNodeConstants.EXPRESSION).accept(this);
		fResult.append(')');
		getChildNode(node, ASTNodeConstants.BODY).accept(this);
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.BlockComment)
	 */
	public boolean visit(BlockComment node) {
		return false; // cant flatten, needs source
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.LineComment)
	 */
	public boolean visit(LineComment node) {
		return false; // cant flatten, needs source
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MemberRef)
	 */
	public boolean visit(MemberRef node) {
		ASTNode qualifier= getChildNode(node, ASTNodeConstants.QUALIFIER);
		if (qualifier != null) {
			qualifier.accept(this);
		}
		fResult.append('#');
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodRef)
	 */
	public boolean visit(MethodRef node) {
		ASTNode qualifier= getChildNode(node, ASTNodeConstants.QUALIFIER);
		if (qualifier != null) {
			qualifier.accept(this);
		}
		fResult.append('#');
		getChildNode(node, ASTNodeConstants.NAME).accept(this);
		fResult.append('(');
		visitList(node, ASTNodeConstants.PARAMETERS, ","); //$NON-NLS-1$
		fResult.append(')');
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodRefParameter)
	 */
	public boolean visit(MethodRefParameter node) {
		getChildNode(node, ASTNodeConstants.TYPE).accept(this);
		ASTNode name= getChildNode(node, ASTNodeConstants.NAME);
		if (name != null) {
			fResult.append(' ');
			name.accept(this);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TagElement)
	 */
	public boolean visit(TagElement node) {
		Object tagName= getAttribute(node, ASTNodeConstants.TAG_NAME);
		if (tagName != null) {
			fResult.append((String) tagName);
		}
		List list= getChildList(node, ASTNodeConstants.FRAGMENTS);
		for (int i= 0; i < list.size(); i++) {
			if (i > 0 || tagName != null) {
				fResult.append(' ');
			}
			ASTNode curr= (ASTNode) list.get(i);
			if (curr instanceof TagElement) {
				fResult.append('{');
				curr.accept(this);
				fResult.append('}');
			} else {
				curr.accept(this);
			}
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TextElement)
	 */
	public boolean visit(TextElement node) {
		fResult.append(getAttribute(node, ASTNodeConstants.TEXT));
		return false;
	}

}
