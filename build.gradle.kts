// Kotlin konfigurasjonen er gitt av pluginen 'aap.conventions' i buildSrc
// og settings.gradle.kts

plugins {
    // Provides a no-op 'build' lifecycle task
    base
    id("aap.conventions")
}

// Merge Detekt reports from all subprojects
val detektReportMergeSarif by tasks.registering(dev.detekt.gradle.report.ReportMergeTask::class) {
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

subprojects {
    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        finalizedBy(detektReportMergeSarif)
        detektReportMergeSarif.configure {
            input.from(reports.sarif.outputLocation)
        }
    }
}

// Call the tasks of the subprojects
subprojects {
    // no-op; just ensuring subprojects are configured
}
for (taskName in listOf<String>("clean", "build", "assemble", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.path + ":$taskName" })
    }
}
