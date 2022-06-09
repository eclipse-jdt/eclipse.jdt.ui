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
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.common;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.eclipse.jdt.core.dom.*;

/**
 *
 * @author chammer
 *
 * @param <E> - type that extends HelpVisitorProvider that provides HelperVisitor<V, T>
 * @param <V> - type that HelperVisitor uses as map key type
 * @param <T> - type that HelperVisitor uses as map value type
 */
@SuppressWarnings("unchecked")
public class LambdaASTVisitor<E extends HelperVisitorProvider<V,T,E>, V, T> extends ASTVisitor {
	/**
	 *
	 */
	private final HelperVisitor<E,V,T> helperVisitor;

	/**
	 * @param helperVisitor - HelperVisitor
	 */
	LambdaASTVisitor(HelperVisitor<E,V,T> helperVisitor) {
		super(false);
		this.helperVisitor = helperVisitor;
	}

	LambdaASTVisitor(HelperVisitor<E,V,T> helperVisitor, boolean visitjavadoc) {
		super(visitjavadoc);
		this.helperVisitor = helperVisitor;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.AnnotationTypeDeclaration)) {
			return ((BiPredicate<AnnotationTypeDeclaration, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.AnnotationTypeDeclaration))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.AnnotationTypeMemberDeclaration)) {
			return ((BiPredicate<AnnotationTypeMemberDeclaration, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.AnnotationTypeMemberDeclaration))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.AnonymousClassDeclaration)) {
			return ((BiPredicate<AnonymousClassDeclaration, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.AnonymousClassDeclaration))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ArrayAccess node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ArrayAccess)) {
			return ((BiPredicate<ArrayAccess, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ArrayAccess)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ArrayCreation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ArrayCreation)) {
			return ((BiPredicate<ArrayCreation, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ArrayCreation)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ArrayInitializer)) {
			return ((BiPredicate<ArrayInitializer, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ArrayInitializer)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ArrayType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ArrayType)) {
			return ((BiPredicate<ArrayType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ArrayType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(AssertStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.AssertStatement)) {
			return ((BiPredicate<AssertStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.AssertStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(Assignment node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.Assignment)) {
			return ((BiPredicate<Assignment, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.Assignment)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(Block node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.Block)) {
			return ((BiPredicate<Block, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.Block))).test(node, this.helperVisitor.dataholder)
					;
		}
		return true;
	}

	@Override
	public boolean visit(BlockComment node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.BlockComment)) {
			return ((BiPredicate<BlockComment, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.BlockComment)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(BooleanLiteral node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.BooleanLiteral)) {
			return ((BiPredicate<BooleanLiteral, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.BooleanLiteral)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(BreakStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.BreakStatement)) {
			return ((BiPredicate<BreakStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.BreakStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(CastExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.CastExpression)) {
			return ((BiPredicate<CastExpression, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.CastExpression)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(CatchClause node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.CatchClause)) {
			return ((BiPredicate<CatchClause, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.CatchClause)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(CharacterLiteral node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.CharacterLiteral)) {
			return ((BiPredicate<CharacterLiteral, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.CharacterLiteral)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ClassInstanceCreation)) {
			return ((BiPredicate<ClassInstanceCreation, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.ClassInstanceCreation))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(CompilationUnit node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.CompilationUnit)) {
			return ((BiPredicate<CompilationUnit, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.CompilationUnit)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ConditionalExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ConditionalExpression)) {
			return ((BiPredicate<ConditionalExpression, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.ConditionalExpression))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ConstructorInvocation)) {
			return ((BiPredicate<ConstructorInvocation, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.ConstructorInvocation))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ContinueStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ContinueStatement)) {
			return ((BiPredicate<ContinueStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ContinueStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(CreationReference node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.CreationReference)) {
			return ((BiPredicate<CreationReference, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.CreationReference)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(Dimension node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.Dimension)) {
			return ((BiPredicate<Dimension, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.Dimension)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(DoStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.DoStatement)) {
			return ((BiPredicate<DoStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.DoStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(EmptyStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.EmptyStatement)) {
			return ((BiPredicate<EmptyStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.EmptyStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(EnhancedForStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.EnhancedForStatement)) {
			return ((BiPredicate<EnhancedForStatement, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.EnhancedForStatement))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.EnumConstantDeclaration)) {
			return ((BiPredicate<EnumConstantDeclaration, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.EnumConstantDeclaration))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.EnumDeclaration)) {
			return ((BiPredicate<EnumDeclaration, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.EnumDeclaration)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ExportsDirective node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ExportsDirective)) {
			return ((BiPredicate<ExportsDirective, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ExportsDirective)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ExpressionMethodReference node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.BreakStatement)) {
			return ((BiPredicate<ExpressionMethodReference, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.ExpressionMethodReference))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ExpressionStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ExpressionStatement)) {
			return ((BiPredicate<ExpressionStatement, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.ExpressionStatement))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(FieldAccess node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.FieldAccess)) {
			return ((BiPredicate<FieldAccess, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.FieldAccess)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.FieldDeclaration)) {
			return ((BiPredicate<FieldDeclaration, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.FieldDeclaration)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ForStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ForStatement)) {
			return ((BiPredicate<ForStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ForStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(IfStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.IfStatement)) {
			return ((BiPredicate<IfStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.IfStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ImportDeclaration)) {
			return ((BiPredicate<ImportDeclaration, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ImportDeclaration)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(InfixExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.InfixExpression)) {
			return ((BiPredicate<InfixExpression, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.InfixExpression)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(Initializer node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.Initializer)) {
			return ((BiPredicate<Initializer, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.Initializer)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.InstanceofExpression)) {
			return ((BiPredicate<InstanceofExpression, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.InstanceofExpression))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(IntersectionType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.IntersectionType)) {
			return ((BiPredicate<IntersectionType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.IntersectionType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(Javadoc node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.Javadoc)) {
			return ((BiPredicate<Javadoc, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.Javadoc)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(LabeledStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.LabeledStatement)) {
			return ((BiPredicate<LabeledStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.LabeledStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(LambdaExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.LambdaExpression)) {
			return ((BiPredicate<LambdaExpression, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.LambdaExpression)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(LineComment node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.LineComment)) {
			return ((BiPredicate<LineComment, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.LineComment)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(MarkerAnnotation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.MarkerAnnotation)) {
			return ((BiPredicate<MarkerAnnotation, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.MarkerAnnotation)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(MemberRef node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.MemberRef)) {
			return ((BiPredicate<MemberRef, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.MemberRef)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(MemberValuePair node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.MemberValuePair)) {
			return ((BiPredicate<MemberValuePair, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.MemberValuePair)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(MethodRef node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.MethodRef)) {
			return ((BiPredicate<MethodRef, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.MethodRef)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(MethodRefParameter node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.MethodRefParameter)) {
			return ((BiPredicate<MethodRefParameter, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.MethodRefParameter)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.MethodDeclaration)) {
			return ((BiPredicate<MethodDeclaration, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.MethodDeclaration)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.MethodInvocation)) {
			String data=(String) this.helperVisitor.getSupplierData().get(VisitorEnum.MethodInvocation);
			if (data!= null && !node.getName().getIdentifier().equals(data)) {
				return true;
			}
			return ((BiPredicate<MethodInvocation, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.MethodInvocation))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(Modifier node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.Modifier)) {
			return ((BiPredicate<Modifier, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.Modifier)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ModuleDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ModuleDeclaration)) {
			return ((BiPredicate<ModuleDeclaration, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ModuleDeclaration)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ModuleModifier node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ModuleModifier)) {
			return ((BiPredicate<ModuleModifier, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ModuleModifier)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(NameQualifiedType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.NameQualifiedType)) {
			return ((BiPredicate<NameQualifiedType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.NameQualifiedType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.NormalAnnotation)) {
			return ((BiPredicate<NormalAnnotation, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.NormalAnnotation)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(NullLiteral node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.NullLiteral)) {
			return ((BiPredicate<NullLiteral, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.NullLiteral)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(NumberLiteral node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.NumberLiteral)) {
			return ((BiPredicate<NumberLiteral, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.NumberLiteral)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(OpensDirective node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.OpensDirective)) {
			return ((BiPredicate<OpensDirective, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.OpensDirective)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.PackageDeclaration)) {
			return ((BiPredicate<PackageDeclaration, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.PackageDeclaration)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ParameterizedType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ParameterizedType)) {
			return ((BiPredicate<ParameterizedType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ParameterizedType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ParenthesizedExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ParenthesizedExpression)) {
			return ((BiPredicate<ParenthesizedExpression, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.ParenthesizedExpression))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(PatternInstanceofExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.PatternInstanceofExpression)) {
			return ((BiPredicate<PatternInstanceofExpression, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.PatternInstanceofExpression))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(PostfixExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.PostfixExpression)) {
			return ((BiPredicate<PostfixExpression, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.PostfixExpression)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(PrefixExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.PrefixExpression)) {
			return ((BiPredicate<PrefixExpression, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.PrefixExpression)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ProvidesDirective node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ProvidesDirective)) {
			return ((BiPredicate<ProvidesDirective, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ProvidesDirective)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(PrimitiveType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.PrimitiveType)) {
			return ((BiPredicate<PrimitiveType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.PrimitiveType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(QualifiedName node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.QualifiedName)) {
			return ((BiPredicate<QualifiedName, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.QualifiedName)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(QualifiedType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.QualifiedType)) {
			return ((BiPredicate<QualifiedType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.QualifiedType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

//	@Override
//	public boolean visit(ModuleQualifiedName node) {
//		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ModuleQualifiedName)) {
//			return ((BiPredicate<ModuleQualifiedName, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ModuleQualifiedName)))
//					.test(node, this.helperVisitor.dataholder);
//		}
//		return true;
//	}

	@Override
	public boolean visit(RequiresDirective node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.RequiresDirective)) {
			return ((BiPredicate<RequiresDirective, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.RequiresDirective)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(RecordDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.RecordDeclaration)) {
			return ((BiPredicate<RecordDeclaration, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.RecordDeclaration)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ReturnStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ReturnStatement)) {
			return ((BiPredicate<ReturnStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ReturnStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SimpleName node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SimpleName)) {
			return ((BiPredicate<SimpleName, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.SimpleName)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SimpleType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SimpleType)) {
			return ((BiPredicate<SimpleType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.SimpleType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SingleMemberAnnotation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SingleMemberAnnotation)) {
			return ((BiPredicate<SingleMemberAnnotation, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.SingleMemberAnnotation))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SingleVariableDeclaration)) {
			return ((BiPredicate<SingleVariableDeclaration, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.SingleVariableDeclaration))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(StringLiteral node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.StringLiteral)) {
			return ((BiPredicate<StringLiteral, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.StringLiteral)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SuperConstructorInvocation)) {
			return ((BiPredicate<SuperConstructorInvocation, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.SuperConstructorInvocation))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SuperFieldAccess)) {
			return ((BiPredicate<SuperFieldAccess, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.SuperFieldAccess)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SuperMethodInvocation)) {
			return ((BiPredicate<SuperMethodInvocation, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.SuperMethodInvocation))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SuperMethodReference)) {
			return ((BiPredicate<SuperMethodReference, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.SuperMethodReference))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SwitchCase node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SwitchCase)) {
			return ((BiPredicate<SwitchCase, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.SwitchCase)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SwitchExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SwitchExpression)) {
			return ((BiPredicate<SwitchExpression, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.SwitchExpression)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SwitchStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SwitchStatement)) {
			return ((BiPredicate<SwitchStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.SwitchStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(SynchronizedStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.SynchronizedStatement)) {
			return ((BiPredicate<SynchronizedStatement, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.SynchronizedStatement))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TagElement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TagElement)) {
			return ((BiPredicate<TagElement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.TagElement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TextBlock node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TextBlock)) {
			return ((BiPredicate<TextBlock, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.TextBlock)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TextElement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TextElement)) {
			return ((BiPredicate<TextElement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.TextElement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ThisExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ThisExpression)) {
			return ((BiPredicate<ThisExpression, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ThisExpression)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.ThrowStatement)) {
			return ((BiPredicate<ThrowStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.ThrowStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TryStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TryStatement)) {
			return ((BiPredicate<TryStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.TryStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TypeDeclaration)) {
			return ((BiPredicate<TypeDeclaration, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.TypeDeclaration)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TypeDeclarationStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TypeDeclarationStatement)) {
			return ((BiPredicate<TypeDeclarationStatement, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.TypeDeclarationStatement))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TypeLiteral node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TypeLiteral)) {
			return ((BiPredicate<TypeLiteral, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.TypeLiteral)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TypeMethodReference node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TypeMethodReference)) {
			return ((BiPredicate<TypeMethodReference, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.TypeMethodReference))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(TypeParameter node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.TypeParameter)) {
			return ((BiPredicate<TypeParameter, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.TypeParameter)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(UnionType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.UnionType)) {
			return ((BiPredicate<UnionType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.UnionType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(UsesDirective node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.UsesDirective)) {
			return ((BiPredicate<UsesDirective, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.UsesDirective)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.VariableDeclarationExpression)) {
			return ((BiPredicate<VariableDeclarationExpression, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.VariableDeclarationExpression))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.VariableDeclarationStatement)) {
			Class<?> data=(Class<?>) this.helperVisitor.getSupplierData().get(VisitorEnum.VariableDeclarationStatement);
			if (data!= null) {
				VariableDeclarationFragment bli = (VariableDeclarationFragment) node.fragments().get(0);
				IVariableBinding resolveBinding = bli.resolveBinding();
				if(resolveBinding!=null) {
					String qualifiedName = resolveBinding.getType().getErasure().getQualifiedName();
					if (!data.getCanonicalName().equals(qualifiedName)) {
						return true;
					}
				}
			}
			return ((BiPredicate<VariableDeclarationStatement, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.VariableDeclarationStatement))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.VariableDeclarationFragment)) {
			return ((BiPredicate<VariableDeclarationFragment, E>) (this.helperVisitor.predicatemap
					.get(VisitorEnum.VariableDeclarationFragment))).test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(WhileStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.WhileStatement)) {
			return ((BiPredicate<WhileStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.WhileStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(WildcardType node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.WildcardType)) {
			return ((BiPredicate<WildcardType, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.WildcardType)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public boolean visit(YieldStatement node) {
		if (this.helperVisitor.predicatemap.containsKey(VisitorEnum.YieldStatement)) {
			return ((BiPredicate<YieldStatement, E>) (this.helperVisitor.predicatemap.get(VisitorEnum.YieldStatement)))
					.test(node, this.helperVisitor.dataholder);
		}
		return true;
	}

	@Override
	public void endVisit(AnnotationTypeDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.AnnotationTypeDeclaration)) {
			((BiConsumer<AnnotationTypeDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.AnnotationTypeDeclaration)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(AnnotationTypeMemberDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.AnnotationTypeMemberDeclaration)) {
			((BiConsumer<AnnotationTypeMemberDeclaration, E>) (this.helperVisitor.consumermap
					.get(VisitorEnum.AnnotationTypeMemberDeclaration))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(AnonymousClassDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.AnonymousClassDeclaration)) {
			((BiConsumer<AnonymousClassDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.AnonymousClassDeclaration)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ArrayAccess node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ArrayAccess)) {
			((BiConsumer<ArrayAccess, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ArrayAccess))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ArrayCreation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ArrayCreation)) {
			((BiConsumer<ArrayCreation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ArrayCreation))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ArrayInitializer node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ArrayInitializer)) {
			((BiConsumer<ArrayInitializer, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ArrayInitializer))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ArrayType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ArrayType)) {
			((BiConsumer<ArrayType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ArrayType))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(AssertStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.AssertStatement)) {
			((BiConsumer<AssertStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.AssertStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(Assignment node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.Assignment)) {
			((BiConsumer<Assignment, E>) (this.helperVisitor.consumermap.get(VisitorEnum.Assignment))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(Block node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.Block)) {
			((BiConsumer<Block, E>) (this.helperVisitor.consumermap.get(VisitorEnum.Block))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(BlockComment node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.BlockComment)) {
			((BiConsumer<BlockComment, E>) (this.helperVisitor.consumermap.get(VisitorEnum.BlockComment))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(BooleanLiteral node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.BooleanLiteral)) {
			((BiConsumer<BooleanLiteral, E>) (this.helperVisitor.consumermap.get(VisitorEnum.BooleanLiteral))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(BreakStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.BreakStatement)) {
			((BiConsumer<BreakStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.BreakStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(CastExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.CastExpression)) {
			((BiConsumer<CastExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.CastExpression))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(CatchClause node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.CatchClause)) {
			((BiConsumer<CatchClause, E>) (this.helperVisitor.consumermap.get(VisitorEnum.CatchClause))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(CharacterLiteral node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.CharacterLiteral)) {
			((BiConsumer<CharacterLiteral, E>) (this.helperVisitor.consumermap.get(VisitorEnum.CharacterLiteral))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ClassInstanceCreation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ClassInstanceCreation)) {
			((BiConsumer<ClassInstanceCreation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ClassInstanceCreation)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(CompilationUnit node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.CompilationUnit)) {
			((BiConsumer<CompilationUnit, E>) (this.helperVisitor.consumermap.get(VisitorEnum.CompilationUnit))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ConditionalExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ConditionalExpression)) {
			((BiConsumer<ConditionalExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ConditionalExpression)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ConstructorInvocation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ConstructorInvocation)) {
			((BiConsumer<ConstructorInvocation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ConstructorInvocation)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ContinueStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ContinueStatement)) {
			((BiConsumer<ContinueStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ContinueStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(CreationReference node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.CreationReference)) {
			((BiConsumer<CreationReference, E>) (this.helperVisitor.consumermap.get(VisitorEnum.CreationReference))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(Dimension node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.Dimension)) {
			((BiConsumer<Dimension, E>) (this.helperVisitor.consumermap.get(VisitorEnum.Dimension))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(DoStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.DoStatement)) {
			((BiConsumer<DoStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.DoStatement))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(EmptyStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.EmptyStatement)) {
			((BiConsumer<EmptyStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.EmptyStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(EnhancedForStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.EnhancedForStatement)) {
			((BiConsumer<EnhancedForStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.EnhancedForStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(EnumConstantDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.EnumConstantDeclaration)) {
			((BiConsumer<EnumConstantDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.EnumConstantDeclaration)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(EnumDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.EnumDeclaration)) {
			((BiConsumer<EnumDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.EnumDeclaration))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ExportsDirective node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ExportsDirective)) {
			((BiConsumer<ExportsDirective, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ExportsDirective))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ExpressionMethodReference node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ExpressionMethodReference)) {
			((BiConsumer<ExpressionMethodReference, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ExpressionMethodReference)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ExpressionStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ExpressionStatement)) {
			((BiConsumer<ExpressionStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ExpressionStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(FieldAccess node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.FieldAccess)) {
			((BiConsumer<FieldAccess, E>) (this.helperVisitor.consumermap.get(VisitorEnum.FieldAccess))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(FieldDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.FieldDeclaration)) {
			((BiConsumer<FieldDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.FieldDeclaration))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ForStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ForStatement)) {
			((BiConsumer<ForStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ForStatement))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(IfStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.IfStatement)) {
			((BiConsumer<IfStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.IfStatement))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ImportDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ImportDeclaration)) {
			((BiConsumer<ImportDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ImportDeclaration))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(InfixExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.InfixExpression)) {
			((BiConsumer<InfixExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.InfixExpression))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(Initializer node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.Initializer)) {
			((BiConsumer<Initializer, E>) (this.helperVisitor.consumermap.get(VisitorEnum.Initializer))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(InstanceofExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.InstanceofExpression)) {
			((BiConsumer<InstanceofExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.InstanceofExpression))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(IntersectionType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.IntersectionType)) {
			((BiConsumer<IntersectionType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.IntersectionType))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(Javadoc node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.Javadoc)) {
			((BiConsumer<Javadoc, E>) (this.helperVisitor.consumermap.get(VisitorEnum.Javadoc))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(LabeledStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.LabeledStatement)) {
			((BiConsumer<LabeledStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.LabeledStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(LambdaExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.LambdaExpression)) {
			((BiConsumer<LambdaExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.LambdaExpression))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(LineComment node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.LineComment)) {
			((BiConsumer<LineComment, E>) (this.helperVisitor.consumermap.get(VisitorEnum.LineComment))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(MarkerAnnotation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.MarkerAnnotation)) {
			((BiConsumer<MarkerAnnotation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.MarkerAnnotation))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(MemberRef node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.MemberRef)) {
			((BiConsumer<MemberRef, E>) (this.helperVisitor.consumermap.get(VisitorEnum.MemberRef))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(MemberValuePair node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.MemberValuePair)) {
			((BiConsumer<MemberValuePair, E>) (this.helperVisitor.consumermap.get(VisitorEnum.MemberValuePair))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(MethodRef node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.MethodRef)) {
			((BiConsumer<MethodRef, E>) (this.helperVisitor.consumermap.get(VisitorEnum.MethodRef))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(MethodRefParameter node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.MethodRefParameter)) {
			((BiConsumer<MethodRefParameter, E>) (this.helperVisitor.consumermap.get(VisitorEnum.MethodRefParameter))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.MethodDeclaration)) {
			((BiConsumer<MethodDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.MethodDeclaration))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(MethodInvocation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.MethodInvocation)) {
			String data=(String) this.helperVisitor.getConsumerData().get(VisitorEnum.MethodInvocation);
			if (data!= null && !node.getName().getIdentifier().equals(data)) {
				return;
			}
			((BiConsumer<MethodInvocation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.MethodInvocation))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(Modifier node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.Modifier)) {
			((BiConsumer<Modifier, E>) (this.helperVisitor.consumermap.get(VisitorEnum.Modifier))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ModuleDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ModuleDeclaration)) {
			((BiConsumer<ModuleDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ModuleDeclaration))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ModuleModifier node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ModuleModifier)) {
			((BiConsumer<ModuleModifier, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ModuleModifier))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(NameQualifiedType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.NameQualifiedType)) {
			((BiConsumer<NameQualifiedType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.NameQualifiedType))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(NormalAnnotation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.NormalAnnotation)) {
			((BiConsumer<NormalAnnotation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.NormalAnnotation))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(NullLiteral node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.NullLiteral)) {
			((BiConsumer<NullLiteral, E>) (this.helperVisitor.consumermap.get(VisitorEnum.NullLiteral))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(NumberLiteral node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.NumberLiteral)) {
			((BiConsumer<NumberLiteral, E>) (this.helperVisitor.consumermap.get(VisitorEnum.NumberLiteral))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(OpensDirective node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.OpensDirective)) {
			((BiConsumer<OpensDirective, E>) (this.helperVisitor.consumermap.get(VisitorEnum.OpensDirective))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(PackageDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.PackageDeclaration)) {
			((BiConsumer<PackageDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.PackageDeclaration))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ParameterizedType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ParameterizedType)) {
			((BiConsumer<ParameterizedType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ParameterizedType))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ParenthesizedExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ParenthesizedExpression)) {
			((BiConsumer<ParenthesizedExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ParenthesizedExpression)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(PatternInstanceofExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.PatternInstanceofExpression)) {
			((BiConsumer<PatternInstanceofExpression, E>) (this.helperVisitor.consumermap
					.get(VisitorEnum.PatternInstanceofExpression))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(PostfixExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.PostfixExpression)) {
			((BiConsumer<PostfixExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.PostfixExpression))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(PrefixExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.PrefixExpression)) {
			((BiConsumer<PrefixExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.PrefixExpression))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ProvidesDirective node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ProvidesDirective)) {
			((BiConsumer<ProvidesDirective, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ProvidesDirective))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(PrimitiveType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.PrimitiveType)) {
			((BiConsumer<PrimitiveType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.PrimitiveType))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(QualifiedName node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.QualifiedName)) {
			((BiConsumer<QualifiedName, E>) (this.helperVisitor.consumermap.get(VisitorEnum.QualifiedName))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(QualifiedType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.QualifiedType)) {
			((BiConsumer<QualifiedType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.QualifiedType))).accept(node, this.helperVisitor.dataholder);
		}
	}

//	@Override
//	public void endVisit(ModuleQualifiedName node) {
//		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ModuleQualifiedName)) {
//			((BiConsumer<ModuleQualifiedName, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ModuleQualifiedName))).accept(node,
//					this.helperVisitor.dataholder);
//		}
//	}

	@Override
	public void endVisit(RequiresDirective node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.RequiresDirective)) {
			((BiConsumer<RequiresDirective, E>) (this.helperVisitor.consumermap.get(VisitorEnum.RequiresDirective))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(RecordDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.RecordDeclaration)) {
			((BiConsumer<RecordDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.RecordDeclaration))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ReturnStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ReturnStatement)) {
			((BiConsumer<ReturnStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ReturnStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SimpleName node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SimpleName)) {
			((BiConsumer<SimpleName, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SimpleName))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SimpleType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SimpleType)) {
			((BiConsumer<SimpleType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SimpleType))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SingleMemberAnnotation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SingleMemberAnnotation)) {
			((BiConsumer<SingleMemberAnnotation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SingleMemberAnnotation)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SingleVariableDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SingleVariableDeclaration)) {
			((BiConsumer<SingleVariableDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SingleVariableDeclaration)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(StringLiteral node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.StringLiteral)) {
			((BiConsumer<StringLiteral, E>) (this.helperVisitor.consumermap.get(VisitorEnum.StringLiteral))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SuperConstructorInvocation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SuperConstructorInvocation)) {
			((BiConsumer<SuperConstructorInvocation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SuperConstructorInvocation)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SuperFieldAccess node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SuperFieldAccess)) {
			((BiConsumer<SuperFieldAccess, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SuperFieldAccess))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SuperMethodInvocation node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SuperMethodInvocation)) {
			((BiConsumer<SuperMethodInvocation, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SuperMethodInvocation)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SuperMethodReference node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SuperMethodReference)) {
			((BiConsumer<SuperMethodReference, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SuperMethodReference))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SwitchCase node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SwitchCase)) {
			((BiConsumer<SwitchCase, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SwitchCase))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SwitchExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SwitchExpression)) {
			((BiConsumer<SwitchExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SwitchExpression))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SwitchStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SwitchStatement)) {
			((BiConsumer<SwitchStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SwitchStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(SynchronizedStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.SynchronizedStatement)) {
			((BiConsumer<SynchronizedStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.SynchronizedStatement)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TagElement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TagElement)) {
			((BiConsumer<TagElement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TagElement))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TextBlock node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TextBlock)) {
			((BiConsumer<TextBlock, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TextBlock))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TextElement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TextElement)) {
			((BiConsumer<TextElement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TextElement))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ThisExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ThisExpression)) {
			((BiConsumer<ThisExpression, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ThisExpression))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(ThrowStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.ThrowStatement)) {
			((BiConsumer<ThrowStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.ThrowStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TryStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TryStatement)) {
			((BiConsumer<TryStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TryStatement))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TypeDeclaration)) {
			((BiConsumer<TypeDeclaration, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TypeDeclaration))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TypeDeclarationStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TypeDeclarationStatement)) {
			((BiConsumer<TypeDeclarationStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TypeDeclarationStatement)))
					.accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TypeLiteral node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TypeLiteral)) {
			((BiConsumer<TypeLiteral, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TypeLiteral))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TypeMethodReference node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TypeMethodReference)) {
			((BiConsumer<TypeMethodReference, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TypeMethodReference))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(TypeParameter node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.TypeParameter)) {
			((BiConsumer<TypeParameter, E>) (this.helperVisitor.consumermap.get(VisitorEnum.TypeParameter))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(UnionType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.UnionType)) {
			((BiConsumer<UnionType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.UnionType))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(UsesDirective node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.UsesDirective)) {
			((BiConsumer<UsesDirective, E>) (this.helperVisitor.consumermap.get(VisitorEnum.UsesDirective))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(VariableDeclarationExpression node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.VariableDeclarationExpression)) {
			((BiConsumer<VariableDeclarationExpression, E>) (this.helperVisitor.consumermap
					.get(VisitorEnum.VariableDeclarationExpression))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(VariableDeclarationStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.VariableDeclarationStatement)) {
			Class<?> data=(Class<?>) this.helperVisitor.getConsumerData().get(VisitorEnum.VariableDeclarationStatement);
			if (data!= null) {
				VariableDeclarationFragment bli = (VariableDeclarationFragment) node.fragments().get(0);
				IVariableBinding resolveBinding = bli.resolveBinding();
				if(resolveBinding!=null) {
					String qualifiedName = resolveBinding.getType().getErasure().getQualifiedName();
					if (!data.getCanonicalName().equals(qualifiedName)) {
						return;
					}
				}
			}
			((BiConsumer<VariableDeclarationStatement, E>) (this.helperVisitor.consumermap
					.get(VisitorEnum.VariableDeclarationStatement))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(VariableDeclarationFragment node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.VariableDeclarationFragment)) {
			((BiConsumer<VariableDeclarationFragment, E>) (this.helperVisitor.consumermap
					.get(VisitorEnum.VariableDeclarationFragment))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(WhileStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.WhileStatement)) {
			((BiConsumer<WhileStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.WhileStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(WildcardType node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.WildcardType)) {
			((BiConsumer<WildcardType, E>) (this.helperVisitor.consumermap.get(VisitorEnum.WildcardType))).accept(node, this.helperVisitor.dataholder);
		}
	}

	@Override
	public void endVisit(YieldStatement node) {
		if (this.helperVisitor.consumermap.containsKey(VisitorEnum.YieldStatement)) {
			((BiConsumer<YieldStatement, E>) (this.helperVisitor.consumermap.get(VisitorEnum.YieldStatement))).accept(node,
					this.helperVisitor.dataholder);
		}
	}
}