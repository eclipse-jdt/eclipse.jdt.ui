package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeBlock;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TemplateUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;

public class MoveInnerToTopRefactoring extends Refactoring{
	
	private IType fType;
	private TextChangeManager fChangeManager;
	private final ImportEditManager fImportEditManager;
	private final CodeGenerationSettings fCodeGenerationSettings;
	
	public MoveInnerToTopRefactoring(IType type, CodeGenerationSettings codeGenerationSettings){
		Assert.isTrue(type.exists());
		Assert.isNotNull(codeGenerationSettings);
		fType= type;
		fCodeGenerationSettings= codeGenerationSettings;
		fImportEditManager= new ImportEditManager(codeGenerationSettings);
	}
	
	public IType getInputType(){
		return fType;
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= Checks.checkAvailability(fType);	
		if (result.hasFatalError())
			return result;
		if (Checks.isTopLevel(fType))
			return RefactoringStatus.createFatalErrorStatus("This refactoring is available only on nested types.");
		if (! JdtFlags.isStatic(fType)) //XXX for now
			return RefactoringStatus.createFatalErrorStatus("This refactoring is available only on static nested types.");
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
		IType orig= (IType)WorkingCopyUtil.getOriginal(fType);
		if (orig == null || ! orig.exists()){
			String key= "The selected type has been deleted from ''{0}''";
			String message= MessageFormat.format(key, new String[]{getInputTypeCu().getElementName()});
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fType= orig;
		
		return Checks.checkIfCuBroken(fType);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();		

			if (getInputTypePackage().getCompilationUnit(getNameForNewCu()).exists()){
				String pattern= "Compilation Unit named ''{0}'' already exists in package ''{1}''";
				String message= MessageFormat.format(pattern, new String[]{getNameForNewCu(), getInputTypePackage().getElementName()});
				result.addFatalError(message);
			}	
			result.merge(Checks.checkCompilationUnitName(getNameForNewCu()));
			
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
		IType type= Checks.findTypeInPackage(getInputTypePackage(), fType.getElementName());
		if (type == null || ! type.exists())
			return null;
		String pattern= "Type named ''{0}'' already exists in package ''{1}''";
		String message= MessageFormat.format(pattern, new String[]{fType.getElementName(), getInputTypePackage().getElementName()});
		return RefactoringStatus.createFatalErrorStatus(message);
	}
	
	private IPackageFragment getInputTypePackage() {
		return fType.getPackageFragment();
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return "Move Nested Type to Top Level";
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Creating change", 1);
			CompositeChange builder= new CompositeChange("Move Nested Type to Top Level");
			builder.addAll(fChangeManager.getAllChanges());
			builder.add(createCompilationUnitForMovedType(new SubProgressMonitor(pm, 1)));
			return builder;	
		} catch(CoreException e){
			throw new JavaModelException(e);
		}	
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			cutType(manager);
			removeUnusedImports(new SubProgressMonitor(pm, 1));
			updateTypeReferences(manager);
			fImportEditManager.fill(manager);
			return manager;
		} finally{
			pm.done();
		}	
	}

	private void updateTypeReferences(TextChangeManager manager) {
		//XXX
	}

	private void removeUnusedImports(IProgressMonitor pm) throws CoreException {
		IType[] types= getTypesReferencedOnlyInInputType(pm);
		for (int i= 0; i < types.length; i++) {
			fImportEditManager.removeImportTo(types[i], getInputTypeCu());
		}
	}
	
	private IType[] getTypesReferencedOnlyInInputType(IProgressMonitor pm) throws JavaModelException{
		//XXX
		return new IType[0];
	}

	private void cutType(TextChangeManager manager) throws CoreException {
		DeleteSourceReferenceEdit edit= new DeleteSourceReferenceEdit(fType, getInputTypeCu());
		manager.get(getInputTypeCu()).addTextEdit("Cut type", edit);
	}

	private ICompilationUnit getInputTypeCu() {
		return fType.getCompilationUnit();
	}

	//----- methods related to creation of the new cu -------
	private IChange createCompilationUnitForMovedType(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCuWC= getInputTypePackage().getCompilationUnit(getNameForNewCu());
		return new CreateTextFileChange(createPathForNewCu(), createSourceForNewCu(newCuWC, pm), true);	
	}

	private String createSourceForNewCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2);
		StringBuffer buff= new StringBuffer();
		buff.append(createCuSourcePrefix(new SubProgressMonitor(pm, 1), newCu));
		buff.append(createTypeSource(new SubProgressMonitor(pm, 1)));
		pm.done();
		return buff.toString();
	}

	private String createCuSourcePrefix(IProgressMonitor pm, ICompilationUnit newCu) throws CoreException{
		pm.beginTask("", 1);
		StringBuffer buffer= new StringBuffer();
		if (fCodeGenerationSettings.createFileComments)
			buffer.append(TemplateUtil.createFileCommentsSource(newCu));
		buffer.append(createPackageDeclarationSource());
		buffer.append(createImportsSource(new SubProgressMonitor(pm, 1)));
		buffer.append(getLineSeperator());
		ICodeFormatter codeFormatter= ToolFactory.createCodeFormatter();
		pm.done();
		return codeFormatter.format(buffer.toString(), 0, null, getLineSeperator());
	}
	
	private String createImportsSource(IProgressMonitor pm) throws JavaModelException {
		IType[] typesReferencedInInputType= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[]{fType}, pm);
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < typesReferencedInInputType.length; i++) {
			IType iType= typesReferencedInInputType[i];
			if (! isImplicityImported(iType))
				buff.append("import ").append(JavaElementUtil.createSignature(iType)).append(";");
		}
		return buff.toString();
	}

	private boolean isImplicityImported(IType iType) {
		return iType.getParent().getElementName().equals("java.lang") || iType.getPackageFragment().equals(getInputTypePackage());
	}

	private String createTypeSource(IProgressMonitor pm) throws JavaModelException {
		String updatedTypeSource= MemberMoveUtil.computeNewSource(fType, pm, fImportEditManager, new IType[]{fType});
		StringBuffer updatedTypeSourceBuffer= new StringBuffer(updatedTypeSource);
		ISourceRange[] ranges= getRangesOfUnneededModifiers();
		SourceRange.reverseSortByOffset(ranges);
		int typeoffset= getTypeDefinitionOffset().getOffset();
		for (int i= 0; i < ranges.length; i++) {
			ISourceRange iSourceRange= ranges[i];
			int offset= iSourceRange.getOffset()  - typeoffset;
			//add 1 to length to remove the space after
			updatedTypeSourceBuffer.delete(offset, offset + iSourceRange.getLength() + 1);
		}
		CodeBlock cb= new CodeBlock(updatedTypeSourceBuffer.toString());
		StringBuffer buffer= new StringBuffer();
		cb.fill(buffer, "", getLineSeperator());
		return buffer.toString().trim();
	}

	private ISourceRange getTypeDefinitionOffset() throws JavaModelException {
		return SourceReferenceSourceRangeComputer.computeSourceRange(fType, fType.getCompilationUnit().getSource());
	}

	private ISourceRange[] getRangesOfUnneededModifiers() throws JavaModelException {
		try {
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(fType.getCompilationUnit().getBuffer().getCharacters());
			scanner.resetTo(fType.getSourceRange().getOffset(), fType.getNameRange().getOffset());
			List result= new ArrayList(2);
			int token= scanner.getNextToken();
			while(token != ITerminalSymbols.TokenNameEOF){
				switch (token){
					case ITerminalSymbols.TokenNamestatic:
					case ITerminalSymbols.TokenNameprotected:
					case ITerminalSymbols.TokenNameprivate:
						result.add(new SourceRange(scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() - scanner.getCurrentTokenStartPosition() +1));
						break;
				}
				token= scanner.getNextToken();
			}
			return (ISourceRange[]) result.toArray(new ISourceRange[result.size()]);
		} catch (InvalidInputException e) {
			return new ISourceRange[0];
		}
	}

	private String createPackageDeclarationSource() {
		return "package " + getInputTypePackage().getElementName() + ";";//$NON-NLS-2$ //$NON-NLS-1$
	}
	
	private IPath createPathForNewCu() throws JavaModelException {
		return ResourceUtil.getFile(getInputTypeCu()).getFullPath()
										.removeLastSegments(1)
										.append(getNameForNewCu());
	}

	private String getNameForNewCu() {
		return fType.getElementName() + ".java";
	}

	private static String getLineSeperator() {
		return System.getProperty("line.separator", "\n");//$NON-NLS-1$ //$NON-NLS-2$
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
}
