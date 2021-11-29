package de.apnmt.organization;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchTest {

    @Test
    void servicesAndRepositoriesShouldNotDependOnWebLayer() {
        JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("de.apnmt.organization");

        noClasses()
            .that()
            .resideInAnyPackage("de.apnmt.organization.service..")
            .or()
            .resideInAnyPackage("de.apnmt.organization.repository..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..de.apnmt.organization.web..")
            .because("Services and repositories should not depend on web layer")
            .check(importedClasses);
    }
}
