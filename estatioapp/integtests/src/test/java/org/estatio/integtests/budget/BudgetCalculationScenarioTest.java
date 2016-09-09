package org.estatio.integtests.budget;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.PropertyRepository;
import org.estatio.dom.budgetassignment.BudgetAssignmentService;
import org.estatio.dom.budgetassignment.BudgetCalculationLinkRepository;
import org.estatio.dom.budgetassignment.ServiceChargeTerm;
import org.estatio.dom.budgetassignment.ServiceChargeTermRepository;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budget.BudgetRepository;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationRepository;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationService;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationStatus;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationType;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.asset.PropertyForOxfGb;
import org.estatio.fixture.budget.BudgetItemAllocationsForOxf;
import org.estatio.fixture.budget.BudgetsForOxf;
import org.estatio.fixture.lease.LeaseItemForServiceChargeBudgetedForOxfTopModel001Gb;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

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
            budget.calculate();

            // then
            assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(75);
            assertThat(budgetCalculationRepository.findByBudgetItemAndStatusAndCalculationType(budget.getItems().first(), BudgetCalculationStatus.TEMPORARY, BudgetCalculationType.BUDGETED).size()).isEqualTo(25);
            assertThat(serviceChargeTermRepository.allServiceChargeTerms().size()).isEqualTo(0);

        }

        public void assignBudget() throws Exception {


            // when
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
            assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(3);

            assertThat(budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getBudgetCalculation()
                    .getValue())
                    .isEqualTo(new BigDecimal("92.31"));
            assertThat(budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(1)
                    .getBudgetCalculation()
                    .getValue())
                    .isEqualTo(new BigDecimal("98.46"));
            assertThat(budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(2)
                    .getBudgetCalculation()
                    .getValue())
                    .isEqualTo(new BigDecimal("320.00"));

            ServiceChargeTerm createdTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();

            assertThat(
                    createdTerm.getCalculatedBudgetedValue())
                    .isEqualTo(new BigDecimal("510.77"));

            assertThat(
                    createdTerm.getCalculatedAuditedValue())
                    .isEqualTo(new BigDecimal("0.00"));

            assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(75);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.ASSIGNED, BudgetCalculationType.BUDGETED).size()).isEqualTo(3);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.TEMPORARY, BudgetCalculationType.BUDGETED).size()).isEqualTo(72);
            assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(3);
            assertThat(serviceChargeTermRepository.allServiceChargeTerms().size()).isEqualTo(1);

        }

        public void assignBudgetWhenUpdated() throws Exception {

            // given
            BudgetItem updatedItem = budget.getItems().first();
            updatedItem.setBudgetedValue(new BigDecimal("45000.00"));
            ServiceChargeTerm existingTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();

            // when
            budget.calculate();
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
            assertThat(existingTerm.getCalculatedBudgetedValue())
                    .isEqualTo(new BigDecimal("556.93"));
            assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(75);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.ASSIGNED, BudgetCalculationType.BUDGETED).size()).isEqualTo(3);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.TEMPORARY, BudgetCalculationType.BUDGETED).size()).isEqualTo(72);
            assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(3);
        }

        public void assignBudgetWhenAudited() throws Exception {

            // given
            BudgetItem auditedItem = budget.getItems().last();
            auditedItem.setAuditedValue(new BigDecimal("45000.00"));
            ServiceChargeTerm existingTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();
            assertThat(existingTerm.getCalculatedBudgetedValue())
                    .isEqualTo(new BigDecimal("556.93"));
            assertThat(existingTerm.getCalculatedAuditedValue()).isEqualTo(new BigDecimal("0.00"));
            assertThat(existingTerm.getBudgetYear())
                    .isEqualTo(budget.getBudgetYear().startDate().getYear());

            // when
            budget.calculate();
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
            assertThat(existingTerm.getCalculatedAuditedValue())
                    .isEqualTo(new BigDecimal("470.77"));
            assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(125);
            assertThat(budgetCalculationRepository.findByBudgetAndCalculationType(budget, BudgetCalculationType.AUDITED).size()).isEqualTo(50);
            assertThat(budgetCalculationRepository.findByBudgetAndCalculationType(budget, BudgetCalculationType.BUDGETED).size()).isEqualTo(75);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.ASSIGNED, BudgetCalculationType.BUDGETED).size()).isEqualTo(3);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.TEMPORARY, BudgetCalculationType.BUDGETED).size()).isEqualTo(72);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.ASSIGNED, BudgetCalculationType.AUDITED).size()).isEqualTo(2);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.TEMPORARY, BudgetCalculationType.AUDITED).size()).isEqualTo(48);
            assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(5);
        }

        public void assignbudgetWhenAuditedAndUpdated() throws Exception {

            // given
            BudgetItem auditedAndUpdatedItem = budget.getItems().last();
            auditedAndUpdatedItem.setAuditedValue(new BigDecimal("46000.00"));
            ServiceChargeTerm existingTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();
            assertThat(existingTerm.getCalculatedBudgetedValue())
                    .isEqualTo(new BigDecimal("556.93"));
            assertThat(existingTerm.getCalculatedAuditedValue()).isEqualTo(new BigDecimal("470.77"));

            // when
            budget.calculate();
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
            assertThat(existingTerm.getCalculatedAuditedValue())
                    .isEqualTo(new BigDecimal("481.23"));

            assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(125);
            assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(5);
        }

        public void assignBudgetWhenAuditedAndUpdatedWithEmptyAuditedValueOnBudgetItem() throws Exception {

            // given
            BudgetItem auditedAndUpdatedItem = budget.getItems().last();
            auditedAndUpdatedItem.setAuditedValue(null);
            ServiceChargeTerm existingTerm = budgetCalculationLinkRepository
                    .allBudgetCalculationLinks().get(0)
                    .getServiceChargeTerm();
            assertThat(existingTerm.getCalculatedBudgetedValue())
                    .isEqualTo(new BigDecimal("556.93"));
            assertThat(existingTerm.getCalculatedAuditedValue()).isEqualTo(new BigDecimal("481.23"));

            // when
            budget.calculate();
            budgetAssignmentService.assignBudgetCalculations(budget);

            // then
            assertThat(existingTerm.getCalculatedAuditedValue())
                    .isEqualTo(new BigDecimal("0.00"));

            assertThat(budgetCalculationRepository.findByBudget(budget).size()).isEqualTo(75);
            assertThat(budgetCalculationRepository.findByBudgetAndCalculationType(budget, BudgetCalculationType.AUDITED).size()).isEqualTo(0);
            assertThat(budgetCalculationRepository.findByBudgetAndCalculationType(budget, BudgetCalculationType.BUDGETED).size()).isEqualTo(75);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.ASSIGNED, BudgetCalculationType.BUDGETED).size()).isEqualTo(3);
            assertThat(budgetCalculationRepository.findByBudgetAndStatusAndCalculationType(budget, BudgetCalculationStatus.TEMPORARY, BudgetCalculationType.BUDGETED).size()).isEqualTo(72);
            assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(3);
        }

    }

}
