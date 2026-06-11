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
import pl.wspa.medivisit.model.Appointment;
import pl.wspa.medivisit.model.Doctor;
import pl.wspa.medivisit.model.User;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorDao doctorDao = new DoctorDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    private Doctor doctor(HttpSession session) {
        User user = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        return doctorDao.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Brak profilu lekarza dla zalogowanego konta"));
    }

    @GetMapping("/day")
    public String day(@RequestParam(required = false) String date, HttpSession session, Model model) {
        Doctor doctor = doctor(session);
        LocalDate parsed;
        try {
            parsed = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        } catch (Exception e) {
            parsed = LocalDate.now();
        }
        model.addAttribute("doctor", doctor);
        model.addAttribute("date", parsed.toString());
        model.addAttribute("visits", appointmentDao.findByDoctorAndDate(doctor.getId(), parsed.toString()));
        return "doctor/day";
    }

    @GetMapping("/all")
    public String all(HttpSession session, Model model) {
        Doctor doctor = doctor(session);
        model.addAttribute("doctor", doctor);
        model.addAttribute("visits", appointmentDao.findByDoctor(doctor.getId()));
        return "doctor/all";
    }

    @PostMapping("/visits/{id}/complete")
    public String complete(@PathVariable int id,
                           @RequestParam(required = false, defaultValue = "") String notes,
                           @RequestParam(required = false) String date,
                           HttpSession session, RedirectAttributes redirect) {
        Optional<Appointment> appointment = appointmentDao.findById(id);
        if (appointment.isEmpty()
                || appointment.get().getDoctorId() != doctor(session).getId()
                || !Appointment.STATUS_PLANNED.equals(appointment.get().getStatus())) {
            redirect.addFlashAttribute("error", "Tej wizyty nie można oznaczyć jako odbytej.");
        } else {
            appointmentDao.complete(id, notes.trim());
            redirect.addFlashAttribute("success", "Wizyta oznaczona jako odbyta, zalecenia zapisane.");
        }
        return "redirect:/doctor/day" + (date == null ? "" : "?date=" + date);
    }

    @PostMapping("/visits/{id}/cancel")
    public String cancel(@PathVariable int id, @RequestParam(required = false) String date,
                         HttpSession session, RedirectAttributes redirect) {
        Optional<Appointment> appointment = appointmentDao.findById(id);
        if (appointment.isEmpty()
                || appointment.get().getDoctorId() != doctor(session).getId()
                || !Appointment.STATUS_PLANNED.equals(appointment.get().getStatus())) {
            redirect.addFlashAttribute("error", "Tej wizyty nie można anulować.");
        } else {
            appointmentDao.updateStatus(id, Appointment.STATUS_CANCELLED);
            redirect.addFlashAttribute("success", "Wizyta została anulowana.");
        }
        return "redirect:/doctor/day" + (date == null ? "" : "?date=" + date);
    }
}
