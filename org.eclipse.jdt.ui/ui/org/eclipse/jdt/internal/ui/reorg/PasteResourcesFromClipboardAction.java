package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.actions.CopyProjectAction;
import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.actions.IRefactoringAction;

public class PasteResourcesFromClipboardAction extends Action implements IRefactoringAction {
	
	private final StructuredSelectionProvider fProvider;

	public PasteResourcesFromClipboardAction(ISelectionProvider selectionProvider) {
		super("Paste");		
		fProvider= StructuredSelectionProvider.createFrom(selectionProvider);
	}

	public void update() {
		setEnabled(canEnableOn(getStructuredSelection()));
	}

	private IStructuredSelection getStructuredSelection() {
		return fProvider.getSelection();
	}

	public void run() {
		//safety net
		update();
		if (! isEnabled())
			return;
		
		IResource[] resourceData = getClipboardResources();		
		if (resourceData == null || resourceData.length == 0)
			return;
			 
		pasteResources(resourceData);
	}

	private void pasteResources(IResource[] resourceData) {
		if (resourceData[0].getType() == IResource.PROJECT)
			pasteProject((IProject) resourceData[0]);
		else
			ClipboardActionUtil.createDnDCopyAction(resourceData, getFirstSelectedResource()).run();
	}
	
	private static void pasteProject(IProject project){
		CopyProjectAction cpa= new CopyProjectAction(getShell());
		cpa.selectionChanged(new StructuredSelection(project));
		if (! cpa.isEnabled())
			return;
		cpa.run();
	}

	//- enablement ---
	private static boolean canEnableOn(IStructuredSelection selection){
		IResource[] resourceData= getClipboardResources();
		if (resourceData == null || resourceData.length == 0)
			return false;
			
		if (ClipboardActionUtil.isOneOpenProject(resourceData))
			return true;
			
		if (selection.size() != 1) //only after we checked the 'one project' case
			return false;
		
		if (StructuredSelectionUtil.getResources(selection).length != 1)
			return false;
		
		if (resourceData == null)
			return ClipboardActionUtil.getFirstResource(selection) instanceof IContainer;	
		
		return canActivateCopyRefactoring(resourceData, ClipboardActionUtil.getFirstResource(selection));
	}

	private static boolean canActivateCopyRefactoring(IResource[] resourceData, IResource selectedResource) {
		try{
			CopyRefactoring ref= createCopyRefactoring(resourceData);
			if (! ref.checkActivation(new NullProgressMonitor()).isOK())
				return false;

			return ref.isValidDestination(ClipboardActionUtil.tryConvertingToJava(selectedResource));
			
		} catch (JavaModelException e){
			return false;
		}	
	}
	
	//-- helpers
	
	private IResource getFirstSelectedResource(){
		return ClipboardActionUtil.getFirstResource(getStructuredSelection());
	}
	
	//--- 		
	private static Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
	}
	
	private static Clipboard getClipboard() {
		return new Clipboard(getShell().getDisplay());
	}
	
	private static IResource[] getClipboardResources() {
		return ((IResource[])getClipboard().getContents(ResourceTransfer.getInstance()));
	}

	private static CopyRefactoring createCopyRefactoring(IResource[] resourceData) {
		return new CopyRefactoring(ClipboardActionUtil.getConvertedResources(resourceData));
	}
}
