/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.lang.reflect.InvocationTargetException;import java.text.MessageFormat;import java.util.ArrayList;import java.util.List;import java.util.MissingResourceException;import java.util.ResourceBundle;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.swt.widgets.TabFolder;import org.eclipse.swt.widgets.TabItem;import org.eclipse.swt.widgets.Widget;import org.eclipse.core.resources.IFolder;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.core.runtime.Path;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.resource.ImageRegistry;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.ViewerFilter;import org.eclipse.ui.ISharedImages;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.model.WorkbenchContentProvider;import org.eclipse.ui.model.WorkbenchLabelProvider;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaConventions;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;import org.eclipse.jdt.internal.ui.dialogs.TypedViewerFilter;import org.eclipse.jdt.internal.ui.preferences.ClasspathVariablesPreferencePage;import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;import org.eclipse.jdt.internal.ui.util.CoreUtility;import org.eclipse.jdt.internal.ui.viewsupport.ImageDisposer;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;import org.eclipse.jdt.internal.ui.wizards.swt.MGridUtil;

public class BuildPathsBlock {
	
	private static final String CLASSPATH= "BuildPathsBlock.classpath";
	private static final String BUILDPATH= "BuildPathsBlock.buildpath";
	
	private static final String TAB_SOURCE= "BuildPathsBlock.tab.source";
	private static final String TAB_PROJECTS= "BuildPathsBlock.tab.projects";
	private static final String TAB_LIBRARIES= "BuildPathsBlock.tab.libraries";
	private static final String TAB_ORDER= "BuildPathsBlock.tab.order";
		
	private static final String BP_INVALIDPATH_ERROR= "BuildPathsBlock.error.InvalidBuildPath";
	private static final String BP_ENTERPATH_ERROR= "BuildPathsBlock.error.EnterBuildPath";
	private static final String BP_PROJNOTEXISTS_ERROR= "BuildPathsBlock.error.BuildPathProjNotExists";
	private static final String BP_CPBP_CLASH_ERROR= "BuildPathsBlock.error.BuildPathInClassPath";
		
	private static final String CP_RECURSIVE_ERROR= "BuildPathsBlock.error.RecursiveClassPath";
	private static final String CP_DUPLICATE_ERROR= "BuildPathsBlock.error.DuplicateEntriesInClassPath";
	private static final String CP_CYCLES_ERROR= "BuildPathsBlock.error.CycleInClassPath";

	private static final String CFD_DIALOG= "BuildPathsBlock.ChooseOutputFolderDialog";
	
	private static final String OPERATION_DESC= "BuildPathsBlock.operationdesc";

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
	
	private ResourceBundle fResourceBundle;
	private IStatusChangeListener fContext;
	private Control fSWTWidget;	
	
	private boolean fIsNewProject;
	
	private SourceContainerWorkbookPage fSourceContainerPage;
	private ProjectsWorkbookPage fProjectsPage;
	private LibrariesWorkbookPage fLibrariesPage;
	
	private BuildPathBasePage fCurrPage;
		
	public BuildPathsBlock(IWorkspaceRoot root, IStatusChangeListener context, boolean isNewProject) {
		fWorkspaceRoot= root;
		fResourceBundle= JavaPlugin.getResourceBundle();
		fContext= context;
		fIsNewProject= isNewProject;
		fSourceContainerPage= null;
		fLibrariesPage= null;
		fProjectsPage= null;
		fCurrPage= null;
				
		BuildPathAdapter adapter= new BuildPathAdapter();			

		fClassPathList= new ListDialogField(new CPListLabelProvider(), ListDialogField.UPDOWN);
		fClassPathList.setDialogFieldListener(adapter);
		fClassPathList.setLabelText(getResourceString(CLASSPATH + ".label"));
		fClassPathList.setUpButtonLabel(getResourceString(CLASSPATH + ".up.button"));
		fClassPathList.setDownButtonLabel(getResourceString(CLASSPATH + ".down.button"));
			
		if (isNewProject) {
			fBuildPathDialogField= new StringDialogField();
		} else {
			StringButtonDialogField dialogField= new StringButtonDialogField(adapter);
			dialogField.setButtonLabel(getResourceString(BUILDPATH + ".button"));
			fBuildPathDialogField= dialogField;
		}
		fBuildPathDialogField.setDialogFieldListener(adapter);
		fBuildPathDialogField.setLabelText(getResourceString(BUILDPATH + ".label"));

		fBuildPathStatus= new StatusInfo();
		fClassPathStatus= new StatusInfo();
		
		fBuildPathDefault= null;
		fCurrProject= null;
		fCurrJProject= null;
		
		fClassPathDefault= null;
		fAddJDKToDefault= true;
	}
	// -------- Resource Bundle ---------
	
	protected String getResourceString(String key) {
		try {
			return fResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";
		}
	}
	
	protected String getFormattedString(String key, String[] args) {
		String str= getResourceString(key);
		return MessageFormat.format(str, args);
	}
	
