/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.corrections.IInvocationContext;
import org.eclipse.jdt.core.corrections.proposals.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.core.corrections.proposals.ChangeCorrectionProposal;
import org.eclipse.jdt.core.corrections.proposals.CodeActionKind;
import org.eclipse.jdt.core.corrections.proposals.IProposalRelevance;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.DimensionRewrite;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jface.text.link.LinkedPositionGroup;

public class QuickAssistProcessorUtil {

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

	public boolean getSplitVariableProposals(IInvocationContext context, ASTNode node,
			ArrayList<ChangeCorrectionProposal> resultingCollections) throws JavaModelException {
		if (resultingCollections == null) {
			return true;
		}
		VariableDeclarationFragment fragment;
		if (node instanceof VariableDeclarationFragment) {
			fragment = (VariableDeclarationFragment) node;
		} else if (node.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY) {
			fragment = (VariableDeclarationFragment) node.getParent();
		} else {
			return false;
		}

		if (fragment.getInitializer() == null) {
			return false;
		}

		Statement statement;
		ASTNode fragParent = fragment.getParent();
		boolean isVarType = false;
		if (fragParent instanceof VariableDeclarationStatement) {
			statement = (VariableDeclarationStatement) fragParent;
			Type type = ((VariableDeclarationStatement) fragParent).getType();
			isVarType = (type == null) ? false : type.isVar();
		} else if (fragParent instanceof VariableDeclarationExpression) {
			if (fragParent.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY) {
				return false;
			}
			statement = (Statement) fragParent.getParent();
			Type type = ((VariableDeclarationExpression) fragParent).getType();
			isVarType = (type == null) ? false : type.isVar();
		} else {
			return false;
		}
		if (!(statement instanceof ForStatement) && !(statement instanceof VariableDeclarationStatement)) {
			return false;
		}
		// statement is ForStatement or VariableDeclarationStatement
		ASTNode statementParent = statement.getParent();
		StructuralPropertyDescriptor property = statement.getLocationInParent();
		if (!property.isChildListProperty()) {
			return false;
		}

		List<? extends ASTNode> list = ASTNodes.getChildListProperty(statementParent,
				(ChildListPropertyDescriptor) property);

		AST ast = statement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);

