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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * The line wrapping tab page.
 */
public class LineWrappingTabPage extends ModifyDialogTabPage {

    /**
     * Represents a line wrapping category. All members are final.
     */
	private final static class Category {
		public final String key;
		public final String name;
		public final String previewText;
		public final List children;
		
		public int index;

		public Category(String key, String previewText, String name) {
			this.key= key;
			this.name= name;
			this.previewText= previewText != null ? createPreviewHeader(name) + previewText : null;
			children= new ArrayList();
		}
		
		public Category(String name) {
		    this(null, null, name);
		}
		
		public String toString() {
			return name;
		}
	}
	

	private final static String PREF_CATEGORY_INDEX= JavaUI.ID_PLUGIN + "formatter_page.line_wrapping_tab_page.last_category_index"; //$NON-NLS-1$ 
	
	
	private final class CategoryListener implements ISelectionChangedListener {
		
		private final List fCategoriesList;
		
		private int fIndex= 0;
		
		public CategoryListener(List categoriesTree) {
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
			
			final Category category= (Category)selection.getFirstElement();
			fDialogSettings.put(PREF_CATEGORY_INDEX, category.index);

			fCurrentKey= category.key;
			
			if (fCurrentKey != null) {
				fOptionsGroup.setText(FormatterMessages.getString("LineWrappingTabPage.group") + category.name.toLowerCase()); //$NON-NLS-1$
			} else {
				fOptionsGroup.setText(""); //$NON-NLS-1$
			}
			fJavaPreview.setPreviewText(category.previewText);
			doUpdatePreview();
			
			final boolean enabled= fWorkingValues.containsKey(category.key);
			fOptionsGroup.setVisible(enabled);
			if (enabled) {
				fForceSplit.setSelection(DefaultCodeFormatterConstants.getForceWrapping(fWorkingValues, category.key));
				fIndentStyleCombo.setText(INDENT_NAMES[DefaultCodeFormatterConstants.getIndentStyle(fWorkingValues, category.key)]);
				final int splitStyle= DefaultCodeFormatterConstants.getWrappingStyle(fWorkingValues, category.key);
				fSplitStyleCombo.setText(SPLIT_NAMES[splitStyle]);
				updateControls(splitStyle);
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
				index= 1; // In order to select a category with preview initially
			}
			final Category category= (Category)fCategoriesList.get(index);
			fCategoriesViewer.setSelection(new StructuredSelection(new Category[] {category}));
		}
	}
	
	protected final static String[] INDENT_NAMES = {
	    FormatterMessages.getString("LineWrappingTabPage.indentation.default"), //$NON-NLS-1$ 
	    FormatterMessages.getString("LineWrappingTabPage.indentation.on_column"), //$NON-NLS-1$ 
	    FormatterMessages.getString("LineWrappingTabPage.indentation.by_one") //$NON-NLS-1$
	};
	
	
	protected final static String[] SPLIT_NAMES = { 
	    FormatterMessages.getString("LineWrappingTabPage.splitting.do_not_split"), //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_when_necessary"), // COMPACT_SPLIT //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.always_wrap_first_others_when_necessary"), // COMPACT_FIRST_BREAK_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always"), // ONE_PER_LINE_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always_indent_all_but_first"), // NEXT_SHIFTED_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always_except_first_only_if_necessary") // NEXT_PER_LINE_SPLIT //$NON-NLS-1$
	};
	

