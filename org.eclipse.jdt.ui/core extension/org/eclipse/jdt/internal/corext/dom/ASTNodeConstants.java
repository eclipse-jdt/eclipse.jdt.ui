package org.eclipse.jdt.internal.corext.dom;

import java.util.List;
import org.eclipse.jdt.core.dom.*;

public class ASTNodeConstants {
	/**
	 * Property for nodes of type
	 * <code>ClassInstanceCreation</code>.
	 */
	public static final int ANONYMOUS_CLASS_DECLARATION = 1;

	/**
	 * Property for nodes of type
	 * <code>ClassInstanceCreation</code>,
	 * <code>ConstructorInvocation</code>,
	 * <code>MethodInvocation</code>,
	 * <code>SuperConstructorInvocation</code>,
	 * <code>SuperMethodInvocation</code>.
	 */
	public static final int ARGUMENTS = 2;

	/**
	 * Property for nodes of type
	 * <code>ArrayAccess</code>.
	 */
	public static final int ARRAY = 3;

	/**
	 * Property for nodes of type
	 * <code>CatchClause</code>,
	 * <code>DoStatement</code>,
	 * <code>ForStatement</code>,
	 * <code>Initializer</code>,
	 * <code>LabeledStatement</code>,
	 * <code>MethodDeclaration</code>,
	 * <code>SynchronizedStatement</code>,
	 * <code>TryStatement</code>,
	 * <code>WhileStatement</code>.
	 */
	public static final int BODY = 4;

	/**
	 * Property for nodes of type
	 * <code>AnonymousClassDeclaration</code>,
	 * <code>TypeDeclaration</code>.
	 */
	public static final int BODY_DECLARATIONS = 5;

	/**
	 * Property for nodes of type
	 * <code>TryStatement</code>.
	 */
	public static final int CATCH_CLAUSES = 6;

	/**
	 * Property for nodes of type
	 * <code>Javadoc</code>.
	 */
	public static final int COMMENT = 7;

	/**
	 * Property for nodes of type
	 * <code>ArrayType</code>.
	 */
	public static final int COMPONENT_TYPE = 8;

	/**
	 * Property for nodes of type
	 * <code>ArrayCreation</code>.
	 */
	public static final int DIMENSIONS = 9;

	/**
	 * Property for nodes of type
	 * <code>ConditionalExpression</code>.
	 */
	public static final int ELSE_EXPRESSION = 10;

	/**
	 * Property for nodes of type
	 * <code>IfStatement</code>.
	 */
	public static final int ELSE_STATEMENT = 11;

	/**
	 * Property for nodes of type
	 * <code>CharacterLiteral</code>,
	 * <code>StringLiteral</code>.
	 */
	public static final int ESCAPED_VALUE = 12;

	/**
	 * Property for nodes of type
	 * <code>CatchClause</code>.
	 */
	public static final int EXCEPTION = 13;

	/**
	 * Property for nodes of type
	 * <code>AssertStatement</code>,
	 * <code>CastExpression</code>,
	 * <code>ClassInstanceCreation</code>,
	 * <code>ConditionalExpression</code>,
	 * <code>DoStatement</code>,
	 * <code>ExpressionStatement</code>,
	 * <code>FieldAccess</code>,
	 * <code>ForStatement</code>,
	 * <code>IfStatement</code>,
	 * <code>MethodInvocation</code>,
	 * <code>ParenthesizedExpression</code>,
	 * <code>ReturnStatement</code>,
	 * <code>SuperConstructorInvocation</code>,
	 * <code>SwitchCase</code>,
	 * <code>SwitchStatement</code>,
	 * <code>SynchronizedStatement</code>,
	 * <code>ThrowStatement</code>,
	 * <code>WhileStatement</code>.
	 */
	public static final int EXPRESSION = 14;

	/**
	 * Property for nodes of type
	 * <code>ArrayInitializer</code>.
	 */
	public static final int EXPRESSIONS = 15;

	/**
	 * Property for nodes of type
	 * <code>InfixExpression</code>.
	 */
	public static final int EXTENDED_OPERANDS = 16;

	/**
	 * Property for nodes of type
	 * <code>MethodDeclaration</code>,
	 * <code>SingleVariableDeclaration</code>,
	 * <code>VariableDeclarationFragment</code>.
	 */
	public static final int EXTRA_DIMENSIONS = 17;

	/**
	 * Property for nodes of type
	 * <code>TryStatement</code>.
	 */
	public static final int FINALLY = 18;

	/**
	 * Property for nodes of type
	 * <code>FieldDeclaration</code>,
	 * <code>VariableDeclarationExpression</code>,
	 * <code>VariableDeclarationStatement</code>.
	 */
	public static final int FRAGMENTS = 19;

	/**
	 * Property for nodes of type
	 * <code>SimpleName</code>.
	 */
	public static final int IDENTIFIER = 20;

	/**
	 * Property for nodes of type
	 * <code>CompilationUnit</code>.
	 */
	public static final int IMPORTS = 21;

	/**
	 * Property for nodes of type
	 * <code>ArrayAccess</code>.
	 */
	public static final int INDEX = 22;

	/**
	 * Property for nodes of type
	 * <code>ArrayCreation</code>,
	 * <code>SingleVariableDeclaration</code>,
	 * <code>VariableDeclarationFragment</code>.
	 */
	public static final int INITIALIZER = 23;

	/**
	 * Property for nodes of type
	 * <code>ForStatement</code>.
	 */
	public static final int INITIALIZERS = 24;

	/**
	 * Property for nodes of type
	 * <code>MethodDeclaration</code>.
	 */
	public static final int IS_CONSTRUCTOR = 25;

	/**
	 * Property for nodes of type
	 * <code>TypeDeclaration</code>.
	 */
	public static final int IS_INTERFACE = 26;

	/**
	 * Property for nodes of type
	 * <code>ImportDeclaration</code>.
	 */
	public static final int IS_ON_DEMAND = 27;

	/**
	 * Property for nodes of type
	 * <code>FieldDeclaration</code>,
	 * <code>Initializer</code>,
	 * <code>MethodDeclaration</code>,
	 * <code>TypeDeclaration</code>.
	 */
	public static final int JAVADOC = 28;

	/**
	 * Property for nodes of type
	 * <code>BreakStatement</code>,
	 * <code>ContinueStatement</code>,
	 * <code>LabeledStatement</code>.
	 */
	public static final int LABEL = 29;

	/**
	 * Property for nodes of type
	 * <code>Assignment</code>.
	 */
	public static final int LEFT_HAND_SIDE = 30;

	/**
	 * Property for nodes of type
	 * <code>InfixExpression</code>,
	 * <code>InstanceofExpression</code>.
	 */
	public static final int LEFT_OPERAND = 31;

	/**
	 * Property for nodes of type
	 * <code>StringLiteral</code>.
	 */
	public static final int LITERAL_VALUE = 32;

	/**
	 * Property for nodes of type
	 * <code>AssertStatement</code>.
	 */
	public static final int MESSAGE = 33;

	/**
	 * Property for nodes of type
	 * <code>FieldDeclaration</code>,
	 * <code>Initializer</code>,
	 * <code>MethodDeclaration</code>,
	 * <code>SingleVariableDeclaration</code>,
	 * <code>TypeDeclaration</code>,
	 * <code>VariableDeclarationExpression</code>,
	 * <code>VariableDeclarationStatement</code>.
	 */
	public static final int MODIFIERS = 34;

