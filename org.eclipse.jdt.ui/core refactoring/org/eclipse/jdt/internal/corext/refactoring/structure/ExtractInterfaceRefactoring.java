/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ASTNodeDeleteUtil;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

public class ExtractInterfaceRefactoring extends Refactoring {

	public static final boolean DEFAULT_DECLARE_METHODS_PUBLIC= true;
	public static final boolean DEFAULT_DECLARE_METHODS_ABSTRACT= true;

	private final CodeGenerationSettings fCodeGenerationSettings;
	private IType fInputType;
	private String fNewInterfaceName;
	private IMember[] fExtractedMembers;
	private boolean fReplaceOccurrences= false;
	private boolean fMarkInterfaceMethodsAsPublic= DEFAULT_DECLARE_METHODS_PUBLIC;
	private boolean fMarkInterfaceMethodsAsAbstract= DEFAULT_DECLARE_METHODS_ABSTRACT;
	private TextChangeManager fChangeManager;

    private final WorkingCopyOwner fWorkingCopyOwner;
	private String fSource;
	
	private ExtractInterfaceRefactoring(IType type, CodeGenerationSettings codeGenerationSettings){
		Assert.isNotNull(type);
		Assert.isNotNull(codeGenerationSettings);
		fInputType= type;
		fCodeGenerationSettings= codeGenerationSettings;
		fExtractedMembers= new IMember[0];
		fWorkingCopyOwner= new RefactoringWorkingCopyOwner();
	}
	
	public static ExtractInterfaceRefactoring create(IType type, CodeGenerationSettings codeGenerationSettings) throws JavaModelException{
		if (! isAvailable(type))
			return null;
		return new ExtractInterfaceRefactoring(type, codeGenerationSettings);
	}
	
