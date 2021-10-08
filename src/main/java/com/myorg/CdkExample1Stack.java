package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;

public class CdkExample1Stack extends Stack {
    public CdkExample1Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkExample1Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "ECS VPC").maxAzs(3).build();

        Cluster cluster = Cluster.Builder.create(this, "Cluster1")
                .vpc(vpc)
                .capacity(AddCapacityOptions.builder()
                        .associatePublicIpAddress(true)
                        .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                        //.maxCapacity(1000)
                        .instanceType(new InstanceType("t2.small"))
                        .build())
                .build();

        // Create a load-balanced Fargate service and make it public
        ApplicationLoadBalancedFargateService service = ApplicationLoadBalancedFargateService.Builder.create(this, "MyFargateService")
                .cluster(cluster)           // Required
                .cpu(512)                   // Default is 256
                .desiredCount(1)            // Default is 1
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromEcrRepository(Repository.fromRepositoryArn(this, "ContainerRepo", "arn:aws:ecr:us-west-2:909857523496:repository/test-ecs"), "latest"))
                                .build())
                .memoryLimitMiB(2048)       // Default is 512
                .publicLoadBalancer(true)   // Default is false
                .build();
        service.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                        .protocol(Protocol.HTTP)
                        .port("80")
                        .path("/actuator/health")
                        .healthyThresholdCount(5)
                        .unhealthyThresholdCount(2)
                        .timeout(Duration.seconds(5))
                        .interval(Duration.seconds(30))
                        .build());

    }
}