	/**
	 * Property for nodes of type
	 * <code>ClassInstanceCreation</code>,
	 * <code>FieldAccess</code>,
	 * <code>ImportDeclaration</code>,
	 * <code>MethodDeclaration</code>,
	 * <code>MethodInvocation</code>,
	 * <code>PackageDeclaration</code>,
	 * <code>QualifiedName</code>,
	 * <code>SimpleType</code>,
	 * <code>SingleVariableDeclaration</code>,
	 * <code>SuperFieldAccess</code>,
	 * <code>SuperMethodInvocation</code>,
	 * <code>TypeDeclaration</code>,
	 * <code>VariableDeclarationFragment</code>.
	 */
	public static final int NAME = 35;

	/**
	 * Property for nodes of type
	 * <code>PostfixExpression</code>,
	 * <code>PrefixExpression</code>.
	 */
	public static final int OPERAND = 36;

	/**
	 * Property for nodes of type
	 * <code>Assignment</code>,
	 * <code>InfixExpression</code>,
	 * <code>PostfixExpression</code>,
	 * <code>PrefixExpression</code>.
	 */
	public static final int OPERATOR = 37;

	/**
	 * Property for nodes of type
	 * <code>CompilationUnit</code>.
	 */
	public static final int PACKAGE = 38;

	/**
	 * Property for nodes of type
	 * <code>MethodDeclaration</code>.
	 */
	public static final int PARAMETERS = 39;

	/**
	 * Property for nodes of type
	 * <code>PrimitiveType</code>.
	 */
	public static final int PRIMITIVE_TYPE_CODE = 40;

	/**
	 * Property for nodes of type
	 * <code>QualifiedName</code>,
	 * <code>SuperFieldAccess</code>,
	 * <code>SuperMethodInvocation</code>,
	 * <code>ThisExpression</code>.
	 */
	public static final int QUALIFIER = 41;

	/**
	 * Property for nodes of type
	 * <code>MethodDeclaration</code>.
	 */
	public static final int RETURN_TYPE = 42;

	/**
	 * Property for nodes of type
	 * <code>Assignment</code>.
	 */
	public static final int RIGHT_HAND_SIDE = 43;

	/**
	 * Property for nodes of type
	 * <code>InfixExpression</code>,
	 * <code>InstanceofExpression</code>.
	 */
	public static final int RIGHT_OPERAND = 44;

	/**
	 * Property for nodes of type
	 * <code>Block</code>,
	 * <code>SwitchStatement</code>.
	 */
	public static final int STATEMENTS = 45;

	/**
	 * Property for nodes of type
	 * <code>TypeDeclaration</code>.
	 */
	public static final int SUPER_INTERFACES = 46;

	/**
	 * Property for nodes of type
	 * <code>TypeDeclaration</code>.
	 */
	public static final int SUPERCLASS = 47;

	/**
	 * Property for nodes of type
	 * <code>ConditionalExpression</code>.
	 */
	public static final int THEN_EXPRESSION = 48;

	/**
	 * Property for nodes of type
	 * <code>IfStatement</code>.
	 */
	public static final int THEN_STATEMENT = 49;

	/**
	 * Property for nodes of type
	 * <code>MethodDeclaration</code>.
	 */
	public static final int THROWN_EXCEPTIONS = 50;

	/**
	 * Property for nodes of type
	 * <code>NumberLiteral</code>.
	 */
	public static final int TOKEN = 51;

	/**
	 * Property for nodes of type
	 * <code>ArrayCreation</code>,
	 * <code>CastExpression</code>,
	 * <code>FieldDeclaration</code>,
	 * <code>SingleVariableDeclaration</code>,
	 * <code>TypeLiteral</code>,
	 * <code>VariableDeclarationExpression</code>,
	 * <code>VariableDeclarationStatement</code>.
	 */
	public static final int TYPE = 52;

	/**
	 * Property for nodes of type
	 * <code>TypeDeclarationStatement</code>.
	 */
	public static final int TYPE_DECLARATION = 53;

	/**
	 * Property for nodes of type
	 * <code>CompilationUnit</code>.
	 */
	public static final int TYPES = 54;

	/**
	 * Property for nodes of type
	 * <code>ForStatement</code>.
	 */
	public static final int UPDATERS = 55;

