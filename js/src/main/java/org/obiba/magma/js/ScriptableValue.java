package org.obiba.magma.js;

import java.util.Date;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.obiba.magma.MagmaDate;
import org.obiba.magma.Value;
import org.obiba.magma.ValueType;
import org.obiba.magma.type.BooleanType;
import org.obiba.magma.type.DateType;
import org.obiba.magma.type.TextType;

/**
 * A {@code Scriptable} implementation for {@code Value} objects.
 * <p/>
 * Methods available on the {@code ScriptableValue} instances are built by the {@code ScriptableValuePrototypeFactory}.
 * It allows extending the methods of {@code ScriptableValue}.
 *
 * @see ScriptableValuePrototypeFactory
 */
public class ScriptableValue extends ScriptableObject {

  private static final long serialVersionUID = -4342110775412157728L;

  static final String VALUE_CLASS_NAME = "Value";

  private Value value;

  private String unit;

  /**
   * No-arg ctor for building the prototype
   */
  ScriptableValue() {

  }

  public ScriptableValue(Scriptable scope, Value value, String unit) {
    super(scope, ScriptableObject.getClassPrototype(scope, VALUE_CLASS_NAME));
    if(value == null) {
      throw new NullPointerException("value cannot be null");
    }
    this.value = value;
    this.unit = unit;
  }

  public ScriptableValue(Scriptable scope, Value value) {
    this(scope, value, null);
  }

  public boolean hasUnit() {
    return unit != null;
  }

  public String getUnit() {
    return unit;
  }

  @Override
  public String getClassName() {
    return VALUE_CLASS_NAME;
  }

  @Override
  public Object getDefaultValue(Class<?> typeHint) {
    Value value = getValue();
    if(value.isSequence()) {
      return value.asSequence().toString();
    }
    Object defaultValue = value.getValue();
    if(value.getValueType().isDateTime()) {
      if(value.isNull()) {
        return Context.toObject(defaultValue, this);
      }
      double jsDate;
      if(value.getValueType() == DateType.get()) {
        jsDate = ((MagmaDate) defaultValue).asDate().getTime();
      } else {
        jsDate = ((Date) defaultValue).getTime();
      }
      return Context.toObject(ScriptRuntime.wrapNumber(jsDate), this);
    }
    if(value.getValueType().isNumeric()) {
      return Context.toNumber(defaultValue);
    }
    if(value.getValueType().equals(BooleanType.get())) {
      return Context.toBoolean(defaultValue);
    }
    if(value.getValueType().equals(TextType.get())) {
      return Context.toString(defaultValue);
    }
    return defaultValue;
  }

  public Value getValue() {
    return value;
  }

  public ValueType getValueType() {
    return getValue().getValueType();
  }

  public boolean contains(Value testValue) {
    if(getValue().isSequence()) {
      return getValue().asSequence().contains(testValue);
    }
    return getValue().equals(testValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.getValue().toString();
  }
}
