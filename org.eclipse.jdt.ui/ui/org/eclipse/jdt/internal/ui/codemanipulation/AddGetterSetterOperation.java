/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

/**
 * For a given field, method stubs for getter and setters are created
 * If the type already contains a getter or setter for this field, the method will not be added.
 * If the field is open in an editor, be sure to pass over its working copy.
 */
public class AddGetterSetterOperation extends WorkspaceModifyOperation {
	
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
						return "" + Character.toUpperCase(secondLetter) + fieldname.substring(2); //$NON-NLS-1$
					}
				}
				return "" + Character.toUpperCase(firstLetter) + fieldname.substring(1); //$NON-NLS-1$
			}
		}
		return fieldname;
	}
	
	
	private String getArgumentName(String accessorName) {
		if (accessorName.length() > 0) {
			char firstLetter= accessorName.charAt(0);
			if (!Character.isLowerCase(firstLetter)) {
				return "" + Character.toLowerCase(firstLetter) + accessorName.substring(1); //$NON-NLS-1$
			}
		}
		return accessorName;
	}
	
	/**
	 * @see WorkbenchModifyOperation#execute
	 */
	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			monitor.beginTask(CodeManipulationMessages.getString("AddGetterSetterOperation.description"), 2); //$NON-NLS-1$
			
			String fieldName= fField.getElementName();
			String accessorName= evalAccessorName(fieldName);
			String argname= getArgumentName(accessorName);
			
			boolean isStatic= Flags.isStatic(fField.getFlags());
			String typeName= Signature.toString(fField.getTypeSignature());
			
			IType parentType= fField.getDeclaringType();
			
			String lineDelim= StubUtility.getLineDelimiterUsed(parentType);
			int indent= StubUtility.getIndentUsed(parentType) + 1;
			
			// test if the getter already exists
			String getterName= "get" + accessorName; //$NON-NLS-1$
			if (StubUtility.findMethod(getterName, new String[0], false, parentType) == null) {
				// create the getter stub
				StringBuffer buf= new StringBuffer();
				buf.append("\t/**\n"); //$NON-NLS-1$
				buf.append("\t * Gets the "); buf.append(argname); buf.append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
				buf.append("\t * @return Returns a "); buf.append(typeName); buf.append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
				buf.append("\t */\n\t"); //$NON-NLS-1$
				buf.append("public "); //$NON-NLS-1$
				if (isStatic) {
					buf.append("static "); //$NON-NLS-1$
				}
				buf.append(typeName);
				buf.append(' '); buf.append(getterName);
				buf.append("() {\n\t\treturn "); buf.append(fieldName); buf.append(";\n\t}\n"); //$NON-NLS-2$ //$NON-NLS-1$
				
				String formattedContent= StubUtility.codeFormat(buf.toString(), indent, lineDelim) + lineDelim;					
				
				fCreatedGetter= parentType.createMethod(buf.toString(), null, true, null);
			}
			
			monitor.worked(1);
						
			// test if the setter already exists
			String setterName= "set" + accessorName; //$NON-NLS-1$
			String[] args= new String[] { fField.getTypeSignature() };
			if (StubUtility.findMethod(setterName, args, false, parentType) == null) {		
				// create the setter stub
								
				StringBuffer buf= new StringBuffer();
				buf.append("\t/**\n"); //$NON-NLS-1$
				buf.append("\t * Sets the "); buf.append(argname); buf.append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
				buf.append("\t * @param "); buf.append(argname); buf.append(" The "); buf.append(argname); buf.append(" to set\n"); //$NON-NLS-3$ //$NON-NLS-1$ //$NON-NLS-2$
				buf.append("\t */\n\t"); //$NON-NLS-1$
				buf.append("public "); //$NON-NLS-1$
				if (isStatic) {
					buf.append("static "); //$NON-NLS-1$
				}
				buf.append("void "); buf.append(setterName); //$NON-NLS-1$
				buf.append('('); buf.append(typeName); buf.append(' '); 
				buf.append(argname); buf.append(") {\n\t\t"); //$NON-NLS-1$
				if (argname.equals(fieldName)) {
					buf.append("this."); //$NON-NLS-1$
				}
				buf.append(fieldName); buf.append("= "); buf.append(argname); buf.append(";\n\t}\n"); //$NON-NLS-1$ //$NON-NLS-2$
				
				String formattedContent= StubUtility.codeFormat(buf.toString(), indent, lineDelim) + lineDelim;				
				
				fCreatedSetter= parentType.createMethod(formattedContent, null, true, null);
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
