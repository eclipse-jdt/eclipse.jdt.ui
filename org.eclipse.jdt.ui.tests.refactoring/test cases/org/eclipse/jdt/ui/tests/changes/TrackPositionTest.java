package org.eclipse.jdt.ui.tests.changes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;

public class TrackPositionTest extends TestCase {

	private static final Class THIS= TrackPositionTest.class;
	private static final String NN= "N.N";
	
	private TextBuffer fBuffer;
	private TextBufferChange fChange;
	
	public TrackPositionTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new TestSuite(THIS);
	}
	
	protected void setUp() throws Exception {
		fBuffer= TextBuffer.create("0123456789");
		fChange= new TextBufferChange(NN, fBuffer);
		fChange.setTrackPositionChanges(true);
	}
	
	protected void tearDown() throws Exception {
		fChange= null;
	}

	public void test1() throws Exception {
		TextEdit edit= SimpleTextEdit.createReplace(2, 2, "xyz");
		fChange.addTextEdit(NN, edit);
		executeChange();
		assert(fChange.getNewTextRange(edit), 2, 3);
	}
	
	public void test2() throws Exception {
		TextEdit edit= SimpleTextEdit.createReplace(5, 3, "xy");
		fChange.addTextEdit(NN, edit);
		TextBuffer preview= fChange.getPreviewTextBuffer();
		assertEquals(fBuffer.getContent(), "0123456789");
		assertEquals(preview.getContent(), "01234xy89");
		assert(fChange.getNewTextRange(edit), 5, 2);
	}
		
	private void executeChange() throws Exception {
		try {
			ChangeContext context= new ChangeContext(new AbortChangeExceptionHandler());
			fChange.aboutToPerform(context, new NullProgressMonitor());
			fChange.perform(context, new NullProgressMonitor());
		} finally {
			fChange.performed();
		}
	}
	
	private void assert(TextRange r, int offset, int length) {
		assertEquals("Offset", offset, r.getOffset());
		assertEquals("Length", length, r.getLength());	
	}	
}