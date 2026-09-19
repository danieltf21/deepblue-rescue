package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired private RescueCenterRepository rescueCenterRepository;
    @Autowired private RescueCaseRepository rescueCaseRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private MedicalRecordRepository medicalRecordRepository;
    @Autowired private SpecialistRepository specialistRepository;
    @Autowired private ExpertiseRepository expertiseRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void flywayExecutedMigrations() {
        var versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history ORDER BY installed_rank",
                String.class
        );
        assertThat(versions).contains("1", "2");
    }

    @Test
    void inheritedMethodsWork() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCenter saved = rescueCenterRepository.save(center);
        assertThat(saved.getId()).isNotNull();

        Optional<RescueCenter> found = rescueCenterRepository.findById(saved.getId());
        assertThat(found).isPresent();

        boolean exists = rescueCenterRepository.existsById(saved.getId());
        assertThat(exists).isTrue();

        long count = rescueCenterRepository.count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void oneToManyRescueCenterCases() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta"));

        RescueCase case1 = new RescueCase("RES-N-001", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-N-002", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);

        center.addCase(case1);
        center.addCase(case2);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-CAR");
        assertThat(cases).hasSize(2);
        assertThat(cases).allMatch(c -> c.getRescueCenter().getId().equals(center.getId()));
    }

    @Test
    void oneToOneCaseAnimal() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-C1", "Center 1", "Santa Marta"));

        RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        rescueCaseRepository.saveAndFlush(rescueCase);

        Optional<RescueCase> reloadedCase = rescueCaseRepository.findByCaseCode("RES-2026-001");
        assertThat(reloadedCase).isPresent();
        assertThat(reloadedCase.get().getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(reloadedCase.get().getAnimal().getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
    }

    @Test
    void oneToOneAnimalMedicalRecord() {
        Animal animal = createAnimalWithCase("DB-MR1", "RES-MR1", "AN-2026-002");

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"),
                "STABLE",
                "Left front flipper injury",
                null
        );

        animal.assignMedicalRecord(record);
        Animal savedAnimal = animalRepository.saveAndFlush(animal);

        assertThat(savedAnimal.getId()).isNotNull();
        assertThat(savedAnimal.getMedicalRecord().getId()).isNotNull();
    }

    @Test
    void manyToManySpecialistExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseThrow(() -> new IllegalStateException("Trauma no existe, revisa V2"));
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseThrow(() -> new IllegalStateException("Rehabilitation no existe, revisa V2"));

        Specialist elena = new Specialist("SPEC-N1", "Elena", "Vargas", "elena.n1@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        Specialist saved = specialistRepository.saveAndFlush(elena);

        assertThat(saved.getExpertiseAreas()).hasSize(2);
    }

    @Test
    void queryMethodByStatus() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-ST1", "Status Center", "Santa Marta"));

        RescueCase case1 = new RescueCase("RES-001", LocalDate.now(), "loc1", RescueStatus.IN_REHABILITATION);
        RescueCase case2 = new RescueCase("RES-002", LocalDate.now(), "loc2", RescueStatus.READY_FOR_RELEASE);
        RescueCase case3 = new RescueCase("RES-003", LocalDate.now(), "loc3", RescueStatus.IN_REHABILITATION);

        center.addCase(case1);
        center.addCase(case2);
        center.addCase(case3);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);
        rescueCaseRepository.saveAndFlush(case3);

        List<RescueCase> result = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(result).hasSize(2);
    }

    @Test
    void queryMethodNavigatingToCenter() {
        RescueCenter centerCar = rescueCenterRepository.save(new RescueCenter("DB-CAR2", "Caribbean", "Santa Marta"));
        RescueCenter centerPac = rescueCenterRepository.save(new RescueCenter("DB-PAC2", "Pacific", "Buenaventura"));

        RescueCase caseCar = new RescueCase("RES-CAR-1", LocalDate.now(), "loc", RescueStatus.ADMITTED);
        centerCar.addCase(caseCar);
        rescueCaseRepository.save(caseCar);
        Animal animalCar = new Animal("AN-CAR-1", "Turtle Car", "Chelonioidea", AnimalSex.FEMALE);
        caseCar.assignAnimal(animalCar);
        rescueCaseRepository.saveAndFlush(caseCar);

        RescueCase casePac = new RescueCase("RES-PAC-1", LocalDate.now(), "loc", RescueStatus.ADMITTED);
        centerPac.addCase(casePac);
        rescueCaseRepository.save(casePac);
        Animal animalPac = new Animal("AN-PAC-1", "Turtle Pac", "Chelonioidea", AnimalSex.MALE);
        casePac.assignAnimal(animalPac);
        rescueCaseRepository.saveAndFlush(casePac);

        List<Animal> animalsFromCar = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR2");

        assertThat(animalsFromCar).hasSize(1);
        assertThat(animalsFromCar.get(0).getAnimalCode()).isEqualTo("AN-CAR-1");
    }

    @Test
    void jpqlSpecialistsByExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise mammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise birds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SPEC-E1", "Elena", "Vargas", "elena.e1@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist("SPEC-E2", "Mateo", "Rios", "mateo.e2@deepblue.org");
        mateo.addExpertise(mammals);
        mateo.addExpertise(rehab);
        specialistRepository.save(mateo);

        Specialist sofia = new Specialist("SPEC-E3", "Sofia", "Diaz", "sofia.e3@deepblue.org");
        sofia.addExpertise(birds);
        sofia.addExpertise(trauma);
        specialistRepository.saveAndFlush(sofia);

        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");

        assertThat(traumaSpecialists)
                .extracting(Specialist::getProfessionalCode)
                .containsExactlyInAnyOrder("SPEC-E1", "SPEC-E3");
    }

    @Test
    void treatmentsOrderedChronologically() {
        Specialist elena = specialistRepository.save(
                new Specialist("SPEC-T1", "Elena", "Vargas", "elena.t1@deepblue.org"));
        Specialist mateo = specialistRepository.save(
                new Specialist("SPEC-T2", "Mateo", "Rios", "mateo.t2@deepblue.org"));

        Animal animal = createAnimalWithCase("DB-TR1", "RES-TR1", "AN-T1");

        Treatment t1 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 1, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning flipper");
        Treatment t2 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 2, 9, 0),
                TreatmentType.HYDRATION, "Fluid therapy");
        Treatment t3 = new Treatment(animal, mateo, LocalDateTime.of(2026, 8, 3, 9, 0),
                TreatmentType.OBSERVATION, "Routine check");

        treatmentRepository.save(t1);
        treatmentRepository.save(t2);
        treatmentRepository.saveAndFlush(t3);

        List<Treatment> ordered = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertThat(ordered).hasSize(3);
        assertThat(ordered.get(0).getType()).isEqualTo(TreatmentType.WOUND_CARE);
        assertThat(ordered.get(1).getType()).isEqualTo(TreatmentType.HYDRATION);
        assertThat(ordered.get(2).getType()).isEqualTo(TreatmentType.OBSERVATION);
    }

    @Test
    void jpqlTreatmentsBetweenDates() {
        Specialist specialist = specialistRepository.save(
                new Specialist("SPEC-I1", "Elena", "Vargas", "elena.i1@deepblue.org"));
        Animal animal = createAnimalWithCase("DB-IN1", "RES-IN1", "AN-I1");

        treatmentRepository.save(new Treatment(animal, specialist,
                LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "t1"));
        treatmentRepository.save(new Treatment(animal, specialist,
                LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "t2"));
        treatmentRepository.saveAndFlush(new Treatment(animal, specialist,
                LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "t3"));

        List<Treatment> result = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPerformedAt()).isEqualTo(LocalDateTime.of(2026, 8, 10, 10, 0));
    }

    @Test
    void uniqueConstraintViolation() {
        createAnimalWithCase("DB-UQ1", "RES-UQ1", "AN-100");

        RescueCenter center2 = rescueCenterRepository.save(
                new RescueCenter("DB-UQ2", "Center UQ2", "Santa Marta"));
        RescueCase case2 = new RescueCase("RES-UQ2", LocalDate.now(), "loc", RescueStatus.ADMITTED);
        center2.addCase(case2);
        rescueCaseRepository.save(case2);

        Animal duplicate = new Animal("AN-100", "Duplicate turtle", null, AnimalSex.MALE);
        case2.assignAnimal(duplicate);

        assertThatThrownBy(() -> rescueCaseRepository.saveAndFlush(case2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }     @Test
    void integratorScenario() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta"));

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100", LocalDate.of(2026, 8, 18), "Bahia Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCase = rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animal = animalRepository.saveAndFlush(animal);

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion");
        animal.assignMedicalRecord(record);
        animal = animalRepository.saveAndFlush(animal);

        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        elena = specialistRepository.saveAndFlush(elena);

        Treatment t1 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 18, 11, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        Treatment t2 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 18, 12, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy");

        treatmentRepository.save(t1);
        treatmentRepository.saveAndFlush(t2);

        assertThat(animal.getId()).isNotNull();
        assertThat(animal.getMedicalRecord().getId()).isNotNull();
        assertThat(elena.getExpertiseAreas()).hasSize(3);
        assertThat(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId())).hasSize(2);
    }     @Test
    void integratorScenarioQueries() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR2", "DeepBlue Caribbean", "Santa Marta"));

        RescueCase rescueCase = new RescueCase(
                "RES-2026-200", LocalDate.of(2026, 8, 18), "Bahia Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCase = rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-2026-200", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animal = animalRepository.saveAndFlush(animal);

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-002", "Elena", "Vargas", "elena2@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        elena = specialistRepository.saveAndFlush(elena);

        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 18, 11, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper"));
        treatmentRepository.saveAndFlush(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 18, 12, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy"));

        boolean caseExists = rescueCaseRepository.findByCaseCode("RES-2026-200").isPresent();
        assertThat(caseExists).isTrue();

        List<RescueCase> inRehab = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertThat(inRehab).isNotEmpty();

        List<Animal> animalsInCenter = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR2");
        assertThat(animalsInCenter).hasSize(1);

        List<Animal> turtles = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
        assertThat(turtles).isNotEmpty();

        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(traumaSpecialists).extracting(Specialist::getProfessionalCode).contains("SPEC-002");

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(treatments).hasSize(2);

        List<Treatment> byRehabExpertise = treatmentRepository.findBySpecialistExpertise("Rehabilitation");
        assertThat(byRehabExpertise).hasSize(2);


        List<Treatment> betweenDates = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 19, 0, 0));
        assertThat(betweenDates).hasSize(2);
    }

    private Animal createAnimalWithCase(String centerCode, String caseCode, String animalCode) {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter(centerCode, "Center " + centerCode, "Santa Marta"));

        RescueCase rescueCase = new RescueCase(caseCode, LocalDate.now(), "loc", RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        rescueCase = rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal(animalCode, "Turtle " + animalCode, "Chelonioidea", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        return animalRepository.saveAndFlush(animal);
    }
}