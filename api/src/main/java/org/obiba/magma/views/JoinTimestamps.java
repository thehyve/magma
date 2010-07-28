package org.obiba.magma.views;

import java.util.Arrays;
import java.util.List;

import org.obiba.magma.Timestamps;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSet;
import org.obiba.magma.ValueTable;
import org.obiba.magma.support.NullTimestamps;
import org.obiba.magma.type.DateTimeType;
import org.obiba.magma.views.JoinTable.JoinedValueSet;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

class JoinTimestamps implements Timestamps {

  private final Iterable<Timestamps> timestamps;

  JoinTimestamps(final ValueSet valueSet, final List<ValueTable> tables) {
    this.timestamps = Iterables.transform(tables, new Function<ValueTable, Timestamps>() {

      @Override
      public Timestamps apply(ValueTable table) {
        ValueSet tableValueSet = ((JoinedValueSet) valueSet).getInnerTableValueSet(table);
        if(tableValueSet != null) {
          return table.getTimestamps(tableValueSet);
        } else {
          return NullTimestamps.INSTANCE;
        }
      }

    });
  }

  @Override
  public Value getCreated() {
    return getTimestamp(ExtractCreatedFunction.INSTANCE, true);
  }

  @Override
  public Value getLastUpdate() {
    return getTimestamp(ExtractLastUpdateFunction.INSTANCE, false);
  }

  /**
   * Extracts all the timestamp values, sorts them and returns either the earliest value or the latest value depending
   * on the {@code earliest} argument.
   * 
   * @param extractTimestampFunction the function used to extract the timestamp to work with (either created or
   * lastUpdate)
   * @param earliest whether to return the earliest value or the latest value
   * @return the earliest/latest timestamp from the set of timestamps. This method never returns null.
   */
  private Value getTimestamp(Function<Timestamps, Value> extractTimestampFunction, boolean earliest) {
    Iterable<Value> created = Iterables.transform(timestamps, extractTimestampFunction);
    Value[] values = Iterables.toArray(getNonNullValues(created), Value.class);
    if(values.length > 0) {
      Arrays.sort(values);
      return values[earliest ? 0 : values.length - 1];
    } else {
      return DateTimeType.get().nullValue();
    }
  }

  /**
   * Filters Value instances that are null or that isNull() returns true out of {@code values}.
   * @param values
   * @return
   */
  private Iterable<Value> getNonNullValues(Iterable<Value> values) {
    return Iterables.filter(values, new Predicate<Value>() {

      @Override
      public boolean apply(Value value) {
        return value != null ? (value.isNull() == false) : false;
      }

    });

  }

  private static final class ExtractLastUpdateFunction implements Function<Timestamps, Value> {

    private static final ExtractLastUpdateFunction INSTANCE = new ExtractLastUpdateFunction();

    @Override
    public Value apply(Timestamps from) {
      return from.getLastUpdate();
    }

  }

  private static final class ExtractCreatedFunction implements Function<Timestamps, Value> {

    private static final ExtractCreatedFunction INSTANCE = new ExtractCreatedFunction();

    @Override
    public Value apply(Timestamps from) {
      return from.getCreated();
    }
  }
}