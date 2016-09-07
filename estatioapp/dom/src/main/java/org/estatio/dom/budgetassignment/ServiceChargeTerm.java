package org.estatio.dom.budgetassignment;

import java.math.BigDecimal;
import java.math.MathContext;
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

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.RenderType;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.estatio.dom.UdoDomainObject2;
import org.estatio.dom.apptenancy.WithApplicationTenancyProperty;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculation;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.lease.Occupancy;

import lombok.Getter;
import lombok.Setter;

@javax.jdo.annotations.PersistenceCapable(
        identityType = IdentityType.DATASTORE
)
@javax.jdo.annotations.DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@javax.jdo.annotations.Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@javax.jdo.annotations.Queries({
        @Query(
                name = "findByOccupancy", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgetassignment.ServiceChargeTerm " +
                        "WHERE occupancy == :occupancy " +
                        "ORDER BY budgetYear DESC"),
        @Query(
                name = "findByOccupancyAndChargeAndBudgetYear", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgetassignment.ServiceChargeTerm " +
                        "WHERE occupancy == :occupancy && charge == :charge && budgetYear == :budgetYear " +
                        "ORDER BY budgetYear DESC")
})
@Unique(name = "ServiceChargeTerm_occupancy_charge_budgetYear_UNQ", members = { "occupancy", "charge", "budgetYear" })
@DomainObject()
public class ServiceChargeTerm extends UdoDomainObject2<ServiceChargeTerm> implements WithApplicationTenancyProperty {

    public ServiceChargeTerm()  {
        super("occupancy, charge, budgetYear");
    }

    @Override public ApplicationTenancy getApplicationTenancy() {
        return getOccupancy().getApplicationTenancy();
    }

    @Column(name = "occupancyId", allowsNull = "false")
    @Getter @Setter
    private Occupancy occupancy;

    @Column(name = "chargeId", allowsNull = "false")
    @Getter @Setter
    private Charge charge;

    @Column(allowsNull = "false")
    @Getter @Setter
    private Integer budgetYear;

    @Column(allowsNull = "true", scale = 2)
    @Getter @Setter
    private BigDecimal calculatedBudgetedValue;

    @Column(allowsNull = "true", scale = 2)
    @Getter @Setter
    private BigDecimal calculatedAuditedValue;

    @Column(allowsNull = "true", scale = 2)
    @Getter @Setter
    private BigDecimal manualBudgetedValue;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public ServiceChargeTerm changeManualBudgetedValue(final BigDecimal manualBudgetedValue){
        setManualBudgetedValue(manualBudgetedValue);
        return this;
    }

    @Column(allowsNull = "true", scale = 2)
    @Getter @Setter
    private BigDecimal manualAuditedValue;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public ServiceChargeTerm changeManualAuditedValue(final BigDecimal manualAuditedValue){
        setManualAuditedValue(manualAuditedValue);
        return this;
    }

    @CollectionLayout(render = RenderType.EAGERLY)
    @Persistent(mappedBy = "serviceChargeTerm", dependentElement = "true")
    @Getter @Setter
    private SortedSet<BudgetCalculationLink> budgetCalculations = new TreeSet<>();

    @Action(semantics = SemanticsOf.SAFE)
    public BigDecimal getEffectiveBudgetedValue(){
        return getManualBudgetedValue()!=null ? getManualBudgetedValue() : getCalculatedBudgetedValue();
    }

    @Action(semantics = SemanticsOf.SAFE)
    public BigDecimal getEffectiveAuditedValue(){
        return getManualAuditedValue()!=null ? getManualAuditedValue() : getCalculatedAuditedValue();
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public void calculate() {
        BigDecimal calculatedBudgetedValue = BigDecimal.ZERO;
        BigDecimal calculatedAuditedValue = BigDecimal.ZERO;
        for (BudgetCalculationLink link : budgetCalculationLinkRepository.findByServiceChargeTerm(this)){
            BudgetCalculation calculation = link.getBudgetCalculation();

            switch (calculation.getCalculationType()){

            case BUDGETED:
                calculatedBudgetedValue = calculatedBudgetedValue.add(effectiveValue(calculation));
                break;

            case AUDITED:
                calculatedAuditedValue = calculatedAuditedValue.add(effectiveValue(calculation));
                break;

            }
        }
        setCalculatedBudgetedValue(calculatedBudgetedValue.setScale(2, BigDecimal.ROUND_HALF_UP));
        setCalculatedAuditedValue(calculatedAuditedValue.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    private BigDecimal effectiveValue(final BudgetCalculation calculation){
        return calculation.getValue().multiply(annualFactor(calculation));
    }

    BigDecimal annualFactor(final BudgetCalculation calculation){

        BigDecimal numberOfDaysInYear = BigDecimal.valueOf(calculation.getBudgetItemAllocation().getBudgetItem().getBudget().getBudgetYear().days());
        BigDecimal numberOfDaysInBudgetInterval = BigDecimal.valueOf(calculation.getBudgetItemAllocation().getBudgetItem().getBudget().getInterval().days());

        return numberOfDaysInBudgetInterval.divide(numberOfDaysInYear, MathContext.DECIMAL64);

    }

    @Programmatic
    public BudgetCalculationLink findOrCreateBudgetCalculationLink(final BudgetCalculation calculation){
        return budgetCalculationLinkRepository.findOrCreateBudgetCalculationLink(calculation, this);
    }

    @Inject
    BudgetCalculationLinkRepository budgetCalculationLinkRepository;

}
