package org.estatio.integtests.budget;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.apache.isis.applib.services.wrapper.HiddenException;

import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.PropertyRepository;
import org.estatio.dom.budgetassignment.ServiceChargeTermRepository;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budget.BudgetRepository;
import org.estatio.dom.budgetassignment.BudgetAssignmentContributions;
import org.estatio.dom.budgetassignment.BudgetCalculationLinkRepository;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationRepository;
import org.estatio.dom.budgeting.keytable.KeyTableRepository;
import org.estatio.dom.lease.LeaseItem;
import org.estatio.dom.lease.LeaseItemType;
import org.estatio.dom.lease.LeaseRepository;
import org.estatio.dom.lease.Occupancy;
import org.estatio.dom.lease.OccupancyRepository;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.asset.PropertyForOxfGb;
import org.estatio.fixture.budget.BudgetItemAllocationsForOxf;
import org.estatio.fixture.budget.BudgetsForOxf;
import org.estatio.fixture.lease.LeaseForOxfTopModel001Gb;
import org.estatio.fixture.lease.LeaseItemForServiceChargeBudgetedForOxfTopModel001Gb;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class BudgetIntegrationTest extends EstatioIntegrationTest {

    @Inject
    BudgetRepository budgetRepository;

    @Inject
    PropertyRepository propertyRepository;

    @Inject
    KeyTableRepository keyTableRepository;

    @Inject
    BudgetCalculationRepository budgetCalculationRepository;

    @Inject
    BudgetCalculationLinkRepository budgetCalculationLinkRepository;

    @Inject
    BudgetAssignmentContributions budgetAssignmentContributions;

    @Inject
    ServiceChargeTermRepository serviceChargeTermRepository;

    @Inject
    LeaseRepository leaseRepository;

    @Inject
    OccupancyRepository occupancyRepository;

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

    public static class RemoveBudget extends BudgetIntegrationTest {

        Property propertyOxf;
        List<Budget> budgetsForOxf;
        Budget budget2015;
        LeaseItem topmodelBudgetServiceChargeItem;
        Occupancy topmodelOccupancy;

        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Before
        public void setUp() throws Exception {
            propertyOxf = propertyRepository.findPropertyByReference(PropertyForOxfGb.REF);
            budgetsForOxf = budgetRepository.findByProperty(propertyOxf);
            budget2015 = budgetRepository.findByPropertyAndStartDate(propertyOxf, BudgetsForOxf.BUDGET_2015_START_DATE);

            // calculate and assign budget2015
            budget2015.calculate();
            budgetAssignmentContributions.assignCalculations(budget2015);

            topmodelBudgetServiceChargeItem = leaseRepository.findLeaseByReference(LeaseForOxfTopModel001Gb.REF).findFirstItemOfType(LeaseItemType.SERVICE_CHARGE_BUDGETED);
            topmodelOccupancy = occupancyRepository.findByLease(topmodelBudgetServiceChargeItem.getLease()).get(0);

        }

        @Test
        public void removeBudget() throws Exception {
            // given
            assertThat(budgetsForOxf.size()).isEqualTo(2);
            assertThat(budgetCalculationRepository.allBudgetCalculations().size()).isEqualTo(75);
            assertThat(budget2015.getKeyTables().size()).isEqualTo(2);
            assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(3);
            assertThat(serviceChargeTermRepository.findByOccupancy(topmodelOccupancy).size()).isEqualTo(1);

            // when
            budget2015.removeBudget(true);

            // then
            assertThat(budgetRepository.findByProperty(propertyOxf).size()).isEqualTo(1);
            assertThat(budgetCalculationRepository.allBudgetCalculations().size()).isEqualTo(0);
            assertThat(budgetCalculationLinkRepository.allBudgetCalculationLinks().size()).isEqualTo(0);
            assertThat(topmodelBudgetServiceChargeItem.getTerms().size()).isEqualTo(0);
            assertThat(keyTableRepository.allKeyTables().size()).isEqualTo(0);
            assertThat(serviceChargeTermRepository.findByOccupancy(topmodelOccupancy).size()).isEqualTo(0);
        }

        @Test
        public void removeBudgetOnlyInPrototypeMode() throws Exception {

            //then
            expectedException.expect(HiddenException.class);
            expectedException.expectMessage("Reason: Prototyping action not visible in production mode.");
            // when
            wrap(budget2015).removeBudget(true);

        }

    }


}
