/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
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
package org.estatio.app.menus.doctemplates;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.incode.module.doctemplates.dom.DocTemplate;
import org.incode.module.doctemplates.dom.DocTemplateRepository;

import org.estatio.dom.RegexValidation;
import org.estatio.dom.UdoDomainRepositoryAndFactory;
import org.estatio.dom.apptenancy.EstatioApplicationTenancyRepository;
import org.estatio.dom.charge.Charge;

@DomainService(nature = NatureOfService.VIEW_MENU_ONLY)
@DomainServiceLayout(
        named = "Other",
        menuBar = DomainServiceLayout.MenuBar.PRIMARY,
        menuOrder = "80.15")
public class DocTemplateMenu extends UdoDomainRepositoryAndFactory<Charge> {

    public DocTemplateMenu() {
        super(DocTemplateMenu.class, Charge.class);
    }

    // //////////////////////////////////////

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_NEITHER)
    @MemberOrder(sequence = "1")
    public DocTemplate newTemplate(
            @Parameter(
                    regexPattern = RegexValidation.REFERENCE,
                    regexPatternReplacement = RegexValidation.REFERENCE_DESCRIPTION)
            @ParameterLayout(named = "Reference")
            final String reference,
            final ApplicationTenancy applicationTenancy,
            @ParameterLayout(named = "Text", multiLine = 14)
            final String templateText) {

        return docTemplateRepository.create(reference, applicationTenancy.getPath(), templateText);
    }

    public List<ApplicationTenancy> choices1NewTemplate() {
        return estatioApplicationTenancyRepository.allCountryTenancies();
    }

    // //////////////////////////////////////

    @Action(semantics = SemanticsOf.SAFE)
    @MemberOrder(sequence = "2")
    public List<DocTemplate> allTemplates() {
        return docTemplateRepository.allTemplates();
    }


    // //////////////////////////////////////

    @Inject
    private EstatioApplicationTenancyRepository estatioApplicationTenancyRepository;

    @Inject
    private DocTemplateRepository docTemplateRepository;


}
