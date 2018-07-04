/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.tests.callgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.ThreadNode;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the CallGraphAnalysis.This creates a virtual state system in each test
 * and tests the segment store returned by the CallGraphAnalysis.
 *
 * @author Sonia Farrah
 *
 */
public class CallGraphAnalysisTest {

    private static final @NonNull String PROCESS_PATH = "Processes";
    private static final @NonNull String THREAD_PATH = "Thread";
    private static final @NonNull String CALLSTACK_PATH = "CallStack";
    private static final String QUARK_0 = "0";
    private static final String QUARK_1 = "1";
    private static final String QUARK_2 = "2";
    private static final Integer SMALL_AMOUNT_OF_SEGMENT = 3;
    private static final int LARGE_AMOUNT_OF_SEGMENTS = 1000;
    private static final String @NonNull [] CSP = { CALLSTACK_PATH };
    private static final String @NonNull [] PP = { PROCESS_PATH };
    private static final String @NonNull [] TP = { THREAD_PATH };

    private static final Object NULL_STATE_VALUE = null;

    /**
     * This class is used to make the CallGraphAnalysis's method
     * iterateOverStateSystem() visible to test
     */
    private class CGAnalysis extends CallGraphAnalysis {

        @Override
        protected boolean iterateOverStateSystem(ITmfStateSystem ss, String[] threadsPattern, String[] processesPattern, String[] callStackPath, IProgressMonitor monitor) {
            return super.iterateOverStateSystem(ss, threadsPattern, processesPattern, callStackPath, monitor);
        }

        @Override
        public @NonNull Iterable<@NonNull ISegmentAspect> getSegmentAspects() {
            return Collections.EMPTY_LIST;
        }

    }

    private static @NonNull ITmfStateSystemBuilder createFixture() {
        IStateHistoryBackend backend = StateHistoryBackendFactory.createInMemoryBackend("Test", 0L);
        return StateSystemFactory.newStateSystem(backend);
    }

    /**
     * Test cascade state system. The call stack's structure used in this test
     * is shown below:
     *
     * <pre>
     *  ________
     *   ______
     *    ____
     *
     * </pre>
     */
    @Test
    public void CascadeTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        // Build the state system
        long start = 1;
        long end = 1001;
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(PROCESS_PATH, THREAD_PATH, CALLSTACK_PATH);
        for (int i = 1; i <= SMALL_AMOUNT_OF_SEGMENT; i++) {
            int quark = fixture.getQuarkRelativeAndAdd(parentQuark, Integer.toString(i));
            fixture.modifyAttribute(start, NULL_STATE_VALUE, quark);
            fixture.modifyAttribute(start + i, i, quark);
            fixture.modifyAttribute(end - i, NULL_STATE_VALUE, quark);
        }

