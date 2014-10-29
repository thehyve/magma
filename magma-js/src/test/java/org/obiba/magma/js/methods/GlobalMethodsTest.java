package org.obiba.magma.js.methods;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.obiba.magma.*;
import org.obiba.magma.js.AbstractJsTest;
import org.obiba.magma.js.MagmaContext;
import org.obiba.magma.js.ScriptableValue;
import org.obiba.magma.support.StaticDatasource;
import org.obiba.magma.support.StaticValueTable;
import org.obiba.magma.type.IntegerType;
import org.obiba.magma.type.TextType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mozilla.javascript.Context.getCurrentContext;

public class GlobalMethodsTest extends AbstractJsTest {

//  private static final Logger log = LoggerFactory.getLogger(GlobalMethodsTest.class);

    private static String codeVariable = "code";
    private static String targetVariable = "target";
    private static String acode1 = "Acode1";
    private static String acode2 = "Acode2";
    private static String acode3 = "Acode3";
    private static String bcode1 = "Bcode1";
    private static String bcode2 = "Bcode2";
    private static String bcode3 = "Bcode3";

  @Test
  public void test_newValue_inferred_int() throws Exception {
    ScriptableValue sv = GlobalMethods.newValue(getCurrentContext(), getSharedScope(), new Object[] { 1 }, null);
    assertThat(sv.getValue().isNull()).isFalse();
    assertThat(sv.getValue().isSequence()).isFalse();
    assertThat((IntegerType) sv.getValueType()).isEqualTo(IntegerType.get());
    assertThat((Long) sv.getValue().getValue()).isEqualTo(1l);
  }

  @Test
  public void test_newValue_int() throws Exception {
    ScriptableValue sv = GlobalMethods
        .newValue(getCurrentContext(), getSharedScope(), new Object[] { "1", "integer" }, null);
    assertThat(sv.getValue().isNull()).isFalse();
    assertThat(sv.getValue().isSequence()).isFalse();
    assertThat((IntegerType) sv.getValueType()).isEqualTo(IntegerType.get());
    assertThat((Long) sv.getValue().getValue()).isEqualTo(1l);
  }

  @Test(expected = MagmaRuntimeException.class)
  public void test_newValue_wrong_type() throws Exception {
    GlobalMethods.newValue(getCurrentContext(), getSharedScope(), new Object[] { "qwerty", "integer" }, null);
  }

  @Test
  public void test_newSequence_int() throws Exception {
    ScriptableValue sv = GlobalMethods
        .newSequence(getCurrentContext(), getSharedScope(), new Object[] { new NativeArray(new Object[] { 1, 2, 3 }) },
            null);
    assertThat(sv.getValue().isSequence()).isTrue();
    assertThat((IntegerType) sv.getValueType()).isEqualTo(IntegerType.get());
    assertThat(sv.getValue().getLength()).isEqualTo(3);
    ValueSequence sequence = sv.getValue().asSequence();
    for(int i = 1; i <= 3; i++) {
      Value value = sequence.get(i - 1);
      assertThat((IntegerType) value.getValueType()).isEqualTo(IntegerType.get());
      assertThat((Long) value.getValue()).isEqualTo((long) i);
    }
  }

  @Test
  public void test_newSequence_String() throws Exception {
    ScriptableValue sv = GlobalMethods.newSequence(getCurrentContext(), getSharedScope(),
        new Object[] { new NativeArray(new Object[] { "1", "2", "3" }) }, null);
    assertThat(sv.getValue().isSequence()).isTrue();
    assertThat((TextType) sv.getValueType()).isEqualTo(TextType.get());
    assertThat(sv.getValue().getLength()).isEqualTo(3l);
    ValueSequence sequence = sv.getValue().asSequence();
    for(int i = 1; i <= 3; i++) {
      Value value = sequence.get(i - 1);
      assertThat((TextType) value.getValueType()).isEqualTo(TextType.get());
      assertThat((String) value.getValue()).isEqualTo(String.valueOf(i));
    }
  }

  @Test
  public void test_newSequence_with_int_type() throws Exception {
    ScriptableValue sv = GlobalMethods.newSequence(getCurrentContext(), getSharedScope(),
        new Object[] { new NativeArray(new Object[] { "1", "2", "3" }), "integer" }, null);
    assertThat(sv.getValue().isSequence()).isTrue();
    assertThat((IntegerType) sv.getValueType()).isEqualTo(IntegerType.get());
    assertThat(sv.getValue().getLength()).isEqualTo(3l);
    ValueSequence sequence = sv.getValue().asSequence();
    for(int i = 1; i <= 3; i++) {
      Value value = sequence.get(i - 1);
      assertThat((IntegerType) value.getValueType()).isEqualTo(IntegerType.get());
      assertThat((Long) value.getValue()).isEqualTo((long) i);
    }
  }

    @Test
    public void test_$_join_2by2() throws Exception {
        List<String> values = getJoinSequenceValues(4, acode1, acode2);
        Assert.assertEquals(2, Collections.frequency(values, bcode1));
        Assert.assertEquals(1, Collections.frequency(values, bcode2));
        Assert.assertEquals(1, Collections.frequency(values, bcode3));
    }

    @Test
    public void test_$_join_1by2() throws Exception {
        List<String> values = getJoinSequenceValues(2, acode1);
        Assert.assertEquals(1, Collections.frequency(values, bcode1));
        Assert.assertEquals(1, Collections.frequency(values, bcode2));
    }

    @Test
    public void test_$_join_no_values() throws Exception {
        getJoinSequenceValues(0, acode3);
    }

