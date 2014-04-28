package org.jiminy.domain.expression;

import org.jiminy.JiminyException;
import org.jiminy.domain.Value;

public class ConstantExpression implements Expression {
   private Value value;
   
   @SuppressWarnings("unused")
   private ConstantExpression() {}
   
   public ConstantExpression(Value value) {
      this.value = value;
   }

   @Override
   public void setValue(Value value) {
      throw new JiminyException("Cannot assign a value to this expression");
   }    

   @Override
   public Value getValue() {
      return value;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      
      sb.append("{").append(this.getClass().getSimpleName()).append(": ");
      sb.append("value: ").append(getValue());
      sb.append("}");
      
      return sb.toString();
   }

   @Override
   public String encode() {
      StringBuilder sb = new StringBuilder();
      switch(value.getType()) {
         case BOOLEAN:  sb.append("CB{"); break;
         case FLOAT:    sb.append("CF{"); break;
         case INT:      sb.append("CI{"); break;
         case STRING:   sb.append("CS{"); break;
         default:
      }
      sb.append(value.getStringValue());
      sb.append("}");
      return sb.toString();
   }

}
