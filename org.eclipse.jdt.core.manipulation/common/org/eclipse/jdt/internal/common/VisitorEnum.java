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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 *
 * @author chammer
 */
public enum VisitorEnum {

	/**
	 *
	 */
	AnnotationTypeDeclaration(ASTNode.ANNOTATION_TYPE_DECLARATION),

	/**
	 *
	 */
	AnnotationTypeMemberDeclaration(ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION),

	/**
	 *
	 */
	AnonymousClassDeclaration(ASTNode.ANONYMOUS_CLASS_DECLARATION),

	/**
	 *
	 */
	ArrayAccess(ASTNode.ARRAY_ACCESS),

	/**
	 *
	 */
	ArrayCreation(ASTNode.ARRAY_CREATION),

	/**
	 *
	 */
	ArrayInitializer(ASTNode.ARRAY_INITIALIZER),

	/**
	 *
	 */
	ArrayType(ASTNode.ARRAY_TYPE),

	/**
	 *
	 */
	AssertStatement(ASTNode.ASSERT_STATEMENT),

	/**
	 *
	 */
	Assignment(ASTNode.ASSIGNMENT),

	/**
	 *
	 */
	Block(ASTNode.BLOCK),

	/**
	 *
	 */
	BlockComment(ASTNode.BLOCK_COMMENT),

	/**
	 *
	 */
	BooleanLiteral(ASTNode.BOOLEAN_LITERAL),

	/**
	 *
	 */
	BreakStatement(ASTNode.BREAK_STATEMENT),

	/**
	 *
	 */
	CastExpression(ASTNode.CAST_EXPRESSION),

	/**
	 *
	 */
	CatchClause(ASTNode.CATCH_CLAUSE),

	/**
	 *
	 */
	CharacterLiteral(ASTNode.CHARACTER_LITERAL),

	/**
	 *
	 */
	ClassInstanceCreation(ASTNode.CLASS_INSTANCE_CREATION),

	/**
	 *
	 */
	CompilationUnit(ASTNode.COMPILATION_UNIT),

	/**
	 *
	 */
	ConditionalExpression(ASTNode.CONDITIONAL_EXPRESSION),

	/**
	 *
	 */
	ConstructorInvocation(ASTNode.CONSTRUCTOR_INVOCATION),

	/**
	 *
	 */
	ContinueStatement(ASTNode.CONTINUE_STATEMENT),

	/**
	 *
	 */
	CreationReference(ASTNode.CREATION_REFERENCE),

	/**
	 *
	 */
	Dimension(ASTNode.DIMENSION),

	/**
	 *
	 */
	DoStatement(ASTNode.DO_STATEMENT),

	/**
	 *
	 */
	EmptyStatement(ASTNode.EMPTY_STATEMENT),

	/**
	 *
	 */
	EnhancedForStatement(ASTNode.ENHANCED_FOR_STATEMENT),

	/**
	 *
	 */
	EnumConstantDeclaration(ASTNode.ENUM_CONSTANT_DECLARATION),

	/**
	 *
	 */
	EnumDeclaration(ASTNode.ENUM_DECLARATION),

	/**
	 *
	 */
	ExportsDirective(ASTNode.EXPORTS_DIRECTIVE),

	/**
	 *
	 */
	ExpressionMethodReference(ASTNode.EXPRESSION_METHOD_REFERENCE),

	/**
	 *
	 */
	ExpressionStatement(ASTNode.EXPRESSION_STATEMENT),

	/**
	 *
	 */
	FieldAccess(ASTNode.FIELD_ACCESS),

	/**
	 *
	 */
	FieldDeclaration(ASTNode.FIELD_DECLARATION),

	/**
	 *
	 */
	ForStatement(ASTNode.FOR_STATEMENT),

	/**
	 *
	 */
	IfStatement(ASTNode.IF_STATEMENT),

	/**
	 *
	 */
	ImportDeclaration(ASTNode.IMPORT_DECLARATION),

	/**
	 *
	 */
	InfixExpression(ASTNode.INFIX_EXPRESSION),

