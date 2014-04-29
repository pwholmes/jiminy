package org.jiminy.domain.expression;

import java.util.ArrayList;
import org.jiminy.JiminyException;
import org.jiminy.domain.DataType;
import org.jiminy.domain.Value;

public class DecisionListExpression implements Expression {
   private Expression condition = null;
   private boolean nodeTrueValue = false;
   private DecisionListExpression nextNode = null;
   
   @SuppressWarnings("unused")
   private DecisionListExpression() {}
   
   public DecisionListExpression(Expression condition, boolean value) {
      this.condition = condition;
      this.nodeTrueValue = value;
   }

   @Override
   public void setValue(Value value) {
      throw new JiminyException("Cannot assign a value to an expression that is not an l-value");
   }

   @Override
   public Value getValue() {
      if (condition.getValue().getBooleanValue() == true)
         return new Value(nodeTrueValue);
      if (nextNode == null)
         throw new JiminyException("Decision list lacks a default value");
      return new Value(nodeTrueValue);
   }

   public DecisionListExpression getNextNode() {
      return nextNode;
   }
   public void setNextNode(DecisionListExpression nextNode) {
      this.nextNode = nextNode;
   }

   @Override
   public String encode() {
      StringBuilder sb = new StringBuilder();

      int numNodes = 0;
      for (DecisionListExpression node = this; node != null; node = node.nextNode)
         numNodes++;

      sb.append("DL").append(numNodes).append("{");
      
      for (DecisionListExpression node = this; node != null; node = node.nextNode) {
         sb.append(node.condition.encode());
         sb.append(node.nodeTrueValue ? "T" : "F");
      }
      
      sb.append("}");

      return sb.toString();
   }
   
   public ArrayList<String> encode2() {
      ArrayList<String> expressions = new ArrayList<String>();
      
      for (DecisionListExpression node = this; node != null; node = node.nextNode)
         expressions.add(node.condition.encode() + (node.nodeTrueValue ? "T" : "F"));
      
      return expressions;
   }
   
   public boolean evaluate() {
      if (condition == null)
         throw new JiminyException("DL condition is null");
      Value value = condition.getValue();
      if (value.getType() != DataType.BOOLEAN)
         throw new JiminyException("Expression evaluates to illegal type (should be boolean): " + value.getType().toString());
      if (value.getBooleanValue())
         return nodeTrueValue;
      if (nextNode == null)
         throw new JiminyException("Decision list lacks default node");
      return nextNode.evaluate();
   }
}
