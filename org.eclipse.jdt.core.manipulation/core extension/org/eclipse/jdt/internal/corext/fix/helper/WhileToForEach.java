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
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
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
			Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed) {
		ReferenceHolder<ASTNode, WhileLoopToChangeHit> dataholder= new ReferenceHolder<>();
		Map<ASTNode, WhileLoopToChangeHit> operationsMap= new LinkedHashMap<>();
		WhileLoopToChangeHit emptyHit= new WhileLoopToChangeHit();
		HelperVisitor.callVariableDeclarationStatementVisitor(Iterator.class, compilationUnit, dataholder, nodesprocessed, (init_iterator, holder_a) -> {
			List<Object> computeVarName= computeVarName(init_iterator);
			if (computeVarName != null) {
				HelperVisitor.callWhileStatementVisitor(init_iterator.getParent(), dataholder, nodesprocessed, (whilestatement, holder) -> {
					String name= computeNextVarname(whilestatement);
					if (computeVarName.get(0).equals(name)) {
						HelperVisitor.callMethodInvocationVisitor(whilestatement.getBody(), dataholder, nodesprocessed, (mi, holder2) -> {
							WhileLoopToChangeHit hit= holder2.computeIfAbsent(whilestatement, k -> new WhileLoopToChangeHit());
							SimpleName sn= ASTNodes.as(mi.getExpression(), SimpleName.class);
							if (sn != null) {
								String identifier= sn.getIdentifier();
								if (!name.equals(identifier))
									return true;
								String method= mi.getName().getFullyQualifiedName();
								if (operationsMap.get(whilestatement) != null || !method.equals("next")) { //$NON-NLS-1$
									operationsMap.put(whilestatement, emptyHit);
									return true;
								}
								hit.iteratordeclaration= init_iterator;
								if (computeVarName.size() == 1) {
									hit.self= true;
								} else {
									hit.collectionsimplename= (SimpleName) computeVarName.get(1);
								}
								hit.whilestatement= whilestatement;
								hit.loopvardeclaration= mi;
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
									hit.loopvarname= vdf.getName().getIdentifier();
								} else {
									if (hit.self) {
										hit.loopvarname= ConvertLoopOperation.modifybasename("i"); //$NON-NLS-1$
									} else {
										hit.loopvarname= ConvertLoopOperation.modifybasename(hit.collectionsimplename.getIdentifier());
									}
									hit.nextwithoutvariabledeclation= true;
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
//				ITypeBinding resolveTypeBinding = expression.resolveTypeBinding();
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
			TextEditGroup group_init, TextEditGroup group_while, TextEditGroup group_next) {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();

		EnhancedForStatement newEnhancedForStatement= ast.newEnhancedForStatement();
		newEnhancedForStatement.setBody(ASTNodes.createMoveTarget(rewrite, hit.whilestatement.getBody()));

		SingleVariableDeclaration result= ast.newSingleVariableDeclaration();

		SimpleName name= ast.newSimpleName(hit.loopvarname);
		result.setName(name);

		Expression expression= hit.loopvardeclaration.getExpression();
		SimpleName variable= ASTNodes.as(expression, SimpleName.class);
		String looptargettype;
		looptargettype= variable.resolveTypeBinding().getErasure().getQualifiedName();
		VariableDeclarationStatement typedAncestor= ASTNodes.getTypedAncestor(hit.loopvardeclaration, VariableDeclarationStatement.class);
		Type type;
		if (hit.nextwithoutvariabledeclation) {
//			ITypeBinding o=hit.loopvardeclaration.getExpression().resolveTypeBinding();
			type= null;
		} else {
			type= typedAncestor.getType();
		}
		ParameterizedType mytype= null;
		SimpleType object= null;
		Type type2= null;
		if (type == null) {
			looptargettype= "java.lang.Object"; //$NON-NLS-1$
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
			SimpleName createMoveTarget= ast.newSimpleName(hit.collectionsimplename.getIdentifier());
			newEnhancedForStatement.setExpression(createMoveTarget);
		}
		ASTNodes.removeButKeepComment(rewrite, hit.iteratordeclaration, group_init);
		if (hit.nextwithoutvariabledeclation) {
			ASTNodes.replaceButKeepComment(rewrite, hit.loopvardeclaration, name, group_next);
		} else {
			ASTNodes.removeButKeepComment(rewrite, ASTNodes.getTypedAncestor(hit.loopvardeclaration, VariableDeclarationStatement.class), group_next);
		}
		ASTNodes.replaceButKeepComment(rewrite, hit.whilestatement, newEnhancedForStatement, group_while);
	}

	@Override
	public String getPreview(boolean afterRefactoring) {
		if (afterRefactoring) {
			return "\nfor (String s : strings) {\n\n	System.out.println(s);\n}\n\n"; //$NON-NLS-1$
		}
		return "Iterator it = lists.iterator();\nwhile (it.hasNext()) {\n    String s = (String) it.next();\n	System.out.println(s);\n}\n\n"; //$NON-NLS-1$
	}
}
