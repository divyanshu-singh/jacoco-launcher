package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class JacocoServiceLauncher {

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Config config = mapper.readValue(new File("jacoco-config.json"), Config.class);
        JacocoConfig jacoco = config.getJacoco();

        String agentArgs = String.format(
                "-javaagent:%s=destfile=%s,output=tcpserver,port=%d,includes=%s,excludes=%s",
                jacoco.getAgentJar(),
                jacoco.getDestFile(),
                jacoco.getPort(),
                String.join(",", jacoco.getIncludes()),
                String.join(",", jacoco.getExcludes())
        );

        List<String> command = List.of(
                "java",
                agentArgs,
                "-jar",
                config.getService().getJar()
        );

        System.out.println("➡️ Launching service:");
        System.out.println(String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process serviceProcess = pb.start();

        System.out.println("▶️ Service running. Press ENTER to stop and generate report...");
        System.in.read();
        serviceProcess.destroy(); // or wait for serviceProcess.waitFor() if you prefer auto stop

        generateReport(jacoco);
        if (config.getSonar().isEnabled()) {
            uploadToSonar(config.getSonar());
        }
    }

    private static void generateReport(JacocoConfig jacoco) throws Exception {
        System.out.println("🧪 Generating JaCoCo report...");

        // ✅ Create report output directory if missing
        File reportDir = new File(jacoco.getReportDir());
        if (!reportDir.exists()) {
            boolean created = reportDir.mkdirs();
            if (!created) {
                System.out.println("❌ Failed to create report directory: " + reportDir.getAbsolutePath());
                return;
            }
        }

        List<String> command = List.of(
                "java",
                "-jar", "target/jacococli.jar",
                "report", jacoco.getDestFile(),
                "--classfiles", jacoco.getClassDir(),
                "--sourcefiles", jacoco.getSourceDir(),
                "--html", jacoco.getReportDir() + "/html",
                "--xml", jacoco.getReportDir() + "/jacoco-report.xml"
        );

        System.out.println("📦 Running: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        int exitCode = p.waitFor();

        if (exitCode == 0) {
            System.out.println("✅ JaCoCo report generated at: " + jacoco.getReportDir());
        } else {
            System.out.println("❌ JaCoCo CLI failed with exit code: " + exitCode);
        }
    }

    private static void uploadToSonar(SonarConfig sonar) throws IOException, InterruptedException {
        System.out.println("☁️ Uploading coverage to SonarQube...");

        List<String> command = List.of(
                sonar.getScannerPath(),
                "-Dproject.settings=" + sonar.getProjectProperties()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();

        System.out.println("🎉 SonarQube upload complete.");
    }

    // JSON config classes
    public static class Config {
        private JacocoConfig jacoco;
        private ServiceConfig service;
        private SonarConfig sonar;
        public JacocoConfig getJacoco() { return jacoco; }
        public ServiceConfig getService() { return service; }
        public SonarConfig getSonar() { return sonar; }
    }

    public static class JacocoConfig {
        private String agentJar;
        private String destFile;
        private int port;
        private List<String> includes;
        private List<String> excludes;
        private String classDir;
        private String sourceDir;
        private String reportDir;

        public String getAgentJar() { return agentJar; }
        public String getDestFile() { return destFile; }
        public int getPort() { return port; }
        public List<String> getIncludes() { return includes; }
        public List<String> getExcludes() { return excludes; }
        public String getClassDir() { return classDir; }
        public String getSourceDir() { return sourceDir; }
        public String getReportDir() { return reportDir; }
    }

    public static class ServiceConfig {
        private String jar;
        public String getJar() { return jar; }
    }

    public static class SonarConfig {
        private boolean enabled;
        private String scannerPath;
        private String projectProperties;

        public boolean isEnabled() { return enabled; }
        public String getScannerPath() { return scannerPath; }
        public String getProjectProperties() { return projectProperties; }
    }
}