	public static boolean isAvailable(IType type) throws JavaModelException {
		if (! Checks.isAvailable(type))
			return false;

		//for now
		if (! Checks.isTopLevel(type))
			return false;
		return true;
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

	public boolean getMarkInterfaceMethodsAsPublic() {
		return fMarkInterfaceMethodsAsPublic;
	}

	public boolean getMarkInterfaceMethodsAsAbstract() {
		return fMarkInterfaceMethodsAsAbstract;
	}

	public void setNewInterfaceName(String newInterfaceName) {
		Assert.isNotNull(newInterfaceName);
		fNewInterfaceName= newInterfaceName;
	}

	public void setReplaceOccurrences(boolean replaceOccurrences) {
		fReplaceOccurrences= replaceOccurrences;
	}

	public void setMarkInterfaceMethodsAsPublic(boolean mark) {
		fMarkInterfaceMethodsAsPublic= mark;
	}

	public void setMarkInterfaceMethodsAsAbstract(boolean mark) {
		fMarkInterfaceMethodsAsAbstract= mark;
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
		
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		IType orig= (IType)WorkingCopyUtil.getOriginal(fInputType);
		if (orig == null || ! orig.exists()){
			String[] keys= new String[]{getInputTypeCU().getElementName()};
			String message= RefactoringCoreMessages.getFormattedString("ExtractInterfaceRefactoring.deleted", keys); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		
		if (Checks.isException(fInputType, pm)){
			String message= RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.no_Throwable"); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}
			
		return Checks.checkIfCuBroken(fInputType);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try {
			RefactoringStatus result= new RefactoringStatus();		
			result.merge(checkNewInterfaceName(fNewInterfaceName));
			if (result.hasFatalError())
				return result;
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1), result);
			if (result.hasFatalError())
				return result;
			result.merge(validateModifiesFiles());
			return result;
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
				return result;
			}	
			result.merge(checkInterfaceTypeName());
			return result;
		} catch (JavaModelException e) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.internal_Error")); //$NON-NLS-1$
		}
	}

	private IFile[] getAllFilesToModify(){
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles(){
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			final DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.name")); //$NON-NLS-1$
			result.addAll(fChangeManager.getAllChanges());
			result.add(createExtractedInterface(new SubProgressMonitor(pm, 1)));
			return result;
		} finally {
			pm.done();
		}
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus status) throws CoreException{
		ICompilationUnit newCuWC= null, typeCu= null;
		try{
			pm.beginTask("", 10); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.analyzing...")); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager(true);
			
			typeCu= WorkingCopyUtil.getNewWorkingCopy(getInputTypeCU(), fWorkingCopyOwner, new SubProgressMonitor(pm, 1));
			ASTParser p= ASTParser.newParser(AST.LEVEL_2_0);
			p.setSource(typeCu);
			p.setResolveBindings(true);
			p.setWorkingCopyOwner(fWorkingCopyOwner);
			p.setCompilerOptions(RefactoringASTParser.getCompilerOptions(typeCu));
			CompilationUnit typeCuNode= (CompilationUnit) p.createAST(null);
			OldASTRewrite typeCuRewrite= new OldASTRewrite(typeCuNode);
			IType theType= (IType)JavaModelUtil.findInCompilationUnit(typeCu, fInputType);
			TypeDeclaration td= ASTNodeSearchUtil.getTypeDeclarationNode(theType, typeCuNode);

			modifyInputTypeCu(typeCu, typeCuNode, typeCuRewrite, td);
			TextEditGroup description= trackReferenceNodes(typeCuNode, typeCuRewrite, td);
			TextChange change= addTextEditFromRewrite(manager, typeCu, typeCuRewrite);
			
			if (! fReplaceOccurrences)
				return manager;

			setContent(typeCu, change.getPreviewContent());
			newCuWC= WorkingCopyUtil.getNewWorkingCopy(getInputClassPackage(), getCuNameForNewInterface(), fWorkingCopyOwner, new SubProgressMonitor(pm, 1));
			setContent(newCuWC, createExtractedInterfaceCUSource(newCuWC, new SubProgressMonitor(pm, 1)));
			IType theInterface= newCuWC.getType(fNewInterfaceName);
			
			CompilationUnitRange[] updatedRanges= ExtractInterfaceUtil.updateReferences(manager, theType, theInterface, fWorkingCopyOwner, 
			        false, new SubProgressMonitor(pm, 9), status, fCodeGenerationSettings);
			if (status.hasFatalError())
				return manager;
			TextEdit[] edits= description.getTextEdits();
			
			for (int i= 0; i < updatedRanges.length; i++) {
				CompilationUnitRange cuRange= updatedRanges[i];
				ICompilationUnit cu= cuRange.getCompilationUnit();
				if(! cu.equals(typeCu))
					continue;
				ISourceRange sourceRange= cuRange.getSourceRange();
				IRegion oldRange= getOldRange(edits, new Region(sourceRange.getOffset(), sourceRange.getLength()), change);
				String typeName= fInputType.getElementName();
				int offset= oldRange.getOffset() + oldRange.getLength() - typeName.length();
				TextEdit edit= new ReplaceEdit(offset, typeName.length(), fNewInterfaceName);
				TextChangeCompatibility.addTextEdit(ExtractInterfaceUtil.getTextChange(manager, cu), RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.update_reference"), edit);  //$NON-NLS-1$
			}
			fSource= ExtractInterfaceUtil.getTextChange(manager, newCuWC).getPreviewContent();
			manager.remove(newCuWC);
			return manager;
		} finally{
			if (newCuWC != null)
				newCuWC.discardWorkingCopy();
			if (typeCu != null)
				typeCu.discardWorkingCopy();
			pm.done();
		}	
	}
	
	private void modifyInputTypeCu(ICompilationUnit typeCu, CompilationUnit typeCuNode, OldASTRewrite typeCuRewrite, TypeDeclaration td) throws CoreException, JavaModelException {
		deleteExtractedFields(typeCuNode, typeCu, typeCuRewrite);
		if (fInputType.isInterface())
			deleteExtractedMethods(typeCuNode, typeCu, typeCuRewrite);
		
		Name newInterfaceName= td.getAST().newSimpleName(fNewInterfaceName);
		td.superInterfaces().add(newInterfaceName);
		typeCuRewrite.markAsInserted(newInterfaceName);
	}

	private static void setContent(ICompilationUnit cu, String newContent) throws JavaModelException {
		cu.getBuffer().setContents(newContent);
		synchronized (cu) {
			cu.reconcile();
		}
	}

	private TextEditGroup trackReferenceNodes(CompilationUnit typeCuNode, OldASTRewrite typeCuRewrite, TypeDeclaration td) {
		ASTNode[] refs= getReferencesToType(typeCuNode, td.resolveBinding());
		TextEditGroup description= new TextEditGroup("N.N"); //$NON-NLS-1$
		for (int i= 0; i < refs.length; i++) {
			typeCuRewrite.markAsTracked(refs[i], description);
		}
		return description;
	}

	private static IRegion getOldRange(TextEdit[] edits, IRegion newRange, TextChange change) {
		for (int i= 0; i < edits.length; i++) {
			TextEdit edit= edits[i];
			if (change.getPreviewEdit(edit).getRegion().equals(newRange))
				return edit.getRegion();
		}
		Assert.isTrue(false, "original text range not found"); //$NON-NLS-1$
		return newRange;
	}

	private static ASTNode[] getReferencesToType(CompilationUnit typeCuNode, final ITypeBinding binding) {
		final Set result= new HashSet();
		typeCuNode.accept(new ASTVisitor(){
			public boolean visit(SimpleName node) {
				if (node.resolveBinding() == binding)
					result.add(node);
				return super.visit(node);
			}
		});
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	private static TextChange addTextEditFromRewrite(TextChangeManager manager, ICompilationUnit cu, OldASTRewrite rewrite) throws CoreException {
		TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
		TextEdit resultingEdits= new MultiTextEdit();
		rewrite.rewriteNode(textBuffer, resultingEdits);

		TextChange textChange= manager.get(cu);
		TextChangeCompatibility.addTextEdit(textChange, RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.update_type_declaration"), resultingEdits); //$NON-NLS-1$
		rewrite.removeModifications();
		return textChange;
	}
    
	private void deleteExtractedMethods(CompilationUnit typeCuNode, ICompilationUnit cu, OldASTRewrite typeCuRewrite) throws CoreException {
		ASTNodeDeleteUtil.markAsDeleted(getExtractedMethods(cu), typeCuNode, typeCuRewrite);
	}

	private void deleteExtractedFields(CompilationUnit typeCuNode, ICompilationUnit cu, OldASTRewrite typeCuRewrite) throws CoreException {
		ASTNodeDeleteUtil.markAsDeleted(getExtractedFields(cu), typeCuNode, typeCuRewrite);
	}
	
	private boolean areAllExtractableMembersOfClass(IMember[] extractedMembers) throws JavaModelException {
		for (int i= 0; i < extractedMembers.length; i++) {
			if (! extractedMembers[i].getParent().equals(fInputType) || ! isExtractableMember(extractedMembers[i]))
				return false;
		}
		return true;
	}

	private static boolean isExtractableMember(IMember iMember) throws JavaModelException {
		switch(iMember.getElementType()){
			case IJavaElement.METHOD: 	return isExtractableMethod((IMethod)iMember);
			case IJavaElement.FIELD:	return isExtractableField((IField)iMember);
			default:					return false;
		}
	}

	private static boolean isExtractableField(IField iField) throws JavaModelException {
		return JdtFlags.isPublic(iField) && JdtFlags.isStatic(iField) && JdtFlags.isFinal(iField);
	}

	private static boolean isExtractableMethod(IMethod iMethod) throws JavaModelException {
		return JdtFlags.isPublic(iMethod) && ! JdtFlags.isStatic(iMethod) && !iMethod.isConstructor();
	}
	
	//----- methods related to creation of the new interface -------
	private Change createExtractedInterface(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		IPath cuPath= ResourceUtil.getFile(getInputTypeCU()).getFullPath();
		IPath interfaceCuPath= cuPath.removeLastSegments(1).append(getCuNameForNewInterface());

		ICompilationUnit newCuWC= null;
		try{
			newCuWC= WorkingCopyUtil.getNewWorkingCopy(getInputClassPackage(), getCuNameForNewInterface(), fWorkingCopyOwner, new SubProgressMonitor(pm, 1));
			Assert.isTrue(! fReplaceOccurrences || fSource != null);
			Assert.isTrue(fSource == null || fReplaceOccurrences);
			String formattedSource= fSource != null ? fSource: createExtractedInterfaceCUSource(newCuWC, new SubProgressMonitor(pm, 1));
			return new CreateTextFileChange(interfaceCuPath, formattedSource, "java");	 //$NON-NLS-1$
		} finally{
			if (newCuWC != null)
				newCuWC.discardWorkingCopy();
			pm.done();	
		}
	}

	private String formatCuSource(String source) {
		return CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, null, getLineSeperator(), fInputType.getJavaProject());
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

	private String createExtractedInterfaceCUSource(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		CompilationUnit cuNode= new RefactoringASTParser(AST.LEVEL_2_0).parse(getInputTypeCU(), true);			
		String typeComment= CodeGeneration.getTypeComment(newCu, fNewInterfaceName, getLineSeperator());//$NON-NLS-1$
		String compilationUnitContent = CodeGeneration.getCompilationUnitContent(newCu, typeComment, createInterfaceSource(cuNode), getLineSeperator());
		if (compilationUnitContent == null)
			compilationUnitContent= ""; //$NON-NLS-1$
		newCu.getBuffer().setContents(compilationUnitContent);
		addImportsToNewCu(newCu, pm, cuNode);
		return formatCuSource(newCu.getSource());
	}

	private void addImportsToNewCu(ICompilationUnit newCu, IProgressMonitor pm, CompilationUnit cuNode) throws CoreException {
		pm.beginTask("", 3); //$NON-NLS-1$
		ImportsStructure is= new ImportsStructure(newCu, fCodeGenerationSettings.importOrder, fCodeGenerationSettings.importThreshold, true);
		addImportsToTypesReferencedInMethodDeclarations(is, new SubProgressMonitor(pm, 1), cuNode);
		addImportsToTypesReferencedInFieldDeclarations(is, new SubProgressMonitor(pm, 1));
		is.create(false, new SubProgressMonitor(pm, 1));
		pm.done();
	}

	private String createInterfaceSource(CompilationUnit cuNode) throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		buff.append(createInterfaceModifierString())
			 .append("interface ")//$NON-NLS-1$
			 .append(fNewInterfaceName)
			 .append(" {")//$NON-NLS-1$
			 .append(getLineSeperator())
			 .append(createInterfaceMemberDeclarationsSource(cuNode))
			 .append("}");//$NON-NLS-1$
		return buff.toString();
	}

	private String createInterfaceModifierString() throws JavaModelException {
		if (JdtFlags.isPublic(fInputType))
			return JdtFlags.VISIBILITY_STRING_PUBLIC + " ";//$NON-NLS-1$
		else
			return JdtFlags.VISIBILITY_STRING_PACKAGE;	
	}

	private String createInterfaceMemberDeclarationsSource(CompilationUnit cuNode) throws JavaModelException {
		if (fExtractedMembers.length == 0)
			return "";//$NON-NLS-1$
		StringBuffer buff= new StringBuffer();
		sortByOffset(fExtractedMembers);
		for (int i= 0; i < fExtractedMembers.length; i++) {
			buff.append(createInterfaceMemberDeclarationsSource(fExtractedMembers[i], cuNode));
			if (i != fExtractedMembers.length - 1)
				buff.append(getLineSeperator());
		}
		return buff.toString();
	}
	
	private static void sortByOffset(IMember[] members) {
		Arrays.sort(members, new Comparator(){
			public int compare(Object o1, Object o2) {
				ISourceReference sr1= (ISourceReference)o1;
				ISourceReference sr2= (ISourceReference)o2;
				try {
					return sr1.getSourceRange().getOffset() - sr2.getSourceRange().getOffset();
				} catch (JavaModelException e) {
					return o1.hashCode() - o2.hashCode();
				}
			}
		});
	}

	private String createInterfaceMemberDeclarationsSource(IMember iMember, CompilationUnit cuNode) throws JavaModelException {
		Assert.isTrue(iMember.getElementType() == IJavaElement.FIELD || iMember.getElementType() == IJavaElement.METHOD);
		if (iMember.getElementType() == IJavaElement.FIELD)
			return createInterfaceFieldDeclarationSource((IField)iMember);
		else 
			return createInterfaceMethodDeclarationsSource((IMethod)iMember, cuNode);
	}
	
	private String createInterfaceFieldDeclarationSource(IField iField) throws JavaModelException {
		return SourceRangeComputer.computeSource(iField);
	}

	private String createInterfaceMethodDeclarationsSource(IMethod iMethod, CompilationUnit cuNode) throws JavaModelException {
		if (fInputType.isInterface())
			return SourceRangeComputer.computeSource(iMethod);
		MethodDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, cuNode);
		if (methodDeclaration == null)
			return ""; //$NON-NLS-1$
		
		int methodDeclarationOffset= methodDeclaration.getReturnType().getStartPosition();
		int length= getMethodDeclarationLength(iMethod, methodDeclaration);
		
		StringBuffer methodDeclarationSource= new StringBuffer();
		methodDeclarationSource.append(getCommentContent(iMethod));
		if (fMarkInterfaceMethodsAsPublic)
			methodDeclarationSource.append("public ");//$NON-NLS-1$
		if (fMarkInterfaceMethodsAsAbstract)
			methodDeclarationSource.append("abstract ");//$NON-NLS-1$
		methodDeclarationSource.append(iMethod.getCompilationUnit().getBuffer().getText(methodDeclarationOffset, length));
		
		if (methodDeclaration.getBody() != null)
			methodDeclarationSource.append(";"); //$NON-NLS-1$
		
		return methodDeclarationSource.toString();	
	}
	
	private static String getCommentContent(IMethod iMethod) throws JavaModelException {
		String rawContent= JavaElementCommentFinder.getCommentContent(iMethod);
		if (rawContent == null)
			return ""; //$NON-NLS-1$
		String[] lines= Strings.convertIntoLines(rawContent);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(iMethod));
	}
	
	private static int getMethodDeclarationLength(IMethod iMethod, MethodDeclaration methodDeclaration) throws JavaModelException{
		int preDeclarationSourceLength= methodDeclaration.getReturnType().getStartPosition() - iMethod.getSourceRange().getOffset();
		if (methodDeclaration.getBody() == null)
			return iMethod.getSourceRange().getLength() - preDeclarationSourceLength;
		else
			return iMethod.getSourceRange().getLength() - methodDeclaration.getBody().getLength() - preDeclarationSourceLength;
	}

	private IField[] getExtractedFields(ICompilationUnit cu){
		List fields= new ArrayList();
		for (int i= 0; i < fExtractedMembers.length; i++) {
			if (fExtractedMembers[i] instanceof IField){
				IJavaElement element= JavaModelUtil.findInCompilationUnit(cu, fExtractedMembers[i]);
				if (element instanceof IField)
					fields.add(element);
			}
		}
		return (IField[]) fields.toArray(new IField[fields.size()]);
	}

	private IMethod[] getExtractedMethods(ICompilationUnit cu){
		List methods= new ArrayList();
		for (int i= 0; i < fExtractedMembers.length; i++) {
			if (fExtractedMembers[i] instanceof IMethod){
				IJavaElement element= JavaModelUtil.findInCompilationUnit(cu, fExtractedMembers[i]);
				if (element instanceof IMethod)
					methods.add(element);
			}
		}
		return (IMethod[]) methods.toArray(new IMethod[methods.size()]);
	}

	private void addImportsToTypesReferencedInFieldDeclarations(ImportsStructure is, IProgressMonitor pm) throws JavaModelException {
		IType[] referencedTypes= ReferenceFinderUtil.getTypesReferencedIn(getExtractedFields(getInputTypeCU()), pm);
		for (int i= 0; i < referencedTypes.length; i++) {
			is.addImport(JavaModelUtil.getFullyQualifiedName(referencedTypes[i]));
		}
	}
	
	private void addImportsToTypesReferencedInMethodDeclarations(ImportsStructure is, IProgressMonitor pm, CompilationUnit cuNode) throws JavaModelException {
		ITypeBinding[] typesUsed= getTypesUsedInExtractedMethodDeclarations(cuNode);
		pm.beginTask("", typesUsed.length); //$NON-NLS-1$
		for (int i= 0; i < typesUsed.length; i++) {
			is.addImport(typesUsed[i]);
			pm.worked(1);
		}
		pm.done();
	}
	
	private ITypeBinding[] getTypesUsedInExtractedMethodDeclarations(CompilationUnit cuNode) throws JavaModelException{
		return getTypesReferencedInDeclarations(getExtractedMethods(getInputTypeCU()), cuNode);
	}

	private ICompilationUnit getInputTypeCU() {
		return fInputType.getCompilationUnit();
	}

	//only in declarations - not in bodies
	public ITypeBinding[] getTypesReferencedInDeclarations(IMethod[] methods, CompilationUnit cuNode) throws JavaModelException{
		Set typesUsed= new HashSet();
		for (int i= 0; i < methods.length; i++) {
			typesUsed.addAll(getTypesUsedInDeclaration(methods[i], cuNode));
		}
		return (ITypeBinding[]) typesUsed.toArray(new ITypeBinding[typesUsed.size()]);
	}

	//Set<ITypeBinding>
	public Set getTypesUsedInDeclaration(IMethod iMethod, CompilationUnit cuNode) throws JavaModelException {
		return ReferenceFinderUtil.getTypesUsedInDeclaration(ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, cuNode));
	}
}
