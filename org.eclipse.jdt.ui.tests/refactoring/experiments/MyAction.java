package experiments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/*
  Action Used for to get something running quickly
 
 you should add this stuff to plugin.xml

<extension
      point="org.eclipse.ui.popupMenus">
   <objectContribution
         objectClass="org.eclipse.jdt.core.ICompilationUnit"
         id="org.eclipse.jdt.internal.ui.nls">
      <action
            label="MyAction"
            class="experiments.MyAction"
            menubarPath="additions"
            enablesFor="1"
            id="MyAction">
      </action>
   </objectContribution>
</extension>
*/
public class MyAction implements IWorkbenchWindowActionDelegate {
	
	public void dispose() {
	}
	public void init(IWorkbenchWindow window) {
	}
	public void selectionChanged(IAction action, ISelection selection) {
	}
	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
	}
	
	protected IStructuredSelection getSelection() {
		IWorkbenchWindow window= JavaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			ISelection selection= window.getSelectionService().getSelection();
			if (selection instanceof IStructuredSelection) {
				return (IStructuredSelection) selection;
			}		
		}
		return null;
	}
	
	private ICompilationUnit getCompilationUnit(IStructuredSelection selection) {
		if (selection == null)
			return null;
		return (ICompilationUnit)selection.getFirstElement();
	}
}

