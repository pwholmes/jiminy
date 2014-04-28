package org.jiminy.domain.expression;

import org.jiminy.JiminyException;
import org.jiminy.domain.DataType;
import org.jiminy.domain.Operator;
import org.jiminy.domain.Value;

public class BinaryExpression implements Expression {
   Expression operand1;
   Operator operator;
   Expression operand2;
   
   @SuppressWarnings("unused")
   private BinaryExpression() {}
   
   public BinaryExpression(Expression operand1, Operator operator, Expression operand2) {
      this.operand1 = operand1;
      this.operator = operator;
      this.operand2 = operand2;
   }

   public Expression getOperand1() {
      return operand1;
   }

   public Operator getOperator() {
      return operator;
   }

   public Expression getOperand2() {
      return operand2;
   }

   @Override
   public void setValue(Value value) {
      throw new JiminyException("Cannot assign a value to an expression that is not an l-value");
   }

   @Override
   public Value getValue() {
      Value value1 = operand1.getValue();
      Value value2 = operand2.getValue();

      switch (operator) {
         case EQUAL_TO:
            if (value1.getType() == DataType.INT && value2.getType() == DataType.FLOAT)
               return new Value(value1.getIntValue() == value2.getFloatValue());
            if (value1.getType() == DataType.FLOAT && value2.getType() == DataType.INT)
               return new Value(value1.getFloatValue() == value2.getIntValue());
            if (value1.getType() != value2.getType())
               throw new JiminyException("Type mismatch: attempting to compare value of type " + value1.getType() + " to value of type " + value2.getType() + ".");
            return new Value(value1.equals(value2));
         default: // should never happen
            throw new JiminyException("Unknown binary operator: " + operator);
      }
   }

   @Override
   public String encode() {
      StringBuilder sb = new StringBuilder();
      sb.append("EB{");
      sb.append(operand1.encode());
      sb.append(Operator.encode(operator));
      sb.append(operand2.encode());
      sb.append("}");
      return sb.toString();
   }
}
