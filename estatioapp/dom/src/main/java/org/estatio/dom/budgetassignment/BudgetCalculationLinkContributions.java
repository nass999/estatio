package org.estatio.dom.budgetassignment;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculation;

@DomainService(nature = NatureOfService.VIEW_CONTRIBUTIONS_ONLY)
public class BudgetCalculationLinkContributions {

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(contributed = Contributed.AS_ASSOCIATION)
    public List<BudgetCalculationLink> serviceChargeTerms(final BudgetCalculation budgetCalculation){
        return budgetCalculationLinkRepository.findByBudgetCalculation(budgetCalculation);
    }

    @Inject
    private BudgetCalculationLinkRepository budgetCalculationLinkRepository;
}
