/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;


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

		public Category(String _key, String _previewText, String _name) {
			this.key= _key;
			this.name= _name;
			this.previewText= _previewText != null ? createPreviewHeader(_name) + _previewText : null; //$NON-NLS-1$
			children= new ArrayList();
		}
		
		/**
		 * @param _name Category name
		 */
		public Category(String _name) {
		    this(null, null, _name);
		}
		
		public String toString() {
			return name;
		}
	}
	

	private final static String PREF_CATEGORY_INDEX= JavaUI.ID_PLUGIN + "formatter_page.line_wrapping_tab_page.last_category_index"; //$NON-NLS-1$ 
	
	
	private final class CategoryListener implements ISelectionChangedListener, IDoubleClickListener {
		
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
		    if (event != null)
		        fSelection= (IStructuredSelection)event.getSelection();
		    
		    if (fSelection.size() == 0) {
		        disableAll();
		        return;
		    }
		    
		    if (!fOptionsGroup.isEnabled())
		        enableDefaultComponents(true);
		    
		    fSelectionState.refreshState(fSelection);
		    
			final Category category= (Category)fSelection.getFirstElement();
			fDialogSettings.put(PREF_CATEGORY_INDEX, category.index);
			
			fOptionsGroup.setText(getGroupLabel(category));
		}
		
		private String getGroupLabel(Category category) {
		    if (fSelection.size() == 1) {
			    if (fSelectionState.getElements().size() == 1)
			        return FormatterMessages.getFormattedString("LineWrappingTabPage.group", category.name.toLowerCase()); //$NON-NLS-1$
			    return FormatterMessages.getFormattedString("LineWrappingTabPage.multi_group", new String[] {category.name.toLowerCase(), Integer.toString(fSelectionState.getElements().size())}); //$NON-NLS-1$
		    }
			return FormatterMessages.getFormattedString("LineWrappingTabPage.multiple_selections", new String[] {Integer.toString(fSelectionState.getElements().size())}); //$NON-NLS-1$
		}
        
        private void disableAll() {
            enableDefaultComponents(false);
            fIndentStyleCombo.setEnabled(false);       
            fForceSplit.setEnabled(false);
        }
        
        private void enableDefaultComponents(boolean enabled) {
            fOptionsGroup.setEnabled(enabled);
            fWrappingStyleCombo.setEnabled(enabled);
            fWrappingStylePolicy.setEnabled(enabled);
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

        public void doubleClick(DoubleClickEvent event) {
            final ISelection selection= event.getSelection();
            if (selection instanceof IStructuredSelection) {
                final Category node= (Category)((IStructuredSelection)selection).getFirstElement();
                fCategoriesViewer.setExpandedState(node, !fCategoriesViewer.getExpandedState(node));
            }
        }
	}
	
	private class SelectionState {
	    private List fElements= new ArrayList();
	    
	    public void refreshState(IStructuredSelection selection) {
	        Map wrappingStyleMap= new HashMap();
		    Map indentStyleMap= new HashMap();
		    Map forceWrappingMap= new HashMap();
	        fElements.clear();
	        evaluateElements(selection.iterator());
	        evaluateMaps(wrappingStyleMap, indentStyleMap, forceWrappingMap);
	        setPreviewText(getPreviewText(wrappingStyleMap, indentStyleMap, forceWrappingMap));
	        refreshControls(wrappingStyleMap, indentStyleMap, forceWrappingMap);
	    }
	    
	    public List getElements() {
	        return fElements;
	    }
	    
	    private void evaluateElements(Iterator iterator) {
            Category category;
            String value;
            while (iterator.hasNext()) {
                category= (Category) iterator.next();
                value= (String)fWorkingValues.get(category.key);
                if (value != null) {
                    if (!fElements.contains(category))
                        fElements.add(category);
                }
                else {
                    evaluateElements(category.children.iterator());
                }
            }
        }
	    
	    private void evaluateMaps(Map wrappingStyleMap, Map indentStyleMap, Map forceWrappingMap) {
	        Iterator iterator= fElements.iterator();
            while (iterator.hasNext()) {
                insertIntoMap(wrappingStyleMap, indentStyleMap, forceWrappingMap, (Category)iterator.next());
            }
	    }
  
        private String getPreviewText(Map wrappingMap, Map indentMap, Map forceMap) {
            Iterator iterator= fElements.iterator();
            String previewText= ""; //$NON-NLS-1$
            while (iterator.hasNext()) {
                Category category= (Category)iterator.next();
                previewText= previewText + category.previewText + "\n\n"; //$NON-NLS-1$
            }
            return previewText;
        }
        
        private void insertIntoMap(Map wrappingMap, Map indentMap, Map forceMap, Category category) {
            final String value= (String)fWorkingValues.get(category.key);
            Integer wrappingStyle;
            Integer indentStyle;
            Boolean forceWrapping;
            
            try {
                wrappingStyle= new Integer(DefaultCodeFormatterConstants.getWrappingStyle(value));
                indentStyle= new Integer(DefaultCodeFormatterConstants.getIndentStyle(value));
                forceWrapping= new Boolean(DefaultCodeFormatterConstants.getForceWrapping(value));
            } catch (IllegalArgumentException e) {
				forceWrapping= new Boolean(false);
				indentStyle= new Integer(DefaultCodeFormatterConstants.INDENT_DEFAULT);
				wrappingStyle= new Integer(DefaultCodeFormatterConstants.WRAP_NO_SPLIT);
			} 
			
            increaseMapEntry(wrappingMap, wrappingStyle);
            increaseMapEntry(indentMap, indentStyle);
            increaseMapEntry(forceMap, forceWrapping);
        }
        
        private void increaseMapEntry(Map map, Object type) {
            Integer count= (Integer)map.get(type);
            if (count == null) // not in map yet -> count == 0
                map.put(type, new Integer(1));
            else
                map.put(type, new Integer(count.intValue() + 1));
        }
                
        private void refreshControls(Map wrappingStyleMap, Map indentStyleMap, Map forceWrappingMap) {
            updateCombos(wrappingStyleMap, indentStyleMap);
            updateButton(forceWrappingMap);
            Integer wrappingStyleMax= getWrappingStyleMax(wrappingStyleMap);
			boolean isInhomogeneous= (fElements.size() != ((Integer)wrappingStyleMap.get(wrappingStyleMax)).intValue());
			updateControlEnablement(isInhomogeneous, wrappingStyleMax.intValue());
		    doUpdatePreview();
        }
        
        private Integer getWrappingStyleMax(Map wrappingStyleMap) {
            int maxCount= 0, maxStyle= 0;
            for (int i=0; i<WRAPPING_NAMES.length; i++) {
                Integer count= (Integer)wrappingStyleMap.get(new Integer(i));
                if (count == null)
                    continue;
                if (count.intValue() > maxCount) {
                    maxCount= count.intValue();
                    maxStyle= i;
                }
            }
            return new Integer(maxStyle);
        }
        
        private void updateButton(Map forceWrappingMap) {
            Integer nrOfTrue= (Integer)forceWrappingMap.get(Boolean.TRUE);
            Integer nrOfFalse= (Integer)forceWrappingMap.get(Boolean.FALSE);
            
            if (nrOfTrue == null || nrOfFalse == null)
                fForceSplit.setSelection(nrOfTrue != null);
            else
                fForceSplit.setSelection(nrOfTrue.intValue() > nrOfFalse.intValue());
            
            int max= getMax(nrOfTrue, nrOfFalse);
            String label= FormatterMessages.getString("LineWrappingTabPage.force_split.checkbox.multi_text"); //$NON-NLS-1$
            fForceSplit.setText(getLabelText(label, max, fElements.size())); //$NON-NLS-1$
        }
        
        private String getLabelText(String label, int count, int nElements) {
            if (nElements == 1 || count == 0)
                return label;
            return FormatterMessages.getFormattedString("LineWrappingTabPage.occurences", new String[] {label, Integer.toString(count), Integer.toString(nElements)}); //$NON-NLS-1$
        }
        
        private int getMax(Integer nrOfTrue, Integer nrOfFalse) {
            if (nrOfTrue == null)
                return nrOfFalse.intValue();
            if (nrOfFalse == null)
                return nrOfTrue.intValue();
            if (nrOfTrue.compareTo(nrOfFalse) >= 0)
                return nrOfTrue.intValue();
            return nrOfFalse.intValue();
        }
        
        private void updateCombos(Map wrappingStyleMap, Map indentStyleMap) {
            updateCombo(fWrappingStyleCombo, wrappingStyleMap, WRAPPING_NAMES);
            updateCombo(fIndentStyleCombo, indentStyleMap, INDENT_NAMES);
        }
        
        private void updateCombo(Combo combo, Map map, final String[] items) {
            String[] newItems= new String[items.length];
            int maxCount= 0, maxStyle= 0;
                        
            for(int i = 0; i < items.length; i++) {
                Integer count= (Integer) map.get(new Integer(i));
                int val= (count == null) ? 0 : count.intValue();
                if (val > maxCount) {
                    maxCount= val;
                    maxStyle= i;
                }                
                newItems[i]= getLabelText(items[i], val, fElements.size()); 
            }
            combo.setItems(newItems);
            combo.setText(newItems[maxStyle]);
        }
	}
	
	protected static final String[] INDENT_NAMES = {
	    FormatterMessages.getString("LineWrappingTabPage.indentation.default"), //$NON-NLS-1$ 
	    FormatterMessages.getString("LineWrappingTabPage.indentation.on_column"), //$NON-NLS-1$ 
	    FormatterMessages.getString("LineWrappingTabPage.indentation.by_one") //$NON-NLS-1$
	};
	
	
	protected static final String[] WRAPPING_NAMES = { 
	    FormatterMessages.getString("LineWrappingTabPage.splitting.do_not_split"), //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_when_necessary"), // COMPACT_SPLIT //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.always_wrap_first_others_when_necessary"), // COMPACT_FIRST_BREAK_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always"), // ONE_PER_LINE_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always_indent_all_but_first"), // NEXT_SHIFTED_SPLIT  //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.splitting.wrap_always_except_first_only_if_necessary") // NEXT_PER_LINE_SPLIT //$NON-NLS-1$
	};
	

	private final Category fCompactIfCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF,
	    "class Example {" + //$NON-NLS-1$
	    "int foo(int argument) {" + //$NON-NLS-1$
	    "  if (argument==0) return 0;" + //$NON-NLS-1$
	    "  if (argument==1) return 42; else return 43;" + //$NON-NLS-1$	
	    "}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.compact_if_else") //$NON-NLS-1$
	);
	

	private final Category fTypeDeclarationSuperclassCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION,
	    "class Example extends OtherClass {}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.extends_clause") //$NON-NLS-1$
	);
	

	private final Category fTypeDeclarationSuperinterfacesCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION,
	    "class Example implements I1, I2, I3 {}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.implements_clause") //$NON-NLS-1$
	);
	
	
	private final Category fConstructorDeclarationsParametersCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION,
	    "class Example {Example(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) { this();}" + //$NON-NLS-1$
	    "Example() {}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.parameters") //$NON-NLS-1$
	); 

	private final Category fMethodDeclarationsParametersCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION,
	    "class Example {void foo(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.parameters") //$NON-NLS-1$
	); 
	
	private final Category fMessageSendArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION,
	    "class Example {void foo() {Other.bar( 100, 200, 300, 400, 500, 600, 700, 800, 900 );}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.arguments") //$NON-NLS-1$
	); 

	private final Category fMessageSendSelectorCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION,
	    "class Example {int foo(Some a) {return a.getFirst();}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.qualified_invocations") //$NON-NLS-1$
	);
	
	private final Category fMethodThrowsClauseCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION, 
	    "class Example {" + //$NON-NLS-1$
	    "int foo() throws FirstException, SecondException, ThirdException {" + //$NON-NLS-1$
	    "  return Other.doSomething();}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.throws_clause") //$NON-NLS-1$
	);

	private final Category fConstructorThrowsClauseCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION, 
	    "class Example {" + //$NON-NLS-1$
	    "Example() throws FirstException, SecondException, ThirdException {" + //$NON-NLS-1$
	    "  return Other.doSomething();}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.throws_clause") //$NON-NLS-1$
	);

	
	private final Category fAllocationExpressionArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION,
	    "class Example {SomeClass foo() {return new SomeClass(100, 200, 300, 400, 500, 600, 700, 800, 900 );}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.object_allocation") //$NON-NLS-1$
	);
	
	private final Category fQualifiedAllocationExpressionCategory= new Category (
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION,
	    "class Example {SomeClass foo() {return SomeOtherClass.new SomeClass(100, 200, 300, 400, 500 );}}", //$NON-NLS-1$
		FormatterMessages.getString("LineWrappingTabPage.qualified_object_allocation") //$NON-NLS-1$
	);
	
	private final Category fArrayInitializerExpressionsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER,
	    "class Example {int [] fArray= {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.array_init") //$NON-NLS-1$
	);
	
	private final Category fExplicitConstructorArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL,
	    "class Example extends AnotherClass {Example() {super(100, 200, 300, 400, 500, 600, 700);}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.explicit_constructor_invocations") //$NON-NLS-1$
	);

	private final Category fConditionalExpressionCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION,
	    "class Example extends AnotherClass {int Example(boolean Argument) {return argument ? 100000 : 200000;}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.conditionals") //$NON-NLS-1$
	);

	private final Category fBinaryExpressionCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
	    "class Example extends AnotherClass {" + //$NON-NLS-1$
	    "int foo() {" + //$NON-NLS-1$
	    "  int sum= 100 + 200 + 300 + 400 + 500 + 600 + 700 + 800;" + //$NON-NLS-1$
	    "  int product= 1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10;" + //$NON-NLS-1$
	    "  boolean val= true && false && true && false && true;" +  //$NON-NLS-1$
	    "  return product / sum;}}", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.binary_exprs") //$NON-NLS-1$
	);
	
	private final Category fEnumConstArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ENUM_CONSTANT,
	    "enum Example {" + //$NON-NLS-1$
	    "CANCELLED, RUNNING, WAITING, FINISHED }", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.enum_arguments") //$NON-NLS-1$
	);
	
	private final Category fEnumDeclInterfacesCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION,
	    "enum Example implements A, B {" + //$NON-NLS-1$
	    "CANCELLED, RUNNING, WAITING, FINISHED }", //$NON-NLS-1$
	    FormatterMessages.getString("LineWrappingTabPage.enum_superinterfaces") //$NON-NLS-1$
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
	protected Label fWrappingStylePolicy;
	protected Combo fWrappingStyleCombo;
	protected Label fIndentStylePolicy;
	protected Combo fIndentStyleCombo;
	protected Button fForceSplit;

	protected CompilationUnitPreview fPreview;

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
	 * The current selection of elements. 
	 */
	protected IStructuredSelection fSelection;
	
	/**
	 * An object containing the state for the UI.
	 */
	SelectionState fSelectionState;
	
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
	 * @param modifyDialog
	 * @param workingValues
	 */
	public LineWrappingTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);

		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		final String previewLineWidth= fDialogSettings.get(PREF_PREVIEW_LINE_WIDTH);
		
		fPreviewPreferences= new HashMap();
		fPreviewPreferences.put(LINE_SPLIT, previewLineWidth != null ? previewLineWidth : Integer.toString(DEFAULT_PREVIEW_WINDOW_LINE_WIDTH));
		
		fCategories= createCategories();
		fCategoryListener= new CategoryListener(fCategories);
	}
	
	/**
	 * @return Create the categories tree.
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

		final Category enumDeclarations= new Category(FormatterMessages.getString("LineWrappingTabPage.enum_decls")); //$NON-NLS-1$
		enumDeclarations.children.add(fEnumConstArgumentsCategory);
		enumDeclarations.children.add(fEnumDeclInterfacesCategory);
		
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
		root.add(enumDeclarations);
		root.add(functionCalls);
		root.add(expressions);
		root.add(statements);
		
		return root;
	}
	
	protected void doCreatePreferences(Composite composite, int numColumns) {
	
		final Group lineWidthGroup= createGroup(numColumns, composite, FormatterMessages.getString("LineWrappingTabPage.width_indent")); //$NON-NLS-1$

		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.max_line_width"), DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, 0, 9999); //$NON-NLS-1$
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.default_indent_wrapped"), DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, 0, 9999); //$NON-NLS-1$
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.getString("LineWrappingTabPage.width_indent.option.default_indent_array"), DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER, 0, 9999); //$NON-NLS-1$ 

		fCategoriesViewer= new TreeViewer(composite /*categoryGroup*/, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL );
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

		final GridData gd= createGridData(numColumns, GridData.FILL_BOTH, 0);
		fCategoriesViewer.getControl().setLayoutData(gd);

		fOptionsGroup = createGroup(numColumns, composite, "");  //$NON-NLS-1$
		
		// label "Select split style:"
		fWrappingStylePolicy= createLabel(numColumns, fOptionsGroup, FormatterMessages.getString("LineWrappingTabPage.wrapping_policy.label.text")); //$NON-NLS-1$
	
		// combo SplitStyleCombo
		fWrappingStyleCombo= new Combo(fOptionsGroup, SWT.SINGLE | SWT.READ_ONLY);
		fWrappingStyleCombo.setItems(WRAPPING_NAMES);
		fWrappingStyleCombo.setLayoutData(createGridData(numColumns, GridData.HORIZONTAL_ALIGN_FILL, 0));
		
		// label "Select indentation style:"
		fIndentStylePolicy= createLabel(numColumns, fOptionsGroup, FormatterMessages.getString("LineWrappingTabPage.indentation_policy.label.text")); //$NON-NLS-1$
		
		// combo SplitStyleCombo
		fIndentStyleCombo= new Combo(fOptionsGroup, SWT.SINGLE | SWT.READ_ONLY);
		fIndentStyleCombo.setItems(INDENT_NAMES);
		fIndentStyleCombo.setLayoutData(createGridData(numColumns, GridData.HORIZONTAL_ALIGN_FILL, 0));
		
		// button "Force split"
		fForceSplit= new Button(fOptionsGroup, SWT.CHECK);
		fForceSplit.setLayoutData(createGridData(numColumns, GridData.HORIZONTAL_ALIGN_FILL, 0));
		fForceSplit.setText(FormatterMessages.getString("LineWrappingTabPage.force_split.checkbox.text")); //$NON-NLS-1$
		
		// selection state object
		fSelectionState= new SelectionState();
	}
	
		
	protected Composite doCreatePreviewPane(Composite composite, int numColumns) {
		
		super.doCreatePreviewPane(composite, numColumns);
		
		final NumberPreference previewLineWidth= new NumberPreference(composite, numColumns / 2, fPreviewPreferences, LINE_SPLIT,
		    0, 9999, FormatterMessages.getString("LineWrappingTabPage.line_width_for_preview.label.text")); //$NON-NLS-1$
		fDefaultFocusManager.add(previewLineWidth);
		previewLineWidth.addObserver(fUpdater);
		previewLineWidth.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				fDialogSettings.put(PREF_PREVIEW_LINE_WIDTH, (String)fPreviewPreferences.get(LINE_SPLIT));
			}
		});
		
		return composite;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doCreateJavaPreview(org.eclipse.swt.widgets.Composite)
     */
    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fPreview= new CompilationUnitPreview(fWorkingValues, parent);
        return fPreview;
    }

	
	protected void initializePage() {
		
		fCategoriesViewer.addSelectionChangedListener(fCategoryListener);
		fCategoriesViewer.addDoubleClickListener(fCategoryListener);
		
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
		fWrappingStyleCombo.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wrappingStyleChanged(((Combo)e.widget).getSelectionIndex());
			}
		});
		
		fCategoryListener.restoreSelection();
		
		fDefaultFocusManager.add(fCategoriesViewer.getControl());
		fDefaultFocusManager.add(fWrappingStyleCombo);
		fDefaultFocusManager.add(fIndentStyleCombo);
		fDefaultFocusManager.add(fForceSplit);
	}
	
	protected void doUpdatePreview() {
		final Object normalSetting= fWorkingValues.get(LINE_SPLIT);
		fWorkingValues.put(LINE_SPLIT, fPreviewPreferences.get(LINE_SPLIT));
		fPreview.update();
		fWorkingValues.put(LINE_SPLIT, normalSetting);
	}
	
	protected void setPreviewText(String text) {
		final Object normalSetting= fWorkingValues.get(LINE_SPLIT);
		fWorkingValues.put(LINE_SPLIT, fPreviewPreferences.get(LINE_SPLIT));
		fPreview.setPreviewText(text);
		fWorkingValues.put(LINE_SPLIT, normalSetting);
	}
	
	protected void forceSplitChanged(boolean forceSplit) {
	    Iterator iterator= fSelectionState.fElements.iterator();
	    String currentKey;
        while (iterator.hasNext()) {
            currentKey= ((Category)iterator.next()).key;
            try {
                changeForceSplit(currentKey, forceSplit);
            } catch (IllegalArgumentException e) {
    			fWorkingValues.put(currentKey, DefaultCodeFormatterConstants.createAlignmentValue(forceSplit, DefaultCodeFormatterConstants.WRAP_NO_SPLIT, DefaultCodeFormatterConstants.INDENT_DEFAULT));
    			JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, 
    			        FormatterMessages.getFormattedString("LineWrappingTabPage.error.invalid_value", currentKey), e)); //$NON-NLS-1$
    		}
        }
        fSelectionState.refreshState(fSelection);
	}
	
	private void changeForceSplit(String currentKey, boolean forceSplit) throws IllegalArgumentException{
		String value= (String)fWorkingValues.get(currentKey);
		value= DefaultCodeFormatterConstants.setForceWrapping(value, forceSplit);
		if (value == null)
		    throw new IllegalArgumentException();
		fWorkingValues.put(currentKey, value);
	}
	
	protected void wrappingStyleChanged(int wrappingStyle) {
	       Iterator iterator= fSelectionState.fElements.iterator();
	       String currentKey;
	        while (iterator.hasNext()) {
	        	currentKey= ((Category)iterator.next()).key;
	        	try {
	        	    changeWrappingStyle(currentKey, wrappingStyle);
	        	} catch (IllegalArgumentException e) {
	    			fWorkingValues.put(currentKey, DefaultCodeFormatterConstants.createAlignmentValue(false, wrappingStyle, DefaultCodeFormatterConstants.INDENT_DEFAULT));
	    			JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, 
	    			        FormatterMessages.getFormattedString("LineWrappingTabPage.error.invalid_value", currentKey), e)); //$NON-NLS-1$
	        	}
	        }
	        fSelectionState.refreshState(fSelection);
	}
	
	private void changeWrappingStyle(String currentKey, int wrappingStyle) throws IllegalArgumentException {
	    String value= (String)fWorkingValues.get(currentKey);
		value= DefaultCodeFormatterConstants.setWrappingStyle(value, wrappingStyle);
		if (value == null)
		    throw new IllegalArgumentException();
		fWorkingValues.put(currentKey, value);
	}
	
	protected void indentStyleChanged(int indentStyle) {
	    Iterator iterator= fSelectionState.fElements.iterator();
	    String currentKey;
        while (iterator.hasNext()) {
            currentKey= ((Category)iterator.next()).key;
        	try {
            	changeIndentStyle(currentKey, indentStyle);
        	} catch (IllegalArgumentException e) {
    			fWorkingValues.put(currentKey, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_NO_SPLIT, indentStyle));
    			JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, 
    			        FormatterMessages.getFormattedString("LineWrappingTabPage.error.invalid_value", currentKey), e)); //$NON-NLS-1$
    		}
        }
        fSelectionState.refreshState(fSelection);
	}
	
	private void changeIndentStyle(String currentKey, int indentStyle) throws IllegalArgumentException{
		String value= (String)fWorkingValues.get(currentKey);
		value= DefaultCodeFormatterConstants.setIndentStyle(value, indentStyle);
		if (value == null)
		    throw new IllegalArgumentException();
		fWorkingValues.put(currentKey, value);
	}
    
    protected void updateControlEnablement(boolean inhomogenous, int wrappingStyle) {
	    boolean doSplit= wrappingStyle != DefaultCodeFormatterConstants.WRAP_NO_SPLIT;
	    fIndentStylePolicy.setEnabled(true);
	    fIndentStyleCombo.setEnabled(inhomogenous || doSplit);
	    fForceSplit.setEnabled(inhomogenous || doSplit);
	}
}
