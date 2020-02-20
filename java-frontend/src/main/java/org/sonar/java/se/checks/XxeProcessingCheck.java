/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.se.checks;

import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2755-experiment")
public class XxeProcessingCheck extends SECheck {

  private enum XxeSecuredConstraint implements Constraint {
    UNSECURED, SECURED;
  }

  private enum SecuringFeature {
    XML_INPUT_FACTORY_SUPPORT_DTD("javax.xml.stream.supportDTD") {
      @Override
      ProgramState checkArguments(ProgramState state, Arguments arguments) {
        if (isSettingFeature(arguments.get(0)) && isSetToFalse(state.peekValue(), arguments.get(1))) {
          return state.addConstraint(state.peekValue(2), XxeSecuredConstraint.SECURED);
        }
        return state;
      }
    };

    private final String featureName;

    SecuringFeature(String featureName) {
      this.featureName = featureName;
    }

    ProgramState checkArguments(ProgramState state, Arguments arguments) {
      return state;
    }

    boolean isSettingFeature(ExpressionTree arg0) {
      return arg0.asConstant(String.class).filter(featureName::equals).isPresent();
    }

    boolean isSetToFalse(@Nullable SymbolicValue sv1, ExpressionTree arg1) {
      return sv1 == SymbolicValue.FALSE_LITERAL
        || arg1.asConstant(String.class).filter("false"::equalsIgnoreCase).isPresent();
    }
  }

  private static class XxeSymbolicValue extends SymbolicValue {
    private final SymbolicValue wrappedValue;
    private final MethodInvocationTree init;

    private XxeSymbolicValue(SymbolicValue wrappedValue, MethodInvocationTree init) {
      this.wrappedValue = wrappedValue;
      this.init = init;
    }

    @Override
    public boolean references(SymbolicValue other) {
      return wrappedValue.equals(other) || wrappedValue.references(other);
    }
  }

  private static final String XML_INPUT_FACTORY_CLASS_NAME = "javax.xml.stream.XMLInputFactory";

  private static final MethodMatcher XML_INPUT_FACTORY_NEW_INSTANCE = MethodMatcher.create()
    .typeDefinition(XML_INPUT_FACTORY_CLASS_NAME)
    .name("newInstance")
    .withAnyParameters();
  private static final MethodMatcher XML_INPUT_FACTORY_SET_PROPERTY = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf(XML_INPUT_FACTORY_CLASS_NAME))
    .name("setProperty")
    .parameters("java.lang.String", "java.lang.Object");

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private static class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private final ConstraintManager constraintManager;

    private PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.constraintManager = context.getConstraintManager();
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (XML_INPUT_FACTORY_NEW_INSTANCE.matches(mit)) {
        constraintManager.setValueFactory(() -> new XxeSymbolicValue(programState.peekValue(), mit));
      } else if (XML_INPUT_FACTORY_SET_PROPERTY.matches(mit)) {
        Arguments arguments = mit.arguments();
        for (SecuringFeature feature : SecuringFeature.values()) {
          programState = feature.checkArguments(programState, arguments);
        }
      }
    }
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    PostStatementVisitor visitor = new PostStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private static class PostStatementVisitor extends CheckerTreeNodeVisitor {

    private PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      SymbolicValue peek = programState.peekValue();
      if (peek != null && XML_INPUT_FACTORY_NEW_INSTANCE.matches(mit)) {
        programState = programState.addConstraint(peek, XxeSecuredConstraint.UNSECURED);
      }
    }
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    // FIXME should we report on usage or at the end of the end of each method?
    for (SymbolicValue symbolicValue : context.getState().getValuesWithConstraints(XxeSecuredConstraint.UNSECURED)) {
      context.reportIssue(ExpressionUtils.methodName(((XxeSymbolicValue) symbolicValue).init),
        this,
        // FIXME message
        "I feel unsecured, idiot!");
    }
  }
}
