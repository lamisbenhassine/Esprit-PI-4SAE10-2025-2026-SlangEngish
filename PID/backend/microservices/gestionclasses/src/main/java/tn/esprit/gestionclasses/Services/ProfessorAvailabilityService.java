package tn.esprit.gestionclasses.Services;



import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import tn.esprit.gestionclasses.DTO.ProfessorAvailabilityResponseDto;

import tn.esprit.gestionclasses.DTO.ProfessorAvailabilitySaveRequestDto;

import tn.esprit.gestionclasses.DTO.ProfessorAvailabilitySlotDto;

import tn.esprit.gestionclasses.Entities.ProfessorSessionAvailability;

import tn.esprit.gestionclasses.Repositories.ProfessorSessionAvailabilityRepository;



import java.util.ArrayList;

import java.util.HashSet;

import java.util.List;

import java.util.Set;

import java.util.stream.Collectors;



@Service

@RequiredArgsConstructor

public class ProfessorAvailabilityService {



    private static final int MAX_DESCRIPTION_LENGTH = 2000;



    private final ProfessorSessionAvailabilityRepository repository;



    public ProfessorAvailabilityResponseDto getAvailability(Long professorUserId) {

        List<ProfessorAvailabilitySlotDto> slots = repository.findByProfessorUserId(professorUserId).stream()

                .map(row -> ProfessorAvailabilitySlotDto.builder()

                        .slotCode(row.getSlotCode())

                        .description(row.getDescription() != null ? row.getDescription() : "")

                        .build())

                .sorted((a, b) -> a.getSlotCode().compareTo(b.getSlotCode()))

                .collect(Collectors.toList());

        return ProfessorAvailabilityResponseDto.builder().slots(slots).build();

    }



    @Transactional

    public ProfessorAvailabilityResponseDto save(Long professorUserId, ProfessorAvailabilitySaveRequestDto request) {

        List<ProfessorAvailabilitySlotDto> input = request.getSlots();

        if (input == null) {

            throw new IllegalArgumentException("slots est requis");

        }



        List<ProfessorAvailabilitySlotDto> cleaned = new ArrayList<>();

        Set<String> seenCodes = new HashSet<>();

        for (ProfessorAvailabilitySlotDto s : input) {

            if (s == null || s.getSlotCode() == null) {

                continue;

            }

            String code = s.getSlotCode().trim();

            if (code.isEmpty()) {

                continue;

            }

            if (!seenCodes.add(code)) {

                throw new IllegalArgumentException("Créneau dupliqué: " + code);

            }

            String desc = s.getDescription() == null ? "" : s.getDescription().trim();

            if (desc.length() > MAX_DESCRIPTION_LENGTH) {

                throw new IllegalArgumentException(

                        "La description ne peut pas dépasser " + MAX_DESCRIPTION_LENGTH + " caractères.");

            }

            cleaned.add(ProfessorAvailabilitySlotDto.builder()

                    .slotCode(code)

                    .description(desc)

                    .build());

        }



        if (cleaned.size() > 3) {

            throw new IllegalArgumentException("Vous pouvez choisir au plus 3 séances (un seul créneau par jour).");

        }



        Set<String> days = new HashSet<>();

        for (ProfessorAvailabilitySlotDto item : cleaned) {

            String code = item.getSlotCode();

            if (!SessionSlotRegistry.ALLOWED_CODES.contains(code)) {

                throw new IllegalArgumentException("Créneau inconnu: " + code);

            }

            String day = SessionSlotRegistry.dayKey(code);

            if (day == null || !days.add(day)) {

                throw new IllegalArgumentException(

                        "Un seul créneau par jour est autorisé (jour en conflit pour: " + code + ").");

            }

        }



        repository.deleteByProfessorUserId(professorUserId);

        repository.flush();



        if (!cleaned.isEmpty()) {

            List<ProfessorSessionAvailability> rows = cleaned.stream()

                    .map(item -> {

                        ProfessorSessionAvailability row = new ProfessorSessionAvailability();

                        row.setProfessorUserId(professorUserId);

                        row.setSlotCode(item.getSlotCode());

                        String d = item.getDescription();

                        row.setDescription(d != null && !d.isEmpty() ? d : null);

                        return row;

                    })

                    .collect(Collectors.toList());

            repository.saveAll(rows);

        }



        return getAvailability(professorUserId);

    }

}

