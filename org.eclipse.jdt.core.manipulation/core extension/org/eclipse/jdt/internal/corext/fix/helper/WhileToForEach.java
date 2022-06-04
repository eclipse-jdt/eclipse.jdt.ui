/*******************************************************************************
 * Copyright (c) 2021, 2022 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.common.HelperVisitor;
import org.eclipse.jdt.internal.common.ReferenceHolder;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopOperation;
import org.eclipse.jdt.internal.corext.fix.UseIteratorToForLoopFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * Find: while (it.hasNext()){ System.out.println(it.next()); }
 *
 * Rewrite: for(Object o:collection) { System.out.println(o); });
 *
 */
public class WhileToForEach extends AbstractTool<WhileLoopToChangeHit> {

	@Override
	public void find(UseIteratorToForLoopFixCore fixcore, CompilationUnit compilationUnit,
			Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed, boolean createForOnlyIfVarUsed) {
		ReferenceHolder<ASTNode, WhileLoopToChangeHit> dataholder= new ReferenceHolder<>();
		Map<ASTNode, WhileLoopToChangeHit> operationsMap= new LinkedHashMap<>();
		WhileLoopToChangeHit emptyHit= new WhileLoopToChangeHit();
		HelperVisitor.callVariableDeclarationStatementVisitor(Iterator.class, compilationUnit, dataholder, nodesprocessed, (init_iterator, holder_a) -> {
			List<Object> computeVarName= computeVarName(init_iterator);
			if (computeVarName != null) {
				HelperVisitor.callWhileStatementVisitor(init_iterator.getParent(), dataholder, nodesprocessed, (whilestatement, holder) -> {
					String name= computeNextVarname(whilestatement);
					if (computeVarName.get(0).equals(name)) {
						WhileLoopToChangeHit hit= holder.computeIfAbsent(whilestatement, k -> new WhileLoopToChangeHit());
						if (!createForOnlyIfVarUsed) {
							hit.iteratorDeclaration= init_iterator;
							if (computeVarName.size() == 1) {
								hit.self= true;
							} else {
								hit.collectionSimplename= (SimpleName) computeVarName.get(1);
							}
							hit.whileStatement= whilestatement;
							if (hit.self) {
								hit.loopVarName= ConvertLoopOperation.modifybasename("i"); //$NON-NLS-1$
							} else {
								hit.loopVarName= ConvertLoopOperation.modifybasename(hit.collectionSimplename.getIdentifier());
							}
							operationsMap.put(whilestatement, hit);
						}
						HelperVisitor.callMethodInvocationVisitor(whilestatement.getBody(), dataholder, nodesprocessed, (mi, holder2) -> {
							SimpleName sn= ASTNodes.as(mi.getExpression(), SimpleName.class);
							if (sn != null) {
								String identifier= sn.getIdentifier();
								if (!name.equals(identifier))
									return true;
								String method= mi.getName().getFullyQualifiedName();
								WhileLoopToChangeHit previousHit= operationsMap.get(whilestatement);
								if (previousHit != null && (previousHit == emptyHit || previousHit.nextFound || !method.equals("next"))) { //$NON-NLS-1$
									operationsMap.put(whilestatement, emptyHit);
									return true;
								}
								if (ASTNodes.getFirstAncestorOrNull(mi, ExpressionStatement.class) != null
										&& createForOnlyIfVarUsed) {
									operationsMap.put(whilestatement, emptyHit);
									return true;
								}
								hit.nextFound= true;
								hit.iteratorDeclaration= init_iterator;
								hit.whileStatement= whilestatement;
								hit.loopVarDeclaration= mi;
								if (computeVarName.size() == 1) {
									hit.self= true;
								} else {
									hit.collectionSimplename= (SimpleName) computeVarName.get(1);
								}
								VariableDeclarationStatement typedAncestor= ASTNodes.getTypedAncestor(mi, VariableDeclarationStatement.class);
								if (typedAncestor != null) {
									ITypeBinding iteratorTypeArgument= computeTypeArgument(init_iterator);
									ITypeBinding varTypeBinding= typedAncestor.getType().resolveBinding();
									if (varTypeBinding == null || iteratorTypeArgument == null ||
											(!varTypeBinding.isEqualTo(iteratorTypeArgument) && !Bindings.isSuperType(varTypeBinding, iteratorTypeArgument))) {
										operationsMap.put(whilestatement, emptyHit);
										return true;
									}
									VariableDeclarationFragment vdf= (VariableDeclarationFragment) typedAncestor.fragments().get(0);
									hit.loopVarName= vdf.getName().getIdentifier();
								} else {
									if (hit.self) {
										hit.loopVarName= ConvertLoopOperation.modifybasename("i"); //$NON-NLS-1$
									} else {
										hit.loopVarName= ConvertLoopOperation.modifybasename(hit.collectionSimplename.getIdentifier());
									}
									hit.nextWithoutVariableDeclaration= true;
								}
								operationsMap.put(whilestatement, hit);
								HelperVisitor<ReferenceHolder<ASTNode, WhileLoopToChangeHit>, ASTNode, WhileLoopToChangeHit> helperVisitor= holder.getHelperVisitor();
								helperVisitor.nodesprocessed.add(whilestatement);
								holder2.remove(whilestatement);
								return true;
							}
							return true;
						});
					}
					return true;
				});
			}
			return true;
		});
		for (WhileLoopToChangeHit hit : operationsMap.values()) {
			if (hit != emptyHit) {
				operations.add(fixcore.rewrite(hit));
			}
		}
	}

