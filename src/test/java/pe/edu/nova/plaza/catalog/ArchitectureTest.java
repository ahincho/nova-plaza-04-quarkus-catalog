package pe.edu.nova.plaza.catalog;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Las reglas del hexágono sobre el contexto {@code inventory}, con los nombres de las de NestJS: las dependencias
 * apuntan hacia adentro, y el núcleo no conoce ningún framework.
 */
class ArchitectureTest {

    private static final String CONTEXT = "pe.edu.nova.plaza.catalog.inventory";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(CONTEXT);

    @Test
    void domainDependsOnNothing() {
        noClasses().that().resideInAPackage(CONTEXT + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        CONTEXT + ".port..", CONTEXT + ".service..", CONTEXT + ".adapter..", CONTEXT + ".exception..",
                        "jakarta..", "io.quarkus..", "org.hibernate..")
                .check(classes);
    }

    @Test
    void exceptionsDependOnNothing() {
        noClasses().that().resideInAPackage(CONTEXT + ".exception..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        CONTEXT + ".port..", CONTEXT + ".service..", CONTEXT + ".adapter..", CONTEXT + ".domain..")
                .check(classes);
    }

    @Test
    void portsDoNotImportAdaptersNorServices() {
        noClasses().that().resideInAPackage(CONTEXT + ".port..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        CONTEXT + ".adapter..", CONTEXT + ".service..", "jakarta..", "io.quarkus..")
                .check(classes);
    }

    @Test
    void serviceMustNotImportAdapter() {
        noClasses().that().resideInAPackage(CONTEXT + ".service..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        CONTEXT + ".adapter..", "jakarta.persistence..", "io.quarkus.hibernate..")
                .check(classes);
    }

    @Test
    void edgesMustNotKnowEachOther() {
        noClasses().that().resideInAPackage(CONTEXT + ".adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(CONTEXT + ".adapter.out..")
                .check(classes);
        noClasses().that().resideInAPackage(CONTEXT + ".adapter.out..")
                .should().dependOnClassesThat().resideInAPackage(CONTEXT + ".adapter.in..")
                .check(classes);
    }

    @Test
    void webContractsStayAtTheWebEdge() {
        noClasses().that().resideOutsideOfPackage(CONTEXT + ".adapter.in.web..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        CONTEXT + ".adapter.in.web.request..", CONTEXT + ".adapter.in.web.response..")
                .check(classes);
    }
}
