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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.IDialogSettings;
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

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class WhiteSpaceTabPage extends ModifyDialogTabPage {
	
	private final static String PREF_CATEGORY_INDEX= JavaUI.ID_PLUGIN + "formatter_page.white_space_tab_page.last_category_index"; //$NON-NLS-1$ 
	private final static String PREF_OPTION_KEY= JavaUI.ID_PLUGIN + "formatter_page.white_space_tab_page.option_nr"; //$NON-NLS-1$
	
	private final static class Category {
		
		public int index;
	
		public final String previewText;
		public final String name;
		public final Option[] options;
		
		public final List children;
		
		public String toString() {
			return name;
		}
		
		public Category(String name, Option[] options, String previewText) {
			this.name= name;
			this.previewText= previewText != null ? createPreviewHeader(name) + previewText : null;
			this.options= options;
			this.children= new ArrayList();
		}
		
		public Category(String name) {
			this(name, new Option [0], null);
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
	
	
	private final class CategoryListener implements ISelectionChangedListener {
		
		private final List fCategoriesList;
		private final IDialogSettings fDialogSettings;
		
		private int fIndex= 0;
		
		public CategoryListener(List categoriesTree) {
			fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
			fCategoriesList= new ArrayList();
			flatten(fCategoriesList, categoriesTree);
		}

		private void flatten(List categoriesList, List categoriesTree) {
			for (final Iterator iter= categoriesTree.iterator(); iter.hasNext(); ) {
				final Category category= (Category) iter.next();
				category.index= fIndex++;
				categoriesList.add(category);
				flatten(categoriesList, category.children);
			}		
		}
		
		
		public void add(Category category) {
			category.index= fIndex++;
			fCategoriesList.add(category);
		}

		public void selectionChanged(SelectionChangedEvent event) {
			final IStructuredSelection selection= (IStructuredSelection)event.getSelection();
			final Category selected= (Category)selection.getFirstElement();
			if (selected != null) {
				fDialogSettings.put(PREF_CATEGORY_INDEX, selected.index);
				fJavaPreview.setPreviewText(selected.previewText);
				doUpdatePreview();
				fOptionsViewer.setInput(selected.options);
				
				for (int i = 0; i < selected.options.length; i++) {
					final boolean checked= fWorkingValues.get(selected.options[i].key).equals(JavaCore.INSERT);
					fOptionsViewer.setChecked(selected.options[i], checked);
				}
			} else {
				fJavaPreview.setPreviewText(null);
				doUpdatePreview();
				fOptionsViewer.setInput(new Object[0]);
			}
		}
		
		public void restoreSelection() {
			int index;
			try {
				index= fDialogSettings.getInt(PREF_CATEGORY_INDEX);
			} catch (NumberFormatException ex) {
				index= -1;
			}
			if (index < 0 || index > fCategoriesList.size() - 1) {
				index= 1; // in order to select a category with preview initially
			}
			final Category category= (Category)fCategoriesList.get(index);
			fCategoriesViewer.setSelection(new StructuredSelection(new Category[] {category}), true);
		}
	}
	
	private final class OptionListener implements ISelectionChangedListener, ICheckStateListener {

		private final IDialogSettings fDialogSettings;
		
		public OptionListener() {
			fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		}
		public void selectionChanged(SelectionChangedEvent event) {
			final IStructuredSelection selection= (IStructuredSelection)event.getSelection();
			if (selection.isEmpty()) {
				fDialogSettings.put(PREF_OPTION_KEY, ""); //$NON-NLS-1$
			} else {
				fDialogSettings.put(PREF_OPTION_KEY, ((Option)selection.getFirstElement()).key);
			}
		}
		public void checkStateChanged(CheckStateChangedEvent event) {
			final Option option= (Option)event.getElement();
			fWorkingValues.put(option.key, event.getChecked() ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT);
			doUpdatePreview();
		}
		
		public void restoreSelection() {
			final String key= fDialogSettings.get(PREF_OPTION_KEY);
			if (key == null || key.equals("")) return; //$NON-NLS-1$
			Object [] options= (Object [])fOptionsViewer.getInput();
			for (int i= 0; i < options.length; i++) {
				if (((Option)options[i]).key.equals(key)) {
					fOptionsViewer.setSelection(new StructuredSelection(new Object[] {options[i]}));
					break;
				}
			}
			
		}
	}
	
	protected final CategoryListener fCategoryListener;
	protected final OptionListener fOptionListener;
	
	private final static Category fClassCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.classes"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.classes.before_opening_brace_of_a_class")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.classes.before_opening_brace_of_anon_class")), //$NON-NLS-1$
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
	
	private final static Category fAssignmentCategory= new Category(FormatterMessages.getString("WhiteSpaceTabPage.assignments"),  //$NON-NLS-1$
	    new Option [] {
	    	new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.assignments.before_assignment_operator") ), //$NON-NLS-1$
	    	new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR, FormatterMessages.getString("WhiteSpaceTabPage.assignments.after_assignment_operator") ) //$NON-NLS-1$
	},
	"class Example {" + //$NON-NLS-1$
	"void foo() {" + //$NON-NLS-1$
	"int a= 4;" + //$NON-NLS-1$
	"}" + //$NON-NLS-1$
	"}" //$NON-NLS-1$
	);
	
	private final static Category fOperatorCategory= new Category( 
		FormatterMessages.getString("WhiteSpaceTabPage.operators"), //$NON-NLS-1$
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

	
	private final static String fMemberFunctionPreview= 		
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
		"}}"; //$NON-NLS-1$
 
	
	private final static Category fMethodCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.methods"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.between_empty_parens")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_brace")), //$NON-NLS-1$

			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS, FormatterMessages.getString("WhiteSpaceTabPage.before_comma_in_params")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS, FormatterMessages.getString("WhiteSpaceTabPage.after_comma_in_params")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS, FormatterMessages.getString("WhiteSpaceTabPage.before_comma_in_throws")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS, FormatterMessages.getString("WhiteSpaceTabPage.after_comma_in_throws")) //$NON-NLS-1$
		},
		fMemberFunctionPreview
	);
	
	private final static Category fConstructorCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.constructors"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.between_empty_parens")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_brace")), //$NON-NLS-1$

			
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, FormatterMessages.getString("WhiteSpaceTabPage.before_comma_in_params")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, FormatterMessages.getString("WhiteSpaceTabPage.after_comma_in_params")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, FormatterMessages.getString("WhiteSpaceTabPage.before_comma_in_throws")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, FormatterMessages.getString("WhiteSpaceTabPage.after_comma_in_throws")) //$NON-NLS-1$
		},
		fMemberFunctionPreview
	);

	private final static Category fFieldCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.fields"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, FormatterMessages.getString("WhiteSpaceTabPage.fields.before_comma")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, FormatterMessages.getString("WhiteSpaceTabPage.fields.after_comma")) //$NON-NLS-1$
		},
		"class Example { int f1=1,f2=2; }" //$NON-NLS-1$
		);
	
	
	private final static Category fLocalVariableCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.localvars"), //$NON-NLS-1$
		new Option[] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, FormatterMessages.getString("WhiteSpaceTabPage.localvars.before_comma")), //$NON-NLS-1$
			 new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, FormatterMessages.getString("WhiteSpaceTabPage.localvars.after_comma")) //$NON-NLS-1$
		},
		"class Example { void foo() { int a=1,b=2; } }" //$NON-NLS-1$
	);
	
	private final static Category fArrayInitializerCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.arrayinit"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_brace")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_brace")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.before_comma")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.after_comma")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER, FormatterMessages.getString("WhiteSpaceTabPage.between_empty_braces")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"  final int [] fArray1= { 1, 2, 3, 4 };" + //$NON-NLS-1$
		"  final int [] fArray2= {};" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final static Category fArrayDeclarationCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.arraydecls"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_bracket")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.between_empty_brackets")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"  int [] fArray1;" + //$NON-NLS-1$
		"  int [][] fArray2;" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final static Category fArrayElementAccessCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.arrayelem"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_bracket")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_bracket")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_bracket")) //$NON-NLS-1$

		},
		"class Example {" + //$NON-NLS-1$
		"  int foo(int [] array) {" + //$NON-NLS-1$
		"    return array[0] + array[1];" + //$NON-NLS-1$
		"  }" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final static Category fFunctionCallCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.calls"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION, FormatterMessages.getString("WhiteSpaceTabPage.between_empty_parens")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.calls.before_comma_in_method_args")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.calls.after_comma_in_method_args")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.calls.before_comma_in_alloc")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.calls.after_comma_in_alloc")),				 //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.calls.before_comma_in_qalloc")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, FormatterMessages.getString("WhiteSpaceTabPage.calls.after_comma_in_qalloc")) //$NON-NLS-1$
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
	
	private final static Category fStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.statements"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON, FormatterMessages.getString("WhiteSpaceTabPage.before_semicolon")) //$NON-NLS-1$
		}, 
		"class Example {" + //$NON-NLS-1$
		"void foo(int a) {" + //$NON-NLS-1$
		"Other.initialize();" + //$NON-NLS-1$
		"Other.doSomething();" + //$NON-NLS-1$
		"Other.doSomethingElse();" + //$NON-NLS-1$
		"Other.cleanUp();" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final static Category blockStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.blocks"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_brace")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK, FormatterMessages.getString("WhiteSpaceTabPage.after_closing_brace")) //$NON-NLS-1$
		},
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
	
	private final static Category fSwitchStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.switch"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE, FormatterMessages.getString("WhiteSpaceTabPage.switch.before_case_colon")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT, FormatterMessages.getString("WhiteSpaceTabPage.switch.before_default_colon")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_brace")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"void foo( int a) {" + //$NON-NLS-1$
		"switch (a) {" + //$NON-NLS-1$
		"case IConstants.GO: Other.go(); break;" + //$NON-NLS-1$
		"case IConstants.STOP: Other.stop(); break;" + //$NON-NLS-1$
		"case IConstants.WAIT: Other.wait(); break;" + //$NON-NLS-1$
		"default: Other.nothingHappens();" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final static Category fDoWhileCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.do"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")) //$NON-NLS-1$

		},
		"class Example {" + //$NON-NLS-1$
		"void foo() {" + //$NON-NLS-1$
		"  int a= 100;" + //$NON-NLS-1$
		"  while (a-- > 0 ) { Other.doSomething();};" + //$NON-NLS-1$
		" do { Other.doNothing(); } while ( a++ < 1000 );" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final static Category fSynchronizedCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.synchronized"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")), //$NON-NLS-1$
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
	
	private final static Category fTryStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.try"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")) //$NON-NLS-1$
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
	
	private final static Category fIfStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.if"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")) //$NON-NLS-1$
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
	
	private final static Category fForStatementCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.for"), //$NON-NLS-1$
		new Option [] {
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS, FormatterMessages.getString("WhiteSpaceTabPage.for.before_comma_init")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS, FormatterMessages.getString("WhiteSpaceTabPage.for.after_comma_init")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS, FormatterMessages.getString("WhiteSpaceTabPage.for.before_comma_inc")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS, FormatterMessages.getString("WhiteSpaceTabPage.for.after_comma_inc")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR, FormatterMessages.getString("WhiteSpaceTabPage.after_semicolon")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"void foo() {" + //$NON-NLS-1$
		"for (int i=0, j=100; i < 10; i++, j--) {" + //$NON-NLS-1$
		"	Other.enable(i, j );" + //$NON-NLS-1$
		"}}}" //$NON-NLS-1$
	);
	
