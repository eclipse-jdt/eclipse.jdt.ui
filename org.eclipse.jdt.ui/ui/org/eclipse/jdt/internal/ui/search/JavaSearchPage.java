/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

public class JavaSearchPage extends DialogPage implements ISearchPage, IJavaSearchConstants {

	public static final String EXTENSION_POINT_ID= "org.eclipse.jdt.ui.JavaSearchPage";

	private static java.util.List fgPreviousSearchPatterns= new ArrayList(20);

	private Combo fPattern;
	private String fInitialPattern;
	private boolean fFirstTime= true;
	private ISearchPageContainer fContainer;
	
	private Button[] fSearchFor;
	private String[] fSearchForText= {
		JavaPlugin.getResourceString("SearchPage.searchFor.type"),
		JavaPlugin.getResourceString("SearchPage.searchFor.method"),
		JavaPlugin.getResourceString("SearchPage.searchFor.package"),
		JavaPlugin.getResourceString("SearchPage.searchFor.constructor"),
		JavaPlugin.getResourceString("SearchPage.searchFor.field") };

	private Button[] fLimitTo;
	private String[] fLimitToText= {
		JavaPlugin.getResourceString("SearchPage.limitTo.declarations"),
		JavaPlugin.getResourceString("SearchPage.limitTo.implementors"),
		JavaPlugin.getResourceString("SearchPage.limitTo.references"),
		JavaPlugin.getResourceString("SearchPage.limitTo.allOccurrences")};
	
	private IJavaElement fJavaElement;
	
	private static class SearchPatternData {
		public SearchPatternData(int s, int l, String p, IJavaElement element) {
			searchFor= s;
			limitTo= l;
			pattern= p;
			javaElement= element;
		}
		int			searchFor;
		int			limitTo;
		String		pattern;
		IJavaElement	javaElement;
	}

	//---- Action Handling ------------------------------------------------
	
