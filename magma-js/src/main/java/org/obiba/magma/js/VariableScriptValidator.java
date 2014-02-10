package org.obiba.magma.js;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.magma.AttributeAware;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.magma.support.MagmaEngineVariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import static org.obiba.magma.js.JavascriptVariableBuilder.SCRIPT_ATTRIBUTE_NAME;

@SuppressWarnings("ConstantNamingConvention")
class VariableScriptValidator {

  private static final Logger log = LoggerFactory.getLogger(VariableScriptValidator.class);

  private static final Pattern $_CALL = Pattern.compile("\\$\\(['\"](([\\d\\w.:]*))['\"]\\)");

  private static final Pattern $THIS_CALL = Pattern.compile("\\$this\\(['\"](([\\d\\w.:]*))['\"]\\)");

  private static final Pattern $VAR_CALL = Pattern.compile("\\$var\\(['\"](([\\d\\w.:]*))['\"]\\)");

  //  private static final Pattern $JOIN_CALL = Pattern.compile("(\\$join\\((['\"](([\\d\\w.:]*))['\"])*\\))");

  private VariableScriptValidator() {}

  @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
  public static void validateScript(@NotNull Variable variable, @NotNull ValueTable table)
      throws CircularVariableDependencyRuntimeException {

    Stopwatch stopwatch = Stopwatch.createStarted();
    getVariableRefNode(new VariableRefNode(variable.getVariableReference(table), table, getScript(variable)));
    log.trace("Script validation of {} in {}", variable.getName(), stopwatch);
  }

  private static void getVariableRefNode(@NotNull VariableRefNode callerNode) {
    if(Strings.isNullOrEmpty(callerNode.getScript())) {
      log.trace("{} has no script", callerNode.getVariableRef());
    } else {
      log.trace("Analyze {} script: {}", callerNode.getVariableRef(), callerNode.getScript());
      for(VariableRefCall variableRefCall : parseScript(callerNode.getScript())) {
        VariableRefNode calleeNode = asNode(variableRefCall, callerNode.getValueTable());
        callerNode.addCallee(calleeNode);
        getVariableRefNode(calleeNode);
      }
    }
  }

  @VisibleForTesting
  static Set<VariableRefCall> parseScript(CharSequence script) {
    ImmutableSet.Builder<VariableRefCall> builder = ImmutableSet.builder();
    parseSingleArgGlobalMethod(script, $_CALL, "$", builder);
    parseSingleArgGlobalMethod(script, $THIS_CALL, "$this", builder);
    parseSingleArgGlobalMethod(script, $VAR_CALL, "$var", builder);
    return builder.build();
  }

  private static void parseSingleArgGlobalMethod(CharSequence script, Pattern pattern, String method,
      ImmutableSet.Builder<VariableRefCall> builder) {
    Matcher matcher = pattern.matcher(script);
    while(matcher.find()) {
      if(matcher.groupCount() == 2) {
        builder.add(new VariableRefCall(method, matcher.group(1)));
      }
    }
  }

  private static VariableRefNode asNode(VariableRefCall variableRefCall, @NotNull ValueTable table) {

    MagmaEngineVariableResolver reference = MagmaEngineVariableResolver.valueOf(variableRefCall.getVariableRef());
    switch(variableRefCall.getMethod()) {
      case "$":
        if(reference.getDatasourceName() == null || reference.getTableName() == null) {
          Variable variable = reference.resolveSource(table).getVariable();
          return new VariableRefNode(Variable.Reference.getReference(table, variable), table, getScript(variable));
        }
        Variable variable = reference.resolveSource().getVariable();
        return new VariableRefNode(Variable.Reference
            .getReference(reference.getDatasourceName(), reference.getTableName(), variable.getName()),
            MagmaEngine.get().getDatasource(reference.getDatasourceName()).getValueTable(reference.getTableName()),
            getScript(variable));
      case "$this":
      case "$var":
        Variable thisVariable = reference.resolveSource(table).getVariable();
        return new VariableRefNode(Variable.Reference.getReference(table, thisVariable), table,
            getScript(thisVariable));
      default:
        throw new MagmaJsEvaluationRuntimeException("Unsupported method validation for " + variableRefCall.getMethod());
    }
  }

  @Nullable
  private static String getScript(AttributeAware variable) {
    return variable.hasAttribute(SCRIPT_ATTRIBUTE_NAME) //
        ? variable.getAttributeStringValue(SCRIPT_ATTRIBUTE_NAME) //
        : null;
  }

  @VisibleForTesting
  static class VariableRefCall {

    @NotNull
    private final String method;

    @NotNull
    private final String variableRef;

    VariableRefCall(@NotNull String method, @NotNull String variableRef) {
      this.method = method;
      this.variableRef = variableRef;
    }

    @NotNull
    public String getMethod() {
      return method;
    }

    @NotNull
    public String getVariableRef() {
      return variableRef;
    }

    @Override
    public String toString() {
      return method + "('" + variableRef + "')";
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(method, variableRef);
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) return true;
      if(obj == null || getClass() != obj.getClass()) return false;
      VariableRefCall other = (VariableRefCall) obj;
      return Objects.equal(method, other.method) && Objects.equal(variableRef, other.variableRef);
    }

  }

  private static class VariableRefNode {

    @NotNull
    private final String variableRef;

    @NotNull
    private final ValueTable valueTable;

    @Nullable
    private final String script;

    private final Set<VariableRefNode> callers = new HashSet<>();

    VariableRefNode(@NotNull String variableRef, @NotNull ValueTable valueTable, @Nullable String script) {
      this.variableRef = variableRef;
      this.valueTable = valueTable;
      this.script = script;
    }

    @NotNull
    public String getVariableRef() {
      return variableRef;
    }

    public Set<VariableRefNode> getCallers() {
      return callers;
    }

    @Nullable
    public String getScript() {
      return script;
    }

    @NotNull
    public ValueTable getValueTable() {
      return valueTable;
    }

    public void addCallee(@NotNull VariableRefNode callee) throws CircularVariableDependencyRuntimeException {
      callee.callers.add(this);
      checkCircularDependencies(callee, new HashSet<VariableRefNode>());
    }

    private static void checkCircularDependencies(@Nullable VariableRefNode node,
        Collection<VariableRefNode> callersList) throws CircularVariableDependencyRuntimeException {
      if(node == null) return;
      if(callersList.contains(node)) {
        throw new CircularVariableDependencyRuntimeException(node.getVariableRef());
      }
      callersList.add(node);
      for(VariableRefNode caller : node.getCallers()) {
        checkCircularDependencies(caller, callersList);
      }
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
      if(this == o) return true;
      if(!(o instanceof VariableRefNode)) return false;
      return variableRef.equals(((VariableRefNode) o).variableRef);
    }

    @Override
    public int hashCode() {
      return variableRef.hashCode();
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).omitNullValues().addValue(variableRef).add("callers", callers).toString();
    }

  }
}