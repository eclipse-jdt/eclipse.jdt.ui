/*******************************************************************************
 * Copyright (c) 2018, 2019 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - initial API and implementation
 *******************************************************************************/
/* 
 * For each node in the formatter profile preferences tree you can define a preview
 * by putting the code between two line comments:
 * //--PREVIEW--START--<key>[:kind] 
 * //--PREVIEW--END--<key>
 * where key is:
 * - in case of specific formatter option: the option's key, as defined in
 *   DefaultCodeFormatterConstants class
 * - in case of Section: the key defined during section's creation in
 *   FormatterModifyDualog class. Some sections may not have a key defined.
 * and kind is an optional marker for kind of parser to use. Possible values are:
 * COMPILATION_UNIT, EXPRESSION, CLASS_BODY_DECLARATIONS, STATEMENTS, MODULE_INFO.
 * If kind is not defined, the formatter will try all parsers in the above
 * order. Note: module declarations should be marked with MODULE_INFO kind to
 * enable proper syntax highlight. Note 2: certain options work only when kind
 * is defined explicitly, even if the code can be recognized automatically.
 * 
 * Lines starting with "//--PREVIEW--START" or "//--PREVIEW--END" are never
 * included in the preview code, so the preveiew code can be surrounded by
 * multiple start/end comments to assign it to multiple preference nodes, or
 * even a fragment of preview can be assigned to another node (although let's
 * keep it simple and not overuse that possibility).
 * 
 * When a node does not have a preview assigned directly, then its descendants
 * are checked, level by level. The first level that has some previews is used
 * to construct the preview by concatenating previews from all the nodes in that
 * level. If none of the descendants have a preview assigned, then the closest
 * ancestor node is used instead.
 * 
 * Top level classes in this file are added for easier navigation.
 */

class INDENTATION {
//--PREVIEW--START--section-indentation
class Example {
	int[] myArray = { 1, 2, 3, 4, 5, 6 };
	String stringWithTabs = "1	2	3	4";
	String textBlock = """
first line

second line
""";

	void foo(int a, int b, int c, int d, int e, int f) {
		switch (a) {
		case 0: Other.doFoo(); break;
		default: Other.doBaz();
		}
	}
	void bar(List v) {
		for (int i = 0; i < 10; i++) { v.add(Integer.valueOf(i)); }
	}
}
enum MyEnum {
	UNDEFINED(0) {
		void foo() { }
	}
}
@interface MyAnnotation {
	int count() default 1;
}
record MyRecord(int first, int second) {
	MyRecord() {
		Other.doFoo();
	}
}
//--PREVIEW--END--section-indentation

//--PREVIEW--START--section-indentation-align-on-column
class Example {
	int[] myArray = { 1, 2, 3, 4, 5, 6 };
	int theInt = 1;

	String someString = "Hello";
	String stringWithTabs = "1	2	3	4";
	double aDouble = 3.0;

	void foo() {
		int i = 0;
		String str = "123456";
		Object object = null;

		final Object unchanged = new Object();

		while (i < 10) {
			str = i + str;
			object = Arrays.asList(str);
			i += 2;
		}
	}
}
//--PREVIEW--END--section-indentation-align-on-column
}

class BRACES {
//--PREVIEW--START--section-braces
interface Empty {}

enum MyEnum {
	UNDEFINED(0) {
		void foo() {}
	}}
@interface SomeAnnotationType {}
record MyRecord(int first, int second) {
	MyRecord {}
}
class Example {
	SomeClass fField= new SomeClass() {  };
	int [] myArray= {1,2,3,4,5,6};
	int [] emptyArray= new int[] {};
	Example() {
		Runnable r = () -> { fField.set(20); };
	} 
	void bar(int p) {
		for (int i= 0; i<10; i++) {    }    
		switch(p) {      
			case 0:        fField.set(0);        break;      
			case 1: {        break;        }      default:        fField.reset();   
		}  
	}}
//--PREVIEW--END--section-braces
}

