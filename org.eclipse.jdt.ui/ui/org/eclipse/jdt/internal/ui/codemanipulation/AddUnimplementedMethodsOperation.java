/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

/**
 * Evaluates all unimplemented methods and creates them.
 * If the type is open in an editor, be sure to pass over the types working working copy.
 */
public class AddUnimplementedMethodsOperation extends WorkspaceModifyOperation {

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
			
			monitor.beginTask(CodeManipulationMessages.getString("AddUnimplementedMethodsOperation.description"), 3); //$NON-NLS-1$
			
			ITypeHierarchy hierarchy= fType.newSupertypeHierarchy(new SubProgressMonitor(monitor, 1));
			monitor.worked(1);
			
			ArrayList toImplement= new ArrayList();
			ImportsStructure imports= new ImportsStructure(fType.getCompilationUnit());
			StubUtility.evalUnimplementedMethods(fType, hierarchy, toImplement, imports);
			
			int nToImplement= toImplement.size();
			ArrayList createdMethods= new ArrayList(nToImplement);
			
			String lineDelim= StubUtility.getLineDelimiterUsed(fType);
			int indent= StubUtility.getIndentUsed(fType) + 1;
			
			IMethod lastMethod= null;
			for (int i= 0; i < nToImplement; i++) {
				String content= (String) toImplement.get(i);
				
				String formattedContent= StubUtility.codeFormat(content, indent, lineDelim) + lineDelim;
				lastMethod= fType.createMethod(formattedContent, null, true, null);
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

	/**
	 * Returns the created accessors. To be called after a sucessful run.
	 */	
	public IMethod[] getCreatedMethods() {
		return fCreatedMethods;
	}
		
}
