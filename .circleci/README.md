
## Introduction

CircleCI has a workflow configured to build UAT builds once a merge is done to the master branch. In the same way, there is a workflow for production and it runs when a commit is done to any /release_* branch.


## CircleCI Project Configuration

No environment variables are needed as the gradle project is configured and the repo has all dependencies.

## Distribution

Please download the APK from the generated artefacts for distribution