//TODO: include this category
//	private final static Category fAssertCategory= new Category(
//		"'assert'",
//		new Option [] {
//			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT, "WhiteSpaceTabPage.before_colon"),
//			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT, "WhiteSpaceTabPage.after_colon")
//		},
//		"class Example {" +
//		"  void foo(int a) {" +
//		"	 assert a==0 : \"Oops\";" +
//		"  }" +
//		"}"
//	);
	
	private final static Category fLabelCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.labels"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT, FormatterMessages.getString("WhiteSpaceTabPage.before_colon")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT, FormatterMessages.getString("WhiteSpaceTabPage.after_colon")) //$NON-NLS-1$
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
	
	private final static Category fConditionalCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.conditionals"), //$NON-NLS-1$
		new Option [] {
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL, FormatterMessages.getString("WhiteSpaceTabPage.before_question")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL, FormatterMessages.getString("WhiteSpaceTabPage.after_question")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL, FormatterMessages.getString("WhiteSpaceTabPage.before_colon")), //$NON-NLS-1$
		   new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL, FormatterMessages.getString("WhiteSpaceTabPage.after_colon")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"int foo(int a) {" + //$NON-NLS-1$
		"return a > 0 ? 1 : 0;" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"}" //$NON-NLS-1$
	);
	
	private final static Category fTypecastCategory= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.typecasts"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, FormatterMessages.getString("WhiteSpaceTabPage.after_closing_paren")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"MyClass foo( Object o) {" + //$NON-NLS-1$
		"	return ((SomeClass)o).getMyClass();" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final static Category fParenthesizedExpressionOptions= new Category(
		FormatterMessages.getString("WhiteSpaceTabPage.parenexpr"), //$NON-NLS-1$
		new Option [] {
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.before_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.after_opening_paren")), //$NON-NLS-1$
			new Option(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION, FormatterMessages.getString("WhiteSpaceTabPage.before_closing_paren")) //$NON-NLS-1$
		},
		"class Example {" + //$NON-NLS-1$
		"int foo(int a, int b) {" + //$NON-NLS-1$
		"  return  ((a) + (b));" + //$NON-NLS-1$
		"}}" //$NON-NLS-1$
	);
	
	private final static Category fDeclarationsCategory= new Category(FormatterMessages.getString("WhiteSpaceTabPage.decls")); //$NON-NLS-1$
	private final static Category fExpressionsCategory= new Category(FormatterMessages.getString("WhiteSpaceTabPage.exprs")); //$NON-NLS-1$
	private final static Category fArrayCategory= new Category(FormatterMessages.getString("WhiteSpaceTabPage.arrays")); //$NON-NLS-1$

	private final static List fCategories= createCategoriesTree();
	
	protected TreeViewer fCategoriesViewer;
	protected CheckboxTableViewer fOptionsViewer;


	/**
	 * Create a new white space dialog page.
	 */
	public WhiteSpaceTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
		fOptionListener= new OptionListener();
		fCategoryListener= new CategoryListener(fCategories);
	}

	/**
	 * Create the categories three
	 */
	private static List createCategoriesTree() {
		

		fDeclarationsCategory.children.add(fClassCategory);
		fDeclarationsCategory.children.add(fFieldCategory);
		fDeclarationsCategory.children.add(fLocalVariableCategory);
		fDeclarationsCategory.children.add(fConstructorCategory);
		fDeclarationsCategory.children.add(fMethodCategory);

		fDeclarationsCategory.children.add(fLabelCategory);
		
		fArrayCategory.children.add(fArrayDeclarationCategory);
		fArrayCategory.children.add(fArrayInitializerCategory);
		fArrayCategory.children.add(fArrayElementAccessCategory);
		
		fStatementCategory.children.add(blockStatementCategory);
		fStatementCategory.children.add(fIfStatementCategory);
		fStatementCategory.children.add(fDoWhileCategory);
		fStatementCategory.children.add(fForStatementCategory);
		fStatementCategory.children.add(fTryStatementCategory);
		fStatementCategory.children.add(fSwitchStatementCategory);
		fStatementCategory.children.add(fSynchronizedCategory);
		
		fExpressionsCategory.children.add(fFunctionCallCategory);
		fExpressionsCategory.children.add(fAssignmentCategory);
		fExpressionsCategory.children.add(fOperatorCategory);
		fExpressionsCategory.children.add(fParenthesizedExpressionOptions);
		fExpressionsCategory.children.add(fTypecastCategory);
		fExpressionsCategory.children.add(fConditionalCategory);

		final List root= new ArrayList();
		root.add(fDeclarationsCategory);
		root.add(fStatementCategory);
		root.add(fExpressionsCategory);
		root.add(fArrayCategory);
		
		return root;
	}
	

	protected Composite doCreatePreferences(Composite parent) {
		
		final int numColumns= 3;
		
		GridData gd;

		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		createLabel(numColumns, composite, FormatterMessages.getString("WhiteSpaceTabPage.category.label.text")); //$NON-NLS-1$
		
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
				return !((Category)element).children.isEmpty();
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			public void dispose() {}
		});
		
		fCategoriesViewer.setLabelProvider(new LabelProvider());
		
		gd= createGridData(numColumns, GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.heightHint= fPixelConverter.convertHeightInCharsToPixels(3);
		fCategoriesViewer.getControl().setLayoutData(gd);
		
		createLabel(numColumns, composite, FormatterMessages.getString("WhiteSpaceTabPage.checktable.label.text")); //$NON-NLS-1$
		
		fOptionsViewer= CheckboxTableViewer.newCheckList(composite, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		fOptionsViewer.setContentProvider(new ArrayContentProvider());
		fOptionsViewer.setLabelProvider(new LabelProvider());
		
		gd= createGridData(numColumns, GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.heightHint= fPixelConverter.convertHeightInCharsToPixels(3);
		fOptionsViewer.getControl().setLayoutData(gd);
		
		fCategoriesViewer.setInput(fCategories);
		fCategoriesViewer.expandAll();
		

		initializeControls();
		
		return composite;
	}
	
	private void initializeControls() {
		// add listeners
		fCategoriesViewer.addSelectionChangedListener(fCategoryListener);
		fOptionsViewer.addSelectionChangedListener(fOptionListener);
		fOptionsViewer.addCheckStateListener(fOptionListener);
		
		// restore the selections
		fCategoryListener.restoreSelection();
		fOptionListener.restoreSelection();

		// install focus manager
		fDefaultFocusManager.add(fCategoriesViewer.getControl());
		fDefaultFocusManager.add(fOptionsViewer.getControl());
	}
}

















