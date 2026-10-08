package com.argo.arch;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(packages = "com.argo", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

	private static final List<String> PACKAGES = List.of("common", "staff", "customer", "card", "card.admin",
			"order", "order.admin", "security", "security.net", "security.block", "security.guard", "security.web", "config");

	// 依賴指向方向，箭頭左邊不可用右邊
	private static ArchRule forbid(String from, String... to) {
		return noClasses().that().resideInAPackage("com.argo." + from + "..").should()
				.dependOnClassesThat(resideInAnyPackage(prefixed(to)));
	}

	private static String[] prefixed(String... pkgs) {
		return java.util.Arrays.stream(pkgs).map(p -> "com.argo." + p + "..").toArray(String[]::new);
	}

	// 規則裡的套件名都要真的存在
	@Test
	void packagesExist() {
		JavaClasses all = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
				.importPackages("com.argo");
		for (String p : PACKAGES) {
			assertFalse(all.stream().noneMatch(c -> c.getPackageName().startsWith("com.argo." + p)), p);
		}
	}

	@ArchTest
	static final ArchRule noDomainCycles = slices().matching("com.argo.(*)..").should().beFreeOfCycles();

	@ArchTest
	static final ArchRule commonIsBottom = forbid("common", "staff", "customer", "card", "order", "security",
			"config");

	@ArchTest
	static final ArchRule staffOnlyUsesCommon = forbid("staff", "customer", "card", "order", "security", "config");

	@ArchTest
	static final ArchRule customerOnlyUsesCommon = forbid("customer", "staff", "card", "order", "security",
			"config");

	@ArchTest
	static final ArchRule cardNotUpward = forbid("card", "customer", "order", "security", "config");

	// 只有後台管理可用員工權限
	@ArchTest
	static final ArchRule cardCoreNotUsingStaff = noClasses().that().resideInAPackage("com.argo.card..").and()
			.resideOutsideOfPackage("com.argo.card.admin..").should()
			.dependOnClassesThat(resideInAnyPackage(prefixed("staff")));

	@ArchTest
	static final ArchRule orderLayer = forbid("order", "security", "config");

	// 只有後台管理可用員工權限
	@ArchTest
	static final ArchRule orderCoreNotUsingStaff = noClasses().that().resideInAPackage("com.argo.order..").and()
			.resideOutsideOfPackage("com.argo.order.admin..").should()
			.dependOnClassesThat(resideInAnyPackage(prefixed("staff")));

	@ArchTest
	static final ArchRule securityLayer = forbid("security", "customer", "card", "order", "config");

	@ArchTest
	static final ArchRule nobodyUsesConfig = noClasses().that().resideOutsideOfPackage("com.argo.config..")
			.should().dependOnClassesThat(resideInAnyPackage(prefixed("config")));

	@ArchTest
	static final ArchRule securityNet = forbid("security.net", "security.block", "security.guard", "security.web");

	@ArchTest
	static final ArchRule securityBlock = forbid("security.block", "security.guard", "security.web");

	@ArchTest
	static final ArchRule securityGuard = forbid("security.guard", "security.web");

	@ArchTest
	static final ArchRule securityWeb = forbid("security.web", "security.guard");
}
