/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
 *     Red Hat Inc. - moved static methods from QuickAssistProcessor to here
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IDocElement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.text.correction.proposals.NewDefiningMethodProposalCore;

public class QuickAssistProcessorUtil {

	/**
	 * Utility class to find if there is an unqualified reference to a name made that doesn't match
	 * a specified binding.  Used to verify that adding a static import won't cause a logic change in the
	 * Compilation Unit.
	 */
	public static class UnqualifiedReferencesFinder extends ASTVisitor {
		private final String fName;
		private final IBuffer fBuffer;
		private final IBinding fBinding;

		public UnqualifiedReferencesFinder(String name, ICompilationUnit icu, IBinding binding) throws JavaModelException {
			fName= name;
			fBuffer= icu.getBuffer();
			fBinding= binding;
		}

		@Override
		public boolean visit(SimpleName node) {
			if (node.getFullyQualifiedName().equals(fName)) {
				if (node.getStartPosition() > 0 && fBuffer.getChar(node.getStartPosition() - 1) != '.') {
					IBinding binding= node.resolveBinding();
					if (binding != null && binding.getKind() == fBinding.getKind() && !binding.isEqualTo(fBinding)) {
						throw new AbortSearchException();
					}
				}
			}
			return false;
		}
	}

	/**
	 * Returns the functional interface method being implemented by the given method reference.
	 *
	 * @param methodReference the method reference to get the functional method
	 * @return the functional interface method being implemented by <code>methodReference</code> or
	 *         <code>null</code> if it could not be derived
	 */
	public static IMethodBinding getFunctionalMethodForMethodReference(MethodReference methodReference) {
		ITypeBinding targetTypeBinding= ASTNodes.getTargetType(methodReference);
		if (targetTypeBinding == null)
			return null;

		IMethodBinding functionalMethod= targetTypeBinding.getFunctionalInterfaceMethod();
		if (functionalMethod != null && functionalMethod.isSynthetic()) {
			functionalMethod= Bindings.findOverriddenMethodInType(functionalMethod.getDeclaringClass(), functionalMethod);
		}
		return functionalMethod;
	}