    @Test
    public void test_$_join_no_fk() throws Exception {
        getJoinSequenceValues(null, (String)null);
    }

    @Test
    public void test_setOf_ValueSequence() throws Exception {
        ValueSequence inputValue = seqOf(acode1, acode2, acode1);
        Object arg = new ScriptableValue(getSharedScope(), inputValue, null);

        ScriptableValue sv = GlobalMethods.setOf(
                getCurrentContext(),
                getSharedScope(),
                new Object[]{arg},
                null);

        List<String> outputValues = getValues(2, sv.getValue());
        Assert.assertEquals(1, Collections.frequency(outputValues, acode1));
        Assert.assertEquals(1, Collections.frequency(outputValues, acode2));
    }

    @Test
    public void test_setOf_Value() throws Exception {
        Value inputValue = TextType.get().valueOf(acode1);
        Object arg = new ScriptableValue(getSharedScope(), inputValue, null);

        ScriptableValue sv = GlobalMethods.setOf(
                getCurrentContext(),
                getSharedScope(),
                new Object[] { arg },
                null);

        List<String> outputValues = getValues(1, sv.getValue());
        Assert.assertEquals(1, Collections.frequency(outputValues, acode1));
    }

    @Test
    public void test_setOf_Array_implicitType() throws Exception {
        Object arg = new NativeArray(new Object[] { "1", "2", "1" });

        ScriptableValue sv = GlobalMethods.setOf(
                getCurrentContext(),
                getSharedScope(),
                new Object[] { arg },
                null);

        List<String> outputValues = getValues(2, sv.getValue());
        Assert.assertEquals(1, Collections.frequency(outputValues, "1"));
        Assert.assertEquals(1, Collections.frequency(outputValues, "2"));
    }

    @Test
    public void test_setOf_Array_explicitType() throws Exception {
        Object arg = new NativeArray(new Object[] { "1", "2", "1" });

        ScriptableValue sv = GlobalMethods.setOf(
                getCurrentContext(),
                getSharedScope(),
                new Object[] { arg, "integer" },
                null);

        ValueSequence seq = sv.getValue().asSequence();
        //Magma integer maps to Java long
        Assert.assertEquals(1L, seq.get(0).getValue());
        Assert.assertEquals(2L, seq.get(1).getValue());
    }

    private List<String> getJoinSequenceValues(Integer expectedCount, String ... fkValues) {
        MagmaContext ctx = getMagmaContext();
        StaticDatasource ds = new StaticDatasource("datasource");
        MagmaEngine.get().addDatasource(ds);

        VariableEntity entity = createEntity("a");
        ValueTable table = createMainTable(ds, fkValues);
        ValueTable mappingTable = createMappingTable(ds);

        ctx.push(ValueTable.class, table);
        ctx.push(ValueSet.class, table.getValueSet(entity));

        String varRef = String.format("%s.%s:%s", ds.getName(), mappingTable.getName(), targetVariable);
        Object[] args = {varRef, codeVariable};
        Scriptable result = GlobalMethods.$join(getCurrentContext(), getSharedScope(), args, null);

        ScriptableValue sv = (ScriptableValue)result;
        return getValues(expectedCount, sv.getValue());
    }

    private List<String> getValues(Integer expectedCount, Value value) {
        if (expectedCount == null) {
            Assert.assertTrue("should be null", value.isNull());
            return null;
        } else {
            Assert.assertTrue("should be a sequence", value.isSequence());
            ValueSequence seq = value.asSequence();
            Assert.assertEquals("sequence size count mismatch", expectedCount.intValue(), seq.getSize());
            return getStringValues(seq);
        }
    }

    private static List<String> getStringValues(ValueSequence seq) {
        List<String> values = new ArrayList<>();
        for (Value v: seq.getValues()) {
            values.add(v.toString());
        }
        return values;
    }

    private ValueTable createMainTable(StaticDatasource ds, String... values) {
        VariableEntity entity = createEntity("a");
        String[] tableIds = {entity.getIdentifier()};
        StaticValueTable table = new StaticValueTable(ds, "table", Arrays.asList(tableIds));
        table.addVariables(TextType.get(), codeVariable);
        ValueSequence seq = null;
        if (values.length > 0 && values[0] == null) {
            seq = TextType.get().nullSequence();
        } else {
            seq = seqOf(values);
        }

        table.addValues(entity.getIdentifier(), codeVariable, seq);
        ds.addValueTable(table);
        return table;
    }

    private ValueTable createMappingTable(StaticDatasource ds) {
        String[] tableIds = {acode1, acode2, acode3};
        StaticValueTable table = new StaticValueTable(ds, "mapping_table", Arrays.asList(tableIds), "Mapping");
        table.addVariables(TextType.get(), targetVariable);
        table.addValues(acode1, targetVariable, seqOf(bcode1, bcode2));
        table.addValues(acode2, targetVariable, seqOf(bcode1, bcode3));
        table.addValues(acode3, targetVariable, TextType.get().nullValue());
        ds.addValueTable(table);
        return table;
    }


    private static ValueSequence seqOf(String ... values) {
        List<Value> list = new ArrayList<>();
        for (String s: values) {
            list.add(TextType.get().valueOf(s));
        }
        return TextType.get().sequenceOf(list);
    }

    private static VariableEntity createEntity(String id) {
        VariableEntity obj = EasyMock.createMock(VariableEntity.class);
        EasyMock.expect(obj.getIdentifier()).andStubReturn(id);
        EasyMock.expect(obj.getType()).andStubReturn(TextType.get().getName());
        EasyMock.replay(obj);
        return obj;
    }
}
