package eu.stamp_project.testrunner;

import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoveragePerTestMethod;
import eu.stamp_project.testrunner.listener.TestResult;
import eu.stamp_project.testrunner.runner.Failure;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 19/12/17
 */
public class EntryPointTest extends AbstractTest {

    @Before
    public void setUp() {
        EntryPoint.persistence = true;
        EntryPoint.outPrintStream = null;
        EntryPoint.errPrintStream = null;
        EntryPoint.verbose = true;
    }

    @After
    public void tearDown() {
        EntryPoint.blackList.clear();
    }

    @Test
    public void testWithBlackList() throws Exception {
        /*
            EntryPoint should not execute blacklisted test method
         */

        EntryPoint.blackList.add("testFailing");

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "failing.FailingTestClass"
        );

        assertEquals(2, testResult.getRunningTests().size());
        assertEquals(1, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());
        assertEquals(1, testResult.getAssumptionFailingTests().size());
        assertEquals(1, testResult.getIgnoredTests().size());
    }


    // TODO FIXME: This test seems to be flaky. Something happens with the stream of outputs
    @Test
    public void testJVMArgsAndCustomPrintStream() throws Exception {

        /*
            Test the method runTest() of EntryPoint using JVMArgs.
                This test verifies the non-persistence of the JVMArgs.
                This test verifies the usage of JVMArgs.
         */

        /* setup the out stream */
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream outPrint = new PrintStream(outStream);
        EntryPoint.outPrintStream = outPrint;
        /* setup the err stream */
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream errPrint = new PrintStream(errStream);
        EntryPoint.errPrintStream = errPrint;

        /* persistence disabled, in order to clean after the execution */
        EntryPoint.persistence = false;

        EntryPoint.JVMArgs = "-XX:+PrintGCDetails";
        assertNotNull(EntryPoint.JVMArgs);

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "example.TestSuiteExample"
        );

        final String GCdetail = outStream.toString();
        assertTrue(errStream.toString().isEmpty()); // no error occurs
