package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class GetterSetterUtil {
	
	//no instances
	private GetterSetterUtil(){
	}
	
	public static IMethod getGetter(IField field, String[] namePrefixes, String[] nameSuffixes) throws JavaModelException{
		String getterName= new NameProposer(namePrefixes, nameSuffixes).proposeGetterName(field);
		return JavaModelUtil.findMethod(getterName, new String[0], false, field.getDeclaringType());
	}
	
	public static IMethod getSetter(IField field, String[] namePrefixes, String[] nameSuffixes) throws JavaModelException{
		String[] args= new String[] { field.getTypeSignature() };	
		String setterName= new NameProposer(namePrefixes, nameSuffixes).proposeSetterName(field);
		return JavaModelUtil.findMethod(setterName, args, false, field.getDeclaringType());
	}
}
