package org.zhd.purchase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.xy.api.dpi.workflow.ProcessApi;
import org.xy.api.utils.GlobalExceptionController;

/**
 * @author juny
 */
@SpringBootApplication
@EnableFeignClients(basePackageClasses = {ProcessApi.class})
@EnableEurekaClient
@EnableDiscoveryClient
@ComponentScan(basePackages = {"org.zhd.purchase"}, basePackageClasses = {GlobalExceptionController.class})
public class ScpPurchaseServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScpPurchaseServerApplication.class, args);
    }

}
