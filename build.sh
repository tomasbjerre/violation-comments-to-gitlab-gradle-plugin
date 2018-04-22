#!/bin/bash
./gradlew clean cE eclipse build install gitChangelogTask || exit 1
cd violation-comments-to-gitlab-gradle-plugin-example

#
# Get project id: curl -XGET "https://gitlab.com/api/v4/projects/tomas.bjerre85%2Fviolations-test"
# MR: https://gitlab.com/api/v4/projects/2732496/merge_requests
#
./gradlew violationCommentsToGitLab -DGITLAB_URL=https://gitlab.com/ -DGITLAB_MERGEREQUESTIID=1 -DGITLAB_PROJECTID=2732496 -DGITLAB_APITOKEN=$GITLAB_APITOKEN -i --stacktrace
