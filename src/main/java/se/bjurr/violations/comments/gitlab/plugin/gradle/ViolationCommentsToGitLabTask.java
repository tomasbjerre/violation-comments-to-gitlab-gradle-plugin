package se.bjurr.violations.comments.gitlab.plugin.gradle;

import static org.gitlab.api.AuthMethod.HEADER;
import static org.gitlab.api.AuthMethod.URL_PARAMETER;
import static org.gitlab.api.TokenType.ACCESS_TOKEN;
import static org.gitlab.api.TokenType.PRIVATE_TOKEN;
import static se.bjurr.violations.comments.gitlab.lib.ViolationCommentsToGitLabApi.violationCommentsToGitLabApi;
import static se.bjurr.violations.lib.ViolationsApi.violationsApi;
import static se.bjurr.violations.lib.model.SEVERITY.INFO;

import java.util.ArrayList;
import java.util.List;
import org.gitlab.api.AuthMethod;
import org.gitlab.api.TokenType;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import se.bjurr.violations.lib.model.SEVERITY;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.reports.Parser;
import se.bjurr.violations.lib.util.Filtering;

public class ViolationCommentsToGitLabTask extends DefaultTask {

  private List<List<String>> violations = new ArrayList<>();
  private boolean commentOnlyChangedContent = true;
  private boolean createCommentWithAllSingleFileComments = true;
  private String gitLabUrl;
  private String apiToken;
  private String projectId;
  private String mergeRequestId;
  private Boolean ignoreCertificateErrors = true;
  private Boolean apiTokenPrivate = true;
  private Boolean authMethodHeader = true;
  private SEVERITY minSeverity = INFO;
  private Boolean keepOldComments = false;
  private Boolean shouldSetWip = false;

  public void setCommentOnlyChangedContent(boolean commentOnlyChangedContent) {
    this.commentOnlyChangedContent = commentOnlyChangedContent;
  }

  public void setCreateCommentWithAllSingleFileComments(
      boolean createCommentWithAllSingleFileComments) {
    this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
  }

  public void setGitLabUrl(String gitLabUrl) {
    this.gitLabUrl = gitLabUrl;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public void setMergeRequestId(String mergeRequestId) {
    this.mergeRequestId = mergeRequestId;
  }

  public void setIgnoreCertificateErrors(Boolean ignoreCertificateErrors) {
    this.ignoreCertificateErrors = ignoreCertificateErrors;
  }

  public void setApiTokenPrivate(Boolean apiTokenPrivate) {
    this.apiTokenPrivate = apiTokenPrivate;
  }

  public void setAuthMethodHeader(Boolean authMethodHeader) {
    this.authMethodHeader = authMethodHeader;
  }

  public void setMinSeverity(SEVERITY minSeverity) {
    this.minSeverity = minSeverity;
  }

  public void setKeepOldComments(Boolean keepOldComments) {
    this.keepOldComments = keepOldComments;
  }

  public void setShouldSetWip(Boolean shouldSetWip) {
    this.shouldSetWip = shouldSetWip;
  }

  public void setViolations(List<List<String>> violations) {
    this.violations = violations;
  }

  @TaskAction
  public void gitChangelogPluginTasks() throws TaskExecutionException {
    getProject().getExtensions().findByType(ViolationCommentsToGitLabPluginExtension.class);
    if (mergeRequestId == null || mergeRequestId.isEmpty()) {
      getLogger().info("No merge request id defined, will not send violation comments to GitLab.");
      return;
    }

    getLogger()
        .info(
            "Will comment project " + projectId + " and MR " + mergeRequestId + " on " + gitLabUrl);

    List<Violation> allParsedViolations = new ArrayList<>();
    for (final List<String> configuredViolation : violations) {
      final String reporter = configuredViolation.size() >= 4 ? configuredViolation.get(3) : null;

      final List<Violation> parsedViolations =
          violationsApi() //
              .findAll(Parser.valueOf(configuredViolation.get(0))) //
              .inFolder(configuredViolation.get(1)) //
              .withPattern(configuredViolation.get(2)) //
              .withReporter(reporter) //
              .violations();
      if (minSeverity != null) {
        allParsedViolations = Filtering.withAtLEastSeverity(allParsedViolations, minSeverity);
      }
      allParsedViolations.addAll(parsedViolations);
    }

    try {
      final TokenType tokenType = apiTokenPrivate ? PRIVATE_TOKEN : ACCESS_TOKEN;
      final AuthMethod authMethod = authMethodHeader ? HEADER : URL_PARAMETER;
      final Integer mergeRequestIdInteger = Integer.parseInt(mergeRequestId);
      violationCommentsToGitLabApi() //
          .setHostUrl(gitLabUrl) //
          .setProjectId(projectId) //
          .setMergeRequestId(mergeRequestIdInteger) //
          .setApiToken(apiToken) //
          .setTokenType(tokenType) //
          .setMethod(authMethod) //
          .setCommentOnlyChangedContent(commentOnlyChangedContent) //
          .setCreateCommentWithAllSingleFileComments(createCommentWithAllSingleFileComments) //
          /**
           * Cannot yet support single file comments because the API does not support it.
           * https://gitlab.com/gitlab-org/gitlab-ce/issues/14850
           */
          .setIgnoreCertificateErrors(ignoreCertificateErrors) //
          .setViolations(allParsedViolations) //
          .setShouldKeepOldComments(keepOldComments) //
          .setShouldSetWIP(shouldSetWip) //
          .toPullRequest();
    } catch (final Exception e) {
      getLogger().error("", e);
    }
  }
}
