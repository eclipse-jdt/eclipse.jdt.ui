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

package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.actions.ActionContext;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.filters.LibraryFilter;
import org.eclipse.jdt.internal.ui.filters.OutputFolderFilter;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListLabelProvider;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;

/**
 * A package explorer widget that can be used in dialogs. It uses its own 
 * content provider, label provider, element sorter and filter to display
 * elements that are not shown usually in the package explorer of the
 * workspace.
 */
public class DialogPackageExplorer implements IMenuListener, ISelectionChangedListener {
    /**
     * A extended content provider for the package explorer which can additionally display
     * an output folder item.
     */
    private final class PackageContentProvider extends StandardJavaElementContentProvider {
        public PackageContentProvider() {
            super();
        }
        
        /**
         * Get the elements of the current project
         * 
         * @param element the element to get the children from, will
         * not be used, instead the project childrens are returned directly
         * @return returns the children of the project
         */
        public Object[] getElements(Object element) {
            if (fCurrJProject == null)
                return new Object[0];
            return new Object[] {fCurrJProject};
        }
        
        /**
         * Get the children of the current <code>element</code>. If the
         * element is of type <code>IPackageFragmentRoot</code> and
         * displaying the output folders is selected, then an icon for
         * the output folder is created and displayed additionally.
         * 
         * @param element the current element to get the children from
         * @return an array of children
         */
        public Object[] getChildren(Object element) {
            Object[] children= super.getChildren(element);
            if (element instanceof IPackageFragmentRoot && isSelected()) {
                try {
                    IClasspathEntry entry= ((IPackageFragmentRoot) element).getRawClasspathEntry();
                    CPListElement parent= CPListElement.createFromExisting(entry, fCurrJProject);                    
                    CPListElementAttribute outputFolder= new CPListElementAttribute(parent, CPListElement.OUTPUT, 
                            parent.getAttribute(CPListElement.OUTPUT));
                    Object[] extendedChildren= new Object[children.length + 1];
                    System.arraycopy(children, 0, extendedChildren, 1, children.length);
                    extendedChildren[0]= outputFolder;
                    return extendedChildren;
                } catch (JavaModelException e) {
                    JavaPlugin.log(e);
                }
                return null;
            }
            else
                return children;                    
        }
    }
    
    /**
     * A extended label provider for the package explorer which can additionally display
     * an output folder item.
     */
    private final class PackageLabelProvider extends AppearanceAwareLabelProvider {
        private CPListLabelProvider outputFolderLabel;
        
        public PackageLabelProvider(long textFlags, int imageFlags) {
            super(textFlags, imageFlags);
            outputFolderLabel= new CPListLabelProvider();
        }
        
        public String getText(Object element) {
            if (element instanceof CPListElementAttribute)
                return outputFolderLabel.getText(element);
            return super.getText(element);
        }
        
        public Image getImage(Object element) {
            if (element instanceof CPListElementAttribute)
                return outputFolderLabel.getImage(element);
            return super.getImage(element);
        }
    }
    