	/**
	 *
	 */
	Initializer(ASTNode.INITIALIZER),

	/**
	 *
	 */
	InstanceofExpression(ASTNode.INSTANCEOF_EXPRESSION),

	/**
	 *
	 */
	IntersectionType(ASTNode.INTERSECTION_TYPE),

	/**
	 *
	 */
	Javadoc(ASTNode.JAVADOC),

	/**
	 *
	 */
	LabeledStatement(ASTNode.LABELED_STATEMENT),

	/**
	 *
	 */
	LambdaExpression(ASTNode.LAMBDA_EXPRESSION),

	/**
	 *
	 */
	LineComment(ASTNode.LINE_COMMENT),

	/**
	 *
	 */
	MarkerAnnotation(ASTNode.MARKER_ANNOTATION),

	/**
	 *
	 */
	MemberRef(ASTNode.MEMBER_REF),

	/**
	 *
	 */
	MemberValuePair(ASTNode.MEMBER_VALUE_PAIR),

	/**
	 *
	 */
	MethodRef(ASTNode.METHOD_REF),

	/**
	 *
	 */
	MethodRefParameter(ASTNode.METHOD_REF_PARAMETER),

	/**
	 *
	 */
	MethodDeclaration(ASTNode.METHOD_DECLARATION),

	/**
	 *
	 */
	MethodInvocation(ASTNode.METHOD_INVOCATION),

	/**
	 *
	 */
	Modifier(ASTNode.MODIFIER),

	/**
	 *
	 */
	ModuleDeclaration(ASTNode.MODULE_DECLARATION),

	/**
	 *
	 */
	ModuleModifier(ASTNode.MODULE_MODIFIER),

	/**
	 *
	 */
	NameQualifiedType(ASTNode.NAME_QUALIFIED_TYPE),

	/**
	 *
	 */
	NormalAnnotation(ASTNode.NORMAL_ANNOTATION),

	/**
	 *
	 */
	NullLiteral(ASTNode.NULL_LITERAL),

	/**
	 *
	 */
	NumberLiteral(ASTNode.NUMBER_LITERAL),

	/**
	 *
	 */
	OpensDirective(ASTNode.OPENS_DIRECTIVE),

	/**
	 *
	 */
	PackageDeclaration(ASTNode.PACKAGE_DECLARATION),

	/**
	 *
	 */
	ParameterizedType(ASTNode.PARAMETERIZED_TYPE),

	/**
	 *
	 */
	ParenthesizedExpression(ASTNode.PARENTHESIZED_EXPRESSION),

	/**
	 *
	 */
	PatternInstanceofExpression(ASTNode.PATTERN_INSTANCEOF_EXPRESSION),

	/**
	 *
	 */
	PostfixExpression(ASTNode.POSTFIX_EXPRESSION),

	/**
	 *
	 */
	PrefixExpression(ASTNode.PREFIX_EXPRESSION),

	/**
	 *
	 */
	ProvidesDirective(ASTNode.PROVIDES_DIRECTIVE),

	/**
	 *
	 */
	PrimitiveType(ASTNode.PRIMITIVE_TYPE),

	/**
	 *
	 */
	QualifiedName(ASTNode.QUALIFIED_NAME),

	/**
	 *
	 */
	QualifiedType(ASTNode.QUALIFIED_TYPE),
//	ModuleQualifiedName(ASTNode.MODULE_QUALIFIED_NAME),

	/**
	 *
	 */
	RequiresDirective(ASTNode.REQUIRES_DIRECTIVE),

	/**
	 *
	 */
	RecordDeclaration(ASTNode.RECORD_DECLARATION),

	/**
	 *
	 */
	ReturnStatement(ASTNode.RETURN_STATEMENT),

	/**
	 *
	 */
	SimpleName(ASTNode.SIMPLE_NAME),

	/**
	 *
	 */
	SimpleType(ASTNode.SIMPLE_TYPE),

	/**
	 *
	 */
	SingleMemberAnnotation(ASTNode.SINGLE_MEMBER_ANNOTATION),

	/**
	 *
	 */
	SingleVariableDeclaration(ASTNode.SINGLE_VARIABLE_DECLARATION),

