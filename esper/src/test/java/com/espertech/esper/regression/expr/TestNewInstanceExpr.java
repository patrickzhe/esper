/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.expr;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.event.EventTypeAssertionEnum;
import com.espertech.esper.support.event.EventTypeAssertionUtil;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import com.espertech.esper.support.util.SupportModelHelper;
import junit.framework.TestCase;

import java.util.Map;

public class TestNewInstanceExpr extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testNewInstance() {
        runAssertionNewInstance(false);
        runAssertionNewInstance(true);

        runAssertionStreamAlias();

        // try variable
        epService.getEPAdministrator().createEPL("create constant variable java.util.concurrent.atomic.AtomicInteger cnt = new java.util.concurrent.atomic.AtomicInteger(1)");

        // try shallow invalid cases
        SupportMessageAssertUtil.tryInvalid(epService, "select new Dummy() from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'new Dummy()': Failed to resolve new-operator class name 'Dummy'");

        epService.getEPAdministrator().getConfiguration().addImport(MyClassNoCtor.class);
        SupportMessageAssertUtil.tryInvalid(epService, "select new MyClassNoCtor() from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'new MyClassNoCtor()': Failed to find a suitable constructor for class ");
    }

    private void runAssertionStreamAlias() {
        epService.getEPAdministrator().getConfiguration().addImport(MyClassObjectCtor.class);
        epService.getEPAdministrator().createEPL("select " +
                "new MyClassObjectCtor(sb) as c0 " +
                "from SupportBean as sb").addListener(listener);

        SupportBean sb = new SupportBean();
        epService.getEPRuntime().sendEvent(sb);
        EventBean event = listener.assertOneGetNewAndReset();
        assertSame(sb, ((MyClassObjectCtor) event.get("c0")).getObject());
    }

    private void runAssertionNewInstance(boolean soda) {
        epService.getEPAdministrator().getConfiguration().addImport(SupportBean.class);

        String epl = "select " +
                "new SupportBean(\"A\",intPrimitive) as c0, " +
                "new SupportBean(\"B\",intPrimitive+10), " +
                "new SupportBean() as c2, " +
                "new SupportBean(\"ABC\",0).getTheString() as c3 " +
                "from SupportBean";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        stmt.addListener(listener);
        Object[][] expectedAggType = new Object[][]{{"c0", SupportBean.class}, {"new SupportBean(\"B\",intPrimitive+10)", SupportBean.class}};
        EventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmt.getEventType(), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);

        String[] fields = "theString,intPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertPropsPOJO(event.get("c0"), fields, new Object[] {"A", 10});
        EPAssertionUtil.assertPropsPOJO(((Map) event.getUnderlying()).get("new SupportBean(\"B\",intPrimitive+10)"), fields, new Object[] {"B", 20});
        EPAssertionUtil.assertPropsPOJO(event.get("c2"), fields, new Object[] {null, 0});
        assertEquals("ABC", event.get("c3"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    public static class MyClassNoCtor {
        private MyClassNoCtor() {
        }
    }

    public static class MyClassObjectCtor {
        private final Object object;

        public MyClassObjectCtor(Object object) {
            this.object = object;
        }

        public Object getObject() {
            return object;
        }
    }
}
