/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Add first version of ReplceQualifiedTypeFixCore
 *     Carsten Hammer (Copilot assisted)- Contributed with LoopAnalyzer and parts of the code for prepareForEach
 */
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class EnhancedForLoopToForEachFixCore extends CompilationUnitRewriteOperationsFixCore {

	public EnhancedForLoopToForEachFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static EnhancedForLoopToForEachFixCore createReplaceEnhancedLoop(CompilationUnit compilationUnit, EnhancedForStatement forStatement) {

		LoopBodyAnalyzer analyzer = new LoopBodyAnalyzer();
		Statement body = forStatement.getBody();
		body.accept(analyzer);
		boolean hasUnconvertiblePatterns = analyzer.hasBreak()
                || analyzer.hasLabeledContinue()
                || analyzer.hasTryCatch()
                || analyzer.hasSynchronized()
                || analyzer.hasNestedLoop()
                || analyzer.hasVoidReturn();

		if (hasUnconvertiblePatterns) {
			return null;
		}

		SimpleName loopItemName = forStatement.getParameter() == null ? null : forStatement.getParameter().getName();
		Expression expression = forStatement.getExpression() == null ? null : forStatement.getExpression();

		SimpleName expressionName = expression instanceof SimpleName ? (SimpleName) expression : null;
		if (loopItemName == null || expressionName == null) {
			// We can't create a forEach because either the expression name or the loop item name is missing.
			return null;
		}

		ITypeBinding binding = expressionName.resolveTypeBinding();

		if (binding == null || binding.isArray()) {
			return null;
		}

		ReplaceEnhancedForWithForEachRewriteOperation refwfeop = new ReplaceEnhancedForWithForEachRewriteOperation(forStatement, loopItemName, expressionName);

		String label = CorrectionMessages.QuickAssistProcessor_convert_enhanced_for_to_foreach;
		return new EnhancedForLoopToForEachFixCore(label, compilationUnit, refwfeop); //new EnhancedForLoopToForEachFixCore();
	}
  /**
   * Visitor to analyze loop body for truly unconvertible control flow.
   *
   * <p>Tracks patterns that CANNOT be converted to stream operations:
   * <ul>
   *   <li>break statements</li>
   *   <li>labeled continue statements</li>
   *   <li>try-catch statements (checked exceptions require Try-with-resources or explicit handling)</li>
   *   <li>synchronized statements (synchronization semantics differ in streams)</li>
   *   <li>traditional for loops (complex control flow)</li>
   *   <li>while loops (complex control flow)</li>
   *   <li>do-while loops (complex control flow)</li>
   * </ul>
   * </p>
   *
   * <p>Note: unlabeled continue, return, and add() calls are potentially
   * convertible as filter, match, and collect patterns respectively.
   * Those are handled by analyzeAndAddOperations().</p>
   */
  private static class LoopBodyAnalyzer extends ASTVisitor {
      private boolean hasBreak = false;
      private boolean hasLabeledContinue = false;
      private boolean hasTryCatch = false;
      private boolean hasSynchronized = false;
      private boolean hasNestedLoop = false;
      private boolean hasVoidReturn = false;

      @Override
      public boolean visit(BreakStatement node) {
          hasBreak = true;
          return false;
      }

      @Override
      public boolean visit(ContinueStatement node) {
          // Only labeled continue is truly unconvertible
          // Unlabeled continue can be converted to a negated filter
          if (node.getLabel() != null) {
              hasLabeledContinue = true;
          }
          return false;
      }

      @Override
      public boolean visit(ReturnStatement node) {
          // Void return exits the enclosing method, not the loop
          // This cannot be converted to stream operations
          if (node.getExpression() == null) {
              hasVoidReturn = true;
          }
          // Note: boolean returns (return true/false) are potentially convertible
          // as match patterns — handled separately in analyzeStatements()
          return false;
      }

      @Override
      public boolean visit(TryStatement node) {
          // Try-catch cannot be easily converted to stream operations
          // due to checked exception handling requirements
          hasTryCatch = true;
          return false;
      }

      @Override
      public boolean visit(SynchronizedStatement node) {
          // Synchronized blocks have different semantics in streams
          // (parallelStream vs sequential processing)
          hasSynchronized = true;
          return false;
      }

      @Override
      public boolean visit(ForStatement node) {
          // Traditional for loops inside the body → unconvertible
          hasNestedLoop = true;
          return false;
      }

      @Override
      public boolean visit(WhileStatement node) {
          // While loops inside the body → unconvertible
          hasNestedLoop = true;
          return false;
      }

      @Override
      public boolean visit(DoStatement node) {
          // Do-while loops inside the body → unconvertible
          hasNestedLoop = true;
          return false;
      }

      public boolean hasBreak() { return hasBreak; }
      public boolean hasLabeledContinue() { return hasLabeledContinue; }
      public boolean hasTryCatch() { return hasTryCatch; }
      public boolean hasSynchronized() { return hasSynchronized; }
      public boolean hasNestedLoop() { return hasNestedLoop; }
      public boolean hasVoidReturn() { return hasVoidReturn; }
  }

  public static class ReplaceEnhancedForWithForEachRewriteOperation extends CompilationUnitRewriteOperation {

	public static final String FOR_EACH_METHOD = "forEach";  //$NON-NLS-1$

	  EnhancedForStatement loopToModify;
	  String variableName;
	  String expressionName;

	  public ReplaceEnhancedForWithForEachRewriteOperation(EnhancedForStatement loopToModify, SimpleName varName, SimpleName exprName) {
		  this.loopToModify = loopToModify;
		  this.variableName = varName.getFullyQualifiedName();
		  this.expressionName = exprName.getFullyQualifiedName();
	  }

	  @Override
	  public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
		  AST ast = cuRewrite.getRoot().getAST();
		  TextEditGroup group= createTextEditGroup(CorrectionMessages.QuickAssistProcessor_convert_enhanced_for_to_foreach, cuRewrite);
		  ASTRewrite rewrite = cuRewrite.getASTRewrite();
	      Expression forEachExpression = prepareForEach(ast);
	      ExpressionStatement forEachexpressionStatement = ast.newExpressionStatement(forEachExpression);
	      ASTNodes.replaceButKeepComment(rewrite, loopToModify, forEachexpressionStatement, group);
	  }

	  public Expression prepareForEach(AST ast) {
		  // This method has been adapted from sandbox code: https://github.com/carstenartur/sandbox
		  MethodInvocation forEachCall = ast.newMethodInvocation();
		  forEachCall.setExpression(ast.newSimpleName(expressionName));
	      forEachCall.setName(ast.newSimpleName(FOR_EACH_METHOD));
	      LambdaExpression lambda = ast.newLambdaExpression();
	      VariableDeclarationFragment param = ast.newVariableDeclarationFragment();
	      param.setName(ast.newSimpleName(this.variableName));
	      lambda.parameters().add(param);
	      lambda.setParentheses(false);
	      if (loopToModify.getBody() instanceof Block) {
	    	  Block block = (Block) loopToModify.getBody();
	    	  if (block.statements().size() == 1) {
                  // Single statement - extract as expression
                  Statement stmt = (Statement) block.statements().get(0);
                  if (stmt instanceof ExpressionStatement) {
                      ExpressionStatement exprStmt = (ExpressionStatement) stmt;
                      lambda.setBody(ASTNode.copySubtree(ast, exprStmt.getExpression()));
                  } else {
                      // Not an expression statement, copy the whole statement as block
                      Block lambdaBlock = ast.newBlock();
                      lambdaBlock.statements().add(ASTNode.copySubtree(ast, stmt));
                      lambda.setBody(lambdaBlock);
                  }
              } else {
                  // Multiple statements - copy all into a block
                  Block lambdaBlock = ast.newBlock();
                  for (Object stmt : block.statements()) {
                      lambdaBlock.statements().add(ASTNode.copySubtree(ast, (Statement) stmt));
                  }
                  lambda.setBody(lambdaBlock);
              }
	      }
	      forEachCall.arguments().add(lambda);
		  return forEachCall;
	  }

  }
}
