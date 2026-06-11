package pl.wspa.medivisit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pl.wspa.medivisit.db.Database;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        Database.init();
        SpringApplication.run(Main.class, args);
    }
}
