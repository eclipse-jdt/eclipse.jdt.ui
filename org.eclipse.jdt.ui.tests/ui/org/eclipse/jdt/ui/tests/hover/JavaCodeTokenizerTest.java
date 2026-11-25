/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc. - Initial API and implementation (using Claude AI)
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.hover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.eclipse.jdt.core.manipulation.internal.javadoc.JavaCodeTokenizer;
import org.eclipse.jdt.core.manipulation.internal.javadoc.JavaCodeTokenizer.JavaCodeToken;

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
	private static String kwreturn(String text) {
		return "<span class=\"jc-keyword-return\">" + text + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$
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
	public void testEnumMultipleConstants2() {
		String code= "enum Color {\n    RED,\n    GREEN,\n    BLUE\n}";
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
	public void testQualifiedFieldAccessInSetter() {
		String code= "class C {\n    int x;\n    void set(int x) {\n        this.x = x;\n    }\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		// this.x should be colored as field, not param
		assertTrue(result.contains(kw("this") + "." + field("x") + " =")); //$NON-NLS-1$
		// unqualified x should be colored as param
		assertTrue(result.contains("= " + param("x") + ";")); //$NON-NLS-1$
		// param declaration
		assertTrue(result.contains(kw("int") + " " + param("x") + ")")); //$NON-NLS-1$
		// field declaration
		assertTrue(result.contains(kw("int") + " " + field("x") + ";")); //$NON-NLS-1$
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
	public void testBodylessMethodParameterLeak() {
		String code= "class C {\n    int x;\n    abstract void f(int x);\n    int g() {\n        return x;\n    }\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		// f's param x should not leak into g — x in g() should be field, not param
		assertTrue(result.contains(kw("int") + " " + field("x") + ";")); //$NON-NLS-1$
		assertTrue(result.contains(kw("int") + " " + param("x") + ")")); //$NON-NLS-1$
		assertTrue(result.contains(kwreturn("return") + " " + field("x") + ";")); //$NON-NLS-1$
	}

	@Test
	public void testClassLiteralDoesNotSetTypeBody() {
		String code= "Class<?> t = String.class;\nif (true) {\n    int x = 1;\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		// x should be local var decl, not field — .class must not set expectTypeBody
		assertTrue(result.contains(localDecl("x"))); //$NON-NLS-1$
	}

	@Test
	public void testClassLiteralWithWhitespace() {
		String code= "Class<?> t = String . class;\nif (true) {\n    int x = 1;\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(localDecl("x"))); //$NON-NLS-1$
	}

	@Test
	public void testClassLiteralWithComment() {
		String code= "Class<?> t = String./* comment */class;\nif (true) {\n    int x = 1;\n}";
		String result= JavaCodeTokenizer.tokenize(code);
		assertTrue(result.contains(localDecl("x"))); //$NON-NLS-1$
	}

	@Test
	public void testNestedTypeFieldShadowing() {
		String code= "class Outer {\n"
				+ "    int x;\n"
				+ "    class Inner {\n"
				+ "        int x;\n"
				+ "    }\n"
				+ "    void f() {\n"
				+ "        x++;\n"
				+ "    }\n"
				+ "}";
		String result= JavaCodeTokenizer.tokenize(code);
		// inner x must not erase outer x — x in f() should still be colored as field
		assertTrue(result.contains(field("x") + "++")); //$NON-NLS-1$
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

	// ---- tokenizeToTokens() helpers ----

	private static String tokenText(String code, JavaCodeToken token) {
		return code.substring(token.offset(), token.offset() + token.length());
	}

	private static JavaCodeToken findFirstToken(List<JavaCodeToken> tokens, String cssClass) {
		for (JavaCodeToken t : tokens) {
			if (cssClass.equals(t.cssClass())) {
				return t;
			}
		}
		return null;
	}

	private static List<JavaCodeToken> findAllTokens(List<JavaCodeToken> tokens, String cssClass) {
		return tokens.stream().filter(t -> cssClass.equals(t.cssClass())).toList();
	}

	private static void assertTokensCoverEntireInput(String code, List<JavaCodeToken> tokens) {
		int expectedOffset= 0;
		for (JavaCodeToken token : tokens) {
			assertEquals("Gap or overlap at offset " + expectedOffset, expectedOffset, token.offset()); //$NON-NLS-1$
			assertTrue("Token length must be positive at offset " + token.offset(), token.length() > 0); //$NON-NLS-1$
			expectedOffset += token.length();
		}
		assertEquals("Tokens must cover entire input", code.length(), expectedOffset); //$NON-NLS-1$
	}

	// ---- tokenizeToTokens() tests ----

	@Test
	public void testTokensEmptyString() {
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(""); //$NON-NLS-1$
		assertTrue(tokens.isEmpty());
	}

	@Test
	public void testTokensCoverageComplete() {
		String code= "int x = 5;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
	}

	@Test
	public void testTokensCoverageCompleteComplex() {
		String code= "public static void main(String[] args) {\n" //$NON-NLS-1$
				+ "    List<String> items = new ArrayList<>();\n" //$NON-NLS-1$
				+ "    for (String item : items) {\n" //$NON-NLS-1$
				+ "        System.out.println(item);\n" //$NON-NLS-1$
				+ "    }\n" //$NON-NLS-1$
				+ "}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
	}

	@Test
	public void testTokensGapsFilled() {
		String code= "int x = 5;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		// "int" = keyword, " " = null, "x" = local-var-decl, " = " = null, "5" = number, ";" = null
		JavaCodeToken space= tokens.get(1);
		assertNull(space.cssClass());
		assertEquals(" ", tokenText(code, space)); //$NON-NLS-1$
	}

	@Test
	public void testTokensKeyword() {
		String code= "int x = 5;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		JavaCodeToken kwToken= tokens.get(0);
		assertEquals("jc-keyword", kwToken.cssClass()); //$NON-NLS-1$
		assertEquals(0, kwToken.offset());
		assertEquals(3, kwToken.length());
		assertEquals("int", tokenText(code, kwToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensReturnKeyword() {
		String code= "return 42;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		JavaCodeToken retToken= findFirstToken(tokens, "jc-keyword-return"); //$NON-NLS-1$
		assertNotNull(retToken);
		assertEquals("return", tokenText(code, retToken)); //$NON-NLS-1$
		assertEquals(0, retToken.offset());
		assertEquals(6, retToken.length());
	}

	@Test
	public void testTokensMultipleKeywords() {
		String code= "public class Foo {}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		List<JavaCodeToken> kwTokens= findAllTokens(tokens, "jc-keyword"); //$NON-NLS-1$
		assertEquals(2, kwTokens.size());
		assertEquals("public", tokenText(code, kwTokens.get(0))); //$NON-NLS-1$
		assertEquals("class", tokenText(code, kwTokens.get(1))); //$NON-NLS-1$
	}

	@Test
	public void testTokensStringLiteral() {
		String code= "\"hello world\""; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-string", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals(0, tokens.get(0).offset());
		assertEquals(code.length(), tokens.get(0).length());
	}

	@Test
	public void testTokensStringWithEscapes() {
		String code= "\"hello\\nworld\""; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		JavaCodeToken strToken= findFirstToken(tokens, "jc-string"); //$NON-NLS-1$
		assertNotNull(strToken);
		assertEquals(code, tokenText(code, strToken));
	}

	@Test
	public void testTokensCharLiteral() {
		String code= "'a'"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-string", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals("'a'", tokenText(code, tokens.get(0))); //$NON-NLS-1$
	}

	@Test
	public void testTokensTextBlock() {
		String code= "\"\"\"\n    hello\n    \"\"\""; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-string", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals(code.length(), tokens.get(0).length());
	}

	@Test
	public void testTokensSingleLineComment() {
		String code= "// this is a comment"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-comment", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals(code, tokenText(code, tokens.get(0)));
	}

	@Test
	public void testTokensMultiLineComment() {
		String code= "/* multi\n   line */"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-comment", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals(code, tokenText(code, tokens.get(0)));
	}

	@Test
	public void testTokensAnnotation() {
		String code= "@Override"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-annotation", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals("@Override", tokenText(code, tokens.get(0))); //$NON-NLS-1$
	}

	@Test
	public void testTokensTypeName() {
		String code= "String name = \"hello\";"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		JavaCodeToken typeToken= findFirstToken(tokens, "jc-type"); //$NON-NLS-1$
		assertNotNull(typeToken);
		assertEquals("String", tokenText(code, typeToken)); //$NON-NLS-1$
		assertEquals(0, typeToken.offset());
		assertEquals(6, typeToken.length());
	}

	@Test
	public void testTokensMethodDeclaration() {
		String code= "void foo(int x) {}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken methodDeclToken= findFirstToken(tokens, "jc-method-decl"); //$NON-NLS-1$
		assertNotNull(methodDeclToken);
		assertEquals("foo", tokenText(code, methodDeclToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensMethodInvocation() {
		String code= "println(42);"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		JavaCodeToken methodToken= findFirstToken(tokens, "jc-method"); //$NON-NLS-1$
		assertNotNull(methodToken);
		assertEquals("println", tokenText(code, methodToken)); //$NON-NLS-1$
		assertEquals(0, methodToken.offset());
	}

	@Test
	public void testTokensParameter() {
		String code= "void foo(String bar) {\n    println(bar);\n}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		List<JavaCodeToken> paramTokens= findAllTokens(tokens, "jc-param"); //$NON-NLS-1$
		assertEquals(2, paramTokens.size());
		assertEquals("bar", tokenText(code, paramTokens.get(0))); //$NON-NLS-1$
		assertEquals("bar", tokenText(code, paramTokens.get(1))); //$NON-NLS-1$
		// First is in parameter list, second is in method body
		assertTrue(paramTokens.get(0).offset() < paramTokens.get(1).offset());
	}

	@Test
	public void testTokensLocalVarDeclAndRef() {
		String code= "int x = 5;\nprintln(x);"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken declToken= findFirstToken(tokens, "jc-local-var-decl"); //$NON-NLS-1$
		assertNotNull(declToken);
		assertEquals("x", tokenText(code, declToken)); //$NON-NLS-1$
		JavaCodeToken refToken= findFirstToken(tokens, "jc-local-var"); //$NON-NLS-1$
		assertNotNull(refToken);
		assertEquals("x", tokenText(code, refToken)); //$NON-NLS-1$
		assertTrue(refToken.offset() > declToken.offset());
	}

	@Test
	public void testTokensNumber() {
		String code= "42"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-number", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals("42", tokenText(code, tokens.get(0))); //$NON-NLS-1$
	}

	@Test
	public void testTokensHexNumber() {
		String code= "0xFF"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-number", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals("0xFF", tokenText(code, tokens.get(0))); //$NON-NLS-1$
	}

	@Test
	public void testTokensBinaryNumber() {
		String code= "0b1010"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-number", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals("0b1010", tokenText(code, tokens.get(0))); //$NON-NLS-1$
	}

	@Test
	public void testTokensFloatNumber() {
		String code= "3.14f"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-number", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals("3.14f", tokenText(code, tokens.get(0))); //$NON-NLS-1$
	}

	@Test
	public void testTokensUnderscoreNumber() {
		String code= "1_000_000"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertEquals(1, tokens.size());
		assertEquals("jc-number", tokens.get(0).cssClass()); //$NON-NLS-1$
		assertEquals("1_000_000", tokenText(code, tokens.get(0))); //$NON-NLS-1$
	}

	@Test
	public void testTokensGenerics() {
		String code= "List<String> items;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken typeToken= findFirstToken(tokens, "jc-type"); //$NON-NLS-1$
		assertNotNull(typeToken);
		assertEquals("List", tokenText(code, typeToken)); //$NON-NLS-1$
		JavaCodeToken typeArgToken= findFirstToken(tokens, "jc-type-arg"); //$NON-NLS-1$
		assertNotNull(typeArgToken);
		assertEquals("String", tokenText(code, typeArgToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensNestedGenerics() {
		String code= "Map<String, List<Integer>> map;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		List<JavaCodeToken> typeArgTokens= findAllTokens(tokens, "jc-type-arg"); //$NON-NLS-1$
		assertEquals(3, typeArgTokens.size());
		assertEquals("String", tokenText(code, typeArgTokens.get(0))); //$NON-NLS-1$
		assertEquals("List", tokenText(code, typeArgTokens.get(1))); //$NON-NLS-1$
		assertEquals("Integer", tokenText(code, typeArgTokens.get(2))); //$NON-NLS-1$
	}

	@Test
	public void testTokensInstanceField() {
		String code= "class Foo {\n    String name;\n}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken fieldToken= findFirstToken(tokens, "jc-field"); //$NON-NLS-1$
		assertNotNull(fieldToken);
		assertEquals("name", tokenText(code, fieldToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensStaticField() {
		String code= "class Foo {\n    static int count;\n}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken sfToken= findFirstToken(tokens, "jc-static-field"); //$NON-NLS-1$
		assertNotNull(sfToken);
		assertEquals("count", tokenText(code, sfToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensStaticFinalField() {
		String code= "class Foo {\n    static final int MAX = 100;\n}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken sffToken= findFirstToken(tokens, "jc-static-final-field"); //$NON-NLS-1$
		assertNotNull(sffToken);
		assertEquals("MAX", tokenText(code, sffToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensEnum() {
		String code= "enum Color {\n    RED, GREEN, BLUE;\n}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken enumToken= findFirstToken(tokens, "jc-enum"); //$NON-NLS-1$
		assertNotNull(enumToken);
		assertEquals("Color", tokenText(code, enumToken)); //$NON-NLS-1$
		List<JavaCodeToken> constTokens= findAllTokens(tokens, "jc-static-final-field"); //$NON-NLS-1$
		assertEquals(3, constTokens.size());
		assertEquals("RED", tokenText(code, constTokens.get(0))); //$NON-NLS-1$
		assertEquals("GREEN", tokenText(code, constTokens.get(1))); //$NON-NLS-1$
		assertEquals("BLUE", tokenText(code, constTokens.get(2))); //$NON-NLS-1$
	}

	@Test
	public void testTokensForLoop() {
		String code= "for (int i = 0; i < 10; i++) {}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken declToken= findFirstToken(tokens, "jc-local-var-decl"); //$NON-NLS-1$
		assertNotNull(declToken);
		assertEquals("i", tokenText(code, declToken)); //$NON-NLS-1$
		List<JavaCodeToken> refTokens= findAllTokens(tokens, "jc-local-var"); //$NON-NLS-1$
		assertEquals(2, refTokens.size());
		for (JavaCodeToken ref : refTokens) {
			assertEquals("i", tokenText(code, ref)); //$NON-NLS-1$
		}
	}

	@Test
	public void testTokensVarKeyword() {
		String code= "var count = 0;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken varToken= findFirstToken(tokens, "jc-keyword"); //$NON-NLS-1$
		assertNotNull(varToken);
		assertEquals("var", tokenText(code, varToken)); //$NON-NLS-1$
		JavaCodeToken declToken= findFirstToken(tokens, "jc-local-var-decl"); //$NON-NLS-1$
		assertNotNull(declToken);
		assertEquals("count", tokenText(code, declToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensArrayDecl() {
		String code= "String[] args;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken typeToken= findFirstToken(tokens, "jc-type"); //$NON-NLS-1$
		assertNotNull(typeToken);
		assertEquals("String", tokenText(code, typeToken)); //$NON-NLS-1$
		JavaCodeToken declToken= findFirstToken(tokens, "jc-local-var-decl"); //$NON-NLS-1$
		assertNotNull(declToken);
		assertEquals("args", tokenText(code, declToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensVarargs() {
		String code= "void foo(String... args) {}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken paramToken= findFirstToken(tokens, "jc-param"); //$NON-NLS-1$
		assertNotNull(paramToken);
		assertEquals("args", tokenText(code, paramToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensAnnotatedMethod() {
		String code= "@Override\npublic void run() {}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken annToken= findFirstToken(tokens, "jc-annotation"); //$NON-NLS-1$
		assertNotNull(annToken);
		assertEquals("@Override", tokenText(code, annToken)); //$NON-NLS-1$
		assertEquals(0, annToken.offset());
		JavaCodeToken methodDeclToken= findFirstToken(tokens, "jc-method-decl"); //$NON-NLS-1$
		assertNotNull(methodDeclToken);
		assertEquals("run", tokenText(code, methodDeclToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensChainedMethods() {
		String code= "list.stream().filter(x).collect();"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		List<JavaCodeToken> methodTokens= findAllTokens(tokens, "jc-method"); //$NON-NLS-1$
		assertEquals(3, methodTokens.size());
		assertEquals("stream", tokenText(code, methodTokens.get(0))); //$NON-NLS-1$
		assertEquals("filter", tokenText(code, methodTokens.get(1))); //$NON-NLS-1$
		assertEquals("collect", tokenText(code, methodTokens.get(2))); //$NON-NLS-1$
	}

	@Test
	public void testTokensOffsetAccuracy() {
		String code= "  int x = 5;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		// First token should be the leading whitespace with null cssClass
		JavaCodeToken leading= tokens.get(0);
		assertNull(leading.cssClass());
		assertEquals(0, leading.offset());
		assertEquals(2, leading.length());
		// "int" keyword at offset 2
		JavaCodeToken kwToken= tokens.get(1);
		assertEquals("jc-keyword", kwToken.cssClass()); //$NON-NLS-1$
		assertEquals(2, kwToken.offset());
		assertEquals(3, kwToken.length());
	}

	@Test
	public void testTokensComplexExample() {
		String code= "public static void main(String[] args) {\n" //$NON-NLS-1$
				+ "    List<String> items = new ArrayList<>();\n" //$NON-NLS-1$
				+ "    for (String item : items) {\n" //$NON-NLS-1$
				+ "        System.out.println(item);\n" //$NON-NLS-1$
				+ "    }\n" //$NON-NLS-1$
				+ "}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		assertNotNull(findFirstToken(tokens, "jc-method-decl")); //$NON-NLS-1$
		assertNotNull(findFirstToken(tokens, "jc-param")); //$NON-NLS-1$
		assertNotNull(findFirstToken(tokens, "jc-type")); //$NON-NLS-1$
		assertNotNull(findFirstToken(tokens, "jc-type-arg")); //$NON-NLS-1$
		assertNotNull(findFirstToken(tokens, "jc-local-var-decl")); //$NON-NLS-1$
		assertNotNull(findFirstToken(tokens, "jc-local-var")); //$NON-NLS-1$
		assertNotNull(findFirstToken(tokens, "jc-method")); //$NON-NLS-1$
		assertNotNull(findFirstToken(tokens, "jc-keyword")); //$NON-NLS-1$

		JavaCodeToken mainDecl= findFirstToken(tokens, "jc-method-decl"); //$NON-NLS-1$
		assertEquals("main", tokenText(code, mainDecl)); //$NON-NLS-1$

		JavaCodeToken paramToken= findFirstToken(tokens, "jc-param"); //$NON-NLS-1$
		assertEquals("args", tokenText(code, paramToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensFieldReference() {
		String code= "class Foo {\n    int count;\n    void inc() {\n        count++;\n    }\n}"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		List<JavaCodeToken> fieldTokens= findAllTokens(tokens, "jc-field"); //$NON-NLS-1$
		assertEquals(2, fieldTokens.size());
		assertEquals("count", tokenText(code, fieldTokens.get(0))); //$NON-NLS-1$
		assertEquals("count", tokenText(code, fieldTokens.get(1))); //$NON-NLS-1$
	}

	@Test
	public void testTokensOnlyWhitespaceAndPunctuation() {
		String code= "  { }  "; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		for (JavaCodeToken token : tokens) {
			assertNull(token.cssClass());
		}
	}

	@Test
	public void testTokensCommentAmidCode() {
		String code= "int /* mid */ x = 5;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		JavaCodeToken kwToken= findFirstToken(tokens, "jc-keyword"); //$NON-NLS-1$
		assertNotNull(kwToken);
		assertEquals("int", tokenText(code, kwToken)); //$NON-NLS-1$
		JavaCodeToken cmtToken= findFirstToken(tokens, "jc-comment"); //$NON-NLS-1$
		assertNotNull(cmtToken);
		assertEquals("/* mid */", tokenText(code, cmtToken)); //$NON-NLS-1$
	}

	@Test
	public void testTokensEnumQualifiedAccess() {
		String code= "enum Rgb {\n    RED, GREEN, BLUE;\n}\nRgb field = Rgb.RED;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		List<JavaCodeToken> enumTokens= findAllTokens(tokens, "jc-enum"); //$NON-NLS-1$
		assertTrue(enumTokens.size() >= 2);
		for (JavaCodeToken t : enumTokens) {
			assertEquals("Rgb", tokenText(code, t)); //$NON-NLS-1$
		}
	}

	@Test
	public void testTokensParameterScopeCleanup() {
		String code= "void foo(String bar) {\n    println(bar);\n}\nbar;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		List<JavaCodeToken> paramTokens= findAllTokens(tokens, "jc-param"); //$NON-NLS-1$
		// "bar" in param list and in body, but NOT after closing brace
		assertEquals(2, paramTokens.size());
		// The trailing "bar;" should have no CSS class (it's in a null token)
		int lastBarOffset= code.lastIndexOf("bar"); //$NON-NLS-1$
		for (JavaCodeToken t : tokens) {
			if (t.offset() == lastBarOffset) {
				assertNull(t.cssClass());
				break;
			}
		}
	}

	@Test
	public void testTokensLocalVarScopeCleanup() {
		String code= "{\n    int x = 1;\n}\nx;"; //$NON-NLS-1$
		List<JavaCodeToken> tokens= JavaCodeTokenizer.tokenizeToTokens(code);
		assertTokensCoverEntireInput(code, tokens);
		// x after closing brace should not be local-var
		int lastXOffset= code.lastIndexOf("x"); //$NON-NLS-1$
		for (JavaCodeToken t : tokens) {
			if (t.offset() == lastXOffset) {
				assertNull(t.cssClass());
				break;
			}
		}
	}
}
