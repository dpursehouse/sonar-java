/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.plugins.java;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;

public class DroppedPropertiesSensorTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test() throws Exception {
    SensorContextTester contextTester = SensorContextTester.create(tmp.newFolder());
    MapSettings mapSettings = new MapSettings().setProperty("sonar.jacoco.reportPaths", "/path");
    contextTester.setSettings(mapSettings);
    List<String> analysisWarnings = new ArrayList<>();
    DroppedPropertiesSensor sensor = new DroppedPropertiesSensor(analysisWarnings::add);
    sensor.execute(contextTester);

    String msg = "Property 'sonar.jacoco.reportPaths' is no longer supported. Use JaCoCo's xml report and sonar-jacoco plugin.";
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(msg);
    assertThat(analysisWarnings).containsExactly(msg);
  }

  @Test
  public void test_empty() throws Exception {
    SensorContextTester contextTester = SensorContextTester.create(tmp.newFolder());
    List<String> analysisWarnings = new ArrayList<>();
    DroppedPropertiesSensor sensor = new DroppedPropertiesSensor(analysisWarnings::add);
    sensor.execute(contextTester);

    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    assertThat(analysisWarnings).isEmpty();
  }

  @Test
  public void test_descriptor() throws Exception {
    DroppedPropertiesSensor sensor = new DroppedPropertiesSensor(w -> {
    });
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isNotBlank();
    Configuration emptyConfig = new MapSettings().asConfig();
    assertThat(descriptor.configurationPredicate().test(emptyConfig)).isFalse();
    Configuration removedProperty = new MapSettings().setProperty("sonar.jacoco.reportPaths", "/path").asConfig();
    assertThat(descriptor.configurationPredicate().test(removedProperty)).isTrue();
  }

}