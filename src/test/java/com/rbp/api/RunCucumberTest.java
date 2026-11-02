package com.rbp.api;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * Entry point for {@code mvn test}. Reporting, tag filtering and other settings are in
 * {@code src/test/resources/junit-platform.properties}; any of them can be overridden
 * with {@code -D}, e.g. {@code -Dcucumber.filter.tags=@smoke}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.rbp.api")
public class RunCucumberTest {
}
