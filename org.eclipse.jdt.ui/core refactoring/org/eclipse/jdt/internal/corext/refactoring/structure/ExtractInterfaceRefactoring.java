/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class ExtractInterfaceRefactoring extends Refactoring {

	private static final String INTERFACE_METHOD_MODIFIERS = "public abstract "; //$NON-NLS-1$
	private final CodeGenerationSettings fCodeGenerationSettings;
	private final ASTNodeMappingManager fASTMappingManager;
	private IType fInputType;
	private String fNewInterfaceName;
	private IMember[] fExtractedMembers;
	private boolean fReplaceOccurrences= false;
	private TextChangeManager fChangeManager;
    private Set fUpdatedTypeReferenceNodes; //Set of ASTNodes
	
	public ExtractInterfaceRefactoring(IType type, CodeGenerationSettings codeGenerationSettings){
		Assert.isNotNull(type);
		Assert.isNotNull(codeGenerationSettings);
		fInputType= type;
		fCodeGenerationSettings= codeGenerationSettings;
		fExtractedMembers= new IMember[0];
		fASTMappingManager= new ASTNodeMappingManager();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.name"); //$NON-NLS-1$
	}
	
	public IType getInputType() {
		return fInputType;
	}

	public String getNewInterfaceName() {
		return fNewInterfaceName;
	}

	public boolean isReplaceOccurrences() {
		return fReplaceOccurrences;
	}

	public void setNewInterfaceName(String newInterfaceName) {
		Assert.isNotNull(newInterfaceName);
		fNewInterfaceName= newInterfaceName;
	}

	public void setReplaceOccurrences(boolean replaceOccurrences) {
		fReplaceOccurrences= replaceOccurrences;
	}
	
	public void setExtractedMembers(IMember[] extractedMembers) throws JavaModelException{
		Assert.isTrue(areAllExtractableMembersOfClass(extractedMembers));
		fExtractedMembers= extractedMembers;
	}
	
	public IMember[] getExtractableMembers() throws JavaModelException{
		List members= new ArrayList();
		IJavaElement[] children= fInputType.getChildren();
		for (int i= 0; i < children.length; i++) {
			if (children[i] instanceof IMember && isExtractableMember((IMember)children[i]))
				members.add(children[i]);
		}
		return (IMember[]) members.toArray(new IMember[members.size()]);
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= Checks.checkAvailability(fInputType);	
		if (result.hasFatalError())
			return result;

		if (fInputType.isLocal())
			result.addFatalError(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.no_local_types")); //$NON-NLS-1$
		if (result.hasFatalError())
			return result;

		if (fInputType.isAnonymous())
			result.addFatalError(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.no_anonymous_types")); //$NON-NLS-1$
		if (result.hasFatalError())
			return result;

		//XXX for now
		if (! Checks.isTopLevel(fInputType))
			result.addFatalError(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.no_member_classes")); //$NON-NLS-1$
		if (result.hasFatalError())
			return result;
			
		return result;	
	}
	
	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		IType orig= (IType)WorkingCopyUtil.getOriginal(fInputType);
		if (orig == null || ! orig.exists()){
			String[] keys= new String[]{fInputType.getCompilationUnit().getElementName()};
			String message= RefactoringCoreMessages.getFormattedString("ExtractInterfaceRefactoring.deleted", keys); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fInputType= orig;
		
		if (Checks.isException(fInputType, pm)){
			String message= RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.no_Throwable"); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}
			
		return Checks.checkIfCuBroken(fInputType);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();		
			result.merge(checkNewInterfaceName(fNewInterfaceName));
			if (result.hasFatalError())
				return result;
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}

	private RefactoringStatus checkInterfaceTypeName() throws JavaModelException {
		IType type= Checks.findTypeInPackage(getInputClassPackage(), fNewInterfaceName);
		if (type == null || ! type.exists())
			return null;
		String[] keys= new String[]{fNewInterfaceName, getInputClassPackage().getElementName()};
		String message= RefactoringCoreMessages.getFormattedString("ExtractInterfaceRefactoring.type_exists", keys); //$NON-NLS-1$
		return RefactoringStatus.createFatalErrorStatus(message);
	}
	
	public RefactoringStatus checkNewInterfaceName(String newName){
		try {
			RefactoringStatus result= Checks.checkTypeName(newName);
			if (result.hasFatalError())
				return result;
			result.merge(Checks.checkCompilationUnitName(newName + ".java")); //$NON-NLS-1$
			if (result.hasFatalError())
				return result;
	
			if (getInputClassPackage().getCompilationUnit(getCuNameForNewInterface()).exists()){
				String[] keys= new String[]{getCuNameForNewInterface(), getInputClassPackage().getElementName()};
				String message= RefactoringCoreMessages.getFormattedString("ExtractInterfaceRefactoring.compilation_Unit_exists", keys); //$NON-NLS-1$
				result.addFatalError(message);
				if (result.hasFatalError())
					return result;
			}	
			result.merge(checkInterfaceTypeName());
			return result;
		} catch (JavaModelException e) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.internal_Error")); //$NON-NLS-1$
		}
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			CompositeChange builder= new CompositeChange(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.name")); //$NON-NLS-1$
			builder.addAll(fChangeManager.getAllChanges());
			builder.add(createExtractedInterface(new SubProgressMonitor(pm, 1)));
			return builder;
		} catch(JavaModelException e){	
			throw e;
		} catch(CoreException e){
			throw new JavaModelException(e);
		}	finally{
			clearIntermediateState();
			pm.done();
		}
	}

	private void clearIntermediateState() {
		fASTMappingManager.clear();
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 10); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.analyzing...")); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			updateTypeDeclaration(manager, new SubProgressMonitor(pm, 1));
			if (fReplaceOccurrences)
				updateReferences(manager, new SubProgressMonitor(pm, 9));
			else
				pm.worked(1);	
				
			deleteExtractedFields(manager);
			if (fInputType.isInterface())
				deleteExtractedMethods(manager);
				
			return manager;
		} finally{
			pm.done();
		}	
	}
    
	private void deleteExtractedMethods(TextChangeManager manager) throws CoreException {
		IMethod[] methods= getExtractedMethods();
		for (int i= 0; i < methods.length; i++) {
			deleteExtractedMember(manager, methods[i], RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.delete_method")); //$NON-NLS-1$
		}
	}    
	
	private void deleteExtractedFields(TextChangeManager manager) throws CoreException {
		IField[] fields= getExtractedFields();
		for (int i= 0; i < fields.length; i++) {
			deleteExtractedMember(manager, fields[i], RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.delete_field")); //$NON-NLS-1$
		}
	}
	
	private static void deleteExtractedMember(TextChangeManager manager, IMember member, String editName) throws CoreException {
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(member.getCompilationUnit());
		manager.get(wc).addTextEdit(editName, new DeleteSourceReferenceEdit(member, wc));
	}

	private void updateReferences(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException, CoreException {
		fUpdatedTypeReferenceNodes= UseSupertypeWherePossibleUtil.updateReferences(manager, fExtractedMembers, fNewInterfaceName, fInputType, fCodeGenerationSettings, fASTMappingManager, pm);
	}
	
	private boolean areAllExtractableMembersOfClass(IMember[] extractedMembers) throws JavaModelException {
		for (int i= 0; i < extractedMembers.length; i++) {
			if (! extractedMembers[i].getParent().equals(fInputType))
				return false;
			if (! isExtractableMember(extractedMembers[i]))
				return false;
		}
		return true;
	}

	private static boolean isExtractableMember(IMember iMember) throws JavaModelException {
		if (iMember.getElementType() == IJavaElement.METHOD)
			return isExtractableMethod((IMethod)iMember);
		if (iMember.getElementType() == IJavaElement.FIELD)	
			return isExtractableField((IField)iMember);
		return false;	
	}

	private static boolean isExtractableField(IField iField) throws JavaModelException {
		if (! JdtFlags.isPublic(iField))
			return false;
		if (! JdtFlags.isStatic(iField))
			return false;
		if (! JdtFlags.isFinal(iField))
			return false;
		return true;		
	}

	private static boolean isExtractableMethod(IMethod iMethod) throws JavaModelException {
		if (! JdtFlags.isPublic(iMethod))
			return false;
		if (JdtFlags.isStatic(iMethod))
			return false;
		if (iMethod.isConstructor())	
			return false;
		return true;		
	}
	
	//----- methods related to creation of the new interface -------
	private IChange createExtractedInterface(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		IPath cuPath= ResourceUtil.getFile(fInputType.getCompilationUnit()).getFullPath();
		IPath interfaceCuPath= cuPath.removeLastSegments(1).append(getCuNameForNewInterface());

		ICompilationUnit newCuWC= null;
		try{
			newCuWC= WorkingCopyUtil.getNewWorkingCopy(getInputClassPackage(), getCuNameForNewInterface());
			String formattedSource= formatSource(createExtractedInterfaceCUSource(newCuWC, new SubProgressMonitor(pm, 1)));
			return new CreateTextFileChange(interfaceCuPath, formattedSource, true);	
		} finally{
			if (newCuWC != null)
				newCuWC.destroy();
			pm.done();	
		}
	}

	private String formatSource(String source) {
		return ToolFactory.createCodeFormatter().format(source, 0, null, getLineSeperator());
	}
	
	private String getCuNameForNewInterface() {
		return fNewInterfaceName + ".java"; //$NON-NLS-1$
	}
	
	private IPackageFragment getInputClassPackage() {
		return fInputType.getPackageFragment();
	}

	private String getLineSeperator() {
		try {
			return StubUtility.getLineDelimiterUsed(fInputType);
		} catch (JavaModelException e) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private boolean inputClassHasDirectSuperinterfaces() throws JavaModelException {
		return fInputType.getSuperInterfaceNames().length > 0;
	}

	private void updateTypeDeclaration(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);//$NON-NLS-1$
		String editName= RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.update_Type_Declaration"); //$NON-NLS-1$
		int offset= computeIndexOfSuperinterfaceNameInsertion();
		String text=  computeTextOfSuperinterfaceNameInsertion();
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fInputType.getCompilationUnit());
		manager.get(cu).addTextEdit(editName, SimpleTextEdit.createInsert(offset, text));
		pm.done();
	}

	private String computeTextOfSuperinterfaceNameInsertion() throws JavaModelException {
		if (inputClassHasDirectSuperinterfaces())
			return ", " + fNewInterfaceName; //$NON-NLS-1$
		if (fInputType.isClass())	
			return " implements " + fNewInterfaceName;	//$NON-NLS-1$
		else
			return " extends " + fNewInterfaceName;	//$NON-NLS-1$
	}

	private int computeIndexOfSuperinterfaceNameInsertion() throws JavaModelException {
		TypeDeclaration typeNode= getTypeDeclarationNode();
		if (typeNode.superInterfaces().isEmpty()){
			if (typeNode.getSuperclass() == null)
				return ASTNodes.getExclusiveEnd(typeNode.getName());
			else 
				return ASTNodes.getExclusiveEnd(typeNode.getSuperclass());
		} else {
			Name lastInterfaceName= (Name)typeNode.superInterfaces().get(typeNode.superInterfaces().size() - 1);
			return ASTNodes.getExclusiveEnd(lastInterfaceName);
		}
	}

	private TypeDeclaration getTypeDeclarationNode() throws JavaModelException {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(fInputType.getNameRange().getOffset(), fInputType.getNameRange().getLength() +1), true);
		getAST(fInputType.getCompilationUnit()).accept(analyzer);
		if (analyzer.getFirstSelectedNode() != null){
			if (analyzer.getFirstSelectedNode().getParent() instanceof TypeDeclaration)
				return (TypeDeclaration)analyzer.getFirstSelectedNode().getParent();
		}
		return null;	
	}

	private String createExtractedInterfaceCUSource(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		String typeComment= StubUtility.getTypeComment(newCu, fNewInterfaceName, "");//$NON-NLS-1$
		if (typeComment == null)
			typeComment= ""; //$NON-NLS-1$
		String compilationUnitContent = StubUtility.getCompilationUnitContent(newCu, typeComment, createInterfaceSource(), getLineSeperator());
		if (compilationUnitContent == null)
			compilationUnitContent= ""; //$NON-NLS-1$
		newCu.getBuffer().setContents(compilationUnitContent);
		addImportsToNewCu(newCu, pm);
		return newCu.getSource();
	}

	private void addImportsToNewCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 3); //$NON-NLS-1$
		ImportsStructure is= new ImportsStructure(newCu, fCodeGenerationSettings.importOrder, fCodeGenerationSettings.importThreshold, true);
		addImportsToTypesReferencedInMethodDeclarations(is, new SubProgressMonitor(pm, 1));
		addImportsToTypesReferencedInFieldDeclarations(is, new SubProgressMonitor(pm, 1));
		is.create(false, new SubProgressMonitor(pm, 1));
		pm.done();
	}

	private String createInterfaceSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		buff.append(createInterfaceModifierString())
			 .append("interface ")//$NON-NLS-1$
			 .append(fNewInterfaceName)
			 .append(" {")//$NON-NLS-1$
			 .append(getLineSeperator())
			 .append(createInterfaceMemberDeclarationsSource())
			 .append("}");//$NON-NLS-1$
		return buff.toString();
	}

	private String createInterfaceModifierString() throws JavaModelException {
		if (JdtFlags.isPublic(fInputType))
			return JdtFlags.VISIBILITY_STRING_PUBLIC + " ";//$NON-NLS-1$
		else
			return JdtFlags.VISIBILITY_STRING_PACKAGE;	
	}

	private String createInterfaceMemberDeclarationsSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		sortByOffset(fExtractedMembers);
		for (int i= 0; i < fExtractedMembers.length; i++) {
			buff.append(createInterfaceMemberDeclarationsSource(fExtractedMembers[i]));
			if (i + 1 != fExtractedMembers.length)
				buff.append(getLineSeperator());
		}
		return buff.toString();
	}
	
	private static void sortByOffset(IMember[] members) {
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2) {
				ISourceReference sr1= (ISourceReference)o1;
				ISourceReference sr2= (ISourceReference)o2;
				try {
					return sr1.getSourceRange().getOffset() - sr2.getSourceRange().getOffset();
				} catch (JavaModelException e) {
					return 0;
				}
			}
		};
		Arrays.sort(members, comparator);
	}

	private String createInterfaceMemberDeclarationsSource(IMember iMember) throws JavaModelException {
		Assert.isTrue(iMember.getElementType() == IJavaElement.FIELD || iMember.getElementType() == IJavaElement.METHOD);
		if (iMember.getElementType() == IJavaElement.FIELD)
			return createInterfaceFieldDeclarationSource((IField)iMember);
		else 
			return createInterfaceMethodDeclarationsSource((IMethod)iMember);
	}
	
	private String createInterfaceFieldDeclarationSource(IField iField) throws JavaModelException {
		FieldDeclaration fieldDeclaration= getFieldDeclarationNode(iField);
		if (fieldDeclaration == null)
			return ""; //$NON-NLS-1$

		StringBuffer fieldSource= new StringBuffer(SourceRangeComputer.computeSource(iField));
		
		if (fUpdatedTypeReferenceNodes != null){
			int offset= SourceRangeComputer.computeSourceRange(iField, iField.getCompilationUnit().getSource()).getOffset();
			replaceReferencesInFieldDeclaration(fieldDeclaration, offset, fieldSource);
		}	
			
		return fieldSource.toString();
	}

	private String createInterfaceMethodDeclarationsSource(IMethod iMethod) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(iMethod);
		if (methodDeclaration == null)
			return ""; //$NON-NLS-1$
	
		if (fInputType.isClass()){
			int methodDeclarationOffset= methodDeclaration.getReturnType().getStartPosition();
			int length= getMethodDeclarationLength(iMethod, methodDeclaration);
			
			StringBuffer methodDeclarationSource= new StringBuffer();
			String methodComment= getCommentContent(iMethod);
			if (methodComment != null)
				methodDeclarationSource.append(methodComment);
			methodDeclarationSource.append(INTERFACE_METHOD_MODIFIERS);//$NON-NLS-1$
			methodDeclarationSource.append(iMethod.getCompilationUnit().getBuffer().getText(methodDeclarationOffset, length));
			
			if (methodDeclaration.getBody() != null)
				methodDeclarationSource.append(";"); //$NON-NLS-1$
			
			if (fUpdatedTypeReferenceNodes != null)
		        replaceReferencesInMethodDeclaration(methodDeclaration, methodDeclarationOffset - INTERFACE_METHOD_MODIFIERS.length(), methodDeclarationSource);
			return methodDeclarationSource.toString();		
		} else {
			StringBuffer source= new StringBuffer(SourceRangeComputer.computeSource(iMethod));
			if (fUpdatedTypeReferenceNodes != null){
				int offset= SourceRangeComputer.computeSourceRange(iMethod, iMethod.getCompilationUnit().getSource()).getOffset();
				replaceReferencesInMethodDeclaration(methodDeclaration, offset, source);
			}
			return source.toString();
		}	
	}
	
	private static String getCommentContent(IMethod iMethod) throws JavaModelException {
		String rawContent= JavaElementCommentFinder.getCommentContent(iMethod);
		if (rawContent == null)
			return null;
		String[] lines= Strings.convertIntoLines(rawContent);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(iMethod));
	}
	
    private void replaceReferencesInMethodDeclaration(MethodDeclaration methodDeclaration, int methodDeclarationOffset, StringBuffer methodDeclarationSource) {
        SingleVariableDeclaration[] params= (SingleVariableDeclaration[]) methodDeclaration.parameters().toArray(new SingleVariableDeclaration[methodDeclaration.parameters().size()]);
        for (int i= params.length - 1; i >= 0; i--) {//iterate backwards to preserve indices
            SingleVariableDeclaration declaration= params[i];
            replaceReferencesIfUpdated(methodDeclarationOffset, methodDeclarationSource, declaration.getType());
        }
        replaceReferencesIfUpdated(methodDeclarationOffset, methodDeclarationSource, methodDeclaration.getReturnType());
    }

	private void replaceReferencesInFieldDeclaration(FieldDeclaration fieldDeclaration, int offset, StringBuffer fieldSource) {
		ASTNode[] inputTypeReferences= getInputTypeReferencesUsedInside(fieldDeclaration);
		sortBackwardsByStartPosition(inputTypeReferences);
		for (int i= 0; i < inputTypeReferences.length; i++) {
			ASTNode node= inputTypeReferences[i];
			replaceReferencesIfUpdated(offset, fieldSource, node);
		}
	}
	
	private static void sortBackwardsByStartPosition(ASTNode[] nodes) {
		Arrays.sort(nodes, new Comparator(){
			public int compare(Object o1, Object o2) {
				return ((ASTNode)o1).getStartPosition() - ((ASTNode)o2).getStartPosition();
			}
		});
	}
	
	private ASTNode[] getInputTypeReferencesUsedInside(FieldDeclaration fieldDeclaration) {
		final Set nodes= new HashSet(0);
		ASTVisitor collector= new ASTVisitor(){
			public boolean visit(SimpleName node) {
				if (isReferenceToInputType(node))
					nodes.add(node);
				return false;
			}
			public boolean visit(SimpleType node) {
				if (isReferenceToInputType(node))
					nodes.add(node);
				return false;
			}
			private boolean isReferenceToInputType(SimpleType node) {
				return isBindingOfInputType(node.resolveBinding());
			}
			private boolean isReferenceToInputType(SimpleName node) {
				return isBindingOfInputType(node.resolveBinding());
			}
			private boolean isBindingOfInputType(IBinding iBinding) {
				if (!(iBinding instanceof ITypeBinding))
					return false;
				ITypeBinding tb= (ITypeBinding)iBinding;
				if (tb.isPrimitive())
					return false;
				if (tb.isNullType())	
					return false;
				if (! tb.getName().equals(getInputType().getElementName()))	
					return false;
				return JavaModelUtil.getFullyQualifiedName(getInputType()).equals(Bindings.getFullyQualifiedImportName(tb));
			}
		};
		fieldDeclaration.accept(collector);
		return (ASTNode[]) nodes.toArray(new ASTNode[nodes.size()]);
	}

    private void replaceReferencesIfUpdated(int methodDeclarationOffset, StringBuffer methodDeclarationSource, ASTNode node) {
        if (fUpdatedTypeReferenceNodes.contains(node))
        	methodDeclarationSource.replace(node.getStartPosition() - methodDeclarationOffset, ASTNodes.getExclusiveEnd(node) - methodDeclarationOffset, fNewInterfaceName);
    }
	
	private static int getMethodDeclarationLength(IMethod iMethod, MethodDeclaration methodDeclaration) throws JavaModelException{
		int preDeclarationSourceLength= methodDeclaration.getReturnType().getStartPosition() - iMethod.getSourceRange().getOffset();
		if (methodDeclaration.getBody() == null)
			return methodDeclaration.getLength() - preDeclarationSourceLength;
		else
			return iMethod.getSourceRange().getLength() - methodDeclaration.getBody().getLength() - preDeclarationSourceLength;
	}

	private MethodDeclaration getMethodDeclarationNode(IMethod iMethod) throws JavaModelException{
		return ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, fASTMappingManager);
	}

	private FieldDeclaration getFieldDeclarationNode(IField iField) throws JavaModelException {
		return ASTNodeSearchUtil.getFieldDeclarationNode(iField, fASTMappingManager);
	}
	
    private IField[] getExtractedFields() {
        List fields= new ArrayList();
        for (int i= 0; i < fExtractedMembers.length; i++) {
            if (fExtractedMembers[i] instanceof IField)
                fields.add(fExtractedMembers[i]);
        }
        return (IField[]) fields.toArray(new IField[fields.size()]);
    }

	private IMethod[] getExtractedMethods() {
		List methods= new ArrayList();
		for (int i= 0; i < fExtractedMembers.length; i++) {
			if (fExtractedMembers[i] instanceof IMethod)
				methods.add(fExtractedMembers[i]);
		}
		return (IMethod[]) methods.toArray(new IMethod[methods.size()]);
	}

	private void addImportsToTypesReferencedInFieldDeclarations(ImportsStructure is, IProgressMonitor pm) throws JavaModelException {
		IType[] referencedTypes= ReferenceFinderUtil.getTypesReferencedIn(getExtractedFields(), pm);
		for (int i= 0; i < referencedTypes.length; i++) {
			IType type= referencedTypes[i];
			is.addImport(JavaModelUtil.getFullyQualifiedName(type));
		}
	}
	
	private void addImportsToTypesReferencedInMethodDeclarations(ImportsStructure is, IProgressMonitor pm) throws JavaModelException {
		ITypeBinding[] typesUsed= getTypesUsedInExtractedMethodDeclarations();
		pm.beginTask("", typesUsed.length); //$NON-NLS-1$
		for (int i= 0; i < typesUsed.length; i++) {
			is.addImport(typesUsed[i]);
			pm.worked(1);
		}
		pm.done();
	}
	
	private ITypeBinding[] getTypesUsedInExtractedMethodDeclarations() throws JavaModelException{
		return ReferenceFinderUtil.getTypesReferencedInDeclarations(getExtractedMethods(), fASTMappingManager);
	}	

	private CompilationUnit getAST(ICompilationUnit cu){
		return fASTMappingManager.getAST(cu);
	}
}
