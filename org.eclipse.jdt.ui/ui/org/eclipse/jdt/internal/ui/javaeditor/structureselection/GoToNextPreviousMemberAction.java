package org.eclipse.jdt.internal.ui.javaeditor.structureselection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public class GoToNextPreviousMemberAction extends Action {

	public static final String NEXT_MEMBER= "GoToNextMember"; //$NON-NLS-1$
	public static final String PREVIOUS_MEMBER= "GoToPreviousMember"; //$NON-NLS-1$
	private CompilationUnitEditor fEditor;
	private boolean fIsGotoNext;

	public static GoToNextPreviousMemberAction newGoToNextMemberAction(CompilationUnitEditor editor) {
		return new GoToNextPreviousMemberAction(editor, "Go To N&ext Member", true);
	}

	public static GoToNextPreviousMemberAction newGoToPreviousMemberAction(CompilationUnitEditor editor) {
		return new GoToNextPreviousMemberAction(editor, "Go To Previ&ous Member", false);
	}
	
	private GoToNextPreviousMemberAction(CompilationUnitEditor editor, String text, boolean isGotoNext) {
		super(text);
		Assert.isNotNull(editor);
		fEditor= editor;
		fIsGotoNext= isGotoNext;
		setEnabled(null != SelectionConverter.getInputAsCompilationUnit(fEditor));
	}
	
	/*
	 * This constructor is for testing purpose only.
	 */
	public GoToNextPreviousMemberAction(boolean isSelectNext) {
		super(""); //$NON-NLS-1$
		fIsGotoNext= isSelectNext;
	}
	
	/* (non-JavaDoc)
	 * Method declared in IAction.
	 */
	public final  void run() {
		ITextSelection selection= getTextSelection();
		ISourceRange newRange= getNewSelectionRange(createSourceRange(selection), getCompilationUnit());
		// Check if new selection differs from current selection
		if (selection.getOffset() == newRange.getOffset() && selection.getLength() == newRange.getLength())
			return;
		fEditor.selectAndReveal(newRange.getOffset(), newRange.getLength());
	}

	private ICompilationUnit getCompilationUnit() {
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
	}
	
	private ITextSelection getTextSelection() {
		return (ITextSelection)fEditor.getSelectionProvider().getSelection();
	}
	
	public ISourceRange getNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu){
		try{
			Integer[] offsetArray= createOffsetArray(cu);
			if (offsetArray.length == 0)
				return oldSourceRange;
			Arrays.sort(offsetArray);
			Integer oldOffset= new Integer(oldSourceRange.getOffset());
			int index= Arrays.binarySearch(offsetArray, oldOffset);
			
			if (fIsGotoNext)
				return createNewSourceRange(getNextOffset(index, offsetArray, oldOffset));
			else
				return createNewSourceRange(getPreviousOffset(index, offsetArray, oldOffset));

	 	}	catch (JavaModelException e){
	 		JavaPlugin.log(e); //dialog would be too heavy here
	 		return oldSourceRange;
	 	}
	}
	
	private static Integer getPreviousOffset(int index, Integer[] offsetArray, Integer oldOffset){
		if (index == -1)
			return oldOffset;
		if (index == 0)
			return offsetArray[0];
		if (index > 0)
			return offsetArray[index - 1];
		Assert.isTrue(index < -1);
		int absIndex= Math.abs(index);
		return offsetArray[absIndex - 2];	
	}
	
	private static Integer getNextOffset(int index, Integer[] offsetArray, Integer oldOffset){
		if (index == -1)
			return offsetArray[0];

		if (index == 0){
			if (offsetArray.length != 1)
				return offsetArray[1];
			else	
				return offsetArray[0];
		}	
		if (index > 0){
			if (index == offsetArray.length - 1)
				return oldOffset;
			return offsetArray[index + 1];
		}	
		Assert.isTrue(index < -1);
		int absIndex= Math.abs(index);
		if (absIndex > offsetArray.length)
			return oldOffset;
		else	
			return offsetArray[absIndex - 1];	
	}
	
	private static ISourceRange createNewSourceRange(Integer offset){
		return new SourceRange(offset.intValue(), 0);
	}

	private static Integer[] createOffsetArray(ICompilationUnit cu) throws JavaModelException {
		List result= new ArrayList();
		IType[] types= cu.getAllTypes();
		for (int i= 0; i < types.length; i++) {
			IType iType= types[i];
			result.add(new Integer(iType.getNameRange().getOffset()));
			result.add(new Integer(iType.getSourceRange().getOffset() + iType.getSourceRange().getLength()));
			addMemberOffsetList(iType.getMethods(), result);
			addMemberOffsetList(iType.getFields(), result);
			addMemberOffsetList(iType.getInitializers(), result);
		}
		return (Integer[]) result.toArray(new Integer[result.size()]);
	}

	private static void addMemberOffsetList(IMember[] members, List result) throws JavaModelException {
		for (int i= 0; i < members.length; i++) {
			result.add(new Integer(getOffset(members[i])));
		}
	}

	private static int getOffset(IMember iMember) throws JavaModelException {
		//workaround for bug 23257
		if (iMember.getNameRange() != null && iMember.getNameRange().getOffset() >= 0)
			return iMember.getNameRange().getOffset();
		return iMember.getSourceRange().getOffset();
	}
	
	//-- private helper methods
	
	private static ISourceRange createSourceRange(ITextSelection ts){
		return new SourceRange(ts.getOffset(), ts.getLength());
	}
}
