package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.CopyProjectAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;

public class JdtCopyAction extends ReorgDestinationAction {

	protected JdtCopyAction(IWorkbenchSite site) {
		super(site);
	}

	ReorgRefactoring createRefactoring(List elements){
		return new CopyRefactoring(elements, new ReorgQueries(), createUpdateClasspathQuery(getShell()));
	}
	
	String getActionName() {
		return ReorgMessages.getString("copyAction.name"); //$NON-NLS-1$
	}
	
	String getDestinationDialogMessage() {
		return ReorgMessages.getString("copyAction.destination.label"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * @see IRefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		if (ClipboardActionUtil.hasOnlyProjects(selection))
			return selection.size() == 1;
		else
			return super.canOperateOn(selection);
	}
	
	protected void run(IStructuredSelection selection) {
		if (ClipboardActionUtil.hasOnlyProjects(selection)){
			copyProject(selection);
		}	else {
			super.run(selection);
		}
	}

	private void copyProject(IStructuredSelection selection){
		CopyProjectAction action= new CopyProjectAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(selection);
		action.run();
	}

	public static IPackageFragmentRootManipulationQuery createUpdateClasspathQuery(Shell shell){
		String messagePattern= ReorgMessages.getString("JdtCopyAction.referenced") + //$NON-NLS-1$
			ReorgMessages.getString("JdtCopyAction.update_classpath"); //$NON-NLS-1$
		return new PackageFragmentRootManipulationQuery(shell, ReorgMessages.getString("JdtCopyAction.Copy"), messagePattern); //$NON-NLS-1$
	}
}
