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
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

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

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.filters.EmptyInnerPackageFilter;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.search.IJavaSearchUIConstants;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.PrettySignature;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.RowLayouter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.LibraryFilter;

public class NLSSearchPage extends DialogPage implements ISearchPage, IJavaSearchConstants {

	public static final String EXTENSION_POINT_ID= "org.eclipse.jdt.ui.nls.NLSSearchPage"; //$NON-NLS-1$

	private static java.util.List fgPreviousSearchPatterns= new ArrayList(20);

	private Combo fWrapperClassCombo;
	private Text fPropertyFileText;
	private boolean fFirstTime= true;
	private ISearchPageContainer fContainer;

	private IJavaElement fWrapperClass;

	private static class SearchPatternData {

		String			propertyFileName;
		IFile			propertyFile;
		String			wrapperClassName;
		IJavaElement	wrapperClass;
		int			scope;
		IWorkingSet[]	workingSets;

		public SearchPatternData(String wrapperClassName, IJavaElement wrapperClass, String p) {
			this(wrapperClassName, wrapperClass, p, ISearchPageContainer.WORKSPACE_SCOPE, null);
		}

		public SearchPatternData(String wrapperClassName, IJavaElement wrapperClass, String p, int scope , IWorkingSet[] workingSets) {
			if (wrapperClassName == null)
				this.wrapperClassName= "";  //$NON-NLS-1$
			else
				this.wrapperClassName= wrapperClassName;
			this.wrapperClass= wrapperClass;
			this.scope= scope;
			this.workingSets= workingSets;
			propertyFileName= p;
			if (p != null && p.length() > 0) {
				IPath path= new Path(propertyFileName);
				if (path.segmentCount() >= 2)
					propertyFile= JavaPlugin.getWorkspace().getRoot().getFile(path);
			}
		}
	}

	//---- Action Handling ------------------------------------------------