	/**
	 *Returns <code>true</code> if a node has the given property.
	 */
	public static boolean hasChildProperty(ASTNode node, int property) {
		switch (node.getNodeType()) {
			case ASTNode.ANONYMOUS_CLASS_DECLARATION :
				return property == BODY_DECLARATIONS;
			case ASTNode.ARRAY_ACCESS :
				return (property == ARRAY) || (property == INDEX);
			case ASTNode.ARRAY_CREATION :
				return (property == DIMENSIONS) || (property == INITIALIZER) || (property == TYPE);
			case ASTNode.ARRAY_INITIALIZER :
				return property == EXPRESSIONS;
			case ASTNode.ARRAY_TYPE :
				return property == COMPONENT_TYPE;
			case ASTNode.ASSERT_STATEMENT :
				return (property == EXPRESSION) || (property == MESSAGE);
			case ASTNode.ASSIGNMENT :
				return (property == LEFT_HAND_SIDE) || (property == OPERATOR) || (property == RIGHT_HAND_SIDE);
			case ASTNode.BLOCK :
				return property == STATEMENTS;
			case ASTNode.BREAK_STATEMENT :
				return property == LABEL;
			case ASTNode.CAST_EXPRESSION :
				return (property == EXPRESSION) || (property == TYPE);
			case ASTNode.CATCH_CLAUSE :
				return (property == BODY) || (property == EXCEPTION);
			case ASTNode.CHARACTER_LITERAL :
				return property == ESCAPED_VALUE;
			case ASTNode.CLASS_INSTANCE_CREATION :
				switch (property) {
					case ANONYMOUS_CLASS_DECLARATION :
					case ARGUMENTS :
					case EXPRESSION :
					case NAME :
						return true;
				}
				return false;
			case ASTNode.COMPILATION_UNIT :
				return (property == IMPORTS) || (property == PACKAGE) || (property == TYPES);
			case ASTNode.CONDITIONAL_EXPRESSION :
				return (property == ELSE_EXPRESSION) || (property == EXPRESSION) || (property == THEN_EXPRESSION);
			case ASTNode.CONSTRUCTOR_INVOCATION :
				return property == ARGUMENTS;
			case ASTNode.CONTINUE_STATEMENT :
				return property == LABEL;
			case ASTNode.DO_STATEMENT :
				return (property == BODY) || (property == EXPRESSION);
			case ASTNode.EXPRESSION_STATEMENT :
				return property == EXPRESSION;
			case ASTNode.FIELD_ACCESS :
				return (property == EXPRESSION) || (property == NAME);
			case ASTNode.FIELD_DECLARATION :
				switch (property) {
					case FRAGMENTS :
					case JAVADOC :
					case MODIFIERS :
					case TYPE :
						return true;
				}
				return false;
			case ASTNode.FOR_STATEMENT :
				switch (property) {
					case BODY :
					case EXPRESSION :
					case INITIALIZERS :
					case UPDATERS :
						return true;
				}
				return false;
			case ASTNode.IF_STATEMENT :
				return (property == ELSE_STATEMENT) || (property == EXPRESSION) || (property == THEN_STATEMENT);
			case ASTNode.IMPORT_DECLARATION :
				return (property == IS_ON_DEMAND) || (property == NAME);
			case ASTNode.INFIX_EXPRESSION :
				switch (property) {
					case EXTENDED_OPERANDS :
					case LEFT_OPERAND :
					case OPERATOR :
					case RIGHT_OPERAND :
						return true;
				}
				return false;
			case ASTNode.INITIALIZER :
				return (property == BODY) || (property == JAVADOC) || (property == MODIFIERS);
			case ASTNode.INSTANCEOF_EXPRESSION :
				return (property == LEFT_OPERAND) || (property == RIGHT_OPERAND);
			case ASTNode.JAVADOC :
				return property == COMMENT;
			case ASTNode.LABELED_STATEMENT :
				return (property == BODY) || (property == LABEL);
			case ASTNode.METHOD_DECLARATION :
				switch (property) {
					case BODY :
					case EXTRA_DIMENSIONS :
					case IS_CONSTRUCTOR :
					case JAVADOC :
					case MODIFIERS :
					case NAME :
					case PARAMETERS :
					case RETURN_TYPE :
					case THROWN_EXCEPTIONS :
						return true;
				}
				return false;
			case ASTNode.METHOD_INVOCATION :
				return (property == ARGUMENTS) || (property == EXPRESSION) || (property == NAME);
			case ASTNode.NUMBER_LITERAL :
				return property == TOKEN;
			case ASTNode.PACKAGE_DECLARATION :
				return property == NAME;
			case ASTNode.PARENTHESIZED_EXPRESSION :
				return property == EXPRESSION;
			case ASTNode.POSTFIX_EXPRESSION :
				return (property == OPERAND) || (property == OPERATOR);
			case ASTNode.PREFIX_EXPRESSION :
				return (property == OPERAND) || (property == OPERATOR);
			case ASTNode.PRIMITIVE_TYPE :
				return property == PRIMITIVE_TYPE_CODE;
			case ASTNode.QUALIFIED_NAME :
				return (property == NAME) || (property == QUALIFIER);
			case ASTNode.RETURN_STATEMENT :
				return property == EXPRESSION;
			case ASTNode.SIMPLE_NAME :
				return property == IDENTIFIER;
			case ASTNode.SIMPLE_TYPE :
				return property == NAME;
			case ASTNode.SINGLE_VARIABLE_DECLARATION :
				switch (property) {
					case EXTRA_DIMENSIONS :
					case INITIALIZER :
					case MODIFIERS :
					case NAME :
					case TYPE :
						return true;
				}
				return false;
			case ASTNode.STRING_LITERAL :
				return (property == ESCAPED_VALUE) || (property == LITERAL_VALUE);
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
				return (property == ARGUMENTS) || (property == EXPRESSION);
			case ASTNode.SUPER_FIELD_ACCESS :
				return (property == NAME) || (property == QUALIFIER);
			case ASTNode.SUPER_METHOD_INVOCATION :
				return (property == ARGUMENTS) || (property == NAME) || (property == QUALIFIER);
			case ASTNode.SWITCH_CASE :
				return property == EXPRESSION;
			case ASTNode.SWITCH_STATEMENT :
				return (property == EXPRESSION) || (property == STATEMENTS);
			case ASTNode.SYNCHRONIZED_STATEMENT :
				return (property == BODY) || (property == EXPRESSION);
			case ASTNode.THIS_EXPRESSION :
				return property == QUALIFIER;
			case ASTNode.THROW_STATEMENT :
				return property == EXPRESSION;
			case ASTNode.TRY_STATEMENT :
				return (property == BODY) || (property == CATCH_CLAUSES) || (property == FINALLY);
			case ASTNode.TYPE_DECLARATION :
				switch (property) {
					case BODY_DECLARATIONS :
					case IS_INTERFACE :
					case JAVADOC :
					case MODIFIERS :
					case NAME :
					case SUPER_INTERFACES :
					case SUPERCLASS :
						return true;
				}
				return false;
			case ASTNode.TYPE_DECLARATION_STATEMENT :
				return property == TYPE_DECLARATION;
			case ASTNode.TYPE_LITERAL :
				return property == TYPE;
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
				return (property == FRAGMENTS) || (property == MODIFIERS) || (property == TYPE);
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
				return (property == EXTRA_DIMENSIONS) || (property == INITIALIZER) || (property == NAME);
			case ASTNode.VARIABLE_DECLARATION_STATEMENT :
				return (property == FRAGMENTS) || (property == MODIFIERS) || (property == TYPE);
			case ASTNode.WHILE_STATEMENT :
				return (property == BODY) || (property == EXPRESSION);
		}
		return false;
	}

	/**
	  * Returns <code>true</code> if property of a node is a list property.
	 */
	public static boolean isListProperty(int property) {
		switch (property) {
			case ARGUMENTS :
			case BODY_DECLARATIONS :
			case CATCH_CLAUSES :
			case DIMENSIONS :
			case EXPRESSIONS :
			case EXTENDED_OPERANDS :
			case FRAGMENTS :
			case IMPORTS :
			case INITIALIZERS :
			case PARAMETERS :
			case STATEMENTS :
			case SUPER_INTERFACES :
			case THROWN_EXCEPTIONS :
			case TYPES :
			case UPDATERS :
				return true;
		}
		return false;
	}

	/**
	  * Returns <code>true</code> if property of a node is an attribute property (Not a List and not an ASTNode).
	 */
	public static boolean isAttributeProperty(int property) {
		switch (property) {
			case COMMENT :
			case ESCAPED_VALUE :
			case EXTRA_DIMENSIONS :
			case IDENTIFIER :
			case IS_CONSTRUCTOR :
			case IS_INTERFACE :
			case IS_ON_DEMAND :
			case LITERAL_VALUE :
			case MODIFIERS :
			case OPERATOR :
			case PRIMITIVE_TYPE_CODE :
			case TOKEN :
				return true;
		}
		return false;
	}

	/**
	 * Gets a property in a list.
	 */
	public static ASTNode getNodeChild(ASTNode node, int property, int index) {
		if (!isListProperty(property)) {
			throw new IllegalArgumentException();
		}
		return (ASTNode) ((List) getNodeChild(node, property)).get(index);
	}

