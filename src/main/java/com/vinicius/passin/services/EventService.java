package com.vinicius.passin.services;

import com.vinicius.passin.domain.attendee.Attendee;
import com.vinicius.passin.domain.event.Event;
import com.vinicius.passin.domain.event.exceptions.EventFullException;
import com.vinicius.passin.domain.event.exceptions.EventNotFoundException;
import com.vinicius.passin.dto.attendee.AttendeeIdDTO;
import com.vinicius.passin.dto.attendee.AttendeeRequestDTO;
import com.vinicius.passin.dto.event.EventIdDTO;
import com.vinicius.passin.dto.event.EventRequestDTO;
import com.vinicius.passin.dto.event.EventResponseDTO;
import com.vinicius.passin.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final AttendeeService attendeeService;

    public EventResponseDTO getEventDetail(String eventId) {
        Event event = this.getEventById(eventId);
        List<Attendee> attendeeList = this.attendeeService.getAllAttendeesFromEvents(eventId);
        return new EventResponseDTO(event, attendeeList.size());
    }

    public EventIdDTO createEvent(EventRequestDTO eventDTO){
        Event newEvent = new Event();

        newEvent.setTitle(eventDTO.title());
        newEvent.setDetails(eventDTO.details());
        newEvent.setMaximumAttendees(eventDTO.maximumAttendees());
        newEvent.setSlug(this.createSlug(eventDTO.title()));

        this.eventRepository.save(newEvent);

        return new EventIdDTO(newEvent.getId());
    }

    public AttendeeIdDTO registerAttendeeOnEvent(String eventId, AttendeeRequestDTO attendeeRequestDTO){
        this.attendeeService.verifyAttendeeSubscription(attendeeRequestDTO.email(), eventId);

        Event event = this.getEventById(eventId);
        List<Attendee> attendeeList = this.attendeeService.getAllAttendeesFromEvents(eventId);

        if(event.getMaximumAttendees() <= attendeeList.size()) throw new EventFullException("Event is full");

        Attendee newAttendee = new Attendee();
        newAttendee.setName(attendeeRequestDTO.name());
        newAttendee.setEmail(attendeeRequestDTO.email());
        newAttendee.setEvent(event);
        newAttendee.setCreatedAt(LocalDateTime.now());
        this.attendeeService.registerAttendee(newAttendee);

        return new AttendeeIdDTO(newAttendee.getId());
    }

    private Event getEventById(String eventId){
        return this.eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException("Event not found with ID:" + eventId));
    }


    private String createSlug(String text){
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
            return normalized.replaceAll("[\\p{InCOMBINING_DIACRITICAL_MARKS}]", "")
                    .replaceAll("[^\\w\\s]", "")
                    .replaceAll("\\s+", "-")
                    .toLowerCase();
    }
}
