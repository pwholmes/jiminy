package org.jiminy.domain.expression;

import java.util.ArrayList;
import org.jiminy.JiminyException;
import org.jiminy.domain.DecisionListResult;
import org.jiminy.domain.Value;

public class DecisionListExpression implements Expression {
   public Expression condition;
   public boolean value;
   public DecisionListExpression nextNode = null;
   public DecisionListResult nodeResult = DecisionListResult.ERROR;
   
   @SuppressWarnings("unused")
   private DecisionListExpression() {}
   
   public DecisionListExpression(Expression condition, boolean value) {
      this.condition = condition;
      this.value = value;
   }

   @Override
   public void setValue(Value value) {
      throw new JiminyException("Cannot assign a value to an expression that is not an l-value");
   }

   @Override
   public Value getValue() {
      if (condition.getValue().getBooleanValue() == true)
         return new Value(value);
      if (nextNode == null)
         throw new JiminyException("Decision list lacks a default value, which is needed");
      return new Value(value);
   }

   public String encodeToString() {
      StringBuilder sb = new StringBuilder();
      sb.append("DL");

      // Note that we *could* do this recursively, but that leads to an overly verbose and messy encoding
      // Count the number of nodes in the DL
      int numNodes = 0;
      for (DecisionListExpression node = this; node != null; node = node.nextNode)
         numNodes++;
      sb.append(numNodes);
      sb.append("{");
      
      // Loop through the nodes again and this time encode them 
      for (DecisionListExpression node = this; node != null; node = node.nextNode) {
         sb.append(node.condition.encode());
         sb.append(node.value ? "T" : "F");
         if (node.nextNode != null)
            sb.append(",");
      }

      sb.append("}");
      return sb.toString();
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
         sb.append(node.value ? "T" : "F");
      }
      
      sb.append("}");

      return sb.toString();
   }
   
   public ArrayList<String> encode2() {
      ArrayList<String> expressions = new ArrayList<String>();
      
      for (DecisionListExpression node = this; node != null; node = node.nextNode)
         expressions.add(node.condition.encode() + (node.value ? "T" : "F"));
      
      return expressions;
   }
}
