/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rodrigo Kumpera <kumpera AT gmail.com> - bug 95232
 *     
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetPage;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.filters.EmptyInnerPackageFilter;
import org.eclipse.jdt.internal.ui.util.JavaUIHelp;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * The Java working set page allows the user to create
 * and edit a Java working set.
 * <p>
 * Workspace elements can be added/removed from a tree into
 * a list.
 * </p>
 * 
 * @since 2.0
 */
public class JavaWorkingSetPage extends WizardPage implements IWorkingSetPage {
	
	private final class AddedElementsFilter extends EmptyInnerPackageFilter {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (fSelectedElements.contains(element))
				return false;
			
			return super.select(viewer, parentElement, element);
		}
		
	}

	final private static String PAGE_TITLE= WorkingSetMessages.JavaWorkingSetPage_title; 
	final private static String PAGE_ID= "javaWorkingSetPage"; //$NON-NLS-1$
	
	private Text fWorkingSetName;
	private TreeViewer fTree;
	private TableViewer fTable;
	private ITreeContentProvider fTreeContentProvider;
	
	private boolean fFirstCheck;
	private final HashSet fSelectedElements;
	private IWorkingSet fWorkingSet;

	/**
	 * Default constructor.
	 */
	public JavaWorkingSetPage() {
		super(PAGE_ID, PAGE_TITLE, JavaPluginImages.DESC_WIZBAN_JAVA_WORKINGSET);
		setDescription(WorkingSetMessages.JavaWorkingSetPage_workingSet_description);
		fSelectedElements= new HashSet();
		fFirstCheck= true;
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);

		Label label= new Label(composite, SWT.WRAP);
		label.setText(WorkingSetMessages.JavaWorkingSetPage_workingSet_name); 
		GridData gd= new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gd);

		fWorkingSetName= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fWorkingSetName.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fWorkingSetName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validateInput();
				}
			}
		);
		
		Composite leftCenterRightComposite= new Composite(composite, SWT.NONE);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint= convertHeightInCharsToPixels(20);
		leftCenterRightComposite.setLayoutData(gridData);
		GridLayout gridLayout= new GridLayout(3, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		leftCenterRightComposite.setLayout(gridLayout);
		
		Composite leftComposite= new Composite(leftCenterRightComposite, SWT.NONE);
		gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint= convertWidthInCharsToPixels(40);
		leftComposite.setLayoutData(gridData);
		gridLayout= new GridLayout(1, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		leftComposite.setLayout(gridLayout);
		
		Composite centerComposite = new Composite(leftCenterRightComposite, SWT.NONE);
		gridLayout= new GridLayout(1, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		centerComposite.setLayout(gridLayout);
		centerComposite.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		
		Composite rightComposite= new Composite(leftCenterRightComposite, SWT.NONE);
		gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint= convertWidthInCharsToPixels(40);
		rightComposite.setLayoutData(gridData);
		gridLayout= new GridLayout(1, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		rightComposite.setLayout(gridLayout);
		
		createTree(leftComposite);
		createTable(rightComposite);
	
		if (fWorkingSet != null)
			fWorkingSetName.setText(fWorkingSet.getName());

		initializeSelectedElements();
		validateInput();
		
		fTable.setInput(fSelectedElements);
		fTable.refresh(true);
		fTree.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
		
		Object[] selection= getActivePartSelection();
		if (selection.length > 0) {
			try {
				fTree.getTree().setRedraw(false);
				
				for (int i= 0; i < selection.length; i++) {				
					fTree.expandToLevel(selection[i], 0);
				}
				fTree.setSelection(new StructuredSelection(selection));
			} finally {
				fTree.getTree().setRedraw(true);
			}
		}
		
		createButtonBar(centerComposite);
		
		fWorkingSetName.setFocus();
		fWorkingSetName.setSelection(0, fWorkingSetName.getText().length());

		Dialog.applyDialogFont(composite);
		// Set help for the page 
		JavaUIHelp.setHelp(fTable, IJavaHelpContextIds.JAVA_WORKING_SET_PAGE);
	}

	private void createTree(Composite parent) {
		
		Label label= new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false));
		label.setText(WorkingSetMessages.JavaWorkingSetPage_workspace_content);
		
		fTree= new TreeViewer(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		ColoredViewersManager.install(fTree);
		fTree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		fTreeContentProvider= new JavaWorkingSetPageContentProvider();
		fTree.setContentProvider(fTreeContentProvider);
		
		AppearanceAwareLabelProvider javaElementLabelProvider= 
			new AppearanceAwareLabelProvider(
				AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED,
				AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS
			);
		
		fTree.setLabelProvider(new DecoratingJavaLabelProvider(javaElementLabelProvider));
		fTree.setComparator(new JavaElementComparator());
		final AddedElementsFilter filter= new AddedElementsFilter();
		fTree.addFilter(filter);
		fTree.setUseHashlookup(true);
	}

	private void createButtonBar(Composite parent) {
		Label spacer= new Label(parent, SWT.NONE);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		final Button addButton= new Button(parent, SWT.PUSH);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		addButton.setText(WorkingSetMessages.JavaWorkingSetPage_add_button);
		addButton.setEnabled(!fTree.getSelection().isEmpty());
		SWTUtil.setButtonDimensionHint(addButton);
		
		final Button addAllButton= new Button(parent, SWT.PUSH);
		addAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		addAllButton.setText(WorkingSetMessages.JavaWorkingSetPage_addAll_button);
		addAllButton.setEnabled(fTree.getTree().getItems().length > 0);
		SWTUtil.setButtonDimensionHint(addAllButton);
		
		final Button removeButton= new Button(parent, SWT.PUSH);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		removeButton.setText(WorkingSetMessages.JavaWorkingSetPage_remove_button);
		removeButton.setEnabled(!fTable.getSelection().isEmpty());
		SWTUtil.setButtonDimensionHint(removeButton);
		
		final Button removeAllButton= new Button(parent, SWT.PUSH);
		removeAllButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		removeAllButton.setText(WorkingSetMessages.JavaWorkingSetPage_removeAll_button);
		removeAllButton.setEnabled(!fSelectedElements.isEmpty());
		SWTUtil.setButtonDimensionHint(removeAllButton);
		
		fTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				addButton.setEnabled(!event.getSelection().isEmpty());
			}
		});
		
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection= (IStructuredSelection) fTree.getSelection();
				fSelectedElements.addAll(selection.toList());
				Object[] selectedElements= selection.toArray();
				fTable.add(selectedElements);
				fTree.remove(selectedElements);
				fTable.setSelection(selection);
				fTable.getControl().setFocus();
				validateInput();
				
				removeAllButton.setEnabled(true);
				addAllButton.setEnabled(fTree.getTree().getItems().length > 0);
			}
		});
		
		fTable.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeButton.setEnabled(!event.getSelection().isEmpty());
			}
		});
		
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection= (IStructuredSelection) fTable.getSelection();
				fSelectedElements.removeAll(selection.toList());
				Object[] selectedElements= selection.toArray();
				fTable.remove(selectedElements);
				try {
					fTree.getTree().setRedraw(false);
					for (int i= 0; i < selectedElements.length; i++) {
						fTree.refresh(fTreeContentProvider.getParent(selectedElements[i]), true);
					}
				} finally {
					fTree.getTree().setRedraw(true);
				}
				fTree.setSelection(selection);
				fTree.getControl().setFocus();
				validateInput();
				
				addAllButton.setEnabled(true);
				removeAllButton.setEnabled(!fSelectedElements.isEmpty());
			}
		});
		
		addAllButton.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] items= fTree.getTree().getItems();
				for (int i= 0; i < items.length; i++) {
					fSelectedElements.add(items[i].getData());
				}
				fTable.refresh();
				fTree.refresh();
				
				addAllButton.setEnabled(false);
				removeAllButton.setEnabled(true);
			}
		});
		
		removeAllButton.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				fSelectedElements.clear();
				
				fTable.refresh();
				fTree.refresh();
				
				removeAllButton.setEnabled(false);
				addAllButton.setEnabled(true);
			}
		});

	}

	private void createTable(Composite parent) {
		Label label= new Label(parent, SWT.WRAP);
		label.setText(WorkingSetMessages.JavaWorkingSetPage_workingSet_content); 
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		fTable= new TableViewer(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		ColoredViewersManager.install(fTable);
		
		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		fTable.getControl().setLayoutData(gd);
		
		fTable.setContentProvider(new IStructuredContentProvider() {

			public Object[] getElements(Object inputElement) {
				return fSelectedElements.toArray();
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
		});
		
		AppearanceAwareLabelProvider javaElementLabelProvider= new AppearanceAwareLabelProvider(
			AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED,
			AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS
		);
		fTable.setLabelProvider(new DecoratingJavaLabelProvider(javaElementLabelProvider));
		fTable.setComparator(new JavaElementComparator());
		fTable.addFilter(new EmptyInnerPackageFilter());
		fTable.setUseHashlookup(true);
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public IWorkingSet getSelection() {
		return fWorkingSet;
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public void setSelection(IWorkingSet workingSet) {
		Assert.isNotNull(workingSet, "Working set must not be null"); //$NON-NLS-1$
		fWorkingSet= workingSet;
		if (getContainer() != null && getShell() != null && fWorkingSetName != null) {
			fFirstCheck= false;
			fWorkingSetName.setText(fWorkingSet.getName());
			initializeSelectedElements();
			validateInput();
		}
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public void finish() {
		String workingSetName= fWorkingSetName.getText();
		HashSet elements= fSelectedElements;
		if (fWorkingSet == null) {
			IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
			fWorkingSet= workingSetManager.createWorkingSet(workingSetName, (IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
		} else {
			// Add inaccessible resources
			IAdaptable[] oldItems= fWorkingSet.getElements();
			HashSet closedWithChildren= new HashSet(elements.size());
			for (int i= 0; i < oldItems.length; i++) {
				IResource oldResource= null;
				if (oldItems[i] instanceof IResource) {
					oldResource= (IResource)oldItems[i];
				} else {
					oldResource= (IResource)oldItems[i].getAdapter(IResource.class);
				}
				if (oldResource != null && oldResource.isAccessible() == false) {
					IProject project= oldResource.getProject();
					if (closedWithChildren.contains(project) || elements.contains(project)) {
						elements.add(oldItems[i]);
						elements.remove(project);
						closedWithChildren.add(project);
					}
				}
			}
			fWorkingSet.setName(workingSetName);
			fWorkingSet.setElements((IAdaptable[]) elements.toArray(new IAdaptable[elements.size()]));
		}
	}

	private void validateInput() {
		String errorMessage= null; 
		String infoMessage= null;
		String newText= fWorkingSetName.getText();

		if (newText.equals(newText.trim()) == false)
			errorMessage = WorkingSetMessages.JavaWorkingSetPage_warning_nameWhitespace; 
		if (newText.equals("")) { //$NON-NLS-1$
			if (fFirstCheck) {
				setPageComplete(false);
				fFirstCheck= false;
				return;
			}
			else				
				errorMessage= WorkingSetMessages.JavaWorkingSetPage_warning_nameMustNotBeEmpty; 
		}

		fFirstCheck= false;

		if (errorMessage == null && (fWorkingSet == null || newText.equals(fWorkingSet.getName()) == false)) {
			IWorkingSet[] workingSets= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
			for (int i= 0; i < workingSets.length; i++) {
				if (newText.equals(workingSets[i].getName())) {
					errorMessage= WorkingSetMessages.JavaWorkingSetPage_warning_workingSetExists; 
				}
			}
		}
		
		if (!hasSelectedElement())
			infoMessage= WorkingSetMessages.JavaWorkingSetPage_warning_resourceMustBeChecked;

		setMessage(infoMessage, INFORMATION);
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}
	
	private boolean hasSelectedElement() {
		return !fSelectedElements.isEmpty();
	}
	
	private void initializeSelectedElements() {
		if (fWorkingSet == null)
			return;
		
		Object[] elements= fWorkingSet.getElements();
		
		// Use closed project for elements in closed project
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			if (element instanceof IResource) {
				IProject project= ((IResource)element).getProject();
				if (!project.isAccessible())
					elements[i]= project;
			}
			if (element instanceof IJavaElement) {
				IJavaProject jProject= ((IJavaElement)element).getJavaProject();
				if (jProject != null && !jProject.getProject().isAccessible()) 
					elements[i]= jProject.getProject();
			}
		}

		fSelectedElements.addAll(Arrays.asList(elements));
	}

	private Object[] getActivePartSelection() {
		final Object[][] result= new Object[1][];
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				IWorkbenchPage page= JavaPlugin.getActivePage();
				if (page == null)
					return;
				
				IWorkbenchPart part= JavaPlugin.getActivePage().getActivePart();
				if (part == null)
					return;
				
				try {
					Object[] elements= SelectionConverter.getStructuredSelection(part).toArray();
					for (int i= 0; i < elements.length; i++) {
						if (elements[i] instanceof IResource) {
							IJavaElement je= (IJavaElement)((IResource)elements[i]).getAdapter(IJavaElement.class);
							if (je != null && je.exists() &&  je.getJavaProject().isOnClasspath((IResource)elements[i]))
								elements[i]= je;
						}
					}
					result[0]= elements;
				} catch (JavaModelException e) {
					return;
				}
			}
		});
		
		if (result[0] == null)
			return new Object[0];
		
		return result[0];
	}
}
