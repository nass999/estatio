package org.estatio.dom.budgetassignment;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.RestrictTo;

@DomainService(nature = NatureOfService.VIEW_MENU_ONLY)
@DomainServiceLayout(menuBar = DomainServiceLayout.MenuBar.PRIMARY, named = "Budgets")
public class ServiceChargeTermMenu {


    @Action(restrictTo = RestrictTo.PROTOTYPING)
    public List<ServiceChargeTerm> allServiceChargeTerms(){
        return serviceChargeTermRepository.allServiceChargeTerms();
    }

    @Inject
    private ServiceChargeTermRepository serviceChargeTermRepository;

}
