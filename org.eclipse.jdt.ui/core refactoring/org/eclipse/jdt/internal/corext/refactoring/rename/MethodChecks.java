package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry.Context;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class MethodChecks {

	//no instances
	private MethodChecks(){
	}
	
	public static boolean isVirtual(IMethod method) throws JavaModelException {
		if (method.getDeclaringType().isInterface())
			return false;
		if (Flags.isPrivate(method.getFlags()))	
			return false;
		if (Flags.isStatic(method.getFlags()))	
			return false;
		return true;	
	}	
		
	public static RefactoringStatus checkIfOverridesAnother(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod overrides= MethodChecks.overridesAnotherMethod(method, pm);
		if (overrides == null)
			return null;

		Context context= JavaSourceContext.create(overrides);
		String msg= "The selected method overrides method \'" + JavaElementUtil.createMethodSignature(overrides) + "\'"
						 + " declared in type \'" + overrides.getDeclaringType().getFullyQualifiedName() + "\'. Reform the operation there.";
		return RefactoringStatus.createFatalErrorStatus(msg, context);
	}
	
	public static RefactoringStatus checkIfComesFromInterface(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod inInterface= MethodChecks.isDeclaredInInterface(method, pm);
			
		if (inInterface == null)
			return null;

		Context context= JavaSourceContext.create(inInterface);
		String msg= "The selected method is an implementation of method \'" + JavaElementUtil.createMethodSignature(inInterface) + "\'"
						 + " declared in type \'" + inInterface.getDeclaringType().getFullyQualifiedName() + "\'.";
		return RefactoringStatus.createFatalErrorStatus(msg, context);
	}
	
	//works for virtual methods
	private static IMethod isDeclaredInInterface(IMethod method, IProgressMonitor pm) throws JavaModelException {
		try{	
			pm.beginTask("", 4);
			ITypeHierarchy hier= method.getDeclaringType().newTypeHierarchy(new SubProgressMonitor(pm, 1));
			IType[] classes= hier.getAllClasses();
			IProgressMonitor subPm= new SubProgressMonitor(pm, 3);
			subPm.beginTask("", classes.length);
			for (int i= 0; i < classes.length; i++) {
				ITypeHierarchy superTypes= classes[i].newSupertypeHierarchy(new SubProgressMonitor(subPm, 1));
				IType[] superinterfaces= superTypes.getAllSuperInterfaces(classes[i]);
				for (int j= 0; j < superinterfaces.length; j++) {
					IMethod found= Checks.findMethod(method, superinterfaces[j]);
					if (found != null)
						return found;
				}
				subPm.worked(1);
			}
			return null;
		} finally{
			pm.done();
		}
	}
	
	private static IMethod overridesAnotherMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod found= JavaModelUtil.findMethodImplementationInHierarchy(
						method.getDeclaringType().newSupertypeHierarchy(pm), 
						method.getDeclaringType(), 
						method.getElementName(), 
						method.getParameterTypes(), 
						method.isConstructor());
		
		boolean overrides= (found != null && (! Flags.isStatic(found.getFlags())) && (! Flags.isPrivate(found.getFlags())));	
		if (overrides)
			return found;
		else
			return null;	
	}
	
}
