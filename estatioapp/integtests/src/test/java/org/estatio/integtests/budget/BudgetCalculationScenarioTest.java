package org.estatio.integtests.budget;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.PropertyRepository;
import org.estatio.dom.budgetassignment.BudgetAssignmentService;
import org.estatio.dom.budgetassignment.ServiceChargeTerm;
import org.estatio.dom.budgetassignment.ServiceChargeTermRepository;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budget.BudgetRepository;
import org.estatio.dom.budgetassignment.BudgetCalculationLinkRepository;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationRepository;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationService;
import org.estatio.dom.budgeting.budgetcalculation.CalculationType;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.asset.PropertyForOxfGb;
import org.estatio.fixture.budget.BudgetItemAllocationsForOxf;
import org.estatio.fixture.budget.BudgetsForOxf;
import org.estatio.fixture.lease.LeaseItemForServiceChargeBudgetedForOxfTopModel001Gb;
import org.estatio.integtests.EstatioIntegrationTest;

public class BudgetCalculationScenarioTest extends EstatioIntegrationTest {

    @Inject
    BudgetCalculationRepository budgetCalculationRepository;

    @Inject
    BudgetCalculationLinkRepository budgetCalculationLinkRepository;

    @Inject
    PropertyRepository propertyRepository;

    @Inject
    BudgetRepository budgetRepository;

    @Inject
    BudgetCalculationService budgetCalculationService;

    @Inject
    BudgetAssignmentService budgetAssignmentService;

