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
	
	
	private final Category assignmentCategory= new Category("Assignments", 
			new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATORS, "before assignment operator" ),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATORS, "after assignment operator" )
	},
	"class Example {" +
	"void foo() {" +
	"int a= 4;" +
	"}" +
	"}"
	);
	
	
	
	private final Category operatorCategory= new Category( "Operators",
		new Option[] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, "before binary operators" ),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, "after binary operators" ),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR, "before unary operators" ),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR, "after unary operators" ),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR, "before prefix operators" ),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR, "after prefix operators" ),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR, "before postfix operators" ),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR,  "after postfix operators" )
		},
		"class Example {" +
		"void foo() {" +
		"int a = 1 + 2 - 3 * -4 / 5;" +
		"Other.check( a-- );" +
		"Other.check( ++a );" +
		"Other.check( -a );" +
		"boolean d = !Other.isValid();" +
		"}}"
		);
	
	
	private final Category classCategory= new Category(
		"Classes",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_TYPE_OPEN_BRACE, "before opening brace of a class"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ANONYMOUS_TYPE_OPEN_BRACE, "before opening brace of an anonymous class"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES, "before comma in implements clause "),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES, "after comma in implements clause")
		},
		"class Example extends C2 implements I1, I2, I3 {" +
		"void run() {" +
		"  setHandler( new IHandler() {" +
		"    void handleThis(Event e) {" +
		"      forward(e);" +
		"    }" +
		"  });" +
		"}" +
		"}"
		);

	
	private final Category memberFunctionCategory= new Category(
		"Member Functions",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FIRST_ARGUMENT, "after opening parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN, "before closing parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS, "between empty parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_METHOD_OPEN_BRACE, "before opening brace")
		},
		"class Example {" +
		"int fField;" +
		"Example(int a, int b) throws E1, E2 {" +
		"fField= a / b;" +
		"}" +
		"int foo( int a, int b, int c, int d ) {" +
		"return a + b + c + d;" +
		"}" +
		"int foo() throws E1, E2 {" +
		"return 0;" +
		"}}"
	);
	
	
	private final Category methodCategory= new Category(
		"Methods",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS, "before comma in arguments"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_ARGUMENTS, "after comma in arguments"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_THROWS, "before comma in throws clause"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_THROWS, "after comma in throws clause")
		},
		memberFunctionCategory.previewText
	);
	
	
	private final Category constructorCategory= new Category(
		"Constructors",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_ARGUMENTS, "before comma in arguments"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_ARGUMENTS, "after comma in arguments"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_THROWS, "before comma in throws clause"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_THROWS, "after comma in throws clause")
		},
		memberFunctionCategory.previewText
	);
						
	
	
	private final Category fieldCategory= new Category(
		"Fields",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, "before comma in multiple field declarations"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, "after comma in multiple field declarations")
		},
		"class Example { int f1=1,f2=2; }"
		);
	
	
	private final Category localVariableCategory= new Category(
		"Local variables",
		new Option[] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, "before comma in multiple local declarations"),
			 new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, "after comma in multiple local declarations")
		},
		"class Example { void foo() { int a=1,b=2; } }"
	);
	
	private final Category arrayInitializerCategory= new Category(
		"Array initializers",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FIRST_INITIALIZER, "after opening brace"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, "before closing brace"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER, "before comma"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER, "after comma"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARRAY_INITIALIZER, "between empty braces")
		},
		"class Example {" +
		"  final int [] fArray1= { 1, 2, 3, 4 };" +
		"  final int [] fArray2= {};" +
		"}"
	);
	
	private final Category arrayDeclarationCategory= new Category(
		"Array declarations",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_TYPE_REFERENCE, "before brackets"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE, "between brackets")
		},
		"class Example {" +
		"  int [] fArray1;" +
		"  int [][] fArray2;" +
		"}"
	);
	
	private final Category arrayElementAccessCategory= new Category(
		"Array element access",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_REFERENCE, "before brackets"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_REFERENCE, "within brackets")
		},
		"class Example {" +
		"  int foo(int [] array) {" +
		"    return array[0] + array[1];" +
		"  }" +
		"}"
		);
		
	
	private final Category functionCallCategory= new Category(
		"Method and constructor calls",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND, "before opening parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND, "after opening and before closing parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MESSAGESEND_ARGUMENTS, "before comma in method arguments"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MESSAGESEND_ARGUMENTS, "after comma in method arguments"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION, "before comma in object allocation arguments"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION, "after comma in object allocation arguments"),				
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS, "before comma in qualified object allocation arguments"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS, "after comma in qualified object allocation arguments")
		},
		"class Example extends AnotherExample{" +
		"  IStore fStore;" +
		"  IOther fOther;" +
		"  Example() { this(4, 5, 6); }" +
		"  Example(int a, int b, int c) { " +
		"    super(a, b, c);" +
		"    oneMethod( a, b, c );" +
		"    anotherMethod();" +
		"    fStore= new Store( 1, 2, 3 );" +
		"    fOther= new Other();" +
		"  }" + 
		"}"
	);
	
	
	
	
	
	private final Category statementCategory= new Category(
		"Control statements",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON, "before semicolon")
		}, 
		"class Example {" +
		"void foo(int a) {" +
		"Other.initialize();" +
		"Other.doSomething();" +
		"Other.doSomethingElse();" +
		"Other.cleanUp();" +
		"}}"
	);
	
	private final Category blockStatementCategory= new Category(
		"Blocks",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BLOCK_OPEN_BRACE, "before opening brace"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BLOCK_CLOSE_BRACE, "after closing brace")
		},
		"class Example {" +
		"void foo() {" +
		"try {" +
		"} catch (Exception e) {} finally {" +
		"}" +
		"for (int i=0; i < a; i++) {}" +
		"if (true) {} else {};" +
		"while( false) {}" +
		"{" +
		"}" +
		"do {" +
		"} while( false );" +
		"}}"
	);
	
	
	private final Category switchStatementCategory= new Category(
		"'switch case'",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE, "before colon in case"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT, "before colon in default"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SWITCH_OPEN_BRACE, "before opening brace"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SWITCH_CONDITION, "before condition"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_SWITCH_CONDITION, "in condition")
		},
		"class Example {" +
		"void foo( int a) {" +
		"switch (a) {" +
		"case IConstants.GO: Other.go(); break;" +
		"case IConstants.STOP: Other.stop(); break;" +
		"case IConstants.WAIT: Other.wait(); break;" +
		"default: Other.nothingHappens();" +
		"}" +
		"}}"
	);
	
	
	private final Category doWhileCategory= new Category(
		"'do while'",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_WHILE_CONDITION, "before condition"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_WHILE_CONDITION, "in condition")
		},
		"class Example {" +
		"void foo() {" +
		"  int a= 100;" +
		"  while (a-- > 0 ) { Other.doSomething();};" +
		" do { Other.doNothing(); } while ( a++ < 1000 );" +
		"}}"
	);
	
	
	private final Category synchronizedCategory= new Category(
		"'synchronized'",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SYNCHRONIZED_CONDITION, "before condition"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_SYNCHRONIZED_CONDITION, "in condition")
		},
		"class Example {" +
		"Mutex fMutex;" +
		"void run() {" +
		"  synchronized(fMutex) {" +
		"    Other.doSomething();" +
		"  }" +
		"}" +
		"} "
	);
	
	
	private final Category tryStatementCategory= new Category(
		"'try finally' / 'try catch'",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CATCH_EXPRESSION, "before catch expression"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_CATCH_EXPRESSION, "in catch expression")
		},
		"class Example {" +
		"  void foo() {" +
		"    try {" +
		"      return 8 / 0;" +
		"    } catch (Exception e) {" +
		"      System.out.println(\"Something happened\");" +
		"    }" +
		"  }" +
		"}"
	);
	
	
	private final Category ifStatementCategory= new Category(
		"'if else'",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_IF_CONDITION, "before condition"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_IF_CONDITION, "in condition")
		},
		"class Example {" +
		"int fField;" +
		"int foo(boolean a) {" +
		"if (a) {" +
		"return 500;" +
		"} else {" +
		"return 400;" +
		"}}}"
	);
	
	
	private final Category forStatementCategory= new Category(
		"'for'",
		new Option [] {
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FOR_PAREN, "before opening parenthesis"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_FOR_PARENS, "after opening and before closing parenthesis"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS, "before comma in initialization"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS, "after comma in initialization"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS, "before comma in increments"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS, "after comma in increments"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR, "after semicolon")
		},
		"class Example {" +
		"void foo() {" +
		"for (int i=0, j=100; i < 10; i++, j--) {" +
		"	Other.enable(i, j );" +
		"}}}"
	);
	
	private final Category labelCategory= new Category(
		"Labels",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT, "before colon"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT, "after colon")
		},
		"class Example {" +
		"  void foo() {" +
		"  outer: " +
		"    for (int i=0; i < 100; i++) {" +
		"      for (int j=0; j < 100; j++) {" +
		"        if (i+j < 100) {" +
		"			continue outer;" +
		"        }" +
		"      }" +
		"    }" +
		"  }" +
		"}"
	);
	
	
	private final Category conditionalCategory= new Category(
		"Conditionals",
		new Option [] {
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL, "before question mark"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL, "after question mark"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL, "before colon"),
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL, "after colon")
		},
		"class Example {" +
		"int foo(int a) {" +
		"return a > 0 ? 1 : 0;" +
		"}" +
		"}"
	);
	
	
	private final Category typecastCategory= new Category(
		"Type casts",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST, "after opening parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST, "before closing parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, "after closing parenthesis")
		},
		"class Example {" +
		"MyClass foo( Object o) {" +
		"	return ((SomeClass)o).getMyClass();" +
		"}}"
	);
		
	
	
	private final Category parenthesizedExpressionOptions= new Category(
		"Parenthesized Expressions",
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION, "before opening parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION, "after opening parenthesis"),
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION, "before closing parenthesis")
		},
		"class Example {" +
		"int foo(int a, int b) {" +
		"  return  ((a) + (b));" +
		"}}"
	);
	
	private final Category declarationsCategory= new Category("Declarations");
	private final Category expressionsCategory= new Category("Expressions");
	private final Category arrayCategory= new Category("Arrays");

	private final Collection fCategories;
	
	private TreeViewer fCategoriesViewer;
	private CheckboxTableViewer fOptionsViewer;


	
	public WhiteSpaceTabPage(Map workingValues) {
		super(workingValues);		
		fCategories= createCategoriesTree();
	}

	/**
	 * Create the categories three
	 */
	private Collection createCategoriesTree() {
		
		memberFunctionCategory.addChild(constructorCategory);
		memberFunctionCategory.addChild(methodCategory);

		declarationsCategory.addChild(classCategory);
		declarationsCategory.addChild(fieldCategory);
		declarationsCategory.addChild(localVariableCategory);
		declarationsCategory.addChild(memberFunctionCategory);
		declarationsCategory.addChild(labelCategory);
		
		arrayCategory.addChild(arrayDeclarationCategory);
		arrayCategory.addChild(arrayInitializerCategory);
		arrayCategory.addChild(arrayElementAccessCategory);
		
		statementCategory.addChild(blockStatementCategory);
		statementCategory.addChild(ifStatementCategory);
		statementCategory.addChild(doWhileCategory);
		statementCategory.addChild(forStatementCategory);
		statementCategory.addChild(tryStatementCategory);
		statementCategory.addChild(switchStatementCategory);
		statementCategory.addChild(synchronizedCategory);
		
		expressionsCategory.addChild(functionCallCategory);
		expressionsCategory.addChild(assignmentCategory);
		expressionsCategory.addChild(operatorCategory);
		expressionsCategory.addChild(parenthesizedExpressionOptions);
		expressionsCategory.addChild(typecastCategory);
		expressionsCategory.addChild(conditionalCategory);

		Collection root= new ArrayList();
		root.add(declarationsCategory);
		root.add(statementCategory);
		root.add(expressionsCategory);
		root.add(arrayCategory);
		
		return root;
	}
	

	protected Composite doCreatePreferences(Composite parent) {
		
		final int numColumns= 3;

		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		// label "categories"
		createLabel(numColumns, composite, "&Category:");
		
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
		createLabel(numColumns, composite, "Insert &white space:");
		
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
		fCategoriesViewer.setSelection( new StructuredSelection( classCategory ));
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

















