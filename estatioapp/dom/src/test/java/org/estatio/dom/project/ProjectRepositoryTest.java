/*
 *
 *  Copyright 2012-2015 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.dom.project;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.query.Query;

import org.estatio.dom.FinderInteraction;
import org.estatio.dom.FinderInteraction.FinderMethod;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectRepositoryTest {

    FinderInteraction finderInteraction;

    ProjectRepository projectRepository;

    @Before
    public void setup() {
    	projectRepository = new ProjectRepository() {

            @Override
            protected <T> T firstMatch(Query<T> query) {
                finderInteraction = new FinderInteraction(query, FinderMethod.FIRST_MATCH);
                return null;
            }

            @Override
            protected List<Project> allInstances() {
                finderInteraction = new FinderInteraction(null, FinderMethod.ALL_INSTANCES);
                return null;
            }

            @Override
            protected <T> List<T> allMatches(Query<T> query) {
                finderInteraction = new FinderInteraction(query, FinderMethod.ALL_MATCHES);
                return null;
            }
        };
    }



    public static class FindProject extends ProjectRepositoryTest {

        @Test
        public void happyCase() {

        	projectRepository.findProject("some?search*Phrase");

            assertThat(finderInteraction.getFinderMethod()).isEqualTo(FinderMethod.ALL_MATCHES);
            assertThat(finderInteraction.getResultType()).isEqualTo(Project.class);
            assertThat(finderInteraction.getQueryName()).isEqualTo("matchByReferenceOrName");
            assertThat(finderInteraction.getArgumentsByParameterName().get("matcher")).isEqualTo((Object) "(?i)some.search.*Phrase");
            assertThat(finderInteraction.getArgumentsByParameterName()).hasSize(1);
        }

    }
}