	private static String computeNextVarname(WhileStatement whilestatement) {
		String name= null;
		Expression exp= whilestatement.getExpression();
		if (exp instanceof MethodInvocation) {
			MethodInvocation mi= (MethodInvocation) exp;
			if (mi.getName().getIdentifier().equals("hasNext")) { //$NON-NLS-1$
				SimpleName variable= ASTNodes.as(mi.getExpression(), SimpleName.class);
				if (variable != null) {
					IBinding resolveBinding= variable.resolveBinding();
					name= resolveBinding.getName();
				}
			}
		}
		return name;
	}

	private static List<Object> computeVarName(VariableDeclarationStatement node_a) {
		List<Object> name= new ArrayList<>();
		VariableDeclarationFragment bli= (VariableDeclarationFragment) node_a.fragments().get(0);
		name.add(bli.getName().getIdentifier());
		Expression exp= bli.getInitializer();
		MethodInvocation mi= ASTNodes.as(exp, MethodInvocation.class);
		if (mi != null && mi.getName().toString().equals("iterator")) { //$NON-NLS-1$
			ITypeBinding iterableAncestor= null;
			IMethodBinding miBinding= mi.resolveMethodBinding();
			if (miBinding != null) {
				iterableAncestor= ASTNodes.findImplementedType(miBinding.getDeclaringClass(), Iterable.class.getCanonicalName());
			}
			if (iterableAncestor == null || iterableAncestor.isRawType()) {
				return null;
			}
			SimpleName sn= ASTNodes.as(mi.getExpression(), SimpleName.class);
			if (sn != null) {
				name.add(sn);
			}
		}
		return name;
	}

	private static ITypeBinding computeTypeArgument(VariableDeclarationStatement node_a) {
		VariableDeclarationFragment bli= (VariableDeclarationFragment) node_a.fragments().get(0);
		Expression exp= bli.getInitializer();
		MethodInvocation mi= ASTNodes.as(exp, MethodInvocation.class);
		if (mi != null && mi.getName().toString().equals("iterator")) { //$NON-NLS-1$
			ITypeBinding iterableAncestor= null;
			IMethodBinding miBinding= mi.resolveMethodBinding();
			if (miBinding != null) {
				iterableAncestor= ASTNodes.findImplementedType(miBinding.getDeclaringClass(), Iterable.class.getCanonicalName());
			}
			if (iterableAncestor != null) {
				ITypeBinding[] typeArgs= iterableAncestor.getTypeArguments();
				if (typeArgs.length > 0) {
					return typeArgs[0];
				}
			}
		} else {
			ITypeBinding varTypeBinding= node_a.getType().resolveBinding();
			if (varTypeBinding != null) {
				ITypeBinding[] typeArgs= varTypeBinding.getTypeArguments();
				if (typeArgs.length > 0) {
					return typeArgs[0];
				}
			}
		}
		return node_a.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
	}

