/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CodeStyleFix;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Creates fixes which can resolve code style issues 
 * @see org.eclipse.jdt.internal.corext.fix.CodeStyleFix
 */
public class CodeStyleCleanUp extends AbstractCleanUp {
	
	/**
	 * Adds a 'this' qualifier to field accesses.<p>
	 * i.e.:<pre><code>
	 *   int fField;
	 *   void foo() {fField= 10;} -> void foo() {this.fField= 10;}</pre></code>
	 */
	public static final int QUALIFY_FIELD_ACCESS= 1;
	
	
	/**
	 * Changes non static accesses to static members to static accesses.<p>
	 * i.e.:<pre><code>
	 * class E {
	 *   public static int i;
	 *   void foo() {(new E()).i= 10;} -> void foo() {E.i= 10;}
	 * }</code></pre>
	 */
	public static final int CHANGE_NON_STATIC_ACCESS_TO_STATIC= 2;
	
	/**
	 * Qualify static field accesses with declaring type.<p>
	 * i.e.:<pre><code>
	 * class E {
	 *   public static int i;
	 *   void foo() {i= 10;} -> void foo() {E.i= 10;}
	 * }</code></pre>
	 */
	public static final int QUALIFY_STATIC_FIELD_ACCESS= 4;
	
	/**
	 * Changes indirect accesses to static members to direct ones.<p>
	 * i.e.:<pre><code>
	 * class E {public static int i;}
	 * class ESub extends E {
	 *   void foo() {ESub.i= 10;} -> void foo() {E.i= 10;}
	 * }</code></pre>
	 */
	public static final int CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT= 8;

	/**
	 * Adds a 'this' qualifier to method accesses.<p>
	 * i.e.:<pre><code>
	 *   int method(){};
	 *   void foo() {method()} -> void foo() {this.method();}</pre></code>
	 */
	public static final int QUALIFY_METHOD_ACCESS= 16;
	
	/**
	 * Qualifies static method accesses with declaring type.<p>
	 * i.e.:<pre><code>
	 * class E {
	 *   public static int m();
	 *   void foo() {m();} -> void foo() {E.m();}
	 * }</code></pre>
	 */
	public static final int QUALIFY_STATIC_METHOD_ACCESS= 32;
	
	/**
	 * Removes 'this' qualifier to field accesses.<p>
	 * i.e.:<pre><code>
	 *   int fField;
	 *   void foo() {this.fField= 10;} -> void foo() {fField= 10;}</pre></code>
	 */
	public static final int REMOVE_THIS_FIELD_QUALIFIER= 64;

	/**
	 * Removes 'this' qualifier to method accesses.<p>
	 * i.e.:<pre><code>
	 *   int method(){};
	 *   void foo() {this.method()} -> void foo() {method();}</pre></code>
	 */
	public static final int REMOVE_THIS_METHOD_QUALIFIER=128;

	private static final int DEFAULT_FLAG= CHANGE_NON_STATIC_ACCESS_TO_STATIC | CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT;
	private static final String SECTION_NAME= "CleanUp_CodeStyle"; //$NON-NLS-1$

	public CodeStyleCleanUp(int flag) {
		super(flag);
	}

