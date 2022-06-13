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
package org.eclipse.jdt.internal.common;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.eclipse.jdt.core.dom.*;

/**
 * This class allows to use Lambda expressions for building up your visitor processing
 *
 * @author chammer
 *
 * @param <E> - type extending HelperVisitorProvider mapping V -> E entries
 * @param <V> - key type for HelperVisitorProvider
 * @param <T> - value type for HelperVisitorProvider
 */
public class HelperVisitor<E extends HelperVisitorProvider<V, T, E>,V,T> {

	ASTVisitor astvisitor;

	/**
	 *
	 */
	public E dataholder;

	/**
	 * This map contains one VisitorSupplier per kind if supplied Each BiPredicate is called with
	 * two parameters 1) ASTNode 2) your data object Call is processed when build(ASTNode) is
	 * called.
	 */
	Map<VisitorEnum, BiPredicate<? extends ASTNode, E>> predicatemap;

	/**
	 * This map contains one VisitorConsumer per kind if supplied Each BiConsumer is called with two
	 * parameters 1) ASTNode 2) your data object Call is processed when build(ASTNode) is called.
	 * Because the "visitend" does not return a boolean we need a consumer instead of a supplier
	 * here.
	 */
	Map<VisitorEnum, BiConsumer<? extends ASTNode, E>> consumermap;

	/**
	 * Here we store data to implement convenience methods like method visitor where the method name
	 * can be given as parameter
	 */
	Map<VisitorEnum, Object> predicatedata;

	Map<VisitorEnum, Object> consumerdata;

	/**
	 *
	 * @return - Map of visitor kinds -> BiPredicates
	 */
	public Map<VisitorEnum, BiPredicate<? extends ASTNode, E>> getSuppliermap() {
		return predicatemap;
	}

	/**
	 *
	 * @return - Map of visitor kinds -> BiConsumers
	 */
	public Map<VisitorEnum, BiConsumer<? extends ASTNode, E>> getConsumermap() {
		return consumermap;
	}

	/**
	 *
	 */
	public Set<ASTNode> nodesprocessed;

	/**
	 *
	 * @return - set of nodes processed
	 */
	public Set<ASTNode> getNodesprocessed() {
		return nodesprocessed;
	}

	/**
	 *
	 * @param nodesprocessed - set of nodes processed
	 * @param dataholder - HelperVisitorProvider providing this HelperVisitor
	 */
	public HelperVisitor(Set<ASTNode> nodesprocessed, E dataholder) {
		this.predicatemap= new LinkedHashMap<>();
		this.consumermap= new LinkedHashMap<>();
		this.predicatedata= new HashMap<>();
		this.consumerdata= new HashMap<>();

		this.dataholder= dataholder;
		dataholder.setHelperVisitor(this);
		this.nodesprocessed= nodesprocessed;
	}

	/**
	 *
	 * @param node - ASTNode
	 * @return - HelperVisitor
	 */
	public HelperVisitor<E, V, T> build(ASTNode node) {
		return build(node, false);
	}

	/**
	 *
	 * @param node - ASTNode
	 * @param visitjavadoc - true if Javadoc comments should be visited
	 * @return - HelperVisitor
	 */
	public HelperVisitor<E, V, T> build(ASTNode node, boolean visitjavadoc) {
		astvisitor= new LambdaASTVisitor<>(this, visitjavadoc);
		node.accept(astvisitor);
		return this;
	}

	/**
	 * Add BiPredicate for visitor kind
	 *
	 * @param key - visitor kind
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate added or null
	 */
	public BiPredicate<? extends ASTNode, E> add(VisitorEnum key, BiPredicate<? extends ASTNode, E> bs) {
		return predicatemap.put(key, bs);
	}

	/**
	 * Add BiPrediate for visitor kind with additional Object
	 *
	 * @param object - additional Object to put in predicate map
	 * @param key - visitor kind
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate added or null
	 */
	public BiPredicate<? extends ASTNode, E> add(Object object, VisitorEnum key, BiPredicate<? extends ASTNode, E> bs) {
		this.predicatedata.put(key, object);
		return predicatemap.put(key, bs);
	}

	/**
	 * Add BiConsumer for visitor kind to consumer map
	 *
	 * @param key - visitor kind
	 * @param bc - BiConsumer
	 * @return - previous BiConsumer for visitor kind or null
	 */
	public BiConsumer<? extends ASTNode, E> addEnd(VisitorEnum key, BiConsumer<? extends ASTNode, E> bc) {
		return consumermap.put(key, bc);
	}

	/**
	 * Add BiPredidate and BiConsumer to predicate and consumer maps
	 *
	 * @param key - visitor kind
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc - BiConsumer
	 */
	public void add(VisitorEnum key, BiPredicate<ASTNode, E> bs, BiConsumer<? extends ASTNode, E> bc) {
		predicatemap.put(key, bs);
		consumermap.put(key, bc);
	}

	/**
	 * Remove visitor kind entries from predicate and consumer maps
	 *
	 * @param ve - visitor kind
	 */
	public void removeVisitor(VisitorEnum ve) {
		this.predicatemap.remove(ve);
		this.consumermap.remove(ve);
	}

	/**
	 * Get consumer map
	 *
	 * @return - Map of visitor kind to consumer
	 */
	protected Map<VisitorEnum, Object> getConsumerData() {
		return this.consumerdata;
	}

	/**
	 * Get predicate map
	 *
	 * @return - Map of visitor kind to predicate
	 */
	protected Map<VisitorEnum, Object> getSupplierData() {
		return this.predicatedata;
	}

