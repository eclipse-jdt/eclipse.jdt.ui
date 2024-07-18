/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *     Microsoft Corporation - read formatting options from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditProcessor;
import org.eclipse.text.edits.UndoEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.code.SourceAnalyzer.NameData;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

/**
 * A SourceProvider encapsulates a piece of code (source) and the logic
 * to inline it into given CallContexts.
 */
public class SourceProvider {

	private ITypeRoot fTypeRoot;
	private IDocument fDocument;
	private MethodDeclaration fDeclaration;
	private SourceAnalyzer fAnalyzer;
	private boolean fMustEvalReturnedExpression;
	private boolean fReturnValueNeedsLocalVariable;
	private List<Expression> fReturnExpressions;
	private IDocument fSource;

	private static final int EXPRESSION_MODE= 1;
	private static final int STATEMENT_MODE= 2;
	private static final int RETURN_STATEMENT_MODE= 3;
	private int fMarkerMode;


	private class ReturnAnalyzer extends ASTVisitor {
		@Override
		public boolean visit(ReturnStatement node) {
			Expression expression= node.getExpression();
			if (!ASTNodes.isLiteral(expression) && !(expression instanceof Name)) {
				fMustEvalReturnedExpression= true;
			}
			if (Invocations.isInvocation(expression) || expression instanceof ClassInstanceCreation) {
				fReturnValueNeedsLocalVariable= false;
			}
			fReturnExpressions.add(expression);
			return false;
		}
	}

	public SourceProvider(ITypeRoot typeRoot, MethodDeclaration declaration) {
		super();
		fTypeRoot= typeRoot;
		fDeclaration= declaration;
		List<SingleVariableDeclaration> parameters= fDeclaration.parameters();
		for (SingleVariableDeclaration element : parameters) {
			ParameterData data= new ParameterData(element);
			element.setProperty(ParameterData.PROPERTY, data);
		}
		fAnalyzer= new SourceAnalyzer(fTypeRoot, fDeclaration);
		fReturnValueNeedsLocalVariable= true;
		fReturnExpressions= new ArrayList<>();
	}

	/**
	 * TODO: unit's source does not match contents of source document and declaration node.
	 * @param typeRoot the type root
	 * @param source document containing the content of the type root
	 * @param declaration method declaration node
	 */
	public SourceProvider(ITypeRoot typeRoot, IDocument source, MethodDeclaration declaration) {
		this(typeRoot, declaration);
		fSource= source;
	}

	public RefactoringStatus checkActivation() throws JavaModelException {
		return fAnalyzer.checkActivation();
	}

