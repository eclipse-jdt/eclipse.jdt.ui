/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
 *       o inline call a field initializer: could detect self reference 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=44417)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditProcessor;
import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.refactoring.code.SourceAnalyzer.NameData;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class SourceProvider {

	private ICompilationUnit fCUnit;
	private IDocument fDocument;
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
		fRewriter= ASTRewrite.create(fDeclaration.getAST());
		fAnalyzer= new SourceAnalyzer(fCUnit, fDeclaration);
		fReturnValueNeedsLocalVariable= true;
		fReturnExpressions= new ArrayList();
	}
	
	public RefactoringStatus checkActivation() throws JavaModelException {
		return fAnalyzer.checkActivation();
	}
	
	public void initialize() throws JavaModelException {
		fDocument= new Document(fCUnit.getBuffer().getContents());
		fAnalyzer.initialize();
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

	static class VariableReferenceFinder extends ASTVisitor {
		private boolean fResult;
		private IVariableBinding fBinding;
		public VariableReferenceFinder(IVariableBinding binding) {
			fBinding= binding;
		}
		public boolean getResult() {
			return fResult;
		}
		public boolean visit(SimpleName node) {
			if(!fResult) {
				fResult= Bindings.equals(fBinding, node.resolveBinding()); 
			}
			return false;
		}
	}

	/**
	 * Checks whether variable is referenced by the method declaration or not.
	 * 
	 * @param binding binding of variable to check.
	 * @return <code>true</code> if variable is referenced by the method, otherwise <code>false</code>
	 */
	public boolean isVariableReferenced(IVariableBinding binding) {
		VariableReferenceFinder finder= new VariableReferenceFinder(binding);
		fDeclaration.accept(finder);
		return finder.getResult();
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
	
	public boolean isLastStatementReturn() {
		List statements= fDeclaration.getBody().statements();
		if (statements.size() == 0)
			return false;
		return statements.get(statements.size() - 1) instanceof ReturnStatement;
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
	
	public boolean returnsConditionalExpression() {
		ASTNode last= getLastStatement();
		if (last instanceof ReturnStatement) {
			return ((ReturnStatement)last).getExpression() instanceof ConditionalExpression;
		}
		return false;
	}
	
	public int getReceiversToBeUpdated() {
		return fAnalyzer.getImplicitReceivers().size();
	}
	
	public boolean isVarargs() {
		return fDeclaration.isVarargs();
	}
	
	public int getVarargIndex() {
		return fDeclaration.parameters().size() - 1;
	}
	
	public TextEdit getDeleteEdit() {
		final ASTRewrite rewriter= ASTRewrite.create(fDeclaration.getParent().getAST());
		rewriter.remove(fDeclaration, null);
		return rewriter.rewriteAST(fDocument, fCUnit.getJavaProject().getOptions(true));
	}
	
	public String[] getCodeBlocks(CallContext context) throws CoreException {
		replaceParameterWithExpression(context.arguments);
		updateImplicitReceivers(context);
		makeNamesUnique(context.scope);
		updateTypeReferences(context);
		updateStaticReferences(context);
		updateTypeVariables(context);
		updateMethodTypeVariable(context);
		
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

		final TextEdit dummy= fRewriter.rewriteAST(fDocument, fCUnit.getJavaProject().getOptions(true));
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
		MultiTextEdit root= new MultiTextEdit(0, fDocument.getLength());
		root.addChildren(markers);
		
		try {
			TextEditProcessor processor= new TextEditProcessor(fDocument, root, TextEdit.CREATE_UNDO | TextEdit.UPDATE_REGIONS);
			UndoEdit undo= processor.performEdits();
			String[] result= getBlocks(fCUnit, markers);
			// It is faster to undo the changes than coping the buffer over and over again.
			processor= new TextEditProcessor(fDocument, undo, TextEdit.UPDATE_REGIONS);
			processor.performEdits();
			return result;
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return new String[] {};
	}

	private void replaceParameterWithExpression(String[] expressions) {
		for (int i= 0; i < expressions.length; i++) {
			String expression= expressions[i];
			ParameterData parameter= getParameterData(i);
			List references= parameter.references();
			for (Iterator iter= references.iterator(); iter.hasNext();) {
				ASTNode element= (ASTNode) iter.next();
				ASTNode newNode= fRewriter.createStringPlaceholder(expression, element.getNodeType());
				fRewriter.replace(element, newNode, null);
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
					ASTNode newNode= fRewriter.createStringPlaceholder(newName, ASTNode.METHOD_INVOCATION);
					fRewriter.replace(element, newNode, null);
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
				fRewriter.set(inv, MethodInvocation.EXPRESSION_PROPERTY, createReceiver(context, (IMethodBinding)inv.getName().resolveBinding()), null);
			} else if (node instanceof ClassInstanceCreation) {
				final ClassInstanceCreation inst= (ClassInstanceCreation)node;
				fRewriter.set(inst, ClassInstanceCreation.EXPRESSION_PROPERTY, createReceiver(context, inst.resolveConstructorBinding()), null);
			} else if (node instanceof ThisExpression) {
				fRewriter.replace(node, fRewriter.createStringPlaceholder(context.receiver, ASTNode.METHOD_INVOCATION), null);
			} else if (node instanceof FieldAccess) { 
				final FieldAccess access= (FieldAccess)node;
				fRewriter.set(access, FieldAccess.EXPRESSION_PROPERTY, createReceiver(context, access.resolveFieldBinding()), null);
			} else if (node instanceof SimpleName && ((SimpleName)node).resolveBinding() instanceof IVariableBinding) {
				IVariableBinding vb= (IVariableBinding)((SimpleName)node).resolveBinding();
				if (vb.isField()) { 
					Expression receiver= createReceiver(context, vb);
					if (receiver != null) {
						FieldAccess access= node.getAST().newFieldAccess();
						ASTNode target= fRewriter.createMoveTarget(node);
						access.setName((SimpleName)target);
						access.setExpression(receiver);
						fRewriter.replace(node, access, null);
					}
				}
			}
		}
	}
	
	private void updateTypeReferences(CallContext context) {
		ImportRewrite importer= context.importer;
		for (Iterator iter= fAnalyzer.getTypesToImport().iterator(); iter.hasNext();) {
			Name element= (Name)iter.next();
			ITypeBinding binding= ASTNodes.getTypeBinding(element);
			if (binding != null && !binding.isLocal()) {
				String s= importer.addImport(binding);
				if (!ASTNodes.asString(element).equals(s)) {
					fRewriter.replace(element, fRewriter.createStringPlaceholder(s, ASTNode.SIMPLE_NAME), null);
				}
			}
		}
	}
	
	private void updateStaticReferences(CallContext context) {
		ImportRewrite importer= context.importer;
		for (Iterator iter= fAnalyzer.getStaticsToImport().iterator(); iter.hasNext();) {
			Name element= (Name)iter.next();
			IBinding binding= element.resolveBinding();
			if (binding != null) {
				String s= importer.addStaticImport(binding);
				if (!ASTNodes.asString(element).equals(s)) {
					fRewriter.replace(element, fRewriter.createStringPlaceholder(s, ASTNode.SIMPLE_NAME), null);
				}
			}
		}
		
	}

	private Expression createReceiver(CallContext context, IMethodBinding method) {
		String receiver= getReceiver(context, method.getModifiers());
		if (receiver == null)
			return null;
		return (Expression)fRewriter.createStringPlaceholder(receiver, ASTNode.METHOD_INVOCATION);
	}
	
	private Expression createReceiver(CallContext context, IVariableBinding field) {
		String receiver= getReceiver(context, field.getModifiers());
		if (receiver == null)
			return null;
		return (Expression)fRewriter.createStringPlaceholder(receiver, ASTNode.SIMPLE_NAME);
	}
	
	private String getReceiver(CallContext context, int modifiers) {
		String receiver= context.receiver;
		ITypeBinding invocationType= ASTNodes.getEnclosingType(context.invocation);
		ITypeBinding sourceType= fDeclaration.resolveBinding().getDeclaringClass();
		if (!context.receiverIsStatic && Modifier.isStatic(modifiers)) {
			if ("this".equals(receiver) && invocationType != null && Bindings.equals(invocationType, sourceType)) { //$NON-NLS-1$
				receiver= null;
			} else {
				receiver= context.importer.addImport(sourceType);
			}
		}
		return receiver;
	}

	private void updateTypeVariables(CallContext context) {
		ITypeBinding type= context.getReceiverType();
		if (type == null)
			return;
		rewriteReferences(type.getTypeArguments(), fAnalyzer.getTypeParameterReferences());
	}
	
	private void updateMethodTypeVariable(CallContext context) {
		IMethodBinding method= Invocations.resolveBinding(context.invocation);
		if (method == null)
			return;
		rewriteReferences(method.getTypeArguments(), fAnalyzer.getMethodTypeParameterReferences());
	}

	private void rewriteReferences(ITypeBinding[] typeArguments, List typeParameterReferences) {
		if (typeArguments.length == 0)
			return;
		Assert.isTrue(typeArguments.length == typeParameterReferences.size());
		for (int i= 0; i < typeArguments.length; i++) {
			SourceAnalyzer.NameData refData= (NameData)typeParameterReferences.get(i);
			List references= refData.references();
			String newName= typeArguments[i].getName();
			for (Iterator iter= references.iterator(); iter.hasNext();) {
				SimpleName name= (SimpleName)iter.next();
				fRewriter.replace(name, fRewriter.createStringPlaceholder(newName, ASTNode.SIMPLE_NAME), null);
			}
		}
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
	
	private String[] getBlocks(ICompilationUnit unit, RangeMarker[] markers) throws BadLocationException {
		String[] result= new String[markers.length];
		for (int i= 0; i < markers.length; i++) {
			RangeMarker marker= markers[i];
			String content= fDocument.get(marker.getOffset(), marker.getLength());
			String lines[]= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(unit.getJavaProject()), false);
			result[i]= Strings.concatenate(lines, TextUtilities.getDefaultLineDelimiter(fDocument));
		}
		return result;
	}
}
