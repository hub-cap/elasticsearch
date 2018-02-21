package org.elasticsearch.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input

class UpdateVersionTask extends DefaultTask {
  @Input
  VersionCollection bwcVersions
  private File checkoutDir
  private Task cloneTask

  UpdateVersionTask() {
    checkoutDir = new File("${project.buildDir}/bwc/checkout")
    cloneTask = createCloneTask()
  }

  private Task createCloneTask() {
    // TODO: When this is called it ensures a fresh checkout of elasticsearch
    Exec cloneTask = project.tasks.create(
        name: "UpdateVersionCloneElasticsearchTask",
        type: LoggedExec.class)
    cloneTask.commandLine('git', 'clone', 'git@github.com:elastic/elasticsearch', checkoutDir)
    return cloneTask
  }

  private Task createBranchTask(String branchName, String startPoint) {
    Exec branchTask = project.tasks.create(
        name: "Branch${branchName}From${startPoint}",
        type: LoggedExec.class,
        dependsOn: cloneTask
    )
    branchTask.commandLine('git', '-C', checkoutDir, 'branch', branchName, startPoint)
    return branchTask
  }

  /**
   * freezes a major. This assumes the major is on master. It will create a new branch for major.0 and major.x, making major.x the major.1
   * of the series. It will then make current major+1 alpha1.
   * @param freezeVersion
   */
  void stageMajor() {
    if (bwcVersions.nextMinorSnapshot == null) {
      throw new GradleException("This version does not appear to be an unreleased major version from master")
    }

    Task branchNextBugfix = createBranchTask("${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor}", 'master')
    Task branchNextMinor = createBranchTask("${bwcVersions.currentVersion.major}.x", "${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor}")

    dependsOn(branchNextBugfix, branchNextMinor)

    println("branch ${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor} from master")//
    println("make current = ${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor}.0-beta1. it should still be at alphaX")
    println("remove ${bwcVersions.nextMinorSnapshot.major}.${bwcVersions.nextMinorSnapshot.minor}.${bwcVersions.nextMinorSnapshot.revision}")
    println("branch ${bwcVersions.currentVersion.major}.x in git from ${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor}")
    println("make current = ${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor + 1}.0. It should be at beta1")
    println("remove ${bwcVersions.nextMinorSnapshot.major}.${bwcVersions.nextMinorSnapshot.minor}.${bwcVersions.nextMinorSnapshot.revision}")
    println("checkout master")
    println("make current = ${bwcVersions.currentVersion.major + 1}.0.0-alpha1")
    println("add ${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor}.0-beta1, ${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor + 1}.0")
    println("remove ${bwcVersions.nextMinorSnapshot.major}.${bwcVersions.nextMinorSnapshot.minor}.${bwcVersions.nextMinorSnapshot.revision}")
  }

  /**
   * freezes a minor. This assumes a major.x exists and it is already set to the feature freeze version. This will bump the major.x
   * version and create a branch for the current major.x version.
   * @param freezeVersion
   */
  void stageMinor() {
    if (bwcVersions.nextMinorSnapshot == null) {
      throw new GradleException("This version does not have a minor snapshot to branch from")
    }
    println("branch ${bwcVersions.nextMinorSnapshot.major}.${bwcVersions.nextMinorSnapshot.minor} from ${bwcVersions.nextMinorSnapshot.major}.x")
    println("current is unchanged, as it is already the value of ${bwcVersions.nextMinorSnapshot.major}.x")
    println("checkout ${bwcVersions.nextMinorSnapshot.major}.x")
    println("make current = ${bwcVersions.nextMinorSnapshot.major}.${bwcVersions.nextMinorSnapshot.minor + 1}.0")
    println("checkout master")
    println("add ${bwcVersions.nextMinorSnapshot.major}.${bwcVersions.nextMinorSnapshot.minor + 1}.0")
  }

  void releaseMaintenanceBugfix() {
    if (bwcVersions.nextMinorSnapshot == null || bwcVersions.nextBugfixSnapshot == null || bwcVersions.maintenanceBugfixSnapshot == null) {
      throw new GradleException("This version does not appear to be an unreleased major version from master")
    }
    // master does not get maint bugfix releases due to the limitations of wire/index compat being n-1 versions
    println("checkout ${bwcVersions.nextMinorSnapshot.major}.x")
    println("add ${bwcVersions.maintenanceBugfixSnapshot.major}.${bwcVersions.maintenanceBugfixSnapshot.minor}.${bwcVersions.maintenanceBugfixSnapshot.revision + 1}")
    // a staged minor can be null during a bugfix release
    if (bwcVersions.stagedMinorSnapshot != null) {
      println("checkout ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor}")
      println("add ${bwcVersions.maintenanceBugfixSnapshot.major}.${bwcVersions.maintenanceBugfixSnapshot.minor}.${bwcVersions.maintenanceBugfixSnapshot.revision + 1}")
    }
    println("checkout ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}")
    println("add ${bwcVersions.maintenanceBugfixSnapshot.major}.${bwcVersions.maintenanceBugfixSnapshot.minor}.${bwcVersions.maintenanceBugfixSnapshot.revision + 1}")
    println("checkout ${bwcVersions.maintenanceBugfixSnapshot.major}.${bwcVersions.maintenanceBugfixSnapshot.minor}")
    println("make current = ${bwcVersions.maintenanceBugfixSnapshot.major}.${bwcVersions.maintenanceBugfixSnapshot.minor}.${bwcVersions.maintenanceBugfixSnapshot.revision + 1}")
  }

