package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TemplateUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;

public class ExtractInterfaceRefactoring extends Refactoring {

	private final CodeGenerationSettings fCodeGenerationSettings;
	private final ASTNodeMappingManager fASTMappingManager;
	private IType fInputClass;
	private String fNewInterfaceName;
	private IMember[] fExtractedMembers;
	private boolean fReplaceOccurrences= false;
	private TextChangeManager fChangeManager;
	
	public ExtractInterfaceRefactoring(IType clazz, CodeGenerationSettings codeGenerationSettings){
		Assert.isNotNull(clazz);
		Assert.isNotNull(codeGenerationSettings);
		fInputClass= clazz;
		fCodeGenerationSettings= codeGenerationSettings;
		fExtractedMembers= new IMember[0];
		fASTMappingManager= new ASTNodeMappingManager();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return "Extract Interface";
	}
	
	public IType getInputClass() {
		return fInputClass;
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
		IJavaElement[] children= fInputClass.getChildren();
		for (int i= 0; i < children.length; i++) {
			if (children[i] instanceof IMember && isExtractableMember((IMember)children[i]))
				members.add(children[i]);
		}
		return (IMember[]) members.toArray(new IMember[members.size()]);
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= Checks.checkAvailability(fInputClass);	
		if (result.hasFatalError())
			return result;
		if (fInputClass.isInterface())
			result.addFatalError("Cannot perform Extract Interface on interfaces.");
		if (result.hasFatalError())
			return result;

		if (fInputClass.isLocal())
			result.addFatalError("Cannot perform Extract Interface on local types.");
		if (result.hasFatalError())
			return result;

		if (fInputClass.isAnonymous())
			result.addFatalError("Cannot perform Extract Interface on anonymous types.");
		if (result.hasFatalError())
			return result;

		//XXX for now
		if (! Checks.isTopLevel(fInputClass))
			result.addFatalError("Cannot perform Extract Interface on member classes.");
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
		IType orig= (IType)WorkingCopyUtil.getOriginal(fInputClass);
		if (orig == null || ! orig.exists()){
			String key= "The selected type has been deleted from ''{0}''";
			String message= MessageFormat.format(key, new String[]{fInputClass.getCompilationUnit().getElementName()});
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fInputClass= orig;
		
		if (Checks.isException(fInputClass, pm)){
			String message= "Extract Interface refactoring is not available on \"java.lang.Throwable\" and its subclasses";
			return RefactoringStatus.createFatalErrorStatus(message);
		}
			
		return Checks.checkIfCuBroken(fInputClass);
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
		String pattern= "Type named ''{0}'' already exists in package ''{1}''";
		String message= MessageFormat.format(pattern, new String[]{fNewInterfaceName, getInputClassPackage().getElementName()});
		return RefactoringStatus.createFatalErrorStatus(message);
	}
	
	public RefactoringStatus checkNewInterfaceName(String newName){
		try {
			RefactoringStatus result= Checks.checkTypeName(newName);
			if (result.hasFatalError())
				return result;
			result.merge(Checks.checkCompilationUnitName(newName + ".java"));
			if (result.hasFatalError())
				return result;
	
			if (getInputClassPackage().getCompilationUnit(getCuNameForNewInterface()).exists()){
				String pattern= "Compilation Unit named ''{0}'' already exists in package ''{1}''";
				String message= MessageFormat.format(pattern, new String[]{getCuNameForNewInterface(), getInputClassPackage().getElementName()});
				result.addFatalError(message);
				if (result.hasFatalError())
					return result;
			}	
			result.merge(checkInterfaceTypeName());
			return result;
		} catch (JavaModelException e) {
			return RefactoringStatus.createFatalErrorStatus("Internal Error. Please see log for details.");
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
			pm.beginTask("", 1);
			CompositeChange builder= new CompositeChange("Extract Interface");
			builder.addAll(fChangeManager.getAllChanges());
			builder.add(createExtractedInterface());
			return builder;	
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
			pm.setTaskName("Analyzing...");
			TextChangeManager manager= new TextChangeManager();
			updateTypeDeclaration(manager, new SubProgressMonitor(pm, 1));
			if (fReplaceOccurrences)
				updateReferences(manager, new SubProgressMonitor(pm, 9));
			else
				pm.worked(1);	
			return manager;
		} finally{
			pm.done();
		}	
	}

	private void updateReferences(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException, CoreException {
		UseSupertypeWherePossibleUtil.updateReferences(manager, fExtractedMembers, fNewInterfaceName, fInputClass, fCodeGenerationSettings, fASTMappingManager, pm);
	}
	
	private boolean areAllExtractableMembersOfClass(IMember[] extractedMembers) throws JavaModelException {
		for (int i= 0; i < extractedMembers.length; i++) {
			if (! extractedMembers[i].getParent().equals(fInputClass))
				return false;
			if (! isExtractableMember(extractedMembers[i]))
				return false;
		}
		return true;
	}

	private static boolean isExtractableMember(IMember iMember) throws JavaModelException {
		if (iMember.getElementType() == IJavaElement.METHOD)
			return isExtractableMethod((IMethod)iMember);
//		if (iMember.getElementType() == IJavaElement.FIELD)	
//			return isExtractableField((IField)iMember);
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
	private IChange createExtractedInterface() throws CoreException {
		String lineSeparator= getLineSeperator(); 
		IPath cuPath= ResourceUtil.getFile(fInputClass.getCompilationUnit()).getFullPath();
		IPath interfaceCuPath= cuPath
										.removeLastSegments(1)
										.append(getCuNameForNewInterface());
		//XXX need to destroy
		ICompilationUnit newCuWC= getInputClassPackage().getCompilationUnit(getCuNameForNewInterface());
		String source= createExtractedInterfaceCUSource(newCuWC);
		String formattedSource= ToolFactory.createCodeFormatter().format(source, 0, null, lineSeparator);
		return new CreateTextFileChange(interfaceCuPath, formattedSource, true);	
	}
	
	private String getCuNameForNewInterface() {
		return fNewInterfaceName + ".java";
	}
	
	private IPackageFragment getInputClassPackage() {
		return fInputClass.getPackageFragment();
	}

	private String getLineSeperator() {
		try {
			return StubUtility.getLineDelimiterUsed(fInputClass);
		} catch (JavaModelException e) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private boolean inputClassHasDirectSuperinterfaces() throws JavaModelException {
		return fInputClass.getSuperInterfaceNames().length > 0;
	}

	private void updateTypeDeclaration(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);//$NON-NLS-1$
		String editName= "Update Type Declaration";
		int offset= computeIndexOfSuperinterfaceNameInsertion();
		String text=  computeTextOfSuperinterfaceNameInsertion();
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fInputClass.getCompilationUnit());
		manager.get(cu).addTextEdit(editName, SimpleTextEdit.createInsert(offset, text));
		pm.done();
	}

	private String computeTextOfSuperinterfaceNameInsertion() throws JavaModelException {
		if (! inputClassHasDirectSuperinterfaces())
			return " implements " + fNewInterfaceName;	//$NON-NLS-1$
		else
			return ", " + fNewInterfaceName; //$NON-NLS-1$
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
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(fInputClass.getNameRange().getOffset(), fInputClass.getNameRange().getLength() +1), true);
		getAST(fInputClass.getCompilationUnit()).accept(analyzer);
		if (analyzer.getFirstSelectedNode() != null){
			if (analyzer.getFirstSelectedNode().getParent() instanceof TypeDeclaration)
				return (TypeDeclaration)analyzer.getFirstSelectedNode().getParent();
		}
		//XXX workaround for 21757
		if (analyzer.getLastCoveringNode() instanceof TypeDeclaration)
			return (TypeDeclaration) analyzer.getLastCoveringNode();
		return null;	
	}

	private String createExtractedInterfaceCUSource(ICompilationUnit newCu) throws CoreException {
		StringBuffer buffer= new StringBuffer();
		if (fCodeGenerationSettings.createFileComments)
			buffer.append(TemplateUtil.createFileCommentsSource(newCu));
		if (! getInputClassPackage().isDefaultPackage())	
			buffer.append(createPackageDeclarationSource());
		buffer.append(createImportsSource());
		if (fCodeGenerationSettings.createComments){
			buffer.append(getLineSeperator());
			buffer.append(TemplateUtil.createTypeCommentSource(newCu));
		}	
		buffer.append(createInterfaceSource());
		return buffer.toString();
	}

	private String createInterfaceSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		buff.append(createInterfaceModifierString())
			 .append("interface ")
			 .append(fNewInterfaceName)
			 .append(" {")
			 .append(createInterfaceMemberDeclarationsSource())
			 .append("}");
		return buff.toString();
	}

