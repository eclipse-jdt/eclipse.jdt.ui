/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.corext.codemanipulation.AddMemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.ChangeVisibilityEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementMapper;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;

public class SelfEncapsulateFieldRefactoring extends Refactoring {

	private IField fField;
	private int fTabWidth;
	private TextChangeManager fChangeManager;
	private CompositeChange fChange;
	
	private FieldDeclaration fFieldDeclaration;
	private TypeDeclaration fTypeDeclaration;

	private String fGetterName;
	private List fUsedGetterNames;
	private String fSetterName;
	private List fUsedSetterNames;
	private int fInsertionIndex;
	
	public SelfEncapsulateFieldRefactoring(IField field, int tabWidth) {
		fField= field;
		Assert.isNotNull(fField);
		fTabWidth= tabWidth;
		fChangeManager= new TextChangeManager();
		fChange= new CompositeChange(getName());
		String proposal= createProposal();
		fGetterName= createGetterProposal(proposal);
		fSetterName= createSetterProposal(proposal);
	}
	
	//---- Setter and Getter methdos ----------------------------------------------------------
	
	public IField getField() {
		return fField;
	}

	public String getGetterName() {
		return fGetterName;
	}
		
	public void setGetterName(String name) {
		fGetterName= name;
		Assert.isNotNull(fGetterName);
	}

	public String getSetterName() {
		return fSetterName;
	}
	
	public void setSetterName(String name) {
		fSetterName= name;
		Assert.isNotNull(fSetterName);
	}
	
	public void setInsertionIndex(int index) {
		fInsertionIndex= index;
	}
	
