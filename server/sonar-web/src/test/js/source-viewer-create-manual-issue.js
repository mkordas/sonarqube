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
/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-create-manual-issue');
lib.configureCasper();


casper.test.begin(testName('source-viewer-create-manual-issue'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        lib.mockRequestFromFile('/api/issues/create', 'create-issue.json');
        lib.mockRequestFromFile('/api/issues/show', 'create-issue.json');
        lib.mockRequestFromFile('/api/rules/search*', 'api-rules-search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/source-viewer/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line-number[data-line-number="3"]');
      })

      .then(function () {
        casper.click('.source-line-number[data-line-number="3"]');
        casper.waitForSelector('.js-add-manual-issue');
      })

      .then(function () {
        casper.click('.js-add-manual-issue');
        casper.waitForSelector('.js-manual-issue-form');
      })

      .then(function () {
        casper.fill('.js-manual-issue-form', {
          rule: 'manual:api',
          message: 'An issue message'
        }, true);
      })

      .then(function () {
        casper.waitForSelector('.source-line-code.has-issues[data-line-number="3"]', function () {
          test.assertExists('.source-line-code.has-issues[data-line-number="3"]');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