	public boolean performAction() {
		SearchPatternData data= getPatternData();
		IWorkspace workspace= JavaPlugin.getWorkspace();
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		JavaSearchResultCollector collector= new JavaSearchResultCollector();
		JavaSearchOperation op= null;
		if (data.javaElement != null && getPattern().equals(fInitialPattern))
			op= new JavaSearchOperation(workspace, data.javaElement, data.limitTo, scope, collector);
		else {
			data.javaElement= null;
			op= new JavaSearchOperation(workspace, data.pattern, data.searchFor, data.limitTo, scope, collector);
		}
		Shell shell= getControl().getShell();
		try {
			getContainer().getRunnableContext().run(true, true, op);
		} catch (InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, JavaPlugin.getResourceBundle(), "Search.Error.search.");
			return false;
		} catch (InterruptedException ex) {
			return false;
		}
		return true;
	}
	
	private int getLimitTo() {
		for (int i= 0; i < fLimitTo.length; i++) {
			if (fLimitTo[i].getSelection())
				return i;
		}
		Assert.isTrue(false, "Should never happen");
		return -1;
	}

	private String[] getPreviousSearchPatterns() {
		// Search results are not persistent
		int patternCount= fgPreviousSearchPatterns.size();
		String [] patterns= new String[patternCount];
		for (int i= 0; i < patternCount; i++)
			patterns[i]= ((SearchPatternData) fgPreviousSearchPatterns.get(patternCount - 1 - i)).pattern;
		return patterns;
	}
	
	private int getSearchFor() {
		for (int i= 0; i < fSearchFor.length; i++) {
			if (fSearchFor[i].getSelection())
				return i;
		}
		Assert.isTrue(false, "Should never happen");
		return -1;
	}
	
	private String getPattern() {
		return fPattern.getText();
	}

	/**
	 * Return search pattern data and update previous searches.
	 * An existing entry will be updated.
	 */
	private SearchPatternData getPatternData() {
		String pattern= getPattern();
		SearchPatternData match= null;
		int i= 0;
		int size= fgPreviousSearchPatterns.size();
		while (match == null && i < size) {
			match= (SearchPatternData) fgPreviousSearchPatterns.get(i);
			i++;
			if (!pattern.equals(match.pattern))
				match= null;
		};
		if (match == null) {
			match= new SearchPatternData(getSearchFor(), getLimitTo(), getPattern(), fJavaElement);
			fgPreviousSearchPatterns.add(match);
		}
		else {
			match.searchFor= getSearchFor();
			match.limitTo= getLimitTo();
			match.javaElement= fJavaElement;
		};
		return match;
	}

	/*
	 * Implements method from IDialogPage
	 */
	public void setVisible(boolean visible) {
		if (visible && fPattern != null) {
			if (fFirstTime) {
				fFirstTime= false;
				// Set item and text here to prevent page from resizing
				fPattern.setItems(getPreviousSearchPatterns());
				initSelections();
			}
			fPattern.setFocus();
			getContainer().setPerformActionEnabled(fPattern.getText().length() > 0);
		}
		super.setVisible(visible);
	}
	
	public boolean isValid() {
		return true;
	}

	//---- Widget creation ------------------------------------------------

	/**
	 * Creates the page's content.
	 */
	public void createControl(Composite parent) {
		GridData gd;
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2; layout.makeColumnsEqualWidth= true;
		layout.horizontalSpacing= 10;
		result.setLayout(layout);
		
		RowLayouter layouter= new RowLayouter(layout.numColumns);
		gd= new GridData();
		gd.horizontalAlignment= gd.FILL;
		layouter.setDefaultGridData(gd, 0);
		layouter.setDefaultGridData(gd, 1);
		layouter.setDefaultSpan();
		
		layouter.perform(createExpression(result));
		layouter.perform(createSearchFor(result), createLimitTo(result), -1);
		
		SelectionAdapter javaElementInitializer= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fJavaElement= null;
			}
		};

		fSearchFor[FIELD].addSelectionListener(javaElementInitializer);
		fSearchFor[METHOD].addSelectionListener(javaElementInitializer);
		fSearchFor[PACKAGE].addSelectionListener(javaElementInitializer);
		
		fSearchFor[TYPE].addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				boolean state= ((Button)event.widget).getSelection();
				boolean implState= fLimitTo[IMPLEMENTORS].getSelection();
				if (!state && implState) {
					fLimitTo[IMPLEMENTORS].setSelection(false);
					fLimitTo[REFERENCES].setSelection(true);
				}
				fLimitTo[IMPLEMENTORS].setEnabled(state);
				fJavaElement= null;
			}
		});
		setControl(result);
		
		WorkbenchHelp.setHelp(result, new Object[] { IJavaHelpContextIds.JAVA_SEARCH_PAGE });	
	}

	private Control createExpression(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText(JavaPlugin.getResourceString("SearchPage.expression.label"));
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		result.setLayout(layout);
		
		// Pattern combo
		fPattern= new Combo(result, SWT.SINGLE | SWT.BORDER);
		fPattern.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fPattern.getSelectionIndex() < 0)
					return;
				int index= fgPreviousSearchPatterns.size() - 1 - fPattern.getSelectionIndex();
				SearchPatternData values= (SearchPatternData) fgPreviousSearchPatterns.get(index);
				for (int i= 0; i < fSearchFor.length; i++)
					fSearchFor[i].setSelection(false);
				for (int i= 0; i < fLimitTo.length; i++)
					fLimitTo[i].setSelection(false);
				fSearchFor[values.searchFor].setSelection(true);
				fLimitTo[values.limitTo].setSelection(true);
				fLimitTo[IMPLEMENTORS].setEnabled((values.searchFor == TYPE));
				fLimitTo[DECLARATIONS].setEnabled((values.searchFor != PACKAGE));
				fLimitTo[ALL_OCCURRENCES].setEnabled((values.searchFor != PACKAGE));				
				fInitialPattern= values.pattern;
				fPattern.setText(fInitialPattern);
				fJavaElement= values.javaElement;
			}
		});
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getContainer().setPerformActionEnabled(fPattern.getText().length() > 0);
			}
		});
		fPattern.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// Pattern info
		Label label= new Label(result, SWT.LEFT);
		label.setText(JavaPlugin.getResourceString("SearchPage.expression.pattern"));
		return result;
	}
		
	private Control createSearchFor(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText(JavaPlugin.getResourceString("SearchPage.searchFor.label"));
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		result.setLayout(layout);

		fSearchFor= new Button[fSearchForText.length];
		for (int i= 0; i < fSearchForText.length; i++) {
			Button button= new Button(result, SWT.RADIO);
			button.setText(fSearchForText[i]);
			fSearchFor[i]= button;
		}
		
		return result;		
	}
	
	private Control createLimitTo(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText(JavaPlugin.getResourceString("SearchPage.limitTo.label"));
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		fLimitTo= new Button[fLimitToText.length];
		for (int i= 0; i < fLimitToText.length; i++) {
			Button button= new Button(result, SWT.RADIO);
			button.setText(fLimitToText[i]);
			fLimitTo[i]= button;
		}
		return result;		
	}	
	
	private void initSelections() {
		fJavaElement= null;
		ISelection selection= getSelection();
		SearchPatternData values= null;
		values= tryTypedTextSelection(selection);
		if (values == null)
			values= trySelection(selection);
		if (values == null)
			values= trySimpleTextSelection(selection);
		if (values == null)
			values= getDefaultInitValues();
					
		fSearchFor[values.searchFor].setSelection(true);
		fLimitTo[values.limitTo].setSelection(true);
		if (values.searchFor != TYPE)
			fLimitTo[IMPLEMENTORS].setEnabled(false);

		fInitialPattern= values.pattern;
		fPattern.setText(fInitialPattern);
	}

	private SearchPatternData tryTypedTextSelection(ISelection selection) {
		if (selection instanceof ITextSelection) {
			IEditorPart e= getEditorPart();
			if (e != null) {
				ITextSelection ts= (ITextSelection)selection;
				ICodeAssist assist= getCodeAssist(e);
				if (assist != null) {
					IJavaElement[] elements= null;
					try {
						elements= assist.codeSelect(ts.getOffset(), ts.getLength());
					} catch (JavaModelException ex) {
						ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.createJavaElement.");
					}
					if (elements != null && elements.length > 0) {
						if (elements.length == 1)
							fJavaElement= elements[0];
						else
							fJavaElement= chooseFromList(Arrays.asList(elements));
						if (fJavaElement != null)
							return determineInitValuesFrom(fJavaElement);
					}
				}
			}
		}
		return null;
	}
	
	private ICodeAssist getCodeAssist(IEditorPart editorPart) {
		IEditorInput input= editorPart.getEditorInput();
		if (input instanceof ClassFileEditorInput)
			return ((ClassFileEditorInput)input).getClassFile();
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(input);
	}

	private SearchPatternData trySelection(ISelection selection) {
		SearchPatternData result= null;
		if (selection == null)
			return result;
		Object o= null;
		if (selection instanceof IStructuredSelection)
			o= ((IStructuredSelection)selection).getFirstElement();
		if (o instanceof IJavaElement) {
			fJavaElement= (IJavaElement)o;
			result= determineInitValuesFrom(fJavaElement);
		} else if (o instanceof ISearchResultViewEntry) {
			fJavaElement= getJavaElement(((ISearchResultViewEntry)o).getSelectedMarker());
			result= determineInitValuesFrom(fJavaElement);
		} else if (o instanceof IAdaptable) {
			IWorkbenchAdapter element= (IWorkbenchAdapter)((IAdaptable)o).getAdapter(IWorkbenchAdapter.class);
			if (element != null)
				result= new SearchPatternData(TYPE, REFERENCES, element.getLabel(o), null);
		}
		return result;
	}

	private IJavaElement getJavaElement(IMarker marker) {
		try {
			return JavaCore.create((String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.createJavaElement.");
			return null;
		}
	}
	
	private SearchPatternData determineInitValuesFrom(IJavaElement element) {
		if (element == null)
			return null;
		int searchFor= UNKNOWN;
		int limitTo= UNKNOWN;
		String pattern= null; 
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				searchFor= PACKAGE;
				limitTo= REFERENCES;
				pattern= element.getElementName();
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				searchFor= PACKAGE;
				limitTo= REFERENCES;
				pattern= element.getElementName();
				break;
			case IJavaElement.PACKAGE_DECLARATION:
				searchFor= PACKAGE;
				limitTo= REFERENCES;
				pattern= element.getElementName();
				break;
			case IJavaElement.IMPORT_DECLARATION:
				pattern= element.getElementName();
				IImportDeclaration declaration= (IImportDeclaration)element;
				if (declaration.isOnDemand()) {
					searchFor= PACKAGE;
					int index= pattern.lastIndexOf('.');
					pattern= pattern.substring(0, index);
				} else {
					searchFor= TYPE;
				}
				limitTo= DECLARATIONS;
				break;
			case IJavaElement.TYPE:
				searchFor= TYPE;
				limitTo= REFERENCES;
				pattern= JavaModelUtility.getFullyQualifiedName((IType)element);
				break;
			case IJavaElement.COMPILATION_UNIT:
				ICompilationUnit cu= (ICompilationUnit)element;
				String mainTypeName= element.getElementName().substring(0, element.getElementName().length() - 5);
				IType mainType= cu.getType(mainTypeName);
				mainTypeName= JavaModelUtility.getTypeQualifiedName(mainType);
				try {					
					mainType= JavaModelUtility.findTypeInCompilationUnit(cu, mainTypeName);
					if (mainType == null) {
						// fetch type which is declared first in the file
						IType[] types= cu.getTypes();
						if (types.length > 0)
							mainType= types[0];
						else
							break;
					}
				} catch (JavaModelException ex) {
					ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.javaElementAccess.");
					break;
				}
				searchFor= TYPE;
				limitTo= REFERENCES;
				pattern= JavaModelUtility.getFullyQualifiedName((IType)mainType);
				break;
			case IJavaElement.CLASS_FILE:
				IClassFile cf= (IClassFile)element;
				try {					
					mainType= cf.getType();
				} catch (JavaModelException ex) {
					ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.javaElementAccess.");
					break;
				}
				if (mainType == null)
					break;
				searchFor= TYPE;
				limitTo= REFERENCES;
				pattern= JavaModelUtility.getFullyQualifiedName(mainType);
				break;
			case IJavaElement.FIELD:
				searchFor= FIELD;
				limitTo= REFERENCES;
				IType type= ((IField)element).getDeclaringType();
				StringBuffer buffer= new StringBuffer();
				buffer.append(JavaModelUtility.getFullyQualifiedName(type));
				buffer.append('.');
				buffer.append(element.getElementName());
				pattern= buffer.toString();
				break;
			case IJavaElement.METHOD:
				searchFor= METHOD;
				try {
					IMethod method= (IMethod)element;
					if (method.isConstructor())
						searchFor= CONSTRUCTOR;
				} catch (JavaModelException ex) {
					ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.javaElementAccess.");
					break;
				}		
				limitTo= REFERENCES;
				pattern= PrettySignature.getMethodSignature((IMethod)element);
				break;
		}
		if (searchFor != UNKNOWN && limitTo != UNKNOWN && pattern != null)
			return new SearchPatternData(searchFor, limitTo, pattern, element);
			
		return null;	
	}
	
	private SearchPatternData trySimpleTextSelection(ISelection selection) {
		SearchPatternData result= null;
		if (selection instanceof ITextSelection) {
			BufferedReader reader= new BufferedReader(new StringReader(((ITextSelection)selection).getText()));
			String text;
			try {
				text= reader.readLine();
				if (text == null)
					text= "";
			} catch (IOException ex) {
				text= "";
			}
			result= new SearchPatternData(TYPE, REFERENCES, text, null);
		}
		return result;
	}
	
	private SearchPatternData getDefaultInitValues() {
		return new SearchPatternData(TYPE, REFERENCES, "", null);
	}	

	private IJavaElement chooseFromList(java.util.List openChoices) {
		ILabelProvider labelProvider= new JavaElementLabelProvider(
			  JavaElementLabelProvider.SHOW_DEFAULT 
			| JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider, true, false);
		dialog.setTitle(JavaPlugin.getResourceString("SearchElementSelectionDialog.title"));
		dialog.setMessage(JavaPlugin.getResourceString("SearchElementSelectionDialog.message"));
		if (dialog.open(openChoices) == dialog.OK)
			return (IJavaElement)Arrays.asList(dialog.getResult()).get(0);
		return null;
	}

	/*
	 * Implements method from ISearchPage
	 */
	public void setContainer(ISearchPageContainer container) {
		fContainer= container;
	}
	
	/**
	 * Returns the search page's container.
	 */
	private ISearchPageContainer getContainer() {
		return fContainer;
	}
	
	/**
	 * Returns the current active selection.
	 */
	private ISelection getSelection() {
		return fContainer.getSelection();
	}
	
	/**
	 * Returns the current active editor part.
	 */
	private IEditorPart getEditorPart() {
		IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page= window.getActivePage();
			if (page != null)
				return page.getActiveEditor();
		}
		return null;
	}
}