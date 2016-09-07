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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import org.estatio.dom.AbstractBeanPropertiesTest;
import org.estatio.dom.budgeting.allocation.BudgetItemAllocation;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculation;
import org.estatio.dom.budgeting.budgetcalculation.CalculationType;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.lease.Occupancy;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceChargeTermTest {

    public static class BeanProperties extends AbstractBeanPropertiesTest {

        @Test
        public void test() {
            final ServiceChargeTerm pojo = new ServiceChargeTerm();
            newPojoTester()
                    .withFixture(pojos(Occupancy.class, Occupancy.class))
                    .withFixture(pojos(Charge.class, Charge.class))
                    .exercise(pojo);
        }

    }

    public static class calculateTest extends ServiceChargeTermTest {

        @Rule
        public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

        @Mock
        BudgetCalculationLinkRepository budgetCalculationLinkRepository;

        ServiceChargeTerm serviceChargeTerm;
        BudgetCalculationLink budgetCalculationLink1;
        BudgetCalculationLink budgetCalculationLink2;
        BudgetCalculation budgetCalculation1;
        BudgetCalculation budgetCalculation2;
        BudgetItemAllocation budgetItemAllocation1;
        BudgetItemAllocation budgetItemAllocation2;
        BudgetItem budgetItem;
        Budget budget;

        LocalDate startDate = new LocalDate(2016, 01, 01);
        LocalDate endDate = new LocalDate(2016, 12, 31);

        @Before
        public void setup(){
            budget = new Budget();
            budget.setStartDate(startDate);
            budget.setEndDate(endDate);

            budgetItem = new BudgetItem();
            budgetItem.setBudget(budget);

            budgetItemAllocation1 = new BudgetItemAllocation();
            budgetItemAllocation1.setBudgetItem(budgetItem);
            budgetItemAllocation2 = new BudgetItemAllocation();
            budgetItemAllocation2.setBudgetItem(budgetItem);

            budgetCalculation1 = new BudgetCalculation();
            budgetCalculation1.setBudgetItemAllocation(budgetItemAllocation1);
            budgetCalculation1.setValue(new BigDecimal("99.99"));
            budgetCalculation1.setCalculationType(CalculationType.BUDGETED);

            budgetCalculation2 = new BudgetCalculation();
            budgetCalculation2.setBudgetItemAllocation(budgetItemAllocation2);
            budgetCalculation2.setValue(new BigDecimal("101.01"));
            budgetCalculation2.setCalculationType(CalculationType.BUDGETED);

            budgetCalculationLink1 = new BudgetCalculationLink();
            budgetCalculationLink1.setBudgetCalculation(budgetCalculation1);

            budgetCalculationLink2 = new BudgetCalculationLink();
            budgetCalculationLink2.setBudgetCalculation(budgetCalculation2);

            serviceChargeTerm = new ServiceChargeTerm();
            serviceChargeTerm.budgetCalculationLinkRepository = budgetCalculationLinkRepository;

        }

        @Test
        public void calculate() {

            // expect
            context.checking(new Expectations() {
                {
                    allowing(budgetCalculationLinkRepository).findByServiceChargeTerm(serviceChargeTerm);
                    will(returnValue(Arrays.asList(budgetCalculationLink1, budgetCalculationLink2)));
                }

            });

            // when
            serviceChargeTerm.calculate();

            // then
            assertThat(serviceChargeTerm.annualFactor(budgetCalculation1)).isEqualTo(BigDecimal.ONE);
            assertThat(serviceChargeTerm.getCalculatedBudgetedValue()).isEqualTo(new BigDecimal("201.00"));
            assertThat(serviceChargeTerm.getCalculatedAuditedValue()).isEqualTo(new BigDecimal("0.00"));

            // and when
            budget.setEndDate(new LocalDate(2016, 01, 01));
            serviceChargeTerm.calculate();

            // then
            assertThat(serviceChargeTerm.annualFactor(budgetCalculation1)).isEqualTo(
                    BigDecimal.ONE.
                            divide(new BigDecimal("366"), MathContext.DECIMAL64)
            );
            assertThat(serviceChargeTerm.getCalculatedBudgetedValue()).isEqualTo(new BigDecimal("0.55"));

            // and when
            budgetCalculation2.setCalculationType(CalculationType.AUDITED);
            serviceChargeTerm.calculate();

            // then
            assertThat(serviceChargeTerm.getCalculatedBudgetedValue()).isEqualTo(new BigDecimal("0.27"));
            assertThat(serviceChargeTerm.getCalculatedAuditedValue()).isEqualTo(new BigDecimal("0.28"));

        }

    }

}
