/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.sef;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.AddMemberChange;
import org.eclipse.jdt.internal.core.refactoring.text.ChangeVisibilityChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.util.AST;
import org.eclipse.jdt.internal.core.refactoring.util.JavaElementMapper;
import org.eclipse.jdt.internal.core.refactoring.util.TextBufferChangeManager;

public class SelfEncapsulateFieldRefactoring extends Refactoring {

	private IField fField;
	private int fTabWidth;
	private TextBufferChangeManager fChangeManager;
	private CompositeChange fChange;
	
	private FieldDeclaration fFieldDeclaration;
	private TypeDeclaration fTypeDeclaration;

	private String fGetterName;
	private String fSetterName;
	private int fInsertionIndex;
	
	public SelfEncapsulateFieldRefactoring(IField field, ITextBufferChangeCreator creator, int tabWidth) {
		fField= field;
		Assert.isNotNull(fField);
		fTabWidth= tabWidth;
		Assert.isNotNull(creator);
		fChangeManager= new TextBufferChangeManager(creator);
		fChange= new CompositeChange(getName());
		String proposal= createProposal();
		fGetterName= createGetterProposal(proposal);
		fSetterName= createSetterProposal(proposal);
	}
	
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

	public RefactoringStatus checkMethodNames() {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkFieldName(fGetterName));
		result.merge(Checks.checkFieldName(fSetterName));
		return result;
	}
		
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
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
				ITextBufferChange change= fChangeManager.get(unit);
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
	}

	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		CompositeChange result= new CompositeChange(getName());
		pm.beginTask("", 10);
		pm.subTask("Create changes");
		addChanges(result, new SubProgressMonitor(pm, 2));
		ITextBufferChange[] changes= fChangeManager.getAllChanges();
		SubProgressMonitor sub= new SubProgressMonitor(pm, 8);
		sub.beginTask("", changes.length);
		for (int i= 0; i < changes.length; i++) {
			result.addChange(changes[i]);
			sub.worked(1);
		}
		sub.done();
		pm.done();
		return result;
	}

	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Self Encapsulate Field";
	}
	
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
	
	public String createGetterProposal(String proposal) {
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

	private void addChanges(CompositeChange parent, IProgressMonitor pm) throws JavaModelException {
		int insertionIndex= fInsertionIndex;
		int insertionKind= AddMemberChange.INSERT_AFTER;
		if (insertionIndex < 0) {
			insertionIndex= 0;
			insertionKind= AddMemberChange.INSERT_BEFORE;
		}
							
		IMethod method= fField.getDeclaringType().getMethods()[insertionIndex];
		ITextBufferChange change= fChangeManager.get(fField.getCompilationUnit());
		
		change.addSimpleTextChange(new ChangeVisibilityChange(fField, "private"));
		
		String modifiers= createModifiers();
		String type= Signature.toString(fField.getTypeSignature());
		if (!Flags.isFinal(fField.getFlags()))
			change.addSimpleTextChange(createSetterMethod(insertionKind, method, modifiers, type));
		change.addSimpleTextChange(createGetterMethod(insertionKind, method, modifiers, type));
			
	}

	private AddMemberChange createSetterMethod(int insertionKind, IMethod method, String modifiers, String type) throws JavaModelException {
		String argname= createArgName();
		return new AddMemberChange(
			"Add Setter method",
			method,
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
	
	private AddMemberChange createGetterMethod(int insertionKind, IMethod method, String modifiers, String type) {
		return new AddMemberChange(
			"Add Getter method",
			method,
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

