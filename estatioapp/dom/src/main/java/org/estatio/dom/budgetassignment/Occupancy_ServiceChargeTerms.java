package org.estatio.dom.budgetassignment;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.RenderType;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budget.BudgetRepository;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.lease.Occupancy;

@Mixin
public class Occupancy_ServiceChargeTerms {

    private final Occupancy occupancy;

    public Occupancy_ServiceChargeTerms(Occupancy occupancy){
        this.occupancy = occupancy;
    }

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(contributed = Contributed.AS_ASSOCIATION)
    @CollectionLayout(render = RenderType.EAGERLY)
    public List<ServiceChargeTerm> serviceChargeTerms(){
        return serviceChargeTermRepository.findByOccupancy(occupancy);
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    public ServiceChargeTerm findOrCreateServiceChargeTerm(final Charge charge, final Budget budget){
        return serviceChargeTermRepository.findOrCreateServiceChargeTerm(occupancy, charge, budget.getStartDate().getYear());
    }

    public List<Budget> choices1FindOrCreateServiceChargeTerm(final Charge charge, final Budget budget){
        return budgetRepository.findByProperty(occupancy.getUnit().getProperty());
    }

    @Inject
    private ServiceChargeTermRepository serviceChargeTermRepository;

    @Inject
    private BudgetRepository budgetRepository;

}
