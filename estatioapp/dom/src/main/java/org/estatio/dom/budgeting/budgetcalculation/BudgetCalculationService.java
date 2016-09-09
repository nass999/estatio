package org.estatio.dom.budgeting.budgetcalculation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;

import org.estatio.dom.budgetassignment.BudgetCalculationLinkRepository;
import org.estatio.dom.budgeting.Distributable;
import org.estatio.dom.budgeting.DistributionService;
import org.estatio.dom.budgeting.allocation.BudgetItemAllocation;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.dom.budgeting.keyitem.KeyItem;

@DomainService(nature = NatureOfService.DOMAIN)
public class BudgetCalculationService {

    public List<BudgetCalculationResult> calculate(final Budget budget) {

        removeTemporaryCalculations(budget);

        List<BudgetCalculationResult> result = new ArrayList<>();
        for (BudgetItem budgetItem : budget.getItems()) {

            result.addAll(calculate(budgetItem));

        }

        return result;
    }

    public void removeTemporaryCalculations(final Budget budget) {
        List<BudgetCalculation> calcsToBeRemoved = new ArrayList<>();
        calcsToBeRemoved.addAll(budgetCalculationRepository.findByBudgetAndCalculationType(budget, CalculationType.BUDGETED_TEMP));
        calcsToBeRemoved.addAll(budgetCalculationRepository.findByBudgetAndCalculationType(budget, CalculationType.AUDITED_TEMP));
        for (BudgetCalculation calc : calcsToBeRemoved){
            calc.remove();
        }
    }

    private List<BudgetCalculationResult> calculate(final BudgetItem budgetItem) {

        List<BudgetCalculationResult> result = new ArrayList<>();
        for (BudgetItemAllocation itemAllocation : budgetItem.getBudgetItemAllocations()) {

            result.addAll(calculate(itemAllocation));

        }

        return result;
    }

    private List<BudgetCalculationResult> calculate(final BudgetItemAllocation itemAllocation) {

        List<BudgetCalculationResult> results = new ArrayList<>();

        BigDecimal budgetedTotal = percentageOf(itemAllocation.getBudgetItem().getBudgetedValue(), itemAllocation.getPercentage());
        results.addAll(calculateForTotalAndType(itemAllocation, budgetedTotal, CalculationType.BUDGETED_TEMP));

        if (itemAllocation.getBudgetItem().getAuditedValue() != null){
            BigDecimal auditedTotal = percentageOf(itemAllocation.getBudgetItem().getAuditedValue(), itemAllocation.getPercentage());
            results.addAll(calculateForTotalAndType(itemAllocation,auditedTotal,CalculationType.AUDITED_TEMP));
        }

        return results;
    }

    private List<BudgetCalculationResult> calculateForTotalAndType(final BudgetItemAllocation itemAllocation, final BigDecimal total, final CalculationType calculationType) {

        List<Distributable> results = new ArrayList<>();

        BigDecimal keySum = itemAllocation.getKeyTable().getKeyValueMethod().keySum(itemAllocation.getKeyTable());

        for (KeyItem keyItem : itemAllocation.getKeyTable().getItems()) {

            BudgetCalculationResult calculationResult;

            // case all values in keyTable are zero
            if (keySum.compareTo(BigDecimal.ZERO) == 0) {
                calculationResult = new BudgetCalculationResult(
                        itemAllocation,
                        keyItem,
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        calculationType);
            } else {
                calculationResult = new BudgetCalculationResult(
                        itemAllocation,
                        keyItem,
                        BigDecimal.ONE,
                        total.multiply(keyItem.getValue()).
                                divide(keySum, MathContext.DECIMAL64),
                        calculationType);

            }

            results.add(calculationResult);
        }

        DistributionService distributionService = new DistributionService();
        distributionService.distribute(results, total, 2);

        return (List<BudgetCalculationResult>) (Object) results;

    }

    private BigDecimal percentageOf(final BigDecimal value, final BigDecimal percentage) {
        return value
                .multiply(percentage)
                .divide(new BigDecimal(100), MathContext.DECIMAL64);
    }


    @Inject
    private BudgetCalculationRepository budgetCalculationRepository;

    @Inject
    private BudgetCalculationLinkRepository budgetCalculationLinkRepository;


}
