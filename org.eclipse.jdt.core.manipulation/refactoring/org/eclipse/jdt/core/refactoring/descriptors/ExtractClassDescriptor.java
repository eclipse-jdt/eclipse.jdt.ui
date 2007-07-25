/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.refactoring.descriptors;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

import org.eclipse.jdt.internal.core.refactoring.descriptors.JavaRefactoringDescriptorUtil;

/**
 * Refactoring descriptor for the extract class refactoring.
 * <p>
 * An instance of this refactoring descriptor may be obtained by calling
 * {@link RefactoringContribution#createDescriptor()} on a refactoring
 * contribution requested by invoking
 * {@link RefactoringCore#getRefactoringContribution(String)} with the
 * appropriate refactoring id.
 * </p>
 * 
 * @since 3.4
 */
public class ExtractClassDescriptor extends JavaRefactoringDescriptor {

	private static final String PACKAGE_NAME= "packageName"; //$NON-NLS-1$

	private static final String CLASS_NAME= "className"; //$NON-NLS-1$

	private static final String FIELD_NAME= "fieldName"; //$NON-NLS-1$

	private static final String CREATE_TOP_LEVEL= "createTopLevel"; //$NON-NLS-1$

	private static final String CREATE_GETTER= "createGetter"; //$NON-NLS-1$

	private static final String CREATE_SETTER= "createSetter"; //$NON-NLS-1$

	private static final String ENCLOSING_TYPE= "enclosingType"; //$NON-NLS-1$

	private static final String NEW_FIELD_COUNT= "newFieldCount"; //$NON-NLS-1$

	private static final String CREATE_FIELD_COUNT= "createFieldCount"; //$NON-NLS-1$

	private static final String CREATE_FIELD= "createField"; //$NON-NLS-1$

	private static final String NEW_FIELD_NAME= "newFieldName"; //$NON-NLS-1$

	private static final String OLD_FIELD_NAME= "oldFieldName"; //$NON-NLS-1$

	private static final String OLD_FIELD_COUNT= "oldFieldCount"; //$NON-NLS-1$


	/**
	 * Instances of {@link ExtractClassDescriptor.Field} describe which fields will be moved to
	 * the extracted class and their new name there.
	 */
	public static class Field {
		private final String fFieldName;
		private String fNewFieldName;
		private boolean fCreateField= true;

		private Field(String fieldName) {
			super();
			this.fFieldName= fieldName;
			this.fNewFieldName= fieldName;
		}

		/**
		 * The name of the field in the selected type
		 * 
		 * @return the name of the field in the selected type
		 */
		public String getFieldName() {
			return fFieldName;
		}

		/**
		 * The name of the field in the extracted class. The default is the same as in the selected type
		 * 
		 * @return the name of the field in the extracted class
		 */
		public String getNewFieldName() {
			return fNewFieldName;
		}

		/**
		 * Sets the name of the field in the extracted class. The default is the same as in the selected type
		 * 
		 * @param newFieldName the new field name. Must not be <code>null</code>
		 */
		public void setNewFieldName(String newFieldName) {
			Assert.isNotNull(newFieldName);
			this.fNewFieldName= newFieldName;
		}

		public int hashCode() {
			final int prime= 31;
			int result= 1;
			result= prime * result + ((fFieldName == null) ? 0 : fFieldName.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Field other= (Field) obj;
			if (fFieldName == null) {
				if (other.fFieldName != null)
					return false;
			} else if (!fFieldName.equals(other.fFieldName))
				return false;
			return true;
		}

		public String toString() {
			return "Field:" + fFieldName + " new name:" + fNewFieldName + " create field:" + fCreateField; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}


		/**
		 * Returns whether the field will be moved to extracted class. The default is <code>true</code>
		 * 
		 * @return <code>true</code> if the field will be moved
		 */
		public boolean isCreateField() {
			return fCreateField;
		}

		/**
		 * Sets whether the field will be moved to extracted class. The default is <code>true</code>
		 * 
		 * @param createField if <code>true</code> the field will be moved
		 */
		public void setCreateField(boolean createField) {
			fCreateField= createField;
		}

	}

	private Field[] fFields;

