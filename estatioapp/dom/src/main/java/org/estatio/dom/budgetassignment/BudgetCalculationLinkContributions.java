package org.estatio.dom.budgetassignment;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.SemanticsOf;

@DomainService(nature = NatureOfService.VIEW_CONTRIBUTIONS_ONLY)
public class BudgetCalculationLinkContributions {


    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(contributed = Contributed.AS_ASSOCIATION)
    public List<BudgetCalculationLink> budgetCalculations(final ServiceChargeTerm serviceChargeTerm){
        return budgetCalculationLinkRepository.findByServiceChargeTerm(serviceChargeTerm);
    }

    @Inject
    private BudgetCalculationLinkRepository budgetCalculationLinkRepository;

}