	//----activation checking ----------------------------------------------------------
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result=  new RefactoringStatus();
		ICompilationUnit unit= fField.getCompilationUnit();
		if (!unit.isStructureKnown()) {
			result.addFatalError(RefactoringCoreMessages.getFormattedString(
				"SelfEncapsulateField.syntax_errors",
				unit.getElementName()));
			return result;
		}
		JavaElementMapper mapper= new JavaElementMapper(fField);
		AstNode node= mapper.getResult();
		if (node == null || !(node instanceof FieldDeclaration)) {
			return mappingErrorFound(result, mapper);
		}
		fFieldDeclaration= (FieldDeclaration)node;
		if (fFieldDeclaration.binding.type.isBaseType()) {
			result.addFatalError("Self Encapsulate Field is not applicable to base types.");
			return result;
		}
		computeUsedNames();
		return result;
	}

	private RefactoringStatus mappingErrorFound(RefactoringStatus result, JavaElementMapper mapper) {
		IProblem[] problems= mapper.getProblems();		
		if (problems.length > 0) {
			result.addFatalError(RefactoringCoreMessages.getFormattedString(
				"SelfEncapsulateField.compilation_error",
				new Object[]{new Integer(problems[0].getSourceLineNumber()), problems[0].getMessage()}));
		} else {
			result.addFatalError("Internal error. Cannot map Java element to AST node.");
		}
		return result;
	}

	//---- Input checking ----------------------------------------------------------
	
	public RefactoringStatus checkMethodNames() {
		RefactoringStatus result= new RefactoringStatus();
		checkName(result, fGetterName, fUsedGetterNames);
		checkName(result, fSetterName, fUsedSetterNames);
		return result;
	}
	
	private static void checkName(RefactoringStatus status, String name, List usedNames) {
		status.merge(Checks.checkMethodName(name));
		for (Iterator iter= usedNames.iterator(); iter.hasNext(); ) {
			String selector= (String)iter.next();
			if (selector.equals(name))
				status.addFatalError(RefactoringCoreMessages.getFormattedString(
					"SelfEncapsulateField.method_exists",
					name));
		}
	}	
	
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try {
			RefactoringStatus result= new RefactoringStatus();
			fChangeManager.clear();
			pm.beginTask("Checking preconditions", 11);
			result.merge(checkMethodNames());
			pm.worked(1);
			if (result.hasFatalError())
				return result;
			pm.setTaskName("Searching for affected compilation units");
			ICompilationUnit[] affectedCUs= RefactoringSearchEngine.findAffectedCompilationUnits(
				new SubProgressMonitor(pm, 5), SearchEngine.createWorkspaceScope(),
				SearchEngine.createSearchPattern(fField, IJavaSearchConstants.REFERENCES));
			pm.setTaskName("Analyzing");	
			IProgressMonitor sub= new SubProgressMonitor(pm, 5);
			sub.beginTask("", affectedCUs.length);
			for (int i= 0; i < affectedCUs.length; i++) {
				ICompilationUnit unit= affectedCUs[i];
				if (unit.isStructureKnown()) {
					sub.subTask(unit.getElementName());
					AST ast= new AST(affectedCUs[i]);
					TextChange change= fChangeManager.get(unit);
					AccessAnalyzer analyzer= new AccessAnalyzer(this, fFieldDeclaration, change);
					ast.accept(analyzer);
					if (ast.hasProblems()) {
						compilerErrorFound(result, unit);
					}
					result.merge(analyzer.getStatus());
					if (result.hasFatalError())
						break;
				} else {
					syntaxErrorFound(result, unit);
				}
				sub.worked(1);
			}
			sub.done();
			return result;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}

	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try {
			CompositeChange result= new CompositeChange(getName());
			pm.beginTask("", 10);
			pm.subTask("Create changes");
			addChanges(result, new SubProgressMonitor(pm, 2));
			TextChange[] changes= fChangeManager.getAllChanges();
			SubProgressMonitor sub= new SubProgressMonitor(pm, 8);
			sub.beginTask("", changes.length);
			for (int i= 0; i < changes.length; i++) {
				result.addChange(changes[i]);
				sub.worked(1);
			}
			sub.done();
			pm.done();
			return result;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}

	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Self Encapsulate Field";
	}
	
	//---- Helper methods -------------------------------------------------------------
	
	private String createProposal() {
		String name= fField.getElementName();
		if (name.length() > 1 && name.charAt(0) == 'f' && Character.isUpperCase(name.charAt(1))) {
			name= name.substring(1);
		} else {
			name= Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
		return name;
	}
	
	private String createSetterProposal(String proposal) {
		return RefactoringCoreMessages.getFormattedString(
			"SelfEncapsulateField.setter",
			proposal);
	}
	
	private String createGetterProposal(String proposal) {
		return RefactoringCoreMessages.getFormattedString(
			"SelfEncapsulateField.getter",
			proposal);
	}
	
	private void compilerErrorFound(RefactoringStatus result, ICompilationUnit unit) {
		result.addError(RefactoringCoreMessages.getFormattedString(
			"SelfEncapsulateField.compiler_errors_update",
			unit.getElementName()));
	}

	private void syntaxErrorFound(RefactoringStatus result, ICompilationUnit unit) {
		result.addError(RefactoringCoreMessages.getFormattedString(
			"SelfEncapsulateField.syntax_errors_update",
			unit.getElementName()));
	}

	private void computeUsedNames() {
		fUsedGetterNames= new ArrayList(0);
		fUsedSetterNames= new ArrayList(0);
		TypeBinding type= fFieldDeclaration.binding.type;
		MethodBinding[] methods= fFieldDeclaration.binding.declaringClass.methods();
		for (int i= 0; i < methods.length; i++) {
			MethodBinding method= methods[i];
			TypeBinding[] parameters= methods[i].parameters;
			if (parameters == null || parameters.length == 0) {
				fUsedGetterNames.add(new String(method.selector));
			} else if (parameters.length == 1 && parameters[0] == type) {
				fUsedSetterNames.add(new String(method.selector));
			}
		}
	}
	
	private void addChanges(CompositeChange parent, IProgressMonitor pm) throws CoreException {
		int insertionKind= AddMemberEdit.INSERT_AFTER;
		IMember sibling= null;
		IMethod[] methods= fField.getDeclaringType().getMethods();
		if (fInsertionIndex < 0) {
			if (methods.length > 0) {
				sibling= methods[0];
				insertionKind= AddMemberEdit.INSERT_BEFORE;
			} else {
				IField[] fields= fField.getDeclaringType().getFields();
				sibling= fields[fields.length - 1];
			}
		} else {
			sibling= methods[fInsertionIndex];
		}
							
		TextChange change= fChangeManager.get(fField.getCompilationUnit());
		
		change.addTextEdit("Change visibility to private", new ChangeVisibilityEdit(fField, "private"));
		
		String modifiers= createModifiers();
		String type= Signature.toString(fField.getTypeSignature());
		if (!Flags.isFinal(fField.getFlags()))
			change.addTextEdit("Add Setter method", createSetterMethod(insertionKind, sibling, modifiers, type));
		change.addTextEdit("Add Getter method", createGetterMethod(insertionKind, sibling, modifiers, type));	
	}

	private TextEdit createSetterMethod(int insertionKind, IMember sibling, String modifiers, String type) throws JavaModelException {
		String argname= createArgName();
		return new AddMemberEdit(
			sibling,
			insertionKind,
			new String[] {
				modifiers + " void " + getSetterName() + "(" + type + " " + argname + ") {",
				(Flags.isStatic(fField.getFlags())
					? fField.getDeclaringType().getElementName() + "."
					: "this.") + fField.getElementName() + " = " + argname + ";",
				"}"
			},
			fTabWidth);
	}
	
	private TextEdit createGetterMethod(int insertionKind, IMember sibling, String modifiers, String type) {
		return new AddMemberEdit(
			sibling,
			insertionKind,
			new String[] {
				modifiers + " " + type + " " + getGetterName() + "() {",
				"return ", fField.getElementName() + ";",
				"}"
			},
			fTabWidth);
	}

	private String createArgName() {
		String result= createProposal();
		return Character.toLowerCase(result.charAt(0)) + result.substring(1);
	}
	
	private String createModifiers() throws JavaModelException {
		StringBuffer result= new StringBuffer();
		int flags= fField.getFlags();
		if (Flags.isPublic(flags))	result.append("public "); //$NON-NLS-1$
		if (Flags.isProtected(flags)) result.append("protected "); //$NON-NLS-1$
		if (Flags.isPrivate(flags))	result.append("private "); //$NON-NLS-1$
		if (Flags.isStatic(flags)) result.append("static "); //$NON-NLS-1$
		return result.toString();
	}
}

