package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;

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
	
	//works for virtual methods
	public static boolean isDeclaredInInterface(IMethod method, IProgressMonitor pm) throws JavaModelException {
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
					if (Checks.findMethod(method, superinterfaces[j]) != null)
						return true;
				}
				subPm.worked(1);
			}
			return false;
		} finally{
			pm.done();
		}
	}
	
	public static boolean overridesAnotherMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		//XXX: use the commented code once this is fixed: 1GCZZS1: ITPJCORE:WINNT - inconsistent search for method declarations
		//XXX: and delete findMethod
		
//		IType declaringType= getMethod().getDeclaringType();	
//		ITypeHierarchy superTypes= declaringType.newSupertypeHierarchy(new SubProgressMonitor(pm, 1));
//		pm.worked(1);
//		IJavaSearchScope scope= SearchEngine.createHierarchyScope(declaringType);
//		ISearchPattern pattern= SearchEngine.createSearchPattern(getMethod(), IJavaSearchConstants.DECLARATIONS);
//		SearchResultCollector collector= new SearchResultCollector(new SubProgressMonitor(pm, 1));
//		new SearchEngine().search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);
//		pm.done();
//		for (Iterator iter= collector.getResults().iterator(); iter.hasNext(); ){
//			SearchResult result= (SearchResult)iter.next();
//			IType t= ((IMethod)result.getEnclosingElement()).getDeclaringType();
//			if ((!t.equals(declaringType)) && superTypes.contains(t))
//				return true;
//		}
//		return false;
		IMethod found= findInHierarchy(method.getDeclaringType().newSupertypeHierarchy(new SubProgressMonitor(pm, 1)), method);
		return (found != null && (! Flags.isStatic(found.getFlags())) && (! Flags.isPrivate(found.getFlags())));	
	}
	
	/**
	 * Finds a method in a type's hierarchy
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * The input type of the hierarchy is not searched for the method
	 * @return The first found method or null, if nothing found
	 */	
	private static final IMethod findInHierarchy(ITypeHierarchy hierarchy, IMethod method) throws JavaModelException {
		IType curr= hierarchy.getSuperclass(hierarchy.getType());
		while (curr != null) {
			IMethod found= Checks.findMethod(method, curr);
			if (found != null) {
				return found;
			}
			curr= hierarchy.getSuperclass(curr);
		}
		return null;
	}
	
}
