package org.eclipse.jdt.internal.ui.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;

import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SelectionStatusDialog;


import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.HintTextGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class ExtendedNewFolderDialog extends SelectionStatusDialog {
    private final class FolderNameField extends Observable implements IDialogFieldListener {
        private StringDialogField fNameDialogField;
        
        public FolderNameField(Composite parent) {
            createControls(parent);
        }
        
        private void createControls(Composite parent) {
            Composite folderGroup = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.numColumns = 2;
            folderGroup.setLayout(layout);
            GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
            
            fNameDialogField= new StringDialogField();
    		fNameDialogField.setLabelText(NewWizardMessages.getString("NewFolderDialog.folderNameGroup.label")); //$NON-NLS-1$
    		fNameDialogField.doFillIntoGrid(folderGroup, layout.numColumns);
    		LayoutUtil.setHorizontalGrabbing(fNameDialogField.getTextControl(null));
    		folderGroup.setLayoutData(gridData);
            fNameDialogField.setDialogFieldListener(this);
        }
        
        public StringDialogField getNameDialogField() {
            return fNameDialogField;
        }
        
        public String getText() {
            return fNameDialogField.getText();
        }
        
        protected void fireEvent() {
            setChanged();
            notifyObservers();
        }

        public void dialogFieldChanged(DialogField field) {
            fireEvent();
        }
    }
    
    private final class FolderTypeGroup {
        protected Button fSourceFolderRadio;
//        protected Button fPackageFolderRadio;
    	
        public FolderTypeGroup(Composite parent) {
            createControls(parent);
        }
        
        private void createControls(Composite parent) {
            final Group group= new Group(parent, SWT.NONE);
    		GridLayout layout = new GridLayout();
            layout.numColumns = 1;
    		group.setLayout(layout);
    		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    		group.setText(NewWizardMessages.getString("NewFolderDialog.TypeGroup.title")); //$NON-NLS-1$
            
//    		if (fType != NewSourceContainerWorkbookPage.HintTextGroup.FOLDER) {
	            fSourceFolderRadio= new Button(group, SWT.RADIO);	            
	    		fSourceFolderRadio.setText(NewWizardMessages.getString("NewFolderDialog.folderTypeGroup.source.desc")); //$NON-NLS-1$
	    		fSourceFolderRadio.setSelection(true);
//    		}
    		
    		/*if (fType == FRAGMENT_ROOT || fType == FRAGMENT) {
    		    fPackageFolderRadio= new Button(group, SWT.RADIO);
    		    fPackageFolderRadio.addSelectionListener(new SelectionListener() {
	                
	                public void widgetSelected(SelectionEvent event) {
	                    // TODO fill in the event reaction
	                }
	                
	                public void widgetDefaultSelected(SelectionEvent event) {
	                    //TODO fill in the event reaction
	                }
	            });
	            
    		    fPackageFolderRadio.setText(NewWizardMessages.getString("NewFolderDialog.folderTypeGroup.pack.desc")); //$NON-NLS-1$
    		}*/
    		
            Button normalFolderRadio= new Button(group, SWT.RADIO);
    		normalFolderRadio.setText(NewWizardMessages.getString("NewFolderDialog.folderTypeGroup.normal.desc")); //$NON-NLS-1$
    		normalFolderRadio.setSelection(fSourceFolderRadio == null);
        }
        
        public boolean isSourceFolderType() {
            return fSourceFolderRadio != null && fSourceFolderRadio.getSelection();
        }
        
        //public void setSelectionListener(SelectionListener listener){
         //   fSourceFolderRadio.addSelectionListener(listener);
          //  fNormalFolderRadio.addSelectionListener(listener);
        //}
        
        /*public boolean isPackageFolderType() {
            return fPackageFolderRadio != null && fPackageFolderRadio.getSelection();
        }*/
    }
    
    private final class DependenciesGroup extends Observable implements IStringButtonAdapter, IDialogFieldListener{
        protected SelectionButtonDialogField fCopyFromButton;
        protected SelectionButtonDialogField fLinkToButton;
        protected StringButtonDialogField fCopyLocation;
        protected StringButtonDialogField fLinkLocation;
        
        private static final String DIALOGSTORE_LAST_EXTERNAL_LOC= JavaUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$
        
        public DependenciesGroup(Composite parent) {
            createControls(parent);
        }
        
        private void createControls(Composite parent) {
            final int numColumns= 4;
            
            final Group group= new Group(parent, SWT.NONE);
    		GridLayout layout = new GridLayout();
            layout.numColumns = numColumns;
    		group.setLayout(layout);
    		GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
    		gridData.minimumWidth= 430;
    		group.setLayoutData(gridData);
    		group.setText(NewWizardMessages.getString("NewFolderDialog.DependencyGroup.title")); //$NON-NLS-1$
    		
            SelectionButtonDialogField noneButton= new SelectionButtonDialogField(SWT.RADIO);
            noneButton.setDialogFieldListener(this);
            
            noneButton.setLabelText(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.none.desc")); //$NON-NLS-1$
            noneButton.setSelection(true);
    		
            fCopyFromButton= new SelectionButtonDialogField(SWT.RADIO);
            fCopyFromButton.setLabelText(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.copy.desc")); //$NON-NLS-1$
            
            fCopyLocation= new StringButtonDialogField(this);
			fCopyLocation.setLabelText(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.locationLabel.desc")); //$NON-NLS-1$
			fCopyLocation.setButtonLabel(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.browseButton.desc")); //$NON-NLS-1$
			fCopyFromButton.attachDialogField(fCopyLocation);
            fCopyFromButton.setDialogFieldListener(this);
            fCopyLocation.setDialogFieldListener(this);
    		
            fLinkToButton= new SelectionButtonDialogField(SWT.RADIO);
            fLinkToButton.setLabelText(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.link.desc")); //$NON-NLS-1$
            
            
            fLinkLocation= new StringButtonDialogField(this);
            fLinkLocation.setLabelText(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.locationLabel.desc")); //$NON-NLS-1$
            fLinkLocation.setButtonLabel(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.browseButton.desc")); //$NON-NLS-1$
            fLinkLocation.setDialogFieldListener(this);
            
            SelectionButtonDialogField variables= new SelectionButtonDialogField(SWT.PUSH);
            variables.setLabelText(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.variables.desc")); //$NON-NLS-1$
            fLinkToButton.attachDialogFields(new DialogField[] {fLinkLocation, variables});
            fLinkToButton.setDialogFieldListener(this);
			
            noneButton.doFillIntoGrid(group, numColumns);
            fCopyFromButton.doFillIntoGrid(group, numColumns);
			fCopyLocation.doFillIntoGrid(group, numColumns - 1);
			if (fType != DialogPackageExplorerActionGroup.JAVA_PROJECT) {
			    fLinkToButton.setLabelText(NewWizardMessages.getString("NewFolderDialog.dependenciesGroup.link.descDisabled")); //$NON-NLS-1$
                fLinkToButton.setEnabled(false);
            }
			    fLinkToButton.doFillIntoGrid(group, numColumns);
			    fLinkLocation.doFillIntoGrid(group, numColumns - 1);
			    LayoutUtil.setHorizontalGrabbing(fLinkLocation.getTextControl(null));
//			}			
			
			variables.doFillIntoGrid(group, 1);
			LayoutUtil.setHorizontalGrabbing(fCopyLocation.getTextControl(null));
        }
        
        public String getLinkTarget() {
            return fLinkLocation.getText();
        }
        
        public String getCopyTarget() {
            return fCopyLocation.getText();
        }
        
        public boolean linkTargetSelected() {
            return fLinkToButton.isSelected();
        }
        
        public boolean copyTargetSelected() {
            return fCopyFromButton.isSelected();
        }

        /* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void changeControlPressed(DialogField field) {
		    StringButtonDialogField selectedField= field == fLinkLocation ? fLinkLocation : fCopyLocation; 
			final DirectoryDialog dialog= new DirectoryDialog(getShell());
			dialog.setMessage(NewWizardMessages.getString("JavaProjectWizardFirstPage.directory.message")); //$NON-NLS-1$
			String directoryName = getLinkTarget().trim();
			if (directoryName.length() == 0) {
				String prevLocation= JavaPlugin.getDefault().getDialogSettings().get(DIALOGSTORE_LAST_EXTERNAL_LOC);
				if (prevLocation != null) {
					directoryName= prevLocation;
				}
			}
		
			if (directoryName.length() > 0) {
				final File path = new File(directoryName);
				if (path.exists())
					dialog.setFilterPath(new Path(directoryName).toOSString());
			}
			final String selectedDirectory = dialog.open();
			if (selectedDirectory != null) {
			    selectedField.setText(selectedDirectory);
				JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LAST_EXTERNAL_LOC, selectedDirectory);
			}
		}
        
        public void dialogFieldChanged(DialogField field) {
            fireEvent();
        }
		
        protected void fireEvent() {
            setChanged();
            notifyObservers();
        }
    }
    
    /**
	 * Validate this page and show appropriate warnings and error NewWizardMessages.
	 */
	private final class Validator implements Observer {

		public void update(Observable o, Object arg) {

			final String name= fFolderNameField.getText();
			
            if (!validateFolderName(name))
                return;
            
            if (fDependenciesGroup.linkTargetSelected()) {
                validateLinkedResource(fDependenciesGroup.getLinkTarget());
                return;
            }
            
            if (fDependenciesGroup.copyTargetSelected()) {
                validateLocation(fDependenciesGroup.getCopyTarget());
                return;
            }
			
			updateStatus(IStatus.OK, ""); //$NON-NLS-1$
		}
		
		/**
         * Validates this page's controls.
         *
         * @return IStatus indicating the validation result. IStatus.OK if the 
         * 	specified link target is valid given the linkHandle.
         */
        private IStatus validateLinkLocation(IResource linkHandle) {
            IWorkspace workspace = JavaPlugin.getWorkspace();
            IPath path = new Path(fDependenciesGroup.getLinkTarget());

            IStatus locationStatus = workspace.validateLinkLocation(linkHandle, path);
            if (locationStatus.getMessage().equals(NewWizardMessages.getFormattedString("NewFolderDialog.links.parentNotProject", linkHandle.getName())) && //$NON-NLS-1$
                    container.getType() == IResource.PROJECT)
                locationStatus= Status.OK_STATUS;
            else if (locationStatus.getSeverity() == IStatus.ERROR)
                return locationStatus;

            // use the resolved link target name
            String resolvedLinkTarget = resolveVariable();
            path = new Path(resolvedLinkTarget);
            File linkTargetFile = new Path(resolvedLinkTarget).toFile();
            if (linkTargetFile.exists()) {
                IStatus fileTypeStatus = validateFileType(linkTargetFile);
                if (fileTypeStatus.isOK() == false)
                    return fileTypeStatus;
            } else if (locationStatus.getSeverity() == IStatus.OK) {
                // locationStatus takes precedence over missing location warning.
                return createStatus(
                        IStatus.WARNING,
                        NewWizardMessages
                                .getString("NewFolderDialog.linkTargetNonExistent")); //$NON-NLS-1$	
            }
            return locationStatus;
        }
        
        /**
         * Validates the type of the given file against the link type specified
         * in the constructor.
         * 
         * @param linkTargetFile file to validate
         * @return IStatus indicating the validation result. IStatus.OK if the 
         * 	given file is valid.
         */
        private IStatus validateFileType(File linkTargetFile) {
            if (linkTargetFile.isDirectory() == false)
                return createStatus(IStatus.ERROR, NewWizardMessages
                        .getString("NewFolderDialog.linkTargetNotFolder")); //$NON-NLS-1$
            return createStatus(IStatus.OK, ""); //$NON-NLS-1$
        }
        
        /**
         * Returns a new status object with the given severity and message.
         * 
         * @return a new status object with the given severity and message.
         */
        private IStatus createStatus(int severity, String message) {
            return new Status(severity, JavaPlugin.getPluginId(), severity, message, null);
        } 
        
        /**
         * Tries to resolve the value entered in the link target field as 
         * a variable, if the value is a relative path.
         * Displays the resolved value if the entered value is a variable.
         */
        private String resolveVariable() {
            IPathVariableManager pathVariableManager = ResourcesPlugin
                    .getWorkspace().getPathVariableManager();
            IPath path = new Path(fDependenciesGroup.getLinkTarget());
            IPath resolvedPath = pathVariableManager.resolvePath(path);
            return resolvedPath.toOSString();
        }
        
        /**
         * Checks whether the folder name and link location are valid.
         * Disable the OK button if the folder name and link location are valid.
         * a message that indicates the problem otherwise.
         */
        private void validateLinkedResource(String text) {
            boolean valid = validateLocation(text);

            if (valid) {
                IFolder linkHandle = createFolderHandle(text);
                IStatus status = validateLinkLocation(linkHandle);

                if (status.getSeverity() != IStatus.ERROR)
                    getOkButton().setEnabled(true);
                else
                    getOkButton().setEnabled(false);

                if (status.isOK() == false)
                    updateStatus(status);
            } else
                getOkButton().setEnabled(false);
        }
        
        private boolean validateLocation(String text) {
            IPath path = new Path(text);
            if (!path.toFile().exists()) {
                updateStatus(IStatus.ERROR, NewWizardMessages.getFormattedString(
                        "NewFolderDialog.notExists", new Object[] { text })); //$NON-NLS-1$
                return false;
            }
            updateStatus(IStatus.OK, "");  //$NON-NLS-1$
            return true;
        }
        
        /**
         * Checks if the folder name is valid.
         *
         * @return <code>true</code> if validation was
         * correct, <code>false</code> otherwise
         */
        private boolean validateFolderName(String name) {
            IWorkspace workspace = container.getWorkspace();
            IStatus nameStatus = workspace.validateName(name, IResource.FOLDER);

            if (name.length() == 0) { //$NON-NLS-1$
                updateStatus(IStatus.ERROR, NewWizardMessages
                        .getString("NewFolderDialog.folderNameEmpty")); //$NON-NLS-1$
                return false;
            }
            
            if (nameStatus.isOK() == false) {
                updateStatus(nameStatus);
                return false;
            }
            
            IPath path = new Path(name);
            if (container.getFolder(path).exists()
                    || container.getFile(path).exists()) {
                updateStatus(IStatus.ERROR, NewWizardMessages.getFormattedString(
                        "NewFolderDialog.folderNameEmpty.alreadyExists", new Object[] { name })); //$NON-NLS-1$
                return false;
            }
            updateStatus(IStatus.OK, ""); //$NON-NLS-1$
            return true;
        }
	}
	
	private final class CopyFolder {

	    public void copy(String sourceDir, String destDir, IWorkspaceRoot workspaceRoot, String name) throws IOException, StringIndexOutOfBoundsException {
	        File source = new File(sourceDir);
	        File dest = new File(destDir);
	        if (!(source.isDirectory() && source.exists()))
	          throw new IOException();
	        if( !dest.exists() )
	          dest.mkdir();
	        copy(source.getPath(), name, source, workspaceRoot);
        }

	    private void copy(String from, String folderName, File source, IWorkspaceRoot workspaceRoot) {
	        File dir[] = new File(from).listFiles();
	        for (int i = 0; i < dir.length; i++) {
	            if (dir[i].isDirectory()) {
	               createFolder(new Path(dir[i].getPath().substring(source.getPath().length())).makeRelative().toString(), folderName, workspaceRoot);
	               copy(dir[i].getPath(), folderName, source, workspaceRoot);
	            }
	            else {              
	                File fileFrom = new File(dir[i].getPath());
	                try {
	                   FileInputStream in = new FileInputStream(fileFrom);
	                   createFile(new Path(dir[i].getPath().substring(source.getPath().length())).makeRelative().toString(), in, folderName, workspaceRoot);
	                }
	                catch(FileNotFoundException e) {
	                      e.printStackTrace();
	                }
	            }
	        }
	    }
	    
	    private void createFolder(String name, String folderName, IWorkspaceRoot workspaceRoot) {
	        IPath path= container.getFullPath().append(folderName+name);
	        IFolder folderHandle = workspaceRoot.getFolder(path);
	        try {
	            folderHandle.create(false, true, new NullProgressMonitor());
	        } catch (CoreException e) {
	            JavaPlugin.log(e);
	        }
	    }
	    
	    private void createFile(String name, FileInputStream stream, String folderName, IWorkspaceRoot workspaceRoot) {
	        IPath path= container.getFullPath().append(folderName+name);
	        IFile fileHandle= workspaceRoot.getFile(path);
	        try { 
	            fileHandle.create(stream, true, null);
	        } catch (CoreException e) {
	            JavaPlugin.log(e);
	        }	        
	    }
	}
	
    private FolderNameField fFolderNameField;
    protected FolderTypeGroup fFolderTypeGroup;
    protected DependenciesGroup fDependenciesGroup;
    private int fType;
    private IContainer container;

    /**
     * Creates a NewFolderDialog
     * 
     * @param parentShell parent of the new dialog
     * @param container parent of the new folder
     * @param type specifies the type of the parent folder. Must be one of the constants of
     * <code>NewSourceContainerWorkbookPage.HintTextGroup</code>
     * 
     * @see HintTextGroup
     */
    public ExtendedNewFolderDialog(Shell parentShell, IContainer container, int type) {
        super(parentShell);
        this.container = container;
        this.fType= type;
        setTitle(NewWizardMessages.getString("NewFolderDialog.title")); //$NON-NLS-1$
        setShellStyle(getShellStyle() | SWT.RESIZE);
        setStatusLineAboveButtons(true);      
    }

    /**
     * Creates the folder using the name and link target entered
     * by the user.
     * Sets the dialog result to the created folder.  
     */
    protected void computeResult() {
        //Do nothing here as we 
    	//need to know the result
    }

    /* (non-Javadoc)
     * Method declared in Window.
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
    }

    /**
     * @see org.eclipse.jface.window.Window#create()
     */
    public void create() {
        super.create();
        // initially disable the ok button since we don't preset the
        // folder name field
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    /* (non-Javadoc)
     * Method declared on Dialog.
     */
    protected Control createDialogArea(Composite parent) {        
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Label label= new Label(composite, SWT.NONE);
        label.setText(NewWizardMessages.getFormattedString("NewFolderDialog.createIn", container.getFullPath().toString())); //$NON-NLS-1$
        
        fFolderNameField= new FolderNameField(composite);
        
        fFolderTypeGroup= new FolderTypeGroup(composite); //$NON-NLS-1$
        fDependenciesGroup= new DependenciesGroup(composite);
        
		Validator validator= new Validator();
        fDependenciesGroup.addObserver(validator);
        fFolderNameField.addObserver(validator);

        return composite;
    }

    /**
     * Creates a folder resource handle for the folder with the given name.
     * The folder handle is created relative to the container specified during 
     * object creation. 
     *
     * @param folderName the name of the folder resource to create a handle for
     * @return the new folder resource handle
     */
    private IFolder createFolderHandle(String folderName) {
        IWorkspaceRoot workspaceRoot = container.getWorkspace().getRoot();
        IPath folderPath = container.getFullPath().append(folderName);
        IFolder folderHandle = workspaceRoot.getFolder(folderPath);

        return folderHandle;
    }

    /**
     * Creates a new folder with the given name and optionally linking to
     * the specified link target.
     * 
     * @param folderName name of the new folder
     * @param linkTargetName name of the link target folder. may be null.
     * @return IFolder the new folder
     */
    private IFolder createNewFolder(final String folderName, final String linkTargetName) {
        final IFolder folderHandle = createFolderHandle(folderName);
//        final boolean isSourceFolder= fFolderTypeGroup.isSourceFolderType();
//        final boolean isPackageFolder= fFolderTypeGroup.isPackageFolderType();
        final boolean copyFromFolder= fDependenciesGroup.copyTargetSelected();
        final boolean linkToFolder= fDependenciesGroup.linkTargetSelected();
        final CopyFolder copyFactory= new CopyFolder();
        
        WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {
            public void execute(IProgressMonitor monitor) throws CoreException {
                try {
                    monitor.beginTask(NewWizardMessages
                            .getString("NewFolderDialog.progress"), 2000); //$NON-NLS-1$
                    if (monitor.isCanceled())
                        throw new OperationCanceledException();
                    
                    if (copyFromFolder) {
                        // copy folder
                        folderHandle.create(false, true, monitor);
                        copyFactory.copy(fDependenciesGroup.getCopyTarget(), container.getLocation().toOSString()+"\\"+folderName, container.getWorkspace().getRoot(), folderName+"/"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    else if (linkToFolder){
                        // create link to folder
                        folderHandle.createLink(new Path(fDependenciesGroup.getLinkTarget()),
                                IResource.ALLOW_MISSING_LOCAL, monitor);
                    }
                    else {
                        //create normal folder
                        folderHandle.create(false, true, monitor);
                    }
                    
                    if (monitor.isCanceled())
                        throw new OperationCanceledException();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (StringIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                finally {
                    monitor.done();
                }
            }
        };
        
        try {
            new ProgressMonitorDialog(getShell())
                    .run(true, true, operation);
        } catch (InterruptedException exception) {
            return null;
        } catch (InvocationTargetException exception) {
            if (exception.getTargetException() instanceof CoreException) {
                ErrorDialog.openError(getShell(), NewWizardMessages
                        .getString("NewFolderDialog.errorTitle"), //$NON-NLS-1$
                        null, // no special message
                        ((CoreException) exception.getTargetException())
                                .getStatus());
            } else {
                // CoreExceptions are handled above, but unexpected runtime exceptions and errors may still occur.
                JavaPlugin.log(new Exception(MessageFormat.format(
                        "Exception in {0}.createNewFolder(): {1}", //$NON-NLS-1$
                        new Object[] { getClass().getName(),
                                exception.getTargetException() })));
                MessageDialog.openError(getShell(), NewWizardMessages
                        .getString("NewFolderDialog.errorTitle"), //$NON-NLS-1$
                        NewWizardMessages.getFormattedString(
                                "NewFolderDialog.internalError", //$NON-NLS-1$
                                new Object[] { exception.getTargetException()
                                        .getMessage() }));
            }
            return null;
        }
        
        return folderHandle;
    }

    /**
     * Update the dialog's status line to reflect the given status. It is safe to call
     * this method before the dialog has been opened.
     */
    protected void updateStatus(IStatus status) {
        super.updateStatus(status);
    }

    /**
     * Update the dialog's status line to reflect the given status. It is safe to call
     * this method before the dialog has been opened.
     * @param severity
     * @param message
     */
    private void updateStatus(int severity, String message) {
        updateStatus(new Status(severity, JavaPlugin.getPluginId(), severity, message, null));
    }
    
    /* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#okPressed()
	 */
	protected void okPressed() {
		String linkTarget = fDependenciesGroup.getLinkTarget();
		linkTarget= linkTarget.equals("") ? null : linkTarget;  //$NON-NLS-1$
        IFolder folder = createNewFolder(fFolderNameField.getText(), linkTarget);
        if (folder == null)
            return;
        
        Boolean isSourceFolderType= new Boolean(fFolderTypeGroup.isSourceFolderType());
//        Boolean isPackageFolderType= new Boolean(fFolderTypeGroup.isPackageFolderType());
        setSelectionResult(new Object[] {folder, isSourceFolderType});//, isPackageFolderType});
        
		super.okPressed();
	}
    
    /**
     * Returns the created folder or <code>null</code>
     * if there is none.
     * 
     * @return created folder or <code>null</code>
     */
    public IFolder getCreatedFolder() {
        Object[] result= getResult();
        if (result != null && result.length == 2)
            return (IFolder) result[0];
        return null;
    }
    
    /**
     * Returns wheter the new folder is meant
     * to be a source folder or not.
     * 
     * @return <code>true</code> if it is a
     * source folder, <code>false</code> otherwise.
     */
    public boolean isSourceFolder() {
        Object[] result= getResult();
        if (result != null && result.length == 2)
            return ((Boolean)result[1]).booleanValue();
        return false;
    }
}
