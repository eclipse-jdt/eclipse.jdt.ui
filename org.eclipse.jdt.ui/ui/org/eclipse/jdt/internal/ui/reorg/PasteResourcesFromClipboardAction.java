package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.actions.CopyProjectAction;
import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class PasteResourcesFromClipboardAction extends SelectionDispatchAction {

	protected PasteResourcesFromClipboardAction(UnifiedSite site) {
		super(site);
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}
	
	public void run(IStructuredSelection selection) {
		IResource[] resourceData = getClipboardResources();		
		if (resourceData == null || resourceData.length == 0)
			return;
			 
		pasteResources(selection, resourceData);
	}

	private void pasteResources(IStructuredSelection selection, IResource[] resourceData) {
		if (resourceData[0].getType() == IResource.PROJECT)
			pasteProject((IProject) resourceData[0]);
		else
			ReorgActionFactory.createDnDCopyAction(resourceData, getFirstSelectedResource(selection)).run();
	}
	
	private void pasteProject(IProject project){
		CopyProjectAction cpa= new CopyProjectAction(getShell());
		cpa.selectionChanged(new StructuredSelection(project));
		if (! cpa.isEnabled())
			return;
		cpa.run();
	}

	//- enablement ---
	private boolean canOperateOn(IStructuredSelection selection){
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
	
	private IResource getFirstSelectedResource(IStructuredSelection selection){
		return ClipboardActionUtil.getFirstResource(selection);
	}
	
	private Clipboard getClipboard() {
		if (getShell() != null)
			return new Clipboard(getShell().getDisplay());
		else
			return new Clipboard(SWTUtil.getStandardDisplay());	
	}
	
	private IResource[] getClipboardResources() {
		return ((IResource[])getClipboard().getContents(ResourceTransfer.getInstance()));
	}

	private static CopyRefactoring createCopyRefactoring(IResource[] resourceData) {
		return new CopyRefactoring(ClipboardActionUtil.getConvertedResources(resourceData));
	}
}