	private String createInterfaceModifierString() throws JavaModelException {
		if (JdtFlags.isPublic(fInputClass))
			return "public ";
		else
			return "";	
	}

	private String createInterfaceMemberDeclarationsSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		sortByOffset(fExtractedMembers);
		for (int i= 0; i < fExtractedMembers.length; i++) {
			buff.append(createInterfaceMemberDeclarationsSource(fExtractedMembers[i]));
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
			return iMember.getSource();
		else 
			return createInterfaceMethodDeclarationsSource((IMethod)iMember);
	}

	private String createInterfaceMethodDeclarationsSource(IMethod iMethod) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(iMethod);
		if (methodDeclaration == null)
			return ""; 
	
		int offset= methodDeclaration.getReturnType().getStartPosition();
		int length= getMethodDeclarationLength(iMethod, methodDeclaration);
		String methodDeclarationSource= iMethod.getCompilationUnit().getBuffer().getText(offset, length);
		if (methodDeclaration.getBody() == null)
			return methodDeclarationSource;
		else
			return methodDeclarationSource + ";";
	}
	
	private static int getMethodDeclarationLength(IMethod iMethod, MethodDeclaration methodDeclaration) throws JavaModelException{
		int preDeclarationSourceLength= methodDeclaration.getReturnType().getStartPosition() - iMethod.getSourceRange().getOffset();
		if (methodDeclaration.getBody() == null)
			return methodDeclaration.getLength() - preDeclarationSourceLength;
		else
			return iMethod.getSourceRange().getLength() - methodDeclaration.getBody().getLength() - preDeclarationSourceLength;
	}

	//XXX same as in the util class
	private MethodDeclaration getMethodDeclarationNode(IMethod iMethod) throws JavaModelException{
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(iMethod.getSourceRange().getOffset(), iMethod.getSourceRange().getLength()), true);
		getAST(iMethod.getCompilationUnit()).accept(analyzer);
		if (! (analyzer.getFirstSelectedNode() instanceof MethodDeclaration))
			return null;
		return (MethodDeclaration) analyzer.getFirstSelectedNode();
	}
	
	private String createImportsSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		ITypeBinding[] typesUsed= getTypesUsedInExtractedMemberDeclarations();
		for (int i= 0; i < typesUsed.length; i++) {
			ITypeBinding binding= typesUsed[i];
			if (binding != null && ! binding.isPrimitive()){
				buff.append("import ");//$NON-NLS-1$
				buff.append(Bindings.getFullyQualifiedImportName(binding));
				buff.append(";");//$NON-NLS-1$
			}	
		}
		return buff.toString();
	}

	private ITypeBinding[] getTypesUsedInExtractedMemberDeclarations() throws JavaModelException{
		Set typesUsed= new HashSet();
		for (int i= 0; i < fExtractedMembers.length; i++) {
			if (fExtractedMembers[i].getElementType() == IJavaElement.METHOD)
				typesUsed.addAll(getTypesUsedInDeclaration((IMethod)fExtractedMembers[i]));
			else if (fExtractedMembers[i].getElementType() == IJavaElement.FIELD)	
				typesUsed.addAll(getTypesUsedInDeclaration((IField)fExtractedMembers[i]));	
			else
				Assert.isTrue(false);	
		}
		return (ITypeBinding[]) typesUsed.toArray(new ITypeBinding[typesUsed.size()]);
	}
	
	//set of ITypeBindings
	private Set getTypesUsedInDeclaration(IField iField) {
		//XXX
		return new HashSet(0);
	}

	//set of ITypeBindings
	private Set getTypesUsedInDeclaration(IMethod iMethod) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(iMethod);
		if (methodDeclaration == null)
			return new HashSet(0);
		Set result= new HashSet();	
		result.add(methodDeclaration.getReturnType().resolveBinding());
			
		for (Iterator iter= methodDeclaration.parameters().iterator(); iter.hasNext();) {
			result.add(((SingleVariableDeclaration) iter.next()).getType().resolveBinding()); 
		}
		
		for (Iterator iter= methodDeclaration.thrownExceptions().iterator(); iter.hasNext();) {
			result.add(((Name) iter.next()).resolveTypeBinding());
		}
		return result;
	}

	private String createPackageDeclarationSource() {
		return "package " + getInputClassPackage().getElementName() + ";";//$NON-NLS-2$ //$NON-NLS-1$
	}
	private CompilationUnit getAST(ICompilationUnit cu){
		return fASTMappingManager.getAST(cu);
	}
	
	private ICompilationUnit getCompilationUnit(ASTNode node) {
		return fASTMappingManager.getCompilationUnit(node);
	}
}
