/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Copied from package org.eclipse.jdt.internal.corext.fix.LambdaExpressionsFix
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jerome Cambon <jerome.cambon@oracle.com> - [1.8][clean up][quick assist] Convert lambda to anonymous must qualify references to 'this'/'super' - https://bugs.eclipse.org/430573
 *     Stephan Herrmann - Contribution for Bug 463360 - [override method][null] generating method override should not create redundant null annotations
 *     Red Hat Inc. - modified to create core class in jdt.core.manipulation
 *     Microsoft Corporation - read preferences from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.util.IModifierConstants;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class LambdaExpressionsFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class FunctionalAnonymousClassesFinder extends ASTVisitor {
		private final ArrayList<ClassInstanceCreation> fNodes= new ArrayList<>();

		public static ArrayList<ClassInstanceCreation> perform(ASTNode node) {
			FunctionalAnonymousClassesFinder finder= new FunctionalAnonymousClassesFinder();
			node.accept(finder);
			return finder.fNodes;
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			if (isFunctionalAnonymous(node) && !fConversionRemovesAnnotations) {
				fNodes.add(node);
			}
			return true;
		}
	}

	public static final class LambdaExpressionsFinder extends ASTVisitor {
		private final ArrayList<LambdaExpression> fNodes= new ArrayList<>();

		public static ArrayList<LambdaExpression> perform(ASTNode node) {
			LambdaExpressionsFinder finder= new LambdaExpressionsFinder();
			node.accept(finder);
			return finder.fNodes;
		}

		@Override
		public boolean visit(LambdaExpression node) {
			ITypeBinding typeBinding= node.resolveTypeBinding();
			if (typeBinding != null && typeBinding.getFunctionalInterfaceMethod() != null) {
				fNodes.add(node);
			}
			return true;
		}
	}

	public static final class SuperThisReferenceFinder extends HierarchicalASTVisitor {
		private ITypeBinding fFunctionalInterface;
		private MethodDeclaration fMethodDeclaration;

		static boolean hasReference(MethodDeclaration node) {
			try {
				SuperThisReferenceFinder finder= new SuperThisReferenceFinder();
				ClassInstanceCreation cic= (ClassInstanceCreation) node.getParent().getParent();
				finder.fFunctionalInterface= cic.getType().resolveBinding();
				finder.fMethodDeclaration= node;
				node.accept(finder);
			} catch (AbortSearchException e) {
				return true;
			}
			return false;
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(BodyDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			return node == fMethodDeclaration;
		}

		@Override
		public boolean visit(ThisExpression node) {
			if (node.getQualifier() == null) {
				throw new AbortSearchException();
			}
			return true; // references to outer scope are harmless
		}

		@Override
		public boolean visit(SuperMethodInvocation node) {
			if (node.getQualifier() == null) {
				throw new AbortSearchException();
			} else {
				IBinding qualifierType= node.getQualifier().resolveBinding();
				if (qualifierType instanceof ITypeBinding && ((ITypeBinding) qualifierType).isInterface()) {
					throw new AbortSearchException(); // JLS8: new overloaded meaning of 'interface'.super.'method'(..)
				}
			}
			return true; // references to outer scopes are harmless
		}

		@Override
		public boolean visit(SuperFieldAccess node) {
			if (node.getQualifier() == null) {
				throw new AbortSearchException();
			}
			return true; // references to outer scope are harmless
		}

		@Override
		public boolean visit(MethodInvocation node) {
			IMethodBinding binding= node.resolveMethodBinding();
			if (binding != null && !JdtFlags.isStatic(binding) && node.getExpression() == null && Bindings.isSuperType(binding.getDeclaringClass(), fFunctionalInterface, false)) {
				throw new AbortSearchException();
			}
			return true;
		}
	}

	public static final class FinalFieldAccessInFieldDeclarationFinder extends HierarchicalASTVisitor {
		private MethodDeclaration fMethodDeclaration;
		private ASTNode fFieldDeclaration;
		static boolean hasReference(MethodDeclaration node) {
			try {
				FinalFieldAccessInFieldDeclarationFinder finder= new FinalFieldAccessInFieldDeclarationFinder();
				finder.fMethodDeclaration= node;
				finder.fFieldDeclaration= finder.findFieldDeclaration(node);
				if (finder.fFieldDeclaration == null) {
					return false;
				}
				node.accept(finder);
			} catch (AbortSearchException e) {
				return true;
			}
			return false;
		}

		private ASTNode findFieldDeclaration(ASTNode node) {
			while (node != null) {
				if (node instanceof FieldDeclaration) {
					return node;
				}
				if (node instanceof AbstractTypeDeclaration) {
					return null;
				}
				node= node.getParent();
			}
			return null;
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(BodyDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			return node == fMethodDeclaration;
		}

		private void checkForUninitializedFinalReference(IBinding binding) {
			if (binding instanceof IVariableBinding) {
				int modifiers= binding.getModifiers();
				if ((modifiers & IModifierConstants.ACC_FINAL) == IModifierConstants.ACC_FINAL) {
					if (((IVariableBinding) binding).isField()) {
						ASTNode decl= ((CompilationUnit)fMethodDeclaration.getRoot()).findDeclaringNode(binding);
						if (decl instanceof VariableDeclaration && ((VariableDeclaration)decl).getInitializer() == null) {
							throw new AbortSearchException();
						}
					}
				}
			}
		}

		@Override
		public boolean visit(SuperFieldAccess node) {
			IVariableBinding binding= node.resolveFieldBinding();
			if (binding == null) {
				return true;
			}
			IVariableBinding decl= binding.getVariableDeclaration();
			checkForUninitializedFinalReference(decl);
			return true;
		}

		@Override
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			checkForUninitializedFinalReference(binding);
			return true;
		}

		@Override
		public boolean visit(QualifiedName node) {
			IBinding binding= node.resolveBinding();
			checkForUninitializedFinalReference(binding);
			return true;
		}

		@Override
		public boolean visit(FieldAccess node) {
			IVariableBinding binding= node.resolveFieldBinding();
			if (binding == null) {
				return true;
			}
			IVariableBinding decl= binding.getVariableDeclaration();
			checkForUninitializedFinalReference(decl);
			return true;
		}

	}

	public static final class SuperThisQualifier extends HierarchicalASTVisitor {
		private ITypeBinding fQualifierTypeBinding;
		private ImportRewrite fImportRewrite;
		private ASTRewrite fASTRewrite;
		private TextEditGroup fGroup;

		public static void perform(LambdaExpression lambdaExpression, ITypeBinding parentTypeBinding, CompilationUnitRewrite cuRewrite, TextEditGroup group) {
			SuperThisQualifier qualifier= new SuperThisQualifier();
			qualifier.fQualifierTypeBinding= parentTypeBinding;
			qualifier.fImportRewrite= cuRewrite.getImportRewrite();
			qualifier.fASTRewrite= cuRewrite.getASTRewrite();
			qualifier.fGroup= group;
			lambdaExpression.accept(qualifier);
		}

		public Name getQualifierTypeName() {
			String typeName= fImportRewrite.addImport(fQualifierTypeBinding);
			return fASTRewrite.getAST().newName(typeName);
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(BodyDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(SuperFieldAccess node) {
			if (node.getQualifier() == null) {
				fASTRewrite.set(node, SuperFieldAccess.QUALIFIER_PROPERTY, getQualifierTypeName(), fGroup);
			}
			return true;
		}

		@Override
		public boolean visit(SuperMethodInvocation node) {
			if (node.getQualifier() == null) {
				fASTRewrite.set(node, SuperMethodInvocation.QUALIFIER_PROPERTY, getQualifierTypeName(), fGroup);
			}
			return true;
		}

		@Override
		public boolean visit(ThisExpression node) {
			if (node.getQualifier() == null) {
				fASTRewrite.set(node, ThisExpression.QUALIFIER_PROPERTY, getQualifierTypeName(), fGroup);
			}
			return true;
		}
	}

	public static final class AnnotationsFinder extends ASTVisitor {
		public static boolean hasAnnotations(SingleVariableDeclaration methodParameter) {
			try {
				AnnotationsFinder finder= new AnnotationsFinder();
				methodParameter.accept(finder);
			} catch (AbortSearchException e) {
				return true;
			}
			return false;
		}

		@Override
		public boolean visit(MarkerAnnotation node) {
			throw new AbortSearchException();
		}

		@Override
		public boolean visit(NormalAnnotation node) {
			throw new AbortSearchException();
		}

		@Override
		public boolean visit(SingleMemberAnnotation node) {
			throw new AbortSearchException();
		}
	}

	private static final class MethodRecursionFinder extends HierarchicalASTVisitor {
		private MethodDeclaration fMethodDeclaration;
		private IMethodBinding fMethodBinding;
		private ASTNode fFieldDeclaration;

		private static boolean isRecursiveLocal(MethodDeclaration node) {
			try {
				MethodRecursionFinder finder= new MethodRecursionFinder();
				finder.fMethodDeclaration= node;
				finder.fFieldDeclaration= finder.findFieldDeclaration(node);
				if (finder.fFieldDeclaration != null) {
					return false;
				}
				finder.fMethodBinding= finder.fMethodDeclaration.resolveBinding();
				if (finder.fMethodBinding == null) {
					return false;
				}
				node.accept(finder);
			} catch (AbortSearchException e) {
				return true;
			}

			return false;
		}

		private ASTNode findFieldDeclaration(ASTNode node) {
			ASTNode originalNode= node;
			while (node != null) {
				if (node instanceof FieldDeclaration) {
					return node;
				}
				if (node instanceof AbstractTypeDeclaration) {
					return null;
				}
				if (node instanceof MethodDeclaration && node != originalNode) {
					return null;
				}
				node= node.getParent();
			}
			return null;
		}

		@Override
		public boolean visit(MethodInvocation node) {
			if (node.getExpression() == null) {
				IMethodBinding binding= node.resolveMethodBinding();
				if (binding != null && binding.isEqualTo(fMethodBinding)) {
					throw new AbortSearchException();
				}
			}
			return true;
		}
	}

	/**
	 * In order to make parameters implicit, those ones should be the same in the same order.
	 *
	 * @param node the lambda expression
	 * @param arguments The arguments in the expression
	 * @return true if the parameters are obvious
	 */
	private static boolean areSameIdentifiers(MethodDeclaration node, List<Expression> arguments) {
		for (int i= 0; i < node.parameters().size(); i++) {
			Expression expression= ASTNodes.getUnparenthesedExpression(arguments.get(i));

			if (!(expression instanceof SimpleName) || !isSameIdentifier(node, i, (SimpleName) expression)) {
				return false;
			}
		}

		return true;
	}

	private static boolean isSameIdentifier(final MethodDeclaration node, final int i, final SimpleName argument) {
		SingleVariableDeclaration decl= (SingleVariableDeclaration) node.parameters().get(i);
		return decl.getName().getIdentifier().equals(argument.getIdentifier());
	}

	private enum MethodRefStatus {
		NO_REF, TYPE_REF, METHOD_REF;
	}

	private static record MethodInvocationStatus(MethodRefStatus status, ITypeBinding classBinding) {}

	public static MethodInvocationStatus checkMethodInvocation(MethodDeclaration visited, MethodInvocation methodInvocation) {
		Expression calledExpression= methodInvocation.getExpression();
		List<Expression> arguments= methodInvocation.arguments();
		MethodRefStatus actionType= MethodRefStatus.NO_REF;
		ITypeBinding classBinding= null;

		if (visited.parameters().size() == arguments.size()) {
			if (areSameIdentifiers(visited, arguments)) {
				IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
				ITypeBinding calledType= null;

				if (methodBinding != null) {
					calledType= methodBinding.getDeclaringClass();
				} else if (calledExpression != null) {
					calledType= calledExpression.resolveTypeBinding();
				} else {
					// For an unknown reason, MethodInvocation.resolveMethodBinding() seems to fail when the method is defined in the class we are
					AbstractTypeDeclaration enclosingType= ASTNodes.getTypedAncestor(visited, AbstractTypeDeclaration.class);

					if (enclosingType != null) {
						ITypeBinding enclosingTypeBinding= enclosingType.resolveBinding();

						if (enclosingTypeBinding != null) {
							List<Type> argumentTypes= methodInvocation.typeArguments();
							String[] parameterTypeNames= new String[methodInvocation.arguments().size()];

							for (int i= 0; i < argumentTypes.size(); i++) {
								Type argumentType= argumentTypes.get(i);

								if (argumentType.resolveBinding() == null) {
									return null;
								}

								parameterTypeNames[i]= argumentType.resolveBinding().getQualifiedName();
							}

							for (IMethodBinding declaredMethods : enclosingTypeBinding.getDeclaredMethods()) {
								if (ASTNodes.usesGivenSignature(declaredMethods, enclosingTypeBinding.getQualifiedName(), methodInvocation.getName().getIdentifier(), parameterTypeNames)) {
									calledType= enclosingTypeBinding;
									break;
								}
							}
						}
					}
				}

				if (Boolean.TRUE.equals(ASTNodes.isStatic(methodInvocation))
						&& calledType != null) {
					boolean valid= true;

					if (!arguments.isEmpty()
							&& arguments.get(0).resolveTypeBinding() != null
							&& arguments.get(0).resolveTypeBinding().isSubTypeCompatible(calledType)) {
						String[] remainingParams= new String[arguments.size() - 1];

						for (int i= 0; i < arguments.size() - 1; i++) {
							ITypeBinding resolveTypeBinding= arguments.get(i + 1).resolveTypeBinding();

							if (resolveTypeBinding == null) {
								valid= false;
								break;
							}

							remainingParams[i]= resolveTypeBinding.getQualifiedName();
						}

						if (valid) {
							for (IMethodBinding declaredMethodBinding : calledType.getDeclaredMethods()) {
								if (!Modifier.isStatic(declaredMethodBinding.getModifiers())
										&& ASTNodes.usesGivenSignature(declaredMethodBinding, calledType.getQualifiedName(), methodInvocation.getName().getIdentifier(), remainingParams)) {
									valid= false;
									break;
								}
							}
						}
					}

					if (valid) {
						actionType= MethodRefStatus.TYPE_REF;
						classBinding= calledType;
					}
				}

				if (actionType != MethodRefStatus.TYPE_REF) {
					if (calledExpression == null) {
						if (calledType != null) {
							ITypeBinding enclosingType= Bindings.getBindingOfParentType(visited.getParent().getParent());

							if (enclosingType != null && Bindings.isSuperType(calledType, enclosingType)) {
								actionType= MethodRefStatus.METHOD_REF;
							}
						}
					} else if (calledExpression instanceof StringLiteral
							|| calledExpression instanceof NumberLiteral
							|| calledExpression instanceof ThisExpression) {
						actionType= MethodRefStatus.METHOD_REF;
					} else if (calledExpression instanceof FieldAccess) {
						FieldAccess fieldAccess= (FieldAccess) calledExpression;

						if (fieldAccess.resolveFieldBinding() != null && fieldAccess.resolveFieldBinding().isEffectivelyFinal()) {
							actionType= MethodRefStatus.METHOD_REF;
						}
					} else if (calledExpression instanceof SuperFieldAccess) {
						SuperFieldAccess fieldAccess= (SuperFieldAccess) calledExpression;

						if (fieldAccess.resolveFieldBinding() != null && fieldAccess.resolveFieldBinding().isEffectivelyFinal()) {
							actionType= MethodRefStatus.METHOD_REF;
						}
					}
				}
			}
		} else if (calledExpression instanceof SimpleName && visited.parameters().size() == arguments.size() + 1) {
			SimpleName calledObject= (SimpleName) calledExpression;

			if (isSameIdentifier(visited, 0, calledObject)) {
				boolean valid= true;

				for (int i= 0; i < arguments.size(); i++) {
					ASTNode expression= ASTNodes.getUnparenthesedExpression(arguments.get(i));

					if (!(expression instanceof SimpleName) || !isSameIdentifier(visited, i + 1, (SimpleName) expression)) {
						valid= false;
						break;
					}
				}

				if (valid) {
					ITypeBinding klass= null;

					if (calledExpression.resolveTypeBinding() != null) {
						klass= calledExpression.resolveTypeBinding();
					} else if (methodInvocation.resolveMethodBinding() != null && methodInvocation.resolveMethodBinding().getDeclaringClass() != null) {
						klass= methodInvocation.resolveMethodBinding().getDeclaringClass();
					}

					if (klass != null) {
						String[] cumulativeParams= new String[arguments.size() + 1];
						cumulativeParams[0]= klass.getQualifiedName();

						for (int i= 0; i < arguments.size(); i++) {
							ITypeBinding resolveTypeBinding= arguments.get(i).resolveTypeBinding();

							if (resolveTypeBinding == null) {
								valid= false;
								break;
							}

							cumulativeParams[i + 1]= resolveTypeBinding.getQualifiedName();
						}

						if (valid) {
							for (IMethodBinding declaredMethodBinding : klass.getDeclaredMethods()) {
								if (Modifier.isStatic(declaredMethodBinding.getModifiers())
										&& ASTNodes.usesGivenSignature(declaredMethodBinding, klass.getQualifiedName(), methodInvocation.getName().getIdentifier(), cumulativeParams)) {
									valid= false;
									break;
								}
							}
						}

						if (valid) {
							actionType= MethodRefStatus.TYPE_REF;
							classBinding= klass;
						}
					}
				}
			}
		}
		return new MethodInvocationStatus(actionType, classBinding);
	}

	private static Type copyType(final CompilationUnitRewrite cuRewrite, final AST ast, final ASTNode node, final ITypeBinding typeBinding) {
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		ImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(node, importRewrite);
		ITypeBinding modifiedType;

		if (typeBinding.getTypeParameters().length == 0) {
			if (typeBinding.isCapture()) {
				ITypeBinding[] bounds= typeBinding.getTypeBounds();
				if (bounds.length > 0) {
					modifiedType= bounds[0];
				} else {
					modifiedType= typeBinding.getErasure();
				}
			} else {
				modifiedType= typeBinding.getErasure();
			}
		} else {
			modifiedType= typeBinding;
		}

		return ASTNodeFactory.newCreationType(ast, modifiedType, importRewrite, importContext);
	}

	public static class CreateLambdaOperation extends CompilationUnitRewriteOperation {
		private final List<ClassInstanceCreation> fExpressions;
		private final boolean fSimplifyLambda;

		public CreateLambdaOperation(List<ClassInstanceCreation> expressions, boolean simplifyLambda) {
			fExpressions= expressions;
			fSimplifyLambda= simplifyLambda;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {

			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ImportRemover importRemover= cuRewrite.getImportRemover();
			final AST ast= rewrite.getAST();

			HashMap<ClassInstanceCreation, HashSet<String>> cicToNewNames= new HashMap<>();
			for (int i= 0; i < fExpressions.size(); i++) {
				ClassInstanceCreation classInstanceCreation= fExpressions.get(i);

				AnonymousClassDeclaration anonymTypeDecl= classInstanceCreation.getAnonymousClassDeclaration();
				List<BodyDeclaration> bodyDeclarations= anonymTypeDecl.bodyDeclarations();

				Object object= bodyDeclarations.get(0);

				if (!(object instanceof MethodDeclaration)) {
					continue;
				}

				HashSet<String> excludedNames= new HashSet<>();
				if (i != 0) {
					for (ClassInstanceCreation convertedCic : fExpressions.subList(0, i)) {
						if (ASTNodes.isParent(convertedCic, classInstanceCreation)) {
							excludedNames.addAll(cicToNewNames.get(convertedCic));
						}
					}
				}

				final MethodDeclaration methodDeclaration= (MethodDeclaration) object;
				TextEditGroup group= createTextEditGroup(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression, cuRewrite);
				HashSet<String> newNames= makeNamesUnique(excludedNames, methodDeclaration, rewrite, group);
				cicToNewNames.put(classInstanceCreation, new HashSet<>(newNames));
				List<SingleVariableDeclaration> methodParameters= methodDeclaration.parameters();

				Block body= methodDeclaration.getBody();
				List<Statement> statements= body.statements();
				ASTNode lambdaBody= body;
				if (statements.size() == 1) {
					// use short form with just an expression body if possible
					Statement statement= statements.get(0);
					if (statement instanceof ExpressionStatement) {
						lambdaBody= ((ExpressionStatement) statement).getExpression();
					} else if (statement instanceof ReturnStatement) {
						Expression returnExpression= ((ReturnStatement) statement).getExpression();
						if (returnExpression != null) {
							lambdaBody= returnExpression;
						}
					}
				}

				// use short form with inferred parameter types and without parentheses if possible
				boolean createExplicitlyTypedParameters= false;
				for (SingleVariableDeclaration methodParameter : methodParameters) {
					if (AnnotationsFinder.hasAnnotations(methodParameter)) {
						createExplicitlyTypedParameters= true;
						break;
					}
				}

				Expression cicReplacement= null;
				if (fSimplifyLambda) {
					if (lambdaBody instanceof MethodInvocation methodInvocation) {
						MethodInvocationStatus methodInvocationCheck= checkMethodInvocation(methodDeclaration, methodInvocation);
						if (methodInvocationCheck != null) {
							if (methodInvocationCheck.status() == MethodRefStatus.METHOD_REF) {
								ExpressionMethodReference methodRef= ast.newExpressionMethodReference();
								if (methodInvocation.getExpression() != null) {
									methodRef.setExpression(ASTNodes.createMoveTarget(rewrite, methodInvocation.getExpression()));
								} else {
									methodRef.setExpression(ast.newThisExpression());
								}
								methodRef.setName(ASTNodes.createMoveTarget(rewrite, methodInvocation.getName()));
								cicReplacement= castMethodRefIfNeeded(cuRewrite, ast, methodRef, classInstanceCreation);
							} else if (methodInvocationCheck.status() == MethodRefStatus.TYPE_REF) {
								TypeMethodReference typeMethodRef= ast.newTypeMethodReference();
								typeMethodRef.setType(copyType(cuRewrite, ast, methodInvocation, methodInvocationCheck.classBinding()));
								typeMethodRef.setName(ASTNodes.createMoveTarget(rewrite, methodInvocation.getName()));
								cicReplacement= castMethodRefIfNeeded(cuRewrite, ast, typeMethodRef, classInstanceCreation);
							}
						}
					} else if (lambdaBody instanceof ClassInstanceCreation invokedClassInstanceCreation) {
						List<Expression> arguments= invokedClassInstanceCreation.arguments();

						if (methodDeclaration.parameters().size() == arguments.size()
								&& areSameIdentifiers(methodDeclaration, arguments)
								&& classInstanceCreation.getAnonymousClassDeclaration() == null) {
							CreationReference creationRef= ast.newCreationReference();
							creationRef.setType(copyType(cuRewrite, ast, classInstanceCreation, classInstanceCreation.resolveTypeBinding()));
							cicReplacement= creationRef;
						}
					} else if (lambdaBody instanceof SuperMethodInvocation superMethodInvocation) {
						List<Expression> arguments= superMethodInvocation.arguments();

						if (methodDeclaration.parameters().size() == arguments.size() && areSameIdentifiers(methodDeclaration, arguments)) {
							SuperMethodReference superMethodRef= ast.newSuperMethodReference();
							superMethodRef.setName(ASTNodes.createMoveTarget(rewrite, superMethodInvocation.getName()));
							cicReplacement= castMethodRefIfNeeded(cuRewrite, ast, superMethodRef, classInstanceCreation);
						}
					} else if (lambdaBody instanceof InstanceofExpression instanceofExpression) {
						Expression leftOp= instanceofExpression.getLeftOperand();
						if (methodDeclaration.parameters().size() == 1 && areSameIdentifiers(methodDeclaration, List.of(leftOp))) {
							ExpressionMethodReference instanceofMethodReference= ast.newExpressionMethodReference();
							TypeLiteral typeLiteral= ast.newTypeLiteral();
							typeLiteral.setType(copyType(cuRewrite, ast, instanceofExpression, instanceofExpression.getRightOperand().resolveBinding()));
							instanceofMethodReference.setName(ast.newSimpleName("isInstance")); //$NON-NLS-1$
							instanceofMethodReference.setExpression(typeLiteral);
							cicReplacement= castMethodRefIfNeeded(cuRewrite, ast, instanceofMethodReference, classInstanceCreation);
						}
					}
				}

				if (cicReplacement == null) {
					LambdaExpression lambdaExpression= ast.newLambdaExpression();
					List<VariableDeclaration> lambdaParameters= lambdaExpression.parameters();
					lambdaExpression.setParentheses(createExplicitlyTypedParameters || methodParameters.size() != 1);
					for (SingleVariableDeclaration methodParameter : methodParameters) {
						if (createExplicitlyTypedParameters) {
							lambdaParameters.add((SingleVariableDeclaration) rewrite.createCopyTarget(methodParameter));
							importRemover.registerRetainedNode(methodParameter);
						} else {
							VariableDeclarationFragment lambdaParameter= ast.newVariableDeclarationFragment();
							lambdaParameter.setName((SimpleName) rewrite.createCopyTarget(methodParameter.getName()));
							lambdaParameters.add(lambdaParameter);
						}
					}

					final Set<ITypeBinding> inheritedTypes= new HashSet<>();
					collectInheritedTypes(anonymTypeDecl.resolveBinding(), inheritedTypes);

					ASTVisitor inheritedFieldsVisitor= new ASTVisitor() {
						@Override
						public boolean visit(final SimpleName node) {
							if ((!(node.getParent() instanceof QualifiedName) || node.getLocationInParent() != QualifiedName.NAME_PROPERTY)
									&& (!(node.getParent() instanceof FieldAccess) || node.getLocationInParent() != FieldAccess.NAME_PROPERTY)
									&& (!(node.getParent() instanceof SuperFieldAccess) || node.getLocationInParent() != SuperFieldAccess.NAME_PROPERTY)
									&& node.resolveBinding() != null
									&& node.resolveBinding().getKind() == IBinding.VARIABLE) {
								IVariableBinding variableBinding= (IVariableBinding) node.resolveBinding();

								if (variableBinding != null
										&& (variableBinding.getModifiers() & Modifier.STATIC) != 0
										&& variableBinding.isField()
										&& inheritedTypes.contains(variableBinding.getDeclaringClass())) {
									ITypeBinding cicBinding= classInstanceCreation.getType().resolveBinding();
									if (cicBinding != null) {
										Name replacement= ast.newName(cicBinding.getName() + "." + node.getFullyQualifiedName()); //$NON-NLS-1$
										rewrite.replace(node, replacement, group);
									}
									return false;
								}
							}

							return true;
						}
					};
					lambdaBody.accept(inheritedFieldsVisitor);

					ASTNode fragment= ASTNodes.getFirstAncestorOrNull(classInstanceCreation, VariableDeclarationFragment.class, BodyDeclaration.class);

					if (fragment instanceof VariableDeclarationFragment) {
						final VariableDeclarationFragment actualFragment= (VariableDeclarationFragment) fragment;

						if (actualFragment.getParent() instanceof FieldDeclaration) {
							FieldDeclaration fieldDeclaration= (FieldDeclaration) actualFragment.getParent();

							TypeDeclaration declarationClass= ASTNodes.getFirstAncestorOrNull(fieldDeclaration, TypeDeclaration.class);

							if (declarationClass != null) {
								TypeDeclaration typeDeclaration= declarationClass;

								final List<FieldDeclaration> nextFields= new ArrayList<>(typeDeclaration.getFields().length);
								boolean isBefore= true;

								for (FieldDeclaration oneField : typeDeclaration.getFields()) {
									if (oneField == fieldDeclaration) {
										isBefore= false;
									}

									if (!isBefore) {
										nextFields.add(oneField);
									}
								}

								ASTVisitor visitor= new ASTVisitor() {
									@Override
									public boolean visit(final MethodInvocation node) {
										ITypeBinding fieldType= fieldDeclaration.getType().resolveBinding();
										ASTNode declaration= ASTNodes.findDeclaration(node.resolveMethodBinding(), declarationClass);

										if (node.getExpression() == null && fieldType != null && methodDeclaration == declaration) {
											ASTNode replacement;

											if ((fieldDeclaration.getModifiers() & Modifier.STATIC) != 0) {
												SimpleName copyOfClassName= (SimpleName) rewrite.createCopyTarget(typeDeclaration.getName());
												replacement= ast.newQualifiedName(copyOfClassName, (SimpleName) rewrite.createCopyTarget(actualFragment.getName()));
											} else {
												FieldAccess newFieldAccess= ast.newFieldAccess();
												newFieldAccess.setExpression(ast.newThisExpression());
												newFieldAccess.setName((SimpleName) rewrite.createCopyTarget(actualFragment.getName()));
												replacement= newFieldAccess;
											}

											rewrite.set(node, MethodInvocation.EXPRESSION_PROPERTY, replacement, group);

											return false;
										}

										return true;
									}

									@Override
									public boolean visit(final SimpleName node) {
										if ((!(node.getParent() instanceof QualifiedName) || node.getLocationInParent() != QualifiedName.NAME_PROPERTY)
												&& (!(node.getParent() instanceof FieldAccess) || node.getLocationInParent() != FieldAccess.NAME_PROPERTY)
												&& (!(node.getParent() instanceof SuperFieldAccess) || node.getLocationInParent() != SuperFieldAccess.NAME_PROPERTY)) {
											ASTNode declaration= ASTNodes.findDeclaration(node.resolveBinding(), declarationClass);

											if (declaration instanceof VariableDeclarationFragment && declaration.getParent() instanceof FieldDeclaration) {
												FieldDeclaration currentField= (FieldDeclaration) declaration.getParent();

												if (nextFields.contains(currentField)) {
													if ((currentField.getModifiers() & Modifier.STATIC) != 0) {
														SimpleName copyOfClassName= (SimpleName) rewrite.createCopyTarget(typeDeclaration.getName());
														QualifiedName replacement= ast.newQualifiedName(copyOfClassName, ASTNodes.createMoveTarget(rewrite, node));
														rewrite.replace(node, replacement, group);
													} else {
														FieldAccess newFieldAccess= ast.newFieldAccess();
														newFieldAccess.setExpression(ast.newThisExpression());
														newFieldAccess.setName(ASTNodes.createMoveTarget(rewrite, node));
														rewrite.replace(node, newFieldAccess, group);
													}

													return false;
												}
											}
										}

										return true;
									}

									@Override
									public boolean visit(final ThisExpression node) {
										Name qualifier= node.getQualifier();

										if (qualifier != null
												&& qualifier.resolveBinding() != null
												&& qualifier.resolveBinding().getKind() == IBinding.TYPE
												&& Objects.equals(qualifier.resolveBinding(), typeDeclaration.resolveBinding())) {
											rewrite.remove(qualifier, group);
										}

										return true;
									}
								};
								lambdaBody.accept(visitor);
							}
						}
					}

					//TODO: Bug 421479: [1.8][clean up][quick assist] convert anonymous to lambda must consider lost scope of interface
					//				lambdaBody.accept(new InterfaceAccessQualifier(rewrite, classInstanceCreation.getType().resolveBinding())); //TODO: maybe need a separate ASTRewrite and string placeholder

					lambdaExpression.setBody(ASTNodes.getCopyOrReplacement(rewrite, lambdaBody, group));
					cicReplacement= lambdaExpression;
				}

				ITypeBinding targetTypeBinding= ASTNodes.getTargetType(classInstanceCreation);
				if (needCastForWildcardArgument(classInstanceCreation) || ASTNodes.isTargetAmbiguous(classInstanceCreation, ASTNodes.isExplicitlyTypedLambda(cicReplacement)) || targetTypeBinding.getFunctionalInterfaceMethod() == null) {
					CastExpression cast= ast.newCastExpression();
					cast.setExpression(cicReplacement);
					ImportRewrite importRewrite= cuRewrite.getImportRewrite();
					ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(classInstanceCreation, importRewrite);
					Type castType= importRewrite.addImport(classInstanceCreation.getType().resolveBinding(), ast, importRewriteContext, TypeLocation.CAST);
					cast.setType(castType);
					importRemover.registerAddedImports(castType);
					cicReplacement= cast;
				}
				ASTNodes.replaceButKeepComment(rewrite, classInstanceCreation, cicReplacement, group);

				importRemover.registerRemovedNode(classInstanceCreation);
				importRemover.registerRetainedNode(lambdaBody);
			}
		}

		private boolean needCastForWildcardArgument(ClassInstanceCreation classInstanceCreation) {
			if (classInstanceCreation.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY) {
				MethodInvocation parent= (MethodInvocation)classInstanceCreation.getParent();
				List<Expression> arguments= parent.arguments();
				IMethodBinding methodBinding= parent.resolveMethodBinding();
				if (methodBinding != null) {
					ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
					int index= -1;
					for (int i= 0; i < arguments.size(); ++i) {
						if (arguments.get(i) == classInstanceCreation) {
							index= i;
							break;
						}
					}
					if (index >= 0) {
						ITypeBinding parameterTypeBinding= null;
						if (index < parameterTypes.length) {
							parameterTypeBinding= parameterTypes[index];
						} else {
							parameterTypeBinding= parameterTypes[parameterTypes.length - 1].getComponentType();
						}
						ITypeBinding classInstanceParameterizedType= classInstanceCreation.getType().resolveBinding();
						ITypeBinding[] classInstanceTypeArguments= classInstanceParameterizedType.getTypeArguments();
						ITypeBinding[] typeArguments= parameterTypeBinding.getTypeArguments();
						for (int i= 0; i < typeArguments.length; ++i) {
							ITypeBinding typeArgument= typeArguments[i];
							if (typeArgument.isWildcardType()) {
								ITypeBinding bound= typeArgument.getBound();
								if (bound == null) {
									bound= typeArgument.getErasure();
								}
								ITypeBinding classInstanceTypeArgument= classInstanceTypeArguments[i];
								if (!bound.isSubTypeCompatible(classInstanceTypeArgument)) {
									return true;
								}
							}
						}
					}
				}
			}
			return false;
		}

		private Expression castMethodRefIfNeeded(final CompilationUnitRewrite cuRewrite, AST ast, Expression methodRef, Expression visited) {
			boolean needCast= false;
			Expression replacementNode= methodRef;
			if (visited.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY) {
				MethodInvocation parent= (MethodInvocation) visited.getParent();
				List<Expression> args= parent.arguments();
				IMethodBinding parentBinding= parent.resolveMethodBinding();
				if (parentBinding != null) {
					ITypeBinding parentTypeBinding= parentBinding.getDeclaringClass();
					while (parentTypeBinding != null) {
						IMethodBinding[] parentTypeMethods= parentTypeBinding.getDeclaredMethods();
						for (IMethodBinding parentTypeMethod : parentTypeMethods) {
							if (parentTypeMethod.getName().equals(parentBinding.getName())
									&& parentTypeMethod.getParameterTypes().length == args.size()
									&& !parentTypeMethod.isEqualTo(parentBinding)) {
								needCast= true;
								break;
							}
						}
						if (!needCast) {
							parentTypeBinding= parentTypeBinding.getSuperclass();
						} else {
							break;
						}
					}
					if (needCast) {
						for (Expression arg : args) {
							if (arg == visited) {
								CastExpression cast= ast.newCastExpression();
								cast.setExpression(methodRef);
								ITypeBinding argTypeBinding= arg.resolveTypeBinding();
								if (argTypeBinding == null) {
									return replacementNode;
								}
								ImportRewrite importRewriter= cuRewrite.getImportRewrite();
								Type argType= importRewriter.addImport(argTypeBinding, ast);
								cast.setType(argType);
								replacementNode= cast;
							}
						}
					}
				}
			}
			return replacementNode;
		}

		private static void collectInheritedTypes(final ITypeBinding anonymType, final Set<ITypeBinding> inheritedTypes) {
			if (anonymType != null) {
				ITypeBinding motherType= anonymType.getSuperclass();

				if (motherType != null) {
					inheritedTypes.add(motherType);
					collectInheritedTypes(motherType, inheritedTypes);
				}

				ITypeBinding[] interfaces= anonymType.getInterfaces();

				if (interfaces != null && interfaces.length > 0) {
					Collections.addAll(inheritedTypes, interfaces);

					for (ITypeBinding iTypeBinding : interfaces) {
						collectInheritedTypes(iTypeBinding, inheritedTypes);
					}
				}
			}
		}

		private HashSet<String> makeNamesUnique(HashSet<String> namesFromUpperScope, MethodDeclaration methodDeclaration, ASTRewrite rewrite, TextEditGroup group) {
			HashSet<String> newNames= new HashSet<>();
			namesFromUpperScope.addAll(ASTNodes.getVisibleLocalVariablesInScope(methodDeclaration));
			List<SimpleName> simpleNamesInMethod= getNamesInMethod(methodDeclaration);
			List<String> namesInMethod= new ArrayList<>();
			for (SimpleName name : simpleNamesInMethod) {
				namesInMethod.add(name.getIdentifier());
			}

			for (int i= 0; i < simpleNamesInMethod.size(); i++) {
				SimpleName name= simpleNamesInMethod.get(i);
				String identifier= namesInMethod.get(i);
				if (namesFromUpperScope.contains(identifier)) {
					String newIdentifier= createName(identifier, namesFromUpperScope, namesInMethod, newNames);
					namesFromUpperScope.add(newIdentifier);
					newNames.add(newIdentifier);
					SimpleName[] references= LinkedNodeFinder.findByNode(name.getRoot(), name);
					for (SimpleName ref : references) {
						rewrite.set(ref, SimpleName.IDENTIFIER_PROPERTY, newIdentifier, group);
					}
				}
			}

			return newNames;
		}

		private List<SimpleName> getNamesInMethod(MethodDeclaration methodDeclaration) {
			class NamesCollector extends HierarchicalASTVisitor {
				private int fTypeCounter;

				private List<SimpleName> fNames= new ArrayList<>();

				@Override
				public boolean visit(AbstractTypeDeclaration node) {
					if (fTypeCounter++ == 0) {
						fNames.add(node.getName());
					}
					return true;
				}

				@Override
				public void endVisit(AbstractTypeDeclaration node) {
					fTypeCounter--;
				}

				@Override
				public boolean visit(AnonymousClassDeclaration node) {
					fTypeCounter++;
					return true;
				}

				@Override
				public void endVisit(AnonymousClassDeclaration node) {
					fTypeCounter--;
				}

				@Override
				public boolean visit(VariableDeclaration node) {
					if (fTypeCounter == 0) {
						fNames.add(node.getName());
					}
					return true;
				}
			}

			NamesCollector namesCollector= new NamesCollector();
			methodDeclaration.accept(namesCollector);
			return namesCollector.fNames;
		}

		private String createName(String candidate, HashSet<String> excludedNames, List<String> namesInMethod, HashSet<String> newNames) {
			int i= 1;
			String result= candidate;
			while (excludedNames.contains(result) || newNames.contains(result) || namesInMethod.contains(result)) {
				result= candidate + i++;
			}
			return result;
		}
	}

	public interface IAnonymousClassCreationOperation {

		MethodDeclaration getMethodDeclaration(ICompilationUnit cu, ASTRewrite rewrite, ImportRewrite rewrites,
				ImportRewriteContext context, IMethodBinding binding, String[] parameterNames, ITypeBinding targetType,
				boolean inInterface, ASTNode astNode) throws CoreException;

	}

	public static class CreateAnonymousClassCreationOperation extends CompilationUnitRewriteOperation implements IAnonymousClassCreationOperation {

		private final List<LambdaExpression> fExpressions;

		public CreateAnonymousClassCreationOperation(List<LambdaExpression> changedNodes) {
			fExpressions= changedNodes;
		}

		@Override
		public MethodDeclaration getMethodDeclaration(ICompilationUnit cu, ASTRewrite rewrite, ImportRewrite rewrites,
				ImportRewriteContext context, IMethodBinding binding, String[] parameterNames, ITypeBinding targetType,
				boolean inInterface, ASTNode astNode) throws CoreException {
			final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu);
			MethodDeclaration methodDeclaration= StubUtility2Core.createImplementationStubCore(cu, rewrite, rewrites, context, binding, parameterNames, targetType, settings, inInterface,
					astNode,
					false);
			return methodDeclaration;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			rewriteAST(this, cuRewrite, model);
		}

		public void rewriteAST(IAnonymousClassCreationOperation op, CompilationUnitRewrite cuRewrite, @SuppressWarnings("unused") LinkedProposalModelCore model) throws CoreException {

			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= rewrite.getAST();

			for (LambdaExpression lambdaExpression : fExpressions) {
				TextEditGroup group= createTextEditGroup(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation, cuRewrite);

				ITypeBinding lambdaTypeBinding= lambdaExpression.resolveTypeBinding();
				IMethodBinding methodBinding= lambdaTypeBinding.getFunctionalInterfaceMethod();
				List<VariableDeclaration> parameters= lambdaExpression.parameters();
				String[] parameterNames= new String[parameters.size()];
				for (int i= 0; i < parameterNames.length; i++) {
					parameterNames[i]= parameters.get(i).getName().getIdentifier();
				}

				ImportRewrite importRewrite= cuRewrite.getImportRewrite();
				ImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(lambdaExpression, importRewrite);

				MethodDeclaration methodDeclaration= op.getMethodDeclaration(cuRewrite.getCu(), rewrite, importRewrite, importContext, methodBinding, parameterNames, lambdaTypeBinding, false,
						lambdaExpression);

				// Qualify reference to this or super
				ASTNode parentType= ASTResolving.findParentType(lambdaExpression);
				ITypeBinding parentTypeBinding= null;
				if (parentType instanceof AbstractTypeDeclaration) {
					parentTypeBinding= ((AbstractTypeDeclaration) parentType).resolveBinding();
				} else if (parentType instanceof AnonymousClassDeclaration) {
					parentTypeBinding= ((AnonymousClassDeclaration) parentType).resolveBinding();
				}
				if (parentTypeBinding != null) {
					parentTypeBinding= Bindings.normalizeTypeBinding(parentTypeBinding);
					if (parentTypeBinding != null) {
						SuperThisQualifier.perform(lambdaExpression, parentTypeBinding.getTypeDeclaration(), cuRewrite, group);
					}
				}

				Block block;
				ASTNode lambdaBody= lambdaExpression.getBody();
				if (lambdaBody instanceof Block) {
					block= (Block) ASTNodes.getCopyOrReplacement(rewrite, lambdaBody, group);
				} else {
					block= ast.newBlock();
					List<Statement> statements= block.statements();
					ITypeBinding returnType= methodBinding.getReturnType();
					Expression copyTarget= (Expression) ASTNodes.getCopyOrReplacement(rewrite, lambdaBody, group);
					if (Bindings.isVoidType(returnType)) {
						ExpressionStatement newExpressionStatement= ast.newExpressionStatement(copyTarget);
						statements.add(newExpressionStatement);
					} else {
						ReturnStatement returnStatement= ast.newReturnStatement();
						returnStatement.setExpression(copyTarget);
						statements.add(returnStatement);
					}
				}
				methodDeclaration.setBody(block);

				AnonymousClassDeclaration anonymousClassDeclaration= ast.newAnonymousClassDeclaration();
				List<BodyDeclaration> bodyDeclarations= anonymousClassDeclaration.bodyDeclarations();
				bodyDeclarations.add(methodDeclaration);

				Type creationType= ASTNodeFactory.newCreationType(ast, lambdaTypeBinding, importRewrite, importContext);

				ClassInstanceCreation classInstanceCreation= ast.newClassInstanceCreation();
				classInstanceCreation.setType(creationType);
				classInstanceCreation.setAnonymousClassDeclaration(anonymousClassDeclaration);

				ASTNode toReplace= lambdaExpression;
				if (lambdaExpression.getLocationInParent() == CastExpression.EXPRESSION_PROPERTY && lambdaTypeBinding.isEqualTo(((CastExpression) lambdaExpression.getParent()).resolveTypeBinding())) {
					// remove cast to same type as the anonymous will use
					toReplace= lambdaExpression.getParent();
				}
				rewrite.replace(toReplace, classInstanceCreation, group);
			}
		}
	}

	private static boolean fConversionRemovesAnnotations;

	public static LambdaExpressionsFixCore createConvertToLambdaFix(ClassInstanceCreation cic) {
		CompilationUnit root= (CompilationUnit) cic.getRoot();
		if (!JavaModelUtil.is1d8OrHigher(root.getJavaElement().getJavaProject())) {
			return null;
		}

		if (!LambdaExpressionsFixCore.isFunctionalAnonymous(cic)) {
			return null;
		}

		CreateLambdaOperation op= new CreateLambdaOperation(Collections.singletonList(cic), true);
		String message;
		if (fConversionRemovesAnnotations) {
			message= FixMessages.LambdaExpressionsFix_convert_to_lambda_expression_removes_annotations;
		} else {
			message= FixMessages.LambdaExpressionsFix_convert_to_lambda_expression;
		}
		return new LambdaExpressionsFixCore(message, root, new CompilationUnitRewriteOperation[] { op });
	}

	public static IProposableFix createConvertToAnonymousClassCreationsFix(LambdaExpression lambda) {
		// offer the quick assist at pre 1.8 levels as well to get rid of the compilation error (TODO: offer this as a quick fix in that case)

		if (lambda.resolveTypeBinding() == null || lambda.resolveTypeBinding().getFunctionalInterfaceMethod() == null) {
			return null;
		}

		CreateAnonymousClassCreationOperation op= new CreateAnonymousClassCreationOperation(Collections.singletonList(lambda));
		CompilationUnit root= (CompilationUnit) lambda.getRoot();
		return new LambdaExpressionsFixCore(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation, root, new CompilationUnitRewriteOperation[] { op });
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean useLambda, boolean useAnonymous, boolean simplifyLambda) {
		if (!JavaModelUtil.is1d8OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			return null;
		}

		if (useLambda) {
			ArrayList<ClassInstanceCreation> convertibleNodes= FunctionalAnonymousClassesFinder.perform(compilationUnit);
			if (convertibleNodes.isEmpty()) {
				return null;
			}

			Collections.reverse(convertibleNodes); // process nested anonymous classes first
			CompilationUnitRewriteOperation op= new CreateLambdaOperation(convertibleNodes, simplifyLambda);
			return new LambdaExpressionsFixCore(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression, compilationUnit, new CompilationUnitRewriteOperation[] { op });

		} else if (useAnonymous) {
			ArrayList<LambdaExpression> convertibleNodes= LambdaExpressionsFinder.perform(compilationUnit);
			if (convertibleNodes.isEmpty()) {
				return null;
			}

			Collections.reverse(convertibleNodes); // process nested lambdas first
			CompilationUnitRewriteOperation op= new CreateAnonymousClassCreationOperation(convertibleNodes);
			return new LambdaExpressionsFixCore(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation, compilationUnit, new CompilationUnitRewriteOperation[] { op });

		}
		return null;
	}

	public LambdaExpressionsFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

	public static boolean isFunctionalAnonymous(ClassInstanceCreation node) {
		ITypeBinding typeBinding= node.resolveTypeBinding();
		if (typeBinding == null) {
			return false;
		}
		ITypeBinding[] interfaces= typeBinding.getInterfaces();
		if (interfaces.length != 1) {
			return false;
		}
		if (interfaces[0].getFunctionalInterfaceMethod() == null || interfaces[0].getFunctionalInterfaceMethod().isGenericMethod()) {
			return false;
		}

		AnonymousClassDeclaration anonymTypeDecl= node.getAnonymousClassDeclaration();
		if (anonymTypeDecl == null || anonymTypeDecl.resolveBinding() == null) {
			return false;
		}

		List<BodyDeclaration> bodyDeclarations= anonymTypeDecl.bodyDeclarations();
		// cannot convert if there are fields or additional methods
		if (bodyDeclarations.size() != 1) {
			return false;
		}
		BodyDeclaration bodyDeclaration= bodyDeclarations.get(0);
		if (!(bodyDeclaration instanceof MethodDeclaration)) {
			return false;
		}

		MethodDeclaration methodDecl= (MethodDeclaration) bodyDeclaration;
		IMethodBinding methodBinding= methodDecl.resolveBinding();

		if (methodBinding == null) {
			return false;
		}
		// generic lambda expressions are not allowed
		if (methodBinding.isGenericMethod()) {
			return false;
		}

		int modifiers= methodBinding.getModifiers();
		if (Modifier.isSynchronized(modifiers) || Modifier.isStrictfp(modifiers)) {
			return false;
		}

		// lambda cannot refer to 'this'/'super' literals
		if (SuperThisReferenceFinder.hasReference(methodDecl)) {
			return false;
		}

		// lambda cannot access this and so we should avoid lambda conversion
		// when anonymous class is used to initialize field and refers to
		// final fields that may or may not be initialized
		if (FinalFieldAccessInFieldDeclarationFinder.hasReference(methodDecl)) {
			return false;
		}

		if (ASTNodes.getTargetType(node) == null) {
			return false;
		}

		// Cannot handle recursive calls in a locally declared anonymous class
		if (MethodRecursionFinder.isRecursiveLocal(methodDecl)) {
			return false;
		}

		// Check if annotations other than @Override and @Deprecated will be removed
		checkAnnotationsRemoval(methodBinding);

		return true;
	}

	public static void checkAnnotationsRemoval(IMethodBinding methodBinding) {
		fConversionRemovesAnnotations= false;
		IAnnotationBinding[] declarationAnnotations= methodBinding.getAnnotations();
		for (IAnnotationBinding declarationAnnotation : declarationAnnotations) {
			ITypeBinding annotationType= declarationAnnotation.getAnnotationType();
			if (annotationType != null) {
				String qualifiedName= annotationType.getQualifiedName();
				if (!"java.lang.Override".equals(qualifiedName) && !"java.lang.Deprecated".equals(qualifiedName)) { //$NON-NLS-1$ //$NON-NLS-2$
					fConversionRemovesAnnotations= true;
					return;
				}
			}
		}
	}

	/*private static boolean isInTargetTypeContext(ClassInstanceCreation node) {
		ITypeBinding targetType= ASTNodes.getTargetType(node);
		return targetType != null && targetType.getFunctionalInterfaceMethod() != null;


		//TODO: probably incomplete, should reuse https://bugs.eclipse.org/bugs/show_bug.cgi?id=408966#c6
		StructuralPropertyDescriptor locationInParent= node.getLocationInParent();

		if (locationInParent == ReturnStatement.EXPRESSION_PROPERTY) {
			MethodDeclaration methodDeclaration= ASTResolving.findParentMethodDeclaration(node);
			if (methodDeclaration == null)
				return false;
			IMethodBinding methodBinding= methodDeclaration.resolveBinding();
			if (methodBinding == null)
				return false;
			//TODO: could also cast to the CIC type instead of aborting...
			return methodBinding.getReturnType().getFunctionalInterfaceMethod() != null;
		}

		//TODO: should also check whether variable is of a functional type
		return locationInParent == SingleVariableDeclaration.INITIALIZER_PROPERTY
				|| locationInParent == VariableDeclarationFragment.INITIALIZER_PROPERTY
				|| locationInParent == Assignment.RIGHT_HAND_SIDE_PROPERTY
				|| locationInParent == ArrayInitializer.EXPRESSIONS_PROPERTY

				|| locationInParent == MethodInvocation.ARGUMENTS_PROPERTY
				|| locationInParent == SuperMethodInvocation.ARGUMENTS_PROPERTY
				|| locationInParent == ConstructorInvocation.ARGUMENTS_PROPERTY
				|| locationInParent == SuperConstructorInvocation.ARGUMENTS_PROPERTY
				|| locationInParent == ClassInstanceCreation.ARGUMENTS_PROPERTY
				|| locationInParent == EnumConstantDeclaration.ARGUMENTS_PROPERTY

				|| locationInParent == LambdaExpression.BODY_PROPERTY
				|| locationInParent == ConditionalExpression.THEN_EXPRESSION_PROPERTY
				|| locationInParent == ConditionalExpression.ELSE_EXPRESSION_PROPERTY
				|| locationInParent == CastExpression.EXPRESSION_PROPERTY;

	}*/
}
