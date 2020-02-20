package org.sonar.java.se.checks;

import org.junit.Test;
import org.sonar.java.se.JavaCheckVerifier;

public class XxeProcessingCheckTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck.java", new XxeProcessingCheck());
  }

}
