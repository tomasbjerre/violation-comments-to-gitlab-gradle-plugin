package se.bjurr.violations.comments.gitlab.plugin.gradle;

import static se.bjurr.violations.comments.gitlab.lib.ViolationCommentsToGitLabApi.violationCommentsToGitLabApi;
import static se.bjurr.violations.lib.ViolationsApi.violationsApi;
import static se.bjurr.violations.lib.model.SEVERITY.INFO;

import java.util.ArrayList;
import java.util.List;
import org.gitlab4j.api.Constants.TokenType;
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
  private boolean commentOnlyChangedFiles = true;
  private boolean createCommentWithAllSingleFileComments = true;
  private boolean createSingleFileComments = true;
  private String gitLabUrl;
  private String apiToken;
  private String projectId;
  private String mergeRequestIid;
  private Boolean ignoreCertificateErrors = true;
  private Boolean apiTokenPrivate = true;
  private SEVERITY minSeverity = INFO;
  private Boolean keepOldComments = false;
  private Boolean shouldSetWip = false;
  private String commentTemplate;
  private String proxyServer;
  private String proxyUser;
  private String proxyPassword;
  private Integer maxNumberOfComments;

  public void setMaxNumberOfComments(final Integer maxNumberOfComments) {
    this.maxNumberOfComments = maxNumberOfComments;
  }

  public void setCommentOnlyChangedFiles(final boolean commentOnlyChangedFiles) {
    this.commentOnlyChangedFiles = commentOnlyChangedFiles;
  }

  public void setCommentOnlyChangedContent(final boolean commentOnlyChangedContent) {
    this.commentOnlyChangedContent = commentOnlyChangedContent;
  }

  public void setCreateCommentWithAllSingleFileComments(
      final boolean createCommentWithAllSingleFileComments) {
    this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
  }

  public void setCreateSingleFileComments(final boolean createSingleFileComments) {
    this.createSingleFileComments = createSingleFileComments;
  }

  public void setGitLabUrl(final String gitLabUrl) {
    this.gitLabUrl = gitLabUrl;
  }

  public void setApiToken(final String apiToken) {
    this.apiToken = apiToken;
  }

  public void setProjectId(final String projectId) {
    this.projectId = projectId;
  }

  public void setMergeRequestIid(final String mergeRequestIid) {
    this.mergeRequestIid = mergeRequestIid;
  }

  public void setIgnoreCertificateErrors(final Boolean ignoreCertificateErrors) {
    this.ignoreCertificateErrors = ignoreCertificateErrors;
  }

  public void setApiTokenPrivate(final Boolean apiTokenPrivate) {
    this.apiTokenPrivate = apiTokenPrivate;
  }

  public void setMinSeverity(final SEVERITY minSeverity) {
    this.minSeverity = minSeverity;
  }

  public void setKeepOldComments(final Boolean keepOldComments) {
    this.keepOldComments = keepOldComments;
  }

  public void setShouldSetWip(final Boolean shouldSetWip) {
    this.shouldSetWip = shouldSetWip;
  }

  public void setViolations(final List<List<String>> violations) {
    this.violations = violations;
  }

  public void setCommentTemplate(final String commentTemplate) {
    this.commentTemplate = commentTemplate;
  }

  public void setProxyServer(final String proxyServer) {
    this.proxyServer = proxyServer;
  }

  public void setProxyUser(final String proxyUser) {
    this.proxyUser = proxyUser;
  }

  public void setProxyPassword(final String proxyPassword) {
    this.proxyPassword = proxyPassword;
  }

  @TaskAction
  public void gitChangelogPluginTasks() throws TaskExecutionException {
    getProject().getExtensions().findByType(ViolationCommentsToGitLabPluginExtension.class);
    if (mergeRequestIid == null || mergeRequestIid.isEmpty()) {
      getLogger().info("No merge request iid defined, will not send violation comments to GitLab.");
      return;
    }

    getLogger()
        .info(
            "Will comment project "
                + projectId
                + " and MR "
                + mergeRequestIid
                + " on "
                + gitLabUrl);

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
      final TokenType tokenType = apiTokenPrivate ? TokenType.PRIVATE : TokenType.ACCESS;
      final Integer mergeRequestIidInteger = Integer.parseInt(mergeRequestIid);
      violationCommentsToGitLabApi() //
          .setHostUrl(gitLabUrl) //
          .setProjectId(projectId) //
          .setMergeRequestIid(mergeRequestIidInteger) //
          .setApiToken(apiToken) //
          .setTokenType(tokenType) //
          .setCommentOnlyChangedContent(commentOnlyChangedContent) //
          .withShouldCommentOnlyChangedFiles(commentOnlyChangedFiles) //
          .setCreateCommentWithAllSingleFileComments(createCommentWithAllSingleFileComments) //
          .setCreateSingleFileComments(createSingleFileComments) //
          .setIgnoreCertificateErrors(ignoreCertificateErrors) //
          .setViolations(allParsedViolations) //
          .setShouldKeepOldComments(keepOldComments) //
          .setShouldSetWIP(shouldSetWip) //
          .setCommentTemplate(commentTemplate) //
          .setProxyServer(proxyServer) //
          .setProxyUser(proxyUser) //
          .setProxyPassword(proxyPassword) //
          .setMaxNumberOfViolations(maxNumberOfComments) //
          .toPullRequest();
    } catch (final Exception e) {
      getLogger().error("", e);
    }
  }
}
