# JaCoCo + SonarQube Integration for Code Coverage (QA Perspective)

## Introduction
Code coverage is a crucial quality metric in any QA strategy. It ensures that our **automated test suites (unit, integration, API tests)** are exercising the important parts of the codebase.

This guide explains how to set up **JaCoCo** (Java Code Coverage) with **SonarQube** for code coverage analysis, and how to integrate it into a **CI/CD pipeline**.

---

## Tools We’ll Use
- **JaCoCo Agent** → instruments Java code and records runtime coverage  
- **JaCoCo CLI** → converts raw coverage (`.exec`) into reports  
- **SonarQube** → central dashboard for code quality & coverage metrics  
- **CI/CD (Jenkins, GitHub Actions, GitLab CI, etc.)** → automation  

---

## Workflow Overview

### 1. Run Service with JaCoCo Agent
Start your service with the agent attached:

```bash
java -javaagent:libs/jacocoagent.jar=destfile=jacoco.exec,includes=com.yourcompany.* \
     -jar target/your-service.jar

destfile → location of coverage execution data
includes / excludes → control which packages to measure

2. Execute Automated Tests

Start your service with the JaCoCo agent.
Run your JUnit/TestNG/API tests (from the same repo or a QA repo).
Coverage data is captured in jacoco.exec.

3. Generate Coverage Reports

Convert raw data into reports:

java -jar libs/jacococli.jar report jacoco.exec \
     --classfiles target/classes \
     --sourcefiles src/main/java \
     --html report/html \
     --xml report/jacoco.xml

HTML report → for local browsing
XML report → required by SonarQube

4. Upload to SonarQube

Configure sonar-project.properties:

sonar.projectKey=my-service
sonar.sources=src/main/java
sonar.java.binaries=target/classes
sonar.coverage.jacoco.xmlReportPaths=report/jacoco.xml

Run analysis:
bash
sonar-scanner

Or using Maven:
mvn sonar:sonar \
  -Dsonar.projectKey=my-service \
  -Dsonar.coverage.jacoco.xmlReportPaths=report/jacoco.xml

CI/CD Example (GitHub Actions)
name: Build and Coverage

on: [push, pull_request]

jobs:
  coverage:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build Service
        run: mvn -f service/pom.xml clean package -DskipTests

      - name: Start Service with JaCoCo Agent
        run: java -javaagent:libs/jacocoagent.jar=destfile=jacoco.exec,includes=com.yourcompany.* \
                 -jar service/target/your-service.jar &

      - name: Run Tests
        run: mvn -f tests/pom.xml test

      - name: Generate Coverage Report
        run: |
          java -jar libs/jacococli.jar report jacoco.exec \
               --classfiles service/target/classes \
               --sourcefiles service/src/main/java \
               --xml report/jacoco.xml \
               --html report/html

      - name: SonarQube Analysis
        run: sonar-scanner

QA Benefits

Visibility → See which code paths your tests actually cover
Automation → Reports are auto-generated in CI/CD
Shift Left → Developers & QA get fast feedback on PRs
Quality Gates → SonarQube can block merges if coverage < threshold

Next Steps

Exclude generated/boilerplate code from coverage
Configure SonarQube Quality Gates to enforce coverage standards
Track branch & condition coverage, not just line coverage

Outcome
With this setup, QA teams get automated coverage tracking and SonarQube dashboards for
visibility, helping improve test effectiveness and code quality.