  /**
   * Release a next bugfix. This assumes a versions.nextMinorSnapshot
   */
  void releaseNextBugfix() {
    if (bwcVersions.nextMinorSnapshot == null || bwcVersions.nextBugfixSnapshot == null) {
      throw new GradleException("This version does not appear to be an unreleased major version from master")
    }
    println("checkout master")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision + 1}")
    println("checkout ${bwcVersions.nextMinorSnapshot.major}.x")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision + 1}")
    // a staged minor can be null during a bugfix release
    if (bwcVersions.stagedMinorSnapshot != null) {
      println("checkout ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor}")
      println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision + 1}")
    }
    println("checkout ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}")
    println("make current = ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision + 1}")
  }

  /**
   * Release a staged minor. This assumes a versions.nextMinorSnapshot also exists
   */
  void releaseStagedMinor() {
    if (bwcVersions.nextMinorSnapshot == null || bwcVersions.stagedMinorSnapshot == null) {
      throw new GradleException("Could not find a next minor or staged minor snapshot")
    }
    println("checkout master")
    println("add ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor}.${bwcVersions.stagedMinorSnapshot.revision + 1}")
    println("remove ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor - 1}.latest")
    println("checkout ${bwcVersions.nextMinorSnapshot.major}.x")
    println("add ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor}.${bwcVersions.stagedMinorSnapshot.revision + 1}")
    println("remove ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor - 1}.latest")
    println("checkout ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor}")
    println("make current = ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor}.${bwcVersions.stagedMinorSnapshot.revision + 1}")
    println("do not remove ${bwcVersions.stagedMinorSnapshot.major}.${bwcVersions.stagedMinorSnapshot.minor - 1}.latest, in case of emergency release")
  }

  /**
   * Release an alpha. Assumes the versions.nextBugfixSnapshot is the version being released
   */
  void releaseAlpha() {
    String alphaSuffix = "-alpha"
    // there will not be a staged minor
    if (bwcVersions.nextMinorSnapshot == null || bwcVersions.nextBugfixSnapshot == null) {
      throw new GradleException("This version does not appear to be an unreleased major version from master")
    }
    // determine the current status
    if (bwcVersions.currentVersion.suffix == null || bwcVersions.currentVersion.suffix.startsWith(alphaSuffix) == false) {
      throw new GradleException("Could not release an alpha from ${bwcVersions.currentVersion}")
    }

    int nextAlpha = bwcVersions.currentVersion.suffix.substring(alphaSuffix.length()).toInteger() + 1

    println("checkout master")
    println("make current = ${bwcVersions.currentVersion.major}.${bwcVersions.currentVersion.minor}.${bwcVersions.currentVersion.revision}${alphaSuffix}${nextAlpha}")
  }

  void releaseBeta() {
    String betaSuffix = "-beta"
    // there will not be a staged minor
    if (bwcVersions.nextMinorSnapshot == null || bwcVersions.nextBugfixSnapshot == null) {
      throw new GradleException("Could not find a next minor or staged minor snapshot")
    }

    // determine the current status
    // specifically do not check for alpha because a stage task will take care of creating the first beta when it branches
    if (bwcVersions.nextBugfixSnapshot.suffix == null || bwcVersions.nextBugfixSnapshot.suffix.startsWith(betaSuffix) == false) {
      throw new GradleException("Could not release a new from ${bwcVersions.nextBugfixSnapshot} with suffix ${betaSuffix}")
    }

    int nextBeta = bwcVersions.nextBugfixSnapshot.suffix.substring(betaSuffix.length()).toInteger() + 1

    println("checkout master")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}${betaSuffix}${nextBeta}")
    println("checkout ${bwcVersions.nextMinorSnapshot.major}.x")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}${betaSuffix}${nextBeta}")
    println("checkout ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}")
    println("make current = ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}${betaSuffix}${nextBeta}")
  }

  void releaseRC() {
    String betaSuffix = "-beta"
    String rcSuffix = "-rc"
    Integer nextRc = 0
    // there will not be a staged minor
    if (bwcVersions.nextMinorSnapshot == null || bwcVersions.nextBugfixSnapshot == null) {
      throw new GradleException("Could not find a next minor or staged minor snapshot")
    }

    if (bwcVersions.nextBugfixSnapshot.suffix == null) {
      throw new GradleException("Could not release a new from ${bwcVersions.nextBugfixSnapshot} with suffix ${rcSuffix}")
    }

    if (bwcVersions.nextBugfixSnapshot.suffix.startsWith(betaSuffix)) {
      // This is a reset from betas over to RCs
      nextRc = 1
    } else if (bwcVersions.nextBugfixSnapshot.suffix.startsWith(rcSuffix)) {
      nextRc = bwcVersions.nextBugfixSnapshot.suffix.substring(rcSuffix.length()).toInteger() + 1
    } else {
      throw new GradleException("Could not release a new from ${bwcVersions.nextBugfixSnapshot} with suffix ${rcSuffix}")
    }

    println("checkout master")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}${rcSuffix}${nextRc}")
    println("checkout ${bwcVersions.nextMinorSnapshot.major}.x")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}${rcSuffix}${nextRc}")
    println("checkout ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}")
    println("make current = ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}${rcSuffix}${nextRc}")
  }

  void releaseMajor() {
    if (bwcVersions.nextMinorSnapshot == null || bwcVersions.nextBugfixSnapshot == null) {
      throw new GradleException("Could not find a next minor or staged minor snapshot")
    }
    // by this time the major version is in the nextBugfix, due to the special casing of
    println("checkout master")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision + 1}")
    println("checkout ${bwcVersions.nextMinorSnapshot.major}.x")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision + 1}")
    println("checkout ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}")
    println("add ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision}")
    println("make current = ${bwcVersions.nextBugfixSnapshot.major}.${bwcVersions.nextBugfixSnapshot.minor}.${bwcVersions.nextBugfixSnapshot.revision + 1}")
  }
}
