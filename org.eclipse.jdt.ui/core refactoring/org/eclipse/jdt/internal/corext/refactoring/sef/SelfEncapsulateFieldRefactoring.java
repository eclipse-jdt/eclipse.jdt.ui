/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class SelfEncapsulateFieldRefactoring extends Refactoring {

	private IField fField;
	private CodeGenerationSettings fSettings;
	private TextChangeManager fChangeManager;
	private CompositeChange fChange;
	
	private VariableDeclarationFragment fFieldDeclaration;

	private int fVisibility;
	private String fGetterName;
	private String fSetterName;
	private String fArgName;
	private boolean fSetterMustReturnValue;
	private int fInsertionIndex;
	private boolean fEncapsulateDeclaringClass;
	
	private List fUsedReadNames;
	private List fUsedModifyNames;
	private TextEdit[] fNewMethods;
	
	private static final String NO_NAME= ""; //$NON-NLS-1$
	
	public SelfEncapsulateFieldRefactoring(IField field, CodeGenerationSettings settings) {
		Assert.isNotNull(field);
		Assert.isNotNull(settings);
		fField= field;
		fSettings= settings;
		fChangeManager= new TextChangeManager();
		fChange= new CompositeChange(getName());
		NameProposer proposer= new NameProposer(fSettings.fieldPrefixes, fSettings.fieldSuffixes);
		fGetterName= RefactoringCoreMessages.getFormattedString("SelfEncapsulateField.getter", new String[] { proposer.proposeAccessorName(field)}); //$NON-NLS-1$
		fSetterName= RefactoringCoreMessages.getFormattedString("SelfEncapsulateField.setter", new String[] { proposer.proposeAccessorName(field)}); //$NON-NLS-1$
		fEncapsulateDeclaringClass= true;
		fArgName= proposer.proposeArgName(field);
		checkArgName();
		fNewMethods= new TextEdit[2];
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
	
	public int getVisibility() {
		return fVisibility;
	}
	
	public void setVisibility(int visibility) {
		fVisibility= visibility;
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
		try {
			fVisibility= (fField.getFlags() & (Flags.AccPublic | Flags.AccProtected | Flags.AccPrivate));
			RefactoringStatus result=  new RefactoringStatus();
			ASTNode node= JavaElementMapper.perform(fField, VariableDeclarationFragment.class);
			if (node == null || !(node instanceof VariableDeclarationFragment)) {
				return mappingErrorFound(result, node);
			}
			fFieldDeclaration= (VariableDeclarationFragment)node;
			if (fFieldDeclaration.resolveBinding() == null) {
				if (!processCompilerError(result, node))
					result.addFatalError(RefactoringCoreMessages.getString("SelfEncapsulateField.type_not_resolveable")); //$NON-NLS-1$
				return result;
			}
			result.merge(Checks.validateModifiesFiles(new IFile[]{ResourceUtil.getFile(fField.getCompilationUnit())}));
			if (result.hasFatalError())
				return result;
			computeUsedNames();
			return result;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
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
			"SelfEncapsulateField.compiler_errors_field",  //$NON-NLS-1$
			new String[] { fField.getElementName(), messages[0].getMessage()}));
		return true;
	}

	private String getMappingErrorMessage() {
		return RefactoringCoreMessages.getFormattedString(
			"SelfEncapsulateField.cannot_analyze_selected_field", //$NON-NLS-1$
			new String[] {fField.getElementName()});
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
					"SelfEncapsulateField.method_exists", //$NON-NLS-1$
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
			pm.beginTask(NO_NAME, 11);
			pm.setTaskName(RefactoringCoreMessages.getString("SelfEncapsulateField.checking_preconditions")); //$NON-NLS-1$
			result.merge(checkMethodNames());
			pm.worked(1);
			if (result.hasFatalError())
				return result;
			pm.setTaskName(RefactoringCoreMessages.getString("SelfEncapsulateField.searching_for_cunits")); //$NON-NLS-1$
			ICompilationUnit[] affectedCUs= RefactoringSearchEngine.findAffectedCompilationUnits(
				new SubProgressMonitor(pm, 5), RefactoringScopeFactory.create(fField),
				SearchEngine.createSearchPattern(fField, IJavaSearchConstants.REFERENCES));
			
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(affectedCUs)));
			checkInHierarchy(result);
			if (result.hasFatalError())
				return result;
				
			pm.setTaskName(RefactoringCoreMessages.getString("SelfEncapsulateField.analyzing"));	 //$NON-NLS-1$
			IProgressMonitor sub= new SubProgressMonitor(pm, 5);
			sub.beginTask(NO_NAME, affectedCUs.length);
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
			if (result.hasFatalError())
				return result;
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
			addGetterSetterChanges();
			TextChange[] changes= fChangeManager.getAllChanges();
			pm.beginTask(NO_NAME, changes.length);
			pm.setTaskName(RefactoringCoreMessages.getString("SelfEncapsulateField.create_changes")); //$NON-NLS-1$
			for (int i= 0; i < changes.length; i++) {
				result.add(changes[i]);
				pm.worked(1);
			}
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
		return RefactoringCoreMessages.getString("SelfEncapsulateField.name"); //$NON-NLS-1$
	}
	
	//---- Helper methods -------------------------------------------------------------
	
	private boolean isProcessable(RefactoringStatus result, CompilationUnit root, ICompilationUnit element) {
		Message[] messages= root.getMessages();
		if (messages.length != 0) {
			result.addError(RefactoringCoreMessages.getFormattedString(
				"SelfEncapsulateField.compiler_errors_update", //$NON-NLS-1$
				element.getElementName()), JavaSourceContext.create(element));
		}
		return true;
	}
	
	private void checkInHierarchy(RefactoringStatus status)	throws CoreException {
		TypeDeclaration declaration= (TypeDeclaration)ASTNodes.getParent(fFieldDeclaration, ASTNode.TYPE_DECLARATION);
		ITypeBinding type= declaration.resolveBinding();
		if (type != null) {
			ITypeBinding fieldType= fFieldDeclaration.resolveBinding().getType();
			status.merge(Checks.checkMethodInHierarchy(type, fGetterName, fieldType, new ITypeBinding[0], fField.getJavaProject()));
			status.merge(Checks.checkMethodInHierarchy(type, fSetterName, fFieldDeclaration.getAST().resolveWellKnownType("void"), //$NON-NLS-1$
				new ITypeBinding[] {fieldType}, 
				fField.getJavaProject()));
		}
	}
	
	private void computeUsedNames() {
		fUsedReadNames= new ArrayList(0);
		fUsedModifyNames= new ArrayList(0);
		IVariableBinding binding= fFieldDeclaration.resolveBinding();
		ITypeBinding type= binding.getType();
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
	
	private void addGetterSetterChanges() throws CoreException {
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
		
		if (!JdtFlags.isPrivate(fField))
			change.addTextEdit(RefactoringCoreMessages.getString("SelfEncapsulateField.change_visibility"), new ChangeVisibilityEdit(fField, JdtFlags.VISIBILITY_CODE_PRIVATE)); //$NON-NLS-1$
		
		String modifiers= createModifiers();
		String type= Signature.toString(fField.getTypeSignature());
		if (!JdtFlags.isFinal(fField))
			change.addTextEdit(RefactoringCoreMessages.getString("SelfEncapsulateField.add_setter"), createSetterMethod(insertionKind, sibling, modifiers, type)); //$NON-NLS-1$
		change.addTextEdit(RefactoringCoreMessages.getString("SelfEncapsulateField.add_getter"), createGetterMethod(insertionKind, sibling, modifiers, type));	 //$NON-NLS-1$
	}

	private TextEdit createSetterMethod(int insertionKind, IMember sibling, String modifiers, String type) throws JavaModelException {
		String returnType= createSetterReturnType();
		String returnStatement= fSetterMustReturnValue ? "return " : ""; //$NON-NLS-1$ //$NON-NLS-2$
		return fNewMethods[1]= createMemberEdit(
			sibling,
			insertionKind,
			new String[] {
				modifiers + "  " + returnType + " " + getSetterName() + "(" + type + " " + fArgName + ") {", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				returnStatement + createFieldAccess() + " = " + fArgName + ";", //$NON-NLS-1$ //$NON-NLS-2$
				"}" //$NON-NLS-1$
			});
	}
	
	private TextEdit createGetterMethod(int insertionKind, IMember sibling, String modifiers, String type) {
		return fNewMethods[0]= createMemberEdit(
			sibling,
			insertionKind,
			new String[] {
				modifiers + " " + type + " " + getGetterName() + "() {", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"return " + fField.getElementName() + ";", //$NON-NLS-1$ //$NON-NLS-2$
				"}" //$NON-NLS-1$
			});
	}

	private MemberEdit createMemberEdit(IMember sibling, int insertionKind, String[] source) {
		MemberEdit result= new MemberEdit(sibling, insertionKind, source, fSettings.tabWidth);
		result.setUseFormatter(true);
		return result;
	}

	private String createModifiers() throws JavaModelException {
		StringBuffer result= new StringBuffer();
		if (Flags.isPublic(fVisibility)) 
			result.append("public "); //$NON-NLS-1$
		else if (Flags.isProtected(fVisibility)) 
			result.append("protected "); //$NON-NLS-1$
		else if (Flags.isPrivate(fVisibility))
			result.append("private "); //$NON-NLS-1$
		if (JdtFlags.isStatic(fField)) result.append("static "); //$NON-NLS-1$
		return result.toString();
	}
	
	private String createSetterReturnType() throws JavaModelException {
		return fSetterMustReturnValue ? Signature.toString(fField.getTypeSignature()) : "void"; //$NON-NLS-1$
	}
	
	private String createFieldAccess() throws JavaModelException {
		String fieldName= fField.getElementName();
		if (fArgName.equals(fieldName)) {
			return (JdtFlags.isStatic(fField) 
				? fField.getDeclaringType().getElementName() + "."  //$NON-NLS-1$
				: "this.") + fieldName; //$NON-NLS-1$
		} else {
			return fieldName;
		}
	}
	
	private void checkArgName() {
		String fieldName= fField.getElementName();
		boolean isStatic= true;
		try {
			isStatic= JdtFlags.isStatic(fField);
		} catch(JavaModelException e) {
		}
		if (isStatic && fArgName.equals(fieldName) && fieldName.equals(fField.getDeclaringType().getElementName()))
			fArgName= "_" + fieldName; //$NON-NLS-1$
	}	
}

