/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class WhiteSpaceTabPage extends ModifyDialogTabPage implements ISelectionChangedListener, ICheckStateListener {
	
	
	private final static class Category {
		public final String previewText;
		public final String name;
		public final Option[] options;
		
		public final Collection children;
		
		public String toString() {
			return name;
		}
		
		public Category(String name, Option[] options, String previewText) {
			this.name= name;
			this.previewText= previewText;
			this.options= options;
			this.children= new ArrayList();
		}
		
		public Category(String name) {
			this(name, new Option [0], null);
		}
		
		public void addChild(Category child) {
			children.add(child);
		}
		
		public boolean hasChildren() {
			return !children.isEmpty();
		}
		
		public String getPreviewText() {
			if (previewText == null) {
				return null;
			}
			return createPreviewHeader(name) + previewText;
		}
	}
	
	private final static class Option {
		public final String key;
		public final String name;

		public Option(String key, String name) {
			this.key= key;
			this.name= name;
		}
		
		public String toString() {
			return name;
		}
	}
	
	private final Category fAssignmentCategory= new Category(FormatterMessages.getString("WhiteSpaceTabPage.assignments.title"),  //$NON-NLS-1$
	    new Option [] {
	    	new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATORS, FormatterMessages.getString("WhiteSpaceTabPage.assignments.before_assignment_operator") ), //$NON-NLS-1$
	    	new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATORS, FormatterMessages.getString("WhiteSpaceTabPage.assignemtns.after_assignment_operator") ) //$NON-NLS-1$
	},
	"class Example {" + //$NON-NLS-1$
	"void foo() {" + //$NON-NLS-1$
	"int a= 4;" + //$NON-NLS-1$
	"}" + //$NON-NLS-1$
	"}" //$NON-NLS-1$
	);
	
	private final Category fOperatorCategory= new Category( FormatterMessages.getString("WhiteSpaceTabPage.operators.title"), //$NON-NLS-1$
		new Option[] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.operators.before_binary_operators") ), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.operators.after_binary_operators") ), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.operators.before_unary_operators") ), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.operators.after_unary_operators") ), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.operators.before_prefix_operators") ), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.operators.after_prefix_operators") ), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.operators.before_postfix_operators") ), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR,  FormatterMessages.getString("WhiteSpaceTabPage.operators.after_postfix_operators") ) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"void foo() {" + //$NON-NLS-1$
		"int a = 1 + 2 - 3 * -4 / 5;" + //$NON-NLS-1$
		"Other.check( a-- );" + //$NON-NLS-1$
		"Other.check( ++a );" + //$NON-NLS-1$
		"Other.check( -a );" + //$NON-NLS-1$
		"boolean d = !Other.isValid();" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
		
	private final Category fClassCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.classes.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_TYPE_OPEN_BRACE, FormatterMessages.getString("WhiteSpaceTabPage.classes.before_opening_brace_of_a_class")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ANONYMOUS_TYPE_OPEN_BRACE, FormatterMessages.getString("WhiteSpaceTabPage.classes.before_opening_brace_of_anon_class")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES, FormatterMessages.getString("WhiteSpaceTabPage.classes.before_comma_implements")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES, FormatterMessages.getString("WhiteSpaceTabPage.classes.after_comma_implements")) //$NON-NLS-1$
		},
		"class Example extends C2 implements I1, I2, I3 {" + //$NON-NLS-1$
		"void run() {" + //$NON-NLS-1$
		"  setHandler( new IHandler() {" + //$NON-NLS-1$
		"    void handleThis(Event e) {" + //$NON-NLS-1$
		"      forward(e);" + //$NON-NLS-1$
		"    }" + //$NON-NLS-1$
		"  });" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final Category fMemberFunctionCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.memberfunctions.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FIRST_ARGUMENT, FormatterMessages.getString("WhiteSpaceTabPage.memberfunctions.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN, FormatterMessages.getString("WhiteSpaceTabPage.memberfunctions.before_closing_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.memberfunctions.between_empty_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_METHOD_OPEN_BRACE, FormatterMessages.getString("WhiteSpaceTabPage.memberfunctions.before_opening_brace")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"int fField;" + //$NON-NLS-1$
		"Example(int a, int b) throws E1, E2 {" + //$NON-NLS-1$
		"fField= a / b;" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"int foo( int a, int b, int c, int d ) {" + //$NON-NLS-1$
		"return a + b + c + d;" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"int foo() throws E1, E2 {" + //$NON-NLS-1$
		"return 0;" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final Category fMethodCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.methods.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.methods.before_comma_in_args")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.methods.after_comma_in_args")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_THROWS, FormatterMessages.getString("WhiteSpaceTabPage.methods.before_comma_in_throws")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_THROWS, FormatterMessages.getString("WhiteSpaceTabPage.methods.after_comma_in_throws")) //$NON-NLS-1$
		},
		fMemberFunctionCategory.previewText
	);
	
	private final Category fConstructorCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.constructors.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.constructors.before_comma_in_args")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.constructors.after_comma_in_args")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_THROWS, FormatterMessages.getString("WhiteSpaceTabPage.constructors.before_comma_in_throws")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_THROWS, FormatterMessages.getString("WhiteSpaceTabPage.constructors.after_comma_in_throws")) //$NON-NLS-1$
		},
		fMemberFunctionCategory.previewText
	);

	private final Category fFieldCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.fields.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, FormatterMessages.getString("WhiteSpaceTabPage.fields.before_comma")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, FormatterMessages.getString("WhiteSpaceTabPage.fields.after_comma")) //$NON-NLS-1$
		},
		"class Example { int f1=1,f2=2; }" //$NON-NLS-1$
		);
	
	
	private final Category fLocalVariableCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.localvars.title"), //$NON-NLS-1$
		new Option[] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, FormatterMessages.getString("WhiteSpaceTabPage.localvars.before_comma")), //$NON-NLS-1$
			 new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, FormatterMessages.getString("WhiteSpaceTabPage.localvars.after_comma")) //$NON-NLS-1$
		},
		"class Example { void foo() { int a=1,b=2; } }" //$NON-NLS-1$
	);
	
	private final Category fArrayInitializerCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.arrayinit.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FIRST_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.arrayinit.after_opening_brace")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.arrayinit.before_closing_brace")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.arrayinit.before_comma")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.arrayinit.after_comma")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.arrayinit.between_empty_braces")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"  final int [] fArray1= { 1, 2, 3, 4 };" + //$NON-NLS-1$
		"  final int [] fArray2= {};" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final Category fArrayDeclarationCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.arraydecls.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_TYPE_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.arraydecls.before_brackets")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.arraydecls.between_brackets")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"  int [] fArray1;" + //$NON-NLS-1$
		"  int [][] fArray2;" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final Category fArrayElementAccessCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.arrayelem.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.arrayelem.before_brackets")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.arrayelem.within_brackets")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"  int foo(int [] array) {" + //$NON-NLS-1$
		"    return array[0] + array[1];" + //$NON-NLS-1$
		"  }" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final Category fFunctionCallCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.calls.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND, FormatterMessages.getString("WhiteSpaceTabPage.calls.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND, FormatterMessages.getString("WhiteSpaceTabPage.calls.after_opening_before_closing")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MESSAGESEND_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.calls.before_comma_in_method_args")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MESSAGESEND_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.calls.after_comma_in_method_args")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.calls.before_comma_in_alloc")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.calls.after_comma_in_alloc")),				 //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.calls.before_comma_in_qalloc")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.calls.after_comma_in_qalloc")) //$NON-NLS-1$
		},
		"class Example extends AnotherExample{" + //$NON-NLS-1$
		"  IStore fStore;" + //$NON-NLS-1$
		"  IOther fOther;" + //$NON-NLS-1$
		"  Example() { this(4, 5, 6); }" + //$NON-NLS-1$
		"  Example(int a, int b, int c) { " + //$NON-NLS-1$
		"    super(a, b, c);" + //$NON-NLS-1$
		"    oneMethod( a, b, c );" + //$NON-NLS-1$
		"    anotherMethod();" + //$NON-NLS-1$
		"    fStore= new Store( 1, 2, 3 );" + //$NON-NLS-1$
		"    fOther= new Other();" + //$NON-NLS-1$
		"  }" +  //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final Category fStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.statements.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON, FormatterMessages.getString("WhiteSpaceTabPage.statements.before_semicolon")) //$NON-NLS-1$
		}, 
		"class Example {" + //$NON-NLS-1$
		"void foo(int a) {" + //$NON-NLS-1$
		"Other.initialize();" + //$NON-NLS-1$
		"Other.doSomething();" + //$NON-NLS-1$
		"Other.doSomethingElse();" + //$NON-NLS-1$
		"Other.cleanUp();" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final Category blockStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.blocks.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BLOCK_OPEN_BRACE, FormatterMessages.getString("WhiteSpaceTabPage.blocks.before_opening_brace")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BLOCK_CLOSE_BRACE, FormatterMessages.getString("WhiteSpaceTabPage.blocks.after_closing_brace")) //$NON-NLS-1$
		},
		"case IConstants.GO: Other.go(); break;" +
		"case IConstants.STOP: Other.stop(); break;" +
		"case IConstants.WAIT: Other.wait(); break;" +
		"class Example {" + //$NON-NLS-1$
		"void foo() {" + //$NON-NLS-1$
		"try {" + //$NON-NLS-1$
		"} catch (Exception e) {} finally {" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"for (int i=0; i < a; i++) {}" + //$NON-NLS-1$
		"if (true) {} else {};" + //$NON-NLS-1$
		"while( false) {}" + //$NON-NLS-1$
		"{" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"do {" + //$NON-NLS-1$
		"} while( false );" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final Category fSwitchStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.switch.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE, FormatterMessages.getString("WhiteSpaceTabPage.switch.before_case_colon")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT, FormatterMessages.getString("WhiteSpaceTabPage.switch.before_default_colon")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SWITCH_OPEN_BRACE, FormatterMessages.getString("WhiteSpaceTabPage.switch.before_opening_brace")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SWITCH_CONDITION, FormatterMessages.getString("WhiteSpaceTabPage.switch.before_condition")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_SWITCH_CONDITION, FormatterMessages.getString("WhiteSpaceTabPage.switch.in_condition")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"void foo( int a) {" + //$NON-NLS-1$
		"switch (a) {" + //$NON-NLS-1$
		"default: Other.nothingHappens();" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final Category fDoWhileCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.do.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_WHILE_CONDITION, FormatterMessages.getString("WhiteSpaceTabPage.do.before_condition")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_WHILE_CONDITION, FormatterMessages.getString("WhiteSpaceTabPage.do.in_condition")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"void foo() {" + //$NON-NLS-1$
		"  int a= 100;" + //$NON-NLS-1$
		"  while (a-- > 0 ) { Other.doSomething();};" + //$NON-NLS-1$
		" do { Other.doNothing(); } while ( a++ < 1000 );" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final Category fSynchronizedCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.synchronized.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SYNCHRONIZED_CONDITION, FormatterMessages.getString("WhiteSpaceTabPage.synchronized.before_condition")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_SYNCHRONIZED_CONDITION, FormatterMessages.getString("WhiteSpaceTabPage.synchronized.in_condition")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"Mutex fMutex;" + //$NON-NLS-1$
		"void run() {" + //$NON-NLS-1$
		"  synchronized(fMutex) {" + //$NON-NLS-1$
		"    Other.doSomething();" + //$NON-NLS-1$
		"  }" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"} " //$NON-NLS-1$
	);
	
	private final Category fTryStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.try.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CATCH_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.try.before_catch_expr")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_CATCH_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.try.in_catch_expr")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"  void foo() {" + //$NON-NLS-1$
		"    try {" + //$NON-NLS-1$
		"      return 8 / 0;" + //$NON-NLS-1$
		"    } catch (Exception e) {" + //$NON-NLS-1$
		"      System.out.println(\"Something happened\");" + //$NON-NLS-1$
		"    }" + //$NON-NLS-1$
		"  }" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final Category fIfStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.if.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_IF_CONDITION, FormatterMessages.getString("WhiteSpaceTabPage.if.before_condition")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_IF_CONDITION, FormatterMessages.getString("WhiteSpaceTabPage.if.in_condition")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"int fField;" + //$NON-NLS-1$
		"int foo(boolean a) {" + //$NON-NLS-1$
		"if (a) {" + //$NON-NLS-1$
		"return 500;" + //$NON-NLS-1$
		"} else {" + //$NON-NLS-1$
		"return 400;" + //$NON-NLS-1$
		"}}}" //$NON-NLS-1$
	);
	
	private final Category fForStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.for.title"), //$NON-NLS-1$
		new Option [] {
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FOR_PAREN, FormatterMessages.getString("WhiteSpaceTabPage.for.before_opening_paren")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_FOR_PARENS, FormatterMessages.getString("WhiteSpaceTabPage.for.after_opening_before_closing_paren")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS, FormatterMessages.getString("WhiteSpaceTabPage.for.before_comma_init")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS, FormatterMessages.getString("WhiteSpaceTabPage.for.after_comma_init")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS, FormatterMessages.getString("WhiteSpaceTabPage.for.before_comma_inc")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS, FormatterMessages.getString("WhiteSpaceTabPage.for.after_comma_inc")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR, FormatterMessages.getString("WhiteSpaceTabPage.for.after_semicolon")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"void foo() {" + //$NON-NLS-1$
		"for (int i=0, j=100; i < 10; i++, j--) {" + //$NON-NLS-1$
		"	Other.enable(i, j );" + //$NON-NLS-1$
		"}}}" //$NON-NLS-1$
	);
	
