/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusTool;
import org.eclipse.jdt.internal.ui.preferences.ClasspathVariablesPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDisposer;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridUtil;

public class BuildPathsBlock {

	private IWorkspaceRoot fWorkspaceRoot;

	private ListDialogField fClassPathList;
	private StringDialogField fBuildPathDialogField;
	
	private StatusInfo fClassPathStatus;
	private StatusInfo fBuildPathStatus;
	
	private IProject fCurrProject;
	private IJavaProject fCurrJProject;
		
	private IPath fBuildPathDefault;
	private IClasspathEntry[] fClassPathDefault;
	private boolean fAddJDKToDefault;
	
	private IPath fOutputLocationPath;
	
	private IStatusChangeListener fContext;
	private Control fSWTWidget;	
	
	private boolean fIsNewProject;
	
	private SourceContainerWorkbookPage fSourceContainerPage;
	private ProjectsWorkbookPage fProjectsPage;
	private LibrariesWorkbookPage fLibrariesPage;
	
	private BuildPathBasePage fCurrPage;
		
	public BuildPathsBlock(IWorkspaceRoot root, IStatusChangeListener context, boolean isNewProject) {
		fWorkspaceRoot= root;
		fContext= context;
		fIsNewProject= isNewProject;
		fSourceContainerPage= null;
		fLibrariesPage= null;
		fProjectsPage= null;
		fCurrPage= null;
				
		BuildPathAdapter adapter= new BuildPathAdapter();			

		fClassPathList= new ListDialogField(new CPListLabelProvider(), ListDialogField.UPDOWN);
		fClassPathList.setDialogFieldListener(adapter);
		fClassPathList.setLabelText(NewWizardMessages.getString("BuildPathsBlock.classpath.label")); //$NON-NLS-1$
		fClassPathList.setUpButtonLabel(NewWizardMessages.getString("BuildPathsBlock.classpath.up.button")); //$NON-NLS-1$
		fClassPathList.setDownButtonLabel(NewWizardMessages.getString("BuildPathsBlock.classpath.down.button")); //$NON-NLS-1$
			
		if (isNewProject) {
			fBuildPathDialogField= new StringDialogField();
		} else {
			StringButtonDialogField dialogField= new StringButtonDialogField(adapter);
			dialogField.setButtonLabel(NewWizardMessages.getString("BuildPathsBlock.buildpath.button")); //$NON-NLS-1$
			fBuildPathDialogField= dialogField;
		}
		fBuildPathDialogField.setDialogFieldListener(adapter);
		fBuildPathDialogField.setLabelText(NewWizardMessages.getString("BuildPathsBlock.buildpath.label")); //$NON-NLS-1$

		fBuildPathStatus= new StatusInfo();
		fClassPathStatus= new StatusInfo();
		
		fBuildPathDefault= null;
		fCurrProject= null;
		fCurrJProject= null;
		
		fClassPathDefault= null;
		fAddJDKToDefault= true;
	}
	
	// -------- UI creation ---------
	
