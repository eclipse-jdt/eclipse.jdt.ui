/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * For a given field, method stubs for getter and setters are created
 * If the type already contains a getter or setter for this field, the method will not be added.
 * If the field is open in an editor, be sure to pass over its working copy.
 */
public class AddGetterSetterOperation extends WorkspaceModifyOperation {
	
	private static final String OP_DESC= "AddGetterSetterOperation.description";
	
	private IField fField;
	private IMethod fCreatedSetter, fCreatedGetter;
	
	public AddGetterSetterOperation(IField field) {
		super();
		fField= field;
		fCreatedGetter= null;
		fCreatedSetter= null;
	}
	
	private String evalAccessorName(String fieldname) {
		if (fieldname.length() > 0) {
			char firstLetter= fieldname.charAt(0);			
			if (!Character.isUpperCase(firstLetter)) {
				if (fieldname.length() > 1) {
					char secondLetter= fieldname.charAt(1);
					if (Character.isUpperCase(secondLetter)) {
						return fieldname.substring(1);
					}
					if (firstLetter == '_') {
						return "" + Character.toUpperCase(secondLetter) + fieldname.substring(2);
					}
				}
				return "" + Character.toUpperCase(firstLetter) + fieldname.substring(1);
			}
		}
		return fieldname;
	}
	
	
	private String getArgumentName(String accessorName) {
		if (accessorName.length() > 0) {
			char firstLetter= accessorName.charAt(0);
			if (!Character.isLowerCase(firstLetter)) {
				return "" + Character.toLowerCase(firstLetter) + accessorName.substring(1);
			}
		}
		return accessorName;
	}
	
	/**
	 * @see WorkbenchModifyOperation#execute
	 */
	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			String desc= JavaPlugin.getResourceString(OP_DESC);
			monitor.beginTask(desc, 2);
			
			String fieldName= fField.getElementName();
			String accessorName= evalAccessorName(fieldName);
			String argname= getArgumentName(accessorName);
			
			boolean isStatic= Flags.isStatic(fField.getFlags());
			String typeName= Signature.toString(fField.getTypeSignature());
			
			IType parentType= fField.getDeclaringType();
			
			// test if the getter already exists
			String getterName= "get" + accessorName;
			if (StubUtility.findMethod(getterName, new String[0], false, parentType) == null) {
				// create the getter stub
				StringBuffer buf= new StringBuffer();
				buf.append("\t/**\n");
				buf.append("\t * Gets the "); buf.append(argname); buf.append("\n");
				buf.append("\t * @return Returns a "); buf.append(typeName); buf.append("\n");
				buf.append("\t */\n\t");
				buf.append("public ");
				if (isStatic) {
					buf.append("static ");
				}
				buf.append(typeName);
				buf.append(' '); buf.append(getterName);
				buf.append("() {\n\t\treturn "); buf.append(fieldName); buf.append(";\n\t}\n\n");
				
				fCreatedGetter= parentType.createMethod(buf.toString(), null, true, monitor);
			}
			
			monitor.worked(1);
						
			// test if the setter already exists
			String setterName= "set" + accessorName;
			String[] args= new String[] { fField.getTypeSignature() };
			if (StubUtility.findMethod(setterName, args, false, parentType) == null) {		
				// create the setter stub
								
				StringBuffer buf= new StringBuffer();
				buf.append("\t/**\n");
				buf.append("\t * Sets the "); buf.append(argname); buf.append("\n");
				buf.append("\t * @param "); buf.append(argname); buf.append(" The "); buf.append(argname); buf.append(" to set\n");
				buf.append("\t */\n\t");
				buf.append("public ");
				if (isStatic) {
					buf.append("static ");
				}
				buf.append("void "); buf.append(setterName);
				buf.append('('); buf.append(typeName); buf.append(' '); 
				buf.append(argname); buf.append(") {\n\t\t");
				if (argname.equals(fieldName)) {
					buf.append("this.");
				}
				buf.append(fieldName); buf.append("= "); buf.append(argname); buf.append(";\n\t}\n\n");
				
				fCreatedSetter= parentType.createMethod(buf.toString(), null, true, monitor);
			}
			
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}
	
	public IMethod getCreatedSetter() {
		return fCreatedSetter;
	}
	
	public IMethod getCreatedGetter() {
		return fCreatedGetter;
	}	
		
}
