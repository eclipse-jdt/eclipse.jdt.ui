/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 *       o bug "inline method - doesn't handle implicit cast" (see
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *       o bug inline method: compile error (array related)
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38471)
 *       o inline call that is used in a field initializer 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38137)
 *       o inline call a field initializer: could detect self reference 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=44417)
 *       o Allow 'this' constructor to be inlined  
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.TypeBindingVisitor;
import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InputFlowAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

public class CallInliner {

	private ICompilationUnit fCUnit;
	private ImportRewrite fImportEdit;
	private ITextFileBuffer fBuffer;
	private SourceProvider fSourceProvider;
	
	private BodyDeclaration fBodyDeclaration;
	private CodeScopeBuilder.Scope fRootScope;
	private int fNumberOfLocals;
	
	private ASTNode fInvocation;
	private ASTRewrite fRewrite;
	private Block fBlock;
	private int fInsertionIndex;
	private boolean fNeedsStatement;
	private ASTNode fTargetNode;
	private FlowContext fFlowContext;
	private FlowInfo fFlowInfo;
	private CodeScopeBuilder.Scope fInvocationScope;
	private boolean fFieldInitializer;
	private List fLocals;
	private CallContext fContext;
	
	private class InlineEvaluator extends HierarchicalASTVisitor {
		private ParameterData fFormalArgument;
		private boolean fResult;
		public InlineEvaluator(ParameterData argument) {
			fFormalArgument= argument;
		}
		public boolean getResult() {
			return fResult;
		}
		private boolean setResult(boolean result) {
			fResult= result;
			return false;
		}
		public boolean visit(Expression node) {
			int accessMode= fFormalArgument.getSimplifiedAccessMode();
			if (accessMode == FlowInfo.WRITE)
				return setResult(false);
			if (accessMode == FlowInfo.UNUSED)
				return setResult(true);
			if (ASTNodes.isLiteral(node))
				return setResult(true);
			return setResult(fFormalArgument.getNumberOfAccesses() <= 1);
		}
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding instanceof IVariableBinding) {
				int accessMode = fFormalArgument.getSimplifiedAccessMode();
				if (accessMode == FlowInfo.READ || accessMode == FlowInfo.UNUSED)
					return setResult(true);
				// from now on we only have write accesses.
				IVariableBinding vb= (IVariableBinding)binding;
				if (vb.isField())
					return setResult(false);
				return setResult(fFlowInfo.hasAccessMode(fFlowContext, vb, FlowInfo.UNUSED | FlowInfo.WRITE));
			}
			return setResult(false);
		}
		public boolean visit(FieldAccess node) {
			return visit(node.getName());
		}
		public boolean visit(SuperFieldAccess node) {
			return visit(node.getName());
		}
		public boolean visit(ThisExpression node) {
			int accessMode= fFormalArgument.getSimplifiedAccessMode();
			if (accessMode == FlowInfo.READ || accessMode == FlowInfo.UNUSED)
				return setResult(true);
			return setResult(false);
		}
	}

	private static class AmbiguousMethodAnalyzer implements TypeBindingVisitor {
		private String methodName;
		private ITypeBinding[] types;
		private IMethodBinding original;

		public AmbiguousMethodAnalyzer(IMethodBinding original, ITypeBinding[] types) {
			this.original= original;
			this.methodName= original.getName();
			this.types= types;
		}

		public boolean visit(ITypeBinding node) {
			IMethodBinding[] methods= node.getDeclaredMethods();
			for (int i= 0; i < methods.length; i++) {
				IMethodBinding candidate= methods[i];
				if (candidate == original) {
					continue;
				}
				if (methodName.equals(candidate.getName())) {
					if (canImplicitlyCall(candidate)) {
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * @param candidate method binding to check
		 * @return <code>true</code> if the method can be called without explicit casts, 
		 * otherwise <code>false</code>.
		 */
		private boolean canImplicitlyCall(IMethodBinding candidate) {
			ITypeBinding[] parameters= candidate.getParameterTypes();
			if (parameters.length != types.length) {
				return false;
			}
			for (int i= 0; i < parameters.length; i++) {
				if (!TypeRules.canAssign(types[i], parameters[i])) {
					return false;
				}
			}
			return true;
		}
	}

	public CallInliner(ICompilationUnit unit, SourceProvider provider, CodeGenerationSettings settings) throws CoreException {
		super();
		fCUnit= unit;
		fBuffer= RefactoringFileBuffers.connect(fCUnit);
		fSourceProvider= provider;
		fImportEdit= new ImportRewrite(fCUnit);
		fLocals= new ArrayList(3);
	}

	public void dispose() {
		try {
			RefactoringFileBuffers.disconnect(fCUnit);
		} catch (CoreException exception) {
			JavaPlugin.log(exception);
		}
	}
	
	/* package */ ITextFileBuffer getBuffer() {
		return fBuffer;
	}
	
	public ImportRewrite getImportEdit() {
		return fImportEdit;
	}
	
	public ASTNode getTargetNode() {
		return fTargetNode;
	}
	
	public void initialize(BodyDeclaration declaration) {
		fBodyDeclaration= declaration;
		fRootScope= CodeScopeBuilder.perform(declaration, fSourceProvider.getDeclaration().resolveBinding());
		fNumberOfLocals= 0;
		switch (declaration.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
			case ASTNode.INITIALIZER:
				fNumberOfLocals= LocalVariableIndex.perform(declaration);
				break;
		}
	}

	public RefactoringStatus initialize(ASTNode invocation, int severity) {
		RefactoringStatus result= new RefactoringStatus();
		fInvocation= invocation;
		fLocals= new ArrayList(3);
		
		checkMethodDeclaration(result, severity);
		if (result.getSeverity() >= severity)
			return result;
		
		initializeRewriter();
		initializeTargetNode();
		flowAnalysis();
		
		fContext= new CallContext(fInvocation, fInvocationScope, fTargetNode.getNodeType(), fImportEdit);
		
		try {
			computeRealArguments();
			computeReceiver();
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		checkInvocationContext(result, severity);
		if (result.getSeverity() >= severity)
			return result;		
		return result;
	}

	private void initializeRewriter() {
		// field initializer can be inside of a block if used in a local class
		// but block can't be a child of field initializer
		ASTNode parentField= ASTNodes.getParent(fInvocation, ASTNode.FIELD_DECLARATION);
		if(parentField != null) {
			fRewrite= ASTRewrite.create(parentField.getAST());
			fFieldInitializer= true;
		}
		else {
			ASTNode parentBlock= ASTNodes.getParent(fInvocation, ASTNode.BLOCK);
			Assert.isNotNull(parentBlock);
			fRewrite= ASTRewrite.create(parentBlock.getAST());
		}
	}

	private void initializeTargetNode() {
		ASTNode parent= fInvocation.getParent();
		int nodeType= parent.getNodeType();
		if (nodeType == ASTNode.EXPRESSION_STATEMENT || nodeType == ASTNode.RETURN_STATEMENT) {
			fTargetNode= parent;
		} else {
			fTargetNode= fInvocation;
		}
	}

	// the checks depend on invocation context and therefore can't be done in SourceAnalyzer
	private void checkMethodDeclaration(RefactoringStatus result, int severity) {
		MethodDeclaration methodDeclaration= fSourceProvider.getDeclaration();
		// it is not allowed to inline constructor invocation only if it is used for class instance creation
		// if constructor is invoked from another constructor then we can inline such invocation
		if (fInvocation.getNodeType() != ASTNode.CONSTRUCTOR_INVOCATION && methodDeclaration.isConstructor()) {
			result.addEntry(new RefactoringStatusEntry(
				severity,  
				RefactoringCoreMessages.getString("CallInliner.constructors"), //$NON-NLS-1$
				JavaStatusContext.create(fCUnit, fInvocation)));
		}
	}

	private void checkInvocationContext(RefactoringStatus result, int severity) {
		if (ASTNodes.getParent(fInvocation, FieldDeclaration.class) != null) {
			// it is allowed to inline a method used for field initialization
			// if only it consists of single return statement
			if(fSourceProvider.getNumberOfStatements() > 1) {
				addEntry(result,
					RefactoringCoreMessages.getString("CallInliner.field_initializer_simple"), //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER, severity);
				return;
			}
			int argumentsCount= fContext.arguments.length;
			for (int i= 0; i < argumentsCount; i++) {
				ParameterData parameter= fSourceProvider.getParameterData(i);
				if(parameter.isWrite()) {
					addEntry(result,
						RefactoringCoreMessages.getString("CallInliner.field_initialize_write_parameter"), //$NON-NLS-1$
						RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER, severity);
					return;
				}
			}
			if(fLocals.size() > 0) {
				addEntry(result,
					RefactoringCoreMessages.getString("CallInliner.field_initialize_new_local"), //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER, severity);
				return;
			}
			// verify that the field is not referenced by the initializer method 
			VariableDeclarationFragment variable= (VariableDeclarationFragment)ASTNodes.getParent(fInvocation, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
			if(fSourceProvider.isVariableReferenced(variable.resolveBinding())) {
				addEntry(result,
					RefactoringCoreMessages.getString("CallInliner.field_initialize_self_reference"), //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER, severity);
				return;
			}
		}
		if (fSourceProvider.isExecutionFlowInterrupted()) {
			VariableDeclaration vDecl= (VariableDeclaration)ASTNodes.getParent(fInvocation, VariableDeclaration.class);
			if (vDecl != null) {
				addEntry(result, RefactoringCoreMessages.getString("CallInliner.execution_flow"),  //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_LOCAL_INITIALIZER, severity);
				return;
			}
		}
		if (fInvocation.getNodeType() == ASTNode.METHOD_INVOCATION) {
			Expression exp= ((MethodInvocation)fInvocation).getExpression();
			if (exp != null && exp.resolveTypeBinding() == null) {
				addEntry(result, RefactoringCoreMessages.getString("CallInliner.receiver_type"), //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_NULL_BINDING, severity);
				return;
			}
		}
		int nodeType= fTargetNode.getNodeType();
		if (nodeType == ASTNode.EXPRESSION_STATEMENT) {
			if (fSourceProvider.isExecutionFlowInterrupted()) {
				addEntry(result, RefactoringCoreMessages.getString("CallInliner.execution_flow"),  //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_EXECUTION_FLOW, severity);
				return;
			}
		} else if (nodeType == ASTNode.METHOD_INVOCATION) {
			ASTNode parent= fTargetNode.getParent();
			if (parent.getNodeType() == ASTNode.ASSIGNMENT || isSingleDeclaration(parent)) {
				// this is ok
			} else if (isMultiDeclarationFragment(parent)) {
				if (!fSourceProvider.isSimpleFunction()) {
					addEntry(result, RefactoringCoreMessages.getString("CallInliner.multiDeclaration"), //$NON-NLS-1$
						RefactoringStatusCodes.INLINE_METHOD_INITIALIZER_IN_FRAGEMENT, severity);
					return;
				}
			} else if (fSourceProvider.getNumberOfStatements() > 1 ) {
				addEntry(result, RefactoringCoreMessages.getString("CallInliner.simple_functions"), //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_ONLY_SIMPLE_FUNCTIONS, severity);
				return;
			} else if (!fSourceProvider.isSimpleFunction()) {
				addEntry(result, RefactoringCoreMessages.getString("CallInliner.execution_flow"),  //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_EXECUTION_FLOW, severity);
				return;
			}
		}		
	}

	private static boolean isMultiDeclarationFragment(ASTNode node) {
		int nodeType= node.getNodeType();
		if (nodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= node.getParent();
			if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vs= (VariableDeclarationStatement)node;
				return vs.fragments().size() > 1;
			}
		}
		return false;
	}
	
	private static boolean isSingleDeclaration(ASTNode node) {
		int type= node.getNodeType();
		if (type == ASTNode.SINGLE_VARIABLE_DECLARATION)
			return true;
		if (type == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= node.getParent();
			if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vs= (VariableDeclarationStatement)node;
				return vs.fragments().size() == 1;
			}
		}
		return false;
	}
	
	private void addEntry(RefactoringStatus result, String message, int code, int severity) {
		result.addEntry(new RefactoringStatusEntry(
			severity, message, 
			JavaStatusContext.create(fCUnit, fInvocation),
			Corext.getPluginId(),
			code, null));
	}

	private void flowAnalysis() {
		fInvocationScope= fRootScope.findScope(fTargetNode.getStartPosition(), fTargetNode.getLength());
		fInvocationScope.setCursor(fTargetNode.getStartPosition());
		fFlowContext= new FlowContext(0, fNumberOfLocals + 1);
		fFlowContext.setConsiderAccessMode(true);
		fFlowContext.setComputeMode(FlowContext.ARGUMENTS);
		Selection selection= Selection.createFromStartLength(fInvocation.getStartPosition(), fInvocation.getLength());
		switch (fBodyDeclaration.getNodeType()) {
			case ASTNode.INITIALIZER:
			case ASTNode.FIELD_DECLARATION:
			case ASTNode.METHOD_DECLARATION:
				fFlowInfo= new InputFlowAnalyzer(fFlowContext, selection).perform(fBodyDeclaration);
				break;
			default:
				Assert.isTrue(false, "Should not happen");			 //$NON-NLS-1$
		}
	}
	
	public TextEdit perform() throws CoreException {
		
		String[] blocks= fSourceProvider.getCodeBlocks(fContext);
		if(!fFieldInitializer) {
			initializeInsertionPoint(fSourceProvider.getNumberOfStatements() + fLocals.size());
		}
		
		addNewLocals();
		replaceCall(blocks);
		
		return fRewrite.rewriteAST(fBuffer.getDocument(), fCUnit.getJavaProject().getOptions(true));
	}

	private void computeRealArguments() throws BadLocationException {
		List arguments= Invocations.getArguments(fInvocation);
		String[] realArguments= new String[arguments.size()];
		for (int i= 0; i < arguments.size(); i++) {
			Expression expression= (Expression)arguments.get(i);
			ParameterData parameter= fSourceProvider.getParameterData(i);
			if (canInline(expression, parameter)) {
				realArguments[i] = getContent(expression);
				// fixes bugs #35905, #38471
				if(expression instanceof CastExpression || expression instanceof ArrayCreation) {
					realArguments[i] = "(" + realArguments[i] + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
				String name= fInvocationScope.createName(parameter.getName(), true);
				realArguments[i]= name;
				fLocals.add(createLocalDeclaration(
					parameter.getTypeBinding(), name, 
					(Expression)fRewrite.createCopyTarget(expression)));
			}
		}
		fContext.arguments= realArguments;
	}
	
	private void computeReceiver() throws BadLocationException {
		Expression receiver= Invocations.getExpression(fInvocation);
		if (receiver == null)
			return;
		final boolean isName= receiver instanceof Name;
		if (isName)
			fContext.receiverIsStatic= ((Name)receiver).resolveBinding() instanceof ITypeBinding;
		if (ASTNodes.isLiteral(receiver) || isName) {
			fContext.receiver= fBuffer.getDocument().get(receiver.getStartPosition(), receiver.getLength());
			return;
		}
		switch(fSourceProvider.getReceiversToBeUpdated()) {
			case 0:
				// Make sure we evaluate the current receiver. Best is to assign to
				// local.
				fLocals.add(createLocalDeclaration(
					receiver.resolveTypeBinding(), 
					fInvocationScope.createName("r", true),  //$NON-NLS-1$
					(Expression)fRewrite.createCopyTarget(receiver)));
				return;
			case 1:
				fContext.receiver= fBuffer.getDocument().get(receiver.getStartPosition(), receiver.getLength());
				return;
			default:
				String local= fInvocationScope.createName("r", true); //$NON-NLS-1$
					fLocals.add(createLocalDeclaration(
					receiver.resolveTypeBinding(), 
					local, 
					(Expression)fRewrite.createCopyTarget(receiver)));
				fContext.receiver= local;
				return;
		}
	}

	private void addNewLocals() {
		for (Iterator iter= fLocals.iterator(); iter.hasNext();) {
			ASTNode element= (ASTNode)iter.next();
			fRewrite.getListRewrite(fBlock, Block.STATEMENTS_PROPERTY).insertAt(element, fInsertionIndex++, null);
		}
	}

	private void replaceCall(String[] blocks) {
		// Inline empty body
		if (blocks.length == 0) {
			if (fNeedsStatement) {
				fRewrite.replace(fTargetNode, fTargetNode.getAST().newEmptyStatement(), null);
			} else {
				fRewrite.remove(fTargetNode, null);
			}
		} else {
			ASTNode node= null;
			for (int i= 0; i < blocks.length - 1; i++) {
				node= fRewrite.createStringPlaceholder(blocks[i], ASTNode.RETURN_STATEMENT);
				fRewrite.getListRewrite(fBlock, Block.STATEMENTS_PROPERTY).insertAt(node, fInsertionIndex++, null);
			}
			String block= blocks[blocks.length - 1];
			// We can inline a call where the declaration is a function and the call itself
			// is a statement. In this case we have to create a temporary variable if the
			// returned expression must be evaluated.
			if (fContext.callMode == ASTNode.EXPRESSION_STATEMENT && fSourceProvider.hasReturnValue()) {
				if (fSourceProvider.mustEvaluateReturnedExpression()) {
					if (fSourceProvider.returnValueNeedsLocalVariable()) {
						node= createLocalDeclaration(
							fSourceProvider.getReturnType(), 
							fInvocationScope.createName(fSourceProvider.getMethodName(), true), 
							(Expression)fRewrite.createStringPlaceholder(block, ASTNode.METHOD_INVOCATION));
					} else {
						node= fTargetNode.getAST().newExpressionStatement(
							(Expression)fRewrite.createStringPlaceholder(block, ASTNode.METHOD_INVOCATION));
					}
				} else {
					node= null;
				}
			} else if (fTargetNode instanceof Expression) {
				node= fRewrite.createStringPlaceholder(block, ASTNode.METHOD_INVOCATION);
				
				// fixes bug #24941
				if(needsExplicitCast()) {
					AST ast= node.getAST();
					CastExpression castExpression= ast.newCastExpression();
					ITypeBinding returnType= fSourceProvider.getReturnType();
					fImportEdit.addImport(returnType);
					castExpression.setType(ASTNodeFactory.newType(ast, returnType, false));
					castExpression.setExpression((Expression)node);
					node= castExpression;
				}
				
				if (needsParenthesis()) {
					ParenthesizedExpression pExp= fTargetNode.getAST().newParenthesizedExpression();
					pExp.setExpression((Expression)node);
					node= pExp;
				}
			} else {
				node= fRewrite.createStringPlaceholder(block, ASTNode.RETURN_STATEMENT);
			}
			
			// Now replace the target node with the source node
			if (node != null) {
				if (fTargetNode == null) {
					fRewrite.getListRewrite(fBlock, Block.STATEMENTS_PROPERTY).insertAt(node, fInsertionIndex++, null);
				} else {
					fRewrite.replace(fTargetNode, node, null);
				}
			} else {
				if (fTargetNode != null) {
					fRewrite.remove(fTargetNode, null);
				}
			}
		}
	}

	/**
	 * @return <code>true</code> if explicit cast is needed otherwise <code>false</code>
	 */
	private boolean needsExplicitCast() {
		// if the return type of the method is the same as the type of the
		// returned expression then we don't need an explicit cast.
		if (fSourceProvider.returnTypeMatchesReturnExpressions())
				return false;		 
		ASTNode parent= fTargetNode.getParent();
		int nodeType= parent.getNodeType();
		if (nodeType == ASTNode.METHOD_INVOCATION) {
			MethodInvocation methodInvocation= (MethodInvocation)parent;
			if(methodInvocation.getExpression() == fTargetNode)
				return false;
			IMethodBinding method= methodInvocation.resolveMethodBinding();
			ITypeBinding[] parameters= method.getParameterTypes();
			int argumentIndex= methodInvocation.arguments().indexOf(fInvocation);
			List returnExprs= fSourceProvider.getReturnExpressions();
			// it is inferred that only methods consisting of a single 
			// return statement can be inlined as parameters in other 
			// method invocations
			if (returnExprs.size() != 1)
				return false;
			parameters[argumentIndex]= ((Expression)returnExprs.get(0)).resolveTypeBinding();

			ITypeBinding type= ASTNodes.getReceiverTypeBinding(methodInvocation);
			TypeBindingVisitor visitor= new AmbiguousMethodAnalyzer(method, parameters);
			if(!visitor.visit(type)) {
				return true;
			}
			else if(type.isInterface()) {
				return !Bindings.visitInterfaces(type, visitor);
			}
			else if(Modifier.isAbstract(type.getModifiers())) {
				return !Bindings.visitHierarchy(type, visitor);
			}
			else {
				// it is not needed to visit interfaces if receiver is a concrete class
				return !Bindings.visitSuperclasses(type, visitor);
			}
		}
		return false;
	}

	private boolean needsParenthesis() {
		if (!fSourceProvider.needsReturnedExpressionParenthesis())
			return false;
		ASTNode parent= fTargetNode.getParent();
		int type= parent.getNodeType();
		return 
			type == ASTNode.METHOD_INVOCATION || 
			(parent instanceof Expression && type != ASTNode.ASSIGNMENT) ||
			(fSourceProvider.returnsConditionalExpression() &&
				type == ASTNode.VARIABLE_DECLARATION_FRAGMENT &&  
				((VariableDeclarationFragment)parent).getInitializer() == fTargetNode);
	}
	
	private VariableDeclarationStatement createLocalDeclaration(ITypeBinding type, String name, Expression initializer) {
		String typeName= fImportEdit.addImport(type);
		VariableDeclarationStatement decl= (VariableDeclarationStatement)ASTNodeFactory.newStatement(
			fInvocation.getAST(), typeName + " " + name + ";"); //$NON-NLS-1$ //$NON-NLS-2$
		((VariableDeclarationFragment)decl.fragments().get(0)).setInitializer(initializer);
		return decl;
	}

	private boolean canInline(Expression actualParameter, ParameterData formalParameter) {
		InlineEvaluator evaluator= new InlineEvaluator(formalParameter);
		actualParameter.accept(evaluator);
		return evaluator.getResult();
	}
	
	private void initializeInsertionPoint(int nos) {
		fBlock= null;
		fInsertionIndex= -1;
		fNeedsStatement= false;
		ASTNode parentStatement= ASTNodes.getParent(fInvocation, Statement.class);
		ASTNode container= parentStatement.getParent();
		int type= container.getNodeType();
		if (type == ASTNode.BLOCK) {
			fBlock= (Block) container;
			fInsertionIndex= fBlock.statements().indexOf(parentStatement);
		} else if (isControlStatement(container)) {
			fNeedsStatement= true;
			if (nos > 1) {
				Block block= fInvocation.getAST().newBlock();
				fBlock= block;
				fInsertionIndex= 0;
				Statement currentStatement= null;
				switch(type) {
					case ASTNode.FOR_STATEMENT:
						currentStatement= ((ForStatement)container).getBody();
						break;
					case ASTNode.WHILE_STATEMENT:
						currentStatement= ((WhileStatement)container).getBody();
						break;
					case ASTNode.DO_STATEMENT:
						currentStatement= ((DoStatement)container).getBody();
						break;
					case ASTNode.IF_STATEMENT:
						IfStatement node= (IfStatement)container;
						Statement thenPart= node.getThenStatement();
						if (fTargetNode == thenPart || ASTNodes.isParent(fTargetNode, thenPart)) {
							currentStatement= thenPart;
						} else {
							currentStatement= node.getElseStatement();
						}
						break;
				}
				Assert.isNotNull(currentStatement);
				// The method to be inlined is not the body of the control statement.
				if (currentStatement != fTargetNode) {
					fRewrite.getListRewrite(fBlock, Block.STATEMENTS_PROPERTY).insertLast(fRewrite.createCopyTarget(currentStatement), null);
				} else {
					// We can't replace a copy with something else. So we
					// have to insert all statements to be inlined.
					fTargetNode= null;
				}
				fRewrite.replace(currentStatement, block, null);
			}
		}
		// We only insert one new statement or we delete the existing call. 
		// So there is no need to have an insertion index.
	}

	private String getContent(ASTNode node) throws BadLocationException {
		return fBuffer.getDocument().get(node.getStartPosition(), node.getLength());
	}

	private boolean isControlStatement(ASTNode node) {
		int type= node.getNodeType();
		return type == ASTNode.IF_STATEMENT || type == ASTNode.FOR_STATEMENT ||
		        type == ASTNode.WHILE_STATEMENT || type == ASTNode.DO_STATEMENT;
	}
}
