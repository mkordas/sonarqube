/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class SourceFiltersTest extends AbstractMavenTest {

  static final String PROJECT = "com.sonarsource.it.projects.batch:source-filters";

  @BeforeClass
  public static void scanProject() {
    orchestrator.getDatabase().truncateInspectionTables();
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/source-filters"));
    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      build.setProperty("sonar.exclusions", "src/main/java/sourceFilters/**/*BeExcluded.java");
    } else {
      build.setProperty("sonar.exclusions", "sourceFilters/**/*BeExcluded.java");
    }
    build.setGoals(cleanSonarGoal())
      .setProperties("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testExclusionProperty() {
    assertThat(getResource(PROJECT + ":sourceFilters.ToBeExcluded"), nullValue());
    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.5") && mojoVersion().isGreaterThanOrEquals("2.4")) {
      assertThat(getMeasure(PROJECT, "files").getIntValue(), is(2));
    } else {
      assertThat(getMeasure(PROJECT, "files").getIntValue(), is(1));
    }
  }

  private Measure getMeasure(String resourceKey, String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  private Resource getResource(String resourceKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey));
  }
}
