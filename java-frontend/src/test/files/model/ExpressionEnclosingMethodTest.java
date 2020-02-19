class EnclosingMethod {

  {
    // null
    myField = null;
  }

  static {
    // null
    myField = null;
  }

  String myField;

  java.util.function.Supplier<Object> s = () -> {
    // null
    myField = null;
  };

  public void fun() {
    // fun
    myField = null;
  }
}

interface EnclosingMethodInterface {
  static final String myField;

  static final java.util.function.Supplier<Object> s = () -> {
    // null
    I1.myField = null;
  };

  default void fun() {
    // fun
    I1.myField = null;
  }
}
