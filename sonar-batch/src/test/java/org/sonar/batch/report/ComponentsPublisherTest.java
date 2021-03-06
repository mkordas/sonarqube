/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.report;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.Constants.ComponentLinkType;
import org.sonar.batch.protocol.output.BatchReport.Component;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.protocol.output.FileStructure;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentsPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ProjectReactor reactor;
  private ResourceCache resourceCache;
  private ComponentsPublisher publisher;

  @Before
  public void prepare() {
    reactor = new ProjectReactor(ProjectDefinition.create().setKey("foo"));
    resourceCache = new ResourceCache();
    publisher = new ComponentsPublisher(reactor, resourceCache);
  }

  @Test
  public void add_components_to_report() throws Exception {
    // inputs
    Project root = new Project("foo").setName("Root project")
      .setAnalysisDate(DateUtils.parseDate(("2012-12-12")));
    root.setId(1).setUuid("PROJECT_UUID");
    resourceCache.add(root, null).setSnapshot(new Snapshot().setId(11));

    Project module1 = new Project("module1").setName("Module1");
    module1.setParent(root);
    module1.setId(2).setUuid("MODULE_UUID");
    resourceCache.add(module1, root).setSnapshot(new Snapshot().setId(12));
    reactor.getRoot().addSubProject(ProjectDefinition.create().setKey("module1"));

    Directory dir = Directory.create("src");
    dir.setEffectiveKey("foo:src");
    dir.setId(3).setUuid("DIR_UUID");
    resourceCache.add(dir, module1).setSnapshot(new Snapshot().setId(13));

    org.sonar.api.resources.File file = org.sonar.api.resources.File.create("src/Foo.java", Java.INSTANCE, false);
    file.setEffectiveKey("foo:src/Foo.java");
    file.setId(4).setUuid("FILE_UUID");
    resourceCache.add(file, dir).setSnapshot(new Snapshot().setId(14));

    org.sonar.api.resources.File fileWithoutLang = org.sonar.api.resources.File.create("src/make", null, false);
    fileWithoutLang.setEffectiveKey("foo:src/make");
    fileWithoutLang.setId(5).setUuid("FILE_WITHOUT_LANG_UUID");
    resourceCache.add(fileWithoutLang, dir).setSnapshot(new Snapshot().setId(15));

    org.sonar.api.resources.File testFile = org.sonar.api.resources.File.create("test/FooTest.java", Java.INSTANCE, true);
    testFile.setEffectiveKey("foo:test/FooTest.java");
    testFile.setId(6).setUuid("TEST_FILE_UUID");
    resourceCache.add(testFile, dir).setSnapshot(new Snapshot().setId(16));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);
    publisher.publish(writer);

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 2)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 3)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 4)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 5)).isTrue();
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 6)).isTrue();

    // no such reference
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 7)).isFalse();

    BatchReportReader reader = new BatchReportReader(outputDir);
    Component rootProtobuf = reader.readComponent(1);
    assertThat(rootProtobuf.getLinksCount()).isEqualTo(0);

  }

  @Test
  public void add_components_with_links_and_branch() throws Exception {
    // inputs
    Project root = new Project("foo:my_branch").setName("Root project")
      .setAnalysisDate(DateUtils.parseDate(("2012-12-12")));
    root.setId(1).setUuid("PROJECT_UUID");
    resourceCache.add(root, null).setSnapshot(new Snapshot().setId(11));
    reactor.getRoot().properties().put(CoreProperties.LINKS_HOME_PAGE, "http://home");
    reactor.getRoot().properties().put(CoreProperties.PROJECT_BRANCH_PROPERTY, "my_branch");

    Project module1 = new Project("module1:my_branch").setName("Module1");
    module1.setParent(root);
    module1.setId(2).setUuid("MODULE_UUID");
    resourceCache.add(module1, root).setSnapshot(new Snapshot().setId(12));
    ProjectDefinition moduleDef = ProjectDefinition.create().setKey("module1");
    moduleDef.properties().put(CoreProperties.LINKS_CI, "http://ci");
    reactor.getRoot().addSubProject(moduleDef);

    Directory dir = Directory.create("src");
    dir.setEffectiveKey("foo:src");
    dir.setId(3).setUuid("DIR_UUID");
    resourceCache.add(dir, module1).setSnapshot(new Snapshot().setId(13));

    org.sonar.api.resources.File file = org.sonar.api.resources.File.create("src/Foo.java", Java.INSTANCE, false);
    file.setEffectiveKey("foo:src/Foo.java");
    file.setId(4).setUuid("FILE_UUID");
    resourceCache.add(file, dir).setSnapshot(new Snapshot().setId(14));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);
    publisher.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);
    Component rootProtobuf = reader.readComponent(1);
    assertThat(rootProtobuf.getLinksCount()).isEqualTo(1);
    assertThat(rootProtobuf.getLinks(0).getType()).isEqualTo(ComponentLinkType.HOME);
    assertThat(rootProtobuf.getLinks(0).getHref()).isEqualTo("http://home");

    Component module1Protobuf = reader.readComponent(2);
    assertThat(module1Protobuf.getLinksCount()).isEqualTo(1);
    assertThat(module1Protobuf.getLinks(0).getType()).isEqualTo(ComponentLinkType.CI);
    assertThat(module1Protobuf.getLinks(0).getHref()).isEqualTo("http://ci");
  }
}
