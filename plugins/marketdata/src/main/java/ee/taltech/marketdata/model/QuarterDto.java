package ee.taltech.marketdata.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class QuarterDto {
    String entryId;
    String partyId;
    Timestamp dateImported = null;
    String sourceOfData;
    Integer year;
    Integer month;
    Integer quarter;
    BigDecimal stateTaxes;
    BigDecimal workforceTaxes;
    BigDecimal revenue;
    Integer numberOfEmployees;
}
