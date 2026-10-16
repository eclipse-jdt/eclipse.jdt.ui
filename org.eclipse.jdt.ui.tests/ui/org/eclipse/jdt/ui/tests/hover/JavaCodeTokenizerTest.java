/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc. - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.hover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.jdt.core.manipulation.internal.javadoc.JavaCodeTokenizer;

public class JavaCodeTokenizerTest {

	private static String kw(String text) {
		return "<span class=\"jc-keyword\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String str(String text) {
		return "<span class=\"jc-string\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String cmt(String text) {
		return "<span class=\"jc-comment\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String ann(String text) {
		return "<span class=\"jc-annotation\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String type(String text) {
		return "<span class=\"jc-type\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String methodDecl(String text) {
		return "<span class=\"jc-method-decl\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String method(String text) {
		return "<span class=\"jc-method\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String param(String text) {
		return "<span class=\"jc-param\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String localDecl(String text) {
		return "<span class=\"jc-local-var-decl\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String local(String text) {
		return "<span class=\"jc-local-var\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String num(String text) {
		return "<span class=\"jc-number\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String typeArg(String text) {
		return "<span class=\"jc-type-arg\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String field(String text) {
		return "<span class=\"jc-field\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String staticField(String text) {
		return "<span class=\"jc-static-field\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String staticFinalField(String text) {
		return "<span class=\"jc-static-final-field\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	private static String enumType(String text) {
		return "<span class=\"jc-enum\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testKeywords() {
		assertEquals(kw("public") + " " + kw("class") + " " + type("Foo") + " {}",
				JavaCodeTokenizer.tokenize("public class Foo {}"));
	}

	@Test
	public void testStringLiteral() {
		assertEquals(str("\"hello world\""),
				JavaCodeTokenizer.tokenize("\"hello world\""));
	}

	@Test
	public void testCharLiteral() {
		assertEquals(str("'a'"),
				JavaCodeTokenizer.tokenize("'a'"));
	}

	@Test
	public void testSingleLineComment() {
		assertEquals(cmt("// comment"),
				JavaCodeTokenizer.tokenize("// comment"));
	}

	@Test
	public void testMultiLineComment() {
		assertEquals(cmt("/* comment */"),
				JavaCodeTokenizer.tokenize("/* comment */"));
	}

	@Test
	public void testAnnotation() {
		assertEquals(ann("@Override"),
				JavaCodeTokenizer.tokenize("@Override"));
	}

	@Test
	public void testTypeName() {
		assertEquals(type("String") + " " + localDecl("name") + " = " + str("\"hello\"") + ";",
				JavaCodeTokenizer.tokenize("String name = \"hello\";"));
	}

