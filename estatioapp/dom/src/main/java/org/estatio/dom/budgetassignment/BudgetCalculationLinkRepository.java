package org.estatio.dom.budgetassignment;

import java.util.List;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;

import org.estatio.dom.UdoDomainRepositoryAndFactory;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculation;

@DomainService(repositoryFor = BudgetCalculationLink.class, nature = NatureOfService.DOMAIN)
public class BudgetCalculationLinkRepository extends UdoDomainRepositoryAndFactory<BudgetCalculationLink> {

    public BudgetCalculationLinkRepository() {
        super(BudgetCalculationLinkRepository.class, BudgetCalculationLink.class);
    }

    public BudgetCalculationLink findOrCreateBudgetCalculationLink(
            final BudgetCalculation calculation,
            final ServiceChargeTerm term) {

        BudgetCalculationLink budgetCalculationLink = findByBudgetCalculationAndServiceChargeTerm(calculation, term);

        return budgetCalculationLink == null ? createBudgetCalculationLink(calculation,term) : budgetCalculationLink;

    }

    public BudgetCalculationLink createBudgetCalculationLink(
            final BudgetCalculation budgetCalculation,
            final ServiceChargeTerm serviceChargeTerm) {

        BudgetCalculationLink budgetCalculationLink = newTransientInstance(BudgetCalculationLink.class);
        budgetCalculationLink.setBudgetCalculation(budgetCalculation);
        budgetCalculationLink.setServiceChargeTerm(serviceChargeTerm);

        persistIfNotAlready(budgetCalculationLink);

        return budgetCalculationLink;

    }

    public List<BudgetCalculationLink> allBudgetCalculationLinks(){
        return allInstances();
    }

    public List<BudgetCalculationLink> findByServiceChargeTerm(final ServiceChargeTerm serviceChargeTerm) {
        return allMatches("findByServiceChargeTerm", "serviceChargeTerm", serviceChargeTerm);
    }

    public BudgetCalculationLink findByBudgetCalculationAndServiceChargeTerm(final BudgetCalculation budgetCalculation, final ServiceChargeTerm serviceChargeTerm) {
        return uniqueMatch("findByBudgetCalculationAndServiceChargeTerm", "budgetCalculation", budgetCalculation, "serviceChargeTerm", serviceChargeTerm);
    }


}
