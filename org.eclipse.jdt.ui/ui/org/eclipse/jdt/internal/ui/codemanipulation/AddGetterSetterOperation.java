/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

/**
 * For given fields, method stubs for getters and setters are created.
 */
public class AddGetterSetterOperation extends WorkspaceModifyOperation {

	private IField[] fFields;
	private List fCreatedAccessors;
	
	private IRequestQuery fSkipExistingQuery;
	private IRequestQuery fSkipFinalSettersQuery;
	
	private boolean fSkipAllFinalSetters;
	private boolean fSkipAllExisting;
	
	
	/**
	 * Creates the operation.
	 * @param fields The fields to create setter/getters for.
	 * @param skipFinalSettersQuery Callback to ask if the setter can be skipped for a final field.
	 *        Argument of the query is the final field.
	 * @param skipExistingQuery Callback to ask if setter / getters that already exist can be skipped.
	 *        Argument of the query is the existing method.
	 */
	public AddGetterSetterOperation(IField[] fields, IRequestQuery skipFinalSettersQuery, IRequestQuery skipExistingQuery) {
		super();
		fFields= fields;
		fSkipExistingQuery= skipExistingQuery;
		fSkipFinalSettersQuery= skipFinalSettersQuery;
		
		fCreatedAccessors= new ArrayList();
	}
	
	/**
	 * The policy to evaluate the base name (no 'set'/'get' of the accessor.
	 */
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
	
	/**
	 * Creates the name of the parameter from an accessor name.
	 */	
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
	public void execute(IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {			
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			int nFields= fFields.length;			
			monitor.beginTask(CodeManipulationMessages.getString("AddGetterSetterOperation.description"), nFields); //$NON-NLS-1$
			
			for (int i= 0; i < nFields; i++) {
				generateStubs(fFields[i], new SubProgressMonitor(monitor, 1));
				if (monitor.isCanceled()) {
					throw new InterruptedException();
				}
			}
		} finally {
			monitor.done();
		}
	}
	
	private boolean querySkipFinalSetters(IField field) throws InterruptedException {
		if (!fSkipAllFinalSetters) {
			switch (fSkipFinalSettersQuery.doQuery(field)) {
				case IRequestQuery.CANCEL:
					throw new InterruptedException();
				case IRequestQuery.NO:
					return false;
				case IRequestQuery.YES_ALL:
					fSkipAllFinalSetters= true;
			}
		}
		return true;
	}
	
	private boolean querySkipExistingMethods(IMethod method) throws InterruptedException {
		if (!fSkipAllExisting) {
			switch (fSkipExistingQuery.doQuery(method)) {
				case IRequestQuery.CANCEL:
					throw new InterruptedException();
				case IRequestQuery.NO:
					return false;
				case IRequestQuery.YES_ALL:
					fSkipAllExisting= true;
			}
		}
		return true;
	}	
	
	/**
	 * Creates setter and getter for a given field.
	 */
	private void generateStubs(IField field, IProgressMonitor monitor) throws JavaModelException, InterruptedException {
		try {
			monitor.beginTask(CodeManipulationMessages.getFormattedString("AddGetterSetterOperation.processField", field.getElementName()), 2); //$NON-NLS-1$
	
			fSkipAllFinalSetters= false;
			fSkipAllExisting= false;
	
			String fieldName= field.getElementName();
			String accessorName= evalAccessorName(fieldName);
			String argname= getArgumentName(accessorName);
			
			boolean isStatic= Flags.isStatic(field.getFlags());
			String typeName= Signature.toString(field.getTypeSignature());
			
			IType parentType= field.getDeclaringType();
			
			String lineDelim= StubUtility.getLineDelimiterUsed(parentType);
			int indent= StubUtility.getIndentUsed(parentType) + 1;
			
			// test if the getter already exists
			String getterName= "get" + accessorName; //$NON-NLS-1$
			IMethod existing= StubUtility.findMethod(getterName, new String[0], false, parentType);
			if (!(existing != null &&  querySkipExistingMethods(existing))) {			
				// create the getter stub
				StringBuffer buf= new StringBuffer();
				buf.append("/**\n"); //$NON-NLS-1$
				buf.append(" * Gets the "); buf.append(argname); buf.append(".\n"); //$NON-NLS-1$ //$NON-NLS-2$
				buf.append(" * @return Returns a "); buf.append(typeName); buf.append('\n'); //$NON-NLS-1$
				buf.append(" */\n"); //$NON-NLS-1$
				buf.append("public "); //$NON-NLS-1$
				if (isStatic) {
					buf.append("static "); //$NON-NLS-1$
				}
				buf.append(typeName);
				buf.append(' '); buf.append(getterName);
				buf.append("() {\nreturn "); buf.append(fieldName); buf.append(";\n}\n"); //$NON-NLS-2$ //$NON-NLS-1$
				
				IJavaElement sibling= null;
				if (existing != null) {
					sibling= StubUtility.findSibling(existing);
					existing.delete(false, null);
				}				
				
				String formattedContent= StubUtility.codeFormat(buf.toString(), indent, lineDelim) + lineDelim;
				fCreatedAccessors.add(parentType.createMethod(formattedContent, sibling, true, null));
			}
			
			monitor.worked(1);
									
			String setterName= "set" + accessorName; //$NON-NLS-1$
			String[] args= new String[] { field.getTypeSignature() };		
			
			// test if the setter already exists or field is final
			boolean isFinal= Flags.isFinal(field.getFlags());
			existing= StubUtility.findMethod(setterName, args, false, parentType);
			if (!(isFinal && querySkipFinalSetters(field)) && !(existing != null && querySkipExistingMethods(existing))) {
				// create the setter stub
				StringBuffer buf= new StringBuffer();
				buf.append("/**\n"); //$NON-NLS-1$
				buf.append(" * Sets the "); buf.append(argname); buf.append(".\n"); //$NON-NLS-1$ //$NON-NLS-2$
				buf.append(" * @param "); buf.append(argname); buf.append(" The "); buf.append(argname); buf.append(" to set\n"); //$NON-NLS-3$ //$NON-NLS-1$ //$NON-NLS-2$
				buf.append(" */\n"); //$NON-NLS-1$
				buf.append("public "); //$NON-NLS-1$
				if (isStatic) {
					buf.append("static "); //$NON-NLS-1$
				}
				buf.append("void "); buf.append(setterName); //$NON-NLS-1$
				buf.append('('); buf.append(typeName); buf.append(' '); 
				buf.append(argname); buf.append(") {\n"); //$NON-NLS-1$
				if (argname.equals(fieldName)) {
					if (isStatic) {
						buf.append(parentType.getElementName());
						buf.append('.');
					} else {
						buf.append("this."); //$NON-NLS-1$
					}
				}
				buf.append(fieldName); buf.append("= "); buf.append(argname); buf.append(";\n}\n"); //$NON-NLS-1$ //$NON-NLS-2$
				
				IJavaElement sibling= null;
				if (existing != null) {
					sibling= StubUtility.findSibling(existing);
					existing.delete(false, null);
				}
				
				String formattedContent= StubUtility.codeFormat(buf.toString(), indent, lineDelim) + lineDelim;
				fCreatedAccessors.add(parentType.createMethod(formattedContent, sibling, true, null));
			}
		} finally {
			monitor.done();
		}
	}
	

	/**
	 * Returns the created accessors. To be called after a sucessful run.
	 */
	public IMethod[] getCreatedAccessors() {
		return (IMethod[]) fCreatedAccessors.toArray(new IMethod[fCreatedAccessors.size()]);
	}
}
