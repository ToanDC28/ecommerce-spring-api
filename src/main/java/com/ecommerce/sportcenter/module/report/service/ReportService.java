package com.ecommerce.sportcenter.module.report.service;

import com.ecommerce.sportcenter.module.report.dto.response.DebtRow;
import com.ecommerce.sportcenter.module.report.dto.response.ProfitResponse;
import com.ecommerce.sportcenter.module.report.dto.response.RevenueByTypeResponse;
import com.ecommerce.sportcenter.module.report.dto.response.RevenuePoint;
import com.ecommerce.sportcenter.module.report.dto.response.SalaryCostResponse;
import com.ecommerce.sportcenter.module.report.dto.response.StockValueResponse;
import com.ecommerce.sportcenter.module.report.dto.response.TopMaterialRow;
import com.ecommerce.sportcenter.module.report.dto.response.WorkOrderProfitRow;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    List<RevenuePoint> revenue(LocalDate from, LocalDate to, String groupBy);

    RevenueByTypeResponse revenueByType(LocalDate from, LocalDate to);

    List<TopMaterialRow> topMaterials(int limit, LocalDate from, LocalDate to);

    StockValueResponse stockValue();

    List<DebtRow> supplierDebt();

    List<DebtRow> customerDebt();

    SalaryCostResponse salaryCost(String period);

    ProfitResponse profit(LocalDate from, LocalDate to);

    List<WorkOrderProfitRow> workOrderProfit(LocalDate from, LocalDate to);
}