	private final static Category fCompactIfCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF,
	    "class Example {" + //$NON-NLS-1$
	    "int foo(int argument) {" + //$NON-NLS-1$
	    "  if (argument==0) return 0;" + //$NON-NLS-1$
	    "  if (argument==1) return 42; else return 43;" + //$NON-NLS-1$	
	    "}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.compact_if_else") //$NON-NLS-1$
	);
	

	private final static Category fTypeDeclarationSuperclassCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION,
	    "class Example extends OtherClass {}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.extends_clause") //$NON-NLS-1$
	);
	

	private final static Category fTypeDeclarationSuperinterfacesCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION,
	    "class Example implements I1, I2, I3 {}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.implements_clause") //$NON-NLS-1$
	);
	
	
	private final static Category fConstructorDeclarationsParametersCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION,
	    "class Example {Example(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) { this();}" + //$NON-NLS-1$
	    "Example() {}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.parameters") //$NON-NLS-1$
	); 

	private final static Category fMethodDeclarationsParametersCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION,
	    "class Example {void foo(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.parameters") //$NON-NLS-1$
	); 
	
	private final static Category fMessageSendArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION,
	    "class Example {void foo() {Other.bar( 100, 200, 300, 400, 500, 600, 700, 800, 900 );}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.arguments") //$NON-NLS-1$
	); 

	private final static Category fMessageSendSelectorCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION,
	    "class Example {int foo(Some a) {return a.getFirst();}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.qualified_invocations") //$NON-NLS-1$
	);
	
	private final static Category fMethodThrowsClauseCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION, 
	    "class Example {" + //$NON-NLS-1$
	    "int foo() throws FirstException, SecondException, ThirdException {" + //$NON-NLS-1$
	    "  return Other.doSomething();}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.throws_clause") //$NON-NLS-1$
	);

	private final static Category fConstructorThrowsClauseCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION, 
	    "class Example {" + //$NON-NLS-1$
	    "Example() throws FirstException, SecondException, ThirdException {" + //$NON-NLS-1$
	    "  return Other.doSomething();}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.throws_clause") //$NON-NLS-1$
	);

	
	private final static Category fAllocationExpressionArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION,
	    "class Example {SomeClass foo() {return new SomeClass(100, 200, 300, 400, 500, 600, 700, 800, 900 );}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.object_allocation") //$NON-NLS-1$
	);
	
	private final static Category fQualifiedAllocationExpressionCategory= new Category (
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION,
	    "class Example {SomeClass foo() {return SomeOtherClass.new SomeClass(100, 200, 300, 400, 500 );}}", //$NON-NLS-1$
		FormatterMessages.getString("LineWrappingTabPage.qualified_object_allocation") //$NON-NLS-1$
	);
	
	private final static Category fArrayInitializerExpressionsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER,
	    "class Example {int [] fArray= {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.array_init") //$NON-NLS-1$
	);
	
	private final static Category fExplicitConstructorArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL,
	    "class Example extends AnotherClass {Example() {super(100, 200, 300, 400, 500, 600, 700);}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.explicit_constructor_invocations") //$NON-NLS-1$
	);

	private final static Category fConditionalExpressionCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION,
	    "class Example extends AnotherClass {int Example(boolean Argument) {return argument ? 100000 : 200000;}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.conditionals") //$NON-NLS-1$
	);

	private final static Category fBinaryExpressionCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
	    "class Example extends AnotherClass {" + //$NON-NLS-1$
	    "int foo() {" + //$NON-NLS-1$
	    "  int sum= 100 + 200 + 300 + 400 + 500 + 600 + 700 + 800;" + //$NON-NLS-1$
	    "  int product= 1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10;" + //$NON-NLS-1$
	    "  boolean val= true && false && true && false && true;" +  //$NON-NLS-1$
	    "  return product / sum;}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.binary_exprs") //$NON-NLS-1$
	);
	
	/**
	 * The default preview line width.
	 */
	private static int DEFAULT_PREVIEW_WINDOW_LINE_WIDTH= 40;
	
	/**
	 * The key to save the user's preview window width in the dialog settings.
	 */
	private static final String PREF_PREVIEW_LINE_WIDTH= JavaUI.ID_PLUGIN + ".codeformatter.line_wrapping_tab_page.preview_line_width"; //$NON-NLS-1$
	
	/**
	 * The dialog settings.
	 */
	protected final IDialogSettings fDialogSettings;	
	
	protected TreeViewer fCategoriesViewer;
	protected Combo fSplitStyleCombo;
	protected Label fIndentStyleLabel;
	protected Combo fIndentStyleCombo;
	protected Button fForceSplit;


	protected Group fOptionsGroup;

	/**
	 * A collection containing the categories tree. This is used as model for the tree viewer.
	 * @see TreeViewer
	 */
	private final List fCategories;
	
	/**
	 * The category listener which makes the selection persistent.
	 */
	protected final CategoryListener fCategoryListener;

	/**
	 * The key representing the category for which the options are currently shown. 
	 */
	protected String fCurrentKey;
	
	/**
	 * A special options store wherein the preview line width is kept.
	 */
	protected final Map fPreviewPreferences;
	
	/**
	 * The key for the preview line width.
	 */
	private final String LINE_SPLIT= DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT;
	
	/**
	 * Create a new line wrapping tab page.
	 */
	public LineWrappingTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);

		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		final String previewLineWidth= fDialogSettings.get(PREF_PREVIEW_LINE_WIDTH);
		
		fPreviewPreferences= new HashMap();
		fPreviewPreferences.put(LINE_SPLIT, previewLineWidth != null ? previewLineWidth : Integer.toString(DEFAULT_PREVIEW_WINDOW_LINE_WIDTH));
		
		fCategories= createCategories();
		fCategoryListener= new CategoryListener(fCategories);
		
		fCurrentKey= ((Category)fCategories.iterator().next()).key;
	}
	
	/**
	 * Create the categories tree.
	 */
	protected List createCategories() {

		final Category classDeclarations= new Category(FormatterMessages.getString("LineWrappingTabPage.class_decls")); //$NON-NLS-1$
		classDeclarations.children.add(fTypeDeclarationSuperclassCategory);
		classDeclarations.children.add(fTypeDeclarationSuperinterfacesCategory);
		
		final Category constructorDeclarations= new Category(null, null, FormatterMessages.getString("LineWrappingTabPage.constructor_decls")); //$NON-NLS-1$
		constructorDeclarations.children.add(fConstructorDeclarationsParametersCategory);
		constructorDeclarations.children.add(fConstructorThrowsClauseCategory);

		final Category methodDeclarations= new Category(null, null, FormatterMessages.getString("LineWrappingTabPage.method_decls")); //$NON-NLS-1$
		methodDeclarations.children.add(fMethodDeclarationsParametersCategory);
		methodDeclarations.children.add(fMethodThrowsClauseCategory);
		
		final Category functionCalls= new Category(FormatterMessages.getString("LineWrappingTabPage.function_calls")); //$NON-NLS-1$
		functionCalls.children.add(fMessageSendArgumentsCategory);
		functionCalls.children.add(fMessageSendSelectorCategory);
		functionCalls.children.add(fExplicitConstructorArgumentsCategory);
		functionCalls.children.add(fAllocationExpressionArgumentsCategory);
		functionCalls.children.add(fQualifiedAllocationExpressionCategory);
		
		final Category expressions= new Category(FormatterMessages.getString("LineWrappingTabPage.expressions")); //$NON-NLS-1$
		expressions.children.add(fBinaryExpressionCategory);
		expressions.children.add(fConditionalExpressionCategory);
		expressions.children.add(fArrayInitializerExpressionsCategory);
		
		final Category statements= new Category(FormatterMessages.getString("LineWrappingTabPage.statements")); //$NON-NLS-1$
		statements.children.add(fCompactIfCategory);
		
		final List root= new ArrayList();
		root.add(classDeclarations);
		root.add(constructorDeclarations);
		root.add(methodDeclarations);
		root.add(functionCalls);
		root.add(expressions);
		root.add(statements);
		
		return root;
	}
	
	protected Composite doCreatePreferences(Composite parent) {
	
		final int numColumns= 3;
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));

		final Group lineWidthGroup= new Group(composite, SWT.NONE);
		lineWidthGroup.setLayout(createGridLayout(numColumns, true));
		lineWidthGroup.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL));
		lineWidthGroup.setText(FormatterMessages.getString("LineWrappingTabPage.width_indent")); //$NON-NLS-1$
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.max_line_width"), DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, 0, Integer.MAX_VALUE); //$NON-NLS-1$
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.default_indent_wrapped"), DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, 0, 9999); //$NON-NLS-1$
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.default_indent_array"), DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER, 0, Integer.MAX_VALUE); //$NON-NLS-1$ 
		
		final Group categoryGroup= new Group(composite, SWT.NONE);
		categoryGroup.setLayout(createGridLayout(numColumns, true));
		categoryGroup.setLayoutData(createGridData(numColumns, GridData.FILL_BOTH));
		categoryGroup.setText(FormatterMessages.getString("LineWrappingTabPage.line_wrapping_group")); //$NON-NLS-1$

		createLabel(numColumns, categoryGroup, FormatterMessages.getString("LineWrappingTabPage.category.label.text")); //$NON-NLS-1$
		
		fCategoriesViewer= new TreeViewer(categoryGroup, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL );
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
		fCategoriesViewer.setInput(fCategories);
		
		fCategoriesViewer.setExpandedElements(fCategories.toArray());

		final GridData gd= createGridData(numColumns, GridData.FILL_BOTH);
		fCategoriesViewer.getControl().setLayoutData(gd);

		fOptionsGroup = new Group(categoryGroup, SWT.NONE);
		fOptionsGroup.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL));
		fOptionsGroup.setLayout(createGridLayout(numColumns, true));

		
		// label "Select split style:"
		createLabel(numColumns, fOptionsGroup, FormatterMessages.getString("LineWrappingTabPage.wrapping_policy.label.text")); //$NON-NLS-1$
	
		// combo SplitStyleCombo
		fSplitStyleCombo= new Combo(fOptionsGroup, SWT.SINGLE | SWT.READ_ONLY);
		fSplitStyleCombo.setItems(SPLIT_NAMES);
		fSplitStyleCombo.setLayoutData(createGridData(numColumns ));
		
		// label "Select indentation style:"
		fIndentStyleLabel= createLabel(numColumns, fOptionsGroup, FormatterMessages.getString("LineWrappingTabPage.indentation_policy.label.text")); //$NON-NLS-1$
		
		// combo SplitStyleCombo
		fIndentStyleCombo= new Combo(fOptionsGroup, SWT.SINGLE | SWT.READ_ONLY);
		fIndentStyleCombo.setItems(INDENT_NAMES);
		fIndentStyleCombo.setLayoutData(createGridData(numColumns));
		
		// button "Force split"
		fForceSplit= new Button(fOptionsGroup, SWT.CHECK);
		fForceSplit.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL));
		fForceSplit.setText(FormatterMessages.getString("LineWrappingTabPage.force_split.checkbox.text")); //$NON-NLS-1$
		
		return composite;
	}
	
		
	protected Composite doCreatePreview(Composite parent) {
		final int numColumns= 2;
		
		final Composite composite= new Composite(parent, SWT.NONE);

		composite.setLayout(createGridLayout(numColumns, false));

		final Composite preview= super.doCreatePreview(composite);
		preview.setLayoutData(createGridData(numColumns, GridData.FILL_BOTH));
		
		final Composite lineWidthComposite= new Composite(composite, SWT.NONE);
		lineWidthComposite.setLayoutData(createGridData(1, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));
		lineWidthComposite.setLayout(createGridLayout(2, false));

		final NumberPreference previewLineWidth= new NumberPreference(lineWidthComposite, 2, fPreviewPreferences, LINE_SPLIT,
			0, Integer.MAX_VALUE, FormatterMessages.getString("LineWrappingTabPage.line_width_for_preview.label.text")); //$NON-NLS-1$
		fDefaultFocusManager.add(previewLineWidth);
		previewLineWidth.addObserver(fUpdater);
		previewLineWidth.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				fDialogSettings.put(PREF_PREVIEW_LINE_WIDTH, (String)fPreviewPreferences.get(LINE_SPLIT));
			}
		});
		
		initializeControls();
		
		return composite;
	}
	
	private void initializeControls() {
		
		fCategoriesViewer.addSelectionChangedListener(fCategoryListener);
		
		fForceSplit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				forceSplitChanged(fForceSplit.getSelection());
			}
		});
		fIndentStyleCombo.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				indentStyleChanged(((Combo)e.widget).getSelectionIndex());
			}
		});
		fSplitStyleCombo.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				splitStyleChanged(((Combo)e.widget).getSelectionIndex());
			}
		});
		
		fCategoryListener.restoreSelection();
		
		fDefaultFocusManager.add(fCategoriesViewer.getControl());
		fDefaultFocusManager.add(fSplitStyleCombo);
		fDefaultFocusManager.add(fIndentStyleCombo);
		fDefaultFocusManager.add(fForceSplit);
	}
	
	protected void doUpdatePreview() {
		final Object normalSetting= fWorkingValues.get(LINE_SPLIT);
		fWorkingValues.put(LINE_SPLIT, fPreviewPreferences.get(LINE_SPLIT));
		super.doUpdatePreview();
		fWorkingValues.put(LINE_SPLIT, normalSetting);
	}

	
	protected void forceSplitChanged(boolean forceSplit) {
		DefaultCodeFormatterConstants.setForceWrapping(fWorkingValues, fCurrentKey, forceSplit);
		doUpdatePreview();
	}
	
	protected void splitStyleChanged(int splitStyle) {
	    DefaultCodeFormatterConstants.setWrappingStyle(fWorkingValues, fCurrentKey, splitStyle);
		updateControls(splitStyle);
		doUpdatePreview();
	}
	
	protected void indentStyleChanged(int indentStyle) {
		DefaultCodeFormatterConstants.setIndentStyle(fWorkingValues, fCurrentKey, indentStyle);
		doUpdatePreview();
	}
	
	protected void updateControls(int splitStyle) {
	    boolean doSplit= splitStyle != DefaultCodeFormatterConstants.WRAP_NO_SPLIT;
	    fIndentStyleLabel.setEnabled(doSplit);
	    fIndentStyleCombo.setEnabled(doSplit);
	    fForceSplit.setEnabled(doSplit);
	}
}