	/**
	 *
	 */
	StringLiteral(ASTNode.STRING_LITERAL),

	/**
	 *
	 */
	SuperConstructorInvocation(ASTNode.SUPER_CONSTRUCTOR_INVOCATION),

	/**
	 *
	 */
	SuperFieldAccess(ASTNode.SUPER_FIELD_ACCESS),

	/**
	 *
	 */
	SuperMethodInvocation(ASTNode.SUPER_METHOD_INVOCATION),

	/**
	 *
	 */
	SuperMethodReference(ASTNode.SUPER_METHOD_REFERENCE),

	/**
	 *
	 */
	SwitchCase(ASTNode.SWITCH_CASE),

	/**
	 *
	 */
	SwitchExpression(ASTNode.SWITCH_EXPRESSION),

	/**
	 *
	 */
	SwitchStatement(ASTNode.SWITCH_STATEMENT),

	/**
	 *
	 */
	SynchronizedStatement(ASTNode.SYNCHRONIZED_STATEMENT),

	/**
	 *
	 */
	TagElement(ASTNode.TAG_ELEMENT),

	/**
	 *
	 */
	TextBlock(ASTNode.TEXT_BLOCK),

	/**
	 *
	 */
	TextElement(ASTNode.TEXT_ELEMENT),

	/**
	 *
	 */
	ThisExpression(ASTNode.THIS_EXPRESSION),

	/**
	 *
	 */
	ThrowStatement(ASTNode.THROW_STATEMENT),

	/**
	 *
	 */
	TryStatement(ASTNode.TRY_STATEMENT),

	/**
	 *
	 */
	TypeDeclaration(ASTNode.TYPE_DECLARATION),

	/**
	 *
	 */
	TypeDeclarationStatement(ASTNode.TYPE_DECLARATION_STATEMENT),

	/**
	 *
	 */
	TypeLiteral(ASTNode.TYPE_LITERAL),

	/**
	 *
	 */
	TypeMethodReference(ASTNode.TYPE_METHOD_REFERENCE),

	/**
	 *
	 */
	TypeParameter(ASTNode.TYPE_PARAMETER),

	/**
	 *
	 */
	UnionType(ASTNode.UNION_TYPE),

	/**
	 *
	 */
	UsesDirective(ASTNode.USES_DIRECTIVE),

	/**
	 *
	 */
	VariableDeclarationExpression(ASTNode.VARIABLE_DECLARATION_EXPRESSION),

	/**
	 *
	 */
	VariableDeclarationStatement(ASTNode.VARIABLE_DECLARATION_STATEMENT),

	/**
	 *
	 */
	VariableDeclarationFragment(ASTNode.VARIABLE_DECLARATION_FRAGMENT),

	/**
	 *
	 */
	WhileStatement(ASTNode.WHILE_STATEMENT),

	/**
	 *
	 */
	WildcardType(ASTNode.WILDCARD_TYPE),

	/**
	 *
	 */
	YieldStatement(ASTNode.YIELD_STATEMENT);

	int nodetype;

	VisitorEnum(int nodetype) {
		this.nodetype= nodetype;
	}

	/**
	 *
	 * @return - node type
	 */
	public int getValue() {
		return nodetype;
	}

	/**
	 *
	 * @return - Stream if VisitorEnum values
	 */
	public static Stream<VisitorEnum> stream() {
		return Stream.of(VisitorEnum.values());
	}

	static final Map<Integer, VisitorEnum> values= Arrays.stream(VisitorEnum.values())
			.collect(Collectors.toMap(VisitorEnum::getValue, Function.identity()));

	/**
	 *
	 * @param nodetype - node type
	 * @return - corresponding VistorEnum
	 */
	public static VisitorEnum fromNodetype(final int nodetype) {
		return values.get(nodetype);
	}

	/**
	 *
	 * @param node - ASTNode
	 * @return - corresponding VistorEnum
	 */
	public static VisitorEnum fromNode(ASTNode node) {
		return fromNodetype(node.getNodeType());
	}
}
