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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ChangeVisibilityEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.BindingIdentifier;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.JavaElementMapper;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class SelfEncapsulateFieldRefactoring extends Refactoring {

	private IField fField;
	private CodeGenerationSettings fSettings;
	private TextChangeManager fChangeManager;
	private CompositeChange fChange;
	
	private VariableDeclarationFragment fFieldDeclaration;
	// private TypeDeclaration fTypeDeclaration;

	private String fGetterName;
	private String fSetterName;
	private String fArgName;
	private boolean fSetterMustReturnValue;
	private int fInsertionIndex;
	private boolean fEncapsulateDeclaringClass;
	
	private List fUsedReadNames;
	private List fUsedModifyNames;
	
	public SelfEncapsulateFieldRefactoring(IField field, CodeGenerationSettings settings) {
		Assert.isNotNull(field);
		Assert.isNotNull(settings);
		fField= field;
		fSettings= settings;
		fChangeManager= new TextChangeManager();
		fChange= new CompositeChange(getName());
		NameProposer proposer= new NameProposer(fSettings.fieldPrefixes, fSettings.fieldSuffixes);
		fGetterName= "get" + proposer.proposeAccessorName(field);
		fSetterName= "set" + proposer.proposeAccessorName(field);
		fEncapsulateDeclaringClass= true;
		fArgName= proposer.proposeArgName(field);
		checkArgName();
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
	
	public void setEncapsulateDeclaringClass(boolean encapsulateDeclaringClass) {
		fEncapsulateDeclaringClass= encapsulateDeclaringClass;
	}

	public boolean getEncapsulateDeclaringClass() {
		return fEncapsulateDeclaringClass;
	}

	//----activation checking ----------------------------------------------------------
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result=  new RefactoringStatus();
		ASTNode node= JavaElementMapper.perform(fField, VariableDeclarationFragment.class);
		if (node == null || !(node instanceof VariableDeclarationFragment)) {
			return mappingErrorFound(result, node);
		}
		fFieldDeclaration= (VariableDeclarationFragment)node;
		if (fFieldDeclaration.resolveBinding() == null) {
			if (!processCompilerError(result, node))
				result.addFatalError("The type of the selected field cannot be resolved. May be an import statement is missing.");
			return result;
		}
		computeUsedNames();
		return result;
	}

	private RefactoringStatus mappingErrorFound(RefactoringStatus result, ASTNode node) {
		if (node != null && (node.getFlags() & ASTNode.MALFORMED) != 0 && processCompilerError(result, node))
			return result;
		result.addFatalError(getMappingErrorMessage());
		return result;
	}

	private boolean processCompilerError(RefactoringStatus result, ASTNode node) {
		Message[] messages= ASTNodes.getMessages(node, ASTNodes.INCLUDE_ALL_PARENTS);
		if (messages.length == 0)
			return false;
		result.addFatalError(RefactoringCoreMessages.getFormattedString(
			"SelfEncapsulateField.compiler_errors_field", 
			new String[] { fField.getElementName(), messages[0].getMessage()}));
		return true;
	}

	private String getMappingErrorMessage() {
		return "Cannot analyze selected field " + fField.getElementName();
	}

	//---- Input checking ----------------------------------------------------------
	
	public RefactoringStatus checkMethodNames() {
		RefactoringStatus result= new RefactoringStatus();
		IType declaringType= fField.getDeclaringType();
		checkName(result, fGetterName, fUsedReadNames, declaringType);
		checkName(result, fSetterName, fUsedModifyNames, declaringType);
		return result;
	}
	
	private static void checkName(RefactoringStatus status, String name, List usedNames, IType type) {
		if ("".equals(name)) { //$NON-NLS-1$
			status.addFatalError(RefactoringCoreMessages.getString("Checks.Choose_name")); //$NON-NLS-1$
			return;
	    }
		status.merge(Checks.checkMethodName(name));
		for (Iterator iter= usedNames.iterator(); iter.hasNext(); ) {
			IMethodBinding method= (IMethodBinding)iter.next();
			String selector= method.getName();
			if (selector.equals(name))
				status.addFatalError(RefactoringCoreMessages.getFormattedString(
					"SelfEncapsulateField.method_exists",
					new String[] {Bindings.asString(method), type.getElementName()}));
		}
	}	
	
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try {
			RefactoringStatus result= new RefactoringStatus();
			fChangeManager.clear();
			pm.beginTask("", 11);
			pm.setTaskName("Checking preconditions");
			pm.subTask("");	// XXX: http://bugs.eclipse.org/bugs/show_bug.cgi?id=6794
			result.merge(checkMethodNames());
			pm.worked(1);
			if (result.hasFatalError())
				return result;
			pm.setTaskName("Searching for affected compilation units");
			pm.subTask(""); 	// XXX: http://bugs.eclipse.org/bugs/show_bug.cgi?id=6794
			ICompilationUnit[] affectedCUs= RefactoringSearchEngine.findAffectedCompilationUnits(
				new SubProgressMonitor(pm, 5), SearchEngine.createWorkspaceScope(),
				SearchEngine.createSearchPattern(fField, IJavaSearchConstants.REFERENCES));
			pm.setTaskName("Analyzing");	
			IProgressMonitor sub= new SubProgressMonitor(pm, 5);
			sub.beginTask("", affectedCUs.length);
			BindingIdentifier fieldIdentifier= new BindingIdentifier(fFieldDeclaration.resolveBinding());
			BindingIdentifier declaringClass= new BindingIdentifier(
				((TypeDeclaration)ASTNodes.getParent(fFieldDeclaration, TypeDeclaration.class)).resolveBinding());
			for (int i= 0; i < affectedCUs.length; i++) {
				ICompilationUnit unit= affectedCUs[i];
				sub.subTask(unit.getElementName());
				CompilationUnit root= AST.parseCompilationUnit(unit, true);
				if (isProcessable(result, root, unit)) {
					TextChange change= fChangeManager.get(unit);
					AccessAnalyzer analyzer= new AccessAnalyzer(this, unit, fieldIdentifier, declaringClass, change);
					root.accept(analyzer);
					result.merge(analyzer.getStatus());
					if (!fSetterMustReturnValue) 
						fSetterMustReturnValue= analyzer.getSetterMustReturnValue();
				}
				if (result.hasFatalError())
					break;
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
			pm.setTaskName("Create changes");
			pm.subTask(""); 	// XXX: http://bugs.eclipse.org/bugs/show_bug.cgi?id=6794
			addChanges(result, new SubProgressMonitor(pm, 2));
			TextChange[] changes= fChangeManager.getAllChanges();
			SubProgressMonitor sub= new SubProgressMonitor(pm, 8);
			sub.beginTask("", changes.length);
			for (int i= 0; i < changes.length; i++) {
				result.add(changes[i]);
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
	
	private boolean isProcessable(RefactoringStatus result, CompilationUnit root, ICompilationUnit element) {
		Message[] messages= root.getMessages();
		if (messages.length != 0) {
			result.addError(RefactoringCoreMessages.getFormattedString(
				"SelfEncapsulateField.compiler_errors_update",
				element.getElementName()), JavaSourceContext.create(element));
		}
		return true;
	} 
	
	private void computeUsedNames() {
		fUsedReadNames= new ArrayList(0);
		fUsedModifyNames= new ArrayList(0);
		IVariableBinding binding= fFieldDeclaration.resolveBinding();
		ITypeBinding type= binding.getType();
		ITypeBinding booleanType= fFieldDeclaration.getAST().resolveWellKnownType("boolean");
		IMethodBinding[] methods= binding.getDeclaringClass().getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			IMethodBinding method= methods[i];
			ITypeBinding[] parameters= methods[i].getParameterTypes();
			if (parameters == null || parameters.length == 0) {
				fUsedReadNames.add(method);
			} else if (parameters.length == 1 && parameters[0] == type) {
				fUsedModifyNames.add(method);
			} 
		}
	}
	
	private void addChanges(CompositeChange parent, IProgressMonitor pm) throws CoreException {
		int insertionKind= MemberEdit.INSERT_AFTER;
		IMember sibling= null;
		IMethod[] methods= fField.getDeclaringType().getMethods();
		if (fInsertionIndex < 0) {
			if (methods.length > 0) {
				sibling= methods[0];
				insertionKind= MemberEdit.INSERT_BEFORE;
			} else {
				IField[] fields= fField.getDeclaringType().getFields();
				sibling= fields[fields.length - 1];
			}
		} else {
			sibling= methods[fInsertionIndex];
		}
							
		TextChange change= fChangeManager.get(fField.getCompilationUnit());
		
		if (!Flags.isPrivate(fField.getFlags()))
			change.addTextEdit("Change visibility to private", new ChangeVisibilityEdit(fField, "private"));
		
		String modifiers= createModifiers();
		String type= Signature.toString(fField.getTypeSignature());
		if (!Flags.isFinal(fField.getFlags()))
			change.addTextEdit("Add Setter method", createSetterMethod(insertionKind, sibling, modifiers, type));
		change.addTextEdit("Add Getter method", createGetterMethod(insertionKind, sibling, modifiers, type));	
	}

	private TextEdit createSetterMethod(int insertionKind, IMember sibling, String modifiers, String type) throws JavaModelException {
		String returnType= createSetterReturnType();
		String returnStatement= fSetterMustReturnValue ? "return " : "";
		return createMemberEdit(
			sibling,
			insertionKind,
			new String[] {
				modifiers + "  " + returnType + " " + getSetterName() + "(" + type + " " + fArgName + ") {",
				returnStatement + createFieldAccess() + " = " + fArgName + ";",
				"}"
			});
	}
	
	private TextEdit createGetterMethod(int insertionKind, IMember sibling, String modifiers, String type) {
		return createMemberEdit(
			sibling,
			insertionKind,
			new String[] {
				modifiers + " " + type + " " + getGetterName() + "() {",
				"return " + fField.getElementName() + ";",
				"}"
			});
	}

	private MemberEdit createMemberEdit(IMember sibling, int insertionKind, String[] source) {
		MemberEdit result= new MemberEdit(sibling, insertionKind, source, fSettings.tabWidth);
		result.setUseFormatter(true);
		return result;
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
	
	private String createSetterReturnType() throws JavaModelException {
		return fSetterMustReturnValue ? Signature.toString(fField.getTypeSignature()) : "void";
	}
	
	private String createFieldAccess() throws JavaModelException {
		String fieldName= fField.getElementName();
		if (fArgName.equals(fieldName)) {
			return (Flags.isStatic(fField.getFlags()) 
				? fField.getDeclaringType().getElementName() + "." 
				: "this.") + fieldName;
		} else {
			return fieldName;
		}
	}
	
	private void checkArgName() {
		String fieldName= fField.getElementName();
		boolean isStatic= true;
		try {
			isStatic= Flags.isStatic(fField.getFlags());
		} catch(JavaModelException e) {
		}
		if (isStatic && fArgName.equals(fieldName) && fieldName.equals(fField.getDeclaringType().getElementName()))
			fArgName= "_" + fieldName;
	}	
}

