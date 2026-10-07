package com.argo.arch;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.argo", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

	// 頂層套件不可循環依賴
	@ArchTest
	static final ArchRule noDomainCycles = slices().matching("com.argo.(*)..").should().beFreeOfCycles();
}
