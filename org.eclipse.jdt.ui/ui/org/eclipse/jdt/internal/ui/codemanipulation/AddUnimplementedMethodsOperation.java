/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.codemanipulation;


import java.util.ArrayList;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.ui.actions.WorkspaceModifyOperation;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.ITypeHierarchy;import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Evaluate all unimplemented methods and create them
 * If the type is open in an editor, be sure to pass over the types working working copy.
 */
public class AddUnimplementedMethodsOperation extends WorkspaceModifyOperation {
	
	private static final String OP_DESC= "AddUnimplementedMethodsOperation.description";
	
	private IType fType;
	private IMethod[] fCreatedMethods;
	private boolean fDoSave;
	
	public AddUnimplementedMethodsOperation(IType type, boolean save) {
		super();
		fType= type;
		fDoSave= save;
		fCreatedMethods= null;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			
			monitor.beginTask(JavaPlugin.getResourceString(OP_DESC), 3);
			
			ITypeHierarchy hierarchy= fType.newSupertypeHierarchy(new SubProgressMonitor(monitor, 1));
			monitor.worked(1);
			
			ArrayList toImplement= new ArrayList();
			ImportsStructure imports= new ImportsStructure(fType.getCompilationUnit());
			StubUtility.evalUnimplementedMethods(fType, hierarchy, toImplement, imports);
			
			int nToImplement= toImplement.size();
			ArrayList createdMethods= new ArrayList(nToImplement);
			
			IMethod lastMethod= null;
			for (int i= 0; i < nToImplement; i++) {
				String content= (String) toImplement.get(i);
				lastMethod= fType.createMethod(content, null, true, null);
				createdMethods.add(lastMethod);
			}
			monitor.worked(1);	

			imports.create(fDoSave, null);
			monitor.worked(1);

			fCreatedMethods= new IMethod[createdMethods.size()];
			createdMethods.toArray(fCreatedMethods);
		} finally {
			monitor.done();
		}
	}
	
	public IMethod[] getCreatedMethods() {
		return fCreatedMethods;
	}
		
}