//TODO: include this category
//	private final Category fAssertCategory= new Category(
//		"'assert'",
//		new Option [] {
//			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT, "before colon"),
//			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT, "after colon")
//		},
//		"class Example {" +
//		"  void foo(int a) {" +
//		"	 assert a==0 : \"Oops\";" +
//		"  }" +
//		"}"
//	);
	
	private final Category fLabelCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.labels.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT, FormatterMessages.getString("WhiteSpaceTabPage.labels.before_colon")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT, FormatterMessages.getString("WhiteSpaceTabPage.labels.after_colon")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"  void foo() {" + //$NON-NLS-1$
		"  outer: " + //$NON-NLS-1$
		"    for (int i=0; i < 100; i++) {" + //$NON-NLS-1$
		"      for (int j=0; j < 100; j++) {" + //$NON-NLS-1$
		"        if (i+j < 100) {" + //$NON-NLS-1$
		"			continue outer;" + //$NON-NLS-1$
		"        }" + //$NON-NLS-1$
		"      }" + //$NON-NLS-1$
		"    }" + //$NON-NLS-1$
		"  }" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final Category fConditionalCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.conditionals.title"), //$NON-NLS-1$
		new Option [] {
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL, FormatterMessages.getString("WhiteSpaceTabPage.conditionals.before_question")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL, FormatterMessages.getString("WhiteSpaceTabPage.conditionals.after_question")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL, FormatterMessages.getString("WhiteSpaceTabPage.conditionals.before_colon")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL, FormatterMessages.getString("WhiteSpaceTabPage.conditionals.after_colon")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"int foo(int a) {" + //$NON-NLS-1$
		"return a > 0 ? 1 : 0;" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final Category fTypecastCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.typecasts.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST, FormatterMessages.getString("WhiteSpaceTabPage.typecasts.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST, FormatterMessages.getString("WhiteSpaceTabPage.typecasts.before_closing_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, FormatterMessages.getString("WhiteSpaceTabPage.typecasts.after_closing_paren")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"MyClass foo( Object o) {" + //$NON-NLS-1$
		"	return ((SomeClass)o).getMyClass();" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final Category fParenthesizedExpressionOptions= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.parenexpr.title"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.parenexpr.before_opening")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.parenexpr.after_opening")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.parenexpr.before_closing")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"int foo(int a, int b) {" + //$NON-NLS-1$
		"  return  ((a) + (b));" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final Category fDeclarationsCategory= new Category(FormatterMessages.getString("WhiteSpaceTabPage.decls.title")); //$NON-NLS-1$
	private final Category fExpressionsCategory= new Category(FormatterMessages.getString("WhiteSpaceTabPage.exprs.title")); //$NON-NLS-1$
	private final Category fArrayCategory= new Category(FormatterMessages.getString("WhiteSpaceTabPage.arrays.title")); //$NON-NLS-1$

	private final Collection fCategories;
	
	private TreeViewer fCategoriesViewer;
	private CheckboxTableViewer fOptionsViewer;


	
	public WhiteSpaceTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);		
		fCategories= createCategoriesTree();
	}

	/**
	 * Create the categories three
	 */
	private Collection createCategoriesTree() {
		
		fMemberFunctionCategory.addChild(fConstructorCategory);
		fMemberFunctionCategory.addChild(fMethodCategory);

		fDeclarationsCategory.addChild(fClassCategory);
		fDeclarationsCategory.addChild(fFieldCategory);
		fDeclarationsCategory.addChild(fLocalVariableCategory);
		fDeclarationsCategory.addChild(fMemberFunctionCategory);
		fDeclarationsCategory.addChild(fLabelCategory);
		
		fArrayCategory.addChild(fArrayDeclarationCategory);
		fArrayCategory.addChild(fArrayInitializerCategory);
		fArrayCategory.addChild(fArrayElementAccessCategory);
		
		fStatementCategory.addChild(blockStatementCategory);
		fStatementCategory.addChild(fIfStatementCategory);
		fStatementCategory.addChild(fDoWhileCategory);
		fStatementCategory.addChild(fForStatementCategory);
		fStatementCategory.addChild(fTryStatementCategory);
		fStatementCategory.addChild(fSwitchStatementCategory);
		fStatementCategory.addChild(fSynchronizedCategory);
		
		fExpressionsCategory.addChild(fFunctionCallCategory);
		fExpressionsCategory.addChild(fAssignmentCategory);
		fExpressionsCategory.addChild(fOperatorCategory);
		fExpressionsCategory.addChild(fParenthesizedExpressionOptions);
		fExpressionsCategory.addChild(fTypecastCategory);
		fExpressionsCategory.addChild(fConditionalCategory);

		Collection root= new ArrayList();
		root.add(fDeclarationsCategory);
		root.add(fStatementCategory);
		root.add(fExpressionsCategory);
		root.add(fArrayCategory);
		
		return root;
	}
	

	protected Composite doCreatePreferences(Composite parent) {
		
		final int numColumns= 3;

		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		// label "categories"
		createLabel(numColumns, composite, FormatterMessages.getString("WhiteSpaceTabPage.category.label.text")); //$NON-NLS-1$
		
		// tree with categories
		fCategoriesViewer= new TreeViewer(composite, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		fCategoriesViewer.setContentProvider(new ITreeContentProvider() {
			public Object[] getElements(Object inputElement) {
				return ((Collection)inputElement).toArray();
			}
			public Object[] getChildren(Object parentElement) {
				return ((Category)parentElement).children.toArray();
			}
			public Object getParent(Object element) { return null; }
			public boolean hasChildren(Object element) {
				return ((Category)element).hasChildren();
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			public void dispose() {}
		});
		
		fCategoriesViewer.setLabelProvider(new LabelProvider());
		fCategoriesViewer.setInput(fCategories);
		fCategoriesViewer.setExpandedElements(fCategories.toArray());
		
		final GridData gd= createGridData(numColumns, GridData.FILL_HORIZONTAL);
		gd.heightHint= fPixelConverter.convertHeightInCharsToPixels(17);
		fCategoriesViewer.getControl().setLayoutData(gd);
		
		// label "Insert white space..."
		createLabel(numColumns, composite, FormatterMessages.getString("WhiteSpaceTabPage.checktable.label.text")); //$NON-NLS-1$
		
		// table with checkboxes
		fOptionsViewer= CheckboxTableViewer.newCheckList(composite, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		fOptionsViewer.setContentProvider(new ArrayContentProvider());
		fOptionsViewer.setLabelProvider(new LabelProvider());
		fOptionsViewer.getControl().setLayoutData(createGridData(numColumns, GridData.FILL_BOTH));
		
		return composite;
	}
	
	protected void doInitializeControls() {
		fCategoriesViewer.addSelectionChangedListener(this);
		fOptionsViewer.addCheckStateListener(this);
		fCategoriesViewer.getTree().setFocus();
		fCategoriesViewer.setSelection( new StructuredSelection( fClassCategory ));
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
		final Category selected= (Category)((IStructuredSelection)event.getSelection()).getFirstElement();
		fJavaPreview.setPreviewText(selected.getPreviewText());
		updatePreview();
		fOptionsViewer.setInput(selected.options);

		for (int i = 0; i < selected.options.length; i++) {
			final boolean checked= fWorkingValues.get(selected.options[i].key).equals(JavaCore.INSERT);
			fOptionsViewer.setChecked(selected.options[i], checked);
		}
	}

	public void checkStateChanged(CheckStateChangedEvent event) {
		final Option option= (Option)event.getElement();
		fWorkingValues.put(option.key, event.getChecked() ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
		updatePreview();
	}
}

















