/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

/* package */ class ASTNode2String extends GenericVisitor {

	private static class ExistingNode {
		public int start;
		public int len;
		public ASTNode node;
	}

	private StringBuffer fResult;
	private ArrayList fExistingNodes;

	/* package */ ASTNode2String() {
		fResult= new StringBuffer();
	}
	
	public String generateSimple(ASTNode node) {
		fExistingNodes= null;
		fResult.setLength(0);
		node.accept(this);
		return fResult.toString();			
	}
	
	public String generateFormatted(ASTNode node, TextBuffer existingSource, int initialIndentationLevel) {
		fExistingNodes= new ArrayList(5);
		
		fResult.setLength(0);
		node.accept(this);
		
		int nExistingNodes= fExistingNodes.size();
		int[] positions= new int[nExistingNodes*2];
		for (int i= 0; i < nExistingNodes; i++) {
			ExistingNode elem= (ExistingNode) fExistingNodes.get(i);
			positions[2*i]= elem.start;
			positions[2*i + 1]= elem.start + elem.len;
		}
		
		String lineDelimiter= existingSource.getLineDelimiter();
		
		String sourceString= fResult.toString();
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(null);
		String formatted= formatter.format(sourceString, initialIndentationLevel, positions, lineDelimiter);		
		
		int tabWidth= CodeFormatterUtil.getTabWidth();
		StringBuffer buf= new StringBuffer();
		int currPos= 0;
		for (int i= 0; i < nExistingNodes; i++) {
			ExistingNode elem= (ExistingNode) fExistingNodes.get(i);
			int startPos= positions[2*i];
			int endPos= positions[2*i + 1];
			
			buf.append(formatted.substring(currPos, startPos));
			
			int nodeStartLine= existingSource.getLineOfOffset(node.getStartPosition());
			int nodeIndent= existingSource.getLineIndent(nodeStartLine, tabWidth);
			String nodeContent= existingSource.getContent(node.getStartPosition(), node.getLength());
			
			String currLine= getCurrentLine(buf, buf.length());
			int currIndent= Strings.computeIndent(currLine, tabWidth);
			
			if (nodeIndent != currIndent) {
				String[] lines= Strings.convertIntoLines(nodeContent);
				Strings.trimIndentation(lines, tabWidth);
				String indentString= Strings.getIndentString(currLine, tabWidth);
				for (int k= 0; k < lines.length; k++) {
					buf.append(indentString);
					buf.append(lines[k]);
					buf.append(lineDelimiter);
				}
			} else {
				buf.append(nodeContent);
			}			
			currPos= endPos;
		}
		buf.append(formatted.substring(currPos, formatted.length()));
		
		fExistingNodes= null;
		return buf.toString();
	}

	private String getCurrentLine(StringBuffer buf, int pos) {
		for (int i= pos - 1; i>= 0; i--) {
			char ch= buf.charAt(i);
			if (ch == '\n' || ch == '\r') {
				return buf.substring(i + 1, pos);
			}
		}
		return buf.toString();
	}

	protected boolean visitNode(ASTNode node) {
		Assert.isTrue(false, "No implementation to flatten node: " + node.toString()); //$NON-NLS-1$
		return false;
	}

	/**
	 * Appends the text representation of the given modifier flags, followed by a single space.
	 * 
	 * @param modifiers the modifiers
	 */
	public static void printModifiers(int modifiers, StringBuffer buf) {
		if (Modifier.isPublic(modifiers)) {
			buf.append("public ");//$NON-NLS-1$
		}
		if (Modifier.isProtected(modifiers)) {
			buf.append("protected ");//$NON-NLS-1$
		}
		if (Modifier.isPrivate(modifiers)) {
			buf.append("private ");//$NON-NLS-1$
		}
		if (Modifier.isStatic(modifiers)) {
			buf.append("static ");//$NON-NLS-1$
		}
		if (Modifier.isAbstract(modifiers)) {
			buf.append("abstract ");//$NON-NLS-1$
		}
		if (Modifier.isFinal(modifiers)) {
			buf.append("final ");//$NON-NLS-1$
		}
		if (Modifier.isSynchronized(modifiers)) {
			buf.append("synchronized ");//$NON-NLS-1$
		}
		if (Modifier.isVolatile(modifiers)) {
			buf.append("volatile ");//$NON-NLS-1$
		}
		if (Modifier.isNative(modifiers)) {
			buf.append("native ");//$NON-NLS-1$
		}
		if (Modifier.isStrictfp(modifiers)) {
			buf.append("strictfp ");//$NON-NLS-1$
		}
		if (Modifier.isTransient(modifiers)) {
			buf.append("transient ");//$NON-NLS-1$
		}
	}
	
	private boolean isExisting(ASTNode node) {
		return fExistingNodes != null && node.getStartPosition() != -1;
	}
	
	private void appendPlaceholder(ASTNode node, String placeHolder) {
		ExistingNode existingNode= new ExistingNode();
		existingNode.start= fResult.length();
		existingNode.len= placeHolder.length();
		existingNode.node= node;
		fExistingNodes.add(existingNode);
		
		fResult.append(placeHolder);
	}		
	
	private void preserve(Expression node) {
		appendPlaceholder(node, "z");
	}

	private void preserve(Statement node) {
		appendPlaceholder(node, "z;");
	}
	
	private void preserve(BodyDeclaration node) {
		appendPlaceholder(node, "int i;");
	}		
	
	/*
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		fResult.append("{");//$NON-NLS-1$
		for (Iterator it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
			BodyDeclaration b = (BodyDeclaration) it.next();
			b.accept(this);
		}
		fResult.append("}");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}
		node.getArray().accept(this);
		fResult.append("[");//$NON-NLS-1$
		node.getIndex().accept(this);
		fResult.append("]");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}	
		fResult.append("new ");//$NON-NLS-1$
		ArrayType at = node.getType();
		int dims = at.getDimensions();
		Type elementType = at.getElementType();
		elementType.accept(this);
		for (Iterator it = node.dimensions().iterator(); it.hasNext(); ) {
			fResult.append("[");//$NON-NLS-1$
			Expression e = (Expression) it.next();
			e.accept(this);
			fResult.append("]");//$NON-NLS-1$
			dims--;
		}
		// add empty "[]" for each extra array dimension
		for (int i= 0; i < dims; i++) {
			fResult.append("[]");//$NON-NLS-1$
		}
		if (node.getInitializer() != null) {
			fResult.append("=");//$NON-NLS-1$
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}
		fResult.append("{");//$NON-NLS-1$
		for (Iterator it = node.expressions().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			fResult.append(",");//$NON-NLS-1$
		}
		fResult.append("}");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		node.getComponentType().accept(this);
		fResult.append("[]");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		fResult.append("assert ");//$NON-NLS-1$
		node.getExpression().accept(this);
		if (node.getMessage() != null) {
			fResult.append(" : ");//$NON-NLS-1$
			node.getMessage().accept(this);
		}
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		node.getLeftHandSide().accept(this);
		fResult.append(node.getOperator().toString());
		node.getRightHandSide().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		fResult.append("{");//$NON-NLS-1$
		for (Iterator it = node.statements().iterator(); it.hasNext(); ) {
			Statement s = (Statement) it.next();
			s.accept(this);
		}
		fResult.append("}");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		if (node.booleanValue() == true) {
			fResult.append("true");//$NON-NLS-1$
		} else {
			fResult.append("false");//$NON-NLS-1$
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		fResult.append("break");//$NON-NLS-1$
		if (node.getLabel() != null) {
			fResult.append(" ");//$NON-NLS-1$
			node.getLabel().accept(this);
		}
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CastExpression)
	 */
	public boolean visit(CastExpression node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		fResult.append("(");//$NON-NLS-1$
		node.getType().accept(this);
		fResult.append(")");//$NON-NLS-1$
		node.getExpression().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		fResult.append("catch (");//$NON-NLS-1$
		node.getException().accept(this);
		fResult.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append(node.getEscapedValue());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			fResult.append(".");//$NON-NLS-1$
		}
		fResult.append("new ");//$NON-NLS-1$
		node.getName().accept(this);
		fResult.append("(");//$NON-NLS-1$
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(",");//$NON-NLS-1$
			}
		}
		fResult.append(")");//$NON-NLS-1$
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
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		node.getExpression().accept(this);
		fResult.append("?");//$NON-NLS-1$
		node.getThenExpression().accept(this);
		fResult.append(":");//$NON-NLS-1$
		node.getElseExpression().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("this(");//$NON-NLS-1$
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(",");//$NON-NLS-1$
			}
		}
		fResult.append(");");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("continue");//$NON-NLS-1$
		if (node.getLabel() != null) {
			fResult.append(" ");//$NON-NLS-1$
			node.getLabel().accept(this);
		}
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("do ");//$NON-NLS-1$
		node.getBody().accept(this);
		fResult.append(" while (");//$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(");");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		node.getExpression().accept(this);
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}				
		node.getExpression().accept(this);
		fResult.append(".");//$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}				
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		printModifiers(node.getModifiers(), fResult);
		node.getType().accept(this);
		fResult.append(" ");//$NON-NLS-1$
		for (Iterator it = node.fragments().iterator(); it.hasNext(); ) {
			VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				fResult.append(", ");//$NON-NLS-1$
			}
		}
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("for (");//$NON-NLS-1$
		for (Iterator it = node.initializers().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
		}
		fResult.append("; ");//$NON-NLS-1$
		for (Iterator it = node.updaters().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
		}
		fResult.append("; ");//$NON-NLS-1$
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
		}
		fResult.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}	
		fResult.append("if (");//$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(") ");//$NON-NLS-1$
		node.getThenStatement().accept(this);
		if (node.getElseStatement() != null) {
			fResult.append(" else ");//$NON-NLS-1$
			node.getElseStatement().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		fResult.append("import ");//$NON-NLS-1$
		node.getName().accept(this);
		if (node.isOnDemand()) {
			fResult.append(".*");//$NON-NLS-1$
		}
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		node.getLeftOperand().accept(this);
		fResult.append(" ");//$NON-NLS-1$
		fResult.append(node.getOperator().toString());
		fResult.append(" ");//$NON-NLS-1$
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
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		node.getLeftOperand().accept(this);
		fResult.append(" instanceof ");//$NON-NLS-1$
		node.getRightOperand().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
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
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		node.getLabel().accept(this);
		fResult.append(": ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		printModifiers(node.getModifiers(), fResult);
		if (!node.isConstructor()) {
			node.getReturnType().accept(this);
			fResult.append(" ");//$NON-NLS-1$
		}
		node.getName().accept(this);
		fResult.append("(");//$NON-NLS-1$
		for (Iterator it = node.parameters().iterator(); it.hasNext(); ) {
			SingleVariableDeclaration v = (SingleVariableDeclaration) it.next();
			v.accept(this);
			if (it.hasNext()) {
				fResult.append(",");//$NON-NLS-1$
			}
		}
		fResult.append(")");//$NON-NLS-1$
		if (!node.thrownExceptions().isEmpty()) {
			fResult.append(" throws ");//$NON-NLS-1$
			for (Iterator it = node.thrownExceptions().iterator(); it.hasNext(); ) {
				Name n = (Name) it.next();
				n.accept(this);
				if (it.hasNext()) {
					fResult.append(", ");//$NON-NLS-1$
				}
			}
			fResult.append(" ");//$NON-NLS-1$
		}
		if (node.getBody() == null) {
			fResult.append(";");//$NON-NLS-1$
		} else {
			node.getBody().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			fResult.append(".");//$NON-NLS-1$
		}
		node.getName().accept(this);
		fResult.append("(");//$NON-NLS-1$
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(",");//$NON-NLS-1$
			}
		}
		fResult.append(")");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("null");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append(node.getToken());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		fResult.append("package ");//$NON-NLS-1$
		node.getName().accept(this);
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("(");//$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(")");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		node.getOperand().accept(this);
		fResult.append(node.getOperator().toString());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
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
		fResult.append(".");//$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		fResult.append("return");//$NON-NLS-1$
		if (node.getExpression() != null) {
			fResult.append(" ");//$NON-NLS-1$
			node.getExpression().accept(this);
		}
		fResult.append(";");//$NON-NLS-1$
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
		fResult.append(" ");//$NON-NLS-1$
		node.getName().accept(this);
		if (node.getInitializer() != null) {
			fResult.append("=");//$NON-NLS-1$
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append(node.getEscapedValue());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			fResult.append(".");//$NON-NLS-1$
		}
		fResult.append("super(");//$NON-NLS-1$
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(",");//$NON-NLS-1$
			}
		}
		fResult.append(");");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			fResult.append(".");//$NON-NLS-1$
		}
		fResult.append("super.");//$NON-NLS-1$
		node.getName().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			fResult.append(".");//$NON-NLS-1$
		}
		fResult.append("super.");//$NON-NLS-1$
		node.getName().accept(this);
		fResult.append("(");//$NON-NLS-1$
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
			if (it.hasNext()) {
				fResult.append(",");//$NON-NLS-1$
			}
		}
		fResult.append(")");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		fResult.append("case ");//$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(": ");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("switch (");//$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(") ");//$NON-NLS-1$
		fResult.append("{");//$NON-NLS-1$
		for (Iterator it = node.statements().iterator(); it.hasNext(); ) {
			Statement s = (Statement) it.next();
			s.accept(this);
		}
		fResult.append("}");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SynchronizedStatement)
	 */
	public boolean visit(SynchronizedStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("synchronized (");//$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		if (node.getQualifier() != null) {
			node.getQualifier().accept(this);
			fResult.append(".");//$NON-NLS-1$
		}
		fResult.append("this");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("throw ");//$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		fResult.append("try ");//$NON-NLS-1$
		node.getBody().accept(this);
		fResult.append(" ");//$NON-NLS-1$
		for (Iterator it = node.catchClauses().iterator(); it.hasNext(); ) {
			CatchClause cc = (CatchClause) it.next();
			cc.accept(this);
		}
		if (node.getFinally() != null) {
			node.getFinally().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}			
		if (node.getJavadoc() != null) {
			node.getJavadoc().accept(this);
		}
		printModifiers(node.getModifiers(), fResult);
		fResult.append(node.isInterface() ? "interface " : "class ");//$NON-NLS-2$//$NON-NLS-1$
		node.getName().accept(this);
		fResult.append(" ");//$NON-NLS-1$
		if (!node.isInterface() && node.getSuperclass() != null) {
			fResult.append("extends ");//$NON-NLS-1$
			node.getSuperclass().accept(this);
			fResult.append(" ");//$NON-NLS-1$
		}
		if (!node.superInterfaces().isEmpty()) {
			fResult.append(node.isInterface() ? "extends " : "implements ");//$NON-NLS-2$//$NON-NLS-1$
			for (Iterator it = node.superInterfaces().iterator(); it.hasNext(); ) {
				Name n = (Name) it.next();
				n.accept(this);
				if (it.hasNext()) {
					fResult.append(", ");//$NON-NLS-1$
				}
			}
			fResult.append(" ");//$NON-NLS-1$
		}
		fResult.append("{");//$NON-NLS-1$
		for (Iterator it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
			BodyDeclaration d = (BodyDeclaration) it.next();
			d.accept(this);
		}
		fResult.append("}");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}		
		node.getTypeDeclaration().accept(this);
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}
		node.getType().accept(this);
		fResult.append(".class");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}
		printModifiers(node.getModifiers(), fResult);
		node.getType().accept(this);
		fResult.append(" ");//$NON-NLS-1$
		for (Iterator it = node.fragments().iterator(); it.hasNext(); ) {
			VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				fResult.append(", ");//$NON-NLS-1$
			}
		}
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		node.getName().accept(this);
		for (int i = 0; i < node.getExtraDimensions(); i++) {
			fResult.append("[]");//$NON-NLS-1$
		}
		if (node.getInitializer() != null) {
			fResult.append("=");//$NON-NLS-1$
			node.getInitializer().accept(this);
		}
		return false;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}
		printModifiers(node.getModifiers(), fResult);
		node.getType().accept(this);
		fResult.append(" ");//$NON-NLS-1$
		for (Iterator it = node.fragments().iterator(); it.hasNext(); ) {
			VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
			f.accept(this);
			if (it.hasNext()) {
				fResult.append(", ");//$NON-NLS-1$
			}
		}
		fResult.append(";");//$NON-NLS-1$
		return false;
	}

	/*
	 * @see ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		if (isExisting(node)) {
			preserve(node);
			return false;
		}
		fResult.append("while (");//$NON-NLS-1$
		node.getExpression().accept(this);
		fResult.append(") ");//$NON-NLS-1$
		node.getBody().accept(this);
		return false;
	}	
}
