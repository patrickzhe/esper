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

package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public class EnumEvalMinMaxByEvents extends EnumEvalBase implements EnumEval {

    private final boolean max;

    public EnumEvalMinMaxByEvents(ExprEvaluator innerExpression, int streamCountIncoming, boolean max) {
        super(innerExpression, streamCountIncoming);
        this.max = max;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Comparable minKey = null;
        EventBean result = null;

        Collection<EventBean> beans = (Collection<EventBean>) target;
        for (EventBean next : beans) {
            eventsLambda[streamNumLambda] = next;

            Object comparable = innerExpression.evaluate(eventsLambda, isNewData, context);
            if (comparable == null) {
                continue;
            }

            if (minKey == null) {
                minKey = (Comparable) comparable;
                result = next;
            }
            else {
                if (max) {
                    if (minKey.compareTo(comparable) < 0) {
                        minKey = (Comparable) comparable;
                        result = next;
                    }
                }
                else {
                    if (minKey.compareTo(comparable) > 0) {
                        minKey = (Comparable) comparable;
                        result = next;
                    }
                }
            }
        }

        return result;  // unpack of EventBean to underlying performed at another stage 
    }
}
