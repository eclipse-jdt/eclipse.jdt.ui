package org.eclipse.jdt.internal.ui.launcher;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.debug.core.model.IDebugElement;

import org.eclipse.jdt.debug.core.IJavaVariable;


public class OpenOnVariableAction extends OpenTypeAction {
	
	private static final Set fPrimitiveTypes= initPrimitiveTypes();

	protected IDebugElement getDebugElement(IAdaptable element) {
		return (IDebugElement)element.getAdapter(IJavaVariable.class);
	}
	
	protected String getTypeNameToOpen(IDebugElement element) {
		String refType= ((IJavaVariable)element).getReferenceTypeName();
		refType= removeArray(refType);
		if (fPrimitiveTypes.contains(refType))
			return null;
		return refType;
	}
	
	protected String removeArray(String typeName) {
		if (typeName == null)
			return null;
		int index= typeName.indexOf('[');
		if (index > 0)
			return typeName.substring(0, index);
		return typeName;
	}
	
	public boolean isEnabledFor(Object o){
		if (!(o instanceof IAdaptable))
			return false;
		IJavaVariable element= (IJavaVariable)getDebugElement((IAdaptable)o);
		if (element != null) {
			return getTypeNameToOpen(element) != null;
		}
		return false;
	}

	private static Set initPrimitiveTypes() {
		HashSet set= new HashSet();
		set.add("short");
		set.add("int");
		set.add("long");
		set.add("float");
		set.add("double");
		set.add("boolean");
		set.add("byte");
		set.add("char");
		return set;
	}
}