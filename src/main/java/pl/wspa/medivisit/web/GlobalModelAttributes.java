package pl.wspa.medivisit.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pl.wspa.medivisit.model.User;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("user")
    public User user(HttpSession session) {
        return (User) session.getAttribute(AuthInterceptor.SESSION_USER);
    }
}