	protected String getFormattedString(String key, String arg) {
		String str= getResourceString(key);
		return MessageFormat.format(str, new String[] { arg });
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
		folder.setLayoutData(MGridUtil.createFill());
		folder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tabChanged(e.item);
			}	
		});
		
		ImageRegistry imageRegistry= JavaPlugin.getDefault().getImageRegistry();
		
		TabItem item;
				
		fSourceContainerPage= new SourceContainerWorkbookPage(fWorkspaceRoot, fClassPathList, fIsNewProject);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(getResourceString(TAB_SOURCE));
		item.setImage(imageRegistry.get(JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT));
		item.setData(fSourceContainerPage);		
		item.setControl(fSourceContainerPage.getControl(folder));
		
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();	
		Image projectImage= workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT);
		
		fProjectsPage= new ProjectsWorkbookPage(fClassPathList);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(getResourceString(TAB_PROJECTS));
		item.setImage(projectImage);
		item.setData(fProjectsPage);
		item.setControl(fProjectsPage.getControl(folder));
		
		fLibrariesPage= new LibrariesWorkbookPage(fWorkspaceRoot, fClassPathList);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(getResourceString(TAB_LIBRARIES));
		item.setImage(imageRegistry.get(JavaPluginImages.IMG_OBJS_JAR));
		item.setData(fLibrariesPage);
		item.setControl(fLibrariesPage.getControl(folder));
		
		// a non shared image
		Image cpoImage= JavaPluginImages.DESC_TOOL_CLASSPATH_ORDER.createImage();
		composite.addDisposeListener(new ImageDisposer(cpoImage));	
		
		ClasspathOrderingWorkbookPage ordpage= new ClasspathOrderingWorkbookPage(fClassPathList);		
		item= new TabItem(folder, SWT.NONE);
		item.setText(getResourceString(TAB_ORDER));
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
						fBuildPathDialogField.setText("");
					}
					
					List newClassPath= new ArrayList();
					IClasspathEntry[] cp= fCurrJProject.getRawClasspath();
					for (int i= 0; i < cp.length; i++) {
						IClasspathEntry curr= cp[i];
						int entryKind= curr.getEntryKind();
						IResource res= null;
						if (entryKind != IClasspathEntry.CPE_VARIABLE) {
							res= fWorkspaceRoot.findMember(curr.getPath());
						}
						CPListElement elem= new CPListElement(curr, res);
						if (curr.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
							IPackageFragmentRoot root= fCurrJProject.findPackageFragmentRoot(curr.getPath());
							if (root != null) {
								elem.setSourceAttachment(root.getSourceAttachmentPath(), root.getSourceAttachmentRootPath());
								elem.setJavaDocLocation(JavaDocAccess.getJavaDocLocation(root));
							}
						}
						newClassPath.add(elem);						
					}
					fClassPathList.setElements(newClassPath);
				} else {
					setDefaultAttributes(fCurrProject);
				}
			} catch (JavaModelException e) {
				ErrorDialog.openError(getShell(), "Error", "will set to default", e.getStatus());
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
			IClasspathEntry entry= JavaCore.newSourceEntry(project.getFullPath());
			vec.add(new CPListElement(entry, project));
		} else {
			for (int i= 0; i < fClassPathDefault.length; i++) {
				IClasspathEntry entry= fClassPathDefault[i];
				IResource res= fWorkspaceRoot.findMember(entry.getPath());			
				vec.add(new CPListElement(entry, res));
			}
		}
		
		if (fAddJDKToDefault || fClassPathDefault == null) {
			IPath jdkPath= JavaBasePreferencePage.getJDKPath();
			if (jdkPath != null) {
				IResource res= fWorkspaceRoot.findMember(jdkPath);
				CPListElement elem= new CPListElement(JavaCore.newLibraryEntry(jdkPath), res);
				//CPListElement elem= new CPListElement(JavaCore.newVariableEntry(ClasspathVariablesPreferencePage.JDKLIB_VARIABLE), null);
				IPath[] attach= JavaBasePreferencePage.getJDKSourceAttachment();
				if (attach != null) {
					elem.setSourceAttachment(attach[0], attach[1]);
				}
				vec.add(elem);
			}
		}
		return vec;
	}
	
	private IPath getBuildPathDefault(IProject project) {
		if (fBuildPathDefault != null) {
			return fBuildPathDefault;
		} else {
			return project.getFullPath().append("bin");
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
			if (field == fBuildPathDialogField) {
				IFolder folder= chooseFolder();
				if (folder != null) {
					fBuildPathDialogField.setText(folder.getFullPath().toString());
				}
			}
		}
		
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			if (field == fClassPathList) {
				updateClassPathStatus();
				updateBuildPathStatus();
			} else if (field == fBuildPathDialogField) {
				updateBuildPathStatus();
			}
			doStatusLineUpdate();
		}
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
		List elements= fClassPathList.getElements();
		IPath buildPath= new Path(fBuildPathDialogField.getText());
		IClasspathEntry[] entries= new IClasspathEntry[elements.size()];
		
		for (int i= elements.size()-1 ; i >= 0 ; i--) {
			CPListElement currelement= (CPListElement)elements.get(i);
			int entryKind= currelement.getEntryKind();
			IPath curr= currelement.getPath();
			for (int j= 0; j < i; j++) {
				CPListElement cpelement= (CPListElement)elements.get(j);
				IPath p= cpelement.getPath();
				if (curr.equals(p)) {
					// duplicate entry
					fClassPathStatus.setError(getFormattedString(CP_DUPLICATE_ERROR, p.toString()));
					return;
				} else {
					// nested entry (applies only if they are of the same kind)
					if (cpelement.getEntryKind() == entryKind && JavaConventions.isOverlappingRoots(curr, p)) {
						fClassPathStatus.setError(getFormattedString(CP_RECURSIVE_ERROR, new String[] { curr.toString(), p.toString()}));
						return;
					}
				}
			}
			entries[i]= currelement.getClasspathEntry();
		}
		if (fCurrJProject.hasClasspathCycle(entries)) {
			fClassPathStatus.setError(getResourceString(CP_CYCLES_ERROR));
			return;
		}
		fClassPathStatus.setOK();
	}
	
	private void updateBuildPathStatus() {
		fOutputLocationPath= null;
		
		String text= fBuildPathDialogField.getText();
		if ("".equals(text)) {
			fBuildPathStatus.setError(getResourceString(BP_ENTERPATH_ERROR));
			return;
		}
		IPath path= new Path(text);
		
		IResource res= fWorkspaceRoot.findMember(path);
		if (res != null) {
			// if exists, must be a folder or project
			if (res.getType() == IResource.FILE) {
				fBuildPathStatus.setError(getResourceString(BP_INVALIDPATH_ERROR));
				return;
			}
		} else {
			// allows the build path to be in a different project, but must exists and be open
			IPath projPath= path.uptoSegment(1);
			if (!projPath.equals(fCurrProject.getFullPath())) {
				IProject proj= (IProject)fWorkspaceRoot.findMember(projPath);
				if (proj == null || !proj.isOpen()) {
					fBuildPathStatus.setError(getResourceString(BP_PROJNOTEXISTS_ERROR));
					return;
				}
			}
		}
		fOutputLocationPath= path;
		
		/*
		// test if the build path is overlapping with a class path entry
		// (exception: a source contianer may be equal to the bin folder (but not nested))
		Vector elements= fClassPathList.getElements();
		for (int i= elements.size()-1 ; i >= 0 ; i--) {
			CPListElement listElement= (CPListElement)elements.elementAt(i);
			IPath cppath= listElement.getPath();
			if (path.isPrefixOf(cppath)) {
				if ((listElement.getEntryKind() == IClasspathEntry.CPE_SOURCE) && !path.equals(cppath)) {
					fBuildPathStatus.setError(getFormattedString(BP_CPBP_CLASH_ERROR, cppath.toString()));
				}
				return;
			}
		}
		*/		
		
				
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
				monitor.beginTask(getResourceString(OPERATION_DESC), 10);
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
		
		fCurrJProject.setRawClasspath(classpath, new SubProgressMonitor(monitor, 3));

		// attach source for libraries
		for (int i= 0; i < nEntries; i++) {
			CPListElement entry= ((CPListElement)classPathEntries.get(i));
			IClasspathEntry cp= entry.getClasspathEntry();
			if (cp.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				cp= JavaCore.getResolvedClasspathVariable(ClasspathVariablesPreferencePage.JDKLIB_VARIABLE);
			}
			if (cp != null && cp.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				attachSource(cp.getPath(), entry);
			}
		}
		monitor.worked(1);
	}
	
	private void attachSource(IPath path, CPListElement entry) throws CoreException {
		IPackageFragmentRoot root= fCurrJProject.findPackageFragmentRoot(path);
		if (root != null && root.isArchive()) {
			IPath sourcePath= entry.getSourceAttachmentPath();
			IPath sourcePrefix= entry.getSourceAttachmentRootPath();
			root.attachSource(sourcePath, sourcePrefix, null);
			JavaDocAccess.setJavaDocLocation(root, entry.getJavaDocLocation());
		}
	}	
	
	// ---------- util method ------------
		
	private IFolder chooseFolder() {
		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		IProject[] allProjects= fWorkspaceRoot.getProjects();
		ArrayList rejectedElements= new ArrayList(allProjects.length);
		for (int i= 0; i < allProjects.length; i++) {
			if (!allProjects[i].equals(fCurrProject)) {
				rejectedElements.add(allProjects[i]);
			}
		}
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedElements);

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();
		
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), getResourceString(CFD_DIALOG + ".title"), null, lp, cp);
		dialog.setValidator(validator);
		dialog.setMessage(getResourceString(CFD_DIALOG + ".description"));
		dialog.addFilter(filter);
		
		IResource initSelection= null;
		if (fOutputLocationPath != null) {
			initSelection= fWorkspaceRoot.findMember(fOutputLocationPath);
		}
		
		if (dialog.open(fWorkspaceRoot, initSelection) == dialog.OK) {
			return (IFolder)dialog.getSelectedElement();
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