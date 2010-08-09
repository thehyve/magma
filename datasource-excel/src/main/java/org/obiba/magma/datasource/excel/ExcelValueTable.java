package org.obiba.magma.datasource.excel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.obiba.magma.Initialisable;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.NoSuchValueSetException;
import org.obiba.magma.Timestamps;
import org.obiba.magma.ValueSet;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.datasource.excel.support.ExcelUtil;
import org.obiba.magma.datasource.excel.support.VariableConverter;
import org.obiba.magma.support.AbstractValueTable;
import org.obiba.magma.support.VariableEntityBean;
import org.obiba.magma.support.VariableEntityProvider;
import org.obiba.magma.type.TextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class ExcelValueTable extends AbstractValueTable implements Initialisable {

  private static final Logger log = LoggerFactory.getLogger(ExcelValueTable.class);

  private Sheet valueTableSheet;

  /** Maps a variable's name to its Column index valueTableSheet */
  private final Map<String, Integer> variableColumns = Maps.newHashMap();

  private VariableConverter converter;

  public ExcelValueTable(ExcelDatasource excelDatasource, String name, String entityType) {
    super(excelDatasource, name);
    setVariableEntityProvider(new ExcelVariableEntityProvider(entityType));
    converter = new VariableConverter(this);
  }

  @Override
  public void initialise() {
    super.initialise();
    try {
      readVariables();
    } catch(RuntimeException e) {
      throw e;
    } catch(Exception e) {
      throw new MagmaRuntimeException(e);
    }
  }

  @Override
  public ExcelDatasource getDatasource() {
    return (ExcelDatasource) super.getDatasource();
  }

  @Override
  public ValueSet getValueSet(VariableEntity entity) throws NoSuchValueSetException {
    throw new UnsupportedOperationException("getValueSet not supported");
  }

  public VariableConverter getVariableConverter() {
    return converter;
  }

  int findVariableColumn(Variable variable) {
    // Lookup in column cache
    Integer columnIndex = this.variableColumns.get(variable.getName());
    if(columnIndex != null) {
      return columnIndex;
    }
    Row variableNameRow = getValueTableSheet().getRow(0);
    for(int i = 0; i < variableNameRow.getPhysicalNumberOfCells(); i++) {
      Cell cell = variableNameRow.getCell(i);
      if(ExcelUtil.getCellValueAsString(cell).equals(variable.getName())) {
        this.variableColumns.put(variable.getName(), i);
        return i;
      }
    }
    return -1;
  }

  int getVariableColumn(Variable variable) {
    int column = findVariableColumn(variable);
    if(column == -1) {
      // Add it
      Row variableNameRow = getValueTableSheet().getRow(0);
      Cell variableColumn = variableNameRow.createCell(variableNameRow.getPhysicalNumberOfCells(), Cell.CELL_TYPE_STRING);
      ExcelUtil.setCellValue(variableColumn, TextType.get(), variable.getName());
      variableColumn.setCellStyle(getDatasource().getHeaderCellStyle());
      column = variableColumn.getColumnIndex();
      this.variableColumns.put(variable.getName(), column);
    }
    return column;
  }

  /**
   * Get the value sheet. Create it if necessary.
   * @return
   */
  Sheet getValueTableSheet() {
    if(valueTableSheet == null) {
      valueTableSheet = getDatasource().createSheetIfNotExist(getName());

      if(valueTableSheet.getPhysicalNumberOfRows() <= 0) {
        valueTableSheet.createRow(0);
      }

      // First column is for storing the Variable Entity identifiers
      Cell cell = valueTableSheet.getRow(0).createCell(0);
      ExcelUtil.setCellValue(cell, TextType.get(), "Entity ID");
      cell.setCellStyle(getDatasource().getHeaderCellStyle());
    }
    return valueTableSheet;
  }

  /**
   * Read the variables either from the Variables sheet or from sheet headers.
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void readVariables() throws FileNotFoundException, IOException {
    if(isFromVariablesSheet()) {
      // read variables from Variables sheet
      readVariablesFromVariablesSheet();
    } else {
      // read variables from the sheet headers
      readVariablesFromTableSheet();
    }
  }

  /**
   * Variables are defined by column names and value type is text. First column is assumed to be participant identifier.
   */
  private void readVariablesFromTableSheet() {
    Sheet sheet = getDatasource().getSheet(getName());
    if(sheet != null) {
      Row variableNameRow = getValueTableSheet().getRow(0);
      for(int i = 1; i < variableNameRow.getPhysicalNumberOfCells(); i++) {
        // variable is just a name and with text values
        Cell cell = variableNameRow.getCell(i);
        String name = ExcelUtil.getCellValueAsString(cell);
        Variable.Builder variableBuilder = Variable.Builder.newVariable(name, TextType.get(), getEntityType());
        addVariableValueSource(new ExcelVariableValueSource(variableBuilder.build()));
      }
    }
  }

  /**
   * Variables are read from the variables sheet.
   */
  private void readVariablesFromVariablesSheet() {
    Sheet variablesSheet = getDatasource().getVariablesSheet();

    int variableRowCount = variablesSheet.getPhysicalNumberOfRows();
    for(int i = 1; i < variableRowCount; i++) {
      Row variableRow = variablesSheet.getRow(i);
      if(converter.isVariableRow(variableRow)) {
        Variable variable = converter.unmarshall(variableRow);
        addVariableValueSource(new ExcelVariableValueSource(variable));
      }
    }
  }

  private boolean isFromVariablesSheet() {
    Sheet varSheet = getDatasource().getVariablesSheet();
    return varSheet != null && varSheet.getPhysicalNumberOfRows() > 0;
  }

  private class ExcelVariableEntityProvider implements VariableEntityProvider {

    private String entityType;

    public ExcelVariableEntityProvider(String entityType) {
      if(entityType == null || entityType.trim().length() == 0) {
        this.entityType = "Participant";
      } else {
        this.entityType = entityType.trim();
      }
    }

    @Override
    public String getEntityType() {
      return entityType;
    }

    @Override
    public Set<VariableEntity> getVariableEntities() {
      ImmutableSet.Builder<VariableEntity> entitiesBuilder = ImmutableSet.builder();

      if(valueTableSheet != null) {
        for(int i = 1; i < valueTableSheet.getPhysicalNumberOfRows(); i++) {
          Cell cell = valueTableSheet.getRow(i).getCell(0);
          entitiesBuilder.add(new VariableEntityBean(entityType, cell.getStringCellValue()));
        }
      }

      return entitiesBuilder.build();
    }

    @Override
    public boolean isForEntityType(String entityType) {
      return getEntityType().equals(entityType);
    }

  }

  @Override
  public Timestamps getTimestamps(ValueSet valueSet) {
    return getDatasource().getTimestamps();
  }
}
