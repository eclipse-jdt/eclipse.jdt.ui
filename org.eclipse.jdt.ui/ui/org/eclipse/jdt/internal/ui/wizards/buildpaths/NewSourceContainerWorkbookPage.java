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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.filters.LibraryFilter;
import org.eclipse.jdt.internal.ui.filters.OutputFolderFilter;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerLabelProvider;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.packageview.WorkingSetAwareContentProvider;
import org.eclipse.jdt.internal.ui.packageview.WorkingSetAwareLabelProvider;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.wizards.ClasspathModifier;
import org.eclipse.jdt.internal.ui.wizards.ExtendedNewFolderDialog;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.ClasspathModifier.IFolderCreationQuery;
import org.eclipse.jdt.internal.ui.wizards.ClasspathModifier.IOutputFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.ClasspathModifier.IOutputLocationQuery;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;

public class NewSourceContainerWorkbookPage extends BuildPathBasePage implements Observer {
    /**
     * A package explorer widget that can be used in dialogs. It uses its own 
     * content provider, label provider, element sorter and filter to display
     * elements that are not shown usually in the package explorer of the
     * workspace.
     */
    private final class PackageExplorer implements SelectionListener{
        /**
         * A extended content provider for the package explorer which can additionally display
         * an output folder item.
         */
        private final class PackageContentProvider extends WorkingSetAwareContentProvider{
            public PackageContentProvider(WorkingSetModel model) {
                super(null, true, model);
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
                        ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.PackageExplorer.PackageContentProvider.Error.Title"), e.getMessage()); //$NON-NLS-1$
                    }
                    return null;
                }
                else
                    return children;                    
            }
            
