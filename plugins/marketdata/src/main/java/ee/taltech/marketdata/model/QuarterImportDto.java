package ee.taltech.marketdata.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class QuarterImportDto {
    Timestamp dateImported = null;
    String sourceOfData;
    Integer year;
    Integer quarter;
    List<QuarterDto> quarters;
}
