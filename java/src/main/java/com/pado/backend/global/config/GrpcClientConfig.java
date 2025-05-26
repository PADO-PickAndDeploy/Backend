package com.pado.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import provision.ProvisioningServiceGrpc;

@Configuration
public class GrpcClientConfig {

    @Bean
    public ManagedChannel channel() {
        return ManagedChannelBuilder.forAddress("localhost", 50051)
            .usePlaintext()
            .build();
    }

    @Bean
    public ProvisioningServiceGrpc.ProvisioningServiceBlockingStub provisioningStub(ManagedChannel channel) {
        return ProvisioningServiceGrpc.newBlockingStub(channel);
    }
}