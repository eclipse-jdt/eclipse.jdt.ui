package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class MethodChecks {

	//no instances
	private MethodChecks(){
	}
	
	public static boolean isVirtual(IMethod method) throws JavaModelException {
		if (method.isConstructor())
			return false;
		if (method.getDeclaringType().isInterface())
			return false;
		if (JdtFlags.isPrivate(method))	
			return false;
		if (JdtFlags.isStatic(method))	
			return false;
		return true;	
	}	
		
	public static RefactoringStatus checkIfOverridesAnother(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod overrides= MethodChecks.overridesAnotherMethod(method, pm);
		if (overrides == null)
			return null;

		Context context= JavaSourceContext.create(overrides);
		String message= RefactoringCoreMessages.getFormattedString("MethodChecks.overrides", //$NON-NLS-1$
				new String[]{JavaElementUtil.createMethodSignature(overrides), JavaModelUtil.getFullyQualifiedName(overrides.getDeclaringType())});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, overrides, RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD);
	}
	
	public static RefactoringStatus checkIfComesFromInterface(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod inInterface= MethodChecks.isDeclaredInInterface(method, pm);
			
		if (inInterface == null)
			return null;

		Context context= JavaSourceContext.create(inInterface);
		String message= RefactoringCoreMessages.getFormattedString("MethodChecks.implements", //$NON-NLS-1$
				new String[]{JavaElementUtil.createMethodSignature(inInterface), JavaModelUtil.getFullyQualifiedName(inInterface.getDeclaringType())});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, inInterface, RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE);
	}
	
	//works for virtual methods
	private static IMethod isDeclaredInInterface(IMethod method, IProgressMonitor pm) throws JavaModelException {
		try{	
			pm.beginTask("", 4); //$NON-NLS-1$
			ITypeHierarchy hier= method.getDeclaringType().newTypeHierarchy(new SubProgressMonitor(pm, 1));
			IType[] classes= hier.getAllClasses();
			IProgressMonitor subPm= new SubProgressMonitor(pm, 3);
			subPm.beginTask("", classes.length); //$NON-NLS-1$
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
	
	public static IMethod overridesAnotherMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod found= JavaModelUtil.findMethodDeclarationInHierarchy(
						method.getDeclaringType().newSupertypeHierarchy(pm), 
						method.getDeclaringType(), 
						method.getElementName(), 
						method.getParameterTypes(), 
						method.isConstructor());
		
		boolean overrides= (found != null && (! JdtFlags.isStatic(found)) && (! JdtFlags.isPrivate(found)));	
		if (overrides)
			return found;
		else
			return null;	
	}
	
}