	/**
	 * Gets a ASTNode child by the property id. Booleans and integer attributes are returned boxed.
	 */
	public static Object getNodeChild(ASTNode node, int property) {
		switch (node.getNodeType()) {
			case ASTNode.ANONYMOUS_CLASS_DECLARATION :
				if (property == BODY_DECLARATIONS) {
					return ((AnonymousClassDeclaration) node).bodyDeclarations();
				}
				break;
			case ASTNode.ARRAY_ACCESS :
				switch (property) {
					case ARRAY :
						return ((ArrayAccess) node).getArray();
					case INDEX :
						return ((ArrayAccess) node).getIndex();
				}
				break;
			case ASTNode.ARRAY_CREATION :
				switch (property) {
					case DIMENSIONS :
						return ((ArrayCreation) node).dimensions();
					case INITIALIZER :
						return ((ArrayCreation) node).getInitializer();
					case TYPE :
						return ((ArrayCreation) node).getType();
				}
				break;
			case ASTNode.ARRAY_INITIALIZER :
				if (property == EXPRESSIONS) {
					return ((ArrayInitializer) node).expressions();
				}
				break;
			case ASTNode.ARRAY_TYPE :
				if (property == COMPONENT_TYPE) {
					return ((ArrayType) node).getComponentType();
				}
				break;
			case ASTNode.ASSERT_STATEMENT :
				switch (property) {
					case EXPRESSION :
						return ((AssertStatement) node).getExpression();
					case MESSAGE :
						return ((AssertStatement) node).getMessage();
				}
				break;
			case ASTNode.ASSIGNMENT :
				switch (property) {
					case LEFT_HAND_SIDE :
						return ((Assignment) node).getLeftHandSide();
					case OPERATOR :
						return ((Assignment) node).getOperator();
					case RIGHT_HAND_SIDE :
						return ((Assignment) node).getRightHandSide();
				}
				break;
			case ASTNode.BLOCK :
				if (property == STATEMENTS) {
					return ((Block) node).statements();
				}
				break;
			case ASTNode.BREAK_STATEMENT :
				if (property == LABEL) {
					return ((BreakStatement) node).getLabel();
				}
				break;
			case ASTNode.CAST_EXPRESSION :
				switch (property) {
					case EXPRESSION :
						return ((CastExpression) node).getExpression();
					case TYPE :
						return ((CastExpression) node).getType();
				}
				break;
			case ASTNode.CATCH_CLAUSE :
				switch (property) {
					case BODY :
						return ((CatchClause) node).getBody();
					case EXCEPTION :
						return ((CatchClause) node).getException();
				}
				break;
			case ASTNode.CHARACTER_LITERAL :
				if (property == ESCAPED_VALUE) {
					return ((CharacterLiteral) node).getEscapedValue();
				}
				break;
			case ASTNode.CLASS_INSTANCE_CREATION :
				switch (property) {
					case ARGUMENTS :
						return ((ClassInstanceCreation) node).arguments();
					case ANONYMOUS_CLASS_DECLARATION :
						return ((ClassInstanceCreation) node).getAnonymousClassDeclaration();
					case EXPRESSION :
						return ((ClassInstanceCreation) node).getExpression();
					case NAME :
						return ((ClassInstanceCreation) node).getName();
				}
				break;
			case ASTNode.COMPILATION_UNIT :
				switch (property) {
					case PACKAGE :
						return ((CompilationUnit) node).getPackage();
					case IMPORTS :
						return ((CompilationUnit) node).imports();
					case TYPES :
						return ((CompilationUnit) node).types();
				}
				break;
			case ASTNode.CONDITIONAL_EXPRESSION :
				switch (property) {
					case ELSE_EXPRESSION :
						return ((ConditionalExpression) node).getElseExpression();
					case EXPRESSION :
						return ((ConditionalExpression) node).getExpression();
					case THEN_EXPRESSION :
						return ((ConditionalExpression) node).getThenExpression();
				}
				break;
			case ASTNode.CONSTRUCTOR_INVOCATION :
				if (property == ARGUMENTS) {
					return ((ConstructorInvocation) node).arguments();
				}
				break;
			case ASTNode.CONTINUE_STATEMENT :
				if (property == LABEL) {
					return ((ContinueStatement) node).getLabel();
				}
				break;
			case ASTNode.DO_STATEMENT :
				switch (property) {
					case BODY :
						return ((DoStatement) node).getBody();
					case EXPRESSION :
						return ((DoStatement) node).getExpression();
				}
				break;
			case ASTNode.EXPRESSION_STATEMENT :
				if (property == EXPRESSION) {
					return ((ExpressionStatement) node).getExpression();
				}
				break;
			case ASTNode.FIELD_ACCESS :
				switch (property) {
					case EXPRESSION :
						return ((FieldAccess) node).getExpression();
					case NAME :
						return ((FieldAccess) node).getName();
				}
				break;
			case ASTNode.FIELD_DECLARATION :
				switch (property) {
					case FRAGMENTS :
						return ((FieldDeclaration) node).fragments();
					case JAVADOC :
						return ((FieldDeclaration) node).getJavadoc();
					case MODIFIERS :
						return new Integer(((FieldDeclaration) node).getModifiers());
					case TYPE :
						return ((FieldDeclaration) node).getType();
				}
				break;
			case ASTNode.FOR_STATEMENT :
				switch (property) {
					case BODY :
						return ((ForStatement) node).getBody();
					case EXPRESSION :
						return ((ForStatement) node).getExpression();
					case INITIALIZERS :
						return ((ForStatement) node).initializers();
					case UPDATERS :
						return ((ForStatement) node).updaters();
				}
				break;
			case ASTNode.IF_STATEMENT :
				switch (property) {
					case ELSE_STATEMENT :
						return ((IfStatement) node).getElseStatement();
					case EXPRESSION :
						return ((IfStatement) node).getExpression();
					case THEN_STATEMENT :
						return ((IfStatement) node).getThenStatement();
				}
				break;
			case ASTNode.IMPORT_DECLARATION :
				switch (property) {
					case NAME :
						return ((ImportDeclaration) node).getName();
					case IS_ON_DEMAND :
						return new Boolean(((ImportDeclaration) node).isOnDemand());
				}
				break;
			case ASTNode.INFIX_EXPRESSION :
				switch (property) {
					case EXTENDED_OPERANDS :
						return ((InfixExpression) node).extendedOperands();
					case LEFT_OPERAND :
						return ((InfixExpression) node).getLeftOperand();
					case OPERATOR :
						return ((InfixExpression) node).getOperator();
					case RIGHT_OPERAND :
						return ((InfixExpression) node).getRightOperand();
				}
				break;
			case ASTNode.INITIALIZER :
				switch (property) {
					case BODY :
						return ((Initializer) node).getBody();
					case JAVADOC :
						return ((Initializer) node).getJavadoc();
					case MODIFIERS :
						return new Integer(((Initializer) node).getModifiers());
				}
				break;
			case ASTNode.INSTANCEOF_EXPRESSION :
				switch (property) {
					case LEFT_OPERAND :
						return ((InstanceofExpression) node).getLeftOperand();
					case RIGHT_OPERAND :
						return ((InstanceofExpression) node).getRightOperand();
				}
				break;
			case ASTNode.JAVADOC :
				if (property == COMMENT) {
					return ((Javadoc) node).getComment();
				}
				break;
			case ASTNode.LABELED_STATEMENT :
				switch (property) {
					case BODY :
						return ((LabeledStatement) node).getBody();
					case LABEL :
						return ((LabeledStatement) node).getLabel();
				}
				break;
			case ASTNode.METHOD_DECLARATION :
				switch (property) {
					case BODY :
						return ((MethodDeclaration) node).getBody();
					case EXTRA_DIMENSIONS :
						return new Integer(((MethodDeclaration) node).getExtraDimensions());
					case JAVADOC :
						return ((MethodDeclaration) node).getJavadoc();
					case MODIFIERS :
						return new Integer(((MethodDeclaration) node).getModifiers());
					case NAME :
						return ((MethodDeclaration) node).getName();
					case RETURN_TYPE :
						return ((MethodDeclaration) node).getReturnType();
					case IS_CONSTRUCTOR :
						return new Boolean(((MethodDeclaration) node).isConstructor());
					case PARAMETERS :
						return ((MethodDeclaration) node).parameters();
					case THROWN_EXCEPTIONS :
						return ((MethodDeclaration) node).thrownExceptions();
				}
				break;
			case ASTNode.METHOD_INVOCATION :
				switch (property) {
					case ARGUMENTS :
						return ((MethodInvocation) node).arguments();
					case EXPRESSION :
						return ((MethodInvocation) node).getExpression();
					case NAME :
						return ((MethodInvocation) node).getName();
				}
				break;
			case ASTNode.NUMBER_LITERAL :
				if (property == TOKEN) {
					return ((NumberLiteral) node).getToken();
				}
				break;
			case ASTNode.PACKAGE_DECLARATION :
				if (property == NAME) {
					return ((PackageDeclaration) node).getName();
				}
				break;
			case ASTNode.PARENTHESIZED_EXPRESSION :
				if (property == EXPRESSION) {
					return ((ParenthesizedExpression) node).getExpression();
				}
				break;
			case ASTNode.POSTFIX_EXPRESSION :
				switch (property) {
					case OPERAND :
						return ((PostfixExpression) node).getOperand();
					case OPERATOR :
						return ((PostfixExpression) node).getOperator();
				}
				break;
			case ASTNode.PREFIX_EXPRESSION :
				switch (property) {
					case OPERAND :
						return ((PrefixExpression) node).getOperand();
					case OPERATOR :
						return ((PrefixExpression) node).getOperator();
				}
				break;
			case ASTNode.PRIMITIVE_TYPE :
				if (property == PRIMITIVE_TYPE_CODE) {
					return ((PrimitiveType) node).getPrimitiveTypeCode();
				}
				break;
			case ASTNode.QUALIFIED_NAME :
				switch (property) {
					case NAME :
						return ((QualifiedName) node).getName();
					case QUALIFIER :
						return ((QualifiedName) node).getQualifier();
				}
				break;
			case ASTNode.RETURN_STATEMENT :
				if (property == EXPRESSION) {
					return ((ReturnStatement) node).getExpression();
				}
				break;
			case ASTNode.SIMPLE_NAME :
				if (property == IDENTIFIER) {
					return ((SimpleName) node).getIdentifier();
				}
				break;
			case ASTNode.SIMPLE_TYPE :
				if (property == NAME) {
					return ((SimpleType) node).getName();
				}
				break;
			case ASTNode.SINGLE_VARIABLE_DECLARATION :
				switch (property) {
					case EXTRA_DIMENSIONS :
						return new Integer(((SingleVariableDeclaration) node).getExtraDimensions());
					case INITIALIZER :
						return ((SingleVariableDeclaration) node).getInitializer();
					case MODIFIERS :
						return new Integer(((SingleVariableDeclaration) node).getModifiers());
					case NAME :
						return ((SingleVariableDeclaration) node).getName();
					case TYPE :
						return ((SingleVariableDeclaration) node).getType();
				}
				break;
			case ASTNode.STRING_LITERAL :
				switch (property) {
					case ESCAPED_VALUE :
						return ((StringLiteral) node).getEscapedValue();
					case LITERAL_VALUE :
						return ((StringLiteral) node).getLiteralValue();
				}
				break;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
				switch (property) {
					case ARGUMENTS :
						return ((SuperConstructorInvocation) node).arguments();
					case EXPRESSION :
						return ((SuperConstructorInvocation) node).getExpression();
				}
				break;
			case ASTNode.SUPER_FIELD_ACCESS :
				switch (property) {
					case NAME :
						return ((SuperFieldAccess) node).getName();
					case QUALIFIER :
						return ((SuperFieldAccess) node).getQualifier();
				}
				break;
			case ASTNode.SUPER_METHOD_INVOCATION :
				switch (property) {
					case ARGUMENTS :
						return ((SuperMethodInvocation) node).arguments();
					case NAME :
						return ((SuperMethodInvocation) node).getName();
					case QUALIFIER :
						return ((SuperMethodInvocation) node).getQualifier();
				}
				break;
			case ASTNode.SWITCH_CASE :
				if (property == EXPRESSION) {
					return ((SwitchCase) node).getExpression();
				}
				break;
			case ASTNode.SWITCH_STATEMENT :
				switch (property) {
					case EXPRESSION :
						return ((SwitchStatement) node).getExpression();
					case STATEMENTS :
						return ((SwitchStatement) node).statements();
				}
				break;
			case ASTNode.SYNCHRONIZED_STATEMENT :
				switch (property) {
					case BODY :
						return ((SynchronizedStatement) node).getBody();
					case EXPRESSION :
						return ((SynchronizedStatement) node).getExpression();
				}
				break;
			case ASTNode.THIS_EXPRESSION :
				if (property == QUALIFIER) {
					return ((ThisExpression) node).getQualifier();
				}
				break;
			case ASTNode.THROW_STATEMENT :
				if (property == EXPRESSION) {
					return ((ThrowStatement) node).getExpression();
				}
				break;
			case ASTNode.TRY_STATEMENT :
				switch (property) {
					case CATCH_CLAUSES :
						return ((TryStatement) node).catchClauses();
					case BODY :
						return ((TryStatement) node).getBody();
					case FINALLY :
						return ((TryStatement) node).getFinally();
				}
				break;
			case ASTNode.TYPE_DECLARATION :
				switch (property) {
					case BODY_DECLARATIONS :
						return ((TypeDeclaration) node).bodyDeclarations();
					case JAVADOC :
						return ((TypeDeclaration) node).getJavadoc();
					case MODIFIERS :
						return new Integer(((TypeDeclaration) node).getModifiers());
					case NAME :
						return ((TypeDeclaration) node).getName();
					case SUPERCLASS :
						return ((TypeDeclaration) node).getSuperclass();
					case IS_INTERFACE :
						return new Boolean(((TypeDeclaration) node).isInterface());
					case SUPER_INTERFACES :
						return ((TypeDeclaration) node).superInterfaces();
				}
				break;
			case ASTNode.TYPE_DECLARATION_STATEMENT :
				if (property == TYPE_DECLARATION) {
					return ((TypeDeclarationStatement) node).getTypeDeclaration();
				}
				break;
			case ASTNode.TYPE_LITERAL :
				if (property == TYPE) {
					return ((TypeLiteral) node).getType();
				}
				break;
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
				switch (property) {
					case FRAGMENTS :
						return ((VariableDeclarationExpression) node).fragments();
					case MODIFIERS :
						return new Integer(((VariableDeclarationExpression) node).getModifiers());
					case TYPE :
						return ((VariableDeclarationExpression) node).getType();
				}
				break;
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
				switch (property) {
					case EXTRA_DIMENSIONS :
						return new Integer(((VariableDeclarationFragment) node).getExtraDimensions());
					case INITIALIZER :
						return ((VariableDeclarationFragment) node).getInitializer();
					case NAME :
						return ((VariableDeclarationFragment) node).getName();
				}
				break;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT :
				switch (property) {
					case FRAGMENTS :
						return ((VariableDeclarationStatement) node).fragments();
					case MODIFIERS :
						return new Integer(((VariableDeclarationStatement) node).getModifiers());
					case TYPE :
						return ((VariableDeclarationStatement) node).getType();
				}
				break;
			case ASTNode.WHILE_STATEMENT :
				switch (property) {
					case BODY :
						return ((WhileStatement) node).getBody();
					case EXPRESSION :
						return ((WhileStatement) node).getExpression();
				}
				break;
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Gets the child property a node is located at its parent.
	 */
	public static int getPropertyOfNode(ASTNode node) {
		ASTNode parent = node.getParent();
		if (parent == null) {
			throw new IllegalArgumentException();
		}
		switch (parent.getNodeType()) {
			case ASTNode.ANONYMOUS_CLASS_DECLARATION :
				return BODY_DECLARATIONS;
			case ASTNode.ARRAY_ACCESS :
				if (((ArrayAccess) parent).getArray() == node)
					return ARRAY;
				return INDEX;
			case ASTNode.ARRAY_CREATION :
				ArrayCreation arrayCreation = (ArrayCreation) parent;
				if (arrayCreation.getInitializer() == node)
					return INITIALIZER;
				if (arrayCreation.getType() == node)
					return TYPE;
				return DIMENSIONS;
			case ASTNode.ARRAY_INITIALIZER :
				return EXPRESSIONS;
			case ASTNode.ARRAY_TYPE :
				return COMPONENT_TYPE;
			case ASTNode.ASSERT_STATEMENT :
				if (((AssertStatement) parent).getExpression() == node)
					return EXPRESSION;
				return MESSAGE;
			case ASTNode.ASSIGNMENT :
				if (((Assignment) parent).getLeftHandSide() == node)
					return LEFT_HAND_SIDE;
				return RIGHT_HAND_SIDE;
			case ASTNode.BLOCK :
				return STATEMENTS;
			case ASTNode.BREAK_STATEMENT :
				return LABEL;
			case ASTNode.CAST_EXPRESSION :
				if (((CastExpression) parent).getExpression() == node)
					return EXPRESSION;
				return TYPE;
			case ASTNode.CATCH_CLAUSE :
				if (((CatchClause) parent).getBody() == node)
					return BODY;
				return EXCEPTION;
			case ASTNode.CLASS_INSTANCE_CREATION :
				ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) parent;
				if (classInstanceCreation.getAnonymousClassDeclaration() == node)
					return ANONYMOUS_CLASS_DECLARATION;
				if (classInstanceCreation.getExpression() == node)
					return EXPRESSION;
				if (classInstanceCreation.getName() == node)
					return NAME;
				return ARGUMENTS;
			case ASTNode.COMPILATION_UNIT :
				CompilationUnit compilationUnit = (CompilationUnit) parent;
				if (compilationUnit.getPackage() == node)
					return PACKAGE;
				if (compilationUnit.imports().contains(node))
					return IMPORTS;
				return TYPES;
			case ASTNode.CONDITIONAL_EXPRESSION :
				ConditionalExpression conditionalExpression = (ConditionalExpression) parent;
				if (conditionalExpression.getElseExpression() == node)
					return ELSE_EXPRESSION;
				if (conditionalExpression.getExpression() == node)
					return EXPRESSION;
				return THEN_EXPRESSION;
			case ASTNode.CONSTRUCTOR_INVOCATION :
				return ARGUMENTS;
			case ASTNode.CONTINUE_STATEMENT :
				return LABEL;
			case ASTNode.DO_STATEMENT :
				if (((DoStatement) parent).getBody() == node)
					return BODY;
				return EXPRESSION;
			case ASTNode.EXPRESSION_STATEMENT :
				return EXPRESSION;
			case ASTNode.FIELD_ACCESS :
				if (((FieldAccess) parent).getExpression() == node)
					return EXPRESSION;
				return NAME;
			case ASTNode.FIELD_DECLARATION :
				FieldDeclaration fieldDeclaration = (FieldDeclaration) parent;
				if (fieldDeclaration.getJavadoc() == node)
					return JAVADOC;
				if (fieldDeclaration.getType() == node)
					return TYPE;
				return FRAGMENTS;
			case ASTNode.FOR_STATEMENT :
				ForStatement forStatement = (ForStatement) parent;
				if (forStatement.getBody() == node)
					return BODY;
				if (forStatement.getExpression() == node)
					return EXPRESSION;
				if (forStatement.initializers().contains(node))
					return INITIALIZERS;
				return UPDATERS;
			case ASTNode.IF_STATEMENT :
				IfStatement ifStatement = (IfStatement) parent;
				if (ifStatement.getElseStatement() == node)
					return ELSE_STATEMENT;
				if (ifStatement.getExpression() == node)
					return EXPRESSION;
				return THEN_STATEMENT;
			case ASTNode.IMPORT_DECLARATION :
				return NAME;
			case ASTNode.INFIX_EXPRESSION :
				InfixExpression infixExpression = (InfixExpression) parent;
				if (infixExpression.getLeftOperand() == node)
					return LEFT_OPERAND;
				if (infixExpression.getRightOperand() == node)
					return RIGHT_OPERAND;
				return EXTENDED_OPERANDS;
			case ASTNode.INITIALIZER :
				if (((Initializer) parent).getBody() == node)
					return BODY;
				return JAVADOC;
			case ASTNode.INSTANCEOF_EXPRESSION :
				if (((InstanceofExpression) parent).getLeftOperand() == node)
					return LEFT_OPERAND;
				return RIGHT_OPERAND;
			case ASTNode.LABELED_STATEMENT :
				if (((LabeledStatement) parent).getBody() == node)
					return BODY;
				return LABEL;
			case ASTNode.METHOD_DECLARATION :
				MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
				if (methodDeclaration.getBody() == node)
					return BODY;
				if (methodDeclaration.getJavadoc() == node)
					return JAVADOC;
				if (methodDeclaration.getName() == node)
					return NAME;
				if (methodDeclaration.getReturnType() == node)
					return RETURN_TYPE;
				if (methodDeclaration.parameters().contains(node))
					return PARAMETERS;
				return THROWN_EXCEPTIONS;
			case ASTNode.METHOD_INVOCATION :
				MethodInvocation methodInvocation = (MethodInvocation) parent;
				if (methodInvocation.getExpression() == node)
					return EXPRESSION;
				if (methodInvocation.getName() == node)
					return NAME;
				return ARGUMENTS;
			case ASTNode.PACKAGE_DECLARATION :
				return NAME;
			case ASTNode.PARENTHESIZED_EXPRESSION :
				return EXPRESSION;
			case ASTNode.POSTFIX_EXPRESSION :
				return OPERAND;
			case ASTNode.PREFIX_EXPRESSION :
				return OPERAND;
			case ASTNode.QUALIFIED_NAME :
				if (((QualifiedName) parent).getName() == node)
					return NAME;
				return QUALIFIER;
			case ASTNode.RETURN_STATEMENT :
				return EXPRESSION;
			case ASTNode.SIMPLE_TYPE :
				return NAME;
			case ASTNode.SINGLE_VARIABLE_DECLARATION :
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) parent;
				if (singleVariableDeclaration.getInitializer() == node)
					return INITIALIZER;
				if (singleVariableDeclaration.getName() == node)
					return NAME;
				return TYPE;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
				if (((SuperConstructorInvocation) parent).getExpression() == node)
					return EXPRESSION;
				return ARGUMENTS;
			case ASTNode.SUPER_FIELD_ACCESS :
				if (((SuperFieldAccess) parent).getName() == node)
					return NAME;
				return QUALIFIER;
			case ASTNode.SUPER_METHOD_INVOCATION :
				SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation) parent;
				if (superMethodInvocation.getName() == node)
					return NAME;
				if (superMethodInvocation.getQualifier() == node)
					return QUALIFIER;
				return ARGUMENTS;
			case ASTNode.SWITCH_CASE :
				return EXPRESSION;
			case ASTNode.SWITCH_STATEMENT :
				if (((SwitchStatement) parent).getExpression() == node)
					return EXPRESSION;
				return STATEMENTS;
			case ASTNode.SYNCHRONIZED_STATEMENT :
				if (((SynchronizedStatement) parent).getBody() == node)
					return BODY;
				return EXPRESSION;
			case ASTNode.THIS_EXPRESSION :
				return QUALIFIER;
			case ASTNode.THROW_STATEMENT :
				return EXPRESSION;
			case ASTNode.TRY_STATEMENT :
				TryStatement tryStatement = (TryStatement) parent;
				if (tryStatement.getBody() == node)
					return BODY;
				if (tryStatement.getFinally() == node)
					return FINALLY;
				return CATCH_CLAUSES;
			case ASTNode.TYPE_DECLARATION :
				TypeDeclaration typeDeclaration = (TypeDeclaration) parent;
				if (typeDeclaration.getJavadoc() == node)
					return JAVADOC;
				if (typeDeclaration.getName() == node)
					return NAME;
				if (typeDeclaration.getSuperclass() == node)
					return SUPERCLASS;
				if (typeDeclaration.bodyDeclarations().contains(node))
					return BODY_DECLARATIONS;
				return SUPER_INTERFACES;
			case ASTNode.TYPE_DECLARATION_STATEMENT :
				return TYPE_DECLARATION;
			case ASTNode.TYPE_LITERAL :
				return TYPE;
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
				if (((VariableDeclarationExpression) parent).getType() == node)
					return TYPE;
				return FRAGMENTS;
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
				if (((VariableDeclarationFragment) parent).getInitializer() == node)
					return INITIALIZER;
				return NAME;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT :
				if (((VariableDeclarationStatement) parent).getType() == node)
					return TYPE;
				return FRAGMENTS;
			case ASTNode.WHILE_STATEMENT :
				if (((WhileStatement) parent).getBody() == node)
					return BODY;
				return EXPRESSION;
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Returns the properties of all children.
	 */
	public static int[] getNodeChildProperties(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.ANONYMOUS_CLASS_DECLARATION :
				return new int[] { BODY_DECLARATIONS };
			case ASTNode.ARRAY_ACCESS :
				return new int[] { ARRAY, INDEX };
			case ASTNode.ARRAY_CREATION :
				return new int[] { TYPE, DIMENSIONS, INITIALIZER };
			case ASTNode.ARRAY_INITIALIZER :
				return new int[] { EXPRESSIONS };
			case ASTNode.ARRAY_TYPE :
				return new int[] { COMPONENT_TYPE };
			case ASTNode.ASSERT_STATEMENT :
				return new int[] { EXPRESSION, MESSAGE };
			case ASTNode.ASSIGNMENT :
				return new int[] { OPERATOR, LEFT_HAND_SIDE, RIGHT_HAND_SIDE };
			case ASTNode.BLOCK :
				return new int[] { STATEMENTS };
			case ASTNode.BREAK_STATEMENT :
				return new int[] { LABEL };
			case ASTNode.CAST_EXPRESSION :
				return new int[] { TYPE, EXPRESSION };
			case ASTNode.CATCH_CLAUSE :
				return new int[] { EXCEPTION, BODY };
			case ASTNode.CHARACTER_LITERAL :
				return new int[] { ESCAPED_VALUE };
			case ASTNode.CLASS_INSTANCE_CREATION :
				return new int[] { EXPRESSION, NAME, ARGUMENTS, ANONYMOUS_CLASS_DECLARATION };
			case ASTNode.COMPILATION_UNIT :
				return new int[] { PACKAGE, IMPORTS, TYPES };
			case ASTNode.CONDITIONAL_EXPRESSION :
				return new int[] { EXPRESSION, THEN_EXPRESSION, ELSE_EXPRESSION };
			case ASTNode.CONSTRUCTOR_INVOCATION :
				return new int[] { ARGUMENTS };
			case ASTNode.CONTINUE_STATEMENT :
				return new int[] { LABEL };
			case ASTNode.DO_STATEMENT :
				return new int[] { EXPRESSION, BODY };
			case ASTNode.EXPRESSION_STATEMENT :
				return new int[] { EXPRESSION };
			case ASTNode.FIELD_ACCESS :
				return new int[] { EXPRESSION, NAME };
			case ASTNode.FIELD_DECLARATION :
				return new int[] { MODIFIERS, TYPE, FRAGMENTS, JAVADOC };
			case ASTNode.FOR_STATEMENT :
				return new int[] { INITIALIZERS, EXPRESSION, UPDATERS, BODY };
			case ASTNode.IF_STATEMENT :
				return new int[] { EXPRESSION, THEN_STATEMENT, ELSE_STATEMENT };
			case ASTNode.IMPORT_DECLARATION :
				return new int[] { NAME, IS_ON_DEMAND };
			case ASTNode.INFIX_EXPRESSION :
				return new int[] { OPERATOR, LEFT_OPERAND, RIGHT_OPERAND, EXTENDED_OPERANDS };
			case ASTNode.INITIALIZER :
				return new int[] { MODIFIERS, BODY, JAVADOC };
			case ASTNode.INSTANCEOF_EXPRESSION :
				return new int[] { LEFT_OPERAND, RIGHT_OPERAND };
			case ASTNode.JAVADOC :
				return new int[] { COMMENT };
			case ASTNode.LABELED_STATEMENT :
				return new int[] { LABEL, BODY };
			case ASTNode.METHOD_DECLARATION :
				return new int[] { IS_CONSTRUCTOR, MODIFIERS, NAME, PARAMETERS, THROWN_EXCEPTIONS, RETURN_TYPE, EXTRA_DIMENSIONS, BODY, JAVADOC };
			case ASTNode.METHOD_INVOCATION :
				return new int[] { EXPRESSION, NAME, ARGUMENTS };
			case ASTNode.NUMBER_LITERAL :
				return new int[] { TOKEN };
			case ASTNode.PACKAGE_DECLARATION :
				return new int[] { NAME };
			case ASTNode.PARENTHESIZED_EXPRESSION :
				return new int[] { EXPRESSION };
			case ASTNode.POSTFIX_EXPRESSION :
				return new int[] { OPERATOR, OPERAND };
			case ASTNode.PREFIX_EXPRESSION :
				return new int[] { OPERATOR, OPERAND };
			case ASTNode.PRIMITIVE_TYPE :
				return new int[] { PRIMITIVE_TYPE_CODE };
			case ASTNode.QUALIFIED_NAME :
				return new int[] { QUALIFIER, NAME };
			case ASTNode.RETURN_STATEMENT :
				return new int[] { EXPRESSION };
			case ASTNode.SIMPLE_NAME :
				return new int[] { IDENTIFIER };
			case ASTNode.SIMPLE_TYPE :
				return new int[] { NAME };
			case ASTNode.SINGLE_VARIABLE_DECLARATION :
				return new int[] { MODIFIERS, NAME, TYPE, EXTRA_DIMENSIONS, INITIALIZER };
			case ASTNode.STRING_LITERAL :
				return new int[] { ESCAPED_VALUE, LITERAL_VALUE };
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
				return new int[] { EXPRESSION, ARGUMENTS };
			case ASTNode.SUPER_FIELD_ACCESS :
				return new int[] { QUALIFIER, NAME };
			case ASTNode.SUPER_METHOD_INVOCATION :
				return new int[] { QUALIFIER, NAME, ARGUMENTS };
			case ASTNode.SWITCH_CASE :
				return new int[] { EXPRESSION };
			case ASTNode.SWITCH_STATEMENT :
				return new int[] { EXPRESSION, STATEMENTS };
			case ASTNode.SYNCHRONIZED_STATEMENT :
				return new int[] { EXPRESSION, BODY };
			case ASTNode.THIS_EXPRESSION :
				return new int[] { QUALIFIER };
			case ASTNode.THROW_STATEMENT :
				return new int[] { EXPRESSION };
			case ASTNode.TRY_STATEMENT :
				return new int[] { BODY, CATCH_CLAUSES, FINALLY };
			case ASTNode.TYPE_DECLARATION :
				return new int[] { IS_INTERFACE, MODIFIERS, NAME, SUPERCLASS, SUPER_INTERFACES, BODY_DECLARATIONS, JAVADOC };
			case ASTNode.TYPE_DECLARATION_STATEMENT :
				return new int[] { TYPE_DECLARATION };
			case ASTNode.TYPE_LITERAL :
				return new int[] { TYPE };
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
				return new int[] { MODIFIERS, TYPE, FRAGMENTS };
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
				return new int[] { NAME, EXTRA_DIMENSIONS, INITIALIZER };
			case ASTNode.VARIABLE_DECLARATION_STATEMENT :
				return new int[] { MODIFIERS, TYPE, FRAGMENTS };
			case ASTNode.WHILE_STATEMENT :
				return new int[] { EXPRESSION, BODY };
		}
		return new int[0];
	}

