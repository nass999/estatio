package org.estatio.integtests.budgetassignment;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.estatio.dom.asset.Unit;
import org.estatio.dom.asset.UnitRepository;
import org.estatio.dom.budgetassignment.ServiceChargeTerm;
import org.estatio.dom.budgetassignment.ServiceChargeTermRepository;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.charge.ChargeRepository;
import org.estatio.dom.lease.Occupancy;
import org.estatio.dom.lease.OccupancyRepository;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.charge.ChargeRefData;
import org.estatio.fixture.lease.LeaseForOxfTopModel001Gb;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceChargeTermRepositoryTest extends EstatioIntegrationTest {

    @Inject
    ServiceChargeTermRepository serviceChargeTermRepository;

    @Inject
    OccupancyRepository occupancyRepository;

    @Inject
    ChargeRepository chargeRepository;

    @Inject
    UnitRepository unitRepository;

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(final ExecutionContext executionContext) {
                executionContext.executeChild(this, new EstatioBaseLineFixture());
                executionContext.executeChild(this, new LeaseForOxfTopModel001Gb());
            }
        });
    }

    public static class FindOrCreate extends ServiceChargeTermRepositoryTest {

        ServiceChargeTerm serviceChargeTerm;

        @Test
        public void findOrCreateWorksAndIsIdempotent() throws Exception {
            // given
            Unit unit1 = unitRepository.findUnitByReference("OXF-001");
            Occupancy occupancyTopModel = occupancyRepository.findByUnit(unit1).get(0);
            Charge charge = chargeRepository.findByReference(ChargeRefData.GB_SERVICE_CHARGE);
            Integer budgetYear2000 = 2000;
            Integer budgetYear2001 = 2001;

            // when
            serviceChargeTerm = serviceChargeTermRepository.findOrCreateServiceChargeTerm(occupancyTopModel, charge, budgetYear2000);

            // then
            assertThat(serviceChargeTermRepository.allServiceChargeTerms().size()).isEqualTo(1);
            assertThat(serviceChargeTerm.getOccupancy()).isEqualTo(occupancyTopModel);
            assertThat(serviceChargeTerm.getCharge()).isEqualTo(charge);
            assertThat(serviceChargeTerm.getBudgetYear()).isEqualTo(budgetYear2000);

            // and when
            serviceChargeTermRepository.findOrCreateServiceChargeTerm(occupancyTopModel, charge, budgetYear2000);

            // then still
            assertThat(serviceChargeTermRepository.allServiceChargeTerms().size()).isEqualTo(1);

            // but when
            serviceChargeTermRepository.findOrCreateServiceChargeTerm(occupancyTopModel, charge, budgetYear2001);

            // then
            assertThat(serviceChargeTermRepository.allServiceChargeTerms().size()).isEqualTo(2);

        }

    }



}