	public CodeStyleCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}
	
	public CodeStyleCleanUp(Map options) {
		super(options);
    }
	
	public CodeStyleCleanUp() {
		super();
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return CodeStyleFix.createCleanUp(compilationUnit, 
				isFlag(QUALIFY_FIELD_ACCESS), 
				isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC), 
				isFlag(QUALIFY_STATIC_FIELD_ACCESS), 
				isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT),
				isFlag(QUALIFY_METHOD_ACCESS),
				isFlag(QUALIFY_STATIC_METHOD_ACCESS),
				isFlag(REMOVE_THIS_FIELD_QUALIFIER),
				isFlag(REMOVE_THIS_METHOD_QUALIFIER));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return CodeStyleFix.createCleanUp(compilationUnit, problems,
				isFlag(QUALIFY_FIELD_ACCESS), 
				isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC), 
				isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC))
			options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.WARNING);
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT))
			options.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.WARNING);
		return options;
	}

	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(getSection(settings, SECTION_NAME));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isFlag(QUALIFY_FIELD_ACCESS))
			result.add(MultiFixMessages.CodeStyleMultiFix_AddThisQualifier_description);
		if (isFlag(QUALIFY_STATIC_FIELD_ACCESS))
			result.add(MultiFixMessages.CodeStyleMultiFix_QualifyAccessToStaticField);
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC))
			result.add(MultiFixMessages.CodeStyleMultiFix_ChangeNonStaticAccess_description);
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT))
			result.add(MultiFixMessages.CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect);
		if (isFlag(QUALIFY_METHOD_ACCESS))
			result.add(MultiFixMessages.CodeStyleCleanUp_QualifyNonStaticMethod_description);
		if (isFlag(QUALIFY_STATIC_METHOD_ACCESS)) 
			result.add(MultiFixMessages.CodeStyleCleanUp_QualifyStaticMethod_description);
		if (isFlag(REMOVE_THIS_FIELD_QUALIFIER))
			result.add(MultiFixMessages.CodeStyleCleanUp_removeFieldThis_description);
		if (isFlag(REMOVE_THIS_METHOD_QUALIFIER))
			result.add(MultiFixMessages.CodeStyleCleanUp_removeMethodThis_description);
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		buf.append("private int value;\n"); //$NON-NLS-1$
		buf.append("public int get() {\n"); //$NON-NLS-1$
		if (isFlag(QUALIFY_FIELD_ACCESS)) {
			buf.append("    return this.value + this.value;\n"); //$NON-NLS-1$
		} else if (isFlag(REMOVE_THIS_FIELD_QUALIFIER)) {
			buf.append("    return value + value;\n"); //$NON-NLS-1$
		} else {
			buf.append("    return this.value + value;\n"); //$NON-NLS-1$
		}
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("public int getZero() {\n"); //$NON-NLS-1$
		if (isFlag(QUALIFY_METHOD_ACCESS)) {
			buf.append("    return this.get() - this.get();\n"); //$NON-NLS-1$
		} else if (isFlag(REMOVE_THIS_METHOD_QUALIFIER)) {
			buf.append("    return get() - get();\n"); //$NON-NLS-1$
		} else {
			buf.append("    return this.get() - get();\n"); //$NON-NLS-1$
		}
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("class E {\n"); //$NON-NLS-1$
		buf.append("    public static int NUMBER;\n"); //$NON-NLS-1$
		buf.append("    public static void set(int i) {\n"); //$NON-NLS-1$
		if (isFlag(QUALIFY_STATIC_FIELD_ACCESS)) {
			buf.append("        E.NUMBER= i;\n"); //$NON-NLS-1$
		} else {
			buf.append("        NUMBER= i;\n"); //$NON-NLS-1$
		}
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("    public void reset() {\n"); //$NON-NLS-1$
		if (isFlag(QUALIFY_STATIC_METHOD_ACCESS)) {
			buf.append("        E.set(0);\n"); //$NON-NLS-1$
		} else {
			buf.append("        set(0);\n"); //$NON-NLS-1$
		}
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("class ESub extends E {\n"); //$NON-NLS-1$
		buf.append("    public void reset() {\n"); //$NON-NLS-1$
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT)) {
			buf.append("        E.NUMBER= 0;\n"); //$NON-NLS-1$
		} else {
			buf.append("        ESub.NUMBER= 0;\n"); //$NON-NLS-1$
		}
		buf.append("    }\n"); //$NON-NLS-1$
		buf.append("}\n"); //$NON-NLS-1$
		buf.append("\n"); //$NON-NLS-1$
		buf.append("public void dec() {\n"); //$NON-NLS-1$
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC)) {
			buf.append("    E.NUMBER--;\n"); //$NON-NLS-1$
		} else {
			buf.append("    (new E()).NUMBER--;\n"); //$NON-NLS-1$
		}
		buf.append("}\n"); //$NON-NLS-1$
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isFlag(QUALIFY_FIELD_ACCESS)) {
			CodeStyleFix fix= CodeStyleFix.createAddFieldQualifierFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT)) {
			CodeStyleFix fix= CodeStyleFix.createIndirectAccessToStaticFix(compilationUnit, problem);
			if (fix != null)
				return true;
		}
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC)) {
			CodeStyleFix[] fixes= CodeStyleFix.createNonStaticAccessFixes(compilationUnit, problem);
			if (fixes != null && fixes.length > 0)
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isFlag(QUALIFY_FIELD_ACCESS))
			result+= getNumberOfProblems(problems, IProblem.UnqualifiedFieldAccess);
		if (isFlag(CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT)) {
			for (int i=0;i<problems.length;i++) {
				int id= problems[i].getID();
				if (id == IProblem.IndirectAccessToStaticField || id == IProblem.IndirectAccessToStaticMethod)
					result++;
			}
		}
		if (isFlag(CHANGE_NON_STATIC_ACCESS_TO_STATIC)) {
			for (int i=0;i<problems.length;i++) {
				int id= problems[i].getID();
				if (id == IProblem.NonStaticAccessToStaticField || id == IProblem.NonStaticAccessToStaticMethod)
					result++;
			}
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDefaultFlag() {
		return DEFAULT_FLAG;
	}
	

	protected int createFlag(Map options) {
		int result= 0;
		
		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS))) {
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS))) {
				result|= QUALIFY_FIELD_ACCESS;
			}
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY))) {
				result|= REMOVE_THIS_FIELD_QUALIFIER;
			}
		}
		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS))) {
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS))) {
				result|= QUALIFY_METHOD_ACCESS;
			}
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY))) {
				result|= REMOVE_THIS_METHOD_QUALIFIER;
			}
		}
		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS))) {
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD))) {
				result|= QUALIFY_STATIC_FIELD_ACCESS;
			}
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD))) {
				result|= QUALIFY_STATIC_METHOD_ACCESS;
			}
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS))) {
				result|= CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT;
			}
			if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS))) {
				result|= CHANGE_NON_STATIC_ACCESS_TO_STATIC;
			}
		}
		return result;
	}
	
}
