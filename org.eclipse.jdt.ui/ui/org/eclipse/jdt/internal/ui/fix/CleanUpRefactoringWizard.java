/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CleanUpRefactoringWizard extends RefactoringWizard {
	
	private static final String CLEAN_UP_WIZARD_SETTINGS_SECTION_ID= "CleanUpWizard"; //$NON-NLS-1$
	
	private class SelectCUPage extends UserInputWizardPage {

		private ContainerCheckedTreeViewer fTreeViewer;

		public SelectCUPage(String name) {
			super(name);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			
			createViewer(composite);
			setControl(composite);
			
			Dialog.applyDialogFont(composite);
		}
		
		private TreeViewer createViewer(Composite parent) {
			fTreeViewer= new ContainerCheckedTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint= convertWidthInCharsToPixels(40);
			gd.heightHint= convertHeightInCharsToPixels(15);
			fTreeViewer.getTree().setLayoutData(gd);
			fTreeViewer.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS));
			fTreeViewer.setContentProvider(new StandardJavaElementContentProvider());
			fTreeViewer.setSorter(new JavaElementSorter());
			fTreeViewer.addFilter(new ViewerFilter() {

				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof IJavaElement) {
						IJavaElement jElement= (IJavaElement)element;
						return !jElement.isReadOnly();
					} else {
						return false;
					}
				}
				
			});
			IJavaModel create= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
			fTreeViewer.setInput(create);
			checkElements(fTreeViewer, (CleanUpRefactoring)getRefactoring());
			return fTreeViewer;
		}
		
		private void checkElements(CheckboxTreeViewer treeViewer, CleanUpRefactoring refactoring) {
			ICompilationUnit[] compilationUnits= refactoring.getCompilationUnits();
			for (int i= 0; i < compilationUnits.length; i++) {
				ICompilationUnit compilationUnit= compilationUnits[i];
				treeViewer.expandToLevel(compilationUnit, 0);
				treeViewer.setChecked(compilationUnit, true);
			}
			if (compilationUnits.length > 0)
				treeViewer.setSelection(new StructuredSelection(smallestCommonParents(compilationUnits)), true);
		}
		
		private IJavaElement[] smallestCommonParents(IJavaElement[] elements) {
			if (elements.length == 1) {
				return elements;
			} else {
				List parents= new ArrayList();
				boolean hasParents= false;
				
				IJavaElement parent= getParent(elements[0]);
				if (parent == null) {
					parent= elements[0];
				} else {
					hasParents= true;
				}
				parents.add(parent);
				
				for (int i= 1; i < elements.length; i++) {
					parent= getParent(elements[i]);
					if (getParent(elements[i - 1]) != parent) {
						if (parent == null) {
							parent= elements[i];
						} else {
							hasParents= true;
						}
						if (!parents.contains(parent)) {
							parents.add(parent);
						}
					}
				}
				
				IJavaElement[] parentsArray= (IJavaElement[])parents.toArray(new IJavaElement[parents.size()]);
				if (hasParents) {
					return smallestCommonParents(parentsArray);
				}
				return parentsArray;
			}
		}
		
		private IJavaElement getParent(IJavaElement element) {
			if (element instanceof ICompilationUnit) {
				return element.getParent();
			} else if (element instanceof IPackageFragment){
				return element.getParent().getParent();
			} else {
				return element.getParent();
			}
		}

		protected boolean performFinish() {
			initializeRefactoring();
			return super.performFinish();
		}
	
		public IWizardPage getNextPage() {
			initializeRefactoring();
			return super.getNextPage();
		}

		private void initializeRefactoring() {
			CleanUpRefactoring refactoring= (CleanUpRefactoring)getRefactoring();
			refactoring.clearCompilationUnits();
			Object[] checkedElements= fTreeViewer.getCheckedElements();
			for (int i= 0; i < checkedElements.length; i++) {
				if (checkedElements[i] instanceof ICompilationUnit)
					refactoring.addCompilationUnit((ICompilationUnit)checkedElements[i]);
			}
			if (!refactoring.hasCleanUps()) {
				ICleanUp[] cleanUps= createAllCleanUps();
				for (int i= 0; i < cleanUps.length; i++) {
					refactoring.addCleanUp(cleanUps[i]);
				}
			}
		}
		
		private ICleanUp[] createAllCleanUps() {
			IDialogSettings section= getCleanUpWizardSettings();
			
			ICleanUp[] result= new ICleanUp[8];
			result[0]= new CodeStyleCleanUp(section);
			result[1]= new ControlStatementsCleanUp(section);
			result[2]= new UnusedCodeCleanUp(section);
			result[3]= new Java50CleanUp(section);
			result[4]= new StringCleanUp(section);
			result[5]= new PotentialProgrammingProblemsCleanUp(section);
			result[6]= new UnnecessaryCodeCleanUp(section);
			result[7]= new ExpressionsCleanUp(section);
			
			return result;
		}
	}
	
	private class SelectCleanUpPage extends UserInputWizardPage {
		
		private class TabFolderLayout extends Layout {

			protected Point computeSize (Composite composite, int wHint, int hHint, boolean flushCache) {
				if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
					return new Point(wHint, hHint);
					
				Control [] children = composite.getChildren ();
				int count = children.length;
				int maxWidth = 0, maxHeight = 0;
				for (int i=0; i<count; i++) {
					Control child = children [i];
					Point pt = child.computeSize (SWT.DEFAULT, SWT.DEFAULT, flushCache);
					maxWidth = Math.max (maxWidth, pt.x);
					maxHeight = Math.max (maxHeight, pt.y);
				}
				
				if (wHint != SWT.DEFAULT)
					maxWidth= wHint;
				if (hHint != SWT.DEFAULT)
					maxHeight= hHint;
				
				return new Point(maxWidth, maxHeight);	
				
			}
			
			protected void layout (Composite composite, boolean flushCache) {
				Rectangle rect= composite.getClientArea();
			
				Control[] children = composite.getChildren();
				for (int i = 0; i < children.length; i++) {
					children[i].setBounds(rect);
				}
			}
		}
		
		private ICleanUp[] fCleanUps;
		
		public SelectCleanUpPage(String name) {
			super(name);
		}
		
		public void createControl(Composite parent) {
			TabFolder tabFolder= new TabFolder(parent, SWT.NONE);
			tabFolder.setLayout(new TabFolderLayout());
			tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			createGroups(tabFolder);
			
			setControl(tabFolder);
			Dialog.applyDialogFont(tabFolder);
		}
		
		private void createGroups(TabFolder parent) {
			CleanUpRefactoring refactoring= (CleanUpRefactoring)getRefactoring();
			IJavaProject[] projects= refactoring.getProjects();
			final IJavaProject project;
			if (projects.length == 1) {
				project= projects[0];
			} else {
				project= null;
			}
			
			IDialogSettings section= getCleanUpWizardSettings();
			
			fCleanUps= new ICleanUp[8];
			
			ScrolledComposite codeStyleTab= createTab(parent, MultiFixMessages.CleanUpRefactoringWizard_CodeStyleSection_description);
			Composite codeStyle= fillCodeStyleTab(codeStyleTab, project, section);
			codeStyleTab.setContent(codeStyle);
			codeStyleTab.setMinSize(codeStyle.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			ScrolledComposite unnecessaryCodeTab= createTab(parent, MultiFixMessages.CleanUpRefactoringWizard_UnnecessaryCode_tabLabel);
			Composite unnecessaryCode= fillUnnecessaryCodeTab(unnecessaryCodeTab, project, section);
			unnecessaryCodeTab.setContent(unnecessaryCode);
			unnecessaryCodeTab.setMinSize(unnecessaryCode.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			ScrolledComposite missingCodeTab= createTab(parent, MultiFixMessages.CleanUpRefactoringWizard_MissingCode_tabLabel);
			Composite missingCode= fillMissingCodeTab(missingCodeTab, project, section);
			missingCodeTab.setContent(missingCode);
			missingCodeTab.setMinSize(missingCode.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
		
		private Composite fillCodeStyleTab(Composite parent, final IJavaProject project, IDialogSettings settings) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Composite groups= new Composite(composite, SWT.NONE);
			groups.setLayout(new GridLayout(1, false));
			groups.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Composite group= createGroup(groups, MultiFixMessages.CleanUpRefactoringWizard_memberAccesses_sectionDescription);
			
			fCleanUps[0]= new CodeStyleCleanUp(settings);
			fCleanUps[0].createConfigurationControl(group, project);

			group= createGroup(groups, MultiFixMessages.CleanUpRefactoringWizard_controlStatements_sectionDescription);
			
			fCleanUps[1]= new ControlStatementsCleanUp(settings);
			fCleanUps[1].createConfigurationControl(group, project);
			
			group= createGroup(groups, MultiFixMessages.CleanUpRefactoringWizard_expressions_sectionDescription);
			
			fCleanUps[2]= new ExpressionsCleanUp(settings);
			fCleanUps[2].createConfigurationControl(group, project);

			addEnableButtonsGroup(composite, new ICleanUp[] {fCleanUps[0], fCleanUps[1], fCleanUps[2]});
			
			return composite;
		}
		
		private Composite fillUnnecessaryCodeTab(ScrolledComposite parent, final IJavaProject project, IDialogSettings section) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Composite groups= new Composite(composite, SWT.NONE);
			groups.setLayout(new GridLayout(1, false));
			groups.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			//Unused Code Group
			Composite group= createGroup(groups, MultiFixMessages.CleanUpRefactoringWizard_UnusedCodeSection_description);
			
			fCleanUps[3]= new UnusedCodeCleanUp(section);
			fCleanUps[3].createConfigurationControl(group, project);
			
			group= createGroup(groups, MultiFixMessages.CleanUpRefactoringWizard_UnnecessaryCode_section );
			
			fCleanUps[6]= new UnnecessaryCodeCleanUp(section);
			fCleanUps[6].createConfigurationControl(group, project);
			
			fCleanUps[7]= new StringCleanUp(section);
			fCleanUps[7].createConfigurationControl(group, project);
			
			addEnableButtonsGroup(composite, new ICleanUp[] {fCleanUps[3], fCleanUps[6], fCleanUps[7]});
			
			return composite;
		}

		private Composite fillMissingCodeTab(ScrolledComposite parent, final IJavaProject project, IDialogSettings section) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Composite groups= new Composite(composite, SWT.NONE);
			groups.setLayout(new GridLayout(1, false));
			groups.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			//Java50Fix Group
			Composite group= createGroup(groups, MultiFixMessages.CleanUpRefactoringWizard_Annotations_sectionName);
			fCleanUps[4]= new Java50CleanUp(section);
			fCleanUps[4].createConfigurationControl(group, project);

			//Potential Programming Problems Group
			group= createGroup(groups, MultiFixMessages.CleanUpRefactoringWizard_PotentialProgrammingProblems_description);
			fCleanUps[5]= new PotentialProgrammingProblemsCleanUp(section);
			fCleanUps[5].createConfigurationControl(group, project);

			addEnableButtonsGroup(composite, new ICleanUp[] {fCleanUps[4], fCleanUps[5]});
			
			return composite;
		}
		

		private void addEnableButtonsGroup(Composite parent, final ICleanUp[] cleanUps) {
			Composite down= new Composite(parent, SWT.NONE);
			down.setLayout(new GridLayout(2, false));
			down.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Composite space= new Composite(down, SWT.NONE);
			space.setLayout(new GridLayout(1, true));
			space.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Composite buttons= new Composite(down, SWT.NONE);
			buttons.setLayout(new GridLayout(3, true));
			buttons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			
			Button all= new Button(buttons, SWT.PUSH);
			all.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			all.setText(MultiFixMessages.CleanUpRefactoringWizard_EnableAllButton_label);
			all.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					for (int i= 0; i < cleanUps.length; i++) {
						cleanUps[i].select(65535);
					}
				}	
			});
			
			Button none= new Button(buttons, SWT.PUSH);
			none.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			none.setText(MultiFixMessages.CleanUpRefactoringWizard_DisableAllButton_label);
			none.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					for (int i= 0; i < cleanUps.length; i++) {
						cleanUps[i].select(0);
					}
				}	
			});
			
			Button def= new Button(buttons, SWT.PUSH);
			def.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			def.setText(MultiFixMessages.CleanUpRefactoringWizard_EnableDefaultsButton_label);
			def.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					for (int i= 0; i < cleanUps.length; i++) {
						cleanUps[i].select(cleanUps[i].getDefaultFlag());
					}
				}	
			});
		}

		private ScrolledComposite createTab(TabFolder parent, String label) {
			TabItem csTab= new TabItem(parent, SWT.NONE);
			csTab.setText(label);
			
			ScrolledComposite scrolled= new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
			scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			scrolled.setLayout(new GridLayout(1, false));
			scrolled.setExpandHorizontal(true);
			scrolled.setExpandVertical(true);			
			csTab.setControl(scrolled);
			return scrolled;
		}

		private Composite createGroup(Composite parent, String description) {
			
			Group group= new Group(parent, SWT.NONE);
			group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			group.setLayout(new GridLayout(1, true));
			group.setText(description);
			
			return group;
		}

		protected boolean performFinish() {
			initializeRefactoring();
			storeSettings();
			return super.performFinish();
		}
	
		public IWizardPage getNextPage() {
			initializeRefactoring();
			storeSettings();
			return super.getNextPage();
		}
		
		private void storeSettings() {
			if (fCleanUps != null) {
				IDialogSettings settings= getCleanUpWizardSettings();
				for (int i= 0; i < fCleanUps.length; i++) {
					fCleanUps[i].saveSettings(settings);
				}
			}
		}

		private void initializeRefactoring() {
			if (fCleanUps != null) {
				CleanUpRefactoring refactoring= (CleanUpRefactoring)getRefactoring();
				refactoring.clearCleanUps();
				for (int i= 0; i < fCleanUps.length; i++) {
					refactoring.addCleanUp(fCleanUps[i]);
				}
			}
		}
		
	}
	
	private final boolean fShowCUPage;
	private final boolean fShowCleanUpPage;
	
	public CleanUpRefactoringWizard(CleanUpRefactoring refactoring, int flags, boolean showCUPage, boolean showCleanUpPage) {
		super(refactoring, flags);
		fShowCUPage= showCUPage;
		fShowCleanUpPage= showCleanUpPage;
		setDefaultPageTitle(MultiFixMessages.CleanUpRefactoringWizard_PageTitle);
		setWindowTitle(MultiFixMessages.CleanUpRefactoringWizard_WindowTitle);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		if (fShowCUPage) {
			SelectCUPage selectCUPage= new SelectCUPage(MultiFixMessages.CleanUpRefactoringWizard_SelectCompilationUnitsPage_name);
			if (fShowCleanUpPage) {
				selectCUPage.setMessage(MultiFixMessages.CleanUpRefactoringWizard_SelectCompilationUnitsPage_message);
			} else {
				ICleanUp[] cleanUps= ((CleanUpRefactoring)getRefactoring()).getCleanUps();
				if (cleanUps.length == 1) {
					ICleanUp cleanUp= cleanUps[0];
					String[] descriptions= cleanUp.getDescriptions();
					if (descriptions.length == 1) {
						selectCUPage.setMessage(Messages.format(MultiFixMessages.CleanUpRefactoringWizard_SelectCompilationUnitsPage_preSingleSelect_message, descriptions[0]));
					} else {
						selectCUPage.setMessage(MultiFixMessages.CleanUpRefactoringWizard_SelectCompilationUnitsPage_preSelect_message);
					}
				} else {
					selectCUPage.setMessage(MultiFixMessages.CleanUpRefactoringWizard_SelectCompilationUnitsPage_preSelect_message);
				}
			}
			addPage(selectCUPage);
		}
		
		if (fShowCleanUpPage){
			SelectCleanUpPage selectSolverPage= new SelectCleanUpPage(MultiFixMessages.CleanUpRefactoringWizard_SelectCleanUpsPage_name);
			if (fShowCUPage) {
				selectSolverPage.setMessage(MultiFixMessages.CleanUpRefactoringWizard_SelectCleanUpsPage_message);
			} else {
				ICompilationUnit[] compilationUnits= ((CleanUpRefactoring)getRefactoring()).getCompilationUnits();
				if (compilationUnits.length == 1) {
					String label= JavaElementLabels.getElementLabel(compilationUnits[0], JavaElementLabels.ALL_DEFAULT);
					selectSolverPage.setMessage(Messages.format(MultiFixMessages.CleanUpRefactoringWizard_SelectCleanUpsPage_preSingleSelect_message, label));
				} else {
					selectSolverPage.setMessage(MultiFixMessages.CleanUpRefactoringWizard_SelectCleanUpsPage_message);
				}
			}
			addPage(selectSolverPage);
		}
	}
	
	private static IDialogSettings getCleanUpWizardSettings() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		IDialogSettings section= settings.getSection(CLEAN_UP_WIZARD_SETTINGS_SECTION_ID);
		if (section == null) {
			section= settings.addNewSection(CLEAN_UP_WIZARD_SETTINGS_SECTION_ID);
		}
		return section;
	}


}
