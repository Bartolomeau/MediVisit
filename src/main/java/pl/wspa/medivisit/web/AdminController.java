package pl.wspa.medivisit.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.wspa.medivisit.dao.AppointmentDao;
import pl.wspa.medivisit.dao.DoctorDao;
import pl.wspa.medivisit.dao.SpecializationDao;
import pl.wspa.medivisit.dao.UserDao;
import pl.wspa.medivisit.model.Appointment;
import pl.wspa.medivisit.model.Doctor;
import pl.wspa.medivisit.model.User;
import pl.wspa.medivisit.util.PasswordUtil;
import pl.wspa.medivisit.util.Validators;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserDao userDao = new UserDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final SpecializationDao specializationDao = new SpecializationDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    // ===================== statystyki =====================

    @GetMapping("/stats")
    public String stats(Model model) {
        long patients = userDao.findAll().stream()
                .filter(u -> User.ROLE_PATIENT.equals(u.getRole())).count();
        model.addAttribute("patients", patients);
        model.addAttribute("doctorsCount", doctorDao.findAll().size());
        model.addAttribute("planned", appointmentDao.countByStatus(Appointment.STATUS_PLANNED));
        model.addAttribute("done", appointmentDao.countByStatus(Appointment.STATUS_DONE));
        model.addAttribute("cancelled", appointmentDao.countByStatus(Appointment.STATUS_CANCELLED));

        Map<String, Integer> bySpec = appointmentDao.countBySpecialization();
        int max = bySpec.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        model.addAttribute("bySpec", bySpec);
        model.addAttribute("maxSpec", Math.max(max, 1));
        return "admin/stats";
    }

    // ===================== lekarze =====================

    @GetMapping("/doctors")
    public String doctors(Model model) {
        model.addAttribute("doctors", doctorDao.findAll());
        return "admin/doctors";
    }

    @GetMapping("/doctors/new")
    public String newDoctorForm(Model model, RedirectAttributes redirect) {
        if (specializationDao.findAll().isEmpty()) {
            redirect.addFlashAttribute("error", "Najpierw dodaj co najmniej jedną specjalizację.");
            return "redirect:/admin/specializations";
        }
        model.addAttribute("specs", specializationDao.findAll());
        model.addAttribute("isNew", true);
        return "admin/doctor-form";
    }

    @PostMapping("/doctors/new")
    public String createDoctor(@RequestParam String firstName, @RequestParam String lastName,
                               @RequestParam String email, @RequestParam String password,
                               @RequestParam int specId, @RequestParam String room,
                               @RequestParam String workStart, @RequestParam String workEnd,
                               @RequestParam int slotMinutes,
                               Model model, RedirectAttributes redirect) {
        String error = validateDoctorCommon(firstName, lastName, workStart, workEnd, slotMinutes);
        if (error == null && !Validators.isValidEmail(email)) {
            error = "Podaj poprawny adres e-mail.";
        }
        if (error == null && userDao.findByEmail(email).isPresent()) {
            error = "Konto z tym adresem e-mail już istnieje.";
        }
        if (error == null && !Validators.isValidPassword(password)) {
            error = "Hasło: min. 8 znaków, co najmniej jedna litera i cyfra.";
        }
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("specs", specializationDao.findAll());
            model.addAttribute("isNew", true);
            return "admin/doctor-form";
        }

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(email);
        user.setPhone(null);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(User.ROLE_DOCTOR);
        int userId = userDao.insert(user);
        doctorDao.insert(userId, specId, room.trim(), workStart, workEnd, slotMinutes);

        redirect.addFlashAttribute("success", "Lekarz " + firstName + " " + lastName + " został dodany.");
        return "redirect:/admin/doctors";
    }

    @GetMapping("/doctors/{id}/edit")
    public String editDoctorForm(@PathVariable int id, Model model, RedirectAttributes redirect) {
        Optional<Doctor> doctor = doctorDao.findById(id);
        if (doctor.isEmpty()) {
            redirect.addFlashAttribute("error", "Nie znaleziono lekarza.");
            return "redirect:/admin/doctors";
        }
        model.addAttribute("doctor", doctor.get());
        model.addAttribute("specs", specializationDao.findAll());
        model.addAttribute("isNew", false);
        return "admin/doctor-form";
    }

    @PostMapping("/doctors/{id}/edit")
    public String updateDoctor(@PathVariable int id,
                               @RequestParam String firstName, @RequestParam String lastName,
                               @RequestParam int specId, @RequestParam String room,
                               @RequestParam String workStart, @RequestParam String workEnd,
                               @RequestParam int slotMinutes,
                               Model model, RedirectAttributes redirect) {
        Optional<Doctor> doctor = doctorDao.findById(id);
        if (doctor.isEmpty()) {
            redirect.addFlashAttribute("error", "Nie znaleziono lekarza.");
            return "redirect:/admin/doctors";
        }
        String error = validateDoctorCommon(firstName, lastName, workStart, workEnd, slotMinutes);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("doctor", doctor.get());
            model.addAttribute("specs", specializationDao.findAll());
            model.addAttribute("isNew", false);
            return "admin/doctor-form";
        }
        userDao.updateProfile(doctor.get().getUserId(), firstName, lastName, null);
        doctorDao.update(id, specId, room.trim(), workStart, workEnd, slotMinutes);
        redirect.addFlashAttribute("success", "Dane lekarza zostały zaktualizowane.");
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/{id}/delete")
    public String deleteDoctor(@PathVariable int id, RedirectAttributes redirect) {
        Optional<Doctor> doctor = doctorDao.findById(id);
        if (doctor.isEmpty()) {
            redirect.addFlashAttribute("error", "Nie znaleziono lekarza.");
        } else {
            doctorDao.deleteWithUser(doctor.get());
            redirect.addFlashAttribute("success",
                    "Konto " + doctor.get().getFullName() + " i jego wizyty zostały usunięte.");
        }
        return "redirect:/admin/doctors";
    }

    private String validateDoctorCommon(String firstName, String lastName,
                                        String workStart, String workEnd, int slotMinutes) {
        if (!Validators.isNotBlank(firstName) || !Validators.isNotBlank(lastName)) {
            return "Podaj imię i nazwisko.";
        }
        if (!workStart.matches("\\d{2}:\\d{2}") || !workEnd.matches("\\d{2}:\\d{2}")
                || workStart.compareTo(workEnd) >= 0) {
            return "Godzina rozpoczęcia musi być wcześniejsza niż zakończenia.";
        }
        if (slotMinutes < 10 || slotMinutes > 60) {
            return "Długość wizyty: od 10 do 60 minut.";
        }
        return null;
    }

    // ===================== specjalizacje =====================

    @GetMapping("/specializations")
    public String specializations(Model model) {
        model.addAttribute("specs", specializationDao.findAll());
        return "admin/specializations";
    }

    @PostMapping("/specializations")
    public String addSpecialization(@RequestParam String name, RedirectAttributes redirect) {
        if (!Validators.isNotBlank(name)) {
            redirect.addFlashAttribute("error", "Nazwa nie może być pusta.");
        } else {
            try {
                specializationDao.insert(name);
                redirect.addFlashAttribute("success", "Specjalizacja dodana.");
            } catch (IllegalStateException e) {
                redirect.addFlashAttribute("error", "Specjalizacja o tej nazwie już istnieje.");
            }
        }
        return "redirect:/admin/specializations";
    }

    @PostMapping("/specializations/{id}/rename")
    public String renameSpecialization(@PathVariable int id, @RequestParam String name,
                                       RedirectAttributes redirect) {
        if (!Validators.isNotBlank(name)) {
            redirect.addFlashAttribute("error", "Nazwa nie może być pusta.");
        } else {
            specializationDao.update(id, name);
            redirect.addFlashAttribute("success", "Nazwa specjalizacji zmieniona.");
        }
        return "redirect:/admin/specializations";
    }

    @PostMapping("/specializations/{id}/delete")
    public String deleteSpecialization(@PathVariable int id, RedirectAttributes redirect) {
        if (!specializationDao.delete(id)) {
            redirect.addFlashAttribute("error",
                    "Do tej specjalizacji przypisani są lekarze – najpierw zmień ich specjalizację.");
        } else {
            redirect.addFlashAttribute("success", "Specjalizacja usunięta.");
        }
        return "redirect:/admin/specializations";
    }

    // ===================== uzytkownicy =====================

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userDao.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        User current = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (id == current.getId()) {
            redirect.addFlashAttribute("error", "Nie możesz usunąć własnego konta.");
        } else {
            userDao.delete(id);
            redirect.addFlashAttribute("success", "Konto użytkownika zostało usunięte.");
        }
        return "redirect:/admin/users";
    }

    // ===================== wizyty =====================

    @GetMapping("/visits")
    public String visits(@RequestParam(required = false) String status, Model model) {
        List<Appointment> visits = appointmentDao.findAll();
        if (status != null && !status.isBlank() && !"Wszystkie".equals(status)) {
            visits = visits.stream().filter(a -> status.equals(a.getStatus())).toList();
        }
        model.addAttribute("visits", visits);
        model.addAttribute("status", status == null ? "Wszystkie" : status);
        return "admin/visits";
    }
}