	/**
	 * Returns the name of a property.
	 */
	public static String getPropertyName(int property) {
		switch (property) {
			case ANONYMOUS_CLASS_DECLARATION :
				return "ANONYMOUS_CLASS_DECLARATION"; //$NON-NLS-1$
			case ARGUMENTS :
				return "ARGUMENTS"; //$NON-NLS-1$
			case ARRAY :
				return "ARRAY"; //$NON-NLS-1$
			case BODY :
				return "BODY"; //$NON-NLS-1$
			case BODY_DECLARATIONS :
				return "BODY_DECLARATIONS"; //$NON-NLS-1$
			case CATCH_CLAUSES :
				return "CATCH_CLAUSES"; //$NON-NLS-1$
			case COMMENT :
				return "COMMENT"; //$NON-NLS-1$
			case COMPONENT_TYPE :
				return "COMPONENT_TYPE"; //$NON-NLS-1$
			case DIMENSIONS :
				return "DIMENSIONS"; //$NON-NLS-1$
			case ELSE_EXPRESSION :
				return "ELSE_EXPRESSION"; //$NON-NLS-1$
			case ELSE_STATEMENT :
				return "ELSE_STATEMENT"; //$NON-NLS-1$
			case ESCAPED_VALUE :
				return "ESCAPED_VALUE"; //$NON-NLS-1$
			case EXCEPTION :
				return "EXCEPTION"; //$NON-NLS-1$
			case EXPRESSION :
				return "EXPRESSION"; //$NON-NLS-1$
			case EXPRESSIONS :
				return "EXPRESSIONS"; //$NON-NLS-1$
			case EXTENDED_OPERANDS :
				return "EXTENDED_OPERANDS"; //$NON-NLS-1$
			case EXTRA_DIMENSIONS :
				return "EXTRA_DIMENSIONS"; //$NON-NLS-1$
			case FINALLY :
				return "FINALLY"; //$NON-NLS-1$
			case FRAGMENTS :
				return "FRAGMENTS"; //$NON-NLS-1$
			case IDENTIFIER :
				return "IDENTIFIER"; //$NON-NLS-1$
			case IMPORTS :
				return "IMPORTS"; //$NON-NLS-1$
			case INDEX :
				return "INDEX"; //$NON-NLS-1$
			case INITIALIZER :
				return "INITIALIZER"; //$NON-NLS-1$
			case INITIALIZERS :
				return "INITIALIZERS"; //$NON-NLS-1$
			case IS_CONSTRUCTOR :
				return "IS_CONSTRUCTOR"; //$NON-NLS-1$
			case IS_INTERFACE :
				return "IS_INTERFACE"; //$NON-NLS-1$
			case IS_ON_DEMAND :
				return "IS_ON_DEMAND"; //$NON-NLS-1$
			case JAVADOC :
				return "JAVADOC"; //$NON-NLS-1$
			case LABEL :
				return "LABEL"; //$NON-NLS-1$
			case LEFT_HAND_SIDE :
				return "LEFT_HAND_SIDE"; //$NON-NLS-1$
			case LEFT_OPERAND :
				return "LEFT_OPERAND"; //$NON-NLS-1$
			case LITERAL_VALUE :
				return "LITERAL_VALUE"; //$NON-NLS-1$
			case MESSAGE :
				return "MESSAGE"; //$NON-NLS-1$
			case MODIFIERS :
				return "MODIFIERS"; //$NON-NLS-1$
			case NAME :
				return "NAME"; //$NON-NLS-1$
			case OPERAND :
				return "OPERAND"; //$NON-NLS-1$
			case OPERATOR :
				return "OPERATOR"; //$NON-NLS-1$
			case PACKAGE :
				return "PACKAGE"; //$NON-NLS-1$
			case PARAMETERS :
				return "PARAMETERS"; //$NON-NLS-1$
			case PRIMITIVE_TYPE_CODE :
				return "PRIMITIVE_TYPE_CODE"; //$NON-NLS-1$
			case QUALIFIER :
				return "QUALIFIER"; //$NON-NLS-1$
			case RETURN_TYPE :
				return "RETURN_TYPE"; //$NON-NLS-1$
			case RIGHT_HAND_SIDE :
				return "RIGHT_HAND_SIDE"; //$NON-NLS-1$
			case RIGHT_OPERAND :
				return "RIGHT_OPERAND"; //$NON-NLS-1$
			case STATEMENTS :
				return "STATEMENTS"; //$NON-NLS-1$
			case SUPERCLASS :
				return "SUPERCLASS"; //$NON-NLS-1$
			case SUPER_INTERFACES :
				return "SUPER_INTERFACES"; //$NON-NLS-1$
			case THEN_EXPRESSION :
				return "THEN_EXPRESSION"; //$NON-NLS-1$
			case THEN_STATEMENT :
				return "THEN_STATEMENT"; //$NON-NLS-1$
			case THROWN_EXCEPTIONS :
				return "THROWN_EXCEPTIONS"; //$NON-NLS-1$
			case TOKEN :
				return "TOKEN"; //$NON-NLS-1$
			case TYPE :
				return "TYPE"; //$NON-NLS-1$
			case TYPE_DECLARATION :
				return "TYPE_DECLARATION"; //$NON-NLS-1$
			case TYPES :
				return "TYPES"; //$NON-NLS-1$
			case UPDATERS :
				return "UPDATERS"; //$NON-NLS-1$
		}
		throw new IllegalArgumentException();
	}

}