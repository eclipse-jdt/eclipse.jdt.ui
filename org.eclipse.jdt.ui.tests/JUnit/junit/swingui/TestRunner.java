package junit.swingui;

import junit.framework.*;
import junit.util.*;

import java.util.Vector;
import java.lang.reflect.*;
import java.text.NumberFormat;
import java.net.URL;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

 
/**
 * A simple user interface to run tests.
 * Enter the name of a class with a suite method which should return
 * the tests to be run.
 * <pre>
 * Synopsis: java java.swingui.TestRunner [TestCase]
 * </pre>
 * TestRunner takes as an optional argument the name of the testcase class to be run.
 */
public class TestRunner extends Object implements TestListener, DocumentListener {
	protected JFrame fFrame;
	private Thread fRunner;
	private TestResult fTestResult;
	private TestSuiteLoader fTestLoader;
	
	private TraceFrame fTraceFrame;
	private TestBrowser fTestBrowser;
	private JComboBox fSuiteCombo;
	
	private JButton fRun;
	private ProgressBar fProgressIndicator;
	private JList fFailureList;
	private DefaultListModel fFailures;

	private JLabel fLogo;
	private JTextField fNumberOfErrors;
	private JTextField fNumberOfFailures;
	private JTextField fNumberOfRuns;
	private JButton fQuitButton;
	private JButton fShowErrorButton;
	private JButton fRerunButton;
	private JTextField fStatusLine;
	private JPanel fPanel;
	
	private static Font PLAIN_FONT= new Font("dialog", Font.PLAIN, 12);
	private static Font BOLD_FONT= new Font("dialog", Font.BOLD, 12);
	private static final int GAP= 4;
	private static final String SUITE_METHODNAME= "suite";

	static class FailureListCellRenderer extends DefaultListCellRenderer {
		private ImageIcon fFailureIcon;
		private ImageIcon fErrorIcon;
		
		FailureListCellRenderer() {
	    	super();
	    	URL url= getClass().getResource("failure.gif");
			fFailureIcon= new ImageIcon(url);
	    	url= getClass().getResource("error.gif");
	    	fErrorIcon= new ImageIcon(url);
		}

		public Component getListCellRendererComponent(
			JList list, Object value, int modelIndex, 
			boolean isSelected, boolean cellHasFocus) {

	    	TestFailure failure= (TestFailure)value;
	    	String text= failure.failedTest().toString();
			String msg= failure.thrownException().getMessage();
			if (msg != null) 
				text+= ":" + StringUtil.truncate(msg, 200); 
 
			if (failure.thrownException() instanceof AssertionFailedError) 
	    		setIcon(fFailureIcon);
	    	else 
	    		setIcon(fErrorIcon);
	    	return super.getListCellRendererComponent(list, text, modelIndex, isSelected, cellHasFocus);
		}
	}
	
	public TestRunner() {
	} 
	
	private void about() {
		AboutDialog about= new AboutDialog(fFrame); 
		about.setModal(true);
		about.setLocation(300, 300);
		about.show();
	}
	
	public void addError(Test test, Throwable t) {
		postError(test, t);
	}
	