	public Control createControl(Composite parent) {
		fSWTWidget= parent;
		
		Composite composite= new Composite(parent, SWT.NONE);	
		
		MGridLayout layout= new MGridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 5;	
		layout.minimumWidth= 450;
		layout.minimumHeight= 350;
		layout.numColumns= 1;		
		composite.setLayout(layout);
		
		TabFolder folder= new TabFolder(composite, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(MGridUtil.createFill());
		folder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tabChanged(e.item);
			}	
		});
		
		ImageRegistry imageRegistry= JavaPlugin.getDefault().getImageRegistry();
		
		TabItem item;
				
		fSourceContainerPage= new SourceContainerWorkbookPage(fWorkspaceRoot, fClassPathList, fBuildPathDialogField, fIsNewProject);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.getString("BuildPathsBlock.tab.source")); //$NON-NLS-1$
		item.setImage(imageRegistry.get(JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT));
		item.setData(fSourceContainerPage);		
		item.setControl(fSourceContainerPage.getControl(folder));
		
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();	
		Image projectImage= workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT);
		
		fProjectsPage= new ProjectsWorkbookPage(fClassPathList);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.getString("BuildPathsBlock.tab.projects")); //$NON-NLS-1$
		item.setImage(projectImage);
		item.setData(fProjectsPage);
		item.setControl(fProjectsPage.getControl(folder));
		
		fLibrariesPage= new LibrariesWorkbookPage(fWorkspaceRoot, fClassPathList);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.getString("BuildPathsBlock.tab.libraries")); //$NON-NLS-1$
		item.setImage(imageRegistry.get(JavaPluginImages.IMG_OBJS_LIBRARY));
		item.setData(fLibrariesPage);
		item.setControl(fLibrariesPage.getControl(folder));
		
		// a non shared image
		Image cpoImage= JavaPluginImages.DESC_TOOL_CLASSPATH_ORDER.createImage();
		composite.addDisposeListener(new ImageDisposer(cpoImage));	
		
		ClasspathOrderingWorkbookPage ordpage= new ClasspathOrderingWorkbookPage(fClassPathList);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.getString("BuildPathsBlock.tab.order")); //$NON-NLS-1$
		item.setImage(cpoImage);
		item.setData(ordpage);
		item.setControl(ordpage.getControl(folder));
						
				
		if (fCurrJProject != null) {
			fSourceContainerPage.init(fCurrJProject);
			fLibrariesPage.init(fCurrJProject);
			fProjectsPage.init(fCurrJProject);
		}		
						
		Composite editorcomp= new Composite(composite, SWT.NONE);	
	
		DialogField[] editors= new DialogField[] { fBuildPathDialogField };
		LayoutUtil.doDefaultLayout(editorcomp, editors, true);
		
		editorcomp.setLayoutData(MGridUtil.createHorizontalFill());
		
		if (fIsNewProject) {
			folder.setSelection(0);
			fCurrPage= fSourceContainerPage;
		} else {
			folder.setSelection(3);
			fCurrPage= ordpage;
		}

		WorkbenchHelp.setHelp(composite, new Object[] { IJavaHelpContextIds.BUILD_PATH_BLOCK });				
		return composite;
	}
	
	private Shell getShell() {
		if (fSWTWidget != null) {
			return fSWTWidget.getShell();
		}
		// to do: error dialog
		//return Utilities.getFocusShell(Display.getCurrent());
		return null;
	}
		
	// -------- Initialization ---------
		
	public void init(IProject project, boolean force) {
		if (!project.equals(fCurrProject) || force) {
			fCurrProject= project;
			fCurrJProject= JavaCore.create(fCurrProject);			
			try {
				if (fCurrProject.hasNature(JavaCore.NATURE_ID)) {
					IPath outputDir= fCurrJProject.getOutputLocation();
					if (outputDir != null) {
						fBuildPathDialogField.setText(outputDir.toString());
					} else {
						fBuildPathDialogField.setText(""); //$NON-NLS-1$
					}
					
					List newClassPath= new ArrayList();
					IClasspathEntry[] cp= fCurrJProject.getRawClasspath();
					for (int i= 0; i < cp.length; i++) {
						IClasspathEntry curr= cp[i];
						int entryKind= curr.getEntryKind();
						IPath path= curr.getPath();
						// get the resource
						IResource res= null;
						boolean isMissing= false;
						
						if (entryKind != IClasspathEntry.CPE_VARIABLE) {
							res= fWorkspaceRoot.findMember(path);
							if (res == null) {
								isMissing= entryKind != IClasspathEntry.CPE_LIBRARY || !path.toFile().isFile();
							}
						} else {
							IPath resolvedPath= JavaCore.getResolvedVariablePath(path);
							isMissing= (resolvedPath == null) || !resolvedPath.toFile().isFile();
						}												
						CPListElement elem= new CPListElement(entryKind, path, res, curr.getSourceAttachmentPath(), curr.getSourceAttachmentRootPath());
						elem.setIsMissing(isMissing);
						newClassPath.add(elem);			
					}
					fClassPathList.setElements(newClassPath);
				} else {
					setDefaultAttributes(fCurrProject);
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
				setDefaultAttributes(fCurrProject);			
			} catch (CoreException e) {
				// must be a new (not created) project
				setDefaultAttributes(fCurrProject);
			}
			if (fSourceContainerPage != null) {
				fSourceContainerPage.init(fCurrJProject);
				fProjectsPage.init(fCurrJProject);
				fLibrariesPage.init(fCurrJProject);
			}
			
		}	
		
		doStatusLineUpdate();
	}	
	
	// -------- public api --------
	
	public void setDefaultOutputFolder(IPath outputPath) {
		fBuildPathDefault= outputPath;
	}
	
	public void setDefaultClassPath(IClasspathEntry[] entries, boolean appendDefaultJDK) {
		fClassPathDefault= entries;
		fAddJDKToDefault= appendDefaultJDK;
	}
	
	public IJavaProject getJavaProject() {
		return fCurrJProject;
	}	
	
	// -------- default settings --------
	
	private List getClassPathDefault(IProject project) {
		List vec= new ArrayList();
		if (fClassPathDefault == null) {
			IResource srcFolder;
			if (JavaBasePreferencePage.useSrcAndBinFolders()) {
				srcFolder= project.getFolder("src"); //$NON-NLS-1$
			} else {
				srcFolder= project;
			}
			vec.add(new CPListElement(IClasspathEntry.CPE_SOURCE, srcFolder.getFullPath(), srcFolder));
		} else {
			for (int i= 0; i < fClassPathDefault.length; i++) {
				IClasspathEntry entry= fClassPathDefault[i];
				IResource res= fWorkspaceRoot.findMember(entry.getPath());			
				vec.add(new CPListElement(entry.getEntryKind(), entry.getPath(), res, entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath()));
			}
		}
		
		if (fAddJDKToDefault || fClassPathDefault == null) {
			IPath libPath= new Path(ClasspathVariablesPreferencePage.JRELIB_VARIABLE);
			IPath attachPath= new Path(ClasspathVariablesPreferencePage.JRESRC_VARIABLE);
			IPath attachRoot= new Path(ClasspathVariablesPreferencePage.JRESRCROOT_VARIABLE);
			CPListElement elem= new CPListElement(IClasspathEntry.CPE_VARIABLE, libPath, null, attachPath, attachRoot);
			vec.add(elem);
		}
		return vec;
	}
	
	private IPath getBuildPathDefault(IProject project) {
		if (fBuildPathDefault != null) {
			return fBuildPathDefault;
		} else {
			if (JavaBasePreferencePage.useSrcAndBinFolders()) {
				return project.getFullPath().append("bin"); //$NON-NLS-1$
			} else {
				return project.getFullPath();
			}
		}
	}	

	private void setDefaultAttributes(IProject project) {
		fClassPathList.setElements(getClassPathDefault(project));
		IPath outputDir= getBuildPathDefault(project);
		fBuildPathDialogField.setText(outputDir.toString());
	}

		
	private class BuildPathAdapter implements IStringButtonAdapter, IDialogFieldListener {

		// -------- IStringButtonAdapter --------
		public void changeControlPressed(DialogField field) {
			buildPathChangeControlPressed(field);
		}
		
		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			buildPathDialogFieldChanged(field);
		}
	}
	
	private void buildPathChangeControlPressed(DialogField field) {
		if (field == fBuildPathDialogField) {
			IContainer container= chooseContainer();
			if (container != null) {
				fBuildPathDialogField.setText(container.getFullPath().toString());
			}
		}
	}
	
	private void buildPathDialogFieldChanged(DialogField field) {
		if (field == fClassPathList) {
			updateClassPathStatus();
			updateBuildPathStatus();
		} else if (field == fBuildPathDialogField) {
			updateBuildPathStatus();
		}
		doStatusLineUpdate();
	}	
	

	
	// -------- verification -------------------------------
	
	private void doStatusLineUpdate() {
		IStatus res= findMostSevereStatus();
		fContext.statusChanged(res);
	}
	
	private IStatus findMostSevereStatus() {
		return StatusTool.getMoreSevere(fClassPathStatus, fBuildPathStatus);
	}
	
	
	// a class path entry should not be the prefix of another entry
	private void updateClassPathStatus() {
		fClassPathStatus.setOK();
		
		List elements= fClassPathList.getElements();
	
		boolean entryMissing= false;
		IClasspathEntry[] entries= new IClasspathEntry[elements.size()];

		for (int i= elements.size()-1 ; i >= 0 ; i--) {
			CPListElement currElement= (CPListElement)elements.get(i);
			entries[i]= currElement.getClasspathEntry();
			entryMissing= entryMissing || currElement.isMissing();			
		}
				
		if (entryMissing) {
			fClassPathStatus.setWarning(NewWizardMessages.getString("BuildPathsBlock.warning.EntryMissing")); //$NON-NLS-1$
		}
				
		if (fCurrJProject.hasClasspathCycle(entries)) {
			fClassPathStatus.setWarning(NewWizardMessages.getString("BuildPathsBlock.warning.CycleInClassPath")); //$NON-NLS-1$
		}	
	}
	
	private void updateBuildPathStatus() {
		fOutputLocationPath= null;
		
		String text= fBuildPathDialogField.getText();
		if ("".equals(text)) { //$NON-NLS-1$
			fBuildPathStatus.setError(NewWizardMessages.getString("BuildPathsBlock.error.EnterBuildPath")); //$NON-NLS-1$
			return;
		}
		IPath path= new Path(text);
		
		IResource res= fWorkspaceRoot.findMember(path);
		if (res != null) {
			// if exists, must be a folder or project
			if (res.getType() == IResource.FILE) {
				fBuildPathStatus.setError(NewWizardMessages.getString("BuildPathsBlock.error.InvalidBuildPath")); //$NON-NLS-1$
				return;
			}
		} else {
			// allows the build path to be in a different project, but must exists and be open
			IPath projPath= path.uptoSegment(1);
			if (!projPath.equals(fCurrProject.getFullPath())) {
				IProject proj= (IProject)fWorkspaceRoot.findMember(projPath);
				if (proj == null || !proj.isOpen()) {
					fBuildPathStatus.setError(NewWizardMessages.getString("BuildPathsBlock.error.BuildPathProjNotExists")); //$NON-NLS-1$
					return;
				}
			}
		}
		fOutputLocationPath= path;
		
		List elements= fClassPathList.getElements();
		IClasspathEntry[] entries= new IClasspathEntry[elements.size()];
		
		for (int i= elements.size()-1 ; i >= 0 ; i--) {
			CPListElement currElement= (CPListElement)elements.get(i);
			entries[i]= currElement.getClasspathEntry();
		}
		
		IStatus status= JavaConventions.validateClasspath(fCurrJProject, entries, path);
		if (!status.isOK()) {
			fBuildPathStatus.setError(status.getMessage());
			return;
		}
				
		fBuildPathStatus.setOK();
	}
	
	// -------- creation -------------------------------
	
	public IRunnableWithProgress getRunnable() {
		final List classPathEntries= fClassPathList.getElements();
		final IPath path= new Path(fBuildPathDialogField.getText());
		
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				if (monitor == null) {
					monitor= new NullProgressMonitor();
				}				
				monitor.beginTask(NewWizardMessages.getString("BuildPathsBlock.operationdesc"), 12); //$NON-NLS-1$
				try {
					setJavaProjectProperties(classPathEntries, path, monitor);
				} catch (CoreException e) { 
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
	}		
	
	private void setJavaProjectProperties(List classPathEntries, IPath buildPath, IProgressMonitor monitor) throws CoreException {
		if (!fCurrProject.exists()) {
			fCurrProject.create(null);
		}
		
		if (!fCurrProject.isOpen()) {
			fCurrProject.open(null);
		}
		
		// create java nature
		if (!fCurrProject.hasNature(JavaCore.NATURE_ID)) {
			CoreUtility.addNatureToProject(fCurrProject, JavaCore.NATURE_ID, null);
		}
		monitor.worked(1);
		
		String[] prevRequiredProjects= fCurrJProject.getRequiredProjectNames();
		
		// create and set the output path first
		if (!fWorkspaceRoot.exists(buildPath)) {
			IFolder folder= fWorkspaceRoot.getFolder(buildPath);
			CoreUtility.createFolder(folder, true, true, null);			
		}
		monitor.worked(1);
		
		fCurrJProject.setOutputLocation(buildPath, new SubProgressMonitor(monitor, 3));
		
		int nEntries= classPathEntries.size();
		IClasspathEntry[] classpath= new IClasspathEntry[nEntries];
		
		// create and set the class path
		for (int i= 0; i < nEntries; i++) {
			CPListElement entry= ((CPListElement)classPathEntries.get(i));
			IResource res= entry.getResource();
			if ((res instanceof IFolder) && !res.exists()) {
				CoreUtility.createFolder((IFolder)res, true, true, null);
			}
			classpath[i]= entry.getClasspathEntry();
		}	
		monitor.worked(1);
			
		fCurrJProject.setRawClasspath(classpath, new SubProgressMonitor(monitor, 5));
		
		JavaModelUtil.updateRequiredProjects(fCurrJProject, prevRequiredProjects, new SubProgressMonitor(monitor, 1));
		
	}
	
	// ---------- util method ------------
		
	private IContainer chooseContainer() {
		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		IProject[] allProjects= fWorkspaceRoot.getProjects();
		ArrayList rejectedElements= new ArrayList(allProjects.length);
		for (int i= 0; i < allProjects.length; i++) {
			if (!allProjects[i].equals(fCurrProject)) {
				rejectedElements.add(allProjects[i]);
			}
		}
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedElements.toArray());

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		IResource initSelection= null;
		if (fOutputLocationPath != null) {
			initSelection= fWorkspaceRoot.findMember(fOutputLocationPath);
		}
		
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setTitle(NewWizardMessages.getString("BuildPathsBlock.ChooseOutputFolderDialog.title")); //$NON-NLS-1$
		dialog.setValidator(validator);
		dialog.setMessage(NewWizardMessages.getString("BuildPathsBlock.ChooseOutputFolderDialog.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(fWorkspaceRoot);
		dialog.setInitialSelection(initSelection);
		
		if (dialog.open() == dialog.OK) {
			return (IContainer)dialog.getSelectedElement();
		}
		return null;
	}
	
	// -------- tab switching ----------
	
	private void tabChanged(Widget widget) {
		if (widget instanceof TabItem) {
			BuildPathBasePage newPage= (BuildPathBasePage) ((TabItem) widget).getData();
			if (fCurrPage != null) {
				List selection= fCurrPage.getSelection();
				if (!selection.isEmpty()) {
					newPage.setSelection(selection);
				}
			}
			fCurrPage= newPage;
		}
	}		
}