	public void initialize() throws JavaModelException {
		fDocument= fSource == null ? new Document(fTypeRoot.getBuffer().getContents()) : fSource;
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
		@Override
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

	// returns true if the declaration has a vararg and
	// the body contains an array access to the vararg
	public boolean hasArrayAccess() {
		return fAnalyzer.hasArrayAccess();
	}

	public boolean hasSuperMethodInvocation() {
		return fAnalyzer.hasSuperMethodInvocation();
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
		List<Statement> statements= fDeclaration.getBody().statements();
		if (statements.size() != 1)
			return false;
		return statements.get(0) instanceof ReturnStatement;
	}

	public boolean isLastStatementReturn() {
		List<Statement> statements= fDeclaration.getBody().statements();
		if (statements.size() == 0)
			return false;
		return statements.get(statements.size() - 1) instanceof ReturnStatement;
	}

	public boolean isDangligIf() {
		List<Statement> statements= fDeclaration.getBody().statements();
		if (statements.size() != 1)
			return false;

		ASTNode p= statements.get(0);

		while (true) {
			if (p instanceof IfStatement) {
				return ((IfStatement) p).getElseStatement() == null;
			} else {

				ChildPropertyDescriptor childD;
				if (p instanceof WhileStatement) {
					childD= WhileStatement.BODY_PROPERTY;
				} else if (p instanceof ForStatement) {
					childD= ForStatement.BODY_PROPERTY;
				} else if (p instanceof EnhancedForStatement) {
					childD= EnhancedForStatement.BODY_PROPERTY;
				} else if (p instanceof DoStatement) {
					childD= DoStatement.BODY_PROPERTY;
				} else if (p instanceof LabeledStatement) {
					childD= LabeledStatement.BODY_PROPERTY;
				} else {
					return false;
				}
				Statement body= (Statement) p.getStructuralProperty(childD);
				if (body instanceof Block) {
					return false;
				} else {
					p= body;
				}
			}
		}
	}

	public MethodDeclaration getDeclaration() {
		return fDeclaration;
	}

	public boolean isSynchronized() {
		List<IExtendedModifier> modifierList= fDeclaration.modifiers();
		for (IExtendedModifier modifier : modifierList) {
			if (modifier instanceof Modifier m && m.isSynchronized()) {
				return true;
			}
		}
		return false;
	}

	public boolean isStatic() {
		List<IExtendedModifier> modifierList= fDeclaration.modifiers();
		for (IExtendedModifier modifier : modifierList) {
			if (modifier instanceof Modifier m && m.isStatic()) {
				return true;
			}
		}
		return false;
	}

	public String getMethodName() {
		return fDeclaration.getName().getIdentifier();
	}

	public ITypeBinding getReturnType() {
		return fDeclaration.resolveBinding().getReturnType();
	}

	public List<Expression> getReturnExpressions() {
		return fReturnExpressions;
	}

	public boolean returnTypeMatchesReturnExpressions() {
		ITypeBinding returnType= getReturnType();
		for (Expression expression : fReturnExpressions) {
			if (!Bindings.equals(returnType, expression.resolveTypeBinding()))
				return false;
		}
		return true;
	}

	public ParameterData getParameterData(int index) {
		SingleVariableDeclaration decl= (SingleVariableDeclaration)fDeclaration.parameters().get(index);
		return (ParameterData)decl.getProperty(ParameterData.PROPERTY);
	}

	public ITypeRoot getTypeRoot() {
		return fTypeRoot;
	}

	public boolean needsReturnedExpressionParenthesis(ASTNode parent, StructuralPropertyDescriptor locationInParent) {
		ASTNode last= getLastStatement();
		if (last instanceof ReturnStatement) {
			return NecessaryParenthesesChecker.needsParentheses(((ReturnStatement)last).getExpression(), parent, locationInParent);
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
		final ASTRewrite rewriter= ASTRewrite.create(fDeclaration.getAST());
		rewriter.remove(fDeclaration, null);
		Map<String, String> options= fTypeRoot instanceof ICompilationUnit ?
				((ICompilationUnit) fTypeRoot).getOptions(true) : fTypeRoot.getJavaProject().getOptions(true);
		return rewriter.rewriteAST(fDocument, options);
	}

	public String[] getCodeBlocks(CallContext context, ImportRewrite importRewrite) throws CoreException {
		final ASTRewrite rewriter= ASTRewrite.create(fDeclaration.getAST());
		replaceParameterWithExpression(rewriter, context, importRewrite);
		updateImplicitReceivers(rewriter, context);
		makeNamesUnique(rewriter, context.scope);
		updateTypeReferences(rewriter, context);
		updateStaticReferences(rewriter, context);
		updateInnerStaticReferences(rewriter, context);
		updateTypeVariables(rewriter, context);
		updateMethodTypeVariable(rewriter, context);
		updateReturnStatements(rewriter, context);
		List<IRegion> ranges= null;
		if (hasReturnValue()) {
			if (context.callMode == ASTNode.RETURN_STATEMENT
					|| context.callMode == ASTNode.YIELD_STATEMENT) {
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

		Map<String, String> options= fTypeRoot instanceof ICompilationUnit ?
				((ICompilationUnit) fTypeRoot).getOptions(true) : fTypeRoot.getJavaProject().getOptions(true);
		final TextEdit dummy= rewriter.rewriteAST(fDocument, options);
		int size= ranges.size();
		RangeMarker[] markers= new RangeMarker[size];
		for (int i= 0; i < markers.length; i++) {
			IRegion range= ranges.get(i);
			markers[i]= new RangeMarker(range.getOffset(), range.getLength());
		}
		int split;
		if (size <= 1) {
			split= Integer.MAX_VALUE;
		} else {
			IRegion region= ranges.get(0);
			split= region.getOffset() + region.getLength();
		}
		for (TextEdit edit : dummy.removeChildren()) {
			int pos= edit.getOffset() >= split ? 1 : 0;
			markers[pos].addChild(edit);
		}
		MultiTextEdit root= new MultiTextEdit(0, fDocument.getLength());
		root.addChildren(markers);

		try {
			TextEditProcessor processor= new TextEditProcessor(fDocument, root, TextEdit.CREATE_UNDO | TextEdit.UPDATE_REGIONS);
			UndoEdit undo= processor.performEdits();
			String[] result= getBlocks(markers);
			// It is faster to undo the changes than coping the buffer over and over again.
			processor= new TextEditProcessor(fDocument, undo, TextEdit.UPDATE_REGIONS);
			processor.performEdits();
			return result;
		} catch (MalformedTreeException | BadLocationException exception) {
			JavaManipulationPlugin.log(exception);
		}
		return new String[] {};
	}

	private Expression createParenthesizedExpression(Expression newExpression, AST ast) {
		ParenthesizedExpression parenthesized= ast.newParenthesizedExpression();
		parenthesized.setExpression(newExpression);
		return parenthesized;
	}

	private void replaceParameterWithExpression(ASTRewrite rewriter, CallContext context, ImportRewrite importRewrite) throws CoreException {
		Expression[] arguments= context.arguments;
		try {
			ITextFileBuffer buffer= RefactoringFileBuffers.acquire(context.compilationUnit);
			for (int i= 0; i < arguments.length; i++) {
				Expression expression= arguments[i];
				String expressionString= null;
				if (expression instanceof SimpleName) {
					expressionString= ((SimpleName)expression).getIdentifier();
				} else {
					try {
						expressionString= buffer.getDocument().get(expression.getStartPosition(), expression.getLength());
					} catch (BadLocationException exception) {
						JavaManipulationPlugin.log(exception);
						continue;
					}
				}
				ParameterData parameter= getParameterData(i);
				List<SimpleName> references= parameter.references();
				for (SimpleName simpleName : references) {
					ASTNode element= simpleName;
					Expression newExpression= (Expression)rewriter.createStringPlaceholder(expressionString, expression.getNodeType());
					AST ast= rewriter.getAST();
					ITypeBinding explicitCast= ASTNodes.getExplicitCast(expression, (Expression)element);
					if (explicitCast != null) {
						CastExpression cast= ast.newCastExpression();
						if (NecessaryParenthesesChecker.needsParentheses(expression, cast, CastExpression.EXPRESSION_PROPERTY)) {
							newExpression= createParenthesizedExpression(newExpression, ast);
						}
						cast.setExpression(newExpression);
						ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(expression, importRewrite);
						cast.setType(importRewrite.addImport(explicitCast, ast, importRewriteContext, TypeLocation.CAST));
						expression= newExpression= cast;
					}
					if (NecessaryParenthesesChecker.needsParentheses(expression, element.getParent(), element.getLocationInParent())) {
						newExpression= createParenthesizedExpression(newExpression, ast);
					}
					rewriter.replace(element, newExpression, null);
				}
			}
		} finally {
			RefactoringFileBuffers.release(context.compilationUnit);
		}
	}

	private void makeNamesUnique(ASTRewrite rewriter, CodeScopeBuilder.Scope scope) {
		Collection<NameData> usedCalleeNames= fAnalyzer.getUsedNames();
		for (NameData nd : usedCalleeNames) {
			if (scope.isInUse(nd.getName())) {
				String newName= scope.createName(nd.getName(), true);
				List<SimpleName> references= nd.references();
				for (SimpleName element : references) {
					ASTNode newNode= rewriter.createStringPlaceholder(newName, ASTNode.SIMPLE_NAME);
					rewriter.replace(element, newNode, null);
				}
			}
		}
	}

	private void updateImplicitReceivers(ASTRewrite rewriter, CallContext context) {
		if (context.receiver == null)
			return;
		List<Expression> implicitReceivers= fAnalyzer.getImplicitReceivers();
		for (Expression expression : implicitReceivers) {
			ASTNode node= expression;
			ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(node, context.importer);
			if (node instanceof MethodInvocation) {
				final MethodInvocation inv= (MethodInvocation)node;
				rewriter.set(inv, MethodInvocation.EXPRESSION_PROPERTY, createReceiver(rewriter, context, (IMethodBinding)inv.getName().resolveBinding(), importRewriteContext), null);
			} else if (node instanceof ClassInstanceCreation) {
				final ClassInstanceCreation inst= (ClassInstanceCreation)node;
				rewriter.set(inst, ClassInstanceCreation.EXPRESSION_PROPERTY, createReceiver(rewriter, context, inst.resolveConstructorBinding(), importRewriteContext), null);
			} else if (node instanceof ThisExpression) {
				rewriter.replace(node, rewriter.createStringPlaceholder(context.receiver, ASTNode.METHOD_INVOCATION), null);
			} else if (node instanceof FieldAccess) {
				final FieldAccess access= (FieldAccess)node;
				rewriter.set(access, FieldAccess.EXPRESSION_PROPERTY, createReceiver(rewriter, context, access.resolveFieldBinding(), importRewriteContext), null);
			} else if (node instanceof SimpleName && ((SimpleName)node).resolveBinding() instanceof IVariableBinding) {
				IVariableBinding vb= (IVariableBinding)((SimpleName)node).resolveBinding();
				if (vb.isField()) {
					Expression receiver= createReceiver(rewriter, context, vb, importRewriteContext);
					if (receiver != null) {
						if (!vb.isEnumConstant() || node.getLocationInParent() != SwitchCase.EXPRESSIONS2_PROPERTY) {
							FieldAccess access= node.getAST().newFieldAccess();
							ASTNode target= rewriter.createMoveTarget(node);
							access.setName((SimpleName)target);
							access.setExpression(receiver);
							rewriter.replace(node, access, null);
						}
					}
				}
			}
		}
	}

	private void updateTypeReferences(ASTRewrite rewriter, CallContext context) {
		ImportRewrite importer= context.importer;
		for (SimpleName simpleName : fAnalyzer.getTypesToImport()) {
			Name element= simpleName;
			ITypeBinding binding= ASTNodes.getTypeBinding(element);
			if (binding != null && !binding.isLocal()) {
				// We have collected names not types. So we have to import
				// the declaration type if we reference a parameterized type
				// since we have an entry for every name node (e.g. one for
				// Vector and one for Integer in Vector<Integer>.
				if (binding.isParameterizedType()) {
					binding= binding.getTypeDeclaration();
				}
				String[] bindingNameComponents= Bindings.getNameComponents(binding);
				try {
					IType[] types= context.compilationUnit.getAllTypes();
					for (IType type : types) {
						String typeName= type.getFullyQualifiedName();
						String[] typeNameComponents= typeName.split("\\.|\\$"); //$NON-NLS-1$
						if (Arrays.equals(bindingNameComponents, typeNameComponents)) {
							return;
						}
					}
					String s= importer.addImport(binding);
					if (!ASTNodes.asString(element).equals(s)) {
						rewriter.replace(element, rewriter.createStringPlaceholder(s, ASTNode.SIMPLE_NAME), null);
					}
				} catch (JavaModelException e) {
					// do nothing
				}
			}
		}
	}

	private void updateStaticReferences(ASTRewrite rewriter, CallContext context) {
		ImportRewrite importer= context.importer;
		for (SimpleName simpleName : fAnalyzer.getStaticsToImport()) {
			Name element= simpleName;
			IBinding binding= element.resolveBinding();
			if (binding != null) {
				String s= importer.addStaticImport(binding);
				if (!ASTNodes.asString(element).equals(s)) {
					rewriter.replace(element, rewriter.createStringPlaceholder(s, ASTNode.SIMPLE_NAME), null);
				}
			}
		}

	}

	private class UpdateStaticQualifierVisitor extends ASTVisitor {
		private final List<IVariableBinding> fTargetStaticFields;
		private final List<IMethodBinding> fTargetStaticMethods;
		private final ASTRewrite fRewriter;
		private final String fQualifierNeeded;

		public UpdateStaticQualifierVisitor(ASTRewrite rewriter, List<IVariableBinding> targetStaticFields, List<IMethodBinding> targetStaticMethods, String qualifierNeeded) {
			this.fTargetStaticFields= targetStaticFields;
			this.fTargetStaticMethods= targetStaticMethods;
			this.fRewriter= rewriter;
			this.fQualifierNeeded= qualifierNeeded;
		}

		@Override
		public boolean visit(SimpleName name) {
			if (name.getLocationInParent() == MethodInvocation.NAME_PROPERTY) {
				MethodInvocation node= (MethodInvocation) name.getParent();
				if (node.getExpression() == null) {
					IMethodBinding binding= node.resolveMethodBinding();
					if (Modifier.isStatic(binding.getModifiers())) {
						for (IMethodBinding staticMethodBinding : fTargetStaticMethods) {
							if (staticMethodBinding.getName().equals(binding.getName())) {
								AST ast= fRewriter.getAST();
								Name newName= ast.newName(fQualifierNeeded + "." + name.getFullyQualifiedName()); //$NON-NLS-1$
								fRewriter.replace(name, newName, null);
							}
						}
					}
				}
			} else {
				IBinding binding= name.resolveBinding();
				if (binding instanceof IVariableBinding varBinding && varBinding.isField()) {
					for (IVariableBinding staticFieldBinding : fTargetStaticFields) {
						if (staticFieldBinding.getName().equals(varBinding.getName())) {
							AST ast= fRewriter.getAST();
							Name newName= ast.newName(fQualifierNeeded + "." + name.getFullyQualifiedName()); //$NON-NLS-1$
							fRewriter.replace(name, newName, null);
						}
					}
				}
			}
			return false;
		}
	}
	private void updateInnerStaticReferences(ASTRewrite rewriter, CallContext context) {
		if (context.invocation instanceof MethodInvocation invocation) {
			AbstractTypeDeclaration typeDecl= ASTNodes.getFirstAncestorOrNull(invocation, AbstractTypeDeclaration.class);
			AbstractTypeDeclaration sourceTypeDecl= ASTNodes.getFirstAncestorOrNull(fDeclaration, AbstractTypeDeclaration.class);
			if (typeDecl != null && !typeDecl.isPackageMemberTypeDeclaration() && typeDecl != sourceTypeDecl) {
				ITypeBinding typeDeclBinding= typeDecl.resolveBinding();
				List<IVariableBinding> targetStaticFields= new ArrayList<>();
				List<IMethodBinding> targetStaticMethods= new ArrayList<>();
				if (typeDeclBinding != null) {
					IVariableBinding[] fields= typeDeclBinding.getDeclaredFields();
					IMethodBinding[] methods= typeDeclBinding.getDeclaredMethods();
					for (IVariableBinding field : fields) {
						if (Modifier.isStatic(field.getModifiers())) {
							targetStaticFields.add(field);
						}
					}
					for (IMethodBinding method : methods) {
						if (Modifier.isStatic(method.getModifiers())) {
							targetStaticMethods.add(method);
						}
					}
					getAccessibleStaticFieldsAndMethods(typeDeclBinding.getSuperclass(), targetStaticFields, targetStaticMethods);
					ITypeBinding[] typeInterfaces= typeDeclBinding.getInterfaces();
					for (ITypeBinding typeInterface : typeInterfaces) {
						getAccessibleStaticFieldsAndMethods(typeInterface, targetStaticFields, targetStaticMethods);
					}
					if (!targetStaticFields.isEmpty() || !targetStaticMethods.isEmpty()) {
						ITypeBinding sourceTypeBinding= sourceTypeDecl.resolveBinding();
						if (sourceTypeBinding != null) {
							String qualifiedName= sourceTypeBinding.getQualifiedName();
							String packageName= sourceTypeBinding.getPackage().getName();
							String qualifierNeeded= qualifiedName.substring(packageName.length() + 1);
							UpdateStaticQualifierVisitor visitor= new UpdateStaticQualifierVisitor(rewriter, targetStaticFields, targetStaticMethods, qualifierNeeded);
							fDeclaration.accept(visitor);
						}
					}
				}
			}
		}
	}

	private void getAccessibleStaticFieldsAndMethods(ITypeBinding typeBinding, List<IVariableBinding> fieldList, List<IMethodBinding> methodList) {
		if (typeBinding == null) {
			return;
		}
		IVariableBinding[] fields= typeBinding.getDeclaredFields();
		IMethodBinding[] methods= typeBinding.getDeclaredMethods();
		for (IVariableBinding field : fields) {
			int fieldModifiers= field.getModifiers();
			if (Modifier.isStatic(fieldModifiers) && !Modifier.isPrivate(fieldModifiers)) {
				fieldList.add(field);
			}
		}
		for (IMethodBinding method : methods) {
			int methodModifiers= method.getModifiers();
			if (Modifier.isStatic(methodModifiers) && !Modifier.isPrivate(methodModifiers)) {
				methodList.add(method);
			}
		}
		getAccessibleStaticFieldsAndMethods(typeBinding.getSuperclass(), fieldList, methodList);
		ITypeBinding[] typeInterfaces= typeBinding.getInterfaces();
		for (ITypeBinding typeInterface : typeInterfaces) {
			getAccessibleStaticFieldsAndMethods(typeInterface, fieldList, methodList);
		}
	}

	private Expression createReceiver(ASTRewrite rewriter, CallContext context, IMethodBinding method, ImportRewriteContext importRewriteContext) {
		String receiver= getReceiver(context, method.getModifiers(), importRewriteContext);
		if (receiver == null)
			return null;
		return (Expression)rewriter.createStringPlaceholder(receiver, ASTNode.METHOD_INVOCATION);
	}

	private Expression createReceiver(ASTRewrite rewriter, CallContext context, IVariableBinding field, ImportRewriteContext importRewriteContext) {
		String receiver= getReceiver(context, field.getModifiers(), importRewriteContext);
		if (receiver == null)
			return null;
		return (Expression)rewriter.createStringPlaceholder(receiver, ASTNode.SIMPLE_NAME);
	}

	private String getReceiver(CallContext context, int modifiers, ImportRewriteContext importRewriteContext) {
		String receiver= context.receiver;
		ITypeBinding invocationType= ASTNodes.getEnclosingType(context.invocation);
		ITypeBinding sourceType= fDeclaration.resolveBinding().getDeclaringClass();

		if (invocationType != null && invocationType.getName().equals(receiver)) {
			return null;
		}

		if (!context.receiverIsStatic && Modifier.isStatic(modifiers)) {
			if ("this".equals(receiver) && invocationType != null && Bindings.equals(invocationType, sourceType)) { //$NON-NLS-1$
				receiver= null;
			} else {
				receiver= context.importer.addImport(sourceType, importRewriteContext);
			}
		}
		return receiver;
	}

	private void updateTypeVariables(ASTRewrite rewriter, CallContext context) {
		ITypeBinding type= context.getReceiverType();
		if (type == null)
			return;
		rewriteReferences(rewriter, type.getTypeArguments(), fAnalyzer.getTypeParameterReferences());
	}

	private void updateMethodTypeVariable(ASTRewrite rewriter, CallContext context) {
		IMethodBinding method= Invocations.resolveBinding(context.invocation);
		if (method == null)
			return;
		rewriteReferences(rewriter, method.getTypeArguments(), fAnalyzer.getMethodTypeParameterReferences());
	}

	private void updateReturnStatements(ASTRewrite rewriter, CallContext context) {
		if (rewriter != null && context != null && context.callMode == ASTNode.YIELD_STATEMENT) {
			Block nodeToVisit= fDeclaration.getBody();
			ASTVisitor visitor= new ASTVisitor() {
				@Override
				public boolean visit(ReturnStatement node) {
					Expression exp= node.getExpression();
					YieldStatement yStmt= rewriter.getAST().newYieldStatement();
					yStmt.setExpression((Expression) rewriter.createMoveTarget(exp));
					rewriter.replace(node, yStmt, null);
					return false;
				}
			};
			nodeToVisit.accept(visitor);
		}
	}

	private void rewriteReferences(ASTRewrite rewriter, ITypeBinding[] typeArguments, List<NameData> typeParameterReferences) {
		if (typeArguments.length == 0)
			return;
		Assert.isTrue(typeArguments.length == typeParameterReferences.size());
		for (int i= 0; i < typeArguments.length; i++) {
			SourceAnalyzer.NameData refData= typeParameterReferences.get(i);
			List<SimpleName> references= refData.references();
			String newName= typeArguments[i].getName();
			for (SimpleName name : references) {
				rewriter.replace(name, rewriter.createStringPlaceholder(newName, ASTNode.SIMPLE_NAME), null);
			}
		}
	}

	private ASTNode getLastStatement() {
		List<Statement> statements= fDeclaration.getBody().statements();
		if (statements.isEmpty())
			return null;
		return statements.get(statements.size() - 1);
	}

	private List<IRegion> getReturnStatementRanges() {
		fMarkerMode= RETURN_STATEMENT_MODE;
		List<IRegion> result= new ArrayList<>(1);
		List<Statement> statements= fDeclaration.getBody().statements();
		int size= statements.size();
		if (size <= 1)
			return result;
		result.add(createRange(statements, size - 2));
		return result;
	}

	private List<IRegion> getStatementRanges() {
		fMarkerMode= STATEMENT_MODE;
		List<IRegion> result= new ArrayList<>(1);
		List<Statement> statements= fDeclaration.getBody().statements();
		int size= statements.size();
		if (size == 0)
			return result;
		result.add(createRange(statements, size - 1));
		return result;
	}

	private List<IRegion> getExpressionRanges() {
		fMarkerMode= EXPRESSION_MODE;
		List<IRegion> result= new ArrayList<>(2);
		List<Statement> statements= fDeclaration.getBody().statements();
		ReturnStatement rs= null;
		int size= statements.size();
		ASTNode node;
		switch (size) {
			case 0:
				return result;
			case 1:
				node= statements.get(0);
				if (node.getNodeType() == ASTNode.RETURN_STATEMENT) {
					rs= (ReturnStatement)node;
				} else {
					result.add(createRange(node, node));
				}
				break;
			default: {
				node= statements.get(size - 1);
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
			result.add(createRange(exp, exp));
		}
		return result;
	}

	private IRegion createRange(List<Statement> statements, int end) {
		ASTNode first= statements.get(0);
		ASTNode last= statements.get(end);
		return createRange(first, last);
	}

	private IRegion createRange(ASTNode first, ASTNode last) {
		ASTNode root= first.getRoot();
		if (root instanceof CompilationUnit) {
			CompilationUnit unit= (CompilationUnit)root;
			int start= unit.getExtendedStartPosition(first);
			int length = unit.getExtendedStartPosition(last) - start + unit.getExtendedLength(last);
			IRegion range= new Region(start, length);
			return range;
		} else {
			int start= first.getStartPosition();
			int length = last.getStartPosition() - start + last.getLength();
			IRegion range= new Region(start, length);
			return range;
		}
	}

	private String[] getBlocks(RangeMarker[] markers) throws BadLocationException {
		String[] result= new String[markers.length];
		final ICompilationUnit cu= fTypeRoot instanceof ICompilationUnit ? (ICompilationUnit) fTypeRoot : null;
		final IJavaProject project= fTypeRoot.getJavaProject();
		for (int i= 0; i < markers.length; i++) {
			RangeMarker marker= markers[i];
			String content= fDocument.get(marker.getOffset(), marker.getLength());
			String lines[]= Strings.convertIntoLines(content);
			if (cu != null) {
				Strings.trimIndentation(lines, cu, false);
			} else {
				Strings.trimIndentation(lines, project, false);
			}
			if (fMarkerMode == STATEMENT_MODE && lines.length == 2 && isSingleControlStatementWithoutBlock()) {
				lines[1]= cu != null ? CodeFormatterUtil.createIndentString(1, cu) + lines[1] : CodeFormatterUtil.createIndentString(1, project) + lines[1];
			}
			result[i]= Strings.concatenate(lines, TextUtilities.getDefaultLineDelimiter(fDocument));
		}
		return result;
	}

	private boolean isSingleControlStatementWithoutBlock() {
		List<Statement> statements= fDeclaration.getBody().statements();
		int size= statements.size();
		if (size != 1)
			return false;
		Statement statement= statements.get(size - 1);
		int nodeType= statement.getNodeType();
		if (nodeType == ASTNode.IF_STATEMENT) {
			IfStatement ifStatement= (IfStatement) statement;
			return !(ifStatement.getThenStatement() instanceof Block)
				&& !(ifStatement.getElseStatement() instanceof Block);
		} else if (nodeType == ASTNode.FOR_STATEMENT) {
			return !(((ForStatement)statement).getBody() instanceof Block);
		} else if (nodeType == ASTNode.WHILE_STATEMENT) {
			return !(((WhileStatement)statement).getBody() instanceof Block);
		}
		return false;
	}

	public RefactoringStatus checkAccessCompatible(ASTNode targetNode) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (fAnalyzer.onlyAccessesPublic()) {
			return result;
		}
		IMethodBinding declBinding= fDeclaration.resolveBinding();
		if (declBinding == null) {
			result.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_methoddeclaration_has_errors, JavaStatusContext.create(fTypeRoot));
			return result;
		}
		ASTNode targetRoot= targetNode.getRoot();
		if (targetRoot instanceof CompilationUnit cu && (fTypeRoot instanceof ICompilationUnit rootICU)) {
			AbstractTypeDeclaration typeNode= ASTNodes.getTopLevelTypeDeclaration(targetNode);
			AbstractTypeDeclaration declTypeNode= ASTNodes.getTopLevelTypeDeclaration(fDeclaration);
			if (typeNode != null && declTypeNode != null) {
				ITypeBinding typeNodeBinding= typeNode.resolveBinding();
				ITypeBinding declTypeNodeBinding= declTypeNode.resolveBinding();
				if (typeNodeBinding != null && declTypeNodeBinding != null) {
					if (typeNodeBinding.isEqualTo(declTypeNodeBinding)) {
						return result;
					}
				}
			}
			if (fAnalyzer.accessesPrivate()) {
				result.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_methoddeclaration_accesses_private, JavaStatusContext.create(fTypeRoot));
				return result;
			}
			PackageDeclaration packageDecl= cu.getPackage();
			IPackageDeclaration[] rootPackageDecls= rootICU.getPackageDeclarations();
			if (packageDecl.getName().getFullyQualifiedName().equals(rootPackageDecls[0].getElementName())) {
				return result;
			}
			if (fAnalyzer.accessesPackagePrivate()) {
				result.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_methoddeclaration_accesses_package_private, JavaStatusContext.create(fTypeRoot));
				return result;
			}
			ITypeBinding typeBinding= declBinding.getDeclaringClass();
			if (typeBinding == null) {
				result.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_methoddeclaration_has_errors, JavaStatusContext.create(fTypeRoot));
				return result;
			}
			if (typeNode != null) {
				ITypeBinding targetTypeBinding= typeNode.resolveBinding();
				if (targetTypeBinding != null) {
					if (ASTNodes.findImplementedType(targetTypeBinding, typeBinding.getQualifiedName()) == null
							|| (targetNode instanceof MethodInvocation invocation && invocation.getExpression() != null && !(invocation.getExpression() instanceof ThisExpression))) {
						result.addFatalError(RefactoringCoreMessages.InlineMethodRefactoring_SourceAnalyzer_methoddeclaration_accesses_protected, JavaStatusContext.create(fTypeRoot));
					}
				}
			}
		}
		return result;
	}
}
