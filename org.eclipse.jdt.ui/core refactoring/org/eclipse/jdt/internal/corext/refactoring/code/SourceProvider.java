/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       o bug "inline method - doesn't handle implicit cast" (see
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *       o inline call that is used in a field initializer 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38137)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class SourceProvider {

	private ICompilationUnit fCUnit;
	private TextBuffer fBuffer;
	private MethodDeclaration fDeclaration;
	private ASTRewrite fRewriter;
	private SourceAnalyzer fAnalyzer;
	private boolean fMustEvalReturnedExpression;
	private boolean fReturnValueNeedsLocalVariable;
	private List fReturnExpressions;
	
	private class ReturnAnalyzer extends ASTVisitor {
		public boolean visit(ReturnStatement node) {
			Expression expression= node.getExpression();
			if (!(ASTNodes.isLiteral(expression) || expression instanceof Name)) {
				fMustEvalReturnedExpression= true;
			}
			if (Invocations.isInvocation(expression) || expression instanceof ClassInstanceCreation) {
				fReturnValueNeedsLocalVariable= false;
			}
			fReturnExpressions.add(expression);
			return false;
		}
	}

	public SourceProvider(ICompilationUnit unit, MethodDeclaration declaration) {
		super();
		fCUnit= unit;
		fDeclaration= declaration;
		List parameters= fDeclaration.parameters();
		for (Iterator iter= parameters.iterator(); iter.hasNext();) {
			SingleVariableDeclaration element= (SingleVariableDeclaration)iter.next();
			ParameterData data= new ParameterData(element);
			element.setProperty(ParameterData.PROPERTY, data);
		}
		fRewriter= new ASTRewrite(fDeclaration);
		fAnalyzer= new SourceAnalyzer(fCUnit, fDeclaration);
		fReturnValueNeedsLocalVariable= true;
		fReturnExpressions= new ArrayList();
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		return fAnalyzer.checkActivation();
	}
	
	public void initialize() throws JavaModelException {
		fBuffer= TextBuffer.create(fCUnit.getBuffer().getContents());
		fAnalyzer.analyzeParameters();
		if (hasReturnValue()) {
			ASTNode last= getLastStatement();
			if (last != null) {
				ReturnAnalyzer analyzer= new ReturnAnalyzer();
				last.accept(analyzer);
			}
		}
	}

	public boolean isExecutionFlowInterrupted() {
		return fAnalyzer.isExecutionFlowInterrupted();
	}
	
	public boolean hasReturnValue() {
		IMethodBinding binding= fDeclaration.resolveBinding();
		return binding.getReturnType() != fDeclaration.getAST().resolveWellKnownType("void"); //$NON-NLS-1$
	}
	
	public boolean mustEvaluateReturnedExpression() {
		return fMustEvalReturnedExpression;
	}
	
	public boolean returnValueNeedsLocalVariable() {
		return fReturnValueNeedsLocalVariable;
	}
	
	public int getNumberOfStatements() {
		return fDeclaration.getBody().statements().size();
	}
	
	public boolean isSimpleFunction() {
		List statements= fDeclaration.getBody().statements();
		if (statements.size() != 1)
			return false;
		return statements.get(0) instanceof ReturnStatement;
	}
	
	public MethodDeclaration getDeclaration() {
		return fDeclaration;
	}
	
	public String getMethodName() {
		return fDeclaration.getName().getIdentifier();
	}
	
	public ITypeBinding getReturnType() {
		return fDeclaration.resolveBinding().getReturnType();
	}
	
	public List getReturnExpressions() {
		return fReturnExpressions;
	}
	
	public boolean returnTypeMatchesReturnExpressions() {
		ITypeBinding returnType= getReturnType();
		for (Iterator iter= fReturnExpressions.iterator(); iter.hasNext();) {
			Expression expression= (Expression)iter.next();
			if (!Bindings.equals(returnType, expression.resolveTypeBinding()))
				return false;
		}
		return true;
	}
	
	public ParameterData getParameterData(int index) {
		SingleVariableDeclaration decl= (SingleVariableDeclaration)fDeclaration.parameters().get(index);
		return (ParameterData)decl.getProperty(ParameterData.PROPERTY);
	}
	
	public ICompilationUnit getCompilationUnit() {
		return fCUnit;
	}
	
	public boolean needsReturnedExpressionParenthesis() {
		ASTNode last= getLastStatement();
		if (last instanceof ReturnStatement) {
			return ASTNodes.needsParentheses(((ReturnStatement)last).getExpression());
		}
		return false;
	}
	
	public int getReceiversToBeUpdated() {
		return fAnalyzer.getImplicitReceivers().size();
	}
	
	public TextEdit getDeleteEdit() {
		ASTRewrite rewriter= new ASTRewrite(fDeclaration.getParent());
		rewriter.markAsRemoved(fDeclaration);
		MultiTextEdit result= new MultiTextEdit();
		rewriter.rewriteNode(fBuffer, result);
		rewriter.removeModifications();
		return result;
	}
	
	public String[] getCodeBlocks(CallContext context) throws CoreException {
		replaceParameterWithExpression(context.arguments);
		updateImplicitReceivers(context);
		makeNamesUnique(context.scope);
		updateTypes(context);
		
		List ranges= null;
		if (hasReturnValue()) {
			if (context.callMode == ASTNode.RETURN_STATEMENT) {
				ranges= getStatementRanges();
			} else {
				ranges= getExpressionRanges();
			}
		} else {
			ASTNode last= getLastStatement();
			if (last != null && last.getNodeType() == ASTNode.RETURN_STATEMENT) {
				ranges= getReturnStatementRanges();
			} else {
				ranges= getStatementRanges();
			}
		}
		
		MultiTextEdit dummy= new MultiTextEdit();
		fRewriter.rewriteNode(fBuffer, dummy);

		int size= ranges.size();
		RangeMarker[] markers= new RangeMarker[size];
		for (int i= 0; i < markers.length; i++) {
			IRegion range= (IRegion)ranges.get(i);
			markers[i]= new RangeMarker(range.getOffset(), range.getLength());
		}
		int split;
		if (size <= 1) {
			split= Integer.MAX_VALUE;
		} else {
			IRegion region= (IRegion)ranges.get(0);
			split= region.getOffset() + region.getLength();
		}
		TextEdit[] edits= dummy.removeChildren();
		for (int i= 0; i < edits.length; i++) {
			TextEdit edit= edits[i];
			int pos= edit.getOffset() >= split ? 1 : 0;
			markers[pos].addChild(edit);
		}
		MultiTextEdit root= new MultiTextEdit();
		root.addChildren(markers);
		TextBufferEditor editor= new TextBufferEditor(fBuffer);
		editor.add(root);
		UndoEdit undo= editor.performEdits(null);
		String[] result= getBlocks(markers);
		// It is faster to undo the changes than coping the buffer over and over again.
		TextBufferEditor undoEditor= new TextBufferEditor(fBuffer);
		undoEditor.add(undo);
		undoEditor.performEdits(null);
		fRewriter.removeModifications();
		return result;
	}

	private void replaceParameterWithExpression(String[] expressions) {
		for (int i= 0; i < expressions.length; i++) {
			String expression= expressions[i];
			ParameterData parameter= getParameterData(i);
			List references= parameter.references();
			for (Iterator iter= references.iterator(); iter.hasNext();) {
				ASTNode element= (ASTNode) iter.next();
				ASTNode newNode= fRewriter.createPlaceholder(expression, ASTRewrite.getPlaceholderType(element));
				fRewriter.markAsReplaced(element, newNode);
			}
		}
	}

	private void makeNamesUnique(CodeScopeBuilder.Scope scope) {
		Collection usedCalleeNames= fAnalyzer.getUsedNames();
		for (Iterator iter= usedCalleeNames.iterator(); iter.hasNext();) {
			SourceAnalyzer.NameData nd= (SourceAnalyzer.NameData) iter.next();
			if (scope.isInUse(nd.getName())) {
				String newName= scope.createName(nd.getName(), true);
				List references= nd.references();
				for (Iterator refs= references.iterator(); refs.hasNext();) {
					SimpleName element= (SimpleName) refs.next();
					ASTNode newNode= fRewriter.createPlaceholder(newName, ASTRewrite.EXPRESSION);
					fRewriter.markAsReplaced(element, newNode);
				}
			}
		}
	}
	
	private void updateImplicitReceivers(CallContext context) {
		if (context.receiver == null)
			return;
		List implicitReceivers= fAnalyzer.getImplicitReceivers();
		for (Iterator iter= implicitReceivers.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode)iter.next();
			if (node instanceof MethodInvocation) {
				final MethodInvocation inv= (MethodInvocation)node;
				inv.setExpression(createReceiver(context, (IMethodBinding)inv.getName().resolveBinding()));
			} else if (node instanceof ClassInstanceCreation) {
				final ClassInstanceCreation inst= (ClassInstanceCreation)node;
				inst.setExpression(createReceiver(context, inst.resolveConstructorBinding()));
			} else if (node instanceof Expression) {
				fRewriter.markAsReplaced(node, fRewriter.createPlaceholder(context.receiver, ASTRewrite.EXPRESSION));
			}
		}
	}
	
	private void updateTypes(CallContext context) {
		ImportRewrite importer= context.importer;
		for (Iterator iter= fAnalyzer.getUsedTypes().iterator(); iter.hasNext();) {
			Name element= (Name)iter.next();
			ITypeBinding binding= ASTNodes.getTypeBinding(element);
			if (binding != null && !binding.isLocal()) {
				String s= importer.addImport(binding);
				if (!ASTNodes.asString(element).equals(s)) {
					fRewriter.markAsReplaced(element, fRewriter.createPlaceholder(s, ASTRewrite.EXPRESSION));
				}
			}
		}
	}

	private Expression createReceiver(CallContext context, IMethodBinding method) {
		String receiver= context.receiver;
		if (!context.receiverIsStatic && Modifier.isStatic(method.getModifiers())) {
			receiver= context.importer.addImport(fDeclaration.resolveBinding().getDeclaringClass()); 
		}
		Expression exp= (Expression)fRewriter.createPlaceholder(receiver, ASTRewrite.EXPRESSION);
		fRewriter.markAsInserted(exp);
		return exp;
	}
	
	private ASTNode getLastStatement() {
		List statements= fDeclaration.getBody().statements();
		if (statements.isEmpty())
			return null;
		return (ASTNode)statements.get(statements.size() - 1);
	}

	private List getReturnStatementRanges() {
		List result= new ArrayList(1);
		List statements= fDeclaration.getBody().statements();
		int size= statements.size();
		if (size <= 1)
			return result;
		result.add(createRange(statements, size - 2));
		return result;
	}

	private List getStatementRanges() {
		List result= new ArrayList(1);
		List statements= fDeclaration.getBody().statements();
		int size= statements.size();
		if (size == 0)
			return result;
		result.add(createRange(statements, size - 1));
		return result;
	}

	private List getExpressionRanges() {
		List result= new ArrayList(2);
		List statements= fDeclaration.getBody().statements();
		ReturnStatement rs= null;
		int size= statements.size();
		ASTNode node;
		switch (size) {
			case 0:
				return result;
			case 1:
				node= (ASTNode)statements.get(0);
				if (node.getNodeType() == ASTNode.RETURN_STATEMENT) {
					rs= (ReturnStatement)node;
				} else {
					result.add(new Region(node.getStartPosition(), node.getLength()));
				}
				break;
			default: {
				node= (ASTNode)statements.get(size - 1);
				if (node.getNodeType() == ASTNode.RETURN_STATEMENT) {
					result.add(createRange(statements, size - 2));
					rs= (ReturnStatement)node;
				} else {
					result.add(createRange(statements, size - 1));
				}
				break;
			}
		}
		if (rs != null) {
			Expression exp= rs.getExpression();
			result.add(new Region(exp.getStartPosition(), exp.getLength()));
		}
		return result;
	}
	
	private IRegion createRange(List statements, int end) {
		int start= ((ASTNode)statements.get(0)).getStartPosition();
		ASTNode last= (ASTNode)statements.get(end);
		int length = last.getStartPosition() - start + last.getLength();
		IRegion range= new Region(start, length);
		return range;
	}
	
	private String[] getBlocks(RangeMarker[] markers) {
		String[] result= new String[markers.length];
		for (int i= 0; i < markers.length; i++) {
			RangeMarker marker= markers[i];
			String content= fBuffer.getContent(marker.getOffset(), marker.getLength());
			String lines[]= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
			result[i]= Strings.concatenate(lines, fBuffer.getLineDelimiter());
		}
		return result;
	}
}