	@Test
	public void testLocalVariableDeclarationAndReference() {
		String code= "int x = 5;\nSystem.out.println(x);";
		String expected= kw("int") + " " + localDecl("x") + " = " + num("5") + ";\n"
				+ type("System") + ".out." + method("println") + "(" + local("x") + ");";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testMethodDeclarationWithParams() {
		String code= "void foo(String bar, int baz) {\n    println(bar);\n}";
		String expected= kw("void") + " " + methodDecl("foo")
				+ "(" + type("String") + " " + param("bar") + ", " + kw("int") + " " + param("baz") + ") {\n"
				+ "    " + method("println") + "(" + param("bar") + ");\n}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testParameterScopeCleanup() {
		String code= "void foo(String bar) {\n    println(bar);\n}\nbar;";
		String result= JavaCodeTokenizer.tokenize(code);
		// After the method body closes, 'bar' should no longer be colored as param
		String expected= kw("void") + " " + methodDecl("foo")
				+ "(" + type("String") + " " + param("bar") + ") {\n"
				+ "    " + method("println") + "(" + param("bar") + ");\n}\nbar;";
		assertEquals(expected, result);
	}

	@Test
	public void testMethodInvocation() {
		assertEquals(method("test") + "(" + num("42") + ");",
				JavaCodeTokenizer.tokenize("test(42);"));
	}

	@Test
	public void testGenerics() {
		String code= "List<String> items = new ArrayList<>();";
		String expected= type("List") + "&lt;" + typeArg("String") + "&gt; " + localDecl("items")
				+ " = " + kw("new") + " " + type("ArrayList") + "&lt;&gt;();";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testNestedGenerics() {
		String code= "Map<String, List<Integer>> map;";
		String expected= type("Map") + "&lt;" + typeArg("String") + ", " + typeArg("List")
				+ "&lt;" + typeArg("Integer") + "&gt;&gt; " + localDecl("map") + ";";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testWildcardGenerics() {
		String code= "List<? extends Number> list;";
		String expected= type("List") + "&lt;? " + kw("extends") + " " + typeArg("Number") + "&gt; " + localDecl("list") + ";";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testNumberLiterals() {
		assertEquals(num("42"), JavaCodeTokenizer.tokenize("42"));
		assertEquals(num("3.14"), JavaCodeTokenizer.tokenize("3.14"));
		assertEquals(num("0xFF"), JavaCodeTokenizer.tokenize("0xFF"));
		assertEquals(num("0b1010"), JavaCodeTokenizer.tokenize("0b1010"));
		assertEquals(num("42L"), JavaCodeTokenizer.tokenize("42L"));
		assertEquals(num("3.14f"), JavaCodeTokenizer.tokenize("3.14f"));
		assertEquals(num("1_000_000"), JavaCodeTokenizer.tokenize("1_000_000"));
	}

	@Test
	public void testArrayTypeDeclaration() {
		String code= "String[] args;";
		String expected= type("String") + "[] " + localDecl("args") + ";";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testVarargs() {
		String code= "void foo(String... args) {}";
		String expected= kw("void") + " " + methodDecl("foo")
				+ "(" + type("String") + "... " + param("args") + ") {}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testAnnotatedMethod() {
		String code= "@Override\npublic void run() {}";
		String expected= ann("@Override") + "\n" + kw("public") + " " + kw("void") + " " + methodDecl("run") + "() {}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testForLoop() {
		String code= "for (int i = 0; i < 10; i++) {}";
		String expected= kw("for") + " (" + kw("int") + " " + localDecl("i") + " = " + num("0")
				+ "; " + local("i") + " &lt; " + num("10") + "; " + local("i") + "++) {}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testEnhancedForLoop() {
		String code= "for (String item : items) {}";
		String expected= kw("for") + " (" + type("String") + " " + localDecl("item") + " : items) {}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testTextBlock() {
		String code= "String s = \"\"\"\n    hello\n    \"\"\";";
		String expected= type("String") + " " + localDecl("s") + " = " + str("\"\"\"\n    hello\n    \"\"\"") + ";";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testVarKeyword() {
		String code= "var count = 0;";
		String expected= kw("var") + " " + localDecl("count") + " = " + num("0") + ";";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testChainedMethodCalls() {
		String code= "list.stream().filter(x).collect();";
		String expected= "list." + method("stream") + "()." + method("filter") + "(x)." + method("collect") + "();";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testLocalVariableScopeCleanup() {
		String code= "{\n    int x = 1;\n}\nx;";
		String result= JavaCodeTokenizer.tokenize(code);
		// After closing brace, x should no longer be colored as local var
		String expected= "{\n    " + kw("int") + " " + localDecl("x") + " = " + num("1") + ";\n}\nx;";
		assertEquals(expected, result);
	}

	@Test
	public void testStaticMethodCall() {
		String code= "String.valueOf(42);";
		String expected= type("String") + "." + method("valueOf") + "(" + num("42") + ");";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testComplexExample() {
		String code= "public static void main(String[] args) {\n"
				+ "    List<String> items = new ArrayList<>();\n"
				+ "    for (String item : items) {\n"
				+ "        System.out.println(item);\n"
				+ "    }\n"
				+ "}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains("<span class=\"jc-method-decl\">main</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-param\">args</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-type\">List</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-type-arg\">String</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-local-var-decl\">items</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-local-var\">items</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-local-var-decl\">item</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-local-var\">item</span>")); //$NON-NLS-1$
	}

	@Test
	public void testInstanceField() {
		String code= "class Foo {\n    String name;\n}";
		String expected= kw("class") + " " + type("Foo") + " {\n    " + type("String") + " " + field("name") + ";\n}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testStaticField() {
		String code= "class Foo {\n    static int count;\n}";
		String expected= kw("class") + " " + type("Foo") + " {\n    " + kw("static") + " " + kw("int") + " " + staticField("count") + ";\n}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testStaticFinalField() {
		String code= "class Foo {\n    static final int MAX = 100;\n}";
		String expected= kw("class") + " " + type("Foo") + " {\n    " + kw("static") + " " + kw("final") + " " + kw("int") + " " + staticFinalField("MAX") + " = " + num("100") + ";\n}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testFieldReference() {
		String code= "class Foo {\n    int count;\n    void inc() {\n        count++;\n    }\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains("<span class=\"jc-field\">count</span>;")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-field\">count</span>++")); //$NON-NLS-1$
	}

	@Test
	public void testFieldShadowedByLocal() {
		String code= "class Foo {\n    int x;\n    void test() {\n        int x = 1;\n        x++;\n    }\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains("<span class=\"jc-field\">x</span>;")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-local-var-decl\">x</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-local-var\">x</span>++")); //$NON-NLS-1$
	}

	@Test
	public void testFieldShadowedByParam() {
		String code= "class Foo {\n    int x;\n    void test(int x) {\n        x++;\n    }\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains("<span class=\"jc-field\">x</span>;")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-param\">x</span>)")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-param\">x</span>++")); //$NON-NLS-1$
	}

	@Test
	public void testPrivateField() {
		String code= "class Foo {\n    private String name;\n}";
		String expected= kw("class") + " " + type("Foo") + " {\n    " + kw("private") + " " + type("String") + " " + field("name") + ";\n}";
		assertEquals(expected, JavaCodeTokenizer.tokenize(code));
	}

	@Test
	public void testStaticInitializerBlock() {
		String code= "class Foo {\n    static int x;\n    static {\n        int y = 1;\n        y++;\n    }\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains("<span class=\"jc-static-field\">x</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-local-var-decl\">y</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-local-var\">y</span>")); //$NON-NLS-1$
	}

	@Test
	public void testUppercaseConstantField() {
		String code= "class Foo {\n    static final String DEFAULT_NAME = \"hello\";\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains("<span class=\"jc-static-final-field\">DEFAULT_NAME</span>")); //$NON-NLS-1$
	}

	@Test
	public void testFieldScopeCleanup() {
		String code= "class Foo {\n    int x;\n}\nx;";
		String result= JavaCodeTokenizer.tokenize(code);
		// After class body closes, x should no longer be colored as field
		assertTrue(result.endsWith("}\nx;")); //$NON-NLS-1$
	}

	@Test
	public void testEnumFields() {
		String code= "enum Color {\n    RED;\n    private int value;\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(enumType("Color"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("RED"))); //$NON-NLS-1$
		assertTrue(result.contains(field("value"))); //$NON-NLS-1$
	}

	@Test
	public void testEnumMultipleConstants() {
		String code= "enum Color {\n    RED,\n    GREEN,\n    BLUE;\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(enumType("Color"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("RED"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("GREEN"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("BLUE"))); //$NON-NLS-1$
	}

	@Test
	public void testEnumConstantsWithArguments() {
		String code= "enum Planet {\n    EARTH(1.0),\n    MARS(0.38);\n    private double gravity;\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(enumType("Planet"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("EARTH"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("MARS"))); //$NON-NLS-1$
		assertTrue(result.contains(field("gravity"))); //$NON-NLS-1$
	}

	@Test
	public void testEnumConstantWithBody() {
		String code= "enum Op {\n    PLUS {\n        int apply(int a) { return a; }\n    };\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(enumType("Op"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("PLUS"))); //$NON-NLS-1$
		assertTrue(result.contains(methodDecl("apply"))); //$NON-NLS-1$
	}

	@Test
	public void testEnumConstantReferenceInMethod() {
		String code= "enum Color {\n    RED, GREEN;\n    Color other() {\n        return RED;\n    }\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(staticFinalField("RED") + ",")); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("GREEN"))); //$NON-NLS-1$
		assertTrue(result.contains(enumType("Color") + " " + methodDecl("other"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("RED") + ";")); //$NON-NLS-1$
	}

	@Test
	public void testEnumTypeQualifiedConstantAccess() {
		String code= "enum Rgb {\n    RED, GREEN, BLUE;\n}\nRgb field = Rgb.RED;";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(kw("enum") + " " + enumType("Rgb"))); //$NON-NLS-1$
		assertTrue(result.contains(staticFinalField("RED") + ",")); //$NON-NLS-1$
		assertTrue(result.contains(enumType("Rgb") + " " + localDecl("field"))); //$NON-NLS-1$
		assertTrue(result.contains(enumType("Rgb") + "." + staticFinalField("RED") + ";")); //$NON-NLS-1$
	}

	@Test
	public void testEnumTypeInMethodParam() {
		String code= "enum Status { OK, ERROR; }\nvoid handle(Status status) {}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(enumType("Status") + " " + param("status"))); //$NON-NLS-1$
	}

	@Test
	public void testEnumDotMethodNotConstant() {
		String code= "enum Color { RED; }\nColor.valueOf(\"RED\");";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(enumType("Color") + "." + method("valueOf"))); //$NON-NLS-1$
	}

	@Test
	public void testMethodInTypeBody() {
		String code= "class Foo {\n    int x;\n    void test() {\n        println(x);\n    }\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains("<span class=\"jc-field\">x</span>;")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-method-decl\">test</span>")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-field\">x</span>)")); //$NON-NLS-1$
	}

	@Test
	public void testMultipleFieldsAndMethods() {
		String code= "class Foo {\n"
				+ "    private int count;\n"
				+ "    static String name;\n"
				+ "    static final int MAX = 10;\n"
				+ "    void test() {\n"
				+ "        count = MAX;\n"
				+ "        name = \"hello\";\n"
				+ "    }\n"
				+ "}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains("<span class=\"jc-field\">count</span>;")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-static-field\">name</span>;")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-static-final-field\">MAX</span> =")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-field\">count</span> =")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-static-final-field\">MAX</span>;")); //$NON-NLS-1$
		assertTrue(result.contains("<span class=\"jc-static-field\">name</span> =")); //$NON-NLS-1$
	}
}