	public boolean performAction() {
		SearchUI.activateSearchResultView();
		SearchPatternData data= getPatternData();
		if (data.wrapperClass == null || data.propertyFile == null)
			return false;
		IWorkspace workspace= JavaPlugin.getWorkspace();

		// Setup search scope
		IJavaSearchScope scope= null;
		String scopeDescription= ""; //$NON-NLS-1$
		switch (getContainer().getSelectedScope()) {
			case ISearchPageContainer.WORKSPACE_SCOPE :
				scopeDescription= NLSSearchMessages.getString("WorkspaceScope"); //$NON-NLS-1$
				scope= SearchEngine.createWorkspaceScope();
				break;
			case ISearchPageContainer.SELECTION_SCOPE :
				scopeDescription= NLSSearchMessages.getString("SelectionScope"); //$NON-NLS-1$
				scope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(getSelection());
				break;
			case ISearchPageContainer.SELECTED_PROJECTS_SCOPE :
				scope= JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(getSelection());
				IProject[] projects= JavaSearchScopeFactory.getInstance().getJavaProjects(scope);
				if (projects.length > 1)
					scopeDescription= NLSSearchMessages.getFormattedString("EnclosingProjectsScope", projects[0].getName()); //$NON-NLS-1$
				else if (projects.length == 1)
					scopeDescription= NLSSearchMessages.getFormattedString("EnclosingProjectScope", projects[0].getName()); //$NON-NLS-1$
				else 
					scopeDescription= NLSSearchMessages.getFormattedString("EnclosingProjectScope", ""); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case ISearchPageContainer.WORKING_SET_SCOPE :
				IWorkingSet[] workingSets= getContainer().getSelectedWorkingSets();
				// should not happen - just to be sure
				if (workingSets == null || workingSets.length < 1)
					return false;
				scopeDescription= NLSSearchMessages.getFormattedString("WorkingSetScope", new String[] { SearchUtil.toString(workingSets)}); //$NON-NLS-1$
				scope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(getContainer().getSelectedWorkingSets());
				SearchUtil.updateLRUWorkingSets(getContainer().getSelectedWorkingSets());
		}

		NLSSearchResultCollector collector= new NLSSearchResultCollector(data.propertyFile);
		NLSSearchOperation op= new NLSSearchOperation(workspace, data.wrapperClass, REFERENCES, scope, scopeDescription, collector);
		Shell shell= getControl().getShell();
		try {
			getContainer().getRunnableContext().run(true, true, op);
		} catch (InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, NLSSearchMessages.getString("Search.Error.search.title"), NLSSearchMessages.getString("Search.Error.search.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return false;
		} catch (InterruptedException ex) {
			return false;
		}
		return true;
	}

	private String[] getPreviousSearchPatterns() {
		// Search results are not persistent
		int patternCount= fgPreviousSearchPatterns.size();
		String[] patterns= new String[patternCount];
		for (int i= 0; i < patternCount; i++)
			patterns[i]= ((SearchPatternData) fgPreviousSearchPatterns.get(patternCount - 1 - i)).wrapperClassName;
		return patterns;
	}

	private String getWrapperClassName() {
		return fWrapperClassCombo.getText();
	}
	/**
	 * Return search pattern data and update previous searches.
	 * An existing entry will be updated.
	 */
	private SearchPatternData getPatternData() {
		String pattern= getWrapperClassName();
		SearchPatternData match= null;
		int i= 0;
		int size= fgPreviousSearchPatterns.size();

		if (fWrapperClass == null) {
			SelectionDialog dialog= createWrapperClassSelectionDialog();
			if (dialog == null)
				fWrapperClass= null;
			else if (dialog.open() == IDialogConstants.OK_ID) {
				Object[] types= dialog.getResult();
				if (types != null && types.length > 0) {
					fWrapperClass= (IType) types[0];
					fWrapperClassCombo.setText(PrettySignature.getSignature(fWrapperClass));
					pattern= getWrapperClassName();
				}
			}
		}

		while (match == null && i < size) {
			match= (SearchPatternData) fgPreviousSearchPatterns.get(i);
			i++;
			if (!pattern.equals(match.wrapperClassName))
				match= null;
		}
		if (match == null) {
			match= new SearchPatternData(pattern, fWrapperClass, fPropertyFileText.getText());
			fgPreviousSearchPatterns.add(match);
		} else {
			match.wrapperClass= fWrapperClass;
			match.propertyFileName= fPropertyFileText.getText();
			match.scope= getContainer().getSelectedScope();
			match.workingSets= getContainer().getSelectedWorkingSets();
			match.propertyFile= null;
			if (match.propertyFileName != null) {
				IPath path= new Path(match.propertyFileName);
				if (path.segmentCount() >= 2)
					match.propertyFile= JavaPlugin.getWorkspace().getRoot().getFile(path);
			}
		}
		return match;
	}
	/*
	 * Implements method from IDialogPage
	 */
	public void setVisible(boolean visible) {
		if (visible && fWrapperClassCombo != null) {
			if (fFirstTime) {
				JavaPlugin.getDefault().getImageRegistry();
				fFirstTime= false;
				// Set item and text here to prevent page from resizing
				fWrapperClassCombo.setItems(getPreviousSearchPatterns());
				initSelections();
			}
			fWrapperClassCombo.setFocus();
			getContainer().setPerformActionEnabled(getWrapperClassName().length() > 0 && fPropertyFileText.getText().length() > 0);
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
		layout.numColumns= 2;
		layout.makeColumnsEqualWidth= true;
		layout.horizontalSpacing= 10;
		result.setLayout(layout);
		result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		RowLayouter layouter= new RowLayouter(layout.numColumns);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		layouter.setDefaultGridData(gd, 0);
		layouter.setDefaultGridData(gd, 1);
		layouter.setDefaultSpan();

		layouter.perform(createWrapperClassControl(result));
		layouter.perform(createPropertyFileControl(result));

		setControl(result);

		Dialog.applyDialogFont(result);
		WorkbenchHelp.setHelp(result, IJavaHelpContextIds.NLS_SEARCH_PAGE);	
	}
	/**
	 * Creates the control for the wrapper class
	 */
	private Control createWrapperClassControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		Label label= new Label(result, SWT.NORMAL);
		label.setText(NLSSearchMessages.getString("NLSSearchPage.wrapperClassGroup.text")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);
		
		// Wrapper class combo
		fWrapperClassCombo= new Combo(result, SWT.SINGLE | SWT.BORDER);
		fWrapperClassCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fWrapperClassCombo.getSelectionIndex() < 0)
					return;
				int index= fgPreviousSearchPatterns.size() - 1 - fWrapperClassCombo.getSelectionIndex();
				SearchPatternData values= (SearchPatternData) fgPreviousSearchPatterns.get(index);
				fWrapperClass= values.wrapperClass;
				fWrapperClassCombo.setText(values.wrapperClassName);
				fPropertyFileText.setText(values.propertyFileName);
				if (values.workingSets != null)
					getContainer().setSelectedWorkingSets(values.workingSets);
				else
					getContainer().setSelectedScope(values.scope);
			}
		});
		fWrapperClassCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fWrapperClass != null && !PrettySignature.getSignature(fWrapperClass).equals(fWrapperClassCombo.getText()))
					fWrapperClass= null;
				getContainer().setPerformActionEnabled(fWrapperClassCombo.getText().length() > 0 && fPropertyFileText.getText().length() > 0);
			}
		});
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(30);
		fWrapperClassCombo.setLayoutData(gd);

		// browse button
		Button browseButton= new Button(result, SWT.PUSH);
		browseButton.setText(NLSSearchMessages.getString("NLSSearchPage.wrapperClassBrowseButton.text")); //$NON-NLS-1$
		browseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(browseButton);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseWrapperClassButtonPressed();
			}
		});

		return result;
	}
	/**
	 * Creates the control for the property file
	 */
	private Control createPropertyFileControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		
		Label label= new Label(result, SWT.NORMAL);
		label.setText(NLSSearchMessages.getString("NLSSearchPage.propertyFileGroup.text")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);

		fPropertyFileText= new Text(result, SWT.SINGLE | SWT.BORDER);
		fPropertyFileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getContainer().setPerformActionEnabled(fWrapperClassCombo.getText().length() > 0 && fPropertyFileText.getText().length() > 0);
			}
		});
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(30);
		fPropertyFileText.setLayoutData(gd);

		// browse button
		Button browseButton= new Button(result, SWT.PUSH);
		browseButton.setText(NLSSearchMessages.getString("NLSSearchPage.propertyFileBrowseButton.text")); //$NON-NLS-1$
		browseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(browseButton);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowsePropertiesButtonPressed();
			}
		});

		return result;
	}

	private void initSelections() {
		fWrapperClass= null;
		ISelection selection= getSelection();
		SearchPatternData values= null;
		values= tryIfPropertyFileSelected(selection);
		if (values == null)
			values= tryTypedTextSelection(selection);
		if (values == null)
			values= trySelection(selection);
		if (values == null)
			values= trySimpleTextSelection(selection);
		if (values == null)
			values= getDefaultInitValues();
		fPropertyFileText.setText(values.propertyFileName);
		fWrapperClass= values.wrapperClass;
		if (fWrapperClass != null)
			fWrapperClassCombo.setText(PrettySignature.getSignature(fWrapperClass));
		else
			fWrapperClassCombo.setText(values.wrapperClassName); //$NON-NLS-1$
	}

	private SearchPatternData tryTypedTextSelection(ISelection selection) {
		if (selection instanceof ITextSelection) {
			IEditorPart e= getEditorPart();
			if (e != null) {
				ITextSelection ts= (ITextSelection) selection;
				ICodeAssist assist= getCodeAssist(e);
				if (assist != null) {
					IJavaElement[] elements= null;
					try {
						elements= assist.codeSelect(ts.getOffset(), ts.getLength());
					} catch (JavaModelException ex) {
						ExceptionHandler.handle(ex, NLSSearchMessages.getString("Search.Error.createJavaElement.title"), NLSSearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
					}
					if (elements != null && elements.length > 0) {
						if (elements.length == 1)
							fWrapperClass= elements[0];
						else
							fWrapperClass= chooseFromList(elements);
						if (fWrapperClass != null)
							return determineInitValuesFrom(fWrapperClass);
					}
				}
			}
		}
		return null;
	}

	private ICodeAssist getCodeAssist(IEditorPart editorPart) {
		IEditorInput input= editorPart.getEditorInput();
		if (input instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput) input).getClassFile();
		IWorkingCopyManager manager= JavaUI.getWorkingCopyManager();
		return manager.getWorkingCopy(input);
	}

	private SearchPatternData trySelection(ISelection selection) {
		SearchPatternData result= null;
		if (selection == null)
			return result;
		Object o= null;
		if (selection instanceof IStructuredSelection)
			o= ((IStructuredSelection) selection).getFirstElement();
		if (o instanceof IJavaElement) {
			fWrapperClass= (IJavaElement) o;
			result= determineInitValuesFrom(fWrapperClass);
		} else if (o instanceof ISearchResultViewEntry) {
			fWrapperClass= getJavaElement(((ISearchResultViewEntry) o).getSelectedMarker());
			result= determineInitValuesFrom(fWrapperClass);
		} else if (o instanceof IAdaptable) {
			IWorkbenchAdapter element= (IWorkbenchAdapter) ((IAdaptable) o).getAdapter(IWorkbenchAdapter.class);
			if (element != null)
				result= new SearchPatternData(element.getLabel(o), null, ""); //$NON-NLS-1$
		}
		return result;
	}

	private IJavaElement getJavaElement(IMarker marker) {
		try {
			return JavaCore.create((String) marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, NLSSearchMessages.getString("Search.Error.createJavaElement.title"), NLSSearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
	}

	private SearchPatternData determineInitValuesFrom(IJavaElement element) {
		if (element == null)
			return null;
		int searchFor= UNKNOWN;
		String pattern= null;
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT :
				searchFor= PACKAGE;
				pattern= element.getElementName();
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				searchFor= PACKAGE;
				pattern= element.getElementName();
				break;
			case IJavaElement.PACKAGE_DECLARATION :
				searchFor= PACKAGE;
				pattern= element.getElementName();
				break;
			case IJavaElement.IMPORT_DECLARATION :
				pattern= element.getElementName();
				IImportDeclaration declaration= (IImportDeclaration) element;
				if (declaration.isOnDemand()) {
					searchFor= PACKAGE;
					int index= pattern.lastIndexOf('.');
					pattern= pattern.substring(0, index);
				} else {
					searchFor= TYPE;
				}
				break;
			case IJavaElement.TYPE :
				searchFor= TYPE;
				pattern= JavaModelUtil.getFullyQualifiedName((IType) element);
				break;
			case IJavaElement.COMPILATION_UNIT :
				ICompilationUnit cu= (ICompilationUnit) element;
				String mainTypeName= element.getElementName().substring(0, element.getElementName().indexOf(".")); //$NON-NLS-1$
				IType mainType= cu.getType(mainTypeName);
				mainTypeName= JavaModelUtil.getTypeQualifiedName(mainType);
				try {
					mainType= JavaModelUtil.findTypeInCompilationUnit(cu, mainTypeName);
					if (mainType == null) {
						// fetch type which is declared first in the file
						IType[] types= cu.getTypes();
						if (types.length > 0)
							mainType= types[0];
						else
							break;
					}
				} catch (JavaModelException ex) {
					ExceptionHandler.handle(ex, NLSSearchMessages.getString("Search.Error.javaElementAccess.title"), NLSSearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
					break;
				}
				searchFor= TYPE;
				element= mainType;
				pattern= JavaModelUtil.getFullyQualifiedName(mainType);
				break;
			case IJavaElement.CLASS_FILE :
				IClassFile cf= (IClassFile) element;
				try {
					mainType= cf.getType();
				} catch (JavaModelException ex) {
					ExceptionHandler.handle(ex, NLSSearchMessages.getString("Search.Error.javaElementAccess.title"), NLSSearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
					break;
				}
				if (mainType == null)
					break;
				searchFor= TYPE;
				pattern= JavaModelUtil.getFullyQualifiedName(mainType);
				break;
			case IJavaElement.FIELD :
				searchFor= FIELD;
				IType type= ((IField) element).getDeclaringType();
				StringBuffer buffer= new StringBuffer();
				buffer.append(JavaModelUtil.getFullyQualifiedName(type));
				buffer.append('.');
				buffer.append(element.getElementName());
				pattern= buffer.toString();
				break;
			case IJavaElement.METHOD :
				searchFor= METHOD;
				try {
					IMethod method= (IMethod) element;
					if (method.isConstructor())
						searchFor= CONSTRUCTOR;
				} catch (JavaModelException ex) {
					ExceptionHandler.handle(ex, NLSSearchMessages.getString("Search.Error.javaElementAccess.title"), NLSSearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
					break;
				}
				pattern= PrettySignature.getMethodSignature((IMethod) element);
				break;
		}
		if (searchFor == TYPE && pattern != null) {
			String propertyFilePathStr= ""; //$NON-NLS-1$
			// make suggestion for properties file
			IPath path= element.getPath().removeFileExtension().addFileExtension("properties"); //$NON-NLS-1$
			propertyFilePathStr= path.toString();
			return new SearchPatternData(pattern, element, propertyFilePathStr); //$NON-NLS-1$
		}

		return null;
	}

	private SearchPatternData trySimpleTextSelection(ISelection selection) {
		SearchPatternData result= null;
		if (selection instanceof ITextSelection) {
			BufferedReader reader= new BufferedReader(new StringReader(((ITextSelection) selection).getText()));
			String text;
			try {
				text= reader.readLine();
				if (text == null)
					text= ""; //$NON-NLS-1$
			} catch (IOException ex) {
				text= ""; //$NON-NLS-1$
			}
			result= new SearchPatternData(text, null, ""); //$NON-NLS-1$
		}
		return result;
	}

	private SearchPatternData tryIfPropertyFileSelected(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object o= ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof IFile && ((IFile) o).getFileExtension().equalsIgnoreCase("properties")) { //$NON-NLS-1$
				IPath propertyFullPath= ((IFile)o).getFullPath();
				String typePathStr= null;

				// try to be smarter and find a corresponding CU
				IPath cuPath= propertyFullPath.removeFileExtension().addFileExtension("java"); //$NON-NLS-1$
				IFile cuFile= (IFile)JavaPlugin.getWorkspace().getRoot().findMember(cuPath);
				IType type= null;
				if (cuFile != null && cuFile.exists()) {
					IJavaElement  cu= JavaCore.create(cuFile);
					if (cu != null && cu.exists() && cu.getElementType() == IJavaElement.COMPILATION_UNIT)
						type= ((ICompilationUnit)cu).findPrimaryType();
						if (type != null)
							typePathStr= JavaModelUtil.getFullyQualifiedName(type);
						else {
							IPath propertyFile= propertyFullPath.removeFirstSegments(propertyFullPath.segmentCount() - 1);
							typePathStr= propertyFile.removeFileExtension().toString();
						}
				}
				return new SearchPatternData(typePathStr, type, propertyFullPath.toString());
			}
		}
		return null;
	}

	private SearchPatternData getDefaultInitValues() {
		return new SearchPatternData("", null, ""); //$NON-NLS-2$ //$NON-NLS-1$
	}

	private IJavaElement chooseFromList(IJavaElement[] openChoices) {
		int flags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_QUALIFIED;
		ILabelProvider labelProvider= new JavaElementLabelProvider(flags);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setIgnoreCase(true);
		dialog.setMultipleSelection(false);
		dialog.setTitle(NLSSearchMessages.getString("SearchElementSelectionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NLSSearchMessages.getString("SearchElementSelectionDialog.message")); //$NON-NLS-1$
		dialog.setElements(openChoices);
		if (dialog.open() == Window.OK)
			return (IJavaElement)dialog.getFirstResult();
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

	protected void handleBrowseWrapperClassButtonPressed() {
		SelectionDialog dialog= createWrapperClassSelectionDialog();
		if (dialog == null || dialog.open() == IDialogConstants.CANCEL_ID)
			return;

		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			fWrapperClass= (IType) types[0];
			fWrapperClassCombo.setText(PrettySignature.getSignature(fWrapperClass));
		}
	}

	protected void handleBrowsePropertiesButtonPressed() {
		ElementTreeSelectionDialog dialog= createWorkspaceFileSelectionDialog(NLSSearchMessages.getString("NLSSearchPage.propertiesFileSelectionDialog.title"), NLSSearchMessages.getString("NLSSearchPage.propertiesFileSelectionDialog.message")); //$NON-NLS-2$ //$NON-NLS-1$
		dialog.setSorter(new JavaElementSorter());
		dialog.setInitialSelections(new String[] { fPropertyFileText.getText()});
		if (dialog.open() == Window.OK) {
			Object[] resources= dialog.getResult();
			if (resources.length == 1)
				fPropertyFileText.setText(((IResource) resources[0]).getFullPath().toString());
		}
	}
	/**
	 * Creates and returns a dialog to choose an existing workspace file.
	 */
	protected ElementTreeSelectionDialog createWorkspaceFileSelectionDialog(String title, String message) {
		int labelFlags= JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS | JavaElementLabelProvider.SHOW_SMALL_ICONS;
		ITreeContentProvider contentProvider= new StandardJavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(labelFlags);
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), labelProvider, contentProvider);
		dialog.setAllowMultiple(false);
		dialog.setDoubleClickSelects(true);
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				// only single selection
				if (selection.length == 1 && (selection[0] instanceof IFile) && (((IFile) selection[0]).getFileExtension().equalsIgnoreCase("properties"))) //$NON-NLS-1$
					return new StatusInfo();
				else
					return new StatusInfo(IStatus.ERROR, "");//$NON-NLS-1$
			}
		});
		dialog.addFilter(new EmptyInnerPackageFilter());
		dialog.addFilter(new LibraryFilter());
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setStatusLineAboveButtons(true);
		dialog.setInput(JavaCore.create(JavaPlugin.getWorkspace().getRoot()));
		return dialog;
	}
	/**
	 * Creates and returns a dialog to choose an existing type.
	 */
	protected SelectionDialog createWrapperClassSelectionDialog() {
		Shell shell= getControl().getShell();
		SelectionDialog dialog= null;
		try {
			String filter= getWrapperClassName();
			int lastDot= filter.lastIndexOf('.');
			if (lastDot > -1 && lastDot != filter.length() - 1)
				filter= filter.substring(lastDot + 1);
			dialog= JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell), SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES, false, filter);
			if (fWrapperClass != null)
				dialog.setInitialSelections(new Object[] {fWrapperClass});
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, NLSSearchMessages.getString("NLSSearchPage.Error.createTypeDialog.title"), NLSSearchMessages.getString("NLSSearchPage.Error.createTypeDialog.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}

		dialog.setTitle(NLSSearchMessages.getString("NLSSearchPage.wrapperClassDialog.title")); //$NON-NLS-1$
		dialog.setInitialSelections(new Object[] { getUnqualifiedType(getWrapperClassName())});
		dialog.setMessage(NLSSearchMessages.getString("NLSSearchPage.wrapperClassDialog.message")); //$NON-NLS-1$
		return dialog;
	}	

	public static String getUnqualifiedType(String typeName) {
		if (typeName == null)
			return null;
		int lastDotIndex= typeName.lastIndexOf('.');
		if (lastDotIndex < 0)
			return typeName;
		if (lastDotIndex > typeName.length() - 1)
			return ""; //$NON-NLS-1$
		return typeName.substring(lastDotIndex + 1);
	}
}
