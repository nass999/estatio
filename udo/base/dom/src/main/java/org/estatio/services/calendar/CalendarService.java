/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.services.calendar;

import javax.inject.Inject;

import org.joda.time.LocalDate;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Programmatic;

@DomainService(nature = NatureOfService.DOMAIN)
public class CalendarService {

    private static final int MONTHS_IN_QUARTER = 3;

    // cannot have the same id as the superclass
    // (that is also registered as a service by virtue of @DomainService)
    public String getId() {
        return "estatioClockService";
    }

    /**
     * @deprecated - use {@link org.apache.isis.applib.services.clock.ClockService#nowAsMillis()}.
     */
    @Deprecated
    @Programmatic
    public long timestamp() {
        return clockService.nowAsMillis();
    }
    
    // //////////////////////////////////////

    @Programmatic
    public LocalDate beginningOfMonth() {
        return beginningOfMonth(clockService.now());
    }

    static LocalDate beginningOfMonth(final LocalDate date) {
        final int dayOfMonth = date.getDayOfMonth();
        return date.minusDays(dayOfMonth-1);
    }

    // //////////////////////////////////////

    @Programmatic
    public LocalDate beginningOfQuarter() {
        final LocalDate date = clockService.now();
        return beginningOfQuarter(date);
    }

    @Programmatic
    public LocalDate beginningOfNextQuarter() {
        final LocalDate date = clockService.now().plusMonths(3);
        return beginningOfQuarter(date);
    }
    
    static LocalDate beginningOfQuarter(final LocalDate date) {
        final LocalDate beginningOfMonth = beginningOfMonth(date);
        final int monthOfYear = beginningOfMonth.getMonthOfYear();
        final int quarter = (monthOfYear-1)/MONTHS_IN_QUARTER; // 0, 1, 2, 3
        final int monthStartOfQuarter = quarter*MONTHS_IN_QUARTER+1;
        return beginningOfMonth.minusMonths(monthOfYear-monthStartOfQuarter);
    }

    @Inject
    org.apache.isis.applib.services.clock.ClockService clockService;
}
