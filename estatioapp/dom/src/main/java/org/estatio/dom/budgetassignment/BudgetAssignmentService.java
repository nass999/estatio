package org.estatio.dom.budgetassignment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;

import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculation;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationRepository;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.lease.Occupancy;
import org.estatio.dom.lease.OccupancyRepository;

@DomainService(nature = NatureOfService.DOMAIN)
public class BudgetAssignmentService {


    public List<BudgetCalculationLink> assignBudgetCalculations(final Budget budget) {

        List<BudgetCalculationLink> result = new ArrayList<>();

        for (Charge invoiceCharge : budget.getInvoiceCharges()) {

            List<BudgetCalculation> calculationsForCharge = budgetCalculationRepository.findByBudgetAndCharge(budget, invoiceCharge);

            for (Occupancy occupancy : occupancyRepository.occupanciesByPropertyAndInterval(budget.getProperty(), budget.getInterval())) {

                List<BudgetCalculation> budgetCalculationsForOccupancy = calculationsForOccupancy(calculationsForCharge, occupancy);

                // find or create service charge term and assign calculations
                if (budgetCalculationsForOccupancy.size()>0){

                    ServiceChargeTerm serviceChargeTerm = serviceChargeTermRepository.findOrCreateServiceChargeTerm(occupancy, invoiceCharge, budget.getBudgetYear());
                    for (BudgetCalculation budgetCalculation : budgetCalculationsForOccupancy){
                        budgetCalculationLinkRepository.findOrCreateBudgetCalculationLink(budgetCalculation, serviceChargeTerm);
                    }
                    serviceChargeTerm.calculate();

                }


            }

        }

        return result;
    }


    private List<BudgetCalculation> calculationsForOccupancy(final List<BudgetCalculation> calculationList, final Occupancy occupancy){
        List<BudgetCalculation> result = new ArrayList<>();

        for (BudgetCalculation budgetCalculation : calculationList){

            if (budgetCalculation.getKeyItem().getUnit().equals(occupancy.getUnit())){
                result.add(budgetCalculation);
            }
        }

        return result;
    }


    @Inject
    private BudgetCalculationRepository budgetCalculationRepository;

    @Inject
    private BudgetCalculationLinkRepository budgetCalculationLinkRepository;

    @Inject
    private OccupancyRepository occupancyRepository;

    @Inject
    private ServiceChargeTermRepository serviceChargeTermRepository;


}