    @Inject
    ServiceChargeTermRepository serviceChargeTermRepository;


    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(final ExecutionContext executionContext) {
                executionContext.executeChild(this, new EstatioBaseLineFixture());
                executionContext.executeChild(this, new BudgetItemAllocationsForOxf());
                executionContext.executeChild(this, new LeaseItemForServiceChargeBudgetedForOxfTopModel001Gb());
            }
        });
    }


    public static class Calculate extends BudgetCalculationScenarioTest {

        Property property;
        Budget budget;

        @Before
        public void setup() {
            // given
            property = propertyRepository.findPropertyByReference(PropertyForOxfGb.REF);
            budget = budgetRepository.findByPropertyAndStartDate(property, BudgetsForOxf.BUDGET_2015_START_DATE);
        }

        @Test
        public void CalculateAndAssign() throws Exception {
            calculation();
            assignBudget();
            assignBudgetWhenUpdated();
            assignBudgetWhenAudited();
            assignbudgetWhenAuditedAndUpdated();
            assignBudgetWhenAuditedAndUpdatedWithEmptyAuditedValueOnBudgetItem();
        }

        public void calculation() throws Exception {


            // when
            budgetCalculationRepository
                    .resetAndUpdateOrCreateBudgetCalculations(
                            budget,
                            budgetCalculationService.calculate(budget));

            // then
            Assertions.assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(75);
            Assertions.assertThat(budgetCalculationRepository.findByBudgetItemAndCalculationType(budget.getItems().first(), CalculationType.BUDGETED).size()).isEqualTo(25);
            Assertions.assertThat(serviceChargeTermRepository.allServiceChargeTerms().size()).isEqualTo(0);

        }

        public void assignBudget() throws Exception {


            // when
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
            Assertions.assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(3);

            Assertions.assertThat(budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getBudgetCalculation()
                    .getValue())
                    .isEqualTo(new BigDecimal("92.31"));
            Assertions.assertThat(budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(1)
                    .getBudgetCalculation()
                    .getValue())
                    .isEqualTo(new BigDecimal("98.46"));
            Assertions.assertThat(budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(2)
                    .getBudgetCalculation()
                    .getValue())
                    .isEqualTo(new BigDecimal("320.00"));

            ServiceChargeTerm createdTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();

            Assertions.assertThat(
                    createdTerm.getCalculatedBudgetedValue())
                    .isEqualTo(new BigDecimal("510.77"));

            Assertions.assertThat(
                    createdTerm.getCalculatedAuditedValue())
                    .isEqualTo(new BigDecimal("0.00"));

            Assertions.assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(75);
            Assertions.assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(3);
            Assertions.assertThat(serviceChargeTermRepository.allServiceChargeTerms().size()).isEqualTo(1);

        }

        public void assignBudgetWhenUpdated() throws Exception {

            // given
            BudgetItem updatedItem = budget.getItems().first();
            updatedItem.setBudgetedValue(new BigDecimal("45000.00"));
            ServiceChargeTerm existingTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();

            // when
            budgetCalculationRepository
                    .resetAndUpdateOrCreateBudgetCalculations(
                            budget,
                            budgetCalculationService.calculate(budget));
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
//            Assertions.assertThat(existingTerm.getBudgetedValue())
//                    .isEqualTo(new BigDecimal("556.93"));
            Assertions.assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(75);
            Assertions.assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(3);
        }

        public void assignBudgetWhenAudited() throws Exception {

            // given
            BudgetItem auditedItem = budget.getItems().last();
            auditedItem.setAuditedValue(new BigDecimal("45000.00"));
            ServiceChargeTerm existingTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();
//            Assertions.assertThat(existingTerm.getBudgetedValue())
//                    .isEqualTo(new BigDecimal("556.93"));
//            Assertions.assertThat(existingTerm.getAuditedValue()).isEqualTo(BigDecimal.ZERO);
//            Assertions.assertThat(existingTerm.getInterval())
//                    .isEqualTo(budget.getInterval());

            // when
            budgetCalculationRepository
                    .resetAndUpdateOrCreateBudgetCalculations(
                            budget,
                            budgetCalculationService.calculate(budget));
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
//            Assertions.assertThat(existingTerm.getAuditedValue())
//                    .isEqualTo(new BigDecimal("470.77"));
            Assertions.assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(125);
            Assertions.assertThat(budgetCalculationRepository.findByBudgetAndCalculationType(budget, CalculationType.AUDITED).size()).isEqualTo(50);
            Assertions.assertThat(budgetCalculationRepository.findByBudgetAndCalculationType(budget, CalculationType.BUDGETED).size()).isEqualTo(75);
            Assertions.assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(5);
        }

        public void assignbudgetWhenAuditedAndUpdated() throws Exception {

            // given
            BudgetItem auditedAndUpdatedItem = budget.getItems().last();
            auditedAndUpdatedItem.setAuditedValue(new BigDecimal("46000.00"));
            ServiceChargeTerm existingTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();
//            Assertions.assertThat(existingTerm.getBudgetedValue())
//                    .isEqualTo(new BigDecimal("556.93"));
//            Assertions.assertThat(existingTerm.getAuditedValue()).isEqualTo(new BigDecimal("470.77"));

            // when
            budgetCalculationRepository
                    .resetAndUpdateOrCreateBudgetCalculations(
                            budget,
                            budgetCalculationService.calculate(budget));
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
//            Assertions.assertThat(existingTerm.getAuditedValue())
//                    .isEqualTo(new BigDecimal("481.23"));

            Assertions.assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(125);
            Assertions.assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(5);
        }

        public void assignBudgetWhenAuditedAndUpdatedWithEmptyAuditedValueOnBudgetItem() throws Exception {

            // given
            BudgetItem auditedAndUpdatedItem = budget.getItems().last();
            auditedAndUpdatedItem.setAuditedValue(null);
            ServiceChargeTerm existingTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();
//            Assertions.assertThat(existingTerm.getBudgetedValue())
//                    .isEqualTo(new BigDecimal("556.93"));
//            Assertions.assertThat(existingTerm.getAuditedValue()).isEqualTo(new BigDecimal("481.23"));

            // when
            budgetCalculationRepository
                    .resetAndUpdateOrCreateBudgetCalculations(
                            budget,
                            budgetCalculationService.calculate(budget));
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
//            Assertions.assertThat(existingTerm.getAuditedValue())
//                    .isEqualTo(BigDecimal.ZERO);

            Assertions.assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(125);
            Assertions.assertThat(budgetCalculationRepository.findByBudgetAndCalculationType(budget, CalculationType.AUDITED).size()).isEqualTo(50);
            Assertions.assertThat(budgetCalculationRepository.findByBudgetAndCalculationType(budget, CalculationType.AUDITED).get(0).getValue()).isEqualTo(BigDecimal.ZERO);
            Assertions.assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(5);
        }

    }

}
