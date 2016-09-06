/*
 *
 *  Copyright 2012-2015 Eurocommercial Properties NV
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
package org.estatio.dom.budgetassignment;

import java.util.List;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.estatio.dom.UdoDomainRepositoryAndFactory;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.lease.Occupancy;
import org.estatio.dom.valuetypes.LocalDateInterval;

@DomainService(repositoryFor = ServiceChargeTerm.class, nature = NatureOfService.DOMAIN)
@DomainServiceLayout()
public class ServiceChargeTermRepository extends UdoDomainRepositoryAndFactory<ServiceChargeTerm> {

    public ServiceChargeTermRepository() {
        super(ServiceChargeTermRepository.class, ServiceChargeTerm.class);
    }

    // //////////////////////////////////////

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    public ServiceChargeTerm newServiceChargeTerm(
            final Occupancy occupancy,
            final Charge charge,
            final Integer budgetYear) {
        ServiceChargeTerm serviceChargeTerm = newTransientInstance();
        serviceChargeTerm.setOccupancy(occupancy);
        serviceChargeTerm.setCharge(charge);
        serviceChargeTerm.setBudgetYear(budgetYear);
        persistIfNotAlready(serviceChargeTerm);

        return serviceChargeTerm;
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public ServiceChargeTerm findOrCreateServiceChargeTerm(
            final Occupancy occupancy,
            final Charge charge,
            final LocalDateInterval budgetYear) {
        return findOrCreateServiceChargeTerm(occupancy, charge, budgetYear.asInterval().getStart().getYear());
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public ServiceChargeTerm findOrCreateServiceChargeTerm(
            final Occupancy occupancy,
            final Charge charge,
            final Integer budgetYear) {
        ServiceChargeTerm term = findUnique(occupancy,charge,budgetYear);
        return term == null ? newServiceChargeTerm(occupancy, charge, budgetYear) : term;
    }


    public List<ServiceChargeTerm> findByOccupancy(final Occupancy occupancy){
        return allMatches("findByOccupancy", "occupancy", occupancy);
    }

    public ServiceChargeTerm findUnique(final Occupancy occupancy, final Charge charge, final Integer budgetYear){
        return uniqueMatch("findByOccupancyAndChargeAndBudgetYear",
                "occupancy", occupancy,
                "charge", charge,
                "budgetYear", budgetYear
        );
    }

    public List<ServiceChargeTerm> allServiceChargeTerms() {
        return allInstances();
    }

}