		String label = CorrectionMessages.QuickAssistProcessor_splitdeclaration_description;
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, CodeActionKind.RefactorRewrite,
				context.getCompilationUnit(), rewrite, IProposalRelevance.SPLIT_VARIABLE_DECLARATION);

		// for multiple declarations; all must be moved outside, leave none behind
		if (statement instanceof ForStatement) {
			IBuffer buffer = context.getCompilationUnit().getBuffer();
			ForStatement forStatement = (ForStatement) statement;
			VariableDeclarationExpression oldVarDecl = (VariableDeclarationExpression) fragParent;
			Type type = oldVarDecl.getType();
			ITypeBinding tBinding = type.resolveBinding();
			List<VariableDeclarationFragment> oldFragments = oldVarDecl.fragments();
			CompilationUnit cup = (CompilationUnit) fragment.getRoot();
			ListRewrite forListRewrite = rewrite.getListRewrite(forStatement, ForStatement.INITIALIZERS_PROPERTY);
			// create the new initializers
			for (VariableDeclarationFragment oldFragment : oldFragments) {
				int extendedStartPositionFragment = cup.getExtendedStartPosition(oldFragment);
				int extendedLengthFragment = cup.getExtendedLength(oldFragment);
				StringBuilder codeFragment = new StringBuilder(
						buffer.getText(extendedStartPositionFragment, extendedLengthFragment));
				if (oldFragment.getInitializer() == null) {
					ITypeBinding typeBinding = type.resolveBinding();
					if ("Z".equals(typeBinding.getBinaryName())) { //$NON-NLS-1$
						codeFragment.append(" = false"); //$NON-NLS-1$
					} else if (type.isPrimitiveType()) {
						codeFragment.append(" = 0"); //$NON-NLS-1$
					} else {
						codeFragment.append(" = null"); //$NON-NLS-1$
					}
				}
				Assignment newAssignmentFragment = (Assignment) rewrite.createStringPlaceholder(codeFragment.toString(),
						ASTNode.ASSIGNMENT);
				forListRewrite.insertLast(newAssignmentFragment, null);
			}

			// create the new declarations
			Type nType = null;
			if (isVarType) {
				ImportRewrite importRewrite = proposal.createImportRewrite(context.getASTRoot());
				ImportRewriteContext icontext = new ContextSensitiveImportRewriteContext(cup, importRewrite);
				nType = importRewrite.addImport(tBinding, ast, icontext, TypeLocation.LOCAL_VARIABLE);
				String codeDeclaration = tBinding.getName();
				String commentToken = ""; //$NON-NLS-1$
				int extendedStatementStart = cup.getExtendedStartPosition(oldVarDecl);
				if (oldVarDecl.getStartPosition() > extendedStatementStart) {
					commentToken = buffer.getText(extendedStatementStart,
							oldVarDecl.getStartPosition() - extendedStatementStart);
				}
				codeDeclaration = commentToken + codeDeclaration;
				nType = (Type) rewrite.createStringPlaceholder(codeDeclaration.trim(), type.getNodeType());
			} else {
				int extendedStartPositionDeclaration = cup.getExtendedStartPosition(oldVarDecl);
				int firstFragmentStart = ((ASTNode) oldVarDecl.fragments().get(0)).getStartPosition();
				String codeDeclaration = buffer.getText(extendedStartPositionDeclaration,
						firstFragmentStart - extendedStartPositionDeclaration);
				nType = (Type) rewrite.createStringPlaceholder(codeDeclaration.trim(), type.getNodeType());
			}

			VariableDeclarationFragment newFrag = ast.newVariableDeclarationFragment();
			VariableDeclarationStatement newVarDec = ast.newVariableDeclarationStatement(newFrag);
			newVarDec.setType(nType);
			newFrag.setName(ast.newSimpleName(oldFragments.get(0).getName().getIdentifier()));
			newFrag.extraDimensions()
					.addAll(DimensionRewrite.copyDimensions(oldFragments.get(0).extraDimensions(), rewrite));

			for (int i = 1; i < oldFragments.size(); i++) {
				VariableDeclarationFragment oldFragment = oldFragments.get(i);
				newFrag = ast.newVariableDeclarationFragment();
				newFrag.setName(ast.newSimpleName(oldFragment.getName().getIdentifier()));
				newFrag.extraDimensions()
						.addAll(DimensionRewrite.copyDimensions(oldFragment.extraDimensions(), rewrite));
				newVarDec.fragments().add(newFrag);
			}
			newVarDec.modifiers().addAll(ASTNodeFactory.newModifiers(ast, oldVarDecl.getModifiers()));

			ListRewrite listRewriter = rewrite.getListRewrite(statementParent, (ChildListPropertyDescriptor) property);
			listRewriter.insertBefore(newVarDec, statement, null);

			rewrite.remove(oldVarDecl, null);

			resultingCollections.add(proposal);
			return true;
		}

		int insertIndex = list.indexOf(statement);
		ITypeBinding binding = fragment.getInitializer().resolveTypeBinding();
		Expression placeholder = (Expression) rewrite.createMoveTarget(fragment.getInitializer());
		if (placeholder instanceof ArrayInitializer && binding != null && binding.isArray()) {
			ArrayCreation creation = ast.newArrayCreation();
			creation.setInitializer((ArrayInitializer) placeholder);
			final ITypeBinding componentType = binding.getElementType();
			Type type = null;
			if (componentType.isPrimitive()) {
				type = ast.newPrimitiveType(PrimitiveType.toCode(componentType.getName()));
			} else {
				type = ast.newSimpleType(ast.newSimpleName(componentType.getName()));
			}
			creation.setType(ast.newArrayType(type, binding.getDimensions()));
			placeholder = creation;
		}

		Assignment assignment = ast.newAssignment();
		assignment.setRightHandSide(placeholder);
		assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().getIdentifier()));

		// statement is VariableDeclarationStatement
		Statement newStatement = ast.newExpressionStatement(assignment);
		;
		insertIndex += 1; // add after declaration

		if (isVarType) {
			VariableDeclarationStatement varDecl = (VariableDeclarationStatement) statement;
			CompilationUnit cup = (CompilationUnit) fragment.getRoot();
			Type type = varDecl.getType();
			ITypeBinding tBinding = type.resolveBinding();
			ImportRewrite importRewrite = proposal.createImportRewrite(context.getASTRoot());
			ImportRewriteContext icontext = new ContextSensitiveImportRewriteContext(cup, importRewrite);
			Type nType = importRewrite.addImport(tBinding, ast, icontext, TypeLocation.LOCAL_VARIABLE);
			rewrite.set(varDecl, VariableDeclarationStatement.TYPE_PROPERTY, nType, null);
			DimensionRewrite.removeAllChildren(fragment, VariableDeclarationFragment.EXTRA_DIMENSIONS2_PROPERTY,
					rewrite, null);
		}

		ListRewrite listRewriter = rewrite.getListRewrite(statementParent, (ChildListPropertyDescriptor) property);
		listRewriter.insertAt(newStatement, insertIndex, null);
		resultingCollections.add(proposal);
		return true;
	}
}
