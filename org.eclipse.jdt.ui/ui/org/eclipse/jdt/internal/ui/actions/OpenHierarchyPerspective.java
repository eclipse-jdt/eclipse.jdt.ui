package org.eclipse.jdt.internal.ui.actions;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jface.action.Action;import org.eclipse.ui.IPerspectiveDescriptor;import org.eclipse.ui.IPerspectiveRegistry;import org.eclipse.ui.PlatformUI;
public class OpenHierarchyPerspective extends Action {	private IType fType;	private static IPerspectiveRegistry fgRegistry;	
	public OpenHierarchyPerspective(IType type) {		super("Open Hierarchy Perspective");		fType= type;	}	
	/**
	 * @see Action#run()
	 */
	public void run() {		if (fgRegistry == null)			fgRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor pd= fgRegistry.findPerspectiveWithId(JavaUI.ID_HIERARCHYPERSPECTIVE);		if (pd == null)			System.out.println("perspective not found");			}

}
