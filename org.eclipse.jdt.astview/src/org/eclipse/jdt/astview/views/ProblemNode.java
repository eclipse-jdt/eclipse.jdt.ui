/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.astview.views;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 *
 */
public class ProblemNode extends ASTAttribute {

	private final IProblem fProblem;
	private final Object fParent;

	public ProblemNode(Object parent, IProblem problem) {
		fParent= parent;
		fProblem= problem;
	}

	@Override
	public Object getParent() {
		return fParent;
	}

	@Override
	public Object[] getChildren() {
		String[] arguments= fProblem.getArguments();
		ArrayList<GeneralAttribute> children= new ArrayList<>();

		children.add(new GeneralAttribute(this, "CONSTANT NAME", getConstantName()));
		children.add(new GeneralAttribute(this, "ID", getErrorLabel()));
		children.add(new GeneralAttribute(this, "OPTION FOR CONFIGURABLE SEVERITY", JavaCore.getOptionForConfigurableSeverity(fProblem.getID())));
		if (fProblem instanceof CategorizedProblem) {
			children.add(new GeneralAttribute(this, "CATEGORY ID", getCategoryCode()));
			children.add(new GeneralAttribute(this, "MARKER TYPE", ((CategorizedProblem) fProblem).getMarkerType()));
		}
		for (int i= 0; i < arguments.length; i++) {
			children.add(new GeneralAttribute(this, "ARGUMENT " + i, arguments[i]));
		}
		return children.toArray();
	}

	@Override
	public String getLabel() {
		StringBuilder buf= new StringBuilder();
		int offset= fProblem.getSourceStart();
		int length= fProblem.getSourceEnd() + 1 - offset;

		if (fProblem.isError())
			buf.append("E");
		if (fProblem.isWarning())
			buf.append("W");
		if (fProblem.isInfo())
			buf.append("I");
		buf.append('[').append(offset).append(", ").append(length).append(']').append(' ');
		buf.append(fProblem.getMessage());

		return buf.toString();
	}

	private String getErrorLabel() {
		int id= fProblem.getID();
		StringBuilder buf= new StringBuilder();

		if ((id & IProblem.TypeRelated) != 0) {
			buf.append("TypeRelated + "); //$NON-NLS-1$
		}
		if ((id & IProblem.FieldRelated) != 0) {
			buf.append("FieldRelated + "); //$NON-NLS-1$
		}
		if ((id & IProblem.ConstructorRelated) != 0) {
			buf.append("ConstructorRelated + "); //$NON-NLS-1$
		}
		if ((id & IProblem.MethodRelated) != 0) {
			buf.append("MethodRelated + "); //$NON-NLS-1$
		}
		if ((id & IProblem.ImportRelated) != 0) {
			buf.append("ImportRelated + "); //$NON-NLS-1$
		}
		if ((id & IProblem.Internal) != 0) {
			buf.append("Internal + "); //$NON-NLS-1$
		}
		if ((id & IProblem.Syntax) != 0) {
			buf.append("Syntax + "); //$NON-NLS-1$
		}
		if ((id & IProblem.Javadoc) != 0) {
			buf.append("Javadoc + "); //$NON-NLS-1$
		}
		buf.append(id & IProblem.IgnoreCategoriesMask);

		buf.append(" = 0x").append(Integer.toHexString(id)).append(" = ").append(id);

		return buf.toString();
	}

	private String getConstantName() {
		int id= fProblem.getID();
		for (Field f : IProblem.class.getFields()) {
			try {
				if (f.getType() == int.class && f.getInt(f) == id) {
					return "IProblem." + f.getName();
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}
		return "<UNKNOWN CONSTANT>";
	}

	private String getCategoryCode() {
		CategorizedProblem categorized= (CategorizedProblem) fProblem;
		int categoryID= categorized.getCategoryID();
		StringBuilder buf= new StringBuilder();

		switch (categoryID) {
			case CategorizedProblem.CAT_UNSPECIFIED:
				buf.append("Unspecified");
				break;

			case CategorizedProblem.CAT_BUILDPATH:
				buf.append("Buildpath");
				break;
			case CategorizedProblem.CAT_SYNTAX:
				buf.append("Syntax");
				break;
			case CategorizedProblem.CAT_IMPORT:
				buf.append("Import");
				break;
			case CategorizedProblem.CAT_TYPE:
				buf.append("Type");
				break;
			case CategorizedProblem.CAT_MEMBER:
				buf.append("Member");
				break;
			case CategorizedProblem.CAT_INTERNAL:
				buf.append("Internal");
				break;
			case CategorizedProblem.CAT_JAVADOC:
				buf.append("Javadoc");
				break;
			case CategorizedProblem.CAT_CODE_STYLE:
				buf.append("Code Style");
				break;
			case CategorizedProblem.CAT_POTENTIAL_PROGRAMMING_PROBLEM:
				buf.append("Potential Programming Problem");
				break;
			case CategorizedProblem.CAT_NAME_SHADOWING_CONFLICT:
				buf.append("Name Shadowing Conflict");
				break;
			case CategorizedProblem.CAT_DEPRECATION:
				buf.append("Deprecation");
				break;
			case CategorizedProblem.CAT_UNNECESSARY_CODE:
				buf.append("Unnecessary Code");
				break;
			case CategorizedProblem.CAT_UNCHECKED_RAW:
				buf.append("Unchecked Raw");
				break;
			case CategorizedProblem.CAT_NLS:
				buf.append("NLS");
				break;
			case CategorizedProblem.CAT_RESTRICTION:
				buf.append("Restriction");
				break;
			case CategorizedProblem.CAT_MODULE:
				buf.append("Module");
				break;
			default:
				buf.append("<UNKNOWN CATEGORY>");
				break;
		}

		buf.append(" = ").append(categoryID);

		return buf.toString();
	}

	@Override
	public Image getImage() {
		return null;
	}

	/**
	 * @return Returns the offset of the problem
	 */
	public int getOffset() {
		return fProblem.getSourceStart();
	}

	/**
	 * @return Returns the length of the problem
	 */
	public int getLength() {
		return fProblem.getSourceEnd() + 1 - fProblem.getSourceStart();
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}

		ProblemNode other= (ProblemNode) obj;
		if (!Objects.equals(fParent, other.fParent)) {
			return false;
		}

		if (!Objects.equals(fProblem, other.fProblem)) {
			return false;
		}

		return true;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0) + (fProblem != null ? fProblem.hashCode() : 0);
	}
}
