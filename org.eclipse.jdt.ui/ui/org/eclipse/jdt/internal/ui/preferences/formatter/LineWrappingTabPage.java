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

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

 
/**
 * @author sib
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LineWrappingTabPage extends ModifyDialogTabPage implements ISelectionChangedListener {


	private static class Category {
		final public String key;
		final public String name;
		final public String previewText;
		final public Collection children;
		
		public Category( String key, String previewText, String name, Collection children ) {
			this.key= key;
			this.name= name;
			this.previewText= previewText;
			this.children= children;
		}
		
		public String toString() {
			return name;
		}
	}
	

	private final String COMPACT_IF_PREVIEW= 
	createPreviewHeader("Compact If") +
	"class Example {" +
	"int foo(int argument) {" +
	"  if (argument==0) return 0;" +
	"  if (argument==1) return 42; else return 43;" +	
	"}}";
	private final Category compactIfCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_COMPACT_IF_ALIGNMENT,
			COMPACT_IF_PREVIEW,
			"Compact 'if...else'", null);
	
	

	private final String TYPE_DECLARATION_SUPERCLASS_PREVIEW= 
	createPreviewHeader("Superclass Declaration") +
	"class Example extends OtherClass {}";
	
	private final Category typeDeclarationSuperclassCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_SUPERCLASS_ALIGNMENT,
			TYPE_DECLARATION_SUPERCLASS_PREVIEW, "'extends' clause", null);
	

	private final String TYPE_DECLARATION_SUPERINTERFACES_PREVIEW=
	createPreviewHeader("Superinterfaces Declaration") +
	"class Example implements I1, I2, I3 {}";
	private final Category typeDeclarationSuperinterfacesCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_SUPERINTERFACES_ALIGNMENT,
			TYPE_DECLARATION_SUPERINTERFACES_PREVIEW, "'implements' clause", null);
	
	
	private final String METHOD_DECLARATION_ARGUMENTS_PREVIEW= 
	createPreviewHeader("Method Declaration Parameters") +
	"class Example {" +
	"void foo(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {" +
	"}" +
	"}";
	private final Category methodDeclarationsArgumentsCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_METHOD_DECLARATION_ARGUMENTS_ALIGNMENT,
			METHOD_DECLARATION_ARGUMENTS_PREVIEW, "Parameters", null);
			
	
	

	private final String MESSAGE_SEND_ARGUMENTS_PREVIEW=
	createPreviewHeader("Method Call Arguments") +
	"class Example {" +
	"void foo() {" +
	"  Other.bar( 100, 200, 300, 400, 500, 600, 700, 800, 900 );" +
	"}" +
	"}";
	private final Category messageSendArgumentsCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_MESSAGE_SEND_ARGUMENTS_ALIGNMENT,
			MESSAGE_SEND_ARGUMENTS_PREVIEW, "Arguments", null);
	
		
	private final String MESSAGE_SEND_SELECTOR_PREVIEW=
	createPreviewHeader("Method Call Selector") +
	"class Example {" +
	"int foo(Some a) {" +
	"  return a.getFirst();" +
	"}" +
	"}";
	private final Category messageSendSelectorCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_MESSAGE_SEND_SELECTOR_ALIGNMENT,
			MESSAGE_SEND_SELECTOR_PREVIEW, "Qualified invocations", null);
	
	
	private final String METHOD_THROWS_CLAUSE_PREVIEW= 
	createPreviewHeader("Method Declaration Throws Clause") +
	"class Example {" +
	"int foo() throws FirstException, SecondException, ThirdException {" +
	"  return Other.doSomething();" +
	"}" +
	"}";
	private final Category methodThrowsClauseCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_METHOD_THROWS_CLAUSE_ALIGNMENT, 
			METHOD_THROWS_CLAUSE_PREVIEW, "'throws' clause", null);
	
	
	private final String ALLOCATION_EXPRESSION_ARGUMENTS_PREVIEW= 
	createPreviewHeader("Allocation Expressions") +
	"class Example {" +
	"SomeClass foo() {" +
	"  return new SomeClass(100, 200, 300, 400, 500, 600, 700, 800, 900 );" +
	"}" +
	"}";
	private final Category allocationExpressionArgumentsCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT,
			ALLOCATION_EXPRESSION_ARGUMENTS_PREVIEW, "Object allocation arguments", null);
	
	
	private final String QUALIFIED_ALLOCATION_EXPRESSION_PREVIEW= 
	createPreviewHeader("Qualified Allocation Expressions") +
	"class Example {" +
	"SomeClass foo() {" +
	"  return SomeOtherClass.new SomeClass(100, 200, 300, 400, 500 );" +
	"}" +
	"}";
	private final Category qualifiedAllocationExpressionCategory= new Category (
			DefaultCodeFormatterConstants.FORMATTER_QUALIFIED_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT,
			QUALIFIED_ALLOCATION_EXPRESSION_PREVIEW, "Qualified object allocation arguments", null);
	
	
	private final String ARRAY_INITIALIZER_EXPRESSIONS_PREVIEW= 
	createPreviewHeader("Array Initializers") +
	"class Example {" +
	"int [] fArray= {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};" +
	"}";
	private final Category arrayInitializerExpressionsCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_EXPRESSIONS_ALIGNMENT,
			ARRAY_INITIALIZER_EXPRESSIONS_PREVIEW, "Array initializers", null);
	

	private final String EXPLICIT_CONSTRUCTOR_ARGUMENTS_PREVIEW=
	createPreviewHeader("Explicit Constructor Arguments") +
	"class Example extends AnotherClass {" +
	"Example() {" +
	"  super(100, 200, 300, 400, 500, 600, 700);" +
	"}" +
	"}";
	private final Category explicitConstructorArgumentsCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_EXPLICIT_CONSTRUCTOR_ARGUMENTS_ALIGNMENT,
			EXPLICIT_CONSTRUCTOR_ARGUMENTS_PREVIEW,	"Explicit constructor invocations",	null);
	

	private final String CONDITIONAL_EXPRESSION_PREVIEW= 
	createPreviewHeader("Conditional Expressions") +
	"class Example extends AnotherClass {" +
	"int Example(boolean Argument) {" +
	"  return argument ? 100000 : 200000;" +
	"}" +
	"}";
	private final Category conditionalExpressionCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_CONDITIONAL_EXPRESSION_ALIGNMENT,
			CONDITIONAL_EXPRESSION_PREVIEW, "Conditionals",	null);
	

	private final String BINARY_EXPRESSION_PREVIEW= 
	createPreviewHeader("Binary Expressions") +
	"class Example extends AnotherClass {" +
	"int foo() {" +
	"  int sum= 100 + 200 + 300 + 400 + 500 + 600 + 700 + 800;" +
	"  int product= 1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10;" +
	"  boolean val= true && false && true && false && true;" + //(1 == 2) && ( 2 == 3 ) || (4==5) || false && true;" +
	"  return product / sum;" +
	"}" +
	"}";
	private final Category binaryExpressionCategory= new Category(
			DefaultCodeFormatterConstants.FORMATTER_BINARY_EXPRESSION_ALIGNMENT,
			BINARY_EXPRESSION_PREVIEW, "Binary expressions", null);
	

	
	
	// force splitting
	private final int ALIGNMENT_FORCE= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FORCE);
	
	// indentation style
	private final int NO_ALIGNMENT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_NO_ALIGNMENT);
	private final int INDENT_ON_COLUMN= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_INDENT_ON_COLUMN);
	private final int INDENT_BY_ONE= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_INDENT_BY_ONE);

	private final String [] INDENT_STYLE_NAMES= { "Default indentation", "Indent on column", "Indent by one" };
	private final int [] INDENT_STYLE_VALUES= { NO_ALIGNMENT, INDENT_ON_COLUMN, INDENT_BY_ONE };
	
	private final int fIndentMask;
	
	
	// splitting style
	private final int COMPACT_SPLIT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_COMPACT_SPLIT);
	private final int FIRST_BREAK_SPLIT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_COMPACT_FIRST_BREAK_SPLIT);
	private final int ONE_PER_LINE_SPLIT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_ONE_PER_LINE_SPLIT);
	private final int NEXT_SHIFTED_SPLIT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_NEXT_SHIFTED_SPLIT);
	private final int NEXT_PER_LINE_SPLIT= Integer.parseInt(DefaultCodeFormatterConstants.FORMATTER_NEXT_PER_LINE_SPLIT);
	
	private final String[] SPLIT_STYLE_NAMES = { 
				"Wrap only when necessary", // COMPACT_SPLIT
				"Always wrap first element, others when necessary", // COMPACT_FIRST_BREAK_SPLIT 
				"Wrap always", // ONE_PER_LINE_SPLIT 
				"Wrap always, indent all but the first element", // NEXT_SHIFTED_SPLIT 
				"Wrap always, except first element only if necessary" }; // NEXT_PER_LINE_SPLIT
	private final int [] SPLIT_STYLE_VALUES = { COMPACT_SPLIT, FIRST_BREAK_SPLIT, ONE_PER_LINE_SPLIT, NEXT_SHIFTED_SPLIT, NEXT_PER_LINE_SPLIT };  
				
	private final int fSplitMask;


	

	// other fields
	
	private static int DEFAULT_PREVIEW_WINDOW_LINE_WIDTH= 40;
		
	private TreeViewer fCategoriesViewer;
	protected Combo fSplitStyleCombo;
	protected Combo fIndentStyleCombo;
	protected Button fForceSplit;
	private Composite fOptionsComposite;

	
	private final Collection fCategories;

	private String fCurrentKey;
	
	protected final Map fPreviewPreferences;
	private final String LINE_SPLIT= DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT;
	
	
	public LineWrappingTabPage(Map workingValues) {
		super(workingValues);
		
		fPreviewPreferences= new HashMap();
		fPreviewPreferences.put(LINE_SPLIT, Integer.toString(DEFAULT_PREVIEW_WINDOW_LINE_WIDTH));
		
		fCategories= compileCategories();
		
		fCurrentKey= ((Category)fCategories.iterator().next()).key;
		
		// calculate masks
		fSplitMask= getMask( SPLIT_STYLE_VALUES );
		fIndentMask= getMask( INDENT_STYLE_VALUES );
	}
	
	protected Collection compileCategories() {

		final List classDeclarationsChildren= new ArrayList(2);
		classDeclarationsChildren.add(typeDeclarationSuperclassCategory);
		classDeclarationsChildren.add(typeDeclarationSuperinterfacesCategory);
		final Category classDeclarations= new Category(null, null, "Class Declarations", classDeclarationsChildren );
		
		final List methodDeclarationsChildren= new ArrayList(2);
		methodDeclarationsChildren.add(methodDeclarationsArgumentsCategory);
		methodDeclarationsChildren.add(methodThrowsClauseCategory);
		final Category methodDeclarations= new Category(null, null, "Method Declarations", methodDeclarationsChildren );
		
		final List functionCallsChildren= new ArrayList(5);
		functionCallsChildren.add(messageSendArgumentsCategory);
		functionCallsChildren.add(messageSendSelectorCategory);
		functionCallsChildren.add(explicitConstructorArgumentsCategory);
		functionCallsChildren.add(allocationExpressionArgumentsCategory);
		functionCallsChildren.add(qualifiedAllocationExpressionCategory);
		final Category functionCalls= new Category(null, null, "Function Calls", functionCallsChildren);
		
		final List expressionsChildren= new ArrayList(3);
		expressionsChildren.add(binaryExpressionCategory);
		expressionsChildren.add(conditionalExpressionCategory);
		expressionsChildren.add(arrayInitializerExpressionsCategory);
		final Category expressions= new Category(null, null, "Expressions", expressionsChildren);
		
		final List statementsChildren= new ArrayList();
		statementsChildren.add(compactIfCategory);
		final Category statements= new Category(null, null, "Statements", statementsChildren);
		
		final List root= new ArrayList();
		root.add(classDeclarations);
		root.add(methodDeclarations);
		root.add(functionCalls);
		root.add(expressions);
		root.add(statements);
		
		return root;
	}
	
	protected int getMask(int [] values) {
		int mask= 0;
		for (int i= 0; i < values.length; i++) {
			mask |= values[i];
		}
		return mask; 
	}
	
	protected int getOffsetOfMask( int mask ) {
		int offset= 0;
		if (mask == 0) return offset;
		while (((mask >> offset) & 0x01) == 0) {
			offset++;
		}
		return offset;
	}

	protected Composite doCreatePreferences(Composite parent) {
	
		final int numColumns= 3;
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		createLabel(numColumns, composite, "&Category:");
		
		fCategoriesViewer= new TreeViewer(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL );
		fCategoriesViewer.setContentProvider(new ITreeContentProvider() {
			public Object[] getElements(Object inputElement) {
				return ((Collection)inputElement).toArray();
			}
			public Object[] getChildren(Object parentElement) {
				return ((Category)parentElement).children.toArray();
			}
			public Object getParent(Object element) { return null; }
			public boolean hasChildren(Object element) {
				return ((Category)element).children != null;
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			public void dispose() {}
		});
		fCategoriesViewer.setLabelProvider(new LabelProvider());
		fCategoriesViewer.setInput(fCategories);
		
		fCategoriesViewer.setExpandedElements(fCategories.toArray());

		final GridData gd= createGridData(numColumns, GridData.FILL_BOTH);
//		gd.heightHint= fPixelConverter.convertHeightInCharsToPixels(CATEGORIES_VIEWER_LINES);
		fCategoriesViewer.getControl().setLayoutData(gd);
		

		final Group group= new Group(composite, SWT.SHADOW_ETCHED_OUT);
		group.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL));
		group.setLayout(createGridLayout(numColumns, false));
		
		
		// composite for options
		fOptionsComposite= new Composite(group, SWT.NONE);
		fOptionsComposite.setLayoutData(createGridData(numColumns, GridData.FILL_BOTH));
		fOptionsComposite.setLayout(createGridLayout(numColumns, true));
		
		// label "Select split style:"
		createLabel(numColumns, fOptionsComposite, "Line &wrapping policy:");
	
		// combo SplitStyleCombo
		fSplitStyleCombo= new Combo(fOptionsComposite, SWT.SINGLE | SWT.READ_ONLY);
		fSplitStyleCombo.setItems(SPLIT_STYLE_NAMES);
		fSplitStyleCombo.setLayoutData(createGridData(numColumns ) /*, GridData.FILL_HORIZONTAL)*/);
		
		// label "Select indentation style:"
		createLabel(numColumns, fOptionsComposite, "Indent&ation policy:");
		
		// combo SplitStyleCombo
		fIndentStyleCombo= new Combo(fOptionsComposite, SWT.SINGLE | SWT.READ_ONLY);
		fIndentStyleCombo.setItems(INDENT_STYLE_NAMES);
		fIndentStyleCombo.setLayoutData(createGridData(numColumns /*GridData.FILL_HORIZONTAL)*/));
		
		// button "Force split"
		fForceSplit= new Button(fOptionsComposite, SWT.CHECK);
		fForceSplit.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL));
		fForceSplit.setText("&Force split");
		
		return composite;
	}
	
		
	protected Composite doCreatePreview(Composite parent) {
		final int numColumns= 4;
		
		final Composite composite= new Composite(parent, SWT.NONE);

		composite.setLayout(createGridLayout(numColumns, false));

		final Composite preview= super.doCreatePreview(composite);
		preview.setLayoutData(createGridData(numColumns, GridData.FILL_BOTH));
		
		final NumberPreference np= new NumberPreference(composite, numColumns, fPreviewPreferences, LINE_SPLIT,
				0, Integer.MAX_VALUE, "Set line width for preview window:");
		np.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				updatePreview();
			}
		});
		
		return composite;
	}
	
	protected void doInitializeControls() {
		
		fCategoriesViewer.addSelectionChangedListener(this);
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
		
		fCategoriesViewer.setSelection(new StructuredSelection(fCategories.iterator().next()), true);
	}
	
	protected void updatePreview() {
		final Object normalSetting= fWorkingValues.get(LINE_SPLIT);
		fWorkingValues.put(LINE_SPLIT, fPreviewPreferences.get(LINE_SPLIT));
		super.updatePreview();
		fWorkingValues.put(LINE_SPLIT, normalSetting);
	}

	public void selectionChanged(SelectionChangedEvent event) {
		final Category c= (Category)((IStructuredSelection)event.getSelection()).getFirstElement();
		fCurrentKey= c.key;
		fJavaPreview.setPreviewText(c.previewText);
		updatePreview();
		final String key= (String)fWorkingValues.get(c.key);

		final boolean enabled= key != null;
		fOptionsComposite.setVisible(enabled);
		
		if (enabled) {
			final int value= Integer.parseInt((String)fWorkingValues.get(c.key));		
			
			final int forceSplit= value & ALIGNMENT_FORCE;
			fForceSplit.setSelection(forceSplit == ALIGNMENT_FORCE); 

			final int indent= value & fIndentMask;
			for (int i= 0; i < INDENT_STYLE_VALUES.length; i++) {
				if (INDENT_STYLE_VALUES[i] == indent) {
					fIndentStyleCombo.setText(INDENT_STYLE_NAMES[i]);
					break;
				}
			}
			
			final int split= value & fSplitMask;
			for (int i= 0; i < SPLIT_STYLE_VALUES.length; i++) {
				if (SPLIT_STYLE_VALUES[i] == split) {
					fSplitStyleCombo.setText(SPLIT_STYLE_NAMES[i]);
					break;
				}
			}
		}
	}
	
	protected void forceSplitChanged(boolean state) {
		int bitField= Integer.parseInt((String)fWorkingValues.get(fCurrentKey));
		if (state) {
			bitField |= ALIGNMENT_FORCE;
		} else {
			bitField &= ~ALIGNMENT_FORCE;
		}
		fWorkingValues.put(fCurrentKey, Integer.toString(bitField));
		updatePreview();
	}
	
	protected void splitStyleChanged(int index) {
		int bitField= Integer.parseInt((String)fWorkingValues.get(fCurrentKey));
		bitField = (bitField & ~fSplitMask) | SPLIT_STYLE_VALUES[index];
		fWorkingValues.put(fCurrentKey, Integer.toString(bitField));
		updatePreview();
	}
	
	protected void indentStyleChanged(int index) {
		int bitField= Integer.parseInt((String)fWorkingValues.get(fCurrentKey));
		bitField = (bitField & ~fIndentMask) | INDENT_STYLE_VALUES[index];
		fWorkingValues.put(fCurrentKey, Integer.toString(bitField));
		updatePreview();
	}	
}