//        assertTrue(GCdetail + " should contain GC detail, e.g. the word \"Heap\".", GCdetail.contains("Heap")); // it print the GC Details TODO FIXME

        assertEquals(7, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());
    }

    @Test
    public void testPersistence() throws Exception {

        /*
            test the persistence boolean.
            First run, (default behavior), the persistence is enabled
            Second run, the persistence is set to false
         */

        EntryPoint.JVMArgs = "-XX:+PrintGCDetails";
        assertNotNull(EntryPoint.JVMArgs);

        TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "example.TestSuiteExample"
        );

        assertNotNull(EntryPoint.JVMArgs);
        assertEquals(7, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());

        EntryPoint.persistence = false;
        EntryPoint.JVMArgs = "-XX:+PrintGCDetails";
        assertNotNull(EntryPoint.JVMArgs);

        testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "example.TestSuiteExample"
        );

        assertNull(EntryPoint.JVMArgs);
        assertEquals(7, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());
    }

    @Test
    public void testOnEasyMockTests() throws Exception {

        /*
            Test to run test class that use easymock framework
         */

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + EASYMOCK_CP +
                        ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "easymock.LoginControllerIntegrationTest"
        );
        assertEquals(7, testResult.getRunningTests().size());
        assertEquals(7, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());
    }

    @Test
    public void testOnFailingTest() throws Exception {
        /*
            EntryPoint should return a proper testResult.
            This testResult contains:
                - three running test
                - one failing test
                - one passing test
                - one assumption failing test
                - one ignored test
         */

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "failing.FailingTestClass"
        );

        assertEquals(3, testResult.getRunningTests().size());
        assertEquals(1, testResult.getPassingTests().size());
        assertEquals(1, testResult.getFailingTests().size());
        assertEquals(1, testResult.getAssumptionFailingTests().size());
        assertEquals(1, testResult.getIgnoredTests().size());

        final Failure testFailing = testResult.getFailureOf("testFailing");
        assertEquals("testFailing", testFailing.testCaseName);

        try {
            testResult.getFailureOf("testPassing");
            fail("Should have throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(true); // expected
        }
    }

    @Test
    public void testTimeOut() {
        EntryPoint.timeoutInMs = 1;
        try {
            EntryPoint.runTests(
                    JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                    new String[]{"example.TestSuiteExample", "example.TestSuiteExample2"}
            );
            fail("Should have thrown a Time out Exception");
        } catch (Exception e) {
            assertTrue(true); // success!
        }
        EntryPoint.timeoutInMs = 10000;
    }

    @Test
    public void testRunTestClasses() throws Exception {

        /*
            Test the method runTestClasses() of EntryPoint.
                It should return the testResult with the result of the execution of the list of test classes.
         */

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                new String[]{"example.TestSuiteExample", "example.TestSuiteExample2"}
        );
        assertEquals(13, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());
    }

    @Test
    public void testRunTestTestClass() throws Exception {

        /*
            Test the method runTest() of EntryPoint.
                It should return the testResult with the result of the execution of the test class.
         */

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "example.TestSuiteExample"
        );
        assertEquals(7, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());
    }

    @Ignore
    @Test //TODO FIXME FLAKY
    public void testRunTestTestMethods() throws Exception {

        /*
            Test the method runTest() of EntryPoint.
                It should return the testResult with the result of the execution of the test class.
         */

        EntryPoint.verbose = true;

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "example.TestSuiteExample",
                new String[]{"test4", "test9"}
        );
        assertEquals(2, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());

        EntryPoint.verbose = false;
    }

    @Test
    public void testRunCoverage() throws Exception {

        /*
            Test the runCoverage() of EntryPoint.
                It should return the CoverageResult with the instruction jUnit4Coverage computed by Jacoco.
         */
        final String classpath = MAVEN_HOME + "org/jacoco/org.jacoco.core/0.7.9/org.jacoco.core-0.7.9.jar" + ConstantsHelper.PATH_SEPARATOR +
                MAVEN_HOME + "org/ow2/asm/asm-debug-all/5.2/asm-debug-all-5.2.jar" + ConstantsHelper.PATH_SEPARATOR +
                MAVEN_HOME + "commons-io/commons-io/2.5/commons-io-2.5.jar" + ConstantsHelper.PATH_SEPARATOR +
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + JUNIT5_CP;
        ;

        final Coverage coverage = EntryPoint.runCoverage(
                classpath + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                TEST_PROJECT_CLASSES,
                "example.TestSuiteExample",
                new String[]{"test8", "test3"}
        );
        assertEquals(23, coverage.getInstructionsCovered());
        assertEquals(107, coverage.getInstructionsTotal());
    }

    @Test
    public void testRunGlobalCoverage() throws Exception {

        /*
            Test the runCoverage() of EntryPoint.
                It should return the CoverageResult with the instruction coverage computed by Jacoco.
         */
        final String classpath = MAVEN_HOME + "org/jacoco/org.jacoco.core/0.7.9/org.jacoco.core-0.7.9.jar" + ConstantsHelper.PATH_SEPARATOR +
                MAVEN_HOME + "org/ow2/asm/asm-debug-all/5.2/asm-debug-all-5.2.jar" + ConstantsHelper.PATH_SEPARATOR +
                MAVEN_HOME + "commons-io/commons-io/2.5/commons-io-2.5.jar" + ConstantsHelper.PATH_SEPARATOR +
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + JUNIT5_CP;

        final Coverage coverage = EntryPoint.runCoverage(
                classpath + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                TEST_PROJECT_CLASSES,
                "example.TestSuiteExample"
        );

        assertEquals(30, coverage.getInstructionsCovered());
        assertEquals(107, coverage.getInstructionsTotal());
    }

    @Test
    public void testRunGlobalCoverageWithBlackList() throws Exception {

        /*
            Test the runCoverage() of EntryPoint with blacklisted test methods.
         */
        final String classpath = MAVEN_HOME + "org/jacoco/org.jacoco.core/0.7.9/org.jacoco.core-0.7.9.jar" + ConstantsHelper.PATH_SEPARATOR +
                MAVEN_HOME + "org/ow2/asm/asm-debug-all/5.2/asm-debug-all-5.2.jar" + ConstantsHelper.PATH_SEPARATOR +
                MAVEN_HOME + "commons-io/commons-io/2.5/commons-io-2.5.jar" + ConstantsHelper.PATH_SEPARATOR +
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + JUNIT5_CP;
        ;

        EntryPoint.blackList.add("test8");
        EntryPoint.blackList.add("test6");
        EntryPoint.blackList.add("test3");
        EntryPoint.blackList.add("test4");
        EntryPoint.blackList.add("test7");
        EntryPoint.blackList.add("test9");

        final Coverage coverage = EntryPoint.runCoverage(
                classpath + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                TEST_PROJECT_CLASSES,
                "example.TestSuiteExample"
        );

        assertEquals(23, coverage.getInstructionsCovered());
        assertEquals(107, coverage.getInstructionsTotal());
    }

    @Test
    public void testRunCoveragePerTestMethods() throws Exception {

        /*
            Test the runCoveragePerTestMethods() of EntryPoint.
                It should return the CoverageResult with the instruction coverage computed by Jacoco.
         */
        final String classpath = MAVEN_HOME + "org/jacoco/org.jacoco.core/0.7.9/org.jacoco.core-0.7.9.jar" + ConstantsHelper.PATH_SEPARATOR +
                MAVEN_HOME + "org/ow2/asm/asm-debug-all/5.2/asm-debug-all-5.2.jar" + ConstantsHelper.PATH_SEPARATOR +
                MAVEN_HOME + "commons-io/commons-io/2.5/commons-io-2.5.jar" + ConstantsHelper.PATH_SEPARATOR +
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + JUNIT5_CP;

        final CoveragePerTestMethod coveragePerTestMethod = EntryPoint.runCoveragePerTestMethods(
                classpath + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                TEST_PROJECT_CLASSES,
                "example.TestSuiteExample",
                new String[]{"test8", "test3"}
        );

        assertEquals(23, coveragePerTestMethod.getCoverageOf("test3").getInstructionsCovered());
        assertEquals(107, coveragePerTestMethod.getCoverageOf("test3").getInstructionsTotal());
        assertEquals(23, coveragePerTestMethod.getCoverageOf("test8").getInstructionsCovered());
        assertEquals(107, coveragePerTestMethod.getCoverageOf("test8").getInstructionsTotal());
    }

    @Test
    public void testOnParametrized() throws TimeoutException {

        /*
            Test the execution of Parametrized test class
         */

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "example.ParametrizedTestSuiteExample"
        );
        System.out.println(testResult);
        assertEquals(10, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());
    }

    @Test
    public void testOnParametrizedForOneTestMethod() throws TimeoutException {

        /*
            Test the execution of Parametrized test
         */

        final TestResult testResult = EntryPoint.runTests(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES,
                "example.ParametrizedTestSuiteExample",
                "test3"
        );
        assertEquals(2, testResult.getPassingTests().size());
        assertEquals(0, testResult.getFailingTests().size());
    }

    @Test
    public void testOnParametrizedForOneTestMethodCoveragePerTestMethod() throws TimeoutException {

        /*
            Test the execution of Parametrized test
         */

        EntryPoint.runCoveragePerTestMethods(
                JUNIT_CP + ConstantsHelper.PATH_SEPARATOR + TEST_PROJECT_CLASSES
                + ConstantsHelper.PATH_SEPARATOR + JUNIT5_CP,
                TEST_PROJECT_CLASSES,
                "example.ParametrizedTestSuiteExample",
                "test3:test4:test7"
        );
    }
}
