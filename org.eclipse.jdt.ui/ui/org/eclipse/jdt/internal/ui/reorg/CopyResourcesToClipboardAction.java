package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.actions.IRefactoringAction;

class CopyResourcesToClipboardAction extends Action implements IRefactoringAction {
	
	private static final String fgLineDelim= System.getProperty("line.separator"); //$NON-NLS-1$
	private final StructuredSelectionProvider fProvider;
	
	public CopyResourcesToClipboardAction(ISelectionProvider selectionProvider) {
		super("&Copy");
		fProvider= StructuredSelectionProvider.createFrom(selectionProvider);
	}

	private IStructuredSelection getStructuredSelection() {
		return fProvider.getSelection();
	}

	public void update() {
		setEnabled(canOperateOn(getStructuredSelection()));
	}

	public void run() {
		//safety net
		update();
		if (! isEnabled())
			return;
			
		IResource[] resources= getSelectedResources();
		getClipboard().setContents(
			new Object[] { 
					resources, 
					getFileLocations(resources), 
					getFileNamesText(resources)}, 
			new Transfer[] { 
					ResourceTransfer.getInstance(), 
					FileTransfer.getInstance(), 
					TextTransfer.getInstance()});
	}
	
	public static boolean canOperateOn(IStructuredSelection selection){
		if (StructuredSelectionUtil.hasNonResources(selection)) 
			return false;
		
		IResource[] selectedResources= StructuredSelectionUtil.getResources(selection);
		if (selectedResources.length == 0)
			return false;
		
		if (! areResourcesOfValidType(selectedResources))
			return false;

		if (ClipboardActionUtil.isOneOpenProject(selectedResources))
			return true;
		
		if (! haveCommonParent(selectedResources))
			return false;
		
		IRefactoringAction ca= ClipboardActionUtil.createDnDCopyAction(selection.toList(), ClipboardActionUtil.getFirstResource(selection));
		ca.update();
		return ca.isEnabled();
	}
	
	private static boolean areResourcesOfValidType(IResource[] resources){
		boolean onlyProjectsSelected= ClipboardActionUtil.resourcesAreOfType(resources, IResource.PROJECT);
		boolean onlyFilesFoldersSelected= ClipboardActionUtil.resourcesAreOfType(resources, IResource.FILE | IResource.FOLDER);

		if (!onlyFilesFoldersSelected && !onlyProjectsSelected)
			return false;
		if (onlyFilesFoldersSelected && onlyProjectsSelected)
			return false;
	
		return true;
	}
	
	private static boolean haveCommonParent(IResource[] resources){
		if (haveCommonParentAsResources(resources))
			return true;
			
		/* 
		 * special case - must be able to select packages:
		 * p
		 * p.q
		 */
		if (! ClipboardActionUtil.resourcesAreOfType(resources, IResource.FOLDER)) 
			return false;

		IPackageFragment[] packages= getPackages(resources); 
		if (packages.length != resources.length)
			return false;
			
		IJavaElement firstJavaParent= packages[0].getParent();
		if (firstJavaParent == null)
			return false;
		
		for (int i= 0; i < packages.length; i++) {
			if (! firstJavaParent.equals(packages[i].getParent()))
				return false;	
		}	
		return true;	
	}
	
	private static IPackageFragment[] getPackages(IResource[] resources){
		List packages= new ArrayList(resources.length);
		for (int i= 0; i < resources.length; i++) {
			IJavaElement element= JavaCore.create(resources[i]);
			if (element != null && element.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
				packages.add(element);
		}
		return (IPackageFragment[]) packages.toArray(new IPackageFragment[packages.size()]);
	}
	
	private static boolean haveCommonParentAsResources(IResource[] resources){
		IContainer firstParent = resources[0].getParent();
		if (firstParent == null) 
			return false;
	
		for (int i= 0; i < resources.length; i++) {
			if (!resources[i].getParent().equals(firstParent)) 
				return false;
		}
		return true;
	}

	private IResource[] getSelectedResources() {
		return StructuredSelectionUtil.getResources(getStructuredSelection());
	}

	private static String getFileNamesText(IResource[] resources) {
		ILabelProvider labelProvider= getLabelProvider();
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < resources.length; i++) {
			if (i > 0)
				buf.append(fgLineDelim);	
			buf.append(getName(resources[i], labelProvider));
		}
		return buf.toString();
	}
	
	private static ILabelProvider getLabelProvider(){
		return new JavaElementLabelProvider(
			JavaElementLabelProvider.SHOW_VARIABLE
			+ JavaElementLabelProvider.SHOW_PARAMETERS
			+ JavaElementLabelProvider.SHOW_TYPE
		);		
	}

	private static String getName(IResource resource, ILabelProvider labelProvider){
		IJavaElement javeElement= JavaCore.create(resource);
		if (javeElement == null)
			return labelProvider.getText(resource);	
		else
			return labelProvider.getText(javeElement);
	}
	
	private static String[] getFileLocations(IResource[] resources) {
		String[] fileLocations= new String[resources.length];
		for (int i= 0; i < resources.length; i++) {
			fileLocations[i]= resources[i].getLocation().toOSString();
		}
		return fileLocations;
	}

	private static Clipboard getClipboard() {
		return new Clipboard(JavaPlugin.getActiveWorkbenchShell().getDisplay());
	}
}