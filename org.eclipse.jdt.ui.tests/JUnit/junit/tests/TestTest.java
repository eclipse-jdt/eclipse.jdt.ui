package junit.tests;

import java.util.Vector;
import junit.framework.*;
import junit.util.StringUtil;

/**
 * A test case testing the testing framework.
 *
 */
public class TestTest extends TestCase {
	
	static class TornDown extends TestCase {
		boolean fTornDown= false;
		
		TornDown(String name) {
			super(name);
		}
		protected void tearDown() {
			fTornDown= true;
		}
		protected void runTest() {
			throw new Error();
		}
	}

	public TestTest(String name) {
		super(name);
	}
	public void testAssertEquals() {
		Object o= new Object();
		assertEquals(o, o);
	}
	public void testAssertEqualsNull() {
		assertEquals(null, null);
	}
	public void testAssertNull() {
		TestCase succeeds= new TestCase("isnull") {
			protected void runTest() {
				assertNull(null);
			}
		};
		verifySuccess(succeeds);
	}
	public void testAssertNullNotEqualsNull() {
		TestCase fails= new TestCase("fails") {
			protected void runTest() {
				assertEquals(null, new Object());
			}
		};
		verifyFailure(fails);
	}
	public void testAssertSame() {
		Object o= new Object();
		assertSame(o, o);
	}
	public void testAssertSameFails() {
		TestCase assertSame= new TestCase("assertSame") {
			protected void runTest() {
				assertSame(new Integer(1), new Integer(1));
			}
		};
		verifyFailure(assertSame);
	}
	public void testCaseToString() {
		// This test wins the award for twisted snake tail eating while
		// writing self tests. And you thought those weird anonymous
		// inner classes were bad...
		
		assertEquals("testCaseToString(junit.tests.TestTest)", toString());
	}
	public void testError() {
		TestCase error= new TestCase("error") {
			protected void runTest() {
				throw new Error();
			}
		};
		verifyError(error);
	}
	public void testFail() {
		TestCase failure= new TestCase("fail") {
			protected void runTest() {
				fail();
			}
		};
		verifyFailure(failure);
	}
	public void testFailAssertNotNull() {
		TestCase failure= new TestCase("fails") {
			protected void runTest() {
				assertNotNull(null);
			}
		};
		verifyFailure(failure);
	}
	public void testFailure() {
		TestCase failure= new TestCase("failure") {
			protected void runTest() {
				assert(false);
			}
		};
		verifyFailure(failure);
	}
	public void testFailureException() {
		try {
			fail();
		} 
		catch (AssertionFailedError e) {
			return;
		}
		fail();
		
	}
	public void testRunAndTearDownFails() {
		TornDown fails= new TornDown("fails") {
			protected void tearDown() {
				super.tearDown();
				throw new Error();
			}
			protected void runTest() {
				throw new Error();
			}
		};
		verifyError(fails);
		assert(fails.fTornDown);
	}
	public void testRunnerPrinting() {
		assertEquals("1.05", StringUtil.elapsedTimeAsString(1050));
	}
	public void testSetupFails() {
		TestCase fails= new TestCase("success") {
			protected void setUp() {
				throw new Error();
			}
			protected void runTest() {
			}
		};
		verifyError(fails);
	}
	public void testSucceedAssertNotNull() {
		assertNotNull(new Object());
	}
	public void testSuccess() {
		TestCase success= new TestCase("success") {
			protected void runTest() {
				assert(true);
			}
		};
		verifySuccess(success);
	}
	public void testTearDownAfterError() {

		TornDown fails= new TornDown("fails");
		verifyError(fails);
		assert(fails.fTornDown);
	}
	public void testTearDownFails() {
		TestCase fails= new TestCase("success") {
			protected void tearDown() {
				throw new Error();
			}
			protected void runTest() {
			}
		};
		verifyError(fails);
	}
	public void testTearDownSetupFails() {
		TornDown fails= new TornDown("fails") {
			protected void setUp() {
				throw new Error();
			}
		};
		verifyError(fails);
		assert(!fails.fTornDown);
	}
	public void testWasNotSuccessful() {
		TestCase failure= new TestCase("fail") {
			protected void runTest() {
				fail();
			}
		};
		TestResult result= failure.run();
		assert(result.runCount() == 1);
		assert(result.failureCount() == 1);
		assert(result.errorCount() == 0);
		assert(! result.wasSuccessful());
	}
	public void testWasRun() {
		WasRun test= new WasRun("");
		test.run();
		assert(test.fWasRun);
	}
	public void testWasSuccessful() {
		TestCase success= new TestCase("success") {
			protected void runTest() {
				assert(true);
			}
		};
		TestResult result= success.run();
		assert(result.runCount() == 1);
		assert(result.failureCount() == 0);
		assert(result.errorCount() == 0);
		assert(result.wasSuccessful());
	}
	private void verifyError(TestCase test) {
		TestResult result= test.run();
		assert(result.runCount() == 1);
		assert(result.failureCount() == 0);
		assert(result.errorCount() == 1);
	}
	private void verifyFailure(TestCase test) {
		TestResult result= test.run();
		assert(result.runCount() == 1);
		assert(result.failureCount() == 1);
		assert(result.errorCount() == 0);
	}
	private void verifySuccess(TestCase test) {
		TestResult result= test.run();
		assert(result.runCount() == 1);
		assert(result.failureCount() == 0);
		assert(result.errorCount() == 0);
	}
}