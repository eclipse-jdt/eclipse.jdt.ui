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
				fOptionsGroup.setText(FormatterMessages.getString("LineWrappingTabPage.group.title") + category.name.toLowerCase()); //$NON-NLS-1$
			} else {
				fOptionsGroup.setText(""); //$NON-NLS-1$
			}
			fJavaPreview.setPreviewText(category.previewText);
			doUpdatePreview();
			
			final boolean enabled= fWorkingValues.containsKey(category.key);
			fOptionsGroup.setVisible(enabled);
			if (enabled) {
				final AlignmentValue value= new AlignmentValue((String)fWorkingValues.get(category.key));
				fForceSplit.setSelection(value.getForceSplit()); 
				fIndentStyleCombo.setText(INDENT_NAMES[value.getIndentStyleIndex()]);
				fSplitStyleCombo.setText(SPLIT_NAMES[value.getSplitStyleIndex()]);
				updateControls(SPLIT_VALUES[value.getSplitStyleIndex()]);
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
				index= 0;
			}
			final Category category= (Category)fCategoriesList.get(index);
			fCategoriesViewer.setSelection(new StructuredSelection(new Category[] {category}));
		}
	}
	
	
	
	
	
	
	
	/* 
	 * Force splitting 
	 */
	protected final static int DO_NOT_FORCE_SPLIT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_NO_ALIGNMENT);
	protected final static int FORCE_SPLIT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FORCE);

	
	/*
	 * Indentation styles 
	 */
	protected final static int INDENT_DEFAULT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_NO_ALIGNMENT);
	protected final static int INDENT_ON_COLUMN= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_INDENT_ON_COLUMN);
	protected final static int INDENT_BY_ONE= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_INDENT_BY_ONE);

	protected final static int [] INDENT_VALUES= { 
	    INDENT_DEFAULT, 
	    INDENT_ON_COLUMN, 
	    INDENT_BY_ONE 
	};
	
	protected final static String[] INDENT_NAMES = {
	    FormatterMessages.getString("LineWrappingTabPage.indentation.default"), //$NON-NLS-1$ 
	    FormatterMessages.getString("LineWrappingTabPage.indentation.on_column"), //$NON-NLS-1$ 
	    FormatterMessages.getString("LineWrappingTabPage.indentation.by_one") //$NON-NLS-1$
	};
	
	
	/* 
	 * Splitting styles. 
	 */
	protected final static int SPLIT_DO_NOT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_NO_ALIGNMENT);
	protected final static int SPLIT_COMPACT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_COMPACT_SPLIT);
	protected final static int SPLIT_FIRST_BREAK= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_COMPACT_FIRST_BREAK_SPLIT);
	protected final static int SPLIT_ONE_PER_LINE= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_ONE_PER_LINE_SPLIT);
	protected final static int SPLIT_NEXT_SHIFTED= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_NEXT_SHIFTED_SPLIT);
	protected final static int SPLIT_NEXT_PER_LINE= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_NEXT_PER_LINE_SPLIT);
	
	protected final static int [] SPLIT_VALUES = { 
	    SPLIT_DO_NOT, 
	    SPLIT_COMPACT, 
	    SPLIT_FIRST_BREAK, 
	    SPLIT_ONE_PER_LINE, 
	    SPLIT_NEXT_SHIFTED, 
	    SPLIT_NEXT_PER_LINE 
	};  
	
	protected final static String[] SPLIT_NAMES = { 
	    FormatterMessages.getString("LineWrappingTabPage.splitting.do_not_split"), //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_when_necessary"), // COMPACT_SPLIT //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.always_wrap_first_others_when_necessary"), // COMPACT_FIRST_BREAK_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always"), // ONE_PER_LINE_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always_indent_all_but_first"), // NEXT_SHIFTED_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always_except_first_only_if_necessary") // NEXT_PER_LINE_SPLIT //$NON-NLS-1$
	};

	/**
	 * A wrapper class for the code formatter line wrapping option, which encodes three different options into one.
	 */
	private final static class AlignmentValue {

	    private final static int INDENT_MASK= calculateMask(INDENT_VALUES);
	    private final static int SPLIT_MASK= calculateMask(SPLIT_VALUES);

	    private int fBitField;
	    
	    private static int calculateMask(int [] values) {
	        int mask= 0;
	        for (int i= 0; i < values.length; i++) {
	            mask |= values[i];
	        }
	        return mask; 
	    }
	    
	    public AlignmentValue(String value) {
	        setValue(value);
	    }
	    
	    public void setValue(String value) {
	        try {
	            fBitField= Integer.parseInt(value);
	        } catch (NumberFormatException e) {
	            fBitField= 0;
	            JavaPlugin.log(e);
	        }
	    }

	    public String getValue() {
	        return Integer.toString(fBitField);
	    }
	    
	    public boolean getForceSplit() {
	        return (fBitField & FORCE_SPLIT) != 0;
	    }
	    
	    public void setForceSplit(boolean enabled) {
	        if (enabled) {
	            fBitField |= FORCE_SPLIT;
	        } else {
	            fBitField &= ~FORCE_SPLIT;
	        }
	    }
	    
	    public int getIndentStyleIndex() {
	        final int indent= fBitField & INDENT_MASK;
	        for (int i= 0; i < INDENT_VALUES.length; i++)
	            if (INDENT_VALUES[i] == indent) return i;
	        return 0;
	    }
	    
	    public void setIndentStyleIndex(int index) {
	        fBitField = (fBitField & ~INDENT_MASK) | INDENT_VALUES[index];
	    }
	    
	    public int getSplitStyleIndex() {
	        final int split= fBitField & SPLIT_MASK;
	        for (int i= 0; i < SPLIT_VALUES.length; i++)
	            if (SPLIT_VALUES[i] == split) return i;
	        return 0;
	    }
	    
	    public void setSplitStyleIndex(int index) {
	        fBitField = (fBitField & ~SPLIT_MASK) | SPLIT_VALUES[index];
	    }
	}
	

	private final static Category fCompactIfCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_COMPACT_IF_ALIGNMENT,
	    "class Example {" + //$NON-NLS-1$
	    "int foo(int argument) {" + //$NON-NLS-1$
	    "  if (argument==0) return 0;" + //$NON-NLS-1$
	    "  if (argument==1) return 42; else return 43;" + //$NON-NLS-1$	
	    "}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.compact_if_else.title") //$NON-NLS-1$
	);
	

	private final static Category fTypeDeclarationSuperclassCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_SUPERCLASS_ALIGNMENT,
	    "class Example extends OtherClass {}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.extends_clause.title") //$NON-NLS-1$
	);
	

	private final static Category fTypeDeclarationSuperinterfacesCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_SUPERINTERFACES_ALIGNMENT,
	    "class Example implements I1, I2, I3 {}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.implements_clause.title") //$NON-NLS-1$
	);	

	private final static Category fMethodDeclarationsArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_METHOD_DECLARATION_ARGUMENTS_ALIGNMENT,
	    "class Example {void foo(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.parameters.title") //$NON-NLS-1$
	); 
	
	private final static Category fMessageSendArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_MESSAGE_SEND_ARGUMENTS_ALIGNMENT,
	    "class Example {void foo() {Other.bar( 100, 200, 300, 400, 500, 600, 700, 800, 900 );}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.arguments.title") //$NON-NLS-1$
	); 

	private final static Category fMessageSendSelectorCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_MESSAGE_SEND_SELECTOR_ALIGNMENT,
	    "class Example {int foo(Some a) {return a.getFirst();}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.qualified_invocations.title") //$NON-NLS-1$
	);
	
	private final static Category fMethodThrowsClauseCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_METHOD_THROWS_CLAUSE_ALIGNMENT, 
	    "class Example {" + //$NON-NLS-1$
	    "int foo() throws FirstException, SecondException, ThirdException {" + //$NON-NLS-1$
	    "  return Other.doSomething();}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.throws_clause.title") //$NON-NLS-1$
	);
	
	private final static Category fAllocationExpressionArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT,
	    "class Example {SomeClass foo() {return new SomeClass(100, 200, 300, 400, 500, 600, 700, 800, 900 );}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.object_allocation.title") //$NON-NLS-1$
	);
	
	private final static Category fQualifiedAllocationExpressionCategory= new Category (
	    DefaultCodeFormatterConstants.FORMATTER_QUALIFIED_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT,
	    "class Example {SomeClass foo() {return SomeOtherClass.new SomeClass(100, 200, 300, 400, 500 );}}", //$NON-NLS-1$
		FormatterMessages.getString("LineWrappingTabPage.qualified_object_allocation.title") //$NON-NLS-1$
	);
	
	private final static Category fArrayInitializerExpressionsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_EXPRESSIONS_ALIGNMENT,
	    "class Example {int [] fArray= {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.array_init.title") //$NON-NLS-1$
	);
	
	private final static Category fExplicitConstructorArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_EXPLICIT_CONSTRUCTOR_ARGUMENTS_ALIGNMENT,
	    "class Example extends AnotherClass {Example() {super(100, 200, 300, 400, 500, 600, 700);}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.explicit_constructor_invocations.title") //$NON-NLS-1$
	);

	private final static Category fConditionalExpressionCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_CONDITIONAL_EXPRESSION_ALIGNMENT,
	    "class Example extends AnotherClass {int Example(boolean Argument) {return argument ? 100000 : 200000;}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.conditionals.title") //$NON-NLS-1$
	);

	private final static Category fBinaryExpressionCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_BINARY_EXPRESSION_ALIGNMENT,
	    "class Example extends AnotherClass {" + //$NON-NLS-1$
	    "int foo() {" + //$NON-NLS-1$
	    "  int sum= 100 + 200 + 300 + 400 + 500 + 600 + 700 + 800;" + //$NON-NLS-1$
	    "  int product= 1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10;" + //$NON-NLS-1$
	    "  boolean val= true && false && true && false && true;" +  //$NON-NLS-1$
	    "  return product / sum;}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.binary_exprs.title") //$NON-NLS-1$
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

		final Category classDeclarations= new Category(FormatterMessages.getString("LineWrappingTabPage.class_decls.title")); //$NON-NLS-1$
		classDeclarations.children.add(fTypeDeclarationSuperclassCategory);
		classDeclarations.children.add(fTypeDeclarationSuperinterfacesCategory);
		
		final Category methodDeclarations= new Category(null, null, FormatterMessages.getString("LineWrappingTabPage.method_decls.title")); //$NON-NLS-1$
		methodDeclarations.children.add(fMethodDeclarationsArgumentsCategory);
		methodDeclarations.children.add(fMethodThrowsClauseCategory);
		
		final Category functionCalls= new Category(FormatterMessages.getString("LineWrappingTabPage.function_calls.title")); //$NON-NLS-1$
		functionCalls.children.add(fMessageSendArgumentsCategory);
		functionCalls.children.add(fMessageSendSelectorCategory);
		functionCalls.children.add(fExplicitConstructorArgumentsCategory);
		functionCalls.children.add(fAllocationExpressionArgumentsCategory);
		functionCalls.children.add(fQualifiedAllocationExpressionCategory);
		
		final Category expressions= new Category(FormatterMessages.getString("LineWrappingTabPage.expressions.title")); //$NON-NLS-1$
		expressions.children.add(fBinaryExpressionCategory);
		expressions.children.add(fConditionalExpressionCategory);
		expressions.children.add(fArrayInitializerExpressionsCategory);
		
		final Category statements= new Category(FormatterMessages.getString("LineWrappingTabPage.statements.title")); //$NON-NLS-1$
		statements.children.add(fCompactIfCategory);
		
		final List root= new ArrayList();
		root.add(classDeclarations);
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
		lineWidthGroup.setText(FormatterMessages.getString("LineWrappingTabPage.width_indent.title")); //$NON-NLS-1$
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.max_line_width"), DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, 0, Integer.MAX_VALUE); //$NON-NLS-1$
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.default_indent_wrapped"), DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, 0, 9999); //$NON-NLS-1$
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.default_indent_array"), DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_CONTINUATION_INDENTATION, 0, Integer.MAX_VALUE); //$NON-NLS-1$ 
		
		final Group categoryGroup= new Group(composite, SWT.NONE);
		categoryGroup.setLayout(createGridLayout(numColumns, true));
		categoryGroup.setLayoutData(createGridData(numColumns, GridData.FILL_BOTH));
		categoryGroup.setText(FormatterMessages.getString("LineWrappingTabPage.line_wrapping_group.title")); //$NON-NLS-1$

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
		
