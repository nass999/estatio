/*
 * Copyright 2015 Yodo Int. Projects and Consultancy
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.estatio.dom.budgetassignment;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.query.Query;
import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import org.estatio.dom.FinderInteraction;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.lease.Occupancy;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceChargeRepositoryTest {

    FinderInteraction finderInteraction;

    ServiceChargeTermRepository serviceChargeRepo;

    @Before
    public void setup() {
        serviceChargeRepo = new ServiceChargeTermRepository() {

            @Override
            protected <T> T firstMatch(Query<T> query) {
                finderInteraction = new FinderInteraction(query, FinderInteraction.FinderMethod.FIRST_MATCH);
                return null;
            }

            @Override
            protected <T> T uniqueMatch(Query<T> query) {
                finderInteraction = new FinderInteraction(query, FinderInteraction.FinderMethod.UNIQUE_MATCH);
                return null;
            }

            @Override
            protected List<ServiceChargeTerm> allInstances() {
                finderInteraction = new FinderInteraction(null, FinderInteraction.FinderMethod.ALL_INSTANCES);
                return null;
            }

            @Override
            protected <T> List<T> allMatches(Query<T> query) {
                finderInteraction = new FinderInteraction(query, FinderInteraction.FinderMethod.ALL_MATCHES);
                return null;
            }
        };
    }

    public static class FindByOccupancy extends ServiceChargeRepositoryTest {

        @Test
        public void happyCase() {

            Occupancy occupancy = new Occupancy();
            serviceChargeRepo.findByOccupancy(occupancy);

            assertThat(finderInteraction.getFinderMethod()).isEqualTo(FinderInteraction.FinderMethod.ALL_MATCHES);
            assertThat(finderInteraction.getResultType()).isEqualTo(ServiceChargeTerm.class);
            assertThat(finderInteraction.getQueryName()).isEqualTo("findByOccupancy");
            assertThat(finderInteraction.getArgumentsByParameterName().get("occupancy")).isEqualTo((Object) occupancy);
            assertThat(finderInteraction.getArgumentsByParameterName()).hasSize(1);
        }

    }

    public static class Findunique extends ServiceChargeRepositoryTest {

        @Test
        public void happyCase() {

            Occupancy occupancy = new Occupancy();
            Charge charge = new Charge();
            Integer budgetYear = 2016;
            serviceChargeRepo.findUnique(occupancy, charge, budgetYear);

            assertThat(finderInteraction.getFinderMethod()).isEqualTo(FinderInteraction.FinderMethod.UNIQUE_MATCH);
            assertThat(finderInteraction.getResultType()).isEqualTo(ServiceChargeTerm.class);
            assertThat(finderInteraction.getQueryName()).isEqualTo("findByOccupancyAndChargeAndBudgetYear");
            assertThat(finderInteraction.getArgumentsByParameterName().get("occupancy")).isEqualTo((Object) occupancy);
            assertThat(finderInteraction.getArgumentsByParameterName().get("charge")).isEqualTo((Object) charge);
            assertThat(finderInteraction.getArgumentsByParameterName().get("budgetYear")).isEqualTo((Object) budgetYear);
            assertThat(finderInteraction.getArgumentsByParameterName()).hasSize(3);
        }

    }


    public static class NewServiceChargeTerm extends ServiceChargeRepositoryTest {

        @Rule
        public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

        @Mock
        private DomainObjectContainer mockContainer;

        ServiceChargeTermRepository serviceChargeTermRepository;

        @Before
        public void setup() {
            serviceChargeTermRepository = new ServiceChargeTermRepository();
            serviceChargeTermRepository.setContainer(mockContainer);
        }

        @Test
        public void newServiceChargeTerm() {

            //given
            Occupancy occupancy = new Occupancy();
            Charge charge = new Charge();
            Integer budgetYear = 2016;
            final ServiceChargeTerm serviceChargeTerm = new ServiceChargeTerm();

            // expect
            context.checking(new Expectations() {
                {
                    oneOf(mockContainer).newTransientInstance(ServiceChargeTerm.class);
                    will(returnValue(serviceChargeTerm));
                    oneOf(mockContainer).persistIfNotAlready(serviceChargeTerm);
                }

            });

            //when
            ServiceChargeTerm newScTerm = serviceChargeTermRepository.newServiceChargeTerm(occupancy, charge, budgetYear);

            //then
            assertThat(newScTerm.getOccupancy()).isEqualTo(occupancy);
            assertThat(newScTerm.getCharge()).isEqualTo(charge);
            assertThat(newScTerm.getBudgetYear()).isEqualTo(budgetYear);


        }

    }

}
