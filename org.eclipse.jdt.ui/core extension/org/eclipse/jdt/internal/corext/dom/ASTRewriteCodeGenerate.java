/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

/* package */ class ASTRewriteCodeGenerate extends ASTNode2String {

	private static class ExistingNode {
		public int start;
		public int last;
		public ASTNode node;
	}

	private ArrayList fExistingNodes;

	/* package */ public ASTRewriteCodeGenerate() {
		super();
	}
	
	public String generateFormatted(ASTNode node, TextBuffer existingSource, int initialIndentationLevel) {
		fExistingNodes= new ArrayList(5);
		
		node.accept(this);
		
		int nExistingNodes= fExistingNodes.size();
		int[] positions= new int[nExistingNodes*2];
		for (int i= 0; i < nExistingNodes; i++) {
			ExistingNode elem= (ExistingNode) fExistingNodes.get(i);
			positions[2*i]= elem.start;
			positions[2*i + 1]= elem.last;
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
			int existingStartPos= positions[2*i];
			int existingEndPos= positions[2*i + 1] + 1;
			ASTNode existingNode= elem.node;
			
			buf.append(formatted.substring(currPos, existingStartPos));
			
			int nodeStartLine= existingSource.getLineOfOffset(existingNode.getStartPosition());
			int nodeIndent= existingSource.getLineIndent(nodeStartLine, tabWidth);
			String nodeContent= existingSource.getContent(existingNode.getStartPosition(), existingNode.getLength());
			
			String currLine= getCurrentLine(buf, buf.length());
			int currIndent= Strings.computeIndent(currLine, tabWidth);
			
			if (nodeIndent != currIndent) {
				String[] lines= Strings.convertIntoLines(nodeContent);
				String indentString= Strings.getIndentString(currLine, tabWidth);
				for (int k= 0; k < lines.length; k++) {
					if (k > 0) {
						buf.append(lineDelimiter);
						buf.append(indentString); // no indent for first line (contained in the formatted string)
						buf.append(Strings.trimIndent(lines[k], nodeIndent, tabWidth));
					} else {
						buf.append(lines[k]);
					}
				}
			} else {
				buf.append(nodeContent);
			}			
			currPos= existingEndPos;
		}
		buf.append(formatted.substring(currPos, formatted.length()));
		String res= buf.toString();
		
		fExistingNodes= null;
		fResult.setLength(0);
		
		return res;
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
	
	private ASTNode getExisting(ASTNode node) {
		if (fExistingNodes != null) {
			if (node.getStartPosition() != -1) {
				return node;
			} else if (ASTRewriteAnalyzer.isInsertNodeForExisting(node)) {
				return ASTRewriteAnalyzer.getModifiedNode(node);
			}
		}
		return null;
	}
	
	private void appendPlaceholder(ASTNode node, String placeHolder) {
		int currPos= fResult.length();
		fResult.append(placeHolder);
		
		ExistingNode existingNode= new ExistingNode();
		existingNode.start= currPos;
		existingNode.last= fResult.length() - 1;
		existingNode.node= node;
		fExistingNodes.add(existingNode);
	}
	
	private boolean preserveExisting(ASTNode node, String placeHolder) {
		ASTNode existing= getExisting(node);
		if (existing != null) {
			appendPlaceholder(existing, placeHolder);
			return true;
		}
		return false;
	}
	
	private boolean preserveExisting(Expression node) {
		return preserveExisting(node, "z"); // represent as a variable reference.
	}
	
	private boolean preserveExisting(Block node) { // special rule for block (=statement)
		return preserveExisting(node, "{}");
	}
	
	private boolean preserveExisting(Statement node) {
		return preserveExisting(node, "z;");
	}
	
	private boolean preserveExisting(BodyDeclaration node) {
		return preserveExisting(node, "int i;"); // represent as field declaration
	}
	
	private boolean preserveExisting(ASTNode node) {
		if (getExisting(node) != null) {
			System.out.println("Tree contains existing node of type: " + node.getClass().getName());
		}
		return false;
	}		
	
	protected boolean visitNode(ASTNode node) {
		Assert.isTrue(false, "No implementation to flatten node: " + node.toString()); //$NON-NLS-1$
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(CastExpression)
	 */
	public boolean visit(CastExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(CompilationUnit)
	 */
	public boolean visit(CompilationUnit node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(Javadoc node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SingleVariableDeclaration)
	 */
	public boolean visit(SingleVariableDeclaration node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(SynchronizedStatement)
	 */
	public boolean visit(SynchronizedStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}

	/*
	 * @see ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		if (preserveExisting(node)) {
			return false;
		}
		return super.visit(node);
	}	
}
