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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.filters.LibraryFilter;
import org.eclipse.jdt.internal.ui.filters.OutputFolderFilter;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListLabelProvider;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup.DialogExplorerActionContext;
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
            if ((element instanceof IPackageFragmentRoot || (element instanceof IJavaProject && fCurrJProject.isOnClasspath(fCurrJProject))) && isSelected()) {
                try {
                    IClasspathEntry entry;
                    if (element instanceof IPackageFragmentRoot)
                        entry= ((IPackageFragmentRoot) element).getRawClasspathEntry();
                    else
                        entry= ClasspathModifier.getClasspathEntryFor(fCurrJProject.getPath(), fCurrJProject);
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
            String text= super.getText(element);
            try {
                if (element instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot root= (IPackageFragmentRoot)element;
                    if (ClasspathModifier.filtersSet(root)) {
                        IClasspathEntry entry= root.getRawClasspathEntry();
                        int excluded= entry.getExclusionPatterns().length;
                        if (excluded == 1)
                            return NewWizardMessages.getFormattedString("DialogPackageExplorer.LabelProvider.SingleExcluded", text); //$NON-NLS-1$ //$NON-NLS-2$
                        else if (excluded > 1)
                            return NewWizardMessages.getFormattedString("DialogPackageExplorer.LabelProvider.MultiExcluded", new Object[] {text, new Integer(excluded)}); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                if (element instanceof IJavaProject) {
                    IJavaProject project= (IJavaProject)element;
                    if (project.isOnClasspath(project)) {
                        IPackageFragmentRoot root= project.findPackageFragmentRoot(project.getPath());
                        if (ClasspathModifier.filtersSet(root)) {
                            IClasspathEntry entry= root.getRawClasspathEntry();
                            int excluded= entry.getExclusionPatterns().length;
                            if (excluded == 1)
                                return NewWizardMessages.getFormattedString("DialogPackageExplorer.LabelProvider.SingleExcluded", text); //$NON-NLS-1$ //$NON-NLS-2$
                            else if (excluded > 1)
                                return NewWizardMessages.getFormattedString("DialogPackageExplorer.LabelProvider.MultiExcluded", new Object[] {text, new Integer(excluded)}); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                }
                if (element instanceof IFile || element instanceof IFolder) {
                    IResource resource= (IResource)element;
                        if (ClasspathModifier.isExcluded(resource, fCurrJProject))
                            return NewWizardMessages.getFormattedString("DialogPackageExplorer.LabelProvider.Excluded", text); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } catch (JavaModelException e) {
                JavaPlugin.log(e);
            }
            return text;
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider#getForeground(java.lang.Object)
         */
        public Color getForeground(Object element) {
            try {
                if (element instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot root= (IPackageFragmentRoot)element;
                    if (ClasspathModifier.filtersSet(root))
                        return getBlueColor();
                }
                if (element instanceof IJavaProject) {
                    IJavaProject project= (IJavaProject)element;
                    if (project.isOnClasspath(project)) {
                        IPackageFragmentRoot root= project.findPackageFragmentRoot(project.getPath());
                        if (ClasspathModifier.filtersSet(root))
                            return getBlueColor();
                    }
                }
                if (element instanceof IFile || element instanceof IFolder) {
                    IResource resource= (IResource)element;
                    if (ClasspathModifier.isExcluded(resource, fCurrJProject))
                        return getBlueColor();
                } 
            } catch (JavaModelException e) {
                JavaPlugin.log(e);
            }
            return null;
        }
        
        private Color getBlueColor() {
            return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
        }
        
        public Image getImage(Object element) {
            if (element instanceof CPListElementAttribute)
                return outputFolderLabel.getImage(element);
            return super.getImage(element);
        }
        
        public void dispose() {
            outputFolderLabel.dispose();
            super.dispose();
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
                if (root.getElementName().endsWith(".jar") || root.getElementName().endsWith(".zip")) //$NON-NLS-1$ //$NON-NLS-2$
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
     * @see #showOutputFolders(boolean)
     * @see #isSelected()
     */
    private boolean fShowOutputFolders= false;
    
    /** Stores the current selection in the tree 
     * @see #getSelection()
     */
    private List fCurrentSelection;
    
    /** The current java project
     * @see #setInput(IJavaProject)
     */
    private IJavaProject fCurrJProject;
    
    public DialogPackageExplorer() {
        fActionGroup= null;
        fCurrentSelection= new ArrayList();
        fCurrJProject= null;
    }
    
    public Control createControl(Composite parent) {
        fPackageViewer= new TreeViewer(parent, SWT.MULTI);
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
        parent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                fContextMenu.dispose();
            }
        });
        
        return fPackageViewer.getControl();
    }
    
    /**
     * Sets the action group for the package explorer.
     * The action group is necessary to populate the 
     * context menu with available actions. If no 
     * context menu is needed, then this method does not 
     * have to be called.
     * 
     * Should only be called once.
     *  
     * @param actionGroup the action group to be used for 
     * the context menu.
     */
    public void setActionGroup(final DialogPackageExplorerActionGroup actionGroup) {
        fActionGroup= actionGroup;
        fPackageViewer.getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (actionGroup != null)
                    actionGroup.dispose();
            }
        });
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
     * Refresh the project tree
     */
    public void refresh() {
        fPackageViewer.refresh(true);
    }
    
    /**
     * Set the selection and focus to the list of elements
     * @param elements the object to be selected and displayed
     */
    public void setSelection(List elements) {
        if (elements == null || elements.size() == 0)
            return;
        fPackageViewer.refresh();
        IStructuredSelection selection= new StructuredSelection(elements);
        fPackageViewer.setSelection(selection, true);
        fPackageViewer.getTree().setFocus();
        
        if (elements.size() == 1 && elements.get(0) instanceof IJavaProject)
            fPackageViewer.expandToLevel(elements.get(0), 1);
    }
    
    /**
     * The the current list of selected elements. The 
     * list may be empty if no element is selected.
     * 
     * @return a list of elements currently selected 
     * in the tree
     */
    public List getSelection() {
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
    private boolean isSelected() {
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
    public void showOutputFolders(boolean showOutputFolders) {
        fShowOutputFolders= showOutputFolders;
        fPackageViewer.refresh();
    }

    /**
     * Inform the <code>fActionGroup</code> about the selection change and store the 
     * latest selection.
     * 
     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     * @see DialogPackageExplorerActionGroup#setContext(DialogExplorerActionContext)
     */
    public void selectionChanged(SelectionChangedEvent event) {
        fCurrentSelection= ((IStructuredSelection)event.getSelection()).toList();
        try {
            if (fActionGroup != null)
                fActionGroup.setContext(new DialogExplorerActionContext(fCurrentSelection, fCurrJProject));
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
    }
}
