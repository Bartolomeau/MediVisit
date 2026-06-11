# MediVisit – system rejestracji wizyt w przychodni

![logo](assets/logo.svg)

Projekt zespołowy systemu informatycznego – WSPA Lublin, Informatyka, semestr VI.

Repozytorium: https://github.com/Bartolomeau/MediVisit

Aplikacja **webowa** (Java 17 + Spring Boot + Thymeleaf) do zarządzania przychodnią
lekarską
pacjenci rezerwują wizyty online, lekarze prowadzą grafik i zapisują zalecenia,
administrator zarządza całym systemem.

## Funkcjonalności

### Pacjent
- rejestracja konta i logowanie,
- umawianie wizyt: wybór specjalizacji → lekarza → daty → **wolnej godziny**
  (aplikacja pokazuje wyłącznie wolne sloty z grafiku lekarza, z pominięciem
  godzin już zajętych i minionych),
- przegląd swoich wizyt, anulowanie nadchodzących wizyt,
- podgląd zaleceń lekarza po odbytej wizycie,
- edycja profilu i zmiana hasła.

### Lekarz
- grafik dnia (lista wizyt na wybraną datę),
- oznaczanie wizyt jako odbyte wraz z zapisem zaleceń dla pacjenta,
- anulowanie wizyt, historia wszystkich wizyt,
- edycja profilu i zmiana hasła.

### Administrator
- statystyki przychodni (liczby pacjentów, lekarzy, wizyt + wykres wizyt
  według specjalizacji),
- zarządzanie lekarzami (dodawanie z kontem, edycja grafiku/gabinetu, usuwanie),
- zarządzanie specjalizacjami (CRUD),
- zarządzanie użytkownikami (przegląd, usuwanie kont),
- przegląd wszystkich wizyt z filtrem po statusie.

## Bezpieczeństwo
- hasła haszowane algorytmem **bcrypt** (jBCrypt, koszt 10),
- walidacja danych po stronie serwera i przeglądarki (e-mail, telefon,
  siła hasła, pola obowiązkowe, poprawność dat),
- kontrola dostępu na podstawie ról – interceptor blokuje wejście na cudze
  panele (`/patient`, `/doctor`, `/admin`) i wymusza zalogowanie,
- wszystkie zapytania SQL parametryzowane (`PreparedStatement`) – ochrona
  przed SQL injection,
- szablony Thymeleaf domyślnie escapują dane – ochrona przed XSS,
- unikalność terminów wymuszana na poziomie bazy (`UNIQUE(doctor_id, date, time)`).

## Technologie
| Warstwa | Technologia |
|---|---|
| Język | Java 17 |
| Backend | Spring Boot 3 (Spring MVC, wbudowany Tomcat) |
| Frontend | Thymeleaf + własny arkusz CSS |
| Baza danych | SQLite (sqlite-jdbc) – plik `medivisit.db` |
| Hasła | jBCrypt |
| Budowanie | Maven (spring-boot-maven-plugin → uruchamialny JAR) |

Struktura bazy danych: [`src/main/resources/schema.sql`](src/main/resources/schema.sql)
(4 tabele: `users`, `specializations`, `doctors`, `appointments`).

## Instrukcja uruchomienia

### Wymagania
- Java (JDK) 17 lub nowsza – np. [Adoptium Temurin](https://adoptium.net/)
- Apache Maven 3.8+ (tylko do budowania ze źródeł)

### Sposób 1: gotowy plik JAR
```
java -jar medivisit-1.0.0.jar
```
Następnie otwórz w przeglądarce: **http://localhost:8080**

### Sposób 2: budowanie ze źródeł
```
mvn package
java -jar target/medivisit-1.0.0.jar
```

Przy pierwszym uruchomieniu aplikacja sama tworzy plik bazy `medivisit.db`
(struktura ze `schema.sql`) i wypełnia go danymi startowymi – nie trzeba
instalować ani konfigurować żadnego serwera bazy danych. Aby zacząć od
czystej bazy, wystarczy usunąć plik `medivisit.db` i uruchomić ponownie.

### Konta demonstracyjne
| Rola | Login | Hasło |
|---|---|---|
| Administrator | `admin@medivisit.pl` | `Admin123!` |
| Lekarz | `j.kowalski@medivisit.pl` | `Lekarz123!` |
| Pacjent | `pacjent@medivisit.pl` | `Pacjent123!` |

Nowych pacjentów można rejestrować z poziomu strony logowania
(„Nie masz konta? Zarejestruj się”).

## Struktura projektu
```
MediVisit/
├── pom.xml                       konfiguracja Maven
├── assets/logo.svg               logo aplikacji
├── src/main/resources/
│   ├── schema.sql                definicja struktury bazy danych
│   ├── application.properties    konfiguracja Spring Boot
│   ├── static/                   CSS i logo
│   └── templates/                szablony Thymeleaf (widoki)
└── src/main/java/pl/wspa/medivisit/
    ├── Main.java                 start aplikacji (Spring Boot)
    ├── db/                       połączenie z bazą, inicjalizacja, dane startowe
    ├── dao/                      operacje na danych (CRUD)
    ├── model/                    klasy encji
    ├── util/                     bcrypt, walidacja
    └── web/                      kontrolery, kontrola dostępu (interceptor)
```