	@Override
	public void rewrite(UseIteratorToForLoopFixCore upp, final WhileLoopToChangeHit hit, final CompilationUnitRewrite cuRewrite,
			TextEditGroup group) {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();

		EnhancedForStatement newEnhancedForStatement= ast.newEnhancedForStatement();
		newEnhancedForStatement.setBody(ASTNodes.createMoveTarget(rewrite, hit.whileStatement.getBody()));

		SingleVariableDeclaration result= ast.newSingleVariableDeclaration();

		SimpleName name= ast.newSimpleName(hit.loopVarName);
		result.setName(name);

		String looptargettype;
		Type type;
		if (hit.nextWithoutVariableDeclaration || !hit.nextFound) {
			type= null;
		} else {
			Expression expression= hit.loopVarDeclaration.getExpression();
			SimpleName variable= ASTNodes.as(expression, SimpleName.class);
			looptargettype= variable.resolveTypeBinding().getErasure().getQualifiedName();
			VariableDeclarationStatement typedAncestor= ASTNodes.getTypedAncestor(hit.loopVarDeclaration, VariableDeclarationStatement.class);
			type= typedAncestor.getType();
		}
		ParameterizedType mytype= null;
		SimpleType object= null;
		Type type2= null;
		if (type == null) {
			looptargettype= "java.lang.Object"; //$NON-NLS-1$
			ITypeBinding binding= computeTypeArgument(hit.iteratorDeclaration);
			if (binding != null) {
				looptargettype= binding.getQualifiedName();
			}
			Type collectionType= ast.newSimpleType(addImport(looptargettype, cuRewrite, ast));
			result.setType(collectionType);
		} else if (type instanceof ParameterizedType) {
			mytype= (ParameterizedType) type;
			type2= mytype.getType();
			object= (SimpleType) mytype.typeArguments().get(0);
			looptargettype= type2.resolveBinding().getErasure().getQualifiedName();
			Type collectionType= ast.newSimpleType(addImport(looptargettype, cuRewrite, ast));
			ParameterizedType genericType= ast.newParameterizedType(collectionType);
			String fullyQualifiedName= object.getName().getFullyQualifiedName();
			genericType.typeArguments().add(ast.newSimpleType(ast.newName(fullyQualifiedName)));
			result.setType(genericType);
		} else {
			looptargettype= type.resolveBinding().getQualifiedName();
			Type collectionType= ast.newSimpleType(addImport(looptargettype, cuRewrite, ast));
			result.setType(collectionType);
		}

		newEnhancedForStatement.setParameter(result);
		if (hit.self) {
			ThisExpression newThisExpression= ast.newThisExpression();
			newEnhancedForStatement.setExpression(newThisExpression);
		} else {
			SimpleName createMoveTarget= ast.newSimpleName(hit.collectionSimplename.getIdentifier());
			newEnhancedForStatement.setExpression(createMoveTarget);
		}
		ASTNodes.removeButKeepComment(rewrite, hit.iteratorDeclaration, group);
		if (hit.nextFound) {
			if (hit.nextWithoutVariableDeclaration) {
				// remove it.next(); expression statements
				ASTNode loopVarDeclaration= hit.loopVarDeclaration;
				while (loopVarDeclaration.getParent() instanceof ParenthesizedExpression) {
					loopVarDeclaration= loopVarDeclaration.getParent();
				}
				if (loopVarDeclaration.getLocationInParent() == ExpressionStatement.EXPRESSION_PROPERTY) {
					rewrite.remove(loopVarDeclaration.getParent(), group);
				} else {
					ASTNodes.replaceButKeepComment(rewrite, hit.loopVarDeclaration, name, group);
				}
			} else {
				ASTNodes.removeButKeepComment(rewrite, ASTNodes.getTypedAncestor(hit.loopVarDeclaration, VariableDeclarationStatement.class), group);
			}
		}
		ASTNodes.replaceButKeepComment(rewrite, hit.whileStatement, newEnhancedForStatement, group);
	}

	@Override
	public String getPreview(boolean afterRefactoring) {
		if (afterRefactoring) {
			return "\nfor (String s : strings) {\n\n	System.out.println(s);\n}\n\n"; //$NON-NLS-1$
		}
		return "Iterator it = lists.iterator();\nwhile (it.hasNext()) {\n    String s = (String) it.next();\n	System.out.println(s);\n}\n\n"; //$NON-NLS-1$
	}
}