        fixture.closeHistory(1002);
        // Execute the CallGraphAnalysis
        CGAnalysis cga = new CGAnalysis();
        assertTrue(cga.iterateOverStateSystem(fixture, TP, PP, CSP, new NullProgressMonitor()));
        ISegmentStore<@NonNull ISegment> segmentStore = cga.getSegmentStore();
        // Test the segment store generated by the analysis
        assertNotNull(segmentStore);
        Object[] segments = segmentStore.toArray();
        assertEquals("Number of segments Found", 3, segments.length);
        ICalledFunction f1 = (ICalledFunction) segments[0];
        ICalledFunction f2 = (ICalledFunction) segments[1];
        ICalledFunction f3 = (ICalledFunction) segments[2];
        assertEquals("Test the parenthood", NonNullUtils.checkNotNull(f2.getParent()).getSymbol(), f1.getSymbol());
        assertEquals("Children number:First parent", 1, f1.getChildren().size());
        assertEquals("Children number:Second parent", 1, f2.getChildren().size());
        assertTrue("Children number:Second parent", f3.getChildren().isEmpty());
        assertTrue("Children number:Child(leaf)", ((ICalledFunction) segments[2]).getChildren().isEmpty());
        assertEquals("Parent's self time", 2, f1.getSelfTime());
        assertEquals("Child's self time", 2, f2.getSelfTime());
        assertEquals("The leaf's self time", 994, f3.getSelfTime());
        assertEquals("Test first function's duration", 998, f1.getLength());
        assertEquals("Test second function's duration", 996, f2.getLength());
        assertEquals("Test third function's duration", 994, f3.getLength());
        assertEquals("Depth:First parent", 0, f1.getDepth());
        assertEquals("Depth:Second parent", 1, f2.getDepth());
        assertEquals("Depth:Last child", 2, f3.getDepth());
        cga.dispose();
    }

    /**
     * Build a pyramid shaped call stack.This call stack contains three
     * functions ,Its structure is shown below :
     *
     * <pre>
     *    __
     *   ____
     *  ______
     * </pre>
     */
    private static void buildPyramidCallStack(ITmfStateSystemBuilder fixture) {
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(PROCESS_PATH, THREAD_PATH, CALLSTACK_PATH);
        // Create the first function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        fixture.modifyAttribute(0, NULL_STATE_VALUE, quark);
        fixture.modifyAttribute(10, 0, quark);
        fixture.modifyAttribute(20, NULL_STATE_VALUE, quark);
        // Create the second function
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        fixture.modifyAttribute(0, NULL_STATE_VALUE, quark);
        fixture.modifyAttribute(5, 1, quark);
        fixture.modifyAttribute(25, NULL_STATE_VALUE, quark);
        // Create the third function
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_2);
        fixture.modifyAttribute(0, 2, quark);
        fixture.modifyAttribute(30, NULL_STATE_VALUE, quark);

        fixture.closeHistory(31);
    }

    /**
     * Test pyramid state system. The call stack's structure used in this test
     * is shown below:
     *
     * <pre>
     *    __
     *   ____
     *  ______
     * </pre>
     */
    @Test
    public void PyramidTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        buildPyramidCallStack(fixture);
        // Execute the callGraphAnalysis
        CGAnalysis cga = new CGAnalysis();
        assertTrue(cga.iterateOverStateSystem(fixture, TP, PP, CSP, new NullProgressMonitor()));
        ISegmentStore<@NonNull ISegment> segmentStore = cga.getSegmentStore();
        assertNotNull(segmentStore);
        Object[] segments = segmentStore.toArray();
        ICalledFunction f1 = (ICalledFunction) segments[0];
        assertEquals("Number of segments Found", 1, segments.length);
        assertEquals("Callees number", 0, f1.getChildren().size());
        assertEquals("Function's self time", 10, f1.getSelfTime());
        assertEquals("Compare the function's self time and total time", f1.getLength(), f1.getSelfTime());
        assertEquals("Function's depth", 0, f1.getDepth());
        cga.dispose();
    }

    /**
     * Test mutliRoots state system. The call stack's structure used in this
     * test is shown below:
     *
     * <pre>
     * ___ ___
     *  _   _
     * </pre>
     */
    @Test
    public void multiFunctionRootsTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(PROCESS_PATH, THREAD_PATH, CALLSTACK_PATH);
        // Create the first root function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        fixture.modifyAttribute(0, 0, quark);
        fixture.modifyAttribute(20, NULL_STATE_VALUE, quark);
        // Create the second root function
        fixture.modifyAttribute(30, 1, quark);
        fixture.modifyAttribute(50, NULL_STATE_VALUE, quark);
        // Create the first root function's callee
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        fixture.modifyAttribute(0, 2, quark);
        fixture.modifyAttribute(10, NULL_STATE_VALUE, quark);
        // Create the second root function's callee
        fixture.modifyAttribute(30, 3, quark);
        fixture.modifyAttribute(40, NULL_STATE_VALUE, quark);
        fixture.closeHistory(51);

        // Execute the callGraphAnalysis
        CGAnalysis cga = new CGAnalysis();
        assertTrue(cga.iterateOverStateSystem(fixture, TP, PP, CSP, new NullProgressMonitor()));
        ISegmentStore<@NonNull ISegment> segmentStore = cga.getSegmentStore();
        // Test the segment store
        assertNotNull(segmentStore);
        Object[] segments = segmentStore.toArray();
        List<@NonNull ICalledFunction> threads = cga.getRootFunctions();
        assertEquals("Number of root functions", 2, threads.size());
        assertEquals("Number of children: first root function", 1, threads.get(0).getChildren().size());
        assertEquals("Number of children: first root function", 1, threads.get(1).getChildren().size());
        ICalledFunction firstChild = threads.get(0).getChildren().get(0);
        ICalledFunction secondChild = threads.get(1).getChildren().get(0);
        assertEquals("Number of segments found", 4, segments.length);
        assertNotNull(firstChild.getParent());
        assertNotNull(secondChild.getParent());

        assertEquals("Test of parenthood", NonNullUtils.checkNotNull(firstChild.getParent()).getSymbol(), threads.get(0).getSymbol());
        assertEquals("Test of parenthood", NonNullUtils.checkNotNull(secondChild.getParent()).getSymbol(), threads.get(1).getSymbol());
        cga.dispose();
    }

    /**
     * Test state system with a Large amount of segments. All segments have the
     * same length. The call stack's structure used in this test is shown below:
     *
     * <pre>
     * _____
     * _____
     * _____
     * .....
     * </pre>
     */
    @Test
    public void LargeTest() {
        // Build the state system
        ITmfStateSystemBuilder fixture = createFixture();
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(PROCESS_PATH, THREAD_PATH, CALLSTACK_PATH);
        for (int i = 0; i < LARGE_AMOUNT_OF_SEGMENTS; i++) {
            int quark = fixture.getQuarkRelativeAndAdd(parentQuark, Integer.toString(i));
            fixture.pushAttribute(0, i, quark);
        }
        for (int i = 0; i < LARGE_AMOUNT_OF_SEGMENTS; i++) {
            int quark = fixture.getQuarkRelativeAndAdd(parentQuark, Integer.toString(i));
            fixture.popAttribute(10, quark);
        }
        fixture.closeHistory(11);
        // Execute the callGraphAnalysis
        CGAnalysis cga = new CGAnalysis();
        assertTrue(cga.iterateOverStateSystem(fixture, TP, PP, CSP, new NullProgressMonitor()));
        ISegmentStore<@NonNull ISegment> segmentStore = cga.getSegmentStore();
        // Test segment store
        assertNotNull(segmentStore);
        Object[] segments = segmentStore.toArray();
        assertEquals("Number of segments found", LARGE_AMOUNT_OF_SEGMENTS, segments.length);
        for (int i = 1; i < LARGE_AMOUNT_OF_SEGMENTS; i++) {
            assertEquals("Test parenthood", ((ICalledFunction) segments[i - 1]).getSymbol(), NonNullUtils.checkNotNull(((ICalledFunction) segments[i]).getParent()).getSymbol());
        }
        cga.dispose();
    }

    /**
     * Test an empty state system
     */
    @Test
    public void EmptyStateSystemTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        fixture.closeHistory(1002);
        CGAnalysis cga = new CGAnalysis();
        assertTrue(cga.iterateOverStateSystem(fixture, TP, PP, CSP, new NullProgressMonitor()));
        ISegmentStore<@NonNull ISegment> segmentStore = cga.getSegmentStore();
        assertNotNull(segmentStore);
        Object[] segments = segmentStore.toArray();
        assertEquals("Number of root functions", 0, segments.length);
        cga.dispose();
    }

    /**
     * Test a tree shaped call stack. The root function calls the same function
     * twice. The call stack's structure used in this test is shown below:
     *
     * <pre>
     *---------1----------
     * --2--  -3-  -2-
     *  -3-
     * </pre>
     */
    @Test
    public void treeTest() {
        // Build the state system
        ITmfStateSystemBuilder fixture = createFixture();
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(PROCESS_PATH, THREAD_PATH, CALLSTACK_PATH);
        // Create the root function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        fixture.modifyAttribute(0, 0, quark);
        fixture.modifyAttribute(100, NULL_STATE_VALUE, quark);
        // Create the first child
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        fixture.modifyAttribute(0, NULL_STATE_VALUE, quark);
        fixture.modifyAttribute(10, 1, quark);
        fixture.modifyAttribute(40, NULL_STATE_VALUE, quark);
        // Create the second child
        fixture.modifyAttribute(50, 2, quark);
        fixture.modifyAttribute(70, NULL_STATE_VALUE, quark);
        // Create the third child
        fixture.modifyAttribute(80, 1, quark);
        fixture.modifyAttribute(100, NULL_STATE_VALUE, quark);
        // Create the leaf
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_2);
        fixture.modifyAttribute(0, NULL_STATE_VALUE, quark);
        fixture.modifyAttribute(15, 3, quark);
        fixture.modifyAttribute(20, NULL_STATE_VALUE, quark);
        fixture.closeHistory(102);

        // Execute the callGraphAnalysis
        CGAnalysis cga = new CGAnalysis();
        assertTrue(cga.iterateOverStateSystem(fixture, TP, PP, CSP, new NullProgressMonitor()));
        ISegmentStore<@NonNull ISegment> segmentStore = cga.getSegmentStore();
        // Test segment store
        assertNotNull(segmentStore);
        Object[] segments = segmentStore.toArray();
        assertEquals("Number of segments found", 5, segments.length);
        List<@NonNull ICalledFunction> rootFunctions = cga.getRootFunctions();
        assertEquals("Test the number of root functions found", 1, rootFunctions.size());
        ICalledFunction rootFunction = rootFunctions.get(0);

        // Test the segments links
        assertEquals("Children number:First parent", 3, rootFunction.getChildren().size());
        List<@NonNull ICalledFunction> firstDepthFunctions = rootFunctions.get(0).getChildren();
        ICalledFunction firstChild = firstDepthFunctions.get(0);
        ICalledFunction secondChild = firstDepthFunctions.get(1);
        ICalledFunction thirdChild = firstDepthFunctions.get(2);
        assertEquals("Test parenthood: First child", rootFunction.getSymbol(), NonNullUtils.checkNotNull(firstChild.getParent()).getSymbol());
        assertEquals("Test parenthood: Second parent", rootFunction.getSymbol(), NonNullUtils.checkNotNull(secondChild.getParent()).getSymbol());
        assertEquals("Test parenthood: Third parent", rootFunction.getSymbol(), NonNullUtils.checkNotNull(thirdChild.getParent()).getSymbol());
        assertEquals("Children number: First child", 1, firstChild.getChildren().size());
        ICalledFunction leaf = firstChild.getChildren().get(0);
        assertEquals("Test parenthood: Third parent", firstChild.getSymbol(), NonNullUtils.checkNotNull(leaf.getParent()).getSymbol());
        assertTrue("Children number:leaf", leaf.getChildren().isEmpty());

        // Test the segments self time
        assertEquals("Parent's self time", 30, rootFunction.getSelfTime());
        assertEquals("First child's self time", 25, firstChild.getSelfTime());
        assertEquals("Second child's self time", 20, secondChild.getSelfTime());
        assertEquals("Third child's self time", 20, thirdChild.getSelfTime());
        assertEquals("Leaf's self time", 5, leaf.getSelfTime());
        // Test the segments duration
        assertEquals("Test first function's duration", 100, rootFunction.getLength());
        assertEquals("Test first child's duration", 30, firstChild.getLength());
        assertEquals("Test second child's duration", 20, secondChild.getLength());
        assertEquals("Test third child's duration", 20, thirdChild.getLength());
        assertEquals("Test leaf's duration", 5, leaf.getLength());

        // Test the segments Depth
        assertEquals("Depth: Parent", 0, rootFunction.getDepth());
        assertEquals("Depth: First child", 1, firstChild.getDepth());
        assertEquals("Depth: Second child", 1, secondChild.getDepth());
        assertEquals("Depth: Third child", 1, thirdChild.getDepth());
        assertEquals("Depth: Leaf", 2, leaf.getDepth());

        // Test if the first child and the third one have the same address
        assertEquals("Test the address of two functions", firstChild.getSymbol(), thirdChild.getSymbol());

        // test Flamegraph
        Collection<@NonNull ThreadNode> flameGraph = cga.getFlameGraph();
        assertNotNull("Test Flamegraph", flameGraph);
        assertFalse(flameGraph.isEmpty());
        ThreadNode flamegraphRoot = flameGraph.iterator().next();
        assertNotNull("Test Flamegraph root", flamegraphRoot);

        Collection<@NonNull ThreadNode> threadGraph = cga.getThreadNodes();
        assertNotNull("Test ThreadNodes", threadGraph);
        assertFalse(threadGraph.isEmpty());
        ThreadNode threadgraphRoot = threadGraph.iterator().next();
        localAssertEquals("Test Flamegraph root", threadgraphRoot, flamegraphRoot);

        cga.dispose();
    }

    private void localAssertEquals(String message, AggregatedCalledFunction aggregatedCalledFunction, AggregatedCalledFunction actualElem) {
        if (Objects.equals(aggregatedCalledFunction, actualElem)) {
            return;
        }
        assertNotNull(message, aggregatedCalledFunction);
        assertNotNull(message, actualElem);
        Assert.assertEquals(message, aggregatedCalledFunction.getDepth(), actualElem.getDepth());
        Assert.assertEquals(message, aggregatedCalledFunction.getDuration(), actualElem.getDuration());
        Assert.assertEquals(message, aggregatedCalledFunction.getMaxDepth(), actualElem.getMaxDepth());
        Assert.assertEquals(message, aggregatedCalledFunction.getMaxDepth(), actualElem.getMaxDepth());
        Assert.assertEquals(message, aggregatedCalledFunction.getSelfTime(), actualElem.getSelfTime());
        localAssertEquals(message, aggregatedCalledFunction.getChildren(), actualElem.getChildren());
    }

    private void localAssertEquals(String message, @NonNull Collection<@NonNull AggregatedCalledFunction> expected, @NonNull Collection<@NonNull AggregatedCalledFunction> actual) {
        Iterator<@NonNull AggregatedCalledFunction> expectedIter = expected.iterator();
        for (AggregatedCalledFunction actualElem : actual) {
            localAssertEquals(message, expectedIter.next(), actualElem);
        }
    }
}
