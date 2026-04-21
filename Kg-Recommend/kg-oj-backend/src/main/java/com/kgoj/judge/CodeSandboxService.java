package com.kgoj.judge;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

@Service
public class CodeSandboxService {

    private static final String IMAGE_NAME = "eclipse-temurin:17-alpine";

    public String executeJavaCode(String code) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375").build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        DockerClient dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();

        try {
            dockerClient.inspectImageCmd(IMAGE_NAME).exec();
        } catch (Exception e) {
            System.out.println("正在拉取判题专用的 Java 容器镜像，请稍候...");
            try {
                dockerClient.pullImageCmd(IMAGE_NAME).start().awaitCompletion();
            } catch (InterruptedException ex) {
                return "拉取镜像被中断";
            }
        }

        String userDir = System.getProperty("user.dir");
        String workspace = userDir + File.separator + "temp_code" + File.separator + UUID.randomUUID().toString();
        File dir = new File(workspace);
        if (!dir.exists()) dir.mkdirs();

        String resultOutput = "";
        try {
            File codeFile = new File(dir, "Main.java");
            Files.write(codeFile.toPath(), code.getBytes(StandardCharsets.UTF_8));

            HostConfig hostConfig = new HostConfig()
                    .withMemory(256 * 1024 * 1024L)
                    .withBinds(new Bind(workspace, new Volume("/app")));

            CreateContainerResponse container = dockerClient.createContainerCmd(IMAGE_NAME)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/app")
                    .withCmd("sh", "-c", "javac Main.java && java Main")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            String containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            StringBuilder output = new StringBuilder();
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            output.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                        }
                    }).awaitCompletion();

            dockerClient.waitContainerCmd(containerId).start().awaitCompletion();
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();

            resultOutput = output.toString();
        } catch (Exception e) {
            resultOutput = "判题沙箱执行异常: " + e.getMessage();
        }

        return resultOutput;
    }
}