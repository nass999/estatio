/*
 *
 *  Copyright 2012-2015 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.dom.budgeting.budget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.VersionStrategy;

import org.joda.time.LocalDate;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.RenderType;
import org.apache.isis.applib.annotation.RestrictTo;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.Where;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.estatio.dom.UdoDomainObject2;
import org.estatio.dom.WithIntervalMutable;
import org.estatio.dom.apptenancy.WithApplicationTenancyProperty;
import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.UnitRepository;
import org.estatio.dom.budgetassignment.BudgetCalculationLink;
import org.estatio.dom.budgetassignment.ServiceChargeTerm;
import org.estatio.dom.budgetassignment.ServiceChargeTermRepository;
import org.estatio.dom.budgeting.allocation.BudgetItemAllocation;
import org.estatio.dom.budgeting.api.BudgetItemCreator;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationRepository;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationService;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.dom.budgeting.budgetitem.BudgetItemRepository;
import org.estatio.dom.budgeting.keytable.FoundationValueType;
import org.estatio.dom.budgeting.keytable.KeyTable;
import org.estatio.dom.budgeting.keytable.KeyTableRepository;
import org.estatio.dom.budgeting.keytable.KeyValueMethod;
import org.estatio.dom.budgeting.viewmodels.BudgetOverview;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.lease.Occupancy;
import org.estatio.dom.lease.OccupancyRepository;
import org.estatio.dom.utils.TitleBuilder;
import org.estatio.dom.valuetypes.LocalDateInterval;

import lombok.Getter;
import lombok.Setter;

@javax.jdo.annotations.PersistenceCapable(
        identityType = IdentityType.DATASTORE
        //      ,schema = "budget"
)
@javax.jdo.annotations.DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@javax.jdo.annotations.Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@javax.jdo.annotations.Queries({
        @Query(
                name = "findByProperty", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgeting.budget.Budget " +
                        "WHERE property == :property " +
                        "ORDER BY startDate DESC"),
        @Query(
                name = "findByPropertyAndStartDate", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgeting.budget.Budget " +
                        "WHERE property == :property && startDate == :startDate")
})
@Unique(name = "Budget_property_startDate_UNQ", members = { "property", "startDate" })
@DomainObject()
public class Budget extends UdoDomainObject2<Budget>
        implements WithIntervalMutable<Budget>, WithApplicationTenancyProperty, BudgetItemCreator {

    public Budget() {
        super("property, startDate");
    }

    public String title() {
        return TitleBuilder.start()
                .withParent(getProperty())
                .withName(getInterval())
                .toString();
    }

    @Column(name = "propertyId", allowsNull = "false")
    @PropertyLayout(hidden = Where.PARENTED_TABLES)
    @Getter @Setter
    private Property property;

    @Column(allowsNull = "true") // done because of inherited implementation WithStartDate
    @Getter @Setter
    private LocalDate startDate;

    @Column(allowsNull = "true")
    @Getter @Setter
    private LocalDate endDate;

    @Action(semantics = SemanticsOf.SAFE)
    public LocalDateInterval getBudgetYear() {
        return new LocalDateInterval(new LocalDate(getStartDate().getYear(),01,01), new LocalDate(new LocalDate(getStartDate().getYear(),12,31)));
    }
    // ////////////////////////////////////////

    @Programmatic
    public LocalDateInterval getInterval() {
        return LocalDateInterval.including(getStartDate(), getEndDate());
    }

    @Programmatic
    public LocalDateInterval getEffectiveInterval() {
        return getInterval();
    }

    public boolean isCurrent() {
        return isActiveOn(getClockService().now());
    }

    private boolean isActiveOn(final LocalDate date) {
        return LocalDateInterval.including(this.getStartDate(), this.getEndDate()).contains(date);
    }

    private WithIntervalMutable.Helper<Budget> changeDates = new WithIntervalMutable.Helper<>(this);

    WithIntervalMutable.Helper<Budget> getChangeDates() {
        return changeDates;
    }

    @Override
    @Action(semantics = SemanticsOf.IDEMPOTENT, hidden = Where.EVERYWHERE)
    public Budget changeDates(
            final LocalDate startDate,
            final @Parameter(optionality = Optionality.OPTIONAL) LocalDate endDate) {
        return getChangeDates().changeDates(startDate, endDate);
    }

    @Override
    public LocalDate default0ChangeDates() {
        return getChangeDates().default0ChangeDates();
    }

    @Override
    public LocalDate default1ChangeDates() {
        return getChangeDates().default1ChangeDates();
    }

    @Override
    public String validateChangeDates(
            final LocalDate startDate,
            final LocalDate endDate) {

        return "Dates should not be changed.";
    }

    @CollectionLayout(render = RenderType.EAGERLY)
    @Persistent(mappedBy = "budget", dependentElement = "true")
    @Getter @Setter
    private SortedSet<BudgetItem> items = new TreeSet<>();

    @CollectionLayout(render = RenderType.EAGERLY)
    @Persistent(mappedBy = "budget", dependentElement = "true")
    @Getter @Setter
    private SortedSet<KeyTable> keyTables = new TreeSet<>();

    @PropertyLayout(hidden = Where.EVERYWHERE)
    @Override public ApplicationTenancy getApplicationTenancy() {
        return getProperty().getApplicationTenancy();
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    @MemberOrder(name = "items", sequence = "1")
    public BudgetItem newBudgetItem(
            final BigDecimal budgetedValue,
            final Charge charge) {
        return budgetItemRepository.newBudgetItem(this, budgetedValue, charge);
    }

    public String validateNewBudgetItem(
            final BigDecimal budgetedValue,
            final Charge charge) {
        return budgetItemRepository.validateNewBudgetItem(this,budgetedValue,charge);
    }

    /*
    * TODO: revisit after refactoring
    * */
    @Action(restrictTo = RestrictTo.PROTOTYPING ,semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
    public void removeBudget(
            @ParameterLayout(named = "This will delete the budget and all associated data including keytables, calculations and generated service charge terms. (You may consider downloading the budget and the keytables beforehand.) Are you sure?")
            final boolean areYouSure
    ){

        /* delete calculation links and service charge terms if needed */
        for (Occupancy occupancy : occupancyRepository.occupanciesByPropertyAndInterval(getProperty(),getInterval())) {
            for (ServiceChargeTerm term : serviceChargeTermRepository.findByOccupancy(occupancy)){
                if (term.getBudgetYear()==getBudgetYear().startDate().getYear()){
                    for (BudgetCalculationLink link : term.getBudgetCalculationLinks()){
                        if (link.getBudgetCalculation()
                                .getBudgetItemAllocation()
                                .getBudgetItem()
                                .getBudget().equals(this)){
                            getContainer().remove(link);
                            getContainer().flush();
                        }
                    }
                    if (term.getBudgetCalculationLinks().size()==0){
                        getContainer().remove(term);
                        getContainer().flush();
                    }
                }
            }
        }

        remove(this);
    }

    public String validateRemoveBudget(final boolean areYouSure){
        return areYouSure ? null : "Please confirm";
    }

    @Action(restrictTo = RestrictTo.PROTOTYPING, semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
    @ActionLayout()
    public Budget removeAllBudgetItems() {
        for (BudgetItem budgetItem : this.getItems()) {
            getContainer().remove(budgetItem);
            getContainer().flush();
        }

        return this;
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    public KeyTable createKeyTable(
            final String name,
            final FoundationValueType foundationValueType,
            final KeyValueMethod keyValueMethod,
            final Integer numberOfDigits) {
        return keyTableRepository.newKeyTable(this, name, foundationValueType, keyValueMethod, numberOfDigits);
    }

    public String validateCreateKeyTable(
            final String name,
            final FoundationValueType foundationValueType,
            final KeyValueMethod keyValueMethod,
            final Integer numberOfDigits) {
        return keyTableRepository.validateNewKeyTable(this, name, foundationValueType, keyValueMethod, numberOfDigits);
    }

    @Programmatic
    public BigDecimal getTotalBudgetedValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (BudgetItem item : getItems()) {
            total = total.add(item.getBudgetedValue());
        }
        return total;
    }

    @Programmatic
    public BigDecimal getTotalAuditedValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (BudgetItem item : getItems()) {
            if (item.getAuditedValue() != null) {
                total = total.add(item.getAuditedValue());
            }
        }
        return total;
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT_ARE_YOU_SURE)
    @ActionLayout()
    public Budget calculate(){
        budgetCalculationService.calculate(this);
        return this;
    }

    @Action(semantics = SemanticsOf.SAFE)
    public BudgetOverview budgetOverview() {
        return new BudgetOverview(this);
    }

    @Programmatic
    public List<Charge> getInvoiceCharges() {
        List<Charge> charges = new ArrayList<>();
        for (BudgetItem budgetItem : getItems()) {
            for (BudgetItemAllocation allocation : budgetItem.getBudgetItemAllocations()) {
                if (!charges.contains(allocation.getCharge())) {
                    charges.add(allocation.getCharge());
                }
            }
        }
        return charges;
    }

    @Override
    @Programmatic
    public BudgetItem findOrCreateBudgetItem(
            final Charge budgetItemCharge,
            final BigDecimal budgetedValue) {
        return budgetItemRepository.findOrCreateBudgetItem(this, budgetItemCharge, budgetedValue);
    }

    @Override
    @Programmatic
    public BudgetItem updateOrCreateBudgetItem(
            final Charge budgetItemCharge,
            final BigDecimal budgetedValue,
            final BigDecimal auditedValue) {
        return budgetItemRepository.updateOrCreateBudgetItem(this, budgetItemCharge, budgetedValue, auditedValue);
    }

    @Inject
    private BudgetItemRepository budgetItemRepository;

    @Inject
    private BudgetRepository budgetRepository;

    @Inject
    private UnitRepository unitRepository;

    @Inject
    private OccupancyRepository occupancyRepository;

    @Inject
    private BudgetCalculationRepository budgetCalculationRepository;

    @Inject
    private KeyTableRepository keyTableRepository;

    @Inject
    private ServiceChargeTermRepository serviceChargeTermRepository;

    @Inject
    private BudgetCalculationService budgetCalculationService;

}
