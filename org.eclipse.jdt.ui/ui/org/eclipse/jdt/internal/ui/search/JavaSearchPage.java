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
package org.eclipse.jdt.internal.ui.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.preferences.PreferencePageSupport;
import org.eclipse.jdt.internal.ui.preferences.SearchParticipantsPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.RowLayouter;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.NewSearchUI;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class JavaSearchPage extends DialogPage implements ISearchPage, IJavaSearchConstants {
	
	private static class SearchPatternData {
		private int			searchFor;
		private int			limitTo;
		private String			pattern;
		private boolean		isCaseSensitive;
		private IJavaElement	javaElement;
		private int			scope;
		private IWorkingSet[]	 	workingSets;
		
		public SearchPatternData(int searchFor, int limitTo, boolean isCaseSensitive, String pattern, IJavaElement element) {
			this(searchFor, limitTo, pattern, isCaseSensitive, element, ISearchPageContainer.WORKSPACE_SCOPE, null);
		}
		
		public SearchPatternData(int s, int l, String p, boolean i, IJavaElement element, int scope, IWorkingSet[] workingSets) {
			setSearchFor(s);
			setLimitTo(l);
			setPattern(p);
			setCaseSensitive(i);
			setJavaElement(element);
			this.setScope(scope);
			this.setWorkingSets(workingSets);
		}

		public void setCaseSensitive(boolean isCaseSensitive) {
			this.isCaseSensitive= isCaseSensitive;
		}

		public boolean isCaseSensitive() {
			return isCaseSensitive;
		}

		public void setJavaElement(IJavaElement javaElement) {
			this.javaElement= javaElement;
		}

		public IJavaElement getJavaElement() {
			return javaElement;
		}

		public void setLimitTo(int limitTo) {
			this.limitTo= limitTo;
		}

		public int getLimitTo() {
			return limitTo;
		}

		public void setPattern(String pattern) {
			this.pattern= pattern;
		}

		public String getPattern() {
			return pattern;
		}

		public void setScope(int scope) {
			this.scope= scope;
		}

		public int getScope() {
			return scope;
		}

		public void setSearchFor(int searchFor) {
			this.searchFor= searchFor;
		}

		public int getSearchFor() {
			return searchFor;
		}

		public void setWorkingSets(IWorkingSet[] workingSets) {
			this.workingSets= workingSets;
		}

		public IWorkingSet[] getWorkingSets() {
			return workingSets;
		}
	}
	
	public static final String PARTICIPANT_EXTENSION_POINT= "org.eclipse.jdt.ui.queryParticipants"; //$NON-NLS-1$

	public static final String EXTENSION_POINT_ID= "org.eclipse.jdt.ui.JavaSearchPage"; //$NON-NLS-1$

	// Dialog store id constants
	private final static String PAGE_NAME= "JavaSearchPage"; //$NON-NLS-1$
	private final static String STORE_CASE_SENSITIVE= PAGE_NAME + "CASE_SENSITIVE"; //$NON-NLS-1$
	private static List fgPreviousSearchPatterns= new ArrayList(20);
	
	private SearchPatternData fInitialData;
	private IStructuredSelection fStructuredSelection;
	private IJavaElement fJavaElement;
	private boolean fFirstTime= true;
	private IDialogSettings fDialogSettings;
	private boolean fIsCaseSensitive;
	
	private Combo fPattern;
	private ISearchPageContainer fContainer;
	private Button fCaseSensitive;
	
	private Button[] fSearchFor;
	private String[] fSearchForText= {
		SearchMessages.getString("SearchPage.searchFor.type"), //$NON-NLS-1$
		SearchMessages.getString("SearchPage.searchFor.method"), //$NON-NLS-1$
		SearchMessages.getString("SearchPage.searchFor.package"), //$NON-NLS-1$
		SearchMessages.getString("SearchPage.searchFor.constructor"), //$NON-NLS-1$
		SearchMessages.getString("SearchPage.searchFor.field")}; //$NON-NLS-1$

	private Button[] fLimitTo;
	private String[] fLimitToText= {
		SearchMessages.getString("SearchPage.limitTo.declarations"), //$NON-NLS-1$
		SearchMessages.getString("SearchPage.limitTo.implementors"), //$NON-NLS-1$
		SearchMessages.getString("SearchPage.limitTo.references"), //$NON-NLS-1$
		SearchMessages.getString("SearchPage.limitTo.allOccurrences"), //$NON-NLS-1$
		SearchMessages.getString("SearchPage.limitTo.readReferences"), //$NON-NLS-1$		
		SearchMessages.getString("SearchPage.limitTo.writeReferences")}; //$NON-NLS-1$


	//---- Action Handling ------------------------------------------------
	
	public boolean performAction() {
		if (true)
			return performNewSearch();
		if (JavaPlugin.getDefault().getPreferenceStore().getBoolean(WorkInProgressPreferencePage.PREF_BGSEARCH)) {
			return performNewSearch();
		} else {
			return performOldSearch();
		}
	}
	
	private boolean performOldSearch() {
		org.eclipse.search.ui.SearchUI.activateSearchResultView();

		SearchPatternData data= getPatternData();
		IWorkspace workspace= JavaPlugin.getWorkspace();

		// Setup search scope
		IJavaSearchScope scope= null;
		String scopeDescription= ""; //$NON-NLS-1$
		switch (getContainer().getSelectedScope()) {
			case ISearchPageContainer.WORKSPACE_SCOPE:
				scopeDescription= SearchMessages.getString("WorkspaceScope"); //$NON-NLS-1$
				scope= SearchEngine.createWorkspaceScope();
				break;
			case ISearchPageContainer.SELECTION_SCOPE:
				scopeDescription= SearchMessages.getString("SelectionScope"); //$NON-NLS-1$
				scope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(fStructuredSelection);
				break;
			case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
				scope= JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(fStructuredSelection);
				IProject[] projects= JavaSearchScopeFactory.getInstance().getJavaProjects(scope);
				if (projects.length > 1)
					scopeDescription= SearchMessages.getFormattedString("EnclosingProjectsScope", projects[0].getName()); //$NON-NLS-1$
				else if (projects.length == 1)
					scopeDescription= SearchMessages.getFormattedString("EnclosingProjectScope", projects[0].getName()); //$NON-NLS-1$
				else 
					scopeDescription= SearchMessages.getFormattedString("EnclosingProjectScope", ""); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case ISearchPageContainer.WORKING_SET_SCOPE:
				IWorkingSet[] workingSets= getContainer().getSelectedWorkingSets();
				// should not happen - just to be sure
				if (workingSets == null || workingSets.length < 1)
					return false;
				scopeDescription= SearchMessages.getFormattedString("WorkingSetScope", SearchUtil.toString(workingSets)); //$NON-NLS-1$
				scope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(getContainer().getSelectedWorkingSets());
				SearchUtil.updateLRUWorkingSets(getContainer().getSelectedWorkingSets());
		}
		
		JavaSearchResultCollector collector= new JavaSearchResultCollector();
		JavaSearchOperation op= null;
		if (data.getJavaElement() != null && getPattern().equals(fInitialData.getPattern())) {
			op= new JavaSearchOperation(workspace, data.getJavaElement(), data.getLimitTo(), scope, scopeDescription, collector);
			if (data.getLimitTo() == IJavaSearchConstants.REFERENCES)
				SearchUtil.warnIfBinaryConstant(data.getJavaElement(), getShell());
		} else {
			data.setJavaElement(null);
			op= new JavaSearchOperation(workspace, data.getPattern(), data.isCaseSensitive(), data.getSearchFor(), data.getLimitTo(), scope, scopeDescription, collector);
		}
		Shell shell= getControl().getShell();
		try {
			getContainer().getRunnableContext().run(true, true, op);
		} catch (InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, SearchMessages.getString("Search.Error.search.title"), SearchMessages.getString("Search.Error.search.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return false;
		} catch (InterruptedException ex) {
			return false;
		}
		return true;
	}
	
	private boolean performNewSearch() {
		org.eclipse.search.ui.NewSearchUI.activateSearchResultView();

		SearchPatternData data= getPatternData();

		// Setup search scope
		IJavaSearchScope scope= null;
		String scopeDescription= ""; //$NON-NLS-1$
		HashSet concernedProjects= new HashSet();
		
		switch (getContainer().getSelectedScope()) {
			case ISearchPageContainer.WORKSPACE_SCOPE:
				collectConcernedProjects(concernedProjects, JavaPlugin.getWorkspace());
				scopeDescription= SearchMessages.getString("WorkspaceScope"); //$NON-NLS-1$
				scope= SearchEngine.createWorkspaceScope();
				break;
			case ISearchPageContainer.SELECTION_SCOPE:
				collectConcernedProjects(concernedProjects, fStructuredSelection);
				scopeDescription= SearchMessages.getString("SelectionScope"); //$NON-NLS-1$
				scope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(fStructuredSelection);
				break;
			case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
				collectConcernedProjects(concernedProjects, fStructuredSelection);
				scope= JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(fStructuredSelection);
				if (concernedProjects.size() >= 1) {
					IProject firstProject= (IProject) concernedProjects.iterator().next();
					if (concernedProjects.size() == 1)
						scopeDescription= SearchMessages.getFormattedString("EnclosingProjectScope", firstProject.getName()); //$NON-NLS-1$
					else
						scopeDescription= SearchMessages.getFormattedString("EnclosingProjectsScope", firstProject.getName()); //$NON-NLS-1$
				} else 
					scopeDescription= SearchMessages.getFormattedString("EnclosingProjectScope", ""); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case ISearchPageContainer.WORKING_SET_SCOPE:
				IWorkingSet[] workingSets= getContainer().getSelectedWorkingSets();
				// should not happen - just to be sure
				if (workingSets == null || workingSets.length < 1)
					return false;
				collectConcernedProjects(concernedProjects, getContainer().getSelectedWorkingSets());
				scopeDescription= SearchMessages.getFormattedString("WorkingSetScope", SearchUtil.toString(workingSets)); //$NON-NLS-1$
				scope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(getContainer().getSelectedWorkingSets());
				SearchUtil.updateLRUWorkingSets(getContainer().getSelectedWorkingSets());
		
		}
		
		JavaSearchQuery textSearchJob= null;
		QuerySpecification querySpec= null;
		if (data.getJavaElement() != null && getPattern().equals(fInitialData.getPattern())) {
			if (data.getLimitTo() == IJavaSearchConstants.REFERENCES)
				SearchUtil.warnIfBinaryConstant(data.getJavaElement(), getShell());
			querySpec= new ElementQuerySpecification(data.getJavaElement(), data.getLimitTo(), scope, scopeDescription);
		} else {
			querySpec= new PatternQuerySpecification(data.getPattern(), data.getSearchFor(), data.isCaseSensitive(), data.getLimitTo(), scope, scopeDescription);
			data.setJavaElement(null);
		} 
		
		IQueryParticipant[] participants= new IQueryParticipant[0];
		try {
			participants= getSearchParticipants(concernedProjects);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), SearchMessages.getString("Search.Error.search.title"), SearchMessages.getString("Search.Error.search.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return false;
		}
		textSearchJob= new JavaSearchQuery(querySpec, participants);
		new SearchResultUpdater((JavaSearchResult) textSearchJob.getSearchResult());
		NewSearchUI.runQuery(textSearchJob);	
		return true;
	}

	private IQueryParticipant[] getSearchParticipants(HashSet concernedProjects) throws CoreException {
		Map participantMap= new HashMap();
		collectParticipants(participantMap, concernedProjects);
		IQueryParticipant[] participants= new IQueryParticipant[participantMap.size()];
		return (IQueryParticipant[]) participantMap.values().toArray(participants);
	}	private int getLimitTo() {
		for (int i= 0; i < fLimitTo.length; i++) {
			if (fLimitTo[i].getSelection())
				return i;
		}
		return -1;
	}

	private void setLimitTo(int searchFor) {
		fLimitTo[DECLARATIONS].setEnabled(true);
		fLimitTo[IMPLEMENTORS].setEnabled(false);
		fLimitTo[REFERENCES].setEnabled(true);			
		fLimitTo[ALL_OCCURRENCES].setEnabled(true);
		fLimitTo[READ_ACCESSES].setEnabled(false);
		fLimitTo[WRITE_ACCESSES].setEnabled(false);
		
		if (!(searchFor == TYPE || searchFor == INTERFACE) && fLimitTo[IMPLEMENTORS].getSelection()) {
			fLimitTo[IMPLEMENTORS].setSelection(false);
			fLimitTo[REFERENCES].setSelection(true);
		}

		if (!(searchFor == FIELD) && (getLimitTo() == READ_ACCESSES || getLimitTo() == WRITE_ACCESSES)) {
			fLimitTo[getLimitTo()].setSelection(false);
			fLimitTo[REFERENCES].setSelection(true);
		}

		switch (searchFor) {
			case TYPE:
			case INTERFACE:
				fLimitTo[IMPLEMENTORS].setEnabled(true);
				break;
			case FIELD:
				fLimitTo[READ_ACCESSES].setEnabled(true);
				fLimitTo[WRITE_ACCESSES].setEnabled(true);
				break;
			default :
				break;
		}
	}

	private String[] getPreviousSearchPatterns() {
		// Search results are not persistent
		int patternCount= fgPreviousSearchPatterns.size();
		String [] patterns= new String[patternCount];
		for (int i= 0; i < patternCount; i++)
			patterns[i]= ((SearchPatternData) fgPreviousSearchPatterns.get(patternCount - 1 - i)).getPattern();
		return patterns;
	}
	
	private int getSearchFor() {
		for (int i= 0; i < fSearchFor.length; i++) {
			if (fSearchFor[i].getSelection())
				return i;
		}
		Assert.isTrue(false, "shouldNeverHappen"); //$NON-NLS-1$
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
			if (!pattern.equals(match.getPattern()))
				match= null;
		}
		if (match == null) {
			match= new SearchPatternData(
							getSearchFor(),
							getLimitTo(),
							pattern,
							fCaseSensitive.getSelection(),
							fJavaElement,
							getContainer().getSelectedScope(),
							getContainer().getSelectedWorkingSets());
			fgPreviousSearchPatterns.add(match);
		}
		else {
			match.setSearchFor(getSearchFor());
			match.setLimitTo(getLimitTo());
			match.setCaseSensitive(fCaseSensitive.getSelection());
			match.setJavaElement(fJavaElement);
			match.setScope(getContainer().getSelectedScope());
			match.setWorkingSets(getContainer().getSelectedWorkingSets());
		}
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
		initializeDialogUnits(parent);
		readConfiguration();
		
		GridData gd;
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(2, false);
		layout.horizontalSpacing= 10;
		result.setLayout(layout);
		result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		RowLayouter layouter= new RowLayouter(layout.numColumns);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.VERTICAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_FILL;
	
		layouter.setDefaultGridData(gd, 0);
		layouter.setDefaultGridData(gd, 1);
		layouter.setDefaultSpan();

		layouter.perform(createExpression(result));
		layouter.perform(createSearchFor(result), createLimitTo(result), -1);

		createParticipants(result);
		
		SelectionAdapter javaElementInitializer= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (getSearchFor() == fInitialData.getSearchFor())
					fJavaElement= fInitialData.getJavaElement();
				else
					fJavaElement= null;
				setLimitTo(getSearchFor());
				updateCaseSensitiveCheckbox();
			}
		};

		fSearchFor[TYPE].addSelectionListener(javaElementInitializer);
		fSearchFor[METHOD].addSelectionListener(javaElementInitializer);
		fSearchFor[FIELD].addSelectionListener(javaElementInitializer);
		fSearchFor[CONSTRUCTOR].addSelectionListener(javaElementInitializer);
		fSearchFor[PACKAGE].addSelectionListener(javaElementInitializer);

		setControl(result);

		Dialog.applyDialogFont(result);
		WorkbenchHelp.setHelp(result, IJavaHelpContextIds.JAVA_SEARCH_PAGE);	
		initSelections();
	}

	private void createParticipants(Composite result) {
		Button selectParticipants= new Button(result, SWT.PUSH);
		selectParticipants.setText(SearchMessages.getString("SearchPage.select_participants.label")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.verticalAlignment= GridData.VERTICAL_ALIGN_BEGINNING;
		gd.horizontalAlignment= GridData.HORIZONTAL_ALIGN_END;
		gd.grabExcessHorizontalSpace= false;
		gd.horizontalAlignment= GridData.END;
		gd.horizontalSpan= 2;
		selectParticipants.setLayoutData(gd);
		selectParticipants.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencePageSupport.showPreferencePage(getShell(), "org.eclipse.jdt.ui.preferences.SearchParticipantsPreferencePage", new SearchParticipantsPreferencePage()); //$NON-NLS-1$
			}

		});
	}


	private Control createExpression(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(2, false);
		result.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan= 2;
		gd.horizontalIndent= 0;
		result.setLayoutData(gd);

		// Pattern text + info
		Label label= new Label(result, SWT.LEFT);
		label.setText(SearchMessages.getString("SearchPage.expression.label")); //$NON-NLS-1$
		gd= new GridData(GridData.BEGINNING);
		gd.horizontalSpan= 2;
//		gd.horizontalIndent= -gd.horizontalIndent;
		label.setLayoutData(gd);

		// Pattern combo
		fPattern= new Combo(result, SWT.SINGLE | SWT.BORDER);
		fPattern.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handlePatternSelected();
			}
		});
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getContainer().setPerformActionEnabled(getPattern().length() > 0);
				updateCaseSensitiveCheckbox();
			}
		});
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		// limit preferred size 
		gd.widthHint= convertWidthInCharsToPixels(50);
		gd.horizontalIndent= -gd.horizontalIndent;
		fPattern.setLayoutData(gd);


		// Ignore case checkbox		
		fCaseSensitive= new Button(result, SWT.CHECK);
		fCaseSensitive.setText(SearchMessages.getString("SearchPage.expression.caseSensitive")); //$NON-NLS-1$
		gd= new GridData();
		fCaseSensitive.setLayoutData(gd);
		fCaseSensitive.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fIsCaseSensitive= fCaseSensitive.getSelection();
				writeConfiguration();
			}
		});
		
		return result;
	}

	private void updateCaseSensitiveCheckbox() {
		if (fInitialData != null && getPattern().equals(fInitialData.getPattern()) && fJavaElement != null) {
			fCaseSensitive.setEnabled(false);
			fCaseSensitive.setSelection(true);
		}
		else {
			fCaseSensitive.setEnabled(true);
			fCaseSensitive.setSelection(fIsCaseSensitive);
		}
	}

	private void handlePatternSelected() {
		if (fPattern.getSelectionIndex() < 0)
			return;
		int index= fgPreviousSearchPatterns.size() - 1 - fPattern.getSelectionIndex();
		fInitialData= (SearchPatternData) fgPreviousSearchPatterns.get(index);
		for (int i= 0; i < fSearchFor.length; i++)
			fSearchFor[i].setSelection(false);
		for (int i= 0; i < fLimitTo.length; i++)
			fLimitTo[i].setSelection(false);
		fSearchFor[fInitialData.getSearchFor()].setSelection(true);
		setLimitTo(fInitialData.getSearchFor());
		fLimitTo[fInitialData.getLimitTo()].setSelection(true);

		fPattern.setText(fInitialData.getPattern());
		fIsCaseSensitive= fInitialData.isCaseSensitive();
		fJavaElement= fInitialData.getJavaElement();
		fCaseSensitive.setEnabled(fJavaElement == null);
		fCaseSensitive.setSelection(fInitialData.isCaseSensitive());

		if (fInitialData.getWorkingSets() != null)
			getContainer().setSelectedWorkingSets(fInitialData.getWorkingSets());
		else
			getContainer().setSelectedScope(fInitialData.getScope());
	}

	private Control createSearchFor(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText(SearchMessages.getString("SearchPage.searchFor.label")); //$NON-NLS-1$
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		result.setLayout(layout);

		fSearchFor= new Button[fSearchForText.length];
		for (int i= 0; i < fSearchForText.length; i++) {
			Button button= new Button(result, SWT.RADIO);
			button.setText(fSearchForText[i]);
			fSearchFor[i]= button;
		}

		// Fill with dummy radio buttons
		Button filler= new Button(result, SWT.RADIO);
		filler.setVisible(false);
		filler= new Button(result, SWT.RADIO);
		filler.setVisible(false);

		return result;		
	}
	
	private Control createLimitTo(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText(SearchMessages.getString("SearchPage.limitTo.label")); //$NON-NLS-1$
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
		fStructuredSelection= asStructuredSelection();
		fInitialData= tryStructuredSelection(fStructuredSelection);
		if (fInitialData == null)
			fInitialData= trySimpleTextSelection(getContainer().getSelection());
		if (fInitialData == null)
			fInitialData= getDefaultInitValues();

		fJavaElement= fInitialData.getJavaElement();
		fCaseSensitive.setSelection(fInitialData.isCaseSensitive());
		fCaseSensitive.setEnabled(fInitialData.getJavaElement() == null);
		fSearchFor[fInitialData.getSearchFor()].setSelection(true);
		setLimitTo(fInitialData.getSearchFor());
		fLimitTo[fInitialData.getLimitTo()].setSelection(true);		
		fPattern.setText(fInitialData.getPattern());
	}

	private SearchPatternData tryStructuredSelection(IStructuredSelection selection) {
		if (selection == null || selection.size() > 1)
			return null;

		Object o= selection.getFirstElement();
		if (o instanceof IJavaElement) {
			return determineInitValuesFrom((IJavaElement)o);
		} else if (o instanceof ISearchResultViewEntry) {
			IJavaElement element= SearchUtil.getJavaElement(((ISearchResultViewEntry)o).getSelectedMarker());
			return determineInitValuesFrom(element);
		} else if (o instanceof LogicalPackage) {
			LogicalPackage lp= (LogicalPackage)o;
			return new SearchPatternData(PACKAGE, REFERENCES, fIsCaseSensitive, lp.getElementName(), null);
		} else if (o instanceof IAdaptable) {
			IJavaElement element= (IJavaElement)((IAdaptable)o).getAdapter(IJavaElement.class);
			if (element != null) {
				return determineInitValuesFrom(element);
			} else {
				IWorkbenchAdapter adapter= (IWorkbenchAdapter)((IAdaptable)o).getAdapter(IWorkbenchAdapter.class);
				if (adapter != null)
					return new SearchPatternData(TYPE, REFERENCES, fIsCaseSensitive, adapter.getLabel(o), null);
			}
		}
		return null;
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
				pattern= JavaModelUtil.getFullyQualifiedName((IType)element);
				break;
			case IJavaElement.COMPILATION_UNIT:
				ICompilationUnit cu= (ICompilationUnit)element;
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
					ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.javaElementAccess.title"), SearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
					break;
				}
				searchFor= TYPE;
				element= mainType;
				limitTo= REFERENCES;
				pattern= JavaModelUtil.getFullyQualifiedName(mainType);
				break;
			case IJavaElement.CLASS_FILE:
				IClassFile cf= (IClassFile)element;
				try {					
					mainType= cf.getType();
				} catch (JavaModelException ex) {
					ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.javaElementAccess.title"), SearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
					break;
				}
				if (mainType == null)
					break;
				element= mainType;
				searchFor= TYPE;
				limitTo= REFERENCES;
				pattern= JavaModelUtil.getFullyQualifiedName(mainType);
				break;
			case IJavaElement.FIELD:
				searchFor= FIELD;
				limitTo= REFERENCES;
				IType type= ((IField)element).getDeclaringType();
				StringBuffer buffer= new StringBuffer();
				buffer.append(JavaModelUtil.getFullyQualifiedName(type));
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
					ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.javaElementAccess.title"), SearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
					break;
				}		
				limitTo= REFERENCES;
				pattern= PrettySignature.getMethodSignature((IMethod)element);
				break;
		}
		if (searchFor != UNKNOWN && limitTo != UNKNOWN && pattern != null)
			return new SearchPatternData(searchFor, limitTo, true, pattern, element);
			
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
					text= ""; //$NON-NLS-1$
			} catch (IOException ex) {
				text= ""; //$NON-NLS-1$
			}
			result= new SearchPatternData(TYPE, REFERENCES, fIsCaseSensitive, text, null);
			}
		return result;
	}
	
	private SearchPatternData getDefaultInitValues() {
		return new SearchPatternData(TYPE, REFERENCES, fIsCaseSensitive, "", null); //$NON-NLS-1$
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
	 * Returns the structured selection from the selection.
	 */
	private IStructuredSelection asStructuredSelection() {
		IWorkbenchWindow wbWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (wbWindow != null) {
			IWorkbenchPage page= wbWindow.getActivePage();
			if (page != null) {
				IWorkbenchPart part= page.getActivePart();
				if (part != null)
					try {
						return SelectionConverter.getStructuredSelection(part);
					} catch (JavaModelException ex) {
						// ignore handled by return
					}
			}
		}
		return StructuredSelection.EMPTY;
	}
	
	//--------------- Configuration handling --------------
	
	/**
	 * Returns the page settings for this Java search page.
	 * 
	 * @return the page settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		fDialogSettings= settings.getSection(PAGE_NAME);
		if (fDialogSettings == null)
			fDialogSettings= settings.addNewSection(PAGE_NAME);
		return fDialogSettings;
	}
	
	/**
	 * Initializes itself from the stored page settings.
	 */
	private void readConfiguration() {
		IDialogSettings s= getDialogSettings();
		fIsCaseSensitive= s.getBoolean(STORE_CASE_SENSITIVE);
	}
	
	private void collectConcernedProjects(Set projects, ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return;
		IStructuredSelection structuredSelection= (IStructuredSelection)selection;
		Iterator elements= structuredSelection.iterator();
		while (elements.hasNext()) {
			IProject project= getProject(elements.next());
			if (project != null)
				projects.add(project);
		}
	}	

	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeConfiguration() {
		IDialogSettings s= getDialogSettings();
		s.put(STORE_CASE_SENSITIVE, fIsCaseSensitive);
	}
	


	private void collectConcernedProjects(Set projects, IWorkingSet[] workingSets) {
		for (int i= 0; i < workingSets.length; i++)
			collectConcernedProjects(projects, workingSets[i]);
	}
	private void collectConcernedProjects(Set projects, IWorkingSet workingSet) {
		IAdaptable[] adaptables= workingSet.getElements();
		for (int i= 0; i < adaptables.length; i++) {
			IProject project= getProject(adaptables[i]);
			if (project != null)
				projects.add(project);
		}
	}

	private void collectConcernedProjects(Set projects, IWorkspace workspace) {
		IProject[] allProjects= workspace.getRoot().getProjects();
		for (int i= 0; i < allProjects.length; i++) {
			if (allProjects[i].isAccessible())
				projects.add(allProjects[i]);
		}
	}

	private IProject getProject(Object element) {
		if (element instanceof ISearchResultViewEntry)
			element= ((ISearchResultViewEntry)element).getGroupByKey();
		if (element instanceof IAdaptable) {
			IAdaptable adaptable= (IAdaptable)element;
			IProject project= (IProject) adaptable.getAdapter(IProject.class);
			if (project != null)
				return project;
			IResource resource= (IResource) adaptable.getAdapter(IResource.class);
			if (resource != null)
				project= resource.getProject();
			if (project != null)
				return project;
		}
		return null;
	}
	
	private void collectParticipants(Map participants, Set projects) throws CoreException {
		Iterator activeParticipants= SearchParticipantsPreferencePage.readActiveParticipants().values().iterator();
		while (activeParticipants.hasNext()) {
			IConfigurationElement participant= (IConfigurationElement) activeParticipants.next();
			String id= participant.getAttribute("id"); //$NON-NLS-1$
			Iterator projectElemnents= projects.iterator();
			while (projectElemnents.hasNext()) {
				IProject project= (IProject) projectElemnents.next();
				if (participants.containsKey(id))
					break;
				if (project.hasNature(participant.getAttribute("nature"))) //$NON-NLS-1$
					participants.put(id, participant.createExecutableExtension("class")); //$NON-NLS-1$
			}
		}
	}

}