//		fCategoriesViewer.setSelection(new StructuredSelection(fTypeDeclarationSuperclassCategory), true);
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

//	public void selectionChanged(SelectionChangedEvent event) {
//		final Category c= (Category)((IStructuredSelection)event.getSelection()).getFirstElement();
//		fCurrentKey= c.key;
//		if (fCurrentKey != null) {
//		    fOptionsGroup.setText(FormatterMessages.getString("LineWrappingTabPage.group.title") + c.name.toLowerCase()); //$NON-NLS-1$
//		} else {
//		    fOptionsGroup.setText(""); //$NON-NLS-1$
//		}
//		fJavaPreview.setPreviewText(c.previewText);
//		doUpdatePreview();
//		
//		final boolean enabled= fWorkingValues.containsKey(c.key);
//		fOptionsGroup.setVisible(enabled);
//		if (enabled) {
//		    final AlignmentValue value= new AlignmentValue((String)fWorkingValues.get(c.key));
//			fForceSplit.setSelection(value.getForceSplit()); 
//			fIndentStyleCombo.setText(INDENT_NAMES[value.getIndentStyleIndex()]);
//			fSplitStyleCombo.setText(SPLIT_NAMES[value.getSplitStyleIndex()]);
//			updateControls(SPLIT_VALUES[value.getSplitStyleIndex()]);
//		}
//	}
	
	protected void forceSplitChanged(boolean enabled) {
	    final AlignmentValue value= new AlignmentValue((String)fWorkingValues.get(fCurrentKey));
	    value.setForceSplit(enabled);
		fWorkingValues.put(fCurrentKey, value.getValue());
		doUpdatePreview();
	}
	
	protected void splitStyleChanged(int index) {
	    final AlignmentValue value= new AlignmentValue((String)fWorkingValues.get(fCurrentKey));
		value.setSplitStyleIndex(index);
		fWorkingValues.put(fCurrentKey, value.getValue());
		updateControls(SPLIT_VALUES[index]);
		doUpdatePreview();
	}
	
	protected void indentStyleChanged(int index) {
	    final AlignmentValue value= new AlignmentValue((String)fWorkingValues.get(fCurrentKey));
	    value.setIndentStyleIndex(index);
		fWorkingValues.put(fCurrentKey, value.getValue());
		doUpdatePreview();
	}
	
	protected void updateControls(int splitStyle) {
	    boolean doSplit= splitStyle != SPLIT_DO_NOT;
	    fIndentStyleLabel.setEnabled(doSplit);
	    fIndentStyleCombo.setEnabled(doSplit);
	    fForceSplit.setEnabled(doSplit);
	}
}
