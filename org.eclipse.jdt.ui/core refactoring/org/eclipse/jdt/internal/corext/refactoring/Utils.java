package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class Utils {
	private Utils(){
	}
	
	public static IType getPublicType(ICompilationUnit cu) throws JavaModelException{
		IType[] types= cu.getTypes();
		if (types == null)
			return null;
		for (int i= 0; i < types.length; i++){
			if (Flags.isPublic(types[i].getFlags()))
				return types[i];
		}
		return null;
	}
}

