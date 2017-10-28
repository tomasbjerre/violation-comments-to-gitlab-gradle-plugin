package se.bjurr.violations.comments.gitlab.plugin.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ViolationCommentsToGitLabGradlePlugin implements Plugin<Project> {
  @Override
  public void apply(Project target) {
    target
        .getExtensions()
        .create("violationCommentsToGitHubPlugin", ViolationCommentsToGitLabPluginExtension.class);
  }
}