	/**
	 * Add BiPredicate to use for AnnotationTypeDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate for annotation type declaration
	 */
	public BiPredicate<? extends ASTNode, E> addAnnotationTypeDeclaration(
			BiPredicate<AnnotationTypeDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.AnnotationTypeDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for AnnotationTypeMemberDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addAnnotationTypeMemberDeclaration(
			BiPredicate<AnnotationTypeMemberDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.AnnotationTypeMemberDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for AnonymousClassDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addAnonymousClassDeclaration(
			BiPredicate<AnonymousClassDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.AnonymousClassDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for ArrayAccess visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addArrayAccess(BiPredicate<ArrayAccess, E> bs) {
		return predicatemap.put(VisitorEnum.ArrayAccess, bs);
	}

	/**
	 * Add BiPredicate to use for ArrayCreation visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addArrayCreation(BiPredicate<ArrayCreation, E> bs) {
		return predicatemap.put(VisitorEnum.ArrayCreation, bs);
	}

	/**
	 * Add BiPredicate to use for ArrayInitializer visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addArrayInitializer(BiPredicate<ArrayInitializer, E> bs) {
		return predicatemap.put(VisitorEnum.ArrayInitializer, bs);
	}

	/**
	 * Add BiPredicat to use for ArrayType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addArrayType(BiPredicate<ArrayType, E> bs) {
		return predicatemap.put(VisitorEnum.ArrayType, bs);
	}

	/**
	 * Add BiPredicate to use for AssertStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addAssertStatement(BiPredicate<AssertStatement, E> bs) {
		return predicatemap.put(VisitorEnum.AssertStatement, bs);
	}

	/**
	 * Add BiPredicate to use for Assignment visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addAssignment(BiPredicate<Assignment, E> bs) {
		return predicatemap.put(VisitorEnum.Assignment, bs);
	}

	/**
	 * Add BiPredicate to use for Block visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addBlock(BiPredicate<Block, E> bs) {
		return predicatemap.put(VisitorEnum.Block, bs);
	}

	/**
	 * Add BiPredicate to use for BlockComment visit
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addBlockComment(BiPredicate<BlockComment, E> bs) {
		return predicatemap.put(VisitorEnum.BlockComment, bs);
	}

	/**
	 * Add BiPredicate to use for BooleanLiteral visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addBooleanLiteral(BiPredicate<BooleanLiteral, E> bs) {
		return predicatemap.put(VisitorEnum.BooleanLiteral, bs);
	}

	/**
	 * Add BiPredicate to use for BreakStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addBreakStatement(BiPredicate<BreakStatement, E> bs) {
		return predicatemap.put(VisitorEnum.BreakStatement, bs);
	}

	/**
	 * Add BiPredicate to use for CastExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addCastExpression(BiPredicate<CastExpression, E> bs) {
		return predicatemap.put(VisitorEnum.CastExpression, bs);
	}

	/**
	 * Add BiPredicate to use for CatchClause visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addCatchClause(BiPredicate<CatchClause, E> bs) {
		return predicatemap.put(VisitorEnum.CatchClause, bs);
	}

	/**
	 * Add BiPredicate to use for CharacterLiteral visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addCharacterLiteral(BiPredicate<CharacterLiteral, E> bs) {
		return predicatemap.put(VisitorEnum.CharacterLiteral, bs);
	}

	/**
	 * Add BiPredicate to use for ClassInstanceCreation visit
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addClassInstanceCreation(BiPredicate<ClassInstanceCreation, E> bs) {
		return predicatemap.put(VisitorEnum.ClassInstanceCreation, bs);
	}

	/**
	 * Add BiPredicate to use for CompilationUnit visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addCompilationUnit(BiPredicate<CompilationUnit, E> bs) {
		return predicatemap.put(VisitorEnum.CompilationUnit, bs);
	}

	/**
	 * Add BiPredicate to use for ConditionalExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addConditionalExpression(BiPredicate<ConditionalExpression, E> bs) {
		return predicatemap.put(VisitorEnum.ConditionalExpression, bs);
	}

	/**
	 * Add BiPredicate to use for ConstructorInvocation visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addConstructorInvocation(BiPredicate<ConstructorInvocation, E> bs) {
		return predicatemap.put(VisitorEnum.ConstructorInvocation, bs);
	}

	/**
	 * Add BiPredicate to use for ContinueStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addContinueStatement(BiPredicate<ContinueStatement, E> bs) {
		return predicatemap.put(VisitorEnum.ContinueStatement, bs);
	}

	/**
	 * Add BiPredicate to use for CreationReference visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addCreationReference(BiPredicate<CreationReference, E> bs) {
		return predicatemap.put(VisitorEnum.CreationReference, bs);
	}

	/**
	 * Add BiPredicate to use for Dimension visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addDimension(BiPredicate<Dimension, E> bs) {
		return predicatemap.put(VisitorEnum.Dimension, bs);
	}

	/**
	 * Add BiPredicate to use for DoStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addDoStatement(BiPredicate<DoStatement, E> bs) {
		return predicatemap.put(VisitorEnum.DoStatement, bs);
	}

	/**
	 * Add BiPredicate to use for EmptyStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addEmptyStatement(BiPredicate<EmptyStatement, E> bs) {
		return predicatemap.put(VisitorEnum.EmptyStatement, bs);
	}

	/**
	 * Add BiPredicate to use for EnhancedForStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addEnhancedForStatement(BiPredicate<EnhancedForStatement, E> bs) {
		return predicatemap.put(VisitorEnum.EnhancedForStatement, bs);
	}

	/**
	 * Add BiPredicate to use for EnumConstantDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addEnumConstantDeclaration(BiPredicate<EnumConstantDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.EnumConstantDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for EnumDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addEnumDeclaration(BiPredicate<EnumDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.EnumDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for ExportsDirective visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addExportsDirective(BiPredicate<ExportsDirective, E> bs) {
		return predicatemap.put(VisitorEnum.ExportsDirective, bs);
	}

	/**
	 * Add BiPredicate to use for ExpressionMethodReference visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addExpressionMethodReference(
			BiPredicate<ExpressionMethodReference, E> bs) {
		return predicatemap.put(VisitorEnum.ExpressionMethodReference, bs);
	}

	/**
	 * Add BiPredicate to use for ExpressionStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addExpressionStatement(BiPredicate<ExpressionStatement, E> bs) {
		return predicatemap.put(VisitorEnum.ExpressionStatement, bs);
	}

	/**
	 * Add BiPredicate to use for FieldAccess visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addFieldAccess(BiPredicate<FieldAccess, E> bs) {
		return predicatemap.put(VisitorEnum.FieldAccess, bs);
	}

	/**
	 * Add BiPredicate to use for FieldDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addFieldDeclaration(BiPredicate<FieldDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.FieldDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for a ForStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addForStatement(BiPredicate<ForStatement, E> bs) {
		return predicatemap.put(VisitorEnum.ForStatement, bs);
	}

	/**
	 * Add BiPredicate to use for IfStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addIfStatement(BiPredicate<IfStatement, E> bs) {
		return predicatemap.put(VisitorEnum.IfStatement, bs);
	}

	/**
	 * Add BiPredicate to use for ImportDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addImportDeclaration(BiPredicate<ImportDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.ImportDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for InfixExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addInfixExpression(BiPredicate<InfixExpression, E> bs) {
		return predicatemap.put(VisitorEnum.InfixExpression, bs);
	}

	/**
	 * Add BiPredicate to use for Initializer visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addInitializer(BiPredicate<Initializer, E> bs) {
		return predicatemap.put(VisitorEnum.Initializer, bs);
	}

	/**
	 * Add BiPredicate to use for InstanceofExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addInstanceofExpression(BiPredicate<InstanceofExpression, E> bs) {
		return predicatemap.put(VisitorEnum.InstanceofExpression, bs);
	}

	/**
	 * Add BiPredicate to use for IntersectionType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addIntersectionType(BiPredicate<IntersectionType, E> bs) {
		return predicatemap.put(VisitorEnum.IntersectionType, bs);
	}

	/**
	 * Add BiPredicate to use for Javadoc visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addJavadoc(BiPredicate<Javadoc, E> bs) {
		return predicatemap.put(VisitorEnum.Javadoc, bs);
	}

	/**
	 * Add BiPredicate to use for LabeledStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addLabeledStatement(BiPredicate<LabeledStatement, E> bs) {
		return predicatemap.put(VisitorEnum.LabeledStatement, bs);
	}

	/**
	 * Add BiPredicate to use for LambdaExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addLambdaExpression(BiPredicate<LambdaExpression, E> bs) {
		return predicatemap.put(VisitorEnum.LambdaExpression, bs);
	}

	/**
	 * Add BiPredicate to use for LineComment visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addLineComment(BiPredicate<LineComment, E> bs) {
		return predicatemap.put(VisitorEnum.LineComment, bs);
	}

	/**
	 * Add BiPredicate to use for MarkerAnnotation visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addMarkerAnnotation(BiPredicate<MarkerAnnotation, E> bs) {
		return predicatemap.put(VisitorEnum.MarkerAnnotation, bs);
	}

	/**
	 * Add BiPredicate to use for MemberRef visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addMemberRef(BiPredicate<MemberRef, E> bs) {
		return predicatemap.put(VisitorEnum.MemberRef, bs);
	}

	/**
	 * Add BiPredicate to use for MemberValuePair visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addMemberValuePair(BiPredicate<MemberValuePair, E> bs) {
		return predicatemap.put(VisitorEnum.MemberValuePair, bs);
	}

	/**
	 * Add BiPredicate to use for MethodRef visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addMethodRef(BiPredicate<MethodRef, E> bs) {
		return predicatemap.put(VisitorEnum.MethodRef, bs);
	}

	/**
	 * Add BiPredicate to use for MethodRefParameter visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addMethodRefParameter(BiPredicate<MethodRefParameter, E> bs) {
		return predicatemap.put(VisitorEnum.MethodRefParameter, bs);
	}

	/**
	 * Add BiPredicate to use for MethodDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addMethodDeclaration(BiPredicate<MethodDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.MethodDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for MethodInvocation visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addMethodInvocation(BiPredicate<MethodInvocation, E> bs) {
		return predicatemap.put(VisitorEnum.MethodInvocation, bs);
	}

	/**
	 * Add BiPredicate to use for MethodInvocation visit where method name is specified
	 *
	 * @param methodname
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addMethodInvocation(String methodname,
			BiPredicate<MethodInvocation, E> bs) {
		this.predicatedata.put(VisitorEnum.MethodInvocation, methodname);
		return predicatemap.put(VisitorEnum.MethodInvocation, bs);
	}

	/**
	 * Add BiPredicate to use for Modifier visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addModifier(BiPredicate<Modifier, E> bs) {
		return predicatemap.put(VisitorEnum.Modifier, bs);
	}

	/**
	 * Add BiPredicate to sue for ModuleDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addModuleDeclaration(BiPredicate<ModuleDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.ModuleDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for ModuleModifier visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addModuleModifier(BiPredicate<ModuleModifier, E> bs) {
		return predicatemap.put(VisitorEnum.ModuleModifier, bs);
	}

	/**
	 * Add BiPredicate to use for NameQualifiedType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addNameQualifiedType(BiPredicate<NameQualifiedType, E> bs) {
		return predicatemap.put(VisitorEnum.NameQualifiedType, bs);
	}

	/**
	 * Add BiPredicate to use for NormalAnnotation visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addNormalAnnotation(BiPredicate<NormalAnnotation, E> bs) {
		return predicatemap.put(VisitorEnum.NormalAnnotation, bs);
	}

	/**
	 * Add BiPredicate to use for NullLiteral visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addNullLiteral(BiPredicate<NullLiteral, E> bs) {
		return predicatemap.put(VisitorEnum.NullLiteral, bs);
	}

	/**
	 * Add BiPredicate to use for NumberLiteral visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addNumberLiteral(BiPredicate<NumberLiteral, E> bs) {
		return predicatemap.put(VisitorEnum.NumberLiteral, bs);
	}

	/**
	 * Add BiPredicate to use for OpensDirective visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addOpensDirective(BiPredicate<OpensDirective, E> bs) {
		return predicatemap.put(VisitorEnum.OpensDirective, bs);
	}

	/**
	 * Add BiPredicate to use for PackageDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addPackageDeclaration(BiPredicate<PackageDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.PackageDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for ParameterizedType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addParameterizedType(BiPredicate<ParameterizedType, E> bs) {
		return predicatemap.put(VisitorEnum.ParameterizedType, bs);
	}

	/**
	 * Add BiPredicate to use for ParenthesizedExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addParenthesizedExpression(BiPredicate<ParenthesizedExpression, E> bs) {
		return predicatemap.put(VisitorEnum.ParenthesizedExpression, bs);
	}

	/**
	 * Add BiPredicate to use for InstanceofExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addPatternInstanceofExpression(
			BiPredicate<PatternInstanceofExpression, E> bs) {
		return predicatemap.put(VisitorEnum.PatternInstanceofExpression, bs);
	}

	/**
	 * Add BiPredicate to use for PostfixExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addPostfixExpression(BiPredicate<PostfixExpression, E> bs) {
		return predicatemap.put(VisitorEnum.PostfixExpression, bs);
	}

	/**
	 * Add BiPredicate to use for PrefixExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addPrefixExpression(BiPredicate<PrefixExpression, E> bs) {
		return predicatemap.put(VisitorEnum.PrefixExpression, bs);
	}

	/**
	 * Add BiPredicate to use for ProvidesDirective visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addProvidesDirective(BiPredicate<ProvidesDirective, E> bs) {
		return predicatemap.put(VisitorEnum.ProvidesDirective, bs);
	}

	/**
	 * Add BiPredicate to use for PrimitiveType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addPrimitiveType(BiPredicate<PrimitiveType, E> bs) {
		return predicatemap.put(VisitorEnum.PrimitiveType, bs);
	}

	/**
	 * Add BiPredicate to use for QualifiedName visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addQualifiedName(BiPredicate<QualifiedName, E> bs) {
		return predicatemap.put(VisitorEnum.QualifiedName, bs);
	}

	/**
	 * Add BiPredicate to use for QualifiedType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addQualifiedType(BiPredicate<QualifiedType, E> bs) {
		return predicatemap.put(VisitorEnum.QualifiedType, bs);
	}

//	public BiPredicate<? extends ASTNode, E> addModuleQualifiedName(
//			BiPredicate<ModuleQualifiedName, E> bs) {
//		return predicatemap.put(VisitorEnum.ModuleQualifiedName, bs);
//	}

	/**
	 * Add BiPredicate to use for RequiresdDirective visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */

	public BiPredicate<? extends ASTNode, E> addRequiresDirective(BiPredicate<RequiresDirective, E> bs) {
		return predicatemap.put(VisitorEnum.RequiresDirective, bs);
	}

	/**
	 * Add BiPredicate to use for RecordDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addRecordDeclaration(BiPredicate<RecordDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.RecordDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for ReturnStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addReturnStatement(BiPredicate<ReturnStatement, E> bs) {
		return predicatemap.put(VisitorEnum.ReturnStatement, bs);
	}

	/**
	 * Add BiPredicate to use for SimpleName visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSimpleName(BiPredicate<SimpleName, E> bs) {
		return predicatemap.put(VisitorEnum.SimpleName, bs);
	}

	/**
	 * Add BiPredicate to use for SimpleType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSimpleType(BiPredicate<SimpleType, E> bs) {
		return predicatemap.put(VisitorEnum.SimpleType, bs);
	}

	/**
	 * Add BiPredicate to use for SingleMemberAnnotation visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSingleMemberAnnotation(BiPredicate<SingleMemberAnnotation, E> bs) {
		return predicatemap.put(VisitorEnum.SingleMemberAnnotation, bs);
	}

	/**
	 * Add BiPredicate to use for SingleVariableDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSingleVariableDeclaration(
			BiPredicate<SingleVariableDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.SingleVariableDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for StringLiteral visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addStringLiteral(BiPredicate<StringLiteral, E> bs) {
		return predicatemap.put(VisitorEnum.StringLiteral, bs);
	}

	/**
	 * Add BiPredicate to use for SuperConstructorInvocation visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSuperConstructorInvocation(
			BiPredicate<SuperConstructorInvocation, E> bs) {
		return predicatemap.put(VisitorEnum.SuperConstructorInvocation, bs);
	}

	/**
	 * Add BiPredicate to use for SuperFieldAccess visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSuperFieldAccess(BiPredicate<SuperFieldAccess, E> bs) {
		return predicatemap.put(VisitorEnum.SuperFieldAccess, bs);
	}

	/**
	 * Add BiPredicate to use for SuperMethodInvocation visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSuperMethodInvocation(BiPredicate<SuperMethodInvocation, E> bs) {
		return predicatemap.put(VisitorEnum.SuperMethodInvocation, bs);
	}

	/**
	 * Add BiPredicate to use for SuperMethodReference visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSuperMethodReference(BiPredicate<SuperMethodReference, E> bs) {
		return predicatemap.put(VisitorEnum.SuperMethodReference, bs);
	}

	/**
	 * Add BiPredicate to use for SwitchCase visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSwitchCase(BiPredicate<SwitchCase, E> bs) {
		return predicatemap.put(VisitorEnum.SwitchCase, bs);
	}

	/**
	 * Add BiPredicate to use for SwitchExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSwitchExpression(BiPredicate<SwitchExpression, E> bs) {
		return predicatemap.put(VisitorEnum.SwitchExpression, bs);
	}

	/**
	 * Add BiPredicate to use for SwitchStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSwitchStatement(BiPredicate<SwitchStatement, E> bs) {
		return predicatemap.put(VisitorEnum.SwitchStatement, bs);
	}

	/**
	 * Add BiPredicate to use for SynchronizedStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addSynchronizedStatement(BiPredicate<SynchronizedStatement, E> bs) {
		return predicatemap.put(VisitorEnum.SynchronizedStatement, bs);
	}

	/**
	 * Add BiPredicate to use for TagElement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTagElement(BiPredicate<TagElement, E> bs) {
		return predicatemap.put(VisitorEnum.TagElement, bs);
	}

	/**
	 * Add BiPredicate to use for TextBlock visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTextBlock(BiPredicate<TextBlock, E> bs) {
		return predicatemap.put(VisitorEnum.TextBlock, bs);
	}

	/**
	 * Add BiPredicate to use for TextElement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTextElement(BiPredicate<TextElement, E> bs) {
		return predicatemap.put(VisitorEnum.TextElement, bs);
	}

	/**
	 * Add BiPredicate to use for ThisExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addThisExpression(BiPredicate<ThisExpression, E> bs) {
		return predicatemap.put(VisitorEnum.ThisExpression, bs);
	}

	/**
	 * Add BiPredicate to use for ThrowStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addThrowStatement(BiPredicate<ThrowStatement, E> bs) {
		return predicatemap.put(VisitorEnum.ThrowStatement, bs);
	}

	/**
	 * Add BiPredicate to use for TryStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTryStatement(BiPredicate<TryStatement, E> bs) {
		return predicatemap.put(VisitorEnum.TryStatement, bs);
	}

	/**
	 * Add BiPredicate to use for TypeDeclaration visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTypeDeclaration(BiPredicate<TypeDeclaration, E> bs) {
		return predicatemap.put(VisitorEnum.TypeDeclaration, bs);
	}

	/**
	 * Add BiPredicate to use for TypeDeclarationStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTypeDeclarationStatement(BiPredicate<TypeDeclarationStatement, E> bs) {
		return predicatemap.put(VisitorEnum.TypeDeclarationStatement, bs);
	}

	/**
	 * Add BiPredicate to use for TypeLiteral visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTypeLiteral(BiPredicate<TypeLiteral, E> bs) {
		return predicatemap.put(VisitorEnum.TypeLiteral, bs);
	}

	/**
	 * Add BiPredicate to use for TypeMethodReference visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTypeMethodReference(BiPredicate<TypeMethodReference, E> bs) {
		return predicatemap.put(VisitorEnum.TypeMethodReference, bs);
	}

	/**
	 * Add BiPredicate to use for TypeParameter visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addTypeParameter(BiPredicate<TypeParameter, E> bs) {
		return predicatemap.put(VisitorEnum.TypeParameter, bs);
	}

	/**
	 * Add BiPredicate to use for UnionType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addUnionType(BiPredicate<UnionType, E> bs) {
		return predicatemap.put(VisitorEnum.UnionType, bs);
	}

	/**
	 * Add BiPredicate to use for UsesDirective visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addUsesDirective(BiPredicate<UsesDirective, E> bs) {
		return predicatemap.put(VisitorEnum.UsesDirective, bs);
	}

	/**
	 * Add BiPredicate to use for VariableDeclarationExpression visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addVariableDeclarationExpression(
			BiPredicate<VariableDeclarationExpression, E> bs) {
		return predicatemap.put(VisitorEnum.VariableDeclarationExpression, bs);
	}

	/**
	 * Add BiPredicate to use for VariableDeclarationStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addVariableDeclarationStatement(
			BiPredicate<VariableDeclarationStatement, E> bs) {
		return predicatemap.put(VisitorEnum.VariableDeclarationStatement, bs);
	}

	/**
	 * Add BiPredicate to use for VariableDeclarationStatement visit when type specified matches
	 *
	 * @param class1 - specified type to match for VariableDeclarationStatement
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addVariableDeclarationStatement(Class<?> class1,
			BiPredicate<VariableDeclarationStatement, E> bs) {
		this.predicatedata.put(VisitorEnum.VariableDeclarationStatement, class1);
		return predicatemap.put(VisitorEnum.VariableDeclarationStatement, bs);
	}

	/**
	 * Add BiPredicate to use for VariableDeclarationFragment visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addVariableDeclarationFragment(
			BiPredicate<VariableDeclarationFragment, E> bs) {
		return predicatemap.put(VisitorEnum.VariableDeclarationFragment, bs);
	}

	/**
	 * Add BiPredicate to use for WhileStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addWhileStatement(BiPredicate<WhileStatement, E> bs) {
		return predicatemap.put(VisitorEnum.WhileStatement, bs);
	}

	/**
	 * Add BiPredicate to use for WildcardType visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addWildcardType(BiPredicate<WildcardType, E> bs) {
		return predicatemap.put(VisitorEnum.WildcardType, bs);
	}

	/**
	 * Add BiPredicate to use for YieldStatement visit
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @return - previous BiPredicate registered
	 */
	public BiPredicate<? extends ASTNode, E> addYieldStatement(BiPredicate<YieldStatement, E> bs) {
		return predicatemap.put(VisitorEnum.YieldStatement, bs);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addAnnotationTypeDeclaration(BiConsumer<AnnotationTypeDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.AnnotationTypeDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addAnnotationTypeMemberDeclaration(
			BiConsumer<AnnotationTypeMemberDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.AnnotationTypeMemberDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addAnonymousClassDeclaration(BiConsumer<AnonymousClassDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.AnonymousClassDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addArrayAccess(BiConsumer<ArrayAccess, E> bc) {
		return consumermap.put(VisitorEnum.ArrayAccess, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addArrayCreation(BiConsumer<ArrayCreation, E> bc) {
		return consumermap.put(VisitorEnum.ArrayCreation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addArrayInitializer(BiConsumer<ArrayInitializer, E> bc) {
		return consumermap.put(VisitorEnum.ArrayInitializer, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addArrayType(BiConsumer<ArrayType, E> bc) {
		return consumermap.put(VisitorEnum.ArrayType, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addAssertStatement(BiConsumer<AssertStatement, E> bc) {
		return consumermap.put(VisitorEnum.AssertStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addAssignment(BiConsumer<Assignment, E> bc) {
		return consumermap.put(VisitorEnum.Assignment, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addBlock(BiConsumer<Block, E> bc) {
		return consumermap.put(VisitorEnum.Block, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addBlockComment(BiConsumer<BlockComment, E> bc) {
		return consumermap.put(VisitorEnum.BlockComment, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addBooleanLiteral(BiConsumer<BooleanLiteral, E> bc) {
		return consumermap.put(VisitorEnum.BooleanLiteral, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addBreakStatement(BiConsumer<BreakStatement, E> bc) {
		return consumermap.put(VisitorEnum.BreakStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addCastExpression(BiConsumer<CastExpression, E> bc) {
		return consumermap.put(VisitorEnum.CastExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addCatchClause(BiConsumer<CatchClause, E> bc) {
		return consumermap.put(VisitorEnum.CatchClause, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addCharacterLiteral(BiConsumer<CharacterLiteral, E> bc) {
		return consumermap.put(VisitorEnum.CharacterLiteral, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addClassInstanceCreation(BiConsumer<ClassInstanceCreation, E> bc) {
		return consumermap.put(VisitorEnum.ClassInstanceCreation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addCompilationUnit(BiConsumer<CompilationUnit, E> bc) {
		return consumermap.put(VisitorEnum.CompilationUnit, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addConditionalExpression(BiConsumer<ConditionalExpression, E> bc) {
		return consumermap.put(VisitorEnum.ConditionalExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addConstructorInvocation(BiConsumer<ConstructorInvocation, E> bc) {
		return consumermap.put(VisitorEnum.ConstructorInvocation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addContinueStatement(BiConsumer<ContinueStatement, E> bc) {
		return consumermap.put(VisitorEnum.ContinueStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addCreationReference(BiConsumer<CreationReference, E> bc) {
		return consumermap.put(VisitorEnum.CreationReference, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addDimension(BiConsumer<Dimension, E> bc) {
		return consumermap.put(VisitorEnum.Dimension, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addDoStatement(BiConsumer<DoStatement, E> bc) {
		return consumermap.put(VisitorEnum.DoStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addEmptyStatement(BiConsumer<EmptyStatement, E> bc) {
		return consumermap.put(VisitorEnum.EmptyStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addEnhancedForStatement(BiConsumer<EnhancedForStatement, E> bc) {
		return consumermap.put(VisitorEnum.EnhancedForStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addEnumConstantDeclaration(BiConsumer<EnumConstantDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.EnumConstantDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addEnumDeclaration(BiConsumer<EnumDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.EnumDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addExportsDirective(BiConsumer<ExportsDirective, E> bc) {
		return consumermap.put(VisitorEnum.ExportsDirective, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addExpressionMethodReference(BiConsumer<ExpressionMethodReference, E> bc) {
		return consumermap.put(VisitorEnum.ExpressionMethodReference, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addExpressionStatement(BiConsumer<ExpressionStatement, E> bc) {
		return consumermap.put(VisitorEnum.ExpressionStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addFieldAccess(BiConsumer<FieldAccess, E> bc) {
		return consumermap.put(VisitorEnum.FieldAccess, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addFieldDeclaration(BiConsumer<FieldDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.FieldDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addForStatement(BiConsumer<ForStatement, E> bc) {
		return consumermap.put(VisitorEnum.ForStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addIfStatement(BiConsumer<IfStatement, E> bc) {
		return consumermap.put(VisitorEnum.IfStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addImportDeclaration(BiConsumer<ImportDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.ImportDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addInfixExpression(BiConsumer<InfixExpression, E> bc) {
		return consumermap.put(VisitorEnum.InfixExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addInitializer(BiConsumer<Initializer, E> bc) {
		return consumermap.put(VisitorEnum.Initializer, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addInstanceofExpression(BiConsumer<InstanceofExpression, E> bc) {
		return consumermap.put(VisitorEnum.InstanceofExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addIntersectionType(BiConsumer<IntersectionType, E> bc) {
		return consumermap.put(VisitorEnum.IntersectionType, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addJavadoc(BiConsumer<Javadoc, E> bc) {
		return consumermap.put(VisitorEnum.Javadoc, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addLabeledStatement(BiConsumer<LabeledStatement, E> bc) {
		return consumermap.put(VisitorEnum.LabeledStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addLambdaExpression(BiConsumer<LambdaExpression, E> bc) {
		return consumermap.put(VisitorEnum.LambdaExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addLineComment(BiConsumer<LineComment, E> bc) {
		return consumermap.put(VisitorEnum.LineComment, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addMarkerAnnotation(BiConsumer<MarkerAnnotation, E> bc) {
		return consumermap.put(VisitorEnum.MarkerAnnotation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addMemberRef(BiConsumer<MemberRef, E> bc) {
		return consumermap.put(VisitorEnum.MemberRef, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addMemberValuePair(BiConsumer<MemberValuePair, E> bc) {
		return consumermap.put(VisitorEnum.MemberValuePair, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addMethodRef(BiConsumer<MethodRef, E> bc) {
		return consumermap.put(VisitorEnum.MethodRef, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addMethodRefParameter(BiConsumer<MethodRefParameter, E> bc) {
		return consumermap.put(VisitorEnum.MethodRefParameter, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addMethodDeclaration(BiConsumer<MethodDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.MethodDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addMethodInvocation(BiConsumer<MethodInvocation, E> bc) {
		return consumermap.put(VisitorEnum.MethodInvocation, bc);
	}

	/**
	 *
	 * @param methodname
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addMethodInvocation(String methodname, BiConsumer<MethodInvocation, E> bc) {
		this.consumerdata.put(VisitorEnum.MethodInvocation, methodname);
		return consumermap.put(VisitorEnum.MethodInvocation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addModifier(BiConsumer<Modifier, E> bc) {
		return consumermap.put(VisitorEnum.Modifier, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addModuleDeclaration(BiConsumer<ModuleDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.ModuleDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addModuleModifier(BiConsumer<ModuleModifier, E> bc) {
		return consumermap.put(VisitorEnum.ModuleModifier, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addNameQualifiedType(BiConsumer<NameQualifiedType, E> bc) {
		return consumermap.put(VisitorEnum.NameQualifiedType, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addNormalAnnotation(BiConsumer<NormalAnnotation, E> bc) {
		return consumermap.put(VisitorEnum.NormalAnnotation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addNullLiteral(BiConsumer<NullLiteral, E> bc) {
		return consumermap.put(VisitorEnum.NullLiteral, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addNumberLiteral(BiConsumer<NumberLiteral, E> bc) {
		return consumermap.put(VisitorEnum.NumberLiteral, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addOpensDirective(BiConsumer<OpensDirective, E> bc) {
		return consumermap.put(VisitorEnum.OpensDirective, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addPackageDeclaration(BiConsumer<PackageDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.PackageDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addParameterizedType(BiConsumer<ParameterizedType, E> bc) {
		return consumermap.put(VisitorEnum.ParameterizedType, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addParenthesizedExpression(BiConsumer<ParenthesizedExpression, E> bc) {
		return consumermap.put(VisitorEnum.ParenthesizedExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addPatternInstanceofExpression(
			BiConsumer<PatternInstanceofExpression, E> bc) {
		return consumermap.put(VisitorEnum.PatternInstanceofExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addPostfixExpression(BiConsumer<PostfixExpression, E> bc) {
		return consumermap.put(VisitorEnum.PostfixExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addPrefixExpression(BiConsumer<PrefixExpression, E> bc) {
		return consumermap.put(VisitorEnum.PrefixExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addProvidesDirective(BiConsumer<ProvidesDirective, E> bc) {
		return consumermap.put(VisitorEnum.ProvidesDirective, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addPrimitiveType(BiConsumer<PrimitiveType, E> bc) {
		return consumermap.put(VisitorEnum.PrimitiveType, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addQualifiedName(BiConsumer<QualifiedName, E> bc) {
		return consumermap.put(VisitorEnum.QualifiedName, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addQualifiedType(BiConsumer<QualifiedType, E> bc) {
		return consumermap.put(VisitorEnum.QualifiedType, bc);
	}

//	public BiConsumer<? extends ASTNode, E> addModuleQualifiedName(BiConsumer<ModuleQualifiedName, E> bc) {
//		return consumermap.put(VisitorEnum.ModuleQualifiedName, bc);
//	}

	/**
	 *
	 * @param bc
	 * @return
	 */

	public BiConsumer<? extends ASTNode, E> addRequiresDirective(BiConsumer<RequiresDirective, E> bc) {
		return consumermap.put(VisitorEnum.RequiresDirective, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addRecordDeclaration(BiConsumer<RecordDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.RecordDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addReturnStatement(BiConsumer<ReturnStatement, E> bc) {
		return consumermap.put(VisitorEnum.ReturnStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSimpleName(BiConsumer<SimpleName, E> bc) {
		return consumermap.put(VisitorEnum.SimpleName, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSimpleType(BiConsumer<SimpleType, E> bc) {
		return consumermap.put(VisitorEnum.SimpleType, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSingleMemberAnnotation(BiConsumer<SingleMemberAnnotation, E> bc) {
		return consumermap.put(VisitorEnum.SingleMemberAnnotation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSingleVariableDeclaration(BiConsumer<SingleVariableDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.SingleVariableDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addStringLiteral(BiConsumer<StringLiteral, E> bc) {
		return consumermap.put(VisitorEnum.StringLiteral, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSuperConstructorInvocation(
			BiConsumer<SuperConstructorInvocation, E> bc) {
		return consumermap.put(VisitorEnum.SuperConstructorInvocation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSuperFieldAccess(BiConsumer<SuperFieldAccess, E> bc) {
		return consumermap.put(VisitorEnum.SuperFieldAccess, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSuperMethodInvocation(BiConsumer<SuperMethodInvocation, E> bc) {
		return consumermap.put(VisitorEnum.SuperMethodInvocation, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSuperMethodReference(BiConsumer<SuperMethodReference, E> bc) {
		return consumermap.put(VisitorEnum.SuperMethodReference, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSwitchCase(BiConsumer<SwitchCase, E> bc) {
		return consumermap.put(VisitorEnum.SwitchCase, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSwitchExpression(BiConsumer<SwitchExpression, E> bc) {
		return consumermap.put(VisitorEnum.SwitchExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSwitchStatement(BiConsumer<SwitchStatement, E> bc) {
		return consumermap.put(VisitorEnum.SwitchStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addSynchronizedStatement(BiConsumer<SynchronizedStatement, E> bc) {
		return consumermap.put(VisitorEnum.SynchronizedStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTagElement(BiConsumer<TagElement, E> bc) {
		return consumermap.put(VisitorEnum.TagElement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTextBlock(BiConsumer<TextBlock, E> bc) {
		return consumermap.put(VisitorEnum.TextBlock, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTextElement(BiConsumer<TextElement, E> bc) {
		return consumermap.put(VisitorEnum.TextElement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addThisExpression(BiConsumer<ThisExpression, E> bc) {
		return consumermap.put(VisitorEnum.ThisExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addThrowStatement(BiConsumer<ThrowStatement, E> bc) {
		return consumermap.put(VisitorEnum.ThrowStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTryStatement(BiConsumer<TryStatement, E> bc) {
		return consumermap.put(VisitorEnum.TryStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTypeDeclaration(BiConsumer<TypeDeclaration, E> bc) {
		return consumermap.put(VisitorEnum.TypeDeclaration, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTypeDeclarationStatement(BiConsumer<TypeDeclarationStatement, E> bc) {
		return consumermap.put(VisitorEnum.TypeDeclarationStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTypeLiteral(BiConsumer<TypeLiteral, E> bc) {
		return consumermap.put(VisitorEnum.TypeLiteral, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTypeMethodReference(BiConsumer<TypeMethodReference, E> bc) {
		return consumermap.put(VisitorEnum.TypeMethodReference, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addTypeParameter(BiConsumer<TypeParameter, E> bc) {
		return consumermap.put(VisitorEnum.TypeParameter, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addUnionType(BiConsumer<UnionType, E> bc) {
		return consumermap.put(VisitorEnum.UnionType, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addUsesDirective(BiConsumer<UsesDirective, E> bc) {
		return consumermap.put(VisitorEnum.UsesDirective, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addVariableDeclarationExpression(
			BiConsumer<VariableDeclarationExpression, E> bc) {
		return consumermap.put(VisitorEnum.VariableDeclarationExpression, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addVariableDeclarationStatement(
			BiConsumer<VariableDeclarationStatement, E> bc) {
		return consumermap.put(VisitorEnum.VariableDeclarationStatement, bc);
	}

	/**
	 *
	 * @param class1
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addVariableDeclarationStatement(Class<?> class1,
			BiConsumer<VariableDeclarationStatement, E> bc) {
		this.consumerdata.put(VisitorEnum.VariableDeclarationStatement, class1);
		return consumermap.put(VisitorEnum.VariableDeclarationStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addVariableDeclarationFragment(
			BiConsumer<VariableDeclarationFragment, E> bc) {
		return consumermap.put(VisitorEnum.VariableDeclarationFragment, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addWhileStatement(BiConsumer<WhileStatement, E> bc) {
		return consumermap.put(VisitorEnum.WhileStatement, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addWildcardType(BiConsumer<WildcardType, E> bc) {
		return consumermap.put(VisitorEnum.WildcardType, bc);
	}

	/**
	 *
	 * @param bc
	 * @return
	 */
	public BiConsumer<? extends ASTNode, E> addYieldStatement(BiConsumer<YieldStatement, E> bc) {
		return consumermap.put(VisitorEnum.YieldStatement, bc);
	}



	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addAnnotationTypeDeclaration(BiPredicate<AnnotationTypeDeclaration, E> bs,
			BiConsumer<AnnotationTypeDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.AnnotationTypeDeclaration, bs);
		consumermap.put(VisitorEnum.AnnotationTypeDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addAnnotationTypeMemberDeclaration(BiPredicate<AnnotationTypeMemberDeclaration, E> bs,
			BiConsumer<AnnotationTypeMemberDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.AnnotationTypeMemberDeclaration, bs);
		consumermap.put(VisitorEnum.AnnotationTypeMemberDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addAnonymousClassDeclaration(BiPredicate<AnonymousClassDeclaration, E> bs,
			BiConsumer<AnonymousClassDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.AnonymousClassDeclaration, bs);
		consumermap.put(VisitorEnum.AnonymousClassDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addArrayAccess(BiPredicate<ArrayAccess, E> bs, BiConsumer<ArrayAccess, E> bc) {
		predicatemap.put(VisitorEnum.ArrayAccess, bs);
		consumermap.put(VisitorEnum.ArrayAccess, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addArrayCreation(BiPredicate<ArrayCreation, E> bs, BiConsumer<ArrayCreation, E> bc) {
		predicatemap.put(VisitorEnum.ArrayCreation, bs);
		consumermap.put(VisitorEnum.ArrayCreation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addArrayInitializer(BiPredicate<ArrayInitializer, E> bs, BiConsumer<ArrayInitializer, E> bc) {
		predicatemap.put(VisitorEnum.ArrayInitializer, bs);
		consumermap.put(VisitorEnum.ArrayInitializer, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addArrayType(BiPredicate<ArrayType, E> bs, BiConsumer<ArrayType, E> bc) {
		predicatemap.put(VisitorEnum.ArrayType, bs);
		consumermap.put(VisitorEnum.ArrayType, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addAssertStatement(BiPredicate<AssertStatement, E> bs, BiConsumer<AssertStatement, E> bc) {
		predicatemap.put(VisitorEnum.AssertStatement, bs);
		consumermap.put(VisitorEnum.AssertStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addAssignment(BiPredicate<Assignment, E> bs, BiConsumer<Assignment, E> bc) {
		predicatemap.put(VisitorEnum.Assignment, bs);
		consumermap.put(VisitorEnum.Assignment, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addBlock(BiPredicate<Block, E> bs, BiConsumer<Block, E> bc) {
		predicatemap.put(VisitorEnum.Block, bs);
		consumermap.put(VisitorEnum.Block, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addBlockComment(BiPredicate<BlockComment, E> bs, BiConsumer<BlockComment, E> bc) {
		predicatemap.put(VisitorEnum.BlockComment, bs);
		consumermap.put(VisitorEnum.BlockComment, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addBooleanLiteral(BiPredicate<BooleanLiteral, E> bs, BiConsumer<BooleanLiteral, E> bc) {
		predicatemap.put(VisitorEnum.BooleanLiteral, bs);
		consumermap.put(VisitorEnum.BooleanLiteral, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addBreakStatement(BiPredicate<BreakStatement, E> bs, BiConsumer<BreakStatement, E> bc) {
		predicatemap.put(VisitorEnum.BreakStatement, bs);
		consumermap.put(VisitorEnum.BreakStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addCastExpression(BiPredicate<CastExpression, E> bs, BiConsumer<CastExpression, E> bc) {
		predicatemap.put(VisitorEnum.CastExpression, bs);
		consumermap.put(VisitorEnum.CastExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addCatchClause(BiPredicate<CatchClause, E> bs, BiConsumer<CatchClause, E> bc) {
		predicatemap.put(VisitorEnum.CatchClause, bs);
		consumermap.put(VisitorEnum.CatchClause, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addCharacterLiteral(BiPredicate<CharacterLiteral, E> bs, BiConsumer<CharacterLiteral, E> bc) {
		predicatemap.put(VisitorEnum.CharacterLiteral, bs);
		consumermap.put(VisitorEnum.CharacterLiteral, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addClassInstanceCreation(BiPredicate<ClassInstanceCreation, E> bs,
			BiConsumer<ClassInstanceCreation, E> bc) {
		predicatemap.put(VisitorEnum.ClassInstanceCreation, bs);
		consumermap.put(VisitorEnum.ClassInstanceCreation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addCompilationUnit(BiPredicate<CompilationUnit, E> bs, BiConsumer<CompilationUnit, E> bc) {
		predicatemap.put(VisitorEnum.CompilationUnit, bs);
		consumermap.put(VisitorEnum.CompilationUnit, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addConditionalExpression(BiPredicate<ConditionalExpression, E> bs,
			BiConsumer<ConditionalExpression, E> bc) {
		predicatemap.put(VisitorEnum.ConditionalExpression, bs);
		consumermap.put(VisitorEnum.ConditionalExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addConstructorInvocation(BiPredicate<ConstructorInvocation, E> bs,
			BiConsumer<ConstructorInvocation, E> bc) {
		predicatemap.put(VisitorEnum.ConstructorInvocation, bs);
		consumermap.put(VisitorEnum.ConstructorInvocation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addContinueStatement(BiPredicate<ContinueStatement, E> bs, BiConsumer<ContinueStatement, E> bc) {
		predicatemap.put(VisitorEnum.ContinueStatement, bs);
		consumermap.put(VisitorEnum.ContinueStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addCreationReference(BiPredicate<CreationReference, E> bs, BiConsumer<CreationReference, E> bc) {
		predicatemap.put(VisitorEnum.CreationReference, bs);
		consumermap.put(VisitorEnum.CreationReference, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addDimension(BiPredicate<Dimension, E> bs, BiConsumer<Dimension, E> bc) {
		predicatemap.put(VisitorEnum.Dimension, bs);
		consumermap.put(VisitorEnum.Dimension, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addDoStatement(BiPredicate<DoStatement, E> bs, BiConsumer<DoStatement, E> bc) {
		predicatemap.put(VisitorEnum.DoStatement, bs);
		consumermap.put(VisitorEnum.DoStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addEmptyStatement(BiPredicate<EmptyStatement, E> bs, BiConsumer<EmptyStatement, E> bc) {
		predicatemap.put(VisitorEnum.EmptyStatement, bs);
		consumermap.put(VisitorEnum.EmptyStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addEnhancedForStatement(BiPredicate<EnhancedForStatement, E> bs,
			BiConsumer<EnhancedForStatement, E> bc) {
		predicatemap.put(VisitorEnum.EnhancedForStatement, bs);
		consumermap.put(VisitorEnum.EnhancedForStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addEnumConstantDeclaration(BiPredicate<EnumConstantDeclaration, E> bs,
			BiConsumer<EnumConstantDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.EnumConstantDeclaration, bs);
		consumermap.put(VisitorEnum.EnumConstantDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addEnumDeclaration(BiPredicate<EnumDeclaration, E> bs, BiConsumer<EnumDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.EnumDeclaration, bs);
		consumermap.put(VisitorEnum.EnumDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addExportsDirective(BiPredicate<ExportsDirective, E> bs, BiConsumer<ExportsDirective, E> bc) {
		predicatemap.put(VisitorEnum.ExportsDirective, bs);
		consumermap.put(VisitorEnum.ExportsDirective, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addExpressionMethodReference(BiPredicate<ExpressionMethodReference, E> bs,
			BiConsumer<ExpressionMethodReference, E> bc) {
		predicatemap.put(VisitorEnum.ExpressionMethodReference, bs);
		consumermap.put(VisitorEnum.ExpressionMethodReference, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addExpressionStatement(BiPredicate<ExpressionStatement, E> bs, BiConsumer<ExpressionStatement, E> bc) {
		predicatemap.put(VisitorEnum.ExpressionStatement, bs);
		consumermap.put(VisitorEnum.ExpressionStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addFieldAccess(BiPredicate<FieldAccess, E> bs, BiConsumer<FieldAccess, E> bc) {
		predicatemap.put(VisitorEnum.FieldAccess, bs);
		consumermap.put(VisitorEnum.FieldAccess, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addFieldDeclaration(BiPredicate<FieldDeclaration, E> bs, BiConsumer<FieldDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.FieldDeclaration, bs);
		consumermap.put(VisitorEnum.FieldDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addForStatement(BiPredicate<ForStatement, E> bs, BiConsumer<ForStatement, E> bc) {
		predicatemap.put(VisitorEnum.ForStatement, bs);
		consumermap.put(VisitorEnum.ForStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addIfStatement(BiPredicate<IfStatement, E> bs, BiConsumer<IfStatement, E> bc) {
		predicatemap.put(VisitorEnum.IfStatement, bs);
		consumermap.put(VisitorEnum.IfStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addImportDeclaration(BiPredicate<ImportDeclaration, E> bs, BiConsumer<ImportDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.ImportDeclaration, bs);
		consumermap.put(VisitorEnum.ImportDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addInfixExpression(BiPredicate<InfixExpression, E> bs, BiConsumer<InfixExpression, E> bc) {
		predicatemap.put(VisitorEnum.InfixExpression, bs);
		consumermap.put(VisitorEnum.InfixExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addInitializer(BiPredicate<Initializer, E> bs, BiConsumer<Initializer, E> bc) {
		predicatemap.put(VisitorEnum.Initializer, bs);
		consumermap.put(VisitorEnum.Initializer, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addInstanceofExpression(BiPredicate<InstanceofExpression, E> bs,
			BiConsumer<InstanceofExpression, E> bc) {
		predicatemap.put(VisitorEnum.InstanceofExpression, bs);
		consumermap.put(VisitorEnum.InstanceofExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addIntersectionType(BiPredicate<IntersectionType, E> bs, BiConsumer<IntersectionType, E> bc) {
		predicatemap.put(VisitorEnum.IntersectionType, bs);
		consumermap.put(VisitorEnum.IntersectionType, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addJavadoc(BiPredicate<Javadoc, E> bs, BiConsumer<Javadoc, E> bc) {
		predicatemap.put(VisitorEnum.Javadoc, bs);
		consumermap.put(VisitorEnum.Javadoc, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addLabeledStatement(BiPredicate<LabeledStatement, E> bs, BiConsumer<LabeledStatement, E> bc) {
		predicatemap.put(VisitorEnum.LabeledStatement, bs);
		consumermap.put(VisitorEnum.LabeledStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addLambdaExpression(BiPredicate<LambdaExpression, E> bs, BiConsumer<LambdaExpression, E> bc) {
		predicatemap.put(VisitorEnum.LambdaExpression, bs);
		consumermap.put(VisitorEnum.LambdaExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addLineComment(BiPredicate<LineComment, E> bs, BiConsumer<LineComment, E> bc) {
		predicatemap.put(VisitorEnum.LineComment, bs);
		consumermap.put(VisitorEnum.LineComment, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addMarkerAnnotation(BiPredicate<MarkerAnnotation, E> bs, BiConsumer<MarkerAnnotation, E> bc) {
		predicatemap.put(VisitorEnum.MarkerAnnotation, bs);
		consumermap.put(VisitorEnum.MarkerAnnotation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addMemberRef(BiPredicate<MemberRef, E> bs, BiConsumer<MemberRef, E> bc) {
		predicatemap.put(VisitorEnum.MemberRef, bs);
		consumermap.put(VisitorEnum.MemberRef, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addMemberValuePair(BiPredicate<MemberValuePair, E> bs, BiConsumer<MemberValuePair, E> bc) {
		predicatemap.put(VisitorEnum.MemberValuePair, bs);
		consumermap.put(VisitorEnum.MemberValuePair, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addMethodRef(BiPredicate<MethodRef, E> bs, BiConsumer<MethodRef, E> bc) {
		predicatemap.put(VisitorEnum.MethodRef, bs);
		consumermap.put(VisitorEnum.MethodRef, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addMethodRefParameter(BiPredicate<MethodRefParameter, E> bs, BiConsumer<MethodRefParameter, E> bc) {
		predicatemap.put(VisitorEnum.MethodRefParameter, bs);
		consumermap.put(VisitorEnum.MethodRefParameter, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addMethodDeclaration(BiPredicate<MethodDeclaration, E> bs, BiConsumer<MethodDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.MethodDeclaration, bs);
		consumermap.put(VisitorEnum.MethodDeclaration, bc);
	}

	/**
	 *
	 * @param methodname
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addMethodInvocation(String methodname, BiPredicate<MethodInvocation, E> bs,
			BiConsumer<MethodInvocation, E> bc) {
		this.predicatedata.put(VisitorEnum.MethodInvocation, methodname);
		predicatemap.put(VisitorEnum.MethodInvocation, bs);
		consumermap.put(VisitorEnum.MethodInvocation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addMethodInvocation(BiPredicate<MethodInvocation, E> bs, BiConsumer<MethodInvocation, E> bc) {
		predicatemap.put(VisitorEnum.MethodInvocation, bs);
		consumermap.put(VisitorEnum.MethodInvocation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addModifier(BiPredicate<Modifier, E> bs, BiConsumer<Modifier, E> bc) {
		predicatemap.put(VisitorEnum.Modifier, bs);
		consumermap.put(VisitorEnum.Modifier, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addModuleDeclaration(BiPredicate<ModuleDeclaration, E> bs, BiConsumer<ModuleDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.ModuleDeclaration, bs);
		consumermap.put(VisitorEnum.ModuleDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addModuleModifier(BiPredicate<ModuleModifier, E> bs, BiConsumer<ModuleModifier, E> bc) {
		predicatemap.put(VisitorEnum.ModuleModifier, bs);
		consumermap.put(VisitorEnum.ModuleModifier, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addNameQualifiedType(BiPredicate<NameQualifiedType, E> bs, BiConsumer<NameQualifiedType, E> bc) {
		predicatemap.put(VisitorEnum.NameQualifiedType, bs);
		consumermap.put(VisitorEnum.NameQualifiedType, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addNormalAnnotation(BiPredicate<NormalAnnotation, E> bs, BiConsumer<NormalAnnotation, E> bc) {
		predicatemap.put(VisitorEnum.NormalAnnotation, bs);
		consumermap.put(VisitorEnum.NormalAnnotation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addNullLiteral(BiPredicate<NullLiteral, E> bs, BiConsumer<NullLiteral, E> bc) {
		predicatemap.put(VisitorEnum.NullLiteral, bs);
		consumermap.put(VisitorEnum.NullLiteral, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addNumberLiteral(BiPredicate<NumberLiteral, E> bs, BiConsumer<NumberLiteral, E> bc) {
		predicatemap.put(VisitorEnum.NumberLiteral, bs);
		consumermap.put(VisitorEnum.NumberLiteral, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addOpensDirective(BiPredicate<OpensDirective, E> bs, BiConsumer<OpensDirective, E> bc) {
		predicatemap.put(VisitorEnum.OpensDirective, bs);
		consumermap.put(VisitorEnum.OpensDirective, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addPackageDeclaration(BiPredicate<PackageDeclaration, E> bs, BiConsumer<PackageDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.PackageDeclaration, bs);
		consumermap.put(VisitorEnum.PackageDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addParameterizedType(BiPredicate<ParameterizedType, E> bs, BiConsumer<ParameterizedType, E> bc) {
		predicatemap.put(VisitorEnum.ParameterizedType, bs);
		consumermap.put(VisitorEnum.ParameterizedType, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addParenthesizedExpression(BiPredicate<ParenthesizedExpression, E> bs,
			BiConsumer<ParenthesizedExpression, E> bc) {
		predicatemap.put(VisitorEnum.ParenthesizedExpression, bs);
		consumermap.put(VisitorEnum.ParenthesizedExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addPatternInstanceofExpression(BiPredicate<PatternInstanceofExpression, E> bs,
			BiConsumer<PatternInstanceofExpression, E> bc) {
		predicatemap.put(VisitorEnum.PatternInstanceofExpression, bs);
		consumermap.put(VisitorEnum.PatternInstanceofExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addPostfixExpression(BiPredicate<PostfixExpression, E> bs, BiConsumer<PostfixExpression, E> bc) {
		predicatemap.put(VisitorEnum.PostfixExpression, bs);
		consumermap.put(VisitorEnum.PostfixExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addPrefixExpression(BiPredicate<PrefixExpression, E> bs, BiConsumer<PrefixExpression, E> bc) {
		predicatemap.put(VisitorEnum.PrefixExpression, bs);
		consumermap.put(VisitorEnum.PrefixExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addProvidesDirective(BiPredicate<ProvidesDirective, E> bs, BiConsumer<ProvidesDirective, E> bc) {
		predicatemap.put(VisitorEnum.ProvidesDirective, bs);
		consumermap.put(VisitorEnum.ProvidesDirective, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addPrimitiveType(BiPredicate<PrimitiveType, E> bs, BiConsumer<PrimitiveType, E> bc) {
		predicatemap.put(VisitorEnum.PrimitiveType, bs);
		consumermap.put(VisitorEnum.PrimitiveType, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addQualifiedName(BiPredicate<QualifiedName, E> bs, BiConsumer<QualifiedName, E> bc) {
		predicatemap.put(VisitorEnum.QualifiedName, bs);
		consumermap.put(VisitorEnum.QualifiedName, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addQualifiedType(BiPredicate<QualifiedType, E> bs, BiConsumer<QualifiedType, E> bc) {
		predicatemap.put(VisitorEnum.QualifiedType, bs);
		consumermap.put(VisitorEnum.QualifiedType, bc);
	}

//	public void addModuleQualifiedName(BiPredicate<ModuleQualifiedName, E> bs,
//			BiConsumer<ModuleQualifiedName, E> bc) {
//		predicatemap.put(VisitorEnum.ModuleQualifiedName, bs);
//		consumermap.put(VisitorEnum.ModuleQualifiedName, bc);
//	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */

	public void addRequiresDirective(BiPredicate<RequiresDirective, E> bs, BiConsumer<RequiresDirective, E> bc) {
		predicatemap.put(VisitorEnum.RequiresDirective, bs);
		consumermap.put(VisitorEnum.RequiresDirective, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addRecordDeclaration(BiPredicate<RecordDeclaration, E> bs, BiConsumer<RecordDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.RecordDeclaration, bs);
		consumermap.put(VisitorEnum.RecordDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addReturnStatement(BiPredicate<ReturnStatement, E> bs, BiConsumer<ReturnStatement, E> bc) {
		predicatemap.put(VisitorEnum.ReturnStatement, bs);
		consumermap.put(VisitorEnum.ReturnStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSimpleName(BiPredicate<SimpleName, E> bs, BiConsumer<SimpleName, E> bc) {
		predicatemap.put(VisitorEnum.SimpleName, bs);
		consumermap.put(VisitorEnum.SimpleName, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSimpleType(BiPredicate<SimpleType, E> bs, BiConsumer<SimpleType, E> bc) {
		predicatemap.put(VisitorEnum.SimpleType, bs);
		consumermap.put(VisitorEnum.SimpleType, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSingleMemberAnnotation(BiPredicate<SingleMemberAnnotation, E> bs,
			BiConsumer<SingleMemberAnnotation, E> bc) {
		predicatemap.put(VisitorEnum.SingleMemberAnnotation, bs);
		consumermap.put(VisitorEnum.SingleMemberAnnotation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSingleVariableDeclaration(BiPredicate<SingleVariableDeclaration, E> bs,
			BiConsumer<SingleVariableDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.SingleVariableDeclaration, bs);
		consumermap.put(VisitorEnum.SingleVariableDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addStringLiteral(BiPredicate<StringLiteral, E> bs, BiConsumer<StringLiteral, E> bc) {
		predicatemap.put(VisitorEnum.StringLiteral, bs);
		consumermap.put(VisitorEnum.StringLiteral, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSuperConstructorInvocation(BiPredicate<SuperConstructorInvocation, E> bs,
			BiConsumer<SuperConstructorInvocation, E> bc) {
		predicatemap.put(VisitorEnum.SuperConstructorInvocation, bs);
		consumermap.put(VisitorEnum.SuperConstructorInvocation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSuperFieldAccess(BiPredicate<SuperFieldAccess, E> bs, BiConsumer<SuperFieldAccess, E> bc) {
		predicatemap.put(VisitorEnum.SuperFieldAccess, bs);
		consumermap.put(VisitorEnum.SuperFieldAccess, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSuperMethodInvocation(BiPredicate<SuperMethodInvocation, E> bs,
			BiConsumer<SuperMethodInvocation, E> bc) {
		predicatemap.put(VisitorEnum.SuperMethodInvocation, bs);
		consumermap.put(VisitorEnum.SuperMethodInvocation, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSuperMethodReference(BiPredicate<SuperMethodReference, E> bs,
			BiConsumer<SuperMethodReference, E> bc) {
		predicatemap.put(VisitorEnum.SuperMethodReference, bs);
		consumermap.put(VisitorEnum.SuperMethodReference, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSwitchCase(BiPredicate<SwitchCase, E> bs, BiConsumer<SwitchCase, E> bc) {
		predicatemap.put(VisitorEnum.SwitchCase, bs);
		consumermap.put(VisitorEnum.SwitchCase, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSwitchExpression(BiPredicate<SwitchExpression, E> bs, BiConsumer<SwitchExpression, E> bc) {
		predicatemap.put(VisitorEnum.SwitchExpression, bs);
		consumermap.put(VisitorEnum.SwitchExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSwitchStatement(BiPredicate<SwitchStatement, E> bs, BiConsumer<SwitchStatement, E> bc) {
		predicatemap.put(VisitorEnum.SwitchStatement, bs);
		consumermap.put(VisitorEnum.SwitchStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addSynchronizedStatement(BiPredicate<SynchronizedStatement, E> bs,
			BiConsumer<SynchronizedStatement, E> bc) {
		predicatemap.put(VisitorEnum.SynchronizedStatement, bs);
		consumermap.put(VisitorEnum.SynchronizedStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTagElement(BiPredicate<TagElement, E> bs, BiConsumer<TagElement, E> bc) {
		predicatemap.put(VisitorEnum.TagElement, bs);
		consumermap.put(VisitorEnum.TagElement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTextBlock(BiPredicate<TextBlock, E> bs, BiConsumer<TextBlock, E> bc) {
		predicatemap.put(VisitorEnum.TextBlock, bs);
		consumermap.put(VisitorEnum.TextBlock, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTextElement(BiPredicate<TextElement, E> bs, BiConsumer<TextElement, E> bc) {
		predicatemap.put(VisitorEnum.TextElement, bs);
		consumermap.put(VisitorEnum.TextElement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addThisExpression(BiPredicate<ThisExpression, E> bs, BiConsumer<ThisExpression, E> bc) {
		predicatemap.put(VisitorEnum.ThisExpression, bs);
		consumermap.put(VisitorEnum.ThisExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addThrowStatement(BiPredicate<ThrowStatement, E> bs, BiConsumer<ThrowStatement, E> bc) {
		predicatemap.put(VisitorEnum.ThrowStatement, bs);
		consumermap.put(VisitorEnum.ThrowStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTryStatement(BiPredicate<TryStatement, E> bs, BiConsumer<TryStatement, E> bc) {
		predicatemap.put(VisitorEnum.TryStatement, bs);
		consumermap.put(VisitorEnum.TryStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTypeDeclaration(BiPredicate<TypeDeclaration, E> bs, BiConsumer<TypeDeclaration, E> bc) {
		predicatemap.put(VisitorEnum.TypeDeclaration, bs);
		consumermap.put(VisitorEnum.TypeDeclaration, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTypeDeclarationStatement(BiPredicate<TypeDeclarationStatement, E> bs,
			BiConsumer<TypeDeclarationStatement, E> bc) {
		predicatemap.put(VisitorEnum.TypeDeclarationStatement, bs);
		consumermap.put(VisitorEnum.TypeDeclarationStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTypeLiteral(BiPredicate<TypeLiteral, E> bs, BiConsumer<TypeLiteral, E> bc) {
		predicatemap.put(VisitorEnum.TypeLiteral, bs);
		consumermap.put(VisitorEnum.TypeLiteral, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTypeMethodReference(BiPredicate<TypeMethodReference, E> bs, BiConsumer<TypeMethodReference, E> bc) {
		predicatemap.put(VisitorEnum.TypeMethodReference, bs);
		consumermap.put(VisitorEnum.TypeMethodReference, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addTypeParameter(BiPredicate<TypeParameter, E> bs, BiConsumer<TypeParameter, E> bc) {
		predicatemap.put(VisitorEnum.TypeParameter, bs);
		consumermap.put(VisitorEnum.TypeParameter, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addUnionType(BiPredicate<UnionType, E> bs, BiConsumer<UnionType, E> bc) {
		predicatemap.put(VisitorEnum.UnionType, bs);
		consumermap.put(VisitorEnum.UnionType, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addUsesDirective(BiPredicate<UsesDirective, E> bs, BiConsumer<UsesDirective, E> bc) {
		predicatemap.put(VisitorEnum.UsesDirective, bs);
		consumermap.put(VisitorEnum.UsesDirective, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addVariableDeclarationExpression(BiPredicate<VariableDeclarationExpression, E> bs,
			BiConsumer<VariableDeclarationExpression, E> bc) {
		predicatemap.put(VisitorEnum.VariableDeclarationExpression, bs);
		consumermap.put(VisitorEnum.VariableDeclarationExpression, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addVariableDeclarationStatement(BiPredicate<VariableDeclarationStatement, E> bs,
			BiConsumer<VariableDeclarationStatement, E> bc) {
		predicatemap.put(VisitorEnum.VariableDeclarationStatement, bs);
		consumermap.put(VisitorEnum.VariableDeclarationStatement, bc);
	}

	/**
	 *
	 * @param class1
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addVariableDeclarationStatement(Class<?> class1, BiPredicate<VariableDeclarationStatement, E> bs,
			BiConsumer<VariableDeclarationStatement, E> bc) {
		predicatedata.put(VisitorEnum.VariableDeclarationStatement, class1);
		consumerdata.put(VisitorEnum.VariableDeclarationStatement, class1);
		predicatemap.put(VisitorEnum.VariableDeclarationStatement, bs);
		consumermap.put(VisitorEnum.VariableDeclarationStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addVariableDeclarationFragment(BiPredicate<VariableDeclarationFragment, E> bs,
			BiConsumer<VariableDeclarationFragment, E> bc) {
		predicatemap.put(VisitorEnum.VariableDeclarationFragment, bs);
		consumermap.put(VisitorEnum.VariableDeclarationFragment, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addWhileStatement(BiPredicate<WhileStatement, E> bs, BiConsumer<WhileStatement, E> bc) {
		predicatemap.put(VisitorEnum.WhileStatement, bs);
		consumermap.put(VisitorEnum.WhileStatement, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addWildcardType(BiPredicate<WildcardType, E> bs, BiConsumer<WildcardType, E> bc) {
		predicatemap.put(VisitorEnum.WildcardType, bs);
		consumermap.put(VisitorEnum.WildcardType, bc);
	}

	/**
	 *
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public void addYieldStatement(BiPredicate<YieldStatement, E> bs, BiConsumer<YieldStatement, E> bc) {
		predicatemap.put(VisitorEnum.YieldStatement, bs);
		consumermap.put(VisitorEnum.YieldStatement, bc);
	}



	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param cu
	 * @param myset
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callVisitor(ASTNode cu, EnumSet<VisitorEnum> myset, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ASTNode, ReferenceHolder<V, T>> bs, BiConsumer<ASTNode, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V,T>, V, T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		myset.forEach(ve -> {
			hv.add(ve, bs, bc);
		});
		hv.build(cu);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param cu
	 * @param myset
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callVisitor(ASTNode cu, EnumSet<VisitorEnum> myset, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ASTNode, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		myset.forEach(ve -> {
			hv.add(ve, bs);
		});
		hv.build(cu);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param cu
	 * @param myset
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callVisitor(ASTNode cu, EnumSet<VisitorEnum> myset, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ASTNode, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		myset.forEach(ve -> {
			hv.addEnd(ve, bc);
		});
		hv.build(cu);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callAnnotationTypeDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<AnnotationTypeDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnnotationTypeDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callAnnotationTypeMemberDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<AnnotationTypeMemberDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnnotationTypeMemberDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callAnonymousClassDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<AnonymousClassDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnonymousClassDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callArrayAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ArrayAccess, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayAccess(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callArrayCreationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ArrayCreation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayCreation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callArrayInitializerVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ArrayInitializer, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayInitializer(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callArrayTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ArrayType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayType(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callAssertStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<AssertStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAssertStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callAssignmentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Assignment, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAssignment(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callBlockVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Block, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBlock(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callBlockCommentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<BlockComment, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBlockComment(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callBooleanLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<BooleanLiteral, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBooleanLiteral(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callBreakStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<BreakStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBreakStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callCastExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CastExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCastExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callCatchClauseVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CatchClause, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCatchClause(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callCharacterLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CharacterLiteral, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCharacterLiteral(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callClassInstanceCreationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ClassInstanceCreation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addClassInstanceCreation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callCompilationUnitVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CompilationUnit, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCompilationUnit(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callConditionalExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ConditionalExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addConditionalExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callConstructorInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ConstructorInvocation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addConstructorInvocation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callContinueStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ContinueStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addContinueStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callCreationReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CreationReference, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCreationReference(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callDimensionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Dimension, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addDimension(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callDoStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<DoStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addDoStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callEmptyStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<EmptyStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEmptyStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callEnhancedForStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<EnhancedForStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnhancedForStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callEnumConstantDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<EnumConstantDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnumConstantDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callEnumDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<EnumDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnumDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callExportsDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ExportsDirective, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExportsDirective(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callExpressionMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ExpressionMethodReference, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExpressionMethodReference(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callExpressionStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ExpressionStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExpressionStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callFieldAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<FieldAccess, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addFieldAccess(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callFieldDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<FieldDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addFieldDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callForStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ForStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addForStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callIfStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<IfStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addIfStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callImportDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ImportDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addImportDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callInfixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<InfixExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInfixExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callInitializerVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Initializer, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInitializer(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callInstanceofExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<InstanceofExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInstanceofExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callIntersectionTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<IntersectionType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addIntersectionType(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callJavadocVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Javadoc, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addJavadoc(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callLabeledStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<LabeledStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLabeledStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callLambdaExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<LambdaExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLambdaExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callLineCommentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<LineComment, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLineComment(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callMarkerAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MarkerAnnotation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMarkerAnnotation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callMemberRefVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MemberRef, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMemberRef(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callMemberValuePairVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MemberValuePair, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMemberValuePair(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callMethodRefVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodRef, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodRef(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callMethodRefParameterVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodRefParameter, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodRefParameter(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callMethodDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callMethodInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodInvocation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodInvocation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param methodname
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callMethodInvocationVisitor(String methodname, ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodInvocation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodInvocation(methodname, bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callModifierVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Modifier, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModifier(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callModuleDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ModuleDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModuleDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callModuleModifierVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ModuleModifier, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModuleModifier(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callNameQualifiedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<NameQualifiedType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNameQualifiedType(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callNormalAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<NormalAnnotation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNormalAnnotation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callNullLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<NullLiteral, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNullLiteral(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callNumberLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<NumberLiteral, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNumberLiteral(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callOpensDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<OpensDirective, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addOpensDirective(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callPackageDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PackageDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPackageDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callParameterizedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ParameterizedType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addParameterizedType(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callParenthesizedExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ParenthesizedExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addParenthesizedExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callPatternInstanceofExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PatternInstanceofExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPatternInstanceofExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callPostfixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PostfixExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPostfixExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callPrefixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PrefixExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPrefixExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callProvidesDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ProvidesDirective, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addProvidesDirective(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callPrimitiveTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PrimitiveType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPrimitiveType(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callQualifiedNameVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<QualifiedName, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addQualifiedName(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callQualifiedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<QualifiedType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addQualifiedType(bs);
		hv.build(node);
	}

//	public static <V,T> void callModuleQualifiedNameVisitor(ASTNode node, ReferenceHolder<V,T> dataholder, BiPredicate<ModuleQualifiedName, ReferenceHolder<V,T>> bs) {  HelperVisitor<ReferenceHolder<V,T>> hv = new HelperVisitor<>(nodesprocessed, dataholder); hv.addModuleQualifiedName(bs); hv.build(node);}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */

	public static <V, T> void callRequiresDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<RequiresDirective, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addRequiresDirective(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callRecordDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<RecordDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addRecordDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callReturnStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ReturnStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addReturnStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSimpleNameVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SimpleName, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSimpleName(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSimpleTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SimpleType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSimpleType(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSingleMemberAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SingleMemberAnnotation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSingleMemberAnnotation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSingleVariableDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SingleVariableDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSingleVariableDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callStringLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<StringLiteral, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addStringLiteral(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSuperConstructorInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SuperConstructorInvocation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperConstructorInvocation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSuperFieldAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SuperFieldAccess, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperFieldAccess(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSuperMethodInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SuperMethodInvocation, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperMethodInvocation(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSuperMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SuperMethodReference, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperMethodReference(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSwitchCaseVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SwitchCase, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchCase(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSwitchExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SwitchExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSwitchStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SwitchStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callSynchronizedStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SynchronizedStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSynchronizedStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTagElementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TagElement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTagElement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTextBlockVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TextBlock, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTextBlock(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTextElementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TextElement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTextElement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callThisExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ThisExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addThisExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callThrowStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ThrowStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addThrowStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTryStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TryStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTryStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTypeDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeDeclaration, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeDeclaration(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTypeDeclarationStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeDeclarationStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeDeclarationStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTypeLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeLiteral, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeLiteral(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTypeMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeMethodReference, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeMethodReference(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callTypeParameterVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeParameter, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeParameter(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callUnionTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<UnionType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addUnionType(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callUsesDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<UsesDirective, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addUsesDirective(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callVariableDeclarationExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<VariableDeclarationExpression, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationExpression(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callVariableDeclarationStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<VariableDeclarationStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param class1
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callVariableDeclarationStatementVisitor(Class<?> class1, ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<VariableDeclarationStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationStatement(class1, bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callVariableDeclarationFragmentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<VariableDeclarationFragment, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationFragment(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callWhileStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<WhileStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addWhileStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callWildcardTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<WildcardType, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addWildcardType(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 */
	public static <V, T> void callYieldStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<YieldStatement, ReferenceHolder<V, T>> bs) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addYieldStatement(bs);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callAnnotationTypeDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<AnnotationTypeDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnnotationTypeDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callAnnotationTypeMemberDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<AnnotationTypeMemberDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnnotationTypeMemberDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callAnonymousClassDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<AnonymousClassDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnonymousClassDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callArrayAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ArrayAccess, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayAccess(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callArrayCreationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ArrayCreation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayCreation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callArrayInitializerVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ArrayInitializer, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayInitializer(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callArrayTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ArrayType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayType(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callAssertStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<AssertStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAssertStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callAssignmentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<Assignment, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAssignment(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callBlockVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<Block, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBlock(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callBlockCommentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<BlockComment, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBlockComment(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callBooleanLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<BooleanLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBooleanLiteral(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callBreakStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<BreakStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBreakStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callCastExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<CastExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCastExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callCatchClauseVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<CatchClause, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCatchClause(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callCharacterLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<CharacterLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCharacterLiteral(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callClassInstanceCreationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ClassInstanceCreation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addClassInstanceCreation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callCompilationUnitVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<CompilationUnit, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCompilationUnit(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callConditionalExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ConditionalExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addConditionalExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callConstructorInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ConstructorInvocation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addConstructorInvocation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callContinueStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ContinueStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addContinueStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callCreationReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<CreationReference, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCreationReference(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callDimensionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<Dimension, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addDimension(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callDoStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<DoStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addDoStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callEmptyStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<EmptyStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEmptyStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callEnhancedForStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<EnhancedForStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnhancedForStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callEnumConstantDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<EnumConstantDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnumConstantDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callEnumDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<EnumDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnumDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callExportsDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ExportsDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExportsDirective(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callExpressionMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ExpressionMethodReference, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExpressionMethodReference(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callExpressionStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ExpressionStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExpressionStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callFieldAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<FieldAccess, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addFieldAccess(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callFieldDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<FieldDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addFieldDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callForStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ForStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addForStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callIfStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<IfStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addIfStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callImportDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ImportDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addImportDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callInfixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<InfixExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInfixExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callInitializerVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<Initializer, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInitializer(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callInstanceofExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<InstanceofExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInstanceofExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callIntersectionTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<IntersectionType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addIntersectionType(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callJavadocVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<Javadoc, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addJavadoc(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callLabeledStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<LabeledStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLabeledStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callLambdaExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<LambdaExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLambdaExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callLineCommentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<LineComment, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLineComment(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callMarkerAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<MarkerAnnotation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMarkerAnnotation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callMemberRefVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<MemberRef, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMemberRef(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callMemberValuePairVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<MemberValuePair, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMemberValuePair(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callMethodRefVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<MethodRef, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodRef(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callMethodRefParameterVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<MethodRefParameter, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodRefParameter(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callMethodDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<MethodDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callMethodInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<MethodInvocation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodInvocation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callModifierVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<Modifier, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModifier(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callModuleDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ModuleDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModuleDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callModuleModifierVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ModuleModifier, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModuleModifier(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callNameQualifiedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<NameQualifiedType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNameQualifiedType(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callNormalAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<NormalAnnotation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNormalAnnotation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callNullLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<NullLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNullLiteral(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callNumberLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<NumberLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNumberLiteral(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callOpensDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<OpensDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addOpensDirective(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callPackageDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<PackageDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPackageDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callParameterizedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ParameterizedType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addParameterizedType(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callParenthesizedExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ParenthesizedExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addParenthesizedExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callPatternInstanceofExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<PatternInstanceofExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPatternInstanceofExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callPostfixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<PostfixExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPostfixExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callPrefixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<PrefixExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPrefixExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callProvidesDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ProvidesDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addProvidesDirective(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callPrimitiveTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<PrimitiveType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPrimitiveType(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callQualifiedNameVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<QualifiedName, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addQualifiedName(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callQualifiedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<QualifiedType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addQualifiedType(bc);
		hv.build(node);
	}

//	public static <V,T> void callModuleQualifiedNameVisitor(ASTNode node, ReferenceHolder<V,T> dataholder, BiConsumer<ModuleQualifiedName, ReferenceHolder<V,T>> bc) {  HelperVisitor<ReferenceHolder<V,T>> hv = new HelperVisitor<>(nodesprocessed, dataholder); hv.addModuleQualifiedName(bc); hv.build(node);}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */

	public static <V, T> void callRequiresDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<RequiresDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addRequiresDirective(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callRecordDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<RecordDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addRecordDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callReturnStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ReturnStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addReturnStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSimpleNameVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SimpleName, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSimpleName(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSimpleTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SimpleType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSimpleType(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSingleMemberAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SingleMemberAnnotation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSingleMemberAnnotation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSingleVariableDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SingleVariableDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSingleVariableDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callStringLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<StringLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addStringLiteral(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSuperConstructorInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SuperConstructorInvocation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperConstructorInvocation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSuperFieldAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SuperFieldAccess, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperFieldAccess(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSuperMethodInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SuperMethodInvocation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperMethodInvocation(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSuperMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SuperMethodReference, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperMethodReference(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSwitchCaseVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SwitchCase, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchCase(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSwitchExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SwitchExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSwitchStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SwitchStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callSynchronizedStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<SynchronizedStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSynchronizedStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTagElementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TagElement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTagElement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTextBlockVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TextBlock, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTextBlock(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTextElementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TextElement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTextElement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callThisExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ThisExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addThisExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callThrowStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<ThrowStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addThrowStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTryStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TryStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTryStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTypeDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TypeDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeDeclaration(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTypeDeclarationStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TypeDeclarationStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeDeclarationStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTypeLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TypeLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeLiteral(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTypeMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TypeMethodReference, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeMethodReference(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callTypeParameterVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<TypeParameter, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeParameter(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callUnionTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<UnionType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addUnionType(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callUsesDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<UsesDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addUsesDirective(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callVariableDeclarationExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<VariableDeclarationExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationExpression(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callVariableDeclarationStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<VariableDeclarationStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param class1
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callVariableDeclarationStatementVisitor(Class<?> class1, ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<VariableDeclarationStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationStatement(class1, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callVariableDeclarationFragmentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<VariableDeclarationFragment, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationFragment(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callWhileStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<WhileStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addWhileStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callWildcardTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<WildcardType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addWildcardType(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bc
	 */
	public static <V, T> void callYieldStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiConsumer<YieldStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addYieldStatement(bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callAnnotationTypeDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<AnnotationTypeDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<AnnotationTypeDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnnotationTypeDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callAnnotationTypeMemberDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<AnnotationTypeMemberDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<AnnotationTypeMemberDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnnotationTypeMemberDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callAnonymousClassDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<AnonymousClassDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<AnonymousClassDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAnonymousClassDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callArrayAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ArrayAccess, ReferenceHolder<V, T>> bs, BiConsumer<ArrayAccess, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayAccess(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callArrayCreationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ArrayCreation, ReferenceHolder<V, T>> bs, BiConsumer<ArrayCreation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayCreation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callArrayInitializerVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ArrayInitializer, ReferenceHolder<V, T>> bs,
			BiConsumer<ArrayInitializer, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayInitializer(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callArrayTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ArrayType, ReferenceHolder<V, T>> bs, BiConsumer<ArrayType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addArrayType(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callAssertStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<AssertStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<AssertStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAssertStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callAssignmentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Assignment, ReferenceHolder<V, T>> bs, BiConsumer<Assignment, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addAssignment(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callBlockVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Block, ReferenceHolder<V, T>> bs, BiConsumer<Block, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBlock(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callBlockCommentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<BlockComment, ReferenceHolder<V, T>> bs, BiConsumer<BlockComment, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBlockComment(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callBooleanLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<BooleanLiteral, ReferenceHolder<V, T>> bs,
			BiConsumer<BooleanLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBooleanLiteral(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callBreakStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<BreakStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<BreakStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addBreakStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callCastExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CastExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<CastExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCastExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callCatchClauseVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CatchClause, ReferenceHolder<V, T>> bs, BiConsumer<CatchClause, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCatchClause(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callCharacterLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CharacterLiteral, ReferenceHolder<V, T>> bs,
			BiConsumer<CharacterLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCharacterLiteral(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callClassInstanceCreationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ClassInstanceCreation, ReferenceHolder<V, T>> bs,
			BiConsumer<ClassInstanceCreation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addClassInstanceCreation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callCompilationUnitVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CompilationUnit, ReferenceHolder<V, T>> bs,
			BiConsumer<CompilationUnit, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCompilationUnit(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callConditionalExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ConditionalExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<ConditionalExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addConditionalExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callConstructorInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ConstructorInvocation, ReferenceHolder<V, T>> bs,
			BiConsumer<ConstructorInvocation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addConstructorInvocation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callContinueStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ContinueStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<ContinueStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addContinueStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callCreationReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<CreationReference, ReferenceHolder<V, T>> bs,
			BiConsumer<CreationReference, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addCreationReference(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callDimensionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Dimension, ReferenceHolder<V, T>> bs, BiConsumer<Dimension, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addDimension(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callDoStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<DoStatement, ReferenceHolder<V, T>> bs, BiConsumer<DoStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addDoStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callEmptyStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<EmptyStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<EmptyStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEmptyStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callEnhancedForStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<EnhancedForStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<EnhancedForStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnhancedForStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callEnumConstantDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<EnumConstantDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<EnumConstantDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnumConstantDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callEnumDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<EnumDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<EnumDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addEnumDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callExportsDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ExportsDirective, ReferenceHolder<V, T>> bs,
			BiConsumer<ExportsDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExportsDirective(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callExpressionMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ExpressionMethodReference, ReferenceHolder<V, T>> bs,
			BiConsumer<ExpressionMethodReference, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExpressionMethodReference(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callExpressionStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ExpressionStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<ExpressionStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addExpressionStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callFieldAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<FieldAccess, ReferenceHolder<V, T>> bs, BiConsumer<FieldAccess, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addFieldAccess(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callFieldDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<FieldDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<FieldDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addFieldDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callForStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ForStatement, ReferenceHolder<V, T>> bs, BiConsumer<ForStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addForStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callIfStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<IfStatement, ReferenceHolder<V, T>> bs, BiConsumer<IfStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addIfStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callImportDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ImportDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<ImportDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addImportDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callInfixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<InfixExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<InfixExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInfixExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callInitializerVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Initializer, ReferenceHolder<V, T>> bs, BiConsumer<Initializer, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInitializer(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callInstanceofExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<InstanceofExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<InstanceofExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addInstanceofExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callIntersectionTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<IntersectionType, ReferenceHolder<V, T>> bs,
			BiConsumer<IntersectionType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addIntersectionType(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callJavadocVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Javadoc, ReferenceHolder<V, T>> bs, BiConsumer<Javadoc, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addJavadoc(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callLabeledStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<LabeledStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<LabeledStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLabeledStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callLambdaExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<LambdaExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<LambdaExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLambdaExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callLineCommentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<LineComment, ReferenceHolder<V, T>> bs, BiConsumer<LineComment, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addLineComment(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callMarkerAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MarkerAnnotation, ReferenceHolder<V, T>> bs,
			BiConsumer<MarkerAnnotation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMarkerAnnotation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callMemberRefVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MemberRef, ReferenceHolder<V, T>> bs, BiConsumer<MemberRef, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMemberRef(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callMemberValuePairVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MemberValuePair, ReferenceHolder<V, T>> bs,
			BiConsumer<MemberValuePair, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMemberValuePair(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callMethodRefVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodRef, ReferenceHolder<V, T>> bs, BiConsumer<MethodRef, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodRef(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callMethodRefParameterVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodRefParameter, ReferenceHolder<V, T>> bs,
			BiConsumer<MethodRefParameter, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodRefParameter(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callMethodDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<MethodDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callMethodInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<MethodInvocation, ReferenceHolder<V, T>> bs,
			BiConsumer<MethodInvocation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addMethodInvocation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callModifierVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<Modifier, ReferenceHolder<V, T>> bs, BiConsumer<Modifier, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModifier(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callModuleDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ModuleDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<ModuleDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModuleDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callModuleModifierVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ModuleModifier, ReferenceHolder<V, T>> bs,
			BiConsumer<ModuleModifier, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addModuleModifier(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callNameQualifiedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<NameQualifiedType, ReferenceHolder<V, T>> bs,
			BiConsumer<NameQualifiedType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNameQualifiedType(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callNormalAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<NormalAnnotation, ReferenceHolder<V, T>> bs,
			BiConsumer<NormalAnnotation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNormalAnnotation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callNullLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<NullLiteral, ReferenceHolder<V, T>> bs, BiConsumer<NullLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNullLiteral(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callNumberLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<NumberLiteral, ReferenceHolder<V, T>> bs, BiConsumer<NumberLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addNumberLiteral(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callOpensDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<OpensDirective, ReferenceHolder<V, T>> bs,
			BiConsumer<OpensDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addOpensDirective(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callPackageDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PackageDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<PackageDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPackageDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callParameterizedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ParameterizedType, ReferenceHolder<V, T>> bs,
			BiConsumer<ParameterizedType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addParameterizedType(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callParenthesizedExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ParenthesizedExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<ParenthesizedExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addParenthesizedExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callPatternInstanceofExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PatternInstanceofExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<PatternInstanceofExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPatternInstanceofExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callPostfixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PostfixExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<PostfixExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPostfixExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callPrefixExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PrefixExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<PrefixExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPrefixExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callProvidesDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ProvidesDirective, ReferenceHolder<V, T>> bs,
			BiConsumer<ProvidesDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addProvidesDirective(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callPrimitiveTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<PrimitiveType, ReferenceHolder<V, T>> bs, BiConsumer<PrimitiveType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addPrimitiveType(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callQualifiedNameVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<QualifiedName, ReferenceHolder<V, T>> bs, BiConsumer<QualifiedName, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addQualifiedName(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callQualifiedTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<QualifiedType, ReferenceHolder<V, T>> bs, BiConsumer<QualifiedType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addQualifiedType(bs, bc);
		hv.build(node);
	}

//	public static <V,T> void callModuleQualifiedNameVisitor(ASTNode node, ReferenceHolder<V,T> dataholder, BiPredicate<ModuleQualifiedName, ReferenceHolder<V,T>> bs, BiConsumer<ModuleQualifiedName, ReferenceHolder<V,T>> bc) {  HelperVisitor<ReferenceHolder<V,T>> hv = new HelperVisitor<>(nodesprocessed, dataholder); hv.addModuleQualifiedName(bs,bc); hv.build(node);}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */

	public static <V, T> void callRequiresDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<RequiresDirective, ReferenceHolder<V, T>> bs,
			BiConsumer<RequiresDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addRequiresDirective(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callRecordDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<RecordDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<RecordDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addRecordDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callReturnStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ReturnStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<ReturnStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addReturnStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSimpleNameVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SimpleName, ReferenceHolder<V, T>> bs, BiConsumer<SimpleName, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSimpleName(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSimpleTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SimpleType, ReferenceHolder<V, T>> bs, BiConsumer<SimpleType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSimpleType(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSingleMemberAnnotationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SingleMemberAnnotation, ReferenceHolder<V, T>> bs,
			BiConsumer<SingleMemberAnnotation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSingleMemberAnnotation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSingleVariableDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SingleVariableDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<SingleVariableDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSingleVariableDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callStringLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<StringLiteral, ReferenceHolder<V, T>> bs, BiConsumer<StringLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addStringLiteral(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSuperConstructorInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SuperConstructorInvocation, ReferenceHolder<V, T>> bs,
			BiConsumer<SuperConstructorInvocation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperConstructorInvocation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSuperFieldAccessVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SuperFieldAccess, ReferenceHolder<V, T>> bs,
			BiConsumer<SuperFieldAccess, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperFieldAccess(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSuperMethodInvocationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SuperMethodInvocation, ReferenceHolder<V, T>> bs,
			BiConsumer<SuperMethodInvocation, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperMethodInvocation(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSuperMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SuperMethodReference, ReferenceHolder<V, T>> bs,
			BiConsumer<SuperMethodReference, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSuperMethodReference(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSwitchCaseVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SwitchCase, ReferenceHolder<V, T>> bs, BiConsumer<SwitchCase, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchCase(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSwitchExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SwitchExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<SwitchExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSwitchStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SwitchStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<SwitchStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSwitchStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callSynchronizedStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<SynchronizedStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<SynchronizedStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addSynchronizedStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTagElementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TagElement, ReferenceHolder<V, T>> bs, BiConsumer<TagElement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTagElement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTextBlockVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TextBlock, ReferenceHolder<V, T>> bs, BiConsumer<TextBlock, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTextBlock(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTextElementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TextElement, ReferenceHolder<V, T>> bs, BiConsumer<TextElement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTextElement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callThisExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ThisExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<ThisExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addThisExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callThrowStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<ThrowStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<ThrowStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addThrowStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTryStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TryStatement, ReferenceHolder<V, T>> bs, BiConsumer<TryStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTryStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTypeDeclarationVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeDeclaration, ReferenceHolder<V, T>> bs,
			BiConsumer<TypeDeclaration, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeDeclaration(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTypeDeclarationStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeDeclarationStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<TypeDeclarationStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeDeclarationStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTypeLiteralVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeLiteral, ReferenceHolder<V, T>> bs, BiConsumer<TypeLiteral, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeLiteral(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTypeMethodReferenceVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeMethodReference, ReferenceHolder<V, T>> bs,
			BiConsumer<TypeMethodReference, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeMethodReference(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callTypeParameterVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<TypeParameter, ReferenceHolder<V, T>> bs, BiConsumer<TypeParameter, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addTypeParameter(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callUnionTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<UnionType, ReferenceHolder<V, T>> bs, BiConsumer<UnionType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addUnionType(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callUsesDirectiveVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<UsesDirective, ReferenceHolder<V, T>> bs, BiConsumer<UsesDirective, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addUsesDirective(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callVariableDeclarationExpressionVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<VariableDeclarationExpression, ReferenceHolder<V, T>> bs,
			BiConsumer<VariableDeclarationExpression, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationExpression(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callVariableDeclarationStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<VariableDeclarationStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<VariableDeclarationStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param class1
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callVariableDeclarationStatementVisitor(Class<?> class1, ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<VariableDeclarationStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<VariableDeclarationStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationStatement(class1, bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callVariableDeclarationFragmentVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<VariableDeclarationFragment, ReferenceHolder<V, T>> bs,
			BiConsumer<VariableDeclarationFragment, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addVariableDeclarationFragment(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callWhileStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<WhileStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<WhileStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addWhileStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callWildcardTypeVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<WildcardType, ReferenceHolder<V, T>> bs, BiConsumer<WildcardType, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addWildcardType(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 * @param <V>
	 * @param <T>
	 * @param node
	 * @param dataholder
	 * @param nodesprocessed - set of nodes processed
	 * @param bs - BiPredicate that can be assigned a lambda expression
	 * @param bc
	 */
	public static <V, T> void callYieldStatementVisitor(ASTNode node, ReferenceHolder<V, T> dataholder, Set<ASTNode> nodesprocessed,
			BiPredicate<YieldStatement, ReferenceHolder<V, T>> bs,
			BiConsumer<YieldStatement, ReferenceHolder<V, T>> bc) {

		HelperVisitor<ReferenceHolder<V, T>,V,T> hv= new HelperVisitor<>(nodesprocessed, dataholder);
		hv.addYieldStatement(bs, bc);
		hv.build(node);
	}

	/**
	 *
	 */
	public void clear() {
		this.consumermap.clear();
		this.consumerdata.clear();
		this.predicatemap.clear();
		this.predicatedata.clear();
	}

}