    /**
     * A extended element sorter for the package explorer which displays the output
     * folder (if any) as first child of a source folder. The other java elements
     * are sorted in the normal way.
     */
    private final class ExtendedJavaElementSorter extends JavaElementSorter {
        public ExtendedJavaElementSorter() {
            super();
        }
        
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (e1 instanceof CPListElementAttribute)
                return -1;
            if (e2 instanceof CPListElementAttribute)
                return 1;
            return super.compare(viewer, e1, e2);
        }
    }
    
    /**
     * A extended filter for the package explorer which filters libraries and
     * files if their name is either ".classpath" or ".project".
     */
    private final class PackageFilter extends LibraryFilter {
        private OutputFolderFilter fOutputFolderFilter= new OutputFolderFilter();
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (element instanceof IFile) {
                IFile file= (IFile) element;
                if (file.getName().equals(".classpath") || file.getName().equals(".project")) //$NON-NLS-1$//$NON-NLS-2$
                    return false;
            }
            if (element instanceof IPackageFragmentRoot) {
                IPackageFragmentRoot root= (IPackageFragmentRoot)element;
                if (root.getElementName().endsWith(".jar")) //$NON-NLS-1$
                    return false;
            }
            return super.select(viewer, parentElement, element) && fOutputFolderFilter.select(viewer, parentElement, element);
        }
    }
    
    /** The tree showing the project like in the package explorer */
    private TreeViewer fPackageViewer;
    /** The tree's context menu */
    private Menu fContextMenu;
    /** The action group which is used to fill the context menu. The action group
     * is also called if the selection on the tree changes */
    private DialogPackageExplorerActionGroup fActionGroup;
    /**
     * Flag to indicate whether output folders
     * can be created or not. This is used to
     * set the content correctly in the case
     * that a IPackageFragmentRoot is selected.
     * 
     * @see #widgetSelected(boolean)
     */
    private boolean fShowOutputFolders= false;
    
    /** Stores the current selection in the tree */
    private Object fCurrentSelection;
    
    /** The current java project */
    private IJavaProject fCurrJProject;
    
    public DialogPackageExplorer() {
        fActionGroup= null;
        fCurrentSelection= null;
        fCurrJProject= null;
    }
    
    public Control createControl(Composite parent) {
        fPackageViewer= new TreeViewer(parent, SWT.SINGLE);
        fPackageViewer.setComparer(WorkingSetModel.COMPARER);
        fPackageViewer.addFilter(new PackageFilter());
        fPackageViewer.setSorter(new ExtendedJavaElementSorter());
        fPackageViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                Object element= ((IStructuredSelection)event.getSelection()).getFirstElement();
                if (fPackageViewer.isExpandable(element)) {
                    fPackageViewer.setExpandedState(element, !fPackageViewer.getExpandedState(element));
                }
            }
        });
        fPackageViewer.addSelectionChangedListener(this);
        
        MenuManager menuMgr= new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(this);
        fContextMenu= menuMgr.createContextMenu(fPackageViewer.getTree());
        fPackageViewer.getTree().setMenu(fContextMenu);
        
        return fPackageViewer.getControl();
    }
    
    /**
     * Sets the action group for the package explorer.
     * The action group is necessary to populate the 
     * context menu with available actions. If no 
     * context menu is needed, then this method does not 
     * have to be called.
     *  
     * @param actionGroup the action group to be used for 
     * the context menu.
     */
    public void setActionGroup(DialogPackageExplorerActionGroup actionGroup) {
        fActionGroup= actionGroup;
    }
    
    /**
     * Populate the context menu with the necessary actions.
     * 
     * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
     */
    public void menuAboutToShow(IMenuManager manager) {
        if (fActionGroup == null) // no context menu
            return;
        JavaPlugin.createStandardGroups(manager);
        fActionGroup.fillContextMenu(manager);
    }
    
    /**
     * Set the content and label provider of the
     * <code>fPackageViewer</code>
     */
    public void setContentProvider() {
        PackageContentProvider contentProvider= null;
        PackageLabelProvider labelProvider;
        if (contentProvider == null) {
            contentProvider= new PackageContentProvider();
            labelProvider= new PackageLabelProvider(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED,
            AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS);
            fPackageViewer.setContentProvider(contentProvider);
            fPackageViewer.setLabelProvider(new DecoratingJavaLabelProvider(labelProvider, false));
        }
    }
    
    /**
     * Set the input for the package explorer.
     * 
     * @param project the project to be displayed
     */
    public void setInput(IJavaProject project) {
        fCurrJProject= project;
        fPackageViewer.setInput(new Object());
        ISelection selection= new StructuredSelection(project);
        fPackageViewer.setSelection(selection);
        fPackageViewer.expandToLevel(2);
    }
    
    /**
     * Set the selection and focus to this object
     * @param obj the object to be selected and displayed
     */
    public void setSelection(Object obj) {
        if (obj == null)
            return;
        fPackageViewer.refresh();
        IStructuredSelection selection= new StructuredSelection(obj);
        fPackageViewer.setSelection(selection, true);
        fPackageViewer.getTree().setFocus();
        
        if (obj instanceof IJavaProject)
            fPackageViewer.expandToLevel(obj, 1);
    }
    
    public Object getSelection() {
        return fCurrentSelection;
    }
    
    /**
     * Get the viewer's control
     * 
     * @return the viewers control
     */
    public Control getViewerControl() {
        return fPackageViewer.getControl();
    }
    
    /**
     * Flag to indicate whether output folders
     * can be created or not. This is used to
     * set the content correctly in the case
     * that a IPackageFragmentRoot is selected.
     */
    public boolean isSelected() {
        return fShowOutputFolders;
    }
    
    /**
     * Method that is called whenever setting of 
     * output folders is allowed or forbidden (for example 
     * on changing a checkbox with this setting):
     * 
     * @param showOutputFolders <code>true</code> if output 
     * folders should be shown, <code>false</code> otherwise.
     */
    public void widgetSelected(boolean showOutputFolders) {
        fShowOutputFolders= showOutputFolders;
        fPackageViewer.refresh();
    }
    
    /**
     * Add listeners to the tree viewer to get informed about 
     * a selection changes on the viewer.
     * 
     * @param listener the listener to be registered
     */
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        fPackageViewer.addSelectionChangedListener(listener);
    }
    
    /**
     * Remove a selection listener
     * 
     * @param listener the listener to be removed
     */
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        fPackageViewer.removeSelectionChangedListener(listener);
    }

    /**
     * Inform the <code>fActionGroup</code> about the selection change and store the 
     * latest selection.
     * 
     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent) 
     */
    public void selectionChanged(SelectionChangedEvent event) {
        fCurrentSelection= ((IStructuredSelection)event.getSelection()).getFirstElement();
        if (fActionGroup != null)
            fActionGroup.setContext(new ActionContext(new StructuredSelection(new Object[] {fCurrentSelection, fCurrJProject})));
    }
}
