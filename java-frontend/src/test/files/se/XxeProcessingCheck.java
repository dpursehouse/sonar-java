package checks.security;

import javax.xml.stream.XMLInputFactory;

class XxeProcessingCheck {
  XMLInputFactory no_property_new_instance() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    return factory;
  }

  XMLInputFactory dtd_with_primitive_false() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory;
  }

  XMLInputFactory dtd_with_primitive_false() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, "false");
    return factory;
  }

  void dtd_with_primitive_conditional(boolean condition) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    if (condition) {
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, "false");
    }
    return factory;
  }
}