	/**
	 * Creates a new refactoring descriptor.
	 */
	public ExtractClassDescriptor() {
		super(IJavaRefactorings.EXTRACT_CLASS);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 *
	 * @param project
	 *            the non-empty name of the project associated with this
	 *            refactoring, or <code>null</code> for a workspace
	 *            refactoring
	 * @param description
	 *            a non-empty human-readable description of the particular
	 *            refactoring instance
	 * @param comment
	 *            the human-readable comment of the particular refactoring
	 *            instance, or <code>null</code> for no comment
	 * @param arguments
	 * 			  a map of arguments that will be persisted and describes
	 * 			  all settings for this refactoring
	 * @param flags
	 *            the flags of the refactoring descriptor
	 * @throws IllegalArgumentException if the argument map contains invalid keys/values            
	 */
	public ExtractClassDescriptor(String project, String description, String comment, Map arguments, int flags) {
		super(IJavaRefactorings.EXTRACT_CLASS, project, description, comment, arguments, flags);
		if (JavaRefactoringDescriptorUtil.getString(arguments, OLD_FIELD_COUNT, true) != null) {
			String[] oldFieldNames= JavaRefactoringDescriptorUtil.getStringArray(arguments, OLD_FIELD_COUNT, OLD_FIELD_NAME, 0);
			boolean[] createField= JavaRefactoringDescriptorUtil.getBooleanArray(arguments, CREATE_FIELD_COUNT, CREATE_FIELD, 0);
			fFields= new Field[oldFieldNames.length];
			for (int i= 0; i < oldFieldNames.length; i++) {
				fFields[i]= new Field(oldFieldNames[i]);
				fFields[i].setCreateField(createField[i]);
				if (createField[i])
					fFields[i].setNewFieldName(JavaRefactoringDescriptorUtil.getString(arguments, JavaRefactoringDescriptorUtil.getAttributeName(NEW_FIELD_NAME, i)));
			}
		}
	}

	/**
	 * Creates {@link Field} objects for all instance fields of the type
	 * 
	 * @param type the type declaring the field that will be moved to the extracted class
	 * @return an instance of {@link Field} for every field declared in type that is not static
	 * @throws JavaModelException if the type does not exist or if an exception occurs while accessing its corresponding resource. 
	 */
	public static Field[] getFields(IType type) throws JavaModelException {
		IField[] fields= type.getFields();
		ArrayList result= new ArrayList();
		for (int i= 0; i < fields.length; i++) {
			IField field= fields[i];
			if (!Flags.isStatic(field.getFlags()))
				result.add(new Field(field.getElementName()));
		}
		return (Field[]) result.toArray(new Field[result.size()]);
	}

	/**
	 * 
	 * @param fields
	 */
	public void setFields(Field[] fields) {
		fFields= fields;
	}

	public Field[] getFields() {
		return fFields;
	}

	public IType getType() {
		return (IType) JavaRefactoringDescriptorUtil.getJavaElement(fArguments, ATTRIBUTE_INPUT, getProject());
	}

	public void setType(IType type) {
		Assert.isNotNull(type);
		String project= type.getJavaProject().getElementName();
		setProject(project);
		JavaRefactoringDescriptorUtil.setJavaElement(fArguments, ATTRIBUTE_INPUT, project, type);
	}

	public String getPackage() {
		return JavaRefactoringDescriptorUtil.getString(fArguments, PACKAGE_NAME, true);
	}

	public void setPackage(String packageName) {
		JavaRefactoringDescriptorUtil.setString(fArguments, PACKAGE_NAME, packageName);
	}

	public String getClassName() {
		return JavaRefactoringDescriptorUtil.getString(fArguments, CLASS_NAME, true);
	}

	public void setClassName(String className) {
		JavaRefactoringDescriptorUtil.setString(fArguments, CLASS_NAME, className);
	}

	public String getFieldName() {
		return JavaRefactoringDescriptorUtil.getString(fArguments, FIELD_NAME, true);
	}

	public void setFieldName(String fieldName) {
		JavaRefactoringDescriptorUtil.setString(fArguments, FIELD_NAME, fieldName);
	}

	public boolean isCreateTopLevel() {
		return JavaRefactoringDescriptorUtil.getBoolean(fArguments, CREATE_TOP_LEVEL, true);
	}

	public void setCreateTopLevel(boolean createTopLevel) {
		JavaRefactoringDescriptorUtil.setBoolean(fArguments, CREATE_TOP_LEVEL, createTopLevel);
	}

	public boolean isCreateGetter() {
		return JavaRefactoringDescriptorUtil.getBoolean(fArguments, CREATE_GETTER, false);
	}

	public void setCreateGetter(boolean createGetter) {
		JavaRefactoringDescriptorUtil.setBoolean(fArguments, CREATE_GETTER, createGetter);
	}

	public boolean isCreateSetter() {
		return JavaRefactoringDescriptorUtil.getBoolean(fArguments, CREATE_SETTER, false);
	}

	public void setCreateSetter(boolean createSetter) {
		JavaRefactoringDescriptorUtil.setBoolean(fArguments, CREATE_SETTER, createSetter);
	}

	public String getEnclosingType() {
		return JavaRefactoringDescriptorUtil.getString(fArguments, ENCLOSING_TYPE, true);
	}

	public void setEnclosingType(String enclosingType) {
		JavaRefactoringDescriptorUtil.setString(fArguments, ENCLOSING_TYPE, enclosingType);
	}

	protected void populateArgumentMap() {
		super.populateArgumentMap();
		if (fFields != null) {
			String[] oldFieldNames= new String[fFields.length];
			String[] newFieldNames= new String[fFields.length];
			boolean[] createField= new boolean[fFields.length];
			for (int i= 0; i < fFields.length; i++) {
				Field field= fFields[i];
				Assert.isNotNull(field);
				oldFieldNames[i]= field.getFieldName();
				createField[i]= field.isCreateField();
				if (field.isCreateField())
					newFieldNames[i]= field.getNewFieldName();
			}
			JavaRefactoringDescriptorUtil.setStringArray(fArguments, OLD_FIELD_COUNT, OLD_FIELD_NAME, oldFieldNames, 0);
			JavaRefactoringDescriptorUtil.setStringArray(fArguments, NEW_FIELD_COUNT, NEW_FIELD_NAME, newFieldNames, 0);
			JavaRefactoringDescriptorUtil.setBooleanArray(fArguments, CREATE_FIELD_COUNT, CREATE_FIELD, createField, 0);
		}
	}

}