	private void postError(final Test test, final Throwable t) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					fNumberOfErrors.setText(Integer.toString(fTestResult.errorCount()));
					appendFailure("Error", test, t);
				}
			}
		);
	}

	public void addFailure(final Test test, final Throwable t) {
		postFailure(test, t);
	}
	
	private void postFailure(final Test test, final Throwable t) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					fNumberOfFailures.setText(Integer.toString(fTestResult.failureCount()));
					appendFailure("Failure", test, t);
				}
			}		
		);
	}

	private void addGrid(JPanel p, Component co, int x, int y, int w, int fill, double wx, int anchor) {
		GridBagConstraints c= new GridBagConstraints();
		c.gridx= x; c.gridy= y;
		c.gridwidth= w;
		c.anchor= anchor;
		c.weightx= wx;
		c.fill= fill;
		if (fill == GridBagConstraints.BOTH || fill == GridBagConstraints.VERTICAL)
			c.weighty= 1.0;
		c.insets= new Insets(y == 0 ? GAP : 0, x == 0 ? GAP : 0, GAP, GAP);
		p.add(co, c);
	}
	
	private void addToHistory(final String suite) {
		for (int i= 0; i < fSuiteCombo.getItemCount(); i++) {
			if (suite.equals(fSuiteCombo.getItemAt(i))) {
				fSuiteCombo.removeItemAt(i);
				fSuiteCombo.insertItemAt(suite, 0);
				fSuiteCombo.setSelectedIndex(0);
				return;
			}
		}
		fSuiteCombo.insertItemAt(suite, 0);
		// prune the history
		for (int i= fSuiteCombo.getItemCount()-1; i > 5; i--) 
			fSuiteCombo.removeItemAt(i);
	}
	
	private void appendFailure(String kind, Test test, Throwable t) {
		fFailures.addElement(new TestFailure(test, t));
	}
		
	public void changedUpdate(DocumentEvent event) {
		textChanged();
	}
	
	protected void connectTestBrowser(Test testSuite, boolean reload) {
		if(fTestBrowser != null && fTestBrowser.isVisible()) {
			if (reload)
				fTestBrowser.showTestTree(testSuite);
			fTestResult.addListener(fTestBrowser.getTestListener());
		}
	}
	
	protected JPanel createCounterPanel() {
		fNumberOfErrors= createOutputField();
		fNumberOfFailures= createOutputField();
		fNumberOfRuns= createOutputField();
	
		JPanel numbersPanel= new JPanel(new GridLayout(2, 3));
		numbersPanel.add(new JLabel("Runs:"));		
		numbersPanel.add(new JLabel("Errors:"));	
		numbersPanel.add(new JLabel("Failures: "));	
		numbersPanel.add(fNumberOfRuns);
		numbersPanel.add(fNumberOfErrors);
		numbersPanel.add(fNumberOfFailures);
		return numbersPanel;
	}
	
	protected JPanel createFailedPanel() {
		JPanel failedPanel= new JPanel(new GridLayout(0, 1, 0, 2));

		fShowErrorButton= new JButton("Show...");
		fShowErrorButton.setEnabled(false);
		fShowErrorButton.setToolTipText("Show the Stack Trace");
		fShowErrorButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showErrorTrace();
				}
			}
		);

		fRerunButton= new JButton("Run");
		fRerunButton.setToolTipText("Run the Selected Test");
		fRerunButton.setEnabled(false);
		fRerunButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					rerun();
				}
			}
		);

		failedPanel.add(fShowErrorButton);
		failedPanel.add(fRerunButton);
		return failedPanel;
	}
	
	protected JList createFailureList(ListModel model) {
		JList list= new JList(model);
		list.setFixedCellWidth(300);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new FailureListCellRenderer());
		list.setToolTipText("Failure - grey X; Error - red X");
		list.setPreferredSize(new Dimension(200, 150));
		list.addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						showErrorTrace();
				}
			}
		);
		list.addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					fShowErrorButton.setEnabled(isErrorSelected());
					fRerunButton.setEnabled(isErrorSelected());
				}
			}
		);
		return list;
	}
	
	/**
	 * Creates the JUnit menu. Clients override this
	 * method to add additional menu items.
	 */
	protected JMenu createJUnitMenu() {
		JMenu menu= new JMenu("JUnit");
		menu.setMnemonic('J');
		JMenuItem mi1= new JMenuItem("About...");
		mi1.addActionListener(
		    new ActionListener() {
		        public void actionPerformed(ActionEvent event) {
		            about();
		        }
		    }
		);
		mi1.setMnemonic('A');
		menu.add(mi1);
		
		JMenuItem mi2= new JMenuItem("Show Test Browser");
		mi2.addActionListener(
		    new ActionListener() {
		        public void actionPerformed(ActionEvent event) {
		            showTestBrowser();
		        }
		    }
		);
		mi2.setMnemonic('S');
		menu.add(mi2);

		menu.addSeparator();
		JMenuItem mi3= new JMenuItem("Exit");
		mi3.addActionListener(
		    new ActionListener() {
		        public void actionPerformed(ActionEvent event) {
		            terminate();
		        }
		    }
		);
		mi3.setMnemonic('x');
		menu.add(mi3);

		return menu;
	}
	
	protected JFrame createFrame(String title) {
		JFrame frame= new JFrame("Run Test Suite");
		Image icon= loadFrameIcon();	
		if (icon != null)
			frame.setIconImage(icon);

		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		frame.setBackground(SystemColor.control);
		
		frame.addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					terminate();
				}
			}
		);
		return frame;
	}
	
	protected JLabel createLogo() {
		java.net.URL url= getClass().getResource("logo.gif");
		return new JLabel(new ImageIcon(url));
	}
	
	protected void createMenus(JMenuBar mb) {
		mb.add(createJUnitMenu());
	}
	
	private JTextField createOutputField() {
		JTextField field= new JTextField("0", 4);
		field.setHorizontalAlignment(JTextField.LEFT);
		field.setFont(BOLD_FONT);
		field.setEditable(false);
		field.setBorder(BorderFactory.createEmptyBorder());
		return field;
	}
	
	protected JButton createQuitButton() {
		JButton quit= new JButton("Exit");
		quit.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					terminate();
				}
			}
		);
		return quit;
	}
	
	protected JButton createRunButton() {
		JButton run= new JButton("Run");
		run.setEnabled(true);
		run.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					runSuite();
				}
			}
		);
		return run;
	}
	
	/**
	 * Hook to plug in a UI component on the run line
	 */
	protected Component createRunExtension() {
		return null;
	}
	
	protected JTextField createStatusLine() {
		JTextField status= new JTextField();
		status.setFont(BOLD_FONT);
		status.setEditable(false);
		status.setForeground(Color.red);
		status.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		Dimension d= status.getPreferredSize();
		d.width= 420;
		status.setPreferredSize(d);
		return status;
	}
	
	protected JComboBox createSuiteCombo() {
		JComboBox combo= new JComboBox();
		combo.setEditable(true);
		combo.getEditor().getEditorComponent().addKeyListener(
			new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					textChanged();
					if (e.getKeyChar() == KeyEvent.VK_ENTER)
						runSuite();
				}
			}
		);
		try {
			loadHistory(combo);
		} catch (IOException e) {
			// fails the first time
		}
		combo.addItemListener(
			new ItemListener() {
				public void itemStateChanged(ItemEvent event) {
					if (event.getStateChange() == ItemEvent.SELECTED) {
						textChanged();
					}
				}
			}
		);

		return combo;
	}
	
	protected TestResult createTestResult() {
		return new TestResult();
	}
	
	protected JFrame createUI(String suiteName) {	
		JFrame frame= createFrame("Run Test Suite");	
		JMenuBar mb= new JMenuBar();
		createMenus(mb);
		frame.setJMenuBar(mb);
	
		JLabel suiteLabel= new JLabel("Enter the name of the Test class:");
		fSuiteCombo= createSuiteCombo();
		fRun= createRunButton();
		Component runExtension= createRunExtension();
		
		JLabel progressLabel= new JLabel("Progress:");
		fProgressIndicator= new ProgressBar();
		JPanel numbersPanel= createCounterPanel();
		
		JLabel failureLabel= new JLabel("Errors and Failures:");
		fFailures= new DefaultListModel();
		fFailureList= createFailureList(fFailures);

		JPanel failedPanel= createFailedPanel();
		fStatusLine= createStatusLine();
		fQuitButton= createQuitButton();
	
		fLogo= createLogo();
		fLogo.setToolTipText("JUnit Version "+Version.id());
		
		JScrollPane scrolledList= new JScrollPane(
			fFailureList, 
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
		);
		
		//---- overall layout
		JPanel panel= new JPanel(new GridBagLayout());
		fPanel= panel;
	
		addGrid(panel, suiteLabel,		 0, 0, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		addGrid(panel, fSuiteCombo, 	 0, 1, 1, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		if (runExtension != null)
			addGrid(panel, runExtension, 1, 1, 1, GridBagConstraints.NONE, 			0.0, GridBagConstraints.WEST);
		addGrid(panel, fRun, 			 2, 1, 1, GridBagConstraints.HORIZONTAL, 	0.0, GridBagConstraints.CENTER);

		addGrid(panel, progressLabel, 	 0, 2, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		addGrid(panel, fProgressIndicator, 0, 3, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		addGrid(panel, fLogo, 			 2, 3, 1, GridBagConstraints.NONE, 			0.0, GridBagConstraints.NORTH);

		addGrid(panel, numbersPanel,	 0, 4, 2, GridBagConstraints.NONE, 			0.0, GridBagConstraints.CENTER);

		addGrid(panel, failureLabel, 	 0, 5, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.WEST);
		addGrid(panel, scrolledList, 	 0, 6, 2, GridBagConstraints.BOTH, 			1.0, GridBagConstraints.WEST);
		addGrid(panel, failedPanel, 	 2, 6, 1, GridBagConstraints.HORIZONTAL, 	0.0, GridBagConstraints.CENTER);
		
		addGrid(panel, fStatusLine, 	 0, 7, 2, GridBagConstraints.HORIZONTAL, 	1.0, GridBagConstraints.CENTER);
		addGrid(panel, fQuitButton, 	 2, 7, 1, GridBagConstraints.HORIZONTAL, 	0.0, GridBagConstraints.CENTER);
		
		frame.setContentPane(panel);
		frame.pack();
		frame.setLocation(200, 200);

		return frame;
	}

	public void endTest(Test test) {
		postEndTest(test);
	}

	private void postEndTest(final Test test) {
		synchUI();
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					if (fTestResult != null) {
						setLabelValue(fNumberOfRuns, fTestResult.runCount());
						fProgressIndicator.step(fTestResult.wasSuccessful());
					}
				}
			}
		);
	}

	protected String getSuiteText() {
		if (fSuiteCombo == null)
			return "";
		JTextField field= (JTextField)fSuiteCombo.getEditor().getEditorComponent();
		return field.getText();
	}

	/**
	 * Loads the named test suite and returns it. Errors during loading
	 * are reported on the status line.
	 */
	protected Test getTest(String suiteClassName) {
		if (suiteClassName.length() <= 0) {
			fStatusLine.setText("");
			return null;
		}
		
		Class testClass= null;
		try {
			testClass= loadSuiteClass(suiteClassName);
		} catch(Exception e) {
			runFailed("Class \""+suiteClassName+"\" not found");
			return null;
		}
			
		Method suiteMethod= null;
		try {
			suiteMethod= testClass.getMethod(SUITE_METHODNAME, new Class[0]);
	 	} catch(Exception e) {
	 		// try to extract a test suite automatically
			fStatusLine.setText("");			
			return new TestSuite(testClass);
		}
	
		Test test= null;
		try {
			test= (Test)suiteMethod.invoke(null, new Class[0]); // static method
			if (test == null)
				return test;
		} catch(Exception e) {
			runFailed("Could not invoke the suite() method");
			return null;
		}
		fStatusLine.setText("");
		return test;
	}
	
	public void insertUpdate(DocumentEvent event) {
		textChanged();
	}
	
	private boolean isErrorSelected() {
		return fFailureList.getSelectedIndex() != -1;
	}
	
	private Image loadFrameIcon() {
		Toolkit toolkit= Toolkit.getDefaultToolkit();
		try {
			java.net.URL url= getClass().getResource("smalllogo.gif");
			return toolkit.createImage((ImageProducer) url.getContent());
		} catch (Exception ex) {
		}
		return null;
	}
	
	private void loadHistory(JComboBox combo) throws IOException {
		BufferedReader br= new BufferedReader(new FileReader(getSettingsFile()));
		int itemCount= 0;
		try {
			String line;
			while ((line= br.readLine()) != null) {
				combo.addItem(line);
				itemCount++;
			}
			if (itemCount > 0)
				combo.setSelectedIndex(0);

		} finally {
			br.close();
		}
	}
	
	private File getSettingsFile() {
	 	String home= System.getProperty("user.home");
	 	Assert.assertNotNull(home); // spec says, this must exist
		if (System.getProperty("os.name").indexOf("Windows") != -1 )
 			return new File(home, "junit.ini");
 		return new File(home,".junit");
 	}

	protected Class loadSuiteClass(String suiteClassName) throws ClassNotFoundException {
		return fTestLoader.load(suiteClassName);
	}
	
	/**
	 * main entrypoint
	 */
	public static void main(String[] args) {
		try {
			// to play with different look and feels...
			//UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.out.println("Couldn't install system look and feel");
		}
		new TestRunner().start(args, new StandardTestSuiteLoader());
	}
	 
	private void postInfo(final String message) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					showInfo(message);
				}
			}
		);
	}
	
	private void postStatus(final String status) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					showStatus(status);
				}
			}
		);
	}
	
	public void removeUpdate(DocumentEvent event) {
		textChanged();
	}
	
	private void rerun() {
		int index= fFailureList.getSelectedIndex();
		if (index == -1)
			return;
	
		TestFailure failure= (TestFailure)fFailures.elementAt(index);
		Test test= failure.failedTest();
		if (!(test instanceof TestCase)) {
			showInfo("Could not reload "+ test.toString());
			return;
		}
		Test reloadedTest= null;
		try {
			Class reloadedTestClass= fTestLoader.reload(test.getClass());
			Class[] classArgs= { String.class };
			Object[] args= new Object[]{((TestCase)test).name()};
			Constructor constructor= reloadedTestClass.getConstructor(classArgs);
			reloadedTest=(Test)constructor.newInstance(args);
		} catch(Exception e) {
			showInfo("Could not reload "+ test.toString());
			return;
		}
		TestResult result= new TestResult();
		reloadedTest.run(result);
		
		String message= reloadedTest.toString();
		if(result.wasSuccessful())
			showInfo(message+" was successful");
		else if (result.errorCount() == 1)
			showStatus(message+" had an error");
		else
			showStatus(message+" had a failure");
	}
	
	protected void reset() {
		setLabelValue(fNumberOfErrors, 0);
		setLabelValue(fNumberOfFailures, 0);
		setLabelValue(fNumberOfRuns, 0);
		fProgressIndicator.reset();
		fShowErrorButton.setEnabled(false);
		fRerunButton.setEnabled(false);
		fFailures.clear();
	}
	
	/**
	 * runs a suite.
	 * @deprecated use runSuite() instead
	 */
	public void run() {
		runSuite();
	}
	
	private void runFailed(String message) {
		showStatus(message);
		fRun.setText("Run");
		fRunner= null;
	}
	
	synchronized public void runSuite() {
		if (fRunner != null) {
			fTestResult.stop();
		} else {
			reset();
			showInfo("Load Test Case...");
			final String suiteName= getSuiteText();
			final Test testSuite= getTest(suiteName);		
			if (testSuite != null) {
				addToHistory(suiteName);
				doRunTest(testSuite, true);
			}
		}
	}
	
	synchronized protected void runTest(final Test testSuite) {
		if (fRunner != null) {
			fTestResult.stop();
		} else {
			reset();	
			if (testSuite != null) 
				doRunTest(testSuite, false);
		}
	}
	
	private void doRunTest(final Test testSuite, final boolean reload) {
		setButtonLabel(fRun, "Stop");
		fRunner= new Thread() {
			public void run() {
				fTestResult= createTestResult();
				fTestResult.addListener(TestRunner.this);
				connectTestBrowser(testSuite, reload);
				TestRunner.this.start(testSuite); 
				postInfo("Running...");
				
				long startTime= System.currentTimeMillis();
				testSuite.run(fTestResult);
					
				if (fTestResult.shouldStop()) {
					postStatus("Stopped");
				} else {
					long endTime= System.currentTimeMillis();
					long runTime= endTime-startTime;
					postInfo("Finished: " + StringUtil.elapsedTimeAsString(runTime) + " seconds");
				}
				setButtonLabel(fRun, "Run");
				fRunner= null;
			}
		};
		fRunner.start();
	}

	private void saveHistory() throws IOException {
		BufferedWriter bw= new BufferedWriter(new FileWriter(getSettingsFile()));
		try {
			for (int i= 0; i < fSuiteCombo.getItemCount(); i++) {
				String testsuite= fSuiteCombo.getItemAt(i).toString();
				bw.write(testsuite, 0, testsuite.length());
				bw.newLine();
			}
		} finally {
			bw.close();
		}
	}
	
	private void setButtonLabel(final JButton button, final String label) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					button.setText(label);
				}
			}
		);
	}
	
	private void setLabelValue(final JTextField label, final int value) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					label.setText(Integer.toString(value));
				}
			}
		);
	}
	
	public void setSuiteName(String suite) {
		fSuiteCombo.addItem(suite);
		fSuiteCombo.setSelectedItem(suite);
	}
	
	private void showErrorTrace() {
		int index= fFailureList.getSelectedIndex();
		if (index == -1)
			return;

		TestFailure failure= (TestFailure) fFailures.elementAt(index);
		Throwable t= failure.thrownException();
		if (fTraceFrame == null) {
			fTraceFrame= new TraceFrame();
			fTraceFrame.setLocation(100, 100);
	   	}
		fTraceFrame.showTrace(t);
		fTraceFrame.setVisible(true);
	}
	
	private void showInfo(final String message) {
		fStatusLine.setFont(PLAIN_FONT);
		fStatusLine.setForeground(Color.black);
		fStatusLine.setText(message);
	}
	
	private void showStatus(String status) {
		fStatusLine.setFont(BOLD_FONT);
		fStatusLine.setForeground(Color.red);
		fStatusLine.setText(status);
	}
	
	private void showTestBrowser() {
		String suiteName= getSuiteText();
		final Test testSuite= getTest(suiteName);
		if (testSuite == null)
			return;
		if (fTestBrowser == null) {
			fTestBrowser= new TestBrowser(this);
			fTestBrowser.setLocation(350, 100);
	   	}
		fTestBrowser.showTestTree(testSuite);
		fTestBrowser.setVisible(true);
	}
	
	/**
	 * Starts the TestRunner
	 */
	public void start(String[] args, TestSuiteLoader loader) {
		fTestLoader= loader;
		
		String suiteName= null;
		if (args.length == 1) 
			suiteName= args[0];
		else if (args.length == 2 && args[0].equals("-c")) 
			suiteName= StringUtil.extractClassName(args[1]);

		fFrame= createUI(suiteName);
		//fFrame.pack(); 
		fFrame.setVisible(true);

		if (suiteName != null) {
			setSuiteName(suiteName);
			runSuite();
		}
	}
	
	private void start(final Test test) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					fProgressIndicator.start(test.countTestCases());
				}
			}
		);
	}
	
	public void startTest(Test test) {
		postInfo("Running: "+test);
	}
	
	/**
	 * Wait until all the events are processed in the event thread
	 */
	private void synchUI() {
		try {
			SwingUtilities.invokeAndWait(
				new Runnable() {
					public void run() {}
				}		
			);
		}
		catch (Exception e) {
		}
	}
	
	/**
	 * Terminates the TestRunner
	 */
	public void terminate() {
		fFrame.dispose();
		try {
			saveHistory();
		} catch (IOException e) {
			System.out.println("Couldn't save test run history");
		}
		System.exit(0);
	}
	
	public void textChanged() {
		fRun.setEnabled(getSuiteText().length() > 0);
		fStatusLine.setText("");
	}
}