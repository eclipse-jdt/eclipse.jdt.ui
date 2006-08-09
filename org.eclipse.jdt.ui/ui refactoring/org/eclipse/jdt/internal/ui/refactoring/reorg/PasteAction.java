/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.CopyProjectOperation;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.TypedSource;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaElementTransfer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ParentChecker;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.workingsets.OthersWorkingSetUpdater;

public class PasteAction extends SelectionDispatchAction{

	private final Clipboard fClipboard;

	public PasteAction(IWorkbenchSite site, Clipboard clipboard) {
		super(site);
		Assert.isNotNull(clipboard);
		fClipboard= clipboard;
		
		setText(ReorgMessages.PasteAction_4); 
		setDescription(ReorgMessages.PasteAction_5); 

		ISharedImages workbenchImages= JavaPlugin.getDefault().getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.PASTE_ACTION);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		// Moved condition checking to run (see http://bugs.eclipse.org/bugs/show_bug.cgi?id=78450)
	}

	private Paster[] createEnabledPasters(TransferData[] availableDataTypes) throws JavaModelException {
		Paster paster;
		Shell shell = getShell();
		List result= new ArrayList(2);
		paster= new ProjectPaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes)) 
			result.add(paster);
		
		paster= new JavaElementAndResourcePaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes)) 
			result.add(paster);

		paster= new TypedSourcePaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes)) 
			result.add(paster);

		paster= new FilePaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes)) 
			result.add(paster);
		
		paster= new WorkingSetPaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes))
			result.add(paster);
		
		paster= new TextPaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes))
			result.add(paster);
		return (Paster[]) result.toArray(new Paster[result.size()]);
	}

	private static Object getContents(final Clipboard clipboard, final Transfer transfer, Shell shell) {
		//see bug 33028 for explanation why we need this
		final Object[] result= new Object[1];
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0]= clipboard.getContents(transfer);
			}
		});
		return result[0];
	}
	
	private static boolean isAvailable(Transfer transfer, TransferData[] availableDataTypes) {
		for (int i= 0; i < availableDataTypes.length; i++) {
			if (transfer.isSupportedType(availableDataTypes[i])) return true;
		}
		return false;
	}

	public void run(IStructuredSelection selection) {
		try {
			TransferData[] availableTypes= fClipboard.getAvailableTypes();
			List elements= selection.toList();
			IResource[] resources= ReorgUtils.getResources(elements);
			IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
			IWorkingSet[] workingSets= ReorgUtils.getWorkingSets(elements);
			Paster[] pasters= createEnabledPasters(availableTypes);
			for (int i= 0; i < pasters.length; i++) {
				if (pasters[i].canPasteOn(javaElements, resources, workingSets)) {
					pasters[i].paste(javaElements, resources, workingSets, availableTypes);
					return;// one is enough
				}
			}
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_disabled); 
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		} catch (InterruptedException e) {
			// OK
		}
	}

	private abstract static class Paster{
		private final Shell fShell;
		private final Clipboard fClipboard2;
		protected Paster(Shell shell, Clipboard clipboard){
			fShell= shell;
			fClipboard2= clipboard;
		}
		protected final Shell getShell() {
			return fShell;
		}
		protected final Clipboard getClipboard() {
			return fClipboard2;
		}

		protected final IResource[] getClipboardResources(TransferData[] availableDataTypes) {
			Transfer transfer= ResourceTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (IResource[])getContents(fClipboard2, transfer, getShell());
			}
			return null;
		}

		protected final IJavaElement[] getClipboardJavaElements(TransferData[] availableDataTypes) {
			Transfer transfer= JavaElementTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (IJavaElement[])getContents(fClipboard2, transfer, getShell());
			}
			return null;
		}
	
		protected final TypedSource[] getClipboardTypedSources(TransferData[] availableDataTypes) {
			Transfer transfer= TypedSourceTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (TypedSource[])getContents(fClipboard2, transfer, getShell());
			}
			return null;
		}
	
		protected final String getClipboardText(TransferData[] availableDataTypes) {
			Transfer transfer= TextTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (String) getContents(fClipboard2, transfer, getShell());
			}
			return null;
		}

		public abstract void paste(IJavaElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaModelException, InterruptedException, InvocationTargetException;
		public abstract boolean canEnable(TransferData[] availableTypes)  throws JavaModelException;
		public abstract boolean canPasteOn(IJavaElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets)  throws JavaModelException;
	}
    
    private static class TextPaster extends Paster {

		private static class CuParser {
			private final IJavaProject fJavaProject;
			private final String fText;
			
			private String fTypeName;
			private String fPackageName;

			public CuParser(IJavaProject javaProject, String text) {
				fJavaProject= javaProject;
				fText= text;
			}

			private void parseText() {
				if (fPackageName != null)
					return;
				
				fPackageName= IPackageFragment.DEFAULT_PACKAGE_NAME;
				ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setProject(fJavaProject);
				parser.setSource(fText.toCharArray());
				CompilationUnit unit= (CompilationUnit) parser.createAST(null);
				
				if (unit == null)
					return;
				
				int typesCount= unit.types().size();
				if (typesCount > 0) {
					// get first most visible type:
					int maxVisibility= Modifier.PRIVATE;
					for (ListIterator iter= unit.types().listIterator(typesCount); iter.hasPrevious();) {
						AbstractTypeDeclaration type= (AbstractTypeDeclaration) iter.previous();
						int visibility= JdtFlags.getVisibilityCode(type);
						if (! JdtFlags.isHigherVisibility(maxVisibility, visibility)) {
							maxVisibility= visibility;
							fTypeName= type.getName().getIdentifier();
						}
					}
				}
				if (fTypeName == null)
					return;
				
				PackageDeclaration pack= unit.getPackage();
				if (pack != null) {
					fPackageName= pack.getName().getFullyQualifiedName();
				}
			}
			
			/**
			 * @return the type name, or <code>null</code> iff the text could not be parsed
			 */
			public String getTypeName() {
				parseText();
				return fTypeName;
			}

			public String getPackageName() {
				parseText();
				return fPackageName;
			}

			public String getText() {
				return fText;
			}
		}
		
		private IPackageFragment fDestinationPack;
		private CuParser fCuParser;
		private TransferData[] fAvailableTypes;
		
		protected TextPaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}
		
		public boolean canEnable(TransferData[] availableTypes) {
			fAvailableTypes= availableTypes;
			return PasteAction.isAvailable(TextTransfer.getInstance(), availableTypes);
		}

		public boolean canPasteOn(IJavaElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets) throws JavaModelException {
			if (selectedWorkingSets.length != 0)
				return false;
			if (resources.length != 0)
				return false; //alternative: create text file?
			if (javaElements.length > 1)
				return false;
			
			String text= getClipboardText(fAvailableTypes);
			IJavaProject javaProject= null;
			IJavaElement destination= null;
			if (javaElements.length == 1) {
				destination= javaElements[0];
				javaProject= destination.getJavaProject();
			}
			fCuParser= new CuParser(javaProject, text);
			
			if (fCuParser.getTypeName() == null)
				return false;
			
			if (destination == null)
				return true;
			
			switch (destination.getElementType()) {
				case IJavaElement.JAVA_PROJECT :
					IPackageFragmentRoot[] packageFragmentRoots= ((IJavaProject) destination).getPackageFragmentRoots();
					for (int i= 0; i < packageFragmentRoots.length; i++) {
						IPackageFragmentRoot packageFragmentRoot= packageFragmentRoots[i];
						if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
							fDestinationPack= packageFragmentRoot.getPackageFragment(fCuParser.getPackageName());
							if (isWritable(fDestinationPack))
								return true;
						}
					}
					return false;
					
				case IJavaElement.PACKAGE_FRAGMENT_ROOT :
					IPackageFragmentRoot packageFragmentRoot= (IPackageFragmentRoot) destination;
					if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
						fDestinationPack= packageFragmentRoot.getPackageFragment(fCuParser.getPackageName());
						return isWritable(fDestinationPack);
					}
					return false;
					
				case IJavaElement.PACKAGE_FRAGMENT :
					fDestinationPack= (IPackageFragment) destination;
					return isWritable(fDestinationPack);
					
				case IJavaElement.COMPILATION_UNIT :
					fDestinationPack= (IPackageFragment) destination.getParent();
					return isWritable(fDestinationPack);
					
				default:
					return false;
			}
		}
		
		private boolean isWritable(IPackageFragment destinationPack) {
			if (destinationPack.exists() && destinationPack.isReadOnly()) {
				return false;
			} else {
				IPackageFragmentRoot packageFragmentRoot= JavaModelUtil.getPackageFragmentRoot(destinationPack);
				try {
					return packageFragmentRoot.exists() && ! packageFragmentRoot.isArchive() && ! packageFragmentRoot.isReadOnly()
							&& packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE;
				} catch (JavaModelException e) {
					return false;
				}
			}
		}

		public void paste(IJavaElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaModelException, InterruptedException, InvocationTargetException{
			final IEditorPart[] editorPart= new IEditorPart[1];
			
			IRunnableWithProgress op= new IRunnableWithProgress() {
				private IPath fVMPath;
				private String fCompilerCompliance;

				public void run(IProgressMonitor pm) throws InvocationTargetException {
					pm.beginTask("", 5); //$NON-NLS-1$
					
					try {
						if (fDestinationPack == null) {
							fDestinationPack= createNewProject(fCuParser, new SubProgressMonitor(pm, 1));
						} else {
							pm.worked(1);
						}
						if (! fDestinationPack.exists()) {
							JavaModelUtil.getPackageFragmentRoot(fDestinationPack).createPackageFragment(fCuParser.getPackageName(), true, new SubProgressMonitor(pm, 1));
						} else {
							pm.worked(1);
						}
						
						final String cuName= fCuParser.getTypeName() + JavaModelUtil.DEFAULT_CU_SUFFIX;
						final ICompilationUnit cu= fDestinationPack.getCompilationUnit(cuName);
						boolean alreadyExists= cu.exists();
						if (alreadyExists) {
							String msg= Messages.format(ReorgMessages.PasteAction_TextPaster_exists, new Object[] {cuName});
							boolean overwrite= MessageDialog.openQuestion(getShell(), ReorgMessages.PasteAction_TextPaster_confirmOverwriting, msg);
							if (! overwrite)
								return;
							
							editorPart[0]= openCu(cu); //Open editor before overwriting to allow undo.
						}
						
						fDestinationPack.createCompilationUnit(cuName, fCuParser.getText(), true, new SubProgressMonitor(pm, 1));
						
						if (!alreadyExists) {
							editorPart[0]= openCu(cu);
						}
						if (!fDestinationPack.getElementName().equals(fCuParser.getPackageName())) {
							if (fDestinationPack.getElementName().length() == 0) {
								removePackageDeclaration(cu);
							} else {
								cu.createPackageDeclaration(fDestinationPack.getElementName(), new SubProgressMonitor(pm, 1));
							}
							if (!alreadyExists && editorPart[0] != null)
								editorPart[0].doSave(new SubProgressMonitor(pm, 1)); //avoid showing error marker due to missing/wrong package declaration
							else
								pm.worked(1);
						} else {
							pm.worked(1);
						}
						BasicNewResourceWizard.selectAndReveal(cu.getResource(), PlatformUI.getWorkbench().getActiveWorkbenchWindow());
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}

				private IPackageFragment createNewProject(CuParser cuParser, SubProgressMonitor pm) throws CoreException {
					pm.beginTask("", 10); //$NON-NLS-1$
					IProject project;
					int i= 1;
					do {
						String name= Messages.format(ReorgMessages.PasteAction_projectName, i == 1 ? (Object) "" : new Integer(i)); //$NON-NLS-1$
						project= JavaPlugin.getWorkspace().getRoot().getProject(name);
						i++;
					} while (project.exists());
					
					BuildPathsBlock.createProject(project, null, new SubProgressMonitor(pm, 3));
					BuildPathsBlock.addJavaNature(project, new SubProgressMonitor(pm, 1));
					IJavaProject javaProject= JavaCore.create(project);
					
					IResource srcFolder;
					IPreferenceStore store= PreferenceConstants.getPreferenceStore();
					String sourceFolderName= store.getString(PreferenceConstants.SRCBIN_SRCNAME);
					if (store.getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ) && sourceFolderName.length() > 0) {
						IFolder folder= project.getFolder(sourceFolderName);
						if (! folder.exists()) {
							folder.create(false, true, new SubProgressMonitor(pm, 1));
						}
						srcFolder= folder;
					} else {
						srcFolder= project;
					}
					
					computeLatestVM();
					if (fCompilerCompliance != null) {
						Map options= javaProject.getOptions(false);
						JavaModelUtil.setCompilanceOptions(options, fCompilerCompliance);
						javaProject.setOptions(options);
					}
					IClasspathEntry srcEntry= JavaCore.newSourceEntry(srcFolder.getFullPath());
					IClasspathEntry jreEntry= JavaCore.newContainerEntry(fVMPath);
					IPath outputLocation= BuildPathsBlock.getDefaultOutputLocation(javaProject);
					IClasspathEntry[] cpes= new IClasspathEntry[] { srcEntry, jreEntry };
					javaProject.setRawClasspath(cpes, outputLocation, new SubProgressMonitor(pm, 1));
					return javaProject.getPackageFragmentRoot(srcFolder).getPackageFragment(cuParser.getPackageName());
				}

				private void computeLatestVM() {
					IVMInstall bestVM= JavaRuntime.getDefaultVMInstall();
					String bestVersion= getVMVersion(bestVM);
					
					IExecutionEnvironmentsManager eeManager= JavaRuntime.getExecutionEnvironmentsManager();
					IExecutionEnvironment bestEE= null;
					
					IExecutionEnvironment[] ees= eeManager.getExecutionEnvironments();
					for (int j= 0; j < ees.length; j++) {
						IExecutionEnvironment ee= ees[j];
						IVMInstall vm= ee.getDefaultVM();
						String ver= getVMVersion(vm);
						if (ver != null && (bestVersion == null || JavaModelUtil.isVersionLessThan(bestVersion, ver))) {
							bestVersion= ver;
							bestEE= ee;
						}
					}
					
					IVMInstallType[] vmTypes= JavaRuntime.getVMInstallTypes();
					for (int i= 0; i < vmTypes.length; i++) {
						IVMInstall[] vms= vmTypes[i].getVMInstalls();
						for (int j= 0; j < vms.length; j++) {
							IVMInstall vm= vms[j];
							String ver= getVMVersion(vm);
							if (ver != null && (bestVersion == null || JavaModelUtil.isVersionLessThan(bestVersion, ver))) {
								bestVersion= ver;
								bestVM= vm;
								bestEE= null;
							}
						}
					}
					
					if (bestEE != null) {
						fVMPath= JavaRuntime.newJREContainerPath(bestEE);
						fCompilerCompliance= bestVersion;
					} else if (bestVM != null) {
						fVMPath= JavaRuntime.newJREContainerPath(bestVM);
						fCompilerCompliance= bestVersion;
					} else {
						fVMPath= JavaRuntime.newDefaultJREContainerPath();
					}
				}

				private String getVMVersion(IVMInstall vm) {
					if (vm instanceof IVMInstall2) {
						IVMInstall2 vm2= (IVMInstall2) vm;
						return JavaModelUtil.getCompilerCompliance(vm2, null);
					} else {
						return null;
					}
				}

				private void removePackageDeclaration(final ICompilationUnit cu) throws JavaModelException, CoreException {
					IPackageDeclaration[] packageDeclarations= cu.getPackageDeclarations();
					if (packageDeclarations.length != 0) {
						ITextFileBuffer buffer= null;
						try {
							buffer= RefactoringFileBuffers.acquire(cu);
							ISourceRange sourceRange= packageDeclarations[0].getSourceRange();
							buffer.getDocument().replace(sourceRange.getOffset(), sourceRange.getLength(), ""); //$NON-NLS-1$
						} catch (BadLocationException e) {
							JavaPlugin.log(e);
						} finally {
							if (buffer != null)
								RefactoringFileBuffers.release(cu);
						}
					}
				}
			};
			
			IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
			if (context == null) {
				context= new BusyIndicatorRunnableContext();
			}
			PlatformUI.getWorkbench().getProgressService().runInUI(context, op, JavaPlugin.getWorkspace().getRoot());
			
			if (editorPart[0] != null)
				editorPart[0].getEditorSite().getPage().activate(editorPart[0]); //activate editor again, since runInUI restores previous active part
		}

		private IEditorPart openCu(ICompilationUnit cu) {
			try {
				return JavaUI.openInEditor(cu, true, true);
			} catch (PartInitException e) {
				JavaPlugin.log(e);
				return null;
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return null;
			}
		}
    }
    
	private static class WorkingSetPaster extends Paster {
		protected WorkingSetPaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}
		public void paste(IJavaElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaModelException, InterruptedException, InvocationTargetException {
			IWorkingSet workingSet= selectedWorkingSets[0];
			Set elements= new HashSet(Arrays.asList(workingSet.getElements()));
			IJavaElement[] javaElements= getClipboardJavaElements(availableTypes);
			if (javaElements != null) {
				for (int i= 0; i < javaElements.length; i++) {
					if (!ReorgUtils.containsElementOrParent(elements, javaElements[i]))
						elements.add(javaElements[i]);
				}
			}
			IResource[] resources= getClipboardResources(availableTypes);
			if (resources != null) {
				List realJavaElements= new ArrayList();
				List realResource= new ArrayList();
				ReorgUtils.splitIntoJavaElementsAndResources(resources, realJavaElements, realResource);
				for (Iterator iter= realJavaElements.iterator(); iter.hasNext();) {
					IJavaElement element= (IJavaElement)iter.next();
					if (!ReorgUtils.containsElementOrParent(elements, element))
						elements.add(element);
				}
				for (Iterator iter= realResource.iterator(); iter.hasNext();) {
					IResource element= (IResource)iter.next();
					if (!ReorgUtils.containsElementOrParent(elements, element))
						elements.add(element);
				}
			}
			workingSet.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
		}
		public boolean canEnable(TransferData[] availableTypes) throws JavaModelException {
			return isAvailable(ResourceTransfer.getInstance(), availableTypes) ||
				isAvailable(JavaElementTransfer.getInstance(), availableTypes);
		}
		public boolean canPasteOn(IJavaElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets) throws JavaModelException {
			if (selectedResources.length != 0 || selectedJavaElements.length != 0 || selectedWorkingSets.length != 1)
				return false;
			IWorkingSet ws= selectedWorkingSets[0];
			return !OthersWorkingSetUpdater.ID.equals(ws.getId());
		}
	}
	
    private static class ProjectPaster extends Paster{
    	
    	protected ProjectPaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}

		public boolean canEnable(TransferData[] availableDataTypes) {
			boolean resourceTransfer= isAvailable(ResourceTransfer.getInstance(), availableDataTypes);
			boolean javaElementTransfer= isAvailable(JavaElementTransfer.getInstance(), availableDataTypes);
			if (! javaElementTransfer)
				return canPasteSimpleProjects(availableDataTypes);
			if (! resourceTransfer)
				return canPasteJavaProjects(availableDataTypes);
			return canPasteJavaProjects(availableDataTypes) && canPasteSimpleProjects(availableDataTypes);
    	}
    	
		public void paste(IJavaElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) {
			pasteProjects(availableTypes);
		}

		private void pasteProjects(TransferData[] availableTypes) {
			pasteProjects(getProjectsToPaste(availableTypes));
		}
		
		private void pasteProjects(IProject[] projects){
			Shell shell= getShell();
			for (int i = 0; i < projects.length; i++) {
				new CopyProjectOperation(shell).copyProject(projects[i]);
			}
		}
		private IProject[] getProjectsToPaste(TransferData[] availableTypes) {
			IResource[] resources= getClipboardResources(availableTypes);
			IJavaElement[] javaElements= getClipboardJavaElements(availableTypes);
			Set result= new HashSet();
			if (resources != null)
				result.addAll(Arrays.asList(resources));
			if (javaElements != null)
				result.addAll(Arrays.asList(ReorgUtils.getNotNulls(ReorgUtils.getResources(javaElements))));
			Assert.isTrue(result.size() > 0);
			return (IProject[]) result.toArray(new IProject[result.size()]);
		}

		public boolean canPasteOn(IJavaElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets) {
			return selectedWorkingSets.length == 0; // Can't paste on working sets here
		}
		
		private boolean canPasteJavaProjects(TransferData[] availableDataTypes) {
			IJavaElement[] javaElements= getClipboardJavaElements(availableDataTypes);
			return 	javaElements != null && 
					javaElements.length != 0 && 
					! ReorgUtils.hasElementsNotOfType(javaElements, IJavaElement.JAVA_PROJECT);
		}

		private boolean canPasteSimpleProjects(TransferData[] availableDataTypes) {
			IResource[] resources= getClipboardResources(availableDataTypes);
			if (resources == null || resources.length == 0) return false;
			for (int i= 0; i < resources.length; i++) {
				if (resources[i].getType() != IResource.PROJECT || ! ((IProject)resources[i]).isOpen())
					return false;
			}
			return true;
		}
    }
    
    private static class FilePaster extends Paster{
		protected FilePaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}

		public void paste(IJavaElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaModelException {
			String[] fileData= getClipboardFiles(availableTypes);
			if (fileData == null)
				return;
    		
			IContainer container= getAsContainer(getTarget(javaElements, resources));
			if (container == null)
				return;
				
			new CopyFilesAndFoldersOperation(getShell()).copyFiles(fileData, container);
		}
		
		private Object getTarget(IJavaElement[] javaElements, IResource[] resources) {
			if (javaElements.length + resources.length == 1){
				if (javaElements.length == 1)
					return javaElements[0];
				else
					return resources[0];
			} else				
				return getCommonParent(javaElements, resources);
		}

		public boolean canPasteOn(IJavaElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets) throws JavaModelException {
			Object target= getTarget(javaElements, resources);
			return target != null && canPasteFilesOn(getAsContainer(target)) && selectedWorkingSets.length == 0;
		}

		public boolean canEnable(TransferData[] availableDataTypes) throws JavaModelException {
			return isAvailable(FileTransfer.getInstance(), availableDataTypes);
		}
				
		private boolean canPasteFilesOn(Object target) {
			boolean isPackageFragment= target instanceof IPackageFragment;
			boolean isJavaProject= target instanceof IJavaProject;
			boolean isPackageFragmentRoot= target instanceof IPackageFragmentRoot;
			boolean isContainer= target instanceof IContainer;
		
			if (!(isPackageFragment || isJavaProject || isPackageFragmentRoot || isContainer)) 
				return false;

			if (isContainer) {
				return true;
			} else {
				IJavaElement element= (IJavaElement)target;
				return !element.isReadOnly();
			}
		}
		
		private IContainer getAsContainer(Object target) throws JavaModelException{
			if (target == null) 
				return null;
			if (target instanceof IContainer) 
				return (IContainer)target;
			if (target instanceof IFile)
				return ((IFile)target).getParent();
			return getAsContainer(((IJavaElement)target).getCorrespondingResource());
		}
		
		private String[] getClipboardFiles(TransferData[] availableDataTypes) {
			Transfer transfer= FileTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (String[])getContents(getClipboard(), transfer, getShell());
			}
			return null;
		}
		private Object getCommonParent(IJavaElement[] javaElements, IResource[] resources) {
			return new ParentChecker(resources, javaElements).getCommonParent();		
		}
    }
    private static class JavaElementAndResourcePaster extends Paster {

		protected JavaElementAndResourcePaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}

		private TransferData[] fAvailableTypes;

		public void paste(IJavaElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaModelException, InterruptedException, InvocationTargetException{
			IResource[] clipboardResources= getClipboardResources(availableTypes);
			if (clipboardResources == null) 
				clipboardResources= new IResource[0];
			IJavaElement[] clipboardJavaElements= getClipboardJavaElements(availableTypes);
			if (clipboardJavaElements == null) 
				clipboardJavaElements= new IJavaElement[0];

			Object destination= getTarget(javaElements, resources);
			if (destination instanceof IJavaElement)
				ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IJavaElement)destination).run(getShell());
			else if (destination instanceof IResource)
				ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IResource)destination).run(getShell());
		}

		private Object getTarget(IJavaElement[] javaElements, IResource[] resources) {
			if (javaElements.length + resources.length == 1){
				if (javaElements.length == 1)
					return javaElements[0];
				else
					return resources[0];
			} else				
				return getCommonParent(javaElements, resources);
		}
		
		private Object getCommonParent(IJavaElement[] javaElements, IResource[] resources) {
			return new ParentChecker(resources, javaElements).getCommonParent();		
		}

		public boolean canPasteOn(IJavaElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets) throws JavaModelException {
			if (selectedWorkingSets.length != 0)
				return false;
			IResource[] clipboardResources= getClipboardResources(fAvailableTypes);
			if (clipboardResources == null) 
				clipboardResources= new IResource[0];
			IJavaElement[] clipboardJavaElements= getClipboardJavaElements(fAvailableTypes);
			if (clipboardJavaElements == null) 
				clipboardJavaElements= new IJavaElement[0];
			Object destination= getTarget(javaElements, resources);
			if (destination instanceof IJavaElement)
				return ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IJavaElement)destination) != null;
			if (destination instanceof IResource)
				return ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IResource)destination) != null;
			return false;
		}
		
		public boolean canEnable(TransferData[] availableTypes) {
			fAvailableTypes= availableTypes;
			return isAvailable(JavaElementTransfer.getInstance(), availableTypes) || isAvailable(ResourceTransfer.getInstance(), availableTypes);
		}
    }
    
    private static class TypedSourcePaster extends Paster{

		protected TypedSourcePaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}
		private TransferData[] fAvailableTypes;

		public boolean canEnable(TransferData[] availableTypes) throws JavaModelException {
			fAvailableTypes= availableTypes;
			return isAvailable(TypedSourceTransfer.getInstance(), availableTypes);
		}

		public boolean canPasteOn(IJavaElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets) throws JavaModelException {
			if (selectedResources.length != 0 || selectedWorkingSets.length != 0)
				return false;
			TypedSource[] typedSources= getClipboardTypedSources(fAvailableTypes);				
			Object destination= getTarget(selectedJavaElements, selectedResources);
			if (destination instanceof IJavaElement)
				return ReorgTypedSourcePasteStarter.create(typedSources, (IJavaElement)destination) != null;
			return false;
		}
		
		public void paste(IJavaElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaModelException, InterruptedException, InvocationTargetException {
			TypedSource[] typedSources= getClipboardTypedSources(availableTypes);
			IJavaElement destination= getTarget(selectedJavaElements, selectedResources);
			ReorgTypedSourcePasteStarter.create(typedSources, destination).run(getShell());		
		}
		
		private static IJavaElement getTarget(IJavaElement[] selectedJavaElements, IResource[] selectedResources) {
			Assert.isTrue(selectedResources.length == 0);
			if (selectedJavaElements.length == 1) 
				return getAsTypeOrCu(selectedJavaElements[0]);
			Object parent= new ParentChecker(selectedResources, selectedJavaElements).getCommonParent();
			if (parent instanceof IJavaElement)
				return getAsTypeOrCu((IJavaElement)parent);
			return null;
		}
		private static IJavaElement getAsTypeOrCu(IJavaElement element) {
			//try to get type first
			if (element.getElementType() == IJavaElement.COMPILATION_UNIT || element.getElementType() == IJavaElement.TYPE)
				return element;
			IJavaElement ancestorType= element.getAncestor(IJavaElement.TYPE);
			if (ancestorType != null)
				return ancestorType;
			return ReorgUtils.getCompilationUnit(element);
		}
		private static class ReorgTypedSourcePasteStarter {
	
			private final PasteTypedSourcesRefactoring fPasteRefactoring;

			private ReorgTypedSourcePasteStarter(PasteTypedSourcesRefactoring pasteRefactoring) {
				Assert.isNotNull(pasteRefactoring);
				fPasteRefactoring= pasteRefactoring;
			}
	
			public static ReorgTypedSourcePasteStarter create(TypedSource[] typedSources, IJavaElement destination) {
				Assert.isNotNull(typedSources);
				Assert.isNotNull(destination);
				PasteTypedSourcesRefactoring pasteRefactoring= PasteTypedSourcesRefactoring.create(typedSources);
				if (pasteRefactoring == null)
					return null;
				if (! pasteRefactoring.setDestination(destination).isOK())
					return null;
				return new ReorgTypedSourcePasteStarter(pasteRefactoring);
			}

			public void run(Shell parent) throws InterruptedException, InvocationTargetException {
				IRunnableContext context= new ProgressMonitorDialog(parent);
				new RefactoringExecutionHelper(fPasteRefactoring, RefactoringCore.getConditionCheckingFailedSeverity(), false, parent, context).perform(false);
			}
		}
		private static class PasteTypedSourcesRefactoring extends Refactoring {
			
			private final TypedSource[] fSources;
			private IJavaElement fDestination;
			
			static PasteTypedSourcesRefactoring create(TypedSource[] sources){
				if (! isAvailable(sources))
					return null;
				return new PasteTypedSourcesRefactoring(sources);
			}
			public RefactoringStatus setDestination(IJavaElement destination) {
				fDestination= destination;
				if (ReorgUtils.getCompilationUnit(destination) == null)
					return RefactoringStatus.createFatalErrorStatus(ReorgMessages.PasteAction_wrong_destination); 
				if (! destination.exists())
					return RefactoringStatus.createFatalErrorStatus(ReorgMessages.PasteAction_element_doesnot_exist); 
				if (! canPasteAll(destination))
					return RefactoringStatus.createFatalErrorStatus(ReorgMessages.PasteAction_invalid_destination); 
				return new RefactoringStatus();
			}
			private boolean canPasteAll(IJavaElement destination) {
				for (int i= 0; i < fSources.length; i++) {
					if (! canPaste(fSources[i].getType(), destination))
						return false;
				}
				return true;
			}
			private static boolean canPaste(int elementType, IJavaElement destination) {
				IType ancestorType= getAncestorType(destination);
				if (ancestorType != null)
					return canPasteToType(elementType);
				return canPasteToCu(elementType);
			}
			private static boolean canPasteToType(int elementType) {
				return 	elementType == IJavaElement.TYPE || 
						elementType == IJavaElement.FIELD || 
						elementType == IJavaElement.INITIALIZER || 
						elementType == IJavaElement.METHOD;
			}
			private static boolean canPasteToCu(int elementType) {
				return	elementType == IJavaElement.PACKAGE_DECLARATION ||
						elementType == IJavaElement.TYPE ||
						elementType == IJavaElement.IMPORT_DECLARATION;
			}
			PasteTypedSourcesRefactoring(TypedSource[] sources){
				Assert.isNotNull(sources);
				Assert.isTrue(sources.length != 0);
				fSources= sources;
			}

			private static boolean isAvailable(TypedSource[] sources) {
				return sources != null && sources.length > 0;
			}

			public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
				return new RefactoringStatus();
			}

			public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
				RefactoringStatus result= Checks.validateModifiesFiles(
					ResourceUtil.getFiles(new ICompilationUnit[]{getDestinationCu()}), getValidationContext());
				return result;
			}

			public Change createChange(IProgressMonitor pm) throws CoreException {
				ASTParser p= ASTParser.newParser(AST.JLS3);
				p.setSource(getDestinationCu());
				CompilationUnit cuNode= (CompilationUnit) p.createAST(pm);
				ASTRewrite rewrite= ASTRewrite.create(cuNode.getAST());
				TypedSource source= null;
				for (int i= fSources.length - 1; i >= 0; i--) {
					source= fSources[i];
					final ASTNode destination= getDestinationNodeForSourceElement(fDestination, source.getType(), cuNode);
					if (destination != null) {
						if (destination instanceof CompilationUnit)
							insertToCu(rewrite, createNewNodeToInsertToCu(source, rewrite), (CompilationUnit) destination);
						else if (destination instanceof AbstractTypeDeclaration)
							insertToType(rewrite, createNewNodeToInsertToType(source, rewrite), (AbstractTypeDeclaration) destination);
					}
				}
				final CompilationUnitChange result= new CompilationUnitChange(ReorgMessages.PasteAction_change_name, getDestinationCu()); 
				try {
					ITextFileBuffer buffer= RefactoringFileBuffers.acquire(getDestinationCu());
					TextEdit rootEdit= rewrite.rewriteAST(buffer.getDocument(), fDestination.getJavaProject().getOptions(true));
					if (getDestinationCu().isWorkingCopy())
						result.setSaveMode(TextFileChange.LEAVE_DIRTY);
					TextChangeCompatibility.addTextEdit(result, ReorgMessages.PasteAction_edit_name, rootEdit); 
				} finally {
					RefactoringFileBuffers.release(getDestinationCu());
				}
				return result;
			}

			private static void insertToType(ASTRewrite rewrite, ASTNode node, AbstractTypeDeclaration typeDeclaration) {
				switch (node.getNodeType()) {
					case ASTNode.ANNOTATION_TYPE_DECLARATION:
					case ASTNode.ENUM_DECLARATION:
					case ASTNode.TYPE_DECLARATION:
					case ASTNode.METHOD_DECLARATION:
					case ASTNode.FIELD_DECLARATION:
					case ASTNode.INITIALIZER:
						rewrite.getListRewrite(typeDeclaration, typeDeclaration.getBodyDeclarationsProperty()).insertAt(node, ASTNodes.getInsertionIndex((BodyDeclaration) node, typeDeclaration.bodyDeclarations()), null);
						break;
					default:
						Assert.isTrue(false, String.valueOf(node.getNodeType()));
				}
			}

			private static void insertToCu(ASTRewrite rewrite, ASTNode node, CompilationUnit cuNode) {
				switch (node.getNodeType()) {
					case ASTNode.TYPE_DECLARATION:
					case ASTNode.ENUM_DECLARATION:
					case ASTNode.ANNOTATION_TYPE_DECLARATION:
						rewrite.getListRewrite(cuNode, CompilationUnit.TYPES_PROPERTY).insertAt(node, ASTNodes.getInsertionIndex((AbstractTypeDeclaration) node, cuNode.types()), null);
						break;
					case ASTNode.IMPORT_DECLARATION:
						rewrite.getListRewrite(cuNode, CompilationUnit.IMPORTS_PROPERTY).insertLast(node, null);
						break;
					case ASTNode.PACKAGE_DECLARATION:
						// only insert if none exists
						if (cuNode.getPackage() == null)
							rewrite.set(cuNode, CompilationUnit.PACKAGE_PROPERTY, node, null);
						break;
					default:
						Assert.isTrue(false, String.valueOf(node.getNodeType()));
				}
			}

			/**
			 * @return an AbstractTypeDeclaration, a CompilationUnit, or null
			 */ 
			private ASTNode getDestinationNodeForSourceElement(IJavaElement destination, int kind, CompilationUnit unit) throws JavaModelException {
				final IType ancestor= getAncestorType(destination);
				if (ancestor != null)
					return ASTNodeSearchUtil.getAbstractTypeDeclarationNode(ancestor, unit);
				if (kind == IJavaElement.TYPE || kind == IJavaElement.PACKAGE_DECLARATION || kind == IJavaElement.IMPORT_DECLARATION || kind == IJavaElement.IMPORT_CONTAINER)
					return unit;
				return null;	
			}
			
			private static IType getAncestorType(IJavaElement destinationElement) {
				return destinationElement.getElementType() == IJavaElement.TYPE ? (IType)destinationElement: (IType)destinationElement.getAncestor(IJavaElement.TYPE);
			}
			private ASTNode createNewNodeToInsertToCu(TypedSource source, ASTRewrite rewrite) {
				switch(source.getType()){
					case IJavaElement.TYPE:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.TYPE_DECLARATION);
					case IJavaElement.PACKAGE_DECLARATION:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.PACKAGE_DECLARATION);
					case IJavaElement.IMPORT_DECLARATION:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.IMPORT_DECLARATION);
					default: Assert.isTrue(false, String.valueOf(source.getType()));
						return null;
				}
			}
			
			private ASTNode createNewNodeToInsertToType(TypedSource source, ASTRewrite rewrite) {
				switch(source.getType()){
					case IJavaElement.TYPE:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.TYPE_DECLARATION);
					case IJavaElement.METHOD:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.METHOD_DECLARATION);
					case IJavaElement.FIELD:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.FIELD_DECLARATION);
					case IJavaElement.INITIALIZER:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.INITIALIZER);
					default: Assert.isTrue(false);
						return null;
				}
			}
			
			private ICompilationUnit getDestinationCu() {
				return ReorgUtils.getCompilationUnit(fDestination);
			}

			public String getName() {
				return ReorgMessages.PasteAction_name; 
			}
		}
    }
}