            public void elementChanged(ElementChangedEvent event) {
                super.elementChanged(event);
            }
        }
        
        /**
         * A extended label provider for the package explorer which can additionally display
         * an output folder item.
         */
        private final class PackageLabelProvider extends WorkingSetAwareLabelProvider {
            private CPListLabelProvider outputFolderLabel;
            
            public PackageLabelProvider(long textFlags, int imageFlags, PackageExplorerContentProvider cp) {
                super(textFlags, imageFlags, cp);
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
                return super.select(viewer, parentElement, element) && fOutputFolderFilter.select(viewer, parentElement, element);
            }
        }
        
        protected TreeViewer fPackageViewer;
        /**
         * Flag to indicate whether output folders
         * can be created or not. This is used to
         * set the content correctly in the case
         * that a IPackageFragmentRoot is selected.
         * 
         * @see #widgetDefaultSelected(SelectionEvent)
         */
        private boolean fIsSelected= false;
        
        public PackageExplorer(Composite parent, int numColumns) {
            createPartControl(parent, numColumns);
        }
        
        private void createPartControl(Composite parent, int numColumns) {
            Composite composite= LayoutHelper.getNewComposite(parent, GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL, numColumns);
            GridData gridData= (GridData)composite.getLayoutData();
            gridData.minimumHeight= 130;
            SashForm sashForm= LayoutHelper.getSashForm(composite);
            sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

            fPackageViewer= new TreeViewer(sashForm, SWT.SINGLE | SWT.BORDER);
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
        }
        
        private WorkingSetAwareLabelProvider createLabelProvider(PackageExplorerContentProvider contentProvider) {
            return new PackageLabelProvider(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED,
                AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS,
                contentProvider);
        }
        
        public void setContentProvider() {
            PackageExplorerContentProvider contentProvider= null;
            PackageExplorerLabelProvider labelProvider;
            WorkingSetModel workingSetModel= new WorkingSetModel();
            if (contentProvider == null) {
                contentProvider= new PackageContentProvider(workingSetModel);
                labelProvider= createLabelProvider(contentProvider);
                boolean isFlatLayout;
                if (PackageExplorerPart.getFromActivePerspective() == null)
                    isFlatLayout= true;
                else
                    isFlatLayout= PackageExplorerPart.getFromActivePerspective().isFlatLayout();
                labelProvider.setIsFlatLayout(isFlatLayout);
                contentProvider.setIsFlatLayout(isFlatLayout);
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
            fPackageViewer.setInput(new Object());
            fPackageViewer.expandToLevel(2);
            ISelection selection= new StructuredSelection(fCurrJProject);
            fPackageViewer.setSelection(selection);
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
            
            if (obj instanceof IResource) {
                if (((IResource)obj).getFileExtension() == null)
                    fPackageViewer.expandToLevel(obj, 1);
            }
            if (obj instanceof IPackageFragment || obj instanceof IPackageFragmentRoot || obj instanceof IJavaProject)
                fPackageViewer.expandToLevel(obj, 1);
        }
        
        public void setListener(ISelectionChangedListener listener) {
            fPackageViewer.addSelectionChangedListener(listener);
        }
        
        public boolean isSelected() {
            return fIsSelected;
        }

        public void widgetSelected(SelectionEvent e) {
            fIsSelected= !fIsSelected;
            fPackageViewer.refresh();
        }

        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }
    
    /**
     * A helper class to create various composites with the same style.
     */
    private static final class LayoutHelper {
        private static SashForm getSashForm(Composite parent) {
            SashForm sashForm= new SashForm(parent, SWT.HORIZONTAL | SWT.NONE);
            sashForm.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
            return sashForm;
        }
        
        private static Composite getNewComposite(Composite parent, int style, int horizontalSpan) {
            Font font= parent.getFont();
            Composite composite= new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout());
            GridData gridData= new GridData(style);
            gridData.horizontalSpan= horizontalSpan;
            composite.setLayoutData(gridData);
            composite.setFont(font);
            return composite;
        }
        
        /*private static Button createButton(String text, int style, Composite parent) {
            Button button= new Button(parent, style);
            button.setText(text);
            GridData gridData= new GridData();
            gridData.horizontalAlignment= GridData.FILL;
            gridData.grabExcessHorizontalSpace= true;
            button.setLayoutData(gridData);
            return button;
        }*/
    }
    
    /**
     * Displays a set of available links to modify or adjust the project.
     * The links contain a short description about the action that it
     * will perform.
     * The set's content depends on the selection made on the project.
     */
    public final class HintTextGroup implements ISelectionChangedListener, SelectionListener {
        /** Java project */
        public static final int JAVA_PROJECT= 0x01;
        /** Package fragment root */
        public static final int PACKAGE_FRAGMENT_ROOT= 0x02;
        /** Package fragment */
        public static final int PACKAGE_FRAGMENT= 0x03;
        /** Compilation unit */
        public static final int COMPILATION_UNIT= 0x04;
        /** File */
        public static final int FILE= 0x05;
        /** Normal folder */
        public static final int FOLDER= 0x06;
        /** Excluded folder */
        public static final int EXCLUDED_FOLDER= 0x07;
        /** Excluded file */
        public static final int EXCLUDED_FILE= 0x08;
        /** Default output folder */
        public static final int DEFAULT_OUTPUT= 0x09;
        /** Included file */
        public static final int INCLUDED_FILE= 0x10;
        /** Included folder */
        public static final int INCLUDED_FOLDER= 0x11;
        /** Output folder (for a source folder) */
        public static final int OUTPUT= 0x12;
        /** A IPackageFragmentRoot with include/exclude filters set */
        public static final int MODIFIED_FRAGMENT_ROOT= 0x13;
        /** Default package fragment */
        public static final int DEFAULT_FRAGMENT= 0x14;
        /** Undefined type */
        public static final int UNDEFINED= 0x15;
        
        private Group fGroup;
        private Composite fGroupComposite;
        private int fLastSelectionType= UNDEFINED;
        private ClasspathModifier fInternalModifier;
        private Object fSelection;
        private IRunnableContext fContext;
        private Image fEdit= null;
        private Image fNewFolder= null;
        private Image fNewOutputFolder= null;
        
        public HintTextGroup(Composite parent, ClasspathModifier modifier, IRunnableContext context) {
            fGroup= new Group(parent, SWT.NONE);
            GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
            gridData.heightHint= 155;
            fGroup.setLayoutData(gridData);
            fGroup.setLayout(new GridLayout());
            fGroup.setText(NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.title")); //$NON-NLS-1$
            
            fInternalModifier= modifier;
            fContext= context;
        }
        
        /**
         * Creates the area of the <code>fGroup</code>
         * where the links are put on.
         */
        private void createGroupArea() {
            fGroupComposite= new Composite(fGroup, SWT.NONE);
            fGroupComposite.setLayout(new TableWrapLayout());
            fGroupComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        }
        
        /**
         * Rebuilds the are of the <code>fGroup</code>.
         * This includes disposing the old one and
         * create a fresh area. 
         *
         *@see #createGroupArea()
         */
        private void rebuildGroupArea() {
            if (fGroupComposite != null)
                fGroupComposite.dispose();
            createGroupArea();
        }
        
        /**
         * Creates a form text.
         * 
         * @param parent the parent to put the form text on
         * @param text the form text to be displayed
         * @return the created form text
         * 
         * @see FormToolkit#createFormText(org.eclipse.swt.widgets.Composite, boolean)
         */
        private FormText createFormText(Composite parent, String text) {
            FormToolkit toolkit= new FormToolkit(getShell().getDisplay());
            FormText formText = toolkit.createFormText(parent, true);
            try {
                formText.setText(text, true, false);
            } catch (SWTException e) {
                formText.setText(e.getMessage(), false, false);
            }
            formText.setBackground(null);
            formText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
            toolkit.dispose();
            return formText;
        }
        
        /**
         * Create a new label containing a link to create a new folder.
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ExtendedNewFolderDialog
         */
        private void createNewFolderLabel(Composite parent, final ClasspathModifier modifier, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.create")); //$NON-NLS-1$
            if (fNewFolder == null)
                fNewFolder= JavaPluginImages.DESC_TOOL_NEWPACKROOT.createImage();
            formText.setImage("create", fNewFolder); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final Object[] result= new Object[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                int type= getType(getSelection());
                                IFolderCreationQuery folderQuery= modifier.getDefaultFolderCreationQuery(getShell(), getSelection(), type);
                                IOutputFolderQuery outputQuery= modifier.getDefaultFolderQuery(getShell());
                                result[0]= modifier.createFolder(folderQuery, outputQuery, fCurrJProject, null);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fNewFolders.add(result[0]);
                            fPackageExplorer.setSelection(result[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            });
        }
        
        /**
         * Create a new label containing a link to edit the filters of an 
         * <code>IPackageFragmentRoot<code>.
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ExclusionInclusionDialog
         */
        private void createEditLabel(Composite parent, final ClasspathModifier modifier, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.Edit")); //$NON-NLS-1$
            if (fEdit == null)
                fEdit= JavaPluginImages.DESC_OBJS_TEXT_EDIT.createImage();
            formText.setImage("edit", fEdit); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final Object[] result= new Object[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                IJavaElement element= (IJavaElement)getSelection();
                                result[0]= modifier.editFilters(element, fCurrJProject, modifier.getDefaultInclusionExclusionQuery(getShell()), monitor);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            });
        }
        
        /*private void createDeleteLabel(Composite parent, String type) {
            FormText formText= createFormText(parent, NewWizardMessages.getFormattedString("JavaProjectWizardFoldersPage.HintTextGroup.delete", type)); //$NON-NLS-1$
            if (fDelete == null)
                fDelete= JavaPluginImages.DESC_REMOVE.createImage();
            formText.setImage("delete", fDelete); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    IStructuredSelection selection= getSelection();
                    
                    DeleteAction deleteAction= new DeleteAction(PackageExplorerPart.getFromActivePerspective().getSite());
                    deleteAction.run(selection); 
                    
                    Object element= selection.getFirstElement();
                    IResource resource= null;
                    
                    try {
                        if (element instanceof IJavaElement)
                            resource= ((IJavaElement)element).getCorrespondingResource();
                        else
                            resource= (IResource) element;
                    } catch (JavaModelException err) {
                        JavaPlugin.log(err);
                    }                    
                    
                    if (resource == null || !resource.exists()) {
                        fPackageExplorer.setSelection(fCurrJProject);
                        if (element instanceof IJavaElement)
                            fModifier.removeFromFilters(((IJavaElement)element).getPath());
                    }
                }
            });
        }*/
        
        /**
         * Create a new label containing a link to remove an <code>
         * IPackageFragmentRoot</code> from the classpath.
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#removeFromClasspath(IPath, IJavaProject, IProgressMonitor)
         */
        private void createFromClasspathLabel(Composite parent, final ClasspathModifier modifier, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.fromClasspath")); //$NON-NLS-1$
            formText.setImage("fromCP", JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REMOVE_FROM_CP)); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final Object[] result= new Object[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            Object element= getSelection();
                            result[0]= modifier.removeFromClasspath(((IPackageFragmentRoot)element).getPath(), fCurrJProject, monitor);
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            });
        }
        
        /**
         * Create a new label containing a link to add an
         * element to the classpath.
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#addToClasspath(IFolder, IJavaProject, IOutputFolderQuery, IProgressMonitor)
         * @see ClasspathModifier#addToClasspath(IJavaElement, IJavaProject, IOutputFolderQuery, IProgressMonitor)
         */
        private void createToClasspathLabel(Composite parent, final ClasspathModifier modifier, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.toClasspath")); //$NON-NLS-1$
            formText.setImage("toCP", JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ADD_TO_CP)); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final IPackageFragmentRoot[] result= new IPackageFragmentRoot[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                Object element= getSelection();
                                if (element instanceof IFolder)
                                    result[0]= modifier.addToClasspath((IFolder)element, fCurrJProject, modifier.getDefaultFolderQuery(getShell()), monitor);
                                else
                                    result[0]= modifier.addToClasspath((IJavaElement)element, fCurrJProject, modifier.getDefaultFolderQuery(getShell()), monitor);
                                
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            });     
        }
        
        /**
         * Create a new label containing a link to include
         * an element. This means setting the inclusion filter
         * of the source folder parent properly  
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#include(Object, IJavaProject, IProgressMonitor)
         */
        private void createIncludeLabel(Composite parent, final ClasspathModifier modifier, String type, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getFormattedString("JavaProjectWizardFoldersPage.HintTextGroup.Include", type)); //$NON-NLS-1$
            formText.setImage("include", JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD)); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                final IJavaElement[] result= new IJavaElement[1];
                public void linkActivated(HyperlinkEvent e) {
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                Object selection= getSelection();
                                result[0]= modifier.include(selection, fCurrJProject, monitor);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                            forceAreaRebuild(getType(new StructuredSelection(result[0])));
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            });  
        }
        
        /**
         * Create a new label containing a link to exclude
         * an element. This means setting the exclusion filter
         * of the source folder parent properly  
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#exclude(IJavaElement, IJavaProject, IProgressMonitor)
         */
        private void createExcludeLabel(Composite parent, final ClasspathModifier modifier, String text, final IRunnableContext context) {
            FormText formText= createFormText(parent, text); //$NON-NLS-1$
            formText.setImage("exclude", JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE)); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final Object[] result= new Object[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                IJavaElement javaElem= (IJavaElement)getSelection();
                                result[0]= modifier.exclude(javaElem, fCurrJProject, monitor);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            });  
        }
        
        /**
         * Create a new label containing a link to unexclude
         * an element. This is the inverse operation to
         * exclusion.  
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#unExclude(IResource, IJavaProject, IProgressMonitor)
         * @see ClasspathModifier#exclude(IJavaElement, IJavaProject, IProgressMonitor)
         */
        private void createUnexcludeLabel(Composite parent, final ClasspathModifier modifier, String text, final IRunnableContext context) {
            FormText formText= createFormText(parent, text); //$NON-NLS-1$
            formText.setImage("unexclude", JavaPluginImages.get(JavaPluginImages.IMG_UNDO)); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final Object[] result= new Object[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                IResource resource= (IResource)getSelection();
                                result[0]= modifier.unExclude(resource, fCurrJProject, monitor);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                        }
                        forceAreaRebuild(getType(result[0]));
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            });  
        }
        
        /**
         * Create a new label containing a link to uninclude
         * an element. This is the inverse operation to
         * inclusion.  
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#unInclude(IJavaElement, IJavaProject, IProgressMonitor)
         * @see ClasspathModifier#include(Object, IJavaProject, IProgressMonitor)
         */
        private void createUnincludeLabel(Composite parent, final ClasspathModifier modifier, String type, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getFormattedString("JavaProjectWizardFoldersPage.HintTextGroup.Uninclude", type)); //$NON-NLS-1$
            formText.setImage("uninclude", JavaPluginImages.get(JavaPluginImages.IMG_UNDO)); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final Object[] result= new Object[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                IJavaElement javaElement= (IJavaElement)getSelection();
                                result[0]= modifier.unInclude(javaElement, fCurrJProject, monitor);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            }); 
        }
        
        /**
         * Create a new label containing a link to reset the filtes
         * for inclusion or exclusion of an <code>IPackageFragmentRoot
         * </code>.
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#resetFilters(IPackageFragmentRoot, IJavaProject, IProgressMonitor)
         */
        private void createResetFilterLabel(Composite parent, final ClasspathModifier modifier, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.ResetFilters")); //$NON-NLS-1$
            formText.setImage("resetFilters", JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLEAR)); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final Object[] result= new Object[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                IJavaElement element= (IJavaElement)getSelection();
                                result[0]= modifier.resetFilters(element, fCurrJProject, monitor);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            }); 
        }
        
        /**
         * Create a new label containing a link to create a new output
         * folder for a <code>IPackageFragmentRoot</code>.
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#createOutputFolder(IPackageFragmentRoot, String, IOutputLocationQuery, IJavaProject, IProgressMonitor)
         */
        private void createOutputFolderLabel(Composite parent, final ClasspathModifier modifier, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.SetOutputFolder")); //$NON-NLS-1$
            if (fNewOutputFolder == null)
                fNewOutputFolder= JavaPluginImages.DESC_OBJS_OUTPUT_FOLDER_ATTRIB.createImage();
            formText.setImage("setOutputFolder", fNewOutputFolder); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final CPListElementAttribute[] outputFolder= new CPListElementAttribute[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                IPackageFragmentRoot root= (IPackageFragmentRoot)getSelection();
                                outputFolder[0]= modifier.createOutputFolder(root, fOutputLocationField.getText(), modifier.getDefaultOutputLocationQuery(getShell()), 
                                        fCurrJProject, monitor);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (outputFolder[0] != null) {
                            fUseFolderOutputs.setSelection(true);
                            fPackageExplorer.widgetSelected(null);
                            fPackageExplorer.setSelection(outputFolder[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            }); 
        }
        
        /**
         * Create a new label containing a link to edit an output folder.
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#editOutputFolder(CPListElement, String, IJavaProject, IOutputLocationQuery, IProgressMonitor)
         */
        private void createOutputEditLabel(Composite parent, final ClasspathModifier modifier, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.EditOutputFolder")); //$NON-NLS-1$
            if (fEdit == null)
                fEdit= JavaPluginImages.DESC_OBJS_TEXT_EDIT.createImage();
            formText.setImage("editOutputFolder", fEdit); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                CPListElementAttribute[] result= new CPListElementAttribute[1];
                public void linkActivated(HyperlinkEvent e) {
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            CPListElement selElement=  ((CPListElementAttribute)getSelection()).getParent();
                            try {
                                result[0]= modifier.editOutputFolder(selElement, fOutputLocationField.getText(), 
                                        fCurrJProject, modifier.getDefaultOutputLocationQuery(getShell()), monitor);
                            } catch (JavaModelException err) {
                                throw new InvocationTargetException(err);
                            }
                        }
                    };
                    try {
                        context.run(false, false, op);
                        fPackageExplorer.setSelection(result[0]);
                        
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            }); 
        }
        
        /**
         * Create a new label containing a link to reset an output
         * folder to the default output folder of the project.
         * 
         * @param parent parent to create the label on
         * @param modifier modifier to apply changes to the classpath and/or filters
         * 
         * @see ClasspathModifier#resetOutputFolder(IClasspathEntry, IJavaProject, IProgressMonitor)
         */
        private void createOutputToDefaultLabel(Composite parent, final ClasspathModifier modifier, final IRunnableContext context) {
            FormText formText= createFormText(parent, NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.SetOutputToDefault")); //$NON-NLS-1$
            formText.setImage("setOutputToDefault", JavaPluginImages.get(JavaPluginImages.IMG_UNDO)); //$NON-NLS-1$
            formText.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    final CPListElementAttribute[] result= new CPListElementAttribute[1];
                    IRunnableWithProgress op= new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            CPListElement selElement=  ((CPListElementAttribute)getSelection()).getParent();
                            result[0]= modifier.resetOutputFolder(selElement.getClasspathEntry(), fCurrJProject, monitor);
                        }
                    };
                    try {
                        context.run(false, false, op);
                        if (result[0] != null) {
                            fPackageExplorer.setSelection(result[0]);
                        }
                    } catch (InvocationTargetException err) {
                        ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    } catch  (InterruptedException err) {
                        // cancel pressed
                    }
                }
            }); 
        }
        
        /**
         * Create lables for java project selection
         */
        private void javaProjectSelected() {
            rebuildGroupArea();
            createNewFolderLabel(fGroupComposite, fInternalModifier, fContext);
            try {
                if (fCurrJProject.isOnClasspath(fCurrJProject.getUnderlyingResource())) {
                    createEditLabel(fGroupComposite, fInternalModifier, fContext);
                    IClasspathEntry entry= fInternalModifier.getClasspathEntryFor(fCurrJProject.getPath(), fCurrJProject);
                    if (entry.getInclusionPatterns().length != 0 || entry.getExclusionPatterns().length != 0)
                        createResetFilterLabel(fGroupComposite, fInternalModifier, fContext);
                }
            } catch (JavaModelException e) {
                ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), e.getMessage()); //$NON-NLS-1$
            }                
        }
        
        /**
         * Create lables for fragment root selection
         * 
         * @param filtersSet <code>true</code> if fragment root
         * has inclusion and/or exclusion filters set, <code>
         * false</code> otherwise.
         */
        private void fragmentRootSelected(boolean filtersSet) {
            rebuildGroupArea();
            createNewFolderLabel(fGroupComposite, fInternalModifier, fContext);
            if (!fPackageExplorer.isSelected())
                createOutputFolderLabel(fGroupComposite, fInternalModifier, fContext);
            createEditLabel(fGroupComposite, fInternalModifier, fContext);
            createFromClasspathLabel(fGroupComposite, fInternalModifier, fContext);
            if (filtersSet)
                createResetFilterLabel(fGroupComposite, fInternalModifier, fContext);
//            createDeleteLabel(fGroupComposite, "folder"); //$NON-NLS-1$
        }
        
        /**
         * Create lables for fragment selection
         * 
         * @param included <code>true</code> if fragment
         * is included (this is set on the inclusion filter of its parent
         * source folder, </code>false</code> otherwise.
         * @param isDefaultFragment <code>true</code> if fragment
         * is the project's default fragment, </code>false</code> otherwise.
         */
        private void fragmentSelected(boolean included, boolean isDefaultFragment) {
            rebuildGroupArea();
            if (isDefaultFragment)
                return;
            createNewFolderLabel(fGroupComposite, fInternalModifier, fContext);
            createToClasspathLabel(fGroupComposite, fInternalModifier, fContext);
            if (included)
                createUnincludeLabel(fGroupComposite, fInternalModifier, "package", fContext); //$NON-NLS-1$
            else
                createIncludeLabel(fGroupComposite, fInternalModifier, "folder", fContext); //$NON-NLS-1$
            createExcludeLabel(fGroupComposite, fInternalModifier, 
                    NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.ExcludeFolder"), fContext); //$NON-NLS-1$
//            createDeleteLabel(fGroupComposite, "folder"); //$NON-NLS-1$
        }
        
        /**
         * Create lables for file selection
         * 
         * @param included <code>true</code> if fragment
         * is included (this is set on the inclusion filter of its parent
         * source folder, </code>false</code> otherwise.
         */
        private void javaFileSelected(boolean included) {
            rebuildGroupArea();
            if (included)
                createUnincludeLabel(fGroupComposite, fInternalModifier, "file", fContext); //$NON-NLS-1$
            else
                createIncludeLabel(fGroupComposite, fInternalModifier, "file", fContext); //$NON-NLS-1$
            createExcludeLabel(fGroupComposite, fInternalModifier, 
                    NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.ExcludeFile"), fContext); //$NON-NLS-1$
//            createDeleteLabel(fGroupComposite, "file"); //$NON-NLS-1$
        }
        
        /**
         * Create lables for folder selection
         */
        private void folderSelected() {
            rebuildGroupArea();
            createNewFolderLabel(fGroupComposite, fInternalModifier, fContext);
            createToClasspathLabel(fGroupComposite, fInternalModifier, fContext);
            IResource resource= (IResource)getSelection();
            try {
                if (fCurrJProject.isOnClasspath(fCurrJProject.getUnderlyingResource()) && resource.getProjectRelativePath().segmentCount() == 1 && 
                        fInternalModifier.getFragmentRoot(resource, fCurrJProject, null).equals(fInternalModifier.getFragmentRoot(fCurrJProject.getCorrespondingResource(), fCurrJProject, null))) {
                    createIncludeLabel(fGroupComposite, fInternalModifier, 
                            NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.UnexcludeFolder"), fContext); //$NON-NLS-1$
                    if (fInternalModifier.isExcludedOnProject(resource, fCurrJProject))
                        createUnexcludeLabel(fGroupComposite, fInternalModifier, 
                                NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.UnexcludeFolder"), fContext); //$NON-NLS-1$
                }
//            createDeleteLabel(fGroupComposite, "folder"); //$NON-NLS-1$
            } catch (JavaModelException e) {
                ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), e.getMessage()); //$NON-NLS-1$
            }
        }
        
        /**
         * Create lables for excluded folder selection
         */
        private void excludedFolderSelected() {
            rebuildGroupArea();
            createNewFolderLabel(fGroupComposite, fInternalModifier, fContext);
            createToClasspathLabel(fGroupComposite, fInternalModifier, fContext);
//            if (includeFiltersEmpty())
            if (fInternalModifier.includeFiltersEmpty((IResource) getSelection(), fCurrJProject, null))
                createUnexcludeLabel(fGroupComposite, fInternalModifier, 
                        NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.UnexcludeFolder"), fContext); //$NON-NLS-1$
            createIncludeLabel(fGroupComposite, fInternalModifier, 
                    NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.UnexcludeFolder"), fContext); //$NON-NLS-1$
//            createDeleteLabel(fGroupComposite, "folder"); //$NON-NLS-1$
        }
        
        /**
         * Create lables for file selection which is not
         * an excluded or included file or an ICompilation unit.
         */
        private void fileSelected() {
            rebuildGroupArea();
//            createDeleteLabel(fGroupComposite, "file"); //$NON-NLS-1$
        }
        
        /**
         * Create lables for output folder selection
         * 
         * @param isDefaultOutput <code>true</code> if output folder
         * is the project's default output folder, </code>false</code> otherwise.
         */
        private void outputFolderSelected(boolean isDefaultOutput) {
            rebuildGroupArea();
            createOutputEditLabel(fGroupComposite, fInternalModifier, fContext);
            if (!isDefaultOutput)
                createOutputToDefaultLabel(fGroupComposite, fInternalModifier, fContext);
        }
        
        /**
         * Create lables for excluded file selection
         */
        private void excludedFileSelected() {
            rebuildGroupArea();
            IFile file= (IFile)getSelection();
            IPackageFragment fragment= fInternalModifier.getFragment(file);
            try {
                if (fragment != null || (file.getParent().getFullPath().equals(fCurrJProject.getPath()) && fInternalModifier.isExcludedOnProject(file, fCurrJProject)))
                        createUnexcludeLabel(fGroupComposite, fInternalModifier, 
                                NewWizardMessages.getString("JavaProjectWizardFoldersPage.HintTextGroup.UnexcludeFile"), fContext); //$NON-NLS-1$
            } catch (JavaModelException e) {
                ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), e.getMessage()); //$NON-NLS-1$
            }
            createIncludeLabel(fGroupComposite, fInternalModifier, "file", fContext); //$NON-NLS-1$
//            createDeleteLabel(fGroupComposite, "file"); //$NON-NLS-1$ 
        }
        
        /**
         * Returns the current selection.
         * @return the current selection
         * 
         * @see #selectionChanged(SelectionChangedEvent)
         */
        public Object getSelection() {
            return fSelection;
        }
        
        /**
         * Check whether the current selection of type <code>
         * IPackageFragmentRoot</code> has either it's inclusion or
         * exclusion filter or both set.
         * @return <code>true</code> inclusion or exclusion filter set,
         * <code>false</code> otherwise.
         */
        // TODO move to CPModifier
        private boolean filtersSet() {
            try {
                IPackageFragmentRoot root= (IPackageFragmentRoot)getSelection();
                IClasspathEntry entry= root.getRawClasspathEntry();
                IPath[] inclusions= entry.getInclusionPatterns();
                IPath[] exclusions= entry.getExclusionPatterns();
                if (inclusions != null && inclusions.length > 0)
                    return true;
                if (exclusions != null && exclusions.length > 0)
                    return true;
                return false;
            } catch (JavaModelException e) {
                ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), e.getMessage()); //$NON-NLS-1$
            }
            return false;
        }
        
        /**
         * On selection change, rebuild the content of the hint text group.
         * The content is not rebuilt if the new selection type is the
         * same as the previous one.
         * 
         * @param event selection event
         */
        public void selectionChanged(SelectionChangedEvent event) {
            fSelection= ((IStructuredSelection)event.getSelection()).getFirstElement();
            
            int type= getType(fSelection);
            if (type == fLastSelectionType)
                return;
            
            fLastSelectionType= type;
            if (type == UNDEFINED)
                rebuildGroupArea();
            else
                forceAreaRebuild(type);
        }
        
        /**
         * Forces the hint text group to recompute its content based
         * on the selected type.
         * @param type the selection type Possible types are:<br>
         * HintTextGroup.FOLDER <br>
         * HintTextGroup.EXCLUDED_FOLDER;<br>
         * HintTextGroup.EXCLUDED_FILE<br>
         * HintTextGroup.DEFAULT_OUTPUT<br>
         * HintTextGroup.INCLUDED_FILE<br>
         * HintTextGroup.INCLUDED_FOLDER<br>
         * HintTextGroup.OUTPUT<br>
         * HintTextGroup.MODIFIED_FRAGMENT_ROOT<br>
         * HintTextGroup.DEFAULT_FRAGMENT<br>
         * HintTextGroup.JAVA_PROJECT<br>
         * HintTextGroup.PACKAGE_FRAGMENT_ROOT<br>
         * HintTextGroup.PACKAGE_FRAGMENT<br>
         * HintTextGroup.COMPILATION_UNIT<br>
         * HintTextGroup.FILE<br>
         */
        public void forceAreaRebuild(int type) {
            switch (type) {
                case JAVA_PROJECT: javaProjectSelected(); break;
                case PACKAGE_FRAGMENT_ROOT: fragmentRootSelected(false); break;
                case PACKAGE_FRAGMENT: fragmentSelected(false, false); break;
                case COMPILATION_UNIT: javaFileSelected(false); break;
                case FOLDER: folderSelected(); break;
                case EXCLUDED_FOLDER: excludedFolderSelected(); break;
                case EXCLUDED_FILE: excludedFileSelected(); break;
                case FILE: fileSelected(); break;
                case INCLUDED_FILE: javaFileSelected(true); break;
                case INCLUDED_FOLDER: fragmentSelected(true, false); break;
                case DEFAULT_OUTPUT: outputFolderSelected(true); break;
                case OUTPUT: outputFolderSelected(false); break;
                case MODIFIED_FRAGMENT_ROOT: fragmentRootSelected(true); break;
                case DEFAULT_FRAGMENT: fragmentSelected(false, true); break;
                default: break;
            }
            fGroup.layout(true);
        }
        
        /**
         * Computes the type based on the current selection. The type
         * can be usefull to set the content of the hint text group
         * properly.
         * @see #forceAreaRebuild(int)
         * 
         * @param obj the object to get the type from
         * @return the type of the current selection or UNDEFINED if no
         * appropriate type could be found. Possible types are:<br>
         * HintTextGroup.FOLDER<br>
         * HintTextGroup.EXCLUDED_FOLDER;<br>
         * HintTextGroup.EXCLUDED_FILE<br>
         * HintTextGroup.DEFAULT_OUTPUT<br>
         * HintTextGroup.INCLUDED_FILE<br>
         * HintTextGroup.INCLUDED_FOLDER<br>
         * HintTextGroup.OUTPUT<br>
         * HintTextGroup.MODIFIED_FRAGMENT_ROOT<br>
         * HintTextGroup.DEFAULT_FRAGMENT<br>
         * HintTextGroup.JAVA_PROJECT<br>
         * HintTextGroup.PACKAGE_FRAGMENT_ROOT<br>
         * HintTextGroup.PACKAGE_FRAGMENT<br>
         * HintTextGroup.COMPILATION_UNIT<br>
         * HintTextGroup.FILE<br>
         */
        public int getType(Object obj) {
            if (obj instanceof IJavaProject)
                return JAVA_PROJECT;
            if (obj instanceof IPackageFragmentRoot)
                return filtersSet() ? MODIFIED_FRAGMENT_ROOT : PACKAGE_FRAGMENT_ROOT;
            if (obj instanceof IPackageFragment) {
                if (fInternalModifier.isDefaultFragment((IPackageFragment)obj))
                    return DEFAULT_FRAGMENT;
                if (fInternalModifier.containsPath((IJavaElement)getSelection(), fCurrJProject, null))
                    return INCLUDED_FOLDER;
                return PACKAGE_FRAGMENT;
            }
            if (obj instanceof ICompilationUnit)
                return fInternalModifier.containsPath((IJavaElement)getSelection(), fCurrJProject, null) ? INCLUDED_FILE : COMPILATION_UNIT;
            if (obj instanceof IFolder) {
                return getFolderType((IFolder)obj);
            }
            if (obj instanceof IFile)
                return getFileType((IFile)obj);
            if (obj instanceof CPListElementAttribute)
                return fInternalModifier.isDefaultOutputFolder((CPListElementAttribute) obj) ? DEFAULT_OUTPUT : OUTPUT;
            return UNDEFINED;
        }
        
        /**
         * Get the type of the folder
         * @param folder folder to get the type from
         * @return the type code for the folder. Possible types are:<br>
         * HintTextGroup.FOLDER<br>
         * HintTextGroup.EXCLUDED_FOLDER;<br>
         */
        private int getFolderType(IFolder folder) {
            if (folder.getParent().getFullPath().equals(fCurrJProject.getPath()))
                return FOLDER;
            if (fInternalModifier.getFragment(folder) != null)
                return EXCLUDED_FOLDER;
            try {
                if (fInternalModifier.getFragmentRoot(folder, fCurrJProject, null) == null)
                    return FOLDER;
                if (fInternalModifier.getFragmentRoot(folder, fCurrJProject, null).equals(JavaCore.create(folder.getParent())))
                    return EXCLUDED_FOLDER;
            } catch (JavaModelException e) {
                ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), e.getMessage()); //$NON-NLS-1$
            }
            return FOLDER;              
        }
        
        /**
         * Get the type of the file
         * @param file file to get the type from
         * @return the type code for the file. Possible types are:<br>
         * HintTextGroup.EXCLUDED_FILE<br>
         * HintTextGroup.FILE
         */
        private int getFileType(IFile file) {
            try {
                if (!file.getName().endsWith(".java")) //$NON-NLS-1$
                    return FILE;
                if (file.getParent().getFullPath().equals(fCurrJProject.getPath())) {
                    if (fCurrJProject.isOnClasspath(fCurrJProject.getUnderlyingResource())) //$NON-NLS-1$
                        return EXCLUDED_FILE;
                    return FILE;
                }
                if (fInternalModifier.getFragmentRoot(file, fCurrJProject, null) == null)
                    return FILE;
                if (fInternalModifier.getFragmentRoot(file, fCurrJProject, null).equals(JavaCore.create(file.getParent())))
                    return EXCLUDED_FILE;
                if (fInternalModifier.getFragment(file) == null) {
                    if (fInternalModifier.parentExcluded(file.getParent(), fCurrJProject))
                        return FILE;
                    return EXCLUDED_FILE;
                }
                return EXCLUDED_FILE;
            } catch (JavaModelException e) {
                ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), e.getMessage()); //$NON-NLS-1$
            }
            return UNDEFINED;
        }
        
        /*private int getSelectedType(Object element) {
            if (element instanceof IJavaProject)
                return ExtendedNewFolderDialog.PROJECT;
            if (element instanceof IPackageFragmentRoot)
                return ExtendedNewFolderDialog.FRAGMENT_ROOT;
            if (element instanceof IPackageFragment) 
                return ExtendedNewFolderDialog.FRAGMENT;
            if (element instanceof IFolder)
                return ExtendedNewFolderDialog.FOLDER;
            return UNDEFINED;
        }*/
        
        /**
         * Dispose the images at the end to free
         * resources.
         */
        protected void finalize() {
            disposeImage(fNewFolder);
            disposeImage(fEdit);
            disposeImage(fNewOutputFolder);
        }
        
        private void disposeImage(Image img) {
            if (img != null)
                img.dispose();
        }

        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent e) {
            int type= getType(getSelection());
            if (type == MODIFIED_FRAGMENT_ROOT || type == PACKAGE_FRAGMENT_ROOT)
                forceAreaRebuild(type);
        }

        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }
    
    private ListDialogField fClassPathList;
    private List originalState;
    private IJavaProject fCurrJProject;
    
    private ClasspathModifier fModifier;
    private PackageExplorer fPackageExplorer;
    private HintTextGroup fHintTextGroup;
    private StringDialogField fOutputLocationField;
    private SelectionButtonDialogField fUseFolderOutputs;
    
    private List fNewFolders;
    
    private Control fSWTControl;
    
    private IDocument fCPContent;
    private IDocument fProjectContent;
    private IRunnableContext fRunnableContext;
    
    // TODO complete javadoc
    /**
     * @param classPathList
     * @param outputLocationField
     * @param context a runnable context, can be <code>null</code>
     */
    public NewSourceContainerWorkbookPage(ListDialogField classPathList, StringDialogField outputLocationField, IRunnableContext context) {
        fClassPathList= classPathList;
        originalState= fClassPathList.getElements();
        fNewFolders= new ArrayList();
        fCurrJProject= null;
        fRunnableContext= context;
    
        fOutputLocationField= outputLocationField;
        fUseFolderOutputs= new SelectionButtonDialogField(SWT.CHECK);
        fUseFolderOutputs.setSelection(false);
        fUseFolderOutputs.setLabelText(NewWizardMessages.getString("SourceContainerWorkbookPage.folders.check")); //$NON-NLS-1$
        
        fSWTControl= null;
    }
    
    /**
     * Initializes the controls displaying
     * the content of the java project.
     * 
     * @param jproject the current java project
     */
    public void init(IJavaProject jproject) {
        fCurrJProject= jproject;
        saveFile(fCurrJProject.getProject());
        fPackageExplorer.setContentProvider();
        fPackageExplorer.setInput(fCurrJProject);
        if (new ClasspathModifier().hasOutputFolders(fCurrJProject, null)) {
            fUseFolderOutputs.setSelection(true);
            fPackageExplorer.widgetSelected(null);
        }
        else
            fUseFolderOutputs.setSelection(false);
    }
    
    /**
     * Create a copy of the files ".classpath" and
     * ".project" to restore later in case of an
     * undo operation
     * 
     * @param project the current project
     * 
     * @see #restoreFile(IFile, IDocument)
     * @see #undoAll()
     */
    private void saveFile(IProject project) {
        IFile cPFile= project.getFile(".classpath"); //$NON-NLS-1$
        IFile projectFile= project.getFile(".project"); //$NON-NLS-1$
        fCPContent= null;
        fProjectContent= null;
        if (cPFile.exists())
            fCPContent= getDocument(cPFile.getFullPath());
        if (projectFile.exists())
            fProjectContent= getDocument(projectFile.getFullPath());
    }
    
    /**
     * Restore the previously saved content in
     * <code>document</code> in the given <code>
     * file</code>
     * 
     * @param file the file to store the content to; if
     * <code>file.exists()</code> then the file is deleted
     * first.
     * 
     * @param document contains the content to be
     * stored to the given <code>file</code>
     */
    private void restoreFile(IFile file, IDocument document) {
        if (document == null && file.exists()) {
            try {
                file.delete(true, null);
            } catch (CoreException e) {
                ExceptionHandler.handle(e, getShell(), 
                        NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.CoreException.Title"),  //$NON-NLS-1$
                        e.getMessage());
            }
            return;
        }
            
        if (document == null || file == null || !file.exists())
            return;
        try {
            file.delete(true, null);
            if (file.getLocation() == null)
                return;
            File f= file.getLocation().toFile();
            f.createNewFile();
            
            OutputStream stream= new FileOutputStream(f);
            stream.write(document.get().getBytes());
            stream.flush();
            stream.close();
        } catch (IOException e) {
            MessageDialog.openError(getShell(), 
                    NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.IOException.Title"),  //$NON-NLS-1$
                    e.getMessage());
        } catch (CoreException e) {
            ExceptionHandler.handle(e, getShell(), 
                    NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.CoreException.Title"),  //$NON-NLS-1$
                    e.getMessage());
        }
    }
    
    /**
     * Gets the content of a file at the
     * given <code>path</code>
     * 
     * @param path the path to a file
     * @return returns an <code>IDocument</code> with
     * the content of the file at the given path
     * or null if an exception occurred.
     */
    private IDocument getDocument(IPath path) {
        try {
            FileBuffers.getTextFileBufferManager().connect(path, new NullProgressMonitor());
            ITextFileBuffer buffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path);
            IDocument document= buffer.getDocument();
            FileBuffers.getTextFileBufferManager().disconnect(path, new NullProgressMonitor());
            return document;
        } catch (CoreException e) {
            ExceptionHandler.handle(e, getShell(), 
                    NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.CoreException.Title"),  //$NON-NLS-1$
                    e.getMessage());
        }
        return null;
    }
    
    /**
     * Initialize controls and return composite containing
     * these controls.
     * 
     * @param parent the parent composite
     * @return composite containing controls
     */
    public Control getControl(Composite parent) {
        int numColumns= 1;
        Composite composite= new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
        composite.setLayout(new GridLayout(numColumns, false));
        
        fPackageExplorer= new PackageExplorer(composite, numColumns);
        fModifier= new ClasspathModifier();
        fModifier.addObserver(this);
        fHintTextGroup= new HintTextGroup(composite, fModifier, fRunnableContext);
        fUseFolderOutputs.doFillIntoGrid(composite, numColumns);
        fPackageExplorer.setListener(fHintTextGroup);
        fUseFolderOutputs.getSelectionButton(null).addSelectionListener(new SelectionListener(){
            public void widgetSelected(SelectionEvent e) {
                if (!((Button)e.getSource()).getSelection()) {
                    try {
                        new ClasspathModifier().resetOutputFolders(fCurrJProject, null);
                    } catch (JavaModelException err) {
                        ExceptionHandler.handle(err, getShell(),
                                NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                    }
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }           
        });
        fUseFolderOutputs.getSelectionButton(null).addSelectionListener(fPackageExplorer);
        fUseFolderOutputs.getSelectionButton(null).addSelectionListener(fHintTextGroup);
        
        fSWTControl= composite;
        return composite;
    }
    
    private Shell getShell() {
        if (fSWTControl != null) {
            return fSWTControl.getShell();
        }
        return JavaPlugin.getActiveWorkbenchShell();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#getSelection()
     */
    public List getSelection() {
        // TODO return a valid list
        return new ArrayList();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#setSelection(java.util.List)
     */
    public void setSelection(List selection) {
        // TODO add some logic here
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
     */
    public boolean isEntryKind(int kind) {
        return kind == IClasspathEntry.CPE_SOURCE;
    }

    /**
     * Update <code>fClassPathList</code> or 
     * set the output text of <code>fOutputLocationField</code>.
     */
    public void update(Observable o, Object arg) {
        if (arg == null)
            return;
        if (arg instanceof String) {
            fOutputLocationField.setText((String)arg);
            return;
        }
        if (!(arg instanceof IClasspathEntry))
            return;
        IClasspathEntry entry= (IClasspathEntry)arg;
        CPListElement newElement= CPListElement.createFromExisting(entry, fCurrJProject);
        List list= fClassPathList.getElements();
        for (int i= 0; i < list.size(); i++) {
            CPListElement currElem= (CPListElement)list.get(i);
            if (currElem.equals(newElement)) {
                list.remove(i);
                fClassPathList.setElements(list);
                return;
            }
            if (currElem.getPath().equals(newElement.getPath())) {
                list.remove(i);
                list.add(i, newElement);
                fClassPathList.setElements(list);
                return;
            }
        }
        list.add(newElement);
        fClassPathList.setElements(list);
    }
    
    /**
     * Undo all changes. This includes: <br> 
     * <li> Deleting all newly created folders
     * <li> Restore the ".classpath" and ".project" files.
     */
    public void undoAll() {
        fClassPathList.setElements(originalState);
        Iterator iterator= fNewFolders.iterator();
        while (iterator.hasNext()) {
            Object element= iterator.next();
            IFolder folder;
            try {
                if (element instanceof IFolder)
                    folder= (IFolder)element;
                else
                    folder= fCurrJProject.getProject().getWorkspace().getRoot().getFolder(((IJavaElement)element).getPath());
                folder.delete(false, null);
            } catch (CoreException e) {
            }            
        }
        
        IProject project= fCurrJProject.getProject();
        IFile cPFile= project.getFile(".classpath");  //$NON-NLS-1$
        IFile projectFile= project.getFile(".project"); //$NON-NLS-1$
        restoreFile(cPFile, fCPContent);
        restoreFile(projectFile, fProjectContent);

        fNewFolders= new ArrayList();
    }
}
