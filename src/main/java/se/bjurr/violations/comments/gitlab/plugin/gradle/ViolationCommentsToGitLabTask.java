package se.bjurr.violations.comments.gitlab.plugin.gradle;

import static se.bjurr.violations.comments.gitlab.lib.ViolationCommentsToGitLabApi.violationCommentsToGitLabApi;
import static se.bjurr.violations.lib.ViolationsApi.violationsApi;
import static se.bjurr.violations.lib.model.SEVERITY.INFO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.gitlab4j.api.Constants.TokenType;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import se.bjurr.violations.lib.ViolationsLogger;
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
    this.getProject().getExtensions().findByType(ViolationCommentsToGitLabPluginExtension.class);
    if (this.mergeRequestIid == null || this.mergeRequestIid.isEmpty()) {
      this.getLogger()
          .info("No merge request iid defined, will not send violation comments to GitLab.");
      return;
    }

    this.getLogger()
        .info(
            "Will comment project "
                + this.projectId
                + " and MR "
                + this.mergeRequestIid
                + " on "
                + this.gitLabUrl);

    final ViolationsLogger violationsLogger =
        new ViolationsLogger() {
          private LogLevel toGradleLogLevel(final Level level) {
            LogLevel gradleLevel = LogLevel.INFO;
            if (level == Level.FINE) {
              gradleLevel = LogLevel.DEBUG;
            } else if (level == Level.SEVERE) {
              gradleLevel = LogLevel.ERROR;
            } else if (level == Level.WARNING) {
              gradleLevel = LogLevel.WARN;
            }
            return gradleLevel;
          }

          @Override
          public void log(final Level level, final String string) {
            ViolationCommentsToGitLabTask.this
                .getLogger()
                .log(this.toGradleLogLevel(level), string);
          }

          @Override
          public void log(final Level level, final String string, final Throwable t) {
            ViolationCommentsToGitLabTask.this
                .getLogger()
                .log(this.toGradleLogLevel(level), string, t);
          }
        };

    Set<Violation> allParsedViolations = new TreeSet<>();
    for (final List<String> configuredViolation : this.violations) {
      final String reporter = configuredViolation.size() >= 4 ? configuredViolation.get(3) : null;

      final Set<Violation> parsedViolations =
          violationsApi() //
              .withViolationsLogger(violationsLogger) //
              .findAll(Parser.valueOf(configuredViolation.get(0))) //
              .inFolder(configuredViolation.get(1)) //
              .withPattern(configuredViolation.get(2)) //
              .withReporter(reporter) //
              .violations();
      if (this.minSeverity != null) {
        allParsedViolations = Filtering.withAtLEastSeverity(allParsedViolations, this.minSeverity);
      }
      allParsedViolations.addAll(parsedViolations);
    }

    try {
      final TokenType tokenType = this.apiTokenPrivate ? TokenType.PRIVATE : TokenType.ACCESS;
      final Integer mergeRequestIidInteger = Integer.parseInt(this.mergeRequestIid);
      violationCommentsToGitLabApi()
          .setViolationsLogger(violationsLogger)
          .setHostUrl(this.gitLabUrl)
          .setProjectId(this.projectId)
          .setMergeRequestIid(mergeRequestIidInteger)
          .setApiToken(this.apiToken)
          .setTokenType(tokenType)
          .setCommentOnlyChangedContent(this.commentOnlyChangedContent) //
          .withShouldCommentOnlyChangedFiles(this.commentOnlyChangedFiles) //
          .setCreateCommentWithAllSingleFileComments(
              this.createCommentWithAllSingleFileComments) //
          .setCreateSingleFileComments(this.createSingleFileComments) //
          .setIgnoreCertificateErrors(this.ignoreCertificateErrors) //
          .setViolations(allParsedViolations) //
          .setShouldKeepOldComments(this.keepOldComments) //
          .setShouldSetWIP(this.shouldSetWip) //
          .setCommentTemplate(this.commentTemplate) //
          .setProxyServer(this.proxyServer) //
          .setProxyUser(this.proxyUser) //
          .setProxyPassword(this.proxyPassword) //
          .setMaxNumberOfViolations(this.maxNumberOfComments) //
          .toPullRequest();
    } catch (final Exception e) {
      this.getLogger().error("", e);
    }
  }
}
