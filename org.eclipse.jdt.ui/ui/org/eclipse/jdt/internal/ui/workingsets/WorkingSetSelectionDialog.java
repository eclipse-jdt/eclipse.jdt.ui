/******************************************************************************* 
 * Copyright (c) 2000, 2003 IBM Corporation and others. 
 * All rights reserved. This program and the accompanying materials! 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html 
 * 
 * Contributors: 
 *      IBM Corporation - initial API and implementation 
 * 		Sebastian Davids <sdavids@gmx.de> - Fix for bug 19346 - Dialog font
 *   	should be activated and used by other components.
 *************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.WorkingSet;
import org.eclipse.ui.internal.dialogs.ListContentProvider;
import org.eclipse.ui.internal.dialogs.WorkingSetNewWizard;
import org.eclipse.ui.model.WorkbenchViewerSorter;

/**
 * A working set selection dialog displays a list of working
 * sets available in the workbench.
 * 
 * @see IWorkingSetSelectionDialog
 * @since 2.0
 */
public class WorkingSetSelectionDialog extends SelectionDialog implements
        IWorkingSetSelectionDialog {
    private final static int SIZING_SELECTION_WIDGET_HEIGHT = 200;

    private final static int SIZING_SELECTION_WIDGET_WIDTH = 50;

    private static class WorkingSetLabelProvider extends LabelProvider {
        private Map icons;

        public WorkingSetLabelProvider() {
            icons = new Hashtable();
        }

        public void dispose() {
            Iterator iterator = icons.values().iterator();

            while (iterator.hasNext()) {
                Image icon = (Image) iterator.next();
                icon.dispose();
            }
            super.dispose();
        }

        public Image getImage(Object object) {
            Assert.isTrue(object instanceof IWorkingSet);
            IWorkingSet workingSet = (IWorkingSet) object;
            ImageDescriptor imageDescriptor = workingSet.getImage();

            if (imageDescriptor == null)
                return null;

            Image icon = (Image) icons.get(imageDescriptor);
            if (icon == null) {
                icon = imageDescriptor.createImage();
                icons.put(imageDescriptor, icon);
            }
            return icon;
        }

        public String getText(Object object) {
            Assert.isTrue(object instanceof IWorkingSet);
            IWorkingSet workingSet = (IWorkingSet) object;
            return workingSet.getName();
        }
    }
    
    private List fElements;
    
    private ILabelProvider labelProvider;

    private IStructuredContentProvider contentProvider;

    private TableViewer listViewer;

    private Button newButton;

    private Button detailsButton;

    private Button removeButton;
    
    private Button upButton;
    
    private Button downButton;

    private IWorkingSet[] result;

    private boolean sorted;
    
    private boolean multiSelect;

    private List addedWorkingSets;

    private List removedWorkingSets;

    private Map editedWorkingSets;

    private List removedMRUWorkingSets;

	private int nextButtonId= IDialogConstants.CLIENT_ID + 1;

    /**
     * Creates a working set selection dialog.
     *
     * @param parentShell the parent shell
     * @param multi true=more than one working set can be chosen 
     * 	in the dialog. false=only one working set can be chosen. Multiple
     * 	working sets can still be selected and removed from the list but
     * 	the dialog can only be closed when a single working set is selected.
     */
    public WorkingSetSelectionDialog(Shell parentShell, boolean multi) {
    	this(parentShell, WorkbenchPlugin.getDefault().getWorkingSetManager().getWorkingSets(), multi, true);
    }
    
    public WorkingSetSelectionDialog(Shell parentShell, IWorkingSet[] workingSets, boolean multi, boolean sorted) {
        super(parentShell);
        contentProvider = new ListContentProvider();
        labelProvider = new WorkingSetLabelProvider();
        multiSelect = multi;
        if (multiSelect) {
            setTitle(WorkbenchMessages
                    .getString("WorkingSetSelectionDialog.title.multiSelect")); //$NON-NLS-1$;
            setMessage(WorkbenchMessages
                    .getString("WorkingSetSelectionDialog.message.multiSelect")); //$NON-NLS-1$
        } else {
            setTitle(WorkbenchMessages
                    .getString("WorkingSetSelectionDialog.title")); //$NON-NLS-1$;
            setMessage(WorkbenchMessages
                    .getString("WorkingSetSelectionDialog.message")); //$NON-NLS-1$
        }
    	this.sorted= sorted;
    	fElements= new ArrayList(Arrays.asList(workingSets));
    }

    /**
     * Adds the modify buttons to the dialog.
     * 
     * @param composite Composite to add the buttons to
     */
    private void addModifyButtons(Composite composite) {
        Composite buttonComposite = new Composite(composite, SWT.RIGHT);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        buttonComposite.setLayout(layout);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END
                | GridData.GRAB_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        composite.setData(data);

        newButton = createButton(buttonComposite, nextButtonId++, WorkbenchMessages
                .getString("WorkingSetSelectionDialog.newButton.label"), false); //$NON-NLS-1$
        newButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                createWorkingSet();
            }
        });

        detailsButton = createButton(
                buttonComposite,
                nextButtonId++,
                WorkbenchMessages
                        .getString("WorkingSetSelectionDialog.detailsButton.label"), false); //$NON-NLS-1$
        detailsButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                editSelectedWorkingSet();
            }
        });

        removeButton = createButton(
                buttonComposite,
                nextButtonId++,
                WorkbenchMessages
                        .getString("WorkingSetSelectionDialog.removeButton.label"), false); //$NON-NLS-1$
        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                removeSelectedWorkingSets();
            }
        });
    }

    /**
     * Overrides method from Dialog.
     * 
     * @see Dialog#cancelPressed()
     */
    protected void cancelPressed() {
        restoreAddedWorkingSets();
        restoreChangedWorkingSets();
        restoreRemovedWorkingSets();
        super.cancelPressed();
    }

    /** 
     * Overrides method from Window.
     * 
     * @see org.eclipse.jface.window.Window#configureShell(Shell)
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        WorkbenchHelp.setHelp(shell,
                IWorkbenchHelpContextIds.WORKING_SET_SELECTION_DIALOG);
    }

    /**
     * Overrides method from Dialog.
     * Create the dialog widgets.
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(Composite)
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setFont(parent.getFont());
		
        createMessageArea(composite);
        if (sorted) {
            createListViewer(composite);
        } else {
        	Composite inner = new Composite(composite, SWT.NONE);
        	inner.setFont(composite.getFont());
        	inner.setLayoutData(new GridData(GridData.FILL_BOTH));
        	GridLayout layout= new GridLayout();
        	layout.numColumns= 2; layout.marginHeight= 0; layout.marginWidth= 0;
        	inner.setLayout(layout);
        	createListViewer(inner);
        	createOrderButtons(inner);
        }
        addModifyButtons(composite);
        listViewer.setInput(fElements);

        return composite;
    }

    /**
     * Creates a list viewer according to the multi selection setting
     * 
     * @param parent the list viewer's parent
     */
    private void createListViewer(Composite parent) {
		if (multiSelect) {
        	CheckboxTableViewer ctv= CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.MULTI);
        	ctv.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					updateButtonAvailability();
				}
			});
			listViewer= ctv;
        } else {
        	listViewer = new TableViewer(parent, SWT.BORDER | SWT.MULTI);
        }
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
        data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
        listViewer.getTable().setLayoutData(data);
        listViewer.getTable().setFont(parent.getFont());

        listViewer.setLabelProvider(labelProvider);
        listViewer.setContentProvider(contentProvider);
        if (sorted)
        	listViewer.setSorter(new WorkbenchViewerSorter());
        listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                handleSelectionChanged();
            }
        });
        listViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                okPressed();
            }
        });
	}
    
    private void createOrderButtons(Composite parent) {
    	Composite buttons= new Composite(parent, SWT.NONE);
    	buttons.setFont(parent.getFont());
    	buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
    	GridLayout layout= new GridLayout();
    	layout.marginHeight= 0; layout.marginWidth= 0;
    	buttons.setLayout(layout);
   	
    	upButton= new Button(buttons, SWT.PUSH);
    	upButton.setText("&Up");
    	setButtonLayoutData(upButton);
    	upButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				moveUp(((IStructuredSelection)listViewer.getSelection()).toList());
			}
		});

    	downButton= new Button(buttons, SWT.PUSH);
    	downButton.setText("&Down");
    	setButtonLayoutData(downButton);
    	downButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				moveDown(((IStructuredSelection)listViewer.getSelection()).toList());
			}
		});
    }

	/**
     * Overrides method from Dialog.
     * Sets the initial selection, if any.
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createContents(Composite)
     */
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        setInitialSelection();
        updateButtonAvailability();
        //don't allow ok dismissal until a change has been made. 
        //Fixes bug 22735.
        getOkButton().setEnabled(false);
        return control;
    }

    private void setInitialSelection() {
		List selections = getInitialElementSelections();
        if (!selections.isEmpty()) {
        	if (multiSelect) {
        		((CheckboxTableViewer)listViewer).setCheckedElements(selections.toArray());
        	} else {
        		listViewer.setSelection(new StructuredSelection(selections), true);
        	}
        }
	}

	/**
     * Opens a working set wizard for creating a new working set.
     */
    void createWorkingSet() {
        WorkingSetNewWizard wizard = new WorkingSetNewWizard();
        WizardDialog dialog = new WizardDialog(getShell(), wizard);

        dialog.create();
        WorkbenchHelp.setHelp(dialog.getShell(),
                IWorkbenchHelpContextIds.WORKING_SET_NEW_WIZARD);
        if (dialog.open() == Window.OK) {
            IWorkingSetManager manager = WorkbenchPlugin.getDefault()
                    .getWorkingSetManager();
            IWorkingSet workingSet = wizard.getSelection();

            listViewer.add(workingSet);
            listViewer.setSelection(new StructuredSelection(workingSet), true);
            manager.addWorkingSet(workingSet);
            addedWorkingSets.add(workingSet);
        }
    }

    /**
     * Opens a working set wizard for editing the currently selected 
     * working set.
     * 
     * @see org.eclipse.ui.IWorkingSetPage
     */
    void editSelectedWorkingSet() {
        IWorkingSetManager manager = WorkbenchPlugin.getDefault()
                .getWorkingSetManager();
        IWorkingSet editWorkingSet = (IWorkingSet) getSelectedWorkingSets()
                .get(0);
        IWorkingSetEditWizard wizard = manager
                .createWorkingSetEditWizard(editWorkingSet);
        WizardDialog dialog = new WizardDialog(getShell(), wizard);
        IWorkingSet originalWorkingSet = (IWorkingSet) editedWorkingSets
                .get(editWorkingSet);
        boolean firstEdit = originalWorkingSet == null;

        // save the original working set values for restoration when selection 
        // dialog is cancelled.
        if (firstEdit) {
            originalWorkingSet = new WorkingSet(editWorkingSet.getName(),
                    editWorkingSet.getElements());
        } else {
            editedWorkingSets.remove(editWorkingSet);
        }
        dialog.create();
        WorkbenchHelp.setHelp(dialog.getShell(),
                IWorkbenchHelpContextIds.WORKING_SET_EDIT_WIZARD);
        if (dialog.open() == Window.OK) {
            editWorkingSet = wizard.getSelection();
            listViewer.update(editWorkingSet, null);
            // make sure ok button is enabled when the selected working set 
            // is edited. Fixes bug 33386.
            updateButtonAvailability();
        }
        editedWorkingSets.put(editWorkingSet, originalWorkingSet);
    }

    /**
     * Implements IWorkingSetSelectionDialog.
     *
     * @see org.eclipse.ui.dialogs.IWorkingSetSelectionDialog#getSelection()
     */
    public IWorkingSet[] getSelection() {
        return result;
    }

    /**
     * Returns the selected working sets.
     * 
     * @return the selected working sets
     */
    private List getSelectedWorkingSets() {
    	if (multiSelect) {
    		Object[] checked= ((CheckboxTableViewer)listViewer).getCheckedElements();
    		return new ArrayList(Arrays.asList(checked));
    	} else {
	        ISelection selection = listViewer.getSelection();
	        if (selection instanceof IStructuredSelection)
	            return ((IStructuredSelection) selection).toList();
	        return null;
    	}
    }

    /**
     * Called when the selection has changed.
     */
    void handleSelectionChanged() {
        updateButtonAvailability();
    }

    /**
     * Sets the selected working sets as the dialog result.
     * Overrides method from Dialog
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed() {
        List newResult = getSelectedWorkingSets();

        result = (IWorkingSet[]) newResult.toArray(new IWorkingSet[newResult
                .size()]);
        setResult(newResult);
        super.okPressed();
    }

    /**
     * Overrides method in Dialog
     * 
     * @see org.eclipse.jface.dialogs.Dialog#open()
     */
    public int open() {
        addedWorkingSets = new ArrayList();
        removedWorkingSets = new ArrayList();
        editedWorkingSets = new HashMap();
        removedMRUWorkingSets = new ArrayList();
        return super.open();
    }

    /**
     * Removes the selected working sets from the workbench.
     */
    void removeSelectedWorkingSets() {
        ISelection selection = listViewer.getSelection();

        if (selection instanceof IStructuredSelection) {
            IWorkingSetManager manager = WorkbenchPlugin.getDefault()
                    .getWorkingSetManager();
            Iterator iter = ((IStructuredSelection) selection).iterator();
            while (iter.hasNext()) {
                IWorkingSet workingSet = (IWorkingSet) iter.next();
                if (addedWorkingSets.contains(workingSet)) {
                    addedWorkingSets.remove(workingSet);
                } else {
                    IWorkingSet[] recentWorkingSets = manager
                            .getRecentWorkingSets();
                    for (int i = 0; i < recentWorkingSets.length; i++) {
                        if (workingSet.equals(recentWorkingSets[i])) {
                            removedMRUWorkingSets.add(workingSet);
                            break;
                        }
                    }
                    removedWorkingSets.add(workingSet);
                }
                manager.removeWorkingSet(workingSet);
            }
            listViewer.remove(((IStructuredSelection) selection).toArray());
        }
    }

    /**
     * Removes newly created working sets from the working set manager.
     */
    private void restoreAddedWorkingSets() {
        IWorkingSetManager manager = WorkbenchPlugin.getDefault()
                .getWorkingSetManager();
        Iterator iterator = addedWorkingSets.iterator();

        while (iterator.hasNext()) {
            manager.removeWorkingSet(((IWorkingSet) iterator.next()));
        }
    }

    /**
     * Rolls back changes to working sets.
     */
    private void restoreChangedWorkingSets() {
        Iterator iterator = editedWorkingSets.keySet().iterator();

        while (iterator.hasNext()) {
            IWorkingSet editedWorkingSet = (IWorkingSet) iterator.next();
            IWorkingSet originalWorkingSet = (IWorkingSet) editedWorkingSets
                    .get(editedWorkingSet);

            if (editedWorkingSet.getName().equals(originalWorkingSet.getName()) == false) {
                editedWorkingSet.setName(originalWorkingSet.getName());
            }
            if (editedWorkingSet.getElements().equals(
                    originalWorkingSet.getElements()) == false) {
                editedWorkingSet.setElements(originalWorkingSet.getElements());
            }
        }
    }

    /**
     * Adds back removed working sets to the working set manager.
     */
    private void restoreRemovedWorkingSets() {
        IWorkingSetManager manager = WorkbenchPlugin.getDefault()
                .getWorkingSetManager();
        Iterator iterator = removedWorkingSets.iterator();

        while (iterator.hasNext()) {
            manager.addWorkingSet(((IWorkingSet) iterator.next()));
        }
        iterator = removedMRUWorkingSets.iterator();
        while (iterator.hasNext()) {
            manager.addRecentWorkingSet(((IWorkingSet) iterator.next()));
        }
    }

    /**
     * Implements IWorkingSetSelectionDialog.
     *
     * @see org.eclipse.ui.dialogs.IWorkingSetSelectionDialog#setSelection(IWorkingSet[])
     */
    public void setSelection(IWorkingSet[] workingSets) {
        result = workingSets;
        setInitialSelections(workingSets);
    }

    /**
     * Updates the modify buttons' enabled state based on the 
     * current seleciton.
     */
    private void updateButtonAvailability() {
        ISelection selection = listViewer.getSelection();
        boolean hasSelection = selection != null && !selection.isEmpty();
        boolean hasSingleSelection = hasSelection;

        removeButton.setEnabled(hasSelection);
        if (hasSelection && selection instanceof IStructuredSelection) {
            hasSingleSelection = ((IStructuredSelection) selection).size() == 1;
        }
        detailsButton.setEnabled(hasSingleSelection);
        if (multiSelect == false) {
            getOkButton().setEnabled(
                    hasSelection == false || hasSingleSelection);
        } else {
            getOkButton().setEnabled(true);
        }
        if (upButton != null) {
        	upButton.setEnabled(canMoveUp());
        }
        
        if (downButton != null) {
        	downButton.setEnabled(canMoveDown());
        }
    }
    
	private void moveUp(List toMoveUp) {
		if (toMoveUp.size() > 0) {
			setElements(moveUp(fElements, toMoveUp));
			listViewer.reveal(toMoveUp.get(0));
		}
	}
	
	private void moveDown(List toMoveDown) {
		if (toMoveDown.size() > 0) {
			setElements(reverse(moveUp(reverse(fElements), toMoveDown)));
			listViewer.reveal(toMoveDown.get(toMoveDown.size() - 1));
		}
	}
	
	private void setElements(List elements) {
		fElements= elements;
		listViewer.setInput(fElements);
		updateButtonAvailability();
	}
	
	private List moveUp(List elements, List move) {
		int nElements= elements.size();
		List res= new ArrayList(nElements);
		Object floating= null;
		for (int i= 0; i < nElements; i++) {
			Object curr= elements.get(i);
			if (move.contains(curr)) {
				res.add(curr);
			} else {
				if (floating != null) {
					res.add(floating);
				}
				floating= curr;
			}
		}
		if (floating != null) {
			res.add(floating);
		}
		return res;
	}	
	
	private List reverse(List p) {
		List reverse= new ArrayList(p.size());
		for (int i= p.size()-1; i >= 0; i--) {
			reverse.add(p.get(i));
		}
		return reverse;
	}
	
	private boolean canMoveUp() {
		int[] indc= listViewer.getTable().getSelectionIndices();
		for (int i= 0; i < indc.length; i++) {
			if (indc[i] != i) {
				return true;
			}
		}
		return false;
	}
	
	private boolean canMoveDown() {
		int[] indc= listViewer.getTable().getSelectionIndices();
		int k= fElements.size() - 1;
		for (int i= indc.length - 1; i >= 0 ; i--, k--) {
			if (indc[i] != k) {
				return true;
			}
		}
		return false;
	}	
}