	/**
	 * Converts and replaces the given method reference with corresponding lambda expression in the
	 * given ASTRewrite.
	 *
	 * @param methodReference the method reference to convert
	 * @param functionalMethod the non-generic functional interface method to be implemented by the
	 *            lambda expression
	 * @param astRoot the AST root
	 * @param rewrite the ASTRewrite
	 * @param linkedProposalModel to create linked proposals for lambda's parameters or
	 *            <code>null</code> if linked proposals are not required
	 * @param createBlockBody <code>true</code> if lambda expression's body should be a block
	 *
	 * @return lambda expression used to replace the method reference in the given ASTRewrite
	 * @throws JavaModelException if an exception occurs while accessing the Java element
	 *             corresponding to the <code>functionalMethod</code>
	 */
	public static LambdaExpression convertMethodRefernceToLambda(MethodReference methodReference, IMethodBinding functionalMethod, CompilationUnit astRoot,
			ASTRewrite rewrite, LinkedProposalModelCore linkedProposalModel, boolean createBlockBody) throws JavaModelException {

		AST ast= astRoot.getAST();
		LambdaExpression lambda= ast.newLambdaExpression();

		String[] lambdaParamNames= QuickAssistProcessorUtil.getUniqueParameterNames(methodReference, functionalMethod);
		List<VariableDeclaration> lambdaParameters= lambda.parameters();
		for (int i= 0; i < lambdaParamNames.length; i++) {
			String paramName= lambdaParamNames[i];
			VariableDeclarationFragment lambdaParameter= ast.newVariableDeclarationFragment();
			SimpleName name= ast.newSimpleName(paramName);
			lambdaParameter.setName(name);
			lambdaParameters.add(lambdaParameter);
			if (linkedProposalModel != null) {
				linkedProposalModel.getPositionGroup(name.getIdentifier(), true).addPosition(rewrite.track(name), i == 0);
			}
		}

		int noOfLambdaParameters= lambdaParamNames.length;
		lambda.setParentheses(noOfLambdaParameters != 1);

		ITypeBinding returnTypeBinding= functionalMethod.getReturnType();
		IMethodBinding referredMethodBinding= methodReference.resolveMethodBinding(); // too often null, see bug 440000, bug 440344, bug 333665

		if (methodReference instanceof CreationReference) {
			CreationReference creationRef= (CreationReference) methodReference;
			Type type= creationRef.getType();
			if (type instanceof ArrayType) {
				ArrayCreation arrayCreation= ast.newArrayCreation();
				if (createBlockBody) {
					Block blockBody= QuickAssistProcessorUtil.getBlockBodyForLambda(arrayCreation, returnTypeBinding, ast);
					lambda.setBody(blockBody);
				} else {
					lambda.setBody(arrayCreation);
				}

				ArrayType arrayType= (ArrayType) type;
				Type copiedElementType= (Type) rewrite.createCopyTarget(arrayType.getElementType());
				arrayCreation.setType(ast.newArrayType(copiedElementType, arrayType.getDimensions()));
				SimpleName name= ast.newSimpleName(lambdaParamNames[0]);
				arrayCreation.dimensions().add(name);
				if (linkedProposalModel != null) {
					linkedProposalModel.getPositionGroup(name.getIdentifier(), false).addPosition(rewrite.track(name), LinkedPositionGroup.NO_STOP);
				}
			} else {
				ClassInstanceCreation cic= ast.newClassInstanceCreation();
				if (createBlockBody) {
					Block blockBody= QuickAssistProcessorUtil.getBlockBodyForLambda(cic, returnTypeBinding, ast);
					lambda.setBody(blockBody);
				} else {
					lambda.setBody(cic);
				}

				ITypeBinding typeBinding= type.resolveBinding();
				if (!(type instanceof ParameterizedType) && typeBinding != null && typeBinding.getTypeDeclaration().isGenericType()) {
					cic.setType(ast.newParameterizedType((Type) rewrite.createCopyTarget(type)));
				} else {
					cic.setType((Type) rewrite.createCopyTarget(type));
				}
				List<SimpleName> invocationArgs= QuickAssistProcessorUtil.getInvocationArguments(ast, 0, noOfLambdaParameters, lambdaParamNames);
				cic.arguments().addAll(invocationArgs);
				if (linkedProposalModel != null) {
					for (SimpleName name : invocationArgs) {
						linkedProposalModel.getPositionGroup(name.getIdentifier(), false).addPosition(rewrite.track(name), LinkedPositionGroup.NO_STOP);
					}
				}
				cic.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, methodReference.typeArguments()));
			}

		} else if (referredMethodBinding != null && Modifier.isStatic(referredMethodBinding.getModifiers())) {
			MethodInvocation methodInvocation= ast.newMethodInvocation();
			if (createBlockBody) {
				Block blockBody= QuickAssistProcessorUtil.getBlockBodyForLambda(methodInvocation, returnTypeBinding, ast);
				lambda.setBody(blockBody);
			} else {
				lambda.setBody(methodInvocation);
			}

			Expression expr= null;
			boolean hasConflict= QuickAssistProcessorUtil.hasConflict(methodReference.getStartPosition(), referredMethodBinding, ScopeAnalyzer.METHODS | ScopeAnalyzer.CHECK_VISIBILITY, astRoot);
			if (hasConflict || !Bindings.isSuperType(referredMethodBinding.getDeclaringClass(), ASTNodes.getEnclosingType(methodReference)) || methodReference.typeArguments().size() != 0) {
				if (methodReference instanceof ExpressionMethodReference) {
					ExpressionMethodReference expressionMethodReference= (ExpressionMethodReference) methodReference;
					expr= (Expression) rewrite.createCopyTarget(expressionMethodReference.getExpression());
				} else if (methodReference instanceof TypeMethodReference) {
					Type type= ((TypeMethodReference) methodReference).getType();
					ITypeBinding typeBinding= type.resolveBinding();
					if (typeBinding != null) {
						ImportRewrite importRewrite= StubUtility.createImportRewrite(astRoot, true);
						expr= ast.newName(importRewrite.addImport(typeBinding));
					}
				}
			}
			methodInvocation.setExpression(expr);
			SimpleName methodName= QuickAssistProcessorUtil.getMethodInvocationName(methodReference);
			methodInvocation.setName((SimpleName) rewrite.createCopyTarget(methodName));
			List<SimpleName> invocationArgs= QuickAssistProcessorUtil.getInvocationArguments(ast, 0, noOfLambdaParameters, lambdaParamNames);
			methodInvocation.arguments().addAll(invocationArgs);
			if (linkedProposalModel != null) {
				for (SimpleName name : invocationArgs) {
					linkedProposalModel.getPositionGroup(name.getIdentifier(), false).addPosition(rewrite.track(name), LinkedPositionGroup.NO_STOP);
				}
			}
			methodInvocation.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, methodReference.typeArguments()));

		} else if (methodReference instanceof SuperMethodReference) {
			SuperMethodInvocation superMethodInvocation= ast.newSuperMethodInvocation();
			if (createBlockBody) {
				Block blockBody= QuickAssistProcessorUtil.getBlockBodyForLambda(superMethodInvocation, returnTypeBinding, ast);
				lambda.setBody(blockBody);
			} else {
				lambda.setBody(superMethodInvocation);
			}

			Name superQualifier= ((SuperMethodReference) methodReference).getQualifier();
			if (superQualifier != null) {
				superMethodInvocation.setQualifier((Name) rewrite.createCopyTarget(superQualifier));
			}
			SimpleName methodName= QuickAssistProcessorUtil.getMethodInvocationName(methodReference);
			superMethodInvocation.setName((SimpleName) rewrite.createCopyTarget(methodName));
			List<SimpleName> invocationArgs= QuickAssistProcessorUtil.getInvocationArguments(ast, 0, noOfLambdaParameters, lambdaParamNames);
			superMethodInvocation.arguments().addAll(invocationArgs);
			if (linkedProposalModel != null) {
				for (SimpleName name : invocationArgs) {
					linkedProposalModel.getPositionGroup(name.getIdentifier(), false).addPosition(rewrite.track(name), LinkedPositionGroup.NO_STOP);
				}
			}
			superMethodInvocation.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, methodReference.typeArguments()));

		} else {
			MethodInvocation methodInvocation= ast.newMethodInvocation();
			if (createBlockBody) {
				Block blockBody= QuickAssistProcessorUtil.getBlockBodyForLambda(methodInvocation, returnTypeBinding, ast);
				lambda.setBody(blockBody);
			} else {
				lambda.setBody(methodInvocation);
			}

			boolean isTypeReference= QuickAssistProcessorUtil.isTypeReferenceToInstanceMethod(methodReference);
			if (isTypeReference) {
				SimpleName name= ast.newSimpleName(lambdaParamNames[0]);
				methodInvocation.setExpression(name);
				if (linkedProposalModel != null) {
					linkedProposalModel.getPositionGroup(name.getIdentifier(), false).addPosition(rewrite.track(name), LinkedPositionGroup.NO_STOP);
				}
			} else {
				Expression expr= ((ExpressionMethodReference) methodReference).getExpression();
				if (!(expr instanceof ThisExpression) || (methodReference.typeArguments().size() != 0)) {
					methodInvocation.setExpression((Expression) rewrite.createCopyTarget(expr));
				}
			}
			SimpleName methodName= QuickAssistProcessorUtil.getMethodInvocationName(methodReference);
			methodInvocation.setName((SimpleName) rewrite.createCopyTarget(methodName));
			List<SimpleName> invocationArgs= QuickAssistProcessorUtil.getInvocationArguments(ast, isTypeReference ? 1 : 0, noOfLambdaParameters, lambdaParamNames);
			methodInvocation.arguments().addAll(invocationArgs);
			if (linkedProposalModel != null) {
				for (SimpleName name : invocationArgs) {
					linkedProposalModel.getPositionGroup(name.getIdentifier(), false).addPosition(rewrite.track(name), LinkedPositionGroup.NO_STOP);
				}
			}
			methodInvocation.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, methodReference.typeArguments()));
		}

		rewrite.replace(methodReference, lambda, null);
		return lambda;
	}

	public static Block getBlockBodyForLambda(Expression bodyExpr, ITypeBinding returnTypeBinding, AST ast) {
		Statement statementInBlockBody;
		if (ast.resolveWellKnownType("void").isEqualTo(returnTypeBinding)) { //$NON-NLS-1$
			ExpressionStatement expressionStatement= ast.newExpressionStatement(bodyExpr);
			statementInBlockBody= expressionStatement;
		} else {
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(bodyExpr);
			statementInBlockBody= returnStatement;
		}
		Block blockBody= ast.newBlock();
		blockBody.statements().add(statementInBlockBody);
		return blockBody;
	}

	public static List<Type> getCopiedTypeArguments(ASTRewrite rewrite, List<Type> typeArguments) {
		List<Type> copiedTypeArgs= new ArrayList<>();
		for (Type typeArg : typeArguments) {
			copiedTypeArgs.add((Type) rewrite.createCopyTarget(typeArg));
		}
		return copiedTypeArgs;
	}

	private static SimpleName getMethodInvocationName(MethodReference methodReference) {
		SimpleName name= null;
		if (methodReference instanceof ExpressionMethodReference) {
			name= ((ExpressionMethodReference) methodReference).getName();
		} else if (methodReference instanceof TypeMethodReference) {
			name= ((TypeMethodReference) methodReference).getName();
		} else if (methodReference instanceof SuperMethodReference) {
			name= ((SuperMethodReference) methodReference).getName();
		}
		return name;
	}

	public static String[] getUniqueParameterNames(MethodReference methodReference, IMethodBinding functionalMethod) throws JavaModelException {
		String[] originalParameterNames= ((IMethod) functionalMethod.getJavaElement()).getParameterNames();
		String[] newNames= new String[originalParameterNames.length];
		Set<String> excludedNames= new HashSet<>(ASTNodes.getVisibleLocalVariablesInScope(methodReference));

		for (int i= 0; i < originalParameterNames.length; i++) {
			String paramName= originalParameterNames[i];

			if (excludedNames.contains(paramName)) {
				Set<String> allNamesToExclude= new HashSet<>(excludedNames);
				Collections.addAll(allNamesToExclude, originalParameterNames);

				String newParamName= QuickAssistProcessorUtil.createName(paramName, allNamesToExclude);

				excludedNames.add(newParamName);
				newNames[i]= newParamName;
			} else {
				newNames[i]= paramName;
			}
		}

		return newNames;
	}

	private static String createName(final String nameRoot, final Set<String> excludedNames) {
		int i= 1;
		String candidate;

		do {
			candidate= nameRoot + i++;
		} while (excludedNames.remove(candidate));

		return candidate;
	}

	private static List<SimpleName> getInvocationArguments(AST ast, int begIndex, int noOfLambdaParameters, String[] lambdaParamNames) {
		List<SimpleName> args= new ArrayList<>();
		for (int i= begIndex; i < noOfLambdaParameters; i++) {
			args.add(ast.newSimpleName(lambdaParamNames[i]));
		}
		return args;
	}

	private static boolean hasConflict(int startPosition, IMethodBinding referredMethodBinding, int flags, CompilationUnit cu) {
		ScopeAnalyzer analyzer= new ScopeAnalyzer(cu);
		for (IBinding decl : analyzer.getDeclarationsInScope(startPosition, flags)) {
			if (decl.getName().equals(referredMethodBinding.getName()) && !referredMethodBinding.getMethodDeclaration().isEqualTo(decl))
				return true;
		}
		return false;
	}

	public static boolean isTypeReferenceToInstanceMethod(MethodReference methodReference) {
		if (methodReference instanceof TypeMethodReference)
			return true;
		if (methodReference instanceof ExpressionMethodReference) {
			Expression expression= ((ExpressionMethodReference) methodReference).getExpression();
			if (expression instanceof Name) {
				IBinding nameBinding= ((Name) expression).resolveBinding();
				if (nameBinding instanceof ITypeBinding) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Changes the expression body of the given lambda expression to block form.
	 * {@link LambdaExpression#resolveMethodBinding()} should not be <code>null</code> for the given
	 * lambda expression.
	 *
	 * @param lambda the lambda expression to convert the body form
	 * @param ast the AST to create new nodes
	 * @param rewrite the ASTRewrite
	 */
	public static void changeLambdaBodyToBlock(LambdaExpression lambda, AST ast, ASTRewrite rewrite) {
		Expression bodyExpr= (Expression) rewrite.createMoveTarget(lambda.getBody());
		Block blockBody= getBlockBodyForLambda(bodyExpr, lambda.resolveMethodBinding().getReturnType(), ast);
		rewrite.set(lambda, LambdaExpression.BODY_PROPERTY, blockBody, null);
	}

	/**
	 * Return the first auto closable nodes. When a node that isn't Autoclosable is found the method
	 * returns.
	 *
	 * @param astNodes The nodes covered.
	 * @return List of the first AutoClosable nodes found
	 */
	public static List<ASTNode> getCoveredAutoClosableNodes(List<ASTNode> astNodes) {
		List<ASTNode> autoClosableNodes= new ArrayList<>();
		for (ASTNode astNode : astNodes) {
			if (isAutoClosable(astNode)) {
				autoClosableNodes.add(astNode);
			} else {
				return autoClosableNodes;
			}
		}
		return autoClosableNodes;
	}

	public static int findEndPostion(ASTNode node) {
		int end= node.getStartPosition() + node.getLength();
		Map<SimpleName, IVariableBinding> nodeSimpleNameBindings= getVariableStatementBinding(node);
		List<SimpleName> nodeNames= new ArrayList<>(nodeSimpleNameBindings.keySet());
		if (nodeNames.isEmpty()) {
			return -1;
		}
		SimpleName nodeSimpleName= nodeNames.get(0);
		SimpleName[] coveredNodeBindings= LinkedNodeFinder.findByNode(node.getRoot(), nodeSimpleName);
		if (coveredNodeBindings.length == 0) {
			return -1;
		}
		for (ASTNode astNode : coveredNodeBindings) {
			end= Math.max(end, (astNode.getStartPosition() + astNode.getLength()));
		}
		return end;
	}

	public static Map<SimpleName, IVariableBinding> getVariableStatementBinding(ASTNode astNode) {
		Map<SimpleName, IVariableBinding> variableBindings= new HashMap<>();
		astNode.accept(new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationStatement node) {
				for (Object o : node.fragments()) {
					if (o instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment vdf= (VariableDeclarationFragment) o;
						SimpleName name= vdf.getName();
						IBinding binding= name.resolveBinding();
						if (binding instanceof IVariableBinding) {
							variableBindings.put(name, (IVariableBinding) binding);
							break;
						}
					}
				}
				return false;
			}
		});
		return variableBindings;
	}

	public static boolean isAutoClosable(ITypeBinding typeBinding) {
		return Bindings.findTypeInHierarchy(typeBinding, "java.lang.AutoCloseable") != null; //$NON-NLS-1$
	}

	public static boolean isAutoClosable(ASTNode astNode) {
		Map<SimpleName, IVariableBinding> simpleNames= getVariableStatementBinding(astNode);
		for (Entry<SimpleName, IVariableBinding> entry : simpleNames.entrySet()) {
			ITypeBinding typeBinding= null;
			switch (entry.getKey().getParent().getNodeType()) {
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				case ASTNode.ASSIGNMENT:
					typeBinding= entry.getValue().getType();
					break;
				default:
					continue;
			}
			if (typeBinding != null && isAutoClosable(typeBinding)) {
				return true;
			}
		}
		return false;
	}

	public static int getIndex(int offset, List<Statement> statements) {
		for (int i= 0; i < statements.size(); i++) {
			Statement s= statements.get(i);
			if (offset <= s.getStartPosition()) {
				return i;
			}
			if (offset < s.getStartPosition() + s.getLength()) {
				return -1;
			}
		}
		return statements.size();
	}

	public static boolean isDeprecatedMethodCallWithReplacement(ASTNode node) {
		if (!(node instanceof MethodInvocation)) {
			node= node.getParent();
			if (!(node instanceof MethodInvocation)) {
				return false;
			}
		}
		MethodInvocation methodInvocation= (MethodInvocation) node;
		IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
		if (methodBinding == null) {
			return false;
		}
		IMethod method= (IMethod)methodBinding.getJavaElement();
		if (method == null) {
			return false;
		}
		IAnnotationBinding[] annotations= methodBinding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			if (annotation.getAnnotationType().getQualifiedName().equals("java.lang.Deprecated")) { //$NON-NLS-1$
				CompilationUnit sourceCu= (CompilationUnit)node.getRoot();
				CompilationUnit cu= findCUForMethod(sourceCu, (ICompilationUnit)sourceCu.getJavaElement(), methodBinding);
				if (cu == null) {
					return false;
				}
				try {
					MethodDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, cu);
					Javadoc javadoc= methodDeclaration.getJavadoc();
					if (javadoc == null) {
						return false;
					}
					List<TagElement> tags= javadoc.tags();
					for (TagElement tag : tags) {
						if ("@deprecated".equals(tag.getTagName())) { //$NON-NLS-1$
							List<IDocElement> fragments= tag.fragments();
							if (fragments.size() < 2) {
								return false;
							}
							if (fragments.get(0) instanceof TextElement textElement) {
								String text= textElement.getText().toLowerCase().trim();
								if (text.endsWith("use") || text.endsWith("replace by")) { //$NON-NLS-1$ //$NON-NLS-2$
									if (fragments.get(1) instanceof TagElement tagElement) {
										if ("@link".equals(tagElement.getTagName())) { //$NON-NLS-1$
											List<IDocElement> linkFragments= tagElement.fragments();
											if (linkFragments.size() == 1) {
												IDocElement linkFragment= linkFragments.get(0);
												if (linkFragment instanceof MethodRef methodRef) {
													IMethodBinding refBinding= (IMethodBinding) methodRef.resolveBinding();
													if (refBinding != null) {
														class FindNewMethodVisitor extends ASTVisitor {
															private boolean useMethodIsUsed= false;
															private boolean referencesPrivate= false;
															private boolean referencesProtected= false;
															private boolean referencesPackagePrivate= false;
															@Override
															public boolean visit(MethodInvocation invocation) {
																IMethodBinding binding= invocation.resolveMethodBinding();
																if (binding != null) {
																	if (binding.isEqualTo(refBinding)) {
																		useMethodIsUsed= true;
																	} else {
																		int modifiers= binding.getModifiers();
																		if (Modifier.isPrivate(modifiers)) {
																			referencesPrivate= true;
																		} else if (Modifier.isProtected(modifiers)) {
																			referencesProtected= true;
																		} else if (Modifier.isPublic(modifiers)) {
																			// do nothing
																		} else {
																			referencesPackagePrivate= true;
																		}
																	}
																}
																return true;
															}
															@Override
															public boolean visit(SimpleName name) {
																IBinding binding= name.resolveBinding();
																if (binding instanceof IVariableBinding varBinding
																		&& varBinding.isField()) {
																	int modifiers= varBinding.getModifiers();
																	if (Modifier.isPrivate(modifiers)) {
																		referencesPrivate= true;
																	} else if (Modifier.isProtected(modifiers)) {
																		referencesProtected= true;
																	} else if (Modifier.isPublic(modifiers)) {
																		// do nothing
																	} else {
																		referencesPackagePrivate= true;
																	}
																}
																if (binding instanceof ITypeBinding typeBinding) {
																	int modifiers= typeBinding.getModifiers();
																	if (Modifier.isPrivate(modifiers)) {
																		referencesPrivate= true;
																	} else if (Modifier.isProtected(modifiers)) {
																		referencesProtected= true;
																	} else if (Modifier.isPublic(modifiers)) {
																		// do nothing
																	} else {
																		referencesPackagePrivate= true;
																	}
																}
																return true;
															}
															public boolean isUseMethodUsed() {
																return useMethodIsUsed;
															}
															public boolean referencesPrivate() {
																return referencesPrivate;
															}
															public boolean referencesProtected() {
																return referencesProtected;
															}
															public boolean referencesPackagePrivate() {
																return referencesPackagePrivate;

															}
														}
														FindNewMethodVisitor findNewMethodVisitor= new FindNewMethodVisitor();
														methodDeclaration.accept(findNewMethodVisitor);
														if (!findNewMethodVisitor.isUseMethodUsed()) {
															return false;
														}
														// check access modifiers to ensure that method can be inlined
														// without causing an error
														CompilationUnit cu1= (CompilationUnit)methodInvocation.getRoot();
														CompilationUnit cu2= (CompilationUnit)methodDeclaration.getRoot();
														TypeDeclaration typeDecl2= ASTNodes.getFirstAncestorOrNull(methodDeclaration, TypeDeclaration.class);
														if (typeDecl2 == null) {
															return false;
														}
														int methodDeclarationTypeModifiers= typeDecl2.getModifiers();
														ITypeBinding typeDeclBinding2= typeDecl2.resolveBinding();
														if (findNewMethodVisitor.referencesPrivate() ||
																Modifier.isPrivate(methodDeclarationTypeModifiers)) {
															if (methodInvocation.getRoot() != methodDeclaration.getRoot()) {
																return false;
															}
														} else if (findNewMethodVisitor.referencesProtected() ||
																Modifier.isProtected(methodDeclarationTypeModifiers) ||
																findNewMethodVisitor.referencesPackagePrivate() ||
																!Modifier.isPublic(methodDeclarationTypeModifiers)) {
															String pkgName1= cu1.getPackage().getName().getFullyQualifiedName();
															String pkgName2= cu2.getPackage().getName().getFullyQualifiedName();
															if (pkgName1 == null || pkgName2 == null) {
																return false;
															}
															if (pkgName1.equals(pkgName2)) {
																return true;
															}
															if (findNewMethodVisitor.referencesPackagePrivate() ||
																	!Modifier.isProtected(methodDeclarationTypeModifiers)) {
																return false;
															}
															TypeDeclaration typeDecl1= ASTNodes.getFirstAncestorOrNull(methodInvocation, TypeDeclaration.class);
															if (typeDecl1 == null) {
																return false;
															}
															ITypeBinding typeDeclBinding1= typeDecl1.resolveBinding();
															if (typeDeclBinding1 == null || typeDeclBinding2 == null) {
																return false;
															}
															// if methodDeclaration is in superclass, that is ok, otherwise no
															while (typeDeclBinding1 != null) {
																if (typeDeclBinding1.isEqualTo(typeDeclBinding2)) {
																	return true;
																}
																typeDeclBinding1= typeDeclBinding1.getSuperclass();
															}
															return false;
														}
														return true;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
		return false;

	}

	public static CompilationUnit findCUForMethod(CompilationUnit compilationUnit, ICompilationUnit cu, IMethodBinding methodBinding) {
		ASTNode methodDecl= compilationUnit.findDeclaringNode(methodBinding.getMethodDeclaration());
		if (methodDecl == null) {
			// is methodDecl defined in another CU?
			ITypeBinding declaringTypeDecl= methodBinding.getDeclaringClass().getTypeDeclaration();
			if (declaringTypeDecl.isFromSource()) {
				ICompilationUnit targetCU= null;
				try {
					targetCU= ASTResolving.findCompilationUnitForBinding(cu, compilationUnit, declaringTypeDecl);
				} catch (JavaModelException e) { /* can't do better */
				}
				if (targetCU != null) {
					return ASTResolving.createQuickFixAST(targetCU, null);
				}
			}
			return null;
		}
		return compilationUnit;
	}

	public static String getDeprecatedFieldReplacement(ASTNode node) {
		IBinding binding= null;
		switch (node) {
			case QualifiedName q:
				binding= q.resolveBinding();
				break;
			case SimpleName s:
				if (s.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
					node= s.getParent();
					binding= ((QualifiedName)node).resolveBinding();
				} else {
					binding= s.resolveBinding();
				}
				break;
			case FieldAccess f:
//				if (f.getExpression() instanceof MethodInvocation) {
//					return null;
//				}
				binding= f.resolveFieldBinding();
				break;
			case SuperFieldAccess sf:
				binding= sf.resolveFieldBinding();
				break;
			default:
				return null;
		}
		if (binding instanceof IVariableBinding varBinding && varBinding.isField()) {
			IField field= (IField)varBinding.getJavaElement();
			if (field == null) {
				return null;
			}
			IAnnotationBinding[] annotations= varBinding.getAnnotations();
			for (IAnnotationBinding annotation : annotations) {
				if (annotation.getAnnotationType().getQualifiedName().equals("java.lang.Deprecated")) { //$NON-NLS-1$
					CompilationUnit sourceCu= (CompilationUnit)node.getRoot();
					CompilationUnit cu= findCUForField(sourceCu, (ICompilationUnit)sourceCu.getJavaElement(), varBinding);
					if (cu == null) {
						return null;
					}
					try {
						FieldDeclaration fieldDeclaration= ASTNodeSearchUtil.getFieldDeclarationNode(field, cu);
						Javadoc javadoc= fieldDeclaration.getJavadoc();
						if (javadoc == null) {
							return null;
						}
						List<TagElement> tags= javadoc.tags();
						for (TagElement tag : tags) {
							if ("@deprecated".equals(tag.getTagName())) { //$NON-NLS-1$
								List<IDocElement> fragments= tag.fragments();
								if (fragments.size() < 2) {
									return null;
								}
								if (fragments.get(0) instanceof TextElement textElement) {
									String text= textElement.getText().toLowerCase().trim();
									if (text.endsWith("use") || text.endsWith("replace by")) { //$NON-NLS-1$ //$NON-NLS-2$
										if (fragments.get(1) instanceof TagElement tagElement) {
											if ("@link".equals(tagElement.getTagName())) { //$NON-NLS-1$
												List<IDocElement> linkFragments= tagElement.fragments();
												if (linkFragments.size() == 1) {
													IDocElement linkFragment= linkFragments.get(0);
													if (linkFragment instanceof MemberRef methodRef) {
														IBinding refBinding= methodRef.resolveBinding();
														if (refBinding instanceof IVariableBinding replaceBinding && replaceBinding.isField()) {
															return replaceBinding.getDeclaringClass().getQualifiedName() + "." + replaceBinding.getName(); //$NON-NLS-1$
														}
													}
												}
											}
										}
									}
								}
							}
						}
					} catch (JavaModelException e) {
						// ignore
					}
				}
			}
		}
		return null;
	}

	public static CompilationUnit findCUForField(CompilationUnit compilationUnit, ICompilationUnit cu, IVariableBinding fieldBinding) {
		ASTNode fieldDecl= compilationUnit.findDeclaringNode(fieldBinding.getVariableDeclaration());
		if (fieldDecl == null) {
			// is field defined in another CU?
			ITypeBinding declaringTypeDecl= fieldBinding.getDeclaringClass().getTypeDeclaration();
			if (declaringTypeDecl.isFromSource()) {
				ICompilationUnit targetCU= null;
				try {
					targetCU= ASTResolving.findCompilationUnitForBinding(cu, compilationUnit, declaringTypeDecl);
				} catch (JavaModelException e) { /* can't do better */
				}
				if (targetCU != null) {
					return ASTResolving.createQuickFixAST(targetCU, null);
				}
			}
			return null;
		}
		return compilationUnit;
	}

	public static ASTNode getCopyOfInner(ASTRewrite rewrite, ASTNode statement, boolean toControlStatementBody) {
		if (statement.getNodeType() == ASTNode.BLOCK) {
			Block block= (Block) statement;
			List<Statement> innerStatements= block.statements();
			int nStatements= innerStatements.size();
			if (nStatements == 1) {
				return rewrite.createCopyTarget(innerStatements.get(0));
			} else if (nStatements > 1) {
				if (toControlStatementBody) {
					return rewrite.createCopyTarget(block);
				}
				ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				ASTNode first= innerStatements.get(0);
				ASTNode last= innerStatements.get(nStatements - 1);
				return listRewrite.createCopyTarget(first, last);
			}
			return null;
		} else {
			return rewrite.createCopyTarget(statement);
		}
	}

	public static boolean getCreateInSuperClassProposals(IInvocationContext context, ASTNode node, Collection<Object> resultingCollections) throws CoreException {
		if (!(node instanceof SimpleName) || !(node.getParent() instanceof MethodDeclaration)) {
			return false;
		}
		MethodDeclaration decl= (MethodDeclaration) node.getParent();
		if (decl.getName() != node || decl.resolveBinding() == null || Modifier.isPrivate(decl.getModifiers())) {
			return false;
		}

		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();

		IMethodBinding binding= decl.resolveBinding();
		ITypeBinding[] paramTypes= binding.getParameterTypes();

		ITypeBinding[] superTypes= Bindings.getAllSuperTypes(binding.getDeclaringClass());
		if (resultingCollections == null) {
			for (ITypeBinding curr : superTypes) {
				if (curr.isFromSource() && Bindings.findOverriddenMethodInType(curr, binding) == null) {
					return true;
				}
			}
			return false;
		}
		List<SingleVariableDeclaration> params= decl.parameters();
		String[] paramNames= new String[paramTypes.length];
		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration param= params.get(i);
			paramNames[i]= param.getName().getIdentifier();
		}

		for (ITypeBinding curr : superTypes) {
			if (curr.isFromSource()) {
				IMethodBinding method= Bindings.findOverriddenMethodInType(curr, binding);
				if (method == null) {
					ITypeBinding typeDecl= curr.getTypeDeclaration();
					ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, typeDecl);
					if (targetCU != null) {
						String label= Messages.format(CorrectionMessages.QuickAssistProcessor_createmethodinsuper_description,
								new String[] { BasicElementLabels.getJavaElementName(curr.getName()), BasicElementLabels.getJavaElementName(binding.getName()) });
						resultingCollections.add(new NewDefiningMethodProposalCore(label, targetCU, astRoot, typeDecl, binding, paramNames, IProposalRelevance.CREATE_METHOD_IN_SUPER));
					}
				}
			}
		}
		return true;
	}

}
