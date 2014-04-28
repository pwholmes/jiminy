package org.jiminy.domain.expression;

import org.jiminy.domain.Value;

public interface Expression {
   public void setValue(Value value);
   public Value getValue();
   public String encode();
}
