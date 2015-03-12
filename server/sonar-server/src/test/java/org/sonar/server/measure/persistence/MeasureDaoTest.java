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

package org.sonar.server.measure.persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.RulePriority;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MeasureKey;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class MeasureDaoTest extends AbstractDaoTestCase {

  DbSession session;
  MeasureDao sut;

  @Before
  public void setUp() {
    session = getMyBatis().openSession(false);
    sut = new MeasureDao();
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void get_value_by_key() throws Exception {
    setupData("shared");

    MeasureDto result = sut.getNullableByKey(session, MeasureKey.of("org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc"));
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
  }

  @Test
  //TODO the string must be longer than 4000 char to be persisted in the data field
  public void get_data_by_key() throws Exception {
    setupData("shared");

    MeasureDto result = sut.getNullableByKey(session, MeasureKey.of("org.struts:struts-core:src/org/struts/RequestContext.java", "authors_by_line"));
    assertThat(result.getId()).isEqualTo(20);
    assertThat(result.getData()).isEqualTo("0123456789012345678901234567890123456789");
  }

  @Test
  public void get_text_value_by_key() throws Exception {
    setupData("shared");

    MeasureDto result = sut.getNullableByKey(session, MeasureKey.of("org.struts:struts-core:src/org/struts/RequestContext.java", "coverage_line_hits_data"));
    assertThat(result.getId()).isEqualTo(21);
    assertThat(result.getData()).isEqualTo("36=1;37=1;38=1;39=1;43=1;48=1;53=1");
  }

  @Test
  public void find_by_component_key_and_metrics() throws Exception {
    setupData("shared");

    List<MeasureDto> results = sut.findByComponentKeyAndMetricKeys(session, "org.struts:struts-core:src/org/struts/RequestContext.java",
      newArrayList("ncloc", "authors_by_line"));
    assertThat(results).hasSize(2);

    results = sut.findByComponentKeyAndMetricKeys(session, "org.struts:struts-core:src/org/struts/RequestContext.java", newArrayList("ncloc"));
    assertThat(results).hasSize(1);

    MeasureDto result = results.get(0);
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getKey()).isEqualTo(MeasureKey.of("org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc"));
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);
  }

  @Test
  public void find_by_component_key_and_metric() throws Exception {
    setupData("shared");

    MeasureDto result = sut.findByComponentKeyAndMetricKey("org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc", session);
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getKey()).isEqualTo(MeasureKey.of("org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc"));
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);

    assertThat(sut.findByComponentKeyAndMetricKey("org.struts:struts-core:src/org/struts/RequestContext.java", "unknown", session)).isNull();
  }

  @Test
  public void exists_by_key() throws Exception {
    setupData("shared");

    assertThat(sut.existsByKey(session, MeasureKey.of("org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc"))).isTrue();
    assertThat(sut.existsByKey(session, MeasureKey.of("org.struts:struts-core:src/org/struts/RequestContext.java", "unknown"))).isFalse();
  }

  @Test
  public void insert() throws Exception {
    setupData("empty");

    sut.insert(session, MeasureDto.createFor(MeasureKey.of("org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc"))
      .setSnapshotId(2)
      .setMetricId(3)
      .setCharacteristicId(4)
      .setRuleId(5)
      .setProjectId(6)
      .setPersonId(7)
      .setValue(2.5)
      .setData("measure-value")
      .setTendency(42)
      .setSeverity(RulePriority.valueOfInt(0))
      .setDate(new Date(12345678987654321L))
      .setVariation(1, 1.1d)
      .setVariation(2, 2.2d)
      .setVariation(3, 3.3)
      .setVariation(4, 4.4)
      .setVariation(5, 5.5)
      .setAlertStatus("alert")
      .setAlertText("alert-text")
      .setUrl("www.measures.com")
      .setDescription("measure-description")
      );
    session.commit();

    checkTable("insert", "project_measures");
  }
}
