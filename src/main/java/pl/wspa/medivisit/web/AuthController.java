package pl.wspa.medivisit.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.wspa.medivisit.dao.UserDao;
import pl.wspa.medivisit.model.User;
import pl.wspa.medivisit.util.PasswordUtil;
import pl.wspa.medivisit.util.Validators;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserDao userDao = new UserDao();

    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user == null) {
            return "redirect:/login";
        }
        return switch (user.getRole()) {
            case User.ROLE_ADMIN -> "redirect:/admin/stats";
            case User.ROLE_DOCTOR -> "redirect:/doctor/day";
            default -> "redirect:/patient/book";
        };
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute(AuthInterceptor.SESSION_USER) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpSession session, Model model) {
        if (!Validators.isNotBlank(email) || !Validators.isNotBlank(password)) {
            model.addAttribute("error", "Podaj adres e-mail i hasło.");
            return "login";
        }
        Optional<User> user = userDao.findByEmail(email);
        if (user.isEmpty() || !PasswordUtil.verify(password, user.get().getPasswordHash())) {
            model.addAttribute("error", "Nieprawidłowy adres e-mail lub hasło.");
            model.addAttribute("email", email);
            return "login";
        }
        session.setAttribute(AuthInterceptor.SESSION_USER, user.get());
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String firstName, @RequestParam String lastName,
                           @RequestParam String email, @RequestParam String phone,
                           @RequestParam String password, @RequestParam String confirm,
                           Model model, RedirectAttributes redirect) {
        String error = null;
        if (!Validators.isNotBlank(firstName) || !Validators.isNotBlank(lastName)) {
            error = "Podaj imię i nazwisko.";
        } else if (!Validators.isValidEmail(email)) {
            error = "Podaj poprawny adres e-mail.";
        } else if (!Validators.isValidPhone(phone)) {
            error = "Telefon musi składać się z dokładnie 9 cyfr.";
        } else if (!Validators.isValidPassword(password)) {
            error = "Hasło: min. 8 znaków, co najmniej jedna litera i cyfra.";
        } else if (!password.equals(confirm)) {
            error = "Hasła nie są identyczne.";
        } else if (userDao.findByEmail(email).isPresent()) {
            error = "Konto z tym adresem e-mail już istnieje.";
        }
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            return "register";
        }

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(email);
        user.setPhone(phone.trim());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(User.ROLE_PATIENT);
        userDao.insert(user);

        redirect.addFlashAttribute("success", "Konto utworzone – możesz się zalogować.");
        return "redirect:/login";
    }
}
