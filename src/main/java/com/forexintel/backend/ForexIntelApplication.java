package com.forexintel.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée principal de l'application monolithique modulaire Forex Intel.
 *
 * @author Innocent
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class ForexIntelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForexIntelApplication.class, args);
    }
}