class PARENTHESES {
//--PREVIEW--START--section-parentheses
public class Example {
	enum SomeEnum {
		VALUE1(), VALUE2("example")
	}
	record MyRecord(int first, int second) {
	}
	@SomeAnnotation(key1 = "value1", key2 = "value2")
	void method1() {
		for (int counter = 0; counter < 100; counter++) {
			if (counter % 2 == 0 && counter % 7 == 0 && counter % 13 == 0) {
				try (AutoCloseable resource = null) {
					// read resource
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	@Deprecated()
	void method2(String argument
	) {
		this.method3(this, this, this, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
				"ccccccccccccccccccc");
		do {
			this.method1();
		} while (this.toString()//
				.contains(argument));
	}
	void method3(
			Example argument1, Example argument2, Example argument3, String argument4, String argument5,
			String argument6) {
		method1();
		while (argument1.toString().contains(argument4)
		) {
			argument1.method2(argument5);
		}
	}
	java.util.function.BiConsumer<Integer, Integer> lambda = (Integer a, Integer b) -> {
		switch (a.intValue()) {
			case 0:
				break;
		}
	};
}
//--PREVIEW--END--section-parentheses

//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_method_delcaration
public void emtpyFoo() {doSomething();}
public void shortFoo(String a, String b) {doSomething();}
public void longFoo(Character argument1, Character argument2, Character argument3, Character argument4, Character argument5) {soSomething();}
public void mixedFoo1(
		String argument) { doSomething(); }
public void mixedFoo2(String argument
		) { doSomething(); }
public void mixedFoo3(
		String argument
		) {doSomething(); }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_method_delcaration

{
//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_method_invocation
emtpyFoo();

shortFoo(a, b);

longFoo(argument1, argument2, argument3, argument4, argument5, argument6, argument7, argument8, argument9, argument9);

mixedFoo1(
		argument);

mixedFoo2(argument
		);

mixedFoo3(
		argument
		);
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_method_invocation
}

//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_enum_constant_declaration
enum MyEnum {
	FOO_EMPTY(),
	FOO_SHORT("A", "B", "C"),
	FOO_LONG("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "cccccccccccccccccccccccccccccccccc"),
	FOO_MIXED1(
			"A", "B", "C"),
	FOO_MIXED2("A", "B", "C"
			),
	FOO_MIXED3(
			"A", "B", "C"
			);
	public MyEnum() { }
	public MyEnum(String arg1, String arg2, String arg3) { }
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_enum_constant_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_record_declaration
record EmptReport() {}
record ShortRecord(int foo) {}
record LongRecord(int component1, int component2, int component3, int component4) {}
record MixedRecord1(
		int foo, int bar) {}
record MixedRecord2(int foo, int bar
		) {}
record MixedRecord1(
		int foo, int bar
		) {}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_record_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_annotation
@EmptyAnnotation()
@ShortAnnotation("foo")
@LongAnnotation(key1 = "value1", key2 = "value2", key3 = "value3", key4 = "value4")
@MixedAnnotation1(
		"foo")
@MixedAnnotation2("foo"
		)
@MixedAnnotation3(
		"foo"
		)
int a;
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_annotation

{
//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_lambda_declaration
emptyLambda(() -> {doSomething();});

shorLambda((Integer a, Integer b) -> { doSomething(); });

longLambda((Integer argument1, Integer argument2, Integer argument3, Integer argument4, Integer argument5) -> { doSomething(); });

mixedLambda1((
		Integer a) -> { doSomething(); });

mixedLambda2((Integer a
		) -> { doSomething(); });

mixedLambda3((
		Integer a
		) -> { doSomething(); });
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_lambda_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_if_while_statement
if (shortCondition) { doSometing(); }

if (longCondition("       1        ") && longCondition("       2        ") && longCondition("       2        ")) { doSometing(); }

while (shortCondition) { doSometing(); }

do { doSometing(); } while (longCondition("       1        ") && longCondition("       2        ") && longCondition("       2        "));

if (
		mixedCondition1) { doSometing(); }

if (mixedCondition2
		) { doSometing(); }

if (
		mixedCondition3
		) { doSometing(); }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_if_while_statement

//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_for_statment
for (int i = 0 ; i < 5; i++) { doSometing(); }

for (long longValue = someStartingValue(); longValue < getFinishValueProvider().someFinishValue(); longValue += someIncrementValue()) { doSometing(); }

for (String s : stringValues()) { doSometing(); }

for (
		String s : mixed1())
	for (String s : mixed2()
			) {
		for (
				String s : mixed3()
				)
			doSometing();
	}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_for_statment

//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_switch_statement
switch (value) { case 0: doSomething(); }

switch (getSomeValueProviderFactory().getSomeValueProvider().getSomeValue(someArgument1, someArgument1)) { case 0: doSomething(); }

switch (
		mixed1) { case 0: doSomething(); }

switch (mixed2
		) { case 0: doSomething(); }

switch (
		mixed3
		) { case 0: doSomething(); }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_switch_statement

//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_try_clause
//--PREVIEW--START--org.eclipse.jdt.core.formatter.parentheses_positions_in_catch_clause
try (Resource s = getResource()) {
	doSomething(s);
} catch (Exception e) {
	handleException(e);
}

try (Resouce resource = getSomeResourceProviderFactory().getSomeResourceProvider().getSomeResource(argument1, argument2)) {
	soSomething(resource);
} catch (SomeCheckeckException1 | SomeCheckedException2 | SomeCheckedException3 | SomeCheckedException4 e) {
	handleException(e);
}

try (Resource r1 = mixedResource1(); Resource r2 = mixedResource2()
) {
	try {
		doSomething(r1);
	} catch (
			Exception1 e) {
		doSomething(r2);
	} catch (Exception2 e
			) {
		doSomething(null);
	} catch (
			Exception3 e
			) {
		//
	}
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_try_clause
//--PREVIEW--END--org.eclipse.jdt.core.formatter.parentheses_positions_in_catch_clause
}

}

class WHITESPACE {
//--PREVIEW--START--section-whitespace
//--PREVIEW--END--section-whitespace

class DECLARATIONS{
//--PREVIEW--START--section-whitespace-declarations-classes
class MyClass implements I0, I1, I2 {}

AnonClass object= new AnonClass() {void foo(Some s) { }};
//--PREVIEW--END--section-whitespace-declarations-classes

//--PREVIEW--START--section-whitespace-declarations-fields
class MyClass { int a=0,b=1,c=2,d=3; }
//--PREVIEW--END--section-whitespace-declarations-fields

//--PREVIEW--START--section-whitespace-declarations-localvars
void foo() {
	int a= 0, b= 1, c= 2, d= 3;
}
//--PREVIEW--END--section-whitespace-declarations-localvars

//--PREVIEW--START--section-whitespace-declarations-constructors
MyClass() throws E0, E1 { this(0,0,0);}

MyClass(int x, int y, int z) throws E0, E1 { super(x, y, z, true);}
//--PREVIEW--END--section-whitespace-declarations-constructors

//--PREVIEW--START--section-whitespace-declarations-methods
void foo() throws E0, E1 {};

void bar(int x, int y) throws E0, E1 {}

void format(String s, Object... args) {}
//--PREVIEW--END--section-whitespace-declarations-methods

//--PREVIEW--START--section-whitespace-declarations-labels
void foo() {
label: for (int i = 0; i < list.length; i++) {
	for (int j = 0; j < list[i].length; j++) continue label;
}
}
//--PREVIEW--END--section-whitespace-declarations-labels

//--PREVIEW--START--section-whitespace-declarations-annotations
@Annot(x = 23, y = -3) public class A { }
//--PREVIEW--END--section-whitespace-declarations-annotations

//--PREVIEW--START--section-whitespace-declarations-enums
enum MyEnum { GREEN(0, 1), RED() { void process() {} } }
//--PREVIEW--END--section-whitespace-declarations-enums

//--PREVIEW--START--section-whitespace-declarations-annotationtypes
@interface MyAnnotation { String value(); }

@interface OtherAnnotation { }
//--PREVIEW--END--section-whitespace-declarations-annotationtypes

//--PREVIEW--START--section-whitespace-declarations-records
record MyRecord(int first, int second, int third) {
	public MyRecord {
		Other.foo();
	}
}
//--PREVIEW--END--section-whitespace-declarations-records

//--PREVIEW--START--section-whitespace-declarations-lambdas
Runnable r = () -> process();
//--PREVIEW--END--section-whitespace-declarations-lambdas
}

void CONTROL_STATEMENTS() {

//--PREVIEW--START--section-whitespace-statements-blocks
//--PREVIEW--START--section-whitespace-statements-if
if (condition) { return foo; } else { return bar; }
//--PREVIEW--END--section-whitespace-statements-blocks
//--PREVIEW--END--section-whitespace-statements-if

//--PREVIEW--START--section-whitespace-statements-for
for (int i = 0, j = array.length; i < array.length; i++, j--) {}
for (String s : names) {}
//--PREVIEW--END--section-whitespace-statements-for

//--PREVIEW--START--section-whitespace-statements-switch
Color newColor = switch(color) {
case RED -> GREEN;
case GREEN,BLUD->RED;
default->BLACK;
};

switch (color) { 
case RED:return GREEN;
case GREEN,BLUE:return RED;
default:return BLACK;
}
//--PREVIEW--END--section-whitespace-statements-switch

//--PREVIEW--START--section-whitespace-statements-while
while (condition) {};
do {} while (condition);
//--PREVIEW--END--section-whitespace-statements-while

//--PREVIEW--START--section-whitespace-statements-synchronized
synchronized (list) {list.add(element);}
//--PREVIEW--END--section-whitespace-statements-synchronized

//--PREVIEW--START--section-whitespace-statements-trywithresources
try (FileReader reader1 = new FileReader("file1"); FileReader reader2 = new FileReader("file2")) {}
//--PREVIEW--END--section-whitespace-statements-trywithresources

//--PREVIEW--START--section-whitespace-statements-catch
try { number = Integer.parseInt(value);} catch (NumberFormatException e) {}
//--PREVIEW--END--section-whitespace-statements-catch

//--PREVIEW--START--section-whitespace-statements-assert
assert condition : reportError();
//--PREVIEW--END--section-whitespace-statements-assert

//--PREVIEW--START--section-whitespace-statements-return
return(o);
//--PREVIEW--END--section-whitespace-statements-return

//--PREVIEW--START--section-whitespace-statements-throw
throw(e);
//--PREVIEW--END--section-whitespace-statements-throw

//--PREVIEW--START--org.eclipse.jdt.core.formatter.insert_space_before_semicolon
int a = 4;foo();bar(x, y);
//--PREVIEW--END--org.eclipse.jdt.core.formatter.insert_space_before_semicolon
}

void EXPRESSIONS() {

//--PREVIEW--START--section-whitespace-expressions-calls
foo();
bar(x, y);

String str = new String();
Point point = new Point(x, y);

class MyClass extends OtherClass {
	MyClass() throws E0, E1 { this(0, 0, 0); }
	MyClass(int x, int y, int z) throws E0, E1 { super(x, y, z, true); }
}
//--PREVIEW--END--section-whitespace-expressions-calls

//--PREVIEW--START--section-whitespace-expressions-assignments
//--PREVIEW--START--section-whitespace-expressions-unaryoperators
//--PREVIEW--START--section-whitespace-expressions-binaryoperators
List list = new ArrayList();
int a = -4 + -9;
int b = a++ / --number;
b = (a++) / (--number) + (-9);
String d = "a = " + a;
if (a == b && a > c && !condition)
	c += (a >> 5) & 0xFF;
//--PREVIEW--END--section-whitespace-expressions-assignments
//--PREVIEW--END--section-whitespace-expressions-unaryoperators
//--PREVIEW--END--section-whitespace-expressions-binaryoperators

//--PREVIEW--START--section-whitespace-expressions-parenexpr
result = (a * (b + c + d) * (e + f));
//--PREVIEW--END--section-whitespace-expressions-parenexpr

//--PREVIEW--START--section-whitespace-expressions-typecasts
String s = ((String) object);
//--PREVIEW--END--section-whitespace-expressions-typecasts

//--PREVIEW--START--section-whitespace-expressions-conditionals
String value = condition ? TRUE : FALSE;
//--PREVIEW--END--section-whitespace-expressions-conditionals

void ARRAYS() {
	
//--PREVIEW--START--section-whitespace-arrays-declarations
//--PREVIEW--START--section-whitespace-arrays-allocations
//--PREVIEW--START--section-whitespace-arrays-initializers
int[] array0 = new int[] {};
int[] array1 = new int[] { 1, 2, 3 };
int[] array2 = new int[3];
//--PREVIEW--END--section-whitespace-arrays-declarations
//--PREVIEW--END--section-whitespace-arrays-allocations
//--PREVIEW--END--section-whitespace-arrays-initializers

//--PREVIEW--START--section-whitespace-arrays-references
array[i].foo();
//--PREVIEW--END--section-whitespace-arrays-references

}

void PARAMETERIZED_TYPES() {

//--PREVIEW--START--section-whitespace-parameterizedtypes-references
Map<String, Element> map = new HashMap<String, Element>();
//--PREVIEW--END--section-whitespace-parameterizedtypes-references

//--PREVIEW--START--section-whitespace-parameterizedtypes-arguments
x.<String, Element>foo();
//--PREVIEW--END--section-whitespace-parameterizedtypes-arguments

//--PREVIEW--START--section-whitespace-parameterizedtypes-parameters
class MyGenericType<S, T extends Element & List> {}
//--PREVIEW--END--section-whitespace-parameterizedtypes-parameters

//--PREVIEW--START--section-whitespace-parameterizedtypes-wildcards
Map<X<?>, Y<? extends K, ? super V>> t;
//--PREVIEW--END--section-whitespace-parameterizedtypes-wildcards

}

}

class BLANK_LINES {
//--PREVIEW--START--section-blank-lines
package foo.bar.baz;

/* example comment */

import java.util.List;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.ASTParser;

public class Example {
	public interface ExampleProvider {
		Example getExample();
		List<Example> getManyExamples();
	}
	public class Pair {
		String left;
		String right;
	}
	public Example() {
		initialize(1);
	}
	protected void initialize(int value) {
		Pair pair = new Pair();
		for (int i = 0; i < value; i++) {
			int square = i * i;
			// Between here...










			// ...and here are 10 blank lines
			pair.left = pair.left + square;
		}
		switch (value) {
		case 1:
			pair.right = "";
			break;
		default:
			pair.left = "";
		}
	}
}
class Another {




}
//--PREVIEW--END--section-blank-lines
}

class NEW_LINES {
//--PREVIEW--START--section-newlines:COMPILATION_UNIT
@Deprecated
package com.example; // annotation on package is only allowed in package-info.java

public class Empty {}
@Deprecated class Example {  @Deprecated static int [] fArray= {1, 2, 3, 4, 5 };  Listener fListener= new Listener() {  };
  @Deprecated @Override   public void
bar
(final @SuppressWarnings("unused")
 int i)
 {
@SuppressWarnings("unused") final @Positive int k;
}
  void foo() {    ;;   }
  void empty(@SuppressWarnings("unused") final int i) { }}
enum MyEnum {    @Deprecated UNDEFINED(0) { }}
//--PREVIEW--END--section-newlines

//--PREVIEW--START--section-newlines-controlstatements
class Example {
	void bar() {
		label: do { } while (true);
		try { } catch (Exception e) { } finally { }
	}
	void foo2() {
		if (true) { return; } else if (false) { return; } else { return; }
	}
	void foo(int state) {
		if (true) return;
		if (true) return; else if (false) return; else return;
	}
}
//--PREVIEW--START--section-newlines-controlstatements-simpleloops
class WrapExample {
	void bar2() {
		while(!stop)doSomething();
		for(String s : myStrings)System.out.println(s);
		do doSomethingElse();while(!stop);
	}
}
//--PREVIEW--END--section-newlines-controlstatements-simpleloops
//--PREVIEW--END--section-newlines-controlstatements

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_loop_body_block_on_one_line
class Example{
	public void example() {
		for (int i = 0; i < 10; i++) {
		}
		int a = 10;
		while (a-- > 0) { System.out.println(a); }
		do { a += 2;
		System.out.println(a); } while(a < 50);
	}
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_loop_body_block_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_if_then_body_block_on_one_line
class Example {
	public String example(int a) {
		if (a < 0) { 
			throw new IllegalArgumentException(); }
		if (a == 0) { return null; }
		if (false) {}
		if (a % 3 == 0) {
			System.out.println("fizz"); }
		if (a % 5 == 0) { System.out.println("buzz"); return ""; }
		return Integer.toString(a);
	}
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_if_then_body_block_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_lambda_body_block_on_one_line
class Example {
	Runnable emptyLambda = () -> {};
	Runnable emptyLambda2 = () -> {
	};
	Runnable tinyLambda = () -> { doSomething(); };
	Runnable smallLambda = () -> { doFirstThing(); doSecondThing(); };
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_lambda_body_block_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_code_block_on_one_line
class Example {
	static {
	}
	
	void foo() {
		if (true) {} else {}
		synchronized(this) {}
		try {} finally {}
		
		labeled:{}
	}
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_code_block_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_method_body_on_one_line
public class Example {
	private int something;
	public int getSomething() { return something; }
	public void setSomehing(int something) { this.something = something; }
	public void doNoting() {}
	public void doOneThing() { System.out.println();
	}
	public void doMoreThings() { something = 4; doOneThing(); doOneThing(); }
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_method_body_on_one_line


//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_type_declaration_on_one_line
public class EmptyClass{}
public class TinyClass{ 
	int a; }
public class SmallClass{ int a; String b; }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_type_declaration_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_anonymous_type_declaration_on_one_line
public class AnonymousClasses {
	EmptyClass emptyAnonymous = new EmptyClass() {
	};
	TinyClass tinyAnonymous = new TinyClass() { String b; };
	Object o = new SmallClass() { int a; int getA() { return a; } };
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_anonymous_type_declaration_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_enum_declaration_on_one_line
public enum EmptyEnum {}
public enum TinyEnum{ A;
}
public enum SmallEnum{ VALUE(0); SmallEnum(int val) {}; }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_enum_declaration_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_enum_constant_declaration_on_one_line
public enum EnumConstants {
	EMPTY {
	},
	TINY { int getVal() { return 2; }},
	SMALL { int val = 3; int getVal() { return 3; }};
	int getVal() { return 1; }
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_enum_constant_declaration_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_record_declaration_on_one_line
public record EmptyRecord(int a, int b) {}
public record TinyRecord(int a, int b) {
	static int field;
}
public record SmallRecord(int a, int b) { static int field1; static int field2; }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_record_declaration_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_record_constructor_on_one_line
public record EmptyCompactConstructor(int a, int b) { public EmptyCompactConstructor {} }
public record TinyCompactConstructor(int a, int b) { public TinyCompactConstructor {
	this.a = a; 
}}
public record SmallCompactConstructor(int a, int b) { public SmallCompactConstructor { this.a = a; this.b = b; } }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_record_constructor_on_one_line

//--PREVIEW--START--org.eclipse.jdt.core.formatter.keep_annotation_declaration_on_one_line
public @interface EmptyInterface {}
public @interface TinyInterface { 
	void run(); }
public @interface SmallInteface { int toA(); String toB(); }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.keep_annotation_declaration_on_one_line

}

class LINE_WRAPING {
	
//--PREVIEW--START--section-linewrap
public class Example {
	public List<Integer> list = Arrays.asList(
			111111, 222222, 333333,
			444444, 555555, 666666,
			777777, 888888, 999999, 000000);
	public int[] array = { 111111, 222222, 333333, 
			444444, 555555, 666666,
			777777, 888888, 999999, 000000 };
	public int expression = (111111 + 222222 + 333333) * (444444 + 555555 + 666666) * (777777 * 888888 * 999999 * 000000);
}
//--PREVIEW--END--section-linewrap

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_annotation
@MyAnnotation(value1 = "this is an example", value2 = "of an annotation", value3 = "with several arguments", value4 = "which may need to be wrapped")
class Example {}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_annotation

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_superclass_in_type_declaration
class Example extends OtherClass {}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_superclass_in_type_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_superinterfaces_in_type_declaration
class Example implements I1, I2, I3 {}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_superinterfaces_in_type_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_parameters_in_constructor_declaration
class Example {Example(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) { this();}Example() {}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_parameters_in_constructor_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_throws_clause_in_constructor_declaration
class Example {Example() throws FirstException, SecondException, ThirdException {  return Other.doSomething();}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_throws_clause_in_constructor_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_method_declaration
class Example {public final synchronized java.lang.String a_method_with_a_long_name() {}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_method_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_parameters_in_method_declaration
class Example {void foo(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_parameters_in_method_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_throws_clause_in_method_declaration
class Example {int foo() throws FirstException, SecondException, ThirdException {  return Other.doSomething();}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_throws_clause_in_method_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_enum_constants
enum Example {CANCELLED, RUNNING, WAITING, FINISHED }enum Example {GREEN(0, 255, 0), RED(255, 0, 0)  }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_enum_constants

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_superinterfaces_in_enum_declaration
enum Example implements A, B, C {}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_superinterfaces_in_enum_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_enum_constant
enum Example {GREEN(0, 255, 0), RED(255, 0, 0)  }
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_enum_constant

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_record_components
record Example(int firstNumber, int secondNumbere, String string) {}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_record_components
//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_superinterfaces_in_record_declaration
record Example(int first, int second) implements InterfaceA, InterfaceB, InterfaceC {}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_superinterfaces_in_record_declaration

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_method_invocation
class Example {void foo() {Other.bar( 100,
nested(200,
300,
400,
500,
600,
700,
800,
900 ));}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_method_invocation

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_selector_in_method_invocation
class Example {int foo(Some a) {return a.getFirst();}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_selector_in_method_invocation

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_explicit_constructor_call
class Example extends AnotherClass {Example() {super(100,
200,
300,
400,
500,
600,
700);}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_explicit_constructor_call

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_allocation_expression
class Example {SomeClass foo() {return new SomeClass(100,
200,
300,
400,
500,
600,
700,
800,
900 );}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_allocation_expression

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_qualified_allocation_expression
class Example {SomeClass foo() {return SomeOtherClass.new SomeClass(100,
200,
300,
400,
500 );}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_arguments_in_qualified_allocation_expression

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_multiplicative_operator
//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_additive_operator
//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_string_concatenation
//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_shift_operator
//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_relational_operator
//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_bitwise_operator
//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_logical_operator
class Example {

boolean firstIsGreater = 11111111 + 22222222 +
33333333 + 44444444 +
55555555 + 66666666
>
1.11111111 * 2.22222222 * 3.33333333
* 4.44444444 * 5.55555555 * 6.66666666;

String concatenatedString = "one two three four " + "five six seven eight " + "nine ten eleven twelve";

int shiftedInteger = 0xCAFEFACE >>> 0x00000001
		>>>
		0x00000002
		<<
		0x00000003 >>> 0x00000004;

int bitAritmetic = 0xCAFEFACE | 0x01010101 & 0x02020202 ^ 0x03030303 ^ 0x04040404 | 0x05050505;

boolean multipleConditions = conditionOne && conditionTwo || conditionThree && conditionFour || conditionFive;

}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_multiplicative_operator
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_additive_operator
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_string_concatenation
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_shift_operator
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_relational_operator
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_bitwise_operator
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_logical_operator

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_conditional_expression
//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_conditional_expression_chain
class Example extends AnotherClass {int foo(boolean argument) {
	boolean someValue = condition1() ? value1
	        : condition2() ? value2
	        : condition3 ? value3
	        : value4;
	return argument ? 100000 : 200000;
}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_conditional_expression
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_conditional_expression_chain

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_expressions_in_array_initializer
class Example {int [] fArray= {1,
2,
3,
4,
5,
6,
7,
8,
9,
10,
11,
12};}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_expressions_in_array_initializer

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_assignment
class Example {private static final String string = "TextTextText";void foo() {for (int i = 0; i < 10; i++) {}String s;s = "TextTextText";}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_assignment

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_expressions_in_for_loop_header
class Example {
	void foo(int argument) {
		for (int counter = 0; counter < argument; counter++) {
			doSomething(counter);
		}}}

//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_expressions_in_for_loop_header

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_compact_if
class Example {int foo(int argument) {  if (argument==0) return 0;  if (argument==1) return 42; else return 43;}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_compact_if

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_compact_loops
class Example {int foo(int argument) {  while(!stop)doSomething();  for(String s : myStrings)System.out.println(s);  do doSomethingElse();while(!stop);}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_compact_loops

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_resources_in_try
class Example {void foo() {try (FileReader reader1 = new FileReader("file1");   FileReader reader2 = new FileReader("file2")) {}}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_resources_in_try

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_union_type_in_multicatch
class Example {void foo() {try {} catch (IllegalArgumentException | NullPointerException | ClassCastException e) {  e.printStackTrace();}}}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_union_type_in_multicatch

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_parameterized_type_references
class Example {
	Map<String, ? extends java.lang.Object> map = new HashMap<String, java.lang.Object>();
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_parameterized_type_references

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_type_arguments
class Example {
	void foo(Some someArgument) {
		someArgument.<String, SomeElement, Example>bar();
	}
}
//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_type_arguments

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_type_parameters
class Example<S, T extends Element & List, U> {
}

//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_type_parameters

//--PREVIEW--START--org.eclipse.jdt.core.formatter.alignment_for_module_statements:MODULE_INFO
module example.module0 {
	provides example.other.module1.SomeService with example.module0.Service1, example.module0.Service2;
}

//--PREVIEW--END--org.eclipse.jdt.core.formatter.alignment_for_module_statements

}

class COMMENTS {
//--PREVIEW--START--section-comments:COMPILATION_UNIT
/**
* An example for comment formatting. This example is meant to illustrate the various possibilities offered by <i>Eclipse</i> in order to format comments.
*/
package mypackage;
/**
 * This is the comment for the example interface.
 */
 interface Example {
// This is a long comment    with	whitespace     that should be split in multiple line comments in case the line comment formatting is enabled
int foo3();
 
//	void commented() {
//			System.out.println("indented");
//	}

	//	void indentedCommented() {
	//			System.out.println("indented");
	//	}

/* block comment          on first column*/
 int bar();
	/*
	*
	* These possibilities include:
	* <ul><li>Formatting of header comments.</li><li>Formatting of Javadoc tags</li></ul>
	*/
 int bar2(); // This is a long comment that should be split in multiple line comments in case the line comment formatting is enabled
 /**
 * The following is some sample code which illustrates source formatting within javadoc comments:
 * <pre>public class Example {final int a= 1;final boolean b= true;}</pre>
 * 
 * 
 * 
 * Descriptions of parameters and return values are best appended at end of the javadoc comment.
 * @param first The first parameter. For an optimum result, this should be an odd number
 * between 0 and 100.
 * @param second The second parameter.
 * @throws Exception when the foo operation cannot be performed for one reason or another.
 * @return The result of the foo operation, usually an even number within 0 and 1000.
 */ int foo(int first, int second) throws Exception;
}
class Test {
		void trailingCommented() {
				System.out.println("indented");		// comment
				System.out.println("indent");		// comment
		}
}

//--PREVIEW--END--section-comments
}

class OFF_ON {
//--PREVIEW--START--section-offon
void method1()   {  doSomething();  }

// @formatter:off
void method2()   {  doSomething();  }
// @formatter:on

void method3()   {  doSomething();  }

/* @formatter:off                                           */
              void
              foo()
              ;
              

//--PREVIEW--END--section-offon
}
