/**********************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jonathan Rajotte - Support of machine interface
 **********************************************************************/

package org.eclipse.tracecompass.lttng2.control.ui.tests.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IChannelInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IDomainInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IEventInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.ILoggerInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.ISessionInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.LogLevelType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceChannelOutputType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceDomainType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceEnablement;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceEventType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceJulLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceLog4jLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TracePythonLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceSessionState;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.SessionInfo;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.service.ILttngControlService;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.service.LTTngControlServiceMI;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.service.LttngVersion;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author ejorajo
 *
 */
public class LTTngControlServiceMiTest extends LTTngControlServiceTest {

    private static final String MI_TEST_STREAM = "LTTngServiceMiTest.cfg";

    private static final String SCEN_SESSION_WITH_SYSCALLS = "GetSessionWithSyscalls";
    private static final String SCEN_LIST_SESSION_2_7_COMPAT = "ListSession2.7Compat";
    private static final String SCEN_ENABLING_JUL_LOGGERS = "EnableJulLoggers";
    private static final String SCEN_ENABLING_LOG4J_LOGGERS = "EnableLog4jLoggers";
    private static final String SCEN_ENABLING_PYTHON_LOGGERS = "EnablePythonLoggers";

    @Override
    protected ILttngControlService getControlService() {
        try {
            return new LTTngControlServiceMI(getShell(), new LttngVersion("2.7.0"));
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public void testGetSessionNameGarbage() {
        try {
            fShell.setScenario(SCEN_GET_SESSION_GARBAGE_OUT);
            fService.getSessionNames(new NullProgressMonitor());
        } catch (ExecutionException e) {
            // Success. Parsing of garbage result in an ExecutionException
            // generated by the XML document parser: Unable to parse the xml
            // document.
        }
    }

    @Override
    @Ignore
    public void testCreateLiveSession() throws ExecutionException {
        fShell.setScenario(SCEN_CREATE_LIVE_SESSION);

        ISessionInfo params = new SessionInfo("mysession");
        params.setLive(true);
        params.setStreamedTrace(true);
        params.setNetworkUrl("net://127.0.0.1");
        ISessionInfo sessionInfo = fService.createSession(params, new NullProgressMonitor());
        assertNotNull(sessionInfo);
        assertEquals("mysession", sessionInfo.getName());
        assertEquals(TraceSessionState.INACTIVE, sessionInfo.getSessionState());
        assertTrue(sessionInfo.isStreamedTrace());
        assertTrue(sessionInfo.isLive());
        assertEquals("tcp4://127.0.0.1:5342/ [data: 5343]", sessionInfo.getSessionPath());
        List<String> names = fService.getSessionNames(new NullProgressMonitor());
        assertEquals(names.get(0), "mysession");
        fService.destroySession("mysession", new NullProgressMonitor());
    }

    @Override
    protected String getTestStream() {
        return MI_TEST_STREAM;
    }

    @Override
    protected TraceLogLevel getAllEventTraceLogLevel() {
        return TraceLogLevel.TRACE_DEBUG;
    }

    @Override
    public void testGetKernelProviderNoUstVerbose() {
        // Verbose mode in machine interface is deactivated. This test is
        // ignored.
    }

    @Override
    public void testCreateSession2_1() {
        // 2.1 is not supported by mi. This test is ignored.
    }

    @Override
    public void testGetKernelProviderNoUst3() {
        // Verbose mode in machine interface is deactivated. This test is
        // ignored.
    }

    @Override
    public void testGetKernelProviderNoKernelVerbose() {
        // Verbose mode in machine interface is deactivated. This test is
        // ignored.
    }

    @Override
    public void testCreateSessionVerbose2_1() {
        // Verbose mode in machine interface is deactivated. This test is
        // ignored.
    }

    @Override
    public void testDestroySessionVerbose() {
        // Verbose mode in machine interface is deactivated. This test is
        // ignored.
    }

    @Override
    public void testCreateSessionWithPrompt() {
        // TODO Investigate if this case can happen in production. If yes than
        // we need to rethinks the MI fetching and parsing.
    }

    @Override
    public void testAddContext() {
        // TODO This does not use mi feature.And currently the context enabling
        // is wrong for 2.6.
    }

    @Override
    public void testAddContextFailure() {
        // TODO This does not use mi feature.And currently the context enabling
        // is wrong for 2.6.
    }

    @Override
    public void testCreateSnapshotSession2_5() {
        // not applicable for MI
    }

    /**
     * Tests the listing of syscalls
     */
    @Test
    public void testListSycallEvents() {
        try {
            fShell.setScenario(SCEN_SESSION_WITH_SYSCALLS);
            ISessionInfo session = fService.getSession("mysession", new NullProgressMonitor());

            // Verify Session
            assertNotNull(session);
            assertEquals("mysession", session.getName());
            assertEquals("/home/user/lttng-traces/mysession-20120129-084256", session.getSessionPath());
            assertEquals(TraceSessionState.INACTIVE, session.getSessionState());

            IDomainInfo[] domains = session.getDomains();
            assertNotNull(domains);
            assertEquals(1, domains.length);

            // Verify Kernel domain
            assertEquals("Kernel", domains[0].getName());
            IChannelInfo[] channels =  domains[0].getChannels();
            assertNotNull(channels);
            assertEquals(1, channels.length);

            // Verify Kernel's channel0
            assertEquals("channel0", channels[0].getName());
            assertEquals(4, channels[0].getNumberOfSubBuffers());
            assertEquals("splice()", channels[0].getOutputType().getInName());
            assertEquals(TraceChannelOutputType.SPLICE, channels[0].getOutputType());
            assertEquals(false, channels[0].isOverwriteMode());
            assertEquals(200, channels[0].getReadTimer());
            assertEquals(TraceEnablement.ENABLED, channels[0].getState());
            assertEquals(262144, channels[0].getSubBufferSize());
            assertEquals(0, channels[0].getSwitchTimer());

            // Verify event info
            IEventInfo[] channel0Events = channels[0].getEvents();
            assertNotNull(channel0Events);
            assertEquals(2, channel0Events.length);
            assertEquals("read", channel0Events[0].getName());
            assertEquals(TraceEventType.SYSCALL, channel0Events[0].getEventType());
            assertEquals(TraceEnablement.ENABLED, channel0Events[0].getState());

            assertEquals("write", channel0Events[1].getName());
            assertEquals(TraceEventType.SYSCALL, channel0Events[1].getEventType());
            assertEquals(TraceEnablement.ENABLED, channel0Events[1].getState());
        } catch (ExecutionException e) {
            fail(e.toString());
        }
    }

    /**
     * Test List session for lttng 2.7.
     *
     * This is to make sure that it is possible to parse the output of a session
     * create on a target with LTTng 2.7 installed.
     *
     *
     */
    @Test
    public void testListSessionCompatibility_2_7() {

        /*
           Note the session was created with basic commands:
           lttng create mysession
           lttng enable-event  -a  -k  -s mysession
           lttng enable-channel channel0 -u  -s mysession --buffers-pid
           lttng enable-event  -a  -u  -s mysession -c channel0 --tracepoint
           lttng add-context -u -t vtid -t procname
        */

        try {
            fShell.setScenario(SCEN_LIST_SESSION_2_7_COMPAT);
            ISessionInfo session = fService.getSession("mysession", new NullProgressMonitor());

            // Verify Session
            assertNotNull(session);
            assertEquals("mysession", session.getName());
            assertEquals("/home/user/lttng-traces/mysession-20151020-085614", session.getSessionPath());
            assertEquals(TraceSessionState.INACTIVE, session.getSessionState());

            IDomainInfo[] domains = session.getDomains();
            assertNotNull(domains);
            assertEquals(2, domains.length);

            // Verify Kernel domain
            assertEquals("Kernel", domains[0].getName());
            IChannelInfo[] channels =  domains[0].getChannels();
            assertNotNull(channels);
            assertEquals(1, channels.length);

            // Verify Kernel's channel0
            assertEquals("channel0", channels[0].getName());
            assertEquals(4, channels[0].getNumberOfSubBuffers());
            assertEquals("splice()", channels[0].getOutputType().getInName());
            assertEquals(TraceChannelOutputType.SPLICE, channels[0].getOutputType());
            assertEquals(false, channels[0].isOverwriteMode());
            assertEquals(200000, channels[0].getReadTimer());
            assertEquals(TraceEnablement.ENABLED, channels[0].getState());
            assertEquals(262144, channels[0].getSubBufferSize());
            assertEquals(0, channels[0].getSwitchTimer());

            // Verify event info
            IEventInfo[] channel0Events = channels[0].getEvents();
            assertNotNull(channel0Events);
            assertEquals(2, channel0Events.length);
            assertEquals("*", channel0Events[0].getName());
            assertEquals(TraceEventType.SYSCALL, channel0Events[0].getEventType());
            assertEquals(TraceEnablement.ENABLED, channel0Events[0].getState());

            assertEquals("*", channel0Events[1].getName());
            assertEquals(TraceLogLevel.TRACE_EMERG, channel0Events[1].getLogLevel());
            assertEquals(LogLevelType.LOGLEVEL_ALL, channel0Events[1].getLogLevelType());
            assertEquals(TraceEventType.TRACEPOINT, channel0Events[1].getEventType());
            assertEquals(TraceEnablement.ENABLED, channel0Events[1].getState());

            // Verify domain UST global
            assertEquals("UST global", domains[1].getName());

            IChannelInfo[] ustChannels =  domains[1].getChannels();

            // Verify UST global's channel0
            assertEquals("channel0", ustChannels[0].getName());
            assertEquals(4, ustChannels[0].getNumberOfSubBuffers());
            assertEquals("mmap()", ustChannels[0].getOutputType().getInName());
            assertEquals(TraceChannelOutputType.MMAP, ustChannels[0].getOutputType());
            assertEquals(false, ustChannels[0].isOverwriteMode());
            assertEquals(0, ustChannels[0].getReadTimer());
            assertEquals(TraceEnablement.ENABLED, ustChannels[0].getState());
            assertEquals(4096, ustChannels[0].getSubBufferSize());
            assertEquals(0, ustChannels[0].getSwitchTimer());

            // Verify event info
            IEventInfo[] ustEvents = ustChannels[0].getEvents();
            assertEquals(1, ustEvents.length);

            assertEquals("*", ustEvents[0].getName());
            assertEquals(TraceEventType.TRACEPOINT, ustEvents[0].getEventType());
            assertEquals(TraceEnablement.ENABLED, ustEvents[0].getState());

        } catch (ExecutionException e) {
            fail(e.toString());
        }
    }

    @Override
    public void testEnableJulLoggers() {
        try {
            String sessionName = "mysession";
            // Lists
            List<String> loggerList = new ArrayList<>();
            // Loggers
            String loggerName1 = "logger";
            String loggerName2 = "anotherLogger";
            String allLoggerName = "*";

            fShell.setScenario(SCEN_ENABLING_JUL_LOGGERS);

            // 1) Enabling all loggers
            loggerList.add(allLoggerName);
            fService.enableEvents(sessionName, null, loggerList, TraceDomainType.JUL, null, null, new NullProgressMonitor());
            loggerList.clear();

            // 2) Enabling one logger
            loggerList.add(loggerName1);
            fService.enableEvents(sessionName, null, loggerList, TraceDomainType.JUL, null, null, new NullProgressMonitor());

            // 3) Enabling two loggers with loglevel-only JUL_WARNING and
            //    verifying the attributes of one of them
            loggerList.add(loggerName2);
            fService.enableLogLevel(sessionName, null, loggerList, LogLevelType.LOGLEVEL_ONLY, TraceJulLogLevel.JUL_WARNING, null, TraceDomainType.JUL, new NullProgressMonitor());

            @Nullable
            ISessionInfo session = fService.getSession(sessionName, new NullProgressMonitor());
            assertNotNull(session);
            // Get the list of loggers
            List<ILoggerInfo> loggers = session.getDomains()[1].getLoggers();
            assertNotNull(loggers);
            assertEquals(loggers.size(), 4);
            // Get the "anotherLogger" logger
            ILoggerInfo loggerInfo = loggers.stream()
                    .filter(logger -> logger.getName().equals(loggerName2))
                    .findFirst().get();
            // Verify attributes
            assertEquals(loggerName2, loggerInfo.getName());
            assertEquals(TraceDomainType.JUL, loggerInfo.getDomain());
            assertEquals(TraceJulLogLevel.JUL_WARNING, loggerInfo.getLogLevel());
            assertEquals(LogLevelType.LOGLEVEL_ONLY, loggerInfo.getLogLevelType());
            assertEquals(TraceEnablement.ENABLED, loggerInfo.getState());
        } catch (ExecutionException e) {
            fail(e.toString());
        }
    }

    @Override
    public void testEnableLog4jLoggers() {
        try {
            String sessionName = "mysession";
            // Lists
            List<String> loggerList = new ArrayList<>();
            // Loggers
            String loggerName1 = "logger";
            String loggerName2 = "anotherLogger";
            String allLoggerName = "*";

            fShell.setScenario(SCEN_ENABLING_LOG4J_LOGGERS);

            // 1) Enabling all loggers
            loggerList.add(allLoggerName);
            fService.enableEvents(sessionName, null, loggerList, TraceDomainType.LOG4J, null, null, new NullProgressMonitor());
            loggerList.clear();

            // 2) Enabling one logger
            loggerList.add(loggerName1);
            fService.enableEvents(sessionName, null, loggerList, TraceDomainType.LOG4J, null, null, new NullProgressMonitor());

            // 3) Enabling two loggers with loglevel-only LOG4J_FATAL and
            //    verifying the attributes of one of them
            loggerList.add(loggerName2);
            fService.enableLogLevel(sessionName, null, loggerList, LogLevelType.LOGLEVEL_ONLY, TraceLog4jLogLevel.LOG4J_FATAL, null, TraceDomainType.LOG4J, new NullProgressMonitor());

            @Nullable
            ISessionInfo session = fService.getSession(sessionName, new NullProgressMonitor());
            assertNotNull(session);
            // Get the list of loggers
            List<ILoggerInfo> loggers = session.getDomains()[1].getLoggers();
            assertNotNull(loggers);
            assertEquals(4, loggers.size());
            // Get the "anotherLogger" logger
            ILoggerInfo loggerInfo = loggers.stream()
                    .filter(logger -> logger.getName().equals(loggerName2))
                    .findFirst().get();
            // Verify attributes
            assertEquals(loggerName2, loggerInfo.getName());
            assertEquals(TraceDomainType.LOG4J, loggerInfo.getDomain());
            assertEquals(TraceLog4jLogLevel.LOG4J_FATAL, loggerInfo.getLogLevel());
            assertEquals(LogLevelType.LOGLEVEL_ONLY, loggerInfo.getLogLevelType());
            assertEquals(TraceEnablement.ENABLED, loggerInfo.getState());
        } catch (ExecutionException e) {
            fail(e.toString());
        }
    }

    @Override
    public void testEnablePythonLoggers() {
        try {
            String sessionName = "mysession";
            // Lists
            List<String> loggerList = new ArrayList<>();
            // Loggers
            String loggerName1 = "logger";
            String loggerName2 = "anotherLogger";
            String allLoggerName = "*";

            fShell.setScenario(SCEN_ENABLING_PYTHON_LOGGERS);

            // 1) Enabling all loggers
            loggerList.add(allLoggerName);
            fService.enableEvents(sessionName, null, loggerList, TraceDomainType.PYTHON, null, null, new NullProgressMonitor());
            loggerList.clear();

            // 2) Enabling one logger
            loggerList.add(loggerName1);
            fService.enableEvents(sessionName, null, loggerList, TraceDomainType.PYTHON, null, null, new NullProgressMonitor());

            // 3) Enabling two loggers with loglevel-only PYTHON_CRITICAL and
            //    verifying the attributes of one of them
            loggerList.add(loggerName2);
            fService.enableLogLevel(sessionName, null, loggerList, LogLevelType.LOGLEVEL_ONLY, TracePythonLogLevel.PYTHON_CRITICAL, null, TraceDomainType.PYTHON, new NullProgressMonitor());

            @Nullable
            ISessionInfo session = fService.getSession(sessionName, new NullProgressMonitor());
            assertNotNull(session);
            // Get the list of loggers
            List<ILoggerInfo> loggers = session.getDomains()[1].getLoggers();
            assertNotNull(loggers);
            assertEquals(4, loggers.size());
            // Get the "anotherLogger" logger
            ILoggerInfo loggerInfo = loggers.stream()
                    .filter(logger -> logger.getName().equals(loggerName2))
                    .findFirst().get();
            // Verify attributes
            assertEquals(loggerName2, loggerInfo.getName());
            assertEquals(TraceDomainType.PYTHON, loggerInfo.getDomain());
            assertEquals(TracePythonLogLevel.PYTHON_CRITICAL, loggerInfo.getLogLevel());
            assertEquals(LogLevelType.LOGLEVEL_ONLY, loggerInfo.getLogLevelType());
            assertEquals(TraceEnablement.ENABLED, loggerInfo.getState());
        } catch (ExecutionException e) {
            fail(e.toString());
        }
    }
}
