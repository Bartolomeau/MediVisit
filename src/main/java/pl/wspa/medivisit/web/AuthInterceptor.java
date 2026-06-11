package pl.wspa.medivisit.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import pl.wspa.medivisit.model.User;

/**
 * Kontrola dostepu: wymaga zalogowania, a sciezki /patient, /doctor i /admin
 * sa dostepne tylko dla odpowiedniej roli.
 */
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER = "user";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SESSION_USER);
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }
        String path = request.getRequestURI();
        String requiredRole = null;
        if (path.startsWith("/patient")) requiredRole = User.ROLE_PATIENT;
        else if (path.startsWith("/doctor")) requiredRole = User.ROLE_DOCTOR;
        else if (path.startsWith("/admin")) requiredRole = User.ROLE_ADMIN;

        if (requiredRole != null && !requiredRole.equals(user.getRole())) {
            response.sendRedirect("/");
            return false;
        }
        return true;
    }
}
