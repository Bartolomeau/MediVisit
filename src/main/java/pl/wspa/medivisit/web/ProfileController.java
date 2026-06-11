package pl.wspa.medivisit.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.wspa.medivisit.dao.UserDao;
import pl.wspa.medivisit.model.User;
import pl.wspa.medivisit.util.PasswordUtil;
import pl.wspa.medivisit.util.Validators;

@Controller
public class ProfileController {

    private final UserDao userDao = new UserDao();

    private User user(HttpSession session) {
        return (User) session.getAttribute(AuthInterceptor.SESSION_USER);
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String firstName, @RequestParam String lastName,
                                @RequestParam String phone, HttpSession session,
                                RedirectAttributes redirect) {
        User user = user(session);
        if (!Validators.isNotBlank(firstName) || !Validators.isNotBlank(lastName)) {
            redirect.addFlashAttribute("error", "Imię i nazwisko nie mogą być puste.");
            return "redirect:/profile";
        }
        if (!Validators.isValidPhone(phone)) {
            redirect.addFlashAttribute("error", "Telefon musi składać się z dokładnie 9 cyfr.");
            return "redirect:/profile";
        }
        userDao.updateProfile(user.getId(), firstName, lastName, phone.trim());
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPhone(phone.trim());
        redirect.addFlashAttribute("success", "Dane profilu zostały zapisane.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String current, @RequestParam String newPassword,
                                 @RequestParam String confirm, HttpSession session,
                                 RedirectAttributes redirect) {
        User user = user(session);
        if (!PasswordUtil.verify(current, user.getPasswordHash())) {
            redirect.addFlashAttribute("errorPass", "Obecne hasło jest nieprawidłowe.");
        } else if (!Validators.isValidPassword(newPassword)) {
            redirect.addFlashAttribute("errorPass", "Nowe hasło: min. 8 znaków, litera i cyfra.");
        } else if (!newPassword.equals(confirm)) {
            redirect.addFlashAttribute("errorPass", "Nowe hasła nie są identyczne.");
        } else {
            String hash = PasswordUtil.hash(newPassword);
            userDao.updatePassword(user.getId(), hash);
            user.setPasswordHash(hash);
            redirect.addFlashAttribute("success", "Hasło zostało zmienione.");
        }
        return "redirect:/profile";
    }
}
