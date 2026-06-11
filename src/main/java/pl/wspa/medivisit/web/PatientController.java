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
import pl.wspa.medivisit.model.Appointment;
import pl.wspa.medivisit.model.Doctor;
import pl.wspa.medivisit.model.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/patient")
public class PatientController {

    private final SpecializationDao specializationDao = new SpecializationDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    private User user(HttpSession session) {
        return (User) session.getAttribute(AuthInterceptor.SESSION_USER);
    }

    @GetMapping("/book")
    public String book(@RequestParam(required = false) Integer specId,
                       @RequestParam(required = false) Integer doctorId,
                       @RequestParam(required = false) String date,
                       Model model) {
        model.addAttribute("specs", specializationDao.findAll());
        model.addAttribute("specId", specId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("date", date);
        model.addAttribute("minDate", LocalDate.now().toString());

        if (specId != null) {
            model.addAttribute("doctors", doctorDao.findBySpecialization(specId));
        }
        if (doctorId != null) {
            Optional<Doctor> doctor = doctorDao.findById(doctorId);
            doctor.ifPresent(d -> model.addAttribute("doctor", d));
            if (doctor.isPresent() && date != null && !date.isBlank()) {
                LocalDate parsed = parseDate(date);
                if (parsed == null || parsed.isBefore(LocalDate.now())) {
                    model.addAttribute("dateError", "Wybierz dzisiejszą lub przyszłą datę.");
                } else if (parsed.getDayOfWeek() == DayOfWeek.SATURDAY
                        || parsed.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    model.addAttribute("dateError", "Przychodnia przyjmuje od poniedziałku do piątku.");
                } else {
                    model.addAttribute("slots", appointmentDao.findFreeSlots(doctor.get(), parsed));
                }
            }
        }
        return "patient/book";
    }

    @PostMapping("/book")
    public String doBook(@RequestParam int doctorId, @RequestParam String date,
                         @RequestParam String time,
                         @RequestParam(required = false, defaultValue = "") String reason,
                         HttpSession session, RedirectAttributes redirect) {
        Optional<Doctor> doctor = doctorDao.findById(doctorId);
        LocalDate parsed = parseDate(date);
        if (doctor.isEmpty() || parsed == null || parsed.isBefore(LocalDate.now())
                || parsed.getDayOfWeek().getValue() > 5) {
            redirect.addFlashAttribute("error", "Nieprawidłowe dane rezerwacji.");
            return "redirect:/patient/book";
        }
        if (!appointmentDao.findFreeSlots(doctor.get(), parsed).contains(time)) {
            redirect.addFlashAttribute("error", "Ten termin został właśnie zajęty – wybierz inną godzinę.");
            return "redirect:/patient/book?specId=" + doctor.get().getSpecializationId()
                    + "&doctorId=" + doctorId + "&date=" + date;
        }
        boolean ok = appointmentDao.book(user(session).getId(), doctorId, date, time,
                reason.length() > 500 ? reason.substring(0, 500) : reason.trim());
        if (!ok) {
            redirect.addFlashAttribute("error", "Ten termin został właśnie zajęty – wybierz inną godzinę.");
            return "redirect:/patient/book?specId=" + doctor.get().getSpecializationId()
                    + "&doctorId=" + doctorId + "&date=" + date;
        }
        redirect.addFlashAttribute("success", "Wizyta zarezerwowana: " + date + " godz. " + time
                + ", " + doctor.get().getFullName() + " (gabinet " + doctor.get().getRoom() + ").");
        return "redirect:/patient/visits";
    }

    @GetMapping("/visits")
    public String visits(HttpSession session, Model model) {
        List<Appointment> visits = appointmentDao.findByPatient(user(session).getId());
        model.addAttribute("visits", visits);
        return "patient/visits";
    }

    @PostMapping("/visits/{id}/cancel")
    public String cancel(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        Optional<Appointment> appointment = appointmentDao.findById(id);
        if (appointment.isEmpty()
                || appointment.get().getPatientId() != user(session).getId()
                || !Appointment.STATUS_PLANNED.equals(appointment.get().getStatus())
                || !appointment.get().isUpcoming()) {
            redirect.addFlashAttribute("error", "Tej wizyty nie można anulować.");
        } else {
            appointmentDao.updateStatus(id, Appointment.STATUS_CANCELLED);
            redirect.addFlashAttribute("success", "Wizyta została anulowana.");
        }
        return "redirect:/patient/visits";
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}
